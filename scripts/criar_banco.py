# scripts/criar_banco.py
# Criado por Anderson Honorato
# Cria as tabelas no SQLite/PostgreSQL para armazenar o conhecimento da IA

from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime
from sqlalchemy.orm import declarative_base, sessionmaker
from datetime import datetime
import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv('DATABASE_URL', 'sqlite:///banco_dados/conhecimento.db')
engine = create_engine(DATABASE_URL, echo=False)
Base = declarative_base()
Sessao = sessionmaker(bind=engine)

class ConhecimentoBase(Base):
    '''Tabela principal de conhecimento da IA'''
    __tablename__ = 'conhecimento_base'

    id = Column(Integer, primary_key=True)
    categoria = Column(String(100), nullable=False)
    pergunta = Column(Text, nullable=False)
    resposta = Column(Text, nullable=False)
    fonte = Column(String(200))
    criado_em = Column(DateTime, default=datetime.utcnow)
    dono = Column(String(100), default='Anderson Honorato')

def criar_tabelas():
    Base.metadata.create_all(engine)
    print('Tabelas criadas com sucesso!')

def inserir_exemplos():
    sessao = Sessao()

    exemplos = [
        ConhecimentoBase(
            categoria='negocio',
            pergunta='Como funciona o sistema de tickets do FaleZap?',
            resposta='O sistema de tickets do FaleZap permite abrir, atribuir e fechar tickets de suporte. Cada ticket possui status (aberto, em andamento, resolvido, fechado), prioridade e responsavel. O sistema notifica automaticamente os envolvidos por WhatsApp.',
            fonte='documentacao_interna'
        ),
        ConhecimentoBase(
            categoria='negocio',
            pergunta='Quais sao os status de um ticket no FaleZap?',
            resposta='Os status sao: Aberto (ticket criado, aguardando atendimento), Em andamento (atendente trabalhando na solucao), Resolvido (solucao aplicada, aguardando confirmacao), Fechado (cliente confirmou a solucao). O sistema atualiza o status automaticamente conforme as acoes do atendente.',
            fonte='documentacao_interna'
        ),
        ConhecimentoBase(
            categoria='tecnico',
            pergunta='Qual a stack principal dos projetos desenvolvidos por Anderson Honorato?',
            resposta='Os projetos utilizam principalmente Python (FastAPI, Flask) no backend, React/Next.js no frontend, PostgreSQL ou SQLite como banco de dados, e integracoes com WhatsApp via Evolution API ou Baileys. A infraestrutura usa Docker e deploy em VPS Linux.',
            fonte='projetos_internos'
        ),
        ConhecimentoBase(
            categoria='negocio',
            pergunta='O que e o projeto FaleZap?',
            resposta='FaleZap e uma plataforma de atendimento multicanal via WhatsApp desenvolvida por Anderson Honorato. Ela oferece sistema de tickets, chatbot com IA, multiatendentes, disparo em massa, e integracao com diversas APIs. E voltada para empresas que precisam gerenciar atendimento ao cliente pelo WhatsApp.',
            fonte='documentacao_interna'
        ),
        ConhecimentoBase(
            categoria='tecnico',
            pergunta='Como funciona o chatbot com IA do FaleZap?',
            resposta='O chatbot utiliza processamento de linguagem natural para entender a intencao do cliente e responder automaticamente. Ele pode consultar uma base de conhecimento customizada, seguir fluxos de conversa pre-definidos, e escalar para um atendente humano quando necessario. A IA e treinada com dados especificos de cada empresa.',
            fonte='documentacao_interna'
        ),
        ConhecimentoBase(
            categoria='negocio',
            pergunta='Quais planos o FaleZap oferece?',
            resposta='O FaleZap oferece planos baseados no numero de atendentes e volume de mensagens. Ha um plano basico para pequenas empresas, planos profissionais com recursos avancados como chatbot IA e relatorios, e planos enterprise com integracoes personalizadas e suporte dedicado.',
            fonte='comercial'
        ),
        ConhecimentoBase(
            categoria='tecnico',
            pergunta='O que e a Evolution API?',
            resposta='A Evolution API e uma API open source que facilita a integracao com o WhatsApp, abstraindo a complexidade da conexao WebSocket. Ela suporta multissessoes, envio de mensagens (texto, imagem, audio, video), webhooks para eventos em tempo real, e e amplamente usada em projetos de automacao com WhatsApp no Brasil.',
            fonte='conhecimento_tecnico'
        ),
        ConhecimentoBase(
            categoria='negocio',
            pergunta='Como Anderson Honorato trabalha com desenvolvimento de software?',
            resposta='Anderson Honorato desenvolve solucoes sob medida para empresas, com foco em automacao, chatbots WhatsApp, plataformas SaaS e sistemas web. Ele trabalha com Python, Node.js, React, bancos de dados relacionais e integracoes com IA. O processo inclui levantamento de requisitos, prototipagem, desenvolvimento agil e deploy.',
            fonte='informacao_criador'
        ),
    ]

    for ex in exemplos:
        sessao.add(ex)

    sessao.commit()
    sessao.close()
    print(f'{len(exemplos)} exemplos inseridos!')

if __name__ == '__main__':
    criar_tabelas()
    inserir_exemplos()
