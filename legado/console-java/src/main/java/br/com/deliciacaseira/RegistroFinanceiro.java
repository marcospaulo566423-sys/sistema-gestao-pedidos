package br.com.deliciacaseira;

import java.time.LocalDate;

public final class RegistroFinanceiro {
    private int id;
    private double valor;
    private LocalDate data;
    private String descricao;
    public RegistroFinanceiro(int id, double valor, LocalDate data, String descricao) { setId(id); setValor(valor); setData(data); setDescricao(descricao); }
    public int getId() { return id; }
    public void setId(int id) { if (id <= 0) throw new IllegalArgumentException("ID deve ser positivo."); this.id = id; }
    public double getValor() { return valor; }
    public void setValor(double valor) { if (!Double.isFinite(valor) || valor <= 0) throw new IllegalArgumentException("Valor deve ser positivo."); this.valor = valor; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { if (data == null) throw new IllegalArgumentException("Data obrigatória."); this.data = data; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { if (descricao == null || descricao.isBlank()) throw new IllegalArgumentException("Descrição obrigatória."); this.descricao = descricao.trim(); }
    @Override public String toString() { return "#" + id + " | " + data + " | R$ " + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", valor) + " | " + descricao; }
}
