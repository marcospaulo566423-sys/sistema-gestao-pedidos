package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Item do pedido (composição com Pedido). Guarda o preço unitário do momento da venda,
 * para que mudar o preço do produto não altere pedidos já feitos (corrige B6).
 */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    public static final int QUANTIDADE_MAXIMA = 999;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPedido() { }

    ItemPedido(Pedido pedido, Produto produto, int quantidade, BigDecimal precoUnitario) {
        if (produto == null) throw new RegraNegocioException("Produto obrigatório.");
        if (quantidade <= 0 || quantidade > QUANTIDADE_MAXIMA)
            throw new RegraNegocioException("A quantidade de \"" + produto.getNome() + "\" deve ser entre 1 e "
                    + QUANTIDADE_MAXIMA + ".");
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    /** UML: calcularSubtotal() = quantidade × preço unitário. */
    public BigDecimal calcularSubtotal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
}
