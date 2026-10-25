package br.com.anderflow.metrys.offline;

/** Separa os canais do modelo sem perder marcadores divididos entre tokens. */
public final class FluxoResposta {
 private static final String ABRIR="<think>",FECHAR="</think>";
 private final StringBuilder pendente=new StringBuilder(),atividade=new StringBuilder(),resposta=new StringBuilder();
 private boolean pensando,detectando,inicioAtividade=true;
 public FluxoResposta(boolean raciocinio){pensando=raciocinio;detectando=!raciocinio;}
 public void receber(String parte){
  if(parte==null||parte.isEmpty())return;
  pendente.append(parte);
  if(detectando){
   String s=pendente.toString().trim();
   if(ABRIR.startsWith(s))return;
   detectando=false;
   if(s.startsWith(ABRIR)){pensando=true;pendente.setLength(0);pendente.append(s.substring(ABRIR.length()));inicioAtividade=false;}
  }
  if(!pensando){resposta.append(pendente);pendente.setLength(0);return;}
  if(inicioAtividade){
   String s=pendente.toString().trim();
   if(ABRIR.startsWith(s))return;
   if(s.startsWith(ABRIR)){pendente.setLength(0);pendente.append(s.substring(ABRIR.length()));}
   inicioAtividade=false;
  }
  int fim=pendente.indexOf(FECHAR);
  if(fim>=0){atividade.append(pendente.substring(0,fim));resposta.append(pendente.substring(fim+FECHAR.length()));pendente.setLength(0);pensando=false;return;}
  int guardar=0;
  for(int n=1;n<FECHAR.length()&&n<=pendente.length();n++)if(FECHAR.startsWith(pendente.substring(pendente.length()-n)))guardar=n;
  atividade.append(pendente.substring(0,pendente.length()-guardar));pendente.delete(0,pendente.length()-guardar);
 }
 public void finalizar(){if(pensando)atividade.append(pendente);else resposta.append(pendente);pendente.setLength(0);}
 public String resposta(){return resposta.toString().trim();}
 public String atividade(){return atividade.toString().trim();}
 public boolean pensando(){return pensando;}
 /** Usa texto realmente produzido pelo modelo; não simula etapas executadas. */
 public String titulo(){
  String ultimo="";boolean codigo=false;
  for(String linha:atividade.toString().split("\n")){
   String s=linha.trim();if(s.startsWith("```")){codigo=!codigo;continue;}
   if(!codigo&&!s.isEmpty()&&!s.matches("^[{}<>#=].*"))ultimo=s.replaceAll("\\s+"," ");
  }
  if(ultimo.length()>90){int fim=ultimo.lastIndexOf(' ',87);ultimo=ultimo.substring(0,fim>35?fim:87)+"…";}
  return ultimo;
 }
}
