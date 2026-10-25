#!/usr/bin/env bash
set -euo pipefail
mkdir -p entrega/qa
adb install -r entrega/qa-ui.apk
adb install -r entrega/qa-instrumentacao.apk
adb logcat -c
adb shell am instrument -w -r br.com.anderflow.metrys.offline.test/br.com.anderflow.metrys.offline.TesteInterface | tee entrega/qa/instrumentacao.txt
adb logcat -d -v threadtime > entrega/qa/logcat.txt
grep -q 'INSTRUMENTATION_CODE: -1' entrega/qa/instrumentacao.txt
if grep -E 'FATAL EXCEPTION|Unable to start activity|VerifyError|NoClassDefFoundError' entrega/qa/logcat.txt; then exit 1; fi
adb pull /sdcard/Android/data/br.com.anderflow.metrys.offline/files/metrys-qa.png entrega/qa/captura.png
