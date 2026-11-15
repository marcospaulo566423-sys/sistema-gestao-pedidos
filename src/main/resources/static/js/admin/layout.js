// Cabeçalho e menu do painel, montados em todas as telas administrativas.
import { ROTULOS, api, aviso, dataPorExtenso, el, etiqueta, hora, moeda, mostrarAvisoPendente, telefoneBR } from '../comum.js';

const ICONES = {
  inicio: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V20h14V9.5"/><path d="M9 20v-6h6v6"/></svg>',
  pedidos: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>',
  novo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/></svg>',
  producao: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15c0-4.5 3.6-8 8-8s8 3.5 8 8v2H4v-2Z"/><path d="M9 7.5v3M12 7v3.5M15 7.5v3"/><path d="M3 17h18"/></svg>',
  clientes: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3.5"/><path d="M3 19c0-3.3 2.7-6 6-6s6 2.7 6 6"/><circle cx="17" cy="9" r="2.5"/><path d="M16.5 13.5c2.5.4 4.5 2.6 4.5 5.5"/></svg>',
  produtos: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41 13.42 20.58a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82Z"/><circle cx="7" cy="7" r="1.5"/></svg>',
  estoque: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21 8-9-5-9 5v8l9 5 9-5V8Z"/><path d="m3.3 8 8.7 5 8.7-5M12 13v8"/></svg>',
  financeiro: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><circle cx="12" cy="12" r="3"/><path d="M6 12h.01M18 12h.01"/></svg>',
  relatorios: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="M7 16v-5M12 16V8M17 16v-8"/></svg>',
};

const MENU = [
  ['inicio', 'Início', '/admin/index.html'],
  ['pedidos', 'Pedidos', '/admin/pedidos.html'],
  ['novo', 'Novo pedido', '/admin/pedido-form.html'],
  ['producao', 'Produção', '/admin/producao.html'],
  ['clientes', 'Clientes', '/admin/clientes.html'],
  ['produtos', 'Produtos', '/admin/produtos.html'],
  ['estoque', 'Estoque', '/admin/estoque.html'],
  ['financeiro', 'Financeiro', '/admin/financeiro.html'],
  ['relatorios', 'Relatórios', '/admin/relatorios.html'],
];

function criarIcone(id) {
  const span = el('span', { class: 'icone', 'aria-hidden': 'true' });
  span.innerHTML = ICONES[id] || '';
  return span;
}

export async function iniciarLayout() {
  const pagina = document.body.dataset.pagina;
  const saudacao = el('span', { text: '' });
  const sair = el('button', { type: 'button', class: 'botao pequeno', text: 'Sair' });
  sair.addEventListener('click', async () => {
    try { await api('/api/auth/logout', { metodo: 'POST' }); } finally { location.href = '/entrar.html'; }
  });

  document.getElementById('topo').replaceChildren(
    el('div', { class: 'topo-admin' },
      el('div', { class: 'faixa' },
        el('a', { class: 'marca', href: '/admin/index.html' }, el('img', { src: '/img/logo.svg', alt: '' }), 'Painel da Elaine'),
        el('div', { class: 'usuario' }, saudacao, sair))),
    el('nav', { class: 'menu-admin', 'aria-label': 'Menu do painel' },
      el('ul', {}, MENU.map(([id, nome, href]) => el('li', {},
        el('a', { href, 'aria-current': id === pagina ? 'page' : null },
          criarIcone(id), nome))))));

  mostrarAvisoPendente();
  const sessao = await api('/api/admin/sessao');
  saudacao.textContent = `Olá, ${sessao.nome.split(' ')[0]}!`;
  return sessao;
}

/** Cartão de pedido reutilizado nas telas (lista, painel, produção). */
export function cartaoPedido(p, acoes = []) {
  return el('article', { class: `cartao pedido ${p.status}` },
    el('div', { class: 'linha-topo' },
      el('span', { class: 'hora', text: p.horarioEntregaRetirada ? hora(p.horarioEntregaRetirada) : 'Sem horário' }),
      el('span', { class: 'cliente', text: p.cliente.nome }),
      etiqueta(ROTULOS.status[p.status], p.status),
      etiqueta(ROTULOS.tipo[p.tipoEntrega], p.tipoEntrega),
      p.pagamento ? etiqueta(`Pago · ${ROTULOS.forma[p.pagamento.tipo]}`, 'verde') : null,
      el('span', { class: 'codigo', text: `#${p.codigo}` })),
    el('div', { class: 'suave' },
      `${dataPorExtenso(p.dataEntregaRetirada)} · ${telefoneBR(p.cliente.telefone)} · veio por ${ROTULOS.origem[p.origem]}`),
    p.enderecoEntrega ? el('div', {}, el('strong', { text: 'Entregar em: ' }), p.enderecoEntrega) : null,
    el('ul', {}, p.itens.map((i) => el('li', { text: `${i.quantidade} × ${i.produtoNome}` }))),
    p.observacoes ? el('div', { class: 'obs', text: `Obs.: ${p.observacoes}` }) : null,
    el('div', { class: 'rodape' },
      el('span', { class: 'preco', text: `Total ${moeda(p.total)}${p.formaPagamento && !p.pagamento ? ` · ${ROTULOS.forma[p.formaPagamento]}` : ''}` }),
      acoes.length ? el('div', { class: 'acoes' }, acoes) : null));
}

/** Pergunta simples de confirmação (botões grandes do navegador). */
export function confirmar(mensagem) { return window.confirm(mensagem); }

export { aviso };