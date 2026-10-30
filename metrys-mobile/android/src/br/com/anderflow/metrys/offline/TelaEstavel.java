package br.com.anderflow.metrys.offline;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Entrada independente. Nunca carrega IA, banco gravavel ou conversa automaticamente. */
public final class TelaEstavel extends Activity {
 private int FUNDO=0xff141219, PAINEL=0xff24202c, ROXO=0xffc7a5ef, TEXTO=0xfff7f4fa, SEC=0xffbfb6c8;
 private TextView estado,diagnostico;private LinearLayout raiz;
 private int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+0.5f);}
 private GradientDrawable fundo(int cor,int raio){GradientDrawable d=new GradientDrawable();d.setColor(cor);d.setCornerRadius(dp(raio));return d;}
 private TextView texto(String t,int tam,int cor){TextView v=new TextView(this);v.setText(t);v.setTextColor(cor);v.setTextSize(tam);v.setIncludeFontPadding(false);v.setLineSpacing(dp(4),1.12f);return v;}
 private Button botao(String nome){Button b=new Button(this);b.setText(nome);b.setAllCaps(false);b.setTextColor(FUNDO);b.setTextSize(15);b.setBackground(fundo(ROXO,16));return b;}
 private void espaco(int valor){View v=new View(this);raiz.addView(v,new LinearLayout.LayoutParams(1,dp(valor)));}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  android.content.SharedPreferences tema=getSharedPreferences("estudio_visual",MODE_PRIVATE);
  FUNDO=tema.getInt("fundo",FUNDO);PAINEL=tema.getInt("superficie",PAINEL);ROXO=tema.getInt("destaque",ROXO);
  TEXTO=tema.getInt("texto",TEXTO);SEC=tema.getInt("secundario",SEC);
  getWindow().setStatusBarColor(FUNDO);getWindow().setNavigationBarColor(FUNDO);
  // A entrada fica independente do motor e de consultas SQL automáticas.
  LinearLayout tela=new LinearLayout(this);tela.setOrientation(LinearLayout.VERTICAL);tela.setBackgroundColor(FUNDO);tela.setPadding(dp(20),dp(20),dp(20),dp(17));
  setContentView(tela);raiz=tela;
  LinearLayout cab=new LinearLayout(this);cab.setGravity(Gravity.CENTER_VERTICAL);cab.setOrientation(LinearLayout.HORIZONTAL);
  FitaView icone=new FitaView(this,ROXO);cab.addView(icone,new LinearLayout.LayoutParams(dp(32),dp(32)));
  TextView marca=texto("Metrys",23,TEXTO);marca.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
  LinearLayout.LayoutParams marcaLp=new LinearLayout.LayoutParams(0,-2,1);marcaLp.leftMargin=dp(11);cab.addView(marca,marcaLp);
  TextView menu=texto("•••",23,SEC);menu.setContentDescription("Opções e diagnóstico");menu.setPadding(dp(12),dp(9),dp(5),dp(9));
  menu.setOnClickListener(v->mostrarOpcoes());cab.addView(menu);tela.addView(cab);
  View separador=new View(this);separador.setBackgroundColor(0xff302a37);LinearLayout.LayoutParams divisor=new LinearLayout.LayoutParams(-1,dp(1));divisor.topMargin=dp(19);tela.addView(separador,divisor);
  View respiro=new View(this);tela.addView(respiro,new LinearLayout.LayoutParams(1,0,1));
  LinearLayout centro=new LinearLayout(this);centro.setOrientation(LinearLayout.VERTICAL);centro.setPadding(dp(2),0,dp(2),0);
  TextView nome=texto("Sua próxima ideia começa aqui.",28,TEXTO);nome.setTypeface(Typeface.create("serif",Typeface.NORMAL));centro.addView(nome);
  TextView descricao=texto("Continue uma conversa ou comece outra.",14,SEC);LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(-1,-2);dlp.topMargin=dp(12);centro.addView(descricao,dlp);
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(26);tela.addView(centro,cp);
  Button abrir=botao("Abrir conversa  →");abrir.setContentDescription("Abrir conversa com a IA");tela.addView(abrir,new LinearLayout.LayoutParams(-1,dp(54)));
  abrir.setOnClickListener(v->abrirConversa());
  Button historico=botao("Conversas salvas");historico.setTextColor(TEXTO);historico.setBackground(fundo(PAINEL,16));
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(50));hp.topMargin=dp(9);tela.addView(historico,hp);
  historico.setOnClickListener(v->mostrarHistorico());
  View respiroBaixo=new View(this);tela.addView(respiroBaixo,new LinearLayout.LayoutParams(1,0,1));
  LinearLayout inferior=new LinearLayout(this);inferior.setOrientation(LinearLayout.HORIZONTAL);inferior.setGravity(Gravity.CENTER_VERTICAL);
  TextView status=texto("Metrys · v0.6.16",12,SEC);inferior.addView(status,new LinearLayout.LayoutParams(0,-2,1));
  TextView diagnosticar=texto("Diagnóstico",12,ROXO);diagnosticar.setPadding(dp(8),dp(12),dp(1),dp(12));diagnosticar.setOnClickListener(v->mostrarOpcoes());inferior.addView(diagnosticar);
  tela.addView(inferior);
  // Compatibilidade com o relatório existente: relatório aparece somente a pedido.
 }
 private void mostrarHistorico(){
  ScrollView sc=new ScrollView(this);LinearLayout lista=new LinearLayout(this);lista.setPadding(dp(18),dp(14),dp(18),dp(14));lista.setOrientation(LinearLayout.VERTICAL);sc.addView(lista);
  try{mostrarConversas(lista);}catch(Throwable ex){lista.addView(texto("Não foi possível listar agora. Seus dados permanecem salvos.",14,SEC));}
  new android.app.AlertDialog.Builder(this).setTitle("Conversas salvas").setView(sc).setPositiveButton("Fechar",null).show();
 }
 private void mostrarOpcoes(){
  String[] opcoes={"Abrir conversa", "Conversas salvas", "Copiar último erro", "Sobre o Metrys"};
  new android.app.AlertDialog.Builder(this).setTitle("Meu espaço").setItems(opcoes,(dialogo,indice)->{
   if(indice==0)abrirConversa();
   else if(indice==1)mostrarHistorico();
   else if(indice==2){String dado=lerRelatorio();if(dado.isEmpty())dado=motivoUltimaSaida();
    ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("Metrys - diagnóstico",dado));
    android.widget.Toast.makeText(this,"Diagnóstico copiado",android.widget.Toast.LENGTH_SHORT).show();
   }else new android.app.AlertDialog.Builder(this).setTitle("Metrys v0.6.16").setMessage("O processamento e o histórico permanecem no dispositivo. O motor de IA é carregado apenas ao abrir uma conversa.").setPositiveButton("Entendi",null).show();
  }).show();
 }
 @Override public void onResume(){super.onResume();}
 private void abrirConversa(){try{
  Intent i=new Intent();i.setClassName(getPackageName(),"br.com.anderflow.metrys.offline.TelaOffline");startActivity(i);
 }catch(Throwable e){android.widget.Toast.makeText(this,"Não foi possível abrir o chat: "+e.getClass().getSimpleName(),android.widget.Toast.LENGTH_LONG).show();}}
 private void mostrarConversas(LinearLayout resumo){File base=getDatabasePath("metrys.db");if(!base.isFile()){resumo.addView(texto("Nenhum histórico SQLite disponível nesta instalação.",13,SEC));return;}
  SQLiteDatabase db=null;
  try{db=SQLiteDatabase.openDatabase(base.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
   try(Cursor c=db.rawQuery("SELECT titulo FROM conversas ORDER BY atualizado_em DESC LIMIT 8",null)){
    int n=0;while(c.moveToNext()){String nome=c.getString(0);if(nome==null||nome.trim().isEmpty())nome="Nova conversa";if(nome.length()>64)nome=nome.substring(0,64)+"…";TextView t=texto("• "+nome,14,TEXTO);t.setPadding(0,dp(7),0,dp(7));resumo.addView(t);n++;}
    if(n==0)resumo.addView(texto("Sem conversas salvas.",13,SEC));
   }
  }finally{if(db!=null)db.close();}
 }
 private String arquivosModelo(){File pasta=new File(getFilesDir(),"modelo");File[] itens=pasta.listFiles((dir,nome)->nome.endsWith(".gguf"));if(itens==null||itens.length==0)return "Nenhum modelo instalado encontrado. Abrir a conversa permite gerenciar downloads.";
  StringBuilder b=new StringBuilder();int n=0;for(File item:itens){if(n++==4)break;b.append("• ").append(item.getName()).append(" · ").append(item.length()/1048576L).append(" MB\n");}return b.toString();}
 private String lerRelatorio(){File f=new File(getFilesDir(),"relatorio-falhas-metrys.txt");if(!f.isFile())return "";try(FileInputStream in=new FileInputStream(f)){long saltar=Math.max(0,f.length()-16000);while(saltar>0){long n=in.skip(saltar);if(n<=0)break;saltar-=n;}byte[] b=new byte[16000];int l=in.read(b);return l>0?new String(b,0,l,StandardCharsets.UTF_8):"";}catch(Throwable ex){return "Erro ao ler diagnóstico: "+ex.getClass().getSimpleName();}}
 private String motivoUltimaSaida(){if(Build.VERSION.SDK_INT<30)return "Sem informação de encerramento do Android";try{
  ActivityManager a=(ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
  List<ApplicationExitInfo> lista=a.getHistoricalProcessExitReasons(getPackageName(),0,8);
  for(ApplicationExitInfo info:lista){if(info.getProcessName()!=null&&info.getProcessName().startsWith(getPackageName()))return "Processo: "+info.getProcessName()+"\nMotivo: "+info.getReason()+"\nDescrição: "+info.getDescription();}
 }catch(Throwable ignore){}return "Nenhuma falha recente encontrada pelo Android";}
 private void atualizarDiagnostico(){String texto=lerRelatorio();if(!texto.isEmpty())diagnostico.setText("Falha registrada pelo aplicativo. Use Copiar último erro para compartilhar o diagnóstico.");else diagnostico.setText("Sem relatório Java salvo. Use Copiar último erro para consultar a última falha do Android.");}
}
