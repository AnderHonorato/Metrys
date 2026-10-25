package br.com.anderflow.metrys.offline;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/** Operações de texto restritas a URIs selecionadas pelo usuário via SAF. */
public final class EditorArquivos {
 private static final int LIMITE=256*1024;
 private final Context contexto;
 public static final class Documento {public final String conteudo,sha256;Documento(String c,String h){conteudo=c;sha256=h;}}
 public EditorArquivos(Context app){contexto=app.getApplicationContext();}
 private static String sha(byte[] dados)throws Exception{MessageDigest hash=MessageDigest.getInstance("SHA-256");hash.update(dados);StringBuilder saida=new StringBuilder();for(byte b:hash.digest())saida.append(String.format(Locale.ROOT,"%02x",b&255));return saida.toString();}
 public Documento ler(Uri uri)throws Exception{if(uri==null||!"content".equals(uri.getScheme()))throw new SecurityException("Escolha um arquivo pelo seletor Android.");try(InputStream entrada=contexto.getContentResolver().openInputStream(uri)){
  if(entrada==null)throw new IOException("Não foi possível abrir o documento.");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bloco=new byte[8192];int l;while((l=entrada.read(bloco))!=-1){if(out.size()+l>LIMITE)throw new IOException("Arquivos de texto limitados a 256 KB.");out.write(bloco,0,l);}byte[] dados=out.toByteArray();for(byte b:dados)if(b==0)throw new IOException("Arquivo binário não pode ser editado como texto.");return new Documento(new String(dados,StandardCharsets.UTF_8),sha(dados));
 }}
 /** A gravação no provedor SAF pode não ser atômica. Preserva revisão local recuperável. */
 public File salvar(Uri destino,String texto,String hashEsperado,boolean confirmado)throws Exception{
  new PermissoesMetrys(contexto).exigirEscrita(confirmado);
  if(destino==null||!"content".equals(destino.getScheme()))throw new SecurityException("O arquivo deve vir do seletor Android.");if(texto==null)throw new IOException("Conteúdo inválido.");byte[] novo=texto.getBytes(StandardCharsets.UTF_8);if(novo.length>LIMITE)throw new IOException("Conteúdo excede 256 KB.");
  Documento antigo=null;if(hashEsperado!=null){antigo=ler(destino);if(!antigo.sha256.equals(hashEsperado))throw new IOException("O arquivo mudou desde a abertura. Reabra para evitar sobrescrever uma edição recente.");}
  File revisao=null;if(antigo!=null){File pasta=new File(contexto.getFilesDir(),"revisoes");if(!pasta.isDirectory()&&!pasta.mkdirs())throw new IOException("Falha ao preparar backup do arquivo.");revisao=new File(pasta,UUID.randomUUID().toString()+".txt");try(FileOutputStream out=new FileOutputStream(revisao)){out.write(antigo.conteudo.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}}
  try(OutputStream out=contexto.getContentResolver().openOutputStream(destino,"wt")){if(out==null)throw new IOException("O provedor de arquivos negou a gravação.");out.write(novo);out.flush();}
  catch(IOException erro){throw new IOException("Falha na gravação. Uma revisão do conteúdo anterior foi preservada na área privada do aplicativo, se disponível.",erro);}
  return revisao;
 }
}
