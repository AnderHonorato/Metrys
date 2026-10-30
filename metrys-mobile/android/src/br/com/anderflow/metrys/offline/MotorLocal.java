package br.com.anderflow.metrys.offline;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Carregar, gerar e fechar pertencem ao executor; cancelar pode rodar na UI. */
public final class MotorLocal implements AutoCloseable {
 public interface Saida {void token(String texto);}
 private volatile long identificador; private volatile boolean pronto;
 public MotorLocal(){System.loadLibrary("metrys");identificador=criarNativo();if(identificador==0)throw new IllegalStateException("Não foi possível criar o motor local.");}
 public synchronized void carregar(String arquivo,int contexto,int threads)throws IOException{
  aberto();pronto=false;carregarNativo(identificador,arquivo,contexto,threads);pronto=true;
 }
 public synchronized void gerar(String[] papeis,String[] mensagens,Saida saida,int maxTokens)throws IOException{
  aberto();if(!pronto)throw new IOException("Carregue um modelo antes de gerar.");
  if(papeis==null||mensagens==null||saida==null)throw new IOException("Histórico e saída são obrigatórios.");
  gerarNativo(identificador,papeis,mensagens,saida,maxTokens);
 }
 public void cancelar(){long id=identificador;if(id!=0)cancelarNativo(id);}
 public boolean carregado(){return pronto&&identificador!=0;}
 @Override public synchronized void close(){long id=identificador;identificador=0;pronto=false;if(id!=0)fecharNativo(id);}
 private void aberto()throws IOException{if(identificador==0)throw new IOException("O motor local foi fechado.");}
 private static void entregar(Saida saida,byte[] bytes){saida.token(new String(bytes,StandardCharsets.UTF_8));}
 private static native long criarNativo();
 private static native void carregarNativo(long id,String arquivo,int contexto,int threads)throws IOException;
 private static native void gerarNativo(long id,String[] papeis,String[] mensagens,Saida saida,int maxTokens)throws IOException;
 private static native void cancelarNativo(long id);
 private static native void fecharNativo(long id);
}
