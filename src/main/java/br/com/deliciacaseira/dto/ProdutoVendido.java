package br.com.deliciacaseira.dto;

import java.math.BigDecimal;

/** Linha do relatório de produtos mais vendidos (RF10). */
public record ProdutoVendido(Long produtoId, String produtoNome, Long quantidade, BigDecimal valorTotal) { }
