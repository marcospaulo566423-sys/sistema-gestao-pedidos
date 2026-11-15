package br.com.deliciacaseira.repositorio;

import br.com.deliciacaseira.dominio.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
}
