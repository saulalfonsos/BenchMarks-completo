package gal.usc.etse.es.restdemo.controller;

import gal.usc.etse.es.restdemo.exception.*;
import gal.usc.etse.es.utils.patch.exceptions.JsonPatchFailedException;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Xestión centralizada de erros: todas as respostas de erro seguen o formato Problem Details (RFC 9457).
 * Os erros xenéricos de Spring MVC (400 de validación, 405, 406, 413, 415...) xestionaos a superclase.
 */
@RestControllerAdvice
@ApiResponse(responseCode = "400", description = "Petición mal formada ou datos non válidos",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "401", description = "Falta o JWT ou non é válido",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "403", description = "Sen permisos para a operación",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "404", description = "O recurso non existe",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
public class ErrorController extends ResponseEntityExceptionHandler {

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail error = ProblemDetail.forStatusAndDetail(status, detail);
        error.setTitle(title);
        error.setType(URI.create("/error/" + type));
        return error;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse handle(ResourceNotFoundException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.NOT_FOUND, "not-found", ex.getResource() + " not found",
                ex.getResource() + " with id=" + ex.getId() + " does not exist")).build();
    }

    @ExceptionHandler(DuplicateReviewException.class)
    @ApiResponse(responseCode = "409", description = "Conflito co estado actual do recurso",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public ErrorResponse handle(DuplicateReviewException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.CONFLICT, "duplicated-review", "Bench already reviewed",
                        "User " + ex.getAuthor() + " already reviewed bench " + ex.getBenchID() + ". Modify the existing review instead."))
                .header(HttpHeaders.LOCATION, linkTo(methodOn(ReviewController.class).get(ex.getExistingReviewID())).toUri().toString())
                .build();
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ErrorResponse handle(DuplicateUserException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.CONFLICT, "duplicated-user", "User already exists",
                        "Username " + ex.getUser().getUsername() + " is already taken"))
                .header(HttpHeaders.LOCATION, linkTo(methodOn(UserController.class).get(ex.getUser().getUsername())).toUri().toString())
                .build();
    }

    @ExceptionHandler(InvalidPhotoException.class)
    public ErrorResponse handle(InvalidPhotoException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "invalid-photo", "Invalid photo", ex.getMessage()))
                .header(HttpHeaders.ACCEPT, MediaType.IMAGE_JPEG_VALUE + ", " + MediaType.IMAGE_PNG_VALUE)
                .build();
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ErrorResponse handle(BadCredentialsException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.UNAUTHORIZED, "bad-credentials", "Bad credentials",
                "Provided credentials do not match any registered user")).build();
    }

    @ExceptionHandler(JwtException.class)
    public ErrorResponse handle(JwtException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.UNAUTHORIZED, "invalid-token", "Invalid or expired token", ex.getMessage())).build();
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ErrorResponse handle(InvalidRefreshTokenException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.UNAUTHORIZED, "invalid-refresh-token", "Invalid refresh token",
                "The refresh token is invalid or has expired. Log in again.")).build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handle(AccessDeniedException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.FORBIDDEN, "forbidden", "Access denied",
                "You do not have permission to perform this operation")).build();
    }

    @ExceptionHandler(JsonPatchFailedException.class)
    public ErrorResponse handle(JsonPatchFailedException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.UNPROCESSABLE_CONTENT, "patch-failed", "JSON-Patch could not be applied", ex.getMessage())).build();
    }

    /** O resultado dun JSON-Patch non se pode converter ao tipo do recurso (ex.: texto nun campo numérico). */
    @ExceptionHandler(JacksonException.class)
    public ErrorResponse handle(JacksonException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.UNPROCESSABLE_CONTENT, "patch-failed", "JSON-Patch produced an invalid resource",
                ex.getOriginalMessage())).build();
    }

    /** Validación do recurso resultante dun JSON-Patch e dos parámetros validados con @Validated. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handle(ConstraintViolationException ex) {
        Map<String, String> errors = new TreeMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        ProblemDetail error = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed", "The resource contains invalid values");
        error.setProperty("errors", errors);
        return ErrorResponse.builder(ex, error).build();
    }

    @ExceptionHandler({PropertyReferenceException.class, IllegalArgumentException.class})
    public ErrorResponse handleBadRequest(RuntimeException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.BAD_REQUEST, "bad-request", "Bad request", ex.getMessage())).build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handle(DataIntegrityViolationException ex) {
        return ErrorResponse.builder(ex, problem(HttpStatus.CONFLICT, "conflict", "Conflict",
                "The operation conflicts with the current state of the resource")).build();
    }

    /** Engade ao Problem Detail estándar a lista de campos non válidos. */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                            HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail body = ex.getBody();
        body.setTitle("Validation failed");
        body.setType(URI.create("/error/validation"));
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }
}
