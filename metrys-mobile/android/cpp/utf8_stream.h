#pragma once
#include <string>

/** Acumula fragmentos de tokens e entrega somente UTF-8 completo. */
class MetrysUtf8 {
 std::string pendente;
 public:
 std::string receber(const std::string &entrada){
  pendente+=entrada;std::string saida;size_t p=0;
  while(p<pendente.size()){
   const auto c=static_cast<unsigned char>(pendente[p]);
   size_t n=c<0x80?1:c>=0xc2&&c<=0xdf?2:c>=0xe0&&c<=0xef?3:c>=0xf0&&c<=0xf4?4:0;
   if(!n){saida+=u8"�";p++;continue;}
   if(p+n>pendente.size())break;
   bool valido=true;for(size_t j=1;j<n;j++)if((static_cast<unsigned char>(pendente[p+j])&0xc0)!=0x80)valido=false;
   unsigned char segundo=n>1?static_cast<unsigned char>(pendente[p+1]):0;
   if((c==0xe0&&segundo<0xa0)||(c==0xed&&segundo>=0xa0)||(c==0xf0&&segundo<0x90)||(c==0xf4&&segundo>=0x90))valido=false;
   if(!valido){saida+=u8"�";p++;continue;}
   saida.append(pendente,p,n);p+=n;
  }
  pendente.erase(0,p);return saida;
 }
 std::string finalizar(){if(pendente.empty())return {};pendente.clear();return u8"�";}
};
