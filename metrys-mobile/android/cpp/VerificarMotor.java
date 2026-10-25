package br.com.anderflow.metrys.offline;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Teste real JNI/JVM; exige modelo GGUF do catálogo como primeiro argumento. */
public final class VerificarMotor {
 public static void main(String[] args)throws Exception{
  MotorLocal m=new MotorLocal();
  try{
   m.carregar(args[0],2048,3);if(!m.carregado())throw new AssertionError("Modelo não carregou");
   StringBuilder resposta=new StringBuilder();AtomicInteger partes=new AtomicInteger();long inicio=System.nanoTime();
   m.gerar(new String[]{"system","user"},new String[]{"Você é Metrys. Responda em português do Brasil.","Escreva uma função Python chamada soma(a,b) que retorna a soma. Resposta curta."},s->{resposta.append(s);partes.incrementAndGet();},80);
   if(resposta.length()<5||partes.get()<2)throw new AssertionError("Resposta/streaming ausentes");
   System.out.println("GERACAO_REAL="+resposta.toString().replace('\n',' '));System.out.println("STREAMING_CALLBACKS="+partes);System.out.println("GERACAO_MS="+(System.nanoTime()-inicio)/1000000);
   boolean falhou=false;
   String grande=new String(new char[20000]).replace("\0"," palavra");
   try{m.gerar(new String[]{"user"},new String[]{grande},s->{},32);}catch(IOException e){falhou=e.getMessage().startsWith("Histórico excede o contexto:");}
   if(!falhou)throw new AssertionError("Overflow não rejeitado");
   falhou=false;try{m.gerar(new String[]{"user"},new String[]{"Diga olá"},s->{throw new IllegalStateException("callback-teste");},16);}catch(IllegalStateException e){falhou=e.getMessage().equals("callback-teste");}
   if(!falhou)throw new AssertionError("Exceção callback não preservada");
   CountDownLatch primeiro=new CountDownLatch(1);ExecutorService e=Executors.newSingleThreadExecutor();
   try{
    Future<?> f=e.submit(()->{try{m.gerar(new String[]{"user"},new String[]{"Conte uma história muito longa, com muitos detalhes, sobre uma viagem."},s->{primeiro.countDown();},512);}catch(IOException ex){throw new RuntimeException(ex);}});
    if(!primeiro.await(60,TimeUnit.SECONDS))throw new AssertionError("Nenhum token antes do cancelamento");
    Thread.sleep(8);inicio=System.nanoTime();m.cancelar();f.get(5,TimeUnit.SECONDS);System.out.println("CANCELAMENTO_MS="+(System.nanoTime()-inicio)/1000000);
   }finally{e.shutdownNow();}
   StringBuilder segunda=new StringBuilder();m.gerar(new String[]{"system","user","assistant","user"},new String[]{"Você é Metrys. Respostas curtas.","Quanto é 2+2?","4","E 2+3? Responda só o número."},segunda::append,24);
   if(segunda.length()==0)throw new AssertionError("Segundo envio falhou");System.out.println("SEGUNDO_ENVIO="+segunda.toString().replace('\n',' '));
  }finally{m.close();m.close();}
  System.out.println("SMOKE_JNI_OK");
 }
}
