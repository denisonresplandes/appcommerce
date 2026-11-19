package dev.denison.appcommerce.users.application;

import dev.denison.appcommerce.shared.domain.DomainException;
import dev.denison.appcommerce.users.application.command.CreateUserCommand;
import dev.denison.appcommerce.users.application.exception.EmailAlreadyInUseException;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.domain.User;
import dev.denison.appcommerce.users.domain.UserRepository;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes de unidade do {@link UserCommandService} (JUnit + Mockito, sem Spring e sem banco).
 *
 * <p>O {@link UserRepository} é um <b>mock</b>: o Mockito cria um objeto que implementa a interface, e cada teste diz
 * o que ele responde ({@code when(...).thenReturn(...)}) e, depois, confere como o service o usou
 * ({@code verify(...)}). Assim o teste exercita só a lógica do service. O banco de verdade já é coberto pelo
 * {@code JpaUserRepositoryAdapterTest}.</p>
 *
 * <p>O {@code MockitoExtension} é estrito: um {@code when(...)} que o teste não usa, ou que é chamado com outro
 * argumento, faz o teste falhar. Isso evita stubs sobrando.</p>
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 1, 1);

    // Mock do port de escrita. Sem configuração ele responde "nada": existsByEmail dá false e save dá null.
    // Cada teste configura só o que precisa.
    @Mock
    private UserRepository repository;

    // Captura o User que o service entrega ao save, para conferir o que seria gravado no banco.
    @Captor
    private ArgumentCaptor<User> userCaptor;

    private UserCommandService service;

    // O service é criado com new, passando o mock: sem contexto do Spring.
    @BeforeEach
    void setUp() {
        service = new UserCommandService(repository);
    }

    private static CreateUserCommand validCommand() {
        return new CreateUserCommand("Ana Souza", "ana@mail.com", "11912345678",
                DATE_OF_BIRTH);
    }

    // Simula o banco: devolve o usuário recebido, agora com o id 10, como o adapter faz ao salvar.
    private static User savedWithId10(User user) {
        return User.restore(new UserId(10L), user.getName(), user.getEmail(), user.getPhone(),
                user.getDateOfBirth());
    }

    // Caminho feliz: com o e-mail livre, o usuário é salvo e o service devolve a view com o id gerado.
    @Test
    void createsTheUserAndReturnsTheView() {
        // existsByEmail: o mock diz que o e-mail ainda não está em uso.
        when(repository.existsByEmail(new Email("ana@mail.com"))).thenReturn(false);
        // save: o mock faz o papel do banco e devolve o usuário recebido com o id 10 (o argumento chega no lambda).
        when(repository.save(any(User.class))).thenAnswer(invocation ->
                savedWithId10(invocation.getArgument(0)));

        UserView view = service.create(validCommand());

        assertThat(view).isEqualTo(new UserView(10L, "Ana Souza", "ana@mail.com",
                "11912345678", DATE_OF_BIRTH));
    }

    // O que vai para o save é um usuário novo (sem id) e com os dados já normalizados pelos value objects.
    @Test
    void savesANewUserWithNormalizedData() {
        // save: só para o service ter o que devolver; o que interessa é o que ele recebeu (conferido com o captor).
        when(repository.save(any(User.class))).thenAnswer(invocation ->
                savedWithId10(invocation.getArgument(0)));

        service.create(new CreateUserCommand("  Ana   Souza ", " ANA@Mail.com ",
                "(11) 91234-5678", DATE_OF_BIRTH));

        verify(repository).save(userCaptor.capture());
        User userToSave = userCaptor.getValue();
        assertThat(userToSave.getId()).isNull();
        assertThat(userToSave.getName().value()).isEqualTo("Ana Souza");
        assertThat(userToSave.getEmail().value()).isEqualTo("ana@mail.com");
        assertThat(userToSave.getPhone().value()).isEqualTo("11912345678");
        assertThat(userToSave.getDateOfBirth().value()).isEqualTo(DATE_OF_BIRTH);
    }

    // E-mail já em uso: o service lança o conflito, com o e-mail na mensagem, e não salva nada.
    @Test
    void rejectsAnEmailAlreadyInUse() {
        // existsByEmail: o mock diz que já existe um usuário com esse e-mail.
        when(repository.existsByEmail(new Email("ana@mail.com"))).thenReturn(true);

        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessage("user email address ana@mail.com is already in use");

        verify(repository, never()).save(any());
    }

    // A checagem usa o e-mail normalizado: " ANA@Mail.com " conflita com o "ana@mail.com" já cadastrado, então
    // escrever o e-mail com outras maiúsculas ou espaços não burla a regra. A mensagem também traz o e-mail normalizado.
    @Test
    void checksTheNormalizedEmail() {
        // existsByEmail: só responde true para "ana@mail.com". Se o service passasse outro valor, o teste falharia.
        when(repository.existsByEmail(new Email("ana@mail.com"))).thenReturn(true);

        CreateUserCommand command = new CreateUserCommand("Ana Souza", " ANA@Mail.com ", "11912345678", DATE_OF_BIRTH);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessage("user email address ana@mail.com is already in use");

        verify(repository, never()).save(any());
    }

    // Dados inválidos (um caso para cada value object): nome, e-mail, telefone e data de nascimento.
    static Stream<Arguments> invalidCommands() {
        return Stream.of(
                Arguments.of(new CreateUserCommand("A", "ana@mail.com", "11912345678", DATE_OF_BIRTH),
                        "user name is too short"),
                Arguments.of(new CreateUserCommand("Ana Souza", "ana.mail.com", "11912345678", DATE_OF_BIRTH),
                        "user email address is invalid"),
                Arguments.of(new CreateUserCommand("Ana Souza", "ana@mail.com", "123", DATE_OF_BIRTH),
                        "user phone number must have 10 or 11 digits"),
                Arguments.of(new CreateUserCommand("Ana Souza", "ana@mail.com", "11912345678", null),
                        "user date of birth is required")
        );
    }

    // Dado inválido vira DomainException logo no começo: o repositório nem é consultado (nem o existsByEmail).
    @ParameterizedTest
    @MethodSource("invalidCommands")
    void rejectsInvalidDataWithoutTouchingTheRepository(CreateUserCommand command, String message) {
        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(DomainException.class)
                .hasMessage(message);

        verifyNoInteractions(repository);
    }
}
