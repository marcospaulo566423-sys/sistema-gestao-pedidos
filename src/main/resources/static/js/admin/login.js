import { api, comBotaoOcupado, limparErro, mostrarErro } from '../comum.js';

const form = document.getElementById('form-login');
const botao = document.getElementById('botao-entrar');
form.elements.usuario.focus();

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  limparErro(form);
  const usuario = form.elements.usuario.value.trim();
  const senha = form.elements.senha.value;
  if (!usuario || !senha) return mostrarErro(form, { message: 'Preencha o usuário e a senha.' });
  try {
    await comBotaoOcupado(botao, 'Entrando…', () => api('/api/admin/login', {
      metodo: 'POST',
      formulario: new URLSearchParams({ usuario, senha }),
    }));
    location.href = '/admin/index.html';
  } catch (erro) {
    form.elements.senha.value = '';
    mostrarErro(form, erro);
  }
});
