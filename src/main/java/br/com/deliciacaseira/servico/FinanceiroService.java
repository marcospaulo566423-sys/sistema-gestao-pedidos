package br.com.deliciacaseira.servico;

import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.NaoEncontradoException;
import br.com.deliciacaseira.dominio.Pagamento;
import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.RegistroFinanceiro;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dto.RelatorioDto.EntradaFinanceira;
import br.com.deliciacaseira.dto.RelatorioDto.RegistroVisao;
import br.com.deliciacaseira.dto.RelatorioDto.RelatorioFinanceiro;
import br.com.deliciacaseira.repositorio.RegistroFinanceiroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF08, RF09, RF15, RF20: entradas financeiras. Somente entradas (despesas e lucro não são modelados).
 * Acessível apenas pela área administrativa (Regra 6).
 */
@Service
@Transactional
public class FinanceiroService {

    public static final String AVULSA = "AVULSA";

    private final RegistroFinanceiroRepository registros;
    private final Clock clock;

    public FinanceiroService(RegistroFinanceiroRepository registros, Clock clock) {
        this.registros = registros;
        this.clock = clock;
    }

    /** RF09 / RF20: histórico e relatório por dia, semana, mês, ano ou todos. */
    @Transactional(readOnly = true)
    public RelatorioFinanceiro relatorio(Periodo periodo, LocalDate referencia) {
        Periodo p = periodo == null ? Periodo.MES : periodo;
        LocalDate ref = referencia == null ? LocalDate.now(clock) : referencia;
        Periodo.Intervalo intervalo = p.intervalo(ref);
        List<RegistroVisao> lista = registros.listarNoPeriodo(intervalo.inicio(), intervalo.fim())
                .stream().map(RegistroVisao::de).toList();

        Map<String, BigDecimal> porForma = new LinkedHashMap<>();
        for (FormaPagamento f : FormaPagamento.values()) porForma.put(f.name(), BigDecimal.ZERO);
        porForma.put(AVULSA, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        for (RegistroVisao r : lista) {
            total = total.add(r.valor());
            porForma.merge(r.formaPagamento() == null ? AVULSA : r.formaPagamento().name(), r.valor(), BigDecimal::add);
        }
        boolean todos = p == Periodo.TODOS;
        return new RelatorioFinanceiro(p, todos ? null : intervalo.inicio(), todos ? null : intervalo.fim(),
                lista, total, porForma);
    }

    /** UML: registrarEntradaFinanceira(valor, data, descricao) — entrada manual avulsa. */
    public RegistroVisao registrarEntradaFinanceira(EntradaFinanceira e) {
        exigirDataValida(e.data());
        return RegistroVisao.de(registros.save(new RegistroFinanceiro(e.valor(), e.data(), e.descricao(), agora())));
    }

    /**
     * UML: editarEntradaFinanceira(id) — RF15. Entradas automáticas (de pagamento) podem ter data e
     * descrição ajustadas, mas o valor acompanha o pagamento do pedido.
     */
    public RegistroVisao editarEntradaFinanceira(Long id, EntradaFinanceira e) {
        RegistroFinanceiro r = obter(id);
        exigirDataValida(e.data());
        if (r.isAutomatico() && (e.valor() == null || e.valor().compareTo(r.getValor()) != 0))
            throw new RegraNegocioException("Esta entrada veio do pagamento de um pedido. Para mudar o valor, "
                    + "remova o pagamento no pedido e registre de novo.");
        r.setValor(e.valor());
        r.setData(e.data());
        r.setDescricao(e.descricao());
        return RegistroVisao.de(r);
    }

    public void excluir(Long id) {
        RegistroFinanceiro r = obter(id);
        if (r.isAutomatico())
            throw new RegraNegocioException("Esta entrada veio do pagamento de um pedido. Remova o pagamento no pedido.");
        registros.delete(r);
    }

    /** Decisão 6: o pagamento gera automaticamente a entrada correspondente (uma só, garantido por UNIQUE). */
    public void registrarAutomatica(Pagamento pagamento) {
        registros.save(RegistroFinanceiro.doPagamento(pagamento, agora()));
    }

    public void removerAutomatica(Pagamento pagamento) {
        registros.findByPagamento(pagamento).ifPresent(registros::delete);
        registros.flush();
    }

    private RegistroFinanceiro obter(Long id) {
        return registros.findById(id).orElseThrow(() -> new NaoEncontradoException("Registro financeiro não encontrado."));
    }

    private void exigirDataValida(LocalDate data) {
        if (data != null && data.isAfter(LocalDate.now(clock)))
            throw new RegraNegocioException("A data da entrada não pode ser no futuro.");
    }

    private LocalDateTime agora() { return LocalDateTime.now(clock); }
}
