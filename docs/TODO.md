# TODO

Lista de trabalho do projeto. Os itens estão na ordem em que devem ser feitos, e o arquivo vai sendo atualizado
conforme o projeto avança (itens novos entram, os concluídos são marcados).

A arquitetura e o porquê das regras estão em [`docs/architecture.md`](docs/architecture.md).

Legenda: `[ ]` pendente · `[x]` concluído · 🔴 prioridade alta · 🟠 média · 🟢 baixa

Camadas citadas nos itens: `domain` · `application` · `presentation` · `persistence` (que fica em
`infrastructure/persistence`) · `acl` (que fica em `infrastructure/acl`).

---

## Convenções do projeto

- **Git:** uma branch curta por assunto (`feat/...`, `fix/...`, `refactor/...`, `docs/...`), sempre criada a partir da
  `main` atualizada. Pull Request com merge commit, nunca squash. Mensagens em inglês, no padrão Conventional Commits
  (`tipo: resumo`; no merge do PR, `tipo: resumo (#N)`, com o corpo em inglês). Cada commit deve compilar e passar nos
  testes.
- **Comentários didáticos** nas partes importantes do código e nos testes (o que cada mock faz e por quê).
- **Perfil:** seguir o curso (perfil `test` com H2 em `application-test.properties`).
- **Fora do escopo por enquanto:** segurança (autenticação e autorização) e paginação. Entram quando o curso abordar
  (seção 7).

---

## 1. Criar o projeto

- [ ] **1.1** 🔴 Gerar o projeto no Spring Initializr (Maven, Java 21) com: Spring Web, Spring Data JPA, H2 Database
  e Validation. Segurança fica para depois.
- [ ] **1.2** Criar o repositório no GitHub (vazio, sem README) e conectar com `git remote add origin`.
- [ ] **1.3** Conferir o `.gitignore` (`target/`, `.idea/` e arquivos de IDE).
- [ ] **1.4** Configurar `application.properties`: nome da aplicação, `spring.jpa.open-in-view=false` e o perfil do
  curso fixo (`spring.profiles.active=test`). Em `application-test.properties`: URL do H2, console do H2 e `show-sql`.
- [ ] **1.5** Adicionar este `architecture.md` em `docs/` e este `TODO.md` na raiz.
- [ ] **1.6** Primeiro commit na `main` e push:
  `chore: initial Spring Boot project and architecture docs`.

---

## 2. Base compartilhada (`shared`)

Peças que todos os módulos vão usar. Criar antes do primeiro módulo.

- [ ] **2.1** `domain`: `DomainException` (erro de regra de negócio, vira HTTP 422).
- [ ] **2.2** `application`: `NotFoundException` (base das exceções "não encontrado", vira HTTP 404) e
  `ConflictException` (base das exceções de conflito, vira HTTP 409).
- [ ] **2.3** `presentation`: `GlobalExceptionHandler`, que traduz as exceções acima em respostas HTTP
  padronizadas (`ProblemDetail`) e também trata o erro de validação (400). Se o curso ensinar um formato de erro
  próprio, adaptar para ele.
- [ ] **2.4** `domain`: `AggregateRoot`. Classe base opcional dos agregados; pode ficar para quando houver uso real.
- [ ] **2.5** Teste do `GlobalExceptionHandler`, fixando o contrato de erros (status e corpo de cada caso).

---

## 3. Primeiro módulo: `users`

Primeira entidade do curso. Fazer **de dentro para fora**: domínio, persistência, aplicação, web.

A senha **não entra agora**: ela depende de hash e de segurança, que ficam para quando o curso abordar o assunto
(itens 7.2 e 7.3). Por enquanto o `User` tem nome, e-mail, telefone e data de nascimento.

