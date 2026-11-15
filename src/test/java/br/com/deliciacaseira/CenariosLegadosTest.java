package br.com.deliciacaseira;

import br.com.deliciacaseira.dominio.Administradora;
import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dominio.UnidadeMedida;
import br.com.deliciacaseira.dto.CadastroDto.ClienteVisao;
import br.com.deliciacaseira.dto.CadastroDto.EstoqueEntrada;
import br.com.deliciacaseira.dto.CadastroDto.EstoqueVisao;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoVisao;
import br.com.deliciacaseira.dto.PedidoDto.AdminEntrada;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import br.com.deliciacaseira.dto.PedidoDto.PagamentoEntrada;
import br.com.deliciacaseira.dto.RelatorioDto.EntradaFinanceira;
import br.com.deliciacaseira.dto.RelatorioDto.RelatorioFinanceiro;
import br.com.deliciacaseira.repositorio.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os 32 cenários do antigo Principal.java (projeto NetBeans), convertidos em JUnit.
 * Os cenários de estoque foram reescritos conforme a decisão 3: o estoque agora é de
 * INSUMOS, com controle manual — pedidos não baixam estoque e nunca são bloqueados por ele.
 */
class CenariosLegadosTest extends TesteIntegracao {

    @Autowired PasswordEncoder encoder;
    @Autowired PedidoRepository pedidoRepository;

    private ProdutoVisao coco;
    private ProdutoVisao canela;
    private ClienteVisao maria;

    @BeforeEach
    void preparar() {
        coco = produto("Pão de coco", "45.00");
        canela = produto("Pão de canela", "2.50");
        maria = cliente("Maria Oliveira");
    }

    private AdminVisao pedidoDaMaria(int qtdCoco, int qtdCanela) {
        return pedido(maria, hoje().plusDays(1), item(coco, qtdCoco), item(canela, qtdCanela));
    }

    private EstoqueVisao insumo(String nome, String atual, String minimo) {
        return estoqueService.cadastrarItemEstoque(
                new EstoqueEntrada(nome, new BigDecimal(atual), UnidadeMedida.KG, new BigDecimal(minimo)));
    }

    private BigDecimal saldo(Long id) {
        return estoqueService.consultarEstoque().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow()
                .quantidadeAtual();
    }

    @Test @DisplayName("[01] Login da administradora com credenciais corretas (BCrypt)")
    void c01() {
        Administradora a = new Administradora("Elaine", "elaine", "doce1234", encoder, LocalDateTime.now());
        assertTrue(a.realizarLogin("elaine", "doce1234", encoder));
    }

    @Test @DisplayName("[02] Login rejeita senha incorreta")
    void c02() {
        Administradora a = new Administradora("Elaine", "elaine", "doce1234", encoder, LocalDateTime.now());
        assertFalse(a.realizarLogin("elaine", "errada99", encoder));
    }

    @Test @DisplayName("[03] Cadastro de produto (Pão de coco)")
    void c03() { assertNotNull(coco.id()); assertEquals(new BigDecimal("45.00"), coco.preco()); }

    @Test @DisplayName("[04] Cadastro de produto (Pão de canela)")
    void c04() { assertNotNull(canela.id()); }

    @Test @DisplayName("[05] Não permite produto duplicado (mesmo nome, ignorando maiúsculas)")
    void c05() {
        assertThrows(RegraNegocioException.class,
                () -> produtoService.criar(new ProdutoEntrada("PÃO DE COCO", new BigDecimal("50"), "Duplicado", true)));
    }

    @Test @DisplayName("[06] Cadastro de insumo no estoque (Farinha de trigo)")
    void c06() { assertNotNull(insumo("Farinha de trigo", "8", "2").id()); }

    @Test @DisplayName("[07] Cadastro de insumo no estoque (Açúcar)")
    void c07() { assertNotNull(insumo("Açúcar", "30", "10").id()); }

    @Test @DisplayName("[08] Consulta de estoque")
    void c08() {
        insumo("Farinha de trigo", "8", "2");
        assertTrue(estoqueService.consultarEstoque().stream().anyMatch(e -> e.nome().equals("Farinha de trigo")));
    }

    @Test @DisplayName("[09] Estoque acima do mínimo não gera alerta de compra")
    void c09() {
        EstoqueVisao farinha = insumo("Farinha de trigo", "8", "2");
        assertFalse(farinha.precisaComprar());
        assertTrue(estoqueService.listarItensParaCompra().isEmpty());
    }

    @Test @DisplayName("[10] Subtotal do item = quantidade × preço unitário")
    void c10() { assertEquals(new BigDecimal("45.00"), pedidoDaMaria(1, 6).itens().getFirst().subtotal()); }

