/* Metrys — modo online opcional. Offline nativo intacto. */
(function () {
  'use strict';
  var N = 'metrys.online.v1.';
  var ponte = window.Metrys || null;
  var campo = document.getElementById('campoMensagem');
  var enviar = document.getElementById('enviar');
  var parar = document.getElementById('parar');
  var seletor = document.getElementById('modoModelo');
  var receberOriginal = window.receberEstado;
  if (!campo || !enviar || !seletor || typeof receberOriginal !== 'function') return;
  var online = false, ocupado = false, pedido = '', cancelamento = null, tokenWeb = '', estadoLocal = null;
  function ler(chave, padrao) { try { return JSON.parse(localStorage.getItem(N + chave)) || padrao; } catch (_) { return padrao; } }
  function gravar(chave, valor) { try { localStorage.setItem(N + chave, JSON.stringify(valor)); return true; } catch (_) { return false; } }
  function avisoMetrys(msg) { var el = document.getElementById('aviso'); if (el) { el.textContent = msg; el.classList.add('ativo'); setTimeout(function () { el.classList.remove('ativo'); }, 3300); } }
  function escapar(t) { return String(t || '').replace(/[&<>"']/g, function (c) { return ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[c]; }); }
  function novoId() { return 'n-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 7); }
  var memorias = ler('memorias', []);
  var personalidade = ler('personalidade', 'Seja um parceiro criativo e direto para trabalhos, programação e ecommerce.');
  var chats = ler('chats', []), chatId = ler('chatId', '');
  if (!Array.isArray(memorias)) memorias = [];
  if (!Array.isArray(chats)) chats = [];
  memorias = memorias.filter(function (m) { return m && typeof m.texto === 'string'; }).slice(-40);
  chats = chats.filter(function (c) { return c && c.id && Array.isArray(c.mensagens); }).slice(-20);
  function persistir() { chats = chats.slice(0, 20); chats.forEach(function (c) { c.mensagens = c.mensagens.slice(-40); }); gravar('chats', chats); gravar('chatId', chatId); }
  function chat() {
    var c = chats.find(function (x) { return x.id === chatId; });
    if (!c) { c = { id: novoId(), titulo: 'Nova conversa', mensagens: [] }; chats.unshift(c); chatId = c.id; persistir(); }
    return c;
  }
  var botao = document.createElement('button');
  botao.type = 'button'; botao.id = 'alternarNuvem'; botao.className = 'botao-mini metrys-modo';
  seletor.parentNode.insertBefore(botao, seletor);
  function atualizarBotao() { botao.textContent = online ? '◉ Online' : '◌ Offline'; botao.classList.toggle('ativo', online); botao.setAttribute('aria-pressed', String(online)); }
  function desenhar() {
    if (!online) return;
    var c = chat();
    receberOriginal({fase:ocupado ? 'gerando' : 'pronto', detalhe:ocupado ? 'Consultando modelo remoto…' : 'Nuvem · memorias salvas no aparelho',
      nomeModelo:'Metrys · nuvem', modeloPronto:true,gerando:ocupado,raciocinando:false,raciocinioPublicado:'',
      filaPendente:0,tempoGeracaoMs:0,projetos:[],etapas:[],bytes:0,total:1,
      conversaId:c.id,mensagens:c.mensagens,conversas:chats.map(function (x) { return {id:x.id,titulo:x.titulo,projetoId:''}; })});
  }
  window.receberEstado = function (estado) { estadoLocal = estado; if (!online) receberOriginal(estado); };
  function temToken() { return ponte && typeof ponte.nuvemTemToken === 'function' ? String(ponte.nuvemTemToken()) === 'true' : !!tokenWeb; }
  function escolherModo(novo) {
    if (ocupado) { avisoMetrys('Cancele ou conclua a resposta antes de trocar de modo.'); return; }
    online = novo; atualizarBotao();
    if (online) { desenhar(); if (!temToken()) abrirPainel(); }
    else if (estadoLocal) receberOriginal(estadoLocal);
    else receberOriginal({modeloPronto:false,gerando:false,modeloNome:'Offline',nomeModelo:'Offline',mensagens:[],conversas:[],projetos:[],fase:'ausente',detalhe:'No navegador, a IA offline so funciona no APK.'});
  }
  function promptSistema() {
    var fatos = memorias.map(function (m) { return '- ' + m.texto; }).join('\n').slice(0, 9000);
    return 'Você é Metrys, assistente pessoal do usuario com personalidade propria. Responda em portugues do Brasil. Ajude com programação, projetos, pesquisa e ecommerce. Seja criativo, confiavel, direto e cordial. Confirme antes de executar acoes externas. Nao finja alteracoes em arquivos ou retreinamento. No modo online, seu motor é Kimi K3 hospedado remotamente. Aprendizado por memorias aprovadas, nao por ajuste dos pesos.\nPersonalidade configurada:\n' + personalidade.slice(0, 1500) + '\nMemorias explicitamente salvas:\n' + (fatos || 'Nenhuma.');
  }
  function lembrar(texto) {
    texto = String(texto || '').trim().slice(0, 800);
    if (!texto) { avisoMetrys('Informe o que devo lembrar.'); return; }
    if (memorias.some(function (m) { return m.texto.toLowerCase() === texto.toLowerCase(); })) { avisoMetrys('Essa memoria ja existe.'); return; }
    memorias.push({id:novoId(), texto:texto}); memorias = memorias.slice(-40);
    gravar('memorias', memorias); avisoMetrys('Metrys guardou sua memoria neste dispositivo.');
  }
  function finalizar(id, texto, erro) {
    if (id !== pedido || !ocupado) return;
    ocupado = false; pedido = '';
    if (erro) avisoMetrys(erro);
    else if (texto) chat().mensagens.push({papel:'assistant',texto:String(texto).slice(0, 40000)});
    persistir(); desenhar();
  }
  window.receberNuvem = function (r) { if (r && r.id === pedido) finalizar(r.id,r.texto,r.erro); };
  function textoRemoto(r) {
    if (typeof r.texto === 'string') return r.texto;
    var msg = r.choices && r.choices[0] && r.choices[0].message;
    if (typeof (msg && msg.content) === 'string') return msg.content;
    if (Array.isArray(msg && msg.content)) return msg.content.filter(function (c) {return c && c.text;}).map(function (c) {return c.text;}).join('\n');
    throw Error('Resposta sem texto do servidor.');
  }
  function enviarWeb(mensagens, id) {
    cancelamento = new AbortController();
    var controlador = cancelamento;
    var tempo = setTimeout(function () { controlador.abort(); }, 90000);
    fetch('/api/metrys', {method:'POST',signal:controlador.signal,
      headers:{'Content-Type':'application/json','Authorization':'Bearer ' + tokenWeb},
      body:JSON.stringify({mensagens:mensagens,max_tokens:384})})
      .then(function (resp) { if (!resp.ok) throw Error(resp.status === 401 ? 'Token invalido ou sem permissao.' : resp.status === 402 ? 'Creditos insuficientes.' : 'Erro HTTP ' + resp.status); return resp.json(); })
      .then(function (data) { finalizar(id,textoRemoto(data),''); })
      .catch(function (err) { finalizar(id,'',err.name === 'AbortError' ? 'Requisicao cancelada.' : err.message); })
      .finally(function () {clearTimeout(tempo); if(cancelamento === controlador) cancelamento = null;});
  }
  function enviarTexto() {
    if (!online) return;
    if (ocupado) { avisoMetrys('Aguarde a resposta atual.'); return; }
    var texto = campo.value.trim();
    if (!texto) return;
    if (/^\/lembrar\s+/i.test(texto)) { campo.value=''; lembrar(texto.replace(/^\/lembrar\s+/i,'')); return; }
    if (texto.length > 12000) { avisoMetrys('Limite de 12 mil caracteres por mensagem online.'); return; }
    if (!temToken()) { abrirPainel(); return; }
    var c = chat(); c.mensagens.push({papel:'user',texto:texto});
    if (c.titulo === 'Nova conversa') c.titulo = texto.slice(0, 45);
    campo.value=''; campo.style.height=''; persistir();
    var mensagens = [{role:'system',content:promptSistema()}].concat(c.mensagens.slice(-14).map(function (m) {
      return {role:m.papel === 'assistant' ? 'assistant' : 'user',content:m.texto.slice(0, 8000)};
    }));
    ocupado = true; pedido = novoId(); desenhar();
    var id = pedido;
    if (ponte && typeof ponte.nuvemEnviar === 'function') {
      try { ponte.nuvemEnviar(JSON.stringify({id:id,mensagens:mensagens,max_tokens:384})); }
      catch (_) { finalizar(id,'','Ponte de inferencia Android indisponivel.'); }
    } else enviarWeb(mensagens,id);
  }
  function cancelar() {
    if (!ocupado) return;
    if (cancelamento) cancelamento.abort();
    if (ponte && typeof ponte.nuvemCancelar === 'function') ponte.nuvemCancelar();
    finalizar(pedido,'','Resposta cancelada.');
  }
  function abrirPainel() {
    if (typeof abrirModal !== 'function') return;
    abrirModal('Metrys · Nuvem e memorias',
      '<p class="aviso-modal">O Metrys usa o Kimi K3 remoto. O consumo da API pode ter custo. Nenhuma conversa offline e enviada automaticamente.</p>' +
      '<div class="grupo-preferencias"><h3>Conexao Hugging Face</h3><label>Token <input id="metToken" type="password" autocomplete="off" placeholder="hf_…" /></label>' +
      '<p class="aviso-modal">' + (ponte ? 'Android: token guardado com Android Keystore.' : 'Navegador: token fica somente nesta aba e passa pelo servidor do site.') + '</p>' +
      '<button class="botao-padrao" id="metSalvarToken">Salvar token</button> <button class="botao-padrao" id="metLimparToken">Esquecer token</button></div>' +
      '<div class="grupo-preferencias"><h3>Personalidade</h3><textarea id="metPerfil" maxlength="1500" rows="4" class="campo-memoria"></textarea><button class="botao-padrao" id="metSalvarPerfil">Guardar personalidade</button></div>' +
      '<div class="grupo-preferencias"><h3>O que devo aprender?</h3><p class="aviso-modal">Digite /lembrar no chat, ou escreva uma memoria aqui. Edite ou apague quando quiser.</p>' +
      '<textarea id="metNovaMemoria" maxlength="800" rows="3" class="campo-memoria" placeholder="Informacoes para lembrar"></textarea>' +
      '<button class="botao-padrao" id="metLembrar">Guardar memoria</button><div id="metMemorias"></div>' +
      '<button class="botao-padrao" id="metExportar">Exportar memorias</button></div>');
    document.getElementById('metPerfil').value = personalidade;
    function atualizarMemorias() {
      var area = document.getElementById('metMemorias'); area.replaceChildren();
      memorias.forEach(function (m) {
        var item = document.createElement('div'); item.className = 'memoria-nuvem';
        var editor = document.createElement('textarea'); editor.value = m.texto; editor.maxLength=800; editor.rows=2;
        var salvar = document.createElement('button'); salvar.textContent='Salvar';
        salvar.onclick = function(){m.texto=editor.value.trim().slice(0,800);gravar('memorias',memorias);avisoMetrys('Memoria atualizada.');};
        var apagar = document.createElement('button'); apagar.textContent='Excluir';
        apagar.onclick=function(){memorias=memorias.filter(function(x){return x.id!==m.id;});gravar('memorias',memorias);atualizarMemorias();};
        item.append(editor,salvar,apagar);area.append(item);
      });
    }
    atualizarMemorias();
    document.getElementById('metSalvarToken').onclick = function () {
      var t = document.getElementById('metToken').value.trim();
      if (!/^hf_[a-zA-Z0-9_-]{8,}$/.test(t)) { avisoMetrys('Informe um token HF valido.'); return; }
      if (ponte && typeof ponte.nuvemSalvarToken === 'function') ponte.nuvemSalvarToken(t); else tokenWeb=t;
      document.getElementById('metToken').value=''; avisoMetrys('Token configurado.');
    };
    document.getElementById('metLimparToken').onclick = function () { tokenWeb='';if(ponte&&ponte.nuvemApagarToken) ponte.nuvemApagarToken();avisoMetrys('Token apagado.'); };
    document.getElementById('metSalvarPerfil').onclick = function () { personalidade=document.getElementById('metPerfil').value.trim().slice(0,1500);gravar('personalidade',personalidade);avisoMetrys('Personalidade atualizada.'); };
    document.getElementById('metLembrar').onclick = function () { lembrar(document.getElementById('metNovaMemoria').value); document.getElementById('metNovaMemoria').value='';atualizarMemorias(); };
    document.getElementById('metExportar').onclick = function () {
      var blob=new Blob([JSON.stringify({personalidade:personalidade,memorias:memorias},null,2)],{type:'application/json'});
      if (ponte && typeof ponte.salvarTexto === 'function') { ponte.salvarTexto(JSON.stringify({personalidade:personalidade,memorias:memorias},null,2),'metrys-memorias.json');return; }
      var a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='metrys-memorias.json';a.click();setTimeout(function(){URL.revokeObjectURL(a.href);},1000);
    };
  }
  botao.addEventListener('click',function(){escolherModo(!online);});
  enviar.addEventListener('click',function(ev){if(!online)return;ev.preventDefault();ev.stopImmediatePropagation();enviarTexto();},true);
  campo.addEventListener('keydown',function(ev){if(online&&ev.key==='Enter'&&!ev.shiftKey&&!ev.isComposing){ev.preventDefault();ev.stopImmediatePropagation();enviarTexto();}},true);
  parar.addEventListener('click',function(ev){if(!online)return;ev.preventDefault();ev.stopImmediatePropagation();cancelar();},true);
  seletor.addEventListener('click',function(ev){if(online){ev.preventDefault();ev.stopImmediatePropagation();abrirPainel();}},true);
  var nova=document.getElementById('novaConversa');
  if(nova) nova.addEventListener('click',function(ev){if(!online)return;ev.preventDefault();ev.stopImmediatePropagation();chatId='';chat();desenhar();if(typeof fecharLateral==='function')fecharLateral();},true);
  var arvore=document.getElementById('arvoreConversas');
  if(arvore) arvore.addEventListener('click',function(ev){if(!online)return;var item=ev.target.closest('[data-conversa]');if(item){ev.preventDefault();ev.stopImmediatePropagation();chatId=item.dataset.conversa;persistir();desenhar();if(typeof fecharLateral==='function')fecharLateral();}},true);
  atualizarBotao();
})();
