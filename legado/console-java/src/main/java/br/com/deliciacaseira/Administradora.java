package br.com.deliciacaseira;

public final class Administradora extends Pessoa {
    private String usuario;
    private String senha;
    public Administradora(String nome, String usuario, String senha) { super(nome); setUsuario(usuario); setSenha(senha); }
    public String getUsuario() { return usuario; }
    public final void setUsuario(String usuario) { if (usuario == null || usuario.isBlank()) throw new IllegalArgumentException("Usuário obrigatório."); this.usuario = usuario.trim(); }
    public String getSenha() { return senha; }
    public final void setSenha(String senha) { if (senha == null || senha.length() < 4) throw new IllegalArgumentException("A senha deve possuir ao menos 4 caracteres."); this.senha = senha; }
    public boolean realizarLogin(String usuario, String senha) { return this.usuario.equals(usuario) && this.senha.equals(senha); }
}
