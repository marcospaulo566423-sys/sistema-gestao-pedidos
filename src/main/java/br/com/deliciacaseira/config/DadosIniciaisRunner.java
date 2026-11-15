package br.com.deliciacaseira.config;

import br.com.deliciacaseira.dominio.ItemEstoque;
import br.com.deliciacaseira.dominio.Produto;
import br.com.deliciacaseira.dominio.UnidadeMedida;
import br.com.deliciacaseira.repositorio.ItemEstoqueRepository;
import br.com.deliciacaseira.repositorio.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cadastra o cardápio inicial (os 9 produtos do minimundo) e alguns insumos básicos
 * SOMENTE quando as tabelas estão vazias — nunca sobrescreve dados já cadastrados.
 * Os preços são valores de exemplo: a administradora ajusta em "Produtos".
 * Desligado no perfil "test" para não interferir nos testes automatizados.
 */
@Component
@Profile("!test")
public class DadosIniciaisRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciaisRunner.class);

    private final ProdutoRepository produtos;
    private final ItemEstoqueRepository insumos;

    public DadosIniciaisRunner(ProdutoRepository produtos, ItemEstoqueRepository insumos) {
        this.produtos = produtos;
        this.insumos = insumos;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (produtos.count() == 0) {
            List<Produto> cardapio = List.of(
                    new Produto("Pão de coco", new BigDecimal("15.00"), "Pão caseiro macio recheado com coco."),
                    new Produto("Pão de canela", new BigDecimal("15.00"), "Pão caseiro com canela e açúcar."),
                    new Produto("Pão de doce de leite", new BigDecimal("16.00"), "Pão caseiro recheado com doce de leite."),
                    new Produto("Pão de beijinho", new BigDecimal("16.00"), "Pão caseiro recheado com beijinho."),
                    new Produto("Pão de brigadeiro", new BigDecimal("16.00"), "Pão caseiro recheado com brigadeiro."),
                    new Produto("Pão de gotas de chocolate", new BigDecimal("16.00"), "Pão caseiro com gotas de chocolate."),
                    new Produto("Pão de goiabada", new BigDecimal("15.00"), "Pão caseiro recheado com goiabada."),
                    new Produto("Pão sem recheio", new BigDecimal("12.00"), "Pão caseiro tradicional, sem recheio."),
                    new Produto("Pacote de rosquinha de nata", new BigDecimal("10.00"), "Pacote de rosquinhas de nata caseiras."));
            produtos.saveAll(cardapio);
            log.info("Cardápio inicial cadastrado ({} produtos).", cardapio.size());
        }
        if (insumos.count() == 0) {
            List<ItemEstoque> basicos = List.of(
                    new ItemEstoque("Farinha de trigo", new BigDecimal("10"), UnidadeMedida.KG, new BigDecimal("5")),
                    new ItemEstoque("Açúcar", new BigDecimal("5"), UnidadeMedida.KG, new BigDecimal("2")),
                    new ItemEstoque("Leite", new BigDecimal("6"), UnidadeMedida.LITRO, new BigDecimal("3")),
                    new ItemEstoque("Ovos", new BigDecimal("3"), UnidadeMedida.DUZIA, new BigDecimal("1")),
                    new ItemEstoque("Fermento biológico", new BigDecimal("500"), UnidadeMedida.G, new BigDecimal("200")),
                    new ItemEstoque("Manteiga", new BigDecimal("1"), UnidadeMedida.KG, new BigDecimal("0.5")));
            insumos.saveAll(basicos);
            log.info("Insumos básicos cadastrados ({} itens).", basicos.size());
        }
    }
}
