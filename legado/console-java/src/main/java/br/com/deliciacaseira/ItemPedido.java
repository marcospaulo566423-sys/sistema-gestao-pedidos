package br.com.deliciacaseira;

public final class ItemPedido {
    private int quantidade;
    private Produto produto;
    public ItemPedido(Produto produto, int quantidade) { setProduto(produto); setQuantidade(quantidade); }
    public ItemPedido(int quantidade, Produto produto) { this(produto, quantidade); }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero."); this.quantidade = quantidade; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { if (produto == null) throw new IllegalArgumentException("Produto obrigatório."); this.produto = produto; }
    public double calcularSubtotal() { return quantidade * produto.getPreco(); }
    @Override public String toString() { return quantidade + " x " + produto.getNome() + " = R$ " + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", calcularSubtotal()); }
}
