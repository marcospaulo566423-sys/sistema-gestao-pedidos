package br.com.deliciacaseira.web;

import br.com.deliciacaseira.dominio.NaoEncontradoException;
import br.com.deliciacaseira.dominio.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converte erros em respostas JSON com mensagens simples, sem expor detalhes internos. */
@RestControllerAdvice
public class ApiErros {

    private static final Logger log = LoggerFactory.getLogger(ApiErros.class);

    public record Erro(String mensagem, Map<String, String> campos) {
        Erro(String mensagem) { this(mensagem, Map.of()); }
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Erro> regra(RegraNegocioException e) {
        return ResponseEntity.badRequest().body(new Erro(e.getMessage()));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Erro> naoEncontrado(NaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Erro(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError f : e.getBindingResult().getFieldErrors()) campos.putIfAbsent(f.getField(), f.getDefaultMessage());
        String primeira = campos.values().stream().findFirst().orElse("Confira os dados informados.");
        return ResponseEntity.badRequest().body(new Erro(primeira, campos));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Erro> validacaoParametros(HandlerMethodValidationException e) {
        return ResponseEntity.badRequest().body(new Erro("Confira os dados informados."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Erro> ilegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new Erro("Dados inválidos. Confira os campos preenchidos."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Erro> tipoErrado(MethodArgumentTypeMismatchException e) {
        String msg = "periodo".equals(e.getName())
                ? "Período inválido. Use DIA, SEMANA, MES, ANO ou TODOS."
                : "Valor inválido para \"" + e.getName() + "\".";
        return ResponseEntity.badRequest().body(new Erro(msg));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Erro> arquivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(new Erro("A foto deve ter no máximo 3 MB."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Erro> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new Erro("Não foi possível salvar: os dados conflitam com um registro existente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Erro> inesperado(Exception e) {
        // Erros do próprio Spring (404 de recurso, 405 de método...) mantêm seu status.
        if (e instanceof ErrorResponse er) {
            String msg = er.getStatusCode().value() == 404 ? "Não encontrado." : "Requisição inválida.";
            return ResponseEntity.status(er.getStatusCode()).body(new Erro(msg));
        }
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Erro("Ocorreu um erro inesperado. Tente novamente."));
    }
}
