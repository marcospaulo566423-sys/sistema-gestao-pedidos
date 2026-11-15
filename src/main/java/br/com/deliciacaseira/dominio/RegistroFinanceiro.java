package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entrada financeira (RF08). Somente entradas — despesas e lucro não são modelados.
 * Automática quando gerada por um Pagamento; manual (avulsa) quando lançada pela administradora.
 */
@Entity
@Table(name = "registro_financeiro")
public class RegistroFinanceiro {

    public static final BigDecimal VALOR_MAXIMO = new BigDecimal("100000.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_registro", nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 200)
    private String descricao;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagamento_id", unique = true)
    private Pagamento pagamento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected RegistroFinanceiro() { }

    /** Entrada manual avulsa. */
    public RegistroFinanceiro(BigDecimal valor, LocalDate data, String descricao, LocalDateTime agora) {
        setValor(valor);
        setData(data);
        setDescricao(descricao);
        this.criadoEm = agora;
    }

    /** Entrada automática gerada pelo pagamento de um pedido (decisão 6). */
    public static RegistroFinanceiro doPagamento(Pagamento pagamento, LocalDateTime agora) {
        Pedido pedido = pagamento.getPedido();
        String descricao = "Pedido " + pedido.getCodigo() + " - " + pedido.getCliente().getNome()
                + " (" + pagamento.getTipo().getRotulo() + ")";
        RegistroFinanceiro r = new RegistroFinanceiro(pagamento.getValor(), pagamento.getDataPagamento(),
                descricao.length() > 200 ? descricao.substring(0, 200) : descricao, agora);
        r.pagamento = pagamento;
        return r;
    }

    public boolean isAutomatico() { return pagamento != null; }

    public Long getId() { return id; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.compareTo(VALOR_MAXIMO) > 0)
            throw new RegraNegocioException("Valor inválido. Informe um valor maior que zero.");
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) {
        if (data == null) throw new RegraNegocioException("Data é obrigatória.");
        this.data = data;
    }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = Textos.obrigatorio(descricao, "Descrição", 200); }
    public Pagamento getPagamento() { return pagamento; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
