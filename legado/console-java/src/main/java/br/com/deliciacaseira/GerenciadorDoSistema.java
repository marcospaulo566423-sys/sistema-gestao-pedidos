package br.com.deliciacaseira;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** Centraliza as coleções em memória e as operações do sistema. */
public final class GerenciadorDoSistema {
    private final List<ItemEstoque> itensEstoque = new ArrayList<>();
    private final List<Pedido> pedidos = new ArrayList<>();
    private final List<Produto> produtos = new ArrayList<>();
    private final List<RegistroFinanceiro> registrosFinanceiros = new ArrayList<>();
    private final AtomicInteger proximoRegistroId = new AtomicInteger(1);

    public List<ItemEstoque> getItensEstoque() { return List.copyOf(itensEstoque); }
    public List<Pedido> getPedidos() { return List.copyOf(pedidos); }
    public List<Produto> getProdutos() { return List.copyOf(produtos); }
    public List<RegistroFinanceiro> getRegistrosFinanceiros() { return List.copyOf(registrosFinanceiros); }

    public boolean cadastrarItemEstoque(ItemEstoque itemEstoque) {
        if (itemEstoque == null || buscarEstoquePorId(itemEstoque.getId()) != null) return false;
        itensEstoque.add(itemEstoque); return true;
    }
    /** Sobrecarga para associar um item de estoque ao produto correspondente pelo nome. */
    public boolean cadastrarItemEstoque(Produto item, ItemEstoque itemEstoque) {
        if (item == null || itemEstoque == null || !item.getNome().equalsIgnoreCase(itemEstoque.getNome())) return false;
        if (!produtos.contains(item) && !cadastrarProduto(item)) return false;
        return cadastrarItemEstoque(itemEstoque);
    }
    public boolean consultarEstoque(ItemEstoque item) { return item != null && itensEstoque.stream().anyMatch(i -> i.getId() == item.getId()); }
    public boolean consultarEstoque(String nome) { return nome != null && itensEstoque.stream().anyMatch(i -> i.getNome().equalsIgnoreCase(nome.trim())); }
    /** O UML não define atributo categoria; o filtro pesquisa nome e descrição do produto. */
    public Produto[] listarItensPorCategoria(String filtro) {
        if (filtro == null || filtro.isBlank()) return produtos.toArray(Produto[]::new);
        String termo = filtro.toLowerCase(Locale.ROOT);
        return produtos.stream().filter(p -> p.getNome().toLowerCase(Locale.ROOT).contains(termo)
                || p.getDescricao().toLowerCase(Locale.ROOT).contains(termo)).toArray(Produto[]::new);
    }

