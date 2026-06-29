# scripts/gerar_destilacao.py
# Criado por Anderson Honorato
# Gera dataset de destilacao usando o Nex-N2-Pro como teacher via OpenRouter
# NUNCA para sozinho - retoma automaticamente em caso de erro

import json, os, sys, time, requests
from dotenv import load_dotenv
from tqdm import tqdm
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.temas_treinamento import TEMAS

load_dotenv()

CHAVE = os.getenv('OPENROUTER_API_KEY')
TEACHER = os.getenv('MODELO_TEACHER')
ARQUIVO_SAIDA = 'datasets/dataset_destilacao.jsonl'
LOG_PROGRESSO = 'logs/progresso_destilacao.json'
os.makedirs('logs', exist_ok=True)

def carregar_progresso():
    if os.path.exists(LOG_PROGRESSO):
        with open(LOG_PROGRESSO) as f:
            return json.load(f)
    return {'concluidos': []}

def salvar_progresso(concluidos):
    with open(LOG_PROGRESSO, 'w') as f:
        json.dump({'concluidos': concluidos}, f)

def chamar_teacher(pergunta, tentativa=1):
    while True:
        try:
            resp = requests.post(
                'https://openrouter.ai/api/v1/chat/completions',
                headers={
                    'Authorization': f'Bearer {CHAVE}',
                    'Content-Type': 'application/json',
                    'HTTP-Referer': 'https://github.com/AnderHonorato',
                    'X-Title': 'Metrys Training Pipeline',
                },
                json={
                    'model': TEACHER,
                    'messages': [{'role': 'user', 'content': pergunta}],
                    'max_tokens': 1024,
                },
                timeout=120
            )
            if resp.status_code == 200:
                return resp.json()['choices'][0]['message']['content']
            elif resp.status_code in (401, 403):
                print(f'  ERRO FATAL: HTTP {resp.status_code} - Chave API invalida ou sem permissao.')
                print(f'  Resposta: {resp.text[:500]}')
                sys.exit(2)
            elif resp.status_code == 429:
                print(f'  HTTP 429 (rate limit) - aguardando 60s...')
                time.sleep(60)
            else:
                print(f'  HTTP {resp.status_code} - aguardando 30s e tentando novamente...')
                time.sleep(30)
        except KeyboardInterrupt:
            raise
        except Exception as e:
            print(f'  ERRO: {e} - tentativa {tentativa} - aguardando 60s...')
            tentativa += 1
            time.sleep(60)

def gerar_destilacao():
    progresso = carregar_progresso()
    concluidos = progresso['concluidos']
    pendentes = [t for t in TEMAS if t not in concluidos]

    print(f'Total de temas: {len(TEMAS)} | Ja concluidos: {len(concluidos)} | Pendentes: {len(pendentes)}')

    if not pendentes:
        print('Nada pendente. Destilacao ja esta completa!')
        return

    with open(ARQUIVO_SAIDA, 'a', encoding='utf-8') as f:
        for tema in tqdm(pendentes, desc='Gerando dataset via Teacher'):
            resposta = chamar_teacher(tema)
            exemplo = {'messages': [
                {'role': 'user', 'content': tema},
                {'role': 'assistant', 'content': resposta},
            ]}
            f.write(json.dumps(exemplo, ensure_ascii=False) + '\n')
            f.flush()
            concluidos.append(tema)
            salvar_progresso(concluidos)
            time.sleep(1)

    print(f'Destilacao concluida! Total de exemplos: {len(concluidos)}')

if __name__ == '__main__':
    gerar_destilacao()