    @Test @DisplayName("[11] Total do pedido = soma dos subtotais")
    void c11() { assertEquals(new BigDecimal("60.00"), pedidoDaMaria(1, 6).total()); }

    @Test @DisplayName("[12] Cadastro de pedido (sem baixa automática de estoque — decisão 3)")
    void c12() {
        EstoqueVisao farinha = insumo("Pão de coco", "8", "2"); // mesmo nome do produto, de propósito
        AdminVisao p = pedidoDaMaria(1, 6);
        assertNotNull(p.id());
        assertEquals(StatusPedido.PENDENTE, p.status());
        assertEquals(0, new BigDecimal("8").compareTo(saldo(farinha.id())));
    }

    @Test @DisplayName("[13] Estoque de insumos só muda manualmente (atualizarQuantidade)")
    void c13() {
        EstoqueVisao farinha = insumo("Farinha de trigo", "8", "2");
        pedidoDaMaria(3, 1);
        assertEquals(0, new BigDecimal("8").compareTo(saldo(farinha.id())));
        estoqueService.atualizarQuantidade(farinha.id(), new BigDecimal("7"));
        assertEquals(0, new BigDecimal("7").compareTo(saldo(farinha.id())));
    }

    @Test @DisplayName("[14] Pedido NÃO é bloqueado por estoque; produtos repetidos são somados")
    void c14() {
        insumo("Pão de canela", "1", "0");
        AdminVisao p = pedido(maria, hoje().plusDays(1), item(canela, 13), item(canela, 13));
        assertEquals(1, p.itens().size());
        assertEquals(26, p.itens().getFirst().quantidade());
    }