### 3.1 Domínio (`users/domain`)
- [ ] **3.1.1** `vo/UserId` (record com `Long`, não aceita nulo nem valor menor que 1).
- [ ] **3.1.2** Value objects `vo/Name`, `vo/Email`, `vo/Phone` e `vo/DateOfBirth`, cada um com sua validação e
  normalização (ex.: `Email` em minúsculas e sem espaços nas pontas). Os erros lançam `DomainException`.
- [ ] **3.1.3** `User`: o agregado, com `create(...)` (novo, valida tudo) e `restore(...)` (vindo do banco).
  Métodos de negócio entram aqui (ex.: trocar e-mail e telefone).
- [ ] **3.1.4** `UserRepository`: interface de escrita (`save`, `findById`, `existsByEmail`).
- [ ] **3.1.5** Testes de unidade do domínio (JUnit puro, sem Spring): value objects e `User`.

### 3.2 Persistência (`users/infrastructure/persistence`)
- [ ] **3.2.1** `UserEntity`: entidade JPA (`tb_user`), só colunas e anotações. Construtor padrão sem `public`,
  `equals`/`hashCode` pelo id. Restrição `UNIQUE` em `email`, com nome (`uk_user_email`). A classe é `public`, porque
  o `orders` vai apontar para ela (`architecture.md`, seção 9).
- [ ] **3.2.2** `UserMapper`: `toEntity(User)` e `toDomain(UserEntity)`.
- [ ] **3.2.3** `SpringDataUserRepository` (`JpaRepository`, sem `public`), com `existsByEmail`.
- [ ] **3.2.4** `JpaUserRepositoryAdapter`: implementa `UserRepository` usando o mapper.
- [ ] **3.2.5** `import.sql` com alguns usuários de exemplo.
- [ ] **3.2.6** Teste de que o banco recusa e-mail duplicado (a última linha de defesa, mesmo que a checagem do service
  falhe numa corrida entre duas requisições).

### 3.3 Aplicação (`users/application`)
- [ ] **3.3.1** `view/UserView`: record de leitura.
- [ ] **3.3.2** `command/CreateUserCommand`.
- [ ] **3.3.3** `exception/UserNotFoundException` (herda `NotFoundException`) e `EmailAlreadyInUseException`
  (herda `ConflictException`).
- [ ] **3.3.4** `port/UserQueryRepository`: interface de leitura (devolve `UserView`).
- [ ] **3.3.5** `UserCommandService` (`@Transactional`): criar usuário, conferindo `existsByEmail` antes de salvar.
- [ ] **3.3.6** `UserQueryService` (`@Transactional(readOnly = true)`): buscar por id e listar.
- [ ] **3.3.7** Testes de unidade dos services com Mockito, comentando o que cada mock faz.

### 3.4 Leitura na persistência
- [ ] **3.4.1** `SpringDataUserQueryRepository`: estende `Repository<>` (não `JpaRepository`) e projeta direto em
  `UserView` com `select new`.
- [ ] **3.4.2** `JpaUserQueryRepositoryAdapter`: implementa `UserQueryRepository`.

### 3.5 Web (`users/presentation`)
- [ ] **3.5.1** `dto/CreateUserRequest` e `dto/UserResponse`, com Bean Validation nos campos de entrada
  (`@NotBlank`, `@Email`, `@Size`, `@PastOrPresent`). Os limites de tamanho devem bater com os das colunas.
- [ ] **3.5.2** `UserWebMapper`: DTO ↔ comando/view.
- [ ] **3.5.3** `UserController`: `POST /users` (responde 201 com `Location`), `GET /users/{id}` e `GET /users`.
- [ ] **3.5.4** Testes de API com `MockMvc` (status, headers e JSON de cada endpoint, incluindo os erros: 400 de
  validação, 404 de usuário inexistente, 409 de e-mail duplicado, 422 de regra de domínio).

### 3.6 Fechamento
- [ ] **3.6.1** Rodar `./mvnw clean test` e conferir que tudo passa.
- [ ] **3.6.2** Abrir o PR do módulo (`feat/users-module`) e fazer o merge com merge commit.

