package br.com.deliciacaseira.web;

import br.com.deliciacaseira.dto.CadastroDto.ProdutoCardapio;
import br.com.deliciacaseira.dto.PedidoDto.ConsultaEntrada;
import br.com.deliciacaseira.dto.PedidoDto.PublicoVisao;
import br.com.deliciacaseira.dto.PedidoDto.SiteEntrada;
import br.com.deliciacaseira.servico.PedidoService;
import br.com.deliciacaseira.servico.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Área do Cliente (site, sem login). Nenhum dado financeiro ou de outros clientes é exposto aqui.
 */
@RestController
public class PublicoController {

    private final ProdutoService produtos;
    private final PedidoService pedidos;

    public PublicoController(ProdutoService produtos, PedidoService pedidos) {
        this.produtos = produtos;
        this.pedidos = pedidos;
    }

    /** Cardápio: somente produtos ativos. */
    @GetMapping("/api/publico/produtos")
    public List<ProdutoCardapio> cardapio() { return produtos.listarCardapio(); }

    /** Regra 2: o cliente faz o pedido pelo site. */
    @PostMapping("/api/publico/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicoVisao fazerPedido(@Valid @RequestBody SiteEntrada entrada) { return pedidos.criarPeloSite(entrada); }

    /** "Meus Pedidos": código + telefone. POST para o telefone não aparecer na URL nem em logs. */
    @PostMapping("/api/publico/pedidos/consulta")
    public PublicoVisao consultar(@Valid @RequestBody ConsultaEntrada entrada) { return pedidos.consultarPublico(entrada); }

    /** Token CSRF para as telas do painel (enviado no cabeçalho das requisições que alteram dados). */
    @GetMapping("/api/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}
