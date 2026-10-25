package br.com.anderflow.metrys.offline;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Editor de aparência offline. Modifica somente as preferências de UI, nunca mensagens/modelos. */
public final class EditorVisualMetrys {
 private final Activity activity;
 private final Runnable atualizar;
 private final android.content.SharedPreferences visual, textos;
 private int acento=0xffB59AFF;
 private Dialog dialogo;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable aplicarPendente;
 private TextView visualizacao,amostraUsuario,amostraIA,amostraCodigo;private LinearLayout amostraPainel;
 public EditorVisualMetrys(Activity a,Runnable aplicar){activity=a;atualizar=aplicar;aplicarPendente=()->atualizar.run();visual=a.getSharedPreferences("estudio_visual",Context.MODE_PRIVATE);textos=a.getSharedPreferences("estudio_textos",Context.MODE_PRIVATE);acento=visual.getInt("destaque",acento);}
 private int d(float n){return (int)(n*activity.getResources().getDisplayMetrics().density+.5f);}
 private LinearLayout coluna(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private TextView texto(String s,int tamanho){TextView t=new TextView(activity);t.setText(s);t.setTextSize(tamanho);t.setTextColor(0xffEEE8F3);t.setPadding(d(3),d(6),d(3),d(6));return t;}
 private Button botao(String s,Runnable r){Button b=new Button(activity);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
 private void atualizarVisual(){
  Typeface fonte=Typeface.create(visual.getInt("fonte",0)==2?"serif":"sans-serif",Typeface.NORMAL);
  if(visual.getInt("fonte",0)==0){try{fonte=new Typeface.Builder(activity.getAssets(),"fonts/manrope.ttf").setWeight(400).setFontVariationSettings("'wght' 400").build();}catch(RuntimeException ignored){}}
  int escala=visual.getInt("escala",100),espaco=visual.getInt("espacamento",0);
  for(TextView amostra:new TextView[]{visualizacao,amostraUsuario,amostraIA})if(amostra!=null){amostra.setTypeface(fonte);amostra.setLineSpacing(d(3+espaco),1f);}
  if(amostraUsuario!=null){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)amostraUsuario.getLayoutParams();lp.leftMargin=d(visual.getInt("margem_usuario",32));amostraUsuario.setLayoutParams(lp);}
  if(amostraCodigo!=null)amostraCodigo.setTextSize(13*escala/100f);
  if(visualizacao!=null){visualizacao.setTextColor(acento);visualizacao.setTextSize(17*visual.getInt("escala",100)/100f);GradientDrawable bg=new GradientDrawable();bg.setColor(visual.getInt("superficie",0xff24202F));bg.setCornerRadius(d(18+visual.getInt("raio",0)));visualizacao.setBackground(bg);}
  if(amostraPainel!=null){amostraPainel.setBackgroundColor(visual.getInt("fundo",0xff111018));
   if(amostraUsuario!=null){amostraUsuario.setTextColor(visual.getInt("texto",0xffF8F5FC));GradientDrawable b=new GradientDrawable();b.setColor(visual.getInt("superficie",0xff24202F));b.setCornerRadius(d(14+visual.getInt("raio",0)));amostraUsuario.setBackground(b);amostraUsuario.setTextSize(13*visual.getInt("escala",100)/100f);}
   if(amostraIA!=null){amostraIA.setText(textos.getString("boasvindas","Uma resposta do Metrys sem card desnecessário."));amostraIA.setTextColor(visual.getInt("texto",0xffF8F5FC));amostraIA.setTextSize(13*visual.getInt("escala",100)/100f);}
   if(amostraCodigo!=null){amostraCodigo.setTextColor(visual.getInt("positivo",0xff91DCC0));amostraCodigo.setBackgroundColor(visual.getInt("codigo",0xff11131B));}}
  handler.removeCallbacks(aplicarPendente);handler.postDelayed(aplicarPendente,120);}
 private SeekBar controle(LinearLayout root,String titulo,int min,int max,int valor,java.util.function.IntConsumer cb){
  TextView label=texto(titulo+"  "+valor,13);root.addView(label);SeekBar b=new SeekBar(activity);b.setMax(max-min);b.setProgress(valor-min);b.setProgressTintList(ColorStateList.valueOf(acento));b.setThumbTintList(ColorStateList.valueOf(acento));root.addView(b);
  b.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(!user)return;int v=p+min;label.setText(titulo+"  "+v);cb.accept(v);atualizarVisual();}});return b;
 }
 private void editarTexto(LinearLayout root,String titulo,String id,String padrao){
  root.addView(texto(titulo,13));EditText e=new EditText(activity);e.setText(textos.getString(id,padrao));e.setSingleLine(true);e.setTextSize(15);e.setTextColor(Color.WHITE);e.setSelectAllOnFocus(true);root.addView(e);
  e.setOnFocusChangeListener((v,temFoco)->{if(!temFoco){textos.edit().putString(id,e.getText().toString()).apply();atualizarVisual();}});
  e.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int n){}public void afterTextChanged(android.text.Editable s){}public void onTextChanged(CharSequence s,int start,int before,int count){textos.edit().putString(id,s.toString()).apply();atualizarVisual();}});
 }
 public void abrir(){
  dialogo=new Dialog(activity);dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout principal=coluna();principal.setPadding(d(18),d(13),d(18),d(14));principal.setBackgroundColor(0xff16121E);
  TextView titulo=texto("Estúdio visual · Metrys",23);titulo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);principal.addView(titulo);
  principal.addView(texto("Edite e veja alterações na conversa. O layout fica salvo neste aparelho, sem mexer nas suas mensagens e nos modelos.",13));
  visualizacao=texto("Prévia ao vivo",15);visualizacao.setPadding(d(11),d(10),d(11),d(5));
  amostraPainel=coluna();amostraPainel.setPadding(d(10),d(7),d(10),d(10));
  amostraUsuario=texto("Uma mensagem de exemplo",13);amostraUsuario.setPadding(d(12),d(10),d(12),d(10));amostraPainel.addView(amostraUsuario);
  amostraIA=texto("Uma resposta do Metrys sem card desnecessário.",13);amostraIA.setPadding(d(3),d(10),d(3),d(10));amostraPainel.addView(amostraIA);
  amostraCodigo=texto("$ python main.py   •  Código",13);amostraCodigo.setTypeface(Typeface.MONOSPACE);amostraCodigo.setPadding(d(12),d(10),d(12),d(10));amostraPainel.addView(amostraCodigo);
  atualizarVisual();
  principal.addView(texto("Paleta de superfícies",15));
  int[][] tons={
   {0xff111018,0xff24202F,0xff373042,0xffF8F5FC},{0xff0E1725,0xff1B2940,0xff344C68,0xffF3F7FF},
   {0xff201318,0xff3C2430,0xff573A4C,0xffFFF3F5},{0xff151C19,0xff263A31,0xff3E5A4B,0xffF0FFF5},
   {0xffF5F3F9,0xffFFFFFF,0xffE2DCEB,0xff251C31},{0xff171A1F,0xff2B3037,0xff404957,0xffF4F7FF},
   {0xff211B14,0xff382D21,0xff53442E,0xffFFF6DF},{0xff0B0A10,0xff1B1727,0xff35274D,0xffFFFFFF}};
  String[] nomes={"Violeta","Oceano","Rosa","Floresta","Claro","Grafite","Areia","Noite"};
  for(int linha=0;linha<2;linha++){LinearLayout grupo=new LinearLayout(activity);grupo.setOrientation(LinearLayout.HORIZONTAL);
   for(int j=linha*4;j<linha*4+4;j++){final int k=j;Button b=botao(nomes[j],()->{boolean claro=k==4;acento=claro?0xff6B46BA:0xffB59AFF;visual.edit().putInt("fundo",tons[k][0]).putInt("superficie",tons[k][1]).putInt("borda",tons[k][2]).putInt("texto",tons[k][3]).putInt("secundario",claro?0xff5C5266:0xffBEB5C9).putInt("destaque",acento).putInt("positivo",claro?0xff267354:0xff91DCC0).putInt("codigo",claro?0xffEFEAF5:0xff11131B).apply();atualizarVisual();});b.setTextSize(11);grupo.addView(b,new LinearLayout.LayoutParams(0,d(52),1));}principal.addView(grupo);}
  principal.addView(texto("Cores personalizadas",15));
  String[][] campos={{"Fundo","fundo"},{"Superfície da mensagem","superficie"},{"Borda","borda"},{"Texto","texto"},{"Texto secundário","secundario"},{"Destaque","destaque"},{"Código","codigo"},{"Status positivo","positivo"}};
  for(String[] campo:campos){LinearLayout l=new LinearLayout(activity);l.setGravity(Gravity.CENTER_VERTICAL);
   TextView rot=texto(campo[0],12);l.addView(rot,new LinearLayout.LayoutParams(0,-2,1));EditText cor=new EditText(activity);cor.setSingleLine(true);cor.setTextSize(12);cor.setTextColor(0xffDDD1EA);
   cor.setText(String.format("#%06X",visual.getInt(campo[1],0xffB59AFF)&0xffffff));l.addView(cor,new LinearLayout.LayoutParams(d(110),-2));
   Button aplicarCor=botao("Aplicar",()->{try{int c=Color.parseColor(cor.getText().toString());visual.edit().putInt(campo[1],c).apply();if("destaque".equals(campo[1]))acento=c;atualizarVisual();}catch(Exception e){Toast.makeText(activity,"Cor inválida. Use #RRGGBB.",Toast.LENGTH_SHORT).show();}});aplicarCor.setTextSize(11);l.addView(aplicarCor);principal.addView(l);
  }LinearLayout cores=new LinearLayout(activity);cores.setOrientation(LinearLayout.HORIZONTAL);for(int c:new int[]{0xffB59AFF,0xff77CCD6,0xffF6A1AE,0xff96D8B9,0xffD9B794}){Button chip=new Button(activity);chip.setText("●");chip.setTextColor(c);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(54),1);cores.addView(chip,lp);chip.setOnClickListener(v->{acento=c;visual.edit().putInt("destaque",c).apply();atualizarVisual();});}principal.addView(cores);
  controle(principal,"Tamanho do texto (%)",85,155,visual.getInt("escala",100),v->visual.edit().putInt("escala",v).apply());
  controle(principal,"Arredondamento",0,28,visual.getInt("raio",0),v->visual.edit().putInt("raio",v).apply());
  controle(principal,"Espaço entre linhas",0,12,visual.getInt("espacamento",0),v->visual.edit().putInt("espacamento",v).apply());
  controle(principal,"Tamanho do cabeçalho",15,28,visual.getInt("cabecalho",19),v->visual.edit().putInt("cabecalho",v).apply());
  controle(principal,"Largura do balão do usuário",0,70,visual.getInt("margem_usuario",32),v->visual.edit().putInt("margem_usuario",v).apply());
  controle(principal,"Altura máxima do campo de texto (linhas)",2,10,visual.getInt("linhas",5),v->visual.edit().putInt("linhas",v).apply());
  principal.addView(texto("Fonte",15));String[] fontes={"Manrope", "Sans-serif do Android", "Serif"};int escolhido=visual.getInt("fonte",0);Spinner fonte=new Spinner(activity);ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,fontes);fonte.setAdapter(adapter);fonte.setSelection(Math.max(0,Math.min(escolhido,fontes.length-1)));fonte.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){if(position==visual.getInt("fonte",0))return;visual.edit().putInt("fonte",position).apply();atualizarVisual();}});principal.addView(fonte);
  Switch editar=new Switch(activity);editar.setText("Editar textos por toque prolongado");editar.setTextColor(0xffEEE8F3);editar.setChecked(visual.getBoolean("editar_textos",false));editar.setOnCheckedChangeListener((v,b)->{visual.edit().putBoolean("editar_textos",b).apply();atualizarVisual();});principal.addView(editar);
  Switch compacto=new Switch(activity);compacto.setText("Área de escrita compacta");compacto.setTextColor(0xffEEE8F3);compacto.setChecked(visual.getBoolean("compacto",true));compacto.setOnCheckedChangeListener((v,b)->{visual.edit().putBoolean("compacto",b).apply();atualizarVisual();});principal.addView(compacto);
  principal.addView(texto("Comportamento da interface",15));
  Switch iniciar= new Switch(activity);iniciar.setText("Abrir detalhes da atividade automaticamente");iniciar.setTextColor(0xffEEE8F3);iniciar.setChecked(visual.getBoolean("etapas_abertas",false));iniciar.setOnCheckedChangeListener((b,v)->{visual.edit().putBoolean("etapas_abertas",v).apply();atualizarVisual();});principal.addView(iniciar);
  principal.addView(texto("Textos editáveis",15));editarTexto(principal,"Nome do aplicativo","cabeçalho","Metrys");editarTexto(principal,"Mensagem de boas-vindas","boasvindas","O que vamos criar hoje?");editarTexto(principal,"Campo de mensagem","entrada","Pergunte ou peça para criar algo…");
  principal.addView(texto("A edição vale imediatamente. A geração automática de APK exige um compilador e uma assinatura em ambiente seguro e ainda não está integrada a esta tela.",12));
  Button restaurar=botao("Restaurar aparência padrão",()->new AlertDialog.Builder(activity).setTitle("Restaurar visual?").setMessage("Conversas e modelos serão preservados.").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar",(di,i)->{visual.edit().clear().apply();textos.edit().clear().apply();atualizarVisual();dialogo.dismiss();}).show());principal.addView(restaurar);
  Button salvar=botao("Salvar e voltar ao Metrys",()->{atualizar.run();dialogo.dismiss();Toast.makeText(activity,"Aparência salva no aparelho.",Toast.LENGTH_SHORT).show();});principal.addView(salvar);
  ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.addView(principal);
  LinearLayout moldura=coluna();moldura.setBackgroundColor(0xff16121E);moldura.addView(visualizacao);moldura.addView(amostraPainel);moldura.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialogo.setContentView(moldura);dialogo.setOnDismissListener(di->handler.removeCallbacks(aplicarPendente));dialogo.show();Window w=dialogo.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.88f));}
 }
}
