# scripts/testar_conexao.py
# Criado por Anderson Honorato
# Testa se o Nex-N2-Pro esta acessivel via OpenRouter

import os, requests
from dotenv import load_dotenv

load_dotenv()

def testar_teacher():
    chave = os.getenv('OPENROUTER_API_KEY')
    modelo = os.getenv('MODELO_TEACHER')

    if not chave or chave == 'sk-or-v1-SUA_CHAVE_AQUI':
        print('AVISO: API Key do OpenRouter nao configurada no .env')
        print('Edite o arquivo .env e insira sua chave.')
        return

    print(f'Testando conexao com {modelo}...')
    resposta = requests.post(
        'https://openrouter.ai/api/v1/chat/completions',
        headers={
            'Authorization': f'Bearer {chave}',
            'Content-Type': 'application/json',
        },
        json={
            'model': modelo,
            'messages': [{'role': 'user', 'content': 'Diga: conexao OK'}],
            'max_tokens': 50,
        },
        timeout=30,
    )

    print('STATUS:', resposta.status_code)
    if resposta.status_code == 200:
        dados = resposta.json()
        print('RESPOSTA:', dados['choices'][0]['message']['content'])
        print('TEACHER conectado com sucesso!')
    else:
        print('ERRO:', resposta.text)

if __name__ == '__main__':
    testar_teacher()
