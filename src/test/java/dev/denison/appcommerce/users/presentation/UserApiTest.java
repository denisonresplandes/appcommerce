package dev.denison.appcommerce.users.presentation;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de API dos endpoints de usuário: sobem a aplicação inteira (controller, services, domínio, JPA e o banco
 * H2) e fazem requisições HTTP simuladas com {@code MockMvc}, conferindo status, cabeçalhos e JSON de cada resposta.
 *
 * <p>Aqui nada é simulado (mock): o que o teste exercita é o caminho completo, do JSON ao banco e de volta. As
 * regras de cada camada já têm os seus testes de unidade, e o formato dos erros está no
 * {@code GlobalExceptionHandlerTest}. Este teste confere que as peças funcionam juntas.</p>
 *
 * <p>A classe é {@code @Transactional}: como o {@code MockMvc} executa na mesma thread do teste, tudo o que uma
 * requisição grava é desfeito no fim de cada teste. Assim os testes não interferem uns nos outros e a lista de
 * usuários começa sempre só com os dois do {@code import.sql} (Maria, id 1, e Alex, id 2). O id de um usuário novo
 * não é previsível (o contador do banco não volta atrás), então esses testes nunca esperam um id fixo.</p>
 *
 * <p>Os bodies de erro e de usuário são comparados inteiros, em modo estrito: um campo a mais, a menos ou fora de
 * ordem quebra o teste.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiTest {

    // Os dois usuários do import.sql.
    private static final String MARIA = """
            {"id":1,"name":"Maria Brown","email":"maria@gmail.com","phone":"98888888888","dateOfBirth":"2001-07-25"}""";
    private static final String ALEX = """
            {"id":2,"name":"Alex Green","email":"alex@gmail.com","phone":"97777777777","dateOfBirth":"1987-12-13"}""";

    // E-mail com formato válido e 101 caracteres, um a mais que o limite do DTO e da coluna.
    private static final String TOO_LONG_EMAIL = "a".repeat(60) + "@" + "b".repeat(36) + ".com";

    @Autowired
    private MockMvc mockMvc;

    // Corpo de um POST /users. Os valores são passados como texto, para os testes de erro poderem mandar dados inválidos.
    private static String body(String name, String email, String phone, String dateOfBirth) {
        return """
                {"name":"%s","email":"%s","phone":"%s","dateOfBirth":"%s"}""".formatted(name, email, phone, dateOfBirth);
    }

    private static String validBody() {
        return body("Ana Souza", "ana@mail.com", "11912345678", "1990-01-01");
    }

    private ResultActions postUser(String json) throws Exception {
        return mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // Erro de regra de negócio, de conflito etc.: o corpo é o ProblemDetail inteiro, sem nenhum campo extra.
    private static String problem(int status, String title, String detail, String path) {
        return """
                {"status":%d,"title":"%s","detail":"%s","instance":"%s"}""".formatted(status, title, detail, path);
    }

    // ---------------------------------------------------------------------------------------------------------
    // POST /users
    // ---------------------------------------------------------------------------------------------------------

    // Caminho feliz: 201, o usuário no corpo (com os dados já normalizados pelo domínio) e o cabeçalho Location
    // apontando para o recurso novo, com o mesmo id do corpo.
    @Test
    void createsTheUserAnswering201WithTheLocationHeader() throws Exception {
        // Mandamos o e-mail em maiúsculas e o nome com espaços repetidos: o domínio normaliza os dois.
        MvcResult result = postUser(body("Ana   Souza", "ANA@Mail.com", "11912345678", "1990-01-01"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.phone").value("11912345678"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-01-01"))
                .andExpect(jsonPath("$.length()").value(5))
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://localhost/users/" + id);
    }

    // O Location é um endereço que funciona: seguir o link devolve o mesmo usuário que o POST respondeu.
    @Test
    void theLocationHeaderPointsToTheCreatedUser() throws Exception {
        MvcResult created = postUser(validBody()).andExpect(status().isCreated()).andReturn();
        String location = created.getResponse().getHeader("Location");

        mockMvc.perform(get(URI.create(location)))
                .andExpect(status().isOk())
                .andExpect(content().json(created.getResponse().getContentAsString(), true));
    }

    // Telefone com máscara é recusado: o telefone da API tem só dígitos. Esse formato (15 caracteres) falha nas duas
    // validações do DTO, o tamanho e o "só dígitos", então as duas mensagens aparecem, em ordem alfabética. (O value
    // object Phone aceitaria a máscara, mas o pedido nem chega até ele.)
    @Test
    void rejectsAFormattedPhoneNumber() throws Exception {
        postUser(body("Ana Souza", "ana@mail.com", "(11) 91234-5678", "1990-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Invalid request content.","instance":"/users",
                         "errors":[{"field":"phone","message":"user phone number must contain only digits"},
                                   {"field":"phone","message":"user phone number must have 10 or 11 digits"}]}""",
                        true));
    }

    // Caracteres que não são dígitos são recusados mesmo quando o tamanho está certo. Sem essa validação o value object
    // Phone, que descarta tudo o que não é dígito, gravaria um número diferente do enviado (por exemplo, "1191234567a"
    // viraria "1191234567" e daria 201).
    @ParameterizedTest
    @ValueSource(strings = {"1191234567a", "11-91234567", "12345abcde"})
    void rejectsAPhoneNumberWithNonDigitCharacters(String phone) throws Exception {
        postUser(body("Ana Souza", "ana@mail.com", phone, "1990-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Invalid request content.","instance":"/users",
                         "errors":[{"field":"phone","message":"user phone number must contain only digits"}]}""",
                        true));
    }

    // E-mail com mais de 100 caracteres (o da coluna): 400 no POST e também no GET /users?email=, com a mesma mensagem.
    @Test
    void rejectsATooLongEmailInThePost() throws Exception {
        postUser(body("Ana Souza", TOO_LONG_EMAIL, "11912345678", "1990-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Invalid request content.","instance":"/users",
                         "errors":[{"field":"email","message":"user email address must have at most 100 characters"}]}""",
                        true));
    }

    // E-mail já cadastrado: 409. Vale para o e-mail do import.sql e também para a mesma pessoa escrevendo com
    // outras letras maiúsculas, porque a conferência usa o e-mail normalizado. A mensagem traz o e-mail já
    // normalizado, mesmo quando o pedido veio com maiúsculas.
    @ParameterizedTest
    @ValueSource(strings = {"maria@gmail.com", "MARIA@Gmail.com"})
    void rejectsAnEmailAlreadyInUse(String email) throws Exception {
        postUser(body("Outra Maria", email, "11912345678", "1990-01-01"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json(
                        problem(409, "Conflict", "user email address maria@gmail.com is already in use", "/users"),
                        true));
    }

    // Dados que passam na validação do corpo (formato), mas o domínio recusa: 422 com a mensagem do domínio. Telefone
    // não aparece aqui: o DTO já garante de 10 a 11 dígitos, e é só isso que o domínio exige.
    static Stream<Arguments> businessRuleViolations() {
        return Stream.of(
                // O @Email aceita "ana@mail", mas o domínio exige uma extensão (".com" etc.).
                Arguments.of(body("Ana Souza", "ana@mail", "11912345678", "1990-01-01"),
                        "user email address is invalid"),
                // O nome não pode ter números.
                Arguments.of(body("Ana 2", "ana@mail.com", "11912345678", "1990-01-01"),
                        "user name is invalid: use only letters, spaces, hyphens and apostrophes"),
                // O DTO conta o tamanho do texto cru (2 caracteres), mas o domínio tira os espaços e fica com 1.
                Arguments.of(body("A ", "ana@mail.com", "11912345678", "1990-01-01"),
                        "user name is too short")
        );
    }

    @ParameterizedTest
    @MethodSource("businessRuleViolations")
    void rejectsABusinessRuleViolation(String json, String message) throws Exception {
        postUser(json)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json(problem(422, "Unprocessable Content", message, "/users"), true));
    }

    // Corpo vazio: os quatro campos são obrigatórios. A lista errors vem ordenada pelo nome do campo.
    @Test
    void rejectsAnEmptyBodyListingEveryRequiredField() throws Exception {
        postUser("{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Invalid request content.","instance":"/users",
                         "errors":[
                           {"field":"dateOfBirth","message":"user date of birth is required"},
                           {"field":"email","message":"user email address is required"},
                           {"field":"name","message":"user name is required"},
                           {"field":"phone","message":"user phone number is required"}]}""", true));
    }

    // Valores inválidos: cada campo aparece com a sua mensagem (vinda do messages.properties, com os limites do @Size).
    @Test
    void rejectsInvalidValuesListingEachField() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        postUser(body("A", "x", "1", tomorrow))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Invalid request content.","instance":"/users",
                         "errors":[
                           {"field":"dateOfBirth","message":"user date of birth cannot be later than now"},
                           {"field":"email","message":"user email address is invalid"},
                           {"field":"name","message":"user name must have between 2 and 100 characters"},
                           {"field":"phone","message":"user phone number must have 10 or 11 digits"}]}""", true));
    }

    // Erros que o Spring acusa antes da validação: JSON quebrado e data que não é uma data.
    @Test
    void rejectsAMalformedJson() throws Exception {
        postUser("{\"name\":")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsADateThatIsNotADate() throws Exception {
        postUser(body("Ana Souza", "ana@mail.com", "11912345678", "ontem"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // Nenhuma das requisições recusadas grava algo: depois de um 400, um 409 e um 422, a lista continua só com os
    // dois usuários do import.sql.
    @Test
    void rejectedRequestsDoNotCreateAnyUser() throws Exception {
        postUser("{}").andExpect(status().isBadRequest());
        postUser(body("Outra Maria", "maria@gmail.com", "11912345678", "1990-01-01"))
                .andExpect(status().isConflict());
        postUser(body("Ana 2", "ana@mail.com", "11912345678", "1990-01-01"))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[" + MARIA + "," + ALEX + "]", true));
    }

    // ---------------------------------------------------------------------------------------------------------
    // GET /users/{id}
    // ---------------------------------------------------------------------------------------------------------

    @Test
    void returnsTheUserById() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(MARIA, true));
    }

    // Usuário que não existe: 404 com a mensagem do UserNotFoundException.
    @Test
    void returns404WhenTheUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/users/9999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json(
                        problem(404, "Not Found", "user not found with id 9999", "/users/9999"), true));
    }

    // Id que não é um número positivo: 400 já na entrada (o @Positive do controller), sem chegar ao service.
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectsANonPositiveId(String id) throws Exception {
        mockMvc.perform(get("/users/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Validation failure","instance":"/users/%s",
                         "errors":[{"field":"id","message":"user id must be a positive number"}]}"""
                        .formatted(id), true));
    }

    @Test
    void rejectsANonNumericId() throws Exception {
        mockMvc.perform(get("/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---------------------------------------------------------------------------------------------------------
    // GET /users?email=
    // ---------------------------------------------------------------------------------------------------------

    // Com o parâmetro email a resposta é um único usuário (um objeto), e não a lista.
    @Test
    void returnsTheUserByEmail() throws Exception {
        mockMvc.perform(get("/users").param("email", "maria@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(MARIA, true));
    }

    // A busca usa o e-mail normalizado: escrever com outras letras maiúsculas acha o mesmo usuário.
    @Test
    void findsTheUserIgnoringCase() throws Exception {
        mockMvc.perform(get("/users").param("email", "MARIA@Gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().json(MARIA, true));
    }

    // E-mail que ninguém usa: 404 com o e-mail na mensagem, já normalizado (em minúsculas), como no 409 do POST.
    @Test
    void returns404WhenNoUserHasTheEmail() throws Exception {
        mockMvc.perform(get("/users").param("email", "NOBODY@Mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json(
                        problem(404, "Not Found", "user not found with email nobody@mail.com", "/users"), true));
    }

    // E-mail com mais de 100 caracteres: 400 na entrada, igual ao POST /users (antes o GET chegava ao domínio e dava 422).
    @Test
    void rejectsATooLongEmailInTheSearch() throws Exception {
        mockMvc.perform(get("/users").param("email", TOO_LONG_EMAIL))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Validation failure","instance":"/users",
                         "errors":[{"field":"email","message":"user email address must have at most 100 characters"}]}""",
                        true));
    }

    // E-mail vazio: 400 já na entrada (o @NotBlank do controller), com a mesma lista errors. O @Email aceita o texto
    // vazio, então só a mensagem de "obrigatório" aparece.
    @Test
    void rejectsAnEmptyEmail() throws Exception {
        mockMvc.perform(get("/users").param("email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Validation failure","instance":"/users",
                         "errors":[{"field":"email","message":"user email address is required"}]}""", true));
    }

    // E-mail só com espaços: falha no @NotBlank e também no @Email, então aparecem as duas mensagens, em ordem
    // alfabética (é o mesmo que acontece no POST /users).
    @Test
    void rejectsAnEmailMadeOnlyOfSpaces() throws Exception {
        mockMvc.perform(get("/users").param("email", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Validation failure","instance":"/users",
                         "errors":[{"field":"email","message":"user email address is invalid"},
                                   {"field":"email","message":"user email address is required"}]}""", true));
    }

    // A mensagem do erro não depende do idioma da requisição: vem do messages.properties, e não do texto padrão do
    // Bean Validation (que sairia em português com Accept-Language pt-BR).
    @Test
    void theValidationMessageDoesNotDependOnTheRequestLanguage() throws Exception {
        mockMvc.perform(get("/users").param("email", "").header("Accept-Language", "pt-BR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("user email address is required"));
    }

    // Formato de e-mail inválido: 400 já na entrada (o @Email do controller), sem chegar ao service. Espaços nas pontas
    // também são recusados aqui, como no POST /users: o @Email não aceita, então a normalização do domínio não chega a
    // acontecer.
    @ParameterizedTest
    @ValueSource(strings = {"abc", "maria@", " maria@gmail.com "})
    void rejectsAnEmailWithAnInvalidFormat(String email) throws Exception {
        mockMvc.perform(get("/users").param("email", email))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"status":400,"title":"Bad Request","detail":"Validation failure","instance":"/users",
                         "errors":[{"field":"email","message":"user email address is invalid"}]}""", true));
    }

    // O @Email é mais permissivo que o domínio: "ana@mail" passa na entrada, mas o Email do domínio exige uma extensão
    // (".com" etc.). Por isso este caso chega ao service e vira 422, a mesma regra do POST /users.
    @Test
    void theDomainStillRejectsWhatTheEntryValidationLetsThrough() throws Exception {
        mockMvc.perform(get("/users").param("email", "ana@mail"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json(
                        problem(422, "Unprocessable Content", "user email address is invalid", "/users"), true));
    }

    // ---------------------------------------------------------------------------------------------------------
    // GET /users
    // ---------------------------------------------------------------------------------------------------------

    // A lista vem ordenada pelo id: Maria (1) e depois Alex (2).
    @Test
    void listsTheUsersOrderedById() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[" + MARIA + "," + ALEX + "]", true));
    }

    // Depois de criar um usuário, ele aparece no fim da lista (o id novo é o maior).
    @Test
    void listIncludesTheCreatedUser() throws Exception {
        postUser(validBody()).andExpect(status().isCreated());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[2].email").value("ana@mail.com"));
    }
}
