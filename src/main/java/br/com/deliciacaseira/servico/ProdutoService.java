package br.com.deliciacaseira.servico;

import br.com.deliciacaseira.dominio.NaoEncontradoException;
import br.com.deliciacaseira.dominio.Produto;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dominio.Textos;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoCardapio;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoVisao;
import br.com.deliciacaseira.repositorio.PedidoRepository;
import br.com.deliciacaseira.repositorio.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** RF22 / RF23: catálogo de produtos (cada sabor/variedade é um produto). */
@Service
@Transactional
public class ProdutoService {

    private final ProdutoRepository produtos;
    private final PedidoRepository pedidos;
    private final ArmazenamentoFotos fotos;

    public ProdutoService(ProdutoRepository produtos, PedidoRepository pedidos, ArmazenamentoFotos fotos) {
        this.produtos = produtos;
        this.pedidos = pedidos;
        this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public List<ProdutoCardapio> listarCardapio() {
        return produtos.listarAtivos().stream().map(ProdutoCardapio::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoVisao> listarTodos() {
        return produtos.listarTodos().stream().map(ProdutoVisao::de).toList();
    }

    public ProdutoVisao criar(ProdutoEntrada e) {
        exigirNomeLivre(e.nome(), null);
        Produto p = new Produto(e.nome(), e.preco(), e.descricao());
        if (e.ativo() != null) p.setAtivo(e.ativo());
        return ProdutoVisao.de(produtos.save(p));
    }

    public ProdutoVisao atualizar(Long id, ProdutoEntrada e) {
        Produto p = obter(id);
        exigirNomeLivre(e.nome(), id);
        p.setNome(e.nome());
        p.atualizarPreco(e.preco());
        p.setDescricao(e.descricao());
        if (e.ativo() != null) p.setAtivo(e.ativo());
        return ProdutoVisao.de(p);
    }

    /** Produto usado em pedidos não pode ser apagado (perderia o histórico): deve ser desativado. */
    public void excluir(Long id) {
        Produto p = obter(id);
        if (pedidos.existeItemComProduto(id))
            throw new RegraNegocioException("Este produto aparece em pedidos e não pode ser excluído. "
                    + "Use \"Tirar do cardápio\" para escondê-lo do site.");
        fotos.apagar(p.getFoto());
        produtos.delete(p);
    }

    public ProdutoVisao salvarFoto(Long id, MultipartFile arquivo) {
        Produto p = obter(id);
        String nova = fotos.salvar(arquivo);
        String antiga = p.getFoto();
        p.setFoto(nova);
        fotos.apagar(antiga);
        return ProdutoVisao.de(p);
    }

    public Produto obter(Long id) {
        if (id == null) throw new RegraNegocioException("Escolha o produto.");
        return produtos.findById(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado."));
    }

    private void exigirNomeLivre(String nome, Long idAtual) {
        String limpo = Textos.obrigatorio(nome, "Nome do produto", 100);
        produtos.findByNomeIgnoreCase(limpo).ifPresent(existente -> {
            if (!existente.getId().equals(idAtual))
                throw new RegraNegocioException("Já existe um produto chamado \"" + existente.getNome() + "\".");
        });
    }
}
