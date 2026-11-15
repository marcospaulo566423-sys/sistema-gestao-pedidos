package br.com.deliciacaseira;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContextoTest extends TesteIntegracao {

    @Test
    @DisplayName("Aplicação sobe: Flyway aplica o V1 e o Hibernate valida o mapeamento")
    void contextoSobe() {
        assertNotNull(pedidoService);
    }
}
