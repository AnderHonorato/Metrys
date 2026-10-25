package br.com.anderflow.metrys.offline;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.speech.RecognizerIntent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import android.os.*;
import android.text.*;
import android.text.style.StyleSpan;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import android.widget.*;
import java.util.*;

/** Conversas nativas; inferência, download e persistência ficam no serviço local. */
public final class TelaOffline extends Activity implements ServicoOffline.Ouvidor {
 private int BG=0xff111018,SUPERFICIE=0xff24202F,BORDA=0xff373042;
 private int TEXTO=0xffF8F5FC,SECUNDARIO=0xffBEB5C9,LAVANDA=0xffB59AFF,MENTA=0xff91DCC0,CORAL=0xffF3A38E;
 private static final int IMPORTAR=81,EXPORTAR=82,PREVER=83,DITADO=84,CRIAR_ARQUIVO=85,EDITAR_ARQUIVO=86,EXPORTAR_CODIGO=87,EXPORTAR_ZIP=88;
 private final Handler main=new Handler(Looper.getMainLooper());
 private EditorVisualMetrys editorVisual;private int escalaTexto=100,raioExtra=0,espacamento=0;private boolean compacto=true;
 private ServicoOffline servico;private EstadoOffline estado,pendente;
 private Typeface corpo,titulo;private LinearLayout raiz,painelModelo,conteudo,rodape;
 private ScrollView scroll;private EditText entrada;private ImageButton enviar;private View gavetaOverlay;
 private LinearLayout andamento;private TextView textoAndamento,etapasAndamento,iconeConclusao;private ProgressBar esperaAndamento;private String ultimaEtapa="";private boolean detalhesAbertos=true;
 private TextView situacao,legenda;private FrameLayout camada;private FitaView marca,marcaSplash;
 private View splash;private Dialog dialogo;
 private boolean observando,atualizarAgendado,forcarFundo,boasVindas;
 private String conversaAtual="",conteudoPendenteArquivo="";
 private LinearLayout anexoRascunho;private String textoAnexado="";private boolean convertendoAnexo=false;
 private String codigoPendente="",nomeCodigoPendente="codigo.txt";
 private final ArrayList<MensagemRender> renderizadas=new ArrayList<>();
 private final Runnable atualizar=()->{atualizarAgendado=false;if(observando&&pendente!=null)desenhar(pendente);};
 private final Runnable fimSplash=()->removerSplash();

