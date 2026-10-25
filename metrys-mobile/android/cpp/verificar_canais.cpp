#include "canal_pensamento.h"
#include <cassert>
#include <iostream>
int main(){
 CanalPensamento direto(false);direto.receber("```html\n<think>um exemplo</think>\n```");assert(!direto.pensando());
 CanalPensamento literal(false);literal.receber("O marcador é <think>.");assert(!literal.pensando());
 CanalPensamento fragmentado(false);fragmentado.receber(" <thi");fragmentado.receber("nk>");assert(fragmentado.pensando());fragmentado.receber("Atividade</thi");assert(fragmentado.pensando());fragmentado.receber("nk>Resposta");assert(!fragmentado.pensando());
 fragmentado.receber("<think>exemplo na resposta");assert(!fragmentado.pensando());
 CanalPensamento implicito(true);implicito.receber("Atividade");assert(implicito.pensando());implicito.encerrar();implicito.receber("<think>literal");assert(!implicito.pensando());
 std::cout<<"PASS canais nativos: literal, prefixos fragmentados, fechamento e transição única\n";
}
