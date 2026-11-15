package br.com.deliciacaseira;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

/** Sistema de gestão da Delícia Caseira da Elaine. */
@SpringBootApplication
public class DeliciaCaseiraApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliciaCaseiraApplication.class, args);
    }

    /** Relógio no fuso de Baixa Verde - MG; injetado para que "hoje" e "amanhã" sejam testáveis. */
    @Bean
    public Clock clock(@Value("${app.zona:America/Sao_Paulo}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
