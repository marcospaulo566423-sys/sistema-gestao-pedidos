package br.com.deliciacaseira;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Pedido {
    private int id;
    private LocalDate data;
    private String horario;
    private LocalDate dataEntregaPrevista;
    private String status;
    private String tipoEntrega;
    private String observacoes;
    private final String STATUS_PENDENTE = "PENDENTE";
    private final String STATUS_EM_PRODUCAO = "EM_PRODUCAO";
    private final String STATUS_PRONTO = "PRONTO";
    private final String STATUS_ENTREGUE = "ENTREGUE";
    private final String STATUS_CANCELADO = "CANCELADO";
    private Cliente cliente;
    private final List<ItemPedido> itens = new ArrayList<>();
    private Pagamento pagamento;

    public Pedido(int id, Cliente cliente, LocalDate data, String horario, LocalDate dataEntregaPrevista, String tipoEntrega, String observacoes) {
        setId(id); setCliente(cliente); setData(data); setHorario(horario); setDataEntregaPrevista(dataEntregaPrevista);
        setTipoEntrega(tipoEntrega); setObservacoes(observacoes); this.status = STATUS_PENDENTE;
    }
    public Pedido(int id, Cliente cliente, LocalDate data, String horario, LocalDate dataEntregaPrevista, String status, String tipoEntrega, String observacoes) {
        this(id, cliente, data, horario, dataEntregaPrevista, tipoEntrega, observacoes); atualizarStatus(status);
    }
    public int getId() { return id; }
    public void setId(int id) { if (id <= 0) throw new IllegalArgumentException("ID do pedido deve ser positivo."); this.id = id; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { if (data == null) throw new IllegalArgumentException("Data do pedido obrigatória."); this.data = data; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { if (horario == null || horario.isBlank()) throw new IllegalArgumentException("Horário obrigatório."); this.horario = horario.trim(); }
    public LocalDate getDataEntregaPrevista() { return dataEntregaPrevista; }
    public void setDataEntregaPrevista(LocalDate data) { if (data == null) throw new IllegalArgumentException("Data de entrega obrigatória."); if (this.data != null && data.isBefore(this.data)) throw new IllegalArgumentException("Entrega não pode anteceder o pedido."); this.dataEntregaPrevista = data; }
    public String getStatus() { return status; }
    public void setStatus(String status) { atualizarStatus(status); }
    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { if (tipoEntrega == null || tipoEntrega.isBlank()) throw new IllegalArgumentException("Tipo de entrega obrigatório."); this.tipoEntrega = tipoEntrega.trim(); }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes == null ? "" : observacoes.trim(); }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { if (cliente == null) throw new IllegalArgumentException("Cliente obrigatório."); if (this.cliente != null) this.cliente.removerPedido(this); this.cliente = cliente; cliente.adicionarPedido(this); }
    public List<ItemPedido> getItens() { return Collections.unmodifiableList(itens); }
    public Pagamento getPagamento() { return pagamento; }
    public void setPagamento(Pagamento pagamento) { this.pagamento = pagamento; }
    public String getSTATUS_PENDENTE() { return STATUS_PENDENTE; }
    public String getSTATUS_EM_PRODUCAO() { return STATUS_EM_PRODUCAO; }
    public String getSTATUS_PRONTO() { return STATUS_PRONTO; }
    public String getSTATUS_ENTREGUE() { return STATUS_ENTREGUE; }
    public String getSTATUS_CANCELADO() { return STATUS_CANCELADO; }
    public double calcularTotal() { return itens.stream().mapToDouble(ItemPedido::calcularSubtotal).sum(); }
    public void atualizarStatus(String novoStatus) {
        if (novoStatus == null) throw new IllegalArgumentException("Status obrigatório.");
        String s = novoStatus.trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of(STATUS_PENDENTE, STATUS_EM_PRODUCAO, STATUS_PRONTO, STATUS_ENTREGUE, STATUS_CANCELADO).contains(s)) throw new IllegalArgumentException("Status inválido: " + novoStatus);
        if (STATUS_CANCELADO.equals(status) && !STATUS_CANCELADO.equals(s)) throw new IllegalStateException("Pedido cancelado não pode ser reaberto.");
        if (STATUS_ENTREGUE.equals(status) && !STATUS_ENTREGUE.equals(s)) throw new IllegalStateException("Pedido entregue não pode voltar de status.");
        status = s;
    }
    public boolean adicionarItem(ItemPedido item) { if (item == null || STATUS_CANCELADO.equals(status) || STATUS_ENTREGUE.equals(status)) return false; return itens.add(item); }
    public boolean removerItem(ItemPedido item) { if (item == null || STATUS_CANCELADO.equals(status) || STATUS_ENTREGUE.equals(status)) return false; return itens.remove(item); }
    @Override public String toString() { return "Pedido #" + id + " | " + cliente.getNome() + " | " + data + " " + horario + " | " + status + " | R$ " + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", calcularTotal()); }
}
