#!/usr/bin/env python3
"""Compila instrumentação sem dependências externas; usa classes do build de UI."""
import argparse, subprocess, zipfile
from pathlib import Path

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--ferramentas',type=Path,required=True);parser.add_argument('--saida',type=Path,required=True);args=parser.parse_args()
bt=args.ferramentas/'sdk/build-tools/36.0.0';android=args.ferramentas/'sdk/platforms/android-36/android.jar';jdk=args.ferramentas/'jdk-17.0.20.1+1/bin'
work=root/'build/testes-android';work.mkdir(parents=True,exist_ok=True)
classes=work/'classes';classes.mkdir(exist_ok=True);dex=work/'dex';dex.mkdir(exist_ok=True)
def run(*cmd):subprocess.run([str(x) for x in cmd],check=True)
run(jdk/'javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',str(android)+':'+str(bt/'core-lambda-stubs.jar'),'-cp',root/'build/apk/classes','-d',classes,root/'testes/android/TesteInterface.java')
run(jdk/'java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--min-api','28','--lib',android,'--classpath',root/'build/apk/classes','--output',dex,*classes.rglob('*.class'))
run(bt/'aapt2','link','--manifest',root/'testes/android/AndroidManifest.xml','-I',android,'-o',work/'testes.apk')
with zipfile.ZipFile(work/'testes.apk','a') as z:
 for f in dex.glob('*.dex'):z.write(f,f.name)
args.saida.parent.mkdir(parents=True,exist_ok=True)
run(bt/'zipalign','-f','-P','16','4',work/'testes.apk',args.saida)
