# Metrys Android 0.6.10

Continuação do aplicativo offline existente: mesmo pacote `br.com.anderflow.metrys.offline`, versão interna 19. A assinatura de atualização deve ser a mesma da 0.6.9. Não desinstale nem limpe dados para atualizar: modelos e conversas permanecem no armazenamento privado existente.

Esta árvore torna o código Android diretamente revisável no repositório. A base 0.6.8 e o patch 0.6.9 foram recuperados dos URLs e SHA-256 fixados no workflow anterior `compilar-metrys-0.6.9-chat.yml`; os problemas e alterações estão em [docs/0.6.10-VERIFICACAO.md](docs/0.6.10-VERIFICACAO.md).

## Alterações

- Geração dinâmica, contexto calculado pela memória e transição do canal de atividade para resposta final.
- Atividade preservada por pergunta, painel expansível e títulos a partir do conteúdo gerado, sem etapas simuladas.
- Fila persistente, retomada sem perguntas duplicadas e regeneração que conserva a versão anterior se falhar.
- Controles visuais efetivos, expansão e anexos preservados, recursos/fontes compatíveis com os DEX anteriores.

## Testar e compilar

JDK 17, SDK platform 36, build-tools 36.0.0, NDK 29.0.14206865, CMake e Ninja. O workflow `.github/workflows/metrys-atual.yml` configura as ferramentas e executa testes no Android.

```bash
python3 scripts/testar-evolucao.py
c++ -std=c++17 android/cpp/verificar_canais.cpp -o /tmp/metrys-canais
/tmp/metrys-canais
c++ -std=c++17 android/cpp/verificar_memoria.cpp -o /tmp/metrys-memoria
/tmp/metrys-memoria
bash scripts/compilar-motor.sh
python3 scripts/compilar-apk.py --ferramentas /caminho/ferramentas --sem-assinatura --saida entrega/Metrys-0.6.10-SEM-ASSINATURA.apk
```

O pacote sem assinatura é para assinatura privada, não para instalação. Sem `--sem-assinatura`, o compilador exige o certificado existente em `android/.assinatura/`; não cria uma assinatura nova para a variante original. Nunca publique essa pasta. O layout de ferramentas do script usa `sdk/` e `jdk-17.0.20.1+1/` (pode ser um link para um JDK 17 instalado).

Para testes reais do JNI no servidor, compile CMake sem toolchain Android, apontando `LLAMA_SOURCE_DIR` para o commit fixado; execute `ContextoDinamicoTeste`, `RaciocinioMotorTeste` e `VerificarMotor` com um GGUF compatível. Testes host não equivalem a teste físico ARM64.

O modelo GGUF continua externo ao APK. As licenças do llama.cpp, Qwen e fontes acompanham `android/assets/licencas/`. Os três DEX legados vêm do APK base auditado, cujo código Kotlin original completo não integra esta árvore.

## Pendências explícitas

Editor com posicionamento completamente livre de componentes, compilação de APK no celular, treinamento dos pesos por feedback e execução autônoma de projetos não estão implementados. A qualidade e velocidade dependem do modelo escolhido e do aparelho. Consulte o relatório de verificação para distinguir testes executados de limitações.
