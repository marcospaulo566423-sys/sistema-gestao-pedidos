import {
  ROTULOS, api, aviso, comBotaoOcupado, dataBR, el, etiqueta, hojeISO, lerNumero, limparErro, moeda, mostrarErro, vazio,
} from '../comum.js';
import { confirmar, iniciarLayout } from './layout.js';

const form = document.getElementById('form-entrada');
const lista = document.getElementById('lista');
const referencia = document.getElementById('referencia');
let periodo = 'MES';
let editando = null;

export function textoPeriodo(r) {
  if (r.periodo === 'TODOS') return 'todo o período';
  if (r.inicio === r.fim) return dataBR(r.inicio);
  return `${dataBR(r.inicio)} a ${dataBR(r.fim)}`;
}

function abrirForm(r) {
  editando = r;
  document.getElementById('titulo-form').textContent = r ? 'Editar entrada' : 'Nova entrada avulsa';
  form.elements.valor.value = r ? String(r.valor).replace('.', ',') : '';
  form.elements.valor.disabled = Boolean(r && r.automatico);
  form.elements.data.value = r ? r.data : hojeISO();
  form.elements.data.max = hojeISO();
  form.elements.descricao.value = r ? r.descricao : '';
  document.getElementById('aviso-automatica').hidden = !(r && r.automatico);
  limparErro(form);
  form.hidden = false;
  form.elements.valor.focus();
  form.scrollIntoView({ behavior: 'smooth' });
}

function fecharForm() { form.hidden = true; editando = null; }

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  limparErro(form);
  const valor = editando && editando.automatico ? editando.valor : lerNumero(form.elements.valor.value);
  if (!(valor > 0)) return mostrarErro(form, { message: 'Informe um valor maior que zero. Ex.: 35,00', campos: { valor: 1 } });
  const corpo = { valor, data: form.elements.data.value, descricao: form.elements.descricao.value };
  try {
    await comBotaoOcupado(document.getElementById('botao-salvar'), 'Salvando…', () => (editando
      ? api(`/api/admin/financeiro/${editando.id}`, { metodo: 'PUT', corpo })
      : api('/api/admin/financeiro', { metodo: 'POST', corpo })));
    aviso('Entrada salva.');
    fecharForm();
    carregar();
  } catch (erro) {
    mostrarErro(form, erro);
  }
});

function cartaoTotal(valor, texto) {
  return el('div', { class: 'cartao numero-grande' }, el('span', { class: 'valor', text: moeda(valor) }), el('span', { text: texto }));
}

async function carregar() {
  try {
    const r = await api(`/api/admin/financeiro?periodo=${periodo}&data=${referencia.value || hojeISO()}`);
    document.getElementById('totais').replaceChildren(
      cartaoTotal(r.total, `Total de entradas · ${textoPeriodo(r)}`),
      ...Object.entries(r.totalPorForma).map(([forma, v]) => cartaoTotal(v, forma === 'AVULSA' ? 'Avulsas' : ROTULOS.forma[forma])));

    lista.replaceChildren(...(r.registros.length ? r.registros.map((reg) => el('article', { class: 'cartao item-simples' },
      el('div', { class: 'info' },
        el('strong', { text: moeda(reg.valor) }), ' ',
        reg.automatico ? etiqueta(`Pedido · ${ROTULOS.forma[reg.formaPagamento]}`, 'verde') : etiqueta('Avulsa'),
        el('div', { text: reg.descricao }),
        el('div', { class: 'suave', text: dataBR(reg.data) })),
      el('div', { class: 'acoes' },
        el('button', { type: 'button', class: 'botao secundario', text: '✎ Editar', onclick: () => abrirForm(reg) }),
        reg.automatico ? null : el('button', { type: 'button', class: 'botao perigo', text: '🗑 Excluir',
          onclick: async () => {
            if (!confirmar(`Excluir a entrada de ${moeda(reg.valor)}?`)) return;
            try { await api(`/api/admin/financeiro/${reg.id}`, { metodo: 'DELETE' }); aviso('Entrada excluída.'); carregar(); } catch (erro) { aviso(erro.message, 'erro'); }
          } }))))
      : [vazio('Nenhuma entrada neste período.')]));
  } catch (erro) {
    lista.replaceChildren(vazio(erro.message));
  }
}

async function iniciar() {
  await iniciarLayout();
  referencia.value = hojeISO();
  const grupo = document.getElementById('filtro-periodo');
  grupo.addEventListener('click', (e) => {
    const b = e.target.closest('button');
    if (!b) return;
    grupo.querySelectorAll('button').forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
    periodo = b.dataset.valor;
    carregar();
  });
  referencia.addEventListener('change', carregar);
  document.getElementById('botao-nova').addEventListener('click', () => abrirForm(null));
  document.getElementById('botao-cancelar').addEventListener('click', fecharForm);
  await carregar();
}

iniciar();