### 3.7 Depois, quando o curso pedir
- [ ] **3.7.1** Alterar usuário (`PUT /users/{id}`) e remover (`DELETE /users/{id}`).

---

## 4. Teste de arquitetura

Adicionar logo depois do primeiro módulo funcionando, para que as regras reflitam o código real.

- [ ] **4.1** 🟠 Adicionar a dependência `archunit-junit5` (escopo `test`).
- [ ] **4.2** `ArchitectureTest` com as regras de camadas:
  `domain` sem nada de Spring, JPA ou JSON; `domain` sem conhecer as outras camadas; `presentation` só fala com
  `application` (exceção: o `GlobalExceptionHandler` enxerga o `DomainException` para traduzi-lo); `application`
  sem `infrastructure`/`presentation`/JPA e, do Spring, só com `org.springframework.stereotype` e
  `org.springframework.transaction` (para `@Service` e `@Transactional`; liberar `org.springframework.data.domain`
  quando chegar a paginação, item 7.1); ninguém fora de `infrastructure` usa `infrastructure`.
- [ ] **4.3** Regras de localização: controllers em `presentation`, services em `application`, entidades JPA em
  `infrastructure.persistence`, adapters (`@Repository`) em `infrastructure`.
- [ ] **4.4** Regras entre módulos (valem a partir do segundo módulo): módulos sem ciclos; só `infrastructure.acl`
  usa a `api` do vizinho; entidades JPA do vizinho só em `infrastructure.persistence`. Essa última é o que mantém sob
  controle o acoplamento descrito em `architecture.md`, seção 9.
- [ ] **4.5** Todas as regras com `.because("...")`, explicando o motivo, para a falha ensinar.

---

## 5. Módulo `orders`

Segundo módulo e o primeiro que **conversa com outro**: cada passo abaixo existe por causa dessa conversa.
Mesmo roteiro da seção 3, com os acréscimos de comunicação entre módulos.

- [ ] **5.1** `users/api`: `UserModuleApi` com `existsById(Long)`, implementada por um facade na `application` de
  `users`. É o único pacote de `users` que `orders` poderá enxergar.
- [ ] **5.2** `orders/domain`: `OrderId`, `ClientId` (o `orders` tem o **seu próprio** id de cliente, sem reaproveitar o
  `UserId`), `OrderStatus` com as transições permitidas, `Order` (`create`, `restore`, `pay`, `ship`, `deliver`,
  `cancel`) e `OrderRepository`. Testes de unidade de todas as transições.
- [ ] **5.3** `orders/application`: port `ClientLookup` (`exists(ClientId)`), `OrderView`, `OrderQueryRepository`,
  `OrderQueryService`, exceções `OrderNotFoundException` e `ClientNotFoundException`.
- [ ] **5.4** `orders/infrastructure/acl`: `UserModuleClientLookup`, que implementa `ClientLookup` chamando
  `users.api`.
- [ ] **5.5** `orders/infrastructure/persistence`:
  - `OrderEntity` com `@ManyToOne(fetch = LAZY)` para `UserEntity` e a FK com nome (`fk_order_user`) criada pelo
    mapeamento, mais índice em `tb_order.user_id` (`@Table(indexes = ...)`), com nome, porque nem todo banco cria
    índice para FK sozinho (o PostgreSQL não cria);
  - `OrderMapper` recebendo a `UserEntity` de referência, e adapter criando-a com
    `entityManager.getReference(UserEntity.class, id)` (o `save` deve ser `@Transactional`);
  - consultas de leitura usando `o.client.id`, que não faz join.
- [ ] **5.6** `orders/presentation`: `OrderResponse`, `OrderWebMapper` e `OrderController`
  (`GET /orders`, `GET /orders/{id}`, `GET /users/{id}/orders`, esta última com 404 se o cliente não existir).
