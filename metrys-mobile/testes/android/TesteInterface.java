package br.com.anderflow.metrys.offline;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

/** Executado em Android real/emulado; nenhuma mensagem sai do aparelho. */
public final class TesteInterface extends Instrumentation {
 private static void conferir(boolean valor,String erro){if(!valor)throw new AssertionError(erro);}
 private static Object campo(Object alvo,String nome)throws Exception{Field f=alvo.getClass().getDeclaredField(nome);f.setAccessible(true);return f.get(alvo);}
 private static void definir(Object alvo,String nome,Object valor)throws Exception{Field f=alvo.getClass().getDeclaredField(nome);f.setAccessible(true);f.set(alvo,valor);}
 private static void chamar(Object alvo,String nome,Class<?>[] tipos,Object...args)throws Exception{Method m=alvo.getClass().getDeclaredMethod(nome,tipos);m.setAccessible(true);m.invoke(alvo,args);}
 private void principal(Exame exame){java.util.concurrent.atomic.AtomicReference<Throwable> falha=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->{try{exame.executar();}catch(Throwable e){falha.set(e);}});if(falha.get()!=null)throw new AssertionError("Falha na interface",falha.get());}
 private interface Exame{void executar()throws Exception;}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){
  Bundle resultado=new Bundle();Activity a=null;
  try{
   Intent intent=new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext(),"br.com.anderflow.metrys.offline.TelaOffline").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   a=startActivitySync(intent);waitForIdleSync();final Activity tela=a;
   principal(()->{ServicoOffline.obter(getTargetContext()).remover((ServicoOffline.Ouvidor)tela);definir(tela,"observando",false);});
   testarBanco();testarRegeneracaoFalha();testarInterface(tela);
   waitForIdleSync();try(java.io.FileOutputStream f=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),"metrys-qa.png"))){getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}
   resultado.putString("stream","PASS Android: banco, fila durável, retomada idempotente, regeneração com falha, expansão persistente, tema e anexo\n");
   principal(tela::finish);finish(Activity.RESULT_OK,resultado);
  }catch(Throwable e){resultado.putString("stream",android.util.Log.getStackTraceString(e));if(a!=null){final Activity tela=a;principal(tela::finish);}finish(Activity.RESULT_CANCELED,resultado);}
 }
 private void testarBanco(){
  BancoMetrys b=new BancoMetrys(getTargetContext());String conversa=b.nova();
  BancoMetrys.Pedido primeiro=b.enfileirar(conversa,"Primeira pergunta"),segundo=b.enfileirar(conversa,"Segunda pergunta");
  b.close();b=new BancoMetrys(getTargetContext());
  List<BancoMetrys.Pedido> fila=b.pedidosPendentes();conferir(fila.size()>=2,"Fila sumiu após reabrir banco");
  conferir(fila.get(fila.size()-2).id==primeiro.id&&fila.get(fila.size()-1).id==segundo.id,"Fila fora de ordem");
  b.iniciarPedido(primeiro.id);b.iniciarPedido(primeiro.id);
  List<HistoricoLocal.Mensagem> mensagens=b.mensagens(conversa);conferir(mensagens.size()==1,"Retomada duplicou a pergunta");
  long pergunta=mensagens.get(0).id;b.salvarAtividade(pergunta,"Calculando","Atividade gerada","gerando",500);
  b.concluirGeracao(conversa,"Resposta",false,pergunta,"Calculando","Atividade gerada","concluido",1000,primeiro.id);
  b.close();b=new BancoMetrys(getTargetContext());
  conferir(b.mensagens(conversa).get(0).atividade.equals("Atividade gerada"),"Atividade não persistiu");
  conferir(b.pedidosPendentes().size()==fila.size()-1,"Resposta não removeu apenas seu pedido da fila");
  b.iniciarPedido(segundo.id);conferir(b.mensagens(conversa).size()==3,"Segunda pergunta não seguiu a primeira resposta");
  b.recuperarAtividadesInterrompidas();conferir(b.mensagens(conversa).get(0).estadoAtividade.equals("concluido"),"Recuperação alterou atividade concluída");
  b.excluirConversa(conversa);b.close();
 }
 private void testarRegeneracaoFalha()throws Exception{
  ServicoOffline s=ServicoOffline.obter(getTargetContext());
  long limite=SystemClock.elapsedRealtime()+15000;
  while("".equals(s.estado().conversaId)&&SystemClock.elapsedRealtime()<limite)Thread.sleep(25);
  String original=s.estado().conversaId;
  BancoMetrys b=new BancoMetrys(getTargetContext());String conversa=b.nova();b.adicionar(conversa,"user","Uma pergunta");
  long pergunta=b.mensagens(conversa).get(0).id;b.adicionar(conversa,"assistant","Resposta que deve permanecer");b.salvarAtividade(pergunta,"Título anterior","Atividade anterior","concluido",700);
  synchronized(s){definir(s,"id",conversa);definir(s,"modeloPronto",true);}
  conferir(s.refazerResposta(),"Não iniciou regeneração de teste");
  // APK de UI não tem JNI: o erro de carregamento deve preservar a versão anterior.
  limite=SystemClock.elapsedRealtime()+15000;
  while((Boolean)campo(s,"ocupado")&&SystemClock.elapsedRealtime()<limite)Thread.sleep(25);
  conferir(!(Boolean)campo(s,"ocupado"),"Serviço ficou ocupado após falhar");
  List<HistoricoLocal.Mensagem> ms=b.mensagens(conversa);
  conferir(ms.size()==2&&ms.get(1).texto.equals("Resposta que deve permanecer"),"Falha apagou resposta anterior");
  conferir(ms.get(0).atividade.equals("Atividade anterior"),"Falha apagou atividade anterior");
  synchronized(s){definir(s,"modeloPronto",false);definir(s,"id",original);}b.excluirConversa(conversa);b.close();
 }
 private void testarInterface(Activity tela){
  EstadoOffline e=new EstadoOffline();e.modeloPronto=true;e.fase="pronto";e.conversaId="qa-interface";
  EstadoOffline.Mensagem pergunta=new EstadoOffline.Mensagem("user","Minha pergunta");pergunta.id=101;pergunta.atividade="Verificando o conteúdo.\n```python\nprint('prévia')\n```";pergunta.tituloAtividade="Verificando o conteúdo.";pergunta.estadoAtividade="concluido";pergunta.duracaoMs=1000;
  e.mensagens.add(pergunta);e.mensagens.add(new EstadoOffline.Mensagem("assistant","Resposta final"));
  principal(()->{
   chamar(tela,"desenhar",new Class<?>[]{EstadoOffline.class},e);
   Object r=((List<?>)campo(tela,"renderizadas")).get(0);LinearLayout painel=(LinearLayout)campo(r,"atividadePainel");
   conferir(painel.getVisibility()==View.VISIBLE,"Painel não aparece");
   painel.getChildAt(0).performClick();conferir((Boolean)campo(r,"atividadeAberta"),"Expandir não abriu");
   conferir(((LinearLayout)campo(r,"atividadeDetalhe")).getChildCount()>0,"Atividade expandida está vazia");
   EstadoOffline.Mensagem nova=new EstadoOffline.Mensagem("user","Segunda pergunta");nova.id=102;nova.estadoAtividade="gerando";
   e.mensagens.add(nova);e.gerando=true;e.fase="gerando";e.filaPendente=1;
   chamar(tela,"desenhar",new Class<?>[]{EstadoOffline.class},e);
   Object anterior=((List<?>)campo(tela,"renderizadas")).get(0);
   conferir(anterior==r&&(Boolean)campo(anterior,"atividadeAberta"),"Nova mensagem apagou/fechou atividade anterior");
   conferir(((TextView)campo(anterior,"atividadeTitulo")).getText().toString().equals("Verificando o conteúdo."),"Título gerado foi substituído por etapa padrão");
   EditText entrada=(EditText)campo(tela,"entrada");char[] longo=new char[1400];Arrays.fill(longo,'x');entrada.setText(new String(longo));
   conferir(entrada.length()==0&&((String)campo(tela,"textoAnexado")).length()==1400,"Texto extenso não virou anexo");
   tela.getSharedPreferences("estudio_visual",Context.MODE_PRIVATE).edit().putInt("margem_usuario",60).putInt("codigo",0xff223344).apply();
   chamar(tela,"aplicarMudancaVisual",new Class<?>[0]);chamar(tela,"desenhar",new Class<?>[]{EstadoOffline.class},e);
   Object reconstruido=((List<?>)campo(tela,"renderizadas")).get(0);
   conferir((Boolean)campo(reconstruido,"atividadeAberta"),"Reconstrução do tema perdeu expansão salva");
   conferir(((String)campo(tela,"textoAnexado")).length()==1400,"Tema perdeu anexo em edição");
   ((ScrollView)campo(tela,"scroll")).fullScroll(View.FOCUS_UP);
  });
 }
}
