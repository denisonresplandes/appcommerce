package dev.denison.appcommerce.shared.presentation;

import dev.denison.appcommerce.shared.application.ConflictException;
import dev.denison.appcommerce.shared.application.NotFoundException;
import dev.denison.appcommerce.shared.domain.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o contrato de erros da API: para cada tipo de erro, o status, o {@code Content-Type} e o corpo da resposta.
 *
 * <p>Usa {@code @WebMvcTest}, que sobe só a camada web (controllers, o {@code GlobalExceptionHandler} e a conversão
 * de JSON), sem banco e sem os services. Em vez dos controllers reais, o teste registra um controller próprio
 * ({@link ThrowingController}) que só lança as exceções, então o handler é testado sozinho e isolado dos módulos.</p>
 *
 * <p>Nos erros da aplicação (422, 404, 409) e no 400 de validação o corpo é comparado inteiro e em modo estrito: um
 * campo a mais, a menos ou fora de ordem quebra o teste. Nos erros do próprio Spring MVC (JSON ilegível, 405...) o
 * teste confere status, tipo, título e caminho, mas não o texto do {@code detail}, que é do Spring e pode mudar
 * numa atualização do framework.</p>
 */
@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    // Exceções "de módulo", como UserNotFoundException: herdam das bases e o handler deve tratá-las sem conhecê-las.
    static class ProductNotFoundException extends NotFoundException {
        ProductNotFoundException(String message) {
            super(message);
        }
    }

    static class SkuAlreadyInUseException extends ConflictException {
        SkuAlreadyInUseException(String message) {
            super(message);
        }
    }

    // Corpo de teste com duas validações no mesmo campo (name), para conferir a ordem da lista de erros.
    record SampleRequest(
            @NotBlank(message = "name is required")
            @Size(min = 3, message = "name is too short")
            String name,

            @NotNull(message = "age is required")
            @Min(value = 18, message = "age must be at least 18")
            Integer age
    ) {}

    // Controller só para o teste: cada rota lança uma exceção, ou valida o corpo, ou exige um id numérico.
    @RestController
    static class ThrowingController {

        @GetMapping("/test/domain")
        void domain() {
            throw new DomainException("domain rule violated");
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("thing not found");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("thing already exists");
        }

        @GetMapping("/test/product-not-found")
        void productNotFound() {
            throw new ProductNotFoundException("product not found with id 9");
        }

        @GetMapping("/test/sku-in-use")
        void skuInUse() {
            throw new SkuAlreadyInUseException("sku is already in use");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody SampleRequest request) {
        }

        @GetMapping("/test/items/{id}")
        void item(@PathVariable Long id) {
        }

        @GetMapping("/test/positive/{id}")
        void positive(@PathVariable @Positive(message = "id must be positive") Long id) {
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // Confere o que todo erro tem em comum: status, Content-Type problem+json e os campos status, title e instance.
    private static ResultActions expectProblem(ResultActions result, int expectedStatus, String title, String path)
            throws Exception {
        return result
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    static Stream<Arguments> applicationErrors() {
        return Stream.of(
                Arguments.of("/test/domain", 422, "Unprocessable Content", "domain rule violated"),
                Arguments.of("/test/not-found", 404, "Not Found", "thing not found"),
                Arguments.of("/test/conflict", 409, "Conflict", "thing already exists"),
                // Subclasses criadas pelos módulos caem no handler da base.
                Arguments.of("/test/product-not-found", 404, "Not Found", "product not found with id 9"),
                Arguments.of("/test/sku-in-use", 409, "Conflict", "sku is already in use")
        );
    }

    // DomainException vira 422, NotFoundException 404 e ConflictException 409 (incluindo as subclasses). O corpo é
    // exatamente este: sem "type" e sem nenhum campo extra, e o detail é a mensagem da exceção.
    @ParameterizedTest
    @MethodSource("applicationErrors")
    void translatesApplicationExceptionsToProblemDetail(String path, int expectedStatus, String title, String detail)
            throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"status":%d,"title":"%s","detail":"%s","instance":"%s"}
                        """.formatted(expectedStatus, title, detail, path), true));
    }

    // Corpo vazio: os dois campos obrigatórios falham. A lista errors vem ordenada pelo nome do campo (age, name).
    @Test
    void validationErrorListsEveryInvalidField() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "status": 400,
                          "title": "Bad Request",
                          "detail": "Invalid request content.",
                          "instance": "/test/validation",
                          "errors": [
                            {"field": "age", "message": "age is required"},
                            {"field": "name", "message": "name is required"}
                          ]
                        }
                        """, true));
    }

    // Um campo com duas violações (name em branco e curto) aparece duas vezes, em ordem alfabética de mensagem. A
    // ordem é fixa porque o handler ordena a lista; as violações do Bean Validation chegam sem ordem garantida, e ela
    // pode mudar de uma requisição para outra. Por isso a mesma requisição é repetida: sem a ordenação, alguma das
    // repetições quase certamente viria em outra ordem e o teste falharia.
    @Test
    void validationErrorKeepsAFixedOrderForTheSameField() throws Exception {
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"  \",\"age\":10}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(3))
                    .andExpect(content().json("""
                            {
                              "status": 400,
                              "title": "Bad Request",
                              "detail": "Invalid request content.",
                              "instance": "/test/validation",
                              "errors": [
                                {"field": "age", "message": "age must be at least 18"},
                                {"field": "name", "message": "name is required"},
                                {"field": "name", "message": "name is too short"}
                              ]
                            }
                            """, true));
        }
    }

    // Validação de um parâmetro do caminho (e não do corpo): também é 400 e traz a mesma lista errors, com o nome do
    // parâmetro em "field". O detail é o texto do Spring para esse tipo de erro.
    @Test
    void pathParameterValidationErrorListsTheParameter() throws Exception {
        mockMvc.perform(get("/test/positive/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.instance").value("/test/positive/0"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].message").value("id must be positive"));
    }

    // Um id positivo passa pela validação do parâmetro.
    @Test
    void validPathParameterIsAccepted() throws Exception {
        mockMvc.perform(get("/test/positive/1")).andExpect(status().isOk());
    }

    // Um corpo válido não gera erro nenhum.
    @Test
    void validBodyIsAccepted() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"age\":30}"))
                .andExpect(status().isOk());
    }

    // A partir daqui, erros que o próprio Spring MVC lança: o handler os converte para o mesmo formato (problem+json),
    // então a API não tem dois formatos de erro.

    @Test
    void unreadableJsonIsABadRequestProblem() throws Exception {
        expectProblem(
                mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":")),
                400, "Bad Request", "/test/validation");
    }

    @Test
    void nonNumericIdIsABadRequestProblem() throws Exception {
        expectProblem(mockMvc.perform(get("/test/items/abc")), 400, "Bad Request", "/test/items/abc");
    }

    @Test
    void unsupportedMethodIsAMethodNotAllowedProblem() throws Exception {
        expectProblem(mockMvc.perform(delete("/test/domain")), 405, "Method Not Allowed", "/test/domain");
    }

    @Test
    void unsupportedContentTypeIsAnUnsupportedMediaTypeProblem() throws Exception {
        expectProblem(
                mockMvc.perform(post("/test/validation").contentType(MediaType.TEXT_PLAIN).content("x")),
                415, "Unsupported Media Type", "/test/validation");
    }

    @Test
    void unknownRouteIsANotFoundProblem() throws Exception {
        expectProblem(mockMvc.perform(get("/does-not-exist")), 404, "Not Found", "/does-not-exist");
    }
}
