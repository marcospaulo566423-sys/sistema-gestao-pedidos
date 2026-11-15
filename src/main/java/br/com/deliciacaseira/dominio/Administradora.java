package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Administradora (UML: herda de Pessoa). A senha NUNCA é guardada em texto puro:
 * somente o hash BCrypt fica no banco e não existe getter da senha original (corrige S1/S2).
 */
@Entity
@Table(name = "administradora")
public class Administradora extends Pessoa {

    public static final int TAMANHO_MINIMO_SENHA = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String usuario;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Administradora() { }

    public Administradora(String nome, String usuario, String senha, PasswordEncoder encoder, LocalDateTime agora) {
        super(nome);
        this.usuario = Textos.obrigatorio(usuario, "Usuário", 50).toLowerCase(Locale.ROOT);
        definirSenha(senha, encoder);
        this.criadoEm = agora;
    }

    /** Política mínima de senha (corrige S3): 8+ caracteres, com letras e números. */
    public void definirSenha(String senha, PasswordEncoder encoder) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA
                || senha.chars().noneMatch(Character::isLetter) || senha.chars().noneMatch(Character::isDigit))
            throw new RegraNegocioException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA
                    + " caracteres, com letras e números.");
        this.senhaHash = encoder.encode(senha);
    }

    /** UML: realizarLogin(usuario, senha). A comparação usa BCrypt (tempo constante). */
    public boolean realizarLogin(String usuario, String senha, PasswordEncoder encoder) {
        return usuario != null && senha != null
                && this.usuario.equalsIgnoreCase(usuario.strip())
                && encoder.matches(senha, senhaHash);
    }

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    /** Hash BCrypt (não é a senha). Usado apenas pelo Spring Security. */
    public String getSenhaHash() { return senhaHash; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
