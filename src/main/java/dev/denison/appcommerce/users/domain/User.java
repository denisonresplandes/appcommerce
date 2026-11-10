package dev.denison.appcommerce.users.domain;

import dev.denison.appcommerce.shared.domain.AggregateRoot;
import dev.denison.appcommerce.shared.domain.DomainException;
import dev.denison.appcommerce.users.domain.vo.*;

import java.util.Objects;

/**
 * Agregado do usuário: a raiz que concentra as regras de um usuário.
 *
 * <p>É criado por {@link #create} (usuário novo, ainda sem id) ou por {@link #restore} (usuário vindo do banco,
 * com id). O id é {@code null} enquanto o usuário não foi salvo e não muda na mesma instância (é {@code final}).</p>
 *
 * <p><b>Igualdade ({@code equals} e {@code hashCode}).</b> O usuário é uma entidade, não um VO: dois objetos são o
 * mesmo usuário se têm o mesmo id, mesmo que o e-mail ou o telefone sejam diferentes. Por isso a comparação usa só
 * o id, e não todos os campos.</p>
 *
 * <ul>
 *   <li>Um usuário sem id só é igual a si mesmo. Dois usuários novos nunca são o mesmo, mesmo com dados iguais.</li>
 *   <li>Comparar por id (e não por referência) é necessário porque cada leitura do banco monta uma nova instância
 *       pelo mapper: dois {@code restore} com o mesmo id representam o mesmo usuário.</li>
 *   <li>O {@code hashCode} sai só do id, que não muda. Assim ele continua o mesmo quando o e-mail ou o telefone
 *       são trocados, e o usuário não se perde dentro de um {@code HashSet} ou {@code HashMap}.</li>
 *   <li>Isso vale para o modelo de domínio, que o JPA não gerencia (sem proxies do Hibernate e sem id atribuído
 *       depois). Os cuidados de {@code equals} e {@code hashCode} do JPA ficam na {@code UserEntity}.</li>
 * </ul>
 */
public class User extends AggregateRoot {

    // Por enquanto só o e-mail e o telefone podem ser trocados (changeEmail e changePhone); os demais dados
    // são definidos na criação e não mudam.
    private final UserId id;
    private final Name name;
    private Email email;
    private Phone phone;
    private final DateOfBirth dateOfBirth;

    // Único ponto de construção: create e restore passam por aqui, então a exigência dos dados vale para os dois.
    private User(UserId id, Name name, Email email, Phone phone, DateOfBirth dateOfBirth) {
        DomainException.requireNonNull(name, "user name is required");
        DomainException.requireNonNull(email, "user email address is required");
        DomainException.requireNonNull(phone, "user phone number is required");
        DomainException.requireNonNull(dateOfBirth, "user date of birth is required");

        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Cria um usuário novo, ainda sem id (o id é gerado quando ele é salvo). Todos os dados são obrigatórios.
     *
     * @throws DomainException se algum dado for nulo
     */
    public static User create(Name name, Email email, Phone phone, DateOfBirth dateOfBirth) {
        return new User(null, name, email, phone, dateOfBirth);
    }

    /**
     * Reconstrói um usuário que já existe, vindo do banco (é o que o mapper usa ao ler). Ao contrário de
     * {@link #create}, exige o id.
     *
     * @throws DomainException se o id ou algum dado for nulo
     */
    public static User restore(UserId userId, Name name, Email email, Phone phone, DateOfBirth dateOfBirth) {
        DomainException.requireNonNull(userId, "user id is required");
        return new User(userId, name, email, phone, dateOfBirth);
    }

    /**
     * @return o id do usuário, ou {@code null} se ele ainda não foi salvo
     */
    public UserId getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    /**
     * Troca o e-mail do usuário. Se o novo e-mail for nulo, o e-mail atual é mantido.
     *
     * <p>Este método não confere se o e-mail já pertence a outro usuário: isso depende do banco e é feito pela
     * camada de application, com {@link UserRepository#existsByEmail}.</p>
     *
     * @throws DomainException se o novo e-mail for nulo
     */
    public void changeEmail(Email email) {
        DomainException.requireNonNull(email, "user email address is required");
        this.email = email;
    }

    public Phone getPhone() {
        return phone;
    }

    /**
     * Troca o telefone do usuário. Se o novo telefone for nulo, o telefone atual é mantido.
     *
     * @throws DomainException se o novo telefone for nulo
     */
    public void changePhone(Phone phone) {
        DomainException.requireNonNull(phone, "user phone number is required");
        this.phone = phone;
    }

    public DateOfBirth getDateOfBirth() {
        return dateOfBirth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        // Sem id (usuário novo), só vale a comparação por referência feita acima.
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Mesmo critério do equals: só o id. Como ele é final, o hash não muda quando o usuário é alterado.
        return Objects.hashCode(id);
    }
}
