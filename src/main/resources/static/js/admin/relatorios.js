import { ROTULOS, api, dataBR, el, hojeISO, hora, moeda, vazio } from '../comum.js';
import { iniciarLayout } from './layout.js';

const area = document.getElementById('relatorio');
const titulo = document.getElementById('titulo-relatorio');
const referencia = document.getElementById('referencia');
const estado = { aba: 'pedidos', periodo: 'MES' };

function textoPeriodo(r) {
  if (r.periodo === 'TODOS') return 'todo o período';
  if (r.inicio === r.fim) return dataBR(r.inicio);
  return `${dataBR(r.inicio)} a ${dataBR(r.fim)}`;
}

function numero(valor, texto) {
  return el('div', { class: 'cartao numero-grande' }, el('span', { class: 'valor', text: String(valor) }), el('span', { text: texto }));
}

function tabela(cabecalhos, linhas) {
  return el('div', { class: 'cartao' }, el('table', {},
    el('thead', {}, el('tr', {}, cabecalhos.map(([t, num]) => el('th', { class: num ? 'numero' : null, text: t })))),
    el('tbody', {}, linhas)));
}

/** RF12 / RF19 */
function relatorioPedidos(r) {
  titulo.textContent = `Pedidos · ${textoPeriodo(r)}`;
  const qs = r.quantidadePorStatus;
  area.replaceChildren(
    el('div', { class: 'numeros' },
      numero(qs.PENDENTE, 'pendentes'), numero(qs.CONCLUIDO, 'concluídos'), numero(qs.CANCELADO, 'cancelados'),
      numero(r.retiradas, 'para retirada'), numero(r.entregas, 'para entrega'),
      numero(moeda(r.valorTotal), 'em pedidos (sem cancelados)')),
    r.pedidos.length
      ? tabela([['Dia'], ['Cliente'], ['Produtos'], ['Recebimento'], ['Situação'], ['Total', true]],
        r.pedidos.map((p) => el('tr', {},
          el('td', { text: `${dataBR(p.dataEntregaRetirada)} ${hora(p.horarioEntregaRetirada)}` }),
          el('td', { text: p.cliente.nome }),
          el('td', { text: p.itens.map((i) => `${i.quantidade}× ${i.produtoNome}`).join(', ') }),
          el('td', { text: ROTULOS.tipo[p.tipoEntrega] }),
          el('td', { text: ROTULOS.status[p.status] }),
          el('td', { class: 'numero', text: moeda(p.total) }))))
      : vazio('Nenhum pedido no período.'));
}

/** RF10 */
function relatorioMaisVendidos(r) {
  titulo.textContent = `Mais vendidos · ${textoPeriodo(r)}`;
  const maior = r.produtos.length ? Number(r.produtos[0].quantidade) : 1;
  area.replaceChildren(r.produtos.length
    ? tabela([['#'], ['Produto'], ['Quantidade', true], [''], ['Valor', true]],
      r.produtos.map((p, i) => el('tr', {},
        el('td', { text: `${i + 1}º` }),
        el('td', { text: p.produtoNome }),
        el('td', { class: 'numero', text: String(p.quantidade) }),
        el('td', {}, el('progress', { max: maior, value: p.quantidade, 'aria-label': `${p.quantidade} unidades` })),
        el('td', { class: 'numero', text: moeda(p.valorTotal) }))))
    : vazio('Nenhuma venda no período.'));
}

/** RF20 */
function relatorioFinanceiro(r) {
  titulo.textContent = `Financeiro · ${textoPeriodo(r)}`;
  area.replaceChildren(
    el('div', { class: 'numeros' },
      numero(moeda(r.total), 'total de entradas'),
      ...Object.entries(r.totalPorForma).map(([f, v]) => numero(moeda(v), f === 'AVULSA' ? 'avulsas' : ROTULOS.forma[f]))),
    r.registros.length
      ? tabela([['Data'], ['Descrição'], ['Origem'], ['Valor', true]],
        r.registros.map((reg) => el('tr', {},
          el('td', { text: dataBR(reg.data) }),
          el('td', { text: reg.descricao }),
          el('td', { text: reg.automatico ? `Pedido (${ROTULOS.forma[reg.formaPagamento]})` : 'Avulsa' }),
          el('td', { class: 'numero', text: moeda(reg.valor) }))))
      : vazio('Nenhuma entrada no período.'),
    el('p', { class: 'suave', text: 'O sistema registra apenas entradas (despesas e lucro não são calculados).' }));
}

const RELATORIOS = {
  pedidos: ['/api/admin/relatorios/pedidos', relatorioPedidos],
  'mais-vendidos': ['/api/admin/relatorios/mais-vendidos', relatorioMaisVendidos],
  financeiro: ['/api/admin/relatorios/financeiro', relatorioFinanceiro],
};

async function carregar() {
  const [url, desenhar] = RELATORIOS[estado.aba];
  area.replaceChildren(el('p', { class: 'carregando', text: 'Carregando…' }));
  try {
    desenhar(await api(`${url}?periodo=${estado.periodo}&data=${referencia.value || hojeISO()}`));
  } catch (erro) {
    area.replaceChildren(vazio(erro.message));
  }
}

function segmentado(id, chave) {
  const grupo = document.getElementById(id);
  grupo.addEventListener('click', (e) => {
    const b = e.target.closest('button');
    if (!b) return;
    grupo.querySelectorAll('button').forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
    estado[chave] = b.dataset.valor;
    carregar();
  });
}

async function iniciar() {
  await iniciarLayout();
  referencia.value = hojeISO();
  segmentado('abas', 'aba');
  segmentado('filtro-periodo', 'periodo');
  referencia.addEventListener('change', carregar);
  document.getElementById('botao-imprimir').addEventListener('click', () => window.print());
  await carregar();
}

iniciar();
