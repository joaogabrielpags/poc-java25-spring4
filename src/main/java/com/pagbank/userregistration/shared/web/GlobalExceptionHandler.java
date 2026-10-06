package com.pagbank.userregistration.shared.web;

import com.pagbank.userregistration.addresslookup.CepNotFoundException;
import com.pagbank.userregistration.user.domain.UserAlreadyExistsException;
import com.pagbank.userregistration.user.domain.UserNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tradução centralizada de exceções para RFC 9457 (Problem Details) — T2.4.
 *
 * <p>Mapeamentos: 400 (validação de Bean Validation), 404 ({@link UserNotFoundException}),
 * 409 ({@link UserAlreadyExistsException}), 422 ({@link CepNotFoundException}) e 500 genérico
 * (sem vazar detalhes internos).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final URI VALIDATION_ERROR_TYPE = URI.create("urn:problem:validation-error");
	private static final URI USER_NOT_FOUND_TYPE = URI.create("urn:problem:user-not-found");
	private static final URI USER_ALREADY_EXISTS_TYPE = URI.create("urn:problem:user-already-exists");
	private static final URI CEP_NOT_FOUND_TYPE = URI.create("urn:problem:cep-not-found");
	private static final URI INTERNAL_ERROR_TYPE = URI.create("urn:problem:internal-error");

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ProblemDetail problemDetail =
				ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
		problemDetail.setTitle("Erro de validação");
		problemDetail.setType(VALIDATION_ERROR_TYPE);
		problemDetail.setProperty("errors", fieldErrors(ex));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problemDetail.setTitle("Usuário não encontrado");
		problemDetail.setType(USER_NOT_FOUND_TYPE);
		problemDetail.setProperty("userId", ex.userId().value());
		return problemDetail;
	}

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problemDetail.setTitle("Usuário já cadastrado");
		problemDetail.setType(USER_ALREADY_EXISTS_TYPE);
		return problemDetail;
	}

	@ExceptionHandler(CepNotFoundException.class)
	public ProblemDetail handleCepNotFound(CepNotFoundException ex) {
		ProblemDetail problemDetail =
				ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problemDetail.setTitle("CEP não encontrado");
		problemDetail.setType(CEP_NOT_FOUND_TYPE);
		problemDetail.setProperty("cep", ex.cep().formatted());
		return problemDetail;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpectedError(Exception ex) {
		log.error("Erro inesperado não tratado", ex);
		ProblemDetail problemDetail =
				ProblemDetail.forStatusAndDetail(
						HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado. Contate o suporte.");
		problemDetail.setTitle("Erro interno");
		problemDetail.setType(INTERNAL_ERROR_TYPE);
		return problemDetail;
	}

	private static List<Map<String, String>> fieldErrors(MethodArgumentNotValidException ex) {
		return ex.getBindingResult().getFieldErrors().stream()
				.map(GlobalExceptionHandler::toFieldErrorMap)
				.toList();
	}

	private static Map<String, String> toFieldErrorMap(FieldError fieldError) {
		String message = fieldError.getDefaultMessage();
		return Map.of("field", fieldError.getField(), "message", message == null ? "inválido" : message);
	}
}
