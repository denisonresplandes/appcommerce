package dev.denison.appcommerce.users.domain;

import dev.denison.appcommerce.shared.domain.DomainException;
import dev.denison.appcommerce.users.domain.vo.DateOfBirth;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.Name;
import dev.denison.appcommerce.users.domain.vo.Phone;
import dev.denison.appcommerce.users.domain.vo.UserId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do agregado {@link User} (JUnit puro, sem Spring), agrupados por comportamento: criação,
 * restauração, troca de contato e igualdade.
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
class UserTest {

    // Dados válidos, reaproveitados em todos os testes.
    private static final Name NAME = new Name("João da Silva");
    private static final Email EMAIL = new Email("joao@mail.com");
    private static final Phone PHONE = new Phone("11912345678");
    private static final DateOfBirth DATE_OF_BIRTH = new DateOfBirth(LocalDate.of(
            1990, 5, 20)
    );

    // Usuário novo, sem id, como sai de User.create.
    private static User newUser() {
        return User.create(NAME, EMAIL, PHONE, DATE_OF_BIRTH);
    }

    // Usuário "vindo do banco", com o id informado, como o mapper o monta ao ler.
    private static User restoredUser(long id) {
        return User.restore(new UserId(id), NAME, EMAIL, PHONE, DATE_OF_BIRTH);
    }

    /** User.create: usuário novo, ainda sem id. */
    @Nested
    class Create {

        // O usuário novo guarda os dados recebidos e ainda não tem id (ele só é gerado quando for salvo).
        @Test
        void createsAUserWithoutIdAndWithTheGivenData() {
            User user = newUser();

            assertThat(user.getId()).isNull();
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getPhone()).isEqualTo(PHONE);
            assertThat(user.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH);
        }

        // Todos os dados são obrigatórios. Nome nulo é recusado.
        @Test
        void rejectsNullName() {
            assertThatThrownBy(() -> User.create(null, EMAIL, PHONE, DATE_OF_BIRTH))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user name is required");
        }

        // E-mail nulo é recusado.
        @Test
        void rejectsNullEmail() {
            assertThatThrownBy(() -> User.create(NAME, null, PHONE, DATE_OF_BIRTH))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user email address is required");
        }

        // Telefone nulo é recusado.
        @Test
        void rejectsNullPhone() {
            assertThatThrownBy(() -> User.create(NAME, EMAIL, null, DATE_OF_BIRTH))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user phone number is required");
        }

