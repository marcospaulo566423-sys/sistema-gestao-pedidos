import { api, aviso, comBotaoOcupado, el, etiqueta, lerNumero, limparErro, moeda, mostrarErro, vazio } from '../comum.js';
import { confirmar, iniciarLayout } from './layout.js';

const form = document.getElementById('form-produto');
const lista = document.getElementById('lista');
const arquivo = document.getElementById('arquivo-foto');
let editandoId = null;
let fotoDoProduto = null;

function corpoDe(p, mudancas = {}) {
  return { nome: p.nome, preco: p.preco, descricao: p.descricao, ativo: p.ativo, ...mudancas };
}

function abrirForm(p) {
  editandoId = p ? p.id : null;
  document.getElementById('titulo-form').textContent = p ? `Editar ${p.nome}` : 'Novo produto';
  form.elements.nome.value = p ? p.nome : '';
  form.elements.preco.value = p ? String(p.preco).replace('.', ',') : '';
  form.elements.descricao.value = p?.descricao || '';
  form.elements.ativo.checked = p ? p.ativo : true;
  limparErro(form);
  form.hidden = false;
  form.elements.nome.focus();
  form.scrollIntoView({ behavior: 'smooth' });
}

function fecharForm() { form.hidden = true; editandoId = null; }

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  limparErro(form);
  const preco = lerNumero(form.elements.preco.value);
  if (!(preco > 0)) return mostrarErro(form, { message: 'Informe um preço maior que zero. Ex.: 12,50', campos: { preco: 1 } });
  const corpo = {
    nome: form.elements.nome.value,
    preco,
    descricao: form.elements.descricao.value || null,
    ativo: form.elements.ativo.checked,
  };
  try {
    await comBotaoOcupado(document.getElementById('botao-salvar'), 'Salvando…', () => (editandoId
      ? api(`/api/admin/produtos/${editandoId}`, { metodo: 'PUT', corpo })
      : api('/api/admin/produtos', { metodo: 'POST', corpo })));
    aviso('Produto salvo.');
    fecharForm();
    carregar();
  } catch (erro) {
    mostrarErro(form, erro);
  }
});

async function acao(promessa, mensagem) {
  try {
    await promessa();
    aviso(mensagem);
    carregar();
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

arquivo.addEventListener('change', () => {
  const f = arquivo.files[0];
  if (!f || !fotoDoProduto) return;
  if (f.size > 3 * 1024 * 1024) { aviso('A foto deve ter no máximo 3 MB.', 'erro'); arquivo.value = ''; return; }
  const dados = new FormData();
  dados.append('foto', f);
  const id = fotoDoProduto;
  fotoDoProduto = null;
  arquivo.value = '';
  acao(() => api(`/api/admin/produtos/${id}/foto`, { metodo: 'POST', formulario: dados }), 'Foto atualizada.');
});

function linha(p) {
  return el('article', { class: 'cartao item-simples' },
    el('img', { src: p.fotoUrl || '/img/pao.svg', alt: '', width: 72, height: 72 }),
    el('div', { class: 'info' },
      el('strong', { text: p.nome }), ' ',
      p.ativo ? etiqueta('No cardápio', 'verde') : etiqueta('Fora do cardápio', 'vermelho'),
      el('div', { class: 'preco', text: moeda(p.preco) }),
      p.descricao ? el('div', { class: 'suave', text: p.descricao }) : null),
    el('div', { class: 'acoes' },
      el('button', { type: 'button', class: 'botao secundario', text: '✎ Editar', onclick: () => abrirForm(p) }),
      el('button', { type: 'button', class: 'botao secundario', text: '📷 Foto',
        onclick: () => { fotoDoProduto = p.id; arquivo.click(); } }),
      el('button', { type: 'button', class: 'botao secundario', text: p.ativo ? 'Tirar do cardápio' : 'Voltar ao cardápio',
        onclick: () => acao(() => api(`/api/admin/produtos/${p.id}`, { metodo: 'PUT', corpo: corpoDe(p, { ativo: !p.ativo }) }),
          p.ativo ? 'Produto escondido do site.' : 'Produto voltou ao cardápio.') }),
      el('button', { type: 'button', class: 'botao perigo', text: '🗑 Excluir',
        onclick: () => confirmar(`Excluir ${p.nome}?`)
          && acao(() => api(`/api/admin/produtos/${p.id}`, { metodo: 'DELETE' }), 'Produto excluído.') })));
}

async function carregar() {
  try {
    const produtos = await api('/api/admin/produtos');
    lista.replaceChildren(...(produtos.length ? produtos.map(linha) : [vazio('Nenhum produto cadastrado. Clique em “+ Novo produto”.')]));
  } catch (erro) {
    lista.replaceChildren(vazio(erro.message));
  }
}

async function iniciar() {
  await iniciarLayout();
  document.getElementById('botao-novo').addEventListener('click', () => abrirForm(null));
  document.getElementById('botao-cancelar').addEventListener('click', fecharForm);
  await carregar();
}

iniciar();
