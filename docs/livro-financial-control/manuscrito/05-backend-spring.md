# PARTE VI — Backend Java e Spring

# Capítulo 5 — Anatomia do backend

O backend do Financial Control é um monólito modular Java que expõe uma API
REST sob `/api/v1`. Em agosto de 2026, a auditoria encontrou 270 arquivos Java
de produção, 19 Controllers, 18 Services, 27 Repositories, 27 Entities e 112
DTOs. A execução da suíte terminou com 578 testes aprovados.

Ele é a autoridade das regras financeiras. Angular apresenta valores e melhora
a experiência de preenchimento; saldos, remaining, limites, rateios e
transições são decididos no backend.

## 5.1 Estrutura e stack efetivamente usadas

```text
backend/
├─ pom.xml
├─ mvnw.cmd
└─ src/
   ├─ main/
   │  ├─ java/br/com/financialcontrol/
   │  │  ├─ accounts/
   │  │  ├─ expenses/
   │  │  ├─ incomes/
   │  │  ├─ credit_cards/
   │  │  ├─ credit_card_invoices/
   │  │  ├─ ...demais domínios
   │  │  ├─ config/
   │  │  └─ security/
   │  └─ resources/
   │     ├─ application.yml
   │     └─ db/migration/
   └─ test/
```

| Tecnologia | Uso real |
|---|---|
| Java 25 | linguagem e runtime |
| Spring Boot 4.1.0 | configuração e composição da aplicação |
| Spring Web MVC | Controllers, JSON e HTTP |
| Spring Security | filtro stateless, JWT e Argon2 |
| Spring Data JPA/Hibernate | persistência |
| Jakarta Validation | validação de requests |
| Flyway | criação e evolução do schema |
| Nimbus JOSE JWT | assinatura e leitura HS256 |
| BouncyCastle | suporte ao Argon2id |
| springdoc-openapi | OpenAPI e Swagger UI |
| OpenPDF 3.0.5 | relatórios PDF |
| JUnit/Testcontainers | testes com PostgreSQL real |
| Spotless | Google Java Format no `verify` |

Não há H2, MapStruct, camada DAO adicional, UseCase por CRUD ou arquitetura
hexagonal artificial.

## 5.2 Configuração do Spring Boot

O `application.yml` define:

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:30}
```

`open-in-view: false` evita consultas lazy acidentais durante a serialização.
`ddl-auto: validate` mantém o Flyway como única autoridade estrutural.
`JWT_SECRET` não possui valor padrão: a aplicação falha cedo quando o segredo
não está configurado.

`JacksonConfig` ativa `FAIL_ON_UNKNOWN_PROPERTIES`. Um campo JSON desconhecido
em request não é ignorado silenciosamente; produz erro 400.

## 5.3 Fluxo Controller → Service → Repository

```text
requisição HTTP
      ↓
Controller
  traduz HTTP, @Valid e principal autenticado
      ↓
Service
  regras, ownership, transação e orquestração
      ↓
Repository
  consultas e persistência JPA
      ↓
PostgreSQL
```

Nenhum Controller auditado acessa Repository diretamente.

### Controller

O Controller conhece:

- URL e verbo;
- path variables e query parameters;
- request/response DTO;
- status HTTP;
- `AuthenticatedUser`;
- Service do módulo.

Exemplo representativo:

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public TransferResponse create(
    @AuthenticationPrincipal AuthenticatedUser user,
    @Valid @RequestBody CreateTransferRequest request) {
  return transferService.create(user, request);
}
```

Ele não calcula saldo e não decide se a transferência é válida.

### Service

O Service concentra invariantes. Em uma transferência, ele valida:

1. origem diferente do destino;
2. data não futura;
3. ownership e atividade das contas;
4. tipo `BANK_ACCOUNT`;
5. saldo disponível;
6. ordem de locks;
7. persistência atômica.

### Repository

