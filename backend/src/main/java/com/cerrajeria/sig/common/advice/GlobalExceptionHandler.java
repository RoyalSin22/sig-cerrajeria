package com.cerrajeria.sig.common.advice;

import com.cerrajeria.sig.common.exception.CajaNoAbiertaException;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.common.exception.SaldoPendienteException;
import com.cerrajeria.sig.common.exception.StockInsuficienteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Manejo centralizado de excepciones (Fase 5, seccion 2.4). Todas las
 * respuestas de error usan ProblemDetail (RFC 9457) via
 * ResponseEntityExceptionHandler, sin exponer stacktraces (RNF-02).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CajaNoAbiertaException.class)
    public ProblemDetail handleCajaNoAbierta(CajaNoAbiertaException ex) {
        return buildProblem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ProblemDetail handleStockInsuficiente(StockInsuficienteException ex) {
        return buildProblem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SaldoPendienteException.class)
    public ProblemDetail handleSaldoPendiente(SaldoPendienteException ex) {
        return buildProblem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleCredencialesInvalidas(BadCredentialsException ex) {
        // Mensaje generico a proposito: no revelar si el usuario existe o no.
        return buildProblem(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    // Login de un usuario desactivado: sin esto caia en el handler generico (500)
    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleUsuarioInactivo(DisabledException ex) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Usuario inactivo. Consulta con el administrador.");
    }

    // Un empleado en un endpoint solo-ADMIN (@PreAuthorize): sin esto caia en el
    // handler generico y respondia 500 en vez de 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccesoDenegado(AccessDeniedException ex) {
        return buildProblem(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));

        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "Error de validación");
        problem.setProperty("errores", detalle);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        // Fotos de producto e importacion de inventario (limite en spring.servlet.multipart)
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(buildProblem(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo es demasiado grande (máximo 5 MB)."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        // Ultimo recurso: no exponer detalles internos (stack trace) al cliente,
        // pero si registrarlos: sin esto un 500 no deja rastro para diagnosticarlo.
        log.error("Error inesperado procesando la solicitud", ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta nuevamente.");
    }

    private ProblemDetail buildProblem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