- [ ] **5.7** Testes: de persistência (ida e volta do pedido **fora de transação**, para provar o `LAZY`; FK e índice
  lidos do schema real) e de API.
- [ ] **5.8** Completar o `ArchitectureTest` com as regras entre módulos (item 4.4).
- [ ] **5.9** Lado de escrita do pedido: `OrderCommandService` (criar pedido validando o cliente pelo `ClientLookup`,
  pagar, enviar, entregar, cancelar) e os endpoints correspondentes.

---

## 6. Qualidade e acabamento

- [ ] **6.1** 🟠 `README.md` na raiz: o que é o projeto, como rodar e link para `docs/architecture.md`.
- [ ] **6.2** 🟠 CI no GitHub Actions: `./mvnw verify` a cada push e PR.
- [ ] **6.3** 🟢 Documentação da API (springdoc-openapi).
- [ ] **6.4** 🟢 Migrations (Flyway) no lugar do schema gerado pelo Hibernate + `import.sql`.
- [ ] **6.5** 🟢 `Clock` injetável nas regras que dependem da data atual, para testes determinísticos.
- [ ] **6.6** 🟢 Controle otimista (`@Version`) nas entidades alteradas com frequência (pedidos).
- [ ] **6.7** 🟢 Banco real (PostgreSQL) em um perfil próprio, com Testcontainers nos testes de integração.

---

## 7. Quando o curso abordar

Itens adiados de propósito, para acompanhar o conteúdo do curso.

- [ ] **7.1** Paginação (`Pageable`) nas listagens, com limite de tamanho e ordenação permitida.
- [ ] **7.2** Autenticação e autorização (OAuth2/JWT). Ao chegar aqui:
  - restringir o console do H2 e o `X-Frame-Options` ao perfil de desenvolvimento;
  - remover o perfil `test` fixo do `application.properties` e ativar por `SPRING_PROFILES_ACTIVE`;
  - parar de tratar todos os endpoints como públicos e proteger dados pessoais.
- [ ] **7.3** Senha do usuário: value object `PasswordHash`, port `UserPasswordHasher` no domínio e o adapter com o
  algoritmo de hash na `infrastructure`.
- [ ] **7.4** Demais entidades e módulos do curso: seguir o roteiro da seção 3, respeitando as regras de referência
  entre agregados (id no domínio, associação direta na persistência).
- [ ] **7.5** 🟢 (opcional, fora do curso) Explorar o Spring Modulith (verificação de módulos, eventos entre módulos,
  testes por módulo). No modo padrão ele não aceita o `@ManyToOne` para a entidade de outro módulo, então exigiria
  migrar para a alternativa estrita (`Long clientId`); ver `architecture.md`, seção 9. Conferir a versão compatível
  com o Spring Boot do projeto.
- [ ] **7.6** Validações personalizadas e demais tópicos conforme o curso avançar.

---

## Decisões já tomadas

- **Estilo:** camadas por módulo (monolito modular), com a dependência invertida: o domínio fica no centro e a
  infraestrutura depende dele, no espírito de Clean/Hexagonal.
- **Dois modelos por agregado:** domínio e persistência separados, ligados por mapper.
- **Referência entre agregados:** no domínio, só por id; na persistência, pode ser direta (`@ManyToOne` LAZY).
  O acoplamento com a entidade do outro módulo fica restrito a `infrastructure.persistence`. A alternativa
  (`Long clientId` na persistência) foi avaliada e deixada de lado; o trade-off está em `architecture.md`, seção 9.
- **Dentro do mesmo agregado**, objetos se referenciam diretamente nos dois modelos. Ao carregar o agregado, as
  partes vêm junto (`join fetch`/`@EntityGraph`).
- **CQRS leve:** escrita via agregado e `*CommandService`; leitura via `*View` e `*QueryService`, no mesmo banco.
- **Frameworks por camada:** o `domain` não usa nenhum (Java puro); a `application` pode usar `@Service` e
  `@Transactional`, mas não JPA nem web.
