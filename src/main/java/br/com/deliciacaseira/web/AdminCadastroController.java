package br.com.deliciacaseira.web;

import br.com.deliciacaseira.dto.CadastroDto.ClienteEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ClienteVisao;
import br.com.deliciacaseira.dto.CadastroDto.EstoqueEntrada;
import br.com.deliciacaseira.dto.CadastroDto.EstoqueVisao;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoVisao;
import br.com.deliciacaseira.dto.CadastroDto.QuantidadeEntrada;
import br.com.deliciacaseira.servico.ClienteService;
import br.com.deliciacaseira.servico.EstoqueService;
import br.com.deliciacaseira.servico.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Clientes, produtos e estoque na área administrativa (protegida por login). */
@RestController
public class AdminCadastroController {

    private final ClienteService clientes;
    private final ProdutoService produtos;
    private final EstoqueService estoque;

    public AdminCadastroController(ClienteService clientes, ProdutoService produtos, EstoqueService estoque) {
        this.clientes = clientes;
        this.produtos = produtos;
        this.estoque = estoque;
    }

    // ---------------------------------------------------------------- clientes

    @GetMapping("/api/admin/clientes")
    public List<ClienteVisao> listarClientes(@RequestParam(required = false) String busca) { return clientes.listar(busca); }

    @GetMapping("/api/admin/clientes/{id}")
    public ClienteVisao buscarCliente(@PathVariable Long id) { return clientes.buscar(id); }

    @PostMapping("/api/admin/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteVisao criarCliente(@Valid @RequestBody ClienteEntrada e) { return clientes.criar(e); }

    @PutMapping("/api/admin/clientes/{id}")
    public ClienteVisao atualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteEntrada e) {
        return clientes.atualizar(id, e);
    }

    @DeleteMapping("/api/admin/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCliente(@PathVariable Long id) { clientes.excluir(id); }

    // ---------------------------------------------------------------- produtos

    @GetMapping("/api/admin/produtos")
    public List<ProdutoVisao> listarProdutos() { return produtos.listarTodos(); }

    @PostMapping("/api/admin/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoVisao criarProduto(@Valid @RequestBody ProdutoEntrada e) { return produtos.criar(e); }

    @PutMapping("/api/admin/produtos/{id}")
    public ProdutoVisao atualizarProduto(@PathVariable Long id, @Valid @RequestBody ProdutoEntrada e) {
        return produtos.atualizar(id, e);
    }

    @DeleteMapping("/api/admin/produtos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirProduto(@PathVariable Long id) { produtos.excluir(id); }

    @PostMapping(value = "/api/admin/produtos/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProdutoVisao enviarFoto(@PathVariable Long id, @RequestPart("foto") MultipartFile foto) {
        return produtos.salvarFoto(id, foto);
    }

    // ---------------------------------------------------------------- estoque (insumos)

    @GetMapping("/api/admin/estoque")
    public List<EstoqueVisao> listarEstoque() { return estoque.consultarEstoque(); }

    @GetMapping("/api/admin/estoque/comprar")
    public List<EstoqueVisao> itensParaComprar() { return estoque.listarItensParaCompra(); }

    @PostMapping("/api/admin/estoque")
    @ResponseStatus(HttpStatus.CREATED)
    public EstoqueVisao criarItem(@Valid @RequestBody EstoqueEntrada e) { return estoque.cadastrarItemEstoque(e); }

    @PutMapping("/api/admin/estoque/{id}")
    public EstoqueVisao atualizarItem(@PathVariable Long id, @Valid @RequestBody EstoqueEntrada e) {
        return estoque.atualizar(id, e);
    }

    @PatchMapping("/api/admin/estoque/{id}/quantidade")
    public EstoqueVisao atualizarQuantidade(@PathVariable Long id, @Valid @RequestBody QuantidadeEntrada e) {
        return estoque.atualizarQuantidade(id, e.novaQuantidade());
    }

    @DeleteMapping("/api/admin/estoque/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirItem(@PathVariable Long id) { estoque.excluir(id); }
}
