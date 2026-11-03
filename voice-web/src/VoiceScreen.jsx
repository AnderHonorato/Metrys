import React, { useCallback, useEffect, useRef, useState } from 'react';
import { VoiceRing, useMicrophone } from 'voice-ring';

const ESTADOS = [
  { id: 'idle', nome: 'Repouso', descricao: 'Movimento leve e contínuo', icone: '◌' },
  { id: 'listening', nome: 'Ouvindo', descricao: 'Reage ao áudio recebido', icone: '◉' },
  { id: 'processing', nome: 'Processando', descricao: 'Gira enquanto trabalha', icone: '✳' },
  { id: 'speaking', nome: 'Falando', descricao: 'Movimento de reprodução', icone: '◍' },
];
const TEXTOS = {
  idle: ['Pronto para ouvir', 'Um brilho calmo, esperando a sua voz.'],
  listening: ['Ouvindo você', 'O espectro reage às mudanças de intensidade.'],
  processing: ['Processando', 'A luz percorre o anel enquanto a IA trabalha.'],
  speaking: ['Falando com você', 'O anel acompanha a demonstração de fala.'],
};
const limitar = (v, minimo, maximo) => Math.min(maximo, Math.max(minimo, v));
const formatar = (valor, casas = 2) => Number(valor).toFixed(casas).replace('.', ',');

function ControleFaixa({ id, titulo, valor, minimo, maximo, passo, aoMudar, descricao, formato }) {
  return <div className="controle-faixa">
    <div className="rotulo-controle"><label htmlFor={id}>{titulo}</label><output htmlFor={id}>{formato ? formato(valor) : formatar(valor)}</output></div>
    <input type="range" id={id} min={minimo} max={maximo} step={passo} value={valor} onChange={e => aoMudar(Number(e.target.value))} />
    {descricao && <small>{descricao}</small>}
  </div>;
}

