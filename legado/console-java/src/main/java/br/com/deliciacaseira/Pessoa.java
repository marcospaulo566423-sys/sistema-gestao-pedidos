package br.com.deliciacaseira;

import java.util.Objects;

/** Classe-base das pessoas que utilizam o sistema. */
public class Pessoa {
    private String nome;

    public Pessoa(String nome) { setNome(nome); }
    public String getNome() { return nome; }
    public final void setNome(String nome) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("O nome é obrigatório.");
        this.nome = nome.trim();
    }
    @Override public String toString() { return nome; }
    @Override public boolean equals(Object o) { return o instanceof Pessoa p && nome.equalsIgnoreCase(p.nome); }
    @Override public int hashCode() { return Objects.hash(nome.toLowerCase()); }
}
