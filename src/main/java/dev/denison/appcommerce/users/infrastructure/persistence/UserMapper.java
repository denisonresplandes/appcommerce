package dev.denison.appcommerce.users.infrastructure.persistence;

import dev.denison.appcommerce.users.domain.User;
import dev.denison.appcommerce.users.domain.vo.*;

import java.util.Optional;

/**
 * Converte entre o agregado de domínio ({@link User}) e a entidade JPA ({@link UserEntity}).
 * É a ponte entre os dois modelos: o domínio não conhece o JPA, e a entidade não tem regra de negócio.
 *
 * <p>É uma classe utilitária (só métodos estáticos, sem estado), por isso é {@code final} e o construtor é
 * privado.</p>
 */
public final class UserMapper {

    private UserMapper() {}

    /**
     * Domínio para banco: abre os value objects e copia os valores para a entidade. Para um usuário novo,
     * o id do domínio é {@code null} e a entidade também fica sem id (o banco o gera ao salvar).
     */
    public static UserEntity toEntity(User user) {
        return new UserEntity(
                Optional.ofNullable(user.getId())
                        .map(UserId::value)
                        .orElse(null),
                user.getName().value(),
                user.getEmail().value(),
                user.getDateOfBirth().value(),
                user.getPhone().value()
        );
    }

    /**
     * Banco para domínio: monta um {@link User} com {@link User#restore}, que exige o id.
     * Os valores passam de novo pelas validações dos value objects, então um dado inválido gravado
     * direto na tabela aparece aqui como {@code DomainException}, em vez de circular um {@code User}
     * inválido pela aplicação.
     */
    public static User toDomain(UserEntity userEntity) {
        return User.restore(
                new UserId(userEntity.getId()),
                new Name(userEntity.getName()),
                new Email(userEntity.getEmail()),
                new Phone(userEntity.getPhone()),
                new DateOfBirth(userEntity.getDateOfBirth())
        );
    }
}
