package br.com.deliciacaseira;

public final class Pagamento {
    private String tipo;
    private double valor;
    private boolean registrado;
    public Pagamento(String tipo, double valor) { setTipo(tipo); setValor(valor); }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("Tipo de pagamento obrigatório."); this.tipo = tipo.trim(); }
    public double getValor() { return valor; }
    public void setValor(double valor) { if (!Double.isFinite(valor) || valor <= 0) throw new IllegalArgumentException("Valor do pagamento deve ser positivo."); this.valor = valor; }
    public boolean isRegistrado() { return registrado; }
    public boolean registrarPagamento() { if (tipo.isBlank() || valor <= 0) return false; registrado = true; return true; }
}
