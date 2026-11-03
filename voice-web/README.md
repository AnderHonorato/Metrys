# Metrys — conversa com anel reativo de voz
Prévia web do **Metrys Ateliê** com Voice Ring integrado à interface do chat. Abra `/` para ver o Metrys e toque no microfone no cabeçalho; o anel abre **sobre o chat**. `/anel.html` continua disponível para o laboratório isolado.

## O que funciona
- Navegação e personalização da interface Metrys Ateliê, em uma cópia dos assets da WebView Android (somente apresentação).
- Botão de voz no cabeçalho: abre/fecha sobreposição React mantendo o histórico visual da página.
- Anel com estados idle/listening/processing/speaking e ajustes de cor, direção, velocidade, sensibilidade, tamanho, curvatura e pausa.
- Microfone real para intensidade do anel; fala de demonstração usando a síntese de voz do navegador.
- Ditado com Web Speech Recognition quando disponível e inserção do texto reconhecido no campo de chat, via mensagens de mesma origem.
- Build Vite com duas entradas, a interface principal e o laboratório de anel. HTTPS obrigatório para microfone.

## Limitações
- **Ainda não é uma nova versão do APK**. O aplicativo Android existente abre a página HTTPS; o código nativo e o motor de IA local não foram alterados nesta etapa.
- A versão web não acessa os modelos GGUF nem as conversas privadas do Android: não é uma IA funcional no navegador.
- O reconhecimento de voz depende da compatibilidade do navegador e pode utilizar serviços externos do provedor do navegador; não é garantidamente offline. O áudio captado pelo `useMicrophone` no anel não é enviado à nossa aplicação/backend.
- O visualizador de fala usa nível de demonstração, não mede amplitude real da voz sintetizada.

## Desenvolvimento
```bash
cd voice-web
npm install
npm run dev
npm run build
```
Código React no `src/`, interface Ateliê copiada em `public/metrys/` e ponte web em `public/metrys/integracao-web.js`. Não sobrescrever automaticamente `metrys-mobile/android/` com esta versão: a compilação Android requer integração própria, motor local, permissões e assinatura.
