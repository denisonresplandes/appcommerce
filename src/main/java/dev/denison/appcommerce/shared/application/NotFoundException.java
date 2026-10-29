package dev.denison.appcommerce.shared.application;

/**
 * Base de todas as exceções do tipo "recurso não encontrado".
 *
 * <p>A camada de apresentação precisa transformar erros de negócio em HTTP, mas não deve
 * conhecer cada exceção de cada módulo. Com uma base comum, o {@code GlobalExceptionHandler}
 * trata <i>uma</i> classe e devolve 404 para todas.</p>
 *
 * <p>"Não encontrado" fica na camada de <b>application</b>, pois é o resultado de um caso de uso
 * (consultar o que não existe), não uma regra de negócio violada.
 * Violações de regra continuam sendo {@code DomainException}.</p>
 *
 * <p>Cada módulo cria a sua exceção herdando desta, com uma mensagem própria.</p>
 * */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
