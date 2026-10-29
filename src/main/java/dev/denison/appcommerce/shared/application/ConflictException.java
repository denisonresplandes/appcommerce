package dev.denison.appcommerce.shared.application;

/**
 * Base de todas as exceções de conflito. Vira HTTP 409 no {@code GlobalExceptionHandler}.
 *
 * <p>É diferente de {@code DomainException} que ocorre quando uma regra é violada
 * (exemplo: e-mail inválido, onde o mesmo pedido falharia sempre)</p>
 *
 * <p>Nesta exceção, o dado é válido, mas <i>colide com o que já existe</i>, ou seja,
 * o mesmo pedido poderia ter funcionado se o e-mail estivesse livre.</p>
 *
 * <p>Mesma ideia de {@link NotFoundException}: uma base comum permite ao handler tratar <i>uma</i>
 * classe, sem conhecer as exceções de cada módulo</p>
 * */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
