# Registro histórico da base anterior

Este relatório veio do ZIP de referência e não descreve o motor da 0.6.10.
A verificação atual está em [0.6.10-VERIFICACAO.md](../../docs/0.6.10-VERIFICACAO.md).

Resultados executados novamente após a reconstrução dos arquivos nesta sessão.

- Compilação Android ARM64 com NDK r29: **exit 0, 52/52 etapas** após configuração.
- Artefato: `android/jniLibs/arm64-v8a/libmetrys.so`, **6.143.960 bytes**.
- SHA-256: `0bbc4b1f616cad4b83fc332041db7fe2fe27af4624282a7d8ba9efd1b2bc373a`.
- ELF: AArch64; três segmentos LOAD com alinhamento `0x4000` (16 KiB).
- Dependências: `libm.so`, `libdl.so`, `libc.so`; STL embutida estaticamente.
- Teste UTF-8: todas as fronteiras de fragmentação de acentos, emoji e ideogramas; bytes inválidos e sequência incompleta final; **passou**.
- Compilação host JNI e execução JVM com `-Xcheck:jni`: **exit 0, sem avisos JNI**.

Modelo real usado: Qwen2.5-Coder-1.5B-Instruct Q4_K_M, 1.117.320.768 bytes, SHA-256 `cc324af070c2ecbfd324a30884d2f951a7ff756aba85cb811a6ec436933bb046`, baixado do release publicado no repositório do usuário.

Saída real da geração:

```python
def soma(a, b):
    return a + b
```

- 16 callbacks de streaming; geração em 2.155 ms **no host de compilação**.
- Cancelamento durante decodificação CPU: retorno em 3 ms no mesmo host; código de aborto 2 tratado conforme API.
- Segundo envio com histórico: respondeu `5` a `2 + 3`.
- Contexto excedido: rejeitado antes de decodificar.
- Exceção do callback Java: preservada e motor reutilizável.
- `close()` chamado duas vezes: sem falha.

Não houve teste em Android físico. A execução host não comprova latência, consumo, instalação ou fluidez no Galaxy A35. A validação do APK deve ser registrada separadamente. Logs de execução ficam no diretório de ferramentas e os testes reproduzíveis acompanham estes fontes.
