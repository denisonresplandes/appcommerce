package dev.denison.appcommerce.users.presentation;

import dev.denison.appcommerce.users.application.command.CreateUserCommand;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.presentation.dto.CreateUserRequest;
import dev.denison.appcommerce.users.presentation.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de unidade do {@link UserWebMapper} (sem Spring): conferem a tradução do request para o comando e da view
 * para a resposta.
 *
 * <p>Cada campo usa um valor diferente, para um campo trocado com outro quebrar o teste.</p>
 */
class UserWebMapperTest {

    @Test
    void toCommandCopiesTheRequestFields() {
        CreateUserRequest request = new CreateUserRequest("NAME", "EMAIL", "PHONE", LocalDate.of(1991, 2, 3));

        CreateUserCommand command = UserWebMapper.toCommand(request);

        assertThat(command).isEqualTo(new CreateUserCommand("NAME", "EMAIL", "PHONE", LocalDate.of(1991, 2, 3)));
    }

    @Test
    void toResponseCopiesTheViewFields() {
        UserView view = new UserView(7L, "NAME", "EMAIL", "PHONE", LocalDate.of(1991, 2, 3));

        UserResponse response = UserWebMapper.toResponse(view);

        assertThat(response).isEqualTo(new UserResponse(7L, "NAME", "EMAIL", "PHONE", LocalDate.of(1991, 2, 3)));
    }
}
