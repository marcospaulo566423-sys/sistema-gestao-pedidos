package br.com.deliciacaseira;

import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.CadastroDto.ClienteEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ClienteVisao;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoVisao;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import br.com.deliciacaseira.dto.PedidoDto.ItemEntrada;
import br.com.deliciacaseira.dto.PedidoDto.PagamentoEntrada;
import br.com.deliciacaseira.dto.PedidoDto.SiteEntrada;
import br.com.deliciacaseira.dto.RelatorioDto.EntradaFinanceira;
import br.com.deliciacaseira.dto.RelatorioDto.RegistroVisao;
import br.com.deliciacaseira.repositorio.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras de negócio invioláveis (CLAUDE.md §6), decisões aprovadas e bugs corrigidos da auditoria. */
class RegrasNegocioTest extends TesteIntegracao {

    @Autowired JdbcTemplate jdbc;
    @Autowired ClienteRepository clienteRepository;

    private ProdutoVisao coco;
    private ClienteVisao ana;

    @BeforeEach
    void preparar() {
        coco = produto("Pão de coco", "12.00");
        ana = cliente("Ana");
    }

    private SiteEntrada site(String telefone, LocalDate data, TipoEntrega tipo, String endereco, ItemEntrada... itens) {
        return new SiteEntrada("Cliente do Site", telefone, data, LocalTime.of(10, 0), tipo, endereco,
                FormaPagamento.PIX, "Sem lactose", List.of(itens));
    }

    @Nested
    @DisplayName("Regras 3 e 4 — endereço de entrega")
    class Endereco {

        @Test @DisplayName("Regra 3: entrega sem endereço é rejeitada (painel)")
        void entregaSemEnderecoPainel() {
            assertThrows(RegraNegocioException.class, () -> pedidoService.cadastrarPedido(
                    entrada(ana, hoje().plusDays(1), null, TipoEntrega.ENTREGA, "   ", item(coco, 1))));
        }

        @Test @DisplayName("Regra 3: entrega sem endereço é rejeitada (site) e não cadastra cliente (B1)")
        void entregaSemEnderecoSite() {
            long antes = clienteRepository.count();
            assertThrows(RegraNegocioException.class, () -> pedidoService.criarPeloSite(
                    site("31977776666", hoje().plusDays(1), TipoEntrega.ENTREGA, null, item(coco, 1))));
            assertEquals(antes, clienteRepository.count());
        }

        @Test @DisplayName("Regra 4: retirada não exige endereço, e o cliente pode não ter endereço (R3)")
        void retiradaSemEndereco() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            assertEquals(TipoEntrega.RETIRADA, p.tipoEntrega());
            assertNull(p.enderecoEntrega());
            assertNull(clienteService.buscar(ana.id()).endereco());
        }

