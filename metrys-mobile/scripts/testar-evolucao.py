#!/usr/bin/env python3
"""Testes reproduzíveis sem Android SDK: histórico Java, sintaxe e DDL SQLite.
Não equivalem a executar o APK no Android.
"""
from pathlib import Path
import re, sqlite3, subprocess, tempfile, xml.etree.ElementTree as ET

RAIZ=Path(__file__).resolve().parents[1]
FONTES=list((RAIZ/'android/src').rglob('*.java'))
def rodar(args):
    r=subprocess.run([str(x) for x in args],text=True,capture_output=True)
    if r.returncode:
        raise AssertionError('Falha em '+ ' '.join(map(str,args))+'\n'+r.stdout+'\n'+r.stderr)
    return r.stdout
with tempfile.TemporaryDirectory(prefix='metrys-qa-') as pasta:
    temp=Path(pasta)
    rodar(['javac','-encoding','UTF-8','-d',temp,RAIZ/'testes/AnalisarSintaxe.java'])
    print(rodar(['java','-cp',temp,'AnalisarSintaxe',*FONTES]).strip())
    rodar(['javac','-encoding','UTF-8','-d',temp,RAIZ/'android/src/br/com/anderflow/metrys/offline/HistoricoLocal.java',RAIZ/'testes/HistoricoLocalTeste.java'])
    print(rodar(['java','-cp',temp,'br.com.anderflow.metrys.offline.HistoricoLocalTeste']).strip())
    codigo=(RAIZ/'android/src/br/com/anderflow/metrys/offline/BancoMetrys.java').read_text()
    instrucoes=re.findall(r'db\.execSQL\("([^"\\]*(?:\\.[^"\\]*)*)"\);',codigo)
    assert len(instrucoes)>=12, f'DDL incompleto: {len(instrucoes)}'
    banco=sqlite3.connect(':memory:')
    banco.execute('PRAGMA foreign_keys=ON')
    for ddl in instrucoes:
        banco.execute(ddl)
    assert len(banco.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").fetchall())>=8
    banco.execute("INSERT INTO projetos(id,nome,criado_em) VALUES ('p','Projeto',1)")
    banco.execute("INSERT INTO conversas(id,projeto_id,titulo,criado_em,atualizado_em) VALUES ('c','p','Teste',1,1)")
    banco.execute("INSERT INTO mensagens(conversa_id,papel,texto,criada_em) VALUES ('c','user','Olá % e _',2)")
    banco.execute("INSERT INTO memorias(id,projeto_id,origem_conversa_id,conteudo,criada_em) VALUES ('m','p','c','lembrança explícita',3)")
    assert banco.execute('SELECT count(*) FROM mensagens').fetchone()[0]==1
    try:
        banco.execute("INSERT INTO mensagens(conversa_id,papel,texto,criada_em) VALUES ('nao_existe','user','x',3)")
        raise AssertionError('FK inválida permitida')
    except sqlite3.IntegrityError:pass
    termo=r'%'
    escapado='%'+termo.replace('\\','\\\\').replace('%',r'\%').replace('_',r'\_')+'%'
    assert banco.execute("SELECT count(*) FROM mensagens WHERE texto LIKE ? ESCAPE '\\'",(escapado,)).fetchone()[0]==1
    banco.execute("DELETE FROM conversas WHERE id='c'")
    assert banco.execute('SELECT count(*) FROM mensagens').fetchone()[0]==0
    assert banco.execute('SELECT count(*) FROM memorias').fetchone()[0]==0
    banco.execute('PRAGMA integrity_check')
    assert banco.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
    print(f'PASS SQLite: {len(instrucoes)} DDLs; FKs, cascata, LIKE literal e integridade')
    m=ET.parse(RAIZ/'android/AndroidManifest.xml').getroot()
    ns='{http://schemas.android.com/apk/res/android}'
    assert m.attrib[ns+'versionCode'].isdigit()
    assert m.attrib['package']=='br.com.anderflow.metrys.offline'
    print('PASS manifesto: pacote e versionCode')
print('Limite: testes instrumentados e compilação Android requerem SDK, assinatura e dispositivo.')
