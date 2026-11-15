package br.com.deliciacaseira;

import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.CadastroDto.ClienteVisao;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoVisao;
import br.com.deliciacaseira.dto.ItemProducao;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import br.com.deliciacaseira.dto.PedidoDto.PagamentoEntrada;
import br.com.deliciacaseira.dto.RelatorioDto.EntradaFinanceira;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Produção diária, lembretes, mais vendidos e relatórios (RF05, RF10, RF12, RF19, RF20, RF21). */
class RelatoriosTest extends TesteIntegracao {

    private ProdutoVisao coco;
    private ProdutoVisao brigadeiro;
    private ClienteVisao ana;

    @BeforeEach
    void preparar() {
        coco = produto("Pão de coco", "10.00");
        brigadeiro = produto("Pão de brigadeiro", "15.00");
        ana = cliente("Ana");
    }

    @Test
    @DisplayName("RF05/Regra 8: lista de produção soma por produto, usa a data de entrega e ignora cancelados")
    void listaDeProducao() {
        LocalDate amanha = hoje().plusDays(1);
        pedido(ana, amanha, item(coco, 3), item(brigadeiro, 1));
        pedido(cliente("Bia"), amanha, item(coco, 2));
        AdminVisao cancelado = pedido(cliente("Caio"), amanha, item(coco, 50));
        pedidoService.alterarStatus(cancelado.id(), StatusPedido.CANCELADO);
        pedido(cliente("Duda"), hoje().plusDays(2), item(coco, 7)); // outro dia

        var producao = relatorioService.gerarListaDiariaProducao(amanha);
        assertEquals(List.of(new ItemProducao(brigadeiro.id(), "Pão de brigadeiro", 1L),
                new ItemProducao(coco.id(), "Pão de coco", 5L)), producao.itens());
        assertEquals(6, producao.totalUnidades());
        assertEquals(2, producao.pedidos().size());
    }

    @Test
    @DisplayName("RF21: lembretes mostram apenas pedidos PENDENTES do dia seguinte (R8)")
    void lembretes() {
        AdminVisao amanha = pedido(ana, hoje().plusDays(1), item(coco, 1));
        AdminVisao concluido = pedido(cliente("Bia"), hoje().plusDays(1), item(coco, 1));
        pedidoService.alterarStatus(concluido.id(), StatusPedido.CONCLUIDO);
        pedido(cliente("Caio"), hoje(), item(coco, 1));
        pedido(cliente("Duda"), hoje().plusDays(2), item(coco, 1));

        var lembretes = relatorioService.exibirLembretes();
        assertEquals(hoje().plusDays(1), lembretes.data());
        assertEquals(List.of(amanha.id()), lembretes.pedidos().stream().map(AdminVisao::id).toList());
    }

    @Test
    @DisplayName("RF10: mais vendidos respeita o período e ignora cancelados (R10)")
    void maisVendidos() {
        pedido(ana, hoje(), item(coco, 2), item(brigadeiro, 5));
        AdminVisao cancelado = pedido(cliente("Bia"), hoje(), item(coco, 100));
        pedidoService.alterarStatus(cancelado.id(), StatusPedido.CANCELADO);
        pedido(cliente("Caio"), hoje().plusYears(1), item(coco, 40)); // fora do mês atual

        var lista = relatorioService.listarMaisVendidos(Periodo.MES, hoje()).produtos();
        assertEquals("Pão de brigadeiro", lista.get(0).produtoNome());
        assertEquals(5L, lista.get(0).quantidade());
        assertEquals(2L, lista.get(1).quantidade());
        assertEquals(new BigDecimal("75.00"), lista.get(0).valorTotal());
    }

    @Test
    @DisplayName("RF20: períodos seguem o calendário (semana de segunda a domingo, mês cheio) — R11")
    void periodosDeCalendario() {
        LocalDate quarta = LocalDate.of(2026, 11, 11);
        assertEquals(new Periodo.Intervalo(quarta, quarta), Periodo.DIA.intervalo(quarta));
        assertEquals(new Periodo.Intervalo(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15)), Periodo.SEMANA.intervalo(quarta));
        assertEquals(new Periodo.Intervalo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), Periodo.MES.intervalo(quarta));
        assertEquals(new Periodo.Intervalo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), Periodo.ANO.intervalo(quarta));
        assertThrows(IllegalArgumentException.class, () -> Periodo.valueOf("QUINZENA"));
    }

    @Test
    @DisplayName("RF12/RF19: relatório de pedidos separa retirada/entrega e o total ignora cancelados (R13)")
    void relatorioDePedidos() {
        pedido(ana, hoje(), item(coco, 1));
        pedidoService.cadastrarPedido(entrada(cliente("Bia"), hoje(), null, TipoEntrega.ENTREGA, "Rua C, 1", item(brigadeiro, 2)));
        AdminVisao cancelado = pedido(cliente("Caio"), hoje(), item(coco, 10));
        pedidoService.alterarStatus(cancelado.id(), StatusPedido.CANCELADO);

        var r = relatorioService.gerarRelatorioPedidos(Periodo.DIA, hoje(), null);
        assertEquals(3, r.pedidos().size());
        assertEquals(1L, r.quantidadePorStatus().get(StatusPedido.CANCELADO));
        assertEquals(2L, r.quantidadePorStatus().get(StatusPedido.PENDENTE));
        assertEquals(1, r.retiradas());
        assertEquals(1, r.entregas());
        assertEquals(new BigDecimal("40.00"), r.valorTotal());
    }

    @Test
    @DisplayName("RF20: relatório financeiro soma entradas por forma de pagamento e avulsas")
    void relatorioFinanceiro() {
        AdminVisao p = pedido(ana, hoje(), item(coco, 2));
        pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null));
        financeiroService.registrarEntradaFinanceira(new EntradaFinanceira(new BigDecimal("5"), hoje(), "Venda avulsa"));

        var r = financeiroService.relatorio(Periodo.DIA, hoje());
        assertEquals(new BigDecimal("25.00"), r.total());
        assertEquals(0, new BigDecimal("20").compareTo(r.totalPorForma().get("PIX")));
        assertEquals(0, new BigDecimal("5").compareTo(r.totalPorForma().get("AVULSA")));
        assertTrue(financeiroService.relatorio(Periodo.DIA, hoje().minusDays(1)).registros().isEmpty());
    }

    @Test
    @DisplayName("Painel inicial reúne pendentes de hoje, lembretes de amanhã e itens para comprar")
    void painel() {
        pedido(ana, hoje(), item(coco, 1));
        pedido(cliente("Bia"), hoje().plusDays(1), item(coco, 1));
        var painel = relatorioService.painel();
        assertEquals(1, painel.pendentesHoje());
        assertEquals(2, painel.pendentesTotal());
        assertEquals(1, painel.lembretesAmanha().pedidos().size());
        assertEquals(1, painel.producaoHoje().size());
    }
}
