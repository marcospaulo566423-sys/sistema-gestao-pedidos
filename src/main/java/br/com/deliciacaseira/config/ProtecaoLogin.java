package br.com.deliciacaseira.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra tentativas repetidas de senha: após {@value #MAXIMO_TENTATIVAS} erros seguidos,
 * o usuário fica bloqueado por {@code BLOQUEIO} minutos.
 */
@Component
public class ProtecaoLogin {

    public static final int MAXIMO_TENTATIVAS = 5;
    public static final Duration BLOQUEIO = Duration.ofMinutes(5);
    private static final int LIMITE_REGISTROS = 10_000;

    private record Tentativas(int erros, Instant bloqueadoAte) { }

    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();
    private final Clock clock;

    public ProtecaoLogin(Clock clock) { this.clock = clock; }

    public boolean bloqueado(String usuario) {
        Tentativas t = tentativas.get(chave(usuario));
        return t != null && t.bloqueadoAte() != null && clock.instant().isBefore(t.bloqueadoAte());
    }

    public void registrarFalha(String usuario) {
        if (tentativas.size() > LIMITE_REGISTROS) tentativas.clear();
        tentativas.compute(chave(usuario), (k, t) -> {
            int erros = (t == null || (t.bloqueadoAte() != null && !clock.instant().isBefore(t.bloqueadoAte())))
                    ? 1 : t.erros() + 1;
            return new Tentativas(erros, erros >= MAXIMO_TENTATIVAS ? clock.instant().plus(BLOQUEIO) : null);
        });
    }

    public void registrarSucesso(String usuario) { tentativas.remove(chave(usuario)); }

    private static String chave(String usuario) {
        return usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
    }
}
