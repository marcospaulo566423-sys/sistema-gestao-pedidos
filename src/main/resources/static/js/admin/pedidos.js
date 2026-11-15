import { ROTULOS, api, aviso, dataPorExtenso, el, hojeISO, moeda, vazio } from '../comum.js';
import { cartaoPedido, confirmar, iniciarLayout } from './layout.js';

const lista = document.getElementById('lista');
const contagem = document.getElementById('contagem');
const inputData = document.getElementById('filtro-data');
const inputBusca = document.getElementById('filtro-busca');
const dialogo = document.getElementById('dialogo-pagamento');
const formPagamento = document.getElementById('form-pagamento');

const filtro = { status: 'PENDENTE', tipoEntrega: '' };

function configurarSegmentado(id, chave) {
  const grupo = document.getElementById(id);
  grupo.addEventListener('click', (e) => {
    const botao = e.target.closest('button');
    if (!botao) return;
    grupo.querySelectorAll('button').forEach((b) => b.setAttribute('aria-pressed', String(b === botao)));
    filtro[chave] = botao.dataset.valor;
    carregar();
  });
}

function marcarSegmentado(id, valor) {
  document.getElementById(id).querySelectorAll('button')
    .forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.valor === valor)));
}

// ---------------------------------------------------------------- ações

