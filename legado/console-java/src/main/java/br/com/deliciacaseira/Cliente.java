package br.com.deliciacaseira;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Cliente extends Pessoa {
    private String telefone;
    private String endereco;
    private boolean clienteFrequente;
    private final List<Pedido> pedidos = new ArrayList<>();

    public Cliente(String nome, String telefone, String endereco, boolean clienteFrequente) {
        super(nome); setTelefone(telefone); setEndereco(endereco); this.clienteFrequente = clienteFrequente;
    }
    public String getTelefone() { return telefone; }
    public final void setTelefone(String telefone) { if (telefone == null || telefone.isBlank()) throw new IllegalArgumentException("Telefone obrigatório."); this.telefone = telefone.trim(); }
    public String getEndereco() { return endereco; }
    public final void setEndereco(String endereco) { if (endereco == null || endereco.isBlank()) throw new IllegalArgumentException("Endereço obrigatório."); this.endereco = endereco.trim(); }
    public boolean isClienteFrequente() { return clienteFrequente; }
    public boolean getClienteFrequente() { return clienteFrequente; }
    public final void setClienteFrequente(boolean clienteFrequente) { this.clienteFrequente = clienteFrequente; }
    public List<Pedido> getPedidos() { return Collections.unmodifiableList(pedidos); }
    public void adicionarPedido(Pedido pedido) { if (pedido == null) throw new IllegalArgumentException("Pedido inválido."); if (!pedidos.contains(pedido)) pedidos.add(pedido); }
    public void removerPedido(Pedido pedido) { pedidos.remove(pedido); }
}
