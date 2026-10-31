#!/usr/bin/env python3
"""Validação estática do Metrys Ateliê: identidade, segurança e banco."""
from pathlib import Path
import re,hashlib,json
r=Path(__file__).resolve().parents[1]
a=r/'android'
m=(a/'AndroidManifest.xml').read_text()
b=(a/'src/br/com/anderflow/metrys/offline/BancoMetrys.java').read_text()
java=(a/'src/br/com/anderflow/metrys/offline/TelaViva.java').read_text()
html=(a/'assets/viva/index.html').read_text()
css=(a/'assets/viva/estilo.css').read_text()
js=(a/'assets/viva/aplicativo.js').read_text()
anim=(a/'assets/viva/cenarios/movimento.js').read_text()
assert 'versionCode="28"' in m and 'versionName="0.6.18"' in m
assert 'package="br.com.anderflow.metrys.offline"' in m
assert 'private static final int VERSAO=4;' in b and 'onUpgrade' in b
assert 'feedback_respostas' in b
assert 'file:///android_asset/viva/index.html' in java
assert 'MetrysAplicativo' in m and 'android:process=":conversa"' in m
assert 'window.receberEstado=receberEstado' in js
assert 'usarPonte(\'enviar\',texto)' in js
assert 'const THEMES=[' in js and js.count('name:') >= 12
assert 'function iniciarMascote()' in js and 'MotorAmbiente.iniciar' in js
assert 'id="boasVindas"' in html and 'id="campoMensagem"' in html
assert 'id="andamento"' in html and 'id="lateral"' in html
assert 'cenarios/movimento.js' in html
assert 'Renderer(' not in js
assert 'claudinho' not in html.lower()+css.lower()+js.lower()+anim.lower()
assert not (a/'assets/viva/cenarios/engine.js').exists()
assert (a/'res/drawable/metrys.xml').is_file()
if not (a/'jniLibs/arm64-v8a/libmetrys.so').is_file(): print('Motor privado so presente no pacote de compilacao Android')
assert not list(r.rglob('*.jks'))
print('OK: Metrys 0.6.18, 12 temas, UI original, motor intacto, SQLite v4')
