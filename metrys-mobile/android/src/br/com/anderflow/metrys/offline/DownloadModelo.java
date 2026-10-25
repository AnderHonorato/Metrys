package br.com.anderflow.metrys.offline;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

/** Transferência bloqueante de um modelo identificado por tamanho e SHA-256. */
public final class DownloadModelo {
 private DownloadModelo(){}
 public static final class Spec{
  public final String id;public final String[] urls;public final long bytes;public final String sha256;
  public Spec(String id,String[] urls,long bytes,String sha256){this.id=id;this.urls=urls==null?null:urls.clone();this.bytes=bytes;this.sha256=sha256;}
 }
 public interface Progresso{void onProgress(long baixados,long total,String origem);}
 public static final class Resultado{public final File file;public final String sha256;Resultado(File f,String s){file=f;sha256=s;}}
 public static final class Controle{
  private volatile boolean cancelado;private HttpURLConnection ativa;
  public void cancel(){HttpURLConnection c;synchronized(this){cancelado=true;c=ativa;}if(c!=null)c.disconnect();}
  public boolean isCancelled(){return cancelado;}
  private synchronized void registrar(HttpURLConnection c)throws IOException{verificar(this);ativa=c;}
  private synchronized void liberar(HttpURLConnection c){if(ativa==c)ativa=null;}
 }
 private static final class Invalido extends IOException{Invalido(String m){super(m);}}
 public static Resultado baixar(Spec s,File pasta,Controle c,Progresso p)throws IOException{
  validar(s);if(c==null)c=new Controle();verificar(c);
  if(pasta==null||(!pasta.isDirectory()&&!pasta.mkdirs()))throw new IOException("Não foi possível preparar o armazenamento do modelo.");
  File base=pasta.getCanonicalFile(),destino=filho(base,s.id+".gguf"),parcial=filho(base,s.id+".gguf.part"),meta=filho(base,s.id+".gguf.part.properties");
  if(destino.isFile())try{return new Resultado(destino,conferir(destino,s,c));}catch(Invalido e){remover(destino);}
  IOException ultima=null;
  for(String origem:s.urls){verificar(c);try{
   transferir(s,origem,base,parcial,meta,c,p);String hash=conferir(parcial,s,c);verificar(c);
   try{Files.move(parcial.toPath(),destino.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(parcial.toPath(),destino.toPath(),StandardCopyOption.REPLACE_EXISTING);}
   meta.delete();return new Resultado(destino,hash);
  }catch(IOException e){if(c.isCancelled())throw cancelamento();if(e instanceof Invalido)limpar(parcial,meta);ultima=e;}
   catch(RuntimeException e){if(c.isCancelled())throw cancelamento();throw e;}
  }
  throw new IOException("Não foi possível baixar o modelo. "+(ultima==null?"Nenhuma origem disponível.":ultima.getMessage()),ultima);
 }
 private static void transferir(Spec s,String origem,File pasta,File parcial,File meta,Controle c,Progresso p)throws IOException{
  Properties props=new Properties();if(meta.isFile())try(InputStream e=new FileInputStream(meta)){props.load(e);}
  String etag=props.getProperty("etag","");
  boolean retomar=parcial.isFile()&&parcial.length()>0&&parcial.length()<=s.bytes&&origem.equals(props.getProperty("origem"))&&s.sha256.equalsIgnoreCase(props.getProperty("sha256",""))&&Long.toString(s.bytes).equals(props.getProperty("bytes"))&&forte(etag);
  if(!retomar)limpar(parcial,meta);long inicio=retomar?parcial.length():0;
  if(pasta.getUsableSpace()<s.bytes-inicio+8*1024*1024)throw new IOException("Espaço livre insuficiente para baixar o modelo.");
  HttpURLConnection conexao=abrir(new URL(origem),inicio,etag,c);
  try{
   int status=conexao.getResponseCode();verificar(c);
   if(status==416){if(inicio==s.bytes&&("bytes */"+s.bytes).equals(conexao.getHeaderField("Content-Range")))return;throw new Invalido("HTTP 416: o parcial não corresponde ao modelo completo.");}
   if(status!=200&&status!=206)throw new IOException("A origem recusou o arquivo (HTTP "+status+"). Tente novamente ou use o Drive.");
   String tipo=conexao.getContentType();if(tipo!=null&&(tipo.toLowerCase(Locale.ROOT).contains("html")))throw new Invalido("A origem retornou uma página HTML, não o modelo.");
   String novoEtag=conexao.getHeaderField("ETag");
   if(status==206){
    if(inicio==0||!etag.equals(novoEtag))throw new Invalido("A retomada não preservou a identidade do arquivo.");
    Matcher m=Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matcher(String.valueOf(conexao.getHeaderField("Content-Range")));
    if(!m.matches())throw new Invalido("Content-Range inválido.");
    try{if(Long.parseLong(m.group(1))!=inicio||Long.parseLong(m.group(2))!=s.bytes-1||Long.parseLong(m.group(3))!=s.bytes)throw new Invalido("Content-Range incompatível com o modelo.");}catch(NumberFormatException e){throw new Invalido("Content-Range fora do limite.");}
   }else{inicio=0;limpar(parcial,meta);}
   long anunciado=conexao.getContentLengthLong();if(anunciado>=0&&anunciado!=s.bytes-inicio)throw new Invalido("O tamanho anunciado não corresponde ao modelo.");
   Properties novos=new Properties();novos.setProperty("origem",origem);novos.setProperty("etag",forte(novoEtag)?novoEtag:"");novos.setProperty("bytes",Long.toString(s.bytes));novos.setProperty("sha256",s.sha256);
   try(OutputStream o=new FileOutputStream(meta)){novos.store(o,"Metrys: identidade do download");}
   try(InputStream e=conexao.getInputStream();FileOutputStream o=new FileOutputStream(parcial,inicio>0)){
    byte[] b=new byte[65536];long n=inicio,ultimo=System.nanoTime();boolean magic=inicio>=4;
    if(p!=null)p.onProgress(n,s.bytes,new URL(origem).getHost());
    while(true){verificar(c);int l;
     try{l=e.read(b,0,(int)Math.min(b.length,s.bytes-n+(s.bytes==n?1:0)));}catch(SocketTimeoutException ex){verificar(c);if(System.nanoTime()-ultimo>20000000000L)throw new IOException("A conexão ficou sem enviar dados. Tente novamente para retomar.",ex);continue;}
     if(l<0)break;verificar(c);if(l>s.bytes-n)throw new Invalido("A origem excedeu o tamanho permitido.");
     o.write(b,0,l);n+=l;ultimo=System.nanoTime();if(!magic&&n>=4){o.flush();magic(parcial);magic=true;}if(p!=null)p.onProgress(n,s.bytes,new URL(origem).getHost());
    }
    o.getFD().sync();if(n!=s.bytes)throw new IOException("Download incompleto. Tente novamente para retomar.");
   }
  }finally{c.liberar(conexao);conexao.disconnect();}
 }
 private static HttpURLConnection abrir(URL u,long inicio,String etag,Controle c)throws IOException{
  for(int i=0;i<6;i++){
   validarUrl(u);verificar(c);HttpURLConnection h=(HttpURLConnection)u.openConnection();h.setInstanceFollowRedirects(false);h.setConnectTimeout(15000);h.setReadTimeout(2000);h.setUseCaches(false);h.setAllowUserInteraction(false);h.setRequestProperty("Accept-Encoding","identity");
   if(inicio>0){h.setRequestProperty("Range","bytes="+inicio+"-");h.setRequestProperty("If-Range",etag);}c.registrar(h);
   int status;
   try{status=h.getResponseCode();}catch(IOException ex){c.liberar(h);h.disconnect();throw ex;}
   if(status>=300&&status<=399){String proximo=h.getHeaderField("Location");c.liberar(h);h.disconnect();if(proximo==null)throw new IOException("Redirecionamento sem destino.");URL n=new URL(u,proximo);validarUrl(n);if(u.getProtocol().equals("https")&&!n.getProtocol().equals("https"))throw new IOException("Redirecionamento inseguro rejeitado.");u=n;continue;}
   return h;
  }throw new IOException("A origem redirecionou muitas vezes.");
 }
 private static void validar(Spec s)throws IOException{
  if(s==null||s.id==null||!s.id.matches("[A-Za-z0-9._-]{1,100}")||s.id.equals(".")||s.id.equals("..")||s.bytes<4||s.bytes>16L*1024*1024*1024||s.sha256==null||!s.sha256.matches("[A-Fa-f0-9]{64}")||s.urls==null||s.urls.length==0)throw new IOException("Configuração do modelo inválida.");
  for(String u:s.urls)validarUrl(new URL(u));
 }
 private static void validarUrl(URL u)throws IOException{
  boolean local=u.getHost().equals("127.0.0.1")||u.getHost().equals("localhost")||u.getHost().equals("[::1]");
  if(u.getUserInfo()!=null||(!u.getProtocol().equals("https")&&!(u.getProtocol().equals("http")&&local)))throw new IOException("Somente endereços HTTPS sem credenciais são permitidos.");
 }
 private static boolean forte(String e){return e!=null&&e.startsWith("\"")&&e.endsWith("\"")&&e.length()>2;}
 private static File filho(File p,String n)throws IOException{File f=new File(p,n).getCanonicalFile();if(!p.equals(f.getParentFile()))throw new IOException("Caminho de modelo inválido.");return f;}
 private static String conferir(File f,Spec s,Controle c)throws IOException{
  if(f.length()!=s.bytes)throw new Invalido("Tamanho do modelo incorreto.");magic(f);
  try{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream e=new FileInputStream(f)){byte[] b=new byte[262144];int l;while((l=e.read(b))!=-1){verificar(c);md.update(b,0,l);}}StringBuilder hex=new StringBuilder();for(byte x:md.digest())hex.append(String.format(Locale.ROOT,"%02x",x&255));if(!hex.toString().equalsIgnoreCase(s.sha256))throw new Invalido("A integridade do modelo não confere.");return hex.toString();}catch(NoSuchAlgorithmException ex){throw new IOException("SHA-256 indisponível.",ex);}
 }
 private static void magic(File f)throws IOException{try(InputStream e=new FileInputStream(f)){if(e.read()!='G'||e.read()!='G'||e.read()!='U'||e.read()!='F')throw new Invalido("O arquivo não é um modelo GGUF válido.");}}
 private static void remover(File f)throws IOException{if(f.exists()&&!f.delete())throw new IOException("Não foi possível remover um parcial inválido.");}
 private static void limpar(File p,File m)throws IOException{remover(p);remover(m);}
 private static void verificar(Controle c)throws IOException{if(c.isCancelled())throw cancelamento();}
 private static IOException cancelamento(){return new IOException("Download pausado. O parcial foi mantido para retomar com segurança.");}
}
