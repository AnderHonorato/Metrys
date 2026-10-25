package br.com.anderflow.metrys.offline;

/** Contratos do fluxo que antes descartava respostas sem </think>. */
public final class FluxoRespostaTeste {
 private static void igual(String esperado,String atual){if(!esperado.equals(atual))throw new AssertionError("Esperado ["+esperado+"], recebido ["+atual+"]");}
 public static void main(String[] args){
  FluxoResposta direto=new FluxoResposta(false);
  direto.receber("Olá! Aqui está sua resposta.");
  igual("Olá! Aqui está sua resposta.",direto.resposta());
  igual("",direto.atividade());
  FluxoResposta pensar=new FluxoResposta(true);
  for(String s:new String[]{"<thi","nk>Verificando a mensagem.\nCriando o código soma.","</thi","nk>\n```python\ndef soma(a,b): return a+b\n```"})pensar.receber(s);
  igual("Verificando a mensagem.\nCriando o código soma.",pensar.atividade());
  igual("Criando o código soma.",pensar.titulo());
  igual("```python\ndef soma(a,b): return a+b\n```",pensar.resposta());
  if(pensar.pensando())throw new AssertionError("Pensamento não foi encerrado");
  // O template de Qwen pode já ter aberto <think> antes do primeiro token.
  FluxoResposta implicito=new FluxoResposta(true);
  implicito.receber("Calculando.\n</think>42");
  igual("Calculando.",implicito.atividade());igual("42",implicito.resposta());
  // Modelo sem raciocínio não perde a resposta por causa do catálogo.
  FluxoResposta explicito=new FluxoResposta(false);
  explicito.receber("<think>Ajustando a função.</think>Pronto.");
  igual("Ajustando a função.",explicito.atividade());igual("Pronto.",explicito.resposta());
  FluxoResposta incompleto=new FluxoResposta(true);
  incompleto.receber("Ainda calculando sem marcador final");
  igual("",incompleto.resposta());
  igual("Ainda calculando sem marcador final",incompleto.atividade());
  // Não transforma código intermediário em título.
  FluxoResposta codigo=new FluxoResposta(true);
  codigo.receber("Criando a função.\n```python\nprint('teste')\n```");
  igual("Criando a função.",codigo.titulo());
  String esperada="Olá <texto> UTF-8: ação 🪷";
  for(int passo=1;passo<12;passo++){
   FluxoResposta f=new FluxoResposta(false);String s="<think>Verificando.</think>"+esperada;
   for(int i=0;i<s.length();i+=passo)f.receber(s.substring(i,Math.min(s.length(),i+passo)));
   igual(esperada,f.resposta());igual("Verificando.",f.atividade());
  }
  System.out.println("PASS fluxo: texto direto, marcadores fragmentados, template implícito, atividade, título e Unicode");
 }
}
