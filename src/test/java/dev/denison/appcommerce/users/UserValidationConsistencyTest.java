package dev.denison.appcommerce.users;

import dev.denison.appcommerce.users.domain.vo.Email;
import dev.denison.appcommerce.users.domain.vo.Name;
import dev.denison.appcommerce.users.domain.vo.Phone;
import dev.denison.appcommerce.users.infrastructure.persistence.UserEntity;
import dev.denison.appcommerce.users.presentation.UserController;
import dev.denison.appcommerce.users.presentation.dto.CreateUserRequest;
import jakarta.persistence.Column;
import jakarta.validation.Constraint;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que as três camadas que validam um usuário dizem a mesma coisa: o DTO e o controller (Bean Validation), os
 * value objects do domínio e as colunas do banco.
 *
 * <p>Os mesmos limites aparecem em lugares diferentes porque a {@code presentation} não pode importar o domínio (por
 * isso o DTO escreve {@code max = 100} em vez de usar {@code Name.MAX_LENGTH}). O custo é que alguém pode mudar um
 * lado e esquecer o outro. Estes testes ficam vermelhos quando isso acontece. Eles leem as anotações e as constantes
 * por reflexão, então não sobem o Spring.</p>
 *
 * <p>Este teste fica fora das camadas de propósito: ele precisa enxergar todas ao mesmo tempo.</p>
 */
class UserValidationConsistencyTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    // Chave de mensagem no formato {user.name.required}.
    private static final Pattern MESSAGE_KEY = Pattern.compile("^\\{([A-Za-z0-9_.]+)}$");
    // Parâmetro dentro do texto da mensagem, como {min} e {max}.
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    // ---------------------------------------------------------------------------------------------------------
    // Limites de tamanho: DTO, controller, value objects e colunas
    // ---------------------------------------------------------------------------------------------------------

    private static Field requestField(String name) throws NoSuchFieldException {
        return CreateUserRequest.class.getDeclaredField(name);
    }

    private static int columnLength(String field) throws NoSuchFieldException {
        return UserEntity.class.getDeclaredField(field).getAnnotation(Column.class).length();
    }

    // Os @Size do DTO têm os mesmos limites dos value objects.
    @Test
    void requestSizeLimitsMatchTheValueObjects() throws Exception {
        Size name = requestField("name").getAnnotation(Size.class);
        assertThat(name.min()).as("name min").isEqualTo(Name.MIN_LENGTH);
        assertThat(name.max()).as("name max").isEqualTo(Name.MAX_LENGTH);

        Size email = requestField("email").getAnnotation(Size.class);
        assertThat(email.max()).as("email max").isEqualTo(Email.MAX_LENGTH);

        Size phone = requestField("phone").getAnnotation(Size.class);
        assertThat(phone.min()).as("phone min").isEqualTo(Phone.MIN_LENGTH);
        assertThat(phone.max()).as("phone max").isEqualTo(Phone.MAX_LENGTH);
    }

    // Os limites dos value objects batem com o tamanho das colunas: tudo o que o domínio aceita cabe no banco.
    @Test
    void valueObjectLimitsMatchTheDatabaseColumns() throws Exception {
        assertThat(columnLength("name")).as("name column").isEqualTo(Name.MAX_LENGTH);
        assertThat(columnLength("email")).as("email column").isEqualTo(Email.MAX_LENGTH);
        assertThat(columnLength("phone")).as("phone column").isEqualTo(Phone.MAX_LENGTH);
    }

    // O parâmetro email do GET /users?email= valida o mesmo que o campo email do POST: obrigatório, formato e tamanho.
    @Test
    void theEmailSearchParameterIsValidatedLikeTheRequestField() throws Exception {
        Parameter parameter = UserController.class.getMethod("findByEmail", String.class).getParameters()[0];

        assertThat(parameter.isAnnotationPresent(NotBlank.class)).as("@NotBlank").isTrue();
        assertThat(parameter.isAnnotationPresent(jakarta.validation.constraints.Email.class)).as("@Email").isTrue();
        assertThat(parameter.getAnnotation(Size.class).max()).as("@Size max").isEqualTo(Email.MAX_LENGTH);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Mensagens: toda validação usa uma chave que existe no messages.properties
    // ---------------------------------------------------------------------------------------------------------

    private static Properties loadMessages() throws IOException {
        Properties messages = new Properties();
        try (InputStream in = UserValidationConsistencyTest.class.getResourceAsStream("/messages.properties")) {
            assertThat(in).as("messages.properties no classpath").isNotNull();
            messages.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return messages;
    }

    // Todas as anotações de validação do módulo: as do DTO e as dos parâmetros do controller.
    private static List<Annotation> constraintAnnotations() {
        List<Annotation> all = new ArrayList<>();
        for (Field field : CreateUserRequest.class.getDeclaredFields()) {
            all.addAll(Arrays.asList(field.getAnnotations()));
        }
        for (Method method : UserController.class.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                all.addAll(Arrays.asList(parameter.getAnnotations()));
            }
        }
        return all.stream().filter(a -> a.annotationType().isAnnotationPresent(Constraint.class)).toList();
    }

    // Toda anotação usa message = "{chave}", e a chave existe no messages.properties. Sem isso o texto sai no idioma da
    // requisição (mensagem padrão), ou a própria chave aparece na resposta (chave com erro de digitação). Se o texto usa
    // {min} ou {max}, a anotação precisa ter esse atributo, senão o "{min}" sairia literal.
    @Test
    void everyValidationMessageIsAnExistingKeyWithValidPlaceholders() throws Exception {
        Properties messages = loadMessages();
        List<Annotation> annotations = constraintAnnotations();
        assertThat(annotations).as("anotações encontradas").isNotEmpty();

        List<String> problems = new ArrayList<>();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> type = annotation.annotationType();
            String message = (String) type.getMethod("message").invoke(annotation);

            Matcher key = MESSAGE_KEY.matcher(message);
            if (!key.matches()) {
                problems.add("@" + type.getSimpleName() + " não usa uma chave {...}: \"" + message + "\"");
                continue;
            }
            String text = messages.getProperty(key.group(1));
            if (text == null) {
                problems.add("@" + type.getSimpleName() + ": a chave " + key.group(1) + " não existe no messages.properties");
                continue;
            }
            Matcher placeholder = PLACEHOLDER.matcher(text);
            while (placeholder.find()) {
                try {
                    type.getMethod(placeholder.group(1));
                } catch (NoSuchMethodException e) {
                    problems.add("@" + type.getSimpleName() + ": a mensagem " + key.group(1) + " usa {"
                            + placeholder.group(1) + "}, que a anotação não tem");
                }
            }
        }

        assertThat(problems).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Telefone: o que o DTO aceita, o domínio não altera
    // ---------------------------------------------------------------------------------------------------------

    private static boolean dtoRejectsPhone(String phone) {
        CreateUserRequest request = new CreateUserRequest("Ana Souza", "ana@mail.com", phone, LocalDate.of(1990, 1, 1));
        return VALIDATOR.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone"));
    }

    // O value object Phone tira tudo o que não é dígito (para aceitar "(11) 91234-5678"). Se o DTO deixasse passar
    // um texto com letras ou símbolos, o domínio gravaria um número diferente do enviado, sem avisar. Por isso o DTO
    // precisa recusar tudo o que o Phone alteraria.
    @ParameterizedTest
    @ValueSource(strings = {"1191234567a", "11-91234567", "12345abcde", "(11) 91234-5678", "(11)91234567", "119123456 7"})
    void theDtoRejectsPhoneNumbersThatTheDomainWouldAlter(String phone) {
        assertThat(dtoRejectsPhone(phone)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11912345678", "1112345678"})
    void theDtoAcceptsPhoneNumbersWithTenOrElevenDigits(String phone) {
        assertThat(dtoRejectsPhone(phone)).isFalse();
        assertThat(new Phone(phone).value()).isEqualTo(phone);
    }

    // Teste de propriedade: com milhares de textos montados ao acaso (com semente fixa, então sempre os mesmos), nenhum
    // que o DTO aceite pode ser recusado ou alterado pelo domínio.
    @Test
    void theDomainNeverRejectsOrAltersAPhoneNumberTheDtoAccepts() {
        Random random = new Random(42);
        String symbols = "()- +.ab";
        int accepted = 0;

        for (int i = 0; i < 20_000; i++) {
            // De 8 a 13 caracteres: em volta dos tamanhos válidos (10 e 11), para muitos serem aceitos pelo DTO.
            int length = 8 + random.nextInt(6);
            StringBuilder text = new StringBuilder();
            for (int j = 0; j < length; j++) {
                // 90% dígitos, para muitos textos serem só dígitos e chegarem a ser aceitos pelo DTO.
                text.append(random.nextInt(10) < 9
                        ? (char) ('0' + random.nextInt(10))
                        : symbols.charAt(random.nextInt(symbols.length())));
            }
            String phone = text.toString();

            if (!dtoRejectsPhone(phone)) {
                accepted++;
                assertThat(new Phone(phone).value()).as("telefone aceito pelo DTO: \"%s\"", phone).isEqualTo(phone);
            }
        }

        assertThat(accepted).as("textos aceitos pelo DTO (o teste não pode ser vazio)").isGreaterThan(1_000);
    }
}
