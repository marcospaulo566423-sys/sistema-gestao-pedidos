import {
  api, aviso, avisoNaProximaPagina, comBotaoOcupado, el, hojeISO, limparErro, moeda, mostrarErro, telefoneBR,
} from '../comum.js';
import { iniciarLayout } from './layout.js';

const form = document.getElementById('form-pedido');
const selectCliente = document.getElementById('cliente');
const buscaCliente = document.getElementById('busca-cliente');
const areaItens = document.getElementById('itens');
const campoEndereco = document.getElementById('campo-endereco');
const inputEndereco = document.getElementById('endereco');
const params = new URLSearchParams(location.search);
const idPedido = params.get('id');

let clientes = [];
let produtos = [];

const marcado = (nome) => form.querySelector(`input[name="${nome}"]:checked`)?.value ?? '';
const marcar = (nome, valor) => form.querySelectorAll(`input[name="${nome}"]`).forEach((r) => { r.checked = r.value === (valor ?? ''); });

// ---------------------------------------------------------------- clientes

function preencherClientes(selecionado) {
  const termo = buscaCliente.value.trim().toLowerCase();
  const digitos = termo.replace(/\D/g, '');
  const filtrados = clientes.filter((c) => !termo
    || c.nome.toLowerCase().includes(termo)
    || (digitos && c.telefone.includes(digitos))
    || String(c.id) === String(selecionado));
  selectCliente.replaceChildren(el('option', { value: '', text: filtrados.length ? 'Escolha o cliente…' : 'Nenhum cliente encontrado' }),
    ...filtrados.map((c) => el('option', { value: c.id, text: `${c.clienteFrequente ? '★ ' : ''}${c.nome} — ${telefoneBR(c.telefone)}` })));
  if (selecionado) selectCliente.value = String(selecionado);
}

function clienteSelecionado() { return clientes.find((c) => String(c.id) === selectCliente.value); }

buscaCliente.addEventListener('input', () => preencherClientes(selectCliente.value));
selectCliente.addEventListener('change', () => {
  const c = clienteSelecionado();
  if (c && c.endereco && !inputEndereco.value) inputEndereco.value = c.endereco;
});

const caixaNovoCliente = document.getElementById('novo-cliente');
document.getElementById('botao-novo-cliente').addEventListener('click', () => {
  caixaNovoCliente.hidden = false;
  document.getElementById('nc-nome').focus();
});
document.getElementById('botao-cancelar-cliente').addEventListener('click', () => { caixaNovoCliente.hidden = true; });
document.getElementById('botao-salvar-cliente').addEventListener('click', async (e) => {
  try {
    const novo = await comBotaoOcupado(e.currentTarget, 'Salvando…', () => api('/api/admin/clientes', {
      metodo: 'POST',
      corpo: {
        nome: document.getElementById('nc-nome').value,
        telefone: document.getElementById('nc-telefone').value,
        endereco: document.getElementById('nc-endereco').value || null,
        clienteFrequente: document.getElementById('nc-frequente').checked,
      },
    }));
    clientes.push(novo);
    buscaCliente.value = '';
    preencherClientes(novo.id);
    if (novo.endereco) inputEndereco.value = novo.endereco;
    caixaNovoCliente.hidden = true;
    caixaNovoCliente.querySelectorAll('input').forEach((i) => { if (i.type === 'checkbox') i.checked = false; else i.value = ''; });
    aviso(`Cliente ${novo.nome} cadastrado.`);
  } catch (erro) {
    aviso(erro.message, 'erro');
  }
});

// ---------------------------------------------------------------- itens

function adicionarItem(produtoId = '', quantidade = 1) {
  const select = el('select', { 'aria-label': 'Produto', required: true },
    el('option', { value: '', text: 'Escolha o produto…' }),
    produtos.filter((p) => p.ativo || String(p.id) === String(produtoId))
      .map((p) => el('option', { value: p.id, text: `${p.nome} — ${moeda(p.preco)}${p.ativo ? '' : ' (fora do cardápio)'}` })));
  select.value = String(produtoId);
  const qtd = el('input', { type: 'number', min: 1, max: 999, value: quantidade, 'aria-label': 'Quantidade', inputmode: 'numeric' });
  const subtotal = el('span', { class: 'subtotal' });
  const linha = el('div', { class: 'linha-item' }, select, qtd, subtotal,
    el('button', { type: 'button', class: 'botao perigo pequeno', 'aria-label': 'Remover produto', text: '✖',
      onclick: () => { linha.remove(); atualizarTotal(); } }));
  linha._ler = () => ({ produtoId: select.value ? Number(select.value) : null, quantidade: Number(qtd.value) });
  linha._subtotal = subtotal;
  areaItens.append(linha);
  atualizarTotal();
}

function linhasItens() { return [...areaItens.querySelectorAll('.linha-item')]; }

