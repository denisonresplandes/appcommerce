package dev.denison.appcommerce.users.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste da igualdade da {@link UserEntity} ({@code equals} e {@code hashCode}), com {@code @DataJpaTest} porque os
 * casos que importam dependem do JPA: o id que só é gerado no {@code persist} e o proxy do Hibernate. Cada teste roda
 * numa transação que é desfeita no final.
 *
 * <p>O banco já vem com os usuários do {@code import.sql} (Maria, de id 1, e Alex, de id 2).</p>
 */
@DataJpaTest
class UserEntityTest {

    @Autowired
    private EntityManager entityManager;

    // Monta uma entidade; só o id e o e-mail mudam de um teste para o outro. O id é null para o usuário novo.
    private static UserEntity entity(Long id, String email) {
        return new UserEntity(id, "Ana Souza", email, LocalDate.of(1990, 1, 1), "11912345678");
    }

    // O id é gerado no persist (IDENTITY: o INSERT sai ali mesmo). Se o hash dependesse do id, ele mudaria depois de
    // a entidade já estar no Set, e ela se perderia: o Set procuraria no balde errado.
    // Atenção: o contains() do Set é o que usa o hash. O contains() do assertThat(Set) do AssertJ percorre os
    // elementos com equals e não perceberia o problema.
    @Test
    void aNewEntityIsStillFoundInAHashSetAfterItsIdIsGenerated() {
        UserEntity entity = entity(null, "ana@mail.com");
        Set<UserEntity> users = new HashSet<>();
        users.add(entity);

        entityManager.persist(entity);

        assertThat(entity.getId()).isNotNull();
        assertThat(users.contains(entity)).isTrue();
    }

    // O proxy do Hibernate (getReference, usado nas associações LAZY) é uma subclasse gerada de UserEntity. Ele e o
    // objeto real do mesmo usuário precisam ser iguais nos dois sentidos e ter o mesmo hash, inclusive dentro de um Set.
    // O clear() é necessário: sem ele o getReference devolveria o próprio objeto real, e não um proxy.
    @Test
    void aProxyIsEqualToTheRealEntityOfTheSameUserWithTheSameHashCode() {
        UserEntity real = entityManager.find(UserEntity.class, 1L);
        entityManager.clear();
        UserEntity proxy = entityManager.getReference(UserEntity.class, 1L);

        assertThat(proxy).isNotExactlyInstanceOf(UserEntity.class);
        assertThat(real).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(real);
        assertThat(real).hasSameHashCodeAs(proxy);
        assertThat(new HashSet<>(List.of(real)).contains(proxy)).isTrue();
    }

    // Mesmo id = mesmo usuário, mesmo com dados diferentes. A igualdade vale nos dois sentidos e o hash é igual.
    @Test
    void entitiesWithTheSameIdAreEqualEvenWithDifferentData() {
        UserEntity entity = entity(1L, "ana@mail.com");
        UserEntity sameUserWithOtherEmail = entity(1L, "outro@mail.com");

        assertThat(entity).isEqualTo(sameUserWithOtherEmail);
        assertThat(sameUserWithOtherEmail).isEqualTo(entity);
        assertThat(entity).hasSameHashCodeAs(sameUserWithOtherEmail);
    }

    // Ids diferentes = usuários diferentes, mesmo com todos os dados iguais.
    @Test
    void entitiesWithDifferentIdsAreNotEqualEvenWithTheSameData() {
        assertThat(entity(1L, "ana@mail.com")).isNotEqualTo(entity(2L, "ana@mail.com"));
    }

    // Duas entidades novas (sem id) nunca são iguais, nem com dados idênticos: "sem id" não vale como mesma identidade.
    @Test
    void newEntitiesWithoutIdAreNeverEqualToEachOther() {
        assertThat(entity(null, "ana@mail.com")).isNotEqualTo(entity(null, "ana@mail.com"));
    }

    // Mas uma entidade nova é igual a si mesma (mesma referência).
    @Test
    void aNewEntityIsEqualToItself() {
        UserEntity entity = entity(null, "ana@mail.com");

        assertThat(entity).isEqualTo(entity);
    }

    // Entidade nova e entidade com id nunca são iguais, na ida e na volta (simetria do equals).
    @Test
    void aNewEntityIsNotEqualToOneWithAnIdInEitherDirection() {
        UserEntity newEntity = entity(null, "ana@mail.com");
        UserEntity saved = entity(1L, "ana@mail.com");

        assertThat(newEntity).isNotEqualTo(saved);
        assertThat(saved).isNotEqualTo(newEntity);
    }

    // equals com null ou com um objeto de outro tipo devolve false, sem lançar exceção.
    @Test
    void isNotEqualToNullOrToAnotherType() {
        UserEntity entity = entity(1L, "ana@mail.com");

        assertThat(entity).isNotEqualTo(null);
        assertThat(entity).isNotEqualTo("not a user");
    }
}
