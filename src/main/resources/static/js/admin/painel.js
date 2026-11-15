import { api, dataPorExtenso, el, hojeISO, quantidadeBR, vazio } from '../comum.js';
import { cartaoPedido, iniciarLayout } from './layout.js';

function saudacaoDoHorario() {
  const h = new Date().getHours();
  if (h < 12) return 'Bom dia';
  if (h < 18) return 'Boa tarde';
  return 'Boa noite';
}

function numero(valor, texto, href, alerta) {
  return el('a', { class: `cartao numero-grande${alerta ? ' alerta' : ''}`, href },
    el('span', { class: 'valor', text: String(valor) }), el('span', { text: texto }));
}

async function iniciar() {
  const sessao = await iniciarLayout();
  document.getElementById('titulo').textContent =
    `${saudacaoDoHorario()}, ${sessao.nome.split(' ')[0]}! · ${dataPorExtenso(hojeISO())}`;

  const painel = await api('/api/admin/painel');

  document.getElementById('numeros').replaceChildren(
    numero(painel.pendentesHoje, 'pedidos pendentes para hoje', `/admin/pedidos.html?data=${hojeISO()}`),
    numero(painel.lembretesAmanha.pedidos.length, 'pedidos para amanhã', `/admin/pedidos.html?data=${hojeISO(1)}`),
    numero(painel.pendentesTotal, 'pedidos pendentes no total', '/admin/pedidos.html'),
    numero(painel.itensParaComprar.length, 'itens para comprar', '/admin/estoque.html', painel.itensParaComprar.length > 0));

  const lembretes = document.getElementById('lembretes');
  lembretes.replaceChildren(...(painel.lembretesAmanha.pedidos.length
    ? painel.lembretesAmanha.pedidos.map((p) => cartaoPedido(p))
    : [vazio('Nenhum pedido pendente para amanhã.')]));

  const producao = document.getElementById('producao');
  producao.replaceChildren(painel.producaoHoje.length
    ? el('table', { class: 'producao-tabela' },
      el('tbody', {}, painel.producaoHoje.map((i) => el('tr', {},
        el('td', { text: i.produtoNome }), el('td', { class: 'numero', text: String(i.quantidade) })))))
    : vazio('Nada para produzir hoje.'));

  const comprar = document.getElementById('comprar');
  comprar.replaceChildren(painel.itensParaComprar.length
    ? el('ul', {}, painel.itensParaComprar.map((i) => el('li', {},
      el('strong', { text: i.nome }), ` — tem ${quantidadeBR(i.quantidadeAtual)} ${i.unidadeRotulo} (mínimo ${quantidadeBR(i.quantidadeMinima)})`)))
    : vazio('Tudo certo no estoque.'));
}

iniciar();
