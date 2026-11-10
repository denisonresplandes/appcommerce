package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;

import java.util.regex.Pattern;

/**
 * VO de nome de um usuário.
 *
 * <p>Se existe um {@code Name}, ele é válido e não precisa ser conferido de novo. O construtor normaliza e valida:</p>
 * <ul>
 *   <li>remove os espaços das pontas e junta os repetidos ({@code "  Ana   Maria "} vira {@code "Ana Maria"});</li>
 *   <li>exige entre {@link #MIN_LENGTH} e {@link #MAX_LENGTH} caracteres, contados depois dessa normalização;</li>
 *   <li>exige que comece e termine com letra e, no meio, aceita só letras, espaços, hífen e apóstrofo
 *       (sem números nem símbolos).</li>
 * </ul>
 *
 * <p>As maiúsculas e minúsculas não são alteradas.</p>
 * */
public record Name(
        String value
) {
    // Limites de tamanho do nome, contados depois da normalização.
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 100;

    // \p{L} = qualquer letra Unicode. O nome começa e termina com letra; no meio também aceita espaço, hífen e
    // apóstrofo reto ou tipográfico (’), como em "Ana-Maria" e "D'Ávila". Isso barra nomes só de pontuação.
    private static final Pattern ALLOWED_CHARACTERS = Pattern
            .compile("^\\p{L}(?:[\\p{L}\\s'\\u2019-]*\\p{L})?$");

    public Name {
        DomainException.requireNonNull(value, "user name is required");

        // Normalização: remove espaços nas pontas e junta espaços repetidos.
        value = value.trim().replaceAll("\\s+", " ");

        // Um nome só de espaços fica vazio depois da normalização.
        if (value.isEmpty()) {
            throw new DomainException("user name is required");
        }
        if (value.length() < MIN_LENGTH) {
            throw new DomainException("user name is too short");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainException("user name is too long");
        }
        if (!ALLOWED_CHARACTERS.matcher(value).matches()) {
            throw new DomainException("user name is invalid: use only letters, spaces, hyphens and apostrophes");
        }
    }
}
