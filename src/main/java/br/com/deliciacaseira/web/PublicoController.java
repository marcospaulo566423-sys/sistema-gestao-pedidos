package br.com.deliciacaseira.web;

import br.com.deliciacaseira.config.AdministradoraDetailsService;
import br.com.deliciacaseira.dominio.Cliente;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dominio.Textos;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoCardapio;
import br.com.deliciacaseira.dto.PedidoDto.ConsultaEntrada;
import br.com.deliciacaseira.dto.PedidoDto.PublicoVisao;
import br.com.deliciacaseira.dto.PedidoDto.SiteEntrada;
import br.com.deliciacaseira.repositorio.AdministradoraRepository;
import br.com.deliciacaseira.repositorio.ClienteRepository;
import br.com.deliciacaseira.servico.PedidoService;
import br.com.deliciacaseira.servico.ProdutoService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class PublicoController {

    private final ProdutoService produtos;
    private final PedidoService pedidos;
    private final ClienteRepository clientes;
    private final AdministradoraRepository admins;
    private final PasswordEncoder encoder;

    public PublicoController(ProdutoService produtos, PedidoService pedidos,
                             ClienteRepository clientes, AdministradoraRepository admins,
                             PasswordEncoder encoder) {
        this.produtos = produtos;
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.admins = admins;
        this.encoder = encoder;
    }

    public record AcessoClienteEntrada(String nome, String telefone) {}
    public record CadastroClienteEntrada(String nome, String usuario, String telefone, String senha) {}

    @GetMapping("/api/publico/produtos")
    public List<ProdutoCardapio> cardapio() { return produtos.listarCardapio(); }

    @PostMapping("/api/publico/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicoVisao fazerPedido(@Valid @RequestBody SiteEntrada entrada) { return pedidos.criarPeloSite(entrada); }

    @PostMapping("/api/publico/pedidos/consulta")
    public PublicoVisao consultar(@Valid @RequestBody ConsultaEntrada entrada) { return pedidos.consultarPublico(entrada); }

    @GetMapping("/api/publico/meus-pedidos")
    @Transactional(readOnly = true)
    public List<PublicoVisao> meusPedidos(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RegraNegocioException("Entre na sua conta para ver seus pedidos.");
        }
        String tel = auth.getName().replaceAll("\\D", "");
        var optCliente = clientes.findByTelefone(tel);
        if (optCliente.isEmpty()) {
            optCliente = clientes.findByUsuarioIgnoreCase(auth.getName());
        }
        if (optCliente.isEmpty()) {
            return List.of();
        }
        String telefoneCliente = optCliente.get().getTelefone();
        return pedidos.buscarEntidades(null, null, null, null, null).stream()
                .filter(p -> p.getCliente() != null && telefoneCliente.equals(p.getCliente().getTelefone()))
                .map(PublicoVisao::de)
                .toList();
    }

    @PostMapping("/api/auth/cliente")
    @Transactional
    public Map<String, Object> acessarCliente(@RequestBody AcessoClienteEntrada entrada, HttpServletRequest req) {
        if (entrada == null) throw new RegraNegocioException("Informe seu nome e telefone.");
        String nome = Textos.obrigatorio(entrada.nome(), "Seu nome", 100);
        String telefone = Textos.telefone(entrada.telefone());

        Cliente cliente = clientes.findByTelefone(telefone).orElse(null);
        if (cliente != null) {
            cliente.setNome(nome);
            if (cliente.getUsuario() == null || cliente.getUsuario().isBlank()) {
                cliente.setUsuario(telefone);
            }
            if (cliente.getSenhaHash() == null || cliente.getSenhaHash().isBlank()) {
                cliente.setSenhaHash(encoder.encode(telefone));
            }
        } else {
            cliente = new Cliente(nome, telefone, null, false, LocalDateTime.now());
            cliente.setUsuario(telefone);
            cliente.setSenhaHash(encoder.encode(telefone));
        }
        clientes.save(cliente);

        var auth = new UsernamePasswordAuthenticationToken(
                telefone, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        HttpSession session = req.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return Map.<String, Object>of(
                "perfil", "CLIENTE",
                "nome", cliente.getNome(),
                "telefone", cliente.getTelefone(),
                "endereco", cliente.getEndereco() == null ? "" : cliente.getEndereco()
        );
    }

    @PostMapping("/api/publico/clientes/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> cadastrarCliente(@RequestBody CadastroClienteEntrada entrada, HttpServletRequest req) throws ServletException {
        if (entrada == null) throw new RegraNegocioException("Dados de cadastro inválidos.");
        String nome = Textos.obrigatorio(entrada.nome(), "Nome completo", 100);
        String telefone = Textos.telefone(entrada.telefone());
        String usuario = entrada.usuario() == null ? telefone : entrada.usuario().strip().toLowerCase();
        String senha = (entrada.senha() == null || entrada.senha().isBlank()) ? telefone : entrada.senha();

        if (admins.findByUsuarioIgnoreCase(usuario).isPresent()) {
            throw new RegraNegocioException("Este nome de usuário já está em uso.");
        }

        Cliente cliente = clientes.findByTelefone(telefone).orElse(null);
        if (cliente != null) {
            cliente.setNome(nome);
            cliente.setUsuario(usuario);
            cliente.setSenhaHash(encoder.encode(senha));
        } else {
            cliente = new Cliente(nome, telefone, null, false, LocalDateTime.now());
            cliente.setUsuario(usuario);
            cliente.setSenhaHash(encoder.encode(senha));
        }
        clientes.save(cliente);

        if (req.getUserPrincipal() != null) req.logout();
        req.login(usuario, senha);

        return Map.<String, Object>of(
                "perfil", "CLIENTE",
                "nome", cliente.getNome(),
                "telefone", cliente.getTelefone());
    }

    @GetMapping("/api/auth/sessao")
    public Map<String, Object> sessao(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Map.<String, Object>of("autenticado", false);
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_" + AdministradoraDetailsService.PAPEL));
        if (isAdmin) {
            return Map.<String, Object>of(
                    "autenticado", true,
                    "perfil", "ADMIN",
                    "usuario", auth.getName());
        }
        var optCliente = clientes.findByTelefone(auth.getName().replaceAll("\\D", ""));
        if (optCliente.isEmpty()) {
            optCliente = clientes.findByUsuarioIgnoreCase(auth.getName());
        }
        if (optCliente.isEmpty()) {
            return Map.<String, Object>of("autenticado", false);
        }
        Cliente c = optCliente.get();
        return Map.<String, Object>of(
                "autenticado", true,
                "perfil", "CLIENTE",
                "nome", c.getNome(),
                "telefone", c.getTelefone(),
                "endereco", c.getEndereco() == null ? "" : c.getEndereco());
    }

    @GetMapping("/api/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}