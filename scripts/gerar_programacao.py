# scripts/gerar_programacao.py
# Criado por Anderson Honorato
# Gera dataset sintetico de programacao - sem custo de API
# Enriquece o modelo com conhecimento tecnico em portugues

import json, random, os

CONHECIMENTO = [
    # === Python ===
    {
        'pergunta': 'Como criar uma lista em Python?',
        'resposta': 'Use colchetes: `minha_lista = [1, 2, 3]` ou `list()` para criar vazia. '
                    'Listas sao mutaveis, ordenadas e aceitam elementos duplicados.'
    },
    {
        'pergunta': 'Para que serve o `if __name__ == "__main__"` em Python?',
        'resposta': 'Garante que o codigo dentro desse bloco so execute quando o script '
                    'for rodado diretamente, nao quando importado como modulo. '
                    'Exemplo: `if __name__ == "__main__": main()`'
    },
    {
        'pergunta': 'O que e um dicionario em Python?',
        'resposta': 'E uma estrutura de dados que armazena pares chave-valor: '
                    '`{"nome": "Joao", "idade": 30}`. As chaves sao unicas e imutaveis. '
                    'Acesso rapido por chave em O(1).'
    },
    {
        'pergunta': 'Como funciona o `try/except` em Python?',
        'resposta': 'E usado para tratamento de excecoes. O codigo no bloco `try` executa; '
                    'se ocorrer erro, o `except` captura a excecao especifica. '
                    'Sempre use excecoes especificas (nunca `except:` generico). '
                    'Exemplo: `try: x = 1/0 \\nexcept ZeroDivisionError: print("Erro")`'
    },
    {
        'pergunta': 'Explique list comprehension em Python.',
        'resposta': 'E uma forma concisa de criar listas: `[x*2 for x in range(10) if x > 5]`. '
                    'Mais rapido que loop tradicional, otimizado em C. '
                    'Evite aninhar mais de 2 niveis para manter legibilidade.'
    },
    {
        'pergunta': 'Qual a diferenca entre tupla e lista em Python?',
        'resposta': 'Tuplas sao imutaveis (nao podem ser alteradas apos criadas), '
                    'listas sao mutaveis. Tuplas usam parenteses: `(1, 2, 3)`. '
                    'Sao mais rapidas e podem ser usadas como chaves de dicionario.'
    },
    {
        'pergunta': 'Para que serve o `lambda` em Python?',
        'resposta': 'Cria funcoes anonimas de uma linha: `lambda x: x * 2`. '
                    'Muito usado com `map()`, `filter()`, `sorted(key=lambda...)`. '
                    'Para logica complexa, prefira `def` normal.'
    },
    {
        'pergunta': 'Como funciona heranca em Python?',
        'resposta': 'Uma classe filha herda atributos e metodos da classe pai: '
                    '`class Filho(Pai):`. Use `super().__init__()` para chamar '
                    'o construtor da classe pai. Python suporta heranca multipla.'
    },
    {
        'pergunta': 'O que sao decorators em Python?',
        'resposta': 'Funcoes que modificam o comportamento de outras funcoes. '
                    'Sintaxe: `@meu_decorator` acima da funcao. Muito usados em '
                    'frameworks como Flask (`@app.route`) e FastAPI (`@app.get`).'
    },
    {
        'pergunta': 'Como instalar pacotes em Python?',
        'resposta': 'Use `pip install nome_pacote`. Para listar: `pip list`. '
                    'Para arquivo de dependencias: `pip freeze > requirements.txt`. '
                    'Sempre use ambiente virtual: `python -m venv venv`.'
    },
    {
        'pergunta': 'O que e um generator em Python?',
        'resposta': 'Funcao que usa `yield` em vez de `return`, produzindo valores '
                    'sob demanda. Economiza memoria pois nao gera todos os valores '
                    'de uma vez. Exemplo: `def contagem(): yield 1; yield 2`'
    },
    {
        'pergunta': 'Como ler e escrever arquivos em Python?',
        'resposta': 'Use `with open("arquivo.txt", "r", encoding="utf-8") as f:` '
                    'para leitura segura (fecha automaticamente). Modos: "r" leitura, '
                    '"w" escrita (sobrescreve), "a" append, "rb"/"wb" para binario.'
    },
    {
        'pergunta': 'O que e o `self` em classes Python?',
        'resposta': 'Referencia a instancia atual da classe. Todo metodo de instancia '
                    'deve ter `self` como primeiro parametro. Acessa atributos do '
                    'objeto: `self.nome`. Python passa `self` automaticamente.'
    },

    # === JavaScript ===
    {
        'pergunta': 'Qual a diferenca entre var, let e const em JavaScript?',
        'resposta': '`var`: escopo de funcao, permite redeclaracao. '
                    '`let`: escopo de bloco, nao permite redeclaracao. '
                    '`const`: escopo de bloco, nao permite reatribuicao. '
                    'Prefira `const` por padrao, `let` quando precisar reatribuir.'
    },
    {
        'pergunta': 'Como funciona async/await em JavaScript?',
        'resposta': 'Simplifica codigo assincrono. `async` declara funcao que retorna '
                    'Promise. `await` pausa execucao ate a Promise resolver. '
                    'Substitui `.then()` encadeado por codigo sequencial legivel.'
    },
    {
        'pergunta': 'O que e uma Promise em JavaScript?',
        'resposta': 'Objeto que representa operacao assincrona com 3 estados: '
                    'pending, fulfilled (resolve) e rejected. Metodos: `.then()`, '
                    '`.catch()`, `.finally()`. Base para async/await.'
    },
    {
        'pergunta': 'Explique arrow functions em JavaScript.',
        'resposta': 'Sintaxe concisa: `(a, b) => a + b`. Nao tem seu proprio `this`, '
                    'herdam o `this` do escopo pai. Ideais para callbacks e metodos '
                    'funcionais como map/filter/reduce.'
    },
    {
        'pergunta': 'O que e desestruturacao em JavaScript?',
        'resposta': 'Extrai valores de objetos/arrays para variaveis: '
                    '`const {nome, idade} = pessoa` ou `const [a, b] = [1, 2]`. '
                    'Muito usada em parametros de funcao e imports.'
    },
    {
        'pergunta': 'Como funciona o event loop no Node.js?',
        'resposta': 'Mecanismo que permite operacoes nao-bloqueantes em thread unica. '
                    'Usa call stack, task queue e microtask queue. Promises tem '
                    'prioridade sobre setTimeout no event loop.'
    },

    # === SQL / Banco de Dados ===
    {
        'pergunta': 'O que e uma query JOIN em SQL?',
        'resposta': 'Combina registros de duas ou mais tabelas baseado em coluna '
                    'relacionada. Tipos: INNER JOIN (intersecao), LEFT JOIN (todos '
                    'da esquerda), RIGHT JOIN, FULL OUTER JOIN. Exemplo: '
                    '`SELECT * FROM pedidos JOIN clientes ON pedidos.cliente_id = clientes.id`'
    },
    {
        'pergunta': 'Para que servem indices em banco de dados?',
        'resposta': 'Aceleram consultas criando estrutura de busca rapida (B-tree). '
                    'Use em colunas de WHERE, JOIN e ORDER BY frequentes. Cuidado: '
                    'muitos indices tornam INSERT/UPDATE mais lentos.'
    },
    {
        'pergunta': 'Explique normalizacao de banco de dados.',
        'resposta': 'Processo de organizar dados para reduzir redundancia. Formas normais: '
                    '1FN (valores atomicos), 2FN (sem dependencia parcial de chave), '
                    '3FN (sem dependencia transitiva). Busque 3FN na maioria dos casos.'
    },
    {
        'pergunta': 'Qual a diferenca entre SQL e NoSQL?',
        'resposta': 'SQL: relacional, esquema rigido, tabelas com relacoes, ACID. '
                    'Exemplos: PostgreSQL, MySQL. '
                    'NoSQL: nao-relacional, esquema flexivel, escalabilidade horizontal. '
                    'Exemplos: MongoDB (documentos), Redis (chave-valor), Cassandra.'
    },
    {
        'pergunta': 'O que sao transactions em SQL?',
        'resposta': 'Conjunto de operacoes executadas como unidade atomica. '
                    'Propriedades ACID: Atomicidade, Consistencia, Isolamento, '
                    'Durabilidade. Exemplo: transferencia bancaria entre contas.'
    },

    # === APIs / HTTP ===
    {
        'pergunta': 'O que e uma API REST?',
        'resposta': 'API que segue principios REST: recursos identificados por URLs, '
                    'uso de metodos HTTP (GET, POST, PUT, DELETE), stateless, '
                    'respostas em JSON. Cada endpoint representa um recurso.'
    },
    {
        'pergunta': 'Explique os metodos HTTP principais.',
        'resposta': 'GET: consultar dados. POST: criar recurso. PUT: atualizar '
                    'recurso completo. PATCH: atualizar parcial. DELETE: remover. '
                    'HEAD: so cabecalhos. OPTIONS: metodos permitidos.'
    },
    {
        'pergunta': 'Como funciona autenticacao JWT?',
        'resposta': 'Cliente envia credenciais, servidor retorna token JWT assinado. '
                    'Token contem claims (dados do usuario) em Base64. Cliente envia '
                    'token no header `Authorization: Bearer <token>`. Stateless, '
                    'sem necessidade de sessao no servidor.'
    },
    {
        'pergunta': 'O que sao status codes HTTP?',
        'resposta': 'Codigos de resposta do servidor: 2xx sucesso (200 OK, 201 Created), '
                    '3xx redirecionamento, 4xx erro do cliente (400 Bad Request, '
                    '401 Unauthorized, 404 Not Found), 5xx erro do servidor (500 Internal Error).'
    },

    # === Git ===
    {
        'pergunta': 'Como funciona o fluxo basico do Git?',
        'resposta': '1. `git clone` copia repositorio. 2. `git add` prepara arquivos. '
                    '3. `git commit -m "mensagem"` salva snapshot. '
                    '4. `git push` envia para remoto. 5. `git pull` atualiza do remoto. '
                    '6. `git branch` gerencia ramificacoes.'
    },
    {
        'pergunta': 'Qual a diferenca entre merge e rebase no Git?',
        'resposta': 'Merge: cria commit de merge, preserva historico completo. '
                    'Rebase: reaplica commits em cima de outra branch, historico linear. '
                    'Use rebase para branches locais, merge para branches compartilhadas.'
    },

    # === Docker ===
    {
        'pergunta': 'O que e Docker e para que serve?',
        'resposta': 'Plataforma de containers que empacota aplicacao com todas '
                    'dependencias em ambiente isolado. Garante que funcione igual '
                    'em qualquer maquina. Mais leve que VM, compartilha kernel do host.'
    },
    {
        'pergunta': 'O que e um Dockerfile?',
        'resposta': 'Arquivo de instrucoes para construir imagem Docker. Define '
                    'imagem base (`FROM`), copia arquivos (`COPY`), instala '
                    'dependencias (`RUN`), expoe porta (`EXPOSE`), comando de '
                    'inicializacao (`CMD`).'
    },

    # === Estruturas de Dados e Algoritmos ===
    {
        'pergunta': 'O que e complexidade Big O?',
        'resposta': 'Mede eficiencia de algoritmos no pior caso. O(1): constante. '
                    'O(log n): busca binaria. O(n): percorrer lista. O(n log n): '
                    'merge sort. O(n^2): bubble sort. Quanto menor, mais eficiente.'
    },
    {
        'pergunta': 'Explique recursao.',
        'resposta': 'Funcao que chama a si mesma para resolver problema dividindo '
                    'em subproblemas menores. Necessario caso base para evitar '
                    'loop infinito. Exemplo classico: fatorial `n! = n * (n-1)!`'
    },
    {
        'pergunta': 'O que e design pattern Singleton?',
        'resposta': 'Garante que uma classe tenha apenas uma instancia e fornece '
                    'ponto global de acesso. Util para conexoes de banco, logs, '
                    'configuracoes. Em Python, pode ser implementado com `__new__`.'
    },

    # === Linux / Terminal ===
    {
        'pergunta': 'Quais comandos basicos do terminal Linux?',
        'resposta': '`ls` lista arquivos, `cd` navega diretorios, `pwd` mostra '
                    'caminho atual, `mkdir` cria diretorio, `rm` remove, `cp` copia, '
                    '`mv` move/renomeia, `cat` exibe arquivo, `grep` busca texto, '
                    '`chmod` altera permissoes, `ps` lista processos.'
    },

    # === Desenvolvimento Web ===
    {
        'pergunta': 'O que e CORS e como resolver?',
        'resposta': 'Cross-Origin Resource Sharing - politica de seguranca que '
                    'bloqueia requisicoes de origens diferentes. Resolver: '
                    'configurar header `Access-Control-Allow-Origin` no servidor '
                    'ou usar proxy no desenvolvimento.'
    },
    {
        'pergunta': 'Explique componentes em React.',
        'resposta': 'Blocos reutilizaveis de UI que gerenciam seu proprio estado. '
                    'Podem ser funcionais (com hooks) ou de classe. Recebem props '
                    'e retornam JSX. Componentes permitem composicao e reuso.'
    },
    {
        'pergunta': 'O que e design responsivo?',
        'resposta': 'Tecnica que adapta layout a diferentes tamanhos de tela '
                    '(desktop, tablet, mobile). Usa media queries CSS, grid/flexbox, '
                    'unidades relativas (%, vw, rem) e imagens flexiveis.'
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

    caminho = 'datasets/dataset_programacao.jsonl'
    with open(caminho, 'w', encoding='utf-8') as f:
        for ex in exemplos:
            f.write(json.dumps(ex, ensure_ascii=False) + '\n')

    print(f'Dataset de programacao criado: {len(exemplos)} exemplos em {caminho}')

if __name__ == '__main__':
    gerar()
