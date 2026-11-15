package br.com.deliciacaseira.dominio;

import java.util.Set;

/** Regra 7: o sistema diferencia apenas pedidos Pendentes, Concluídos e Cancelados. */
public enum StatusPedido {
    PENDENTE("Pendente"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusPedido(String rotulo) { this.rotulo = rotulo; }

    public String getRotulo() { return rotulo; }

    /** Transições permitidas. Cancelado é definitivo; concluído pode ser reaberto (correção de engano). */
    public boolean podeMudarPara(StatusPedido novo) {
        return switch (this) {
            case PENDENTE -> Set.of(CONCLUIDO, CANCELADO).contains(novo);
            case CONCLUIDO -> novo == PENDENTE;
            case CANCELADO -> false;
        };
    }
}
