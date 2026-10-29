package dev.denison.appcommerce.shared.domain;

/**
 * Erro de <b>regra de negócio</b>: lançada pelo domínio quando alguém tenta criar ou deixar um objeto em um
 * estado inválido (e-mail sem {@code @}, nome em branco, pedido já entregue sendo cancelado, etc.).
 *
 * <p>Fica em {@code shared.domain} porque todos os módulos usam, e não importa nada de Spring, de JPA ou de
 * web: o domínio continua Java puro. Essa exceção sobe até o {@code GlobalExceptionHandler},
 * na camada de apresentação, que o traduz em HTTP 422.</p>
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    /**
     * Garante que o objeto não é nulo.
     *
     * @throws DomainException com {@code message} se {@code o} for nulo
     */
    public static void requireNonNull(Object o, String message) {
        if (o == null) {
            throw new DomainException(message);
        }
    }

    /**
     * Garante que o texto não é nulo, vazio nem só espaços em branco, e já o devolve
     * <b>sem os espaços das pontas</b>.
     *
     * @return o texto com {@code trim()} aplicado
     * @throws DomainException com {@code message} se {@code text} for nulo ou estiver em branco
     */
    public static String requireNonBlank(String text, String message) {
        requireNonNull(text, message);
        text = text.trim();
        if (text.isBlank()) {
            throw new DomainException(message);
        }
        return text;
    }

    /**
     * Garante que o número não é nulo e é maior que zero. Usado nos ids, que no banco começam em 1.
     *
     * <p>O parâmetro é {@code Long} (e não {@code long}) justamente para poder receber {@code null} e rejeitá-lo com
     * a mesma mensagem, em vez de estourar um {@code NullPointerException}.</p>
     *
     * @throws DomainException com {@code message} se {@code value} for nulo, zero ou negativo
     */
    public static void requirePositive(Long value, String message) {
        requireNonNull(value, message);
        if (value <= 0) {
            throw new DomainException(message);
        }
    }
}
