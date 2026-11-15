package br.com.deliciacaseira.servico;

import br.com.deliciacaseira.dominio.Cliente;
import br.com.deliciacaseira.dominio.NaoEncontradoException;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import br.com.deliciacaseira.dominio.Textos;
import br.com.deliciacaseira.dto.CadastroDto.ClienteEntrada;
import br.com.deliciacaseira.dto.CadastroDto.ClienteVisao;
import br.com.deliciacaseira.repositorio.ClienteRepository;
import br.com.deliciacaseira.repositorio.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** RF16 / RF18: clientes frequentes e seus endereços. */
@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;
    private final Clock clock;

    public ClienteService(ClienteRepository clientes, PedidoRepository pedidos, Clock clock) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ClienteVisao> listar(String busca) {
        String termo = Textos.limpar(busca);
        List<Cliente> lista = termo == null
                ? clientes.listarOrdenado()
                : clientes.buscar("%" + termo.toLowerCase(Locale.ROOT).replaceAll("[%_\\\\]", "") + "%");
        return lista.stream().map(c -> ClienteVisao.de(c, pedidos.countByClienteId(c.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ClienteVisao buscar(Long id) {
        Cliente c = obter(id);
        return ClienteVisao.de(c, pedidos.countByClienteId(id));
    }

    public ClienteVisao criar(ClienteEntrada e) {
        String telefone = Textos.telefone(e.telefone());
        clientes.findByTelefone(telefone).ifPresent(existente -> {
            throw new RegraNegocioException("Já existe um cliente com este telefone: " + existente.getNome() + ".");
        });
        Cliente c = clientes.save(new Cliente(e.nome(), telefone, e.endereco(), e.clienteFrequente(), agora()));
        return ClienteVisao.de(c, 0);
    }

    public ClienteVisao atualizar(Long id, ClienteEntrada e) {
        Cliente c = obter(id);
        String telefone = Textos.telefone(e.telefone());
        if (clientes.existsByTelefoneAndIdNot(telefone, id))
            throw new RegraNegocioException("Já existe outro cliente com este telefone.");
        c.setNome(e.nome());
        c.setTelefone(telefone);
        c.setEndereco(e.endereco());
        c.setClienteFrequente(e.clienteFrequente());
        return ClienteVisao.de(c, pedidos.countByClienteId(id));
    }

    public void excluir(Long id) {
        Cliente c = obter(id);
        if (pedidos.existsByClienteId(id))
            throw new RegraNegocioException("Este cliente tem pedidos registrados e não pode ser excluído.");
        clientes.delete(c);
    }

    /**
     * Pedido pelo site: o cliente é reconhecido pelo telefone. Se não existir, é cadastrado.
     * Dados de um cliente já existente não são sobrescritos pelo site (apenas o endereço vazio é completado).
     */
    public Cliente obterOuCriarParaPedidoSite(String nome, String telefone, String endereco) {
        String tel = Textos.telefone(telefone);
        return clientes.findByTelefone(tel)
                .map(c -> {
                    if (c.getEndereco() == null && endereco != null) c.setEndereco(endereco);
                    return c;
                })
                .orElseGet(() -> clientes.save(new Cliente(nome, tel, endereco, false, agora())));
    }

    public Cliente obter(Long id) {
        if (id == null) throw new RegraNegocioException("Escolha o cliente.");
        return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado."));
    }

    private LocalDateTime agora() { return LocalDateTime.now(clock); }
}
