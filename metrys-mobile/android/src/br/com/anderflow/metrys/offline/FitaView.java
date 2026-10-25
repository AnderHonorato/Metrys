package br.com.anderflow.metrys.offline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;
import java.lang.reflect.Method;

/** Usa a fita original preservada no DEX; desenho próprio serve como reserva. */
public final class FitaView extends FrameLayout {
 private View original;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int cor;
 private ValueAnimator movimento;private float fase=0;private boolean pedida=false;
 public FitaView(Context c,int cor){super(c);this.cor=cor;setWillNotDraw(false);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  try{
   Class<?> tipo=Class.forName("br.com.anderflow.metrys.EstrelaMetrys");
   original=(View)tipo.getConstructor(Context.class,int.class).newInstance(c,cor);
   addView(original,new FrameLayout.LayoutParams(-1,-1));chamar("setEstilo",String.class,"fita");
   chamar("setModo",String.class,"parada");chamar("setAnimando",boolean.class,false);
  }catch(Exception e){original=null;}
 }
 private void chamar(String nome,Class<?> tipo,Object valor){if(original==null)return;try{Method m=original.getClass().getMethod(nome,tipo);m.invoke(original,valor);}catch(Exception ignored){}}
 public void animar(boolean ativa){pedida=ativa;boolean deve=ativa&&ValueAnimator.areAnimatorsEnabled()&&isShown();
  if(original!=null){chamar("setModo",String.class,deve?"pensando":"parada");chamar("setAnimando",boolean.class,deve);return;}
  if(movimento!=null){movimento.cancel();movimento=null;}fase=0;
  if(deve){movimento=ValueAnimator.ofFloat(0,1);movimento.setDuration(1450);movimento.setRepeatCount(ValueAnimator.INFINITE);movimento.addUpdateListener(a->{fase=(Float)a.getAnimatedValue();invalidate();});movimento.start();}invalidate();
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(pedida)post(()->animar(pedida));}
 @Override protected void onDetachedFromWindow(){if(movimento!=null){movimento.cancel();movimento=null;}chamar("setAnimando",boolean.class,false);super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(original!=null)return;
  c.save();float escala=Math.min(getWidth(),getHeight())/100f;c.translate((getWidth()-100*escala)/2,(getHeight()-100*escala)/2);c.scale(escala,escala);
  float pulso=1f+(float)Math.sin(fase*Math.PI*2)*.025f;c.translate(50,50);c.scale(pulso,pulso);c.translate(-50,-50);
  Path fita=new Path();fita.moveTo(20,78);fita.cubicTo(19,60,19,33,24,24);fita.cubicTo(28,14,43,58,50,59);fita.cubicTo(58,59,72,15,77,24);fita.cubicTo(81,32,80,61,80,78);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(13);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setShader(new LinearGradient(15,15,85,80,new int[]{0xff8B65C8,0xffE4C7FF,cor,0xffFAF0FF,0xff8B65C8},null,Shader.TileMode.CLAMP));c.drawPath(fita,p);p.setShader(null);
  p.setStyle(Paint.Style.FILL);p.setColor(0xffF5E6FF);Path estrela=new Path();estrela.moveTo(88,51);estrela.quadTo(89,57,94,58);estrela.quadTo(89,59,88,65);estrela.quadTo(87,59,82,58);estrela.quadTo(87,57,88,51);c.drawPath(estrela,p);c.restore();
 }
}
