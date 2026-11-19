# Arquitetura

O projeto é um **monolito em camadas**, com **DDD leve** e **CQRS leve**. Este documento explica o que isso significa
na prática, como as pastas estão organizadas, quais regras valem para o código e quais convenções de Git são seguidas.

---

## 1. Monolito em camadas

### Monolito
É uma aplicação só: um projeto, um `.jar`, um banco de dados. Os módulos (usuários, pedidos...) vivem
no mesmo processo e conversam por chamadas de método comuns, sem rede entre eles.

### Camadas
Cada módulo é dividido em quatro camadas, e cada uma tem uma responsabilidade:

| Camada | Responsabilidade | Exemplos |
|---|---|---|
| `presentation` | Falar com o mundo externo (HTTP) | Controllers, DTOs de entrada e saída |
| `application` | Coordenar cada caso de uso | Services, comandos, views, exceções de aplicação |
| `domain` | Guardar as regras do negócio | Agregados, value objects, interfaces de repositório |
| `infrastructure` | Resolver os detalhes técnicos | JPA, banco de dados, integrações, configurações |

A regra que organiza tudo é a **direção das dependências**:

```
presentation  →  application  →  domain  ←  infrastructure
```

O controller chama o service, o service usa o domínio, e a infraestrutura **implementa** o que o domínio precisa.
O domínio não conhece ninguém: não importa Spring, JPA nem nada de web. Por isso as regras de negócio podem ser
testadas com um teste simples de JUnit, sem subir o contexto do Spring.

Uma nuance do diagrama: na **leitura**, a infraestrutura também depende da `application`, mas só do port
`UserQueryRepository` e do `UserView` que ele devolve (veja "Camadas e Clean Architecture", logo abaixo).

Regras de cada camada:

- **`domain`**: Java puro. Sem anotações de Spring, JPA ou JSON.
- **`application`**: só conhece o `domain`. Pode usar `@Service` e `@Transactional`, mas não importa JPA, classes de
  web nem nada de `infrastructure` ou `presentation`. Quando precisa de algo externo, define uma interface (um *port*).
- **`infrastructure`**: implementa os ports. É a única camada que conhece JPA.
- **`presentation`**: só chama a `application`. Nunca usa repositórios, agregados ou adapters diretamente. A única
  exceção é o `GlobalExceptionHandler`, que enxerga o `DomainException` para traduzi-lo em HTTP 422.

### Camadas e Clean Architecture

Mescla as duas coisas. A estrutura é de **camadas em pacotes, dentro de módulos** (um monolito
modular), mas a **direção das dependências** segue a ideia da Clean Architecture e da arquitetura hexagonal:

- Numa arquitetura em camadas tradicional, o negócio chama a camada de dados e **depende** dela.
- Aqui é invertido: quem precisa do dado define a interface (o *port*) e a infraestrutura **depende dela** para
  implementá-la. O domínio fica no centro e não conhece o banco. Isso vale nos dois lados:
  - **escrita:** `UserRepository` fica em `domain`, e o `JpaUserRepositoryAdapter` o implementa;
  - **leitura:** `UserQueryRepository` fica em `application`, junto com o `UserView` que ele devolve, e o
    `JpaUserQueryRepositoryAdapter` o implementa. É por isso que a infraestrutura depende da `application`, só desses
    dois tipos. O port de leitura não pode ficar no `domain`, porque o domínio não pode depender de um tipo da
    `application` (o `UserView`).

Diferenças em relação à Clean Architecture "de livro": não há um círculo para cada coisa nem uma classe por caso de
uso (os casos de uso ficam agrupados em services), e a `application` aceita `@Service` e `@Transactional` por
pragmatismo. Também não há interfaces de entrada (*input ports*): o controller chama direto a classe do service, o
que já respeita a direção das dependências (de fora para dentro) e evita uma interface por service sem ganho agora.
O DDD é um assunto à parte e combina com as duas: é ele que define como o modelo de domínio é escrito (agregados,
value objects, regras dentro das classes).

---

## 2. Organização das pastas

A divisão é feita primeiro por **módulo** (a área do negócio) e depois por camada:

