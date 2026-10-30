package br.com.anderflow.metrys.offline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Fonte de verdade local. Todas as mudanças são transacionais e consultas recebem parâmetros. */
public final class BancoMetrys extends SQLiteOpenHelper {
 private static final int VERSAO=4;
 private static final int LIMITE=4*1024*1024;
 public static final class Projeto {
  public final String id,nome;public final boolean arquivado;
  Projeto(String id,String nome,boolean arquivado){this.id=id;this.nome=nome;this.arquivado=arquivado;}
 }
 public static final class Resultado {
  public final String conversaId,titulo,trecho;public final long data;
  Resultado(String id,String titulo,String trecho,long data){this.conversaId=id;this.titulo=titulo;this.trecho=trecho;this.data=data;}
 }
 public static final class Trabalho {
  public final String id,objetivo,estado;public final long atualizadoEm;
  Trabalho(String id,String objetivo,String estado,long atualizadoEm){this.id=id;this.objetivo=objetivo;this.estado=estado;this.atualizadoEm=atualizadoEm;}
 }
 public static final class Memoria {
  public final String id,conteudo,origemId,projetoId;public final long criadaEm;
  Memoria(String id,String conteudo,String origemId,String projetoId,long criadaEm){this.id=id;this.conteudo=conteudo;this.origemId=origemId;this.projetoId=projetoId;this.criadaEm=criadaEm;}
 }
 private final Context contexto;
 public BancoMetrys(Context app){super(app,"metrys.db",null,VERSAO);contexto=app.getApplicationContext();setWriteAheadLoggingEnabled(true);}
 @Override public void onConfigure(SQLiteDatabase db){super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);}
 @Override public void onCreate(SQLiteDatabase db){
  db.execSQL("CREATE TABLE projetos (id TEXT PRIMARY KEY, nome TEXT NOT NULL, instrucao TEXT NOT NULL DEFAULT '', criado_em INTEGER NOT NULL, arquivado_em INTEGER)");
  db.execSQL("CREATE TABLE conversas (id TEXT PRIMARY KEY, projeto_id TEXT REFERENCES projetos(id) ON DELETE SET NULL, titulo TEXT NOT NULL, criado_em INTEGER NOT NULL, atualizado_em INTEGER NOT NULL, arquivado_em INTEGER)");
  db.execSQL("CREATE TABLE mensagens (id INTEGER PRIMARY KEY AUTOINCREMENT, conversa_id TEXT NOT NULL REFERENCES conversas(id) ON DELETE CASCADE, papel TEXT NOT NULL CHECK(papel IN ('user','assistant')), texto TEXT NOT NULL, criada_em INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE memorias (id TEXT PRIMARY KEY, projeto_id TEXT REFERENCES projetos(id) ON DELETE CASCADE, origem_conversa_id TEXT REFERENCES conversas(id) ON DELETE CASCADE, conteudo TEXT NOT NULL, criada_em INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE configuracoes (chave TEXT PRIMARY KEY, valor TEXT NOT NULL)");
  db.execSQL("CREATE TABLE trabalhos (id TEXT PRIMARY KEY, objetivo TEXT NOT NULL, estado TEXT NOT NULL, criado_em INTEGER NOT NULL, atualizado_em INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE trabalhos_etapas (id TEXT PRIMARY KEY, trabalho_id TEXT NOT NULL REFERENCES trabalhos(id) ON DELETE CASCADE, descricao TEXT NOT NULL, estado TEXT NOT NULL, ordem INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE anexos (id TEXT PRIMARY KEY, conversa_id TEXT REFERENCES conversas(id) ON DELETE CASCADE, uri TEXT NOT NULL, nome TEXT, mime TEXT, tamanho INTEGER, criada_em INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE feedback_respostas (conversa_id TEXT NOT NULL REFERENCES conversas(id) ON DELETE CASCADE, hash_texto TEXT NOT NULL, texto_resumo TEXT NOT NULL, nota INTEGER NOT NULL CHECK(nota IN(-1,1)), criado_em INTEGER NOT NULL, PRIMARY KEY(conversa_id,hash_texto))");
  db.execSQL("CREATE INDEX idx_conversas_projeto ON conversas(projeto_id,arquivado_em,atualizado_em)");
  db.execSQL("CREATE INDEX idx_mensagens_conversa ON mensagens(conversa_id,id)");
  db.execSQL("CREATE INDEX idx_memorias_projeto ON memorias(projeto_id,criada_em)");
  db.execSQL("CREATE INDEX idx_trabalhos_etapas ON trabalhos_etapas(trabalho_id,ordem)");
 }
 /** Migração não destrutiva: esquemas locais v1, v2 e v3 permanecem intactos. */
 @Override public void onUpgrade(SQLiteDatabase db,int anterior,int novo){
  if(anterior<1||anterior>VERSAO||novo!=VERSAO)throw new IllegalStateException("Versão SQLite incompatível: "+anterior+" → "+novo);
  // A tabela de feedback pode já existir nas versões antigas; nunca excluir ou recriar dados.
  if(anterior<4){
   db.execSQL("CREATE TABLE IF NOT EXISTS feedback_respostas (conversa_id TEXT NOT NULL REFERENCES conversas(id) ON DELETE CASCADE, hash_texto TEXT NOT NULL, texto_resumo TEXT NOT NULL, nota INTEGER NOT NULL CHECK(nota IN(-1,1)), criado_em INTEGER NOT NULL, PRIMARY KEY(conversa_id,hash_texto))");
  }
 }
 public synchronized String ultimaPergunta(String conversa){
  try(Cursor c=getReadableDatabase().rawQuery("SELECT texto FROM mensagens WHERE conversa_id=? AND papel='user' ORDER BY id DESC LIMIT 1",new String[]{conversa})){return c.moveToFirst()?c.getString(0):"";}
 }
 public synchronized void removerUltimaAssistente(String conversa){
  SQLiteDatabase db=getWritableDatabase();
  try(Cursor c=db.rawQuery("SELECT id,papel FROM mensagens WHERE conversa_id=? ORDER BY id DESC LIMIT 1",new String[]{conversa})){
   if(c.moveToFirst()&&"assistant".equals(c.getString(1)))db.delete("mensagens","id=?",new String[]{c.getString(0)});
  }
 }
 public synchronized void avaliar(String conversa,String texto,int nota){
  if(nota!=1&&nota!=-1)throw new IllegalArgumentException("Avaliação inválida");
  SQLiteDatabase db=getWritableDatabase();exigeConversa(db,conversa);
  String hash=Integer.toHexString(texto.hashCode())+"_"+texto.length();
  ContentValues v=valores("conversa_id",conversa,"hash_texto",hash,"texto_resumo",texto.substring(0,Math.min(260,texto.length())));
  v.put("nota",nota);v.put("criado_em",agora());db.insertWithOnConflict("feedback_respostas",null,v,SQLiteDatabase.CONFLICT_REPLACE);
 }
 public synchronized String resumoFeedback(String conversa){
  StringBuilder b=new StringBuilder();
  try(Cursor c=getReadableDatabase().rawQuery("SELECT nota,texto_resumo FROM feedback_respostas WHERE conversa_id=? ORDER BY criado_em DESC LIMIT 3",new String[]{conversa})){
   while(c.moveToNext()){
    b.append(c.getInt(0)==1?"\nExemplo de resposta bem avaliada: ":"\nEvite repetir o padrão desta resposta mal avaliada: ");
    b.append(c.getString(1).replace('\n',' ').substring(0,Math.min(160,c.getString(1).length())));
   }
  }return b.toString();
 }
 private static long agora(){return System.currentTimeMillis();}
 private static void validarTexto(String texto){if(texto==null||texto.getBytes(StandardCharsets.UTF_8).length>LIMITE)throw new IllegalArgumentException("Texto acima do limite de 4 MB.");}
 private static String nome(String s){if(s==null||s.trim().isEmpty()||s.length()>160)throw new IllegalArgumentException("Use um nome com até 160 caracteres.");return s.trim();}
 private static ContentValues valores(String... pares){ContentValues v=new ContentValues();for(int j=0;j<pares.length;j+=2)v.put(pares[j],pares[j+1]);return v;}
 private static String id(){return UUID.randomUUID().toString();}
 private static void exigeConversa(SQLiteDatabase db,String conversa){try(Cursor c=db.rawQuery("SELECT id FROM conversas WHERE id=?",new String[]{conversa})){if(!c.moveToFirst())throw new IllegalArgumentException("Conversa não encontrada.");}}
 private static void exigeProjeto(SQLiteDatabase db,String projeto){try(Cursor c=db.rawQuery("SELECT id FROM projetos WHERE id=?",new String[]{projeto})){if(!c.moveToFirst())throw new IllegalArgumentException("Projeto não encontrado.");}}

 /** Importação idempotente: ou todos os dados legados entram, ou nada é confirmado. O .bin nunca é excluído. */
 public synchronized int importarLegado(File pasta)throws IOException{
  File legado=new File(pasta,"historico.bin");if(!legado.isFile())return 0;
  SQLiteDatabase db=getWritableDatabase();
  try(Cursor c=db.rawQuery("SELECT valor FROM configuracoes WHERE chave='legado_importado'",null)){if(c.moveToFirst())return 0;}
  HistoricoLocal antigo=new HistoricoLocal(pasta);List<HistoricoLocal.Conversa> conversas=antigo.conversas();Collections.reverse(conversas);
  int inseridas=0;db.beginTransaction();try{
   for(HistoricoLocal.Conversa c:conversas){
    ContentValues cv=valores("id",c.id,"titulo",c.titulo);cv.put("criado_em",agora());cv.put("atualizado_em",agora());
    if(db.insertWithOnConflict("conversas",null,cv,SQLiteDatabase.CONFLICT_IGNORE)!=-1){
     inseridas++;for(HistoricoLocal.Mensagem m:antigo.mensagens(c.id)){ContentValues mv=valores("conversa_id",c.id,"papel",m.papel,"texto",m.texto);mv.put("criada_em",agora());db.insertOrThrow("mensagens",null,mv);}
    }
   }
   db.insertWithOnConflict("configuracoes",null,valores("chave","legado_importado","valor","1"),SQLiteDatabase.CONFLICT_REPLACE);
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
  return inseridas;
 }
 public synchronized String nova(){return nova(null);}
 public synchronized String nova(String projeto){SQLiteDatabase db=getWritableDatabase();if(projeto!=null)exigeProjeto(db,projeto);String novo=id();ContentValues v=valores("id",novo,"titulo","Nova conversa");if(projeto!=null)v.put("projeto_id",projeto);v.put("criado_em",agora());v.put("atualizado_em",agora());db.insertOrThrow("conversas",null,v);return novo;}
 public synchronized List<HistoricoLocal.Conversa> conversas(){return conversas(false);}
 public synchronized List<HistoricoLocal.Conversa> conversas(boolean arquivadas){List<HistoricoLocal.Conversa> lista=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,titulo FROM conversas WHERE arquivado_em IS "+(arquivadas?"NOT NULL":"NULL")+" ORDER BY atualizado_em DESC",null)){while(c.moveToNext())lista.add(new HistoricoLocal.Conversa(c.getString(0),c.getString(1)));}return lista;}
 public synchronized List<HistoricoLocal.Mensagem> mensagens(String conversa){SQLiteDatabase db=getReadableDatabase();exigeConversa(db,conversa);List<HistoricoLocal.Mensagem> lista=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT papel,texto FROM mensagens WHERE conversa_id=? ORDER BY id",new String[]{conversa})){while(c.moveToNext())lista.add(new HistoricoLocal.Mensagem(c.getString(0),c.getString(1)));}return lista;}
 public synchronized void adicionar(String conversa,String papel,String texto){if(!"user".equals(papel)&&!"assistant".equals(papel))throw new IllegalArgumentException("Autor inválido.");validarTexto(texto);SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
  exigeConversa(db,conversa);try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM mensagens WHERE conversa_id=?",new String[]{conversa})){c.moveToFirst();if(c.getInt(0)==0&&"user".equals(papel)){String titulo=texto.replaceAll("\\s+"," ").trim();db.update("conversas",valores("titulo",titulo.substring(0,Math.min(52,titulo.length()))),"id=?",new String[]{conversa});}}
  ContentValues v=valores("conversa_id",conversa,"papel",papel,"texto",texto);v.put("criada_em",agora());db.insertOrThrow("mensagens",null,v);ContentValues atual=new ContentValues();atual.put("atualizado_em",agora());db.update("conversas",atual,"id=?",new String[]{conversa});db.setTransactionSuccessful();
 }finally{db.endTransaction();}}
 public synchronized void atualizarUltima(String conversa,String papel,String texto){validarTexto(texto);SQLiteDatabase db=getWritableDatabase();try(Cursor c=db.rawQuery("SELECT id,papel FROM mensagens WHERE conversa_id=? ORDER BY id DESC LIMIT 1",new String[]{conversa})){if(c.moveToFirst()&&papel.equals(c.getString(1))){ContentValues v=valores("texto",texto);db.update("mensagens",v,"id=?",new String[]{c.getString(0)});return;}}adicionar(conversa,papel,texto);}
 public synchronized String criarProjeto(String titulo){String novo=id();ContentValues v=valores("id",novo,"nome",nome(titulo));v.put("criado_em",agora());getWritableDatabase().insertOrThrow("projetos",null,v);return novo;}
 public synchronized List<Projeto> projetos(boolean incluirArquivados){List<Projeto> lista=new ArrayList<>();String sql="SELECT id,nome,arquivado_em FROM projetos"+(incluirArquivados?"":" WHERE arquivado_em IS NULL")+" ORDER BY criado_em DESC";try(Cursor c=getReadableDatabase().rawQuery(sql,null)){while(c.moveToNext())lista.add(new Projeto(c.getString(0),c.getString(1),!c.isNull(2)));}return lista;}
 public synchronized void renomearConversa(String conversa,String titulo){SQLiteDatabase db=getWritableDatabase();exigeConversa(db,conversa);db.update("conversas",valores("titulo",nome(titulo)),"id=?",new String[]{conversa});}
 public synchronized void renomearProjeto(String projeto,String titulo){SQLiteDatabase db=getWritableDatabase();exigeProjeto(db,projeto);db.update("projetos",valores("nome",nome(titulo)),"id=?",new String[]{projeto});}
 public synchronized void moverConversa(String conversa,String projeto){SQLiteDatabase db=getWritableDatabase();exigeConversa(db,conversa);if(projeto!=null)exigeProjeto(db,projeto);ContentValues v=new ContentValues();if(projeto==null)v.putNull("projeto_id");else v.put("projeto_id",projeto);db.update("conversas",v,"id=?",new String[]{conversa});}
 public synchronized void arquivarConversa(String conversa,boolean arquivar){SQLiteDatabase db=getWritableDatabase();exigeConversa(db,conversa);ContentValues v=new ContentValues();if(arquivar)v.put("arquivado_em",agora());else v.putNull("arquivado_em");db.update("conversas",v,"id=?",new String[]{conversa});}
 public synchronized void arquivarProjeto(String projeto,boolean arquivar){SQLiteDatabase db=getWritableDatabase();exigeProjeto(db,projeto);ContentValues v=new ContentValues();if(arquivar)v.put("arquivado_em",agora());else v.putNull("arquivado_em");db.update("projetos",v,"id=?",new String[]{projeto});}
 public synchronized void excluirConversa(String conversa){SQLiteDatabase db=getWritableDatabase();exigeConversa(db,conversa);db.delete("conversas","id=?",new String[]{conversa});}
 public synchronized void excluirProjeto(String projeto,boolean incluirConversas){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{exigeProjeto(db,projeto);if(incluirConversas)db.delete("conversas","projeto_id=?",new String[]{projeto});else{ContentValues v=new ContentValues();v.putNull("projeto_id");db.update("conversas",v,"projeto_id=?",new String[]{projeto});}db.delete("projetos","id=?",new String[]{projeto});db.setTransactionSuccessful();}finally{db.endTransaction();}}
 public synchronized List<Resultado> buscar(String consulta){String termo=consulta==null?"":consulta.trim();if(termo.isEmpty())return Collections.emptyList();String escapado="%"+termo.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";List<Resultado> lista=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT m.conversa_id,c.titulo,substr(m.texto,1,220),m.criada_em FROM mensagens m JOIN conversas c ON c.id=m.conversa_id WHERE m.texto LIKE ? ESCAPE '\\' ORDER BY m.id DESC LIMIT 60",new String[]{escapado})){while(c.moveToNext())lista.add(new Resultado(c.getString(0),c.getString(1),c.getString(2),c.getLong(3)));}return lista;}
 public synchronized void configuracao(String chave,String valor){if(chave==null||!chave.matches("[a-z_]{1,60}"))throw new IllegalArgumentException("Chave inválida.");validarTexto(valor);getWritableDatabase().insertWithOnConflict("configuracoes",null,valores("chave",chave,"valor",valor),SQLiteDatabase.CONFLICT_REPLACE);}
 public synchronized String configuracao(String chave){try(Cursor c=getReadableDatabase().rawQuery("SELECT valor FROM configuracoes WHERE chave=?",new String[]{chave})){return c.moveToFirst()?c.getString(0):"";}}
 public synchronized void instrucaoProjeto(String projeto,String texto){validarTexto(texto);SQLiteDatabase db=getWritableDatabase();exigeProjeto(db,projeto);db.update("projetos",valores("instrucao",texto),"id=?",new String[]{projeto});}
 public synchronized String instrucaoEfetiva(String conversa){StringBuilder s=new StringBuilder();String geral=configuracao("sobre_usuario"),estilo=configuracao("como_responder");if(!geral.isEmpty())s.append("Informações fornecidas pelo usuário: ").append(limitar(geral,550)).append("\n");if(!estilo.isEmpty())s.append("Preferências de resposta: ").append(limitar(estilo,450)).append("\n");try(Cursor c=getReadableDatabase().rawQuery("SELECT p.instrucao FROM conversas c LEFT JOIN projetos p ON p.id=c.projeto_id WHERE c.id=?",new String[]{conversa})){if(c.moveToFirst()&&!c.isNull(0))s.append("Instruções do projeto: ").append(limitar(c.getString(0),450)).append("\n");}return s.toString();}
 public synchronized String projetoDaConversa(String conversa){try(Cursor c=getReadableDatabase().rawQuery("SELECT projeto_id FROM conversas WHERE id=?",new String[]{conversa})){return c.moveToFirst()?c.getString(0):null;}}
 public synchronized String novoTrabalho(String objetivo){String texto=nome(objetivo),novo=id();ContentValues v=valores("id",novo,"objetivo",texto,"estado","pendente");v.put("criado_em",agora());v.put("atualizado_em",agora());getWritableDatabase().insertOrThrow("trabalhos",null,v);return novo;}
 public synchronized List<Trabalho> trabalhos(){List<Trabalho> lista=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,objetivo,estado,atualizado_em FROM trabalhos ORDER BY atualizado_em DESC LIMIT 100",null)){while(c.moveToNext())lista.add(new Trabalho(c.getString(0),c.getString(1),c.getString(2),c.getLong(3)));}return lista;}
 public synchronized void concluirTrabalho(String trabalho){ContentValues v=valores("estado","concluido");v.put("atualizado_em",agora());if(getWritableDatabase().update("trabalhos",v,"id=?",new String[]{trabalho})==0)throw new IllegalArgumentException("Trabalho não encontrado.");}
 public synchronized String salvarMemoria(String conteudo,String origemConversa){validarTexto(conteudo);if(conteudo.trim().isEmpty()||conteudo.length()>1200)throw new IllegalArgumentException("Memória deve ter até 1.200 caracteres.");SQLiteDatabase db=getWritableDatabase();String projeto=null;if(origemConversa!=null){exigeConversa(db,origemConversa);projeto=projetoDaConversa(origemConversa);}String novo=id();ContentValues v=valores("id",novo,"conteudo",conteudo.trim());if(projeto!=null)v.put("projeto_id",projeto);if(origemConversa!=null)v.put("origem_conversa_id",origemConversa);v.put("criada_em",agora());db.insertOrThrow("memorias",null,v);return novo;}
 public synchronized List<Memoria> memorias(){List<Memoria> lista=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,conteudo,origem_conversa_id,projeto_id,criada_em FROM memorias ORDER BY criada_em DESC LIMIT 250",null)){while(c.moveToNext())lista.add(new Memoria(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4)));}return lista;}
 public synchronized void removerMemoria(String id){getWritableDatabase().delete("memorias","id=?",new String[]{id});}
 /** Somente lembranças que o usuário salvou explicitamente; nunca inventa fatos. */
 public synchronized String recuperarMemorias(String conversa,String pergunta){String p=pergunta.toLowerCase(Locale.ROOT);String escopo=projetoDaConversa(conversa);StringBuilder retorno=new StringBuilder();int quantidade=0;for(Memoria m:memorias()){
  if(m.projetoId!=null&&!m.projetoId.equals(escopo))continue;String[] termos=m.conteudo.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+");boolean relevante=false;for(String palavra:termos)if(palavra.length()>4&&p.contains(palavra)){relevante=true;break;}if(relevante&&quantidade<4){retorno.append("- ").append(limitar(m.conteudo,230)).append(" [memória do usuário, id ").append(m.id).append("]\n");quantidade++;}
 }return retorno.toString();}
 private static String limitar(String texto,int max){return texto.length()>max?texto.substring(0,max):texto;}
}
