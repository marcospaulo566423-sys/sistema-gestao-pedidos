const resultado = document.getElementById('resultado');
const subtitulo = document.getElementById('subtitulo-pedidos');

function formatarMoeda(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatarData(iso) {
  if (!iso) return '';
  const partes = String(iso).split('-');
  if (partes.length === 3) return `${partes[2]}/${partes[1]}/${partes[0]}`;
  return iso;
}

function badgeStatus(st) {
  const mapa = {
    PENDENTE: { texto: 'Pendente', cor: '#9a5b13', fundo: '#fef3c7' },
    CONCLUIDO: { texto: 'Concluído', cor: '#166534', fundo: '#dcfce7' },
    CANCELADO: { texto: 'Cancelado', cor: '#991b1b', fundo: '#fee2e2' }
  };
  const info = mapa[st] || { texto: st, cor: '#374151', fundo: '#f3f4f6' };
  return `<span style="background:${info.fundo}; color:${info.cor}; padding:0.28rem 0.75rem; border-radius:999px; font-size:0.84rem; font-weight:700;">${info.texto}</span>`;
}

(async () => {
  try {
    const meResp = await fetch('/api/auth/sessao');
    const user = meResp.ok ? await meResp.json() : { autenticado: false };

    if (!user.autenticado || user.perfil !== 'CLIENTE') {
      resultado.innerHTML = `
        <div class="cartao" style="text-align:center; padding:2rem;">
          <h2>Entre na sua conta para ver seus pedidos</h2>
          <p class="suave" style="margin:0.5rem 0 1.25rem;">Informe apenas seu nome e telefone para visualizar todas as suas encomendas automaticamente.</p>
          <a href="/entrar.html" class="botao grande">Entrar com meu telefone</a>
        </div>
      `;
      return;
    }

    if (subtitulo) {
      subtitulo.textContent = `Olá, ${user.nome}! Aqui estão todos os pedidos vinculados ao telefone ${user.telefone}.`;
    }

    const pedResp = await fetch('/api/publico/meus-pedidos');
    const pedidos = pedResp.ok ? await pedResp.json() : [];

    if (!pedidos.length) {
      resultado.innerHTML = `
        <div class="cartao" style="text-align:center; padding:2rem;">
          <h2>Nenhum pedido por aqui ainda</h2>
          <p class="suave" style="margin:0.5rem 0 1.25rem;">Você ainda não possui encomendas registradas nesta conta.</p>
          <a href="/pedido.html" class="botao grande">Fazer meu primeiro pedido</a>
        </div>
      `;
      return;
    }

    const cardsHtml = pedidos.map((p, idx) => {
      const itens = p.itens || [];
      const totalCalculado = p.total != null
        ? p.total
        : (p.valorTotal != null ? p.valorTotal : itens.reduce((acc, it) => acc + Number(it.subtotal || 0), 0));

      const itensLista = itens.map(i =>
        `<li>${i.quantidade}x ${i.produtoNome} — <strong>${formatarMoeda(i.subtotal)}</strong></li>`
      ).join('');

      const tipo = p.tipoEntrega === 'ENTREGA'
        ? `Entrega: ${p.enderecoEntrega || ''}`
        : 'Retirada no local';

      return `
        <article class="cartao" style="padding:1.4rem; margin-bottom:1rem;">
          <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.5rem; margin-bottom:0.75rem;">
            <div>
              <strong style="font-size:1.08rem;">Encomenda #${pedidos.length - idx}</strong>
              <span class="suave" style="font-size:0.9rem; margin-left:0.6rem;">Data: ${formatarData(p.dataEntregaRetirada)}${p.horarioEntregaRetirada ? ' às ' + String(p.horarioEntregaRetirada).slice(0,5) : ''}</span>
            </div>
            ${badgeStatus(p.status)}
          </div>
          <ul style="margin:0.5rem 0 0.85rem; padding-left:1.25rem; line-height:1.65;">
            ${itensLista}
          </ul>
          <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.5rem; border-top:1px solid #efe7da; padding-top:0.75rem; font-size:0.95rem;">
            <span>${tipo}</span>
            <strong style="font-size:1.08rem;">Total: ${formatarMoeda(totalCalculado)}</strong>
          </div>
        </article>
      `;
    }).join('');

    resultado.innerHTML = `
      <div style="display:flex; justify-content:flex-end; margin-bottom:1rem;">
        <a href="/pedido.html" class="botao">+ Novo Pedido</a>
      </div>
      ${cardsHtml}
    `;
  } catch (e) {
    console.error(e);
  }
})();