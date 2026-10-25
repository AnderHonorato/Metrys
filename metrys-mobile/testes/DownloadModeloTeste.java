import br.com.anderflow.metrys.offline.DownloadModelo;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class DownloadModeloTeste {
 static byte[] modelo=new byte[196608]; static String hash;
 static void exige(boolean v,String m){if(!v)throw new AssertionError(m);}
 static String sha(byte[] b)throws Exception{StringBuilder s=new StringBuilder();for(byte x:MessageDigest.getInstance("SHA-256").digest(b))s.append(String.format("%02x",x&255));return s.toString();}
 static File pasta()throws Exception{return Files.createTempDirectory("metrys-download-").toFile();}
 static DownloadModelo.Spec spec(String... urls){return new DownloadModelo.Spec("modelo",urls,modelo.length,hash);}
 static void resposta(HttpExchange e,int status,byte[] b)throws IOException{e.sendResponseHeaders(status,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
 static void erro(DownloadModelo.Spec s,File p)throws Exception{boolean falhou=false;try{DownloadModelo.baixar(s,p,new DownloadModelo.Controle(),null);}catch(IOException e){falhou=true;}exige(falhou,"Era esperado erro");exige(!new File(p,"modelo.gguf").exists(),"Promoveu modelo inválido");}
 public static void main(String[] args)throws Exception{
  new Random(41).nextBytes(modelo);System.arraycopy(new byte[]{'G','G','U','F'},0,modelo,0,4);hash=sha(modelo);
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;}));
  AtomicBoolean viuRange=new AtomicBoolean();CountDownLatch iniciou=new CountDownLatch(1);
  server.createContext("/ok",e->{e.getResponseHeaders().set("ETag","\"v1\"");String range=e.getRequestHeaders().getFirst("Range");if(range!=null){int n=Integer.parseInt(range.substring(6,range.length()-1));viuRange.set(true);e.getResponseHeaders().set("Content-Range","bytes "+n+"-"+(modelo.length-1)+"/"+modelo.length);resposta(e,206,Arrays.copyOfRange(modelo,n,modelo.length));}else resposta(e,200,modelo);});
  server.createContext("/ignore",e->{e.getResponseHeaders().set("ETag","\"v1\"");resposta(e,200,modelo);});
  server.createContext("/bad",e->{byte[] b=modelo.clone();b[99]^=1;resposta(e,200,b);});
  server.createContext("/html",e->{e.getResponseHeaders().set("Content-Type","text/html");resposta(e,200,"<html>login</html>".getBytes());});
  server.createContext("/rangebad",e->{e.getResponseHeaders().set("ETag","\"v1\"");e.getResponseHeaders().set("Content-Range","bytes 7-9/10");resposta(e,206,modelo);});
  server.createContext("/full",e->{e.getResponseHeaders().set("Content-Range","bytes */"+modelo.length);e.sendResponseHeaders(416,-1);e.close();});
  server.createContext("/redirect",e->{e.getResponseHeaders().set("Location","/ok");e.sendResponseHeaders(302,-1);e.close();});
  server.createContext("/unsafe",e->{e.getResponseHeaders().set("Location","http://example.com/modelo");e.sendResponseHeaders(302,-1);e.close();});
  server.createContext("/slow",e->{e.getResponseHeaders().set("ETag","\"v1\"");e.sendResponseHeaders(200,modelo.length);try(OutputStream o=e.getResponseBody()){o.write(modelo,0,32768);o.flush();iniciou.countDown();Thread.sleep(6000);o.write(modelo,32768,modelo.length-32768);}catch(Exception ignored){}});
  server.createContext("/pause",e->{e.sendResponseHeaders(200,modelo.length);try(OutputStream o=e.getResponseBody()){o.write(modelo,0,32768);o.flush();Thread.sleep(2400);o.write(modelo,32768,modelo.length-32768);}catch(Exception ignored){}});
  server.createContext("/chunked",e->{e.sendResponseHeaders(200,0);try(OutputStream o=e.getResponseBody()){o.write(modelo);o.write(1);}});
  server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();int n=0;
  try{
   File p=pasta();DownloadModelo.Resultado r=DownloadModelo.baixar(spec(base+"/ok"),p,new DownloadModelo.Controle(),null);exige(r.file.length()==modelo.length&&r.sha256.equals(hash),"Download incorreto");DownloadModelo.baixar(spec(base+"/bad"),p,new DownloadModelo.Controle(),null);System.out.println("PASS download e reutilização verificada");n++;
   p=pasta();parcial(p,base+"/ok",32768,"\"v1\"");DownloadModelo.baixar(spec(base+"/ok"),p,new DownloadModelo.Controle(),null);exige(viuRange.get(),"Não retomou Range");System.out.println("PASS retomada ETag/Range");n++;
   p=pasta();parcial(p,base+"/ignore",32768,"\"v1\"");DownloadModelo.baixar(spec(base+"/ignore"),p,new DownloadModelo.Controle(),null);exige(new File(p,"modelo.gguf").length()==modelo.length,"Concatenou200");System.out.println("PASS 200 reinicia");n++;
   erro(spec(base+"/bad"),pasta());System.out.println("PASS SHA inválido");n++;
   erro(spec(base+"/html"),pasta());System.out.println("PASS HTML rejeitado");n++;
   erro(spec(base+"/rangebad"),pasta());System.out.println("PASS Content-Range inválido");n++;
   p=pasta();parcial(p,base+"/full",modelo.length,"\"v1\"");DownloadModelo.baixar(spec(base+"/full"),p,new DownloadModelo.Controle(),null);System.out.println("PASS 416 completo");n++;
   p=pasta();parcial(p,base+"/full",20,"\"v1\"");erro(spec(base+"/full"),p);System.out.println("PASS 416 incompleto");n++;
   p=pasta();DownloadModelo.baixar(spec(base+"/html",base+"/ok"),p,new DownloadModelo.Controle(),null);System.out.println("PASS fallback mirror");n++;
   DownloadModelo.baixar(spec(base+"/redirect"),pasta(),new DownloadModelo.Controle(),null);erro(spec(base+"/unsafe"),pasta());System.out.println("PASS redirects seguros");n++;
   erro(spec(base+"/chunked"),pasta());System.out.println("PASS bytes excedentes");n++;
   DownloadModelo.Controle controle=new DownloadModelo.Controle();CountDownLatch leu=new CountDownLatch(1);File slow=pasta();AtomicReference<Throwable> falha=new AtomicReference<>();Thread worker=new Thread(()->{try{DownloadModelo.baixar(spec(base+"/slow"),slow,controle,(baix,total,origem)->{if(baix>=32768)leu.countDown();});}catch(Throwable e){falha.set(e);}});worker.start();exige(leu.await(4,TimeUnit.SECONDS),"Cliente não recebeu o parcial");long inicio=System.nanoTime();controle.cancel();worker.join(3500);exige(!worker.isAlive(),"Cancelamento bloqueado");exige((System.nanoTime()-inicio)/1000000<3500,"Cancelamento lento");if(!(falha.get() instanceof IOException)){if(falha.get()!=null)falha.get().printStackTrace();throw new AssertionError("Cancelamento sem IOException: "+falha.get());}exige(new File(slow,"modelo.gguf.part").exists(),"Parcial perdido");System.out.println("PASS cancelamento bloqueado preserva parcial");n++;
   boolean rejeitou=false;try{DownloadModelo.baixar(new DownloadModelo.Spec("../escapa",new String[]{base+"/ok"},modelo.length,hash),pasta(),controle,null);}catch(IOException e){rejeitou=true;}exige(rejeitou,"id hostil aceito");System.out.println("PASS id hostil");n++;
   p=pasta();viuRange.set(false);parcial(p,base+"/ok",1024,"W/\"v1\"");DownloadModelo.baixar(spec(base+"/ok"),p,new DownloadModelo.Controle(),null);exige(!viuRange.get(),"ETag fraco retomado");System.out.println("PASS ETag fraco reinicia");n++;
   byte[] nao=modelo.clone();nao[0]='X';erro(new DownloadModelo.Spec("modelo",new String[]{base+"/ok"},modelo.length,sha(nao)),pasta());System.out.println("PASS combinação hash/conteúdo");n++;
   DownloadModelo.baixar(spec(base+"/pause"),pasta(),new DownloadModelo.Controle(),null);System.out.println("PASS pausa curta no corpo");n++;
   System.out.println("OK: "+n+" cenários HTTP reais.");
  }finally{server.stop(0);}
 }
 static void parcial(File p,String origem,int bytes,String etag)throws Exception{Files.write(new File(p,"modelo.gguf.part").toPath(),Arrays.copyOf(modelo,bytes));Properties props=new Properties();props.setProperty("origem",origem);props.setProperty("etag",etag);props.setProperty("sha256",hash);props.setProperty("bytes",Integer.toString(modelo.length));try(OutputStream o=new FileOutputStream(new File(p,"modelo.gguf.part.properties"))){props.store(o,"");}}
}
