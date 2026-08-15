# Financial Control — Coding Standards

**Version:** 1.1  
**Status:** Active  
**Scope:** Entire project

---

## 1. Purpose and authority

This document defines coding, organizational, naming, persistence, API, frontend, and testing conventions for Financial Control.

It is **not** the highest-authority document.

Hierarchy:

```text
AGENTS.md
  ↓
docs/20–28 — official functional and technical decisions
  ↓
docs/CODING_STANDARDS.md — code and organization conventions
  ↓
.cursor/rules/*.mdc — operational instructions for Cursor
```

This document must not contradict `AGENTS.md` or `docs/20–28`.

`.cursor/rules/*.mdc` files must not create new architectural decisions.

If a technical rule is not decided in a higher document, do not invent a silent decision. Stop and use **DECISÃO PENDENTE DO DESENVOLVEDOR**.

These standards favor practical, maintainable engineering over architectural complexity.

---

## 2. Core Principles

### 2.1 Simplicity First

Prefer the simplest solution that correctly solves the current problem.

Do not introduce abstractions, layers, patterns, or infrastructure without a concrete reason.

### 2.2 Responsibility Over Convention

Every class, method, package, module, and abstraction must have a clear responsibility.

A structure must exist because it provides a meaningful responsibility, not merely because it is considered a common pattern.

### 2.3 No Premature Architecture

Prepare for evolution, but do not build the future before it exists.

### 2.4 Adapt Principles, Not Legacy Implementations

Use conventions appropriate to Java, Spring Boot, Angular, TypeScript, PostgreSQL, and the modern web ecosystem.

Do not copy Delphi naming or layering mechanically.

### 2.5 Backend as the Source of Truth

Business rules that affect financial state must be enforced by the backend.

### 2.6 Technology Independence

The API is a stable contract independent of Java, Spring, JPA, PostgreSQL internals, and Angular.

### 2.7 Avoid Accidental Complexity

When two valid approaches solve the same problem, prefer the one with fewer unnecessary concepts, dependencies, and abstractions.

---

## 3. Official stack versions

Do not invent tutorial versions. Official versions live in `AGENTS.md` and `docs/22-stack-tecnologica.md`:

- Java 25 LTS
- Spring Boot 4.1.x
- Maven 3.9.x (≥ 3.9.12) with Maven Wrapper
- Angular 22.x
- Node.js 22.x LTS (≥ 22.22.3); 24.x ≥ 24.15.0 accepted
- PostgreSQL 18
- Package: `br.com.financialcontrol`
- API: `/api/v1`
- Currency V1: BRL

---

## 4. Architecture

The official backend flow is:

```text
HTTP
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL
```

Controller does **not** access Repository directly.

Even simple reads go through the module Service when they belong to an application feature.

This does **not** mean creating a UseCase per operation.

A small Service is valid:

```text
AccountController
  ↓
AccountService
  ↓
AccountRepository
```

Do not force every operation through extra layers (mapper, use case, domain service, adapter) when the module Service already owns the responsibility.

---

## 5. Project structure

```text
Financeiro/
├── backend/
├── frontend/
├── docs/
│   ├── 20–28
│   └── CODING_STANDARDS.md
└── .cursor/
    └── rules/
```

Do not create empty or speculative folders merely to anticipate future functionality.

Phase 1 must not create empty domain packages only to “prepare the future”.

---

## 6. Module organization

Organize backend code by real domain modules, using plural lowercase names aligned with tables and HTTP resources:

```text
br.com.financialcontrol
├── accounts
├── expenses
├── incomes
├── transfers
├── payments
├── credit_cards
├── credit_card_invoices
├── financial_goals
├── dashboard
└── reports
```

Do **not** create a generic `transactions` module to group incomes, expenses, transfers, and payments.

Do **not** create a generic `common/`, `utils/`, `helpers/`, or `managers/` package without a genuine shared responsibility.

A simple module may contain:

```text
accounts/
├── Account.java
├── AccountService.java
├── AccountRepository.java
├── AccountController.java
└── dto/
    ├── CreateAccountRequest.java
    └── AccountResponse.java
```

