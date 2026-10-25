#include <jni.h>
#include "llama.h"
#include "utf8_stream.h"
#include "canal_pensamento.h"
#include "limite_contexto.h"
#include <algorithm>
#include <atomic>
#include <climits>
#include <cstdint>
#include <cstdlib>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>

namespace {
using Modelo=std::unique_ptr<llama_model,decltype(&llama_model_free)>;
using Contexto=std::unique_ptr<llama_context,decltype(&llama_free)>;
using Amostrador=std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)>;
struct Motor {
 std::atomic<bool> cancelado{false};std::mutex trabalho;
 Modelo modelo{nullptr,llama_model_free};Contexto contexto{nullptr,llama_free};
 ~Motor(){contexto.reset();modelo.reset();}
};
std::mutex registroMutex;std::unordered_map<jlong,std::shared_ptr<Motor>> registro;
std::atomic<jlong> proximo{1};std::once_flag iniciado;
std::shared_ptr<Motor> obter(jlong id){std::lock_guard<std::mutex> lock(registroMutex);auto i=registro.find(id);if(i==registro.end())throw std::runtime_error("O motor local foi fechado.");return i->second;}
bool abortar(void *m){return static_cast<Motor*>(m)->cancelado.load(std::memory_order_relaxed);}
bool progresso(float,void *m){return !abortar(m);}
void erro(JNIEnv *env,const std::exception &e){if(env->ExceptionCheck())return;jclass tipo=env->FindClass("java/io/IOException");if(tipo)env->ThrowNew(tipo,e.what());}
void utf8(std::string &s,uint32_t c){
 if(c<0x80)s.push_back(static_cast<char>(c));
 else if(c<0x800){s.push_back(static_cast<char>(0xc0|(c>>6)));s.push_back(static_cast<char>(0x80|(c&63)));}
 else if(c<0x10000){s.push_back(static_cast<char>(0xe0|(c>>12)));s.push_back(static_cast<char>(0x80|((c>>6)&63)));s.push_back(static_cast<char>(0x80|(c&63)));}
 else{s.push_back(static_cast<char>(0xf0|(c>>18)));s.push_back(static_cast<char>(0x80|((c>>12)&63)));s.push_back(static_cast<char>(0x80|((c>>6)&63)));s.push_back(static_cast<char>(0x80|(c&63)));}
}
std::string texto(JNIEnv *env,jstring v){
 if(!v)throw std::runtime_error("O histórico contém texto nulo.");
 const jsize n=env->GetStringLength(v);const jchar *b=env->GetStringChars(v,nullptr);
 if(!b)throw std::runtime_error("Não há memória para ler o texto.");
 std::string s;
 try{for(jsize i=0;i<n;i++){uint32_t c=b[i];if(c>=0xd800&&c<=0xdbff&&i+1<n&&b[i+1]>=0xdc00&&b[i+1]<=0xdfff){c=0x10000+((c-0xd800)<<10)+(b[++i]-0xdc00);}else if(c>=0xd800&&c<=0xdfff)c=0xfffd;utf8(s,c);}}
 catch(...){env->ReleaseStringChars(v,b);throw;}
 env->ReleaseStringChars(v,b);return s;
}
void emitir(JNIEnv *env,jclass classe,jmethodID entregar,jobject saida,const std::string &s){
 if(s.empty())return;
 jbyteArray b=env->NewByteArray(static_cast<jsize>(s.size()));if(!b)throw std::runtime_error("Memória insuficiente para entregar a resposta.");
 env->SetByteArrayRegion(b,0,static_cast<jsize>(s.size()),reinterpret_cast<const jbyte*>(s.data()));
 if(!env->ExceptionCheck())env->CallStaticVoidMethod(classe,entregar,saida,b);
 env->DeleteLocalRef(b);if(env->ExceptionCheck())throw std::runtime_error("A saída interrompeu a resposta.");
}
struct LimparCache {llama_context *ctx; explicit LimparCache(llama_context *c):ctx(c){llama_memory_clear(llama_get_memory(ctx),true);}~LimparCache(){llama_memory_clear(llama_get_memory(ctx),true);}};
std::string formatar(JNIEnv *env,Motor &m,jobjectArray papeis,jobjectArray mensagens){
 const jsize n=env->GetArrayLength(papeis);
 if(n<1||n>256||env->GetArrayLength(mensagens)!=n)throw std::runtime_error("O histórico deve ter de 1 a 256 mensagens e papéis correspondentes.");
 std::vector<std::string> roles,txts;roles.reserve(n);txts.reserve(n);size_t bytes=0;
 for(jsize i=0;i<n;i++){
  jstring r=static_cast<jstring>(env->GetObjectArrayElement(papeis,i));jstring t=static_cast<jstring>(env->GetObjectArrayElement(mensagens,i));
  try{roles.push_back(texto(env,r));txts.push_back(texto(env,t));}catch(...){if(r)env->DeleteLocalRef(r);if(t)env->DeleteLocalRef(t);throw;}
  env->DeleteLocalRef(r);env->DeleteLocalRef(t);
  if(roles.back()!="system"&&roles.back()!="user"&&roles.back()!="assistant")throw std::runtime_error("Papel inválido: use system, user ou assistant.");
  if(txts.back().find('\0')!=std::string::npos)throw std::runtime_error("A mensagem contém um caractere nulo inválido.");
  bytes+=txts.back().size();if(bytes>1024*1024)throw std::runtime_error("Histórico muito grande. Inicie uma nova conversa.");
 }
 if(roles.back()!="user")throw std::runtime_error("O histórico deve terminar com uma mensagem user.");
 std::vector<llama_chat_message> chat;chat.reserve(n);for(jsize i=0;i<n;i++)chat.push_back({roles[i].c_str(),txts[i].c_str()});
 const char *modelo=llama_model_chat_template(m.modelo.get(),nullptr);if(!modelo)throw std::runtime_error("Este GGUF não contém um template de conversa. Use o modelo indicado.");
 int32_t tam=llama_chat_apply_template(modelo,chat.data(),chat.size(),true,nullptr,0);
 if(tam<0)throw std::runtime_error("O template de conversa deste GGUF não é suportado.");
 std::vector<char> b(static_cast<size_t>(tam)+1);
 int32_t usado=llama_chat_apply_template(modelo,chat.data(),chat.size(),true,b.data(),static_cast<int32_t>(b.size()));
 if(usado<0||usado>tam)throw std::runtime_error("Falha ao aplicar o template de conversa.");
 return std::string(b.data(),usado);
}
std::vector<llama_token> tokenizar(const llama_vocab *v,const std::string &s,bool especiais=true){
 int32_t n=llama_tokenize(v,s.data(),static_cast<int32_t>(s.size()),nullptr,0,especiais,true);
 if(n==INT32_MIN||n==0)throw std::runtime_error("Falha no tamanho do tokenizador.");
 std::vector<llama_token> tokens(static_cast<size_t>(n<0?-n:n));
 n=llama_tokenize(v,s.data(),static_cast<int32_t>(s.size()),tokens.data(),static_cast<int32_t>(tokens.size()),especiais,true);
 if(n<0)throw std::runtime_error("Falha ao tokenizar o histórico.");
 tokens.resize(n);return tokens;
}
bool decodificar(Motor &m,llama_token *tokens,int32_t n){
 if(abortar(&m))return false;
 int status=llama_decode(m.contexto.get(),llama_batch_get_one(tokens,n));
 if(status==2&&abortar(&m))return false;
 if(status!=0)throw std::runtime_error("Falha na inferência CPU (código "+std::to_string(status)+"). Reduza o contexto ou recarregue o modelo.");
 return !abortar(&m);
}
int metaInteiro(llama_model *modelo,const std::string &chave,int padrao){char valor[64];int n=llama_model_meta_val_str(modelo,chave.c_str(),valor,sizeof(valor));if(n<=0||n>=static_cast<int>(sizeof(valor)))return padrao;int v=std::atoi(valor);return v>0?v:padrao;}
uint64_t cachePorToken(llama_model *modelo){
 char arquitetura[64];int n=llama_model_meta_val_str(modelo,"general.architecture",arquitetura,sizeof(arquitetura));
 std::string prefixo=n>0&&n<static_cast<int>(sizeof(arquitetura))?std::string(arquitetura)+".attention.":"";
 int dimensao=llama_model_n_embd(modelo)/std::max(1,llama_model_n_head(modelo));
 int chaves=metaInteiro(modelo,prefixo+"key_length",dimensao),valores=metaInteiro(modelo,prefixo+"value_length",chaves);
 return 2ULL*llama_model_n_layer(modelo)*llama_model_n_head_kv(modelo)*(chaves+valores);
}
}

