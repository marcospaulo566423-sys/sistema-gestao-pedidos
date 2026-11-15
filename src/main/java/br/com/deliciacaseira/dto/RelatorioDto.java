package br.com.deliciacaseira.dto;

import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.RegistroFinanceiro;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dto.CadastroDto.EstoqueVisao;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Financeiro, produção, lembretes e relatórios — somente área administrativa (Regra 6). */
public final class RelatorioDto {
    private RelatorioDto() { }

    // ----------------------------------------------------------------- financeiro (RF08, RF09, RF15, RF20)

    public record EntradaFinanceira(
            @NotNull(message = "Informe o valor.") BigDecimal valor,
            @NotNull(message = "Informe a data.") LocalDate data,
            @NotBlank(message = "Informe a descrição.") @Size(max = 200) String descricao) { }

    public record RegistroVisao(Long id, BigDecimal valor, LocalDate data, String descricao,
                                boolean automatico, String pedidoCodigo, FormaPagamento formaPagamento) {
        public static RegistroVisao de(RegistroFinanceiro r) {
            var pg = r.getPagamento();
            return new RegistroVisao(r.getId(), r.getValor(), r.getData(), r.getDescricao(), r.isAutomatico(),
                    pg == null ? null : pg.getPedido().getCodigo(), pg == null ? null : pg.getTipo());
        }
    }

    /** "totalPorForma": PIX, CARTAO, DINHEIRO e AVULSA (entradas manuais). */
    public record RelatorioFinanceiro(Periodo periodo, LocalDate inicio, LocalDate fim,
                                      List<RegistroVisao> registros, BigDecimal total,
                                      Map<String, BigDecimal> totalPorForma) { }

    // ----------------------------------------------------------------- produção e lembretes (RF05, RF21)

    public record ProducaoDiaria(LocalDate data, List<ItemProducao> itens, long totalUnidades,
                                 List<AdminVisao> pedidos) { }

    public record Lembretes(LocalDate data, List<AdminVisao> pedidos) { }

    // ----------------------------------------------------------------- relatórios (RF10, RF12, RF19)

    public record RelatorioPedidos(Periodo periodo, LocalDate inicio, LocalDate fim, List<AdminVisao> pedidos,
                                   Map<StatusPedido, Long> quantidadePorStatus, long retiradas, long entregas,
                                   BigDecimal valorTotal) { }

    public record MaisVendidos(Periodo periodo, LocalDate inicio, LocalDate fim, List<ProdutoVendido> produtos) { }

    /** Tela inicial do painel. */
    public record Painel(LocalDate hoje, long pendentesHoje, long pendentesTotal, Lembretes lembretesAmanha,
                         List<ItemProducao> producaoHoje, List<EstoqueVisao> itensParaComprar) { }
}
