package br.com.deliciacaseira.dominio;

/** RF17: o pedido é retirado pelo cliente ou entregue pela administradora. */
public enum TipoEntrega {
    RETIRADA("Retirada"),
    ENTREGA("Entrega");

    private final String rotulo;
    TipoEntrega(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
