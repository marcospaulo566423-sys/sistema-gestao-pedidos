package br.com.deliciacaseira.servico;

import br.com.deliciacaseira.dominio.ItemEstoque;
import br.com.deliciacaseira.dominio.NaoEncontradoException;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dominio.Textos;
import br.com.deliciacaseira.dto.CadastroDto.EstoqueEntrada;
import br.com.deliciacaseira.dto.CadastroDto.EstoqueVisao;
import br.com.deliciacaseira.repositorio.ItemEstoqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Estoque de INSUMOS com controle manual e alerta de reposição (decisão 3). */
@Service
@Transactional
public class EstoqueService {

    private final ItemEstoqueRepository itens;

    public EstoqueService(ItemEstoqueRepository itens) { this.itens = itens; }

    /** UML: consultarEstoque(). */
    @Transactional(readOnly = true)
    public List<EstoqueVisao> consultarEstoque() {
        return itens.listarOrdenado().stream().map(EstoqueVisao::de).toList();
    }

    /** UML: listarItensParaCompra() — itens com quantidadeAtual <= quantidadeMinima. */
    @Transactional(readOnly = true)
    public List<EstoqueVisao> listarItensParaCompra() {
        return itens.listarOrdenado().stream().filter(ItemEstoque::precisaComprar).map(EstoqueVisao::de).toList();
    }

    /** UML: cadastrarItemEstoque(item). */
    public EstoqueVisao cadastrarItemEstoque(EstoqueEntrada e) {
        exigirNomeLivre(e.nome(), null);
        return EstoqueVisao.de(itens.save(new ItemEstoque(e.nome(), e.quantidadeAtual(), e.unidadeMedida(), e.quantidadeMinima())));
    }

    public EstoqueVisao atualizar(Long id, EstoqueEntrada e) {
        ItemEstoque i = obter(id);
        exigirNomeLivre(e.nome(), id);
        i.setNome(e.nome());
        i.atualizarQuantidade(e.quantidadeAtual());
        i.setUnidadeMedida(e.unidadeMedida());
        i.setQuantidadeMinima(e.quantidadeMinima());
        return EstoqueVisao.de(i);
    }

    /** UML: atualizarQuantidade(novaQuantidade). */
    public EstoqueVisao atualizarQuantidade(Long id, BigDecimal novaQuantidade) {
        ItemEstoque i = obter(id);
        i.atualizarQuantidade(novaQuantidade);
        return EstoqueVisao.de(i);
    }

    public void excluir(Long id) { itens.delete(obter(id)); }

    private ItemEstoque obter(Long id) {
        return itens.findById(id).orElseThrow(() -> new NaoEncontradoException("Item de estoque não encontrado."));
    }

    private void exigirNomeLivre(String nome, Long idAtual) {
        String limpo = Textos.obrigatorio(nome, "Nome do item", 100);
        itens.findByNomeIgnoreCase(limpo).ifPresent(existente -> {
            if (!existente.getId().equals(idAtual))
                throw new RegraNegocioException("Já existe um item chamado \"" + existente.getNome() + "\" no estoque.");
        });
    }
}
