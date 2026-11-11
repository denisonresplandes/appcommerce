package dev.denison.appcommerce.users.infrastructure.persistence;

import dev.denison.appcommerce.users.domain.User;
import dev.denison.appcommerce.users.domain.vo.DateOfBirth;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.Name;
import dev.denison.appcommerce.users.domain.vo.Phone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de persistência do {@link JpaUserRepositoryAdapter}, com banco H2 de verdade e só a camada JPA carregada
 * ({@code @DataJpaTest}, sem controllers nem services). Cada teste roda numa transação que é desfeita no final.
 *
 * <p>O banco já vem com os usuários do {@code import.sql}; por isso os e-mails usados aqui são outros, para não
 * esbarrar neles.</p>
 */
@DataJpaTest
@Import(JpaUserRepositoryAdapter.class)
class JpaUserRepositoryAdapterTest {

    @Autowired
    private JpaUserRepositoryAdapter repository;

    // Monta um usuário válido. Só o nome e o e-mail mudam de um teste para o outro; o resto não importa aqui.
    private static User newUser(String name, String email) {
        return User.create(new Name(name), new Email(email), new Phone("11912345678"),
                new DateOfBirth(LocalDate.of(1990, 1, 1)));
    }

    // A restrição UNIQUE do e-mail é a última linha de defesa: mesmo que a checagem do service falhe (duas
    // requisições ao mesmo tempo), o banco recusa o segundo usuário com o mesmo e-mail.
    // O primeiro save mostra que o usuário é válido e que o banco o aceita; o segundo só difere em nome, e o e-mail
    // é a única restrição que ele pode violar. Como o id é IDENTITY, o INSERT sai dentro do próprio save, então a
    // exceção também.
    @Test
    void databaseRejectsDuplicateEmail() {
        repository.save(newUser("Ana Souza", "ana@mail.com"));

        assertThatThrownBy(() -> repository.save(newUser("Outra Ana", "ana@mail.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Contraprova: com e-mails diferentes os dois usuários são aceitos, cada um com o seu id. Mostra que a recusa do
    // teste anterior vem do e-mail repetido, e não de qualquer outro dado.
    @Test
    void databaseAcceptsDifferentEmails() {
        User first = repository.save(newUser("Ana Souza", "ana@mail.com"));
        User second = repository.save(newUser("Outra Ana", "outra.ana@mail.com"));

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull().isNotEqualTo(first.getId());
    }
}
