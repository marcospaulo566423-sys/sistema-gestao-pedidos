document.addEventListener('DOMContentLoaded', async () => {
  const nav = document.querySelector('.menu-site');
  if (!nav) return;

  let btnEntrar = document.getElementById('nav-auth-btn');
  if (!btnEntrar) {
    btnEntrar = document.createElement('a');
    btnEntrar.id = 'nav-auth-btn';
    btnEntrar.className = 'botao grande';
    btnEntrar.href = '/entrar.html';
    btnEntrar.textContent = 'Entrar';
    nav.appendChild(btnEntrar);
  }

  try {
    const resp = await fetch('/api/auth/sessao');
    if (resp.ok) {
      const user = await resp.json();
      if (user.autenticado && user.perfil === 'ADMIN') {
        btnEntrar.href = '/admin/index.html';
        btnEntrar.textContent = 'Painel Admin';
      } else if (user.autenticado && user.perfil === 'CLIENTE') {
        const primeiroNome = (user.nome || 'Cliente').split(' ')[0];
        btnEntrar.href = '/meus-pedidos.html';
        btnEntrar.textContent = `Olá, ${primeiroNome}`;
        localStorage.setItem('clienteLogado', JSON.stringify({ nome: user.nome, telefone: user.telefone }));

        // Preenche automaticamente nome e telefone na página de Fazer Pedido
        const campoNome = document.getElementById('nome') || document.querySelector('input[name="nome"]');
        const campoTel = document.getElementById('telefone') || document.querySelector('input[name="telefone"]');
        if (campoNome && !campoNome.value) campoNome.value = user.nome || '';
        if (campoTel && !campoTel.value) campoTel.value = user.telefone || '';

        if (!document.getElementById('nav-sair-btn')) {
          const btnSair = document.createElement('button');
          btnSair.id = 'nav-sair-btn';
          btnSair.type = 'button';
          btnSair.className = 'botao secundario';
          btnSair.textContent = 'Sair';
          btnSair.addEventListener('click', async () => {
            await fetch('/api/auth/logout', { method: 'POST' });
            localStorage.removeItem('clienteLogado');
            window.location.href = '/';
          });
          nav.appendChild(btnSair);
        }
      } else {
        localStorage.removeItem('clienteLogado');
      }
    }
  } catch (e) {}
});