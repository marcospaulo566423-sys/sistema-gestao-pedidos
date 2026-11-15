package br.com.deliciacaseira.config;

import br.com.deliciacaseira.servico.ArmazenamentoFotos;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ArmazenamentoFotos fotos;

    public WebConfig(ArmazenamentoFotos fotos) { this.fotos = fotos; }

    /** Fotos dos produtos enviadas pela administradora, servidas a partir da pasta de uploads. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Files.createDirectories(fotos.getDiretorio());
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de fotos: " + fotos.getDiretorio(), e);
        }
        String local = fotos.getDiretorio().toUri().toString();
        registry.addResourceHandler("/fotos/**")
                .addResourceLocations(local.endsWith("/") ? local : local + "/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/admin", "/admin/index.html");
        registry.addRedirectViewController("/admin/", "/admin/index.html");
    }
}
