package br.com.deliciacaseira.servico;

import br.com.deliciacaseira.dominio.Cliente;
import br.com.deliciacaseira.dominio.NaoEncontradoException;
import br.com.deliciacaseira.dominio.OrigemPedido;
import br.com.deliciacaseira.dominio.Pagamento;
import br.com.deliciacaseira.dominio.Pedido;
import br.com.deliciacaseira.dominio.Produto;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.Textos;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.PedidoDto.AdminEntrada;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import br.com.deliciacaseira.dto.PedidoDto.ConsultaEntrada;
import br.com.deliciacaseira.dto.PedidoDto.ItemEntrada;
import br.com.deliciacaseira.dto.PedidoDto.PagamentoEntrada;
import br.com.deliciacaseira.dto.PedidoDto.PublicoVisao;
import br.com.deliciacaseira.dto.PedidoDto.SiteEntrada;
import br.com.deliciacaseira.repositorio.PagamentoRepository;
import br.com.deliciacaseira.repositorio.PedidoRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pedidos: cadastro pelo site e pelo painel, edição, exclusão, status, pagamento,
 * organização por data e pesquisa (RF02–RF07, RF11, RF13, RF14, RF17–RF19).
 */
@Service
@Transactional
public class PedidoService {

    /** Até quantos dias à frente o cliente pode agendar pelo site. */
    public static final int DIAS_MAXIMOS_AGENDAMENTO_SITE = 90;

    private static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 8;

    /** RF04: organização pela data de entrega/retirada, depois horário (sem horário por último) e id. */
    public static final Comparator<Pedido> ORDEM_POR_ENTREGA = Comparator
            .comparing(Pedido::getDataEntregaRetirada)
            .thenComparing(Pedido::getHorarioEntregaRetirada, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Pedido::getId);

    private final PedidoRepository pedidos;
    private final PagamentoRepository pagamentos;
    private final ClienteService clientes;
    private final ProdutoService produtos;
    private final FinanceiroService financeiro;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PedidoService(PedidoRepository pedidos, PagamentoRepository pagamentos, ClienteService clientes,
                         ProdutoService produtos, FinanceiroService financeiro, Clock clock) {
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
        this.clientes = clientes;
        this.produtos = produtos;
        this.financeiro = financeiro;
        this.clock = clock;
    }

    // ================================================================= cadastro

    /** Regra 2: o cliente faz o pedido pelo site, sem login. */
    public PublicoVisao criarPeloSite(SiteEntrada e) {
        LocalDateTime agora = agora();
        LocalDate hoje = agora.toLocalDate();
        if (e.dataEntregaRetirada().isBefore(hoje))
            throw new RegraNegocioException("A data desejada não pode ser no passado.");
        if (e.dataEntregaRetirada().isAfter(hoje.plusDays(DIAS_MAXIMOS_AGENDAMENTO_SITE)))
            throw new RegraNegocioException("Pelo site, os pedidos podem ser agendados para até "
                    + DIAS_MAXIMOS_AGENDAMENTO_SITE + " dias.");
        // valida a logística ANTES de cadastrar o cliente, para não criar cadastro de pedido inválido (corrige B1)
        if (e.tipoEntrega() == TipoEntrega.ENTREGA && Textos.limpar(e.enderecoEntrega()) == null)
            throw new RegraNegocioException("Para entrega, o endereço é obrigatório.");
        List<Pedido.ItemSolicitado> itens = resolverItens(e.itens(), null);

        Cliente cliente = clientes.obterOuCriarParaPedidoSite(e.nome(), e.telefone(),
                e.tipoEntrega() == TipoEntrega.ENTREGA ? e.enderecoEntrega() : null);
        Pedido p = new Pedido(gerarCodigo(), cliente, OrigemPedido.SITE, agora);
        p.definirDatas(hoje, agora.toLocalTime(), e.dataEntregaRetirada(), e.horarioEntregaRetirada());
        p.definirLogistica(e.tipoEntrega(), e.enderecoEntrega());
        p.setFormaPagamento(e.formaPagamento());
        p.setObservacoes(e.observacoes());
        p.substituirItens(itens);
        return PublicoVisao.de(pedidos.save(p));
    }

