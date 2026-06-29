# scripts/gerar_portugues.py
# Criado por Anderson Honorato
# Gera dataset sintetico de portugues brasileiro
# Foco em acentuacao, gramatica e escrita formal

import json, os

CONHECIMENTO = [
    # === Acentuacao ===
    {
        'pergunta': 'Quando usar acento agudo em portugues?',
        'resposta': 'O acento agudo (´) indica vogal tonica aberta: '
                    'cafe, voce, avo, pe, so. Usado nas vogais a, e, o. '
                    'Exemplos: sabia (verbo) vs sabia (substantivo), '
                    'fabrica (substantivo) vs fabrica (verbo).'
    },
    {
        'pergunta': 'Quando usar acento circunflexo em portugues?',
        'resposta': 'O acento circunflexo (^) indica vogal tonica fechada: '
                    'voce, onibus, mes, portugues, tres. Usado nas vogais a, e, o. '
                    'Ajuda a diferenciar palavras como tem (singular) e tem (plural).'
    },
    {
        'pergunta': 'O que e crase e quando usar?',
        'resposta': 'Crase (`a` + `a` = a) e a fusao da preposicao "a" com '
                    'o artigo "a". Use antes de palavras femininas: '
                    '"Vou a escola", "Referente a proposta". '
                    'NUNCA use antes de: palavras masculinas, verbos, '
                    'pronomes pessoais, plural generico ("a vendas").'
    },
    {
        'pergunta': 'Qual a diferenca entre por e por?',
        'resposta': '"Por" (sem acento) e preposicao: "Fiz por voce". '
                    '"Por" (com acento) e verbo: "Ele vai por o livro na mesa". '
                    'Regra simples: se pode substituir por "colocar", use acento.'
    },
    {
        'pergunta': 'Quando usar tem e tem (com acento)?',
        'resposta': '"Tem" (sem acento): 3a pessoa do singular - "Ele tem um carro". '
                    '"Tem" (com acento circunflexo): 3a pessoa do plural - '
                    '"Eles tem dois carros". O acento diferencia singular do plural.'
    },
    {
        'pergunta': 'Qual a diferenca entre esta e esta?',
        'resposta': '"Esta" (sem acento): pronome demonstrativo feminino - '
                    '"Esta caneta e azul". '
                    '"Esta" (com acento): verbo estar, 3a pessoa do singular - '
                    '"Ela esta feliz hoje".'
    },
    {
        'pergunta': 'Quando usar veem e vem?',
        'resposta': '"Vem" (sem acento): 3a pessoa do singular - "Ele vem amanha". '
                    '"Veem" (com acento): 3a pessoa do plural - "Eles veem o filme". '
                    'A reforma ortografica de 2009 manteve o acento em "veem".'
    },
    {
        'pergunta': 'Quais palavras perderam acento com a reforma ortografica?',
        'resposta': 'Ideia (antes: ideia), assembleia (antes: assembleia), '
                    'heroico (antes: heroico), joia (antes: joia), '
                    'plateia (antes: plateia), europeia (antes: europeia). '
                    'Ditongos abertos ei e oi em paroxitonas perderam acento.'
    },
    {
        'pergunta': 'Qual a diferenca entre para e para?',
        'resposta': '"Para" (sem acento): preposicao - "Isto e para voce". '
                    '"Para" (com acento): verbo parar, 3a pessoa - "Ela para o carro". '
                    'A reforma de 2009 manteve o acento diferencial em "para/para".'
    },

    # === Pontuacao ===
    {
        'pergunta': 'Como usar a virgula corretamente em portugues?',
        'resposta': 'Use virgula para: 1) separar itens em lista; '
                    '2) isolar vocativo ("Joao, venha ca"); '
                    '3) isolar adjunto adverbial deslocado ("Ontem, choveu muito"); '
                    '4) separar oracoes coordenadas com conjuncao; '
                    '5) isolar apostos explicativos. '
                    'NUNCA separe sujeito do verbo com virgula.'
    },
    {
        'pergunta': 'Quando usar ponto e virgula?',
        'resposta': 'Ponto e virgula (;) indica pausa maior que virgula e menor '
                    'que ponto. Use para: 1) separar itens complexos em listas; '
                    '2) separar oracoes coordenadas longas; '
                    '3) antes de conjuncoes adversativas como "porem" e "contudo".'
    },

    # === Gramatica ===
    {
        'pergunta': 'O que e concordancia verbal?',
        'resposta': 'O verbo concorda em numero (singular/plural) e pessoa (1a, 2a, 3a) '
                    'com o sujeito. Ex: "Os alunos estudam" (plural), '
                    '"O aluno estuda" (singular). Sujeito composto antes do verbo: '
                    'verbo no plural. Sujeito composto depois: pode concordar com o mais proximo.'
    },
    {
        'pergunta': 'O que e concordancia nominal?',
        'resposta': 'Adjetivos, artigos, numerais e pronomes concordam em genero '
                    '(masculino/feminino) e numero (singular/plural) com o substantivo. '
                    'Ex: "As casas bonitas" (feminino plural). '
                    'Quando ha mais de um substantivo, o adjetivo concorda com o mais proximo ou vai ao plural.'
    },
    {
        'pergunta': 'Explique a diferenca entre mas e mais.',
        'resposta': '"Mas" (sem i): conjuncao adversativa, equivalente a "porem" - '
                    '"Queria ir, mas choveu". '
                    '"Mais" (com i): adverbio de intensidade, oposto de menos - '
                    '"Quero mais cafe".'
    },
    {
        'pergunta': 'Quando usar mal ou mau?',
        'resposta': '"Mal": adverbio (oposto de bem) ou substantivo - '
                    '"Ele dormiu mal", "O mal do seculo". '
                    '"Mau": adjetivo (oposto de bom) - "Ele e um mau motorista". '
                    'Dica: troque por "bom"/"bem" para testar qual usar.'
    },
    {
        'pergunta': 'Qual a diferenca entre ha e a?',
        'resposta': '"Ha": verbo haver indicando tempo passado - '
                    '"Cheguei ha duas horas", "Havia muitas pessoas". '
                    '"A": preposicao, artigo ou pronome (tempo futuro) - '
                    '"Estamos a dois dias do evento", "A casa e bonita".'
    },
    {
        'pergunta': 'Explique os porques da lingua portuguesa.',
        'resposta': '"Por que": inicio de pergunta ou "por qual motivo" - '
                    '"Por que voce foi?" '
                    '"Porque": resposta/explicacao (pois) - "Fui porque quis". '
                    '"Por que": final de frase antes de pontuacao - "Voce sabe por que?" '
                    '"Porquê": substantivo (motivo) - "Nao sei o porquê disso".'
    },
    {
        'pergunta': 'Como escrever um email profissional em portugues?',
        'resposta': 'Estrutura: 1) Saudacao adequada ("Prezado Sr. Silva"); '
                    '2) Contexto breve no primeiro paragrafo; '
                    '3) Desenvolvimento claro e objetivo; '
                    '4) Encerramento com call-to-action; '
                    '5) Assinatura com nome, cargo e contato. '
                    'Use linguagem formal, evite girias e revise a ortografia.'
    },

    # === Ortografia ===
    {
        'pergunta': 'Qual a regra para usar S ou Z em portugues?',
        'resposta': 'Regras para S: adjetivos de origem/posse (frances, portugues), '
                    'palavras derivadas de verbos com ND/RT (prender > preso). '
                    'Regras para Z: sufixo -izar de verbos (realizar, atualizar), '
                    'substantivos abstratos de adjetivos (belo > beleza).'
    },
    {
        'pergunta': 'Quando usar X ou CH em portugues?',
        'resposta': 'Use CH: depois de ditongo (caixa, feixe nao seguem esta regra), '
                    'palavras de origem latina com PL/CL/FL (chave < clave). '
                    'Use X: depois de ditongo (caixa, peixe), depois de EN inicial '
                    '(enxame, enxugar), palavras indigenas e africanas (xodo, xingar).'
    },
    {
        'pergunta': 'Qual a diferenca entre sessoes, secoes e cessoes?',
        'resposta': '"Sessoes": reunioes, periodos de atividade - '
                    '"Sessao de cinema". '
                    '"Secoes": divisoes, partes, departamentos - '
                    '"Secao de esportes do jornal". '
                    '"Cessoes": ato de ceder, transferir - '
                    '"Cessao de direitos autorais".'
    },
    {
        'pergunta': 'Quando usar onde e aonde?',
        'resposta': '"Onde": lugar fixo, sem movimento - "Onde voce mora?" '
                    '"Aonde": lugar com movimento, destino - "Aonde voce vai?" '
                    'Dica: "aonde" = "a" + "onde", indica direcao.'
    },
]

def gerar():
    exemplos = []
    for item in CONHECIMENTO:
        exemplos.append({
            'messages': [
                {'role': 'user', 'content': item['pergunta']},
                {'role': 'assistant', 'content': item['resposta']},
            ]
        })

    caminho = 'datasets/dataset_portugues.jsonl'
    with open(caminho, 'w', encoding='utf-8') as f:
        for ex in exemplos:
            f.write(json.dumps(ex, ensure_ascii=False) + '\n')

    print(f'Dataset de portugues criado: {len(exemplos)} exemplos em {caminho}')

if __name__ == '__main__':
    gerar()
