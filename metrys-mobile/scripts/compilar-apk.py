#!/usr/bin/env python3
"""Compila a interface offline preservando os DEX originais e as fontes."""
import argparse,hashlib,json,os,secrets,shutil,subprocess,zipfile
from pathlib import Path
RAIZ=Path(__file__).resolve().parents[1]
def rodar(*args,capturar=False):
 r=subprocess.run([str(a) for a in args],check=True,text=True,stdout=subprocess.PIPE if capturar else None,stderr=subprocess.STDOUT if capturar else None)
 return r.stdout or ''
def sha(p):
 with open(p,'rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--ferramentas',type=Path,default=RAIZ.parent/'ferramentas');p.add_argument('--saida',type=Path,default=RAIZ/'entrega/Metrys-0.6.10.apk');p.add_argument('--qa-ui',action='store_true');p.add_argument('--variante',choices=('previa','original'),default='original',help='Prévia instala ao lado da versão atual; original exige chave de atualização');p.add_argument('--sem-assinatura',action='store_true',help='Gera pacote para assinatura privada; não é instalável');a=p.parse_args()
 bt=a.ferramentas/'sdk/build-tools/36.0.0';platform=a.ferramentas/'sdk/platforms/android-36/android.jar';jdk=a.ferramentas/'jdk-17.0.20.1+1/bin';base=RAIZ/'base/Metrys-Mobile-0.4.1.apk'
 if sha(base)!='858caf4b39d8bd77c8bb2d41d7288698ba6021f17a85c8a0f11e33d447be86a4':raise RuntimeError('APK base diferente do auditado.')
 w=RAIZ/'build/apk';shutil.rmtree(w,ignore_errors=True);w.mkdir(parents=True)
 for n in ['classes','dex','gerados']:(w/n).mkdir()
 recursos_fonte=w/'res';shutil.copytree(RAIZ/'android/res',recursos_fonte)
 # Os ZIPs distribuídos não incluem TTFs. Recupera as fontes do APK base auditado
 # para manter também os IDs usados pelos DEX legados.
 with zipfile.ZipFile(base) as original:
  for nome in ('lora','manrope'):
   destino=recursos_fonte/'font'/f'{nome}.ttf'
   if not destino.is_file():
    destino.parent.mkdir(parents=True,exist_ok=True);destino.write_bytes(original.read(f'res/font/{nome}.ttf'))
 rodar(bt/'aapt2','compile','--dir',recursos_fonte,'-o',w/'recursos.zip')
 manifesto=RAIZ/'android/AndroidManifest.xml'
 pacote='br.com.anderflow.metrys.offline'
 if a.variante=='previa':
  conteudo=manifesto.read_text(encoding='utf-8').replace('android:label="Metrys Offline"','android:label="Metrys 0.6 Prévia"')
  manifesto=w/'AndroidManifest.xml';manifesto.write_text(conteudo,encoding='utf-8')
  pacote+='.previa'
 opcoes=['--rename-manifest-package',pacote,'--custom-package','br.com.anderflow.metrys.offline'] if a.variante=='previa' else []
 rodar(bt/'aapt2','link','-o',w/'base.apk','--manifest',manifesto,'-I',platform,'--java',w/'gerados','-A',RAIZ/'android/assets',*opcoes,w/'recursos.zip')
 fontes=list((RAIZ/'android/src').rglob('*.java'))+list((w/'gerados').rglob('*.java'))
 rodar(jdk/'javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',str(platform)+os.pathsep+str(bt/'core-lambda-stubs.jar'),'-d',w/'classes',*fontes)
 rodar(jdk/'java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','28','--lib',platform,'--output',w/'dex',*list((w/'classes').rglob('*.class')))
 legados={}
 with zipfile.ZipFile(base) as antigo,zipfile.ZipFile(w/'base.apk','a') as novo:
  for n in antigo.namelist():
   if n.endswith('.dex'):
    b=antigo.read(n);novo.writestr(n,b,compress_type=zipfile.ZIP_DEFLATED);legados[n]=hashlib.sha256(b).hexdigest()
  for i,f in enumerate(sorted((w/'dex').glob('*.dex'))):novo.write(f,'classes'+str(4+i)+'.dex',compress_type=zipfile.ZIP_DEFLATED)
  if not a.qa_ui:
   libs=list((RAIZ/'android/jniLibs/arm64-v8a').glob('*.so'))
   if not libs:raise RuntimeError('Motor ARM64 ausente.')
   for f in libs:novo.write(f,'lib/arm64-v8a/'+f.name,compress_type=zipfile.ZIP_DEFLATED)
 if a.sem_assinatura:
  rodar(bt/'zipalign','-f','-P','16','4',w/'base.apk',w/'alinhado.apk')
  a.saida.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(w/'alinhado.apk',a.saida)
  rodar(bt/'zipalign','-c','-P','16','4',a.saida)
  assinatura='SEM ASSINATURA: requer certificado original antes de instalar.'
 else:
  priv=RAIZ/'android/.assinatura';priv.mkdir(mode=0o700,exist_ok=True);priv.chmod(0o700);key=priv/'metrys-offline.jks';senha=priv/'senha.txt'
  if key.exists() and not senha.exists():raise RuntimeError('Chave existente sem senha; restaure backup privado.')
  if not key.exists():
   if a.variante=='original':raise RuntimeError('Assinatura original não disponível: configure certificado compatível antes de compilar para atualização.')
   senha.write_text(secrets.token_urlsafe(36));senha.chmod(0o600)
   rodar(jdk/'keytool','-genkeypair','-keystore',key,'-storetype','JKS','-storepass:file',senha,'-keypass:file',senha,'-alias','metrys-offline','-keyalg','RSA','-keysize','3072','-validity','20000','-dname','CN=Ander Flow, OU=Metrys Offline, O=Ander Flow, C=BR')
  key.chmod(0o600)
  rodar(bt/'zipalign','-f','-P','16','4',w/'base.apk',w/'alinhado.apk');a.saida.parent.mkdir(parents=True,exist_ok=True)
  rodar(jdk/'java','-jar',bt/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','metrys-offline','--ks-pass','file:'+str(senha),'--v1-signing-enabled','true','--v2-signing-enabled','true','--v3-signing-enabled','true','--v4-signing-enabled','false','--out',a.saida,w/'alinhado.apk')
  assinatura=rodar(jdk/'java','-jar',bt/'lib/apksigner.jar','verify','--verbose','--print-certs',a.saida,capturar=True);rodar(bt/'zipalign','-c','-P','16','4',a.saida)
 manifesto=rodar(bt/'aapt2','dump','badging',a.saida,capturar=True);recursos=rodar(bt/'aapt2','dump','resources',a.saida,capturar=True)
 with zipfile.ZipFile(a.saida) as z:
  for n,s in legados.items():
   if hashlib.sha256(z.read(n)).hexdigest()!=s:raise RuntimeError('DEX legado alterado: '+n)
  if any(x in n for n in z.namelist() for x in ['.jks','.keystore','senha.txt']):raise RuntimeError('Material privado no APK.')
  if len(json.loads(z.read('assets/modelo.json'))['partes'])!=12:raise RuntimeError('Catálogo Drive incompleto.')
 # O APK prévia possui identidade/pacote de recursos próprios e abre o modo legado no app original instalado.
 # A exigência de IDs idênticos só se aplica à variante original (atualização no mesmo pacote).
 if a.variante=='original':
  for r in ['0x7f010000','0x7f020000','0x7f020001','0x7f030000']:
   if r not in recursos:raise RuntimeError('ID recurso legado não preservado: '+r)
 else:
  print('Prévia paralela: conferência de IDs antigos dispensada; modo conectado abre o Metrys original externo.')
 rel={'apk':a.saida.name,'bytes':a.saida.stat().st_size,'sha256':sha(a.saida),'abi':'qa-sem-motor' if a.qa_ui else 'arm64-v8a','dex_legados':legados,'assinatura':assinatura,'manifesto':manifesto,'modelo_externo':True,'variante':a.variante,'application_id':pacote,'assinado':not a.sem_assinatura,'assinatura_efemera':not a.sem_assinatura and a.variante=='previa','teste_aparelho_fisico':False}
 a.saida.with_suffix('.verificacao.json').write_text(json.dumps(rel,ensure_ascii=False,indent=2));print(json.dumps({k:rel[k] for k in ['apk','bytes','sha256']}))
if __name__=='__main__':main()
