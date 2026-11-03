/* Navegacao Organic: atalhos para controles existentes (inclusive modo offline/online). */
(function(){
 'use strict';
 function clique(id){
  const controle=document.getElementById(id);
  if(controle && !controle.disabled && !controle.hidden){controle.click();return true;}
  return false;
 }
 function ativar(){
  const acoes={navHistorico:'menu',navNova:'novaConversa',navModo:'alternarNuvem',navVisual:'temasTopo'};
  Object.keys(acoes).forEach(function(id){
   const b=document.getElementById(id);if(!b)return;
   b.addEventListener('click',function(){
    if(!clique(acoes[id])){
     const alerta=document.getElementById('aviso');
     if(alerta){alerta.textContent='Este recurso ainda nao esta disponivel.';alerta.classList.add('ativo');setTimeout(function(){alerta.classList.remove('ativo');},2400);}
    }
   });
  });
  const alternador=document.getElementById('alternarNuvem');
  const modo=document.getElementById('navModo');
  if(alternador && modo){
   const atualizar=function(){const ligado=alternador.getAttribute('aria-pressed')==='true';
    modo.setAttribute('aria-label',ligado?'Trocar para modo offline':'Trocar para modo online');
    modo.setAttribute('aria-current',ligado?'page':'false');};
   alternador.addEventListener('click',atualizar);atualizar();
  }
 }
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',ativar,{once:true});else ativar();
})();
