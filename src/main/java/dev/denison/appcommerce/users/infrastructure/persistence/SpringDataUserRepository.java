package dev.denison.appcommerce.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório do Spring Data: o Spring gera a implementação em tempo de execução. Trabalha com a
 * {@link UserEntity}, nunca com o {@code User} do domínio.
 *
 * <p>Fica sem {@code public} de propósito: só o {@link JpaUserRepositoryAdapter}, do mesmo pacote, o usa. Quem está
 * fora enxerga apenas a interface {@code UserRepository} do domínio.</p>
 */
@Repository
interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    // Consulta gerada pelo Spring a partir do nome do método (compara a coluna email). Recebe String, e não Email,
    // porque este é o modelo de persistência; o adapter passa o valor já normalizado do value object.
    boolean existsByEmail(String email);
}
