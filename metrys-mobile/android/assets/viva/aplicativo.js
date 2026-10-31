/* Metrys Ateliê 0.6.18: identidade original, comportamento offline preservado. */
'use strict';
const $ = (id) => document.getElementById(id);
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
let preferencias=Object.assign({versaoVisual:3,tema:'mare',acento:null,escurecer:0,opacidade:91,desfoque:8,tamanho:15,largura:740,velocidade:1,sons:false,reduzir:false},ler('preferencias',{}));
const THEMES=[
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
 css.setProperty('--vidro',escuro?`rgba(28,44,48,${r})`:`rgba(255,253,246,${r})`);
 css.setProperty('--pill',escuro?'#355052':'#e7e6db');css.setProperty('--linha',escuro?'rgba(210,244,232,.19)':'rgba(39,67,68,.16)');
 css.setProperty('--fraco',escuro?'#829e9c':'#82918c');css.setProperty('--sombra',escuro?'0 20px 48px rgba(4,10,11,.32)':'0 20px 48px rgba(24,52,51,.10)');
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
  if(t.id==='restaurarVisual'){preferencias=Object.assign(preferencias,{tema:'mare',acento:null,escurecer:0,opacidade:91,desfoque:8,tamanho:15,largura:740});aplicarPreferencias();guardarPreferencias();mostrarPersonalizar('Interface');}
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
 if(!canvas)return;const ctx=canvas.getContext('2d'),w=canvas.width,h=canvas.height;
 ctx.clearRect(0,0,w,h);const k=Math.min(w/110,h/112);ctx.save();ctx.translate(w/2,h/2+Math.sin(tempo/680)*2*k);ctx.scale(k,k);
 const movimento=ultimoHumor==='trabalhando'?Math.sin(tempo/135)*.065:Math.sin(tempo/850)*.025;
 ctx.rotate(movimento);const cor=getComputedStyle(document.documentElement);
 const agua=cor.getPropertyValue('--acento2').trim()||'#298e87';const coral=cor.getPropertyValue('--acento').trim()||'#ee7859';
 ctx.globalAlpha=.2;ctx.fillStyle='#274c50';ctx.beginPath();ctx.ellipse(0,47,31,6,0,0,7);ctx.fill();ctx.globalAlpha=1;
 // Dois pequenos satélites dão personalidade ao personagem, sem remeter a letra ou mascote existente.
 ctx.fillStyle=coral;ctx.beginPath();ctx.arc(-26,-38,9+Math.sin(tempo/500)*1,0,Math.PI*2);ctx.fill();
 ctx.fillStyle=agua;ctx.beginPath();ctx.arc(35,-26,6.2,0,Math.PI*2);ctx.fill();
 const grad=ctx.createLinearGradient(-38,-40,36,48);grad.addColorStop(0,'#b0e7da');grad.addColorStop(.45,'#65c4b4');grad.addColorStop(1,'#388e8a');
 ctx.beginPath();ctx.moveTo(-37,-11);ctx.bezierCurveTo(-47,-42,-15,-53,4,-37);ctx.bezierCurveTo(23,-51,47,-24,38,0);ctx.bezierCurveTo(48,28,21,45,0,40);ctx.bezierCurveTo(-29,51,-46,24,-37,-11);ctx.closePath();ctx.fillStyle=grad;ctx.fill();ctx.lineWidth=2;ctx.strokeStyle='#296c6a';ctx.globalAlpha=.55;ctx.stroke();ctx.globalAlpha=1;
 // Marca circular como espiral expressiva na testa.
 ctx.strokeStyle='#defff4';ctx.lineWidth=2.4;ctx.lineCap='round';ctx.beginPath();ctx.arc(1,-20,11,Math.PI*.5,Math.PI*2.17);ctx.stroke();ctx.beginPath();ctx.arc(1,-20,4,0,Math.PI*1.4);ctx.stroke();
 // Olhos reagem ao ponteiro e ao estado da IA.
 const olhar=Math.max(-2.4,Math.min(2.4,(ponteiro.x-innerWidth/2)/190));
 ctx.fillStyle='#143d3f';if(ultimoHumor==='dormindo'){
  ctx.lineWidth=3;ctx.strokeStyle='#143d3f';for(const x of [-14,15]){ctx.beginPath();ctx.moveTo(x-4,3);ctx.quadraticCurveTo(x,7,x+4,3);ctx.stroke();}
 }else{for(const x of [-14,15]){ctx.beginPath();ctx.ellipse(x+olhar,4,3.1,4.3,0,0,7);ctx.fill();}}
 ctx.strokeStyle='#194e50';ctx.lineWidth=2;ctx.beginPath();ctx.arc(1,15,6,0,Math.PI*.95);ctx.stroke();
 ctx.globalAlpha=.5;ctx.fillStyle=coral;for(const x of [-25,24]){ctx.beginPath();ctx.arc(x,14,6,0,7);ctx.fill()}
 ctx.restore();
}
function iniciarMascote(){
 const cv=e.mascotePixel;const animar=(t)=>{requestAnimationFrame(animar);if(t-mascoteQuadro<80)return;mascoteQuadro=t;desenharMascote(cv,t);const grande=e.mascoteGrande.querySelector('canvas');if(grande)desenharMascote(grande,t);};requestAnimationFrame(animar);
 const g=document.createElement('canvas');g.width=170;g.height=166;g.style.width='165px';g.style.height='155px';g.style.maxWidth='100%';e.mascoteGrande.appendChild(g);
 const pet=e.mascotePequeno;let comeco={x:0,y:0},arrasto={x:0,y:0},ultimo={x:0,y:0,t:0};
 pet.addEventListener('pointerdown',ev=>{arrastando=true;pet.setPointerCapture(ev.pointerId);comeco={x:ev.clientX-mascoteX,y:ev.clientY-mascoteY};arrasto={x:ev.clientX,y:ev.clientY};ultimo={x:ev.clientX,y:ev.clientY,t:performance.now()};mascoteVY=0;pet.style.transition='none';});
 pet.addEventListener('pointermove',ev=>{if(!arrastando)return;mascoteX=ev.clientX-comeco.x;mascoteY=ev.clientY-comeco.y;pet.style.transform=`translate3d(${mascoteX}px,${mascoteY}px,0) rotate(${Math.max(-24,Math.min(24,(mascoteX)/12))}deg)`;ultimo={x:ev.clientX,y:ev.clientY,t:performance.now()};});
 pet.addEventListener('pointerup',ev=>{if(!arrastando)return;arrastando=false;const mexeu=Math.hypot(ev.clientX-arrasto.x,ev.clientY-arrasto.y);if(mexeu<12){falarMascote(['Toda ideia pode crescer.','Estou aqui!','Que tal dar vida a isso?','Me conte seu próximo plano.'][Math.floor(Math.random()*4)]);tocarSom();}
  const inicio=mascoteY,comeco=performance.now();const queda=()=>{const tempo=(performance.now()-comeco)/1000;mascoteY=Math.min(0,inicio+Math.pow(tempo,2)*1200);mascoteX*=.93;pet.style.transform=`translate3d(${mascoteX}px,${mascoteY}px,0) rotate(${Math.sin(tempo*8)*12*(1-Math.min(1,tempo))}deg)`;if(mascoteY<0)requestAnimationFrame(queda);else{mascoteY=0;mascoteX=0;pet.style.transition='transform .45s cubic-bezier(.2,.85,.2,1)';pet.style.transform='translate3d(0,0,0)';}};requestAnimationFrame(queda);
 });
 pet.addEventListener('keydown',ev=>{if(ev.key==='Enter'||ev.key===' '){ev.preventDefault();falarMascote('Vamos criar!');}});
}
function tocarSom(){if(!preferencias.sons)return;try{const C=window.AudioContext||window.webkitAudioContext;const audio=new C(),osc=audio.createOscillator(),gain=audio.createGain();osc.type='sine';osc.frequency.setValueAtTime(630,audio.currentTime);osc.frequency.exponentialRampToValueAtTime(930,audio.currentTime+.12);gain.gain.setValueAtTime(.05,audio.currentTime);gain.gain.exponentialRampToValueAtTime(.001,audio.currentTime+.15);osc.connect(gain);gain.connect(audio.destination);osc.start();osc.stop(audio.currentTime+.15);setTimeout(()=>audio.close(),500);}catch(err){}}
function testarPonte(){if(ponte){try{const salvo=usarPonte('lerPreferencias');if(salvo){const obj=JSON.parse(salvo);if(obj.versaoVisual===3)preferencias=Object.assign(preferencias,obj);}}catch(err){}usarPonte('solicitarEstado');}else{console.info('Prévia do Ateliê sem ponte Android.');}}
function iniciar(){configurarEventos();aplicarPreferencias();iniciarCenario();iniciarMascote();testarPonte();pintarMensagens();pintarStatus();}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',iniciar);else iniciar();
