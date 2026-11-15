package br.com.deliciacaseira.web;

import br.com.deliciacaseira.dominio.StatusPedido;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.PedidoDto.AdminEntrada;
import br.com.deliciacaseira.dto.PedidoDto.AdminVisao;
import br.com.deliciacaseira.dto.PedidoDto.PagamentoEntrada;
import br.com.deliciacaseira.dto.PedidoDto.StatusEntrada;
import br.com.deliciacaseira.servico.PedidoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Pedidos na área administrativa (protegida por login). */
@RestController
@RequestMapping("/api/admin/pedidos")
public class AdminPedidoController {

    private final PedidoService pedidos;

    public AdminPedidoController(PedidoService pedidos) { this.pedidos = pedidos; }

    @GetMapping
    public List<AdminVisao> listar(@RequestParam(required = false) StatusPedido status,
                                   @RequestParam(required = false) TipoEntrega tipoEntrega,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                   @RequestParam(required = false) String busca) {
        return pedidos.listar(status, tipoEntrega, data, data, busca);
    }

    @GetMapping("/{id}")
    public AdminVisao detalhar(@PathVariable Long id) { return pedidos.detalhar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminVisao cadastrar(@Valid @RequestBody AdminEntrada entrada) { return pedidos.cadastrarPedido(entrada); }

    @PutMapping("/{id}")
    public AdminVisao editar(@PathVariable Long id, @Valid @RequestBody AdminEntrada entrada) {
        return pedidos.editarPedido(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { pedidos.excluirPedido(id); }

    @PatchMapping("/{id}/status")
    public AdminVisao alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusEntrada entrada) {
        return pedidos.alterarStatus(id, entrada.status());
    }

    @PostMapping("/{id}/pagamento")
    public AdminVisao registrarPagamento(@PathVariable Long id, @Valid @RequestBody PagamentoEntrada entrada) {
        return pedidos.registrarPagamento(id, entrada);
    }

    @DeleteMapping("/{id}/pagamento")
    public AdminVisao removerPagamento(@PathVariable Long id) { return pedidos.removerPagamento(id); }
}
