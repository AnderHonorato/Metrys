/* Metrys Ateliê: animação procedural ORIGINAL, sem cenários ASCII de terceiros.
 * Respeita redução de movimento, limite de quadros e bateria do Android. */
'use strict';
const MotorAmbiente=(()=>{
 function iniciar(canvas,preferencias){
  const ctx=canvas.getContext('2d',{alpha:true});let w=1,h=1,dpr=1,ultimo=0,quadro=0,ligado=true,tema='mare';
  const gestos=[];const sementes=Array.from({length:22},(_,i)=>({x:(i*67.61%100)/100,y:(i*39.47%100)/100,r:1+i%3,v:.24+(i%6)*.12,angulo:i*2.39}));
  const cor=(nome,padrao)=>getComputedStyle(document.documentElement).getPropertyValue(nome).trim()||padrao;
  function ajustar(){w=Math.max(1,innerWidth);h=Math.max(1,innerHeight);dpr=Math.min(devicePixelRatio||1,1.4);canvas.width=Math.floor(w*dpr);canvas.height=Math.floor(h*dpr);canvas.style.width=w+'px';canvas.style.height=h+'px';ctx.setTransform(dpr,0,0,dpr,0,0);}
  function paleta(){return {a:cor('--acento','#ee7859'),b:cor('--acento2','#298e87'),f:cor('--fundo','#f5f1e8'),p:cor('--pale','#d8eee7')}}
  function arco(x,y,r,angulo,abertura,color,espessura,opacidade){ctx.save();ctx.beginPath();ctx.strokeStyle=color;ctx.lineWidth=espessura;ctx.globalAlpha=opacidade;ctx.arc(x,y,r,angulo,angulo+abertura);ctx.stroke();ctx.restore();}
  function folha(x,y,tamanho,rotacao,color,alpha){ctx.save();ctx.translate(x,y);ctx.rotate(rotacao);ctx.beginPath();ctx.moveTo(0,-tamanho);ctx.bezierCurveTo(tamanho*.7,-tamanho*.5,tamanho*.75,tamanho*.36,0,tamanho);ctx.bezierCurveTo(-tamanho*.6,tamanho*.24,-tamanho*.67,-tamanho*.55,0,-tamanho);ctx.closePath();ctx.globalAlpha=alpha;ctx.fillStyle=color;ctx.fill();ctx.restore();}
  function ondas(t,p){for(let i=0;i<5;i++){ctx.beginPath();const y=h*(.23+i*.175);for(let x=-20;x<=w+20;x+=12){const yy=y+Math.sin(x/120+t*.38+i*1.2)*19+Math.sin(x/58-t*.23+i)*6;if(x===-20)ctx.moveTo(x,yy);else ctx.lineTo(x,yy);}ctx.strokeStyle=i%2?p.b:p.a;ctx.globalAlpha=.05+(i%2)*.026;ctx.lineWidth=1.5;ctx.stroke();}ctx.globalAlpha=1}
  function quadros(t,p){for(let i=0;i<5;i++){const r=76+i*43;arco(w*.84,h*.26,r,t*.025+i*.5,Math.PI*1.4,i%2?p.a:p.b,1.1,.09)}for(let i=0;i<4;i++){arco(w*.08,h*.81,88+i*65,-t*.04+i*.6,Math.PI*.78,p.a,1,.075)}}
  function materia(t,p){for(const [i,s] of sementes.entries()){const x=s.x*w+Math.sin(t*s.v+s.angulo)*16,y=s.y*h+Math.cos(t*s.v*.8+s.angulo)*13;const k=2+s.r*1.2;if(tema==='jardim'||tema==='flora'||tema==='amanhecer'){folha(x,y,k*2.2,t*.1+s.angulo,i%2?p.a:p.b,.14)}else{ctx.beginPath();ctx.fillStyle=i%3===0?p.a:p.b;ctx.globalAlpha=.09+(i%5)*.02;ctx.arc(x,y,k*.6,0,6.3);ctx.fill();ctx.globalAlpha=1;}}}
  function espirais(t,p){const lado=Math.max(w,h);for(let i=0;i<3;i++){ctx.beginPath();for(let k=0;k<70;k++){const r=(k/70)*lado*.4,ang=k*.12+t*.06+i*2.7;const x=w*(.24+i*.33)+Math.cos(ang)*r,y=h*(.25+i*.29)+Math.sin(ang)*r;if(!k)ctx.moveTo(x,y);else ctx.lineTo(x,y);}ctx.strokeStyle=i%2?p.a:p.b;ctx.lineWidth=1.1;ctx.globalAlpha=.065;ctx.stroke();}ctx.globalAlpha=1}
  function quadroAtual(ms){requestAnimationFrame(quadroAtual);if(!ligado||document.hidden)return;const intervalo=preferencias.reduzir||matchMedia('(prefers-reduced-motion:reduce)').matches?220:36;if(ms-ultimo<intervalo)return;ultimo=ms;quadro++;const t=ms/1000*Math.max(.1,Math.min(2,Number(preferencias.velocidade)||1)),p=paleta();ctx.clearRect(0,0,w,h);
   const halo=ctx.createRadialGradient(w*.75,h*.29,10,w*.75,h*.29,Math.max(w,h)*.7);halo.addColorStop(0,p.p);halo.addColorStop(1,'transparent');ctx.globalAlpha=.38;ctx.fillStyle=halo;ctx.fillRect(0,0,w,h);ctx.globalAlpha=1;
   if(tema==='onda'||tema==='mare'||tema==='oceano'||tema==='gelo')ondas(t,p);
   else if(tema==='espiral'||tema==='brasa'||tema==='sol')espirais(t,p);
   else if(tema==='grafite'||tema==='circuito'||tema==='celeste')quadros(t,p);
   else if(tema==='jardim'||tema==='flora'||tema==='amanhecer'){quadros(t*.7,p);ondas(t,p)}
   else quadros(t,p);
   materia(t,p);
   for(let i=gestos.length-1;i>=0;i--){const g=gestos[i];g.r+=2.7;g.alpha-=.018;if(g.alpha<=0){gestos.splice(i,1);continue}arco(g.x,g.y,g.r,0,Math.PI*2,p.b,1.1,g.alpha)}
  }
  function impulso(x,y){if(preferencias.reduzir)return;gestos.push({x,y,r:4,alpha:.45});if(gestos.length>6)gestos.shift();}
  function toque(ev){if(ev.target.closest('button,input,textarea,.modal,#lateral,#dock,.mensagem'))return;impulso(ev.clientX,ev.clientY)}
  ajustar();window.addEventListener('resize',ajustar);window.addEventListener('pointerdown',toque,{passive:true});requestAnimationFrame(quadroAtual);
  return {trocar:(novo)=>{tema=String(novo||'mare');gestos.length=0;},impulso,pausar:(v)=>{ligado=!v},ajustar,parar:()=>{ligado=false;window.removeEventListener('resize',ajustar);window.removeEventListener('pointerdown',toque)}};
 }
 return {iniciar};
})();
