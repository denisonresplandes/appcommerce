package dev.denison.appcommerce.users.presentation;

import dev.denison.appcommerce.users.application.command.CreateUserCommand;
import dev.denison.appcommerce.users.application.view.UserView;
import dev.denison.appcommerce.users.presentation.dto.CreateUserRequest;
import dev.denison.appcommerce.users.presentation.dto.UserResponse;

/**
 * Converte entre os objetos da API HTTP ({@link CreateUserRequest} e {@link UserResponse}) e os da camada
 * {@code application} ({@link CreateUserCommand} e {@link UserView}). Assim o formato do JSON pode mudar sem
 * mexer na {@code application}, e a {@code application} não conhece nada de HTTP.
 *
 * <p>A conversão é só uma cópia campo a campo: não há regra aqui. A validação do formato fica no
 * {@link CreateUserRequest} (Bean Validation, resposta 400 rápida) e as regras de negócio ficam nos value objects do
 * domínio (resposta 422). Por isso este mapper recebe os valores como vieram e não os altera.</p>
 *
 * <p>Esta camada não importa {@code domain} nem {@code infrastructure}: o mapper só enxerga os tipos da
 * {@code application} (o comando e a view) e os DTOs da própria {@code presentation}.</p>
 *
 * <p>É uma classe utilitária (só métodos estáticos, sem estado), por isso é {@code final} e o construtor é
 * privado.</p>
 */
public final class UserWebMapper {

    private UserWebMapper() {}

    /**
     * Requisição para comando: leva os dados do corpo do POST para o {@link CreateUserCommand}, que é o que o
     * {@code UserCommandService} recebe. Os campos já passaram pela validação do {@code @Valid} no controller;
     * as regras do domínio (normalização do e-mail, formato do telefone etc.) são aplicadas depois, na
     * {@code application}.
     */
    public static CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(
                request.name(),
                request.email(),
                request.phone(),
                request.dateOfBirth()
        );
    }

    /**
     * View para resposta: transforma o {@link UserView} (saída da {@code application}, vindo do {@code create} ou
     * das consultas) no {@link UserResponse} que vira o JSON. Serve para os três casos de leitura e para o 201 do
     * POST, então todas as respostas de usuário têm o mesmo formato.
     */
    public static UserResponse toResponse(UserView view) {
        return new UserResponse(
                view.id(),
                view.name(),
                view.email(),
                view.phone(),
                view.dateOfBirth()
        );
    }
}
