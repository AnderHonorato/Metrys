# Motor JNI atual

llama.cpp fixado em `620d2225cc436ab0b001e3200e590e3b286ef9ab`, CPU ARM64, alinhamento de 16 KiB. `scripts/compilar-motor.sh` recompila a biblioteca: alterar apenas o Java não atualiza o JNI.

Contexto 0 seleciona a capacidade do modelo com limite calculado pela memória disponível, pesos e KV cache; geração 0 usa o espaço restante. A geração preserva marcadores `<think>`/`</think>`, reserva espaço para texto final e informa fim do modelo, contexto cheio ou cancelamento. Literais de marcadores dentro da resposta não reabrem o canal. UTF-8 incompleto é acumulado antes de entregar bytes ao Java.

Papéis system/user/assistant e até 256 mensagens por chamada. Turnos antigos podem sair do contexto ativo, mas permanecem no banco. O cache de inferência é limpo após cada chamada, inclusive erro/cancelamento. Não há treinamento dos pesos.

Testes C++: `verificar_utf8.cpp`, `verificar_canais.cpp`, `verificar_memoria.cpp`. JNI real: `VerificarMotor.java` e os testes em `../../testes/`, com um GGUF informado explicitamente. Evidências e limites em `../../docs/0.6.10-VERIFICACAO.md`.
