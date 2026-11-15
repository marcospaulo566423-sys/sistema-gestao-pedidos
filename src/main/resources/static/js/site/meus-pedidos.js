import {
  ROTULOS, api, comBotaoOcupado, dataBR, dataPorExtenso, el, etiqueta, limparErro, moeda, mostrarErro,
} from '../comum.js';

const form = document.getElementById('form-consulta');
const resultado = document.getElementById('resultado');
const botao = document.getElementById('botao-consultar');

const MENSAGEM_STATUS = {
  PENDENTE: 'Recebido! A Elaine vai preparar seu pedido para a data combinada.',
  CONCLUIDO: 'Pedido concluído. Obrigada pela preferência!',
  CANCELADO: 'Este pedido foi cancelado. Em caso de dúvida, fale com a Elaine.',
};

const codigoDaUrl = new URLSearchParams(location.search).get('codigo');
if (codigoDaUrl) {
  form.elements.codigo.value = codigoDaUrl;
  form.elements.telefone.focus();
}

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  limparErro(form);
  resultado.replaceChildren();
  const codigo = form.elements.codigo.value.trim();
  const telefone = form.elements.telefone.value.trim();
  if (!codigo || !telefone) {
    return mostrarErro(form, { message: 'Preencha o código e o telefone.' });
  }
  try {
    const p = await comBotaoOcupado(botao, 'Procurando…',
      () => api('/api/publico/pedidos/consulta', { metodo: 'POST', corpo: { codigo, telefone } }));
    mostrar(p);
  } catch (erro) {
    mostrarErro(form, erro);
  }
});

function mostrar(p) {
  const itens = el('ul', {}, p.itens.map((i) =>
    el('li', {}, el('span', { text: `${i.quantidade} × ${i.produtoNome}` }), el('span', { text: moeda(i.subtotal) }))));

  const detalhes = el('dl', {},
    el('dt', { text: 'Feito em' }), el('dd', { text: dataBR(p.dataPedido) }),
    el('dt', { text: ROTULOS.tipo[p.tipoEntrega] }),
    el('dd', { text: dataPorExtenso(p.dataEntregaRetirada) + (p.horarioEntregaRetirada ? ` às ${p.horarioEntregaRetirada.slice(0, 5)}` : '') }),
    p.enderecoEntrega ? [el('dt', { text: 'Endereço' }), el('dd', { text: p.enderecoEntrega })] : null,
    el('dt', { text: 'Pagamento' }),
    el('dd', { text: `${p.formaPagamento ? ROTULOS.forma[p.formaPagamento] : 'A combinar'} · ${p.pagamentoRegistrado ? 'pago ✔' : 'aguardando'}` }));

  resultado.replaceChildren(el('article', { class: 'cartao resumo consulta' },
    el('h2', { text: `Pedido ${p.codigo}` }),
    el('div', { class: 'situacao' }, etiqueta(ROTULOS.status[p.status], p.status), el('span', { text: MENSAGEM_STATUS[p.status] })),
    el('p', { text: `Olá, ${p.nomeCliente}!` }),
    itens,
    detalhes,
    el('div', { class: 'total-linha' }, el('span', { text: 'Total' }), el('span', { class: 'valor', text: moeda(p.total) }))));
  resultado.scrollIntoView({ behavior: 'smooth' });
}
