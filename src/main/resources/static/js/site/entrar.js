const tabs = document.querySelectorAll('.auth-aba');
const formCliente = document.getElementById('form-cliente');
const formAdmin = document.getElementById('form-admin');
const erroBox = document.getElementById('msg-erro');

function mostrarErro(msg) {
  erroBox.textContent = msg;
  erroBox.classList.add('visivel');
}
function limparErro() {
  erroBox.classList.remove('visivel');
}

tabs.forEach(tab => {
  tab.addEventListener('click', () => {
    limparErro();
    tabs.forEach(t => t.classList.remove('ativa'));
    tab.classList.add('ativa');
    const aba = tab.dataset.aba;
    formCliente.classList.toggle('ativa', aba === 'cliente');
    formAdmin.classList.toggle('ativa', aba === 'admin');
  });
});

const telInput = document.getElementById('cli-tel');
telInput?.addEventListener('input', (e) => {
  let v = e.target.value.replace(/\D/g, '').slice(0, 11);
  if (v.length > 6) v = `(${v.slice(0,2)}) ${v.slice(2,7)}-${v.slice(7)}`;
  else if (v.length > 2) v = `(${v.slice(0,2)}) ${v.slice(2)}`;
  else if (v.length > 0) v = `(${v}`;
  e.target.value = v;
});

formCliente?.addEventListener('submit', async (e) => {
  e.preventDefault();
  limparErro();
  const nome = document.getElementById('cli-nome').value.trim();
  const telefone = document.getElementById('cli-tel').value.replace(/\D/g, '');
  try {
    const resp = await fetch('/api/auth/cliente', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nome, telefone })
    });
    const dados = await resp.json();
    if (!resp.ok) {
      mostrarErro(dados.mensagem || dados.erro || 'Não foi possível entrar.');
      return;
    }
    localStorage.setItem('clienteLogado', JSON.stringify({ nome: dados.nome, telefone: dados.telefone }));
    window.location.href = '/meus-pedidos.html';
  } catch (err) {
    mostrarErro('Erro de conexão com o servidor.');
  }
});

formAdmin?.addEventListener('submit', async (e) => {
  e.preventDefault();
  limparErro();
  const usuario = document.getElementById('adm-usuario').value.trim();
  const senha = document.getElementById('adm-senha').value;
  try {
    const params = new URLSearchParams();
    params.append('usuario', usuario);
    params.append('senha', senha);
    const resp = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    });
    if (!resp.ok) {
      mostrarErro('Usuário ou senha da administradora inválidos.');
      return;
    }
    window.location.href = '/admin/index.html';
  } catch (err) {
    mostrarErro('Erro de conexão com o servidor.');
  }
});