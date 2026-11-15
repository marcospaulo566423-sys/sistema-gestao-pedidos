import {
  ROTULOS, api, comBotaoOcupado, dataPorExtenso, el, hojeISO, limparErro, moeda, mostrarErro, vazio,
} from '../comum.js';
import { fotoDoProduto, protegerFoto } from './fotos.js';

const form = document.getElementById('form-pedido');
const listaProdutos = document.getElementById('lista-produtos');
const campoEndereco = document.getElementById('campo-endereco');
const inputEndereco = document.getElementById('endereco');
const inputData = document.getElementById('data');
const selectHorario = document.getElementById('horario');
const botaoEnviar = document.getElementById('botao-enviar');

/** produtoId -> { produto, quantidade, saida } */
const escolhas = new Map();

// ---------------------------------------------------------------- campos de data/horário

inputData.min = hojeISO();
inputData.max = hojeISO(90);
inputData.value = hojeISO(1);

selectHorario.append(el('option', { value: '', text: 'Qualquer horário' }));
for (let h = 7; h <= 19; h++) {
  for (const m of ['00', '30']) {
    if (h === 19 && m === '30') continue;
    const valor = `${String(h).padStart(2, '0')}:${m}`;
    selectHorario.append(el('option', { value: valor, text: valor }));
  }
}

// ---------------------------------------------------------------- produtos

function alterarQuantidade(id, delta) {
  const e = escolhas.get(id);
  e.quantidade = Math.max(0, Math.min(999, e.quantidade + delta));
  e.saida.value = String(e.quantidade);
  e.linha.classList.toggle('selecionado', e.quantidade > 0);
  atualizarResumo();
}

async function carregarProdutos() {
  try {
    const produtos = await api('/api/publico/produtos');
    listaProdutos.replaceChildren();
    if (produtos.length === 0) {
      listaProdutos.append(vazio('O cardápio está sendo atualizado. Volte em breve!'));
      botaoEnviar.disabled = true;
      return;
    }
    const preSelecionado = new URLSearchParams(location.search).get('produto');
    for (const p of produtos) {
      const saida = el('output', { 'aria-live': 'polite', text: '0' });
      const linha = el('div', { class: 'escolha' },
        el('span', { class: 'foto' }, protegerFoto(el('img', { src: fotoDoProduto(p, 200), alt: '', width: 72, height: 72, loading: 'lazy' }))),
        el('div', {}, el('strong', { text: p.nome }), el('span', { class: 'preco', text: moeda(p.preco) })),
        el('div', { class: 'qtd' },
          el('button', { type: 'button', 'aria-label': `Diminuir ${p.nome}`, text: '−', onclick: () => alterarQuantidade(p.id, -1) }),
          saida,
          el('button', { type: 'button', 'aria-label': `Aumentar ${p.nome}`, text: '+', onclick: () => alterarQuantidade(p.id, 1) })));
      escolhas.set(p.id, { produto: p, quantidade: 0, saida, linha });
      listaProdutos.append(linha);
      if (String(p.id) === preSelecionado) alterarQuantidade(p.id, 1);
    }
    atualizarResumo();
  } catch (erro) {
    listaProdutos.replaceChildren(vazio(erro.message));
  }
}

// ---------------------------------------------------------------- resumo

function valor(nome) {
  const campo = form.elements.namedItem(nome);
  return campo ? campo.value : '';
}

function atualizarResumo() {
  const itens = document.getElementById('resumo-itens');
  const detalhes = document.getElementById('resumo-detalhes');
  itens.replaceChildren();
  let total = 0;
  for (const { produto, quantidade } of escolhas.values()) {
    if (quantidade === 0) continue;
    const subtotal = quantidade * Number(produto.preco);
    total += subtotal;
    itens.append(el('li', {}, el('span', { text: `${quantidade} × ${produto.nome}` }), el('span', { text: moeda(subtotal) })));
  }
  if (!itens.children.length) itens.append(el('li', { class: 'suave', text: 'Nenhum pão escolhido ainda.' }));

  const tipo = valor('tipoEntrega');
  detalhes.replaceChildren(
    el('dt', { text: 'Data' }), el('dd', { text: inputData.value ? dataPorExtenso(inputData.value) : '—' }),
    el('dt', { text: 'Horário' }), el('dd', { text: selectHorario.value || 'Qualquer horário' }),
    el('dt', { text: 'Recebimento' }), el('dd', { text: ROTULOS.tipo[tipo] }),
    el('dt', { text: 'Pagamento' }), el('dd', { text: ROTULOS.forma[valor('formaPagamento')] }));
  if (tipo === 'ENTREGA' && inputEndereco.value.trim()) {
    detalhes.append(el('dt', { text: 'Endereço' }), el('dd', { text: inputEndereco.value.trim() }));
  }
  document.getElementById('resumo-total').textContent = moeda(total);
}

