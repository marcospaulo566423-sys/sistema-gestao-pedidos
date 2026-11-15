package br.com.deliciacaseira.servico;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/** Leitura de datas digitadas pelo usuário (corrige B9: aceita 03/11/2026, além de 2026-11-03). */
public final class Datas {
    private Datas() { }

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ISO_LOCAL_DATE);

    /** Devolve a data, ou null se o texto não for uma data válida. */
    public static LocalDate tentarLer(String texto) {
        if (texto == null) return null;
        String t = texto.strip();
        for (DateTimeFormatter f : FORMATOS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignorada) {
                // tenta o próximo formato
            }
        }
        return null;
    }
}