extern "C" JNIEXPORT jlong JNICALL Java_br_com_anderflow_metrys_offline_MotorLocal_criarNativo(JNIEnv *env,jclass){
 try{std::call_once(iniciado,[]{llama_backend_init();});auto m=std::make_shared<Motor>();jlong id=proximo.fetch_add(1);std::lock_guard<std::mutex> lock(registroMutex);registro.emplace(id,m);return id;}catch(const std::exception&e){erro(env,e);return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_br_com_anderflow_metrys_offline_MotorLocal_carregarNativo(JNIEnv *env,jclass,jlong id,jstring arquivo,jint contexto,jint threads,jlong memoriaDisponivel){
 try{
  auto m=obter(id);std::lock_guard<std::mutex> lock(m->trabalho);m->cancelado=false;m->contexto.reset();m->modelo.reset();
  if((contexto!=0&&(contexto<512||contexto>1048576))||threads<1||threads>8||memoriaDisponivel<0)throw std::runtime_error("Use contexto automático (0) ou de 512 a 1048576, e de 1 a 8 threads.");
  std::string caminho=texto(env,arquivo);if(caminho.empty()||caminho.find('\0')!=std::string::npos)throw std::runtime_error("Caminho de modelo inválido.");
  auto mp=llama_model_default_params();mp.load_mode=LLAMA_LOAD_MODE_MMAP;mp.n_gpu_layers=0;mp.progress_callback=progresso;mp.progress_callback_user_data=m.get();
  Modelo modelo(llama_model_load_from_file(caminho.c_str(),mp),llama_model_free);
  if(!modelo)throw std::runtime_error(abortar(m.get())?"Carregamento cancelado.":"Não foi possível abrir o GGUF. Confira o download e o espaço livre.");
  int escolhido=contextoPelaMemoria(contexto,llama_model_n_ctx_train(modelo.get()),static_cast<uint64_t>(memoriaDisponivel),llama_model_size(modelo.get()),cachePorToken(modelo.get()));
  if(escolhido==0)throw std::runtime_error("Memória disponível insuficiente para este modelo e seu contexto. Feche outros aplicativos ou escolha um modelo menor.");
  auto cp=llama_context_default_params();cp.n_ctx=static_cast<uint32_t>(escolhido);cp.n_batch=128;cp.n_ubatch=128;cp.n_threads=threads;cp.n_threads_batch=threads;
  Contexto ctx(nullptr,llama_free);
  while(cp.n_ctx>=512&&!abortar(m.get())){ctx.reset(llama_init_from_model(modelo.get(),cp));if(ctx)break;cp.n_ctx/=2;}
  if(!ctx)throw std::runtime_error(abortar(m.get())?"Carregamento cancelado.":"Não há memória para criar o contexto. Escolha um modelo menor.");
  llama_set_abort_callback(ctx.get(),abortar,m.get());if(abortar(m.get()))throw std::runtime_error("Carregamento cancelado.");
  m->modelo=std::move(modelo);m->contexto=std::move(ctx);
 }catch(const std::exception&e){erro(env,e);}
}
extern "C" JNIEXPORT jint JNICALL Java_br_com_anderflow_metrys_offline_MotorLocal_gerarNativo(JNIEnv *env,jclass classe,jlong id,jobjectArray papeis,jobjectArray mensagens,jobject saida,jint maxTokens,jboolean raciocinio){
 try{
  auto m=obter(id);std::lock_guard<std::mutex> lock(m->trabalho);
  if(!m->modelo||!m->contexto)throw std::runtime_error("Carregue o modelo antes de gerar.");
  if(!papeis||!mensagens||!saida)throw std::runtime_error("Histórico e saída são obrigatórios.");
  m->cancelado=false;LimparCache limpar(m->contexto.get());auto prompt=formatar(env,*m,papeis,mensagens);
  if(raciocinio)prompt+="<think>\n";
  const auto *vocab=llama_model_get_vocab(m->modelo.get());auto tokens=tokenizar(vocab,prompt);int nctx=static_cast<int>(llama_n_ctx(m->contexto.get()));
  if(maxTokens<0||maxTokens>=nctx)throw std::runtime_error("Use geração automática (0) ou um limite positivo menor que o contexto.");
  int limite=nctx-(maxTokens==0?std::max(64,nctx/8):maxTokens);
  if(static_cast<int>(tokens.size())>limite)throw std::runtime_error("Histórico excede o contexto: "+std::to_string(tokens.size())+" tokens; limite "+std::to_string(limite)+". O histórico salvo não foi apagado. Reduza o anexo ou use um contexto maior.");
  int disponiveis=maxTokens==0?nctx-static_cast<int>(tokens.size()):maxTokens;
  Amostrador sampler(llama_sampler_chain_init(llama_sampler_chain_default_params()),llama_sampler_free);
  if(!sampler)throw std::runtime_error("Memória insuficiente para o amostrador.");
  llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_k(40));llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_p(.9f,1));llama_sampler_chain_add(sampler.get(),llama_sampler_init_temp(.65f));llama_sampler_chain_add(sampler.get(),llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
  jmethodID entregar=env->GetStaticMethodID(classe,"entregar","(Lbr/com/anderflow/metrys/offline/MotorLocal$Saida;[B)V");if(!entregar)throw std::runtime_error("Método de saída JNI não encontrado.");
  size_t lote=llama_n_batch(m->contexto.get());for(size_t p=0;p<tokens.size();p+=lote){int32_t n=static_cast<int32_t>(std::min(lote,tokens.size()-p));if(!decodificar(*m,tokens.data()+p,n))return 2;}
  MetrysUtf8 utf;std::vector<char> pedaco(256);
  const std::string fechamento="\n</think>\n\n";auto fecharTokens=tokenizar(vocab,fechamento,false);
  const int reservaFinal=std::max(16,disponiveis/3);
  CanalPensamento canal(raciocinio);int motivo=1;
  for(int i=0;i<disponiveis&&!abortar(m.get());){
   llama_token token=llama_sampler_sample(sampler.get(),m->contexto.get(),-1);
   bool terminou=llama_vocab_is_eog(vocab,token);
   if(canal.pensando()&&(terminou||i>=disponiveis-reservaFinal-static_cast<int>(fecharTokens.size()))&&i+static_cast<int>(fecharTokens.size())<disponiveis){
    // Reserva espaço para a resposta no mesmo contexto; nunca devolve só atividade.
    emitir(env,classe,entregar,saida,utf.receber(fechamento));
    if(!decodificar(*m,fecharTokens.data(),static_cast<int32_t>(fecharTokens.size())))return 2;
    i+=static_cast<int>(fecharTokens.size());canal.encerrar();continue;
   }
   if(terminou){motivo=0;break;}
   // Marcadores de canal também são tokens especiais em alguns GGUFs.
   int n=llama_token_to_piece(vocab,token,pedaco.data(),static_cast<int>(pedaco.size()),0,true);
   if(n<0){pedaco.resize(static_cast<size_t>(-n));n=llama_token_to_piece(vocab,token,pedaco.data(),static_cast<int>(pedaco.size()),0,true);}
   if(n<0)throw std::runtime_error("Falha ao decodificar token.");
   std::string parte(pedaco.data(),n);canal.receber(parte);
   emitir(env,classe,entregar,saida,utf.receber(parte));
   i++;if(i<disponiveis&&!decodificar(*m,&token,1))return 2;
  }
  if(!abortar(m.get()))emitir(env,classe,entregar,saida,utf.finalizar());
  return abortar(m.get())?2:motivo;
 }catch(const std::exception&e){erro(env,e);return 2;}
}
extern "C" JNIEXPORT jint JNICALL Java_br_com_anderflow_metrys_offline_MotorLocal_contextoNativo(JNIEnv *env,jclass,jlong id){try{auto m=obter(id);std::lock_guard<std::mutex> lock(m->trabalho);return m->contexto?static_cast<jint>(llama_n_ctx(m->contexto.get())):0;}catch(const std::exception&e){erro(env,e);return 0;}}
extern "C" JNIEXPORT void JNICALL Java_br_com_anderflow_metrys_offline_MotorLocal_cancelarNativo(JNIEnv*,jclass,jlong id){try{obter(id)->cancelado=true;}catch(...) {}}
extern "C" JNIEXPORT void JNICALL Java_br_com_anderflow_metrys_offline_MotorLocal_fecharNativo(JNIEnv*,jclass,jlong id){std::lock_guard<std::mutex> lock(registroMutex);registro.erase(id);}
