package br.com.deliciacaseira.dominio;

/** Violação de regra de negócio. A mensagem é mostrada ao usuário, em linguagem simples. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) { super(mensagem); }
}
