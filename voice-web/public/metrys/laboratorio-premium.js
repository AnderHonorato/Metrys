/* Metrys — oito identidades generativas em Canvas 2D.
   Desenhos originais, sem imagens, GIF, biblioteca externa ou acessos de rede.
   Um único loop limitado a 30 fps, pausa fora de tela e movimento reduzido. */
(function(){
'use strict';
var elementos=[].slice.call(document.querySelectorAll('.premium canvas[data-art]'));
if(!elementos.length)return;
var configuracoes=elementos.map(function(canvas){
  return {canvas:canvas,ctx:canvas.getContext('2d',{alpha:true}),id:Number(canvas.dataset.art),visivel:true,ponteiro:{x:0,y:0},ativo:false};
}).filter(function(x){return !!x.ctx});
var velocidade=document.getElementById('premium-velocidade');
var valorVelocidade=document.getElementById('premium-velocidade-valor');
var velocidadeAtual=1,rodando=true,frame=0,inicio=performance.now();
var reduzir=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
var PI=Math.PI,TAU=PI*2;
function limite(v,a,b){return Math.max(a,Math.min(b,v))}
function interpolar(a,b,p){return a+(b-a)*p}
function radical(ctx,x,y,r,stops){
 var g=ctx.createRadialGradient(x,y,0,x,y,r);
 stops.forEach(function(s){g.addColorStop(s[0],s[1])});
 return g;
}
function luz(ctx,x,y,r,cor,forca){
 ctx.save();ctx.globalAlpha=forca==null?1:forca;
 ctx.fillStyle=radical(ctx,x,y,r,[[0,cor],[.25,cor],[1,'rgba(255,255,255,0)']]);
 ctx.beginPath();ctx.arc(x,y,r,0,TAU);ctx.fill();ctx.restore();
}
function linha(ctx,pontos,cor,largura,alpha){
 ctx.save();ctx.strokeStyle=cor;ctx.globalAlpha=alpha==null?1:alpha;ctx.lineWidth=largura;
 ctx.lineJoin='round';ctx.lineCap='round';ctx.beginPath();
 pontos.forEach(function(p,i){if(!i)ctx.moveTo(p[0],p[1]);else ctx.lineTo(p[0],p[1]);});
 ctx.stroke();ctx.restore();
}
function caminho(ctx,fn,n){
 ctx.beginPath();
 for(var i=0;i<=n;i++){var p=fn(i/n);if(i===0)ctx.moveTo(p[0],p[1]);else ctx.lineTo(p[0],p[1]);}
}
function glowStroke(ctx,w,cor,blur){
 ctx.save();ctx.strokeStyle=cor;ctx.lineWidth=w;ctx.lineCap='round';ctx.lineJoin='round';
 ctx.shadowColor=cor;ctx.shadowBlur=blur||10;ctx.stroke();ctx.restore();
}
function estadoAtual(){
 var state=document.body.dataset.state||'ocioso';
 var model=window.MetrysEstadosLab&&window.MetrysEstadosLab.estado();
 return {nome:state,energia:model?model.energia:(state==='ocioso'?.45:state==='escutando'?1.18:state==='pensando'?.95:1.48)};
}
function orbita(ctx,x,y,r,ang,cor,tamanho){
 var px=x+Math.cos(ang)*r,py=y+Math.sin(ang)*r;
 luz(ctx,px,py,tamanho*2.9,cor,.16);
 ctx.save();ctx.fillStyle=cor;ctx.beginPath();ctx.arc(px,py,tamanho,0,TAU);ctx.fill();ctx.restore();
}
function gradiente(ctx,x1,y1,x2,y2,cores){
 var g=ctx.createLinearGradient(x1,y1,x2,y2);
 cores.forEach(function(c){g.addColorStop(c[0],c[1])});return g;
}
function poligono(ctx,pts,fill,stroke,largura){
 ctx.beginPath();pts.forEach(function(p,i){i?ctx.lineTo(p[0],p[1]):ctx.moveTo(p[0],p[1])});
 ctx.closePath();if(fill){ctx.fillStyle=fill;ctx.fill()}if(stroke){ctx.strokeStyle=stroke;ctx.lineWidth=largura||1;ctx.stroke()}
}
function arte7(ctx,t,e,ponteiro){
 ctx.save();ctx.translate(100+ponteiro.x*4,100+ponteiro.y*4);
 luz(ctx,0,0,68,'#f3b879',.22);
 for(var i=0;i<4;i++){
  ctx.save();ctx.rotate((i%2?-1:1)*t*(.12+i*.032)+i*PI/4);
  ctx.scale(1,.69+i*.05);
  ctx.beginPath();ctx.arc(0,0,48+i*12,PI*(.15+i*.31),PI*(1.7+i*.25));
  ctx.strokeStyle=i%2?'rgba(141,112,79,.40)':'rgba(180,105,49,.54)';
  ctx.lineWidth=i===0?7:1.8;ctx.lineCap='round';ctx.stroke();
  ctx.restore();
 }
 for(var j=0;j<3;j++){
  ctx.save();ctx.rotate(t*(j%2?-0.31:.36)+j*2.1);
  ctx.beginPath();ctx.ellipse(0,0,65-j*9,36+j*5,0,.13,5.8);
  glowStroke(ctx,1.15,j===1?'#c8a77e':'#fffaf0',7);
  ctx.restore();
 }
 var centro=radical(ctx,-4,-6,38,[[0,'#fff9e9'],[.18,'#fff1d6'],[.68,'#e9bd85'],[1,'rgba(194,112,45,.12)']]);
 ctx.fillStyle=centro;ctx.beginPath();ctx.arc(0,0,29+Math.sin(t*2*e)*2,0,TAU);ctx.fill();
 ctx.save();ctx.strokeStyle='rgba(255,255,255,.8)';ctx.lineWidth=2;ctx.beginPath();ctx.arc(-7,-9,17,-2.8,-1);ctx.stroke();ctx.restore();
 orbita(ctx,0,0,74,t*.66,'#b2622d',3.4);orbita(ctx,0,0,54,-t*.81+PI,'#d5a261',2.2);
 if(e>1)for(var k=0;k<2;k++){ctx.beginPath();ctx.strokeStyle='rgba(178,98,45,.22)';ctx.lineWidth=1.4;ctx.arc(0,0,84+k*5,t-k*.15,t+PI*(.8+e*.11));ctx.stroke()}
 ctx.restore();
}
function arte8(ctx,t,e,p){
 ctx.save();ctx.translate(100+p.x*3,100+p.y*3);
 luz(ctx,0,0,78,'#d07e46',.1);
 for(var j=0;j<3;j++){
  ctx.save();ctx.rotate(t*(j%2?-.13:.18)+(j*PI/3));
  ctx.beginPath();ctx.ellipse(0,0,65+j*7,35+j*3,-.4,-1.1,2.85);
  ctx.strokeStyle=j===1?'rgba(250,205,154,.65)':'rgba(183,110,76,.35)';ctx.lineWidth=j===1?1.4:2.5;ctx.stroke();
  ctx.restore();
 }
 ctx.save();ctx.rotate(t*.25);
 var aro=gradiente(ctx,-56,-57,62,61,[[0,'#fff2cd'],[.4,'#cf814e'],[.7,'#f7c38e'],[1,'#644437']]);
 ctx.beginPath();ctx.arc(0,0,45,-1.13,2.55);glowStroke(ctx,13,aro,14);
 ctx.restore();
 ctx.fillStyle=radical(ctx,-10,-16,64,[[0,'#443332'],[.45,'#292329'],[1,'#171a1f']]);
 ctx.beginPath();ctx.arc(0,0,35,0,TAU);ctx.fill();
 ctx.save();ctx.rotate(-t*.27);
 ctx.beginPath();ctx.arc(0,0,43,-1.35,1.67);glowStroke(ctx,4,'#ffd5a7',17);
 ctx.restore();
 ctx.save();ctx.rotate(t*.18);
 ctx.beginPath();ctx.ellipse(0,0,83,44,PI/6,0,TAU);
 ctx.strokeStyle='rgba(255,214,175,.24)';ctx.lineWidth=1;ctx.stroke();
 ctx.restore();
 orbita(ctx,0,0,80,t*.42,'#ffdcac',3);orbita(ctx,0,0,80,-t*.61+PI,'#b97a66',2.4);
 if(e>1.2)luz(ctx,16,5,40,'#efad77',.17);
 ctx.restore();
}
function arte9(ctx,t,e,p){
 ctx.save();ctx.translate(100+p.x*6,100+p.y*4);
 var cores=['rgba(199,130,157,.58)','rgba(245,174,152,.47)','rgba(181,147,185,.38)','rgba(240,205,164,.5)','rgba(201,135,131,.28)'];
 for(var i=0;i<7;i++){
  ctx.save();
  var y=(i-3)*12,wig=Math.sin(t*.52+i*.82)*8;
  ctx.translate(wig,y);
  ctx.rotate(Math.sin(t*.25+i)*.13);
  var x=-65;
  ctx.beginPath();
  ctx.moveTo(x,2);
  ctx.bezierCurveTo(-30,-33-i*2,10,30+Math.sin(t+i)*8,70,-13+i*2);
  ctx.bezierCurveTo(29,25+i*2,-14,39,-65,2);
  ctx.closePath();
  ctx.fillStyle=cores[i%cores.length];ctx.fill();
  ctx.restore();
 }
 for(var j=0;j<6;j++){
  var ang=t*(.12+j*.01)+j*TAU/6;
  var x=Math.cos(ang)*58,y=Math.sin(ang)*37;
  luz(ctx,x,y,17,j%2?'#fff0e1':'#efb7c0',.22);
 }
 ctx.save();ctx.rotate(t*.08);
 ctx.beginPath();ctx.ellipse(0,0,51,21,-.45,0,TAU);
 ctx.strokeStyle='rgba(255,255,255,.66)';ctx.lineWidth=2;ctx.stroke();
 ctx.restore();
 var nucleo=radical(ctx,0,0,35,[[0,'#fffaf4'],[.15,'#f9dfcc'],[.65,'rgba(198,123,143,.25)'],[1,'rgba(198,123,143,0)']]);
 ctx.fillStyle=nucleo;ctx.beginPath();ctx.arc(0,0,34+Math.sin(t*2*e)*2,0,TAU);ctx.fill();
 ctx.restore();
}
function arte10(ctx,t,e,p){
 ctx.save();ctx.translate(100+p.x*4,100+p.y*4);ctx.rotate(t*.10);
 var count=10;
 for(var i=0;i<count;i++){
  ctx.save();ctx.rotate(i*TAU/count+Math.sin(t*.45+i)*.035);
  var l=53+Math.sin(t*1.4+i*.75)*7*e;
  ctx.beginPath();ctx.moveTo(0,-9);
  ctx.bezierCurveTo(-22,-37,-19,-l,0,-l-12);
  ctx.bezierCurveTo(19,-l,22,-37,0,-9);
  ctx.fillStyle=gradiente(ctx,-12,-l,18,0,[[0,i%2?'rgba(173,185,149,.83)':'rgba(196,117,66,.8)'],[1,'rgba(255,250,222,.4)']]);
  ctx.fill();
  ctx.strokeStyle='rgba(244,253,227,.65)';ctx.lineWidth=1.1;ctx.stroke();
  ctx.restore();
 }
 ctx.save();ctx.rotate(-t*.22);
 for(var j=0;j<5;j++){
  var a=j*TAU/5+t*.19;
  var x=Math.cos(a)*75,y=Math.sin(a)*75;
  ctx.fillStyle=j%2?'#b77c4c':'#8ca26a';ctx.beginPath();ctx.arc(x,y,2.4,0,TAU);ctx.fill();
 }
 ctx.restore();
 var core=radical(ctx,-2,-2,33,[[0,'#fff7d3'],[.35,'#f0cb8c'],[.72,'#bd855b'],[1,'rgba(114,129,87,.07)']]);
 ctx.fillStyle=core;ctx.beginPath();ctx.arc(0,0,30+Math.sin(t*1.7*e)*2.5,0,TAU);ctx.fill();
 ctx.save();ctx.strokeStyle='rgba(255,255,238,.73)';ctx.lineWidth=2;ctx.beginPath();ctx.arc(0,0,20,-2.4,-.34);ctx.stroke();ctx.restore();
 ctx.restore();
}
function infinito(u){
 var a=u*TAU,den=1+Math.pow(Math.cos(a),2);
 return [Math.sin(a)*72/den,Math.sin(a)*Math.cos(a)*90/den];
}
function arte11(ctx,t,e,p){
 ctx.save();ctx.translate(100+p.x*3,100+p.y*3);
 ctx.rotate(Math.sin(t*.24)*.12);
 for(var k=0;k<3;k++){
  ctx.save();ctx.scale(1+k*.10,1+k*.11);
  caminho(ctx,function(u){return infinito(u)},180);
  ctx.strokeStyle=k===0?'rgba(174,133,105,.15)':'rgba(233,196,153,.26)';
  ctx.lineWidth=k===0?24:2;ctx.stroke();
  ctx.restore();
 }
 caminho(ctx,infinito,200);
 ctx.strokeStyle='#a77b5a';ctx.lineWidth=18;ctx.lineCap='round';ctx.lineJoin='round';ctx.stroke();
 caminho(ctx,infinito,200);
 ctx.strokeStyle=gradiente(ctx,-65,-29,70,34,[[0,'#ead5b0'],[.22,'#b77c55'],[.46,'#fbdfb9'],[.68,'#8b9a70'],[1,'#f2bf88']]);
 ctx.lineWidth=13;ctx.stroke();
 caminho(ctx,infinito,200);
 ctx.strokeStyle='rgba(255,251,238,.66)';ctx.lineWidth=2;ctx.stroke();
 ctx.save();ctx.setLineDash([13,44,21,31]);ctx.lineDashOffset=-t*40*e;ctx.lineWidth=3;ctx.strokeStyle='rgba(255,255,255,.77)';
 caminho(ctx,infinito,200);ctx.stroke();ctx.restore();
 var pos=infinito((t*.045*e)%1);luz(ctx,pos[0],pos[1],21,'#fff8e7',.45);
 ctx.fillStyle='#fff5d7';ctx.beginPath();ctx.arc(pos[0],pos[1],3.4,0,TAU);ctx.fill();
 ctx.restore();
}
function arte12(ctx,t,e,p){
 ctx.save();ctx.translate(100+p.x*4,100+p.y*5);
 for(var i=0;i<13;i++){
  var v=(i-6)/6;
  var yy=v*48;
  var pts=[];
  for(var j=0;j<=100;j++){
   var x=-78+j*1.56;
   var amplitude=(17+24*(1-Math.abs(v)))*(e*.75+.18);
   var env=Math.pow(Math.max(0,1-Math.pow(x/92,2)),1.7);
   var y=yy+Math.sin(x*.038-t*2.2+(i*.28))*amplitude*env+
    Math.cos(x*.018+t*.7+i*.35)*8*env;
   pts.push([x,y]);
  }
  linea(ctx,pts,i%3===0?'#e3b97b':i%2?'#82b5a6':'#b0d1cc',1.5+i%3*.5,.32+(.45*(1-Math.abs(v))));
 }
 var r=16+4*Math.sin(t*2.3*e);
 luz(ctx,0,0,38,'#e7be7a',.14);
 var g=radical(ctx,-4,-4,r+13,[[0,'#fff4d8'],[.29,'#dfc49d'],[.63,'#608b86'],[1,'rgba(16,35,42,.1)']]);
 ctx.fillStyle=g;ctx.beginPath();ctx.arc(0,0,r,0,TAU);ctx.fill();
 for(var z=0;z<3;z++){var a=t*(z%2?-1:.8)+z*TAU/3;orbita(ctx,0,0,45+z*9,a,'#e0b98e',2);}
 ctx.restore();
}
function linea(ctx,pts,cor,w,a){linha(ctx,pts,cor,w,a)}
function arte13(ctx,t,e,p){
 ctx.save();ctx.translate(100+p.x*4,100+p.y*4);ctx.rotate(t*.13);
 var verts=[];
 for(var i=0;i<8;i++){
  var ang=i*TAU/8-PI/8+t*.028;
  verts.push([Math.cos(ang)*71,Math.sin(ang)*71]);
 }
 var centro=[0,0];
 for(var j=0;j<8;j++){
  var a=verts[j],b=verts[(j+1)%8],shift=Math.sin(t*.7+j)*5;
  poligono(ctx,[a,b,[shift,shift]],gradiente(ctx,a[0],a[1],b[0],b[1],[[0,j%2?'rgba(122,167,154,.52)':'rgba(177,124,92,.48)'],[.6,'rgba(255,255,255,.62)'],[1,'rgba(175,190,179,.3)']]),'rgba(255,255,255,.62)',1.1);
 }
 for(var k=0;k<4;k++){
  ctx.save();ctx.rotate(t*(k%2?-.18:.27)+k*.8);
  ctx.beginPath();ctx.ellipse(0,0,82-k*8,70-k*6,0,-.2,1.6);
  ctx.strokeStyle=k%2?'rgba(100,149,130,.55)':'rgba(177,117,87,.55)';
  ctx.lineWidth=1.25;ctx.stroke();ctx.restore();
 }
 for(var h=0;h<8;h++){
  var theta=h*TAU/8+t*.35;
  orbita(ctx,0,0,71,theta,'#fff8f0',1.9);
 }
 var cy=radical(ctx,0,0,45,[[0,'rgba(255,255,255,.97)'],[.3,'rgba(242,207,174,.6)'],[1,'rgba(207,170,138,.03)']]);
 ctx.fillStyle=cy;ctx.beginPath();ctx.arc(0,0,34+Math.sin(t*2*e)*2,0,TAU);ctx.fill();
 ctx.restore();
}
function arte14(ctx,t,e,p){
 ctx.save();ctx.translate(100+p.x*2.2,100+p.y*2.8);
 var sunY=-18+Math.sin(t*.32)*6;
 luz(ctx,0,sunY,65,'#e8b58e',.24);
 var sol=radical(ctx,-8,sunY-13,56,[[0,'#fff6d8'],[.32,'#eec99c'],[.75,'rgba(204,139,86,.53)'],[1,'rgba(204,139,86,0)']]);
 ctx.fillStyle=sol;ctx.beginPath();ctx.arc(0,sunY,36,0,TAU);ctx.fill();
 for(var k=0;k<6;k++){
  var y=4+k*13;
  var shift=t*(k%2?-.7:.65);
  ctx.beginPath();ctx.moveTo(-90,y);
  for(var x=-90;x<=90;x+=4){
   var y1=y+Math.sin(x*.029+shift+k*.26)*(10+k*.8)*e+
     Math.sin(x*.071-shift*.7+k)*2.7;
   ctx.lineTo(x,y1);
  }
  ctx.strokeStyle=k%3===0?'rgba(150,109,75,.8)':k%2?'rgba(115,140,117,.6)':'rgba(232,185,141,.95)';
  ctx.lineWidth=k===0?3:1.3;ctx.stroke();
 }
 ctx.save();ctx.strokeStyle='rgba(255,247,227,.7)';ctx.lineWidth=1;ctx.beginPath();ctx.arc(0,sunY,46,t*.21,t*.21+PI*1.7);ctx.stroke();ctx.restore();
 for(var j=0;j<4;j++){
  var px=-63+j*43+Math.sin(t*.3+j)*8;
  var py=44+j*3+Math.cos(t*.8+j)*4;
  orbita(ctx,px,py,0,0,'#f9ead5',2.3);
 }
 ctx.restore();
}
var renderers={7:arte7,8:arte8,9:arte9,10:arte10,11:arte11,12:arte12,13:arte13,14:arte14};
function ajustar(cfg){
 var c=cfg.canvas,rect=c.getBoundingClientRect();
 var dpr=Math.min(window.devicePixelRatio||1,2);
 var w=Math.max(1,Math.round(rect.width*dpr)),h=Math.max(1,Math.round(rect.height*dpr));
 if(c.width!==w||c.height!==h){c.width=w;c.height=h}
 cfg.ctx.setTransform(w/200,0,0,h/200,0,0);
}
configuracoes.forEach(function(cfg){
 ajustar(cfg);
 cfg.canvas.addEventListener('pointermove',function(ev){
  var rect=cfg.canvas.getBoundingClientRect();
  cfg.ponteiro.x=limite((ev.clientX-rect.left)/rect.width*2-1,-1,1);
  cfg.ponteiro.y=limite((ev.clientY-rect.top)/rect.height*2-1,-1,1);
 },{passive:true});
 cfg.canvas.addEventListener('pointerleave',function(){cfg.ponteiro.x=0;cfg.ponteiro.y=0});
 cfg.canvas.addEventListener('pointerdown',function(ev){
  var rect=cfg.canvas.getBoundingClientRect();
  cfg.ponteiro.x=limite((ev.clientX-rect.left)/rect.width*2-1,-1,1);
  cfg.ponteiro.y=limite((ev.clientY-rect.top)/rect.height*2-1,-1,1);
 },{passive:true});
});
if(velocidade){
 velocidade.addEventListener('input',function(){
  velocidadeAtual=limite(Number(velocidade.value)||1,.2,2.5);
  if(valorVelocidade)valorVelocidade.textContent=velocidadeAtual.toFixed(1)+'×';
 });
}
if('IntersectionObserver' in window){
 var io=new IntersectionObserver(function(entries){
  entries.forEach(function(en){
   var cfg=configuracoes.find(function(x){return x.canvas===en.target});
   if(cfg)cfg.visivel=en.isIntersecting;
  });
 },{rootMargin:'160px',threshold:0});
 configuracoes.forEach(function(x){io.observe(x.canvas)});
}
window.addEventListener('resize',function(){configuracoes.forEach(ajustar)},{passive:true});
document.addEventListener('visibilitychange',function(){rodando=!document.hidden});
function quadro(now){
 requestAnimationFrame(quadro);
 if(!rodando)return;
 if(now-frame<32)return;
 frame=now;
 var tt=reduzir?1:(now-inicio)/1000*velocidadeAtual;
 var e=estadoAtual().energia;
 configuracoes.forEach(function(cfg){
  if(!cfg.visivel)return;
  var ctx=cfg.ctx;
  try{
   ajustar(cfg);ctx.clearRect(0,0,200,200);
   renderers[cfg.id](ctx,tt,e,cfg.ponteiro);
   if(window.MetrysEstadosLab&&window.MetrysEstadosLab.desenhar)window.MetrysEstadosLab.desenhar(ctx,tt,cfg.id);
  }catch(err){
   if(!cfg.erro){console.warn('Metrys: falha no desenho',cfg.id,err);cfg.erro=true}
  }
 });
}
requestAnimationFrame(quadro);
})();
