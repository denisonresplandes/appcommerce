package dev.denison.appcommerce.users.domain.vo;

import dev.denison.appcommerce.shared.domain.DomainException;

/**
 * Value Object (VO) de ID do usuário.
 *
 * <p>Em vez de um {@code Long} solto, o id tem tipo próprio. Assim ele não se confunde com outros números e a
 * regra fica em um lugar só: precisa ser um número positivo (os ids do banco começam em 1).</p>
 *
 * <p>Só existe para usuários que já foram salvos: o {@code User} novo guarda {@code null} no lugar do id até ser
 * salvo. Por ser um record, é imutável e dois {@code UserId} com o mesmo valor são iguais.</p>
 * */
public record UserId(
        Long value
) {
    public UserId {
        DomainException.requirePositive(value, "user id must be a positive number");
    }
}