async function executar(acao, mensagem) {
  try {
    await acao();
    aviso(mensagem);
    await carregar();
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

function botao(texto, classe, aoClicar) {
  return el('button', { type: 'button', class: `botao ${classe}`, text: texto, onclick: aoClicar });
}

function acoesDo(p) {
  const url = `/api/admin/pedidos/${p.id}`;
  const acoes = [];
  if (p.status === 'PENDENTE') {
    acoes.push(botao('✔ Concluir', 'verde', () => executar(
      () => api(`${url}/status`, { metodo: 'PATCH', corpo: { status: 'CONCLUIDO' } }), 'Pedido concluído.')));
    acoes.push(el('a', { class: 'botao secundario', href: `/admin/pedido-form.html?id=${p.id}`, text: '✎ Editar' }));
  }
  if (p.status === 'CONCLUIDO') {
    acoes.push(botao('↺ Reabrir', 'secundario', () => executar(
      () => api(`${url}/status`, { metodo: 'PATCH', corpo: { status: 'PENDENTE' } }), 'Pedido voltou para pendente.')));
  }
  if (!p.pagamento && p.status !== 'CANCELADO') {
    acoes.push(botao('💲 Registrar pagamento', 'secundario', () => abrirPagamento(p)));
  }
  if (p.pagamento) {
    acoes.push(botao('Desfazer pagamento', 'perigo', () => {
      if (confirmar(`Desfazer o pagamento de ${moeda(p.pagamento.valor)}? A entrada também sai do Financeiro.`)) {
        executar(() => api(`${url}/pagamento`, { metodo: 'DELETE' }), 'Pagamento desfeito.');
      }
    }));
  }
  if (p.status === 'PENDENTE' && !p.pagamento) {
    acoes.push(botao('✖ Cancelar pedido', 'perigo', () => {
      if (confirmar(`Cancelar o pedido de ${p.cliente.nome}? Ele continua no histórico como cancelado.`)) {
        executar(() => api(`${url}/status`, { metodo: 'PATCH', corpo: { status: 'CANCELADO' } }), 'Pedido cancelado.');
      }
    }));
  }
  if (!p.pagamento && p.status !== 'CONCLUIDO') {
    acoes.push(botao('🗑 Excluir', 'perigo', () => {
      if (confirmar(`Excluir DEFINITIVAMENTE o pedido de ${p.cliente.nome}? Para desistências, prefira "Cancelar".`)) {
        executar(() => api(url, { metodo: 'DELETE' }), 'Pedido excluído.');
      }
    }));
  }
  return acoes;
}

let pedidoEmPagamento = null;

function abrirPagamento(p) {
  pedidoEmPagamento = p;
  document.getElementById('pagamento-resumo').textContent = `${p.cliente.nome} · Total ${moeda(p.total)}`;
  const forma = p.formaPagamento || 'PIX';
  formPagamento.elements.tipo.forEach((r) => { r.checked = r.value === forma; });
  formPagamento.elements.data.value = hojeISO();
  formPagamento.elements.data.max = hojeISO();
  dialogo.showModal();
}

dialogo.addEventListener('close', () => {
  if (dialogo.returnValue !== 'confirmar' || !pedidoEmPagamento) return;
  const p = pedidoEmPagamento;
  pedidoEmPagamento = null;
  executar(() => api(`/api/admin/pedidos/${p.id}/pagamento`, {
    metodo: 'POST',
    corpo: { tipo: formPagamento.elements.tipo.value, data: formPagamento.elements.data.value || null },
  }), `Pagamento de ${moeda(p.total)} registrado.`);
});

// ---------------------------------------------------------------- lista

async function carregar() {
  const params = new URLSearchParams();
  if (filtro.status) params.set('status', filtro.status);
  if (filtro.tipoEntrega) params.set('tipoEntrega', filtro.tipoEntrega);
  if (inputData.value) params.set('data', inputData.value);
  if (inputBusca.value.trim()) params.set('busca', inputBusca.value.trim());
  history.replaceState(null, '', `?${params}`);

  try {
    const pedidos = await api(`/api/admin/pedidos?${params}`);
    const partes = [`${pedidos.length} pedido(s)`];
    if (filtro.status) partes.push(ROTULOS.status[filtro.status].toLowerCase());
    if (filtro.tipoEntrega) partes.push(`para ${ROTULOS.tipo[filtro.tipoEntrega].toLowerCase()}`);
    contagem.textContent = partes.join(' · ');

    if (pedidos.length === 0) {
      lista.replaceChildren(vazio('Nenhum pedido encontrado com esses filtros.'));
      return;
    }
    // RF04: agrupados e organizados pela data de entrega/retirada
    const grupos = new Map();
    for (const p of pedidos) {
      if (!grupos.has(p.dataEntregaRetirada)) grupos.set(p.dataEntregaRetirada, []);
      grupos.get(p.dataEntregaRetirada).push(p);
    }
    lista.replaceChildren(...[...grupos.entries()].map(([dia, doDia]) =>
      el('section', { class: 'grupo-dia' },
        el('h2', { text: `${dataPorExtenso(dia)} — ${doDia.length} pedido(s)` }),
        el('div', { class: 'lista' }, doDia.map((p) => cartaoPedido(p, acoesDo(p)))))));
  } catch (erro) {
    lista.replaceChildren(vazio(erro.message));
  }
}

// ---------------------------------------------------------------- início

async function iniciar() {
  await iniciarLayout();
  const params = new URLSearchParams(location.search);
  if (params.has('status')) filtro.status = params.get('status');
  else if (params.has('data')) filtro.status = '';
  if (params.has('tipoEntrega')) filtro.tipoEntrega = params.get('tipoEntrega');
  inputData.value = params.get('data') || '';
  inputBusca.value = params.get('busca') || '';
  marcarSegmentado('filtro-status', filtro.status);
  marcarSegmentado('filtro-tipo', filtro.tipoEntrega);

  configurarSegmentado('filtro-status', 'status');
  configurarSegmentado('filtro-tipo', 'tipoEntrega');
  document.getElementById('filtros').addEventListener('submit', (e) => { e.preventDefault(); carregar(); });
  inputData.addEventListener('change', carregar);
  document.getElementById('botao-hoje').addEventListener('click', () => { inputData.value = hojeISO(); carregar(); });
  document.getElementById('botao-amanha').addEventListener('click', () => { inputData.value = hojeISO(1); carregar(); });
  document.getElementById('botao-qualquer-dia').addEventListener('click', () => { inputData.value = ''; carregar(); });
  await carregar();
}

iniciar();
