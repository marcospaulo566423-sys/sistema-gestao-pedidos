import { api, aviso, comBotaoOcupado, el, etiqueta, limparErro, mostrarErro, telefoneBR, vazio } from '../comum.js';
import { confirmar, iniciarLayout } from './layout.js';

const form = document.getElementById('form-cliente');
const lista = document.getElementById('lista');
const busca = document.getElementById('busca');
let editandoId = null;

function abrirForm(cliente) {
  editandoId = cliente ? cliente.id : null;
  document.getElementById('titulo-form').textContent = cliente ? `Editar ${cliente.nome}` : 'Novo cliente';
  form.elements.nome.value = cliente ? cliente.nome : '';
  form.elements.telefone.value = cliente ? telefoneBR(cliente.telefone) : '';
  form.elements.endereco.value = cliente?.endereco || '';
  form.elements.clienteFrequente.checked = cliente ? cliente.clienteFrequente : false;
  limparErro(form);
  form.hidden = false;
  form.elements.nome.focus();
  form.scrollIntoView({ behavior: 'smooth' });
}

function fecharForm() { form.hidden = true; editandoId = null; }

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  limparErro(form);
  const corpo = {
    nome: form.elements.nome.value,
    telefone: form.elements.telefone.value,
    endereco: form.elements.endereco.value || null,
    clienteFrequente: form.elements.clienteFrequente.checked,
  };
  try {
    await comBotaoOcupado(document.getElementById('botao-salvar'), 'Salvando…', () => (editandoId
      ? api(`/api/admin/clientes/${editandoId}`, { metodo: 'PUT', corpo })
      : api('/api/admin/clientes', { metodo: 'POST', corpo })));
    aviso('Cliente salvo.');
    fecharForm();
    carregar();
  } catch (erro) {
    mostrarErro(form, erro);
  }
});

async function excluir(c) {
  if (!confirmar(`Excluir o cliente ${c.nome}?`)) return;
  try {
    await api(`/api/admin/clientes/${c.id}`, { metodo: 'DELETE' });
    aviso('Cliente excluído.');
    carregar();
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
}

let temporizador;
busca.addEventListener('input', () => { clearTimeout(temporizador); temporizador = setTimeout(carregar, 300); });

async function carregar() {
  try {
    const termo = busca.value.trim();
    const clientes = await api(`/api/admin/clientes${termo ? `?busca=${encodeURIComponent(termo)}` : ''}`);
    lista.replaceChildren(...(clientes.length ? clientes.map((c) => el('article', { class: 'cartao item-simples' },
      el('div', { class: 'info' },
        el('strong', { text: c.nome }), ' ',
        c.clienteFrequente ? etiqueta('★ Frequente') : null,
        el('div', { text: telefoneBR(c.telefone) }),
        c.endereco ? el('div', { class: 'suave', text: c.endereco }) : null,
        el('div', { class: 'suave', text: `${c.quantidadePedidos} pedido(s)` })),
      el('div', { class: 'acoes' },
        el('a', { class: 'botao', href: `/admin/pedido-form.html?cliente=${c.id}`, text: '➕ Novo pedido' }),
        el('button', { type: 'button', class: 'botao secundario', text: '✎ Editar', onclick: () => abrirForm(c) }),
        c.quantidadePedidos === 0
          ? el('button', { type: 'button', class: 'botao perigo', text: '🗑 Excluir', onclick: () => excluir(c) })
          : null)))
      : [vazio('Nenhum cliente encontrado.')]));
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
