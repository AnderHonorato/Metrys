# Metrys · Voice Ring (prévia React)
Tela de voz React 19 + Vite com biblioteca oficial `voice-ring` (Libraries.dev).
```bash
cd voice-web
npm install
npm run dev
```
O microfone só é ativado por clique e precisa de HTTPS/localhost. A tela oferece quatro estados, fala de demonstração, emoção, tema e direção simulada. **É uma prévia da interface**, ainda sem transcrição, envio de áudio, integração com o motor offline ou vínculo ao APK Android.

Arquitetura atual: o Metrys Android é um app Java com WebView em `metrys-mobile/android/`, não React. Para incluir este módulo offline no APK será necessário empacotar o build web nos assets, servi-lo por uma origem confiável e integrar permissões nativas; `file://` não oferece acesso confiável a `getUserMedia`.
