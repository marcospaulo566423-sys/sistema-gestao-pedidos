package br.com.deliciacaseira;

import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.OrigemPedido;
import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.CadastroDto.ClienteEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoEntrada;
import br.com.deliciacaseira.dto.PedidoDto.AdminEntrada;
import br.com.deliciacaseira.dto.PedidoDto.ItemEntrada;
import br.com.deliciacaseira.dto.PedidoDto.PagamentoEntrada;
import br.com.deliciacaseira.servico.ClienteService;
import br.com.deliciacaseira.servico.FinanceiroService;
import br.com.deliciacaseira.servico.PedidoService;
import br.com.deliciacaseira.servico.ProdutoService;
import br.com.deliciacaseira.servico.RelatorioService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Roda o sistema contra um PostgreSQL REAL (binários embutidos, sem instalação):
 * garante que o V1__schema.sql do Flyway, a validação do Hibernate e as consultas
 * JPQL funcionam no banco de produção — e não apenas no H2 dos demais testes.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostgresCompatibilidadeTest {

    private static final EmbeddedPostgres POSTGRES = iniciar();

    private static EmbeddedPostgres iniciar() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível iniciar o PostgreSQL embutido", e);
        }
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void parar() throws IOException { POSTGRES.close(); }

    @Autowired PedidoService pedidos;
    @Autowired ClienteService clientes;
    @Autowired ProdutoService produtos;
    @Autowired FinanceiroService financeiro;
    @Autowired RelatorioService relatorios;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    @Test
    @DisplayName("PostgreSQL: Flyway + Hibernate + fluxo pedido → produção → pagamento → relatório")
    void fluxoCompletoNoPostgres() {
        LocalDate amanha = LocalDate.now(clock).plusDays(1);
        var coco = produtos.criar(new ProdutoEntrada("Pão de coco PG", new BigDecimal("12.50"), null, true));
        var maria = clientes.criar(new ClienteEntrada("Maria Postgres", "(31) 98888-7777", null, true));

        var pedido = pedidos.cadastrarPedido(new AdminEntrada(maria.id(), OrigemPedido.PRESENCIAL, null, null,
                amanha, LocalTime.of(9, 0), TipoEntrega.ENTREGA, "Rua A, 10", FormaPagamento.DINHEIRO, "Sem açúcar",
                List.of(new ItemEntrada(coco.id(), 3))));
        assertEquals(new BigDecimal("37.50"), pedido.total());

        assertEquals(3L, relatorios.gerarListaDiariaProducao(amanha).totalUnidades());
        assertEquals(1, relatorios.exibirLembretes().pedidos().size());
        assertEquals(1, pedidos.pesquisarPedido("maria").size());
        assertEquals(1, relatorios.listarMaisVendidos(Periodo.TODOS, null).produtos().size());

        pedidos.registrarPagamento(pedido.id(), new PagamentoEntrada(FormaPagamento.PIX, null, null));
        pedidos.alterarStatus(pedido.id(), StatusPedido.CONCLUIDO);
        var fin = financeiro.relatorio(Periodo.DIA, null);
        assertEquals(new BigDecimal("37.50"), fin.totalPorForma().get("PIX"));
    }

    @Test
    @DisplayName("PostgreSQL: CHECK do banco impede entrega sem endereço e status fora da Regra 7")
    void restricoesDoBanco() {
        Long clienteId = jdbc.queryForObject(
                "INSERT INTO cliente (nome, telefone, cliente_frequente, criado_em) VALUES ('X', '31900000001', false, now()) RETURNING id",
                Long.class);
        String insert = "INSERT INTO pedido (codigo, cliente_id, data_pedido, horario_pedido, data_entrega_retirada, "
                + "status, tipo_entrega, endereco_entrega, origem, criado_em) "
                + "VALUES (?, ?, current_date, current_time, current_date, ?, ?, ?, 'SITE', now())";
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(insert, "PGTEST01", clienteId, "PENDENTE", "ENTREGA", null));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(insert, "PGTEST02", clienteId, "PRONTO", "RETIRADA", null));
        jdbc.update("DELETE FROM cliente WHERE id = ?", clienteId);
    }
}
