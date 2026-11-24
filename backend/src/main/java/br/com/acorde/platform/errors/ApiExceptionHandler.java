package br.com.acorde.platform.errors;

import br.com.acorde.auth.service.OrganizationSelectionRequired;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403, "code", "forbidden", "message", "A operação não é permitida para este perfil"));
    }

    @ExceptionHandler(OrganizationSelectionRequired.class)
    ResponseEntity<Map<String, Object>> handleOrganizationSelection(OrganizationSelectionRequired ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "status", ex.getStatus().value(), "code", ex.getCode(), "message", ex.getMessage(),
                "memberships", ex.getMemberships()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "status", ex.getStatus().value(), "code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleInvalidBody(MethodArgumentNotValidException ex) {
        String field = ex.getBindingResult().getFieldErrors().stream().findFirst()
                .map(FieldError::getField).orElse("request");
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "code", "validation_error", "message", "Campo inválido: " + field));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "code", "validation_error", "message", "Parâmetros inválidos"));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<Map<String, Object>> handleUnreadableRequest(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "code", "invalid_request", "message", "Formato da requisição inválido"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        logger.error("Unhandled API error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500, "code", "internal_error", "message", "Erro interno"));
    }
}