Do not create the second structure prematurely:

```text
CreateAccountUseCase.java
GetAccountUseCase.java
AccountMapper.java
AccountDto.java
AccountView.java
```

---

## 7. Dependency rules

### 7.1 Controllers

Controllers may depend on:

- module services;
- DTOs;
- framework HTTP components.

Controllers must not:

- access repositories;
- contain database queries;
- implement business rules;
- calculate authoritative financial values.

### 7.2 Services

Services coordinate application operations and enforce business rules that do not naturally belong to a single entity.

Services may depend on:

- domain objects / entities;
- repositories;
- other services only when the business relationship requires it.

### 7.3 Domain / entities

Entities may protect their own invariants.

They must not depend on HTTP, Angular, controllers, or presentation logic.

Entity behavior must not become a hidden balance cache. Account balance is derived from financial movements.

Prefer `expense.cancel()` over `expense.setStatus(CANCELLED)` when cancellation has rules.

For incomes: `cancel()` is `EXPECTED` → `CANCELLED` (inutiliza a duplicata). `reverse()` is `RECEIVED` → `EXPECTED` (desfaz o recebimento). They are not the same operation.

Do not model `account.debit(amount)` as mutation of an independent `currentBalance`.

### 7.4 Repositories

Repositories handle persistence and data access.

They must not become business services.

Financial queries must respect the authenticated user.

### 7.5 No circular dependencies

If A depends on B, B must not require A to complete the same responsibility.

---

## 8. Use Case

UseCase is **not** the default abstraction.

Do not create:

```text
CreateExpenseUseCase
GetExpenseUseCase
ListExpenseUseCase
UpdateExpenseUseCase
DeleteExpenseUseCase
```

Use a module Service.

A UseCase may exist only when the operation is:

- a clearly named business action;
- atomic;
- complex enough to justify its own class.

Example that *may* be justified:

```text
TransferMoneyUseCase
```

only if transfer orchestration is genuinely too large for `TransferService`.

If in doubt, keep the logic in the module Service.

---

## 9. Naming

All source-code identifiers use English.

Portuguese may be used for user-facing text, Portuguese documentation, and useful comments.

### 9.1 Java

- Classes: PascalCase — `ExpenseService`, `CreditCard`
- Methods / variables: camelCase — `createExpense()`, `dueDate`
- Constants: UPPER_SNAKE_CASE — `DEFAULT_PAGE_SIZE`
- Packages: lowercase plural — `br.com.financialcontrol.expenses`

### 9.2 No Delphi prefixes

Do not use `m`, `f`, `F`, `g`, `T`, `I`, `cg`, `cf`.

Bad: `TExpense`, `IExpenseService`, `mAmount`  
Good: `Expense`, `ExpenseService`, `amount`

### 9.3 Interfaces

Do not prefix interfaces with `I`.

Do not create `ExpenseService` + `IExpenseService` + `ExpenseServiceImpl` when only one implementation exists.

Spring Data repositories remain framework interfaces. That is not “an interface for every class”.

### 9.4 Enums

```text
public enum ExpenseStatus {
    OPEN,
    PARTIALLY_PAID,
    PAID,
    CANCELLED,
    REFUNDED
}
```

API enums are strings (`"PAID"`), never ordinals (`2`).

### 9.5 Booleans

Java: `isActive`, `isClosed`, `hasBalance`, `canTransfer`

PostgreSQL column: `active`

Do not alternate `active` and `is_active` for the same concept.

### 9.6 Collections

Use plural nouns: `expenses`, `accounts`.

Avoid `expenseList`, `accountArray`.

### 9.7 Avoid unclear abbreviations and generic names

Avoid: `acct`, `amt`, `repo`, `svc`, `data`, `info`, `helper`, `manager`, `utils` when a precise name exists.

---

## 10. DTOs

Every HTTP boundary uses DTOs.

Never expose JPA entities as the public API contract.

Typical pair:

```text
CreateExpenseRequest
ExpenseResponse
```

Do not create duplicates that represent the same contract:

