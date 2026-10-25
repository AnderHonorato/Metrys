#include "limite_contexto.h"
#include <cassert>
#include <iostream>
int main(){
 constexpr uint64_t GiB=1024ULL*1024*1024;
 assert(contextoPelaMemoria(0,32768,8*GiB,GiB,32768)==32768);
 int limitado=contextoPelaMemoria(0,262144,4*GiB,3*GiB,147456);
 assert(limitado>=512&&limitado<8192);
 assert(3*GiB+256*1024*1024+static_cast<uint64_t>(limitado)*147456<=4*GiB);
 assert(contextoPelaMemoria(0,32768,GiB,2*GiB,32768)==0);
 assert(contextoPelaMemoria(4096,32768,0,0,1)==4096);
 assert(contextoPelaMemoria(8192,4096,8*GiB,GiB,32768)==4096);
 std::cout<<"PASS memória: máximo do modelo, limite pela RAM, reserva dos pesos/buffers e falta de memória\n";
}
