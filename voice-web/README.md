# Metrys — Online + Offline + voz

Interface web do **Metrys**, integrada ao chat e ao anel de voz. Na versão 0.6.19, o Metrys também pode consultar o Kimi K3 remotamente, sem baixar os pesos de 1,56 TB. O app Android mantém o motor local anterior, que funciona sem internet quando houver modelo instalado.

**Site:** https://metrys-voice-ring.vercel.app/

## Como usar a nuvem

1. Abra o chat do Metrys no site ou o APK 0.6.19.
2. Toque em **Offline** para mudar para **Online**.
3. Abra os ajustes da nuvem pelo seletor do modelo. Crie um token de acesso em https://huggingface.co/settings/tokens com autorização para Inference Providers.
4. Cole o token no campo protegido. Confira antes os custos em https://huggingface.co/docs/inference-providers/pricing
5. Envie uma mensagem. O modelo base usado no modo remoto é **moonshotai/Kimi-K3**, via roteamento **:cheapest** do Hugging Face.

Não há treino de pesos nem execução do Kimi K3 por CPU local neste modo. O código de personalidade, o histórico online e a memória são controlados pelo Metrys; a inferência ocorre no serviço de terceiros.

## Como o Metrys aprende com você

- Toque no seletor de modelos **dentro do modo online** para editar a personalidade.
- Use **/lembrar Quero que você responda em português** ou o gerenciador para guardar uma preferência.
- Edite e exclua as memórias a qualquer momento; também há exportação em JSON.
- As memórias e conversas são armazenadas **somente no dispositivo atual**, em armazenamento local do app ou navegador, sem sincronização entre dispositivos.
- O modelo remoto recebe o histórico online recente e as memórias selecionadas **quando você envia uma pergunta**. O histórico do modo offline não é enviado automaticamente.

Não grave senhas, documentos sigilosos ou informações sensíveis em memórias sem avaliar os riscos de armazenamento local e transmissão ao provedor.

## Segurança e custos

- **Android**: token de API cifrado com AES-GCM e protegido por Android Keystore; o app faz requisições HTTPS diretamente ao Hugging Face.
- **Web**: token permanece somente na memória da aba, não no localStorage. Cada mensagem passa por uma função do próprio site em /api/metrys, que a transmite ao Hugging Face. A função não configura token global e não persiste o token.
- Não armazene credenciais no GitHub, nas memórias ou em commits.
- É sua responsabilidade conferir cotas e custos; mensagens enviadas pela API podem ser cobradas. A interface não prevê um limite de gastos por conta.
- O modo offline não baixa pesos extras quando você escolhe o modo online. Os pesos offline já instalados ficam preservados.

## Instalador

A workflow **.github/workflows/compilar-metrys-0.6.19-nuvem.yml** produz um APK de teste assinado, sob o nome Metrys, com pacote `br.com.anderflow.metrys.offline.previa`. Isso permite instalar ao lado da versão original sem precisar da chave de assinatura privada usada nas versões anteriores. A assinatura efêmera é diferente a cada execução: para substituir uma prévia por outra, é necessário desinstalar a prévia anterior, o que apaga os dados dessa prévia. **O APK ainda não foi testado fisicamente num Galaxy A35.**

## Desenvolvimento

```bash
cd voice-web
npm install
npm run dev
npm run build
```

A API serverless em `voice-web/api/metrys.js` requer deploy no Vercel para funcionar; no Vite local, o caminho /api não está disponível automaticamente. O anel interativo permanece em `/anel.html`. O reconhecimento de voz depende do navegador e pode usar serviços externos.
