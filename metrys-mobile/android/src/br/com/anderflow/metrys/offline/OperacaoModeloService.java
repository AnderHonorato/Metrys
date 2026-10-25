package br.com.anderflow.metrys.offline;
import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.os.*;
/** Notificação visível exclusivamente durante um download solicitado pelo usuário. */
public final class OperacaoModeloService extends Service{
 private static final int ID=4105;private static final String CANAL="modelo-local";
 private static Notification aviso(Context c,long n,long t){
  int icon=c.getResources().getIdentifier("metrys","drawable",c.getPackageName());PendingIntent abrir=PendingIntent.getActivity(c,1,new Intent(c,TelaOffline.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent pausar=PendingIntent.getService(c,2,new Intent(c,OperacaoModeloService.class).setAction("pausar"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(c,CANAL).setSmallIcon(icon).setContentTitle("Metrys · modelo no aparelho").setContentText("Baixando para usar sem internet").setContentIntent(abrir).setOnlyAlertOnce(true).setOngoing(true).setProgress(1000,t>0?(int)Math.min(1000,n*1000/t):0,t<=0).addAction(new Notification.Action.Builder(icon,"Pausar",pausar).build()).build();
 }
 @Override public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CANAL,"Download do modelo",NotificationManager.IMPORTANCE_LOW));if(Build.VERSION.SDK_INT>=29)startForeground(ID,aviso(this,0,0),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);else startForeground(ID,aviso(this,0,0));}
 @Override public int onStartCommand(Intent i,int flags,int id){if(i!=null&&"pausar".equals(i.getAction())){ServicoOffline.obter(this).cancelarDownload();stopSelf();}return START_NOT_STICKY;}
 @Override public IBinder onBind(Intent i){return null;}
 public static void progresso(Context c,long n,long t){c.getSystemService(NotificationManager.class).notify(ID,aviso(c,n,t));}
}
