/* Vercel Function: proxy exclusivo para Kimi K3 com token fornecido pelo usuario.
   Nao guardar historico, credenciais, logs de perguntas ou chaves no repositorio. */
const ENDPOINT = 'https://router.huggingface.co/v1/chat/completions';
const MODELO = 'moonshotai/Kimi-K3:cheapest';

function json(res, status, data) {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  return res.status(status).json(data);
}
function valida(corpo) {
  if (!corpo || !Array.isArray(corpo.mensagens) || corpo.mensagens.length < 1 || corpo.mensagens.length > 16) return null;
  const lista = [];
  let soma = 0;
  for (const item of corpo.mensagens) {
    if (!item || !['system','user','assistant'].includes(item.role) || typeof item.content !== 'string') return null;
    if (!item.content.trim() || item.content.length > 15000) return null;
    soma += item.content.length;
    lista.push({role:item.role,content:item.content});
  }
  if (soma > 42000) return null;
  return {model:MODELO, messages:lista, max_tokens:Math.min(512,Math.max(1,Number(corpo.max_tokens)||384))};
}
export default async function handler(req, res) {
  if (req.method !== 'POST') return json(res,405,{erro:'Somente POST.'});
  // Mesmo que uma origem externa invoque a rota, ela nao usa nenhum token do servidor.
  const token = (req.headers.authorization || '').replace(/^Bearer\s+/i,'').trim();
  if (!/^hf_[a-zA-Z0-9_-]{8,}$/.test(token)) return json(res,401,{erro:'Token ausente ou invalido.'});
  let corpo = req.body;
  if (typeof corpo === 'string') {
    if (corpo.length > 52000) return json(res,413,{erro:'Requisicao muito grande.'});
    try { corpo = JSON.parse(corpo); } catch (_) { return json(res,400,{erro:'JSON invalido.'}); }
  }
  const pacote = valida(corpo);
  if (!pacote) return json(res,400,{erro:'Mensagens invalidas ou grandes demais.'});
  const controlador = new AbortController();
  const timeout = setTimeout(() => controlador.abort(), 80000);
  try {
    const resposta = await fetch(ENDPOINT, {
      method:'POST', signal:controlador.signal, redirect:'error',
      headers:{'Authorization':'Bearer ' + token,'Content-Type':'application/json'},
      body:JSON.stringify(pacote)
    });
    if (!resposta.ok) {
      const status = [400,401,402,403,429,503].includes(resposta.status) ? resposta.status : 502;
      return json(res,status,{erro:'O provedor nao concluiu a requisicao. Codigo ' + resposta.status});
    }
    const dados = await resposta.json();
    const texto = dados && dados.choices && dados.choices[0] && dados.choices[0].message && dados.choices[0].message.content;
    if (typeof texto !== 'string' && !Array.isArray(texto)) return json(res,502,{erro:'Resposta do provedor sem mensagem.'});
    return json(res,200,{choices:[{message:{content:texto}}]});
  } catch (_) {
    return json(res,504,{erro:'Conexao com o provedor falhou ou expirou.'});
  } finally { clearTimeout(timeout); }
}
