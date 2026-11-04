/* Metrys — estados e paletas no laboratorio, nenhum custo de API. */
(function(){
'use strict';
var defs=[
 {id:'ocioso',name:'Em repouso',about:'Respiração suave, movimentos lentos e terracota natural.',col:'#b2622d',soft:'#f0d8b5',energia:.45},
 {id:'escutando',name:'Ouvindo',about:'Ondas crescentes em dourado quente respondem à escuta.',col:'#bf7935',soft:'#fff1d1',energia:1.15},
 {id:'pensando',name:'Pensando',about:'Órbitas de sálvia e âmbar aceleram em ciclos de raciocínio.',col:'#718452',soft:'#e6ead6',energia:.95},
 {id:'falando',name:'Respondendo',about:'Pulsos de terracota, pêssego e brilho acompanhando a resposta.',col:'#c66a48',soft:'#ffe0bf',energia:1.45},
 {id:'gerando',name:'Gerando',about:'Fluxos de cobre, energia contínua e construção progressiva.',col:'#bd7840',soft:'#fff0c9',energia:1.2},
 {id:'ferramenta',name:'Ferramenta',about:'Rede ativa de conexões em oliva, areia e cobre.',col:'#607854',soft:'#edf1dc',energia:1.1},
 {id:'sucesso',name:'Sucesso',about:'Expansão curta de verde-sálvia com luz clara de confirmação.',col:'#537e56',soft:'#e7f5df',energia:1.32},
 {id:'erro',name:'Erro',about:'Contração curta em vinho queimado, sem flashes agressivos.',col:'#983f49',soft:'#f7deda',energia:.63},
 {id:'cancelado',name:'Cancelado',about:'Energia recolhida e tonalidade de cinza quente.',col:'#80786e',soft:'#eae2d8',energia:.2},
 {id:'memoria',name:'Memória salva',about:'Anel dourado se concentra e fixa um ponto de lembrança.',col:'#b5913f',soft:'#fff2c4',energia:1.08},
 {id:'anexo',name:'Anexo',about:'Entrada suave de linhas azul-acinzentadas e confirmação.',col:'#617d92',soft:'#e4f0f5',energia:.95},
 {id:'offline',name:'Offline',about:'Movimento econômico, oliva e areia, sem brilho exagerado.',col:'#807255',soft:'#e7e0d2',energia:.35},
 {id:'online',name:'Online',about:'Fluxos em cobre e creme, presença e conexão remota.',col:'#b2622d',soft:'#fff0d9',energia:.82}
];
var dictionary={};defs.forEach(function(d){dictionary[d.id]=d});
var cards=[].slice.call(document.querySelectorAll('.card[data-model]'));
var root=document.querySelector('.controls');
if(!root||!cards.length)return;
var group=document.createElement('section');
group.id='eventos-lab';group.setAttribute('aria-label','Eventos adicionais da conversa');
var titulo=document.createElement('div');titulo.className='titulo-eventos';titulo.textContent='Eventos da conversa · teste as reações de todos os agentes';
group.appendChild(titulo);
var outros=defs.filter(function(d){return !['ocioso','escutando','pensando','falando'].includes(d.id)});
outros.forEach(function(d){
 var btn=document.createElement('button');btn.type='button';btn.dataset.evento=d.id;
 btn.textContent=d.name;btn.setAttribute('aria-pressed','false');group.appendChild(btn);
});
var desc=document.createElement('p');desc.id='detalhe-evento';desc.setAttribute('aria-live','polite');group.appendChild(desc);
root.insertAdjacentElement('afterend',group);
cards.forEach(function(card){
 var stage=card.querySelector('.stage');if(!stage)return;
 var halo=document.createElement('span');halo.className='event-ring';halo.setAttribute('aria-hidden','true');
 var label=document.createElement('span');label.className='event-label';label.setAttribute('aria-hidden','true');
 stage.appendChild(halo);stage.appendChild(label);
});
var evento='ocioso';
function aplicar(id){
 var def=dictionary[id];if(!def)return;
 evento=id;document.body.dataset.state=id;
 document.body.style.setProperty('--event-col',def.col);
 document.body.style.setProperty('--event-soft',def.soft);
 desc.textContent='✦ '+def.about;
 cards.forEach(function(card){
   var l=card.querySelector('.event-label');if(l)l.textContent=def.name;
   card.setAttribute('data-evento',id);
 });
 document.querySelectorAll('[data-evento],.controls [data-state]').forEach(function(b){
  if(!b.matches('button'))return;
  var val=b.dataset.evento||b.dataset.state;
  if(val)b.setAttribute('aria-pressed',String(val===id));
 });
}
group.addEventListener('click',function(ev){
 var btn=ev.target.closest('button[data-evento]');if(!btn)return;
 aplicar(btn.dataset.evento);
});
root.addEventListener('click',function(ev){
 var btn=ev.target.closest('button[data-state]');if(!btn)return;
 aplicar(btn.dataset.state);
});
aplicar('ocioso');
window.MetrysEstadosLab={
 nomes:defs.map(function(d){return d.id}),
 estado:function(){return dictionary[evento]},
 selecionar:aplicar,
 desenhar:function(ctx,tt,n){
   var d=dictionary[evento];if(!d)return;
   var t=tt,energy=d.energia,w=200,h=200;
   ctx.save();ctx.translate(100,100);
   // Cada modelo tem movimento um pouco diferente, mas partilha a linguagem do evento.
   var offset=n*.37,dr=75+(n%3)*5;
   if(evento==='escutando'||evento==='falando'||evento==='gerando'){
     for(var i=0;i<3;i++){
       var prog=((t*(evento==='falando'?.95:.58)+i/3+offset)%1);
       var radius=47+prog*58;
       ctx.beginPath();ctx.arc(0,0,radius,0,Math.PI*2);
       ctx.globalAlpha=(1-prog)*.42;ctx.lineWidth=1.5+i*.35;ctx.strokeStyle=d.col;ctx.stroke();
     }
   }else if(evento==='pensando'||evento==='ferramenta'||evento==='online'){
      ctx.globalAlpha=.72;ctx.strokeStyle=d.col;ctx.lineWidth=1.2;
      ctx.beginPath();ctx.ellipse(0,0,dr,dr*.78,t*.2,0,Math.PI*2);ctx.stroke();
      for(var j=0;j<3;j++){
        var ang=t*(evento==='ferramenta'?.85:.48)+(j/3)*Math.PI*2+offset;
        var x=Math.cos(ang)*dr,y=Math.sin(ang)*dr*.78;
        ctx.globalAlpha=.4+.4*Math.sin(ang*2)*Math.sin(ang*2);
        ctx.fillStyle=j%2?d.col:d.soft;ctx.beginPath();ctx.arc(x,y,2.7,0,Math.PI*2);ctx.fill();
      }
   }else if(evento==='sucesso'||evento==='memoria'){
      for(var k=0;k<7;k++){
       var ang=k*Math.PI*2/7+offset;
       var amp=evento==='memoria'?.5:1;
       var r=56+Math.sin(t*1.8+offset)*9*amp;
       ctx.globalAlpha=(.23+.3*(1+Math.sin(t*1.3+k))*.5);
       ctx.fillStyle=k%2?d.col:d.soft;
       ctx.beginPath();ctx.arc(Math.cos(ang)*r,Math.sin(ang)*r,1.6+k%3,0,Math.PI*2);ctx.fill();
      }
   }else if(evento==='erro'){
      var sh=Math.sin(t*22)*2*Math.exp(-(t%2)*3);
      ctx.translate(sh,0);ctx.globalAlpha=.32;ctx.strokeStyle=d.col;ctx.lineWidth=2.3;
      ctx.beginPath();ctx.arc(0,0,67+Math.sin(t*4)*3,0,Math.PI*2);ctx.stroke();
   }else if(evento==='cancelado'||evento==='offline'){
      ctx.globalAlpha=.14;ctx.strokeStyle=d.col;ctx.lineWidth=2;
      ctx.beginPath();ctx.arc(0,0,65+Math.sin(t*.45)*3,0,Math.PI*2);ctx.stroke();
   }else if(evento==='anexo'){
      ctx.globalAlpha=.45;ctx.strokeStyle=d.col;ctx.lineWidth=1.8;
      ctx.save();ctx.rotate(t*.3+offset);ctx.strokeRect(-60,-60,120,120);ctx.restore();
   }else{
      ctx.globalAlpha=.22+.1*Math.sin(t*1.5);ctx.strokeStyle=d.col;ctx.lineWidth=1.3;
      ctx.beginPath();ctx.arc(0,0,70+Math.sin(t)*3,0,Math.PI*2);ctx.stroke();
   }
   ctx.restore();
 }
};
})();
