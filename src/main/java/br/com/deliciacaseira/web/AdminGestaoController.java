package br.com.deliciacaseira.web;

import br.com.deliciacaseira.dominio.Periodo;
import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dto.RelatorioDto.EntradaFinanceira;
import br.com.deliciacaseira.dto.RelatorioDto.Lembretes;
import br.com.deliciacaseira.dto.RelatorioDto.MaisVendidos;
import br.com.deliciacaseira.dto.RelatorioDto.Painel;
import br.com.deliciacaseira.dto.RelatorioDto.ProducaoDiaria;
import br.com.deliciacaseira.dto.RelatorioDto.RegistroVisao;
import br.com.deliciacaseira.dto.RelatorioDto.RelatorioFinanceiro;
import br.com.deliciacaseira.dto.RelatorioDto.RelatorioPedidos;
import br.com.deliciacaseira.repositorio.AdministradoraRepository;
import br.com.deliciacaseira.servico.FinanceiroService;
import br.com.deliciacaseira.servico.RelatorioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** Painel, produção, lembretes, financeiro e relatórios — somente administradora (Regras 1 e 6). */
@RestController
public class AdminGestaoController {

    private final RelatorioService relatorios;
    private final FinanceiroService financeiro;
    private final AdministradoraRepository administradoras;

    public AdminGestaoController(RelatorioService relatorios, FinanceiroService financeiro,
                                 AdministradoraRepository administradoras) {
        this.relatorios = relatorios;
        this.financeiro = financeiro;
        this.administradoras = administradoras;
    }

    @GetMapping("/api/admin/sessao")
    public Map<String, String> sessao(@AuthenticationPrincipal UserDetails usuario) {
        String nome = administradoras.findByUsuarioIgnoreCase(usuario.getUsername())
                .map(a -> a.getNome()).orElse(usuario.getUsername());
        return Map.of("usuario", usuario.getUsername(), "nome", nome);
    }

    @GetMapping("/api/admin/painel")
    public Painel painel() { return relatorios.painel(); }

    @GetMapping("/api/admin/producao")
    public ProducaoDiaria producao(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return relatorios.gerarListaDiariaProducao(data);
    }

    @GetMapping("/api/admin/lembretes")
    public Lembretes lembretes() { return relatorios.exibirLembretes(); }

    // ---------------------------------------------------------------- relatórios

    @GetMapping("/api/admin/relatorios/pedidos")
    public RelatorioPedidos relatorioPedidos(@RequestParam(required = false) Periodo periodo,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                             @RequestParam(required = false) StatusPedido status) {
        return relatorios.gerarRelatorioPedidos(periodo, data, status);
    }

    @GetMapping("/api/admin/relatorios/mais-vendidos")
    public MaisVendidos maisVendidos(@RequestParam(required = false) Periodo periodo,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return relatorios.listarMaisVendidos(periodo, data);
    }

    // ---------------------------------------------------------------- financeiro

    @GetMapping({"/api/admin/financeiro", "/api/admin/relatorios/financeiro"})
    public RelatorioFinanceiro financeiro(@RequestParam(required = false) Periodo periodo,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return financeiro.relatorio(periodo, data);
    }

    @PostMapping("/api/admin/financeiro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroVisao registrarEntrada(@Valid @RequestBody EntradaFinanceira e) {
        return financeiro.registrarEntradaFinanceira(e);
    }

    @PutMapping("/api/admin/financeiro/{id}")
    public RegistroVisao editarEntrada(@PathVariable Long id, @Valid @RequestBody EntradaFinanceira e) {
        return financeiro.editarEntradaFinanceira(id, e);
    }

    @DeleteMapping("/api/admin/financeiro/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirEntrada(@PathVariable Long id) { financeiro.excluir(id); }
}
