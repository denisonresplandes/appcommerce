package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;

/**
 * VO de número de telefone do usuário.
 *
 * <p>Guarda só os dígitos, com DDD: {@code "(11) 91234-5678"} vira {@code "11912345678"}. Aceita telefone fixo
 * (10 dígitos) e celular (11 dígitos). Números com código do país não são aceitos.</p>
 * */
public record Phone(
        String value
) {
    // Com DDD: 10 dígitos para telefone fixo e 11 para celular.
    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 11;

    public Phone {
        DomainException.requireNonNull(value, "user phone number is required");

        // Normalização: remove espaços nas pontas e remove qualquer caracter que não seja número.
        value = value.trim().replaceAll("\\D", "");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new DomainException("user phone number must have " + MIN_LENGTH + " or " + MAX_LENGTH + " digits");
        }
    }
}
