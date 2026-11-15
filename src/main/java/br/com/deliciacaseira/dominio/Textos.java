package br.com.deliciacaseira.dominio;

/** Utilitários de normalização de texto usados pelas entidades. */
public final class Textos {
    private Textos() { }

    /** Remove espaços extras; devolve null para texto vazio. */
    public static String limpar(String texto) {
        if (texto == null) return null;
        String t = texto.strip().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    public static String obrigatorio(String texto, String campo, int max) {
        String t = limpar(texto);
        if (t == null) throw new RegraNegocioException(campo + " é obrigatório.");
        if (t.length() > max) throw new RegraNegocioException(campo + " deve ter no máximo " + max + " caracteres.");
        return t;
    }

    public static String opcional(String texto, String campo, int max) {
        String t = limpar(texto);
        if (t != null && t.length() > max) throw new RegraNegocioException(campo + " deve ter no máximo " + max + " caracteres.");
        return t;
    }

    /**
     * Telefone/WhatsApp: guarda apenas dígitos, com DDD (10 ou 11 dígitos).
     * Aceita o prefixo +55, que é descartado.
     */
    public static String telefone(String telefone) {
        if (telefone == null) throw new RegraNegocioException("Telefone é obrigatório.");
        String digitos = telefone.replaceAll("\\D", "");
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) digitos = digitos.substring(2);
        if (digitos.length() < 10 || digitos.length() > 11)
            throw new RegraNegocioException("Telefone inválido. Informe o DDD e o número, por exemplo (31) 99999-0000.");
        return digitos;
    }
}
