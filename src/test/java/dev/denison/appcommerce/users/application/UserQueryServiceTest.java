package dev.denison.appcommerce.users.application;

import dev.denison.appcommerce.shared.domain.DomainException;
import dev.denison.appcommerce.users.application.exception.UserNotFoundException;
import dev.denison.appcommerce.users.application.port.UserQueryRepository;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes de unidade do {@link UserQueryService} (JUnit + Mockito, sem Spring e sem banco).
 *
 * <p>O {@link UserQueryRepository} é um <b>mock</b>: o Mockito cria um objeto que implementa a interface, e cada
 * teste diz o que ele devolve ({@code when(...).thenReturn(...)}). O service só repassa a consulta e decide o que
 * fazer com o resultado (devolver a view ou lançar "não encontrado"), e é isso que os testes conferem.</p>
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    // Mock do port de leitura. Sem configuração ele responde "nada": Optional vazio e lista vazia.
    @Mock
    private UserQueryRepository repository;

    private UserQueryService service;

    // O service é criado com new, passando o mock: sem contexto do Spring.
    @BeforeEach
    void setUp() {
        service = new UserQueryService(repository);
    }

    private static UserView view(long id, String name, String email) {
        return new UserView(id, name, email, "11912345678", LocalDate.of(1990, 1, 1));
    }

    // Usuário existente: o service devolve a view que o repositório encontrou.
    @Test
    void findsAUserById() {
        UserView maria = view(1L, "Maria Brown", "maria@gmail.com");
        // findById: o mock diz que o usuário de id 1 existe e devolve a view dele.
        when(repository.findById(new UserId(1L))).thenReturn(Optional.of(maria));

        assertThat(service.findById(1L)).isEqualTo(maria);
    }

    // Usuário inexistente: o Optional vazio vira UserNotFoundException (depois vira 404 na API).
    @Test
    void throwsWhenTheUserDoesNotExist() {
        // findById: o mock diz que não há usuário com o id 99.
        when(repository.findById(new UserId(99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("user not found with id 99");
    }

    // Id nulo, zero ou negativo é recusado pelo próprio UserId (DomainException), antes de consultar o repositório.
    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void rejectsInvalidIdsWithoutTouchingTheRepository(Long id) {
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(DomainException.class)
                .hasMessage("user id must be a positive number");

        verifyNoInteractions(repository);
    }

    // Usuário existente: o service devolve a view que o repositório encontrou pelo e-mail.
    @Test
    void findsAUserByEmail() {
        UserView maria = view(1L, "Maria Brown", "maria@gmail.com");
        // findByEmail: o mock diz que existe um usuário com esse e-mail e devolve a view dele.
        when(repository.findByEmail(new Email("maria@gmail.com"))).thenReturn(Optional.of(maria));

        assertThat(service.findByEmail("maria@gmail.com")).isEqualTo(maria);
    }

    // O e-mail é normalizado pelo value object antes da consulta: " MARIA@Gmail.com " procura "maria@gmail.com".
    @Test
    void searchesByTheNormalizedEmail() {
        UserView maria = view(1L, "Maria Brown", "maria@gmail.com");
        // findByEmail: só responde para "maria@gmail.com". Se o service passasse o texto cru, o teste falharia.
        when(repository.findByEmail(new Email("maria@gmail.com"))).thenReturn(Optional.of(maria));

        assertThat(service.findByEmail(" MARIA@Gmail.com ")).isEqualTo(maria);
    }

    // E-mail sem usuário: o Optional vazio vira UserNotFoundException, com o e-mail na mensagem. A mensagem usa o e-mail
    // normalizado (sem espaços e em minúsculas), e não o texto que veio no pedido, como no EmailAlreadyInUseException.
    @Test
    void throwsWhenNoUserHasTheEmail() {
        // findByEmail: o mock diz que ninguém tem esse e-mail.
        when(repository.findByEmail(new Email("nobody@mail.com"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEmail(" NOBODY@Mail.com "))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("user not found with email nobody@mail.com");
    }

    // E-mail nulo ou inválido é recusado pelo próprio Email (DomainException), antes de consultar o repositório.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "ana.mail.com", "ana@mail"})
    void rejectsInvalidEmailsWithoutTouchingTheRepository(String email) {
        assertThatThrownBy(() -> service.findByEmail(email))
                .isInstanceOf(DomainException.class);

        verifyNoInteractions(repository);
    }

    // Listagem: o service devolve a lista do repositório, na mesma ordem.
    @Test
    void listsAllUsers() {
        List<UserView> users = List.of(
                view(1L, "Maria Brown", "maria@gmail.com"),
                view(2L, "Alex Green", "alex@gmail.com")
        );
        // findAll: o mock devolve dois usuários.
        when(repository.findAll()).thenReturn(users);

        assertThat(service.findAll()).containsExactlyElementsOf(users);
        verify(repository).findAll();
    }

    // Sem usuários, a listagem é uma lista vazia (e não nula nem erro).
    @Test
    void returnsAnEmptyListWhenThereAreNoUsers() {
        // findAll: o mock devolve uma lista vazia.
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }
}
