package dev.denison.appcommerce.shared.domain;

/**
 * Base dos agregados do domínio.
 *
 * <p>Agregado é um grupo de objetos tratado como uma unidade de consistência: qualquer alteração passa pela
 * raiz (por exemplo, o {@code User}), que garante as regras do grupo. Por enquanto a classe não tem comportamento
 * e serve só para marcar o conceito; o que for comum a todos os agregados pode ser colocado aqui quando houver
 * uso real.</p>
 *
 * <p>Como todo o domínio, não depende de Spring nem de JPA.</p>
 */
public abstract class AggregateRoot {
}
