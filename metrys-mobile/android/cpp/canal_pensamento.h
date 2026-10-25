#pragma once
#include <string>

/** Marcadores só abrem canal antes da resposta, nunca em um exemplo de código. */
class CanalPensamento {
 bool ativo,detectando;std::string prefixo,janela;
 public:
 explicit CanalPensamento(bool implicito):ativo(implicito),detectando(!implicito){}
 void receber(const std::string &parte){
  std::string texto=parte;
  if(detectando){
   prefixo+=parte;size_t inicio=prefixo.find_first_not_of(" \r\n\t");
   if(inicio==std::string::npos)return;
   prefixo.erase(0,inicio);const std::string abrir="<think>";
   if(prefixo.size()<abrir.size()&&abrir.compare(0,prefixo.size(),prefixo)==0)return;
   detectando=false;ativo=prefixo.compare(0,abrir.size(),abrir)==0;
   texto=ativo?prefixo.substr(abrir.size()):"";prefixo.clear();
  }
  if(!ativo)return;
  janela+=texto;
  if(janela.find("</think>")!=std::string::npos){encerrar();return;}
  if(janela.size()>8)janela.erase(0,janela.size()-8);
 }
 void encerrar(){ativo=false;detectando=false;janela.clear();}
 bool pensando()const{return ativo;}
};
