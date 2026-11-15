package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
public class Cliente extends Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String telefone;

    @Column(length = 255)
    private String endereco;

    @Column(name = "cliente_frequente", nullable = false)
    private boolean clienteFrequente;

    @Column(unique = true, length = 50)
    private String usuario;

    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Cliente() { }

    public Cliente(String nome, String telefone, String endereco, boolean clienteFrequente, LocalDateTime agora) {
        super(nome);
        setTelefone(telefone);
        setEndereco(endereco);
        this.clienteFrequente = clienteFrequente;
        this.criadoEm = agora;
    }

    public Long getId() { return id; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = Textos.telefone(telefone); }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = Textos.opcional(endereco, "Endereço", 255); }
    public boolean isClienteFrequente() { return clienteFrequente; }
    public void setClienteFrequente(boolean clienteFrequente) { this.clienteFrequente = clienteFrequente; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            this.usuario = null;
            return;
        }
        String limpo = usuario.strip().toLowerCase();
        if (!limpo.matches("^[a-z0-9._-]{3,50}$")) {
            throw new RegraNegocioException("O usuário deve ter entre 3 e 50 caracteres (letras, números, ponto ou traço).");
        }
        this.usuario = limpo;
    }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}