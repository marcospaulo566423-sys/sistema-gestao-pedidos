package br.com.deliciacaseira.dto;

import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.ItemPedido;
import br.com.deliciacaseira.dominio.OrigemPedido;
import br.com.deliciacaseira.dominio.Pagamento;
import br.com.deliciacaseira.dominio.Pedido;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Dados de entrada e saída de pedidos. */
public final class PedidoDto {
    private PedidoDto() { }

    public record ItemEntrada(
            @NotNull(message = "Escolha o produto.") Long produtoId,
            @NotNull(message = "Informe a quantidade.")
            @Min(value = 1, message = "A quantidade mínima é 1.")
            @Max(value = ItemPedido.QUANTIDADE_MAXIMA, message = "A quantidade máxima é 999.") Integer quantidade) { }

    /** Pedido feito pelo cliente no site (Regra 2). */
    public record SiteEntrada(
            @NotBlank(message = "Informe seu nome.") @Size(max = 100) String nome,
            @NotBlank(message = "Informe seu telefone/WhatsApp.") @Size(max = 30) String telefone,
            @NotNull(message = "Escolha a data desejada.") LocalDate dataEntregaRetirada,
            LocalTime horarioEntregaRetirada,
            @NotNull(message = "Escolha Retirada ou Entrega.") TipoEntrega tipoEntrega,
            @Size(max = 255) String enderecoEntrega,
            @NotNull(message = "Escolha a forma de pagamento.") FormaPagamento formaPagamento,
            @Size(max = 500) String observacoes,
            @NotEmpty(message = "Escolha pelo menos um produto.") @Size(max = Pedido.MAXIMO_ITENS)
            List<@Valid @NotNull ItemEntrada> itens) { }

    /** Pedido registrado pela administradora (RF02 / RF13: WhatsApp, Presencial ou Site). */
    public record AdminEntrada(
            @NotNull(message = "Escolha o cliente.") Long clienteId,
            @NotNull(message = "Informe a origem do pedido.") OrigemPedido origem,
            LocalDate dataPedido,
            LocalTime horarioPedido,
            @NotNull(message = "Informe a data de entrega ou retirada.") LocalDate dataEntregaRetirada,
            LocalTime horarioEntregaRetirada,
            @NotNull(message = "Escolha Retirada ou Entrega.") TipoEntrega tipoEntrega,
            @Size(max = 255) String enderecoEntrega,
            FormaPagamento formaPagamento,
            @Size(max = 500) String observacoes,
            @NotEmpty(message = "Escolha pelo menos um produto.") @Size(max = Pedido.MAXIMO_ITENS)
            List<@Valid @NotNull ItemEntrada> itens) { }

    /** "Meus Pedidos": o cliente informa o código do pedido e o telefone. */
    public record ConsultaEntrada(
            @NotBlank(message = "Informe o código do pedido.") @Size(max = 20) String codigo,
            @NotBlank(message = "Informe o telefone usado no pedido.") @Size(max = 30) String telefone) { }

    public record StatusEntrada(@NotNull(message = "Informe o novo status.") StatusPedido status) { }

    public record PagamentoEntrada(
            @NotNull(message = "Escolha a forma de pagamento.") FormaPagamento tipo,
            BigDecimal valor,
            LocalDate data) { }

    public record ItemVisao(Long produtoId, String produtoNome, int quantidade,
                            BigDecimal precoUnitario, BigDecimal subtotal) {
        static ItemVisao de(ItemPedido i) {
            return new ItemVisao(i.getProduto().getId(), i.getProduto().getNome(), i.getQuantidade(),
                    i.getPrecoUnitario(), i.calcularSubtotal());
        }
    }

    public record PagamentoVisao(FormaPagamento tipo, BigDecimal valor, LocalDate data) {
        static PagamentoVisao de(Pagamento p) {
            return p == null ? null : new PagamentoVisao(p.getTipo(), p.getValor(), p.getDataPagamento());
        }
    }

    public record ClienteResumo(Long id, String nome, String telefone) { }

    /** Visão completa — somente na área administrativa. */
    public record AdminVisao(
            Long id, String codigo, ClienteResumo cliente,
            LocalDate dataPedido, LocalTime horarioPedido,
            LocalDate dataEntregaRetirada, LocalTime horarioEntregaRetirada,
            StatusPedido status, TipoEntrega tipoEntrega, String enderecoEntrega,
            FormaPagamento formaPagamento, OrigemPedido origem, String observacoes,
            List<ItemVisao> itens, BigDecimal total, PagamentoVisao pagamento) {

        public static AdminVisao de(Pedido p) {
            return new AdminVisao(p.getId(), p.getCodigo(),
                    new ClienteResumo(p.getCliente().getId(), p.getCliente().getNome(), p.getCliente().getTelefone()),
                    p.getDataPedido(), p.getHorarioPedido(), p.getDataEntregaRetirada(), p.getHorarioEntregaRetirada(),
                    p.getStatus(), p.getTipoEntrega(), p.getEnderecoEntrega(), p.getFormaPagamento(), p.getOrigem(),
                    p.getObservacoes(), p.getItens().stream().map(ItemVisao::de).toList(), p.calcularTotal(),
                    PagamentoVisao.de(p.getPagamento()));
        }
    }

    /**
     * Visão pública — o próprio cliente vê o seu pedido. Não contém ids internos,
     * telefone, origem, observações internas nem dados do financeiro (Regra 6).
     */
    public record PublicoVisao(
            String codigo, String nomeCliente, LocalDate dataPedido,
            LocalDate dataEntregaRetirada, LocalTime horarioEntregaRetirada,
            StatusPedido status, TipoEntrega tipoEntrega, String enderecoEntrega,
            FormaPagamento formaPagamento, List<ItemVisao> itens, BigDecimal total, boolean pagamentoRegistrado) {

        public static PublicoVisao de(Pedido p) {
            return new PublicoVisao(p.getCodigo(), p.getCliente().getNome(), p.getDataPedido(),
                    p.getDataEntregaRetirada(), p.getHorarioEntregaRetirada(), p.getStatus(), p.getTipoEntrega(),
                    p.getEnderecoEntrega(), p.getFormaPagamento(),
                    p.getItens().stream().map(ItemVisao::de).toList(), p.calcularTotal(), p.isPago());
        }
    }
}
