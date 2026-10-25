#include "utf8_stream.h"
#include <cassert>
#include <iostream>
int main(){
 const std::string texto=u8"Olá 🧡 código 漢字";
 for(size_t limite=1;limite<=texto.size();limite++){
  MetrysUtf8 fluxo;std::string recebido;
  for(size_t pos=0;pos<texto.size();pos+=limite)recebido+=fluxo.receber(texto.substr(pos,limite));
  recebido+=fluxo.finalizar();assert(recebido==texto);
 }
 MetrysUtf8 ruim;assert(ruim.receber(std::string("\xff\xc0",2))==u8"��");
 MetrysUtf8 curto;assert(curto.receber(std::string("\xf0\x9f",2)).empty());assert(curto.finalizar()==u8"�");
 std::cout<<"UTF8_OK: todas as fronteiras, caracteres suplementares e bytes inválidos\n";
}
