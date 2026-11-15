import { api, el, moeda, vazio } from '../comum.js';
import { fotoDoProduto, protegerFoto, protegerFotosDaPagina } from './fotos.js';

const area = document.getElementById('produtos');

async function carregar() {
  try {
    const produtos = await api('/api/publico/produtos');
    area.replaceChildren();
    if (produtos.length === 0) {
      area.append(vazio('O cardápio está sendo atualizado. Volte em breve!'));
      return;
    }
    for (const p of produtos) {
      const link = `/pedido.html?produto=${encodeURIComponent(p.id)}`;
      area.append(el('article', { class: 'produto' },
        el('a', { class: 'foto', href: link, tabindex: '-1', 'aria-hidden': 'true' },
          protegerFoto(el('img', { src: fotoDoProduto(p, 800), alt: '', loading: 'lazy', decoding: 'async', width: 800, height: 600 }))),
        el('div', { class: 'corpo' },
          el('h3', { text: p.nome }),
          p.descricao ? el('p', { class: 'descricao', text: p.descricao }) : null,
          el('div', { class: 'rodape-produto' },
            el('span', { class: 'preco', text: moeda(p.preco) }),
            el('a', { class: 'botao', href: link, 'aria-label': `Pedir ${p.nome}`, text: 'Pedir' })))));
    }
  } catch (erro) {
    area.replaceChildren(vazio(erro.message));
  }
}

protegerFotosDaPagina();
carregar();
