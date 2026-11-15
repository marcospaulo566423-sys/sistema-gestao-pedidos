package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pagamento (UML: 0..1 por pedido). Criado somente por {@link Pedido#registrarPagamento}. */
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private FormaPagamento tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Pagamento() { }

    Pagamento(Pedido pedido, FormaPagamento tipo, BigDecimal valor, LocalDate dataPagamento, LocalDateTime agora) {
        this.pedido = pedido;
        this.tipo = tipo;
        this.valor = valor;
        this.dataPagamento = dataPagamento;
        this.criadoEm = agora;
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public FormaPagamento getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
