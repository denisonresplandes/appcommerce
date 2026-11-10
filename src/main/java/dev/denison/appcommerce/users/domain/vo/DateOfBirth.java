package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;

import java.time.LocalDate;
import java.time.Period;

/**
 * VO de data de nascimento do usuário.
 *
 * <p>Não pode ser nula nem estar no futuro (hoje ainda é aceito). Também responde se o usuário é adulto, porque
 * essa regra depende só da data de nascimento.</p>
 * */
public record DateOfBirth(
        LocalDate value
) {
    /** Idade, em anos, a partir da qual o usuário é considerado adulto. */
    public static final int AGE_OF_MAJORITY = 18;

    public DateOfBirth {
        DomainException.requireNonNull(value, "user date of birth is required");
        if (value.isAfter(LocalDate.now())) {
            throw new DomainException("user date of birth cannot be later than now");
        }
    }

    /**
     * Indica se o usuário já atingiu a maioridade, comparando a data de nascimento com a data de hoje.
     *
     * <p>Conta anos completos: no dia em que faz {@link #AGE_OF_MAJORITY} anos já é adulto; um dia antes, não.</p>
     */
    public boolean isAdult() {
        return Period.between(value, LocalDate.now()).getYears() >= AGE_OF_MAJORITY;
    }
}
