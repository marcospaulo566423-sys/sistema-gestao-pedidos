package br.com.deliciacaseira.repositorio;

import br.com.deliciacaseira.dominio.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefone(String telefone);

    Optional<Cliente> findByUsuarioIgnoreCase(String usuario);

    boolean existsByUsuarioIgnoreCase(String usuario);

    boolean existsByTelefoneAndIdNot(String telefone, Long id);

    @Query("""
            select c from Cliente c
            where lower(c.nome) like :termo or c.telefone like :termo
            order by c.clienteFrequente desc, lower(c.nome)
            """)
    List<Cliente> buscar(@Param("termo") String termo);

    @Query("select c from Cliente c order by c.clienteFrequente desc, lower(c.nome)")
    List<Cliente> listarOrdenado();
}