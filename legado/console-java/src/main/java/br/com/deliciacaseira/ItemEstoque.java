package br.com.deliciacaseira;

public final class ItemEstoque {
    private int id;
    private String nome;
    private double quantidadeAtual;
    private String unidadeMedida;
    private double quantidadeMinima;
    public ItemEstoque(int id, String nome, double quantidadeAtual, String unidadeMedida, double quantidadeMinima) {
        setId(id); setNome(nome); setQuantidadeAtual(quantidadeAtual); setUnidadeMedida(unidadeMedida); setQuantidadeMinima(quantidadeMinima);
    }
    public int getId() { return id; }
    public void setId(int id) { if (id <= 0) throw new IllegalArgumentException("ID deve ser positivo."); this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório."); this.nome = nome.trim(); }
    public double getQuantidadeAtual() { return quantidadeAtual; }
    public void setQuantidadeAtual(double quantidadeAtual) { if (!Double.isFinite(quantidadeAtual) || quantidadeAtual < 0) throw new IllegalArgumentException("Quantidade inválida."); this.quantidadeAtual = quantidadeAtual; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { if (unidadeMedida == null || unidadeMedida.isBlank()) throw new IllegalArgumentException("Unidade de medida obrigatória."); this.unidadeMedida = unidadeMedida.trim(); }
    public double getQuantidadeMinima() { return quantidadeMinima; }
    public void setQuantidadeMinima(double quantidadeMinima) { if (!Double.isFinite(quantidadeMinima) || quantidadeMinima < 0) throw new IllegalArgumentException("Quantidade mínima inválida."); this.quantidadeMinima = quantidadeMinima; }
    /** Atualiza a quantidade para o novo saldo informado (não soma ao saldo anterior). */
    public void atualizarQuantidade(double novaQuantidade) { setQuantidadeAtual(novaQuantidade); }
    public boolean precisaComprar() { return quantidadeAtual <= quantidadeMinima; }
    public boolean retirar(double quantidade) { if (!Double.isFinite(quantidade) || quantidade <= 0 || quantidade > quantidadeAtual) return false; quantidadeAtual -= quantidade; return true; }
    public void repor(double quantidade) { if (!Double.isFinite(quantidade) || quantidade <= 0) throw new IllegalArgumentException("Reposição deve ser positiva."); quantidadeAtual += quantidade; }
    @Override public String toString() { return nome + ": " + quantidadeAtual + " " + unidadeMedida + " (mínimo: " + quantidadeMinima + ")"; }
}
