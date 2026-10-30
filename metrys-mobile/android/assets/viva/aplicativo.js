/* Metrys Viva — UI em PT-BR. Cenários originais de Lu1sR0/claudinho, MIT. */
'use strict';
const $ = (id) => document.getElementById(id);
const elementos = Object.fromEntries(['cenario','lateral','sombraLateral','parar','menu','fecharLateral','mensagens','mensagensRolagem','boasVindas','tituloConversa','campoMensagem','enviar','arvoreConversas','totalConversas','sobreposicao','tituloModal','modalConteudo','andamento','tituloPensamento','tempoPensamento','conteudoPensamento','abrirPensamento','fila','mascotePequeno','mascotePixel','falaMascote','selecionarArquivo','anexoPendente','temaAtual','aviso','mascoteGrande','modoModelo'].map(id=>[id,$(id)]));
const e = elementos;
const ponte = window.Metrys || null;
const guardar=(chave,valor)=>{try{localStorage.setItem('metrys_viva_'+chave,JSON.stringify(valor));}catch(err){}};
const ler=(chave,padrao)=>{try{const v=localStorage.getItem('metrys_viva_'+chave);return v===null?padrao:JSON.parse(v);}catch(err){return padrao;}};
const escapar=(v)=>String(v??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
const icone=(nome)=>`<svg aria-hidden="true"><use href="#i-${nome}"></use></svg>`;
const rascunho={textoAnexado:'',nomeAnexo:''};
let estado={fase:'preparando',detalhe:'Preparando o modelo…',gerando:false,modeloPronto:false,conversaId:'',nomeModelo:'Modelo offline',conversas:[],projetos:[],mensagens:[],etapas:[],filaPendente:0,tempoGeracaoMs:0,raciocinioPublicado:'',raciocinando:false,trechosGerados:0,bytes:0,total:1};
let tituloAnterior='',ultimoDesenho='',ultimoResumo='',ultimoPensamento='',conversaAnterior='',avisoTimer=0,modalAtual='',raciocinios=ler('raciocinios',{});
let preferencias=Object.assign({tema:'lagoon',acento:'#dc966c',escurecer:6,opacidade:57,desfoque:16,tamanho:14,largura:690,velocidade:1,sons:false,reduzir:false},ler('preferencias',{}));
const TEMAS_NOMES={claude:'Brisa Coral',clouds:'Nuvens',lagoon:'Honu Lagoon',koi:'Lago de Carpas',jelly:'Águas Luminosas',boats:'Barquinhos',fireflies:'Vagalumes',moonlit:'Noite de Lua',lareira:'Lareira',chuva:'Chuva Lo-fi',galaxia:'Galáxia Distante',sakura:'Jardim Sakura',aurora:'Aurora',codigo:'Chuva de Código',arcade:'Arcade'};
const COR_TEMAS={claude:['#221e1c','#cf775c'],clouds:['#67ade5','#fdfbfb'],lagoon:['#a2f4e8','#0b92be'],koi:['#163b4f','#fc9d63'],jelly:['#0a264c','#6fbbea'],boats:['#87b4ee','#f0e7c0'],fireflies:['#0b1c19','#d9f47c'],moonlit:['#131e53','#eee6c6'],lareira:['#281710','#eb9a43'],chuva:['#1c2640','#c4b2e8'],galaxia:['#090c28','#9d87ff'],sakura:['#ffd9e6','#ed7eac'],aurora:['#060c21','#7bffbf'],codigo:['#07151b','#72e7af'],arcade:['#0b0823','#fa8b78']};
let atualTema=null,renderizador=null,ultimoQuadro=0,tempoAnimado=0,ponteiro={x:0,y:0,speed:0},ativo=true;
function aviso(texto){clearTimeout(avisoTimer);e.aviso.textContent=texto;e.aviso.classList.add('ativo');avisoTimer=setTimeout(()=>e.aviso.classList.remove('ativo'),2400);}
function usarPonte(nome,...args){try{if(ponte&&typeof ponte[nome]==='function')return ponte[nome](...args);}catch(ex){aviso('Não foi possível concluir esta ação.');console.error(nome,ex);}return null;}
function guardarPreferencias(){guardar('preferencias',preferencias);usarPonte('salvarPreferencias',JSON.stringify(preferencias));}
function aplicarPreferencias(){
 const nome=preferencias.tema in TEMAS_NOMES?preferencias.tema:'lagoon';const escolhido=THEMES.find(t=>t.id===nome)||THEMES[0];
 const muda=atualTema?.id!==escolhido.id;const antigo=atualTema;
 if(muda&&renderizador&&antigo){try{renderizador.transicao(innerWidth*.52,innerHeight*.42,tempoAnimado,1.12);}catch(err){}}
 atualTema=escolhido;e.temaAtual.textContent=TEMAS_NOMES[nome]||escolhido.name;
 document.body.classList.toggle('claro',Boolean(escolhido.light));
 const css=document.documentElement.style;
 css.setProperty('--acento',preferencias.acento);css.setProperty('--acento2',preferencias.acento);
 css.setProperty('--escurecer',String(Number(preferencias.escurecer)/100));css.setProperty('--blur',preferencias.desfoque+'px');css.setProperty('--fonte',preferencias.tamanho+'px');css.setProperty('--largura',preferencias.largura+'px');
 const r=parseInt(preferencias.opacidade,10)/100;css.setProperty('--vidro',escolhido.light?`rgba(250,253,255,${r})`:`rgba(12,13,25,${r})`);
 if(muda&&renderizador){renderizador.reset();}
 document.body.classList.toggle('reduzido',Boolean(preferencias.reduzir));
}
function iniciarCenario(){
 try{const c=e.cenario;renderizador=Renderer(c,{cw:13,ch:13,ss:2,dpr:1,k:Math.min(innerWidth,innerHeight)/1000});
  const ajustar=()=>renderizador.resize(innerWidth,innerHeight);ajustar();window.addEventListener('resize',ajustar);
  let ultimoPonto={x:0,y:0,t:performance.now()};
  document.addEventListener('pointermove',(ev)=>{const agora=performance.now(),dt=Math.max(8,agora-ultimoPonto.t);ponteiro={x:ev.clientX,y:ev.clientY,speed:Math.hypot(ev.clientX-ultimoPonto.x,ev.clientY-ultimoPonto.y)*1000/dt};ultimoPonto={x:ev.clientX,y:ev.clientY,t:agora};if(renderizador&&!preferencias.reduzir)renderizador.mark(ev.clientX,ev.clientY);},{passive:true});
  e.cenario.addEventListener('pointerdown',(ev)=>{if(!atualTema)return;try{if(typeof atualTema.click==='function')atualTema.click(ev.clientX,ev.clientY,tempoAnimado,renderizador);}catch(err){}},{passive:true});
  window.addEventListener('pointerdown',ev=>{if(ev.target.closest('button,input,textarea,.modal,#lateral,#dock'))return;try{atualTema?.click?.(ev.clientX,ev.clientY,tempoAnimado,renderizador);}catch(err){}},{passive:true});
  const quadro=(tempo)=>{requestAnimationFrame(quadro);if(document.hidden||!ativo||!renderizador||!atualTema)return;if(tempo-ultimoQuadro<(preferencias.reduzir?130:46))return;const intervalo=Math.min(.055,Math.max(.016,(tempo-ultimoQuadro)/1000||.04));ultimoQuadro=tempo;tempoAnimado+=intervalo*preferencias.velocidade;
   try{renderizador.frame(atualTema,tempoAnimado,intervalo,ponteiro)}catch(ex){console.error('Cenário:',ex);renderizador=null;e.cenario.style.background='linear-gradient(135deg,#0c102c,#1f174c)';}
  };requestAnimationFrame(quadro);
 }catch(ex){console.warn('Cenário indisponível:',ex);e.cenario.style.background='linear-gradient(145deg,#112846,#211239)';}
}
function abrirLateral(){document.body.classList.add('lateral-aberta');e.sombraLateral.hidden=false;}
function fecharLateral(){document.body.classList.remove('lateral-aberta');e.sombraLateral.hidden=true;}
function montarLateral(){
 const filtro=$('buscarConversas').value.toLocaleLowerCase('pt-BR').trim();const conversas=(estado.conversas||[]).filter(c=>!filtro||String(c.titulo).toLocaleLowerCase('pt-BR').includes(filtro));const grupos=estado.projetos||[];
 e.totalConversas.textContent=conversas.length?String(conversas.length):'';
 let html='';const semProjeto=conversas.filter(c=>!c.projetoId);
 const lista=(dados)=>dados.map(c=>`<button class="conversa-item ${c.id===estado.conversaId?'ativa':''}" data-conversa="${escapar(c.id)}" title="${escapar(c.titulo)}">${escapar(c.titulo||'Nova conversa')}</button>`).join('');
 if(semProjeto.length){html+=`<div class="item-grupo">${icone('folder')} Conversas</div>`+lista(semProjeto);}
 for(const p of grupos){const dados=conversas.filter(c=>c.projetoId===p.id);if(dados.length||!filtro)html+=`<div class="item-grupo">${icone('folder')} ${escapar(p.nome)}</div>`+lista(dados);}
 e.arvoreConversas.innerHTML=html||'<div class="item-discreto">Nenhuma conversa encontrada.</div>';
}
function textoEmHtml(texto){
 const codigos=[];
 let s=String(texto??'').replace(/```([^\n`]*)\n([\s\S]*?)```/g,(t,ling,body)=>{const i=codigos.length;codigos.push({ling:ling.trim().slice(0,35)||'texto',body});return `\n@@BLOCO_${i}@@\n`;});
 s=escapar(s);
 s=s.replace(/\*\*(.*?)\*\*/g,'<strong>$1</strong>');s=s.replace(/(^|\s)`([^`\n]+)`/g,'$1<code>$2</code>');
 s=s.replace(/^###?\s+(.+)$/gm,'<h3>$1</h3>').replace(/^##\s+(.+)$/gm,'<h2>$1</h2>');
 s=s.split(/\n\s*\n/g).map(par=>{if(!par.trim())return '';if(/^<h[23]>/.test(par))return par;if(/^@@BLOCO_\d+@@$/.test(par.trim()))return par.trim();return `<p>${par.replace(/\n/g,'<br>')}</p>`;}).join('');
 return {html:s,codigos};
}
const blocosCodigo=[];
function htmlMensagem(texto){let r=textoEmHtml(texto);return r.html.replace(/@@BLOCO_(\d+)@@/g,(total,n)=>{const v=r.codigos[Number(n)];if(!v)return '';const id=blocosCodigo.push(v)-1;return `<div class="codigo-terminal"><div class="terminal-titulo"><span class="terminal-bolinhas"><i></i><i></i><i></i></span><strong>${escapar(v.ling)}</strong><button data-codigo="${id}" data-acao="copiar-codigo">Copiar</button><button data-codigo="${id}" data-acao="salvar-codigo">Salvar</button>${/html/i.test(v.ling)?`<button data-codigo="${id}" data-acao="preview">Prévia</button>`:''}</div><pre><code>${escapar(v.body)}</code></pre></div>`;});}
function criarMensagem(msg,indice,lista){
 const usuario=msg.papel==='user',id='mensagem_'+indice,texto=String(msg.texto||'');const classe=usuario?'usuario':'ia';
 const corpo=usuario?`<div class="balao-usuario">${escapar(texto)}</div>`:`<div class="corpo-resposta">${htmlMensagem(texto)}</div>`;
 const botoes=usuario?['copy','share']:['copy','share','like','dislike',...((indice===lista.length-1)?['refresh']:[])];
 const acoes=botoes.map(acao=>{let titulo={copy:'Copiar',share:'Compartilhar',like:'Gostei',dislike:'Não gostei',refresh:'Regenerar resposta'}[acao];return `<button aria-label="${titulo}" title="${titulo}" data-acao="${acao}" data-indice="${indice}">${icone({copy:'copy',share:'share',like:'like',dislike:'dislike',refresh:'refresh'}[acao])}</button>`;}).join('');
 let pensamento='';const chave=estado.conversaId+':'+indice;const registro=raciocinios[chave];if(!usuario&&registro?.texto)pensamento=`<details class="pensamento-msg"><summary>${icone('code')} ${escapar(registro.titulo||'Processamento da IA')}</summary><div class="texto-pensamento">${escapar(registro.texto)}</div></details>`;
 return `<article class="mensagem ${classe}" id="${id}">${pensamento}${corpo}<div class="acoes-msg">${acoes}</div></article>`;
}
function definirTitulo(){const c=(estado.conversas||[]).find(c=>c.id===estado.conversaId);const titulo=c?.titulo||'Nova conversa';if(titulo===tituloAnterior)return;tituloAnterior=titulo;e.tituloConversa.textContent=titulo;}
function pintarMensagens(){
 const lista=estado.mensagens||[];const assinatura=estado.conversaId+'|'+lista.map(m=>(m.papel||'')+':'+(m.texto||'').length+':'+(m.texto||'').slice(-32)).join(';');
 if(assinatura===ultimoDesenho)return;ultimoDesenho=assinatura;blocosCodigo.length=0;
 const sc=e.mensagensRolagem;const pertoDoFim=sc.scrollHeight-sc.scrollTop-sc.clientHeight<170;
 if(!lista.length){e.mensagens.innerHTML='';e.mensagens.appendChild(e.boasVindas);e.boasVindas.hidden=false;return;}
 const mensagensHtml=lista.map((m,i)=>criarMensagem(m,i,lista)).join('');
 e.mensagens.innerHTML=mensagensHtml;
 if(pertoDoFim||conversaAnterior!==estado.conversaId)requestAnimationFrame(()=>sc.scrollTo({top:sc.scrollHeight,behavior:'auto'}));
 conversaAnterior=estado.conversaId;
}
function tituloPorRaciocinio(texto){const limpo=String(texto||'').replace(/<\/?think>/g,'').replace(/\s+/g,' ').trim();if(!limpo)return 'Preparando a resposta…';const primeira=limpo.split(/(?<=[.!?])\s+/)[0]||limpo;const truncado=primeira.length>68?primeira.slice(0,65).trim()+'…':primeira;return truncado||'Processando…';}
function pintarAndamento(){
 const pensando=Boolean(estado.gerando||estado.raciocinando);e.andamento.hidden=!pensando;if(!pensando){document.body.classList.remove('gerando');return;}
 document.body.classList.add('gerando');const titulo=tituloPorRaciocinio(estado.raciocinioPublicado);e.tituloPensamento.textContent=titulo;
 e.tempoPensamento.textContent=estado.tempoGeracaoMs?Math.floor(estado.tempoGeracaoMs/1000)+'s':'';
 const texto=String(estado.raciocinioPublicado||'').trim();e.conteudoPensamento.textContent=texto||'O modelo ainda não publicou detalhes do processamento.';
 const lista=estado.mensagens||[];const indice=lista.length-1;
 if(texto&&indice>=0){const ultima=lista[indice];const alvo=ultima.papel==='assistant'?indice:indice+1;const chave=estado.conversaId+':'+alvo;const reg={titulo,texto:texto.slice(-12000)};
  if(!raciocinios[chave]||raciocinios[chave].texto!==reg.texto){raciocinios[chave]=reg;const chaves=Object.keys(raciocinios);if(chaves.length>50)delete raciocinios[chaves[0]];guardar('raciocinios',raciocinios);}
 }
}
function pintarStatus(){
 const fila=estado.filaPendente||0;e.fila.hidden=fila<=0;e.fila.textContent=`${fila} ${fila===1?'mensagem aguardando':'mensagens aguardando'} · o Metrys responderá em sequência`;
 e.modoModelo.textContent=(estado.nomeModelo||'Modelo').replace(/Metrys\s*/i,'').split('·')[0].trim().slice(0,16)||'Modelo';
 if(estado.fase==='erro'&&!estado.gerando&&estado.detalhe&&estado.detalhe!==ultimoResumo){ultimoResumo=estado.detalhe;aviso(estado.detalhe.slice(0,100));}
}
function receberEstado(novo){try{const dados=typeof novo==='string'?JSON.parse(novo):novo;if(!dados||typeof dados!=='object')return;estado=Object.assign(estado,dados);e.parar.hidden=!estado.gerando;definirTitulo();montarLateral();pintarAndamento();pintarMensagens();pintarStatus();definirHumor(estado.gerando?'trabalhando':'calmo');}catch(ex){console.error('Estado inválido:',ex);}}
window.receberEstado=receberEstado;
window.receberAnexo=(nome,texto,erro)=>{if(erro){aviso(String(erro));return;}rascunho.textoAnexado=String(texto||'');rascunho.nomeAnexo=String(nome||'texto.txt');e.anexoPendente.hidden=false;e.anexoPendente.innerHTML=`${icone('folder')} <button id="editarAnexo">${escapar(rascunho.nomeAnexo)} · editar</button> <button id="removerAnexo">Remover</button>`;$('editarAnexo').onclick=editarAnexo;$('removerAnexo').onclick=()=>{rascunho.textoAnexado='';rascunho.nomeAnexo='';e.anexoPendente.hidden=true;};};
function enviarMensagem(textoPersonalizado){let texto=typeof textoPersonalizado==='string'?textoPersonalizado:e.campoMensagem.value;texto=texto.trim();if(rascunho.textoAnexado){texto+=(texto?'\n\n':'')+`[Arquivo ${rascunho.nomeAnexo}]\n${rascunho.textoAnexado}`;}
 if(!texto){aviso('Escreva uma mensagem para começar.');return;}
 if(!ponte){aviso('Prévia visual. Abra no aplicativo Android para usar a IA.');e.campoMensagem.value='';return;}
 usarPonte('enviar',texto);e.campoMensagem.value='';rascunho.textoAnexado='';rascunho.nomeAnexo='';e.anexoPendente.hidden=true;redimensionarCampo();definirHumor('trabalhando');}
function redimensionarCampo(){e.campoMensagem.style.height='auto';e.campoMensagem.style.height=Math.min(160,e.campoMensagem.scrollHeight)+'px';}
function editarAnexo(){abrirModal('Editar anexo',`<p class="aviso-modal">${escapar(rascunho.nomeAnexo||'Texto anexado')}</p><textarea id="textoAnexoEditor" style="width:100%;min-height:200px;color:var(--texto);background:var(--pill);border:1px solid var(--linha);border-radius:9px;padding:12px;resize:vertical;user-select:text"></textarea><button class="botao-padrao" id="salvarAnexo">Salvar alterações</button>`);$('textoAnexoEditor').value=rascunho.textoAnexado;$('salvarAnexo').onclick=()=>{rascunho.textoAnexado=$('textoAnexoEditor').value;fecharModal();aviso('Texto atualizado.');};}
function abrirModal(titulo,conteudo){modalAtual=titulo;e.tituloModal.textContent=titulo;e.modalConteudo.innerHTML=conteudo;e.sobreposicao.hidden=false;}
function fecharModal(){e.sobreposicao.hidden=true;modalAtual='';}
function pedirNovaConversa(){usarPonte('novaConversa');fecharLateral();fecharModal();aviso('Nova conversa criada.');}
function escolherConversa(id){if(!id)return;usarPonte('selecionarConversa',id);fecharLateral();}
function mostrarTemas(){const lista=THEMES.map(t=>{const nome=TEMAS_NOMES[t.id]||t.name;const cor=COR_TEMAS[t.id]||['#262541','#e4b69a'];return `<button class="tema-item ${preferencias.tema===t.id?'ativo':''}" data-tema="${escapar(t.id)}"><div class="miniatura-tema" style="background:linear-gradient(125deg,${cor[0]},${cor[1]})"><span style="color:${t.light?'#214752':'#fbf4ff'}">░▒▓ ✧ ▓▒░</span></div><b>${escapar(nome)}</b><small>${t.light?'Cenário claro':'Cenário escuro'}</small></button>`;}).join('');abrirModal('Temas vivos',`<p class="aviso-modal">15 cenários interativos. Toque para aplicar instantaneamente.</p><div class="grade-temas">${lista}</div><p class="aviso-modal" style="margin-top:15px">Cenários adaptados de Claudinho (licença MIT). Identidade Metrys independente.</p>`);}
const PALETAS=[['Coral','#dc966c'],['Oceano','#5bc9ed'],['Lavanda','#ab96f4'],['Floresta','#76cfaf'],['Ouro','#e9bf67'],['Rosa','#f399bf'],['Neon','#72e8db'],['Violeta','#9c7dff']];
function mostrarPersonalizar(aba='Cores'){
 abrirModal('Personalizar Metrys',`<div class="abas">${['Cores','Fundo','Interface','Animações'].map(n=>`<button class="${n===aba?'ativa':''}" data-aba="${n}">${n}</button>`).join('')}</div><div id="preferenciasConteudo"></div>`);
 const area=$('preferenciasConteudo');
 if(aba==='Cores')area.innerHTML=`<section class="grupo-preferencias"><h3>Paletas</h3><div class="paletas">${PALETAS.map(p=>`<button data-acento="${p[1]}"><i style="background:${p[1]}"></i>${p[0]}</button>`).join('')}</div></section><section class="grupo-preferencias"><h3>Cores personalizadas</h3><label>Cor de destaque <input type="color" data-config="acento" value="${preferencias.acento}" /></label></section><section class="grupo-preferencias"><h3>Vidro da interface</h3><label>Opacidade <input type="range" min="10" max="97" value="${preferencias.opacidade}" data-config="opacidade"/><span>${preferencias.opacidade}%</span></label><label>Desfoque <input type="range" min="0" max="30" value="${preferencias.desfoque}" data-config="desfoque"/><span>${preferencias.desfoque}px</span></label></section>`;
 else if(aba==='Fundo')area.innerHTML=`<section class="grupo-preferencias"><h3>Cenário atual</h3><p class="aviso-modal">${escapar(TEMAS_NOMES[preferencias.tema])}. Use Temas para escolher qualquer um dos quinze cenários.</p><button class="botao-padrao" id="mudarCenario">Escolher tema</button></section><section class="grupo-preferencias"><label>Escurecer fundo <input type="range" min="0" max="80" value="${preferencias.escurecer}" data-config="escurecer"/><span>${preferencias.escurecer}%</span></label></section><section class="grupo-preferencias"><h3>Imagem ou GIF de fundo</h3><p class="aviso-modal">Você pode aplicar uma imagem desta sessão, sem enviar nada para a internet.</p><input id="imagemFundo" type="file" accept="image/png,image/jpeg,image/webp,image/gif" /><button class="botao-padrao" id="removerFundo">Restaurar cenário animado</button></section>`;
 else if(aba==='Interface')area.innerHTML=`<section class="grupo-preferencias"><h3>Texto e largura</h3><label>Tamanho do texto <input type="range" min="11" max="20" step="1" value="${preferencias.tamanho}" data-config="tamanho"/><span>${preferencias.tamanho}px</span></label><label>Largura do chat <input type="range" min="400" max="1060" step="20" value="${preferencias.largura}" data-config="largura"/><span>${preferencias.largura}px</span></label></section><section class="grupo-preferencias"><h3>Visualização</h3><p class="aviso-modal">Os ajustes são aplicados à conversa real. Feche o editor para visualizar o resultado completo.</p><button class="botao-padrao" id="restaurarVisual">Restaurar aparência padrão</button></section>`;
 else area.innerHTML=`<section class="grupo-preferencias"><h3>Movimento do cenário</h3><label>Velocidade <input type="range" min="0.25" max="2" step="0.25" value="${preferencias.velocidade}" data-config="velocidade"/><span>${preferencias.velocidade}×</span></label><label>Reduzir animações <input type="checkbox" data-config="reduzir" ${preferencias.reduzir?'checked':''} /></label></section><section class="grupo-preferencias"><h3>Sons</h3><label>Efeitos de interação <input type="checkbox" data-config="sons" ${preferencias.sons?'checked':''}/></label><p class="aviso-modal">Os sons são curtos e originais. Não há música de terceiros incluída.</p></section>`;
}
function mostrarModelos(){let modelos=[];try{modelos=JSON.parse(usarPonte('modelos')||'[]');}catch(err){}
 const lista=modelos.map(m=>`<div class="item-modelo"><b>${escapar(m.nome||m.id)}</b><p>${escapar(m.descricao||'Modelo offline compatível com o Metrys.')}</p><button data-modelo="${escapar(m.id)}">Selecionar</button></div>`).join('');
 abrirModal('Modelos e ajustes',`<div class="status-modelo"><b>${escapar(estado.nomeModelo||'Modelo local')}</b><p>${escapar(estado.detalhe||'')}</p></div>${lista||'<p class="aviso-modal">Preparando catálogo de modelos…</p>'}<div class="status-modelo"><button id="baixarModelo" class="botao-padrao">Baixar modelo selecionado</button><p>O download só utiliza a internet quando você autoriza. Os modelos já baixados não são apagados.</p></div>`);
}
function executarAcao(acao,indice){const msg=estado.mensagens?.[indice];const texto=msg?.texto||'';if(acao==='copy')usarPonte('copiar',texto)||copiarFallback(texto);else if(acao==='share')usarPonte('compartilhar',texto);else if(acao==='like'||acao==='dislike'){usarPonte('avaliar',estado.conversaId,texto,acao==='like'?1:-1);aviso(acao==='like'?'Avaliação positiva registrada.':'Avaliação negativa registrada.');}else if(acao==='refresh'){usarPonte('refazerResposta');aviso('Regeneração solicitada.');}}
function copiarFallback(texto){if(!ponte&&navigator.clipboard?.writeText)navigator.clipboard.writeText(texto).then(()=>aviso('Copiado'));}
function mostrarCodigo(indice,acao){const c=blocosCodigo[indice];if(!c)return;if(acao==='copiar-codigo'){usarPonte('copiar',c.body);aviso('Código copiado.');}else if(acao==='salvar-codigo'){usarPonte('salvarTexto',c.body,`codigo.${c.ling.replace(/[^a-z0-9]/gi,'').slice(0,8)||'txt'}`);}
 else if(acao==='preview')abrirModal('Prévia do código',`<p class="aviso-modal">Prévia isolada. Scripts não têm acesso aos arquivos do Metrys.</p><iframe id="preverCodigo" sandbox="allow-scripts" style="width:100%;height:52vh;background:white;border:1px solid var(--linha);border-radius:11px"></iframe>`),$('preverCodigo').srcdoc=c.body;
}
function configurarEventos(){
 $('menu').onclick=abrirLateral;e.fecharLateral.onclick=fecharLateral;e.sombraLateral.onclick=fecharLateral;
 $('novaConversa').onclick=pedirNovaConversa;
 $('buscarConversas').addEventListener('input',montarLateral);
 e.arvoreConversas.addEventListener('click',ev=>{const btn=ev.target.closest('[data-conversa]');if(btn)escolherConversa(btn.dataset.conversa);});
 $('abrirTemas').onclick=$('temasTopo').onclick=mostrarTemas;
 $('abrirPersonalizar').onclick=$('configTopo').onclick=()=>mostrarPersonalizar();
 $('abrirModelos').onclick=$('modoModelo').onclick=mostrarModelos;
 $('modoRaciocinio').onclick=()=>aviso('O Metrys mostra apenas o raciocínio publicado pelo modelo.');
 $('abrirClassico').onclick=()=>usarPonte('abrirClassico');
 $('fecharModal').onclick=fecharModal;e.sobreposicao.addEventListener('click',ev=>{if(ev.target===e.sobreposicao)fecharModal();});
 e.enviar.onclick=()=>enviarMensagem();e.parar.onclick=()=>usarPonte('parar');
 e.campoMensagem.addEventListener('input',redimensionarCampo);
 e.campoMensagem.addEventListener('keydown',ev=>{if(ev.key==='Enter'&&!ev.shiftKey&&!ev.isComposing){ev.preventDefault();enviarMensagem();}});
 e.campoMensagem.addEventListener('paste',ev=>{const valor=ev.clipboardData?.getData('text/plain')||'';if(valor.length<1400)return;ev.preventDefault();rascunho.textoAnexado=valor;rascunho.nomeAnexo='Texto colado.txt';window.receberAnexo(rascunho.nomeAnexo,valor,'');aviso('Texto longo anexado. Toque para editar.');});
 e.mensagens.addEventListener('click',ev=>{const btn=ev.target.closest('[data-acao]');if(!btn)return;let a=btn.dataset.acao;if(a.endsWith('codigo')||a==='preview')mostrarCodigo(Number(btn.dataset.codigo),a);else executarAcao(a,Number(btn.dataset.indice));});
 $('sugestoes').addEventListener('click',ev=>{const btn=ev.target.closest('[data-prompt]');if(btn){e.campoMensagem.value=btn.dataset.prompt;redimensionarCampo();e.campoMensagem.focus();}});
 $('anexar').onclick=()=>{if(ponte)usarPonte('abrirArquivo');else e.selecionarArquivo.click();};
 e.selecionarArquivo.addEventListener('change',()=>{const arq=e.selecionarArquivo.files?.[0];if(!arq)return;if(arq.size>1000000){aviso('Arquivo grande: selecione um texto menor.');return;}const fr=new FileReader();fr.onload=()=>window.receberAnexo(arq.name,String(fr.result),'');fr.readAsText(arq);});
 $('som').onclick=()=>{preferencias.sons=!preferencias.sons;guardarPreferencias();aviso(preferencias.sons?'Sons ligados.':'Sons desligados.');if(preferencias.sons)tocarSom();};
 e.abrirPensamento.onclick=()=>{const abrir=e.conteudoPensamento.hidden;e.conteudoPensamento.hidden=!abrir;e.abrirPensamento.setAttribute('aria-expanded',String(abrir));};
 e.modalConteudo.addEventListener('click',ev=>{const t=ev.target.closest('button');if(!t)return;
  if(t.dataset.tema){preferencias.tema=t.dataset.tema;aplicarPreferencias();guardarPreferencias();mostrarTemas();tocarSom();}
  if(t.dataset.aba)mostrarPersonalizar(t.dataset.aba);
  if(t.dataset.acento){preferencias.acento=t.dataset.acento;aplicarPreferencias();guardarPreferencias();mostrarPersonalizar('Cores');}
  if(t.dataset.modelo){usarPonte('selecionarModelo',t.dataset.modelo);aviso('Modelo selecionado.');fecharModal();}
  if(t.id==='baixarModelo'){usarPonte('baixarModelo');aviso('Download iniciado.');fecharModal();}
  if(t.id==='mudarCenario')mostrarTemas();
  if(t.id==='removerFundo'){e.cenario.style.opacity='1';e.cenario.style.background='';e.cenario.style.backgroundImage='';}
  if(t.id==='restaurarVisual'){preferencias=Object.assign(preferencias,{acento:'#dc966c',escurecer:6,opacidade:57,desfoque:16,tamanho:14,largura:690});aplicarPreferencias();guardarPreferencias();mostrarPersonalizar('Interface');}
 });
 e.modalConteudo.addEventListener('input',ev=>{const alvo=ev.target,k=alvo.dataset.config;if(!k)return;preferencias[k]=alvo.type==='checkbox'?alvo.checked:alvo.type==='range'?Number(alvo.value):alvo.value;aplicarPreferencias();guardarPreferencias();if(alvo.parentElement?.lastElementChild?.tagName==='SPAN')alvo.parentElement.lastElementChild.textContent=alvo.value+(k==='opacidade'||k==='escurecer'?'%':k==='desfoque'||k==='tamanho'?'px':k==='velocidade'?'×':'px');});
 e.modalConteudo.addEventListener('change',ev=>{if(ev.target.id==='imagemFundo'){const arq=ev.target.files?.[0];if(!arq)return;if(arq.size>7_000_000){aviso('Limite de 7 MB para este fundo.');return;}const url=URL.createObjectURL(arq);e.cenario.style.backgroundImage=`url("${url}")`;e.cenario.style.backgroundSize='cover';e.cenario.style.backgroundPosition='center';e.cenario.style.opacity='1';ativo=false;aviso('Imagem aplicada nesta sessão.');}});
 document.addEventListener('visibilitychange',()=>{ativo=!document.hidden;});
 document.addEventListener('keydown',ev=>{if(ev.key==='Escape'){if(!e.sobreposicao.hidden)fecharModal();else fecharLateral();}});
}
let ultimoHumor='calmo',mascoteQuadro=0,mascoteX=0,mascoteY=0,mascoteVY=0,arrastando=false,mascoteFalaTimer=0;
const PIXELS=[
 '...aa...aa...',
 '..abbb.bbb...',
 '.abbbbbbbbba.',
 '.bbbbbbbbbbb.',
 'bbbbbbbbbbbbb',
 'bbwwbbbbwbbbb',
 'bbwwbbbbwbbbb',
 'bbbbbbbbbbbbb',
 '.bbbbpppbbbb.',
 '..bbbbbbbbb..',
 '..bb.....bb..',
 '.bb.......bb.',
];
function definirHumor(h){ultimoHumor=h;}
function falarMascote(fala){clearTimeout(mascoteFalaTimer);e.falaMascote.hidden=false;e.falaMascote.textContent=fala;mascoteFalaTimer=setTimeout(()=>e.falaMascote.hidden=true,2500);}
function desenharMascote(canvas,tempo=0){if(!canvas)return;const ctx=canvas.getContext('2d'),w=canvas.width,h=canvas.height;ctx.clearRect(0,0,w,h);const tamanho=Math.min(w/14,h/13),x=(w-13*tamanho)/2,y=(h-12*tamanho)/2;const inclinacao=ultimoHumor==='trabalhando'?Math.sin(tempo/150)*1:Math.sin(tempo/600)*.45;const paleta={a:'#8062c7',b:'#b899ef',p:'#edb2d1',w:'#f6f2fc'};
 PIXELS.forEach((linha,j)=>[...linha].forEach((p,i)=>{if(p==='.')return;const dx=(j>=10?0:Math.sin(tempo/850+i*.3)*.09),dy=(j<=3?inclinacao:0);ctx.fillStyle=paleta[p]||paleta.b;ctx.fillRect(Math.round(x+(i+dx)*tamanho),Math.round(y+(j+dy)*tamanho),Math.ceil(tamanho),Math.ceil(tamanho));}));
 ctx.fillStyle='#2e2445';let olho=ultimoHumor==='dormindo'?0.5:2;for(const x0 of [3.8,8.5]){ctx.fillRect(x+x0*tamanho+Math.max(-.35,Math.min(.35,ponteiro.x-innerWidth*.5)/300)*tamanho,y+6.3*tamanho,tamanho*.55,tamanho*olho/2);}
 if(ultimoHumor==='trabalhando'){ctx.fillStyle='#f6c99d';ctx.fillRect(x+12*tamanho,y+9*tamanho+Math.sin(tempo/110)*tamanho,tamanho,tamanho);}
}
function iniciarMascote(){
 const cv=e.mascotePixel;const animar=(t)=>{requestAnimationFrame(animar);if(t-mascoteQuadro<80)return;mascoteQuadro=t;desenharMascote(cv,t);const grande=e.mascoteGrande.querySelector('canvas');if(grande)desenharMascote(grande,t);};requestAnimationFrame(animar);
 const g=document.createElement('canvas');g.width=112;g.height=100;g.style.width='100px';g.style.height='90px';g.style.imageRendering='pixelated';e.mascoteGrande.appendChild(g);
 const pet=e.mascotePequeno;let comeco={x:0,y:0},arrasto={x:0,y:0},ultimo={x:0,y:0,t:0};
 pet.addEventListener('pointerdown',ev=>{arrastando=true;pet.setPointerCapture(ev.pointerId);comeco={x:ev.clientX-mascoteX,y:ev.clientY-mascoteY};arrasto={x:ev.clientX,y:ev.clientY};ultimo={x:ev.clientX,y:ev.clientY,t:performance.now()};mascoteVY=0;pet.style.transition='none';});
 pet.addEventListener('pointermove',ev=>{if(!arrastando)return;mascoteX=ev.clientX-comeco.x;mascoteY=ev.clientY-comeco.y;pet.style.transform=`translate3d(${mascoteX}px,${mascoteY}px,0) rotate(${Math.max(-24,Math.min(24,(mascoteX)/12))}deg)`;ultimo={x:ev.clientX,y:ev.clientY,t:performance.now()};});
 pet.addEventListener('pointerup',ev=>{if(!arrastando)return;arrastando=false;const mexeu=Math.hypot(ev.clientX-arrasto.x,ev.clientY-arrasto.y);if(mexeu<12){falarMascote(['Qual vai ser a ideia?','Estou aqui!','Seu código merece carinho.','Vamos criar algo?'][Math.floor(Math.random()*4)]);tocarSom();}
  const inicio=mascoteY,comeco=performance.now();const queda=()=>{const tempo=(performance.now()-comeco)/1000;mascoteY=Math.min(0,inicio+Math.pow(tempo,2)*1200);mascoteX*=.93;pet.style.transform=`translate3d(${mascoteX}px,${mascoteY}px,0) rotate(${Math.sin(tempo*8)*12*(1-Math.min(1,tempo))}deg)`;if(mascoteY<0)requestAnimationFrame(queda);else{mascoteY=0;mascoteX=0;pet.style.transition='transform .45s cubic-bezier(.2,.85,.2,1)';pet.style.transform='translate3d(0,0,0)';}};requestAnimationFrame(queda);
 });
 pet.addEventListener('keydown',ev=>{if(ev.key==='Enter'||ev.key===' '){ev.preventDefault();falarMascote('Bora trabalhar!');}});
}
function tocarSom(){if(!preferencias.sons)return;try{const C=window.AudioContext||window.webkitAudioContext;const audio=new C(),osc=audio.createOscillator(),gain=audio.createGain();osc.type='sine';osc.frequency.setValueAtTime(630,audio.currentTime);osc.frequency.exponentialRampToValueAtTime(930,audio.currentTime+.12);gain.gain.setValueAtTime(.05,audio.currentTime);gain.gain.exponentialRampToValueAtTime(.001,audio.currentTime+.15);osc.connect(gain);gain.connect(audio.destination);osc.start();osc.stop(audio.currentTime+.15);setTimeout(()=>audio.close(),500);}catch(err){}}
function testarPonte(){if(ponte){try{const salvo=usarPonte('lerPreferencias');if(salvo){const obj=JSON.parse(salvo);preferencias=Object.assign(preferencias,obj);}}catch(err){}usarPonte('solicitarEstado');}else{console.info('Metrys prévia visual sem ponte nativa.');}}
function iniciar(){configurarEventos();iniciarCenario();aplicarPreferencias();iniciarMascote();testarPonte();pintarMensagens();pintarStatus();}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',iniciar);else iniciar();