    public boolean cadastrarPedido(Pedido pedido) {
        if (pedido == null || pedido.getItens().isEmpty() || buscarPedidoPorId(pedido.getId()) != null
                || pedido.getStatus().equals("CANCELADO")) return false;
        Map<ItemEstoque, Double> consumo = calcularConsumoEstoque(pedido);
        for (Map.Entry<ItemEstoque, Double> e : consumo.entrySet())
            if (e.getKey().getQuantidadeAtual() < e.getValue()) return false;
        consumo.forEach((item, quantidade) -> item.retirar(quantidade));
        pedidos.add(pedido);
        return true;
    }
    public boolean editarPedido(int id, Pedido pedidoAtualizado) {
        if (pedidoAtualizado == null || pedidoAtualizado.getItens().isEmpty() || pedidoAtualizado.getId() != id
                || pedidoAtualizado.getStatus().equals("CANCELADO")) return false;
        int indice = indicePedido(id); if (indice < 0) return false;
        Pedido antigo = pedidos.get(indice);
        Map<ItemEstoque, Double> diferencas = calcularConsumoEstoque(pedidoAtualizado);
        calcularConsumoEstoque(antigo).forEach((item, quantidade) -> diferencas.merge(item, -quantidade, Double::sum));
        for (Map.Entry<ItemEstoque, Double> e : diferencas.entrySet())
            if (e.getValue() > e.getKey().getQuantidadeAtual()) return false;
        diferencas.forEach((item, delta) -> {
            if (delta > 0) item.retirar(delta);
            else if (delta < 0) item.repor(-delta);
        });
        pedidos.set(indice, pedidoAtualizado);
        if (antigo.getCliente() != pedidoAtualizado.getCliente()) antigo.getCliente().removerPedido(antigo);
        pedidoAtualizado.getCliente().adicionarPedido(pedidoAtualizado);
        return true;
    }
    public boolean excluirPedido(int id) {
        int indice = indicePedido(id); if (indice < 0) return false;
        Pedido removido = pedidos.remove(indice);
        calcularConsumoEstoque(removido).forEach(ItemEstoque::repor);
        removido.getCliente().removerPedido(removido); return true;
    }
    public void organizarPedidosPorData() {
        pedidos.sort(Comparator.comparing(Pedido::getData).thenComparing(Pedido::getHorario).thenComparingInt(Pedido::getId));
    }
    public Pedido[] pesquisarPedido(String filtro) {
        if (filtro == null || filtro.isBlank()) return pedidos.toArray(Pedido[]::new);
        String termo = filtro.toLowerCase(Locale.ROOT);
        return pedidos.stream().filter(p -> Integer.toString(p.getId()).contains(termo)
                || p.getStatus().toLowerCase(Locale.ROOT).contains(termo)
                || p.getCliente().getNome().toLowerCase(Locale.ROOT).contains(termo)
                || p.getData().toString().contains(termo)).toArray(Pedido[]::new);
    }
    public void gerarRelatorioPedidos() {
        System.out.println("\n========== RELATÓRIO DE PEDIDOS ==========");
        if (pedidos.isEmpty()) { System.out.println("Nenhum pedido cadastrado."); return; }
        organizarPedidosPorData();
        pedidos.forEach(p -> System.out.println(p + " | Itens: " + p.getItens().size()));
        System.out.printf(Locale.forLanguageTag("pt-BR"), "Total em pedidos: R$ %.2f%n", pedidos.stream().mapToDouble(Pedido::calcularTotal).sum());
    }
    public void exibirLembretes() {
        System.out.println("\n========== LEMBRETES ==========");
        List<ItemEstoque> comprar = itensEstoque.stream().filter(ItemEstoque::precisaComprar).toList();
        if (comprar.isEmpty()) System.out.println("Estoque dentro do mínimo configurado.");
        else comprar.forEach(i -> System.out.println("REPOR: " + i));
        List<Pedido> pendentes = pedidos.stream().filter(p -> p.getStatus().equals("PENDENTE") || p.getStatus().equals("EM_PRODUCAO")).toList();
        if (!pendentes.isEmpty()) System.out.println("Pedidos que exigem acompanhamento: " + pendentes.size());
    }
    public boolean cadastrarProduto(Produto produto) {
        if (produto == null || produtos.stream().anyMatch(p -> p.getNome().equalsIgnoreCase(produto.getNome()))) return false;
        produtos.add(produto); return true;
    }
    public Produto[] listarMaisVendidos(String generico) {
        return pedidos.stream().flatMap(p -> p.getItens().stream())
                .filter(i -> generico == null || generico.isBlank() || i.getProduto().getNome().toLowerCase(Locale.ROOT).contains(generico.toLowerCase(Locale.ROOT)))
                .collect(Collectors.groupingBy(ItemPedido::getProduto, Collectors.summingInt(ItemPedido::getQuantidade)))
                .entrySet().stream().sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(e -> e.getKey()).toArray(Produto[]::new);
    }
    public boolean registrarEntradaFinanceira(double valor, LocalDate data, String descricao) {
        try {
            RegistroFinanceiro registro = new RegistroFinanceiro(proximoRegistroId.getAndIncrement(), valor, data, descricao);
            registrosFinanceiros.add(registro); return true;
        } catch (IllegalArgumentException ex) { return false; }
    }
    public RegistroFinanceiro[] consultarHistoricoFinanceiro() { return registrosFinanceiros.toArray(RegistroFinanceiro[]::new); }
    /** Gera no console o total de entradas registradas nos últimos "periodo" dias. */
    public void gerarRelatorioFinanceiro(int periodo) {
        if (periodo <= 0) throw new IllegalArgumentException("O período deve ser positivo.");
        LocalDate limite = LocalDate.now().minusDays(periodo - 1L);
        List<RegistroFinanceiro> filtrados = registrosFinanceiros.stream().filter(r -> !r.getData().isBefore(limite)).toList();
        double total = filtrados.stream().mapToDouble(RegistroFinanceiro::getValor).sum();
        System.out.println("\n========== RELATÓRIO FINANCEIRO (" + periodo + " dias) ==========");
        filtrados.forEach(System.out::println);
        System.out.printf(Locale.forLanguageTag("pt-BR"), "Total de entradas: R$ %.2f%n", total);
        System.out.println("Observação: o UML modela somente entradas; despesas e lucro não são calculados.");
    }
    /** Sobrecarga compatível com a operação de registro de entrada mostrada no diagrama. */
    public boolean gerarRelatorioFinanceiro(int id, double valor, LocalDate data, String descricao) {
        try { registrosFinanceiros.add(new RegistroFinanceiro(id, valor, data, descricao)); proximoRegistroId.updateAndGet(n -> Math.max(n, id + 1)); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }

    private ItemEstoque localizarEstoque(String nomeProduto) {
        return itensEstoque.stream().filter(i -> i.getNome().equalsIgnoreCase(nomeProduto)).findFirst().orElse(null);
    }
    private Map<ItemEstoque, Double> calcularConsumoEstoque(Pedido pedido) {
        Map<ItemEstoque, Double> consumo = new HashMap<>();
        for (ItemPedido item : pedido.getItens()) {
            ItemEstoque estoque = localizarEstoque(item.getProduto().getNome());
            if (estoque != null) consumo.merge(estoque, (double) item.getQuantidade(), Double::sum);
        }
        return consumo;
    }
    private ItemEstoque buscarEstoquePorId(int id) { return itensEstoque.stream().filter(i -> i.getId() == id).findFirst().orElse(null); }
    private Pedido buscarPedidoPorId(int id) { return pedidos.stream().filter(p -> p.getId() == id).findFirst().orElse(null); }
    private int indicePedido(int id) { for (int i=0; i<pedidos.size(); i++) if (pedidos.get(i).getId()==id) return i; return -1; }
}
