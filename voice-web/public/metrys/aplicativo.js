/* Metrys Ateliê 0.6.18: identidade original, comportamento offline preservado. */
'use strict';
const $ = (id) => document.getElementById(id);
/* A prévia React abre em HTTPS: o navegador solicita a permissão do microfone.
   O modo de chat offline e a ponte nativa permanecem intactos. */
document.addEventListener('DOMContentLoaded', () => {
 const botaoVoz = document.getElementById('vozTopo');
 botaoVoz?.addEventListener('click', () => {
   window.location.assign('https://metrys-voice-ring.vercel.app/');
 });
});

const elementos = Object.fromEntries(['cenario','lateral','sombraLateral','parar','menu','fecharLateral','mensagens','mensagensRolagem','boasVindas','tituloConversa','campoMensagem','enviar','arvoreConversas','totalConversas','sobreposicao','tituloModal','modalConteudo','andamento','tituloPensamento','tempoPensamento','conteudoPensamento','abrirPensamento','fila','mascotePequeno','mascotePixel','falaMascote','selecionarArquivo','anexoPendente','temaAtual','aviso','mascoteGrande','modoModelo'].map(id=>[id,$(id)]));
const e = elementos;
const ponte = window.Metrys || null;
const guardar=(chave,valor)=>{try{localStorage.setItem('metrys_atelie_'+chave,JSON.stringify(valor));}catch(err){}};
const ler=(chave,padrao)=>{try{const v=localStorage.getItem('metrys_atelie_'+chave);return v===null?padrao:JSON.parse(v);}catch(err){return padrao;}};
const escapar=(v)=>String(v??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
const icone=(nome)=>`<svg aria-hidden="true"><use href="#i-${nome}"></use></svg>`;
const rascunho={textoAnexado:'',nomeAnexo:''};
let estado={fase:'preparando',detalhe:'Preparando sua experiência…',gerando:false,modeloPronto:false,conversaId:'',nomeModelo:'Modelo offline',conversas:[],projetos:[],mensagens:[],etapas:[],filaPendente:0,tempoGeracaoMs:0,raciocinioPublicado:'',raciocinando:false,trechosGerados:0,bytes:0,total:1};
let tituloAnterior='',ultimoDesenho='',ultimoResumo='',ultimoPensamento='',conversaAnterior='',avisoTimer=0,modalAtual='',raciocinios=ler('raciocinios',{});
let preferencias=Object.assign({versaoVisual:4,tema:'organic',acento:null,escurecer:0,opacidade:91,desfoque:8,tamanho:15,largura:740,velocidade:1,sons:false,reduzir:false},ler('preferencias',{}));
const THEMES=[
 {id:'organic',name:'Organic · Papel',light:true,fundo:'#f5ead8',fundo2:'#ebddc5',texto:'#201e1d',texto2:'#645c50',acento:'#b2622d',acento2:'#728157',pale:'#eee7db',cartao:'#f9f4ed'},
 {id:'mare',name:'Maré criativa',light:true,fundo:'#f5f1e8',fundo2:'#eae5d8',texto:'#20373c',texto2:'#617477',acento:'#ee7859',acento2:'#298e87',pale:'#d8eee7',cartao:'#fffcf6'},
 {id:'safira',name:'Safira elétrica',light:false,fundo:'#102735',fundo2:'#162f3b',texto:'#e8f2eb',texto2:'#b3cfca',acento:'#f7a16b',acento2:'#75e6d0',pale:'#224a50',cartao:'#1a343f'},
 {id:'terracota',name:'Terracota',light:true,fundo:'#f8e5d9',fundo2:'#efd2c3',texto:'#573c38',texto2:'#856b61',acento:'#d05c42',acento2:'#7a8a66',pale:'#f5ccc1',cartao:'#fff5ed'},
 {id:'jardim',name:'Jardim secreto',light:true,fundo:'#edf5e8',fundo2:'#dcebd8',texto:'#253d34',texto2:'#607a67',acento:'#ed9d66',acento2:'#3e9b7d',pale:'#d4ead5',cartao:'#f7fcf2'},
 {id:'sol',name:'Sol de papel',light:true,fundo:'#fff3d7',fundo2:'#f9e4b8',texto:'#4b4535',texto2:'#8b7958',acento:'#ed8152',acento2:'#8a9060',pale:'#f6dfaa',cartao:'#fffaec'},
 {id:'grafite',name:'Grafite vivo',light:false,fundo:'#1c2525',fundo2:'#283333',texto:'#f0efde',texto2:'#c1c4b1',acento:'#d4ef6a',acento2:'#77c6b2',pale:'#324440',cartao:'#303d3b'},
 {id:'gelo',name:'Horizonte glaciar',light:true,fundo:'#ecf5f8',fundo2:'#d8eaf2',texto:'#25475c',texto2:'#6e8da0',acento:'#ff8978',acento2:'#458ea4',pale:'#d1ebf1',cartao:'#f8fdff'},
 {id:'brasa',name:'Brasa noturna',light:false,fundo:'#2b1e29',fundo2:'#392736',texto:'#f9e8d8',texto2:'#ceb9c3',acento:'#fa8c67',acento2:'#efc987',pale:'#493447',cartao:'#3c2a39'},
 {id:'celeste',name:'Tinta celeste',light:true,fundo:'#f0edfa',fundo2:'#e1dbf5',texto:'#363453',texto2:'#79708e',acento:'#ee748f',acento2:'#6860aa',pale:'#e7dff6',cartao:'#faf8ff'},
 {id:'flora',name:'Flor de pêssego',light:true,fundo:'#fff0ea',fundo2:'#f9dcd5',texto:'#513d45',texto2:'#886d75',acento:'#e77983',acento2:'#569b89',pale:'#f6d6dc',cartao:'#fff9f5'},
 {id:'oceano',name:'Mar profundo',light:false,fundo:'#0b3034',fundo2:'#144449',texto:'#e5f5e9',texto2:'#a5cfc9',acento:'#ffb46d',acento2:'#63d7c6',pale:'#20565a',cartao:'#17464b'},
 {id:'amanhecer',name:'Amanhecer',light:true,fundo:'#fcf4e9',fundo2:'#f3ddd3',texto:'#424454',texto2:'#817f8d',acento:'#ff8176',acento2:'#7398ac',pale:'#f9dddd',cartao:'#fffaf5'}
];
const TEMAS_NOMES=Object.fromEntries(THEMES.map(t=>[t.id,t.name]));
if(Number(preferencias.versaoVisual||0)<4){preferencias.versaoVisual=4;preferencias.tema='organic';preferencias.acento=null;guardar('preferencias',preferencias);}
let atualTema=null,renderizador=null,ponteiro={x:0,y:0,speed:0},ativo=true;
function aviso(texto){clearTimeout(avisoTimer);e.aviso.textContent=texto;e.aviso.classList.add('ativo');avisoTimer=setTimeout(()=>e.aviso.classList.remove('ativo'),2400);}
function usarPonte(nome,...args){try{if(ponte&&typeof ponte[nome]==='function')return ponte[nome](...args);}catch(ex){aviso('Não foi possível concluir esta ação.');console.error(nome,ex);}return null;}
function guardarPreferencias(){guardar('preferencias',preferencias);usarPonte('salvarPreferencias',JSON.stringify(preferencias));}
function aplicarPreferencias(){
 const escolhido=THEMES.find(t=>t.id===preferencias.tema)||THEMES[0];
 const mudou=atualTema?.id!==escolhido.id;atualTema=escolhido;
 const css=document.documentElement.style;
 const cores={fundo:escolhido.fundo,fundo2:escolhido.fundo2,texto:escolhido.texto,texto2:escolhido.texto2,acento:preferencias.acento||escolhido.acento,acento2:escolhido.acento2,pale:escolhido.pale,cartao:escolhido.cartao};
 Object.entries(cores).forEach(([k,v])=>css.setProperty('--'+k,v));
 const escuro=!escolhido.light;
 const r=Math.max(.24,Math.min(.98,Number(preferencias.opacidade)/100||.91));
 css.setProperty('--vidro',escuro?`rgba(36,32,28,${r})`:`rgba(249,244,237,${r})`);
 css.setProperty('--pill',escuro?'#3a332d':'#eee7db');css.setProperty('--linha',escuro?'rgba(244,235,222,.22)':'rgba(32,30,29,.16)');
 css.setProperty('--fraco',escuro?'#c0b6a5':'#645c50');css.setProperty('--sombra',escuro?'0 16px 35px rgba(13,10,9,.3)':'0 16px 35px rgba(48,35,28,.12)');
 css.setProperty('--escurecer',String((Number(preferencias.escurecer)||0)/100));
 css.setProperty('--blur',Math.max(0,Math.min(30,Number(preferencias.desfoque)||0))+'px');
 css.setProperty('--fonte',Math.max(11,Math.min(22,Number(preferencias.tamanho)||15))+'px');
 css.setProperty('--largura',Math.max(380,Math.min(1200,Number(preferencias.largura)||740))+'px');
 document.documentElement.style.colorScheme=escuro?'dark':'light';document.body.classList.toggle('tema-escuro',escuro);
 document.body.classList.toggle('reduzido',Boolean(preferencias.reduzir));
 e.temaAtual.textContent=escolhido.name;
 if(mudou&&renderizador)renderizador.trocar(escolhido.id);
}
function iniciarCenario(){try{renderizador=MotorAmbiente.iniciar(e.cenario,preferencias);renderizador.trocar(preferencias.tema)}catch(erro){console.warn('Ambiente decorativo indisponível',erro)}}
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
function definirTitulo(){const c=(estado.conversas||[]).find(c=>c.id===estado.conversaId);const titulo=c?.titulo||'Um novo começo';if(titulo===tituloAnterior)return;tituloAnterior=titulo;e.tituloConversa.textContent=titulo;}
function pintarMensagens(){
 const lista=estado.mensagens||[];const assinatura=estado.conversaId+'|'+lista.map(m=>(m.papel||'')+':'+(m.texto||'').length+':'+(m.texto||'').slice(-32)).join(';');
 if(assinatura===ultimoDesenho)return;ultimoDesenho=assinatura;blocosCodigo.length=0;
 const sc=e.mensagensRolagem;const pertoDoFim=sc.scrollHeight-sc.scrollTop-sc.clientHeight<170;
 document.body.classList.toggle('com-mensagens',lista.length>0);if(!lista.length){e.mensagens.innerHTML='';e.mensagens.appendChild(e.boasVindas);e.boasVindas.hidden=false;return;}
 const mensagensHtml=lista.map((m,i)=>criarMensagem(m,i,lista)).join('');
 e.mensagens.innerHTML=mensagensHtml;
 if(pertoDoFim||conversaAnterior!==estado.conversaId)requestAnimationFrame(()=>sc.scrollTo({top:sc.scrollHeight,behavior:'auto'}));
 conversaAnterior=estado.conversaId;
}
function tituloPorRaciocinio(texto){const limpo=String(texto||'').replace(/<\/?think>/g,'').replace(/\s+/g,' ').trim();if(!limpo)return 'Transformando a pergunta em resposta…';const primeira=limpo.split(/(?<=[.!?])\s+/)[0]||limpo;const truncado=primeira.length>68?primeira.slice(0,65).trim()+'…':primeira;return truncado||'Processando…';}
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
function mostrarTemas(){
 const lista=THEMES.map(t=>`<button class="tema-item ${preferencias.tema===t.id?'ativo':''}" data-tema="${t.id}" aria-label="Tema ${escapar(t.name)}"><div class="miniatura-tema" style="background:linear-gradient(140deg,${t.fundo},${t.fundo2});color:${t.acento2};border:1px solid ${t.texto2}55"><span></span></div><b>${escapar(t.name)}</b><small>${t.light?'Luz e movimento':'Profundidade e movimento'}</small></button>`).join('');
 abrirModal('Escolha sua atmosfera',`<p class="aviso-modal">12 mundos visuais desenhados para o Metrys. Mude a atmosfera sem sair da conversa.</p><div class="grade-temas">${lista}</div>`);
}
const PALETAS=[['Coral','#ee7859'],['Oceano','#298e87'],['Lavanda','#ab96f4'],['Floresta','#3e9b7d'],['Ouro','#e9bf67'],['Rosa','#e77983'],['Lima','#b4cd5b'],['Safira','#458ea4']];
function mostrarPersonalizar(aba='Cores'){
 abrirModal('Personalizar Metrys',`<div class="abas">${['Cores','Fundo','Interface','Animações'].map(n=>`<button class="${n===aba?'ativa':''}" data-aba="${n}">${n}</button>`).join('')}</div><div id="preferenciasConteudo"></div>`);
 const area=$('preferenciasConteudo');
 if(aba==='Cores')area.innerHTML=`<section class="grupo-preferencias"><h3>Paletas</h3><div class="paletas">${PALETAS.map(p=>`<button data-acento="${p[1]}"><i style="background:${p[1]}"></i>${p[0]}</button>`).join('')}</div></section><section class="grupo-preferencias"><h3>Cores personalizadas</h3><label>Cor de destaque <input type="color" data-config="acento" value="${preferencias.acento||atualTema.acento}" /></label></section><section class="grupo-preferencias"><h3>Material da interface</h3><label>Opacidade <input type="range" min="10" max="97" value="${preferencias.opacidade}" data-config="opacidade"/><span>${preferencias.opacidade}%</span></label><label>Desfoque <input type="range" min="0" max="30" value="${preferencias.desfoque}" data-config="desfoque"/><span>${preferencias.desfoque}px</span></label></section>`;
 else if(aba==='Fundo')area.innerHTML=`<section class="grupo-preferencias"><h3>Cenário atual</h3><p class="aviso-modal">${escapar(TEMAS_NOMES[preferencias.tema])}. Escolha uma das doze atmosferas criativas.</p><button class="botao-padrao" id="mudarCenario">Escolher tema</button></section><section class="grupo-preferencias"><label>Escurecer fundo <input type="range" min="0" max="80" value="${preferencias.escurecer}" data-config="escurecer"/><span>${preferencias.escurecer}%</span></label></section><section class="grupo-preferencias"><h3>Imagem ou GIF de fundo</h3><p class="aviso-modal">Você pode aplicar uma imagem desta sessão, sem enviar nada para a internet.</p><input id="imagemFundo" type="file" accept="image/png,image/jpeg,image/webp,image/gif" /><button class="botao-padrao" id="removerFundo">Restaurar cenário animado</button></section>`;
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
  if(t.dataset.tema){preferencias.tema=t.dataset.tema;preferencias.acento=null;aplicarPreferencias();guardarPreferencias();mostrarTemas();tocarSom();}
  if(t.dataset.aba)mostrarPersonalizar(t.dataset.aba);
  if(t.dataset.acento){preferencias.acento=t.dataset.acento;aplicarPreferencias();guardarPreferencias();mostrarPersonalizar('Cores');}
  if(t.dataset.modelo){usarPonte('selecionarModelo',t.dataset.modelo);aviso('Modelo selecionado.');fecharModal();}
  if(t.id==='baixarModelo'){usarPonte('baixarModelo');aviso('Download iniciado.');fecharModal();}
  if(t.id==='mudarCenario')mostrarTemas();
  if(t.id==='removerFundo'){e.cenario.style.opacity='1';e.cenario.style.background='';e.cenario.style.backgroundImage='';ativo=true;if(renderizador)renderizador.pausar(false);}
  if(t.id==='restaurarVisual'){preferencias=Object.assign(preferencias,{tema:'organic',acento:null,escurecer:0,opacidade:91,desfoque:8,tamanho:15,largura:740});aplicarPreferencias();guardarPreferencias();mostrarPersonalizar('Interface');}
 });
 e.modalConteudo.addEventListener('input',ev=>{const alvo=ev.target,k=alvo.dataset.config;if(!k)return;preferencias[k]=alvo.type==='checkbox'?alvo.checked:alvo.type==='range'?Number(alvo.value):alvo.value;aplicarPreferencias();guardarPreferencias();if(alvo.parentElement?.lastElementChild?.tagName==='SPAN')alvo.parentElement.lastElementChild.textContent=alvo.value+(k==='opacidade'||k==='escurecer'?'%':k==='desfoque'||k==='tamanho'?'px':k==='velocidade'?'×':'px');});
 e.modalConteudo.addEventListener('change',ev=>{if(ev.target.id==='imagemFundo'){const arq=ev.target.files?.[0];if(!arq)return;if(arq.size>7_000_000){aviso('Limite de 7 MB para este fundo.');return;}const url=URL.createObjectURL(arq);e.cenario.style.backgroundImage=`url("${url}")`;e.cenario.style.backgroundSize='cover';e.cenario.style.backgroundPosition='center';e.cenario.style.opacity='1';ativo=false;if(renderizador)renderizador.pausar(true);aviso('Imagem aplicada nesta sessão.');}});
 document.addEventListener('visibilitychange',()=>{ativo=!document.hidden;});
 document.addEventListener('keydown',ev=>{if(ev.key==='Escape'){if(!e.sobreposicao.hidden)fecharModal();else fecharLateral();}});
}
let ultimoHumor='calmo',mascoteQuadro=0,mascoteX=0,mascoteY=0,mascoteVY=0,arrastando=false,mascoteFalaTimer=0;
function definirHumor(h){ultimoHumor=h;}
function falarMascote(fala){clearTimeout(mascoteFalaTimer);e.falaMascote.hidden=false;e.falaMascote.textContent=fala;mascoteFalaTimer=setTimeout(()=>e.falaMascote.hidden=true,2800);}
function desenharMascote(canvas,tempo=0){
 if(!canvas)return;
 const ctx=canvas.getContext('2d'),w=canvas.width,h=canvas.height;
 const t=preferencias.reduzir?0:tempo/1000;
 const estilo=getComputedStyle(document.documentElement);
 const tinta=estilo.getPropertyValue('--texto').trim()||'#201e1d';
 const terra=estilo.getPropertyValue('--acento').trim()||'#b2622d';
 const salvia=estilo.getPropertyValue('--acento2').trim()||'#728157';
 const papel=estilo.getPropertyValue('--cartao').trim()||'#f9f4ed';
 const k=Math.min(w/116,h/116);
 const oscilacao=preferencias.reduzir?0:Math.sin(t*2.05)*2.4;
 const inclinacao=preferencias.reduzir?0:Math.sin(t*1.05)*.045+(ultimoHumor==='trabalhando'?Math.sin(t*8)*.025:0);
 ctx.clearRect(0,0,w,h);ctx.save();
 ctx.translate(w/2,h/2+oscilacao*k);ctx.scale(k,k);
 ctx.save();ctx.globalAlpha=.11;ctx.fillStyle=tinta;
 ctx.beginPath();ctx.ellipse(0,48,34,5,0,0,Math.PI*2);ctx.fill();ctx.restore();
 // O Metrys e uma pequena dobradura viva, feita de papel e tinta.
 ctx.rotate(inclinacao);
 // Dobra traseira esquerda — duas abas de papel com tinta na borda.
 ctx.fillStyle='#e8c9ab';ctx.strokeStyle=tinta;ctx.lineWidth=1.75;ctx.lineJoin='round';
 ctx.beginPath();ctx.moveTo(-27,-23);ctx.quadraticCurveTo(-42,-48,-16,-52);
 ctx.lineTo(-5,-30);ctx.closePath();ctx.fill();ctx.stroke();
 ctx.fillStyle=salvia;
 ctx.beginPath();ctx.moveTo(19,-24);ctx.quadraticCurveTo(40,-45,38,-49);
 ctx.lineTo(11,-42);ctx.lineTo(5,-28);ctx.closePath();ctx.fill();ctx.stroke();
 // Corpo central de papel: uma silhueta pentagonal macia, nao um circulo generico.
 ctx.shadowColor='rgba(48,35,28,.14)';ctx.shadowBlur=10;ctx.shadowOffsetY=5;
 ctx.fillStyle=papel;ctx.strokeStyle=tinta;ctx.lineWidth=2.1;
 ctx.beginPath();ctx.moveTo(-27,-28);ctx.quadraticCurveTo(-34,-25,-35,-13);
 ctx.lineTo(-33,21);ctx.quadraticCurveTo(-28,36,-8,39);
 ctx.quadraticCurveTo(8,41,25,34);ctx.quadraticCurveTo(35,28,35,11);
 ctx.lineTo(30,-16);ctx.quadraticCurveTo(26,-30,8,-35);
 ctx.quadraticCurveTo(-10,-40,-27,-28);ctx.closePath();ctx.fill();ctx.stroke();
 ctx.shadowBlur=0;ctx.shadowOffsetY=0;
 // Recorte terracota no topo, com dobra triangulada.
 ctx.fillStyle=terra;
 ctx.beginPath();ctx.moveTo(-26,-27);ctx.quadraticCurveTo(-3,-40,26,-26);
 ctx.lineTo(15,-10);ctx.lineTo(-2,-20);ctx.lineTo(-19,-10);ctx.closePath();ctx.fill();
 ctx.fillStyle='#e8c9ab';ctx.globalAlpha=.76;
 ctx.beginPath();ctx.moveTo(-19,-10);ctx.lineTo(-2,-20);ctx.lineTo(2,-1);ctx.closePath();ctx.fill();ctx.globalAlpha=1;
 // Costura do papel e espiral propria que representa curiosidade.
 ctx.strokeStyle=terra;ctx.lineWidth=2;ctx.lineCap='round';
 ctx.beginPath();ctx.moveTo(-27,21);ctx.quadraticCurveTo(-14,29,-2,29);ctx.stroke();
 ctx.beginPath();ctx.arc(21,24,5.5,Math.PI*.28,Math.PI*1.85);ctx.stroke();
 // Bracinhos soltos balancam conforme o ritmo.
 const gesto=preferencias.reduzir?0:Math.sin(t*3.1)*3;
 ctx.strokeStyle=tinta;ctx.lineWidth=2.5;
 ctx.beginPath();ctx.moveTo(-31,7);ctx.quadraticCurveTo(-39,11+gesto,-40,17+gesto);ctx.stroke();
 ctx.beginPath();ctx.moveTo(32,8);ctx.quadraticCurveTo(43,2-gesto,42,-6-gesto);ctx.stroke();
 // Olhos acompanham o ponteiro; piscada organica a cada ~5 segundos.
 const olhar=Math.max(-2.1,Math.min(2.1,(ponteiro.x-innerWidth/2)/240));
 const piscando=ultimoHumor==='dormindo'||(!preferencias.reduzir&&((t+1.3)%5.7)<.14);
 const ys=10;
 ctx.strokeStyle=tinta;ctx.fillStyle=tinta;ctx.lineWidth=2.25;
 for(const x of [-11,12]){
  if(piscando){ctx.beginPath();ctx.moveTo(x-4,ys);ctx.quadraticCurveTo(x,ys+3,x+4,ys);ctx.stroke();}
  else{ctx.beginPath();ctx.ellipse(x+olhar,ys,2.75,3.7,0,0,Math.PI*2);ctx.fill();}
 }
 ctx.beginPath();ctx.arc(1,17,5,0,Math.PI*.93);ctx.stroke();
 // Bochechas discretas de terracota e pes desenhados como sombra.
 ctx.globalAlpha=.3;ctx.fillStyle=terra;
 for(const x of [-22,23]){ctx.beginPath();ctx.ellipse(x,18,5.5,3.2,0,0,7);ctx.fill();}
 ctx.globalAlpha=1;ctx.strokeStyle=tinta;ctx.lineWidth=2;
 ctx.beginPath();ctx.moveTo(-14,38);ctx.lineTo(-17,44);ctx.moveTo(14,38);ctx.lineTo(17,44);ctx.stroke();
 // Um traço vivo flutuante, sem efeitos brilhantes ou neon.
 ctx.save();ctx.translate(31,-37+(preferencias.reduzir?0:Math.sin(t*2.6)*3));
 ctx.strokeStyle=salvia;ctx.lineWidth=2.5;ctx.beginPath();ctx.moveTo(0,-6);ctx.lineTo(0,6);ctx.moveTo(-6,0);ctx.lineTo(6,0);ctx.stroke();ctx.restore();
 ctx.restore();
}
function iniciarMascote(){
 const cv=e.mascotePixel;const animar=(t)=>{requestAnimationFrame(animar);if(t-mascoteQuadro<80)return;mascoteQuadro=t;desenharMascote(cv,t);const grande=e.mascoteGrande.querySelector('canvas');if(grande)desenharMascote(grande,t);};requestAnimationFrame(animar);
 const g=document.createElement('canvas');g.width=170;g.height=166;g.style.width='150px';g.style.height='150px';g.style.maxWidth='100%';e.mascoteGrande.appendChild(g);
 const pet=e.mascotePequeno;let comeco={x:0,y:0},arrasto={x:0,y:0},ultimo={x:0,y:0,t:0};
 pet.addEventListener('pointerdown',ev=>{arrastando=true;pet.setPointerCapture(ev.pointerId);comeco={x:ev.clientX-mascoteX,y:ev.clientY-mascoteY};arrasto={x:ev.clientX,y:ev.clientY};ultimo={x:ev.clientX,y:ev.clientY,t:performance.now()};mascoteVY=0;pet.style.transition='none';});
 pet.addEventListener('pointermove',ev=>{if(!arrastando)return;mascoteX=ev.clientX-comeco.x;mascoteY=ev.clientY-comeco.y;pet.style.transform=`translate3d(${mascoteX}px,${mascoteY}px,0) rotate(${Math.max(-24,Math.min(24,(mascoteX)/12))}deg)`;ultimo={x:ev.clientX,y:ev.clientY,t:performance.now()};});
 pet.addEventListener('pointerup',ev=>{if(!arrastando)return;arrastando=false;const mexeu=Math.hypot(ev.clientX-arrasto.x,ev.clientY-arrasto.y);if(mexeu<12){falarMascote(['Uma ideia de cada vez.','Que bom te ver!','Vamos criar algo novo?','Estou por aqui para ajudar.'][Math.floor(Math.random()*4)]);tocarSom();}
  const inicio=mascoteY,comeco=performance.now();const queda=()=>{const tempo=(performance.now()-comeco)/1000;mascoteY=Math.min(0,inicio+Math.pow(tempo,2)*1200);mascoteX*=.93;pet.style.transform=`translate3d(${mascoteX}px,${mascoteY}px,0) rotate(${Math.sin(tempo*8)*12*(1-Math.min(1,tempo))}deg)`;if(mascoteY<0)requestAnimationFrame(queda);else{mascoteY=0;mascoteX=0;pet.style.transition='transform .45s cubic-bezier(.2,.85,.2,1)';pet.style.transform='translate3d(0,0,0)';}};requestAnimationFrame(queda);
 });
 pet.addEventListener('keydown',ev=>{if(ev.key==='Enter'||ev.key===' '){ev.preventDefault();falarMascote('Vamos criar!');}});
}
function tocarSom(){if(!preferencias.sons)return;try{const C=window.AudioContext||window.webkitAudioContext;const audio=new C(),osc=audio.createOscillator(),gain=audio.createGain();osc.type='sine';osc.frequency.setValueAtTime(630,audio.currentTime);osc.frequency.exponentialRampToValueAtTime(930,audio.currentTime+.12);gain.gain.setValueAtTime(.05,audio.currentTime);gain.gain.exponentialRampToValueAtTime(.001,audio.currentTime+.15);osc.connect(gain);gain.connect(audio.destination);osc.start();osc.stop(audio.currentTime+.15);setTimeout(()=>audio.close(),500);}catch(err){}}
function testarPonte(){if(ponte){try{const salvo=usarPonte('lerPreferencias');if(salvo){const obj=JSON.parse(salvo);if(obj.versaoVisual===4){preferencias=Object.assign(preferencias,obj);aplicarPreferencias();}}}catch(err){}usarPonte('solicitarEstado');}else{console.info('Prévia do Ateliê sem ponte Android.');}}
function iniciar(){configurarEventos();aplicarPreferencias();iniciarCenario();iniciarMascote();testarPonte();pintarMensagens();pintarStatus();}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',iniciar);else iniciar();
