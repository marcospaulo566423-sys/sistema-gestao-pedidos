package br.com.deliciacaseira.config;

import br.com.deliciacaseira.repositorio.AdministradoraRepository;
import br.com.deliciacaseira.repositorio.ClienteRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdministradoraDetailsService implements UserDetailsService {

    public static final String PAPEL = "ADMIN";
    public static final String PAPEL_CLIENTE = "CLIENTE";

    private final AdministradoraRepository admins;
    private final ClienteRepository clientes;
    private final ProtecaoLogin protecao;

    public AdministradoraDetailsService(AdministradoraRepository admins, ClienteRepository clientes, ProtecaoLogin protecao) {
        this.admins = admins;
        this.clientes = clientes;
        this.protecao = protecao;
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) {
        String login = usuario == null ? "" : usuario.strip();

        var admin = admins.findByUsuarioIgnoreCase(login);
        if (admin.isPresent()) {
            var a = admin.get();
            return User.withUsername(a.getUsuario())
                    .password(a.getSenhaHash())
                    .roles(PAPEL)
                    .accountLocked(protecao.bloqueado(a.getUsuario()))
                    .build();
        }

        var cliente = clientes.findByUsuarioIgnoreCase(login).filter(c -> c.getSenhaHash() != null);
        if (cliente.isPresent()) {
            var c = cliente.get();
            return User.withUsername(c.getUsuario())
                    .password(c.getSenhaHash())
                    .roles(PAPEL_CLIENTE)
                    .accountLocked(protecao.bloqueado(c.getUsuario()))
                    .build();
        }

        throw new UsernameNotFoundException("Usuário ou senha incorretos.");
    }
}