```
src/main/java/dev/denison/appcommerce/
│
├── shared/                          # o que é comum a todos os módulos
│   ├── domain/                      #   DomainException, AggregateRoot (opcional)
│   ├── application/                 #   NotFoundException, ConflictException
│   ├── presentation/                #   GlobalExceptionHandler
│   └── infrastructure/config/       #   configurações gerais (ex.: segurança, quando entrar)
│
├── users/                           # um módulo
│   ├── api/                         #   o que OUTROS módulos podem usar (criado quando algum precisar)
│   │
│   ├── presentation/                #   camada web
│   │   ├── UserController
│   │   ├── UserWebMapper            #     DTO <-> comando/view
│   │   └── dto/                     #     CreateUserRequest, UserResponse
│   │
│   ├── application/                 #   casos de uso
│   │   ├── UserCommandService       #     escrita (cria, altera, remove)
│   │   ├── UserQueryService         #     leitura
│   │   ├── command/                 #     CreateUserCommand
│   │   ├── view/                    #     UserView (objeto de saída, sem regras)
│   │   ├── port/                    #     UserQueryRepository (interface de leitura)
│   │   └── exception/               #     UserNotFoundException, EmailAlreadyInUseException
│   │
│   ├── domain/                      #   regras do negócio
│   │   ├── User                     #     o agregado
│   │   ├── UserRepository           #     interface de escrita (port)
│   │   └── vo/                      #     UserId, Name, Email, Phone...
│   │
│   └── infrastructure/
│       └── persistence/             #   tudo de JPA fica aqui
│           ├── UserEntity           #     espelho da tabela
│           ├── UserMapper           #     User <-> UserEntity
│           ├── SpringDataUserRepository         # (escrita, só do pacote)
│           ├── JpaUserRepositoryAdapter         # implementa UserRepository
│           ├── SpringDataUserQueryRepository    # (leitura, só do pacote)
│           └── JpaUserQueryRepositoryAdapter    # implementa UserQueryRepository
│
└── orders/                          # outro módulo, com a mesma estrutura
    └── infrastructure/acl/          #   ponte para os outros módulos (veja a seção 5)
```

Algumas observações:

- Os pacotes só são criados quando têm algo dentro. Não é preciso montar toda a árvore de uma vez.
- Os repositórios do Spring Data ficam **sem `public`** (visíveis só dentro do pacote `persistence`). Quem usa o
  repositório de fora enxerga apenas as interfaces do domínio e da aplicação.
- Os testes seguem a mesma estrutura de pacotes em `src/test/java`.

---

## 3. DDD leve

Aqui são usadas só as partes que trazem benefício para um projeto deste tamanho.

### O que é usado
- **Domínio rico.** A regra de negócio fica dentro da própria classe, não nos services. Um `Order` sabe se pode ser
  pago, enviado ou cancelado. O service apenas chama `order.pay()` e o pedido decide se é permitido.
- **Value Objects.** Em vez de `String email`, existe a classe `Email`, que valida e normaliza no construtor. Se há
  um `Email` na mão, ele é válido, e não precisa ser conferido de novo. São `record`s imutáveis.
- **Aggregate Root.** É a "porta de entrada" de um grupo de dados. `Order` é a raiz do pedido: para mexer em qualquer
  parte dele, passa-se sempre por `Order`.
- **Repositório como interface do domínio.** O domínio define `UserRepository` (`save`, `findById`, `existsByEmail`), e a
  infraestrutura implementa com Spring Data. O domínio pensa em "coleção de usuários", sem saber de JPA.
- **Fábricas com nome.** `User.create(...)` cria um usuário novo, validando tudo. `User.restore(...)`
  reconstrói um que veio do banco, cujos dados já foram validados quando foram salvos.

### O que fica de fora (por enquanto)
Eventos de domínio, contextos delimitados (*bounded contexts*) formais e serviços de domínio elaborados.

---

## 4. Dois modelos: domínio e persistência

Cada agregado tem **duas representações**, ligadas por um mapper:

- a classe de **domínio** (`User`, `Order`): tem as regras, não conhece o banco;
- a classe de **persistência** (`UserEntity`, `OrderEntity`): é o espelho da tabela, cheia de anotações JPA e
  sem regra de negócio.

Isso dá mais trabalho, mas o banco deixa de ditar como o domínio é escrito (e vice-versa).

### Como os objetos se referenciam

Aqui existem **duas regras diferentes**, uma para cada modelo:

| | Modelo de domínio | Modelo de persistência |
|---|---|---|
| Outro **agregado** | só pelo **id** (`ClientId`) | pode ser **direto** (`@ManyToOne UserEntity`) |
| Dentro do **mesmo agregado** | objeto direto (o agregado contém as suas partes) | relação normal (`@OneToMany`, `@Embedded`) |

