package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do VO {@link Email} (JUnit puro, sem Spring): normalização, formato e tamanho.
 *
 * <p>Os testes de erro conferem também a mensagem, porque ela é o que o cliente da API recebe.</p>
 */
class EmailTest {

    private static final String DOMAIN_PART = "@mail.com";

    // Formatos válidos passam sem alteração: ponto, +, _ e % na parte local, e domínio com subdomínios.
    @ParameterizedTest
    @ValueSource(strings = {"joao@mail.com", "joao.silva+loja@mail.com.br", "j_o%ao@sub.mail.org"})
    void acceptsValidAddresses(String input) {
        assertThat(new Email(input).value()).isEqualTo(input);
    }

    // Normalização: tira os espaços das pontas e converte tudo para minúsculas.
    @Test
    void removesOuterSpacesAndConvertsToLowerCase() {
        assertThat(new Email("  JOAO@Mail.COM ").value()).isEqualTo("joao@mail.com");
    }

    // E-mail nulo é recusado como "obrigatório".
    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("user email address is required");
    }

    // Vazio, só espaços e formatos quebrados: sem @, sem parte local, sem domínio, sem extensão, com dois @ ou
    // com espaço no meio.
    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "joao.mail.com", "joao@", "@mail.com", "joao@mail", "joao@@mail.com", "joao silva@mail.com"
    })
    void rejectsInvalidAddresses(String input) {
        assertThatThrownBy(() -> new Email(input))
                .isInstanceOf(DomainException.class)
                .hasMessage("user email address is invalid");
    }

    // O limite é inclusivo: monta um e-mail com exatamente Email.MAX_LENGTH caracteres, que ainda é aceito.
    @Test
    void acceptsTheMaximumLength() {
        String input = "a".repeat(Email.MAX_LENGTH - DOMAIN_PART.length()) + DOMAIN_PART;

        assertThat(input).hasSize(Email.MAX_LENGTH);
        assertThat(new Email(input).value()).isEqualTo(input);
    }

    // Um caractere além do limite é recusado, com a mensagem de "muito longo" (e não a de formato inválido).
    @Test
    void rejectsTooLongAddresses() {
        String input = "a".repeat(Email.MAX_LENGTH - DOMAIN_PART.length() + 1) + DOMAIN_PART;

        assertThatThrownBy(() -> new Email(input))
                .isInstanceOf(DomainException.class)
                .hasMessage("user email address is too long");
    }

    // Escritos de formas diferentes, mas iguais depois da normalização, são o mesmo e-mail.
    @Test
    void addressesWithTheSameNormalizedValueAreEqual() {
        assertThat(new Email(" Joao@Mail.com "))
                .isEqualTo(new Email("joao@mail.com"))
                .hasSameHashCodeAs(new Email("joao@mail.com"));
    }
}
