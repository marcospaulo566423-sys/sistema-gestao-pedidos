# Sistema de Gestão de Encomendas

Sistema web de gestão de pedidos, produção diária, estoque de insumos e controle financeiro para encomendas artesanais.

- **Site do cliente:** visualizar o catálogo, realizar encomendas (retirada ou entrega) e acompanhar o status pelo código do pedido.
- **Painel administrativo:** gestão de pedidos, clientes, catálogo de produtos, lista diária de produção, estoque de insumos, financeiro e relatórios.

**Tecnologias:** Java 25 (LTS) · Spring Boot · Spring Security (senhas com BCrypt) · JPA · Flyway · PostgreSQL · HTML/CSS/JavaScript.

## Pré-requisito

- **JDK 25 (LTS) ou superior** instalado (`java -version`). Não é preciso instalar o Maven: o projeto traz o Maven Wrapper (`mvnw`).

> No Windows (Prompt/PowerShell), troque `./mvnw` por `mvnw.cmd`.

---

## Modo Rápido (sem instalar banco) — recomendado para avaliação

Usa o perfil `local`: um banco H2 em arquivo (modo compatível com PostgreSQL), criado automaticamente na pasta `dados-locais/`.

```bash
git clone [https://github.com/marcospaulo566423-sys/sistema-gestao-pedidos.git](https://github.com/marcospaulo566423-sys/sistema-gestao-pedidos.git)
cd sistema-gestao-pedidos
./mvnw spring-boot:run -Dspring-boot.run.profiles=local