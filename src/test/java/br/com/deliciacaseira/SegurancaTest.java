package br.com.deliciacaseira;

import br.com.deliciacaseira.config.ProtecaoLogin;
import br.com.deliciacaseira.dominio.Administradora;
import br.com.deliciacaseira.dominio.FormaPagamento;
import br.com.deliciacaseira.dominio.TipoEntrega;
import br.com.deliciacaseira.dto.CadastroDto.ProdutoVisao;
import br.com.deliciacaseira.dto.PedidoDto.PublicoVisao;
import br.com.deliciacaseira.dto.PedidoDto.SiteEntrada;
import br.com.deliciacaseira.repositorio.AdministradoraRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Falhas de segurança S1–S8 da auditoria e Regras 1, 2 e 6. */
class SegurancaTest extends TesteIntegracao {

    @Autowired AdministradoraRepository administradoras;
    @Autowired PasswordEncoder encoder;

    // ------------------------------------------------------------------ S1/S2/S4: senha

    @Test @DisplayName("S1: a senha da administradora é guardada como hash BCrypt, nunca em texto puro")
    void senhaComHash() {
        Administradora elaine = administradoras.findByUsuarioIgnoreCase("elaine").orElseThrow();
        assertTrue(elaine.getSenhaHash().startsWith("$2"));
        assertNotEquals("SenhaTeste123", elaine.getSenhaHash());
        assertTrue(encoder.matches("SenhaTeste123", elaine.getSenhaHash()));
    }

    @Test @DisplayName("S3: senha fraca é recusada")
    void senhaFraca() {
        org.junit.jupiter.api.Assertions.assertThrows(br.com.deliciacaseira.dominio.RegraNegocioException.class,
                () -> new Administradora("X", "x", "1234", encoder, LocalDateTime.now()));
        org.junit.jupiter.api.Assertions.assertThrows(br.com.deliciacaseira.dominio.RegraNegocioException.class,
                () -> new Administradora("X", "x", "somenteletras", encoder, LocalDateTime.now()));
    }

    // ------------------------------------------------------------------ S5 / Regras 1 e 6

