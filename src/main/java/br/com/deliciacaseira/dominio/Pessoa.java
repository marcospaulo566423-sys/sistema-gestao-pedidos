package br.com.deliciacaseira.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclasse do UML: agrupa o atributo comum a Cliente e Administradora.
 * A identidade das pessoas é o id do banco — NÃO o nome (corrige B3: homônimos são pessoas diferentes).
 */
@MappedSuperclass
public abstract class Pessoa {

    @Column(nullable = false, length = 100)
    private String nome;

    protected Pessoa() { }

    protected Pessoa(String nome) { setNome(nome); }

    public String getNome() { return nome; }

    public void setNome(String nome) { this.nome = Textos.obrigatorio(nome, "Nome", 100); }

    @Override
    public String toString() { return nome; }
}
