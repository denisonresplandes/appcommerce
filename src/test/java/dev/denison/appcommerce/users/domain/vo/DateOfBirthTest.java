package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do VO {@link DateOfBirth} (JUnit puro, sem Spring): validação da data e regra de maioridade.
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
class DateOfBirthTest {

    // Uma data no passado é aceita e guardada como veio.
    @Test
    void acceptsAPastDate() {
        LocalDate date = LocalDate.of(1990, 5, 20);

        assertThat(new DateOfBirth(date).value()).isEqualTo(date);
    }

    // Hoje ainda é aceito: a data só não pode ser posterior a hoje.
    @Test
    void acceptsToday() {
        LocalDate today = LocalDate.now();

        assertThat(new DateOfBirth(today).value()).isEqualTo(today);
    }

    // Data nula é recusada como "obrigatória".
    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new DateOfBirth(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("user date of birth is required");
    }

    // Amanhã é recusado: ninguém nasce no futuro.
    @Test
    void rejectsAFutureDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> new DateOfBirth(tomorrow))
                .isInstanceOf(DomainException.class)
                .hasMessage("user date of birth cannot be later than now");
    }

    // O isAdult depende da data de hoje, por isso as datas de nascimento abaixo são calculadas a partir dela.

    // Quem tem 30 anos é claramente adulto.
    @Test
    void isAdultWhenOlderThanTheAgeOfMajority() {
        LocalDate date = LocalDate.now().minusYears(30);

        assertThat(new DateOfBirth(date).isAdult()).isTrue();
    }

    // Quem faz 18 anos hoje já é adulto: o limite é inclusivo.
    @Test
    void isAdultOnTheDayOfTheMajorityBirthday() {
        LocalDate date = LocalDate.now().minusYears(DateOfBirth.AGE_OF_MAJORITY);

        assertThat(new DateOfBirth(date).isAdult()).isTrue();
    }

    // Quem só faz 18 anos amanhã ainda não é adulto (conta anos completos).
    @Test
    void isNotAdultOneDayBeforeTheMajorityBirthday() {
        LocalDate date = LocalDate.now().minusYears(DateOfBirth.AGE_OF_MAJORITY).plusDays(1);

        assertThat(new DateOfBirth(date).isAdult()).isFalse();
    }

    // Quem nasceu hoje não é adulto.
    @Test
    void isNotAdultWhenBornToday() {
        assertThat(new DateOfBirth(LocalDate.now()).isAdult()).isFalse();
    }
}
