"""Migra um banco real 0.6.9 sem perder mensagens/modelos; SQL usado pelo app."""
import re, sqlite3
from pathlib import Path

source = (Path(__file__).resolve().parents[1] / 'android/src/br/com/anderflow/metrys/offline/BancoMetrys.java').read_text()
ddl = re.findall(r'db\.execSQL\("([^"\\]*(?:\\.[^"\\]*)*)"\);', source)
db = sqlite3.connect(':memory:')
db.execute('PRAGMA foreign_keys=ON')
for sql in ddl:
    if sql.startswith('CREATE TABLE ') and 'atividades_resposta' not in sql:
        db.execute(sql)
db.execute("INSERT INTO conversas(id,titulo,criado_em,atualizado_em) VALUES ('c','Minha conversa',1,1)")
db.execute("INSERT INTO mensagens(id,conversa_id,papel,texto,criada_em) VALUES (1,'c','user','Olá',1)")
db.execute("INSERT INTO mensagens(id,conversa_id,papel,texto,criada_em) VALUES (2,'c','assistant','Resposta anterior',2)")
migrations = [sql for sql in ddl if sql.startswith('CREATE TABLE IF NOT EXISTS atividades_resposta')]
assert migrations, '0.6.9 não tem migração para preservar atividade por pergunta'
db.execute(migrations[0])
db.execute("INSERT INTO atividades_resposta(pergunta_id,titulo,texto,estado,duracao_ms) VALUES(1,'Calculando','Atividade gerada pelo modelo','concluido',1234)")
assert db.execute('SELECT texto FROM mensagens ORDER BY id').fetchall() == [('Olá',), ('Resposta anterior',)]
assert db.execute('SELECT texto FROM atividades_resposta WHERE pergunta_id=1').fetchone()[0] == 'Atividade gerada pelo modelo'
# Outra pergunta não apaga a atividade anterior; a FK evita órfãos na exclusão.
db.execute("INSERT INTO mensagens(id,conversa_id,papel,texto,criada_em) VALUES (3,'c','user','Nova mensagem',3)")
assert db.execute('SELECT COUNT(*) FROM atividades_resposta').fetchone()[0] == 1
db.execute("DELETE FROM conversas WHERE id='c'")
assert db.execute('SELECT COUNT(*) FROM atividades_resposta').fetchone()[0] == 0
assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
print('PASS migração: mensagens preservadas, atividade persistente, nova pergunta e exclusão em cascata')
queue = [sql for sql in ddl if sql.startswith('CREATE TABLE IF NOT EXISTS fila_mensagens')]
assert queue, 'Mensagens aceitas ainda não têm fila durável'
db.execute(queue[0])
db.execute("INSERT INTO conversas(id,titulo,criado_em,atualizado_em) VALUES ('q','Fila',1,1)")
for texto in ('primeira','segunda','terceira'):
    db.execute("INSERT INTO fila_mensagens(conversa_id,texto) VALUES ('q',?)", (texto,))
assert db.execute('SELECT texto FROM fila_mensagens ORDER BY id').fetchall() == [('primeira',),('segunda',),('terceira',)]
db.execute("DELETE FROM conversas WHERE id='q'")
assert db.execute('SELECT COUNT(*) FROM fila_mensagens').fetchone()[0] == 0
print('PASS fila: aceitação durável, ordem e isolamento por conversa')