Repositories são interfaces exigidas pelo Spring Data:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT a FROM Account a
    WHERE a.id = :id AND a.userId = :userId
    """)
Optional<Account> findByIdAndUserIdForUpdate(UUID id, UUID userId);
```

Não existe DAO sobre o Repository.

## 5.4 DTOs e fronteira HTTP

Entities nunca são serializadas como contrato público. Requests e responses são
records ou classes específicas do endpoint.

```java
public record CreateTransferRequest(
    @NotNull UUID sourceAccountId,
    @NotNull UUID destinationAccountId,
    @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
    @NotNull LocalDate transferDate,
    String description) {}
```

Bean Validation verifica formato e limites estruturais. O Service verifica
regras que dependem do estado persistido.

> **CONCEITO — DTO não é Entity reduzida**
>
> DTO é contrato da fronteira. Ele protege o cliente contra alterações internas,
> impede mass assignment e permite que a API exponha derivados sem criar
> colunas.

Paginação usa o envelope:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

O projeto não expõe diretamente `Page` do Spring Data.

## 5.5 Entities e JPA

Padrões observados:

- `@Table` alinhado ao nome plural da migration;
- ID UUID v7 atribuído pela aplicação;
- `user_id` em dados financeiros;
- `BigDecimal` com precisão 19 e escala 2;
- enum como `EnumType.STRING`;
- `Instant` para instantes e `LocalDate` para calendário financeiro.

Valores derivados não são propriedades persistidas. `AccountService` calcula
saldo; `InstallmentBalanceService` calcula obrigação e remaining; Services de
cartão calculam limites.

## 5.6 Ownership

O usuário vem do JWT:

```java
@AuthenticationPrincipal AuthenticatedUser authenticatedUser
```

O Service consulta sempre pelo par:

```java
repository.findByIdAndUserId(id, authenticatedUser.userId())
```

O body não escolhe o proprietário. Recurso alheio retorna 404, evitando revelar
sua existência. O banco reforça o isolamento com FKs compostas.

## 5.7 Transações, locks e concorrência

Services de escrita usam `@Transactional`; leituras usam
`@Transactional(readOnly = true)` quando apropriado.

Operações críticas usam `PESSIMISTIC_WRITE`. Transferências travam as duas
contas em ordem determinística de UUID:

```java
if (sourceId.compareTo(destinationId) < 0) {
  first = lock(sourceId);
  second = lock(destinationId);
} else {
  first = lock(destinationId);
  second = lock(sourceId);
}
```

Essa ordem reduz deadlocks. Testes de concorrência cobrem transferências,
pagamentos de fatura e metas.

## 5.8 Tratamento centralizado de erros

O envelope real é:

```java
public record ApiError(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    Map<String, String> fields) {}
```

| Exceção | HTTP | Código |
|---|---:|---|
| validação | 400 | `VALIDATION_ERROR` |
| regra de negócio | 400 | `BUSINESS_RULE_VIOLATION` |
| não encontrado | 404 | `NOT_FOUND` |
| não autenticado | 401 | `UNAUTHORIZED` |
| conflito | 409 | `CONFLICT` |
| inesperado | 500 | `INTERNAL_ERROR` |

O projeto não usa RFC 7807.

### Divergência: subcódigos não existem no JSON

Constantes como `SETTLEMENT_NOT_ALLOWED` e
`SURCHARGE_REQUIRES_REMAINING` identificam regras no código e na documentação,
mas a resposta contém somente:

```json
{
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Mensagem da regra em português."
}
```

Não há campo `rule` ou `subCode`. Clientes não devem supor que o nome da
constante é transmitido.

## 5.9 Segurança do backend

```text
POST /auth/login
  ↓
AuthService verifica Argon2id
  ↓
JwtService cria token HS256, sub = UUID
  ↓
Authorization: Bearer ...
  ↓
JwtAuthenticationFilter
  ↓
AuthenticatedUser no SecurityContext
```

`SecurityConfig` usa sessão stateless, desativa CSRF para a API Bearer e
configura CORS. São públicos health, registro, login e documentação OpenAPI. Os
demais endpoints `/api/v1/**` exigem autenticação.

O token expira em 30 minutos por padrão. Não há refresh token, logout de servidor,
OAuth, MFA ou roles na V1.

Senhas usam `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`.
`AuthService` compara um hash dummy quando o login não encontra usuário, reduzindo
diferenças de tempo que poderiam revelar e-mails cadastrados.

## 5.10 OpenAPI e Swagger

Controllers usam `@Tag`, `@Operation`, `@ApiResponses` e
`@SecurityRequirement`. Em desenvolvimento:

- OpenAPI: `http://localhost:8080/v3/api-docs`;
- Swagger UI: `http://localhost:8080/swagger-ui.html`.

As anotações ajudam descoberta, mas `docs/25-api.md` continua sendo o contrato
detalhado de negócio.

## 5.11 Scheduler de faturas

`InvoiceClosingScheduler` executa:

```java
@Scheduled(cron = "0 5 * * * *", zone = "America/Sao_Paulo")
public void closeDueInvoices() {
  creditCardInvoiceService.closeDueInvoices();
}
```

O fechamento é idempotente e automático. Não existe endpoint normal para o
usuário fechar fatura manualmente.

## 5.12 Testes do backend

A suíte auditada executou 578 testes sem falhas.

| Categoria | Exemplos |
|---|---|
| API | `ExpenseApiTest`, `Phase14ApiTest`, `ReportsApiTest` |
| domínio puro | `InvoiceAllocationCalculatorTest` |
| schema | `SchemaContractTest` |
| ownership | `OwnershipAndPersistenceTest` |
| segurança | `JwtServiceTest`, `Argon2PasswordEncoderTest` |
| concorrência | `TransferConcurrencyTest` |

Testcontainers sobe `postgres:18-alpine`. Isso testa SQL, migrations e
constraints do banco oficial, evitando a falsa compatibilidade que um banco em
memória poderia introduzir.

### Lacunas pontuais de cobertura

- refund ACCOUNT/NONE com `settlement` é bloqueado no código, mas não possui
  teste dedicado encontrado;
- o bloqueio de edição de parcela já em fatura não tem teste dedicado;
- a tabela legada `credit_card_invoice_installments` ainda é exigida por teste
  de schema, embora não participe dos Services.

## 5.13 Como adicionar uma funcionalidade

1. Registrar requisito e regras nos documentos oficiais.
2. Parar se modelo, cálculo, status ou ownership não estiverem decididos.
3. Criar migration somente após a decisão.
4. Mapear Entity e Repository.
5. Criar DTOs de request e response.
6. Implementar Service transacional com ownership.
7. Criar Controller fino.
8. Documentar OpenAPI e contrato em `docs/25`.
9. Testar regra, persistência, HTTP, segurança e concorrência quando aplicável.
10. Executar `mvnw.cmd test` e `mvnw.cmd verify`.

> **NÃO FAÇA**
>
> Não crie UseCase para cada verbo CRUD, interface e `Impl` para toda classe,
> mapper automático sem necessidade, nem aceite `userId` do cliente.

## 5.14 Anatomia resumida: transferência

### HTTP

```text
POST /api/v1/transfers
```

### Fluxo

```text
TransferController
  → TransferService
      → lock das contas em ordem UUID
      → valida BANK_ACCOUNT e saldo disponível
      → TransferRepository.save(ACTIVE)
      → trava saldo inicial das duas contas
```

O registro não grava saldos. `AccountService` inclui transferências ACTIVE ao
calcular saldo. A reversão muda o status para REVERSED e preserva histórico.

### Por que essa feature é didática

Ela atravessa todas as decisões centrais:

- DTO e `@Valid`;
- identidade vinda do JWT;
- ownership;
- Service transacional;
- duas linhas com lock;
- valor monetário BigDecimal;
- fato persistido e saldo derivado;
- ação de negócio por POST, sem DELETE;
- teste de efeito no saldo e teste de concorrência.

## 5.15 Pendências que não podem ser preenchidas por suposição

| Tema | Estado |
|---|---|
| valores de `payments.type` | **PENDENTE** |
| edição cadastral de parcela já em fatura | **PENDENTE/deferida** |
| refresh token | **não implementado por decisão de escopo** |
| extrato unificado | **planejado fora do escopo atual** |
| remoção da tabela V13 superada | **requer migration e decisão explícita** |

