import React, { useEffect, useRef, useState } from 'react';
import VoiceScreen from './VoiceScreen.jsx';
import './integracao.css';

/** Emula apenas a camada visual da WebView do Metrys no navegador. */
export default function MetrysIntegrado() {
  const [vozAberta, definirVozAberta] = useState(false);
  const [framePronto, definirFramePronto] = useState(false);
  const frame = useRef(null);
  const ultimaAcao = useRef(null);

  const fechar = () => {
    definirVozAberta(false);
    requestAnimationFrame(() => ultimaAcao.current?.focus?.());
  };
  const inserirTexto = texto => {
    const valor = String(texto || '').trim().slice(0, 5000);
    if (valor) frame.current?.contentWindow?.postMessage({ tipo: 'metrys:transcricao', texto: valor }, window.location.origin);
    fechar();
  };

  useEffect(() => {
    const receber = evento => {
      if (evento.origin !== window.location.origin || evento.source !== frame.current?.contentWindow) return;
      if (evento.data?.tipo === 'metrys:abrir-voz') {
        ultimaAcao.current = frame.current;
        definirVozAberta(true);
      }
    };
    const tecla = evento => { if (evento.key === 'Escape') definirVozAberta(false); };
    window.addEventListener('message', receber);
    window.addEventListener('keydown', tecla);
    return () => {
      window.removeEventListener('message', receber);
      window.removeEventListener('keydown', tecla);
    };
  }, []);
  return (
    <div className="shell-metrys">
      <iframe
        ref={frame}
        title="Metrys Ateliê — interface de chat"
        className="app-metrys-frame"
        src="/metrys/index.html"
        onLoad={() => definirFramePronto(true)}
      />
      {!framePronto && <div className="carregando-metrys">Abrindo o Metrys…</div>}
      {vozAberta && <div className="voz-camada" role="dialog" aria-modal="true" aria-label="Conversa por voz no Metrys">
        <button type="button" className="voltar-ao-chat" onClick={fechar} aria-label="Fechar modo de voz e voltar ao chat">
          <span aria-hidden="true">‹</span> Voltar ao chat
        </button>
        <div className="tela-voz"><VoiceScreen onTexto={inserirTexto}/></div>
      </div>}
    </div>
  );
}
