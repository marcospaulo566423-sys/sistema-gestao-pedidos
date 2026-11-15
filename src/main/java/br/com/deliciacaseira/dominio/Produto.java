package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Produto (RF22/RF23): cada sabor/variedade é um produto.
 * "ativo" permite tirar um produto do cardápio sem apagar o histórico de pedidos que o usam.
 */
@Entity
@Table(name = "produto")
public class Produto {

    public static final BigDecimal PRECO_MAXIMO = new BigDecimal("10000.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    /** Nome do arquivo da foto gerado pelo sistema (nunca o nome enviado pelo usuário). */
    @Column(length = 100)
    private String foto;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Produto() { }

    public Produto(String nome, BigDecimal preco, String descricao) {
        setNome(nome);
        atualizarPreco(preco);
        setDescricao(descricao);
    }

    /** UML: atualizarPreco(novoPreco). Não altera pedidos antigos (o item guarda o preço da venda). */
    public void atualizarPreco(BigDecimal novoPreco) {
        if (novoPreco == null || novoPreco.signum() <= 0 || novoPreco.compareTo(PRECO_MAXIMO) > 0)
            throw new RegraNegocioException("Preço inválido. Informe um valor maior que zero.");
        this.preco = novoPreco.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = Textos.obrigatorio(nome, "Nome do produto", 100); }
    public BigDecimal getPreco() { return preco; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = Textos.opcional(descricao, "Descrição", 500); }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
