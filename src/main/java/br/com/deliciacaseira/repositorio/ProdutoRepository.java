package br.com.deliciacaseira.repositorio;

import br.com.deliciacaseira.dominio.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNomeIgnoreCase(String nome);

    @Query("select p from Produto p where p.ativo = true order by lower(p.nome)")
    List<Produto> listarAtivos();

    @Query("select p from Produto p order by p.ativo desc, lower(p.nome)")
    List<Produto> listarTodos();
}
