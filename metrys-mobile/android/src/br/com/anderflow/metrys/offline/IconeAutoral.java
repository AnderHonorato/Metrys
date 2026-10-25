package br.com.anderflow.metrys.offline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Família vetorial própria; nenhum caractere ou emoji é usado nos controles. */
public final class IconeAutoral extends Drawable {
 private final String tipo; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 public IconeAutoral(String tipo,int cor){this.tipo=tipo;p.setColor(cor);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 private void linha(Canvas c,float...v){Path x=new Path();x.moveTo(v[0],v[1]);for(int i=2;i<v.length;i+=2)x.lineTo(v[i],v[i+1]);c.drawPath(x,p);}
 @Override public void draw(Canvas c){
  Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);
  switch(tipo){
   case "menu":linha(c,4,6,20,6);linha(c,4,12,20,12);linha(c,4,18,20,18);break;
   case "send":linha(c,4,11,20,4,14,20,11,13,4,11);linha(c,11,13,20,4);break;
   case "stop":c.drawRoundRect(6,6,18,18,3,3,p);break;
   case "pause":linha(c,8,5,8,19);linha(c,16,5,16,19);break;
   case "plus":linha(c,12,5,12,19);linha(c,5,12,19,12);break;
   case "close":linha(c,6,6,18,18);linha(c,18,6,6,18);break;
   case "arrow":linha(c,5,12,19,12);linha(c,13,6,19,12,13,18);break;
   case "share":linha(c,6,11,17,5);linha(c,6,13,17,19);c.drawCircle(5,12,2,p);c.drawCircle(18,4,2,p);c.drawCircle(18,20,2,p);break;
   case "like":linha(c,5,11,5,20,2,20,2,11,5,11);linha(c,7,20,19,20,21,12,15,11,14,5,12,4,9,11,7,11,7,20);break;
   case "dislike":linha(c,5,4,5,13,2,13,2,4,5,4);linha(c,7,4,19,4,21,12,15,13,14,19,12,20,9,13,7,13,7,4);break;
   case "copy":c.drawRoundRect(8,8,20,20,3,3,p);linha(c,15,4,7,4,4,7,4,15);break;
   case "download":linha(c,12,3,12,15);linha(c,7,10,12,15,17,10);linha(c,4,16,4,20,20,20,20,16);break;
   case "import":linha(c,4,7,10,7,12,10,20,10,20,20,4,20,4,7);linha(c,15,3,15,14);linha(c,11,10,15,14,19,10);break;
   case "export":linha(c,5,10,5,20,19,20,19,10);linha(c,12,3,12,15);linha(c,7,8,12,3,17,8);break;
   case "folder":linha(c,3,6,9,6,11,9,21,9,19,20,4,20,3,6);linha(c,4,12,20,12);break;
   case "retry":c.drawArc(4,4,20,20,35,280,false,p);linha(c,4,4,4,10,10,10);break;
   case "history":c.drawArc(4,4,20,20,210,320,false,p);linha(c,3,4,3,10,9,10);linha(c,12,7,12,12,16,14);break;
   case "model":c.drawRoundRect(6,6,18,18,4,4,p);c.drawCircle(12,12,2,p);for(int j=8;j<=16;j+=4){linha(c,j,3,j,6);linha(c,j,18,j,21);linha(c,3,j,6,j);linha(c,18,j,21,j);}break;
   case "mic":c.drawRoundRect(9,3,15,15,3,3,p);c.drawArc(6,8,18,19,0,180,false,p);linha(c,12,19,12,22);linha(c,8,22,16,22);break;
   case "expand":linha(c,5,10,5,5,10,5);linha(c,14,5,19,5,19,10);linha(c,5,14,5,19,10,19);linha(c,14,19,19,19,19,14);break;
   case "search":c.drawCircle(10,10,6,p);linha(c,14.5f,14.5f,20,20);break;
   case "bookmark":linha(c,6,4,18,4,18,21,12,17,6,21,6,4);break;
   case "edit":linha(c,4,19,8,18,20,6,17,3,5,15,4,19);break;
   case "archive":c.drawRect(4,7,20,20,p);c.drawRect(3,4,21,7,p);linha(c,9,12,15,12);break;
   case "trash":linha(c,5,6,19,6);linha(c,8,4,16,4);linha(c,7,8,8,20,16,20,17,8);linha(c,10,11,10,17);linha(c,14,11,14,17);break;
   case "tools":linha(c,4,20,10,14);linha(c,14,10,20,4);linha(c,4,4,8,4,11,8,8,11,4,8,4,4);linha(c,13,16,16,13,21,18,18,21,13,16);break;
   default:c.drawCircle(12,12,8,p);c.drawCircle(12,12,2,p);break;
  }
  c.restore();
 }
 @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter filtro){p.setColorFilter(filtro);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 @Override public int getIntrinsicWidth(){return 24;}
 @Override public int getIntrinsicHeight(){return 24;}
}
