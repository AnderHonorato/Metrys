package br.com.anderflow.metrys.offline;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Carregar, gerar e fechar pertencem ao executor; cancelar pode rodar na UI. */
public final class MotorLocal implements AutoCloseable {
 public static final int FIM_MODELO=0,LIMITE_CONTEXTO=1,CANCELADO=2;
 public interface Saida {void token(String texto);}
 private volatile long identificador; private volatile boolean pronto;private volatile int contextoCarregado;
 public MotorLocal(){System.loadLibrary("metrys");identificador=criarNativo();if(identificador==0)throw new IllegalStateException("Não foi possível criar o motor local.");}
 public synchronized void carregar(String arquivo,int contexto,int threads)throws IOException{
  carregar(arquivo,contexto,threads,0);
 }
 public synchronized void carregar(String arquivo,int contexto,int threads,long memoriaDisponivel)throws IOException{
  aberto();pronto=false;contextoCarregado=0;carregarNativo(identificador,arquivo,contexto,threads,memoriaDisponivel);contextoCarregado=contextoNativo(identificador);pronto=true;
 }
 public synchronized void gerar(String[] papeis,String[] mensagens,Saida saida,int maxTokens)throws IOException{
  gerar(papeis,mensagens,saida,maxTokens,false);
 }
 /** maxTokens=0 usa o espaço restante do contexto, com transição para resposta final. */
 public synchronized int gerar(String[] papeis,String[] mensagens,Saida saida,int maxTokens,boolean raciocinio)throws IOException{
  aberto();if(!pronto)throw new IOException("Carregue um modelo antes de gerar.");
  if(papeis==null||mensagens==null||saida==null)throw new IOException("Histórico e saída são obrigatórios.");
  return gerarNativo(identificador,papeis,mensagens,saida,maxTokens,raciocinio);
 }
 public int contextoAtivo(){return contextoCarregado;}
 public void cancelar(){long id=identificador;if(id!=0)cancelarNativo(id);}
 public boolean carregado(){return pronto&&identificador!=0;}
 @Override public synchronized void close(){long id=identificador;identificador=0;pronto=false;contextoCarregado=0;if(id!=0)fecharNativo(id);}
 private void aberto()throws IOException{if(identificador==0)throw new IOException("O motor local foi fechado.");}
 private static void entregar(Saida saida,byte[] bytes){saida.token(new String(bytes,StandardCharsets.UTF_8));}
 private static native long criarNativo();
 private static native void carregarNativo(long id,String arquivo,int contexto,int threads,long memoriaDisponivel)throws IOException;
 private static native int gerarNativo(long id,String[] papeis,String[] mensagens,Saida saida,int maxTokens,boolean raciocinio)throws IOException;
 private static native int contextoNativo(long id)throws IOException;
 private static native void cancelarNativo(long id);
 private static native void fecharNativo(long id);
}