    @Test @DisplayName("Regra 1/6: API administrativa e financeira exige login (401)")
    void apiAdminExigeLogin() throws Exception {
        for (String url : List.of("/api/admin/pedidos", "/api/admin/financeiro", "/api/admin/relatorios/financeiro",
                "/api/admin/clientes", "/api/admin/painel", "/api/admin/producao")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
    }

    @Test @DisplayName("Regra 1: telas do painel redirecionam para o login")
    void telasAdminRedirecionam() throws Exception {
        mvc.perform(get("/admin/index.html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar.html"));
    }

    @Test @DisplayName("Login correto entra; senha errada é recusada com mensagem simples")
    void login() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("usuario", "elaine").param("senha", "SenhaTeste123"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").with(csrf()).param("usuario", "elaine").param("senha", "errada123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha incorretos."));
    }

    @Test @DisplayName("Login sem token CSRF é recusado")
    void loginSemCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").param("usuario", "elaine").param("senha", "SenhaTeste123"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("Após " + ProtecaoLogin.MAXIMO_TENTATIVAS + " senhas erradas, o usuário é bloqueado por alguns minutos")
    void bloqueioPorTentativas() throws Exception {
        administradoras.save(new Administradora("Teste", "teste.bloqueio", "Correta123", encoder, LocalDateTime.now()));
        for (int i = 0; i < ProtecaoLogin.MAXIMO_TENTATIVAS; i++) {
            mvc.perform(post("/api/auth/login").with(csrf()).param("usuario", "teste.bloqueio").param("senha", "errada" + i))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(csrf()).param("usuario", "teste.bloqueio").param("senha", "Correta123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(containsString("Muitas tentativas")));
    }

    @Test @DisplayName("Ações administrativas exigem token CSRF mesmo com login")
    @WithMockUser(roles = "ADMIN")
    void csrfNoPainel() throws Exception {
        mvc.perform(post("/api/admin/financeiro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10,\"data\":\"2026-01-01\",\"descricao\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("Administradora autenticada acessa o financeiro")
    @WithMockUser(roles = "ADMIN")
    void adminAcessaFinanceiro() throws Exception {
        mvc.perform(get("/api/admin/financeiro").param("periodo", "MES")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists());
        mvc.perform(get("/api/admin/financeiro").param("periodo", "QUINZENA")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("Período inválido")));
    }

    @Test @DisplayName("Caminhos não previstos são negados por padrão")
    void negadoPorPadrao() throws Exception {
        mvc.perform(get("/api/qualquer-coisa")).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("Cabeçalhos de segurança (CSP, nosniff, frame) estão presentes")
    void cabecalhos() throws Exception {
        mvc.perform(get("/api/publico/produtos"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    // ------------------------------------------------------------------ Área pública (Regras 2 e 6)

    @Test @DisplayName("Regra 2: cliente faz pedido pelo site sem login")
    void pedidoPeloSiteSemLogin() throws Exception {
        ProdutoVisao coco = produto("Pão de coco", "12.00");
        String json = """
                {"nome":"Carla","telefone":"(31) 99999-1234","dataEntregaRetirada":"%s","horarioEntregaRetirada":"10:30",
                 "tipoEntrega":"ENTREGA","enderecoEntrega":"Rua das Flores, 100","formaPagamento":"PIX",
                 "observacoes":"Feliz aniversário","itens":[{"produtoId":%d,"quantidade":2}]}
                """.formatted(hoje().plusDays(1), coco.id());
        mvc.perform(post("/api/publico/pedidos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").exists())
                .andExpect(jsonPath("$.total").value(24.0))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test @DisplayName("Regra 3 no site: entrega sem endereço retorna mensagem clara")
    void siteEntregaSemEndereco() throws Exception {
        ProdutoVisao coco = produto("Pão de coco", "12.00");
        String json = """
                {"nome":"Carla","telefone":"31999991234","dataEntregaRetirada":"%s","tipoEntrega":"ENTREGA",
                 "formaPagamento":"PIX","itens":[{"produtoId":%d,"quantidade":1}]}
                """.formatted(hoje().plusDays(1), coco.id());
        mvc.perform(post("/api/publico/pedidos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Para entrega, o endereço é obrigatório."));
    }

    @Test @DisplayName("Meus Pedidos: só mostra o pedido quando código E telefone conferem; sem dados internos")
    void consultaPublica() throws Exception {
        ProdutoVisao coco = produto("Pão de coco", "12.00");
        PublicoVisao p = pedidoService.criarPeloSite(new SiteEntrada("Carla", "31999991234", hoje().plusDays(1),
                LocalTime.of(9, 0), TipoEntrega.RETIRADA, null, FormaPagamento.DINHEIRO, null,
                List.of(new br.com.deliciacaseira.dto.PedidoDto.ItemEntrada(coco.id(), 1))));

        mvc.perform(post("/api/publico/pedidos/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + p.codigo().toLowerCase() + "\",\"telefone\":\"(31) 99999-1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(p.codigo()))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.origem").doesNotExist())
                .andExpect(jsonPath("$.observacoes").doesNotExist())
                .andExpect(content().string(not(containsString("31999991234"))));

        mvc.perform(post("/api/publico/pedidos/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + p.codigo() + "\",\"telefone\":\"31900000000\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(containsString("Confira o código e o telefone")));
    }

    @Test @DisplayName("Cardápio público não expõe campos internos")
    void cardapioPublico() throws Exception {
        produto("Pão de coco", "12.00");
        mvc.perform(get("/api/publico/produtos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Pão de coco"))
                .andExpect(jsonPath("$[0].ativo").doesNotExist());
    }

    // ------------------------------------------------------------------ S7: upload

    @Test @DisplayName("S7: upload de foto valida o conteúdo real (texto com extensão .jpg é recusado)")
    @WithMockUser(roles = "ADMIN")
    void uploadFalso() throws Exception {
        ProdutoVisao coco = produto("Pão de coco", "12.00");
        mvc.perform(multipart("/api/admin/produtos/" + coco.id() + "/foto").with(csrf())
                        .file(new MockMultipartFile("foto", "../../ataque.jpg", "image/jpeg", "<script>".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("JPG, PNG ou WEBP")));
    }

    @Test @DisplayName("S7: foto PNG válida é salva com nome gerado pelo sistema")
    @WithMockUser(roles = "ADMIN")
    void uploadValido() throws Exception {
        ProdutoVisao coco = produto("Pão de coco", "12.00");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};
        mvc.perform(multipart("/api/admin/produtos/" + coco.id() + "/foto").with(csrf())
                        .file(new MockMultipartFile("foto", "minha foto.png", "image/png", png)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.matchesPattern("/fotos/[0-9a-f-]{36}\\.png")));
    }
}
