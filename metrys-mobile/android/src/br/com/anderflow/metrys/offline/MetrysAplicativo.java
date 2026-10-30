package br.com.anderflow.metrys.offline;

import android.app.Application;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/** Guarda a ultima excecao Java de qualquer processo sem apagar dados privados. */
public final class MetrysAplicativo extends Application {
 @Override public void onCreate(){
  super.onCreate();
  final Thread.UncaughtExceptionHandler anterior=Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler((thread,ex)->{
   try{
    StringWriter sw=new StringWriter();ex.printStackTrace(new PrintWriter(sw));
    String dados="Metrys falhou em "+System.currentTimeMillis()+"\nThread: "+thread.getName()+"\n"+sw;
    byte[] b=dados.getBytes(StandardCharsets.UTF_8);
    if(b.length>45000)dados=dados.substring(0,Math.min(dados.length(),23000));
    File f=new File(getFilesDir(),"relatorio-falhas-metrys.txt");
    try(FileOutputStream out=new FileOutputStream(f,true)){out.write((dados+"\n---\n").getBytes(StandardCharsets.UTF_8));}
   }catch(Throwable ignorado){}
   if(anterior!=null)anterior.uncaughtException(thread,ex);
   else android.os.Process.killProcess(android.os.Process.myPid());
  });
 }
}
