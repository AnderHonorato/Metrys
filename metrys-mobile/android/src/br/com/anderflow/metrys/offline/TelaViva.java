package br.com.anderflow.metrys.offline;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Metrys Ateliê: interface editorial original integrada ao motor offline existente. */
public final class TelaViva extends Activity implements ServicoOffline.Ouvidor {
 private static final int SELECIONAR_ARQUIVO=712, SALVAR_ARQUIVO=713;
 private final Handler principal=new Handler(Looper.getMainLooper());
 private final ExecutorService tarefas=Executors.newSingleThreadExecutor();
 private volatile ServicoOffline servico;
 private volatile boolean paginaPronta=false,emPrimeiroPlano=false;
 private volatile EstadoOffline aguardando;
 private WebView pagina;
 private String salvarPendente="";

 @Override public void onCreate(Bundle estado){super.onCreate(estado);
  Window janela=getWindow();janela.setStatusBarColor(0xfff5f1e8);janela.setNavigationBarColor(0xfff5f1e8);janela.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  janela.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  pagina=new WebView(this);pagina.setBackgroundColor(0xfff5f1e8);pagina.setLayerType(View.LAYER_TYPE_HARDWARE,null);
  WebSettings w=pagina.getSettings();w.setJavaScriptEnabled(true);w.setDomStorageEnabled(true);
  w.setAllowFileAccess(true);w.setAllowContentAccess(false);w.setAllowFileAccessFromFileURLs(false);w.setAllowUniversalAccessFromFileURLs(false);
  w.setJavaScriptCanOpenWindowsAutomatically(false);w.setSupportMultipleWindows(false);w.setLoadsImagesAutomatically(true);
  w.setBuiltInZoomControls(false);w.setDisplayZoomControls(false);w.setTextZoom(100);
  if(android.os.Build.VERSION.SDK_INT>=26)w.setSafeBrowsingEnabled(true);
  pagina.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){paginaPronta=true;EstadoOffline s=aguardando;if(s!=null)enviarEstado(s);else solicitarEstado();}
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){
    Uri url=req.getUrl();if("file".equals(url.getScheme())&&url.toString().startsWith("file:///android_asset/viva/"))return false;
    if("https".equals(url.getScheme())){try{startActivity(new Intent(Intent.ACTION_VIEW,url));}catch(Exception ignore){} }
    return true;
   }
  });
  pagina.setWebChromeClient(new WebChromeClient(){@Override public boolean onConsoleMessage(android.webkit.ConsoleMessage m){if(m.messageLevel()==android.webkit.ConsoleMessage.MessageLevel.ERROR)android.util.Log.w("MetrysViva",m.message());return true;}});
  pagina.addJavascriptInterface(new PonteAndroid(),"Metrys");
  setContentView(pagina);
  pagina.loadUrl("file:///android_asset/viva/index.html");
  tarefas.execute(()->{try{ServicoOffline s=ServicoOffline.obter(getApplicationContext());servico=s;if(emPrimeiroPlano)s.observar(this);else aguardando=s.estado();}catch(Throwable erro){informarErro(erro);}});
 }
 @Override public void onStart(){super.onStart();emPrimeiroPlano=true;ServicoOffline s=servico;if(s!=null)tarefas.execute(()->{try{s.observar(this);}catch(Throwable erro){informarErro(erro);}});}
 @Override public void onStop(){emPrimeiroPlano=false;ServicoOffline s=servico;if(s!=null)tarefas.execute(()->s.remover(this));super.onStop();}
 @Override public void onDestroy(){paginaPronta=false;emPrimeiroPlano=false;if(pagina!=null){pagina.removeJavascriptInterface("Metrys");pagina.stopLoading();pagina.destroy();pagina=null;}tarefas.shutdownNow();super.onDestroy();}
 @Override public void mudou(EstadoOffline novo){aguardando=novo;enviarEstado(novo);}
 private void enviarEstado(EstadoOffline s){if(!paginaPronta||pagina==null)return;try{JSONObject dado=new JSONObject();
  dado.put("fase",s.fase);dado.put("detalhe",s.detalhe);dado.put("gerando",s.gerando);dado.put("modeloPronto",s.modeloPronto);dado.put("nomeModelo",s.nomeModelo);dado.put("conversaId",s.conversaId);
  dado.put("filaPendente",s.filaPendente);dado.put("tempoGeracaoMs",s.tempoGeracaoMs);dado.put("raciocinioPublicado",s.raciocinioPublicado);dado.put("raciocinando",s.raciocinando);dado.put("trechosGerados",s.trechosGerados);
  dado.put("bytes",s.bytes);dado.put("total",s.total);
  JSONArray conversas=new JSONArray();for(EstadoOffline.Conversa c:s.conversas){JSONObject j=new JSONObject();j.put("id",c.id);j.put("titulo",c.titulo);j.put("projetoId",c.projetoId);conversas.put(j);}dado.put("conversas",conversas);
  JSONArray projetos=new JSONArray();for(EstadoOffline.Projeto p:s.projetos){JSONObject j=new JSONObject();j.put("id",p.id);j.put("nome",p.nome);j.put("arquivado",p.arquivado);projetos.put(j);}dado.put("projetos",projetos);
  JSONArray mensagens=new JSONArray();for(EstadoOffline.Mensagem m:s.mensagens){JSONObject j=new JSONObject();j.put("papel",m.papel);j.put("texto",m.texto);mensagens.put(j);}dado.put("mensagens",mensagens);
  JSONArray etapas=new JSONArray();for(String etapa:s.etapas)etapas.put(etapa);dado.put("etapas",etapas);
  final String javascript="window.receberEstado("+dado.toString()+")";
  principal.post(()->{if(paginaPronta&&pagina!=null)pagina.evaluateJavascript(javascript,null);});
 }catch(Exception erro){informarErro(erro);}}
 private void solicitarEstado(){tarefas.execute(()->{try{ServicoOffline s=servico;if(s!=null)enviarEstado(s.estado());}catch(Throwable erro){informarErro(erro);}});}
 private void informarErro(Throwable erro){android.util.Log.e("MetrysViva","Erro de interface",erro);final String aviso="Falha: "+(erro.getMessage()==null?erro.getClass().getSimpleName():erro.getMessage());principal.post(()->{
  if(paginaPronta&&pagina!=null)pagina.evaluateJavascript("window.receberEstado({fase:'erro',detalhe:"+JSONObject.quote(aviso)+"})",null);
 });}
 private void executar(Operacao acao){tarefas.execute(()->{try{ServicoOffline s=servico;if(s==null){principal.post(()->Toast.makeText(this,"Preparando o Metrys…",Toast.LENGTH_SHORT).show());return;}acao.executar(s);}catch(Throwable erro){informarErro(erro);}});}
 private interface Operacao{void executar(ServicoOffline s);}
 private void enviarAnexo(String nome,String texto,String erro){final String js="window.receberAnexo("+JSONObject.quote(nome)+","+JSONObject.quote(texto)+","+JSONObject.quote(erro)+")";principal.post(()->{if(pagina!=null&&paginaPronta)pagina.evaluateJavascript(js,null);});}
 @Override protected void onActivityResult(int req,int res,Intent dados){super.onActivityResult(req,res,dados);if(res!=RESULT_OK||dados==null||dados.getData()==null)return;Uri uri=dados.getData();
  if(req==SELECIONAR_ARQUIVO){tarefas.execute(()->{
   String nome="Documento.txt";try{try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())nome=c.getString(0);}
    String tipo=getContentResolver().getType(uri);if(tipo!=null&&(tipo.startsWith("image/")||tipo.startsWith("video/")||tipo.contains("pdf")||tipo.contains("zip"))){enviarAnexo(nome,"","Este formato exige um modelo ou visualizador compatível. Selecione um arquivo de texto para anexar ao chat.");return;}
    try(InputStream stream=getContentResolver().openInputStream(uri)){if(stream==null)throw new java.io.IOException("Arquivo não disponível.");ByteArrayOutputStream saida=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int l;while((l=stream.read(buffer))!=-1){if(saida.size()+l>1024*1024)throw new java.io.IOException("Limite de 1 MB por anexo de texto.");saida.write(buffer,0,l);}enviarAnexo(nome,new String(saida.toByteArray(),StandardCharsets.UTF_8),"");}
   }catch(Exception erro){enviarAnexo(nome,"",erro.getMessage()==null?"Falha ao abrir arquivo.":erro.getMessage());}
  });}
  else if(req==SALVAR_ARQUIVO){final String conteudo=salvarPendente;salvarPendente="";tarefas.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("Destino inacessível");out.write(conteudo.getBytes(StandardCharsets.UTF_8));principal.post(()->Toast.makeText(this,"Arquivo salvo",Toast.LENGTH_SHORT).show());}catch(Exception erro){informarErro(erro);}});}
 }
 @Override public void onBackPressed(){if(paginaPronta&&pagina!=null){pagina.evaluateJavascript("(function(){if(!document.getElementById('sobreposicao').hidden){document.getElementById('fecharModal').click();return true;}if(document.body.classList.contains('lateral-aberta')){document.getElementById('fecharLateral').click();return true;}return false;})()",resultado->{if(!"true".equals(resultado))TelaViva.super.onBackPressed();});}else super.onBackPressed();}

 public final class PonteAndroid {
  @JavascriptInterface public void solicitarEstado(){TelaViva.this.solicitarEstado();}
  @JavascriptInterface public void enviar(String texto){if(texto==null||texto.trim().isEmpty())return;final String msg=texto.length()>500000?texto.substring(0,500000):texto;executar(s->s.enviar(msg));}
  @JavascriptInterface public void novaConversa(){executar(ServicoOffline::novaConversa);}
  @JavascriptInterface public void selecionarConversa(String id){if(id!=null&&id.length()<200)executar(s->s.selecionarConversa(id));}
  @JavascriptInterface public void parar(){executar(ServicoOffline::cancelarGeracao);}
  @JavascriptInterface public void refazerResposta(){executar(ServicoOffline::refazerResposta);}
  @JavascriptInterface public void avaliar(String id,String texto,int nota){if(nota==1||nota==-1)executar(s->s.avaliarResposta(id,texto,nota));}
  @JavascriptInterface public void baixarModelo(){executar(ServicoOffline::baixarModelo);}
  @JavascriptInterface public void selecionarModelo(String id){if(id!=null&&id.length()<160)executar(s->s.selecionarModelo(id));}
  @JavascriptInterface public String modelos(){JSONArray a=new JSONArray();ServicoOffline s=servico;if(s!=null)for(JSONObject m:s.modelos())a.put(m);return a.toString();}
  @JavascriptInterface public String lerPreferencias(){return getSharedPreferences("metrys_viva",MODE_PRIVATE).getString("visual","");}
  @JavascriptInterface public void salvarPreferencias(String json){if(json==null||json.length()>5000)return;try{new JSONObject(json);getSharedPreferences("metrys_viva",MODE_PRIVATE).edit().putString("visual",json).apply();}catch(Exception ignore){}}
  @JavascriptInterface public void abrirClassico(){principal.post(()->{try{startActivity(new Intent(TelaViva.this,TelaOffline.class));}catch(Exception erro){informarErro(erro);}});}
  @JavascriptInterface public void abrirRecuperacao(){principal.post(()->startActivity(new Intent(TelaViva.this,TelaEstavel.class)));}
  @JavascriptInterface public void abrirArquivo(){principal.post(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,SELECIONAR_ARQUIVO);}catch(Exception ex){enviarAnexo("","","Não foi possível abrir o seletor de arquivos.");}});}
  @JavascriptInterface public void salvarTexto(String texto,String nome){if(texto==null||texto.length()>1000000)return;principal.post(()->{salvarPendente=texto;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/plain");i.putExtra(Intent.EXTRA_TITLE,(nome==null?"codigo.txt":nome.replaceAll("[^a-zA-Z0-9._-]","_").substring(0,Math.min(80,nome.length()))));try{startActivityForResult(i,SALVAR_ARQUIVO);}catch(Exception ex){informarErro(ex);}});}
  @JavascriptInterface public void copiar(String texto){if(texto==null)return;principal.post(()->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("Metrys",texto));Toast.makeText(TelaViva.this,"Copiado",Toast.LENGTH_SHORT).show();});}
  @JavascriptInterface public void compartilhar(String texto){if(texto==null)return;principal.post(()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,texto);try{startActivity(Intent.createChooser(i,"Compartilhar resposta"));}catch(Exception ex){informarErro(ex);}});}
 }
}
