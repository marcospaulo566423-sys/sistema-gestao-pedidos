package br.com.deliciacaseira.servico;

import br.com.deliciacaseira.dominio.Pedido;
import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.ItemProducao;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import br.com.deliciacaseira.dto.RelatorioDto.Lembretes;
import br.com.deliciacaseira.dto.RelatorioDto.MaisVendidos;
import br.com.deliciacaseira.dto.RelatorioDto.Painel;
import br.com.deliciacaseira.dto.RelatorioDto.ProducaoDiaria;
import br.com.deliciacaseira.dto.RelatorioDto.RelatorioPedidos;
import br.com.deliciacaseira.repositorio.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Produção diária, lembretes, mais vendidos e relatórios de pedidos (RF05, RF10, RF12, RF19, RF21). */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final PedidoRepository pedidos;
    private final PedidoService pedidoService;
    private final EstoqueService estoque;
    private final Clock clock;

    public RelatorioService(PedidoRepository pedidos, PedidoService pedidoService, EstoqueService estoque, Clock clock) {
        this.pedidos = pedidos;
        this.pedidoService = pedidoService;
        this.estoque = estoque;
        this.clock = clock;
    }

    /**
     * UML: gerarListaDiariaProducao() — RF05 / Regra 8. Usa os pedidos cadastrados (não cancelados)
     * com entrega/retirada na data informada.
     */
    public ProducaoDiaria gerarListaDiariaProducao(LocalDate data) {
        LocalDate dia = data == null ? hoje() : data;
        List<ItemProducao> itens = pedidos.producaoDoDia(dia);
        long total = itens.stream().mapToLong(ItemProducao::quantidade).sum();
        List<AdminVisao> lista = pedidoService.buscarEntidades(null, null, dia, dia, null).stream()
                .filter(p -> p.getStatus() != StatusPedido.CANCELADO).map(AdminVisao::de).toList();
        return new ProducaoDiaria(dia, itens, total, lista);
    }

    /** UML: exibirLembretes() — RF21: pedidos PENDENTES do dia seguinte (corrige R8). */
    public Lembretes exibirLembretes() {
        LocalDate amanha = hoje().plusDays(1);
        return new Lembretes(amanha, pedidoService.listar(StatusPedido.PENDENTE, null, amanha, amanha, null));
    }

    /** UML: listarMaisVendidos(periodo) — RF10: filtra de fato pelo período e ignora cancelados (corrige R10). */
    public MaisVendidos listarMaisVendidos(Periodo periodo, LocalDate referencia) {
        Periodo p = periodo == null ? Periodo.MES : periodo;
        Periodo.Intervalo i = p.intervalo(referencia == null ? hoje() : referencia);
        boolean todos = p == Periodo.TODOS;
        return new MaisVendidos(p, todos ? null : i.inicio(), todos ? null : i.fim(),
                pedidos.maisVendidos(i.inicio(), i.fim()));
    }

    /**
     * UML: gerarRelatorioPedidos() — RF12 e RF19. Totais em dinheiro consideram apenas pedidos não
     * cancelados (corrige R13).
     */
    public RelatorioPedidos gerarRelatorioPedidos(Periodo periodo, LocalDate referencia, StatusPedido status) {
        Periodo p = periodo == null ? Periodo.MES : periodo;
        Periodo.Intervalo i = p.intervalo(referencia == null ? hoje() : referencia);
        List<Pedido> lista = pedidoService.buscarEntidades(status, null, i.inicio(), i.fim(), null);

        Map<StatusPedido, Long> porStatus = new EnumMap<>(StatusPedido.class);
        for (StatusPedido s : StatusPedido.values()) porStatus.put(s, 0L);
        lista.forEach(ped -> porStatus.merge(ped.getStatus(), 1L, Long::sum));

        List<Pedido> validos = lista.stream().filter(ped -> ped.getStatus() != StatusPedido.CANCELADO).toList();
        long retiradas = validos.stream().filter(ped -> ped.getTipoEntrega() == TipoEntrega.RETIRADA).count();
        long entregas = validos.stream().filter(ped -> ped.getTipoEntrega() == TipoEntrega.ENTREGA).count();
        BigDecimal valor = validos.stream().map(Pedido::calcularTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean todos = p == Periodo.TODOS;
        return new RelatorioPedidos(p, todos ? null : i.inicio(), todos ? null : i.fim(),
                lista.stream().map(AdminVisao::de).toList(), porStatus, retiradas, entregas, valor);
    }

    /** Tela inicial do painel: o que a Elaine precisa ver primeiro. */
    public Painel painel() {
        LocalDate hoje = hoje();
        long pendentesHoje = pedidoService.buscarEntidades(StatusPedido.PENDENTE, null, hoje, hoje, null).size();
        long pendentesTotal = pedidoService.buscarEntidades(StatusPedido.PENDENTE, null, null, null, null).size();
        return new Painel(hoje, pendentesHoje, pendentesTotal, exibirLembretes(),
                pedidos.producaoDoDia(hoje), estoque.listarItensParaCompra());
    }

    private LocalDate hoje() { return LocalDate.now(clock); }
}