function atualizarEndereco() {
  const entrega = valor('tipoEntrega') === 'ENTREGA';
  campoEndereco.hidden = !entrega;
  inputEndereco.required = entrega;
}

form.addEventListener('input', () => { atualizarEndereco(); atualizarResumo(); });
form.addEventListener('change', () => { atualizarEndereco(); atualizarResumo(); });
form.addEventListener('reset', () => {
  setTimeout(() => {
    for (const id of escolhas.keys()) alterarQuantidade(id, -999);
    inputData.value = hojeISO(1);
    limparErro(form);
    atualizarEndereco();
    atualizarResumo();
  });
});

// ---------------------------------------------------------------- envio

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  limparErro(form);
  const itens = [...escolhas.values()].filter((e) => e.quantidade > 0)
    .map((e) => ({ produtoId: e.produto.id, quantidade: e.quantidade }));
  const tipo = valor('tipoEntrega');

  if (!valor('nome').trim()) return mostrarErro(form, { message: 'Informe seu nome.', campos: { nome: 1 } });
  if (valor('telefone').replace(/\D/g, '').length < 10) {
    return mostrarErro(form, { message: 'Informe seu telefone com DDD.', campos: { telefone: 1 } });
  }
  if (itens.length === 0) return mostrarErro(form, { message: 'Escolha pelo menos um pão (use o botão +).' });
  if (!inputData.value) return mostrarErro(form, { message: 'Escolha a data desejada.', campos: { dataEntregaRetirada: 1 } });
  if (tipo === 'ENTREGA' && !inputEndereco.value.trim()) {
    return mostrarErro(form, { message: 'Para entrega, o endereço é obrigatório.', campos: { enderecoEntrega: 1 } });
  }

  const corpo = {
    nome: valor('nome').trim(),
    telefone: valor('telefone').trim(),
    dataEntregaRetirada: inputData.value,
    horarioEntregaRetirada: selectHorario.value || null,
    tipoEntrega: tipo,
    enderecoEntrega: tipo === 'ENTREGA' ? inputEndereco.value.trim() : null,
    formaPagamento: valor('formaPagamento'),
    observacoes: valor('observacoes').trim() || null,
    itens,
  };

  try {
    const pedido = await comBotaoOcupado(botaoEnviar, 'Enviando…',
      () => api('/api/publico/pedidos', { metodo: 'POST', corpo }));
    mostrarSucesso(pedido);
  } catch (erro) {
    mostrarErro(form, erro);
  }
});

function mostrarSucesso(pedido) {
  form.hidden = true;
  document.querySelector('h1').textContent = 'Obrigada pelo pedido!';
  document.querySelector('.pagina > p.suave').hidden = true;
  const sucesso = document.getElementById('sucesso');
  document.getElementById('codigo').textContent = pedido.codigo;
  document.getElementById('sucesso-detalhes').textContent =
    `${ROTULOS.tipo[pedido.tipoEntrega]} em ${dataPorExtenso(pedido.dataEntregaRetirada)}`
    + `${pedido.horarioEntregaRetirada ? ` às ${pedido.horarioEntregaRetirada.slice(0, 5)}` : ''}`
    + ` · Total ${moeda(pedido.total)} (${ROTULOS.forma[pedido.formaPagamento]}).`;
  document.getElementById('sucesso-acoes').replaceChildren(
    el('a', { class: 'botao grande', href: `/meus-pedidos.html?codigo=${encodeURIComponent(pedido.codigo)}`, text: 'Acompanhar meu pedido' }),
    el('a', { class: 'botao grande secundario', href: '/pedido.html', text: 'Fazer outro pedido' }));
  sucesso.hidden = false;
  sucesso.scrollIntoView({ behavior: 'smooth' });
}

atualizarEndereco();
carregarProdutos();
