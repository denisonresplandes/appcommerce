package dev.denison.appcommerce.shared.presentation;

import dev.denison.appcommerce.shared.application.ConflictException;
import dev.denison.appcommerce.shared.application.NotFoundException;
import dev.denison.appcommerce.shared.domain.DomainException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Traduz as exceções da aplicação em respostas HTTP, para <b>todos</b> os controllers de todos os módulos.
 *
 * <p>Os controllers e os services só lançam exceções; quem decide o status HTTP é esta classe. Todo erro sai no
 * formato padrão {@link ProblemDetail} (RFC 9457, {@code Content-Type: application/problem+json}), com
 * {@code status}, {@code title}, {@code detail} e {@code instance} (o caminho da requisição).</p>
 *
 * <p>Os handlers valem também para as <b>subclasses</b>: {@code UserNotFoundException} é tratada pelo handler de
 * {@link NotFoundException}, então os módulos criam suas exceções sem precisar mexer aqui.</p>
 *
 * <p>Ao estender {@link ResponseEntityExceptionHandler}, os erros do próprio Spring MVC (JSON ilegível, id que não
 * é número, rota inexistente, método ou tipo de conteúdo não aceito) também saem como {@code ProblemDetail}. Sem
 * isso a API teria dois formatos de erro.</p>
 *
 * <p>Esta é a única parte da {@code presentation} que enxerga o {@link DomainException}, justamente para traduzi-lo
 * em HTTP.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Um item da lista {@code errors} do 400: o campo (ou parâmetro) que falhou e a mensagem de validação dele. */
    public record InvalidField(String field, String message) {}

    /**
     * Regra de negócio violada (e-mail inválido, nome curto etc.): 422. O mesmo pedido falharia sempre.
     * O {@code detail} é a mensagem da exceção, que é o que o cliente da API recebe.
     */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    /** Recurso que não existe (por exemplo, usuário com aquele id): 404. */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** Dado válido que colide com o que já existe (por exemplo, e-mail já cadastrado): 409. */
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflictException(ConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /**
     * Falha do Bean Validation no corpo da requisição ({@code @Valid}): 400.
     *
     * <p>Aproveita o {@code ProblemDetail} que o Spring já monta para esse erro (status 400) e acrescenta a lista
     * {@code errors}, com o campo e a mensagem de cada validação que falhou. As mensagens vêm do
     * {@code messages.properties}.</p>
     *
     * <p>A ordem da lista está em {@link #withErrors}.</p>
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<InvalidField> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                .toList();

        return handleExceptionInternal(exception, withErrors(exception.getBody(), errors), headers, status, request);
    }

    /**
     * Falha do Bean Validation em parâmetros do método do controller, fora do corpo (por exemplo,
     * {@code @Positive} no id do caminho): 400, no mesmo formato do erro de validação do corpo.
     *
     * <p>Aqui o {@code field} é o nome do parâmetro ({@code id}, em {@code GET /users/0}). Se um parâmetro for um
     * objeto com {@code @Valid}, o {@code field} é o nome do campo dentro dele, como no erro do corpo.</p>
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<InvalidField> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors()
                        .forEach(error -> errors.add(new InvalidField(error.getField(), error.getDefaultMessage())));
            } else {
                String parameterName = result.getMethodParameter().getParameterName();
                result.getResolvableErrors()
                        .forEach(error -> errors.add(new InvalidField(parameterName, error.getDefaultMessage())));
            }
        }

        return handleExceptionInternal(exception, withErrors(exception.getBody(), errors), headers, status, request);
    }

    /**
     * Acrescenta a lista {@code errors} ao {@code ProblemDetail}, ordenada por campo e depois por mensagem. Os dois
     * handlers de validação usam este método, então a ordem e o formato são os mesmos.
     *
     * <p>A ordenação é necessária porque as violações chegam de um conjunto, sem ordem garantida, e sem ela a resposta
     * poderia mudar de uma execução para outra. O {@code nullsLast} evita erro caso alguma mensagem venha nula.</p>
     */
    private static ProblemDetail withErrors(ProblemDetail problem, List<InvalidField> errors) {
        problem.setProperty("errors", errors.stream()
                .sorted(Comparator.comparing(InvalidField::field)
                        .thenComparing(InvalidField::message, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
        return problem;
    }
}
