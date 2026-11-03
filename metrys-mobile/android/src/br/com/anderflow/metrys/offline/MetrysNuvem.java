package br.com.anderflow.metrys.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;

/** Metrys online: token protegido pelo Android Keystore e HTTPS direto ao provedor. */
public final class MetrysNuvem {
 private MetrysNuvem(){}
 private static final String CHAVE = "metrys.hf.aes.v1";
 private static final String PREFS = "metrys_nuvem_privado";
 private static final String MODELO = "moonshotai/Kimi-K3:cheapest";
 private static final String ENDPOINT = "https://router.huggingface.co/v1/chat/completions";

 private static SecretKey chave() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  java.security.Key k=ks.getKey(CHAVE,null);
  if(k instanceof SecretKey)return (SecretKey)k;
  KeyGenerator gerador=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  gerador.init(new KeyGenParameterSpec.Builder(CHAVE,
   KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
   .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
   .setKeySize(256).build());
  return gerador.generateKey();
 }

 public static boolean salvarToken(Context contexto,String valor) {
  if(valor==null||!valor.matches("hf_[a-zA-Z0-9_-]{8,512}"))return false;
  try{
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
   c.init(Cipher.ENCRYPT_MODE,chave());
   byte[] criptografado=c.doFinal(valor.getBytes(StandardCharsets.UTF_8));
   JSONObject j=new JSONObject();
   j.put("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP));
   j.put("dado",Base64.encodeToString(criptografado,Base64.NO_WRAP));
   return contexto.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("token",j.toString()).commit();
  }catch(Exception erro){return false;}
 }

 public static boolean temToken(Context contexto){
  return !contexto.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("token","").isEmpty();
 }

 public static void apagarToken(Context contexto){
  contexto.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove("token").commit();
 }

 private static String lerToken(Context contexto) throws Exception {
  SharedPreferences prefs=contexto.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  String dado=prefs.getString("token","");
  if(dado.isEmpty())throw new IOException("Configure o token do Hugging Face.");
  JSONObject j=new JSONObject(dado);
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
  c.init(Cipher.DECRYPT_MODE,chave(),
   new GCMParameterSpec(128,Base64.decode(j.getString("iv"),Base64.NO_WRAP)));
  return new String(c.doFinal(Base64.decode(j.getString("dado"),Base64.NO_WRAP)),StandardCharsets.UTF_8);
 }

 private static byte[] lerLimite(InputStream in,int limite) throws IOException {
  ByteArrayOutputStream saida=new ByteArrayOutputStream();
  byte[] bloco=new byte[4096];int n;
  while((n=in.read(bloco))!=-1){
   if(saida.size()+n>limite)throw new IOException("Resposta excessivamente longa.");
   saida.write(bloco,0,n);
  }
  return saida.toByteArray();
 }

 public static String perguntar(Context contexto,String requisicao) throws Exception {
  if(requisicao==null||requisicao.length()>52000)throw new IOException("Contexto muito grande.");
  JSONObject origem=new JSONObject(requisicao);
  JSONArray mensagens=origem.optJSONArray("mensagens");
  if(mensagens==null||mensagens.length()<1||mensagens.length()>16)throw new IOException("Historico invalido.");
  JSONArray filtradas=new JSONArray();int total=0;
  for(int i=0;i<mensagens.length();i++){
   JSONObject item=mensagens.getJSONObject(i);
   String papel=item.optString("role",""),texto=item.optString("content","");
   if(!("system".equals(papel)||"user".equals(papel)||"assistant".equals(papel))
    ||texto.isEmpty()||texto.length()>15000)throw new IOException("Mensagem invalida.");
   total+=texto.length();
   filtradas.put(new JSONObject().put("role",papel).put("content",texto));
  }
  if(total>42000)throw new IOException("Contexto excede o limite.");
  JSONObject corpo=new JSONObject();
  corpo.put("model",MODELO);corpo.put("messages",filtradas);
  corpo.put("max_tokens",Math.min(512,Math.max(1,origem.optInt("max_tokens",384))));
  byte[] dados=corpo.toString().getBytes(StandardCharsets.UTF_8);

  String token=lerToken(contexto);
  HttpsURLConnection conexao=(HttpsURLConnection)new URL(ENDPOINT).openConnection();
  try{
   conexao.setRequestMethod("POST");
   conexao.setConnectTimeout(15000);conexao.setReadTimeout(90000);
   conexao.setInstanceFollowRedirects(false);
   conexao.setRequestProperty("Content-Type","application/json");
   conexao.setRequestProperty("Accept","application/json");
   conexao.setRequestProperty("Authorization","Bearer "+token);
   conexao.setDoOutput(true);
   conexao.setFixedLengthStreamingMode(dados.length);
   try(OutputStream out=conexao.getOutputStream()){out.write(dados);}
   int codigo=conexao.getResponseCode();
   if(codigo!=200){
    if(codigo==401||codigo==403)throw new IOException("Token invalido ou sem permissao.");
    if(codigo==402)throw new IOException("Creditos de inferencia insuficientes.");
    if(codigo==429)throw new IOException("Limite de requisicoes atingido.");
    throw new IOException("O provedor respondeu com HTTP "+codigo+".");
   }
   try(InputStream in=conexao.getInputStream()){
    JSONObject resposta=new JSONObject(new String(lerLimite(in,2000000),StandardCharsets.UTF_8));
    JSONObject mensagem=resposta.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
    Object conteudo=mensagem.opt("content");
    if(conteudo instanceof String)return ((String)conteudo).trim();
    if(conteudo instanceof JSONArray){
     JSONArray blocos=(JSONArray)conteudo;StringBuilder saida=new StringBuilder();
     for(int i=0;i<blocos.length();i++){
      JSONObject bloco=blocos.optJSONObject(i);
      if(bloco!=null)saida.append(bloco.optString("text",""));
     }
     return saida.toString().trim();
    }
    throw new IOException("O modelo retornou uma resposta nao textual.");
   }
  }finally{conexao.disconnect();}
 }
}