O projeto ainda não tem um agregado com partes (como os itens de um pedido), então a segunda linha vale para quando
houver. Um exemplo da primeira:

```java
// DOMÍNIO: o pedido conhece o cliente só pelo id
class Order {
    private ClientId clientId;
    private OrderStatus status;
}

// PERSISTÊNCIA: o pedido aponta para a entidade do usuário, como no banco (chave estrangeira)
@Entity
class OrderEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity client;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}
```

**Por que no domínio é só pelo id:** evita carregar um agregado dentro do outro, mantém cada um independente e deixa
claro onde fica a fronteira de consistência. Quem precisa dos dados do cliente pede ao módulo de usuários.

**Por que na persistência pode ser direto:** ali o modelo descreve o banco, e no banco a ligação é uma chave
estrangeira. Usar a associação do JPA traz a FK criada pelo próprio mapeamento e permite consultas que naveguem entre
as tabelas.

### Como o mapper faz a ponte
Ao salvar, o mapper recebe o `ClientId` do domínio e precisa entregar uma `UserEntity` ao JPA. Ele não acessa o
banco. Quem chama (o adapter, dentro de uma transação) cria uma **referência** com
`entityManager.getReference(UserEntity.class, id)`, que não faz consulta: devolve um objeto que só conhece o id, o
suficiente para gravar a coluna. Ao ler, o caminho inverso é `entity.getClient().getId()`, e com `LAZY` isso também
não consulta nada.

### Cuidados
- **Sempre `LAZY`** nas associações `@ManyToOne`. O padrão do JPA é `EAGER`, que carregaria o usuário junto com
  cada pedido, mesmo sem ninguém precisar.
- **O mapper lê só o id do cliente.** Com `LAZY`, `getClient().getId()` funciona porque o proxy já guarda o id. Ler
  outro campo (`getClient().getName()`) depois que a sessão fechou lança `LazyInitializationException`. Se o domínio
  precisar de mais dados do cliente, ele pede à `users.api`, não à entidade.
- **Partes do mesmo agregado** (`@OneToMany`) são carregadas junto com a raiz, com `join fetch` ou `@EntityGraph`, na
  consulta que o adapter usa. O mapper converte para o domínio depois que a sessão do JPA fechou, e uma coleção
  `LAZY` ainda não carregada lançaria `LazyInitializationException` nesse momento.
- **`equals` pelo id e `hashCode` constante** nas entidades JPA com id gerado pelo banco. O id só existe depois do
  `persist`, e um hash calculado a partir dele mudaria com a entidade já dentro de um `Set`, que então não a
  encontraria mais. O custo é que todas as entidades do tipo caem no mesmo balde, o que só pesa em coleções com
  milhares de elementos; para essas, guarde os **ids** (`Set<Long>`), que têm hash por valor. O `equals` compara com
  `getId()` (não com o campo), porque o outro objeto pode ser um proxy do Hibernate. No domínio (`User`) é diferente:
  o id é `final`, então o hash pode sair dele.
- Usar a entidade de outro módulo é um acoplamento aceito, mas fica **restrito ao pacote
  `infrastructure.persistence`** (o teste de arquitetura, planejado no `TODO.md` §4, vai garantir isso). Domínio, aplicação e web enxergam apenas ids.

---

## 5. Conversa entre módulos

Um módulo não mexe nas coisas internas de outro. Para o comportamento (as regras e as perguntas), a comunicação
passa por duas peças:

- **`api`** (no módulo que oferece): um pacote com a interface pública, por exemplo `UserModuleApi.existsById(id)`.
  É o único pacote que os vizinhos podem importar.
- **`infrastructure/acl`** (no módulo que consome): uma classe que implementa um port do próprio módulo e traduz a
  chamada. É o *Anti-Corruption Layer*, a "alfândega" entre os vocabulários. Exemplo: `orders` define o port
  `ClientLookup`, e `UserModuleClientLookup` o implementa chamando `users.api`.

Resumo das permissões entre módulos:

| Quem | Pode usar |
|---|---|
| `domain`, `application`, `presentation` de um módulo | nada do outro módulo: usam ids próprios e ports próprios (o ACL implementa o port) |
| `infrastructure.acl` | a `api` do outro módulo |
| `infrastructure.persistence` | as **entidades JPA** do outro módulo (para as associações) |

A linha da persistência é a exceção assumida; o motivo e o custo estão na seção 9.

As dependências entre módulos têm **uma direção só** e não podem formar ciclo.

