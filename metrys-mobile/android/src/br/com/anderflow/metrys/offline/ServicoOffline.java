package br.com.anderflow.metrys.offline;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;
import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.concurrent.*;

/** Coordena inferência e persistência locais; somente baixarModelo abre conexões de rede. */
public final class ServicoOffline {
 public interface Ouvidor{void mudou(EstadoOffline estado);}
 private static ServicoOffline unico;
 public static synchronized ServicoOffline obter(Context c){if(unico==null)unico=new ServicoOffline(c.getApplicationContext());return unico;}
 private final Context app;private final File pasta;private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService executor=Executors.newSingleThreadExecutor();private final Set<Ouvidor> ouvintes=new HashSet<>();
 private BancoMetrys historico;private volatile MotorLocal motor;private volatile DownloadModelo.Controle download;
 private volatile boolean ocupado=true,cancelado=false,modeloPronto=false;private volatile JSONObject catalogo;
 private final ArrayList<JSONObject> opcoesModelos=new ArrayList<>();
 private final ArrayList<String> etapas=new ArrayList<>();private volatile int trechosGerados=0;
 private final ArrayDeque<String> filaMensagens=new ArrayDeque<>();
 private boolean avisoDemora=false;
 private final Runnable relogioGeracao=new Runnable(){@Override public void run(){
  final boolean ativo;final long espera;final boolean semTrecho;
  synchronized(ServicoOffline.this){ativo=gerando;espera=SystemClock.elapsedRealtime()-inicioGeracao;semTrecho=primeiroTrecho==0;}
  if(!ativo)return;
  if(semTrecho&&espera>30000&&!avisoDemora){avisoDemora=true;etapa("O modelo ainda não enviou texto. Aguarde ou toque em Parar para cancelar.");}
  if(semTrecho&&espera>180000){cancelado=true;MotorLocal m=motor;if(m!=null)m.cancelar();synchronized(ServicoOffline.this){gerando=false;fase="erro";detalhe="Tempo limite sem resposta (3 minutos). Escolha um modelo menor ou reduza o contexto.";}publicar();return;}
  publicarParcial();main.postDelayed(this,1000);
 }};
 private long inicioGeracao=0,primeiroTrecho=0;private String raciocinioPublicado="";private boolean raciocinando=false;
 private String fase="ausente",detalhe="Baixe o modelo uma vez para conversar sem internet.",id="",textoParcial="";
 private long bytes=0,total=1117320768L;private boolean gerando=false;private long ultimaNotificacao;
 private EstadoOffline ultimoCompleto;private List<EstadoOffline.Mensagem> mensagensPersistidas=Collections.emptyList();
 private ServicoOffline(Context c){
  app=c;pasta=new File(c.getFilesDir(),"modelo");executor.execute(()->{
   try{
    try(InputStream e=app.getAssets().open("modelo.json")){catalogo=new JSONObject(lerTexto(e,1024*1024));}
    catalogo.put("nome","Metrys Coder 1,5B · Leve");catalogo.put("ram_min_gb",4);catalogo.put("contexto",2048);catalogo.put("threads",3);
    opcoesModelos.add(catalogo);
    try(InputStream e=app.getAssets().open("modelos_avancados.json")){JSONArray lista=new JSONArray(lerTexto(e,1024*1024));for(int k=0;k<lista.length();k++)opcoesModelos.add(lista.getJSONObject(k));}
    String salvo=app.getSharedPreferences("modelos_metrys",Context.MODE_PRIVATE).getString("ativo",catalogo.getString("id"));
    for(JSONObject item:opcoesModelos)if(item.getString("id").equals(salvo)){catalogo=item;break;}
    synchronized(this){total=catalogo.getLong("bytes");}
    historico=new BancoMetrys(app);historico.importarLegado(new File(app.getFilesDir(),"conversas"));List<HistoricoLocal.Conversa> cs=historico.conversas();id=cs.isEmpty()?historico.nova():cs.get(0).id;
    if(modelo().isFile()){estado("verificando","Conferindo o modelo salvo no aparelho…");validar(modelo());modeloPronto=true;estado("pronto","Modelo disponível. Suas conversas ficam neste aparelho.");}
    else{File parcial=new File(pasta,catalogo.getString("id")+".gguf.part");synchronized(this){bytes=parcial.length();}publicar();}
   }catch(Exception e){erro(e);}finally{liberar();}
  });
 }
 public synchronized void observar(Ouvidor o){ouvintes.add(o);EstadoOffline s=estado();main.post(()->{synchronized(this){if(!ouvintes.contains(o))return;}o.mudou(s);});}
 public synchronized void remover(Ouvidor o){ouvintes.remove(o);}
 public synchronized EstadoOffline estado(){
  EstadoOffline s=new EstadoOffline();s.fase=fase;s.detalhe=detalhe;s.bytes=bytes;s.total=total;s.gerando=gerando;s.modeloPronto=modeloPronto;s.conversaId=id;
  s.nomeModelo=catalogo==null?"Modelo local":catalogo.optString("nome",catalogo.optString("id"));s.etapas=new ArrayList<>(etapas);s.trechosGerados=trechosGerados;s.raciocinioPublicado=raciocinioPublicado;s.raciocinando=raciocinando;
  s.filaPendente=filaMensagens.size();s.tempoGeracaoMs=inicioGeracao==0?0:(gerando?SystemClock.elapsedRealtime()-inicioGeracao:Math.max(0,primeiroTrecho-inicioGeracao));
  if(historico!=null){for(BancoMetrys.Projeto p:historico.projetos(true))s.projetos.add(new EstadoOffline.Projeto(p.id,p.nome,p.arquivado));for(HistoricoLocal.Conversa c:historico.conversas())s.conversas.add(new EstadoOffline.Conversa(c.id,c.titulo,historico.projetoDaConversa(c.id)));for(HistoricoLocal.Conversa c:historico.conversas(true))s.conversasArquivadas.add(new EstadoOffline.Conversa(c.id,c.titulo,historico.projetoDaConversa(c.id)));if(!id.isEmpty())for(HistoricoLocal.Mensagem m:historico.mensagens(id))s.mensagens.add(new EstadoOffline.Mensagem(m.papel,m.texto));}
  // A base do fluxo guarda somente mensagens persistidas. Parcial nunca vira histórico salvo.
  mensagensPersistidas=new ArrayList<>(s.mensagens);
  if(gerando&&!textoParcial.isEmpty())s.mensagens.add(new EstadoOffline.Mensagem("assistant",textoParcial));
  ultimoCompleto=s;return s;
 }
 /** Atualizações de tokens usam cache: não consultam o SQLite a cada 120 ms. */
 private synchronized EstadoOffline estadoParcial(){
  if(ultimoCompleto==null)return estado();
  EstadoOffline s=new EstadoOffline();s.fase=fase;s.detalhe=detalhe;s.bytes=bytes;s.total=total;s.gerando=gerando;s.modeloPronto=modeloPronto;s.conversaId=id;
  s.nomeModelo=catalogo==null?"Modelo local":catalogo.optString("nome",catalogo.optString("id"));s.etapas=new ArrayList<>(etapas);s.trechosGerados=trechosGerados;s.raciocinioPublicado=raciocinioPublicado;s.raciocinando=raciocinando;
  s.filaPendente=filaMensagens.size();s.tempoGeracaoMs=inicioGeracao==0?0:(gerando?SystemClock.elapsedRealtime()-inicioGeracao:Math.max(0,primeiroTrecho-inicioGeracao));
  s.conversas=ultimoCompleto.conversas;s.conversasArquivadas=ultimoCompleto.conversasArquivadas;s.projetos=ultimoCompleto.projetos;
  s.mensagens=new ArrayList<>(mensagensPersistidas);
  if(gerando&&!textoParcial.isEmpty())s.mensagens.add(new EstadoOffline.Mensagem("assistant",textoParcial));
  return s;
 }
 private void publicar(){publicar(false);}
 private void publicarParcial(){publicar(true);}
 private void publicar(boolean parcial){final EstadoOffline s;final List<Ouvidor> os;synchronized(this){s=parcial?estadoParcial():estado();os=new ArrayList<>(ouvintes);}main.post(()->{for(Ouvidor o:os){synchronized(this){if(!ouvintes.contains(o))continue;}o.mudou(s);}});}
 private void estado(String f,String d){synchronized(this){fase=f;detalhe=d;}publicar();}
 /** Eventos públicos observáveis, não um raciocínio privado inventado. */
 private void etapa(String descricao){synchronized(this){if(etapas.size()>12)etapas.remove(0);etapas.add(descricao);}publicarParcial();}
 public synchronized List<JSONObject> modelos(){return new ArrayList<>(opcoesModelos);}
 public void selecionarModelo(String codigo){
  if(!reservar())return;
  executor.execute(()->{try{
   JSONObject proximo=null;for(JSONObject item:opcoesModelos)if(codigo.equals(item.optString("id"))){proximo=item;break;}
   if(proximo==null)throw new IOException("Modelo não listado.");
   if(motor!=null){motor.close();motor=null;}
   catalogo=proximo;total=catalogo.getLong("bytes");bytes=0;modeloPronto=false;
   app.getSharedPreferences("modelos_metrys",Context.MODE_PRIVATE).edit().putString("ativo",codigo).apply();
   File arq=modelo();if(arq.isFile()){estado("verificando","Conferindo o modelo escolhido…");validar(arq);modeloPronto=true;estado("pronto","Modelo selecionado. Sem novo download.");}
   else {File parcial=new File(pasta,codigo+".gguf.part");bytes=parcial.length();estado("ausente","Modelo selecionado. Baixe uma única vez, sem apagar os outros modelos.");}
  }catch(Exception e){erro(e);}finally{liberar();}});
 }
 private void erro(Throwable e){String m=e.getMessage();estado("erro",m==null?"Não foi possível concluir a operação. Tente novamente.":m);}
 private synchronized boolean reservar(){if(ocupado)return false;ocupado=true;cancelado=false;return true;}
 private void liberar(){boolean pendente;synchronized(this){ocupado=false;gerando=false;textoParcial="";pendente=!filaMensagens.isEmpty();}publicar();if(pendente)main.post(this::executarProximaDaFila);}
 public void baixarModelo(){
  if(catalogo==null){estado("erro","Aguarde a preparação do aplicativo e tente novamente.");return;}if(!reservar())return;
  try{app.startForegroundService(new Intent(app,OperacaoModeloService.class));}catch(RuntimeException e){liberar();erro(new IOException("Mantenha o aplicativo aberto e tente baixar novamente.",e));return;}
  executor.execute(()->{
   try{
    estado("baixando","Iniciando download pelo GitHub…");JSONArray a=catalogo.getJSONArray("urls");String[] urls=new String[a.length()];for(int j=0;j<urls.length;j++)urls[j]=a.getString(j);
    download=new DownloadModelo.Controle();if(cancelado)download.cancel();
    DownloadModelo.Resultado r=DownloadModelo.baixar(new DownloadModelo.Spec(catalogo.getString("id"),urls,total,catalogo.getString("sha256")),pasta,download,(n,t,origem)->{
     synchronized(this){bytes=n;fase="baixando";detalhe="Baixando de "+origem+". Pode pausar e continuar depois.";}long agora=SystemClock.elapsedRealtime();if(agora-ultimaNotificacao>250){ultimaNotificacao=agora;publicar();OperacaoModeloService.progresso(app,n,t);}
    });
    if(cancelado)throw new InterruptedIOException("Download pausado. Você pode continuar depois.");estado("verificando","Conferindo integridade do modelo…");validar(r.file);modeloPronto=true;synchronized(this){bytes=total;}estado("pronto","Download concluído. Você já pode conversar sem internet.");
   }catch(Exception e){erro(e);}finally{download=null;app.stopService(new Intent(app,OperacaoModeloService.class));liberar();}
  });
 }
 public void cancelarDownload(){cancelado=true;final DownloadModelo.Controle c=download;if(c!=null)new Thread(c::cancel,"metrys-pausar").start();}
 public void cancelarGeracao(){cancelado=true;MotorLocal m=motor;if(m!=null)m.cancelar();etapa("Cancelamento solicitado ao motor.");}
 public boolean enviar(String entrada){return enviarInterno(entrada,false);}
 public boolean refazerResposta(){if(historico==null)return false;String pergunta=historico.ultimaPergunta(id);if(pergunta.isEmpty())return false;return enviarInterno(pergunta,true);}
 private boolean enviarInterno(String entrada,boolean refazer){
  final String texto=entrada==null?"":entrada.trim();if(texto.isEmpty())return false;if(!modeloPronto){estado("ausente","Baixe o modelo para começar. Depois disso o chat funciona sem internet.");return false;}if(texto.length()>12000){estado("erro","Texto muito longo para o contexto local. Anexe ou divida em até 12.000 caracteres.");return false;}
  synchronized(this){if(ocupado||gerando){if(refazer||filaMensagens.size()>=16)return false;filaMensagens.addLast(texto);publicarParcial();return true;}}
  if(!reservar())return false;
  executor.execute(()->{
   String conversa=id;StringBuilder resposta=new StringBuilder();boolean salva=false;
   try{
    if(historico==null)throw new IOException("O histórico ainda está sendo preparado.");if(refazer)historico.removerUltimaAssistente(conversa);else historico.adicionar(conversa,"user",texto);synchronized(this){gerando=true;textoParcial="";etapas.clear();trechosGerados=0;raciocinioPublicado="";raciocinando=catalogo.optBoolean("raciocinio",false);inicioGeracao=SystemClock.elapsedRealtime();primeiroTrecho=0;avisoDemora=false;}etapa("Mensagem recebida e salva localmente");main.post(relogioGeracao);estado("carregando","Carregando o modelo na memória do celular…");
    if(motor==null)motor=new MotorLocal();if(!motor.carregado()){etapa("Iniciando "+catalogo.optString("nome","modelo offline"));motor.carregar(modelo().getAbsolutePath(),catalogo.optInt("contexto",2048),catalogo.optInt("threads",3));}else etapa("Modelo já carregado, sem reiniciar");
    if(cancelado)throw new InterruptedIOException("Resposta interrompida.");
    etapa("Modelo pronto na memória");estado("contexto","Organizando o histórico e as informações desta conversa…");
    List<HistoricoLocal.Mensagem> anteriores=historico.mensagens(conversa),recentes=new ArrayList<>();int limite=3600;
    for(int j=anteriores.size()-1;j>=0&&recentes.size()<60;j--){HistoricoLocal.Mensagem m=anteriores.get(j);if(m.texto.length()>limite)break;recentes.add(m);limite-=m.texto.length();}Collections.reverse(recentes);while(!recentes.isEmpty()&&!recentes.get(0).papel.equals("user"))recentes.remove(0);
    List<String> papeis=new ArrayList<>(),mensagens=new ArrayList<>();papeis.add("system");String sistema="Você é Metrys, assistente criado por Ander. Responda em português do Brasil, com clareza. Você funciona localmente e não navega na internet. Não invente ações, arquivos ou resultados. Ao escrever código, use exemplos completos. Em pedidos complexos, comece com um plano público resumido em 1 ou 2 frases, depois apresente a execução e seus resultados, nunca finja ter feito operações que não executou. Exiba explicações úteis, não uma simulação de pensamentos ocultos.";String pessoais=historico.instrucaoEfetiva(conversa)+historico.resumoFeedback(conversa);String lembrancas=historico.recuperarMemorias(conversa,texto);mensagens.add(sistema+"\n"+pessoais+(lembrancas.isEmpty()?"":"\nMemórias explícitas relevantes, nunca confundir com prova independente:\n"+lembrancas));for(HistoricoLocal.Mensagem m:recentes){papeis.add(m.papel);mensagens.add(m.texto);}
    etapa("Contexto preparado: "+recentes.size()+" mensagens recentes");estado("gerando","Aguardando a primeira palavra da resposta…");final boolean modoPensar=catalogo.optBoolean("raciocinio",false);
    final StringBuilder fluxoBruto=new StringBuilder();final long[] ultimo={0};MotorLocal.Saida saida=parte->{
     if(modoPensar){fluxoBruto.append(parte);String bruto=fluxoBruto.toString();int fim=bruto.indexOf("</think>");
      if(fim>=0){String parteFinal=bruto.substring(fim+8);resposta.setLength(0);resposta.append(parteFinal);synchronized(this){raciocinioPublicado=bruto.substring(0,fim).replace("<think>","").trim();raciocinando=false;}}
      else synchronized(this){raciocinioPublicado=bruto.replace("<think>","").trim();}
     }else resposta.append(parte);
     long agora=SystemClock.elapsedRealtime();boolean primeiro=false;synchronized(this){textoParcial=resposta.toString();trechosGerados++;if(primeiroTrecho==0){primeiroTrecho=agora;primeiro=true;}}
     if(primeiro)etapa("Primeiro trecho recebido após "+((agora-inicioGeracao)/1000f)+" s");
     if(agora-ultimo[0]>120){ultimo[0]=agora;publicarParcial();}
    };
    while(true){try{motor.gerar(papeis.toArray(new String[0]),mensagens.toArray(new String[0]),saida,modoPensar?900:512);break;}catch(IOException e){if(cancelado||resposta.length()>0||e.getMessage()==null||!e.getMessage().startsWith("Histórico excede o contexto:")||papeis.size()<=2)throw e;papeis.remove(1);mensagens.remove(1);while(papeis.size()>2&&!papeis.get(1).equals("user")){papeis.remove(1);mensagens.remove(1);}}}
    if(resposta.length()==0&&!cancelado)throw new IOException(modoPensar?"O modelo esgotou o limite antes de concluir o raciocínio. Tente uma pergunta mais curta ou troque para o modelo sem raciocínio.":"O modelo não produziu uma resposta. Tente reformular a mensagem.");if(resposta.length()>0){historico.adicionar(conversa,"assistant",resposta.toString());salva=true;}etapa("Resposta salva · "+resposta.length()+" caracteres gerados");synchronized(this){gerando=false;textoParcial="";}estado("pronto",cancelado?"Resposta interrompida. O texto recebido foi salvo.":"Resposta concluída e salva neste aparelho.");
   }catch(Throwable e){
    if(!salva&&resposta.length()>0)try{historico.adicionar(conversa,"assistant",resposta.toString());}catch(RuntimeException falha){e.addSuppressed(falha);}
    if(e instanceof OutOfMemoryError){MotorLocal m=motor;if(m!=null)m.close();motor=null;erro(new IOException("Memória insuficiente. Feche outros aplicativos e tente novamente."));}else if(cancelado){etapa("Geração interrompida a pedido");estado("pronto","Resposta interrompida. Você pode enviar outra mensagem.");}else{etapa("Falha durante a execução: "+e.getClass().getSimpleName());erro(e);}
   }finally{main.removeCallbacks(relogioGeracao);liberar();}
  });return true;
 }
 private void executarProximaDaFila(){final String proxima;synchronized(this){proxima=filaMensagens.pollFirst();}if(proxima!=null)main.post(()->enviar(proxima));else publicarParcial();}
 public int filaPendente(){synchronized(this){return filaMensagens.size();}}
 public void avaliarResposta(String conversa,String texto,int nota){if(historico==null||texto==null||texto.isEmpty())return;executor.execute(()->{try{historico.avaliar(conversa,texto,nota);publicar();}catch(Exception e){erro(e);}});}
 public void novaConversa(){if(!reservar())return;executor.execute(()->{try{id=historico.nova();synchronized(this){etapas.clear();trechosGerados=0;raciocinioPublicado="";raciocinando=false;}estado(modeloPronto?"pronto":"ausente",modeloPronto?"Nova conversa, memória limpa.":"Baixe o modelo para começar.");}catch(Exception e){erro(e);}finally{liberar();}});}
 public void selecionarConversa(String nova){if(!reservar())return;executor.execute(()->{try{historico.mensagens(nova);id=nova;synchronized(this){etapas.clear();trechosGerados=0;raciocinioPublicado="";raciocinando=false;}publicar();}catch(Exception e){erro(e);}finally{liberar();}});}
 public void novoTrabalho(String objetivo){executarBanco(()->historico.novoTrabalho(objetivo));}
 public void concluirTrabalho(String trabalho){executarBanco(()->historico.concluirTrabalho(trabalho));}
 public String modoPermissao(){return new PermissoesMetrys(app).modo();}
 public void definirModoPermissao(String modo){new PermissoesMetrys(app).definir(modo);publicar();}
 public List<BancoMetrys.Trabalho> trabalhos(){return historico==null?Collections.emptyList():historico.trabalhos();}
 public void criarProjeto(String nome){executarBanco(()->{String projeto=historico.criarProjeto(nome);id=historico.nova(projeto);});}
 public void criarConversaEmProjeto(String projeto){executarBanco(()->id=historico.nova(projeto));}
 public void renomearConversa(String conversa,String nome){executarBanco(()->historico.renomearConversa(conversa,nome));}
 public void renomearProjeto(String projeto,String nome){executarBanco(()->historico.renomearProjeto(projeto,nome));}
 public void moverConversa(String conversa,String projeto){executarBanco(()->historico.moverConversa(conversa,projeto));}
 public void arquivarConversa(String conversa){executarBanco(()->{historico.arquivarConversa(conversa,true);if(conversa.equals(id))selecionarOutraConversa();});}
 public void restaurarConversa(String conversa){executarBanco(()->historico.arquivarConversa(conversa,false));}
 public void arquivarProjeto(String projeto){executarBanco(()->historico.arquivarProjeto(projeto,true));}
 public void restaurarProjeto(String projeto){executarBanco(()->historico.arquivarProjeto(projeto,false));}
 public void excluirConversa(String conversa){executarBanco(()->{historico.excluirConversa(conversa);app.getSharedPreferences("rascunhos",Context.MODE_PRIVATE).edit().remove(conversa).apply();if(conversa.equals(id))selecionarOutraConversa();});}
 public void excluirProjeto(String projeto,boolean comConversas){executarBanco(()->{historico.excluirProjeto(projeto,comConversas);selecionarOutraConversa();});}
 private void selecionarOutraConversa(){List<HistoricoLocal.Conversa> ativas=historico.conversas();id=ativas.isEmpty()?historico.nova():ativas.get(0).id;}
 public List<BancoMetrys.Resultado> buscar(String consulta){return historico==null?Collections.emptyList():historico.buscar(consulta);}
 public List<BancoMetrys.Memoria> memorias(){return historico==null?Collections.emptyList():historico.memorias();}
 public String personalizacao(String chave){return historico==null?"":historico.configuracao(chave);}
 public void salvarPersonalizacao(String sobre,String resposta){executarBanco(()->{historico.configuracao("sobre_usuario",sobre);historico.configuracao("como_responder",resposta);});}
 public void guardarMemoria(String texto){executarBanco(()->historico.salvarMemoria(texto,id));}
 public void apagarMemoria(String memoria){executarBanco(()->historico.removerMemoria(memoria));}
 public void instrucaoProjeto(String projeto,String texto){executarBanco(()->historico.instrucaoProjeto(projeto,texto));}
 private interface AcaoBanco{void executar()throws Exception;}
 private void executarBanco(AcaoBanco acao){if(!reservar()){estado("erro","Conclua a operação atual e tente novamente.");return;}executor.execute(()->{try{if(historico==null)throw new IOException("O banco ainda está sendo preparado.");acao.executar();publicar();}catch(Exception e){erro(e);}finally{liberar();}});}
 public void importarModelo(Uri u){importarPartes(Collections.singletonList(u));}
 public void importarPartes(List<Uri> uris){
  if(uris==null||uris.isEmpty()||!reservar())return;final List<Uri> selecao=new ArrayList<>(uris);executor.execute(()->{
   File tmp=new File(pasta,"importacao.part");
   try{
    if(catalogo==null)throw new IOException("Aguarde a preparação do catálogo.");
    if(selecao.size()>1){JSONArray partes=catalogo.optJSONArray("partes");if(partes==null||selecao.size()!=partes.length())throw new IOException("Selecione todas as partes originais do modelo no Drive.");Map<String,Uri> nomes=new HashMap<>();for(Uri u:selecao)nomes.put(nome(u),u);if(nomes.size()!=selecao.size())throw new IOException("Não selecione a mesma parte mais de uma vez.");selecao.clear();for(int j=0;j<partes.length();j++){String n=partes.getJSONObject(j).getString("nome");Uri u=nomes.get(n);if(u==null)throw new IOException("Falta a parte "+n+". Selecione todas as partes.");selecao.add(u);}}
    if(!pasta.isDirectory()&&!pasta.mkdirs())throw new IOException("Não foi possível preparar o armazenamento.");if(pasta.getUsableSpace()<total+64L*1024*1024)throw new IOException("Libere espaço para o tamanho do modelo selecionado e mais 64 MB.");estado("verificando","Importando o modelo selecionado…");long n=0,ultimo=0;
    try(FileOutputStream o=new FileOutputStream(tmp)){byte[] b=new byte[131072];for(Uri u:selecao)try(InputStream e=app.getContentResolver().openInputStream(u)){if(e==null)throw new IOException("Não foi possível abrir o arquivo selecionado.");int l;while((l=e.read(b))!=-1){if(cancelado)throw new InterruptedIOException("Importação interrompida.");n+=l;if(n>total)throw new IOException("Os arquivos excedem o tamanho do modelo.");o.write(b,0,l);synchronized(this){bytes=n;}long agora=SystemClock.elapsedRealtime();if(agora-ultimo>200){ultimo=agora;publicar();}}}o.getFD().sync();}
    validar(tmp);MotorLocal m=motor;if(m!=null){m.close();motor=null;}try{Files.move(tmp.toPath(),modelo().toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp.toPath(),modelo().toPath(),StandardCopyOption.REPLACE_EXISTING);}modeloPronto=true;estado("pronto","Modelo importado e verificado. O chat já funciona sem internet.");
   }catch(Exception e){tmp.delete();erro(e);}finally{liberar();}
  });
 }
 private String nome(Uri u){try(Cursor c=app.getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}return u.getLastPathSegment();}
 public String enderecoDrive(){JSONObject c=catalogo;return c==null?"":c.optString("drive_pasta","");}
 public void exportarHistorico(Uri u){executor.execute(()->{try{if(historico==null)throw new IOException("O histórico ainda não está pronto.");JSONArray cs=new JSONArray();for(HistoricoLocal.Conversa c:historico.conversas()){JSONObject o=new JSONObject();o.put("id",c.id);o.put("titulo",c.titulo);JSONArray ms=new JSONArray();for(HistoricoLocal.Mensagem m:historico.mensagens(c.id)){JSONObject x=new JSONObject();x.put("papel",m.papel);x.put("texto",m.texto);ms.put(x);}o.put("mensagens",ms);cs.put(o);}JSONObject raiz=new JSONObject();raiz.put("aplicativo","Metrys Offline");raiz.put("versao",1);raiz.put("conversas",cs);try(OutputStream o=app.getContentResolver().openOutputStream(u,"wt")){if(o==null)throw new IOException("Não foi possível exportar.");o.write(raiz.toString(2).getBytes(StandardCharsets.UTF_8));}estado(modeloPronto?"pronto":"ausente","Histórico exportado no local escolhido.");}catch(Exception e){erro(e);}});}
 private File modelo()throws JSONException{return new File(pasta,catalogo.getString("id")+".gguf");}
 /** Evita recalcular 1,1 GB em todo início se o arquivo privado já foi validado. */
 private void validar(File arq)throws Exception{
  if(arq.length()!=total)throw new IOException("Modelo incompleto. Selecione todas as partes ou baixe novamente.");
  String hash=catalogo.getString("sha256");
  boolean definitivo=arq.getCanonicalFile().equals(modelo().getCanonicalFile());
  android.content.SharedPreferences p=app.getSharedPreferences("modelo_integridade",Context.MODE_PRIVATE);
  if(definitivo&&p.getLong("tamanho",-1)==arq.length()&&p.getLong("modificado",-1)==arq.lastModified()&&hash.equalsIgnoreCase(p.getString("hash","")))return;
  MessageDigest md=MessageDigest.getInstance("SHA-256");
  try(InputStream e=new FileInputStream(arq)){
   byte[] b=new byte[262144];int n;while((n=e.read(b))!=-1){if(cancelado)throw new InterruptedIOException("Verificação interrompida.");md.update(b,0,n);}
  }
  StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));
  if(!s.toString().equalsIgnoreCase(hash))throw new IOException("A integridade do modelo não confere. Use o arquivo original.");
  if(definitivo)p.edit().putLong("tamanho",arq.length()).putLong("modificado",arq.lastModified()).putString("hash",hash).apply();
 }
 private static String lerTexto(InputStream e,int max)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=e.read(b))!=-1){if(o.size()+n>max)throw new IOException("Configuração grande demais.");o.write(b,0,n);}return new String(o.toByteArray(),StandardCharsets.UTF_8);}
}
