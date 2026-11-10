package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do VO {@link Name} (JUnit puro, sem Spring): normalização, tamanho e caracteres permitidos.
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
class NameTest {

    private static final String INVALID_CHARACTERS_MESSAGE =
            "user name is invalid: use only letters, spaces, hyphens and apostrophes";

    // Nomes válidos passam sem alteração: com acento, hífen, apóstrofo (reto e tipográfico) e no tamanho mínimo.
    @ParameterizedTest
    @ValueSource(strings = {"João da Silva", "Maria", "Ana-Maria", "D'Ávila", "D’Ávila", "Al"})
    void acceptsValidNames(String input) {
        assertThat(new Name(input).value()).isEqualTo(input);
    }

    // Normalização: tira os espaços das pontas e junta os repetidos no meio.
    @Test
    void removesOuterSpacesAndCollapsesRepeatedOnes() {
        assertThat(new Name("  João   da    Silva  ").value()).isEqualTo("João da Silva");
    }

    // A normalização não mexe nas maiúsculas e minúsculas.
    @Test
    void keepsTheLetterCase() {
        assertThat(new Name("joão DA silva").value()).isEqualTo("joão DA silva");
    }

    // Nome nulo é recusado como "obrigatório".
    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Name(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("user name is required");
    }

    // Vazio ou só espaços também é "obrigatório", pois depois da normalização não sobra nada.
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlank(String input) {
        assertThatThrownBy(() -> new Name(input))
                .isInstanceOf(DomainException.class)
                .hasMessage("user name is required");
    }

    // Menos de Name.MIN_LENGTH caracteres, contados depois da normalização (" A " vira "A").
    @ParameterizedTest
    @ValueSource(strings = {"A", " A "})
    void rejectsTooShortNames(String input) {
        assertThatThrownBy(() -> new Name(input))
                .isInstanceOf(DomainException.class)
                .hasMessage("user name is too short");
    }

    // O limite é inclusivo: um nome com exatamente Name.MAX_LENGTH caracteres ainda é aceito.
    @Test
    void acceptsTheMaximumLength() {
        String input = "a".repeat(Name.MAX_LENGTH);

        assertThat(new Name(input).value()).isEqualTo(input);
    }

    // Um caractere além do limite já é recusado.
    @Test
    void rejectsTooLongNames() {
        String input = "a".repeat(Name.MAX_LENGTH + 1);

        assertThatThrownBy(() -> new Name(input))
                .isInstanceOf(DomainException.class)
                .hasMessage("user name is too long");
    }

    // Números e símbolos não são permitidos (inclusive ponto, underscore e tags HTML).
    @ParameterizedTest
    @ValueSource(strings = {"Joao123", "João@Silva", "Ana_Maria", "Ana.Maria", "<b>Ana</b>"})
    void rejectsNumbersAndSymbols(String input) {
        assertThatThrownBy(() -> new Name(input))
                .isInstanceOf(DomainException.class)
                .hasMessage(INVALID_CHARACTERS_MESSAGE);
    }

    // Hífen e apóstrofo só valem no meio do nome; o nome precisa começar e terminar com letra.
    // Isso também barra nomes feitos só de pontuação ("--").
    @ParameterizedTest
    @ValueSource(strings = {"--", "'-'", "-Ana", "Ana-", "'Ana"})
    void rejectsNamesThatDoNotStartAndEndWithALetter(String input) {
        assertThatThrownBy(() -> new Name(input))
                .isInstanceOf(DomainException.class)
                .hasMessage(INVALID_CHARACTERS_MESSAGE);
    }

    // O VO guarda o valor já normalizado, então nomes que só diferem nos espaços são iguais (e com o mesmo hashCode).
    @Test
    void namesWithTheSameNormalizedValueAreEqual() {
        assertThat(new Name("  Ana   Maria "))
                .isEqualTo(new Name("Ana Maria"))
                .hasSameHashCodeAs(new Name("Ana Maria"));
    }
}
