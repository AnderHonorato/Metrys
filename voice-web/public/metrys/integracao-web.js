/* Metrys Web: voice interface bridge, independent from Android offline engine. */
'use strict';
(() => {
  const ORIGEM = window.location.origin;
  function marcarPrevia() {
    const btn = document.getElementById('vozTopo');
    if (btn) {
      btn.title = 'Abrir conversa por voz dentro do Metrys';
      btn.setAttribute('aria-label', 'Abrir conversa por voz');
      btn.addEventListener('click', event => {
        event.preventDefault();
        event.stopImmediatePropagation();
        if (window.parent !== window) {
          window.parent.postMessage({ tipo: 'metrys:abrir-voz' }, ORIGEM);
        } else {
          window.location.assign('/');
        }
      }, true);
    }
    const titulo = document.getElementById('seloAtelie');
    if (titulo) {
      titulo.textContent = 'METRYS ONLINE · OFFLINE NO APK';
      titulo.title = 'Modo offline funciona no Android; modo online envia mensagens ao Kimi K3 via provedor remoto.';
    }
  }
  window.addEventListener('message', event => {
    if (event.source !== window.parent || event.origin !== ORIGEM) return;
    if (event.data?.tipo !== 'metrys:transcricao' || typeof event.data.texto !== 'string') return;
    const texto = event.data.texto.trim().slice(0, 5000);
    if (!texto) return;
    const campo = document.getElementById('campoMensagem');
    if (!campo) return;
    campo.value = (campo.value.trim() ? campo.value.trim() + '\n\n' : '') + texto;
    campo.dispatchEvent(new Event('input', { bubbles: true }));
    campo.focus();
    if (typeof aviso === 'function') aviso('Sua fala foi inserida no chat.');
  });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', marcarPrevia);
  else marcarPrevia();
})();
