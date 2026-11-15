package br.com.deliciacaseira.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Períodos aceitos nos relatórios (nota do diagrama UML: dia, semana, mês, ano ou todos;
 * qualquer outro valor é rejeitado). Os intervalos seguem o calendário:
 * SEMANA = segunda a domingo da data de referência; MES = mês da data de referência.
 */
public enum Periodo {
    DIA, SEMANA, MES, ANO, TODOS;

    /** Limites usados para "TODOS" (evita parâmetros nulos nas consultas). */
    public static final LocalDate INICIO_DOS_TEMPOS = LocalDate.of(2000, 1, 1);
    public static final LocalDate FIM_DOS_TEMPOS = LocalDate.of(2999, 12, 31);

    public Intervalo intervalo(LocalDate referencia) {
        if (referencia == null) throw new IllegalArgumentException("Data de referência obrigatória.");
        return switch (this) {
            case DIA -> new Intervalo(referencia, referencia);
            case SEMANA -> new Intervalo(
                    referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    referencia.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case MES -> new Intervalo(referencia.withDayOfMonth(1),
                    referencia.with(TemporalAdjusters.lastDayOfMonth()));
            case ANO -> new Intervalo(referencia.withDayOfYear(1),
                    referencia.with(TemporalAdjusters.lastDayOfYear()));
            case TODOS -> new Intervalo(INICIO_DOS_TEMPOS, FIM_DOS_TEMPOS);
        };
    }

    public record Intervalo(LocalDate inicio, LocalDate fim) {
        public boolean contem(LocalDate data) {
            return data != null && !data.isBefore(inicio) && !data.isAfter(fim);
        }
    }
}
