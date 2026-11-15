// =====================================================================
// Funções compartilhadas pelo site e pelo painel.
// Todo texto vindo do servidor é inserido com textContent (proteção contra XSS).
// =====================================================================

export const ROTULOS = {
  status: { PENDENTE: 'Pendente', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado' },
  tipo: { RETIRADA: 'Retirada', ENTREGA: 'Entrega' },
  forma: { PIX: 'Pix', CARTAO: 'Cartão', DINHEIRO: 'Dinheiro' },
  origem: { SITE: 'Site', WHATSAPP: 'WhatsApp', PRESENCIAL: 'Presencial' },
  unidade: { KG: 'kg', G: 'g', LITRO: 'litro', ML: 'mL', UNIDADE: 'unidade', DUZIA: 'dúzia', PACOTE: 'pacote' },
};

export class ErroApi extends Error {
  constructor(mensagem, status, campos) {
    super(mensagem);
    this.status = status;
    this.campos = campos || {};
  }
}

let tokenCsrf = null;

async function obterCsrf(renovar) {
  if (!tokenCsrf || renovar) {
    const resp = await fetch('/api/csrf', { credentials: 'same-origin' });
    tokenCsrf = await resp.json();
  }
  return tokenCsrf;
}

/**
 * Chama a API. Envia o token CSRF nas ações do painel e redireciona para o login
 * se a sessão da administradora tiver expirado.
 */
export async function api(url, { metodo = 'GET', corpo, formulario } = {}) {
  const opcoes = { method: metodo, headers: { Accept: 'application/json' }, credentials: 'same-origin' };
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }
  if (formulario) opcoes.body = formulario;
  const precisaToken = metodo !== 'GET' && !url.startsWith('/api/publico/');

  for (let tentativa = 0; tentativa < 2; tentativa++) {
    if (precisaToken) {
      const t = await obterCsrf(tentativa > 0);
      opcoes.headers[t.headerName] = t.token;
    }
    let resp;
    try {
      resp = await fetch(url, opcoes);
    } catch {
      throw new ErroApi('Sem conexão com o sistema. Verifique a internet e tente de novo.', 0);
    }
    if (resp.status === 403 && precisaToken && tentativa === 0) continue; // token vencido: renova e tenta de novo
    if (resp.status === 401 && url.startsWith('/api/admin/') && url !== '/api/admin/login') {
      location.href = '/admin/login.html';
      throw new ErroApi('Sua sessão terminou. Entre novamente.', 401);
    }
    if (resp.status === 204) return null;
    const texto = await resp.text();
    let dados = null;
    try { dados = texto ? JSON.parse(texto) : null; } catch { dados = null; }
    if (!resp.ok) {
      throw new ErroApi((dados && dados.mensagem) || 'Não foi possível concluir. Tente novamente.', resp.status,
        dados && dados.campos);
    }
    return dados;
  }
  throw new ErroApi('Não foi possível concluir. Recarregue a página e tente novamente.', 403);
}

/** Cria um elemento de forma segura: el('p', { class: 'x', text: 'olá' }, filho1, 'texto'). */
export function el(tag, props = {}, ...filhos) {
  const e = document.createElement(tag);
  for (const [chave, valor] of Object.entries(props || {})) {
    if (valor === undefined || valor === null || valor === false) continue;
    if (chave === 'class') e.className = valor;
    else if (chave === 'text') e.textContent = valor;
    else if (chave.startsWith('on') && typeof valor === 'function') e.addEventListener(chave.slice(2), valor);
    else if (chave === 'dataset') Object.assign(e.dataset, valor);
    else if (valor === true) e.setAttribute(chave, '');
    else e.setAttribute(chave, String(valor));
  }
  for (const f of filhos.flat()) {
    if (f === null || f === undefined || f === false) continue;
    e.append(f instanceof Node ? f : String(f));
  }
  return e;
}

// ---------------------------------------------------------------- formatação

const formatoMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const moeda = (v) => formatoMoeda.format(Number(v || 0));

const doisDigitos = (n) => String(n).padStart(2, '0');

/** Data de hoje (ou deslocada em dias) no formato do campo <input type="date">. */
export function hojeISO(deslocamentoDias = 0) {
  const d = new Date();
  d.setDate(d.getDate() + deslocamentoDias);
  return `${d.getFullYear()}-${doisDigitos(d.getMonth() + 1)}-${doisDigitos(d.getDate())}`;
}

