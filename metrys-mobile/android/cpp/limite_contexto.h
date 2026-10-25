#pragma once
#include <algorithm>
#include <cstdint>

/** Reserva pesos + buffers antes do KV cache; zero indica memória insuficiente. */
inline int contextoPelaMemoria(int pedido,int treinado,uint64_t disponivel,uint64_t pesos,uint64_t bytesPorToken){
 int limite=pedido==0?treinado:std::min(pedido,treinado);
 if(disponivel>0){
  constexpr uint64_t buffers=256ULL*1024*1024;
  if(disponivel<=pesos+buffers||bytesPorToken==0)return 0;
  uint64_t cabem=(disponivel-pesos-buffers)/bytesPorToken;
  limite=static_cast<int>(std::min(static_cast<uint64_t>(limite),cabem));
  limite=(limite/256)*256;
 }
 return limite>=512?limite:0;
}
