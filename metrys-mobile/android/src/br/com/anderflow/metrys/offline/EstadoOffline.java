package br.com.anderflow.metrys.offline;
import java.util.*;
public final class EstadoOffline{
 public String fase="ausente",detalhe="Baixe o modelo uma vez para conversar sem internet.",nomeModelo="Qwen2.5 Coder · 1,5B Q4",conversaId="";
 public int filaPendente=0;public long bytes=0,total=1117320768L;public boolean gerando=false,modeloPronto=false;
 public List<String> etapas=new ArrayList<>();public int trechosGerados=0;public long tempoGeracaoMs=0;public String raciocinioPublicado="";public boolean raciocinando=false;
 public List<Conversa> conversas=new ArrayList<>(),conversasArquivadas=new ArrayList<>();public List<Mensagem> mensagens=new ArrayList<>();public List<Projeto> projetos=new ArrayList<>();
 public static final class Conversa{public String id,titulo,projetoId;public Conversa(String i,String t,String projeto){id=i;titulo=t;projetoId=projeto;}}
 public static final class Projeto{public String id,nome;public boolean arquivado;public Projeto(String id,String nome,boolean arquivado){this.id=id;this.nome=nome;this.arquivado=arquivado;}}
 public static final class Mensagem{
  public String papel,texto,atividade="",tituloAtividade="",estadoAtividade="";public long id,duracaoMs;
  public Mensagem(String p,String t){papel=p;texto=t;}
  public Mensagem(HistoricoLocal.Mensagem m){this(m.papel,m.texto);id=m.id;atividade=m.atividade;tituloAtividade=m.tituloAtividade;estadoAtividade=m.estadoAtividade;duracaoMs=m.duracaoMs;}
 }
}