export function dataBR(iso) {
  if (!iso) return '';
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}

export function diaDaSemana(iso) {
  const [a, m, d] = iso.split('-').map(Number);
  return new Date(a, m - 1, d).toLocaleDateString('pt-BR', { weekday: 'long' });
}

/** "sábado, 15/11/2026" — com "hoje"/"amanhã" quando for o caso. */
export function dataPorExtenso(iso) {
  if (!iso) return '';
  if (iso === hojeISO()) return `Hoje, ${dataBR(iso)}`;
  if (iso === hojeISO(1)) return `Amanhã, ${dataBR(iso)}`;
  const dia = diaDaSemana(iso);
  return `${dia.charAt(0).toUpperCase()}${dia.slice(1)}, ${dataBR(iso)}`;
}

export const hora = (h) => (h ? h.slice(0, 5) : '');

export function telefoneBR(t) {
  if (!t) return '';
  if (t.length === 11) return `(${t.slice(0, 2)}) ${t.slice(2, 7)}-${t.slice(7)}`;
  if (t.length === 10) return `(${t.slice(0, 2)}) ${t.slice(2, 6)}-${t.slice(6)}`;
  return t;
}

export function quantidadeBR(n) {
  return Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 3 });
}

// ---------------------------------------------------------------- interface

/** Mostra uma mensagem rápida na parte de baixo da tela. */
export function aviso(mensagem, tipo = 'ok') {
  let area = document.querySelector('.avisos');
  if (!area) {
    area = el('div', { class: 'avisos', role: 'status', 'aria-live': 'polite' });
    document.body.append(area);
  }
  const caixa = el('div', { class: tipo === 'erro' ? 'aviso erro' : 'aviso', text: mensagem });
  area.append(caixa);
  setTimeout(() => caixa.remove(), tipo === 'erro' ? 7000 : 4000);
}

/** Guarda um aviso para ser mostrado na próxima página (ex.: depois de salvar e voltar à lista). */
export function avisoNaProximaPagina(mensagem) { sessionStorage.setItem('aviso', mensagem); }

export function mostrarAvisoPendente() {
  const m = sessionStorage.getItem('aviso');
  if (m) { sessionStorage.removeItem('aviso'); aviso(m); }
}

/** Mostra o erro no topo do formulário e marca os campos com problema. */
export function mostrarErro(form, erro) {
  let caixa = form.querySelector('.erro-form');
  if (!caixa) {
    caixa = el('div', { class: 'erro-form', role: 'alert' });
    form.prepend(caixa);
  }
  caixa.textContent = erro.message || String(erro);
  caixa.hidden = false;
  form.querySelectorAll('[aria-invalid]').forEach((c) => c.removeAttribute('aria-invalid'));
  for (const nome of Object.keys(erro.campos || {})) {
    const campo = form.elements.namedItem(nome);
    if (campo && campo.setAttribute) campo.setAttribute('aria-invalid', 'true');
  }
  caixa.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

export function limparErro(form) {
  const caixa = form.querySelector('.erro-form');
  if (caixa) caixa.hidden = true;
  form.querySelectorAll('[aria-invalid]').forEach((c) => c.removeAttribute('aria-invalid'));
}

/** Desativa o botão durante a ação, evitando cliques duplos (pedido duplicado). */
export async function comBotaoOcupado(botao, textoOcupado, acao) {
  const original = botao.textContent;
  botao.disabled = true;
  botao.textContent = textoOcupado;
  try {
    return await acao();
  } finally {
    botao.disabled = false;
    botao.textContent = original;
  }
}

export function etiqueta(texto, classe) { return el('span', { class: `etiqueta ${classe || ''}`, text: texto }); }

export const vazio = (texto) => el('p', { class: 'vazio', text: texto });

/** Converte "12,50" ou "12.50" em número. */
export function lerNumero(texto) {
  if (texto === null || texto === undefined) return null;
  const t = String(texto).trim().replace(/\s/g, '');
  if (!t) return null;
  const normalizado = t.includes(',') ? t.replace(/\./g, '').replace(',', '.') : t;
  const n = Number(normalizado);
  return Number.isFinite(n) ? n : NaN;
}