    @Test @DisplayName("[15] Registro do pagamento")
    void c15() {
        AdminVisao p = pedidoDaMaria(1, 6);
        AdminVisao pago = pedidoService.registrarPagamento(p.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null));
        assertEquals(FormaPagamento.PIX, pago.pagamento().tipo());
        assertEquals(new BigDecimal("60.00"), pago.pagamento().valor());
    }

    @Test @DisplayName("[16] Atualização de status do pedido (Pendente → Concluído)")
    void c16() {
        AdminVisao p = pedidoDaMaria(1, 6);
        assertEquals(StatusPedido.CONCLUIDO, pedidoService.alterarStatus(p.id(), StatusPedido.CONCLUIDO).status());
    }

    @Test @DisplayName("[17] Pesquisa de pedido por nome do cliente")
    void c17() {
        pedidoDaMaria(1, 6);
        assertEquals(1, pedidoService.pesquisarPedido("Maria").size());
    }

    @Test @DisplayName("[18] Cardápio público exibe apenas produtos ativos, com descrição")
    void c18() {
        produtoService.criar(new ProdutoEntrada("Pão sem recheio", new BigDecimal("8"), "Tradicional", false));
        var cardapio = produtoService.listarCardapio();
        assertTrue(cardapio.stream().anyMatch(p -> p.nome().equals("Pão de coco") && p.descricao() != null));
        assertTrue(cardapio.stream().noneMatch(p -> p.nome().equals("Pão sem recheio")));
    }

    @Test @DisplayName("[19] Lista de produtos mais vendidos")
    void c19() {
        pedidoDaMaria(1, 6);
        var lista = relatorioService.listarMaisVendidos(Periodo.TODOS, null).produtos();
        assertEquals(2, lista.size());
        assertEquals("Pão de canela", lista.getFirst().produtoNome());
    }

    @Test @DisplayName("[20] Registro de entrada financeira")
    void c20() {
        var r = financeiroService.registrarEntradaFinanceira(
                new EntradaFinanceira(new BigDecimal("60.00"), hoje(), "Recebimento avulso"));
        assertNotNull(r.id());
        assertFalse(r.automatico());
    }

    @Test @DisplayName("[21] Histórico financeiro contém o registro")
    void c21() {
        financeiroService.registrarEntradaFinanceira(new EntradaFinanceira(new BigDecimal("60.00"), hoje(), "Recebimento"));
        RelatorioFinanceiro h = financeiroService.relatorio(Periodo.TODOS, null);
        assertTrue(h.registros().stream().anyMatch(r -> r.descricao().equals("Recebimento")));
    }

    @Test @DisplayName("[22] Status inválido é rejeitado (ex.: EM_PRODUCAO não existe mais)")
    @WithMockUser(roles = "ADMIN")
    void c22() throws Exception {
        AdminVisao p = pedidoDaMaria(1, 6);
        mvc.perform(patch("/api/admin/pedidos/" + p.id() + "/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_PRODUCAO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("[23] Pedido sem itens não é cadastrado")
    void c23() {
        AdminEntrada vazio = new AdminEntrada(maria.id(), br.com.deliciacaseira.dominio.OrigemPedido.WHATSAPP, null, null,
                hoje().plusDays(1), null, TipoEntrega.RETIRADA, null, null, null, List.of());
        assertThrows(RegraNegocioException.class, () -> pedidoService.cadastrarPedido(vazio));
    }

    @Test @DisplayName("[24] Exclusão de pedido")
    void c24() {
        AdminVisao p = pedidoDaMaria(1, 6);
        pedidoService.excluirPedido(p.id());
        assertFalse(pedidoRepository.existsById(p.id()));
    }

    @Test @DisplayName("[25] Exclusão de pedido não altera o estoque de insumos")
    void c25() {
        EstoqueVisao farinha = insumo("Farinha de trigo", "8", "2");
        AdminVisao p = pedidoDaMaria(1, 6);
        pedidoService.excluirPedido(p.id());
        assertEquals(0, new BigDecimal("8").compareTo(saldo(farinha.id())));
    }

    @Test @DisplayName("[26] Pedido excluído não aparece na pesquisa")
    void c26() {
        AdminVisao p = pedidoDaMaria(1, 6);
        pedidoService.excluirPedido(p.id());
        sincronizar();
        assertTrue(pedidoService.pesquisarPedido(p.codigo()).isEmpty());
        assertTrue(pedidoService.pesquisarPedido("Maria").isEmpty());
    }

    @Test @DisplayName("[27] Cadastro do pedido para teste de edição")
    void c27() { assertNotNull(pedido(maria, hoje().plusDays(1), item(coco, 1)).id()); }

    @Test @DisplayName("[28] Edição do pedido")
    void c28() {
        AdminVisao p = pedido(maria, hoje().plusDays(1), item(coco, 1));
        AdminVisao editado = pedidoService.editarPedido(p.id(),
                entrada(maria, hoje().plusDays(2), null, TipoEntrega.RETIRADA, null, item(coco, 2)));
        assertEquals(2, editado.itens().getFirst().quantidade());
        assertEquals(hoje().plusDays(2), editado.dataEntregaRetirada());
    }

    @Test @DisplayName("[29] Edição recalcula o total e preserva o preço original da venda")
    void c29() {
        AdminVisao p = pedido(maria, hoje().plusDays(1), item(coco, 1));
        produtoService.atualizar(coco.id(), new ProdutoEntrada("Pão de coco", new BigDecimal("50.00"), null, true));
        AdminVisao editado = pedidoService.editarPedido(p.id(),
                entrada(maria, hoje().plusDays(1), null, TipoEntrega.RETIRADA, null, item(coco, 2)));
        assertEquals(new BigDecimal("90.00"), editado.total());
    }

    @Test @DisplayName("[30] Exclusão do pedido editado")
    void c30() {
        AdminVisao p = pedido(maria, hoje().plusDays(1), item(coco, 1));
        pedidoService.editarPedido(p.id(), entrada(maria, hoje().plusDays(1), null, TipoEntrega.RETIRADA, null, item(coco, 2)));
        pedidoService.excluirPedido(p.id());
        assertFalse(pedidoRepository.existsById(p.id()));
    }

    @Test @DisplayName("[31] Cliente não fica com pedidos fantasmas após recusa, edição e exclusão (B1/B2)")
    void c31() {
        assertThrows(RegraNegocioException.class, () -> pedidoService.cadastrarPedido(new AdminEntrada(maria.id(),
                br.com.deliciacaseira.dominio.OrigemPedido.WHATSAPP, null, null, hoje().plusDays(1), null,
                TipoEntrega.RETIRADA, null, null, null, List.of())));
        AdminVisao p = pedido(maria, hoje().plusDays(1), item(coco, 1));
        pedidoService.editarPedido(p.id(), entrada(maria, hoje().plusDays(1), null, TipoEntrega.RETIRADA, null, item(coco, 2)));
        sincronizar();
        assertEquals(1, clienteService.buscar(maria.id()).quantidadePedidos());
        pedidoService.excluirPedido(p.id());
        sincronizar();
        assertEquals(0, clienteService.buscar(maria.id()).quantidadePedidos());
    }

    @Test @DisplayName("[32] Alerta de compra quando o estoque atinge o mínimo")
    void c32() {
        EstoqueVisao farinha = insumo("Farinha de trigo", "8", "2");
        EstoqueVisao atualizado = estoqueService.atualizarQuantidade(farinha.id(), new BigDecimal("2"));
        assertTrue(atualizado.precisaComprar());
        assertEquals(1, estoqueService.listarItensParaCompra().size());
    }
}