```text
ExpenseDto
ExpenseRequest
ExpenseResponse
ExpenseView
ExpenseResult
```

Create separate types only when the contract actually differs.

DTOs must not contain business rules.

---

## 11. Mappers

Do not introduce MapStruct in Phase 1 merely by convention.

Manual mapping is acceptable when small and clear.

Do not create `ExpenseMapper`, `AccountMapper`, or `CreditCardMapper` automatically for every entity.

Create a dedicated mapper only when the transformation is large enough to be a real responsibility.

Mappers must not contain business rules.

---

## 12. Interfaces and DAOs

Do not create interfaces only to make mocks easier. Mockito can mock concrete classes.

Interfaces are appropriate when:

- multiple real implementations exist;
- an external boundary must be abstracted;
- a genuinely replaceable strategy exists;
- a contract must exist independently of the implementation.

Do not create a DAO layer over Spring Data.

---

## 13. Validation

- Frontend: UX and immediate feedback.
- API boundary: Bean Validation on request DTOs.
- Domain / service: business invariants.

Frontend validation is never sufficient for financial rules.

---

## 14. Financial values

- Java: `BigDecimal`
- PostgreSQL: `NUMERIC(19,2)`
- Never `float`, `double`, `REAL`, or `DOUBLE PRECISION`
- Never `NUMERIC(19,4)` for V1 BRL monetary amounts
- Official rounding: `RoundingMode.HALF_UP`
- Normalize monetary values to scale 2 when applicable
- No Service may choose a different rounding mode
- Installments: the last installment absorbs residual cents
- Percentages stored as fractions: `5.25%` = `0.0525`  
  `100.00 × 0.0525 = 5.25`

---

## 15. Dates, times, and timezone

Persistence of absolute moments:

- Java: `Instant`
- PostgreSQL: `TIMESTAMP WITH TIME ZONE`
- stored in UTC

Financial calendar:

- timezone `America/Sao_Paulo`
- “today”, due dates, invoice closing, overdue, cycles
- Java: `LocalDate`
- PostgreSQL: `DATE`

The frontend must not use the browser timezone to decide financial rules.

---

## 16. Identifiers

Official strategy: **UUID v7 generated by the application**.

The database stores `UUID` and does not generate the identifier.

Do not mix:

- application-generated UUID;
- database `DEFAULT`;
- `uuid_generate_v4()`;
- `@GeneratedValue`;

without an explicit documented change to this decision.

---

## 17. Database

PostgreSQL 18 is the official database.

Flyway is the only schema owner.

Official Hibernate setting:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Never use `update` or `create` as the schema source.

### 17.1 Naming

- Tables: plural snake_case — `accounts`, `expenses`, `credit_cards`, `financial_goals`
- Columns: snake_case — `id`, `account_id`, `due_date`, `created_at`
- Primary key: `id`
- Foreign keys: `<singular>_id` — `account_id`, `credit_card_id`
- Boolean state: `active`
- Do not prefix every column with a table abbreviation (`acc_id`, `tra_amount`)

### 17.2 Migrations

Location:

```text
backend/src/main/resources/db/migration/
```

Names use the plural table:

```text
V1__create_accounts.sql
V2__create_credit_cards.sql
V3__create_financial_goals.sql
```

Do not alternate `create_account`, `create_accounts`, and `initial_schema` without an explicit reason.

Never modify an already-applied shared/production migration. Create a new one.

### 17.3 Constraints

Use `NOT NULL`, `UNIQUE`, `FOREIGN KEY`, and simple `CHECK` constraints.

Application rules and database constraints complement each other.

Do not use `ON DELETE CASCADE` for financial relationships by default.

Do not implement generic soft delete (`deleted_at` on every table). Financial records use `CANCELLED` / `REFUNDED` / `active`. Income reversal returns the record to `EXPECTED`; it does not use `CANCELLED` or `REFUNDED`.

Ownership: every financial table has `user_id NOT NULL`. Relationships that must belong to the same user use composite FKs `(referenced_id, user_id) → parent (id, user_id)`, with `UNIQUE (id, user_id)` on the parent. JPA maps simple `@JoinColumn` on the id; composite FKs live in Flyway. Details: `docs/23-modelo-de-dados.md` sections 264–266.

