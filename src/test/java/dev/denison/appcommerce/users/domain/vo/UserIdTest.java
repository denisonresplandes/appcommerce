package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do VO {@link UserId} (JUnit puro, sem Spring).
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
class UserIdTest {

    // Qualquer número positivo é aceito, do menor (1) ao maior possível (Long.MAX_VALUE).
    @ParameterizedTest
    @ValueSource(longs = {1, 42, Long.MAX_VALUE})
    void acceptsPositiveNumbers(long value) {
        assertThat(new UserId(value).value()).isEqualTo(value);
    }

    // O id não pode ser nulo.
    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("user id must be a positive number");
    }

    // Zero e números negativos são recusados: os ids do banco começam em 1.
    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void rejectsZeroAndNegativeNumbers(long value) {
        assertThatThrownBy(() -> new UserId(value))
                .isInstanceOf(DomainException.class)
                .hasMessage("user id must be a positive number");
    }

    // Por ser um record, a igualdade é pelo valor: mesmo valor = iguais e com o mesmo hashCode; valor diferente = não.
    @Test
    void idsWithTheSameValueAreEqual() {
        assertThat(new UserId(7L))
                .isEqualTo(new UserId(7L))
                .hasSameHashCodeAs(new UserId(7L))
                .isNotEqualTo(new UserId(8L));
    }
}
