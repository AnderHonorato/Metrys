/* Metrys — indicador real de eventos de conversa.
   Sem rede, sem acesso a tokens, sem leitura de texto ou memoria do usuario. */
(function(){
'use strict';
var dock=document.getElementById('dock');
if(!dock)return;
var estados={
  ocioso:'Pronto para conversar',
  escutando:'Ouvindo',
  pensando:'Organizando a resposta',
  falando:'Resposta disponível',
  gerando:'Gerando resposta',
  ferramenta:'Utilizando ferramenta',
  sucesso:'Concluído',
  erro:'Não foi possível concluir',
  cancelado:'Interrompido',
  memoria:'Memória registrada',
  anexo:'Anexo recebido',
  offline:'Modo offline',
  online:'Modo online'
};
var ele=document.createElement('div');ele.id='estadoVivo';
ele.setAttribute('role','status');ele.setAttribute('aria-live','polite');
var ponto=document.createElement('span');ponto.className='estado-orbita';ponto.setAttribute('aria-hidden','true');
var frase=document.createElement('span');frase.className='estado-texto';
ele.append(ponto,frase);dock.insertBefore(ele,dock.firstChild);
var atual='offline',duracao=null,anteriorGerando=false,ultimaFase='',antigasMensagens=0,atualOnline=false;
function modoOnline(){
 var btn=document.getElementById('alternarNuvem');
 return !!(btn&&btn.getAttribute('aria-pressed')==='true');
}
function limparTempo(){if(duracao!==null){clearTimeout(duracao);duracao=null;}}
function aplicar(tipo,temporario){
 if(!Object.prototype.hasOwnProperty.call(estados,tipo))return;
 limparTempo();
 atual=tipo;
 document.body.dataset.metrysEvento=tipo;
 frase.textContent=estados[tipo];
 if(temporario){
  duracao=setTimeout(function(){
   duracao=null;
   var base=modoOnline()?'online':'offline';
   aplicar(base,false);
  },tipo==='falando'?1150:tipo==='erro'?3200:tipo==='cancelado'?2200:tipo==='sucesso'?1600:1900);
 }
}
function evento(tipo,temporario){
 if(tipo==='online'||tipo==='offline'){atualOnline=tipo==='online';aplicar(tipo,true);return;}
 aplicar(tipo,temporario!==false);
}
window.addEventListener('metrys:evento',function(e){
 if(!e.detail||!e.detail.tipo)return;
 evento(String(e.detail.tipo),e.detail.temporario);
});
var original=window.receberEstado;
if(typeof original==='function'){
 window.receberEstado=function(novo){
  // Preserva a ponte do aplicativo. Inspeciona somente metadados de estado.
  var json=novo;
  if(typeof novo==='string'){try{json=JSON.parse(novo)}catch(_){json=null}}
  var online=modoOnline();
  if(!online&&json&&typeof json==='object'){
   var gerando=!!json.gerando,stage=String(json.fase||'');
   var tamanho=Array.isArray(json.mensagens)?json.mensagens.length:antigasMensagens;
   if(stage==='erro')evento('erro',true);
   else if(stage==='ferramenta'||stage==='executando_ferramenta')evento('ferramenta',false);
   else if(gerando){
     if(json.raciocinando)evento('pensando',false);
     else evento('gerando',false);
   }else if(anteriorGerando&&tamanho>antigasMensagens){
     evento('sucesso',true);
   }else if(anteriorGerando){
     aplicar('offline',false);
   }
   anteriorGerando=gerando;
   antigasMensagens=tamanho;
   ultimaFase=stage;
  }
  return original.apply(this,arguments);
 };
}
var antigoAnexo=window.receberAnexo;
if(typeof antigoAnexo==='function'){
 window.receberAnexo=function(nome,texto,erro){
  var resultado=antigoAnexo.apply(this,arguments);
  if(erro)evento('erro',true);
  else if(nome)evento('anexo',true);
  return resultado;
 };
}
// A barra e o avatar reagem aos metadados de status, sem capturar o conteudo do chat.
aplicar(modoOnline()?'online':'offline',false);
})();
