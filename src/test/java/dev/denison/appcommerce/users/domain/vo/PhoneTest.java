package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do VO {@link Phone} (JUnit puro, sem Spring): normalização e quantidade de dígitos.
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
class PhoneTest {

    // Celular: DDD + 9 dígitos, 11 no total.
    @Test
    void acceptsMobileNumbersWith11Digits() {
        assertThat(new Phone("11912345678").value()).isEqualTo("11912345678");
    }

    // Fixo: DDD + 8 dígitos, 10 no total.
    @Test
    void acceptsLandlineNumbersWith10Digits() {
        assertThat(new Phone("1131234567").value()).isEqualTo("1131234567");
    }

    // Normalização: parênteses, espaços e hífen são removidos, e ficam só os dígitos.
    @Test
    void keepsOnlyTheDigits() {
        assertThat(new Phone("(11) 91234-5678").value()).isEqualTo("11912345678");
        assertThat(new Phone(" 11 3123-4567 ").value()).isEqualTo("1131234567");
    }

    // Telefone nulo é recusado como "obrigatório".
    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Phone(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("user phone number is required");
    }

    // Fora do intervalo de 10 a 11 dígitos: vazio, sem nenhum dígito ("abc"), 9 e 12 dígitos (logo fora dos
    // limites) e um número com código do país (13 dígitos).
    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "119123456", "119123456789", "+55 (11) 91234-5678"})
    void rejectsNumbersWithTheWrongAmountOfDigits(String input) {
        assertThatThrownBy(() -> new Phone(input))
                .isInstanceOf(DomainException.class)
                .hasMessage("user phone number must have 10 or 11 digits");
    }

    // Formatações diferentes do mesmo número resultam em VOs iguais (e com o mesmo hashCode).
    @Test
    void numbersWithTheSameDigitsAreEqual() {
        assertThat(new Phone("(11) 91234-5678"))
                .isEqualTo(new Phone("11912345678"))
                .hasSameHashCodeAs(new Phone("11912345678"));
    }
}
