package br.com.anderflow.metrys.offline;
import java.io.IOException;

/** Integra a estimativa de KV com o carregamento JNI de Qwen3 0.6B. */
public final class MemoriaMotorTeste{
 public static void main(String[] args)throws Exception{
  try(MotorLocal m=new MotorLocal()){
   m.carregar(args[0],0,3,1024L*1024*1024);
   int contexto=m.contextoAtivo();if(contexto<512||contexto>=16384)throw new AssertionError("Estimativa não limitou contexto: "+contexto);
   System.out.println("PASS memória JNI: contexto automático ajustado a 1 GiB disponível = "+contexto);
   boolean recusou=false;try{m.carregar(args[0],0,3,64L*1024*1024);}catch(IOException e){recusou=e.getMessage().contains("Memória disponível insuficiente");}
   if(!recusou||m.carregado())throw new AssertionError("Modelo foi aceito sem memória disponível");
   System.out.println("PASS memória JNI: insuficiência informada antes de alocar contexto");
  }
 }
}
