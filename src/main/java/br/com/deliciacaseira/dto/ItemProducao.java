package br.com.deliciacaseira.dto;

/** Linha da lista diária de produção (RF05): quanto produzir de cada produto. */
public record ItemProducao(Long produtoId, String produtoNome, Long quantidade) { }
