package br.com.deliciacaseira.dominio;

/** Unidades dos insumos do estoque. */
public enum UnidadeMedida {
    KG("kg"),
    G("g"),
    LITRO("litro"),
    ML("mL"),
    UNIDADE("unidade"),
    DUZIA("dúzia"),
    PACOTE("pacote");

    private final String rotulo;
    UnidadeMedida(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