function atualizarTotal() {
  let total = 0;
  for (const linha of linhasItens()) {
    const { produtoId, quantidade } = linha._ler();
    const produto = produtos.find((p) => p.id === produtoId);
    const sub = produto && quantidade > 0 ? Number(produto.preco) * quantidade : 0;
    linha._subtotal.textContent = moeda(sub);
    total += sub;
  }
  document.getElementById('total').textContent = moeda(total);
}

areaItens.addEventListener('input', atualizarTotal);
areaItens.addEventListener('change', atualizarTotal);
document.getElementById('botao-add-item').addEventListener('click', () => adicionarItem());

// ---------------------------------------------------------------- entrega

function atualizarEndereco() {
  const entrega = marcado('tipoEntrega') === 'ENTREGA';
  campoEndereco.hidden = !entrega;
  if (entrega && !inputEndereco.value) {
    const c = clienteSelecionado();
    if (c && c.endereco) inputEndereco.value = c.endereco;
  }
}
form.addEventListener('change', (e) => { if (e.target.name === 'tipoEntrega') atualizarEndereco(); });

// ---------------------------------------------------------------- salvar

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  limparErro(form);
  const itens = linhasItens().map((l) => l._ler()).filter((i) => i.produtoId);
  const tipo = marcado('tipoEntrega');
  if (!selectCliente.value) return mostrarErro(form, { message: 'Escolha o cliente (ou cadastre um novo).', campos: { clienteId: 1 } });
  if (itens.length === 0) return mostrarErro(form, { message: 'Adicione pelo menos um produto.' });
  if (itens.some((i) => !(i.quantidade >= 1))) return mostrarErro(form, { message: 'A quantidade de cada produto deve ser 1 ou mais.' });
  if (!form.elements.dataEntregaRetirada.value) return mostrarErro(form, { message: 'Informe o dia da entrega ou retirada.', campos: { dataEntregaRetirada: 1 } });
  if (tipo === 'ENTREGA' && !inputEndereco.value.trim()) {
    return mostrarErro(form, { message: 'Para entrega, o endereço é obrigatório.', campos: { enderecoEntrega: 1 } });
  }

  const corpo = {
    clienteId: Number(selectCliente.value),
    origem: marcado('origem'),
    dataPedido: form.elements.dataPedido.value || null,
    horarioPedido: form.elements.horarioPedido.value || null,
    dataEntregaRetirada: form.elements.dataEntregaRetirada.value,
    horarioEntregaRetirada: form.elements.horarioEntregaRetirada.value || null,
    tipoEntrega: tipo,
    enderecoEntrega: tipo === 'ENTREGA' ? inputEndereco.value.trim() : null,
    formaPagamento: marcado('formaPagamento') || null,
    observacoes: form.elements.observacoes.value.trim() || null,
    itens,
  };
  try {
    await comBotaoOcupado(document.getElementById('botao-salvar'), 'Salvando…', () => (idPedido
      ? api(`/api/admin/pedidos/${idPedido}`, { metodo: 'PUT', corpo })
      : api('/api/admin/pedidos', { metodo: 'POST', corpo })));
    avisoNaProximaPagina(idPedido ? 'Pedido atualizado.' : 'Pedido salvo!');
    location.href = `/admin/pedidos.html?data=${corpo.dataEntregaRetirada}`;
  } catch (erro) {
    mostrarErro(form, erro);
  }
});

// ---------------------------------------------------------------- início

async function iniciar() {
  await iniciarLayout();
  [clientes, produtos] = await Promise.all([api('/api/admin/clientes'), api('/api/admin/produtos')]);
  form.elements.dataEntregaRetirada.value = hojeISO(1);

  if (idPedido) {
    const p = await api(`/api/admin/pedidos/${idPedido}`);
    document.getElementById('titulo').textContent = `Editar pedido #${p.codigo}`;
    document.title = `Editar pedido · Painel da Elaine`;
    preencherClientes(p.cliente.id);
    marcar('origem', p.origem);
    p.itens.forEach((i) => adicionarItem(i.produtoId, i.quantidade));
    form.elements.dataEntregaRetirada.value = p.dataEntregaRetirada;
    form.elements.horarioEntregaRetirada.value = p.horarioEntregaRetirada ? p.horarioEntregaRetirada.slice(0, 5) : '';
    marcar('tipoEntrega', p.tipoEntrega);
    inputEndereco.value = p.enderecoEntrega || '';
    marcar('formaPagamento', p.formaPagamento);
    form.elements.observacoes.value = p.observacoes || '';
    form.elements.dataPedido.value = p.dataPedido;
    form.elements.horarioPedido.value = p.horarioPedido ? p.horarioPedido.slice(0, 5) : '';
  } else {
    preencherClientes(params.get('cliente'));
    adicionarItem();
  }
  form.elements.dataPedido.max = hojeISO();
  atualizarEndereco();
  atualizarTotal();
}

iniciar();
