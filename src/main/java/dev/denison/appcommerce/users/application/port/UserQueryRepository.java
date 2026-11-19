package dev.denison.appcommerce.users.application.port;

import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Port de leitura do usuário: o que a application precisa para consultar usuários, sem alterar nada.
 *
 * <p>Devolve {@link UserView}, uma projeção só com os dados que a resposta precisa, e não o agregado
 * {@code User}. Por isso a implementação pode montar a view direto da consulta ao banco, sem criar os value objects
 * nem repetir as validações do domínio. O port de escrita, que trabalha com o agregado, é o
 * {@code UserRepository}, que fica no domínio.</p>
 *
 * <p>Este port está na application, e não no domínio, porque devolve {@code UserView}, que também é da
 * application (o domínio não pode depender dela). É só a interface: quem a implementa é a infraestrutura.</p>
 *
 * <p>Os parâmetros são value objects: se o chamador tem um {@code UserId} ou um {@code Email}, o valor já foi
 * validado e normalizado. A infraestrutura é quem extrai o valor simples para consultar o banco.</p>
 */
public interface UserQueryRepository {

    /**
     * Busca o usuário pelo id.
     *
     * @return a view do usuário, ou vazio se não existir
     */
    Optional<UserView> findById(UserId id);

    /**
     * Busca o usuário pelo e-mail. Como o {@link Email} já vem normalizado (sem espaços nas pontas e em minúsculas),
     * a busca não depende de como o e-mail foi digitado.
     *
     * @return a view do usuário, ou vazio se não existir
     */
    Optional<UserView> findByEmail(Email email);

    /**
     * Lista todos os usuários, ordenados pelo id. Devolve lista vazia se não houver nenhum.
     */
    List<UserView> findAll();
}
