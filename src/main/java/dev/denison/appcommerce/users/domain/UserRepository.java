package dev.denison.appcommerce.users.domain;

import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.UserId;

import java.util.Optional;

/**
 * Port de escrita do usuário: o que o domínio precisa de um "repositório" para guardar e recuperar o agregado
 * {@link User}.
 *
 * <p>É só a interface. Quem a implementa é a infraestrutura (um adapter com Spring Data e JPA), e o domínio não
 * sabe nada de banco: pensa nele como uma coleção de usuários. As consultas de leitura (listagens, telas) ficam
 * em outro port, na camada de application.</p>
 */
public interface UserRepository {

    /**
     * Salva o usuário (cria um novo ou atualiza um existente) e devolve <b>outra instância</b>, já com o id.
     *
     * <p>O id do {@link User} é {@code final}, então o objeto recebido nunca ganha o id gerado. Quem chama deve
     * usar o objeto devolvido.</p>
     */
    User save(User user);

    /**
     * Busca o usuário pelo id.
     *
     * @return o usuário, ou vazio se não existir
     */
    Optional<User> findById(UserId userId);

    /**
     * Indica se já existe um usuário com esse e-mail. Serve para recusar e-mail repetido antes de salvar; a
     * restrição de unicidade do banco continua sendo a última defesa, caso duas requisições simultâneas passem
     * por essa conferência.
     */
    boolean existsByEmail(Email email);
}