---

## 6. CQRS leve

CQRS é separar **comandos** (quem altera dados) de **consultas** (quem só lê). Na versão pesada isso envolve bancos
separados, filas e eventos. Aqui a versão é bem mais simples, só uma separação de código.

- **Escrita:** `UserCommandService` cria ou carrega o **agregado**, aplica as regras do domínio e salva pelo
  `UserRepository`. O `create` devolve o `UserView` do usuário salvo (montado a partir do agregado, dentro da
  `application`), para o `POST` responder com o recurso criado sem uma consulta a mais. É um desvio assumido do CQRS
  puro, em que o comando devolveria só o id e o controller faria a consulta.
- **Leitura:** `UserQueryService` busca no banco já montando o `UserView` direto da consulta, com
  `select new ...`. Não monta o agregado nem revalida nada, porque ler não altera nada.

```
POST /users              -> Controller -> UserCommandService -> User (domínio) -> UserRepository -> banco
GET  /users/{id}         -> Controller -> UserQueryService   -> UserQueryRepository -> UserView
GET  /users?email=...    -> Controller -> UserQueryService   -> UserQueryRepository -> UserView
GET  /users              -> Controller -> UserQueryService   -> UserQueryRepository -> lista de UserView
```

Nas consultas, o controller passa tipos simples (`Long`, `String`) ao service. É o service que cria os value objects
(`UserId`, `Email`) e os entrega ao port de leitura.

Por que separar:
- a leitura fica mais rápida e simples (sem agregado, sem validar cada linha lida);
- a escrita fica concentrada nas regras;
- uma tela que junta dados de vários lugares vira uma consulta nova, sem deformar o domínio.

O banco é o **mesmo** nos dois lados, sem filas, sincronização ou eventos.

---

## 7. Exemplo de ponta a ponta

`POST /users`:

1. O **controller** recebe o JSON, faz a validação básica (campo vazio, tamanho) e converte o DTO em um comando.
2. O **service** (`application`) cria os value objects (`Name`, `Email`, `Phone`, `DateOfBirth`), confere se o e-mail já está em uso e
   chama `User.create`.
3. O **domínio** garante que o usuário nasce válido.
4. O **adapter** (`infrastructure`) converte `User` em `UserEntity` com o mapper e salva com Spring Data.
5. O resultado volta como `UserView` e o controller o transforma em `UserResponse`.

Cada passo está em uma camada. Se o banco mudar, só o passo 4 muda.

---

## 8. Padrões usados

| Padrão | Onde | Para quê |
|---|---|---|
| Repository | `UserRepository` | O domínio fala em coleção de agregados, sem saber de JPA |
| Adapter (Ports & Adapters) | `JpaUserRepositoryAdapter` | Implementa o port do domínio com Spring Data |
| Mapper | `UserMapper`, `*WebMapper` | Isola os modelos: domínio ↔ JPA ↔ DTO |
| Service Layer | `*CommandService`, `*QueryService` | Um ponto de orquestração e de transação por caso de uso |
| Facade | `<modulo>.api` | Único contrato que os outros módulos enxergam |
| Anti-Corruption Layer | `<modulo>.infrastructure.acl` | Traduz o vocabulário de um módulo para o de outro |
| Factory Method | `User.create`, `User.restore` | Criação válida, com a intenção no nome |
| Value Object | `Email`, `Phone`... | Validação e normalização dentro do tipo |
| Strategy (via enum) | `OrderStatus.canTransitionTo` | Máquina de estados sem `if/else` espalhado |

Hoje o código tem Repository, Adapter, Mapper, Service Layer, Factory Method e Value Object. Facade, Anti-Corruption
Layer e Strategy descrevem o alvo: entram junto com a primeira `api` e com o módulo `orders`.

---

## 9. Custos desta escolha

- **Mais classes** do que num CRUD direto.
- **Mais mapeamentos** (domínio ↔ entidade ↔ DTO). É o preço de manter os modelos separados.
- **Mais trabalho no começo**, em troca de mudanças mais fáceis depois.


### Trade-off: apontar para a entidade JPA de outro módulo

No domínio, `Order` guarda só um `ClientId`. Na persistência, `OrderEntity` aponta direto para a `UserEntity`
(`@ManyToOne`, LAZY). Foi uma escolha consciente, e ela tem custo:

