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
 private TextView visualizacao,amostraUsuario,amostraIA,amostraCodigo;private LinearLayout amostraPainel,previewTopo,previewCompositor;
 public EditorVisualMetrys(Activity a,Runnable aplicar){activity=a;atualizar=aplicar;aplicarPendente=()->atualizar.run();visual=a.getSharedPreferences("estudio_visual",Context.MODE_PRIVATE);textos=a.getSharedPreferences("estudio_textos",Context.MODE_PRIVATE);acento=visual.getInt("destaque",acento);}
 private int d(float n){return (int)(n*activity.getResources().getDisplayMetrics().density+.5f);}
 private LinearLayout coluna(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private TextView texto(String s,int tamanho){TextView t=new TextView(activity);t.setText(s);t.setTextSize(tamanho);t.setTextColor(0xffEEE8F3);t.setPadding(d(3),d(6),d(3),d(6));return t;}
 private Button botao(String s,Runnable r){Button b=new Button(activity);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
 private void alternar(LinearLayout root,String titulo,String chave,boolean padrao){
  Switch opcao=new Switch(activity);opcao.setText(titulo);opcao.setTextSize(14);opcao.setTextColor(0xffEBE5F2);opcao.setPadding(d(4),d(8),d(4),d(8));
  opcao.setChecked(visual.getBoolean(chave,padrao));opcao.setOnCheckedChangeListener((v,ativo)->{visual.edit().putBoolean(chave,ativo).apply();atualizarVisual();});root.addView(opcao);
 }
 private String[] ordem(String chave,String padrao){String[] ids=visual.getString(chave,padrao).split(",",-1);return ids.length==2?ids:padrao.split(",");}
 private void trocarOrdem(String chave,String padrao){String[] ids=ordem(chave,padrao);visual.edit().putString(chave,ids[1]+","+ids[0]).apply();atualizarVisual();}
 private void montarBarras(){
  if(previewTopo!=null){previewTopo.removeAllViews();previewTopo.setGravity(Gravity.CENTER_VERTICAL);previewTopo.setPadding(d(9),d(4),d(9),d(4));previewTopo.setBackgroundColor(visual.getInt("fundo",0xff111018));
   TextView menu=texto("☰",18);menu.setTextColor(acento);previewTopo.addView(menu);if(visual.getBoolean("mostrar_marca",true)){TextView marca=texto("M",19);marca.setTextColor(acento);previewTopo.addView(marca);}
   TextView cab=texto(textos.getString("cabeçalho","Metrys"),16);cab.setTextColor(visual.getInt("texto",0xffF8F5FC));previewTopo.addView(cab,new LinearLayout.LayoutParams(0,-2,1));
   for(String acao:ordem("ordem_cabecalho","modelo,novo")){boolean habilitado="modelo".equals(acao)?visual.getBoolean("mostrar_modelo",true):visual.getBoolean("mostrar_novo",true);if(!habilitado)continue;
    TextView btn=texto("modelo".equals(acao)?"◈":"＋",19);btn.setGravity(Gravity.CENTER);btn.setTextColor(acento);previewTopo.addView(btn,new LinearLayout.LayoutParams(d(39),d(38)));
    btn.setOnClickListener(v->trocarOrdem("ordem_cabecalho","modelo,novo"));btn.setOnLongClickListener(v->{android.content.ClipData dados=android.content.ClipData.newPlainText("ordem", "cabecalho");v.startDragAndDrop(dados,new View.DragShadowBuilder(v),null,0);return true;});
    btn.setOnDragListener((v,event)->{if(event.getAction()==android.view.DragEvent.ACTION_DROP){trocarOrdem("ordem_cabecalho","modelo,novo");return true;}return true;});
   }
  }
  if(previewCompositor!=null){previewCompositor.removeAllViews();previewCompositor.setGravity(Gravity.CENTER_VERTICAL);previewCompositor.setPadding(d(10),d(7),d(10),d(7));previewCompositor.setBackgroundColor(visual.getInt("fundo",0xff111018));
   String[] itens=ordem("ordem_compositor","anexo,microfone");boolean inversa="microfone".equals(itens[0]);
   String esquerda=inversa?"microfone":"anexo",direita=inversa?"anexo":"microfone";
   montarFerramenta(esquerda);TextView campo=texto(textos.getString("entrada","Mensagem…"),13);campo.setTextColor(visual.getInt("secundario",0xffBEB5C9));campo.setPadding(d(14),d(9),d(4),d(9));campo.setBackground(fundoPreview(visual.getInt("superficie",0xff24202F),visual.getInt("raio",0)+16));previewCompositor.addView(campo,new LinearLayout.LayoutParams(0,-2,1));
   montarFerramenta(direita);TextView enviar=texto("➤",18);enviar.setTextColor(visual.getInt("fundo",0xff111018));enviar.setGravity(Gravity.CENTER);enviar.setBackground(fundoPreview(acento,14));previewCompositor.addView(enviar,new LinearLayout.LayoutParams(d(35),d(35)));
  }
 }
 private GradientDrawable fundoPreview(int cor,int raio){GradientDrawable d=new GradientDrawable();d.setColor(cor);d.setCornerRadius(d(raio));return d;}
 private void montarFerramenta(String id){if(("microfone".equals(id)&&!visual.getBoolean("mostrar_microfone",true))||("anexo".equals(id)&&!visual.getBoolean("mostrar_anexo",true)))return;
  TextView t=texto("anexo".equals(id)?"＋":"♩",19);t.setTextColor(acento);t.setGravity(Gravity.CENTER);previewCompositor.addView(t,new LinearLayout.LayoutParams(d(34),d(39)));
  t.setContentDescription("Toque ou arraste para mudar ordem das ferramentas");t.setOnClickListener(v->trocarOrdem("ordem_compositor","anexo,microfone"));
  t.setOnLongClickListener(v->{v.startDragAndDrop(android.content.ClipData.newPlainText("ordem","compositor"),new View.DragShadowBuilder(v),null,0);return true;});
  t.setOnDragListener((v,event)->{if(event.getAction()==android.view.DragEvent.ACTION_DROP){trocarOrdem("ordem_compositor","anexo,microfone");return true;}return true;});
 }
 private void atualizarVisual(){if(visualizacao!=null){visualizacao.setTextColor(acento);visualizacao.setTextSize(17*visual.getInt("escala",100)/100f);GradientDrawable bg=new GradientDrawable();bg.setColor(visual.getInt("superficie",0xff24202F));bg.setCornerRadius(d(18+visual.getInt("raio",0)));visualizacao.setBackground(bg);}
  if(amostraPainel!=null){amostraPainel.setBackgroundColor(visual.getInt("fundo",0xff111018));
   if(amostraUsuario!=null){amostraUsuario.setTextColor(visual.getInt("texto",0xffF8F5FC));GradientDrawable b=new GradientDrawable();b.setColor(visual.getInt("superficie",0xff24202F));b.setCornerRadius(d(14+visual.getInt("raio",0)));amostraUsuario.setBackground(b);amostraUsuario.setTextSize(13*visual.getInt("escala",100)/100f);amostraUsuario.setTypeface(Typeface.create(visual.getInt("fonte",0)==2?"serif":"sans-serif",Typeface.NORMAL));}
   if(amostraIA!=null){amostraIA.setTextColor(visual.getInt("texto",0xffF8F5FC));amostraIA.setTextSize(13*visual.getInt("escala",100)/100f);amostraIA.setTypeface(Typeface.create(visual.getInt("fonte",0)==2?"serif":"sans-serif",Typeface.NORMAL));}
   if(amostraCodigo!=null){amostraCodigo.setTextColor(visual.getInt("positivo",0xff91DCC0));amostraCodigo.setBackgroundColor(visual.getInt("codigo",0xff11131B));}}
  montarBarras();handler.removeCallbacks(aplicarPendente);handler.postDelayed(aplicarPendente,180);}
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
  visualizacao=texto("PRÉVIA INTERATIVA  ·  Toque ou arraste os controles",12);visualizacao.setPadding(d(11),d(8),d(11),d(8));previewTopo=new LinearLayout(activity);previewTopo.setOrientation(LinearLayout.HORIZONTAL);previewCompositor=new LinearLayout(activity);previewCompositor.setOrientation(LinearLayout.HORIZONTAL);
  amostraPainel=coluna();amostraPainel.setPadding(d(10),d(7),d(10),d(10));
  amostraUsuario=texto("Uma mensagem de exemplo",13);amostraUsuario.setPadding(d(12),d(10),d(12),d(10));amostraPainel.addView(amostraUsuario);
  amostraIA=texto("Uma resposta do Metrys sem card desnecessário.",13);amostraIA.setPadding(d(3),d(10),d(3),d(10));amostraPainel.addView(amostraIA);
  amostraCodigo=texto("$ python main.py   •  Código",13);amostraCodigo.setTypeface(Typeface.MONOSPACE);amostraCodigo.setPadding(d(12),d(10),d(12),d(10));amostraPainel.addView(amostraCodigo);
  atualizarVisual();
  principal.addView(texto("LAYOUT MODULAR",17));
  principal.addView(texto("Toque ou arraste ações na prévia para mudar sua ordem. Você também pode ocultar os botões que não usa.",12));
  alternar(principal,"Mostrar símbolo no cabeçalho","mostrar_marca",true);
  alternar(principal,"Mostrar atividade abaixo do título","mostrar_status",false);
  alternar(principal,"Mostrar botão para trocar de modelo","mostrar_modelo",true);
  alternar(principal,"Mostrar botão de nova conversa","mostrar_novo",true);
  alternar(principal,"Mostrar anexos junto ao campo","mostrar_anexo",true);
  alternar(principal,"Mostrar microfone junto ao campo","mostrar_microfone",true);
  controle(principal,"Altura do cabeçalho (dp)",48,80,visual.getInt("altura_cabecalho",56),v->visual.edit().putInt("altura_cabecalho",v).apply());
  controle(principal,"Margens do campo (dp)",4,30,visual.getInt("margem_compositor",12),v->visual.edit().putInt("margem_compositor",v).apply());
  principal.addView(texto("TEMAS E CORES",17));
  principal.addView(texto("Paleta de superfícies",13));
  int[][] tons={
   {0xff111018,0xff24202F,0xff373042,0xffF8F5FC},{0xff0E1725,0xff1B2940,0xff344C68,0xffF3F7FF},
   {0xff201318,0xff3C2430,0xff573A4C,0xffFFF3F5},{0xff151C19,0xff263A31,0xff3E5A4B,0xffF0FFF5},
   {0xffF5F3F9,0xffFFFFFF,0xffE2DCEB,0xff251C31},{0xff171A1F,0xff2B3037,0xff404957,0xffF4F7FF},
   {0xff211B14,0xff382D21,0xff53442E,0xffFFF6DF},{0xff0B0A10,0xff1B1727,0xff35274D,0xffFFFFFF},
   {0xffFCF8F2,0xffEFE8DF,0xffD4C7B9,0xff332821},{0xff051A20,0xff10333B,0xff2D5961,0xffEAFBFC},
   {0xff160F24,0xff291C3B,0xff433053,0xffF8ECFF},{0xff101010,0xff242424,0xff505050,0xffFAFAFA}};
  String[] nomes={"Violeta","Oceano","Rosa","Floresta","Claro","Grafite","Areia","Noite","Linho","Abismo","Orquídea","Alto contraste"};
  for(int linha=0;linha<3;linha++){LinearLayout grupo=new LinearLayout(activity);grupo.setOrientation(LinearLayout.HORIZONTAL);
   for(int j=linha*4;j<linha*4+4;j++){final int k=j;Button b=botao(nomes[j],()->{visual.edit().putInt("fundo",tons[k][0]).putInt("superficie",tons[k][1]).putInt("borda",tons[k][2]).putInt("texto",tons[k][3]).putInt("secundario",k==4||k==8?0xff63586E:0xffBEB5C9).apply();atualizarVisual();});b.setTextSize(11);grupo.addView(b,new LinearLayout.LayoutParams(0,d(52),1));}principal.addView(grupo);}
  principal.addView(texto("Cores personalizadas",15));
  String[][] campos={{"Fundo","fundo"},{"Superfície da mensagem","superficie"},{"Borda","borda"},{"Texto","texto"},{"Texto secundário","secundario"},{"Destaque","destaque"},{"Código","codigo"},{"Status positivo","positivo"}};
  for(String[] campo:campos){LinearLayout l=new LinearLayout(activity);l.setGravity(Gravity.CENTER_VERTICAL);
   TextView rot=texto(campo[0],12);l.addView(rot,new LinearLayout.LayoutParams(0,-2,1));EditText cor=new EditText(activity);cor.setSingleLine(true);cor.setTextSize(12);cor.setTextColor(0xffDDD1EA);
   cor.setText(String.format("#%06X",visual.getInt(campo[1],0xffB59AFF)&0xffffff));
   cor.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int x){}public void afterTextChanged(android.text.Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){String valor=s.toString();if(valor.matches("#[0-9a-fA-F]{6}")){int c=Color.parseColor(valor);if(visual.getInt(campo[1],0)!=c){visual.edit().putInt(campo[1],c).apply();if("destaque".equals(campo[1]))acento=c;atualizarVisual();}}}});
   l.addView(cor,new LinearLayout.LayoutParams(d(110),-2));
   Button aplicarCor=botao("Aplicar",()->{try{int c=Color.parseColor(cor.getText().toString());visual.edit().putInt(campo[1],c).apply();if("destaque".equals(campo[1]))acento=c;atualizarVisual();}catch(Exception e){Toast.makeText(activity,"Cor inválida. Use #RRGGBB.",Toast.LENGTH_SHORT).show();}});aplicarCor.setTextSize(11);l.addView(aplicarCor);principal.addView(l);
  }LinearLayout cores=new LinearLayout(activity);cores.setOrientation(LinearLayout.HORIZONTAL);for(int c:new int[]{0xffB59AFF,0xff77CCD6,0xffF6A1AE,0xff96D8B9,0xffD9B794}){Button chip=new Button(activity);chip.setText("●");chip.setTextColor(c);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(54),1);cores.addView(chip,lp);chip.setOnClickListener(v->{acento=c;visual.edit().putInt("destaque",c).apply();atualizarVisual();});}principal.addView(cores);
  controle(principal,"Tamanho do texto (%)",85,155,visual.getInt("escala",100),v->visual.edit().putInt("escala",v).apply());
  controle(principal,"Arredondamento",0,28,visual.getInt("raio",0),v->visual.edit().putInt("raio",v).apply());
  controle(principal,"Espaço entre linhas",0,12,visual.getInt("espacamento",0),v->visual.edit().putInt("espacamento",v).apply());
  controle(principal,"Tamanho do cabeçalho",15,28,visual.getInt("cabecalho",19),v->visual.edit().putInt("cabecalho",v).apply());
  controle(principal,"Largura do balão do usuário",0,70,visual.getInt("margem_usuario",32),v->visual.edit().putInt("margem_usuario",v).apply());
  controle(principal,"Altura máxima do campo de texto (linhas)",2,10,visual.getInt("linhas",5),v->visual.edit().putInt("linhas",v).apply());
  principal.addView(texto("TIPOGRAFIA E COMPOSIÇÃO",17));principal.addView(texto("Fonte",15));String[] fontes={"Manrope", "Sans-serif do Android", "Serif"};int escolhido=visual.getInt("fonte",0);Spinner fonte=new Spinner(activity);ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,fontes);fonte.setAdapter(adapter);fonte.setSelection(Math.max(0,Math.min(escolhido,fontes.length-1)));fonte.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){if(position==visual.getInt("fonte",0))return;visual.edit().putInt("fonte",position).apply();atualizarVisual();}});principal.addView(fonte);
  Switch editar=new Switch(activity);editar.setText("Editar textos por toque prolongado");editar.setTextColor(0xffEEE8F3);editar.setChecked(visual.getBoolean("editar_textos",false));editar.setOnCheckedChangeListener((v,b)->{visual.edit().putBoolean("editar_textos",b).apply();atualizarVisual();});principal.addView(editar);
  Switch compacto=new Switch(activity);compacto.setText("Área de escrita compacta");compacto.setTextColor(0xffEEE8F3);compacto.setChecked(visual.getBoolean("compacto",true));compacto.setOnCheckedChangeListener((v,b)->{visual.edit().putBoolean("compacto",b).apply();atualizarVisual();});principal.addView(compacto);
  principal.addView(texto("COMPORTAMENTO DA INTERFACE",17));
  Switch iniciar= new Switch(activity);iniciar.setText("Exibir status detalhado automaticamente");iniciar.setTextColor(0xffEEE8F3);iniciar.setChecked(visual.getBoolean("etapas_abertas",true));iniciar.setOnCheckedChangeListener((b,v)->{visual.edit().putBoolean("etapas_abertas",v).apply();atualizarVisual();});principal.addView(iniciar);
  principal.addView(texto("TEXTOS EDITÁVEIS",17));editarTexto(principal,"Nome do aplicativo","cabeçalho","Metrys");editarTexto(principal,"Mensagem de boas-vindas","boasvindas","Como posso ajudar?");editarTexto(principal,"Campo de mensagem","entrada","Pergunte ou peça para criar algo…");
  principal.addView(texto("As alterações são aplicadas e salvas na hora, sem reiniciar a conversa. Compilar um APK no próprio celular continua indisponível; isso depende de um serviço de compilação seguro.",12));
  Button restaurar=botao("Restaurar aparência padrão",()->new AlertDialog.Builder(activity).setTitle("Restaurar visual?").setMessage("Conversas e modelos serão preservados.").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar",(di,i)->{visual.edit().clear().apply();textos.edit().clear().apply();atualizarVisual();dialogo.dismiss();}).show());principal.addView(restaurar);
  Button salvar=botao("Salvar e voltar ao Metrys",()->{atualizar.run();dialogo.dismiss();Toast.makeText(activity,"Aparência salva no aparelho.",Toast.LENGTH_SHORT).show();});principal.addView(salvar);
  ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.addView(principal);
  LinearLayout moldura=coluna();moldura.setBackgroundColor(0xff16121E);moldura.addView(visualizacao);moldura.addView(previewTopo);moldura.addView(amostraPainel);moldura.addView(previewCompositor);moldura.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialogo.setContentView(moldura);dialogo.setOnDismissListener(di->handler.removeCallbacks(aplicarPendente));dialogo.show();Window w=dialogo.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,-1);}
 }
}
