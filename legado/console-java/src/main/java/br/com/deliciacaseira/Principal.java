package br.com.deliciacaseira;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;

/** Demonstra as funcionalidades principais do DeliciaCaseiraDaElaine. */
public class Principal {
    private static int testes = 0;
    private static int aprovados = 0;
    private static void verificar(String nome, boolean condicao) {
        testes++;
        if (condicao) { aprovados++; System.out.println("[OK] " + nome); }
        else { System.out.println("[FALHOU] " + nome); throw new AssertionError("Teste falhou: " + nome); }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        System.out.println("====================================================");
        System.out.println("       DELICIA CASEIRA DA ELAINE - DEMONSTRAÇÃO");
        System.out.println("====================================================");

        GerenciadorDoSistema sistema = new GerenciadorDoSistema();
        Administradora admin = new Administradora("Elaine", "elaine", "doce123");
        verificar("Login da administradora com credenciais corretas", admin.realizarLogin("elaine", "doce123"));
        verificar("Login rejeita senha incorreta", !admin.realizarLogin("elaine", "errada"));

        Cliente cliente = new Cliente("Maria Oliveira", "31999990000", "Rua das Flores, 100", true);
        Produto bolo = new Produto("Bolo de cenoura", 45.00, "bolo.jpg", "Bolo caseiro com cobertura de chocolate");
        Produto pao = new Produto("Pão de queijo", 2.50, "pao.jpg", "Salgado tradicional mineiro");
        verificar("Cadastro de produto bolo", sistema.cadastrarProduto(bolo));
        verificar("Cadastro de produto pão de queijo", sistema.cadastrarProduto(pao));
        verificar("Não permite produto duplicado", !sistema.cadastrarProduto(new Produto("Bolo de cenoura", 50, "", "Duplicado")));

        ItemEstoque estoqueBolo = new ItemEstoque(1, "Bolo de cenoura", 8, "unidade", 2);
        ItemEstoque estoquePao = new ItemEstoque(2, "Pão de queijo", 30, "unidade", 10);
        verificar("Cadastro de estoque do bolo", sistema.cadastrarItemEstoque(estoqueBolo));
        verificar("Cadastro de estoque do pão de queijo", sistema.cadastrarItemEstoque(pao, estoquePao));
        verificar("Consulta de estoque", sistema.consultarEstoque("Bolo de cenoura"));
        verificar("Estoque acima do mínimo não gera alerta", !estoqueBolo.precisaComprar());

        Pedido pedido = new Pedido(1001, cliente, LocalDate.now(), "14:30", LocalDate.now().plusDays(1), "RETIRADA", "Retirar na loja; bolo para aniversário");
        pedido.adicionarItem(new ItemPedido(bolo, 1));
        pedido.adicionarItem(new ItemPedido(pao, 6));
        verificar("Subtotal do bolo", Math.abs(pedido.getItens().get(0).calcularSubtotal() - 45.0) < 0.001);
        verificar("Total do pedido", Math.abs(pedido.calcularTotal() - 60.0) < 0.001);
        verificar("Cadastro de pedido e baixa automática do estoque", sistema.cadastrarPedido(pedido));
        verificar("Estoque do bolo atualizado após venda", Math.abs(estoqueBolo.getQuantidadeAtual() - 7.0) < 0.001);
        Pedido pedidoSemSaldo = new Pedido(1003, cliente, LocalDate.now(), "15:00", LocalDate.now().plusDays(1), "RETIRADA", "Teste de saldo insuficiente");
        pedidoSemSaldo.adicionarItem(new ItemPedido(pao, 13));
        pedidoSemSaldo.adicionarItem(new ItemPedido(pao, 13));
        verificar("Bloqueia pedido quando a soma dos itens supera o estoque", !sistema.cadastrarPedido(pedidoSemSaldo));

        Pagamento pagamento = new Pagamento("PIX", pedido.calcularTotal());
        verificar("Registro do pagamento", pagamento.registrarPagamento());
        pedido.setPagamento(pagamento);
        pedido.atualizarStatus("EM_PRODUCAO");
        verificar("Atualização de status do pedido", pedido.getStatus().equals("EM_PRODUCAO"));
        pedido.atualizarStatus("PRONTO");

        verificar("Pesquisa de pedido por nome do cliente", sistema.pesquisarPedido("Maria").length == 1);
        verificar("Filtro de produtos por texto descritivo", sistema.listarItensPorCategoria("caseiro").length == 1);
        verificar("Lista de produtos mais vendidos", sistema.listarMaisVendidos("").length == 2);
        verificar("Registro de entrada financeira", sistema.registrarEntradaFinanceira(pedido.calcularTotal(), LocalDate.now(), "Recebimento do pedido #1001"));
        verificar("Histórico financeiro contém o registro", sistema.consultarHistoricoFinanceiro().length == 1);

        sistema.organizarPedidosPorData();
        sistema.gerarRelatorioPedidos();
        sistema.gerarRelatorioFinanceiro(30);
        sistema.exibirLembretes();

        verificar("Status inválido é rejeitado", rejeitaStatusInvalido(pedido));
        verificar("Pedido sem itens não é cadastrado", !sistema.cadastrarPedido(new Pedido(1002, cliente, LocalDate.now(), "15:00", LocalDate.now().plusDays(1), "ENTREGA", "Sem itens")));
        verificar("Exclusão de pedido", sistema.excluirPedido(1001));
        verificar("Exclusão devolve os itens ao estoque", Math.abs(estoqueBolo.getQuantidadeAtual() - 8.0) < 0.001 && Math.abs(estoquePao.getQuantidadeAtual() - 30.0) < 0.001);
        verificar("Pedido excluído não aparece na pesquisa", sistema.pesquisarPedido("1001").length == 0);

        Pedido pedidoEdicao = new Pedido(1004, cliente, LocalDate.now(), "16:00", LocalDate.now().plusDays(1), "RETIRADA", "Pedido para testar edição");
        pedidoEdicao.adicionarItem(new ItemPedido(bolo, 1));
        verificar("Cadastro do pedido para teste de edição", sistema.cadastrarPedido(pedidoEdicao));
        Pedido pedidoAtualizado = new Pedido(1004, cliente, LocalDate.now(), "16:30", LocalDate.now().plusDays(1), "RETIRADA", "Quantidade atualizada");
        pedidoAtualizado.adicionarItem(new ItemPedido(bolo, 2));
        verificar("Edição do pedido", sistema.editarPedido(1004, pedidoAtualizado));
        verificar("Edição recalcula a baixa do estoque", Math.abs(estoqueBolo.getQuantidadeAtual() - 6.0) < 0.001);
        verificar("Exclusão do pedido editado", sistema.excluirPedido(1004));
        verificar("Exclusão do pedido editado restaura o estoque", Math.abs(estoqueBolo.getQuantidadeAtual() - 8.0) < 0.001);
        estoqueBolo.atualizarQuantidade(2);
        verificar("Alerta de compra quando estoque atinge o mínimo", estoqueBolo.precisaComprar());
        sistema.exibirLembretes();

        System.out.println("\n====================================================");
        System.out.println("TESTES CONCLUÍDOS: " + aprovados + "/" + testes + " aprovados");
        System.out.println("====================================================");
    }
    private static boolean rejeitaStatusInvalido(Pedido pedido) {
        try { pedido.atualizarStatus("EM_ANALISE"); return false; }
        catch (IllegalArgumentException ex) { return true; }
    }
}
