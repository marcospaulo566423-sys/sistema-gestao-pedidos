package br.com.deliciacaseira.repositorio;

import br.com.deliciacaseira.dominio.Administradora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradoraRepository extends JpaRepository<Administradora, Long> {
    Optional<Administradora> findByUsuarioIgnoreCase(String usuario);
}