export default function VoiceScreen() {
  const mic = useMicrophone();
  const [estado, definirEstado] = useState('idle');
  const [fonte, definirFonte] = useState('microfone');
  const [nivel, definirNivel] = useState(0.48);
  const [emocao, definirEmocao] = useState(null);
  const [direcao, definirDirecao] = useState(0);
  const [tema, definirTema] = useState('auto');
  const [tamanho, definirTamanho] = useState(270);
  const [sensibilidade, definirSensibilidade] = useState(0.85);
  const [velocidade, definirVelocidade] = useState(1);
  const [curvatura, definirCurvatura] = useState(0.55);
  const [pausado, definirPausado] = useState(false);
  const [erro, definirErro] = useState('');
  const [aba, definirAba] = useState('aparencia');
  const [escutaReal, definirEscutaReal] = useState(false);
  const nivelDaFala = useRef(0);
  const animacao = useRef(0);
  const timeout = useRef(0);
  const versao = useRef(0);

  const parar = useCallback(() => {
    versao.current += 1;
    try { mic.stop?.(); } catch { /* microfone inativo */ }
    try { window.speechSynthesis?.cancel(); } catch { /* sem síntese disponível */ }
    clearTimeout(timeout.current);
    cancelAnimationFrame(animacao.current);
    nivelDaFala.current = 0;
    definirEscutaReal(false);
    definirEstado('idle');
  }, [mic.stop]);

  useEffect(() => () => {
    versao.current += 1;
    try { mic.stop?.(); } catch { /* já parado */ }
    try { window.speechSynthesis?.cancel(); } catch { /* indisponível */ }
    clearTimeout(timeout.current);
    cancelAnimationFrame(animacao.current);
  }, [mic.stop]);

  useEffect(() => {
    const media = window.matchMedia?.('(prefers-color-scheme: dark)');
    const atualizar = () => document.documentElement.style.colorScheme = tema === 'auto'
      ? (media?.matches ? 'dark' : 'light') : tema;
    atualizar();
    media?.addEventListener?.('change', atualizar);
    return () => media?.removeEventListener?.('change', atualizar);
  }, [tema]);

  const escolherFonte = novo => {
    parar();
    definirFonte(novo);
    definirErro('');
  };

  const ouvir = async () => {
    parar();
    definirErro('');
    if (fonte === 'manual') {
      definirEstado('listening');
      return;
    }
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
      definirErro('Para usar o microfone, abra o site em HTTPS num navegador compatível.');
      return;
    }
    try {
      await mic.start();
      definirEscutaReal(true);
      definirEstado('listening');
    } catch (ex) {
      definirEscutaReal(false);
      definirEstado('idle');
      definirErro(ex?.name === 'NotAllowedError'
        ? 'Permissão negada. Autorize o microfone nas configurações do navegador.'
        : 'Não foi possível ativar o microfone. Confira o dispositivo e as permissões.');
    }
  };

  const pensar = () => {
    parar();
    definirErro('');
    definirEstado('processing');
  };

  const falar = () => {
    parar();
    definirErro('');
    definirEstado('speaking');
    const atual = versao.current;
    const inicio = performance.now();
    const animar = instante => {
      if (atual !== versao.current) return;
      const t = (instante - inicio) / 1000;
      nivelDaFala.current = limitar(0.35 + Math.sin(t * 12) * 0.21 + Math.sin(t * 21) * 0.16 + Math.sin(t * 6) * 0.11, 0.05, 1);
      animacao.current = requestAnimationFrame(animar);
    };
    animacao.current = requestAnimationFrame(animar);
    const encerrar = () => { if (atual === versao.current) parar(); };
    if ('speechSynthesis' in window && 'SpeechSynthesisUtterance' in window) {
      const fala = new SpeechSynthesisUtterance('Olá! Eu sou o Metrys. Este é o laboratório do anel de voz. Teste as cores, a direção, a sensibilidade e os movimentos enquanto conversamos.');
      fala.lang = 'pt-BR';
      fala.rate = 0.94;
      fala.onend = encerrar;
      fala.onerror = encerrar;
      window.speechSynthesis.speak(fala);
      timeout.current = window.setTimeout(encerrar, 16000);
    } else {
      // Fallback visual quando o dispositivo não oferece síntese de fala.
      timeout.current = window.setTimeout(encerrar, 5800);
    }
  };

  const mudarEstado = proximo => {
    if (proximo === 'idle') parar();
    else if (proximo === 'listening') ouvir();
    else if (proximo === 'processing') pensar();
    else falar();
  };

  const largura = typeof window !== 'undefined' ? window.innerWidth : 400;
  const tamanhoRenderizado = Math.min(tamanho, Math.max(190, largura - 54));
  const nivelManual = nivel;
  const origemMicrofone = estado === 'listening' && fonte === 'microfone' && escutaReal;
  const nivelRenderizado = estado === 'speaking' ? () => nivelDaFala.current
    : estado === 'listening' && fonte === 'manual' ? () => nivelManual : 0;
  const escuro = tema === 'auto'
    ? (typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches) : tema === 'dark';

  return (
    <div className={`pagina ${escuro ? 'tema-escuro' : 'tema-claro'}`}>
      <div className="luz luz-a" aria-hidden="true"/><div className="luz luz-b" aria-hidden="true"/>
      <header className="cabecalho">
        <div className="marca"><div className="emblema"><span/><span/><span/></div><strong>metrys<span className="ponto">.</span></strong></div>
        <div className="marcadores"><span className="marcador-versao">VOICE RING</span><span className="marcador-lab">LABORATÓRIO</span></div>
      </header>
      <main className="conteudo">
        <div className="etiqueta"><span className={`sinal ${estado}`}/>{estado === 'listening' && origemMicrofone ? 'MICROFONE ATIVO' : 'EXPLORAR ANIMAÇÕES'}</div>
        <div className="palco">
          <div className="halo-palco" aria-hidden="true"/>
          <VoiceRing
            size={tamanhoRenderizado}
            state={estado}
            stream={origemMicrofone ? mic.stream : undefined}
            level={nivelRenderizado}
            emotion={emocao}
            direction={direcao}
            hoverBend={curvatura}
            sensitivity={sensibilidade}
            speed={velocidade}
            theme={tema}
            paused={pausado}
          />
        </div>
        <div className="titulo-central" aria-live="polite">
          <h1>{TEXTOS[estado][0]}<span className="ponto">.</span></h1>
          <p>{TEXTOS[estado][1]}</p>
        </div>
        {erro && <p className="erro" role="alert">{erro}</p>}
        <section className="estados-secao" aria-label="Testar estados do anel">
          <div className="titulo-secao"><h2>Estados do anel</h2><small>4 modos de animação</small></div>
          <div className="estados">
            {ESTADOS.map(item => <button key={item.id} type="button" className={`botao-estado ${estado === item.id ? 'selecionado' : ''}`}
              onClick={() => mudarEstado(item.id)} aria-pressed={estado === item.id} title={item.descricao}>
              <span className="estado-icone" aria-hidden="true">{item.icone}</span><span>{item.nome}</span>
            </button>)}
          </div>
          <div className="ferramentas">
            <button className="acao-parar" type="button" onClick={parar} disabled={estado === 'idle'}>■ Encerrar</button>
            <button className="acao-pausa" type="button" onClick={() => definirPausado(p => !p)} aria-pressed={pausado}>
              {pausado ? '▶ Retomar animação' : 'Ⅱ Pausar animação'}
            </button>
          </div>
        </section>
        <section className="painel-lab" aria-label="Laboratório de personalização">
          <div className="painel-topo"><div><span className="sobretitulo">PERSONALIZE EM TEMPO REAL</span><h2>Estúdio do anel</h2></div><span className="painel-index">01 — 03</span></div>
          <div className="abas" role="tablist" aria-label="Categorias de ajustes">
            {[['aparencia','Aparência'],['reacao','Reação'],['audio','Áudio']].map(([id,nome]) =>
              <button key={id} role="tab" type="button" aria-selected={aba===id} className={aba===id?'aba-ativa':''} onClick={()=>definirAba(id)}>{nome}</button>)}
          </div>
          {aba === 'aparencia' && <div className="grupo-controles" role="tabpanel">
            <div className="campo"><label htmlFor="emocao">Emoção / paleta</label><select id="emocao" value={emocao ?? ''} onChange={e=>definirEmocao(e.target.value||null)}>
              <option value="">Original · espectro</option><option value="positive">Positiva · verde</option>
              <option value="calm">Calma · turquesa</option><option value="negative">Negativa · vermelho</option>
            </select></div>
            <div className="campo"><label htmlFor="tema">Tema</label><select id="tema" value={tema} onChange={e=>definirTema(e.target.value)}>
              <option value="auto">Automático</option><option value="dark">Escuro</option><option value="light">Claro</option>
            </select></div>
            <ControleFaixa id="tamanho" titulo="Tamanho do anel" valor={tamanho} minimo={180} maximo={360} passo={10} aoMudar={definirTamanho} formato={v=>v+' px'}/>
            <ControleFaixa id="direcao" titulo="Direção da voz" valor={direcao} minimo={-1} maximo={1} passo={0.05} aoMudar={definirDirecao}
              formato={v=>v < -.15?'Esquerda '+formatar(Math.abs(v)):v>.15?'Direita '+formatar(v):'Centro'}/>
            <small className="observacao campo-largo">A direção é manual no navegador: a detecção automática com modelo estéreo é exclusiva da implementação SwiftUI para iPhone.</small>
          </div>}
          {aba === 'reacao' && <div className="grupo-controles" role="tabpanel">
            <ControleFaixa id="sensibilidade" titulo="Sensibilidade" valor={sensibilidade} minimo={0.1} maximo={2.5} passo={0.05} aoMudar={definirSensibilidade} descricao="Intensidade da reação ao nível de áudio."/>
            <ControleFaixa id="velocidade" titulo="Velocidade" valor={velocidade} minimo={0.2} maximo={2.5} passo={0.1} aoMudar={definirVelocidade} formato={v=>formatar(v,1)+'×'} descricao="Ritmo das transições e do movimento."/>
            <ControleFaixa id="curvatura" titulo="Curvatura ao passar o cursor" valor={curvatura} minimo={0} maximo={1} passo={0.05} aoMudar={definirCurvatura} descricao="hoverBend: funciona com ponteiro de mouse no desktop."/>
            <div className="campo-largo resumo-reacao"><strong>{pausado?'Animação pausada':'Animação em andamento'}</strong><span>As mudanças são aplicadas ao anel imediatamente, sem reiniciar a página.</span></div>
          </div>}
          {aba === 'audio' && <div className="grupo-controles" role="tabpanel">
            <div className="campo campo-largo"><label>Fonte da animação</label>
              <div className="seletor-fonte">
                <button type="button" className={fonte==='microfone'?'ativo':''} onClick={()=>escolherFonte('microfone')} aria-pressed={fonte==='microfone'}>Microfone real</button>
                <button type="button" className={fonte==='manual'?'ativo':''} onClick={()=>escolherFonte('manual')} aria-pressed={fonte==='manual'}>Nível manual</button>
              </div>
            </div>
            {fonte==='manual'?<div className="campo-largo"><ControleFaixa id="nivel" titulo="Nível de entrada" valor={nivel} minimo={0} maximo={1} passo={0.01} aoMudar={definirNivel} formato={v=>Math.round(v*100)+'%'} descricao="Selecione Ouvindo para acompanhar este controle em tempo real."/></div>:
              <div className="campo-largo nota-fonte"><strong>🎙 Microfone do dispositivo</strong><p>Selecione Ouvindo e autorize o navegador. O áudio alimenta apenas a animação e não é gravado nem enviado a servidores por esta página.</p></div>}
            <div className="campo-largo nota-fonte"><strong>🔊 Reprodução de exemplo</strong><p>O botão Falando usa a voz sintética do navegador com níveis visuais simulados. Não é uma conversa com IA.</p></div>
          </div>}
          <button className="restaurar" type="button" onClick={()=>{
            parar(); definirFonte('microfone'); definirNivel(.48); definirEmocao(null); definirDirecao(0); definirTema('auto');
            definirTamanho(270); definirSensibilidade(.85); definirVelocidade(1); definirCurvatura(.55); definirPausado(false); definirErro('');
          }}>↺ Restaurar configurações padrão</button>
        </section>
        <p className="aviso-final">Prévia visual em React com Voice Ring. O motor de IA offline do Metrys não é acionado nesta página. Os modelos de emoção e direção da versão SwiftUI não funcionam no navegador.</p>
      </main>
      <footer>METRYS <span>·</span> EXPERIÊNCIA DE VOZ</footer>
    </div>
  );
}
