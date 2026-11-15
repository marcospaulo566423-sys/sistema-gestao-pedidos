package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Cliente (UML: herda de Pessoa). O endereço é OPCIONAL (corrige R3 / Regra 4);
 * o endereço de cada entrega fica no próprio Pedido.
 */
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
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
