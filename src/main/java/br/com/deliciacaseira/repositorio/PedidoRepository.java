package br.com.deliciacaseira.repositorio;

import br.com.deliciacaseira.dominio.Pedido;
import br.com.deliciacaseira.dto.ItemProducao;
import br.com.deliciacaseira.dto.ProdutoVendido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    Optional<Pedido> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByClienteId(Long clienteId);

    long countByClienteId(Long clienteId);

    @Query("select count(i) > 0 from ItemPedido i where i.produto.id = :produtoId")
    boolean existeItemComProduto(@Param("produtoId") Long produtoId);

    /** RF05: soma das quantidades por produto dos pedidos NÃO cancelados da data de entrega/retirada. */
    @Query("""
            select new br.com.deliciacaseira.dto.ItemProducao(pr.id, pr.nome, sum(i.quantidade))
            from ItemPedido i join i.pedido p join i.produto pr
            where p.dataEntregaRetirada = :data
              and p.status <> br.com.deliciacaseira.dominio.StatusPedido.CANCELADO
            group by pr.id, pr.nome
            order by pr.nome
            """)
    List<ItemProducao> producaoDoDia(@Param("data") LocalDate data);

    /** RF10: produtos mais vendidos no período (pedidos não cancelados, pela data de entrega/retirada). */
    @Query("""
            select new br.com.deliciacaseira.dto.ProdutoVendido(
                pr.id, pr.nome, sum(i.quantidade), sum(i.precoUnitario * i.quantidade))
            from ItemPedido i join i.pedido p join i.produto pr
            where p.dataEntregaRetirada between :inicio and :fim
              and p.status <> br.com.deliciacaseira.dominio.StatusPedido.CANCELADO
            group by pr.id, pr.nome
            order by sum(i.quantidade) desc, pr.nome
            """)
    List<ProdutoVendido> maisVendidos(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
