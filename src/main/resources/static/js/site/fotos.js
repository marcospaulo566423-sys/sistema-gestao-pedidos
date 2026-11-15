// =====================================================================
// Fotos da vitrine do site.
// A foto cadastrada pela Elaine no painel (fotoUrl) sempre tem prioridade.
// Sem foto cadastrada, usa uma foto ilustrativa do Unsplash escolhida pelo
// nome do produto. Se a imagem não carregar (sem internet), o quadro mostra
// o fundo de casca dourada definido no CSS (.foto.sem-foto).
// =====================================================================

const UNSPLASH = 'https://images.unsplash.com/photo-';

const FOTOS = {
  canela: '1509365465985-25d11c17e812',
  chocolate: '1606101273945-e9eba91c0dc4',
  brigadeiro: '1555507036-ab1f4038808a',
  coco: '1597604391235-a7429b4b350c',
  beijinho: '1598373182133-52452f7691ef',
  'doce de leite': '1483695028939-5bb13f8648b0',
  goiabada: '1549931319-a545dcf3bc73',
  rosquinha: '1558961363-fa8fdf82db35',
  'sem recheio': '1620921568790-c1cf8984624c',
};
const PADRAO = ['1620921568790-c1cf8984624c', '1586444248902-2f64eddc13df', '1589367920969-ab8e050bbb04'];

/** URL da foto ilustrativa para o produto, no tamanho pedido. */
export function fotoIlustrativa(produto, largura = 800) {
  const nome = (produto.nome || '').toLowerCase();
  // "gotas de chocolate" antes de "brigadeiro" etc.: a primeira palavra-chave encontrada vence
  const chave = Object.keys(FOTOS).find((k) => nome.includes(k));
  const id = chave ? FOTOS[chave] : PADRAO[Math.abs(Number(produto.id) || 0) % PADRAO.length];
  const altura = Math.round((largura * 3) / 4);
  return `${UNSPLASH}${id}?auto=format&fit=crop&w=${largura}&h=${altura}&q=70`;
}

export function fotoDoProduto(produto, largura) {
  return produto.fotoUrl || fotoIlustrativa(produto, largura);
}

/** Se a imagem falhar, esconde-a e deixa o quadro com o fundo de reserva. */
export function protegerFoto(img) {
  const marcar = () => img.closest('.foto')?.classList.add('sem-foto');
  img.addEventListener('error', marcar, { once: true });
  if (img.complete && img.naturalWidth === 0 && img.src) marcar();
  return img;
}

/** Liga a proteção em todas as fotos fixas da página (ex.: a do topo). */
export function protegerFotosDaPagina() {
  document.querySelectorAll('.foto img').forEach(protegerFoto);
}
