package br.com.deliciacaseira.repositorio;

import br.com.deliciacaseira.dominio.Pagamento;
import br.com.deliciacaseira.dominio.RegistroFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegistroFinanceiroRepository extends JpaRepository<RegistroFinanceiro, Long> {

    Optional<RegistroFinanceiro> findByPagamento(Pagamento pagamento);

    @Query("""
            select r from RegistroFinanceiro r
            where r.data between :inicio and :fim
            order by r.data desc, r.id desc
            """)
    List<RegistroFinanceiro> listarNoPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