| | `@ManyToOne UserEntity` (adotado) | `Long clientId` (alternativa estrita) |
|---|---|---|
| Chave estrangeira | Criada pelo próprio mapeamento | Escrita à mão (SQL ou Flyway) |
| Consultas | `o.client.name` navega direto | Join manual ou chamada à `users.api` |
| Mapper | Precisa do `getReference` | Só copia o `Long` |
| Independência do módulo | `orders` depende da `UserEntity`, que precisa ser `public` | A `UserEntity` pode ser `package-private`, e o `users` muda o schema à vontade |
| Extrair o módulo para outro serviço | Reescreve entidade, mapper, adapter e consultas | Quase nada muda |
| Proteção | Exige a regra do ArchUnit (planejada, `TODO.md` §4.4) | A própria estrutura impede |

O acoplamento é de **schema**, não de domínio: o domínio de `orders` só conhece o `ClientId`. O que fica acoplado é a
persistência dos dois módulos, e só ela, porque a regra do ArchUnit (planejada, `TODO.md` §4.4) não deixará o resto do `orders` enxergar a `UserEntity`.


---

## 10. Regras

- o `domain` não importa Spring, JPA nem JSON;
- o `presentation` não acessa `domain` nem `infrastructure` (a exceção é o `DomainException`, traduzido pelo
  `GlobalExceptionHandler`);
- a `application` não acessa `infrastructure` nem `presentation`, e do Spring só usa `@Service` e `@Transactional`;
- um módulo só usa a `api` do outro, e as entidades JPA do outro só aparecem em `infrastructure.persistence`;
- não há ciclos entre módulos;
- cada tipo de classe está no pacote certo (controllers em `presentation`, entidades JPA em
  `infrastructure.persistence`...).

Hoje essas regras são conferidas por revisão (e por varredura de imports). O `ArchitectureTest` do `TODO.md` §4 vai
automatizá-las, fazendo o `mvn test` falhar quando uma for quebrada.

---

## 11. Convenções do projeto

### Git
- Uma branch curta por assunto (`feat/...`, `fix/...`, `refactor/...`, `docs/...`), sempre criada a partir da `main`
  atualizada.
- Pull Request com merge commit, nunca squash.
- Mensagens de commit em inglês, no padrão Conventional Commits (`tipo: resumo`). No merge do PR, o título é
  `tipo: resumo (#N)`, com o corpo em inglês.
- A descrição do PR é escrita em português (pt-BR).
- Cada commit deve compilar e passar nos testes.

### API: formato dos dados e dos erros

**Dados.** O JSON usa sempre o formato canônico, na entrada e na saída: telefone só com dígitos (10 ou 11, com DDD) e
datas em ISO (`yyyy-MM-dd`). A máscara (`(11) 91234-5678`, `25/07/2001`) é problema de quem exibe o dado. Assim o que a
API devolve num `GET` pode voltar como entrada num `POST`, e nenhum cliente depende do formato de outro país ou idioma.
O domínio normaliza o que recebe (e-mail em minúsculas e sem espaços nas pontas, espaços repetidos do nome juntados),
e a resposta já traz o valor normalizado. Fora essa normalização, o domínio **não pode alterar em silêncio o que o DTO
aceitou**: o `Phone` descarta tudo o que não é dígito, então o DTO do telefone tem `@Pattern` de só dígitos, para um
`1191234567a` ser recusado (`400`) em vez de gravado como `1191234567`.

**Respostas de sucesso.** `POST /users` responde `201` com o usuário no corpo e o cabeçalho `Location` (URL absoluta do
recurso novo). As consultas respondem `200`.

**Erros.** Todo erro sai no formato `ProblemDetail` (RFC 9457), com `Content-Type: application/problem+json` e os
campos `status`, `title` (o texto padrão do status), `detail` e `instance` (o caminho da requisição):

```json
{"status":422,"title":"Unprocessable Content","detail":"user name is too short","instance":"/users"}
```

| Situação | Status | Quem produz | `detail` |
|---|---|---|---|
| Entrada malformada ou fora do formato: campo vazio, tamanho, formato, JSON ilegível, id não numérico ou não positivo | `400` | Bean Validation no controller e nos DTOs, ou o próprio Spring MVC | no corpo: `Invalid request content.`; em parâmetro: `Validation failure`; nos demais, o texto do Spring |
| Pedido bem formado que viola uma regra de negócio | `422` | `DomainException` | a mensagem da exceção |
| Recurso que não existe | `404` | `NotFoundException` (e suas subclasses) | a mensagem da exceção |
| Dado válido que colide com o que já existe (e-mail em uso) | `409` | `ConflictException` (e suas subclasses) | a mensagem da exceção |
| Rota inexistente, método ou tipo de conteúdo não aceitos | `404`, `405`, `415` | Spring MVC | o texto do Spring |