`invoice_id` belongs to `expense_installments`, never to `expenses`.

Invoice `total_amount` / `paid_amount` / `remaining_amount` are derived, not columns.

Do not add `payments.type` enum/CHECK, installment-edit compensation logic, or invoice-payment allocation columns until `AGENTS.md` §28.3 / `docs/23` §269 is decided. Do not write a preparatory migration to guess those answers.

### 17.4 Other database rules

- JSONB only for genuinely semi-structured data, not core financial modeling
- No stored procedures or triggers for ordinary business workflows
- Indexes from real query patterns, not on every column
- Audit columns: `created_at`, `updated_at`; `created_by` / `updated_by` only when identity is meaningful

Details: `docs/23-modelo-de-dados.md` and `.cursor/rules/database.mdc` (operational reminders).

---

## 18. Transactions

Use `@Transactional` when multiple persistence operations must succeed or fail together.

Example: a transfer must debit the source, credit the destination, and record the transfer atomically.

Do not annotate every method indiscriminately.

Do not place transaction boundaries in repositories merely by convention.

---

## 19. User isolation

Every financial operation must respect the authenticated user from the security context.

Never trust a `userId` sent by the frontend as the owner of the resource.

Incorrect:

```text
GET /api/v1/expenses?userId=123
```

accepted as the owner.

Correct:

```text
GET /api/v1/expenses
```

The backend determines the user from the authenticated context.

Queries and persistence operations must filter by that user.

---

## 20. API

Base path: `/api/v1`

Resources: plural nouns matching the real domain:

```text
/accounts
/expenses
/incomes
/transfers
/payments
/credit-cards
/invoices
/financial-goals
```

HTTP methods:

- `GET` — read
- `POST` — creation or named business action
- `PUT` — full replacement when applicable
- `PATCH` — partial update when applicable

Do not create an endpoint only because a verb exists.

Named business actions:

```text
POST /api/v1/expenses/{id}/pay
POST /api/v1/expenses/{id}/cancel
POST /api/v1/expenses/{id}/refund
POST /api/v1/incomes/{id}/reverse
POST /api/v1/payments/{id}/reverse   (future; not Phase 7)
```

Default successful mutation response: `200 OK` with the updated resource, or `201 Created` for creation.

### 20.1 DELETE

Do not use `DELETE` as the default operation for financial data.

Prefer explicit cancel/reverse actions.

`DELETE` may exist only for a non-financial resource with an explicit rule authorizing physical removal.

### 20.2 Pagination

Do not expose Spring Data `Page`.

External contract:

```text
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 150,
  "totalPages": 8
}
```

Do not use `content` or `totalElements` in the public API.

Do not paginate inherently tiny collections.

### 20.3 Filtering and sorting

Use query parameters:

```text
GET /api/v1/expenses?status=OPEN&startDate=2026-08-01&endDate=2026-08-31
GET /api/v1/expenses?sort=dueDate,asc
```

### 20.4 Errors

The official error contract is defined in `docs/25-api.md`.

Do not replace it with RFC 7807 / Problem Details in this phase.

Do not create a second parallel format.

Example:

```text
{
  "timestamp": "2026-08-12T14:00:00Z",
  "status": 400,
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Não é possível realizar o pagamento.",
  "path": "/api/v1/credit-card-invoices/..."
}
```

Validation errors may include `fields`.

---

## 21. Exceptions

Use a small set of meaningful exception types, for example:

- not found;
- business rule violation;
- conflict;
- invalid request.

Do not create `ExpenseNotFoundException`, `AccountNotFoundException`, and one class per resource.

Use a centralized `@RestControllerAdvice`. Do not put business logic in the exception handler.

---

## 22. Frontend — Angular

The frontend is an independent client. It communicates only through the HTTP API.

Official version: Angular 22.x, TypeScript strict, standalone components.

### 22.1 Feature organization

Organize by real features. Do not automatically create four directories per feature.

