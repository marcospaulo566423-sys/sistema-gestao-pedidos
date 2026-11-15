package br.com.deliciacaseira.dto;

import br.com.deliciacaseira.dominio.Cliente;
import br.com.deliciacaseira.dominio.ItemEstoque;
import br.com.deliciacaseira.dominio.Produto;
import br.com.deliciacaseira.dominio.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Dados de entrada e saída de clientes, produtos e estoque. */
public final class CadastroDto {
    private CadastroDto() { }

    // ----------------------------------------------------------------- clientes (RF16 / RF18)

    public record ClienteEntrada(
            @NotBlank(message = "Informe o nome.") @Size(max = 100) String nome,
            @NotBlank(message = "Informe o telefone.") @Size(max = 30) String telefone,
            @Size(max = 255) String endereco,
            boolean clienteFrequente) { }

    public record ClienteVisao(Long id, String nome, String telefone, String endereco,
                               boolean clienteFrequente, long quantidadePedidos) {
        public static ClienteVisao de(Cliente c, long quantidadePedidos) {
            return new ClienteVisao(c.getId(), c.getNome(), c.getTelefone(), c.getEndereco(),
                    c.isClienteFrequente(), quantidadePedidos);
        }
    }

    // ----------------------------------------------------------------- produtos (RF22 / RF23)

    public record ProdutoEntrada(
            @NotBlank(message = "Informe o nome do produto.") @Size(max = 100) String nome,
            @NotNull(message = "Informe o preço.") @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
            BigDecimal preco,
            @Size(max = 500) String descricao,
            Boolean ativo) { }

    public record ProdutoVisao(Long id, String nome, BigDecimal preco, String descricao, String fotoUrl, boolean ativo) {
        public static ProdutoVisao de(Produto p) {
            return new ProdutoVisao(p.getId(), p.getNome(), p.getPreco(), p.getDescricao(), urlDaFoto(p), p.isAtivo());
        }
    }

    /** Cardápio público: somente o que o cliente precisa ver. */
    public record ProdutoCardapio(Long id, String nome, BigDecimal preco, String descricao, String fotoUrl) {
        public static ProdutoCardapio de(Produto p) {
            return new ProdutoCardapio(p.getId(), p.getNome(), p.getPreco(), p.getDescricao(), urlDaFoto(p));
        }
    }

    static String urlDaFoto(Produto p) { return p.getFoto() == null ? null : "/fotos/" + p.getFoto(); }

    // ----------------------------------------------------------------- estoque (insumos)

    public record EstoqueEntrada(
            @NotBlank(message = "Informe o nome do item.") @Size(max = 100) String nome,
            @NotNull(message = "Informe a quantidade atual.") BigDecimal quantidadeAtual,
            @NotNull(message = "Escolha a unidade de medida.") UnidadeMedida unidadeMedida,
            @NotNull(message = "Informe a quantidade mínima.") BigDecimal quantidadeMinima) { }

    public record QuantidadeEntrada(@NotNull(message = "Informe a nova quantidade.") BigDecimal novaQuantidade) { }

    public record EstoqueVisao(Long id, String nome, BigDecimal quantidadeAtual, UnidadeMedida unidadeMedida,
                               String unidadeRotulo, BigDecimal quantidadeMinima, boolean precisaComprar) {
        public static EstoqueVisao de(ItemEstoque i) {
            return new EstoqueVisao(i.getId(), i.getNome(), i.getQuantidadeAtual().stripTrailingZeros(),
                    i.getUnidadeMedida(), i.getUnidadeMedida().getRotulo(),
                    i.getQuantidadeMinima().stripTrailingZeros(), i.precisaComprar());
        }
    }
}
