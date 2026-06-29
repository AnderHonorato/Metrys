# scripts/mesclar_datasets.py
# Criado por Anderson Honorato
# Une dataset_identidade + dataset_destilacao + dataset_dominio
# A identidade e repetida 3x para ter mais peso no treinamento

import json, random, os

DATASETS = {
    'identidade':   ('datasets/dataset_identidade.jsonl',   1),
    'destilacao':   ('datasets/dataset_destilacao.jsonl',   1),
    'programacao':  ('datasets/dataset_programacao.jsonl',  1),
    'portugues':    ('datasets/dataset_portugues.jsonl',    1),
    'dominio':      ('datasets/dataset_dominio.jsonl',      1),
}

SAIDA = 'datasets/dataset_final.jsonl'

def carregar_jsonl(caminho):
    if not os.path.exists(caminho):
        print(f'Arquivo nao encontrado: {caminho} - pulando.')
        return []
    with open(caminho, encoding='utf-8') as f:
        return [json.loads(linha) for linha in f if linha.strip()]

def mesclar():
    todos = []
    for nome, (caminho, peso) in DATASETS.items():
        dados = carregar_jsonl(caminho)
        todos.extend(dados * peso)
        print(f'{nome}: {len(dados)} exemplos (x{peso} = {len(dados)*peso})')

    random.shuffle(todos)

    with open(SAIDA, 'w', encoding='utf-8') as f:
        for ex in todos:
            f.write(json.dumps(ex, ensure_ascii=False) + '\n')

    print(f'\nDataset final: {len(todos)} exemplos em {SAIDA}')

if __name__ == '__main__':
    mesclar()
