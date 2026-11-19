package dev.denison.appcommerce.users.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Entidade JPA do usuário: o espelho da tabela {@code tb_user}, só com colunas e anotações,
 * sem regra de negócio. As regras ficam no {@code User} (domínio), e o {@link UserMapper}
 * converte um modelo no outro.
 *
 * <p><b>Igualdade ({@code equals} e {@code hashCode}).</b> O {@code equals} compara só o id: dois objetos são o mesmo
 * usuário se têm o mesmo id, e uma entidade nova (sem id) só é igual a si mesma. A comparação usa {@code getId()}, e
 * não o campo, porque o objeto recebido pode ser um proxy do Hibernate, que tem os campos vazios.</p>
 *
 * <p>O {@code hashCode} é <b>constante</b> de propósito: {@code getClass().hashCode()}, o mesmo valor para todos os
 * usuários. O id é gerado no {@code persist}, então um hash calculado a partir dele mudaria com a entidade já dentro de
 * um {@code Set} ou {@code HashMap}, e ela se perderia ali. Com o proxy do Hibernate também funciona: o proxy repassa
 * o {@code hashCode()} ao objeto real, e os dois dão o mesmo valor.</p>
 *
 * <p>O custo é que todas as entidades caem no mesmo balde, o que só pesa em um {@code Set} com milhares de elementos
 * (as coleções de associação do JPA são pequenas). Para guardar muitos usuários numa coleção própria, use os ids
 * ({@code Set<Long>} ou {@code Set<UserId>}), que têm hash por valor.</p>
 */
@Entity
@Table(name = "tb_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 11)
    private String phone;

    // Uso exclusivo do JPA
    UserEntity() {}

    // Usado pelo UserMapper para montar a entidade a partir do domínio. O id é null quando o usuário é novo.
    public UserEntity(Long id, String name, String email, LocalDate dateOfBirth, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserEntity user)) {
            return false;
        }
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        // Constante (veja o Javadoc da classe): o id só existe depois do persist, e o hash não pode mudar com a
        // entidade dentro de um Set.
        return getClass().hashCode();
    }
}
