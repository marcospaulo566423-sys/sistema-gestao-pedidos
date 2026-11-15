import { api, dataPorExtenso, el, hojeISO, vazio } from '../comum.js';
import { cartaoPedido, iniciarLayout } from './layout.js';

const inputData = document.getElementById('data');

async function carregar() {
  const dia = inputData.value || hojeISO();
  history.replaceState(null, '', `?data=${dia}`);
  document.getElementById('titulo').textContent = `Produção · ${dataPorExtenso(dia)}`;
  const quantidades = document.getElementById('quantidades');
  const pedidos = document.getElementById('pedidos');
  try {
    const r = await api(`/api/admin/producao?data=${dia}`);
    quantidades.replaceChildren(r.itens.length
      ? el('table', { class: 'producao-tabela' },
        el('thead', {}, el('tr', {}, el('th', { text: 'Produto' }), el('th', { class: 'numero', text: 'Quantidade' }))),
        el('tbody', {}, r.itens.map((i) => el('tr', {},
          el('td', { text: i.produtoNome }), el('td', { class: 'numero', text: String(i.quantidade) })))),
        el('tfoot', {}, el('tr', {}, el('th', { text: 'Total de unidades' }), el('th', { class: 'numero', text: String(r.totalUnidades) }))))
      : vazio('Nenhum pedido para este dia.'));
    pedidos.replaceChildren(...(r.pedidos.length ? r.pedidos.map((p) => cartaoPedido(p)) : [vazio('Sem pedidos.')]));
  } catch (erro) {
    quantidades.replaceChildren(vazio(erro.message));
  }
}

async function iniciar() {
  await iniciarLayout();
  inputData.value = new URLSearchParams(location.search).get('data') || hojeISO();
  inputData.addEventListener('change', carregar);
  document.getElementById('botao-hoje').addEventListener('click', () => { inputData.value = hojeISO(); carregar(); });
  document.getElementById('botao-amanha').addEventListener('click', () => { inputData.value = hojeISO(1); carregar(); });
  document.getElementById('botao-imprimir').addEventListener('click', () => window.print());
  await carregar();
}

iniciar();
