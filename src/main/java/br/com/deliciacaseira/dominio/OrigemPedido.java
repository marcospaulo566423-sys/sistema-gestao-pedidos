package br.com.deliciacaseira.dominio;

/** RF13: de onde veio o pedido. WhatsApp e Presencial são registrados manualmente pela administradora. */
public enum OrigemPedido {
    SITE("Site"),
    WHATSAPP("WhatsApp"),
    PRESENCIAL("Presencial");

    private final String rotulo;
    OrigemPedido(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
