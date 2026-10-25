package br.com.anderflow.metrys.offline;

public final class RaciocinioMotorTeste {
 public static void main(String[] args)throws Exception{
  try(MotorLocal m=new MotorLocal()){
   m.carregar(args[0],512,3);
   FluxoResposta fluxo=new FluxoResposta(true);
   int resultado=m.gerar(new String[]{"system","user"},new String[]{"Responda em português e entregue uma resposta final.","Qual é 17 vezes 23? Confira o cálculo e entregue o resultado."},fluxo::receber,0,true);
   fluxo.finalizar();
   if(fluxo.atividade().isEmpty())throw new AssertionError("Atividade não chegou ao Java");
   if(fluxo.pensando()||fluxo.resposta().isEmpty())throw new AssertionError("Modelo terminou sem resposta final: "+fluxo.atividade());
   if(resultado==MotorLocal.CANCELADO)throw new AssertionError("Cancelamento inesperado");
   System.out.println("PASS raciocínio real: atividade separada e resposta final; fim="+resultado);
   System.out.println("RESPOSTA_REAL="+fluxo.resposta().replace('\n',' '));
   m.carregar(args[0],4096,3);
   StringBuilder pergunta=new StringBuilder();for(int i=0;i<1900;i++)pergunta.append("teste ");pergunta.append("Responda OK. /no_think");
   StringBuilder resposta=new StringBuilder();m.gerar(new String[]{"user"},new String[]{pergunta.toString()},resposta::append,24);
   if(resposta.length()==0)throw new AssertionError("Contexto longo não gerou texto");
   if(m.contextoAtivo()<4096)throw new AssertionError("Contexto foi reduzido sem necessidade");
   System.out.println("PASS contexto real: entrada acima do antigo teto de 1536 tokens");
  }
 }
}
