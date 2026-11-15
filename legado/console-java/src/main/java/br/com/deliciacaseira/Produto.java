package br.com.deliciacaseira;

public final class Produto {
    private String nome;
    private double preco;
    private String foto;
    private String descricao;
    public Produto(String nome, double preco, String foto, String descricao) { setNome(nome); setPreco(preco); setFoto(foto); setDescricao(descricao); }
    public Produto(String nome, double preco, String descricao) { this(nome, preco, "", descricao); }
    public String getNome() { return nome; }
    public void setNome(String nome) { if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome do produto obrigatório."); this.nome = nome.trim(); }
    public double getPreco() { return preco; }
    public void setPreco(double preco) { if (!Double.isFinite(preco) || preco < 0) throw new IllegalArgumentException("Preço inválido."); this.preco = preco; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto == null ? "" : foto.trim(); }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao == null ? "" : descricao.trim(); }
    public void atualizarPreco(double novoPreco) { setPreco(novoPreco); }
    @Override public String toString() { return nome + " (R$ " + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", preco) + ")"; }
}
