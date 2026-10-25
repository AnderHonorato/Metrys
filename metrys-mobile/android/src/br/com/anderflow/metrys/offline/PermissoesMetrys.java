package br.com.anderflow.metrys.offline;

import android.content.Context;
/** Política obrigatória aplicada também na camada de ferramenta, não somente em botões. */
public final class PermissoesMetrys {
 public static final String SOLICITAR="solicitar",APENAS_TEXTO="apenas_texto",ACESSO_TOTAL="acesso_total";
 private final Context app;
 public PermissoesMetrys(Context c){app=c.getApplicationContext();}
 public String modo(){BancoMetrys db=new BancoMetrys(app);try{String s=db.configuracao("modo_acesso");return s.isEmpty()?SOLICITAR:s;}finally{db.close();}}
 public void definir(String modo){if(!SOLICITAR.equals(modo)&&!APENAS_TEXTO.equals(modo)&&!ACESSO_TOTAL.equals(modo))throw new IllegalArgumentException("Perfil de acesso inválido.");BancoMetrys db=new BancoMetrys(app);try{db.configuracao("modo_acesso",modo);}finally{db.close();}}
 public void exigirEscrita(boolean confirmado){String modo=modo();if(APENAS_TEXTO.equals(modo))throw new SecurityException("Modo Apenas texto: escrita em arquivos bloqueada.");if(!confirmado)throw new SecurityException("A escrita exige confirmação explícita e arquivo concedido pelo Android.");}
}
