package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Item de estoque = INSUMO do negócio (farinha, leite, açúcar...), com controle MANUAL.
 * Não é baixado automaticamente pelos pedidos e nunca bloqueia pedidos (decisão 3; corrige R6).
 */
@Entity
@Table(name = "item_estoque")
public class ItemEstoque {

    private static final BigDecimal QUANTIDADE_MAXIMA = new BigDecimal("1000000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(name = "quantidade_atual", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeAtual;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "unidade_medida", nullable = false, length = 20)
    private UnidadeMedida unidadeMedida;

    @Column(name = "quantidade_minima", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeMinima;

    protected ItemEstoque() { }

    public ItemEstoque(String nome, BigDecimal quantidadeAtual, UnidadeMedida unidadeMedida, BigDecimal quantidadeMinima) {
        setNome(nome);
        atualizarQuantidade(quantidadeAtual);
        setUnidadeMedida(unidadeMedida);
        setQuantidadeMinima(quantidadeMinima);
    }

    /** UML: atualizarQuantidade(novaQuantidade) — define o novo saldo (não soma ao anterior). */
    public void atualizarQuantidade(BigDecimal novaQuantidade) {
        this.quantidadeAtual = validar(novaQuantidade, "Quantidade atual");
    }

    /** UML: precisaComprar() — verdadeiro quando quantidadeAtual <= quantidadeMinima. */
    public boolean precisaComprar() { return quantidadeAtual.compareTo(quantidadeMinima) <= 0; }

    private static BigDecimal validar(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0 || valor.compareTo(QUANTIDADE_MAXIMA) > 0)
            throw new RegraNegocioException(campo + " inválida. Informe zero ou um número positivo.");
        return valor.setScale(3, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = Textos.obrigatorio(nome, "Nome do item", 100); }
    public BigDecimal getQuantidadeAtual() { return quantidadeAtual; }
    public UnidadeMedida getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(UnidadeMedida unidadeMedida) {
        if (unidadeMedida == null) throw new RegraNegocioException("Unidade de medida é obrigatória.");
        this.unidadeMedida = unidadeMedida;
    }
    public BigDecimal getQuantidadeMinima() { return quantidadeMinima; }
    public void setQuantidadeMinima(BigDecimal quantidadeMinima) {
        this.quantidadeMinima = validar(quantidadeMinima, "Quantidade mínima");
    }
}