    /** UML: cadastrarPedido(pedido) — RF02/RF13: registro manual pela administradora. */
    public AdminVisao cadastrarPedido(AdminEntrada e) {
        LocalDateTime agora = agora();
        Pedido p = new Pedido(gerarCodigo(), clientes.obter(e.clienteId()), e.origem(), agora);
        preencher(p, e, agora);
        return AdminVisao.de(pedidos.save(p));
    }

    /** UML: editarPedido(id) — somente pedidos pendentes. */
    public AdminVisao editarPedido(Long id, AdminEntrada e) {
        Pedido p = obter(id);
        if (p.getStatus() != StatusPedido.PENDENTE)
            throw new RegraNegocioException("Só é possível alterar pedidos pendentes. Este pedido está "
                    + p.getStatus().getRotulo().toLowerCase(Locale.ROOT) + ".");
        p.setCliente(clientes.obter(e.clienteId()));
        p.setOrigem(e.origem());
        preencher(p, e, agora());
        return AdminVisao.de(p);
    }

    /**
     * UML: excluirPedido(id). Pedidos com pagamento não podem ser excluídos (preserva o financeiro);
     * para desistências, o recomendado é Cancelar, que mantém o histórico (corrige B10).
     */
    public void excluirPedido(Long id) {
        Pedido p = obter(id);
        if (p.isPago())
            throw new RegraNegocioException("Este pedido tem pagamento registrado. Remova o pagamento antes de excluir.");
        pedidos.delete(p);
    }

    private void preencher(Pedido p, AdminEntrada e, LocalDateTime agora) {
        LocalDate dataPedido = e.dataPedido() != null ? e.dataPedido() : p.getDataPedido();
        LocalTime horarioPedido = e.horarioPedido() != null ? e.horarioPedido() : p.getHorarioPedido();
        if (dataPedido.isAfter(agora.toLocalDate()))
            throw new RegraNegocioException("A data do pedido não pode ser no futuro.");
        p.definirDatas(dataPedido, horarioPedido, e.dataEntregaRetirada(), e.horarioEntregaRetirada());
        p.definirLogistica(e.tipoEntrega(), e.enderecoEntrega());
        if (!p.isPago()) p.setFormaPagamento(e.formaPagamento());
        p.setObservacoes(e.observacoes());
        p.substituirItens(resolverItens(e.itens(), p));
    }

    /**
     * Converte os itens recebidos em produtos. Produtos fora do cardápio só são aceitos se já
     * estavam no pedido que está sendo editado.
     */
    private List<Pedido.ItemSolicitado> resolverItens(List<ItemEntrada> itens, Pedido existente) {
        if (itens == null || itens.isEmpty()) throw new RegraNegocioException("O pedido precisa ter pelo menos um produto.");
        List<Pedido.ItemSolicitado> resultado = new ArrayList<>();
        for (ItemEntrada i : itens) {
            Produto produto = produtos.obter(i.produtoId());
            boolean jaEstavaNoPedido = existente != null
                    && existente.getItens().stream().anyMatch(it -> it.getProduto().getId().equals(produto.getId()));
            if (!produto.isAtivo() && !jaEstavaNoPedido)
                throw new RegraNegocioException("O produto \"" + produto.getNome() + "\" não está disponível no cardápio.");
            resultado.add(new Pedido.ItemSolicitado(produto, i.quantidade()));
        }
        return resultado;
    }

    // ================================================================= status e pagamento

    /** UML: atualizarStatus — RF06 / Regra 7. */
    public AdminVisao alterarStatus(Long id, StatusPedido status) {
        Pedido p = obter(id);
        p.atualizarStatus(status);
        return AdminVisao.de(p);
    }

    /** UML: registrarPagamento() — gera automaticamente a entrada financeira (decisão 6; corrige R5). */
    public AdminVisao registrarPagamento(Long id, PagamentoEntrada e) {
        Pedido p = obter(id);
        LocalDate data = e.data() == null ? LocalDate.now(clock) : e.data();
        if (data.isAfter(LocalDate.now(clock))) throw new RegraNegocioException("A data do pagamento não pode ser no futuro.");
        Pagamento pg = pagamentos.save(p.registrarPagamento(e.tipo(), e.valor(), data, agora()));
        financeiro.registrarAutomatica(pg);
        return AdminVisao.de(p);
    }

