# Metrys Ateliê 0.6.18 — identidade visual inteiramente nova

## Direção autoral

Sem cópia visual do Claudinho, sem marca antiga Metrys, sem letra M no ícone e sem cenas ASCII de terceiros. Conceito: **ateliê editorial em movimento**. Superfícies de papel, tinta azul-petróleo, coral e verde-água. Marca orbital de três elementos e mascote orgânico (nome de trabalho: Fluxo), feito em Canvas 2D com olhos, reações e arrasto. Elementos se movem com animações suaves e respeitam movimento reduzido.

## Recursos

- Interface WebView offline com navegação lateral, nova conversa, histórico e busca, mensagens e campo de escrita.
- 12 atmosferas originais para claro e escuro, com fundos e movimentos reativos: Maré, Safira, Terracota, Jardim, Sol, Grafite, Gelo, Brasa, Celeste, Flora, Oceano e Amanhecer.
- Editor ao vivo com personalização de cor, transparência, largura do chat, tamanhos de fonte, fundo e intensidade das animações.
- Pensamento publicado pelo modelo em painel expansível; ações abaixo de mensagens; código em bloco semelhante a terminal.
- Modelo offline, banco SQLite versão 4, histórico e projeto existentes **preservados**.
- Ícone Android redesenhado como orbital abstrato, sem letra.

## Arquitetura e garantias

A entrada Android mantém `br.com.anderflow.metrys.offline`, pacote e assinatura originais, versão 0.6.18 / código interno 28. A interface usa exclusivamente arquivos locais `android/assets/viva/`. O motor original e os DEX legados permanecem inalterados. A migração SQLite continua v4; jamais rebaixar o banco de usuários.

A interface não altera a capacidade cognitiva do modelo nem adiciona interpretação de PDF, vídeo ou ZIP, edição WYSIWYG de projetos, treinamento de pesos, nem ferramentas de computador. São funções diferentes que exigem implementação própria. Os arquivos de licença do histórico ficam disponíveis no projeto, mas o renderizador ASCII de terceiros não é carregado nem incluído na nova UI.

## QA

Teste de sintaxe JS, verificações de identidade visual, Playwright em 393×852, 360×740 e 1440×900, incluindo menu, troca de temas, modal e campo de mensagem. Compilação Android Java 8/SDK 36 e D8; assinatura com a chave privada original fora do GitHub; validação estática de upgrade e preservação de motor. Um emulador ARM64 ou o Galaxy A35 ainda é necessário para testar o processamento GGUF real e confirmar a abertura após atualização com dados existentes.