        @Test @DisplayName("Regra 3 também é garantida pelo banco (CHECK)")
        void checkNoBanco() {
            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                    "INSERT INTO pedido (codigo, cliente_id, data_pedido, horario_pedido, data_entrega_retirada, status, "
                            + "tipo_entrega, endereco_entrega, origem, criado_em) VALUES ('CHECK001', ?, CURRENT_DATE, "
                            + "CURRENT_TIME, CURRENT_DATE, 'PENDENTE', 'ENTREGA', NULL, 'SITE', CURRENT_TIMESTAMP)", ana.id()));
        }
    }

    @Nested
    @DisplayName("Regra 7 — status Pendente, Concluído e Cancelado")
    class Status {

        @Test @DisplayName("O banco só aceita os 3 status (CHECK)")
        void checkStatus() {
            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                    "INSERT INTO pedido (codigo, cliente_id, data_pedido, horario_pedido, data_entrega_retirada, status, "
                            + "tipo_entrega, origem, criado_em) VALUES ('CHECK002', ?, CURRENT_DATE, CURRENT_TIME, "
                            + "CURRENT_DATE, 'ENTREGUE', 'RETIRADA', 'SITE', CURRENT_TIMESTAMP)", ana.id()));
        }

        @Test @DisplayName("Cancelado é definitivo")
        void canceladoDefinitivo() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.alterarStatus(p.id(), StatusPedido.CANCELADO);
            assertThrows(RegraNegocioException.class, () -> pedidoService.alterarStatus(p.id(), StatusPedido.PENDENTE));
            assertThrows(RegraNegocioException.class, () -> pedidoService.alterarStatus(p.id(), StatusPedido.CONCLUIDO));
        }

        @Test @DisplayName("Concluído pode ser reaberto, mas não cancelado diretamente (B11)")
        void concluido() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.alterarStatus(p.id(), StatusPedido.CONCLUIDO);
            assertThrows(RegraNegocioException.class, () -> pedidoService.alterarStatus(p.id(), StatusPedido.CANCELADO));
            assertEquals(StatusPedido.PENDENTE, pedidoService.alterarStatus(p.id(), StatusPedido.PENDENTE).status());
        }

        @Test @DisplayName("Só pedidos pendentes podem ser editados")
        void edicaoSomentePendente() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.alterarStatus(p.id(), StatusPedido.CONCLUIDO);
            assertThrows(RegraNegocioException.class, () -> pedidoService.editarPedido(p.id(),
                    entrada(ana, hoje().plusDays(1), null, TipoEntrega.RETIRADA, null, item(coco, 5))));
        }
    }

    @Nested
    @DisplayName("Pagamento e financeiro (decisão 6, R4, R5)")
    class Pagamento {

        @Test @DisplayName("Pagamento gera UMA entrada financeira automática")
        void geraEntradaAutomatica() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 2));
            pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.DINHEIRO, null, null));
            List<RegistroVisao> registros = financeiroService.relatorio(Periodo.TODOS, null).registros();
            assertEquals(1, registros.size());
            assertTrue(registros.getFirst().automatico());
            assertEquals(p.codigo(), registros.getFirst().pedidoCodigo());
            assertEquals(0, new BigDecimal("24.00").compareTo(registros.getFirst().valor()));
        }

        @Test @DisplayName("Pagamento duplicado é rejeitado")
        void duplicado() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null));
            assertThrows(RegraNegocioException.class,
                    () -> pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null)));
        }

        @Test @DisplayName("Valor do pagamento diferente do total é rejeitado")
        void valorDiferente() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            assertThrows(RegraNegocioException.class, () -> pedidoService.registrarPagamento(p.id(),
                    new PagamentoEntrada(FormaPagamento.PIX, new BigDecimal("5.00"), null)));
        }

        @Test @DisplayName("Pedido cancelado não recebe pagamento; pedido pago não pode ser cancelado nem excluído")
        void cancelamentoEPagamento() {
            AdminVisao cancelado = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.alterarStatus(cancelado.id(), StatusPedido.CANCELADO);
            assertThrows(RegraNegocioException.class, () -> pedidoService.registrarPagamento(cancelado.id(),
                    new PagamentoEntrada(FormaPagamento.PIX, null, null)));

            AdminVisao pago = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.registrarPagamento(pago.id(), new PagamentoEntrada(FormaPagamento.CARTAO, null, null));
            assertThrows(RegraNegocioException.class, () -> pedidoService.alterarStatus(pago.id(), StatusPedido.CANCELADO));
            assertThrows(RegraNegocioException.class, () -> pedidoService.excluirPedido(pago.id()));
        }

        @Test @DisplayName("Pedido pago não pode ter os produtos alterados")
        void pagoNaoMudaItens() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null));
            assertThrows(RegraNegocioException.class, () -> pedidoService.editarPedido(p.id(),
                    entrada(ana, hoje().plusDays(1), null, TipoEntrega.RETIRADA, null, item(coco, 3))));
        }

        @Test @DisplayName("Remover pagamento remove a entrada automática")
        void removerPagamento() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null));
            AdminVisao semPagamento = pedidoService.removerPagamento(p.id());
            assertNull(semPagamento.pagamento());
            assertTrue(financeiroService.relatorio(Periodo.TODOS, null).registros().isEmpty());
        }

        @Test @DisplayName("Entrada automática: não pode ser excluída nem ter o valor editado (RF15)")
        void entradaAutomaticaProtegida() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 1));
            pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null));
            RegistroVisao r = financeiroService.relatorio(Periodo.TODOS, null).registros().getFirst();
            assertThrows(RegraNegocioException.class, () -> financeiroService.excluir(r.id()));
            assertThrows(RegraNegocioException.class, () -> financeiroService.editarEntradaFinanceira(r.id(),
                    new EntradaFinanceira(new BigDecimal("1.00"), hoje(), "outra")));
            assertEquals("Ajustada", financeiroService.editarEntradaFinanceira(r.id(),
                    new EntradaFinanceira(r.valor(), hoje(), "Ajustada")).descricao());
        }

        @Test @DisplayName("Entrada manual avulsa pode ser editada e excluída; data futura é rejeitada")
        void entradaManual() {
            RegistroVisao r = financeiroService.registrarEntradaFinanceira(
                    new EntradaFinanceira(new BigDecimal("30"), hoje(), "Encomenda de festa"));
            assertEquals(0, new BigDecimal("35.00").compareTo(financeiroService.editarEntradaFinanceira(r.id(),
                    new EntradaFinanceira(new BigDecimal("35"), hoje(), "Encomenda de festa")).valor()));
            financeiroService.excluir(r.id());
            assertTrue(financeiroService.relatorio(Periodo.TODOS, null).registros().isEmpty());
            assertThrows(RegraNegocioException.class, () -> financeiroService.registrarEntradaFinanceira(
                    new EntradaFinanceira(BigDecimal.TEN, hoje().plusDays(1), "Futuro")));
        }
    }

    @Nested
    @DisplayName("Pedido pelo site (Regra 2)")
    class Site {

        @Test @DisplayName("Cliente faz pedido pelo site; recebe código; é reconhecido pelo telefone")
        void pedidoPeloSite() {
            var p1 = pedidoService.criarPeloSite(site("(31) 97777-1111", hoje().plusDays(1), TipoEntrega.RETIRADA, null, item(coco, 2)));
            var p2 = pedidoService.criarPeloSite(site("31977771111", hoje().plusDays(2), TipoEntrega.ENTREGA, "Rua B, 5", item(coco, 1)));
            assertEquals(8, p1.codigo().length());
            assertNotEquals(p1.codigo(), p2.codigo());
            assertEquals(StatusPedido.PENDENTE, p1.status());
            assertEquals(1, clienteRepository.findAll().stream().filter(c -> c.getTelefone().equals("31977771111")).count());
        }

        @Test @DisplayName("Data no passado é rejeitada")
        void dataPassada() {
            assertThrows(RegraNegocioException.class, () -> pedidoService.criarPeloSite(
                    site("31977772222", hoje().minusDays(1), TipoEntrega.RETIRADA, null, item(coco, 1))));
        }

        @Test @DisplayName("Produto fora do cardápio é rejeitado")
        void produtoInativo() {
            ProdutoVisao inativo = produtoService.criar(new ProdutoEntrada("Pão de goiabada", BigDecimal.TEN, null, false));
            assertThrows(RegraNegocioException.class, () -> pedidoService.criarPeloSite(
                    site("31977773333", hoje().plusDays(1), TipoEntrega.RETIRADA, null, item(inativo, 1))));
        }
    }

    @Nested
    @DisplayName("Bugs corrigidos da auditoria")
    class Bugs {

        @Test @DisplayName("B3: clientes homônimos são pessoas diferentes")
        void homonimos() {
            ClienteVisao outraAna = cliente("Ana");
            assertNotEquals(ana.id(), outraAna.id());
        }

        @Test @DisplayName("Telefone duplicado é rejeitado e telefone é normalizado")
        void telefone() {
            ClienteVisao c = clienteService.criar(new ClienteEntrada("Joana", "+55 (31) 98765-4321", null, true));
            assertEquals("31987654321", c.telefone());
            assertThrows(RegraNegocioException.class,
                    () -> clienteService.criar(new ClienteEntrada("Outra", "31 98765 4321", null, false)));
            assertThrows(RegraNegocioException.class,
                    () -> clienteService.criar(new ClienteEntrada("Curto", "1234", null, false)));
        }

        @Test @DisplayName("B4/R9: organização por data de entrega e horário real (9:00 antes de 14:00, sem horário por último)")
        void organizacao() {
            LocalDate amanha = hoje().plusDays(1);
            AdminVisao semHora = pedidoService.cadastrarPedido(entrada(ana, amanha, null, TipoEntrega.RETIRADA, null, item(coco, 1)));
            AdminVisao tarde = pedidoService.cadastrarPedido(entrada(ana, amanha, LocalTime.of(14, 0), TipoEntrega.RETIRADA, null, item(coco, 1)));
            AdminVisao cedo = pedidoService.cadastrarPedido(entrada(ana, amanha, LocalTime.of(9, 0), TipoEntrega.RETIRADA, null, item(coco, 1)));
            AdminVisao hojeP = pedidoService.cadastrarPedido(entrada(ana, hoje(), LocalTime.of(18, 0), TipoEntrega.RETIRADA, null, item(coco, 1)));
            List<Long> ordem = pedidoService.listar(null, null, null, null, null).stream().map(AdminVisao::id).toList();
            assertEquals(List.of(hojeP.id(), cedo.id(), tarde.id(), semHora.id()), ordem);
        }

        @Test @DisplayName("B5: entrega antes da data do pedido é rejeitada")
        void entregaAntesDoPedido() {
            assertThrows(RegraNegocioException.class, () -> pedidoService.cadastrarPedido(
                    entrada(ana, hoje().minusDays(1), null, TipoEntrega.RETIRADA, null, item(coco, 1))));
        }

        @Test @DisplayName("B6: mudar o preço do produto não altera pedidos já feitos")
        void precoCongelado() {
            AdminVisao p = pedido(ana, hoje().plusDays(1), item(coco, 2));
            produtoService.atualizar(coco.id(), new ProdutoEntrada("Pão de coco", new BigDecimal("99.00"), null, true));
            sincronizar();
            assertEquals(new BigDecimal("24.00"), pedidoService.detalhar(p.id()).total());
        }

        @Test @DisplayName("B9: pesquisa por data no formato brasileiro e sem falsos positivos por número")
        void pesquisaPorData() {
            LocalDate dia = hoje().plusDays(3);
            pedido(ana, dia, item(coco, 1));
            pedido(cliente("Bruno"), hoje().plusDays(4), item(coco, 1));
            assertEquals(1, pedidoService.pesquisarPedido(dia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).size());
            assertEquals(1, pedidoService.pesquisarPedido(dia.toString()).size());
            assertTrue(pedidoService.pesquisarPedido("1").isEmpty());
        }

        @Test @DisplayName("Produto usado em pedidos não pode ser excluído (preserva histórico)")
        void produtoEmUso() {
            pedido(ana, hoje().plusDays(1), item(coco, 1));
            assertThrows(RegraNegocioException.class, () -> produtoService.excluir(coco.id()));
        }

        @Test @DisplayName("Cliente com pedidos não pode ser excluído")
        void clienteComPedidos() {
            pedido(ana, hoje().plusDays(1), item(coco, 1));
            assertThrows(RegraNegocioException.class, () -> clienteService.excluir(ana.id()));
        }
    }
}