 @Override public void onCreate(Bundle salvo){
  super.onCreate(salvo);detalhesAbertos=getSharedPreferences("estudio_visual",MODE_PRIVATE).getBoolean("etapas_abertas",false);aplicarConfiguracaoVisual();Window w=getWindow();w.setStatusBarColor(BG);w.setNavigationBarColor(BG);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  if(Build.VERSION.SDK_INT>=30)w.setDecorFitsSystemWindows(false);
  w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  atualizarFontes();
  servico=ServicoOffline.obter(getApplicationContext());construir();desenhar(servico.estado());if(salvo!=null)entrada.setText(salvo.getString("rascunho",entrada.getText().toString()));
  if(salvo==null&&ValueAnimator.areAnimatorsEnabled())mostrarSplash();
 }
 private void atualizarFontes(){int escolha=getSharedPreferences("estudio_visual",MODE_PRIVATE).getInt("fonte",0);if(escolha==1){corpo=Typeface.create("sans-serif",Typeface.NORMAL);titulo=Typeface.create("sans-serif-medium",Typeface.NORMAL);}else if(escolha==2){corpo=Typeface.create("serif",Typeface.NORMAL);titulo=Typeface.create("serif",Typeface.NORMAL);}else{corpo=fonte("manrope",Typeface.create("sans-serif",Typeface.NORMAL));titulo=fonte("lora",Typeface.create("serif",Typeface.NORMAL));}}
 private Typeface fonte(String n,Typeface padrao){try{return new Typeface.Builder(getAssets(),"fonts/"+n+".ttf").setWeight(400).setFontVariationSettings("'wght' 400").build();}catch(Exception e){return padrao;}}
 @Override protected void onStart(){super.onStart();observando=true;servico.observar(this);desenhar(servico.estado());}
 @Override protected void onStop(){
  observando=false;servico.remover(this);main.removeCallbacks(atualizar);atualizarAgendado=false;
  if(marca!=null)marca.animar(false);if(marcaSplash!=null)marcaSplash.animar(false);main.removeCallbacks(fimSplash);if(splash!=null)removerSplash();fecharDialogo();fecharGaveta();super.onStop();
 }
 @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);super.onDestroy();}
 @Override protected void onSaveInstanceState(Bundle b){b.putString("rascunho",entrada.getText().toString());super.onSaveInstanceState(b);}
 @Override public void mudou(EstadoOffline novo){main.post(()->{if(!observando)return;pendente=novo;if(!atualizarAgendado){atualizarAgendado=true;main.postDelayed(atualizar,100);}});}
 private void aplicarConfiguracaoVisual(){
  SharedPreferences sp=getSharedPreferences("estudio_visual",MODE_PRIVATE);
  BG=sp.getInt("fundo",0xff111018);SUPERFICIE=sp.getInt("superficie",0xff24202F);BORDA=sp.getInt("borda",0xff373042);LAVANDA=sp.getInt("destaque",0xffB59AFF);TEXTO=sp.getInt("texto",0xffF8F5FC);SECUNDARIO=sp.getInt("secundario",0xffBEB5C9);MENTA=sp.getInt("positivo",0xff91DCC0);
  escalaTexto=sp.getInt("escala",100);raioExtra=sp.getInt("raio",0);espacamento=sp.getInt("espacamento",0);compacto=sp.getBoolean("compacto",true);
 }
 public void aplicarMudancaVisual(){
  String rascunho=entrada==null?"":entrada.getText().toString();int posicao=scroll==null?0:scroll.getScrollY();
  aplicarConfiguracaoVisual();atualizarFontes();getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  if(splash!=null)removerSplash();if(marca!=null)marca.animar(false);
  construir();if(servico!=null)desenhar(servico.estado());entrada.setText(rascunho);entrada.setSelection(entrada.length());scroll.post(()->scroll.scrollTo(0,posicao));
 }
 private String rotulo(String id,String padrao){return getSharedPreferences("estudio_textos",MODE_PRIVATE).getString(id,padrao);}
 private void editarRotulo(TextView tv,String original){
  if(original==null||original.isEmpty()||original.length()>100||original.startsWith("{")||original.matches(".*\\d{3,}.*"))return;
  final String id=Integer.toHexString(original.hashCode());
  if(getSharedPreferences("estudio_visual",MODE_PRIVATE).getBoolean("editar_textos",false)){
   tv.setOnLongClickListener(v->{final EditText campo=new EditText(this);campo.setText(tv.getText());campo.selectAll();
    new android.app.AlertDialog.Builder(this).setTitle("Editar texto da interface").setView(campo).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{
     String valor=campo.getText().toString();getSharedPreferences("estudio_textos",MODE_PRIVATE).edit().putString(id,valor).apply();tv.setText(valor);
    }).show();return true;});
  }
 }
 private int d(float n){return Math.round(n*getResources().getDisplayMetrics().density);}

 private LinearLayout coluna(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private LinearLayout linha(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private GradientDrawable fundo(int cor,int borda,int raio){GradientDrawable g=new GradientDrawable();g.setColor(cor);g.setCornerRadius(d(Math.max(4,raio+raioExtra)));if(borda!=0)g.setStroke(d(1),borda);return g;}
 private TextView texto(String s,float tamanho,int cor,boolean serif){TextView t=new TextView(this);String origem=s==null?"":s;String chave=Integer.toHexString(origem.hashCode());String exibido=origem.length()<=100?getSharedPreferences("estudio_textos",MODE_PRIVATE).getString(chave,origem):origem;t.setText(exibido);t.setTextSize(tamanho*(escalaTexto/100f));t.setTextColor(cor);t.setTypeface(serif?titulo:corpo);t.setIncludeFontPadding(false);t.setLineSpacing(d(3+espacamento),1f);editarRotulo(t,s);return t;}
 private ImageButton botao(String tipo,String nome,boolean principal,View.OnClickListener clique){
  ImageButton b=new ImageButton(this);b.setImageDrawable(new IconeAutoral(tipo,principal?BG:LAVANDA));b.setScaleType(ImageView.ScaleType.FIT_CENTER);b.setPadding(d(13),d(13),d(13),d(13));
  b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33D4B9EE),fundo(principal?LAVANDA:SUPERFICIE,principal?0:BORDA,18),fundo(Color.WHITE,0,18)));
  b.setContentDescription(nome);b.setTooltipText(nome);b.setOnClickListener(clique);b.setLayoutParams(new LinearLayout.LayoutParams(d(48),d(48)));return b;
 }
 private void espaco(LinearLayout l,int n){l.addView(new View(this),new LinearLayout.LayoutParams(1,d(n)));}
 private void flex(LinearLayout l){l.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));}
 private void margem(LinearLayout l,View v,int n){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(d(48),d(48));p.leftMargin=d(n);l.addView(v,p);}
 private void construir(){
  fecharGaveta();andamento=null;textoAndamento=null;etapasAndamento=null;esperaAndamento=null;iconeConclusao=null;renderizadas.clear();boasVindas=false;conversaAtual="";
  camada=new FrameLayout(this);camada.setBackgroundColor(BG);raiz=coluna();camada.addView(raiz,new FrameLayout.LayoutParams(-1,-1));setContentView(camada);
  raiz.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(x.left,x.top,x.right,x.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});raiz.requestApplyInsets();

  // Cabeçalho compacto e funcional: navegar e iniciar uma conversa sem duas barras de ícones.
  LinearLayout cab=linha();cab.setPadding(d(12),d(7),d(12),d(7));cab.setGravity(Gravity.CENTER_VERTICAL);
  ImageButton abrirMenu=botaoCompacto("menu","Abrir conversas, projetos e busca",()->menuNavegacao());cab.addView(abrirMenu);
  marca=new FitaView(this,LAVANDA);LinearLayout.LayoutParams logo=new LinearLayout.LayoutParams(d(29),d(29));logo.leftMargin=d(9);cab.addView(marca,logo);
  LinearLayout identificacao=coluna();identificacao.setPadding(d(10),0,0,0);
  TextView nome=texto(rotulo("cabeçalho","Metrys"),getSharedPreferences("estudio_visual",MODE_PRIVATE).getInt("cabecalho",19),TEXTO,false);nome.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));identificacao.addView(nome);
  LinearLayout estadoLinha=linha();View ponto=new View(this);ponto.setBackground(fundo(MENTA,0,4));LinearLayout.LayoutParams pd=new LinearLayout.LayoutParams(d(6),d(6));pd.rightMargin=d(6);estadoLinha.addView(ponto,pd);
  situacao=texto("v0.6.10",10,SECUNDARIO,false);estadoLinha.addView(situacao);identificacao.addView(estadoLinha);
  cab.addView(identificacao,new LinearLayout.LayoutParams(0,-2,1));
  cab.addView(botaoCompacto("model","Escolher modelo e configurações",()->ajustes()));
  LinearLayout.LayoutParams nv=new LinearLayout.LayoutParams(d(40),d(40));nv.leftMargin=d(4);cab.addView(botaoCompacto("edit","Nova conversa",()->{servico.novaConversa();forcarFundo=true;}),nv);
  raiz.addView(cab,new LinearLayout.LayoutParams(-1,d(60)));

  painelModelo=coluna();LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.setMargins(d(16),d(7),d(16),0);raiz.addView(painelModelo,mp);
  scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);conteudo=coluna();conteudo.setPadding(d(17),d(8),d(17),d(14));scroll.addView(conteudo,new ScrollView.LayoutParams(-1,-2));raiz.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

  // Um compositor, sem a fileira de sete botões da tela antiga.
  rodape=coluna();rodape.setPadding(d(13),d(compacto?4:12),d(13),d(compacto?9:18));LinearLayout compositor=linha();compositor.setPadding(d(9),d(5),d(7),d(5));compositor.setBackground(fundo(SUPERFICIE,BORDA,23));
  compositor.addView(botaoCompacto("plus","Anexar arquivo, ditar ou ampliar",()->menuAnexos()));
  entrada=new EditText(this);entrada.setTextColor(TEXTO);entrada.setHintTextColor(SECUNDARIO);entrada.setHint(rotulo("entrada","Mensagem para o Metrys…"));entrada.setTextSize(15*(escalaTexto/100f));entrada.setTypeface(corpo);entrada.setBackgroundColor(Color.TRANSPARENT);entrada.setPadding(d(7),d(11),d(4),d(11));entrada.setMinLines(1);entrada.setMaxLines(getSharedPreferences("estudio_visual",MODE_PRIVATE).getInt("linhas",5));
  entrada.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);entrada.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);entrada.setContentDescription("Mensagem para o Metrys");compositor.addView(entrada,new LinearLayout.LayoutParams(0,-2,1));
  compositor.addView(botaoCompacto("mic","Ditar mensagem",()->ditar()));
  enviar=botao("send","Enviar mensagem",true,v->enviarMensagem());enviar.setPadding(d(10),d(10),d(10),d(10));compositor.addView(enviar,new LinearLayout.LayoutParams(d(42),d(42)));
  entrada.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int z){}public void onTextChanged(CharSequence s,int a,int b,int c){if(!convertendoAnexo&&s.length()>1300){textoAnexado=textoAnexado.isEmpty()?s.toString():textoAnexado+"\n\n"+s;convertendoAnexo=true;entrada.setText("");convertendoAnexo=false;atualizarChipAnexo();}
  if(!conversaAtual.isEmpty())getSharedPreferences("rascunhos",MODE_PRIVATE).edit().putString(conversaAtual,entrada.getText().toString()).apply();atualizarEnvio();}public void afterTextChanged(Editable e){}});
  anexoRascunho=coluna();rodape.addView(anexoRascunho,new LinearLayout.LayoutParams(-1,-2));
  rodape.addView(compositor,new LinearLayout.LayoutParams(-1,-2));
  // Editor, histórico e ferramentas ficam no menu; status discreto, não mais uma barra cheia de botões.
  LinearLayout metadados=linha();metadados.setPadding(d(5),d(7),d(5),0);
  TextView aviso=texto("",10,SECUNDARIO,false);metadados.addView(aviso,new LinearLayout.LayoutParams(0,-2,1));
  TextView est=texto("✦ Personalizar",10,LAVANDA,false);est.setPadding(d(8),d(4),d(4),d(4));est.setContentDescription("Abrir estúdio visual");est.setOnClickListener(v->new EditorVisualMetrys(this,this::aplicarMudancaVisual).abrir());metadados.addView(est);
  rodape.addView(metadados);
  legenda=texto("",10,SECUNDARIO,false);legenda.setVisibility(View.GONE);rodape.addView(legenda);
  raiz.addView(rodape,new LinearLayout.LayoutParams(-1,-2));
 }
 private void atualizarChipAnexo(){atualizarEnvio();if(!conversaAtual.isEmpty())getSharedPreferences("rascunhos",MODE_PRIVATE).edit().putString("anexo_"+conversaAtual,textoAnexado).apply();if(anexoRascunho==null)return;anexoRascunho.removeAllViews();if(textoAnexado.isEmpty())return;
  LinearLayout faixa=linha();faixa.setPadding(d(12),d(7),d(8),d(7));faixa.setBackground(fundo(SUPERFICIE,BORDA,12));
  TextView titulo=texto("Texto anexado · "+textoAnexado.length()+" caracteres  |  Abrir e editar",12,LAVANDA,false);faixa.addView(titulo,new LinearLayout.LayoutParams(0,-2,1));titulo.setOnClickListener(v->editarAnexo());
  faixa.addView(botaoCompacto("close","Remover anexo de texto",()->{textoAnexado="";atualizarChipAnexo();}));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=d(6);anexoRascunho.addView(faixa,lp);
 }
 private void editarAnexo(){final EditText campo=new EditText(this);campo.setText(textoAnexado);campo.setTextSize(13);campo.setTextColor(TEXTO);campo.setGravity(Gravity.TOP);campo.setInputType(0x20001);campo.setMinLines(10);
  ScrollView sc=new ScrollView(this);sc.setPadding(d(12),d(8),d(12),d(8));sc.addView(campo);
  new android.app.AlertDialog.Builder(this).setTitle("Editar arquivo de texto").setView(sc).setNegativeButton("Cancelar",null).setPositiveButton("Salvar alterações",(a,b)->{textoAnexado=campo.getText().toString();atualizarChipAnexo();}).show();}
 private void compartilharMensagem(String conteudo){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,conteudo);try{startActivity(Intent.createChooser(i,"Compartilhar mensagem"));}catch(Exception e){Toast.makeText(this,"Nenhum aplicativo para compartilhar.",Toast.LENGTH_SHORT).show();}}
 private ImageButton acaoIcone(String tipo,String nome,Runnable acao){ImageButton b=botaoCompacto(tipo,nome,acao);b.setPadding(d(8),d(8),d(8),d(8));return b;}
 private ImageButton botaoCompacto(String tipo,String descricao,Runnable acao){
  ImageButton b=new ImageButton(this);b.setImageDrawable(new IconeAutoral(tipo,LAVANDA));b.setScaleType(ImageView.ScaleType.FIT_CENTER);b.setPadding(d(10),d(10),d(10),d(10));
  b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x35B59AFF),fundo(Color.TRANSPARENT,0,13),fundo(Color.WHITE,0,13)));
  b.setContentDescription(descricao);b.setTooltipText(descricao);b.setOnClickListener(v->acao.run());b.setLayoutParams(new LinearLayout.LayoutParams(d(40),d(40)));return b;
 }
 private boolean ocupado(EstadoOffline e){return !e.gerando&&("baixando".equals(e.fase)||"verificando".equals(e.fase)||"carregando".equals(e.fase));}
 private void enviarMensagem(){
  String s=entrada.getText().toString().trim();if(!textoAnexado.isEmpty())s=(textoAnexado+"\n"+s).trim();if(s.isEmpty())return;
  if(estado==null||!estado.modeloPronto||ocupado(estado)){ajustes();return;}
  if(servico.enviar(s)){entrada.setText("");textoAnexado="";atualizarChipAnexo();forcarFundo=true;}else Toast.makeText(this,"A fila está cheia. Aguarde ou interrompa a resposta.",Toast.LENGTH_LONG).show();
 }
 private void atualizarEnvio(){if(enviar==null)return;boolean gerando=estado!=null&&estado.gerando;enviar.setImageDrawable(new IconeAutoral("send",BG));String acao=gerando?"Adicionar mensagem à fila":"Enviar mensagem";enviar.setContentDescription(acao);enviar.setTooltipText(acao);boolean ativo=(estado!=null&&!ocupado(estado)&&(!textoAnexado.isEmpty()||entrada.getText().toString().trim().length()>0));enviar.setEnabled(ativo);enviar.setAlpha(ativo?1f:.4f);}
 private void desenhar(EstadoOffline e){
  if(e==null)return;estado=e;boolean pronto=e.modeloPronto&&"pronto".equals(e.fase),erro="erro".equals(e.fase);
  situacao.setText("v0.6.10"+(e.gerando?" · Gerando":erro?" · Falha":""));situacao.setTextColor(erro?CORAL:SECUNDARIO);situacao.setContentDescription("Estado do modelo: "+descricao(e.fase));situacao.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);marca.animar(e.gerando&&observando&&ValueAnimator.areAnimatorsEnabled());renderModelo(painelModelo,e,false);
  boolean mudou=!igual(conversaAtual,e.conversaId),perto=conteudo.getHeight()-scroll.getHeight()-scroll.getScrollY()<d(110);conversaAtual=e.conversaId;if(mudou){String rascunho=getSharedPreferences("rascunhos",MODE_PRIVATE).getString(e.conversaId,"");textoAnexado=getSharedPreferences("rascunhos",MODE_PRIVATE).getString("anexo_"+e.conversaId,"");entrada.setText(rascunho);entrada.setSelection(entrada.length());atualizarChipAnexo();}
  List<EstadoOffline.Mensagem> ms=e.mensagens;int quantidade=ms==null?0:ms.size();if(mudou){conteudo.removeAllViews();renderizadas.clear();boasVindas=false;andamento=null;textoAndamento=null;etapasAndamento=null;esperaAndamento=null;iconeConclusao=null;}
  if(quantidade==0){if(!boasVindas){boasVindas();boasVindas=true;renderizadas.clear();}}
  else{if(boasVindas||renderizadas.size()>quantidade){conteudo.removeAllViews();renderizadas.clear();boasVindas=false;}for(int j=0;j<quantidade;j++){EstadoOffline.Mensagem m=ms.get(j);if(j<renderizadas.size()&&!igual(renderizadas.get(j).papel,m.papel)){conteudo.removeAllViews();renderizadas.clear();j=-1;continue;}if(j>=renderizadas.size())renderizadas.add(criarMensagem(m));MensagemRender r=renderizadas.get(j);boolean fluxo=e.gerando&&"assistant".equals(m.papel)&&j==quantidade-1;
    if(!igual(r.texto,m.texto)||r.fluxoAtivo!=fluxo){r.texto=m.texto;r.fluxoAtivo=fluxo;
     if(fluxo){if(r.textoFluxo==null){r.corpo.removeAllViews();r.textoFluxo=texto("",15,TEXTO,false);r.textoFluxo.setTextIsSelectable(true);r.textoFluxo.setLineSpacing(d(5),1);r.corpo.addView(r.textoFluxo);}r.textoFluxo.setText(seguro(m.texto));}
     else{r.textoFluxo=null;renderTexto(r.corpo,seguro(m.texto));}
    }atualizarAtividade(r,m);}}
  atualizarEnvio();legenda.setText(e.filaPendente>0?(e.filaPausada?"Fila pausada · ":"")+e.filaPendente+" mensagem(ns)"+(e.filaPausada?" · Toque para tentar novamente":" na fila"):"");legenda.setVisibility(e.filaPendente>0?View.VISIBLE:View.GONE);legenda.setOnClickListener(v->{if(estado!=null&&estado.filaPausada)servico.retomarFila();});if(mudou||perto||forcarFundo){forcarFundo=false;scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
 }
 private boolean igual(String a,String b){return a==null?b==null:a.equals(b);}private String seguro(String s){return s==null?"":s;}
 private String descricao(String f){if("baixando".equals(f))return "Baixando o modelo";if("verificando".equals(f))return "Conferindo o arquivo";if("carregando".equals(f))return "Preparando o modelo";if("contexto".equals(f))return "Organizando a conversa";if("gerando".equals(f))return "Criando uma resposta";if("pronto".equals(f))return "Pronto para conversar";if("erro".equals(f))return "O modelo precisa de atenção";return "Prepare seu espaço offline";}
 private void boasVindas(){
  conteudo.removeAllViews();conteudo.setGravity(Gravity.TOP);
  LinearLayout inicio=coluna();inicio.setPadding(d(2),d(34),d(2),d(12));
  TextView titulo=texto(rotulo("boasvindas","O que gostaria de fazer?"),24,TEXTO,false);titulo.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));inicio.addView(titulo);
  espaco(inicio,8);inicio.addView(texto("Escreva sua mensagem para começar.",13,SECUNDARIO,false));
  conteudo.addView(inicio,new LinearLayout.LayoutParams(-1,-2));
 }
 private LinearLayout.LayoutParams peso(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=d(4);p.rightMargin=d(4);return p;}
 private View sugestao(String titulo,String proposta,String icone){
  LinearLayout caixa=coluna();caixa.setMinimumHeight(d(83));caixa.setPadding(d(12),d(10),d(10),d(10));caixa.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33B59AFF),fundo(SUPERFICIE,BORDA,15),fundo(Color.WHITE,0,15)));
  ImageView im=new ImageView(this);im.setImageDrawable(new IconeAutoral(icone,LAVANDA));caixa.addView(im,new LinearLayout.LayoutParams(d(20),d(20)));espaco(caixa,8);
  TextView t=texto(titulo,12,TEXTO,false);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setMaxLines(2);caixa.addView(t);caixa.setContentDescription("Sugestão: "+titulo);caixa.setOnClickListener(v->{entrada.setText(proposta);entrada.setSelection(entrada.length());entrada.requestFocus();});return caixa;
 }
 private void renderModelo(LinearLayout painel,EstadoOffline e,boolean completo){
  boolean pronto=e.modeloPronto&&(e.gerando||"pronto".equals(e.fase)||"gerando".equals(e.fase)||"contexto".equals(e.fase));if(pronto&&!completo){painel.setVisibility(View.GONE);return;}painel.removeAllViews();
  painel.setVisibility(View.VISIBLE);painel.setPadding(d(15),d(14),d(15),d(12));painel.setBackground(fundo(SUPERFICIE,"erro".equals(e.fase)?0xff705045:BORDA,20));painel.addView(texto(descricao(e.fase),15,"erro".equals(e.fase)?CORAL:TEXTO,false));espaco(painel,7);
  String detalhe=seguro(e.detalhe);if(detalhe.isEmpty())detalhe="Baixe um modelo uma vez ou importe um arquivo GGUF. Depois, converse sem internet.";TextView copia=texto(detalhe,12,SECUNDARIO,false);copia.setMaxLines(completo?12:3);painel.addView(copia);
  if("erro".equals(e.fase)&&!e.modeloPronto&&!seguro(servico.enderecoDrive()).isEmpty()){espaco(painel,8);painel.addView(texto("Alternativa: no seletor de importar, abra o Drive e selecione todas as partes juntas.",12,LAVANDA,false));}
  boolean baixando="baixando".equals(e.fase),verificando="verificando".equals(e.fase),carregando="carregando".equals(e.fase);
  if(baixando||verificando||carregando){espaco(painel,12);ProgressBar barra=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);barra.setProgressTintList(ColorStateList.valueOf(LAVANDA));barra.setProgressBackgroundTintList(ColorStateList.valueOf(BORDA));barra.setIndeterminate(!baixando||e.total<=0);barra.setIndeterminateTintList(ColorStateList.valueOf(LAVANDA));barra.setMax(1000);if(e.total>0)barra.setProgress((int)Math.min(1000,e.bytes*1000L/e.total));barra.setContentDescription(baixando?"Download: "+tamanho(e.bytes)+" de "+tamanho(e.total):descricao(e.fase));painel.addView(barra,new LinearLayout.LayoutParams(-1,d(5)));espaco(painel,9);painel.addView(texto(baixando?tamanho(e.bytes)+" / "+tamanho(e.total):"Aguarde um instante…",11,SECUNDARIO,false));}
  if(!pronto&&!verificando&&!carregando&&!(e.modeloPronto&&"erro".equals(e.fase))){espaco(painel,12);LinearLayout a=linha();a.addView(texto(baixando?"Download em andamento":"Modelo para este aparelho",11,SECUNDARIO,false),new LinearLayout.LayoutParams(0,-2,1));if(!baixando&&"erro".equals(e.fase)&&!seguro(servico.enderecoDrive()).isEmpty())margem(a,botao("folder","Abrir pasta do modelo no Google Drive",false,v->abrirDrive()),8);if(!baixando)a.addView(botao("import","Importar modelo GGUF",false,v->{fecharDialogo();importar();}));margem(a,botao(baixando?"pause":"erro".equals(e.fase)?"retry":"download",baixando?"Pausar download":"erro".equals(e.fase)?"Tentar baixar novamente":"Baixar modelo offline",true,v->{if(estado!=null&&"baixando".equals(estado.fase))servico.cancelarDownload();else servico.baixarModelo();fecharDialogo();}),8);painel.addView(a);}
  if(e.modeloPronto&&"erro".equals(e.fase)){espaco(painel,10);painel.addView(texto("O modelo continua disponível. Ajuste a mensagem e envie novamente.",12,LAVANDA,false));}
 }
 private String tamanho(long b){if(b<=0)return "0 MB";if(b>=1024L*1024*1024)return String.format(Locale.getDefault(),"%.2f GB",b/(1024d*1024*1024));return String.format(Locale.getDefault(),"%.1f MB",b/(1024d*1024));}
 private static final class MensagemRender{
  String papel,texto,atividadeTexto="";LinearLayout corpo,atividadePainel,atividadeDetalhe;
  TextView textoFluxo,atividadeTitulo,atividadeSeta,atividadeEstado,pararAtividade;ProgressBar atividadeEspera;
  boolean fluxoAtivo,atividadeAberta;long id;
 }
 private void atualizarAtividade(MensagemRender r,EstadoOffline.Mensagem m){
  boolean ativa="gerando".equals(m.estadoAtividade);
  boolean mostrar=!seguro(m.estadoAtividade).isEmpty()&&(ativa||!seguro(m.atividade).isEmpty()||!"concluido".equals(m.estadoAtividade));
  r.atividadePainel.setVisibility(mostrar?View.VISIBLE:View.GONE);if(!mostrar)return;
  String titulo=seguro(m.tituloAtividade);
  if(titulo.isEmpty())titulo=ativa?"Preparando resposta…":"Atividade da resposta";
  r.atividadeTitulo.setText(titulo);r.atividadeEspera.setVisibility(ativa?View.VISIBLE:View.GONE);
  r.pararAtividade.setVisibility(ativa?View.VISIBLE:View.GONE);
  String situacao=ativa?"Em andamento":"limite".equals(m.estadoAtividade)?"Contexto ativo preenchido · peça para continuar":"erro".equals(m.estadoAtividade)?"Não foi possível concluir":"interrompido".equals(m.estadoAtividade)?"Interrompido":"Concluído";
  r.atividadeEstado.setText(situacao+" · "+Math.max(0,m.duracaoMs)/1000+" s");
  String detalhe=seguro(m.atividade);
  if(detalhe.isEmpty())detalhe=ativa?"Aguardando conteúdo do modelo…":"O modelo não enviou atividade antes de encerrar.";
  if(!igual(r.atividadeTexto,detalhe)){r.atividadeTexto=detalhe;if(r.atividadeAberta)renderTexto(r.atividadeDetalhe,detalhe);}
  r.atividadeDetalhe.setVisibility(r.atividadeAberta?View.VISIBLE:View.GONE);
  r.atividadeSeta.setText(r.atividadeAberta?"▾":"▸");
 }
 private MensagemRender criarMensagem(EstadoOffline.Mensagem m){
  conteudo.setGravity(Gravity.TOP);final MensagemRender r=new MensagemRender();r.papel=m.papel;r.id=m.id;boolean usuario="user".equals(m.papel)||"usuario".equals(m.papel);
  LinearLayout grupo=coluna();LinearLayout.LayoutParams grupoP=new LinearLayout.LayoutParams(-1,-2);grupoP.bottomMargin=d(13+espacamento);
  LinearLayout balhao=coluna();balhao.setPadding(usuario?d(15):d(1),usuario?d(13):d(5),usuario?d(15):d(1),usuario?d(13):d(5));
  if(usuario)balhao.setBackground(fundo(SUPERFICIE,0,20));
  r.corpo=coluna();balhao.addView(r.corpo);LinearLayout.LayoutParams balhaoP=new LinearLayout.LayoutParams(-1,-2);if(usuario)balhaoP.leftMargin=d(getSharedPreferences("estudio_visual",MODE_PRIVATE).getInt("margem_usuario",32));grupo.addView(balhao,balhaoP);
  LinearLayout acoes=linha();acoes.setPadding(d(2),d(3),d(2),0);
  acoes.addView(acaoIcone("copy","Copiar mensagem",()->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("Mensagem",seguro(r.texto)));Toast.makeText(this,"Copiado",Toast.LENGTH_SHORT).show();}));
  acoes.addView(acaoIcone("share","Compartilhar mensagem",()->compartilharMensagem(seguro(r.texto))));
  if(!usuario){
   acoes.addView(acaoIcone("like","Gostei desta resposta",()->{servico.avaliarResposta(conversaAtual,seguro(r.texto),1);Toast.makeText(this,"Avaliação salva e considerada nas próximas respostas desta conversa.",Toast.LENGTH_SHORT).show();}));
   acoes.addView(acaoIcone("dislike","Não gostei desta resposta",()->{servico.avaliarResposta(conversaAtual,seguro(r.texto),-1);Toast.makeText(this,"Feedback negativo salvo para melhorar as próximas respostas.",Toast.LENGTH_SHORT).show();}));
   acoes.addView(acaoIcone("retry","Gerar outra resposta",()->{if(estado!=null&&estado.gerando){Toast.makeText(this,"Aguarde a resposta atual.",Toast.LENGTH_SHORT).show();return;}servico.refazerResposta();}));
  }
  grupo.addView(acoes);
  r.atividadePainel=coluna();r.atividadePainel.setPadding(0,d(8),0,d(10));
  LinearLayout cabAtividade=linha();cabAtividade.setMinimumHeight(d(48));
  r.atividadeEspera=new ProgressBar(this,null,android.R.attr.progressBarStyleSmall);r.atividadeEspera.setIndeterminateTintList(ColorStateList.valueOf(LAVANDA));cabAtividade.addView(r.atividadeEspera,new LinearLayout.LayoutParams(d(20),d(20)));
  r.atividadeTitulo=texto("",13,SECUNDARIO,false);r.atividadeTitulo.setPadding(d(8),d(5),d(8),d(5));cabAtividade.addView(r.atividadeTitulo,new LinearLayout.LayoutParams(0,-2,1));
  r.atividadeSeta=texto("▸",18,LAVANDA,false);cabAtividade.addView(r.atividadeSeta);
  r.atividadeAberta=getSharedPreferences("atividades_abertas",MODE_PRIVATE).getBoolean(Long.toString(r.id),detalhesAbertos);
  cabAtividade.setContentDescription("Expandir ou recolher atividade da resposta");cabAtividade.setOnClickListener(v->{r.atividadeAberta=!r.atividadeAberta;getSharedPreferences("atividades_abertas",MODE_PRIVATE).edit().putBoolean(Long.toString(r.id),r.atividadeAberta).apply();r.atividadeDetalhe.setVisibility(r.atividadeAberta?View.VISIBLE:View.GONE);r.atividadeSeta.setText(r.atividadeAberta?"▾":"▸");if(r.atividadeAberta)renderTexto(r.atividadeDetalhe,r.atividadeTexto);});
  r.atividadePainel.addView(cabAtividade);r.atividadeEstado=texto("",11,SECUNDARIO,false);r.atividadePainel.addView(r.atividadeEstado);
  r.pararAtividade=texto("Parar geração",12,CORAL,false);r.pararAtividade.setPadding(0,d(9),0,d(9));r.pararAtividade.setOnClickListener(v->servico.cancelarGeracao());r.atividadePainel.addView(r.pararAtividade);
  r.atividadeDetalhe=coluna();r.atividadeDetalhe.setPadding(0,d(9),0,0);r.atividadePainel.addView(r.atividadeDetalhe);
  r.atividadePainel.setVisibility(View.GONE);grupo.addView(r.atividadePainel);
  conteudo.addView(grupo,grupoP);return r;
 }
 private void renderTexto(LinearLayout pai,String s){
  pai.removeAllViews();String[] partes=s.split("```",-1);
  for(int j=0;j<partes.length;j++){
   if(partes[j].isEmpty())continue;
   if(j%2==1){
    String bloco=partes[j],linguagem="texto";int quebra=bloco.indexOf('\n');
    if(quebra>=0&&bloco.substring(0,quebra).matches("[A-Za-z0-9+#._-]{0,24}")){linguagem=bloco.substring(0,quebra).trim();bloco=bloco.substring(quebra+1);}
    codigoFormatado(pai,linguagem,bloco);
   }else{
    TextView t=texto("",15,TEXTO,false);t.setLineSpacing(d(5),1);t.setTextIsSelectable(true);t.setText(negrito(partes[j].replaceAll("(?m)^#{1,4} +","")));pai.addView(t,new LinearLayout.LayoutParams(-1,-2));
   }
  }
 }
 private String extensao(String linguagem){String l=linguagem.toLowerCase(Locale.ROOT);if(l.equals("html"))return ".html";if(l.equals("css"))return ".css";if(l.equals("javascript")||l.equals("js"))return ".js";if(l.equals("python")||l.equals("py"))return ".py";if(l.equals("java"))return ".java";if(l.equals("json"))return ".json";if(l.equals("xml"))return ".xml";if(l.equals("bash")||l.equals("sh"))return ".sh";return ".txt";}
 private void codigoFormatado(LinearLayout pai,String linguagem,String codigo){
  final String fonteCodigo=codigo,lang=linguagem==null?"texto":linguagem;
  LinearLayout terminal=coluna();terminal.setBackground(fundo(getSharedPreferences("estudio_visual",MODE_PRIVATE).getInt("codigo",0xff11131B),BORDA,12));
  LinearLayout barra=linha();barra.setPadding(d(12),d(5),d(7),d(5));
  TextView nome=texto(lang.isEmpty()?"Código":lang.toUpperCase(Locale.ROOT),11,MENTA,false);nome.setTypeface(Typeface.MONOSPACE);barra.addView(nome,new LinearLayout.LayoutParams(0,-2,1));
  barra.addView(acaoIcone("copy","Copiar código",()->{ClipboardManager cp=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cp!=null)cp.setPrimaryClip(ClipData.newPlainText("Código",fonteCodigo));Toast.makeText(this,"Código copiado",Toast.LENGTH_SHORT).show();}));
  barra.addView(acaoIcone("download","Salvar código",()->salvarCodigo(fonteCodigo,"codigo"+extensao(lang),false)));
  barra.addView(acaoIcone("folder","Exportar projeto ZIP",()->salvarCodigo(fonteCodigo,"projeto-metrys.zip",true)));
  if(lang.equalsIgnoreCase("html"))barra.addView(acaoIcone("expand","Prévia HTML",()->preverHtml(fonteCodigo)));
  terminal.addView(barra);TextView t=texto(fonteCodigo,13,0xffDAE5F2,false);t.setTypeface(Typeface.MONOSPACE);t.setTextIsSelectable(true);t.setPadding(d(12),d(12),d(12),d(12));
  HorizontalScrollView h=new HorizontalScrollView(this);h.setFillViewport(true);h.addView(t,new ViewGroup.LayoutParams(-2,-2));terminal.addView(h);
  LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.setMargins(0,d(9),0,d(10));pai.addView(terminal,tp);
 }
 private void salvarCodigo(String codigo,String nome,boolean zip){codigoPendente=codigo;nomeCodigoPendente=nome;
  Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(zip?"application/zip":"text/plain");i.putExtra(Intent.EXTRA_TITLE,nome);
  try{startActivityForResult(i,zip?EXPORTAR_ZIP:EXPORTAR_CODIGO);}catch(Exception e){Toast.makeText(this,"Não foi possível abrir o salvamento.",Toast.LENGTH_SHORT).show();}
 }
 private void exportarCodigo(Uri uri,boolean zip){final String conteudo=codigoPendente,nome=nomeCodigoPendente;
  new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
   if(out==null)throw new IOException("Destino não disponível.");
   if(zip){ZipOutputStream pacote=new ZipOutputStream(out);String filename="index.html";String l=conteudo.trim();if(!l.startsWith("<!DOCTYPE")&&!l.startsWith("<html"))filename="codigo.txt";
    pacote.putNextEntry(new ZipEntry(filename));pacote.write(conteudo.getBytes(StandardCharsets.UTF_8));pacote.closeEntry();pacote.finish();}
   else out.write(conteudo.getBytes(StandardCharsets.UTF_8));out.flush();
   main.post(()->Toast.makeText(this,"Arquivo salvo: "+nome,Toast.LENGTH_SHORT).show());
  }catch(Exception e){main.post(()->Toast.makeText(this,"Erro ao exportar arquivo: "+e.getMessage(),Toast.LENGTH_LONG).show());}},"metrys-exportar-codigo").start();
 }
 private void preverHtml(String html){
  Dialog d=new Dialog(this);LinearLayout layout=coluna();layout.setBackgroundColor(0xff15131B);LinearLayout toolbar=linha();toolbar.setPadding(d(12),d(6),d(12),d(6));
  toolbar.addView(texto("Prévia do HTML",16,TEXTO,false),new LinearLayout.LayoutParams(0,-2,1));toolbar.addView(acaoIcone("close","Fechar prévia",d::dismiss));layout.addView(toolbar);
  TextView aviso=texto("Prévia isolada, sem internet e sem acesso a arquivos do aparelho.",11,SECUNDARIO,false);aviso.setPadding(d(12),d(2),d(12),d(8));layout.addView(aviso);
  WebView web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setBlockNetworkLoads(true);web.getSettings().setDomStorageEnabled(false);
  web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,String url){return true;}});
  web.loadDataWithBaseURL("https://metrys-preview.invalid/",html,"text/html","UTF-8",null);layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  d.setContentView(layout);d.setOnDismissListener(v->{web.stopLoading();web.destroy();});d.show();Window win=d.getWindow();if(win!=null)win.setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.85));
 }
 private CharSequence negrito(String s){SpannableStringBuilder b=new SpannableStringBuilder();int inicio=0;while(inicio<s.length()){int a=s.indexOf("**",inicio);if(a<0){b.append(s.substring(inicio));break;}int z=s.indexOf("**",a+2);if(z<0){b.append(s.substring(inicio));break;}b.append(s.substring(inicio,a));int pos=b.length();b.append(s.substring(a+2,z));b.setSpan(new StyleSpan(Typeface.BOLD),pos,b.length(),33);inicio=z+2;}return b;}
 private LinearLayout folha(String nome){
  fecharDialogo();dialogo=new Dialog(this);dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout painel=coluna();painel.setPadding(d(22),d(18),d(22),d(22));painel.setBackground(fundo(BG,BORDA,28));LinearLayout topo=linha();topo.addView(texto(nome,25,TEXTO,true),new LinearLayout.LayoutParams(0,-2,1));topo.addView(botao("close","Fechar janela",false,v->fecharDialogo()));painel.addView(topo);espaco(painel,18);
  ScrollView envoltoria=new ScrollView(this){@Override protected void onMeasure(int l,int a){super.onMeasure(l,View.MeasureSpec.makeMeasureSpec((int)(getResources().getDisplayMetrics().heightPixels*.82f),View.MeasureSpec.AT_MOST));}};envoltoria.addView(painel);dialogo.setContentView(envoltoria);Window w=dialogo.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.dimAmount=.55f;w.setAttributes(p);}return painel;
 }
 private void abrirFolha(){dialogo.show();Window w=dialogo.getWindow();if(w!=null){w.setLayout(getResources().getDisplayMetrics().widthPixels-d(24),-2);w.setGravity(Gravity.BOTTOM);}}
 private void fecharDialogo(){if(dialogo!=null){dialogo.dismiss();dialogo=null;}}
 private void historico(){
  LinearLayout painel=folha("Suas conversas");ScrollView lista=new ScrollView(this);LinearLayout itens=coluna();lista.addView(itens);List<EstadoOffline.Conversa> cs=estado==null?null:estado.conversas;
  if(cs==null||cs.isEmpty())itens.addView(texto("Este é o começo. Suas conversas aparecerão aqui.",14,SECUNDARIO,false));
  else for(final EstadoOffline.Conversa c:cs){LinearLayout row=linha();row.setPadding(d(12),d(4),d(4),d(4));row.setBackground(fundo(igual(c.id,conversaAtual)?0xff302737:SUPERFICIE,0,16));TextView label=texto(seguro(c.titulo).isEmpty()?"Nova conversa":c.titulo,14,TEXTO,false);label.setMaxLines(2);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));row.addView(botao("edit","Opções para "+seguro(c.titulo),false,v->opcoesConversa(c)));row.addView(botao("arrow","Abrir conversa: "+seguro(c.titulo),false,v->{servico.selecionarConversa(c.id);fecharDialogo();}));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=d(8);itens.addView(row,rp);}
  painel.addView(lista,new LinearLayout.LayoutParams(-1,cs!=null&&cs.size()>4?d(330):-2));if(estado!=null&&!estado.conversasArquivadas.isEmpty()){espaco(painel,12);painel.addView(texto("CONVERSAS ARQUIVADAS",11,SECUNDARIO,false));for(EstadoOffline.Conversa arquivada:estado.conversasArquivadas){LinearLayout r=linha();r.addView(texto(arquivada.titulo,13,SECUNDARIO,false),new LinearLayout.LayoutParams(0,-2,1));r.addView(botao("archive","Restaurar conversa arquivada",false,v->{servico.restaurarConversa(arquivada.id);fecharDialogo();}));painel.addView(r);}}espaco(painel,16);LinearLayout a=linha();a.addView(texto("Salvo somente neste aparelho",11,SECUNDARIO,false),new LinearLayout.LayoutParams(0,-2,1));a.addView(botao("export","Exportar histórico",false,v->{fecharDialogo();exportar();}));margem(a,botao("plus","Criar conversa",true,v->{servico.novaConversa();forcarFundo=true;fecharDialogo();}),8);painel.addView(a);abrirFolha();
 }
 private void pedirTexto(String titulo,String inicial,java.util.function.Consumer<String> aoConfirmar){EditText campo=new EditText(this);campo.setSingleLine(false);campo.setText(inicial);campo.setSelectAllOnFocus(true);LinearLayout caixa=coluna();caixa.setPadding(d(20),d(6),d(20),0);caixa.addView(campo);new android.app.AlertDialog.Builder(this).setTitle(titulo).setView(caixa).setNegativeButton("Cancelar",(a,b)->{}).setPositiveButton("Salvar",(a,b)->{String valor=campo.getText().toString().trim();if(!valor.isEmpty())aoConfirmar.accept(valor);else Toast.makeText(this,"Preencha o texto antes de salvar.",Toast.LENGTH_SHORT).show();}).show();}
 private void confirmar(String titulo,String descricao,Runnable acao){new android.app.AlertDialog.Builder(this).setTitle(titulo).setMessage(descricao).setNegativeButton("Cancelar",(a,b)->{}).setPositiveButton("Confirmar",(a,b)->{acao.run();fecharDialogo();}).show();}
 private void opcoesConversa(EstadoOffline.Conversa c){fecharDialogo();String[] opcoes={"Renomear", "Mover para projeto", "Arquivar", "Excluir permanentemente"};new android.app.AlertDialog.Builder(this).setTitle(c.titulo).setItems(opcoes,(d,i)->{switch(i){case 0:pedirTexto("Renomear conversa",c.titulo,s->servico.renomearConversa(c.id,s));break;case 1:escolherProjeto(c.id);break;case 2:servico.arquivarConversa(c.id);break;case 3:confirmar("Excluir conversa?","Todas as mensagens desta conversa serão apagadas do banco do Metrys. Esta ação não pode ser desfeita.",()->servico.excluirConversa(c.id));break;}}).show();}
 private void escolherProjeto(String conversa){List<EstadoOffline.Projeto> lista=new ArrayList<>();if(estado!=null)for(EstadoOffline.Projeto p:estado.projetos)if(!p.arquivado)lista.add(p);String[] opcoes=new String[lista.size()+1];opcoes[0]="Sem projeto";for(int j=0;j<lista.size();j++)opcoes[j+1]=lista.get(j).nome;new android.app.AlertDialog.Builder(this).setTitle("Mover conversa para").setItems(opcoes,(d,i)->servico.moverConversa(conversa,i==0?null:lista.get(i-1).id)).setNegativeButton("Cancelar",null).show();}
 private void projetos(){LinearLayout painel=folha("Projetos Metrys");painel.addView(texto("Organize conversas por assunto. Os dados permanecem no celular.",13,SECUNDARIO,false));espaco(painel,12);
  List<EstadoOffline.Projeto> projetos=estado==null?Collections.emptyList():estado.projetos;for(EstadoOffline.Projeto p:projetos){LinearLayout row=linha();row.setPadding(d(12),d(4),d(4),d(4));row.setBackground(fundo(SUPERFICIE,0,15));row.addView(texto(p.nome+(p.arquivado?" · arquivado":""),14,p.arquivado?SECUNDARIO:TEXTO,false),new LinearLayout.LayoutParams(0,-2,1));row.addView(botao(p.arquivado?"archive":"edit",p.arquivado?"Restaurar projeto":"Opções do projeto "+p.nome,false,v->{if(p.arquivado){servico.restaurarProjeto(p.id);fecharDialogo();}else opcoesProjeto(p);}));if(!p.arquivado)row.addView(botao("plus","Nova conversa em "+p.nome,false,v->{servico.criarConversaEmProjeto(p.id);fecharDialogo();}));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=d(8);painel.addView(row,lp);}
  if(projetos.isEmpty())painel.addView(texto("Nenhum projeto criado.",14,SECUNDARIO,false));espaco(painel,12);LinearLayout rod=linha();rod.addView(texto("Novo projeto",14,LAVANDA,false),new LinearLayout.LayoutParams(0,-2,1));rod.addView(botao("plus","Criar projeto",true,v->{fecharDialogo();pedirTexto("Nome do projeto","",s->servico.criarProjeto(s));}));painel.addView(rod);abrirFolha();
 }
 private void opcoesProjeto(EstadoOffline.Projeto projeto){fecharDialogo();String[] opcoes={"Renomear", "Instruções da IA neste projeto", "Arquivar projeto", "Excluir projeto e manter chats", "Excluir projeto e conversas"};new android.app.AlertDialog.Builder(this).setTitle(projeto.nome).setItems(opcoes,(d,i)->{switch(i){case 0:pedirTexto("Renomear projeto",projeto.nome,s->servico.renomearProjeto(projeto.id,s));break;case 1:pedirTexto("Instruções do projeto","",s->servico.instrucaoProjeto(projeto.id,s));break;case 2:servico.arquivarProjeto(projeto.id);break;case 3:confirmar("Excluir projeto?","O projeto será excluído, mas as conversas permanecerão disponíveis fora dele.",()->servico.excluirProjeto(projeto.id,false));break;case 4:confirmar("Excluir tudo do projeto?","O projeto, todas as suas conversas e mensagens serão excluídos permanentemente.",()->servico.excluirProjeto(projeto.id,true));break;}}).show();}
 private void pesquisar(){fecharDialogo();pedirTexto("Buscar nas conversas","",consulta->{List<BancoMetrys.Resultado> resultados=servico.buscar(consulta);LinearLayout painel=folha("Resultados da busca");painel.addView(texto(resultados.size()+" resultados para “"+consulta+"”",13,SECUNDARIO,false));espaco(painel,14);for(BancoMetrys.Resultado r:resultados){LinearLayout linha=linha();LinearLayout detalhes=coluna();detalhes.addView(texto(r.titulo,14,TEXTO,false));TextView trecho=texto(r.trecho,12,SECUNDARIO,false);trecho.setMaxLines(2);detalhes.addView(trecho);linha.addView(detalhes,new LinearLayout.LayoutParams(0,-2,1));linha.addView(botao("arrow","Abrir resultado",false,v->{servico.selecionarConversa(r.conversaId);fecharDialogo();}));painel.addView(linha);espaco(painel,9);}abrirFolha();});}
 private void editarPersonalizacao(){fecharDialogo();LinearLayout painel=folha("Personalização");painel.addView(texto("Aplicada nas próximas respostas, inclusive na primeira mensagem de novas conversas.",13,SECUNDARIO,false));espaco(painel,15);painel.addView(texto("O que o Metrys deve saber?",15,TEXTO,false));EditText sobre=new EditText(this);sobre.setText(servico.personalizacao("sobre_usuario"));sobre.setHint("Informações e objetivos que deseja compartilhar");sobre.setMinLines(2);sobre.setMaxLines(5);painel.addView(sobre);espaco(painel,15);painel.addView(texto("Como o Metrys deve responder?",15,TEXTO,false));EditText estilo=new EditText(this);estilo.setText(servico.personalizacao("como_responder"));estilo.setHint("Tom, idioma, formato e grau de detalhes");estilo.setMinLines(2);estilo.setMaxLines(5);painel.addView(estilo);espaco(painel,12);Button salvar=new Button(this);salvar.setText("Salvar personalização");salvar.setOnClickListener(v->{servico.salvarPersonalizacao(sobre.getText().toString(),estilo.getText().toString());fecharDialogo();});painel.addView(salvar);abrirFolha();}
 private void gerenciarMemorias(){LinearLayout painel=folha("Memória consciente");painel.addView(texto("Somente lembranças que você escolheu salvar. Você pode removê-las a qualquer momento.",13,SECUNDARIO,false));espaco(painel,12);List<BancoMetrys.Memoria> memorias=servico.memorias();for(BancoMetrys.Memoria m:memorias){LinearLayout row=linha();TextView t=texto(m.conteudo,13,TEXTO,false);t.setMaxLines(4);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));row.addView(botao("trash","Apagar memória",false,v->confirmar("Apagar memória?","Esta lembrança será removida da memória personalizada, sem apagar a conversa original.",()->servico.apagarMemoria(m.id))));painel.addView(row);espaco(painel,9);}if(memorias.isEmpty())painel.addView(texto("Você ainda não salvou memórias.",13,SECUNDARIO,false));espaco(painel,10);Button adicionar=new Button(this);adicionar.setText("Guardar nova memória");adicionar.setOnClickListener(v->{fecharDialogo();pedirTexto("O que devo lembrar?","",s->servico.guardarMemoria(s));});painel.addView(adicionar);abrirFolha();}
 private void menuAnexos(){
  LinearLayout p=folha("Adicionar à mensagem");
  for(String[] item:new String[][]{{"Abrir editor grande","expandir"},{"Anexar texto de arquivo","arquivo"},{"Ditar mensagem","ditado"}}){Button b=new Button(this);b.setText(item[0]);b.setOnClickListener(v->{fecharDialogo();if("expandir".equals(item[1]))expandirEditor();else if("arquivo".equals(item[1]))selecionarTexto();else ditar();});p.addView(b);}
  abrirFolha();
 }
 private void fecharGaveta(){if(gavetaOverlay!=null){View atual=gavetaOverlay;gavetaOverlay=null;if(camada!=null&&atual.getParent()==camada)camada.removeView(atual);}}
 private void opcaoGaveta(LinearLayout painel,String icone,String titulo,Runnable acao){
  LinearLayout linhaMenu=linha();linhaMenu.setPadding(d(14),d(9),d(14),d(9));linhaMenu.setBackground(new RippleDrawable(ColorStateList.valueOf(0x20B59AFF),fundo(Color.TRANSPARENT,0,12),fundo(Color.WHITE,0,12)));
  ImageView icon=new ImageView(this);icon.setImageDrawable(new IconeAutoral(icone,LAVANDA));linhaMenu.addView(icon,new LinearLayout.LayoutParams(d(23),d(23)));
  TextView rot=texto(titulo,15,TEXTO,false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=d(16);linhaMenu.addView(rot,lp);
  linhaMenu.setContentDescription(titulo);linhaMenu.setOnClickListener(v->{fecharGaveta();acao.run();});
  LinearLayout.LayoutParams linhaLp=new LinearLayout.LayoutParams(-1,d(55));linhaLp.bottomMargin=d(3);painel.addView(linhaMenu,linhaLp);
 }
 private void menuNavegacao(){
  fecharGaveta();FrameLayout overlay=new FrameLayout(this);overlay.setBackgroundColor(0x9B09070D);gavetaOverlay=overlay;
  overlay.setOnClickListener(v->fecharGaveta());camada.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
  ScrollView area=new ScrollView(this);area.setFillViewport(true);area.setBackgroundColor(BG);
  LinearLayout painel=coluna();painel.setPadding(d(12),d(38),d(12),d(28));area.addView(painel);
  LinearLayout header=linha();TextView titulo=texto("Meu espaço",22,TEXTO,false);header.addView(titulo,new LinearLayout.LayoutParams(0,-2,1));header.addView(acaoIcone("close","Fechar menu",()->fecharGaveta()));painel.addView(header);espaco(painel,22);
  opcaoGaveta(painel,"edit","Nova conversa",()->{servico.novaConversa();forcarFundo=true;});
  opcaoGaveta(painel,"history","Conversas",this::historico);
  opcaoGaveta(painel,"folder","Projetos",this::projetos);
  opcaoGaveta(painel,"search","Pesquisar",this::pesquisar);
  espaco(painel,24);painel.addView(texto("FERRAMENTAS",11,SECUNDARIO,false));espaco(painel,7);
  opcaoGaveta(painel,"model","Modelos de IA",this::modelosDisponiveis);
  opcaoGaveta(painel,"tools","Estúdio visual",()->new EditorVisualMetrys(this,this::aplicarMudancaVisual).abrir());
  opcaoGaveta(painel,"folder","Arquivos e trabalho",this::workLocal);
  opcaoGaveta(painel,"bookmark","Personalização",this::editarPersonalizacao);
  opcaoGaveta(painel,"bookmark","Memórias",this::gerenciarMemorias);
  espaco(painel,15);TextView versao=texto("Metrys v0.6.10",12,SECUNDARIO,false);painel.addView(versao);
  int largura=Math.min(d(335),(int)(getResources().getDisplayMetrics().widthPixels*.88f));
  FrameLayout.LayoutParams pos=new FrameLayout.LayoutParams(largura,-1,Gravity.START);overlay.addView(area,pos);area.setOnClickListener(v->{});
  area.setTranslationX(-largura);area.animate().translationX(0).setDuration(220).start();
 }
 private void menuFerramentas(){
  LinearLayout p=folha("Ferramentas Metrys");String[] itens={"Estúdio visual · editar a interface","Work local e arquivos","Personalização da IA","Memórias salvas"};
  for(int i=0;i<itens.length;i++){final int j=i;Button b=new Button(this);b.setText(itens[i]);b.setOnClickListener(v->{fecharDialogo();if(j==0)new EditorVisualMetrys(this,this::aplicarMudancaVisual).abrir();else if(j==1)workLocal();else if(j==2)editarPersonalizacao();else gerenciarMemorias();});p.addView(b);}
  abrirFolha();
 }
 private void ajustes(){
  LinearLayout painel=folha("Configurações do Metrys");painel.addView(texto("Um modelo local. Um espaço seu.",15,LAVANDA,false));espaco(painel,12);painel.addView(texto("As respostas são geradas no aparelho. O download inicial precisa de internet; depois disso, o chat funciona sem PC e sem conexão.",13,SECUNDARIO,false));espaco(painel,20);
  if(estado!=null){LinearLayout info=coluna();renderModelo(info,estado,true);painel.addView(info);espaco(painel,14);painel.addView(texto("MODELO",10,SECUNDARIO,false));espaco(painel,6);painel.addView(texto(seguro(estado.nomeModelo).isEmpty()?"Nenhum modelo instalado":estado.nomeModelo,14,TEXTO,false));if(estado.total>0){espaco(painel,5);painel.addView(texto("Arquivo · "+tamanho(estado.total),12,SECUNDARIO,false));}}
  espaco(painel,10);Button escolherModelo=new Button(this);escolherModelo.setText("Trocar modelo · 1,5B / 3B / 4B / Reason");escolherModelo.setOnClickListener(v->{fecharDialogo();modelosDisponiveis();});painel.addView(escolherModelo);
  espaco(painel,10);Button abrirEstudio=new Button(this);abrirEstudio.setText("✦ Estúdio visual · Modo desenvolvedor");abrirEstudio.setOnClickListener(v->{fecharDialogo();new EditorVisualMetrys(this,this::aplicarMudancaVisual).abrir();});painel.addView(abrirEstudio);espaco(painel,12);LinearLayout a=linha();a.addView(texto("Importe um .gguf ou selecione todas as partes do Drive",12,SECUNDARIO,false),new LinearLayout.LayoutParams(0,-2,1));if(!seguro(servico.enderecoDrive()).isEmpty())a.addView(botao("folder","Abrir pasta do modelo no Google Drive",false,v->abrirDrive()));a.addView(botao("import","Escolher modelo GGUF ou todas as partes do Drive",false,v->{fecharDialogo();importar();}));painel.addView(a);espaco(painel,18);Button personalizar=new Button(this);personalizar.setText("Personalização da IA");personalizar.setOnClickListener(v->editarPersonalizacao());painel.addView(personalizar);Button memoria=new Button(this);memoria.setText("Memórias salvas");memoria.setOnClickListener(v->{fecharDialogo();gerenciarMemorias();});painel.addView(memoria);Button permissoes=new Button(this);permissoes.setText("Permissões das ferramentas");permissoes.setOnClickListener(v->selecionarPermissao());painel.addView(permissoes);Button recursos=new Button(this);recursos.setText("Recursos conectados anteriores");recursos.setOnClickListener(v->{fecharDialogo();recursosLegados();});painel.addView(recursos);abrirFolha();
 }
 private void selecionarPermissao(){fecharDialogo();String[] rotulos={"Solicitar autorização (padrão)","Apenas texto (bloqueia escrita)","Acesso total somente aos arquivos autorizados"};String[] ids={PermissoesMetrys.SOLICITAR,PermissoesMetrys.APENAS_TEXTO,PermissoesMetrys.ACESSO_TOTAL};int atual=0;for(int i=0;i<ids.length;i++)if(ids[i].equals(servico.modoPermissao()))atual=i;new android.app.AlertDialog.Builder(this).setTitle("Modo de acesso").setSingleChoiceItems(rotulos,atual,(dialogo,i)->{servico.definirModoPermissao(ids[i]);dialogo.dismiss();}).setNegativeButton("Cancelar",null).show();}
 private void workLocal(){LinearLayout painel=folha("Work local");painel.addView(texto("Ações reais no Android, limitadas aos arquivos que você autorizar. Este modo ainda não executa código ou tarefas automaticamente.",13,SECUNDARIO,false));espaco(painel,15);Button criar=new Button(this);criar.setText("Criar arquivo usando o texto do editor");criar.setOnClickListener(v->{fecharDialogo();criarArquivo();});painel.addView(criar);Button editar=new Button(this);editar.setText("Abrir e editar arquivo de texto");editar.setOnClickListener(v->{fecharDialogo();editarArquivo();});painel.addView(editar);espaco(painel,15);painel.addView(texto("Meus trabalhos",17,TEXTO,true));espaco(painel,10);for(BancoMetrys.Trabalho t:servico.trabalhos()){LinearLayout row=linha();TextView titulo=texto(t.objetivo+" · "+("concluido".equals(t.estado)?"Concluído":"Pendente"),13,TEXTO,false);row.addView(titulo,new LinearLayout.LayoutParams(0,-2,1));if(!"concluido".equals(t.estado))row.addView(botao("bookmark","Marcar trabalho como concluído",false,v->{servico.concluirTrabalho(t.id);fecharDialogo();}));painel.addView(row);}espaco(painel,8);Button novo=new Button(this);novo.setText("Registrar novo objetivo");novo.setOnClickListener(v->{fecharDialogo();pedirTexto("Objetivo do trabalho","",s->servico.novoTrabalho(s));});painel.addView(novo);abrirFolha();}
 private void criarArquivo(){conteudoPendenteArquivo=entrada.getText().toString();if(conteudoPendenteArquivo.trim().isEmpty()){Toast.makeText(this,"Escreva o conteúdo primeiro no editor da conversa.",Toast.LENGTH_LONG).show();return;}Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TITLE,"metrys-arquivo.txt");try{startActivityForResult(intent,CRIAR_ARQUIVO);}catch(Exception e){Toast.makeText(this,"Seletor de arquivos indisponível.",Toast.LENGTH_LONG).show();}}
 private void editarArquivo(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/*");intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(intent,EDITAR_ARQUIVO);}catch(Exception e){Toast.makeText(this,"Seletor de arquivos indisponível.",Toast.LENGTH_LONG).show();}}
 private void salvarArquivo(Uri uri,String texto,String hash){new Thread(()->{try{EditorArquivos editor=new EditorArquivos(this);File revisao=editor.salvar(uri,texto,hash,true);main.post(()->Toast.makeText(this,revisao==null?"Arquivo criado no local selecionado.":"Arquivo atualizado. Revisão privada preservada.",Toast.LENGTH_LONG).show());}catch(Exception e){main.post(()->Toast.makeText(this,"Falha ao salvar: "+e.getMessage(),Toast.LENGTH_LONG).show());}},"metrys-salvar-saf").start();}
 private void abrirEdicao(Uri uri){new Thread(()->{try{EditorArquivos.Documento documento=new EditorArquivos(this).ler(uri);main.post(()->{Dialog tela=new Dialog(this);LinearLayout painel=coluna();painel.setBackgroundColor(BG);painel.setPadding(d(16),d(20),d(16),d(12));painel.addView(texto("Editar arquivo autorizado",19,TEXTO,true));EditText campo=new EditText(this);campo.setText(documento.conteudo);campo.setTextColor(TEXTO);campo.setTypeface(Typeface.MONOSPACE);campo.setGravity(Gravity.TOP);LinearLayout.LayoutParams area=new LinearLayout.LayoutParams(-1,0,1);area.topMargin=d(12);painel.addView(campo,area);Button salvar=new Button(this);salvar.setText("Salvar nova versão");salvar.setOnClickListener(v->{String novo=campo.getText().toString();new android.app.AlertDialog.Builder(this).setTitle("Confirmar alteração").setMessage("Uma cópia do conteúdo anterior será mantida na área privada do Metrys antes da escrita. Deseja salvar?").setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(dg,k)->{salvarArquivo(uri,novo,documento.sha256);tela.dismiss();}).show();});painel.addView(salvar);tela.setContentView(painel);tela.show();Window w=tela.getWindow();if(w!=null)w.setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.83f));});}catch(Exception e){main.post(()->Toast.makeText(this,"Falha ao abrir: "+e.getMessage(),Toast.LENGTH_LONG).show());}},"metrys-abrir-saf").start();}
 private void recursosLegados(){
  LinearLayout painel=folha("Recursos conectados");painel.addView(texto("O Metrys original continua aqui.",18,LAVANDA,true));espaco(painel,12);painel.addView(texto("As ferramentas antigas usam a conexão com seu PC e podem precisar de internet. Elas abrem no modo conectado. Será necessário parear novamente neste aplicativo.",14,SECUNDARIO,false));espaco(painel,20);LinearLayout a=linha();a.addView(texto("Abrir modo conectado",12,TEXTO,false),new LinearLayout.LayoutParams(0,-2,1));a.addView(botao("arrow","Confirmar abertura do Metrys conectado, que usa PC ou internet",true,v->{fecharDialogo();try{Intent i=new Intent();if(getPackageName().endsWith(".previa"))i.setClassName("br.com.anderflow.metrys","br.com.anderflow.metrys.TelaPrincipal");else i.setClassName(this,"br.com.anderflow.metrys.TelaPrincipal");startActivity(i);}catch(Exception e){Toast.makeText(this,"Não foi possível abrir os recursos conectados.",Toast.LENGTH_LONG).show();}}));painel.addView(a);abrirFolha();
 }
 private void expandirEditor(){final Dialog tela=new Dialog(this);LinearLayout panel=coluna();panel.setPadding(d(16),d(20),d(16),d(12));panel.setBackgroundColor(BG);panel.addView(texto("Editar mensagem",22,TEXTO,true));EditText grande=new EditText(this);grande.setText(entrada.getText());grande.setSelection(grande.length());grande.setTextColor(TEXTO);grande.setHintTextColor(SECUNDARIO);grande.setBackground(fundo(SUPERFICIE,BORDA,15));grande.setGravity(Gravity.TOP);grande.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);grande.setPadding(d(14),d(14),d(14),d(14));LinearLayout.LayoutParams area=new LinearLayout.LayoutParams(-1,0,1);area.topMargin=d(14);panel.addView(grande,area);LinearLayout botoes=linha();Button cancelar=new Button(this);cancelar.setText("Cancelar");cancelar.setOnClickListener(v->tela.dismiss());botoes.addView(cancelar,new LinearLayout.LayoutParams(0,-2,1));Button aplicar=new Button(this);aplicar.setText("Usar texto");aplicar.setOnClickListener(v->{entrada.setText(grande.getText().toString());entrada.setSelection(entrada.length());tela.dismiss();});botoes.addView(aplicar,new LinearLayout.LayoutParams(0,-2,1));panel.addView(botoes);tela.setContentView(panel);tela.show();Window w=tela.getWindow();if(w!=null)w.setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.82f));}
 private void ditar(){Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR");intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Dite sua mensagem. Revise antes de enviar.");try{startActivityForResult(intent,DITADO);}catch(Exception e){Toast.makeText(this,"O aparelho não tem um serviço de reconhecimento de voz compatível. O modo offline depende do pacote instalado no Android.",Toast.LENGTH_LONG).show();}}
 private void selecionarTexto(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(intent,PREVER);}catch(Exception e){Toast.makeText(this,"Não há um seletor de arquivos disponível.",Toast.LENGTH_LONG).show();}}
 private void visualizarTexto(Uri uri){new Thread(()->{String conteudo;try(InputStream stream=getContentResolver().openInputStream(uri)){if(stream==null)throw new IOException("Arquivo indisponível.");ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] bloco=new byte[8192];int n;while((n=stream.read(bloco))!=-1){if(buffer.size()+n>128*1024)throw new IOException("Prévia limitada a 128 KB por segurança.");buffer.write(bloco,0,n);}byte[] bytes=buffer.toByteArray();for(byte b:bytes)if(b==0)throw new IOException("Arquivo binário. Esta prévia suporta apenas textos UTF-8.");conteudo=new String(bytes,StandardCharsets.UTF_8);}catch(Exception ex){String erro=ex.getMessage();main.post(()->Toast.makeText(this,erro==null?"Não foi possível abrir arquivo.":erro,Toast.LENGTH_LONG).show());return;}final String textoArquivo=conteudo;main.post(()->{LinearLayout painel=folha("Prévia de arquivo");ScrollView scrollPreview=new ScrollView(this);TextView exibicao=texto(textoArquivo,12,TEXTO,false);exibicao.setTextIsSelectable(true);exibicao.setTypeface(Typeface.MONOSPACE);scrollPreview.addView(exibicao);painel.addView(scrollPreview,new LinearLayout.LayoutParams(-1,d(250)));espaco(painel,12);Button inserir=new Button(this);inserir.setText("Inserir trecho no editor");inserir.setOnClickListener(v->{String trecho=textoArquivo.substring(0,Math.min(1200,textoArquivo.length()));entrada.append((entrada.length()>0?"\n\n":"")+trecho);fecharDialogo();});painel.addView(inserir);abrirFolha();});},"metrys-previa-saf").start();}
 private void modelosDisponiveis(){
  LinearLayout painel=folha("Modelos de IA");
  painel.addView(texto("Aumentar a inteligência usa mais RAM. Cada modelo é baixado só uma vez; os anteriores ficam guardados.",14,SECUNDARIO,false));espaco(painel,15);
  android.app.ActivityManager gerente=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
  android.app.ActivityManager.MemoryInfo memoria=new android.app.ActivityManager.MemoryInfo();if(gerente!=null)gerente.getMemoryInfo(memoria);
  long disponivel=memoria.totalMem; painel.addView(texto("RAM física detectada: "+(disponivel/(1024*1024*1024))+" GB aproximadamente",12,SECUNDARIO,false));espaco(painel,12);
  for(org.json.JSONObject modelo:servico.modelos()){
   String codigo=modelo.optString("id"),titulo=modelo.optString("nome",codigo),perfil=modelo.optString("perfil","Conversa offline");
   long bytesModelo=modelo.optLong("bytes",0);int ram=modelo.optInt("ram_min_gb",4);
   boolean adequado=disponivel==0||disponivel>=ram*950L*1024*1024;
   LinearLayout cartao=coluna();cartao.setPadding(d(14),d(13),d(14),d(13));cartao.setBackground(fundo(SUPERFICIE,BORDA,16));
   cartao.addView(texto(titulo,16,TEXTO,true));espaco(cartao,6);
   cartao.addView(texto(perfil,13,SECUNDARIO,false));espaco(cartao,6);
   cartao.addView(texto(tamanho(bytesModelo)+" · RAM sugerida: "+ram+" GB"+(adequado?"":" · Memória insuficiente para uso confiável"),12,adequado?MENTA:CORAL,false));
   espaco(cartao,8);Button selecionar=new Button(this);selecionar.setText("Selecionar este modelo");selecionar.setOnClickListener(v->{
    Runnable acao=()->{servico.selecionarModelo(codigo);fecharDialogo();};
    if(!adequado)new android.app.AlertDialog.Builder(this).setTitle("Modelo exige mais memória").setMessage("O modelo pode causar lentidão ou encerrar o Metrys por falta de RAM. Deseja continuar mesmo assim?").setNegativeButton("Cancelar",null).setPositiveButton("Selecionar",(a,b)->acao.run()).show();
    else acao.run();
   });cartao.addView(selecionar);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=d(10);painel.addView(cartao,lp);
  }
  painel.addView(texto("O modelo anterior não será removido ao trocar. Depois de selecionar, abra Modelos e configurações para iniciar o download, se necessário.",12,SECUNDARIO,false));abrirFolha();
 }
 private void importar(){Toast.makeText(this,"Selecione um arquivo .gguf ou, no Drive, todas as partes do modelo.",Toast.LENGTH_LONG).show();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.putExtra(Intent.EXTRA_TITLE,"Modelo GGUF ou partes do Drive");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(i,IMPORTAR);}catch(Exception e){Toast.makeText(this,"Nenhum seletor de arquivos disponível.",Toast.LENGTH_LONG).show();}}
 private void exportar(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"metrys-conversas.json");try{startActivityForResult(i,EXPORTAR);}catch(Exception e){Toast.makeText(this,"Não foi possível abrir o seletor de arquivos.",Toast.LENGTH_LONG).show();}}
 private void abrirDrive(){String url=servico.enderecoDrive();if(url==null||url.isEmpty())return;try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"Nenhum aplicativo disponível para abrir o Drive.",Toast.LENGTH_LONG).show();}}
 @Override protected void onActivityResult(int req,int resultado,Intent dados){
  super.onActivityResult(req,resultado,dados);if(resultado!=RESULT_OK||dados==null)return;
  if(req==EXPORTAR_CODIGO||req==EXPORTAR_ZIP){if(dados.getData()!=null)exportarCodigo(dados.getData(),req==EXPORTAR_ZIP);return;}
  if(req==CRIAR_ARQUIVO){if(dados.getData()!=null)salvarArquivo(dados.getData(),conteudoPendenteArquivo,null);return;}
  if(req==EDITAR_ARQUIVO){Uri uri=dados.getData();if(uri!=null){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignorado){}abrirEdicao(uri);}return;}
  if(req==DITADO){ArrayList<String> reconhecimento=dados.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(reconhecimento!=null&&!reconhecimento.isEmpty()){String frase=reconhecimento.get(0);entrada.append((entrada.length()>0?" ":"")+frase);entrada.requestFocus();}return;}
  if(req==PREVER){if(dados.getData()!=null)visualizarTexto(dados.getData());return;}
  if(req==IMPORTAR){ArrayList<Uri> arquivos=new ArrayList<>();ClipData clip=dados.getClipData();if(clip!=null){for(int j=0;j<clip.getItemCount();j++){Uri u=clip.getItemAt(j).getUri();if(u!=null)arquivos.add(u);}}else if(dados.getData()!=null)arquivos.add(dados.getData());for(Uri u:arquivos)try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}if(arquivos.size()>1)servico.importarPartes(arquivos);else if(arquivos.size()==1)servico.importarModelo(arquivos.get(0));}
  else if(req==EXPORTAR&&dados.getData()!=null)servico.exportarHistorico(dados.getData());
 }
 private void mostrarSplash(){LinearLayout a=coluna();a.setGravity(Gravity.CENTER);a.setBackgroundColor(BG);marcaSplash=new FitaView(this,LAVANDA);a.addView(marcaSplash,new LinearLayout.LayoutParams(d(176),d(176)));espaco(a,24);a.addView(texto("metrys",39,TEXTO,true));espaco(a,12);TextView frase=texto("UM ESPAÇO SEU",10,SECUNDARIO,false);frase.setLetterSpacing(.23f);a.addView(frase);splash=a;camada.addView(splash,new FrameLayout.LayoutParams(-1,-1));raiz.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);marcaSplash.animar(true);main.postDelayed(fimSplash,2350);}
 private void removerSplash(){if(splash==null)return;if(marcaSplash!=null)marcaSplash.animar(false);final View antiga=splash;splash=null;raiz.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);if(observando&&ValueAnimator.areAnimatorsEnabled())antiga.animate().alpha(0).setDuration(240).withEndAction(()->camada.removeView(antiga)).start();else camada.removeView(antiga);}
}
