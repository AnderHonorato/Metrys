# Metrys Viva 0.6.17 — interface Android

O Metrys utiliza uma UI local HTML/CSS/JavaScript em `android/assets/viva/`, servida por `file:///android_asset/viva/index.html` dentro de um `WebView` isolado, conectada por `@JavascriptInterface` ao `ServicoOffline` existente. Os dados, o banco de conversas SQLite v4, o identificador do aplicativo e o modelo salvo no armazenamento interno são mantidos.

## Origem visual e créditos

Os mecanismos de cenário ASCII `cenarios/engine.js`, `cenarios/themes.js`, `cenarios/themes2.js` e `cenarios/themes3.js` são adaptações/redistribuições dos arquivos do projeto de código aberto *Claudinho* de Lu1sR0, licenciado sob MIT, na revisão `6e05356f5fe17761ee8fbadfd78af0eefe1d2740` (repositório https://github.com/Lu1sR0/claudinho). A licença original foi preservada em `android/assets/licencas/claudinho-MIT.txt`. Elementos de navegação, identidade/mascote Metrys e ponte Android foram implementados separadamente.

## Recursos desta etapa

- 15 cenários ASCII animados, interação por toque e configuração de desempenho e velocidade.
- Menu lateral com conversas/projetos, tema e personalização; chat responsivo para Android e desktop.
- Mascote pixel Metrys com reação e arrasto; temas claros e escuros; chat flutuante sobre cenário.
- Respostas sem balão da IA, balão do usuário discreto; ações de copiar, compartilhar, avaliar e refazer.
- Blocos de código com aparência de terminal e prévia HTML em iframe isolado.
- Pensamento **publicado pelo modelo** em painel expansível; histórico local de títulos/painéis.
- Envio por fila existente no `ServicoOffline`, sem encerrar a geração; botão independente de parar.
- Modelo offline e arquivos guardados no armazenamento original do aplicativo.

## Limitações e garantias

Esta versão NÃO transforma automaticamente o modelo local em um agente de computador Windows, não instala Electron no Android, nem reproduz ferramentas do Claude Agent SDK. Visualizações e animações da versão Windows foram adaptadas para WebView, não copiados os processos Electron, suas extensões ou funcionalidades proprietárias. PNG/PDF/ZIP enviados no seletor não são interpretados pelo modelo exclusivamente textual; anexos de texto até 1 MB são aceitos. A geração de ZIPs completos não é feita por este editor. Os arquivos de código podem ser exportados pelo seletor Android.

A segurança de instalação depende da manutenção do certificado de assinatura original. Não apagar dados, nem alterar `br.com.anderflow.metrys.offline`, nem reduzir o `versionCode`, nem rebaixar SQLite versão 4. Em caso de crash do motor, preservar o diagnóstico de erro. `0.6.17` usa `versionCode=27`.

## QA

Executar `python scripts/testar-evolucao.py`, `node --check android/assets/viva/aplicativo.js`, teste Playwright `testar_visual.py`, compilação Android SDK 36 com Javac e D8, instalação no emulador e teste no Galaxy A35. Validação em emulador sem modelo verifica a interface e o app; testes de resposta com GGUF no Galaxy ainda são necessários.
