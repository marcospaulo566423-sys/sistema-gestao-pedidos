package br.com.deliciacaseira.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import java.io.IOException;

/**
 * Segurança (corrige S5): somente a administradora autenticada acessa /admin/** e /api/admin/**
 * (Regras 1 e 6). Tudo o que não está explicitamente liberado é negado.
 */
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    private static final String CSP = String.join("; ",
            "default-src 'self'",
            // Fotos de vitrine (Unsplash) e fontes do Google Fonts no site do cliente: só esses hosts.
            "img-src 'self' data: https://images.unsplash.com",
            "style-src 'self' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "script-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, ProtecaoLogin protecao) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/admin/login.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasRole(AdministradoraDetailsService.PAPEL)
                .requestMatchers("/", "/index.html", "/pedido.html", "/meus-pedidos.html", "/favicon.ico",
                        "/css/**", "/js/**", "/img/**", "/fotos/**", "/api/publico/**", "/api/csrf", "/error").permitAll()
                .anyRequest().denyAll())
            // A área pública não usa sessão/cookie para autorizar nada; o painel exige token CSRF.
            .csrf(c -> c.ignoringRequestMatchers("/api/publico/**"))
            .formLogin(f -> f
                .loginPage("/admin/login.html")
                .loginProcessingUrl("/api/admin/login")
                .usernameParameter("usuario")
                .passwordParameter("senha")
                .successHandler((req, res, auth) -> {
                    protecao.registrarSucesso(auth.getName());
                    res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                })
                .failureHandler((req, res, ex) -> {
                    if (!(ex instanceof LockedException)) protecao.registrarFalha(req.getParameter("usuario"));
                    escreverErro(res, ex instanceof LockedException
                            ? "Muitas tentativas erradas. Aguarde 5 minutos e tente de novo."
                            : "Usuário ou senha incorretos.");
                }))
            .logout(l -> l
                .logoutUrl("/api/admin/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT)))
            .exceptionHandling(e -> e
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/admin/login.html"),
                        AnyRequestMatcher.INSTANCE))
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
            .headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives(CSP))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    private static void escreverErro(HttpServletResponse res, String mensagem) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
