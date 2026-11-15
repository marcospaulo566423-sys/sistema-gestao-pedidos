package br.com.deliciacaseira.config;

import br.com.deliciacaseira.dominio.Administradora;
import br.com.deliciacaseira.repositorio.AdministradoraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Cria a conta da administradora na primeira execução a partir de variáveis de ambiente
 * (ADMIN_USUARIO / ADMIN_SENHA) — nenhuma senha fica escrita no código (corrige S4).
 * Com ADMIN_REDEFINIR_SENHA=true, a senha da conta existente é trocada pela de ADMIN_SENHA.
 */
@Component
public class AdministradoraInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradoraInicial.class);

    private final AdministradoraRepository repositorio;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String nome;
    private final String usuario;
    private final String senha;
    private final boolean redefinirSenha;

    public AdministradoraInicial(AdministradoraRepository repositorio, PasswordEncoder encoder, Clock clock,
                                 @Value("${app.admin.nome}") String nome,
                                 @Value("${app.admin.usuario}") String usuario,
                                 @Value("${app.admin.senha:}") String senha,
                                 @Value("${app.admin.redefinir-senha:false}") boolean redefinirSenha) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.clock = clock;
        this.nome = nome;
        this.usuario = usuario;
        this.senha = senha;
        this.redefinirSenha = redefinirSenha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existente = repositorio.findByUsuarioIgnoreCase(usuario);
        if (existente.isPresent()) {
            if (redefinirSenha && !senha.isBlank()) {
                existente.get().definirSenha(senha, encoder);
                log.warn("Senha da administradora '{}' redefinida. Remova ADMIN_REDEFINIR_SENHA agora.", usuario);
            }
            return;
        }
        if (repositorio.count() > 0) return;
        if (senha.isBlank()) {
            log.warn("Nenhuma administradora cadastrada. Defina a variável ADMIN_SENHA "
                    + "(mínimo 8 caracteres, com letras e números) e reinicie o sistema.");
            return;
        }
        repositorio.save(new Administradora(nome, usuario, senha, encoder, LocalDateTime.now(clock)));
        log.info("Administradora '{}' criada.", usuario);
    }
}
