import {
  ROTULOS, api, aviso, comBotaoOcupado, el, etiqueta, lerNumero, limparErro, mostrarErro, quantidadeBR, vazio,
} from '../comum.js';
import { confirmar, iniciarLayout } from './layout.js';

const form = document.getElementById('form-item');
const lista = document.getElementById('lista');
let editandoId = null;

form.elements.unidadeMedida.append(...Object.entries(ROTULOS.unidade).map(([v, t]) => el('option', { value: v, text: t })));

function abrirForm(i) {
  editandoId = i ? i.id : null;
  document.getElementById('titulo-form').textContent = i ? `Editar ${i.nome}` : 'Novo item';
  form.elements.nome.value = i ? i.nome : '';
  form.elements.unidadeMedida.value = i ? i.unidadeMedida : 'KG';
  form.elements.quantidadeAtual.value = i ? quantidadeBR(i.quantidadeAtual) : '';
  form.elements.quantidadeMinima.value = i ? quantidadeBR(i.quantidadeMinima) : '';
  limparErro(form);
  form.hidden = false;
  form.elements.nome.focus();
  form.scrollIntoView({ behavior: 'smooth' });
}

function fecharForm() { form.hidden = true; editandoId = null; }

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  limparErro(form);
  const atual = lerNumero(form.elements.quantidadeAtual.value);
  const minimo = lerNumero(form.elements.quantidadeMinima.value);
  if (!(atual >= 0)) return mostrarErro(form, { message: 'Informe quanto tem agora (zero ou mais).', campos: { quantidadeAtual: 1 } });
  if (!(minimo >= 0)) return mostrarErro(form, { message: 'Informe a quantidade mínima (zero ou mais).', campos: { quantidadeMinima: 1 } });
  const corpo = { nome: form.elements.nome.value, unidadeMedida: form.elements.unidadeMedida.value, quantidadeAtual: atual, quantidadeMinima: minimo };
  try {
    await comBotaoOcupado(document.getElementById('botao-salvar'), 'Salvando…', () => (editandoId
      ? api(`/api/admin/estoque/${editandoId}`, { metodo: 'PUT', corpo })
      : api('/api/admin/estoque', { metodo: 'POST', corpo })));
    aviso('Item salvo.');
    fecharForm();
    carregar();
  } catch (erro) {
    mostrarErro(form, erro);
  }
});

function linha(i) {
  const entrada = el('input', { type: 'text', inputmode: 'decimal', value: quantidadeBR(i.quantidadeAtual), 'aria-label': `Nova quantidade de ${i.nome}` });
  const atualizar = el('button', { type: 'button', class: 'botao', text: 'Atualizar' });
  atualizar.addEventListener('click', async () => {
    const n = lerNumero(entrada.value);
    if (!(n >= 0)) { aviso('Digite um número (zero ou mais).', 'erro'); return; }
    try {
      await api(`/api/admin/estoque/${i.id}/quantidade`, { metodo: 'PATCH', corpo: { novaQuantidade: n } });
      aviso(`${i.nome} atualizado.`);
      carregar();
    } catch (erro) { aviso(erro.message, 'erro'); }
  });
  return el('article', { class: `cartao item-simples${i.precisaComprar ? ' alerta' : ''}` },
    el('div', { class: 'info' },
      el('strong', { text: i.nome }), ' ',
      i.precisaComprar ? etiqueta('🛒 Precisa comprar', 'vermelho') : etiqueta('OK', 'verde'),
      el('div', { text: `Tem ${quantidadeBR(i.quantidadeAtual)} ${i.unidadeRotulo} · mínimo ${quantidadeBR(i.quantidadeMinima)} ${i.unidadeRotulo}` })),
    el('div', { class: 'acoes' },
      el('label', { class: 'sr-only', text: 'Nova quantidade' }), entrada, atualizar,
      el('button', { type: 'button', class: 'botao secundario', text: '✎ Editar', onclick: () => abrirForm(i) }),
      el('button', { type: 'button', class: 'botao perigo', text: '🗑', 'aria-label': `Excluir ${i.nome}`,
        onclick: async () => {
          if (!confirmar(`Excluir ${i.nome} do estoque?`)) return;
          try { await api(`/api/admin/estoque/${i.id}`, { metodo: 'DELETE' }); aviso('Item excluído.'); carregar(); } catch (erro) { aviso(erro.message, 'erro'); }
        } })));
}

async function carregar() {
  try {
    const itens = await api('/api/admin/estoque');
    const ordenados = [...itens].sort((a, b) => Number(b.precisaComprar) - Number(a.precisaComprar));
    lista.replaceChildren(...(ordenados.length ? ordenados.map(linha) : [vazio('Nenhum item cadastrado.')]));
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
