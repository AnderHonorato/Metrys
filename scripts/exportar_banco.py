# scripts/exportar_banco.py
# Criado por Anderson Honorato
# Le o banco de dados e exporta como dataset de treinamento

import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.criar_banco import engine, ConhecimentoBase
from sqlalchemy.orm import sessionmaker

Sessao = sessionmaker(bind=engine)

def exportar_para_jsonl():
    sessao = Sessao()
    registros = sessao.query(ConhecimentoBase).all()
    sessao.close()

    arquivo_saida = 'datasets/dataset_dominio.jsonl'
    with open(arquivo_saida, 'w', encoding='utf-8') as f:
        for r in registros:
            exemplo = {'messages': [
                {'role': 'user', 'content': r.pergunta},
                {'role': 'assistant', 'content': r.resposta},
            ]}
            f.write(json.dumps(exemplo, ensure_ascii=False) + '\n')

    print(f'{len(registros)} registros exportados para {arquivo_saida}')

if __name__ == '__main__':
    exportar_para_jsonl()
