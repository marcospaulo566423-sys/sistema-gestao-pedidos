package br.com.deliciacaseira.repositorio;

import br.com.deliciacaseira.dominio.ItemEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemEstoqueRepository extends JpaRepository<ItemEstoque, Long> {

    Optional<ItemEstoque> findByNomeIgnoreCase(String nome);

    @Query("select i from ItemEstoque i order by lower(i.nome)")
    List<ItemEstoque> listarOrdenado();
}
