package br.com.anderflow.metrys.offline;

/** Usa o motor JNI real; o valor zero deve usar o espaço disponível do contexto. */
public final class ContextoDinamicoTeste {
 public static void main(String[] args)throws Exception{
  try(MotorLocal m=new MotorLocal()){
   m.carregar(args[0],512,3);
   StringBuilder resposta=new StringBuilder();
   m.gerar(new String[]{"user"},new String[]{"Quanto é 2+2? Responda brevemente. /no_think"},resposta::append,0);
   if(resposta.toString().trim().isEmpty())throw new AssertionError("Geração automática não retornou texto");
   System.out.println("PASS contexto automático: geração sem teto fixo de 512/900 tokens");
  }
 }
}
