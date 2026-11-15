package br.com.deliciacaseira.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pedido (UML): pertence a exatamente 1 Cliente, é composto por 1..* ItemPedido
 * e tem 0..1 Pagamento. Concentra as regras que dependem só do próprio pedido.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    public static final int MAXIMO_ITENS = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código curto e aleatório que o cliente usa em "Meus Pedidos" (junto com o telefone). */
    @Column(nullable = false, unique = true, length = 8)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

    @Column(name = "horario_pedido", nullable = false)
    private LocalTime horarioPedido;

    @Column(name = "data_entrega_retirada", nullable = false)
    private LocalDate dataEntregaRetirada;

    @Column(name = "horario_entrega_retirada")
    private LocalTime horarioEntregaRetirada;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusPedido status = StatusPedido.PENDENTE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_entrega", nullable = false, length = 20)
    private TipoEntrega tipoEntrega;

    @Column(name = "endereco_entrega", length = 255)
    private String enderecoEntrega;

    /** Forma de pagamento escolhida/combinada (pode ser registrada antes do pagamento acontecer). */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrigemPedido origem;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPedido> itens = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", fetch = FetchType.LAZY)
    private Pagamento pagamento;

    protected Pedido() { }

    /** Todo pedido nasce PENDENTE (nota do UML). */
    public Pedido(String codigo, Cliente cliente, OrigemPedido origem, LocalDateTime agora) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Código obrigatório.");
        if (cliente == null) throw new RegraNegocioException("Cliente é obrigatório.");
        if (origem == null) throw new RegraNegocioException("Informe a origem do pedido (Site, WhatsApp ou Presencial).");
        this.codigo = codigo;
        this.cliente = cliente;
        this.origem = origem;
        this.criadoEm = agora;
        this.dataPedido = agora.toLocalDate();
        this.horarioPedido = agora.toLocalTime().withSecond(0).withNano(0);
    }

    // ------------------------------------------------------------------ datas (RF03 / RF04)

    /** Define todas as datas de uma vez e valida juntas (corrige B5). */
    public void definirDatas(LocalDate dataPedido, LocalTime horarioPedido,
                             LocalDate dataEntregaRetirada, LocalTime horarioEntregaRetirada) {
        exigirEditavel();
        if (dataPedido == null || horarioPedido == null) throw new RegraNegocioException("Data e horário do pedido são obrigatórios.");
        if (dataEntregaRetirada == null) throw new RegraNegocioException("Informe a data de entrega ou retirada.");
        if (dataEntregaRetirada.isBefore(dataPedido))
            throw new RegraNegocioException("A data de entrega/retirada não pode ser antes da data do pedido.");
        this.dataPedido = dataPedido;
        this.horarioPedido = horarioPedido.withSecond(0).withNano(0);
        this.dataEntregaRetirada = dataEntregaRetirada;
        this.horarioEntregaRetirada = horarioEntregaRetirada == null ? null : horarioEntregaRetirada.withSecond(0).withNano(0);
    }

    // ------------------------------------------------------------------ logística (Regras 3 e 4)

    public void definirLogistica(TipoEntrega tipo, String endereco) {
        exigirEditavel();
        if (tipo == null) throw new RegraNegocioException("Escolha se o pedido será Retirada ou Entrega.");
        String enderecoLimpo = Textos.opcional(endereco, "Endereço de entrega", 255);
        if (tipo == TipoEntrega.ENTREGA && enderecoLimpo == null)
            throw new RegraNegocioException("Para entrega, o endereço é obrigatório.");
        this.tipoEntrega = tipo;
        this.enderecoEntrega = tipo == TipoEntrega.ENTREGA ? enderecoLimpo : null;
    }

    // ------------------------------------------------------------------ itens (RF03)

    /** Item solicitado: produto + quantidade. */
    public record ItemSolicitado(Produto produto, int quantidade) { }

    /**
     * Substitui os itens do pedido. Produtos repetidos são somados. Produtos que já estavam no
     * pedido mantêm o preço da venda original; produtos novos usam o preço atual (corrige B6).
     */
    public void substituirItens(List<ItemSolicitado> solicitados) {
        exigirEditavel();
        if (solicitados == null || solicitados.isEmpty())
            throw new RegraNegocioException("O pedido precisa ter pelo menos um produto.");

        Map<Produto, Integer> quantidades = new LinkedHashMap<>();
        for (ItemSolicitado s : solicitados) {
            if (s == null || s.produto() == null) throw new RegraNegocioException("Produto obrigatório.");
            quantidades.merge(s.produto(), s.quantidade(), Integer::sum);
        }
        if (quantidades.size() > MAXIMO_ITENS)
            throw new RegraNegocioException("Um pedido pode ter no máximo " + MAXIMO_ITENS + " produtos diferentes.");

        Map<Produto, BigDecimal> precosAnteriores = new IdentityHashMap<>();
        itens.forEach(i -> precosAnteriores.put(i.getProduto(), i.getPrecoUnitario()));

        List<ItemPedido> novos = new ArrayList<>();
        quantidades.forEach((produto, qtd) -> novos.add(new ItemPedido(this, produto, qtd,
                precosAnteriores.getOrDefault(produto, produto.getPreco()))));

        BigDecimal novoTotal = novos.stream().map(ItemPedido::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (pagamento != null && novoTotal.compareTo(calcularTotal()) != 0)
            throw new RegraNegocioException("Este pedido já tem pagamento registrado. Remova o pagamento antes de mudar os produtos.");

        itens.clear();
        itens.addAll(novos);
    }

    /** UML: calcularTotal() = soma dos subtotais dos itens. */
    public BigDecimal calcularTotal() {
        return itens.stream().map(ItemPedido::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ status (RF06 / Regra 7)

    /** UML: atualizarStatus(novoStatus) com transições controladas (corrige B11). */
    public void atualizarStatus(StatusPedido novo) {
        if (novo == null) throw new RegraNegocioException("Status obrigatório.");
        if (novo == status) return;
        if (!status.podeMudarPara(novo))
            throw new RegraNegocioException("Não é possível mudar um pedido " + status.getRotulo().toLowerCase()
                    + " para " + novo.getRotulo().toLowerCase() + ".");
        if (novo == StatusPedido.CANCELADO && pagamento != null)
            throw new RegraNegocioException("Este pedido tem pagamento registrado. Remova o pagamento antes de cancelar.");
        this.status = novo;
    }

    // ------------------------------------------------------------------ pagamento (RF07)

    /**
     * Registra o pagamento (0..1). O valor deve ser exatamente o total do pedido;
     * se não for informado, usa o total. Impede pagamento duplicado e de pedido cancelado.
     */
    public Pagamento registrarPagamento(FormaPagamento tipo, BigDecimal valor, LocalDate data, LocalDateTime agora) {
        if (status == StatusPedido.CANCELADO) throw new RegraNegocioException("Pedido cancelado não pode receber pagamento.");
        if (pagamento != null) throw new RegraNegocioException("Este pedido já tem pagamento registrado.");
        if (tipo == null) throw new RegraNegocioException("Escolha a forma de pagamento: Pix, Cartão ou Dinheiro.");
        if (data == null) throw new RegraNegocioException("Informe a data do pagamento.");
        BigDecimal total = calcularTotal();
        BigDecimal valorPago = valor == null ? total : valor;
        if (valorPago.compareTo(total) != 0)
            throw new RegraNegocioException("O valor do pagamento deve ser igual ao total do pedido (R$ " + total + ").");
        if (total.signum() <= 0) throw new RegraNegocioException("Pedido sem valor não pode receber pagamento.");
        this.pagamento = new Pagamento(this, tipo, total, data, agora);
        this.formaPagamento = tipo;
        return pagamento;
    }

    /** Desvincula o pagamento (o serviço apaga o pagamento e a entrada financeira automática). */
    public void removerPagamento() { this.pagamento = null; }

    public boolean isPago() { return pagamento != null; }

    private void exigirEditavel() {
        if (status != StatusPedido.PENDENTE)
            throw new RegraNegocioException("Só é possível alterar pedidos pendentes. Este pedido está "
                    + status.getRotulo().toLowerCase() + ".");
    }

    // ------------------------------------------------------------------ getters / setters simples

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) {
        exigirEditavel();
        if (cliente == null) throw new RegraNegocioException("Cliente é obrigatório.");
        this.cliente = cliente;
    }
    public LocalDate getDataPedido() { return dataPedido; }
    public LocalTime getHorarioPedido() { return horarioPedido; }
    public LocalDate getDataEntregaRetirada() { return dataEntregaRetirada; }
    public LocalTime getHorarioEntregaRetirada() { return horarioEntregaRetirada; }
    public StatusPedido getStatus() { return status; }
    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public String getEnderecoEntrega() { return enderecoEntrega; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public OrigemPedido getOrigem() { return origem; }
    public void setOrigem(OrigemPedido origem) {
        if (origem == null) throw new RegraNegocioException("Informe a origem do pedido.");
        this.origem = origem;
    }
    public String getObservacoes() { return observacoes; }
    /** RF14: observações importantes sobre o pedido. */
    public void setObservacoes(String observacoes) { this.observacoes = Textos.opcional(observacoes, "Observações", 500); }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<ItemPedido> getItens() { return Collections.unmodifiableList(itens); }
    public Pagamento getPagamento() { return pagamento; }
}