O `400` e o `422` se complementam. A validação do DTO barra cedo o que é óbvio (um e-mail sem `@`), e o domínio continua
sendo a autoridade: `ana@mail` passa no `@Email` do DTO, mas o `Email` do domínio exige uma extensão, e o resultado é
`422`. Por isso as mesmas regras podem aparecer nos dois lugares, e o domínio nunca depende do DTO. O mesmo vale para
os parâmetros de consulta: `GET /users?email=` valida o e-mail como o campo do `POST` (obrigatório, formato e no
máximo 100 caracteres), então a mesma entrada dá o mesmo status nos dois endpoints.

Os erros de validação (`400` de `@Valid` no corpo e de parâmetros do caminho ou da consulta, como `@Positive`,
`@NotBlank` e `@Email`) trazem também a lista `errors`, com o campo (ou o nome do parâmetro) e a mensagem, ordenada por campo e
depois por mensagem:

```json
{"status":400,"title":"Bad Request","detail":"Invalid request content.","instance":"/users",
 "errors":[{"field":"email","message":"user email address is invalid"},
           {"field":"name","message":"user name is required"}]}
```

**Mensagens.** O texto de cada erro é fixo e em inglês, e não depende do idioma do cliente nem da máquina:
- as mensagens do Bean Validation ficam em `src/main/resources/messages.properties` (chaves como `user.name.required`,
  com `{min}` e `{max}` tirados da própria anotação). Toda anotação de validação deve usar `message = "{chave}"`:
  sem isso o texto padrão sai no idioma da requisição (`não deve estar em branco` com `Accept-Language: pt-BR`);
- as exceções do domínio e da aplicação montam a própria mensagem (`user not found with id 9`,
  `user email address ana@mail.com is already in use`).

**Onde fica.** O `GlobalExceptionHandler` está em `shared/presentation` e estende `ResponseEntityExceptionHandler`, por
isso os erros do Spring MVC saem no mesmo formato. Os handlers valem para as subclasses: cada módulo cria a sua
exceção (`UserNotFoundException extends NotFoundException`) sem mexer no handler. O `GlobalExceptionHandlerTest` fixa
o contrato (status, tipo e corpo de cada caso), e o `UserApiTest` o confere de ponta a ponta.

**Consistência entre as camadas.** Os mesmos limites aparecem no DTO, no value object e na coluna, porque o
`presentation` não pode importar o domínio. O `UserValidationConsistencyTest` evita que um lado mude sem o outro:
confere que os limites de tamanho são iguais nas três camadas, que toda anotação de validação usa uma chave que existe
no `messages.properties` (com os `{min}` e `{max}` que a anotação realmente tem) e que o domínio nunca recusa nem
altera um telefone que o DTO aceita. Ao criar um campo ou uma regra nova, o teste deve ser estendido.

**Ainda sem tratamento próprio.** Uma exceção inesperada responde `500` com o formato padrão do Spring Boot, e a corrida
de dois `POST` simultâneos com o mesmo e-mail (o banco recusa pela restrição de unicidade) também vira `500`. O caso
natural para essa corrida é um `409`, tratando a `DataIntegrityViolationException` no handler.

---

## 12. Glossário rápido

- **Entidade de domínio**: a classe com as regras (`User`, `Order`). Não tem anotações de banco.
- **Entidade JPA**: a classe espelho da tabela (`UserEntity`). Não tem regras.
- **Value Object**: um valor com regra própria e sem identidade (`Email`, `Phone`).
- **Aggregate Root**: a classe principal de um grupo; é por ela que se altera o grupo.
- **Port**: interface que o domínio ou a aplicação define para dizer "eu preciso disso".
- **Adapter**: classe da infraestrutura que implementa o port.
- **View**: objeto de saída da `application`, sem regras. Na leitura é montado direto da consulta (`select new`); na
  escrita, a partir do agregado recém-salvo, para o `POST` poder devolver o recurso criado.
- **Mapper**: classe que converte um modelo em outro.
- **ProblemDetail**: formato padrão (RFC 9457) das respostas de erro da API (`status`, `title`, `detail`, `instance`).
- **ACL (Anti-Corruption Layer)**: classe que traduz o vocabulário de um módulo para o de outro.