    /** Desfaz um pagamento registrado por engano, removendo também a entrada automática. */
    public AdminVisao removerPagamento(Long id) {
        Pedido p = obter(id);
        Pagamento pg = p.getPagamento();
        if (pg == null) throw new RegraNegocioException("Este pedido não tem pagamento registrado.");
        financeiro.removerAutomatica(pg);
        p.removerPagamento();
        pagamentos.delete(pg);
        pagamentos.flush();
        return AdminVisao.de(p);
    }

    // ================================================================= consultas

    @Transactional(readOnly = true)
    public AdminVisao detalhar(Long id) { return AdminVisao.de(obter(id)); }

    /**
     * RF04, RF06, RF11, RF19: lista organizada pela data de entrega/retirada, com filtros opcionais.
     * "busca" aceita parte do nome do cliente ou uma data (03/11/2026 ou 2026-11-03).
     */
    @Transactional(readOnly = true)
    public List<AdminVisao> listar(StatusPedido status, TipoEntrega tipo, LocalDate inicio, LocalDate fim, String busca) {
        return buscarEntidades(status, tipo, inicio, fim, busca).stream().map(AdminVisao::de).toList();
    }

    /** UML: pesquisarPedido(termo) — RF11. */
    @Transactional(readOnly = true)
    public List<AdminVisao> pesquisarPedido(String termo) { return listar(null, null, null, null, termo); }

    public List<Pedido> buscarEntidades(StatusPedido status, TipoEntrega tipo, LocalDate inicio, LocalDate fim, String busca) {
        String termo = Textos.limpar(busca);
        LocalDate dataBuscada = Datas.tentarLer(termo);
        Specification<Pedido> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (tipo != null) ps.add(cb.equal(root.get("tipoEntrega"), tipo));
            if (inicio != null) ps.add(cb.greaterThanOrEqualTo(root.get("dataEntregaRetirada"), inicio));
            if (fim != null) ps.add(cb.lessThanOrEqualTo(root.get("dataEntregaRetirada"), fim));
            if (dataBuscada != null) {
                ps.add(cb.or(cb.equal(root.get("dataPedido"), dataBuscada),
                        cb.equal(root.get("dataEntregaRetirada"), dataBuscada)));
            } else if (termo != null) {
                Join<Object, Object> cliente = root.join("cliente");
                String padrao = "%" + termo.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                ps.add(cb.or(cb.like(cb.lower(cliente.get("nome")), padrao, '\\'),
                        cb.equal(root.get("codigo"), termo.toUpperCase(Locale.ROOT))));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        return pedidos.findAll(spec).stream().sorted(ORDEM_POR_ENTREGA).toList();
    }

    /**
     * "Meus Pedidos": o cliente vê SOMENTE o pedido cujo código e telefone conferem.
     * A mensagem é a mesma quando o código não existe ou o telefone não confere (não revela pedidos alheios).
     */
    @Transactional(readOnly = true)
    public PublicoVisao consultarPublico(ConsultaEntrada e) {
        NaoEncontradoException naoEncontrado = new NaoEncontradoException(
                "Pedido não encontrado. Confira o código e o telefone informados.");
        String telefone;
        try {
            telefone = Textos.telefone(e.telefone());
        } catch (RegraNegocioException invalido) {
            throw naoEncontrado;
        }
        String codigo = e.codigo().strip().toUpperCase(Locale.ROOT);
        return pedidos.findByCodigo(codigo)
                .filter(p -> p.getCliente().getTelefone().equals(telefone))
                .map(PublicoVisao::de)
                .orElseThrow(() -> naoEncontrado);
    }

    public Pedido obter(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
    }

    /** IDs e códigos são gerados pelo sistema (nota do UML; corrige B8). */
    private String gerarCodigo() {
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) sb.append(ALFABETO_CODIGO.charAt(random.nextInt(ALFABETO_CODIGO.length())));
            String codigo = sb.toString();
            if (!pedidos.existsByCodigo(codigo)) return codigo;
        }
        throw new IllegalStateException("Não foi possível gerar um código de pedido único.");
    }

    private LocalDateTime agora() { return LocalDateTime.now(clock).withNano(0); }
}
