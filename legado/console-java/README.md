# DeliciaCaseiraDaElaine

Sistema de pedidos, produtos, pagamentos e estoque em Java puro, com dados mantidos em listas na memória.

## Ambiente
- JDK 21 (compilação com `--release 21`)
- Sem recursos experimentais e sem bibliotecas externas de execução
- Pacote: `br.com.deliciacaseira`
- Classe principal: `br.com.deliciacaseira.Principal`

## Abrir no NetBeans
1. Extraia o ZIP.
2. No NetBeans, escolha **File > Open Project**.
3. Selecione a pasta `DeliciaCaseiraDaElaine` (a pasta que contém `pom.xml`).
4. Configure o JDK 21 em **Tools > Java Platforms**, se necessário.
5. Execute a classe `Principal` ou use **Run Project**.

## Executar pelo terminal
Com JDK 21 e Maven instalados:

```bash
mvn clean compile
mvn exec:java
```

Alternativamente, compile sem Maven:

```bash
javac --release 21 -encoding UTF-8 -d target/classes src/main/java/br/com/deliciacaseira/*.java
java -cp target/classes br.com.deliciacaseira.Principal
```

## Observações de fidelidade ao UML
- O diagrama usa `date`; a implementação utiliza `java.time.LocalDate`, API padrão e segura do Java.
- O diagrama menciona `Item` em operações de estoque, mas não apresenta uma classe `Item` entre as dez classes solicitadas. Para manter as dez classes, as operações foram tipadas com `ItemEstoque` e há uma sobrecarga que associa `Produto` e `ItemEstoque`.
- O UML não define atributo `categoria` em `Produto` nem em `ItemEstoque`. Por isso, `listarItensPorCategoria` pesquisa o texto informado no nome e na descrição do produto, sem inventar um novo atributo.
- O controle de estoque associa produto e insumo pelo nome idêntico. Ao cadastrar um pedido, o saldo correspondente é baixado se existir um item de estoque com o mesmo nome. Produtos sem item de estoque cadastrado não têm saldo controlado.
- O UML define registros financeiros apenas para entradas; despesas e lucro não são modelados nem calculados.
- Os dados ficam em memória e são perdidos ao encerrar o programa, conforme solicitado (sem banco de dados).

## Classes
Pessoa, Cliente, Administradora, Pedido, ItemPedido, Produto, Pagamento, RegistroFinanceiro, ItemEstoque, GerenciadorDoSistema e Principal (classe de demonstração).
