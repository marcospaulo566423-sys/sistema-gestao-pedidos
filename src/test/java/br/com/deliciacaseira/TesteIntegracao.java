package br.com.deliciacaseira;

import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.OrigemPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.CadastroDto.ClienteEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ClienteVisao;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoVisao;
import br.com.deliciacaseira.dto.PedidoDto.AdminEntrada;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import br.com.deliciacaseira.dto.PedidoDto.ItemEntrada;
import br.com.deliciacaseira.servico.ClienteService;
import br.com.deliciacaseira.servico.EstoqueService;
import br.com.deliciacaseira.servico.FinanceiroService;
import br.com.deliciacaseira.servico.PedidoService;
import br.com.deliciacaseira.servico.ProdutoService;
import br.com.deliciacaseira.servico.RelatorioService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base dos testes de integração: perfil "test" (H2 em modo PostgreSQL + o mesmo Flyway V1),
 * um único contexto Spring compartilhado e rollback automático ao fim de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class TesteIntegracao {

    @Autowired protected MockMvc mvc;
    @Autowired protected PedidoService pedidoService;
    @Autowired protected ClienteService clienteService;
    @Autowired protected ProdutoService produtoService;
    @Autowired protected EstoqueService estoqueService;
    @Autowired protected FinanceiroService financeiroService;
    @Autowired protected RelatorioService relatorioService;
    @Autowired protected EntityManager em;
    @Autowired protected Clock clock;

    private static final AtomicInteger TELEFONES = new AtomicInteger();

    protected LocalDate hoje() { return LocalDate.now(clock); }

    protected ProdutoVisao produto(String nome, String preco) {
        return produtoService.criar(new ProdutoEntrada(nome, new BigDecimal(preco), "Feito na hora", true));
    }

    protected ClienteVisao cliente(String nome) {
        return cliente(nome, String.format("3199%07d", TELEFONES.incrementAndGet()));
    }

    protected ClienteVisao cliente(String nome, String telefone) {
        return clienteService.criar(new ClienteEntrada(nome, telefone, null, false));
    }

    protected static ItemEntrada item(ProdutoVisao produto, int quantidade) {
        return new ItemEntrada(produto.id(), quantidade);
    }

    protected AdminEntrada entrada(ClienteVisao c, LocalDate entrega, LocalTime horario, TipoEntrega tipo,
                                   String endereco, ItemEntrada... itens) {
        return new AdminEntrada(c.id(), OrigemPedido.WHATSAPP, hoje(), LocalTime.of(8, 0), entrega, horario, tipo,
                endereco, FormaPagamento.PIX, null, List.of(itens));
    }

    protected AdminVisao pedido(ClienteVisao c, LocalDate entrega, ItemEntrada... itens) {
        return pedidoService.cadastrarPedido(entrada(c, entrega, null, TipoEntrega.RETIRADA, null, itens));
    }

    /** Força a gravação no banco (dispara CHECKs) e limpa o cache para reler do banco. */
    protected void sincronizar() {
        em.flush();
        em.clear();
    }
}
