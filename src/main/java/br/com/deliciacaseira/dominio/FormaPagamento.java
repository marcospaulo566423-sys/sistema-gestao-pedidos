package br.com.deliciacaseira.dominio;

/** RF07: formas de pagamento aceitas. */
public enum FormaPagamento {
    PIX("Pix"),
    CARTAO("Cartão"),
    DINHEIRO("Dinheiro");

    private final String rotulo;
    FormaPagamento(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
