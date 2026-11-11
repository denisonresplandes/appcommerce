package dev.denison.appcommerce.users.infrastructure.persistence;

import dev.denison.appcommerce.users.domain.User;
import dev.denison.appcommerce.users.domain.UserRepository;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapter que implementa o port {@link UserRepository}, do domínio, com Spring Data JPA. O domínio pede "salvar um
 * usuário"; aqui isso vira: converter para entidade, salvar e converter de volta.
 *
 * <p>É o único ponto que conhece, ao mesmo tempo, o {@code User} e a {@link UserEntity}. O {@code @Repository}
 * registra o bean e faz o Spring traduzir as exceções do JPA para as dele (por exemplo,
 * {@code DataIntegrityViolationException} quando o banco recusa um e-mail repetido).</p>
 */
@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entityToSave = UserMapper.toEntity(user);
        UserEntity savedEntity = springDataUserRepository.save(entityToSave);
        return UserMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return springDataUserRepository.findById(userId.value())
                .map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springDataUserRepository.existsByEmail(email.value());
    }
}
