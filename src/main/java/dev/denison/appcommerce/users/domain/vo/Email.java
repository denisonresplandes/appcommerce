package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * VO de email de usuário.
 *
 * <p>O construtor normaliza (sem espaços nas pontas e em minúsculas, então {@code " Joao@Mail.com "} e
 * {@code "joao@mail.com"} são o mesmo e-mail) e valida: no máximo {@link #MAX_LENGTH} caracteres e formato
 * {@code parte-local@dominio.extensao}.</p>
 *
 * <p>A conferência de formato é simples de propósito: barra os erros comuns de digitação. Saber se o endereço
 * existe de verdade só é possível enviando uma mensagem para ele.</p>
 * */
public record Email(
        String value
) {
    public static final int MAX_LENGTH = 100;

    // parte local, um único @, domínio e extensão com 2 ou mais letras. Não aceita espaços.
    private static final Pattern PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public Email {
        DomainException.requireNonNull(value, "user email address is required");

        // Normalização: remove espaços nas pontas e converte para minúsculo. O Locale.ROOT faz a conversão não
        // depender do idioma da máquina (no turco, por exemplo, o "I" maiúsculo viraria "ı").
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new DomainException("user email address is too long");
        }
        if (value.isEmpty() || !PATTERN.matcher(value).matches()) {
            throw new DomainException("user email address is invalid");
        }
    }
}