If a feature only has:

```text
expenses-page.ts
expenses-page.html
expenses.service.ts
```

do not create empty `components/`, `pages/`, `services/`, and `models/` folders.

Grow the structure when there is a real need.

### 22.2 Naming

```text
ExpenseListComponent
ExpenseService
expense.service.ts
expenses.routes.ts
```

No `I` prefix on TypeScript interfaces.

### 22.3 State

Start with component state, signals when useful, service HTTP state, and route state.

Do not add NgRx in V1.

Do not create `ExpenseStateService` merely to pass a value between two components.

Do not convert every variable into a signal.

Use RxJS where Angular APIs already expose observables.

### 22.4 HTTP

HTTP belongs in feature services, not in components.

Do not hard-code `http://localhost:8080` inside feature code.

Frontend models represent the JSON API contract, not Java/JPA types.

### 22.5 Forms and money display

Typed reactive forms for complex forms.

Frontend may format `1234.5` as `R$ 1.234,50`. It must not treat formatting as financial calculation.

### 22.6 Do not create

- `BaseComponent`
- `GenericCrudService`
- premature design system
- global state by default
- a StateService for every feature

Accessibility is part of frontend correctness.

The frontend must not use the browser timezone to decide financial rules.

---

## 23. Testing

Tests exist to provide confidence, not to maximize count or coverage.

- Pure calculations (installments, rounding, invoice cycle): unit tests
- Persistence, transactions, PostgreSQL behavior: integration tests with Testcontainers
- HTTP contract: API tests
- Important user journeys: few E2E tests later (Playwright)

Do not use H2 as a silent substitute when PostgreSQL behavior matters.

Do not create Factory/Builder for every entity.

Do not create concurrency tests before real concurrent behavior exists.

Do not test trivial getters, DTOs, or framework guarantees (`save()` itself, `BigDecimal` arithmetic).

Financial rules, user isolation, rounding, transfers, invoice payments, and authorization deserve real tests.

Test names describe behavior:

```text
shouldRejectPaymentWhenAmountExceedsAccountBalance()
shouldCancelOpenExpense()
shouldIgnoreClientSuppliedUserId()
```

Details: `docs/27-testes.md`.

---

## 24. Comments, utilities, and scope

Code should be self-explanatory. Comments explain why, constraints, and non-obvious decisions.

Do not create `ExpenseUtils`, `GeneralUtils`, `Helper`, or `CommonUtils` as a dumping ground.

A task should modify only what is necessary. Avoid unrelated refactoring, formatting-only churn, and speculative architecture.

---

## 25. Anti-patterns — do not create automatically

- UseCase per CRUD operation
- Interface + implementation for every class
- DAO over Spring Data
- Mapper for every entity
- MapStruct without a demonstrated need
- Generic `common/`, `utils/`, `helpers/`, `managers/`
- Angular `*StateService` for every feature
- NgRx in V1
- `BaseComponent` / `GenericCrudService`
- Domain Events
- Specification
- Strategy
- Hexagonal Architecture
- Clean Architecture through artificial layers
- Generic `Transaction` / `Card` / `Goal` modules that do not match the official model
- Controller → Repository shortcut
- RFC 7807 as a second error format
- Spring Data `Page` leaked to the API
- `DELETE` as the default for financial records
- `NUMERIC(19,4)` for BRL money
- `current_balance` as an independent source of truth

If one of these appears necessary, first identify the concrete problem it solves. If there is no concrete problem, do not apply it.

---

## 26. Decision process

Before creating a class, interface, service, mapper, component, folder, or abstraction, ask:

> Does a real responsibility justify this **now**?

If not, do not create it.

Before applying an architectural pattern, ask:

> What concrete project problem does this pattern solve?

If none, do not apply it.

When two solutions are equivalent, prefer the simpler one.

When choosing between a project-specific convention and an established Java / Spring / Angular / PostgreSQL convention, prefer the ecosystem convention unless a project document says otherwise.

The project must evolve through real requirements.

> Do not build complexity because it might be needed. Build clarity so that complexity can be added safely when it actually becomes necessary.