        // Data de nascimento nula é recusada.
        @Test
        void rejectsNullDateOfBirth() {
            assertThatThrownBy(() -> User.create(NAME, EMAIL, PHONE, null))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user date of birth is required");
        }
    }

    /** User.restore: usuário que já existe, reconstruído a partir do banco. */
    @Nested
    class Restore {

        // O usuário restaurado guarda o id e os dados recebidos.
        @Test
        void restoresAUserWithTheGivenIdAndData() {
            User user = restoredUser(7L);

            assertThat(user.getId()).isEqualTo(new UserId(7L));
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getPhone()).isEqualTo(PHONE);
            assertThat(user.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH);
        }

        // Ao contrário do create, o restore exige o id: só se restaura um usuário que já foi salvo.
        @Test
        void rejectsNullId() {
            assertThatThrownBy(() -> User.restore(null, NAME, EMAIL, PHONE, DATE_OF_BIRTH))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user id is required");
        }

        // Os dados também são obrigatórios aqui. Testa o nome; as demais conferências são as mesmas do create.
        @Test
        void rejectsNullData() {
            UserId id = new UserId(7L);

            assertThatThrownBy(() -> User.restore(id, null, EMAIL, PHONE, DATE_OF_BIRTH))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user name is required");
        }
    }

    /** changeEmail e changePhone: a troca dos contatos do usuário. */
    @Nested
    class ChangeContacts {

        // Trocar o e-mail altera só o e-mail: id, nome, telefone e data de nascimento ficam como estavam.
        @Test
        void changesTheEmailAndKeepsTheRest() {
            User user = restoredUser(7L);
            Email newEmail = new Email("novo@mail.com");

            user.changeEmail(newEmail);

            assertThat(user.getEmail()).isEqualTo(newEmail);
            assertThat(user.getId()).isEqualTo(new UserId(7L));
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getPhone()).isEqualTo(PHONE);
            assertThat(user.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH);
        }

        // E-mail nulo é recusado, e o usuário continua com o e-mail anterior.
        @Test
        void keepsTheCurrentEmailWhenTheNewOneIsNull() {
            User user = newUser();

            assertThatThrownBy(() -> user.changeEmail(null))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user email address is required");
            assertThat(user.getEmail()).isEqualTo(EMAIL);
        }

        // Trocar o telefone altera só o telefone: id, nome, e-mail e data de nascimento ficam como estavam.
        @Test
        void changesThePhoneAndKeepsTheRest() {
            User user = restoredUser(7L);
            Phone newPhone = new Phone("11 98888-7777");

            user.changePhone(newPhone);

            assertThat(user.getPhone()).isEqualTo(newPhone);
            assertThat(user.getId()).isEqualTo(new UserId(7L));
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH);
        }

        // Telefone nulo é recusado, e o usuário continua com o telefone anterior.
        @Test
        void keepsTheCurrentPhoneWhenTheNewOneIsNull() {
            User user = newUser();

            assertThatThrownBy(() -> user.changePhone(null))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("user phone number is required");
            assertThat(user.getPhone()).isEqualTo(PHONE);
        }
    }

    /** equals e hashCode: a igualdade é só pelo id (veja o Javadoc da classe User). */
    @Nested
    class Equality {

        // Mesmo id = mesmo usuário, mesmo com e-mail diferente. A igualdade vale nos dois sentidos e o hashCode
        // também é igual.
        @Test
        void usersWithTheSameIdAreEqualEvenWithDifferentData() {
            User user = restoredUser(1L);
            User sameUserWithOtherEmail = User.restore(new UserId(1L), NAME, new Email("outro@mail.com"), PHONE, DATE_OF_BIRTH);

            assertThat(user).isEqualTo(sameUserWithOtherEmail);
            assertThat(sameUserWithOtherEmail).isEqualTo(user);
            assertThat(user).hasSameHashCodeAs(sameUserWithOtherEmail);
        }

        // Ids diferentes = usuários diferentes, mesmo com todos os dados iguais.
        @Test
        void usersWithDifferentIdsAreNotEqualEvenWithTheSameData() {
            assertThat(restoredUser(1L)).isNotEqualTo(restoredUser(2L));
        }

        // Dois usuários novos (sem id) nunca são iguais, nem com dados idênticos: "sem id" não vale como mesma
        // identidade.
        @Test
        void newUsersWithoutIdAreNeverEqualToEachOther() {
            assertThat(newUser()).isNotEqualTo(newUser());
        }

        // Mas um usuário novo é igual a si mesmo (mesma referência).
        @Test
        void aNewUserIsEqualToItself() {
            User user = newUser();

            assertThat(user).isEqualTo(user);
        }

        // Usuário novo e usuário com id nunca são iguais, na ida e na volta (simetria do equals).
        @Test
        void aNewUserIsNotEqualToARestoredOneInEitherDirection() {
            User newUser = newUser();
            User restored = restoredUser(1L);

            assertThat(newUser).isNotEqualTo(restored);
            assertThat(restored).isNotEqualTo(newUser);
        }

        // equals com null ou com um objeto de outro tipo devolve false, sem lançar exceção.
        @Test
        void isNotEqualToNullOrToAnotherType() {
            User user = restoredUser(1L);

            assertThat(user).isNotEqualTo(null);
            assertThat(user).isNotEqualTo("not a user");
        }

        // O hashCode depende só do id, então não muda quando o e-mail ou o telefone mudam.
        @Test
        void hashCodeDoesNotChangeWhenEmailOrPhoneChange() {
            User user = restoredUser(1L);
            int before = user.hashCode();

            user.changeEmail(new Email("novo@mail.com"));
            user.changePhone(new Phone("11 98888-7777"));

            assertThat(user.hashCode()).isEqualTo(before);
        }

        // Consequência do teste anterior: o usuário não se perde dentro de um HashSet depois de ser alterado.
        @Test
        void aUserIsStillFoundInAHashSetAfterItsContactsChange() {
            Set<User> users = new HashSet<>();
            User user = restoredUser(1L);
            users.add(user);

            user.changeEmail(new Email("novo@mail.com"));

            assertThat(users).contains(user);
            // Outra instância com o mesmo id (como a que o mapper cria a cada leitura) também é encontrada.
            assertThat(users).contains(restoredUser(1L));
        }

        // Num HashSet, cada usuário novo conta como um elemento diferente: todos têm o mesmo hash (sem id), mas o
        // equals os distingue.
        @Test
        void aHashSetKeepsEachNewUserWithoutId() {
            Set<User> users = new HashSet<>();

            users.add(newUser());
            users.add(newUser());
            users.add(restoredUser(1L));

            assertThat(users).hasSize(3);
        }
    }
}
