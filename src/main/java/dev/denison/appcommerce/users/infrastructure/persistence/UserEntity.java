package dev.denison.appcommerce.users.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidade JPA do usuário: o espelho da tabela {@code tb_user}, só com colunas e anotações,
 * sem regra de negócio. As regras ficam no {@code User} (domínio), e o {@link UserMapper}
 * converte um modelo no outro.
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
        return Objects.hashCode(id);
    }
}
