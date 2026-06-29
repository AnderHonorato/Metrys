# scripts/temas_treinamento.py
# Criado por Anderson Honorato
# Lista de perguntas que o Teacher vai responder para treinar o Student
# EDITE ESTA LISTA conforme o dominio que sua IA deve dominar

TEMAS = [
    # === Programacao Python ===
    'Como criar uma API REST com Python e FastAPI?',
    'Explique decorators em Python com exemplos praticos',
    'Como funciona o gerenciamento de pacotes com pip e requirements.txt?',
    'O que sao list comprehensions em Python e quando usar?',
    'Como tratar excecoes em Python com try/except?',
    'Explique a diferenca entre list, tuple, set e dict em Python',
    'Como criar uma classe em Python com heranca?',
    'O que sao generators e yield em Python?',
    'Como fazer requests HTTP em Python usando a biblioteca requests?',
    'Explique o conceito de ambientes virtuais (venv) em Python',

    # === Programacao JavaScript/TypeScript ===
    'Explique o conceito de assincronismo em JavaScript com async/await',
    'O que sao Promises em JavaScript e como funcionam?',
    'Como funciona o event loop no Node.js?',
    'Explique a diferenca entre var, let e const em JavaScript',
    'O que e TypeScript e quais as vantagens sobre JavaScript?',
    'Como manipular arrays em JavaScript? (map, filter, reduce)',
    'Explique o conceito de closures em JavaScript',
    'Como funciona o sistema de modulos no Node.js (CommonJS vs ES Modules)?',

    # === Bancos de Dados ===
    'Como conectar um banco PostgreSQL com SQLAlchemy?',
    'Explique a diferenca entre SQL e NoSQL',
    'O que sao indices em banco de dados e quando usar?',
    'Como fazer uma query JOIN entre duas tabelas em SQL?',
    'Explique o que e normalizacao de banco de dados',
    'O que sao transacoes em banco de dados e por que sao importantes?',
    'Como modelar um banco relacional para um sistema de e-commerce?',
    'Explique o conceito de migrations em bancos de dados',

    # === APIs e Web ===
    'Como funciona o sistema de autenticacao JWT?',
    'Explique o protocolo HTTP: GET, POST, PUT, DELETE',
    'O que e REST e como projetar uma API RESTful?',
    'Como funciona CORS e por que e importante?',
    'Explique o que e GraphQL e como difere de REST',
    'O que sao webhooks e como implementar?',
    'Como fazer autenticacao OAuth2 em uma aplicacao web?',
    'Explique o conceito de middleware em servidores web',

    # === Docker e DevOps ===
    'O que e Docker e como criar um Dockerfile?',
    'Explique docker-compose e sua utilidade',
    'Como funciona o deploy continuo (CI/CD)?',
    'O que e Git e como funciona o fluxo basico (clone, add, commit, push)?',
    'Explique a diferenca entre Git merge e Git rebase',

    # === Negocios / SaaS ===
    'Como estruturar um SaaS B2B do zero?',
    'Quais sao as melhores estrategias de precificacao para software?',
    'O que e MVP (Minimum Viable Product) e como planejar um?',
    'Como validar uma ideia de startup de tecnologia?',
    'Explique o que e product-market fit',

    # === WhatsApp / Bots ===
    'Como criar um bot WhatsApp com webhooks?',
    'Explique a diferenca entre a API oficial do WhatsApp e Baileys',
    'Como funciona a Evolution API para WhatsApp?',
    'Quais as limitacoes da API do WhatsApp Business?',

    # === IA / Machine Learning ===
    'Como funciona o fine-tuning de modelos de linguagem?',
    'Explique o que e QLoRA e como reduz o uso de memoria',
    'O que sao embeddings e como sao usados em RAG?',
    'Explique a arquitetura Transformer usada em LLMs',
    'O que e o metodo LoRA para fine-tuning eficiente?',
    'Como funciona o processo de tokenizacao em modelos de linguagem?',

    # === Desenvolvimento Web ===
    'Como estruturar um projeto React do zero?',
    'Explique o conceito de componentes em React',
    'O que e estado (state) e props em React?',
    'Como funciona o roteamento em React com React Router?',
    'O que e CSS Flexbox e como usar?',
    'Explique design responsivo e media queries em CSS',

    # === Linux e Terminal ===
    'Quais sao os comandos basicos do terminal Linux?',
    'Como gerenciar permissoes de arquivos no Linux com chmod?',
    'O que e SSH e como conectar a um servidor remoto?',

    # === Conceitos Gerais de Programacao ===
    'Explique programacao orientada a objetos vs funcional',
    'O que sao design patterns e cite 3 exemplos comuns',
    'Como funciona o gerenciamento de memoria em linguagens compiladas vs interpretadas?',
    'Explique o conceito de complexidade algoritmica (Big O)',
    'O que e recursao e quando usar?',
    'Como debugar codigo de forma eficiente?',

    # === Seguranca ===
    'Quais sao as boas praticas de seguranca em aplicacoes web?',
    'Explique SQL injection e como prevenir',
    'O que e XSS (Cross-Site Scripting) e como evitar?',
    'Como armazenar senhas de forma segura (hashing, salting)?',

    # === Portugues e Comunicacao ===
    'Explique a diferenca entre portugues formal e informal',
    'Quais sao as regras basicas de pontuacao em portugues?',
    'Como escrever um email profissional em portugues?',
    'Explique a concordancia verbal em portugues com exemplos',
    'O que e crase e quando usar em portugues?',
    'Como melhorar a comunicacao escrita em ambientes profissionais?',
    'Quais sao os principais erros de portugues em textos tecnicos?',

    # === Conhecimento Geral / Utilidades ===
    'Como aprender uma nova linguagem de programacao de forma eficiente?',
    'O que e open source e como contribuir com projetos?',
    'Como montar um portfolio de desenvolvedor?',
    'Explique metodologias ageis: Scrum e Kanban',
    'Como funciona o processo de code review?',
    'Quais sao as melhores praticas para documentar codigo?',
    'Como escolher a stack tecnologica certa para um projeto?',
]

# Quantidade total de temas para referencia
print(f'Temas de treinamento carregados: {len(TEMAS)}')
