package br.com.deliciacaseira.config;

import br.com.deliciacaseira.repositorio.AdministradoraRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carrega a administradora para o Spring Security (RF01 / Regra 1). */
@Service
public class AdministradoraDetailsService implements UserDetailsService {

    public static final String PAPEL = "ADMIN";

    private final AdministradoraRepository repositorio;
    private final ProtecaoLogin protecao;

    public AdministradoraDetailsService(AdministradoraRepository repositorio, ProtecaoLogin protecao) {
        this.repositorio = repositorio;
        this.protecao = protecao;
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) {
        return repositorio.findByUsuarioIgnoreCase(usuario == null ? "" : usuario.strip())
                .map(a -> User.withUsername(a.getUsuario())
                        .password(a.getSenhaHash())
                        .roles(PAPEL)
                        .accountLocked(protecao.bloqueado(a.getUsuario()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha incorretos."));
    }
}
