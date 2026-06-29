# executar_tudo.py
# Criado por Anderson Honorato
# Script mestre - executa todo o pipeline de treino do inicio ao fim
# Nunca para sozinho. So CTRL+C cancela.

import subprocess, sys, time, os, shutil

DIR_BASE = os.path.dirname(os.path.abspath(__file__))
PYTHON_VENV = os.path.join(DIR_BASE, 'venv_ia', 'Scripts', 'python.exe')
PYTHON = PYTHON_VENV if os.path.exists(PYTHON_VENV) else sys.executable

try:
    from colorama import Fore, Style, init
    init(autoreset=True)
    CORES = True
except ImportError:
    CORES = False
    class Fore:
        CYAN = ''; GREEN = ''; RED = ''; YELLOW = ''
    class Style:
        RESET_ALL = ''

ETAPAS = [
    ('Gerando dataset de identidade',  'scripts/gerar_identidade.py'),
    ('Gerando dataset de destilacao',  'scripts/gerar_destilacao.py'),
    ('Gerando dataset de programacao', 'scripts/gerar_programacao.py'),
    ('Gerando dataset de portugues',   'scripts/gerar_portugues.py'),
    ('Criando banco de dados',         'scripts/criar_banco.py'),
    ('Exportando banco para JSONL',    'scripts/exportar_banco.py'),
    ('Mesclando todos os datasets',    'scripts/mesclar_datasets.py'),
    ('Treinando o modelo',             'scripts/treinar_modelo.py'),
    ('Exportando para GGUF',           'scripts/exportar_gguf.py'),
]

def executar_etapa(descricao, script):
    tentativa = 1
    while True:
        print(f'\n{Fore.CYAN}[{tentativa}a tentativa] {descricao}...{Style.RESET_ALL}')
        resultado = subprocess.run([PYTHON, script])
        if resultado.returncode == 0:
            print(f'{Fore.GREEN}Etapa concluida: {descricao}{Style.RESET_ALL}')
            return
        elif resultado.returncode == 2:
            print(f'{Fore.RED}Erro fatal em: {descricao} - abortando pipeline.{Style.RESET_ALL}')
            sys.exit(2)
        else:
            print(f'{Fore.RED}Erro em: {descricao} - aguardando 30s e tentando novamente...{Style.RESET_ALL}')
            tentativa += 1
            time.sleep(30)

def main():
    print(f'{Fore.YELLOW}==============================================')
    print(f'{Fore.YELLOW}  Metrys - Pipeline de Treinamento')
    print(f'{Fore.YELLOW}  Criado por Anderson Honorato')
    print(f'{Fore.YELLOW}=============================================={Style.RESET_ALL}')
    print('Pressione CTRL+C a qualquer momento para cancelar com seguranca.\n')

    try:
        # Remove checkpoints de treinos antigos com datasets diferentes
        dir_treino = 'modelo_treinado'
        if os.path.isdir(dir_treino):
            for item in os.listdir(dir_treino):
                if item.startswith('checkpoint-'):
                    shutil.rmtree(os.path.join(dir_treino, item))
                    print(f'{Fore.YELLOW}Removido checkpoint antigo: {item}{Style.RESET_ALL}')

        for descricao, script in ETAPAS:
            executar_etapa(descricao, script)

        print(f'\n{Fore.GREEN}==============================================')
        print(f'{Fore.GREEN}  PIPELINE COMPLETO! Seu modelo Metrys esta pronto!')
        print(f'{Fore.GREEN}=============================================={Style.RESET_ALL}')
        print('\nProximos passos:')
        print('  1. Instale Ollama: https://ollama.ai/download')
        print('  2. Configure o modelo:')
        print('     cd modelo_final')
        print('     ollama create metrys -f Modelfile')
        print('  3. Execute: ollama run metrys')
    except KeyboardInterrupt:
        print(f'\n{Fore.YELLOW}Cancelado pelo usuario. Progresso salvo.{Style.RESET_ALL}')

if __name__ == '__main__':
    main()
