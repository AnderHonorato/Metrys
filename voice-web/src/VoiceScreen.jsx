import React, { useCallback, useEffect, useRef, useState } from 'react';
import { VoiceRing, useMicrophone } from 'voice-ring';

const rotulos = {
  idle: ['Pronto para ouvir', 'Toque no botão para ativar o microfone.'],
  listening: ['Ouvindo você', 'O anel acompanha o volume da sua voz.'],
  processing: ['Pensando', 'Prévia da animação de processamento.'],
  speaking: ['Falando', 'Prévia da voz do assistente.']
};

export default function VoiceScreen() {
  const mic = useMicrophone();
  const [estado, definirEstado] = useState('idle');
  const [humor, definirHumor] = useState(null);
  const [tema, definirTema] = useState('dark');
  const [direcao, definirDirecao] = useState(0);
  const [erro, definirErro] = useState('');
  const [controles, mostrarControles] = useState(false);
  const nivelFala = useRef(0);
  const animacao = useRef(null);
  const temporizador = useRef(null);

  const parar = useCallback(() => {
    try { mic.stop?.(); } catch { /* fluxo já encerrado */ }
    try { window.speechSynthesis?.cancel(); } catch { /* sintetizador indisponível */ }
    window.clearTimeout(temporizador.current);
    cancelAnimationFrame(animacao.current);
    nivelFala.current = 0;
    definirEstado('idle');
  }, [mic.stop]);

  useEffect(() => () => {
    try { mic.stop?.(); } catch { /* sem fluxo ativo */ }
    try { window.speechSynthesis?.cancel(); } catch { /* sem fala ativa */ }
    window.clearTimeout(temporizador.current);
    cancelAnimationFrame(animacao.current);
  }, [mic.stop]);

  const escutar = async () => {
    definirErro('');
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
      definirErro('O microfone precisa de uma página HTTPS e de um navegador compatível.');
      return;
    }
    try {
      await mic.start();
      definirEstado('listening');
    } catch (e) {
      definirEstado('idle');
      definirErro(e?.name === 'NotAllowedError'
        ? 'Permissão negada. Autorize o microfone nas configurações do navegador.'
        : 'Não foi possível acessar o microfone. Verifique as permissões do aparelho.');
    }
  };

  const simularProcessamento = () => {
    parar();
    definirErro('');
    definirEstado('processing');
    temporizador.current = window.setTimeout(() => definirEstado('idle'), 4500);
  };

  const simularFala = () => {
    parar();
    definirErro('');
    definirEstado('speaking');

    // O visualizador recebe um nível 0–1 por frame; isto é apenas a prévia,
    // não captura o áudio de saída real do sintetizador.
    const inicio = performance.now();
    const animar = (agora) => {
      const t = (agora - inicio) / 1000;
      nivelFala.current = Math.max(0.08, Math.min(0.9,
        0.32 + 0.2 * Math.sin(t * 11) + 0.16 * Math.sin(t * 23) + 0.08 * Math.sin(t * 4)));
      animacao.current = requestAnimationFrame(animar);
    };
    animacao.current = requestAnimationFrame(animar);

    if ('speechSynthesis' in window) {
      const fala = new SpeechSynthesisUtterance('Olá! Eu sou o Metrys. Esta é uma prévia do meu novo modo de voz. O anel acompanha a conversa e muda suavemente de estado.');
      fala.lang = 'pt-BR';
      fala.rate = 0.96;
      fala.onend = () => { cancelAnimationFrame(animacao.current); nivelFala.current = 0; definirEstado('idle'); };
      fala.onerror = () => { cancelAnimationFrame(animacao.current); nivelFala.current = 0; definirEstado('idle'); };
      window.speechSynthesis.speak(fala);
    } else {
      temporizador.current = window.setTimeout(() => { cancelAnimationFrame(animacao.current); nivelFala.current = 0; definirEstado('idle'); }, 4500);
    }
  };

  const ativo = estado !== 'idle';
  return (
    <div className={`pagina tema-${tema}`}>
      <div className="luz luz-um" aria-hidden="true" />
      <div className="luz luz-dois" aria-hidden="true" />
      <header className="topo">
        <div className="marca"><span className="marca-simbolo">✳</span><span>metrys<span className="ponto">.</span></span></div>
        <span className="identificador">MODO DE VOZ <span className="beta">PRÉVIA</span></span>
      </header>
      <main className="conteudo">
        <div className="eyebrow"><span className={`indicador ${estado}`}/><span>{estado === 'listening' ? 'MICROFONE ATIVO' : 'ASSISTENTE DE VOZ'}</span></div>
        <div className="orbita" aria-label={rotulos[estado][0]}>
          <VoiceRing
            size={Math.min(296, typeof window !== 'undefined' ? window.innerWidth - 56 : 296)}
            state={estado}
            stream={estado === 'listening' ? mic.stream : undefined}
            level={estado === 'speaking' ? () => nivelFala.current : 0}
            emotion={humor}
            direction={direcao}
            sensitivity={0.85}
            hoverBend={true}
            theme={tema}
            paused={false}
          />
        </div>
        <div className="mensagem" aria-live="polite">
          <h1>{rotulos[estado][0]}<span className="ponto">.</span></h1>
          <p>{rotulos[estado][1]}</p>
        </div>
        {erro && <p className="erro" role="alert">{erro}</p>}
        <div className="acoes">
          <button className="principal" onClick={ativo ? parar : escutar}>
            <span className="icone-acao" aria-hidden="true">{ativo ? '■' : '●'}</span>
            {ativo ? 'Encerrar' : 'Começar a ouvir'}
          </button>
          <button className="secundario" onClick={simularFala}>Ouvir demonstração <span aria-hidden="true">↗</span></button>
        </div>
        <button className="abrir-ajustes" onClick={() => mostrarControles(!controles)} aria-expanded={controles}>
          Ajustar visual <span className={controles ? 'girado' : ''}>⌄</span>
        </button>
        {controles && <section className="ajustes" aria-label="Ajustes do anel">
          <div className="campo"><label htmlFor="humor">Emoção</label><select id="humor" value={humor ?? ''} onChange={e => definirHumor(e.target.value || null)}>
            <option value="">Original</option><option value="positive">Positiva</option><option value="calm">Calma</option><option value="negative">Negativa</option>
          </select></div>
          <div className="campo"><label htmlFor="tema">Tema</label><select id="tema" value={tema} onChange={e => definirTema(e.target.value)}>
            <option value="dark">Escuro</option><option value="light">Claro</option>
          </select></div>
          <div className="campo campo-largo"><label htmlFor="direcao">Direção <span>{direcao < -0.15 ? 'Esquerda' : direcao > 0.15 ? 'Direita' : 'Centro'}</span></label>
            <input id="direcao" type="range" min="-1" max="1" step="0.05" value={direcao} onChange={e => definirDirecao(Number(e.target.value))} />
          </div>
          <button className="teste-processando" onClick={simularProcessamento}>Testar animação de processamento</button>
        </section>}
        <p className="nota">O áudio do microfone é usado apenas para animar o anel. Esta prévia não envia áudio nem se conecta ao modelo de IA do Metrys.</p>
      </main>
      <footer>METRYS <span>·</span> VOZ EM MOVIMENTO</footer>
    </div>
  );
}
