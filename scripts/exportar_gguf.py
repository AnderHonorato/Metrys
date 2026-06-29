# scripts/exportar_gguf.py
# Criado por Anderson Honorato
# Exporta o modelo treinado para formato GGUF usando llama.cpp

import os, sys, subprocess
from dotenv import load_dotenv

load_dotenv()

NOME_IA = os.getenv('NOME_IA', 'Metrys').lower()
DIR_MODELO = 'modelo_treinado'
DIR_SAIDA = 'modelo_final'

os.makedirs(DIR_SAIDA, exist_ok=True)

# Caminho para o script de conversao do llama.cpp
LLAMACPP_DIR = os.path.join(os.path.dirname(__file__), '..', 'llama.cpp')
CONVERT_SCRIPT = os.path.join(LLAMACPP_DIR, 'convert_hf_to_gguf.py')

def baixar_llamacpp():
    """Clona o llama.cpp se nao existir"""
    if os.path.exists(CONVERT_SCRIPT):
        print('llama.cpp ja encontrado.')
        return True

    print('Baixando llama.cpp...')
    result = subprocess.run(
        ['git', 'clone', '--depth', '1', 'https://github.com/ggerganov/llama.cpp.git'],
        cwd=os.path.join(os.path.dirname(__file__), '..'),
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        print(f'Erro ao clonar: {result.stderr}')
        print('Tentando metodo alternativo com pip install...')
        return False
    return True

def exportar_gguf():
    if not os.path.exists(os.path.join(DIR_MODELO, 'config.json')):
        print(f'ERRO: Modelo treinado nao encontrado em {DIR_MODELO}/')
        print('Rode treinar_modelo.py primeiro.')
        sys.exit(1)

    arq_saida = os.path.join(DIR_SAIDA, f'{NOME_IA}_q4.gguf')

    if not baixar_llamacpp():
        print('Nao foi possivel obter llama.cpp.')
        print('Voce pode converter manualmente com:')
        print('  1. git clone https://github.com/ggerganov/llama.cpp.git')
        print(f'  2. python llama.cpp/convert_hf_to_gguf.py {DIR_MODELO} --outfile {arq_saida}')
        print('  3. llama.cpp/quantize.exe {arq_saida} Q4_K_M')
        return

    print('Convertendo para GGUF...')
    result = subprocess.run(
        [sys.executable, CONVERT_SCRIPT, DIR_MODELO,
         '--outfile', arq_saida,
         '--outtype', 'f16'],
        capture_output=True, text=True,
    )
    if result.returncode == 0:
        print(f'Modelo GGUF exportado: {arq_saida}')
    else:
        print(f'Erro na conversao: {result.stderr}')
        print(f'Stdout: {result.stdout}')

if __name__ == '__main__':
    exportar_gguf()
