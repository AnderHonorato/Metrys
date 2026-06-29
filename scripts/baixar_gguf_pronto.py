# scripts/baixar_gguf_pronto.py
# Criado por Anderson Honorato
# Baixa o GGUF pronto do Qwen3-0.6B para uso imediato com Ollama
# O modelo Qwen3 ja suporta pensamento (thinking) nativamente

import os, sys
from huggingface_hub import hf_hub_download
from dotenv import load_dotenv

load_dotenv()

NOME_IA = os.getenv('NOME_IA', 'Metrys').lower()
DIR_SAIDA = 'modelo_final'

os.makedirs(DIR_SAIDA, exist_ok=True)

REPO_ID = 'bartowski/Qwen_Qwen3-0.6B-GGUF'
FILENAME = 'Qwen_Qwen3-0.6B-Q4_K_M.gguf'

print(f'Baixando {REPO_ID}/{FILENAME}...')
print(f'Tamanho estimado: ~400MB (modelo 0.6B com Q4_K_M)')

try:
    caminho = hf_hub_download(
        repo_id=REPO_ID,
        filename=FILENAME,
        local_dir=DIR_SAIDA,
        local_dir_use_symlinks=False,
    )
    print(f'GGUF baixado: {caminho}')

    arq_final = os.path.join(DIR_SAIDA, f'{NOME_IA}_q4.gguf')
    if not os.path.exists(arq_final):
        os.rename(caminho, arq_final)
        print(f'Renomeado para: {arq_final}')
    else:
        print(f'Arquivo final ja existe: {arq_final}')

except Exception as e:
    print(f'ERRO: {e}')
    print('Verifique sua conexao com internet e tente novamente.')
    sys.exit(1)

print('\nDownload concluido! Proximos passos:')
print(f'  cd modelo_final')
print(f'  ollama create {NOME_IA} -f Modelfile')
print(f'  ollama run {NOME_IA}')
