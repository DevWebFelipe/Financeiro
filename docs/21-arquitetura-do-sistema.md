# Arquitetura do Sistema — Financial Control

## 0. Hierarquia e versões oficiais

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

- Java 25 LTS
- Spring Boot 4.1.x
- Angular 22.x
- PostgreSQL 18
- Pacote: `br.com.financialcontrol`
- API: `/api/v1`


## 1. Objetivo

Este documento define a arquitetura técnica da aplicação Financial Control.

A arquitetura deve priorizar:

- simplicidade;
- organização;
- separação de responsabilidades;
- testabilidade;
- segurança;
- manutenção;
- evolução incremental;
- aprendizado do desenvolvedor.


# 2. Princípio principal

A aplicação deve ser construída como um sistema profissional, porém sem complexidade desnecessária para a V1.


# 3. Arquitetura geral

O sistema será dividido em:

- frontend;
- backend;
- banco de dados.


Estrutura lógica:

Frontend Angular
        |
        | HTTP/JSON
        v
Backend Spring Boot
        |
        | JDBC/JPA
        v
PostgreSQL


# 4. Frontend

Tecnologia principal:

Angular


# 5. Backend

Tecnologia principal:

Java


Framework:

Spring Boot


# 6. Banco de dados

Banco:

PostgreSQL


# 7. Containers

Docker será utilizado para infraestrutura local.


# 8. Docker

A aplicação deve possuir:

docker-compose.yml


ou:

compose.yaml


para executar pelo menos:

- PostgreSQL.


Na V1 o backend e frontend podem ser executados diretamente durante o desenvolvimento.


# 9. Infraestrutura

O projeto deve permitir futuramente executar:

Frontend

Backend

PostgreSQL


todos via Docker.


Não é obrigatório implementar isso completamente na primeira etapa.


# 10. Java

Utilizar Java **25 LTS**.


# 11. Spring Boot

Utilizar Spring Boot **4.1.x**.


# 12. Dependências

As dependências devem ser adicionadas somente quando necessárias.


Evitar bibliotecas desnecessárias.


# 13. Backend

O backend será uma API REST.


# 14. API

Formato:

JSON


# 15. REST

Utilizar padrões REST consistentes.


Exemplo:

GET /api/v1/accounts

POST /api/v1/accounts

GET /api/v1/accounts/{id}

PUT /api/v1/accounts/{id}

POST /api/v1/expenses/{id}/pay

POST /api/v1/expenses/{id}/cancel

POST /api/v1/expenses/{id}/refund

Não utilizar `DELETE` como operação padrão para dados financeiros.


# 16. Versionamento

A API deve utilizar:

/api/v1


# 17. Controller

Controllers devem ser responsáveis por:

- receber requisições;
- validar entrada básica;
- chamar o Service do módulo;
- retornar respostas HTTP.

Fluxo padrão:

```text
Controller → Service → Repository
```

O Controller **não** acessa o Repository diretamente. Leituras simples também passam pelo Service do módulo. O Service pode ser pequeno.


# 18. Controller

Controllers não devem conter:

- regras financeiras;
- cálculos complexos;
- SQL;
- acesso a Repository;
- lógica de negócio extensa.


# 19. Service

O padrão é **um Service por módulo**, não um UseCase por operação CRUD.

Exemplo:

ExpenseService


deve controlar regras relacionadas às despesas.

Não criar `CreateExpenseUseCase`, `GetExpenseUseCase`, `ListExpenseUseCase` nem equivalentes.

Uma classe `*UseCase` só é aceitável quando a operação for um caso de negócio nomeado, atômico e suficientemente complexo (ex.: `TransferMoneyUseCase`, se a orquestração justificar).


# 20. Service

Exemplos:

AccountService

ExpenseService

IncomeService

TransferService

PaymentService

CreditCardService

CreditCardInvoiceService

FinancialGoalService


# 21. Repository

Repositories são responsáveis pelo acesso aos dados.

Queries de dados financeiros devem respeitar o usuário autenticado obtido do contexto de segurança.

Não aceitar `userId` do cliente como autoridade sobre a propriedade do recurso.


# 22. Repository

Repositories não devem conter regras financeiras.


# 23. Entity

Entities representam dados persistidos no banco.


# 24. DTO

Toda fronteira HTTP deve utilizar DTOs.

A API não deve expor diretamente entidades JPA como contrato público.


# 25. Request DTO

Exemplo:

CreateExpenseRequest


# 26. Response DTO

Exemplo:

ExpenseResponse

Não criar DTOs duplicados sem diferença real de contrato (`ExpenseDto`, `ExpenseRequest`, `ExpenseResponse`, `ExpenseView` e `ExpenseResult` todos iguais).


# 27. Mapper

Mapeamento manual é aceitável quando for pequeno e claro.

Não introduzir MapStruct na Fase 1 apenas por convenção.

Não criar `ExpenseMapper`, `AccountMapper` ou equivalente automaticamente para cada entidade.

Criar um mapper separado somente quando a transformação justificar uma responsabilidade própria.


# 28. Regra

Não misturar:

Entity

DTO

Request

Response


na mesma classe sem necessidade.


# 29. Pacotes

Package principal oficial:

```text
br.com.financialcontrol
```

Organização inicial deve facilitar separação por domínio.

A estrutura definitiva de pacotes será definida na Fase 1.

Exemplo conceitual (não prescritivo de pastas vazias; não criar módulos só para antecipar o futuro):

```text
br.com.financialcontrol
├── auth
├── users
├── security
├── config
├── health
├── accounts
├── expenses
├── incomes
├── transfers
├── balance_adjustments
├── payments
├── credit_cards
├── credit_card_invoices
├── financial_goals
├── payables
├── receivables
├── projections
├── dashboard
└── reports
```

Pacote da Fase 18 (`projections`): visão derivada; **sem** entidade JPA; **sem** tabela (`docs/24` §19.10).

Pacote da Fase 19 (`dashboard`): visão derivada; **sem** entidade JPA; **sem** tabela (`docs/24` §19.11).

Pacote da Fase 20 (`reports`): visão derivada; **sem** entidade JPA; **sem** tabela (`docs/24` §19.12). **CONCLUÍDA E APROVADA**. OpenPDF 3.0.5.

Pacotes da Fase 3:

- `auth` — cadastro e login (`AuthController` → `AuthService` → `UserRepository`);
- `users` — perfil e senha (`UserController` → `UserService` → `UserRepository`);
- `security` — Spring Security, JWT HS256, `AuthenticatedUser`, filtro Bearer;
- `config` — formato de erro oficial, OpenAPI, Jackson.

Pacotes da Fase 4:

- `accounts` — contas financeiras (`AccountController` → `AccountService` → `AccountRepository`).

Pacotes da Fase 14:

- `transfers` — transferências entre contas (`TransferController` → `TransferService` → `TransferRepository`);
- `balance_adjustments` — Acerto de Saldos (`BalanceAdjustmentController` → `BalanceAdjustmentService` → `AccountBalanceAdjustmentRepository`).

Pacotes da Fase 15 (`CONCLUÍDA E APROVADA`):

- `financial_goals` — metas financeiras (`FinancialGoalController` → `FinancialGoalService` → `FinancialGoalRepository`, `GoalContributionRepository`, `GoalRedemptionRepository`).
- Contribuição e resgate: `@Transactional` + lock pessimista da conta vinculada (`AccountService.requireActiveOwnedAccountForUpdate`) e da meta (`findByIdAndUserIdForUpdate`).
- Sem ledger genérico, sem entidade `Transaction`, sem `DELETE` nem reverse de contribuição/resgate.

Contrato: `docs/24` §19.6.

Pacotes da Fase 16 (`CONCLUÍDA E APROVADA`):

- `payables` — visão consolidada de contas a pagar (`PayablesController` → `PayablesService` → consultas de leitura sobre `expenses` / `expense_installments` / `credit_card_invoices`).
- **Sem** entidade JPA `Payable`, **sem** tabela `payables`, **sem** remaining persistido.
- Remaining e totais derivados das fórmulas já oficiais (RN231, `docs/23` §§196–197). Contrato: `docs/24` §19.7.

Pacotes da Fase 17 — Parte 1 (`CONCLUÍDA E APROVADA`):

- `receivables` — visão de leitura de contas a receber (`ReceivablesController` → `ReceivablesService` → consultas em `IncomeRepository`).
- **Sem** entidade JPA `Receivable`, **sem** tabela `receivables`, **sem** remaining persistido.
- Vocabulário de data: `expectedDate` (sem alias `dueDate`). Filtros, ordenação, paginação e resumo **no banco** (não copiar o filtro em memória de payables). Contrato: `docs/24` §19.8.
- A Fase 17 está **`CONCLUÍDA E APROVADA`** (Parte 1 + Parte 2). Pacote `receivables`: visão de leitura; **sem** tabela `receivables`. Domínio `incomes`: tabela `income_movements` (V30); movimentações ACCRUAL/RECEIPT; D73–D94 **fechadas** e **implementadas**. Escrita de responsável em Income (**D89** / RN306) **implementada**. **Não** criar `IncomeMovementService` nem módulo `transactions`.

Pacotes da Fase 18 (`docs/24` §19.10 — **CONCLUÍDA E APROVADA**):

- Pacote `projections` — visão derivada de fluxo de caixa (`ProjectionController` → `ProjectionService` → `ProjectionCalculator` → consultas/repositórios).
- **Sem** entidade JPA `Projection`, **sem** tabela `projections`, **sem** migration.

Pacotes da Fase 19 (`docs/24` §19.11 — **CONCLUÍDA E APROVADA**):

- Pacote `dashboard` — visão derivada consolidada (`DashboardController` → `DashboardService` → `ProjectionService` / `AccountService` / `PayablesService` / `ReceivablesService` / `CreditCardService`).
- **Sem** entidade JPA `Dashboard`, **sem** tabela `dashboard`, **sem** migration.
- Consome remaining e saldo oficiais; não cria fonte de verdade paralela.

Pacotes da Fase 20 (`docs/24` §19.12 — **CONCLUÍDA E APROVADA**):

- Pacote `reports` — visões derivadas de relatórios (`ReportsController` → `ReportsService` → `ExpenseService` / `IncomeService` / `PayablesService` / `ProjectionService` / `AccountService` / serviços de cartão e fatura).
- **Sem** entidade JPA `Report`, **sem** tabela `reports`, **sem** migration.
- **Não** reimplementar RN231, RN240, remaining de receita, `docs/23` §197 nem o `ProjectionService`.
- PDF: OpenPDF 3.0.5.
- Paths `GET /api/v1/reports/expenses-by-category` (e equivalentes by-card / by-responsible / income-by-category) estão **SUPERADOS** (`docs/25` §76).
- Auditoria final: **APROVADA COM RESSALVAS** (ressalva exclusivamente documental/status, corrigida na etapa de fechamento).

Pacotes da Fase 5:

- `categories` — categorias (`CategoryController` → `CategoryService` → `CategoryRepository`).

Pacotes de domínio no plural, alinhados às tabelas e aos recursos HTTP.

Não criar pacote `common` genérico sem responsabilidade compartilhada real.

Não criar módulo genérico `transactions` para agrupar receitas, despesas, transferências e pagamentos.

O conceito de movimentação financeira (receita recebida, despesa efetivada, transferência, ajuste de saldo) é de cálculo de saldo. Não implica criar entidade genérica `Transaction`.


# 29.1 Saldo derivado

O saldo de uma conta é derivado das movimentações financeiras efetivas, a partir do saldo inicial.

Não utilizar `current_balance` como fonte de verdade.

Conceitualmente:

```text
Conta
   │
   └── Movimentações financeiras
          ├── Receita recebida
          ├── Despesa efetivada
          ├── Transferência (ACTIVE)
          └── Acerto de Saldos / BALANCE_ADJUSTMENT (ACTIVE)
```

Isso não autoriza criar agora uma entidade genérica `Transaction`.

A implementação concreta ocorre por domínio (`incomes`, `expenses`, `transfers`, `balance_adjustments`, …), sem ledger paralelo. A Parte 2 de receitas usa fatos `income_movements` **dentro** do domínio `incomes` — não um `Transaction` genérico (`docs/24` §19.9.14). **Implementado** (V30).

Na Fase 6, receita `RECEIVED` passava a participar positivamente do saldo da conta informada (`+ amount`). Receita `EXPECTED` ou `CANCELLED` não participa do saldo efetivo.

**Fase 17 Parte 2 (implementada):** o efeito no saldo é a soma dos RECEIPT `ACTIVE` por `movement_date` (**D83** / RN240). Acréscimos (ACCRUAL) não movimentam conta. Estorno de RECEIPT mantém RN200 (**D80-A**). RN010A considera qualquer RECEIPT histórico (ACTIVE ou REVERSED).

A partir da Fase 7, pagamentos de despesa cuja despesa não está `CANCELLED` nem `REFUNDED` participam negativamente (`− payments.amount`). Criação de despesa não altera saldo. Estorno de despesa (`REFUNDED`) faz esses pagamentos deixarem de ser subtraídos; as linhas de `payments` permanecem.

O estorno de receita desfaz exatamente a movimentação (`− amount`), devolve a duplicata a `EXPECTED` (ativa, não cancelada), limpa `account_id` e `received_date`, e não é bloqueado se o saldo ficar negativo. Essa exceção **não** se aplica a despesa.

O cancelamento (`EXPECTED` → `CANCELLED` em receita; `OPEN` → `CANCELLED` em despesa) não altera saldo. Cancelamento e estorno não são a mesma operação.

**Acerto de Saldos** (`BALANCE_ADJUSTMENT` / tabela `account_balance_adjustments`) está implementado na Fase 14 (`docs/24` §19.5). Status: `CONCLUÍDA E APROVADA`. A arquitetura não usa ledger genérico.


# 29.2 Saldo em datas e períodos

O modelo deve permitir obter:

- saldo inicial;
- saldo em uma data específica (as-of-date — capacidade interna da Fase 14);
- saldo anterior a um período;
- movimentações de um período;
- movimentação líquida;
- saldo final de um período;
- saldo atual.

Extrato unificado completo / `GET /accounts/{id}/statement` e dashboard de apresentação **fora** da Fase 14.


# 30. Organização futura

Manter monólito modular com tendência a pacotes por domínio.

Não criar dezenas de módulos vazios apenas para "preparar o futuro".


# 31. V1

Não criar dezenas de módulos vazios apenas para "preparar o futuro".


# 32. Banco

PostgreSQL será o banco oficial.


# 33. ORM

Utilizar:

Spring Data JPA


com:

Hibernate


# 34. SQL

SQL deve ser utilizado quando necessário.


Não utilizar JPA para operações que claramente ficam melhores com SQL específico.


# 35. Regra

Não transformar consultas simples em SQL nativo sem necessidade.


# 36. Migration

O banco deve ser versionado através de migrations.


# 37. Ferramenta

Utilizar:

Flyway


# 38. Migration

Nunca depender exclusivamente de:

hibernate ddl-auto=create

ou:

hibernate ddl-auto=update


para estruturar o banco.


# 39. Produção

O schema oficial deve ser criado através das migrations.


# 40. Desenvolvimento

O Hibernate pode validar o schema.


Configuração oficial:

spring.jpa.hibernate.ddl-auto=validate

Nunca utilizar `update` ou `create` como fonte do schema.


# 41. Migration inicial

Deve criar as tabelas fundamentais da V1.


# 42. Migration

Cada alteração estrutural deve criar nova migration.


Exemplo:

V1__create_accounts.sql

V2__create_credit_cards.sql

V3__create_financial_goals.sql

O nome da migration deve usar a tabela no plural. Não alternar entre `create_account`, `create_accounts` e `initial_schema` sem razão explícita.


# 43. Banco

Utilizar UUID como identificador principal.


# 44. UUID

Estratégia oficial: UUID v7 gerado pela aplicação.

O banco armazena o identificador como `UUID`.

Não misturar geração na aplicação com default de banco, `uuid_generate_v4()` ou `@GeneratedValue`.


# 45. Datas

Utilizar tipos Java apropriados.


Para datas sem horário:

LocalDate


Para instantes absolutos:

Instant


Timestamps são persistidos em UTC (`TIMESTAMPTZ`).

Regras de calendário financeiro ("hoje", vencimento, fechamento, atraso, ciclos) utilizam `America/Sao_Paulo`.

O ciclo da compra no cartão usa a **data da compra** e o `closing_day`, não o fato de existir fatura OPEN.

O frontend não deve usar o timezone do navegador para decidir regras financeiras.


# 45A. Scheduler (Fase 9)

A V1 admite **Spring `@Scheduled`** exclusivamente para o fechamento/abertura de faturas (RN096A): idempotente; não bloqueia o usuário.

Não introduzir Redis, Kafka, fila nem plataforma genérica de jobs. Não criar sistema genérico de auditoria.


# 46. Dinheiro

Nunca utilizar:

double

float


para valores monetários.


# 47. Dinheiro

Utilizar:

BigDecimal


# 48. Precisão

Valores monetários devem utilizar NUMERIC(19,2) no PostgreSQL e BigDecimal no Java.


# 49. Transações

Operações financeiras devem utilizar:

@Transactional


quando houver múltiplas alterações dependentes.


# 50. Exemplo

Transferência:

debitar conta origem;

creditar conta destino;

registrar transferência.


Tudo deve ocorrer na mesma transação.


# 51. Rollback

Se qualquer etapa crítica falhar:

toda a operação deve ser revertida.


# 52. Concorrência

Operações financeiras devem considerar concorrência.

Leitura de saldo derivado (`GET /accounts/{id}/balance`) **não** exige lock pessimista da conta. Locks pessimistas aplicam-se às operações de escrita financeira (despesa, parcela, payment, transferência, acerto de saldos, **contribuição e resgate de meta** — Fase 15), conforme as RNs de pagamento, a Fase 8 (RN244 / RN240), a Fase 14 (RN258) e a Fase 15 (RN277). Não existe `current_balance` persistido nem ledger genérico.


# 53. Pagamentos

O sistema deve impedir:

dois pagamentos simultâneos;

ultrapassagem do valor devido.


# 54. Segurança

A aplicação deve possuir autenticação.


# 55. V1

Utilizar:

Spring Security


# 56. Autenticação

Utilizar JWT Access Token (HS256, 30 minutos).

Refresh Token não foi implementado na Fase 3.


# 57. JWT

O token identifica o usuário no claim `sub` (UUID).

Também possui `iat` e `exp`.

Senhas: Argon2id.


# 58. Autorização

Toda operação financeira deve validar o usuário autenticado.


# 59. Regra crítica

Nunca confiar em:

userId


enviado pelo frontend.


# 60. User ID

O backend deve obter o usuário a partir do contexto de segurança.


# 61. Isolamento

Toda consulta deve aplicar filtro de usuário quando necessário.


# 62. Exemplo

Errado:

findById(expenseId)


Correto conceitualmente:

findByIdAndUserId(expenseId, authenticatedUserId)


# 63. Regra

Não permitir IDOR.


# 64. Validação

Utilizar:

Jakarta Bean Validation


# 65. Exemplos

@NotNull

@NotBlank

@Positive

@Size

@Email


# 66. Validação

Validar no backend mesmo que o frontend já valide.


# 67. Erros

A API deve possuir formato padronizado de erro.


# 68. Exemplo

HTTP 400

```json
{
  "timestamp": "2026-08-12T14:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Dados inválidos.",
  "path": "/api/v1/auth/register",
  "fields": {
    "password": "A senha deve ter entre 8 e 128 caracteres."
  }
}
```

Contrato oficial: `docs/25-api.md`. Não usar `details` nem RFC 7807.


# 69. Erro de autenticação

HTTP:

401


# 70. Erro de autorização

HTTP:

403


# 71. Recurso inexistente

HTTP:

404


# 72. Regra de negócio

HTTP:

422


quando apropriado.


# 73. Conflito

HTTP:

409


quando houver conflito de estado.


# 74. Erro inesperado

HTTP:

500


Não expor stack trace ao cliente.


# 75. Logging

Utilizar logging estruturado e adequado.


# 76. Logs

Não registrar:

- senha;
- token;
- dados sensíveis;
- informações financeiras desnecessárias.


# 77. Swagger

A API deve possuir documentação OpenAPI.


# 78. Ferramenta

Utilizar:

springdoc-openapi


# 79. Swagger

A documentação deve permitir:

- visualizar endpoints;
- visualizar DTOs;
- visualizar respostas;
- testar endpoints autenticados quando possível.


# 80. Frontend

Angular deve consumir somente a API.


# 81. Regra

Angular não deve acessar PostgreSQL diretamente.


# 82. Regra

Nenhuma regra financeira crítica deve existir exclusivamente no Angular.


# 83. Angular

Utilizar Angular **22.x**.


# 84. Angular

Preferir:

Standalone Components


# 85. Angular

Utilizar:

TypeScript


# 86. Angular

Utilizar:

Reactive Forms


quando houver formulários complexos.


# 87. Angular

Utilizar:

HttpClient


para comunicação com backend.


# 88. Angular

Centralizar comunicação com API em:

services


# 89. Exemplo

account.service.ts


expense.service.ts


invoice.service.ts


# 90. Angular

Componentes devem evitar chamadas HTTP diretamente quando isso puder ser encapsulado em services.


# 91. Estado

A V1 não precisa utilizar uma biblioteca global complexa de estado.


# 92. Estado

Preferir inicialmente:

signals;

services;


e recursos nativos do Angular.


# 93. Regra

Não instalar NgRx apenas por antecipação.


# 94. Formulários

Formulários devem possuir:

- validação;
- mensagens de erro;
- estados de loading;
- tratamento de erro.


# 95. UI

A interface deve priorizar:

- clareza;
- usabilidade;
- responsividade;
- simplicidade.


# 96. Design

Não é necessário criar um design extremamente sofisticado na V1.


# 96A. Identidade visual (Fase 21 — Bloco B1)

Tema inicial: **claro**.

Paleta base aprovada:

```text
#11161C
#2F3B49
#6E7F90
#CFD8E3
#F6F8FB
```

Fonte principal: **Inter**, armazenada localmente no frontend. Pesos: 400, 500, 600 e 700.

Receitas: verdes suaves. Despesas: vermelhos suaves. Evitar saturação excessiva.

Tokens visuais, reset e base styles: `frontend/src/styles/`. Não criar uma biblioteca de componentes neste bloco.

Angular Material, CDK e ECharts permanecem fora do Bloco B1.


# 96B. HTTP e erros (Fase 21 — Bloco B2)

Configuração da API: `frontend/src/app/core/config/`.

- desenvolvimento (`ng serve`): `http://localhost:8080/api/v1`
- produção (`ng build`): `/api/v1` (same-origin)

HttpClient e interceptors funcionais: `frontend/src/app/core/http/`. Sem wrapper de `HttpClient`. O interceptor de Authorization / sessão entra no B3; B2 não acessa `sessionStorage` nem cria `AuthService`.

Contrato de erro: `ApiError` alinhado a `docs/25-api.md`. Normalização segura: `frontend/src/app/core/errors/`. Códigos internos de transporte (`HTTP_TRANSPORT_ERROR`, `HTTP_UNPARSEABLE_RESPONSE`) não são códigos semânticos do backend.

401 é identificado, a sessão é invalidada de forma idempotente e a navegação vai para `/login`. 403, 5xx e erros de rede **não** encerram sessão.


# 96C. Autenticação e sessão (Fase 21 — Bloco B3)

Sessão em `frontend/src/app/core/auth/`. JWT somente em `sessionStorage` (`fc.auth.accessToken`), encapsulado. Sem `localStorage`, sem parser JWT, sem refresh token.

Estados: `loading` | `authenticated` | `unauthenticated`. Inicialização (`provideAppInitializer`): sem token → `unauthenticated`; com token → `GET /users/me`; 200 → `authenticated`; 401 → limpa sessão; 5xx/rede → **não** apaga o token.

Interceptor `Authorization: Bearer` quando há token. AuthGuard aguarda `loading`, permite `authenticated`, redireciona `unauthenticated` para `/login` com `returnUrl` interno (sem open redirect).

Registro (`POST /auth/register`) **não** cria sessão. Login (`POST /auth/login`) armazena `accessToken` e busca `/users/me`. Logout é local.


# 96D. App Shell (Fase 21 — Bloco B4)

Casca autenticada em `frontend/src/app/layout/` (`main-layout`, `header`, `navigation`). O Dashboard placeholder fica em `frontend/src/app/features/dashboard/`.

MainLayout:

- Header;
- Sidebar desktop colapsável;
- Drawer mobile de navegação (exceção estrutural do shell; não é o Drawer genérico de filtros/detalhes);
- `RouterOutlet`.

Estado visual local (signals): `sidebarCollapsed`, `mobileNavigationOpen`. Sem persistência. Sem store global. Sem Material/CDK.

Desktop (`min-width: 64rem`): sidebar permanente, colapsável; header e conteúdo à direita. Mobile: sidebar sai do fluxo; o Header abre o Drawer; o conteúdo ocupa a largura disponível. O Drawer fecha por rota, Escape, botão Fechar ou backdrop; o foco entra no Drawer ao abrir e volta ao botão de menu ao fechar.

Entrada autenticada: `/dashboard` (AuthGuard no layout pai). `/` redireciona para `/dashboard`. Login e registro permanecem públicos (`guestGuard`). Logout no Header usa `AuthService`. O usuário exibido vem de `AuthService.user()` — sem novo `GET /users/me`.

A primeira feature financeira filha do shell é `/accounts` (B7), lazy-loaded.


# 96E. Dashboard funcional (Fase 21 — Bloco B5)

A página `/dashboard` consome **somente** `GET /api/v1/dashboard` (Fase 19). Não chama `/accounts`, `/balance`, `/incomes`, `/expenses` nem `/projections` para montar o painel. Não recalcula saldo. Não há seletor de período: o horizonte é o default oficial de 12 meses.

Feature em `frontend/src/app/features/dashboard/`: `DashboardPage` → `DashboardService` → `HttpClient`. Estados: `loading` | `loaded` | `error`. Empty = loaded sem contas nem cartões ativos. Retry refaz o mesmo GET. Falha do endpoint é erro da página (um único contrato consolidado).

A série mensal vem de `projection.months` do próprio dashboard (sem `events`). Visualização: barras CSS + tabela acessível. ECharts permanece a biblioteca oficial da V1, mas **não** foi instalado neste bloco.

Não somar `payables` com `projection.summary.projectedExpense`. `usedLimit` não é tratado como caixa.


# 96F. Shared UI (Fase 21 — Bloco B6)

`frontend/src/app/shared/` concentra UI genuinamente reutilizável. Não é um depósito nem um design system paralelo.

Existe neste bloco:

- `ErrorState` — mensagem amigável + retry opcional; a feature decide o texto; não interpreta `ApiError`;
- `EmptyState` — ausência de dados, distinta de erro; ação opcional via conteúdo projetado;
- pipes de apresentação: `brlCurrency`, `isoDate` (DATE `YYYY-MM-DD` sem timezone de instante), `yearMonth` (`YYYY-MM`).

Não existem neste bloco: `Button` (o `button` nativo já é estilizado em `base.css`), Loading/Skeleton genérico, diretivas, Dialog, Drawer, Toast, tabela genérica, `shared/services`.

Ícones do shell permanecem SVG locais. Formatação de trimestre permanece no Dashboard. Rótulos de tipo de conta (`BANK_ACCOUNT` / `CASH`) também existem na feature Accounts.


# 96G. Contas (Fase 21 — Bloco B7)

A página `/accounts` é lazy-loaded sob o AuthGuard do MainLayout. A navegação passa a incluir Contas somente neste bloco.

Feature em `frontend/src/app/features/accounts/`: `AccountsPage` → `AccountsService` → `HttpClient`. Sem `HttpClient` na página. Sem recálculo de saldo.

Contrato utilizado:

- `GET /api/v1/accounts` — array, sem paginação, ativas e inativas, ordem `createdAt` ASC. Não inclui saldo corrente.
- `GET /api/v1/accounts/{id}/balance` — `totalBalance`, `reservedAmount`, `availableBalance`. O alias legado `balance` não é exibido.
- `POST /api/v1/accounts` — criação (`name`, `type`, `initialBalance` opcional).
- `PUT /api/v1/accounts/{id}` — apenas `name` e `type`.
- `POST /api/v1/accounts/{id}/deactivate` e `POST /api/v1/accounts/{id}/activate`.

Após a listagem, os saldos oficiais são pedidos em paralelo (`forkJoin`). Lista vazia não dispara balance. Falha de qualquer balance é erro da página + retry. Não há endpoint agregado; não há paginação local.

`PUT /accounts/{id}/initial-balance` existe no backend e permanece fora deste bloco. Sem `/accounts/:id`. Sem extrato.

Estados: `loading` | `loaded` | `empty` | `error`. Empty ≠ erro. Retry refaz listagem + balances. Formulário reativo na mesma rota. `VALIDATION_ERROR.fields` vai para o controle; `BUSINESS_RULE_VIOLATION` na desativação usa mensagem contextual (não `message.includes`).


# 96H. Categorias (Fase 21 — Bloco B8)

A página `/categories` é lazy-loaded sob o AuthGuard do MainLayout. A navegação passa a incluir Categorias neste bloco.

Feature em `frontend/src/app/features/categories/`: `CategoriesPage` → `CategoriesService` → `HttpClient`. Sem recálculo de dados; sem campos inventados.

Contrato utilizado:

- `GET /api/v1/categories` — array, sem paginação, ordem `createdAt` ASC. Query opcionais oficiais: `type` (`INCOME` \| `EXPENSE`), `active` (`true` \| `false`).
- `POST /api/v1/categories` — criação (`name`, `type`).
- `PUT /api/v1/categories/{id}` — apenas `name` e `type`.
- `POST /api/v1/categories/{id}/deactivate` — desativação lógica idempotente.

Não existem no backend: `GET /categories/{id}`, `POST .../activate`, `DELETE`. Sem reativação na UI.

Filtros da tela disparam nova listagem com os query params oficiais (não filtragem client-side). Duplicidade `name + type` → **409** `CONFLICT`. Sem `/categories/:id`.

Estados: `loading` | `loaded` | `empty` | `error`. Formulário reativo na mesma rota. Inativas permanecem visíveis; botão Desativar só para ativas.


# 96I. Despesas (Fase 21 — Bloco B9)

A página `/expenses` é lazy-loaded sob o AuthGuard do MainLayout. A navegação passa a incluir Despesas neste bloco.

Feature em `frontend/src/app/features/expenses/`: `ExpensesPage` → `ExpensesService` → `HttpClient`. Apoio cadastral: `CategoriesService` (categorias `EXPENSE` ativas) e `AccountsService` (nomes de contas). Sem recálculo financeiro; sem campos inventados.

Contrato utilizado:

- `GET /api/v1/expenses` — paginação oficial (`items`, `page`, `size`, `totalItems`, `totalPages`; default `page=0`, `size=20`). Filtros opcionais: `startDate`, `endDate`, `status`, `categoryId`, `accountId`, `responsibleType`, `paymentMethod`. Período usa `due_date` das parcelas.
- `GET /api/v1/expenses/{id}` — detalhe (painel na mesma rota).
- `POST /api/v1/expenses` — criação (`OPEN`; sem payment).
- `PUT /api/v1/expenses/{id}` — somente `OPEN`.
- `GET /api/v1/expenses/{id}/installments` — parcelas oficiais.
- `POST /api/v1/expenses/{id}/pay` — pagamento 1/1 (`ACCOUNT`/`NONE`).
- `POST /api/v1/expenses/{expenseId}/installments/{installmentId}/payments` — pagamento N>1.
- `POST /api/v1/expenses/{id}/cancel` — somente `OPEN`.
- `POST /api/v1/expenses/{id}/refund` — `PARTIALLY_PAID`/`PAID`; corpo vazio para `ACCOUNT`/`NONE`; `settlement` para `CREDIT_CARD`.

Fora deste bloco na UI original: adjustments, reverse de payment, rota dedicada `/expenses/:id`. Criação `CREDIT_CARD` passou a existir no bloco C2 (`docs/21` §96M).

Estados: `loading` | `loaded` | `empty` | `error`. Paginação server-side. Filtros disparam nova listagem. Detalhe, pagamento, cancelamento e estorno em painéis na mesma rota. `VALIDATION_ERROR.fields` nos controles; regras por `error.code`.


# 96J. Receitas (Fase 21 — Bloco B10)

A página `/incomes` é lazy-loaded sob o AuthGuard do MainLayout. A navegação passa a incluir Receitas neste bloco.

Feature em `frontend/src/app/features/incomes/`: `IncomesPage` → `IncomesService` → `HttpClient`. Apoio cadastral: `CategoriesService` (categorias `INCOME` ativas) e `AccountsService` (nomes e select de conta no recebimento). Sem recálculo financeiro; sem campos inventados.

Contrato utilizado:

- `GET /api/v1/incomes` — paginação oficial (`items`, `page`, `size`, `totalItems`, `totalPages`; default `page=0`, `size=20`). Filtros opcionais: `startDate`, `endDate`, `status`, `categoryId`, `accountId`. Período filtra `expectedDate`. Ordem `createdAt` ASC.
- `GET /api/v1/incomes/{id}` — detalhe (painel na mesma rota).
- `POST /api/v1/incomes` — criação (`EXPECTED`; `accountId` e `receivedDate` nulos).
- `PUT /api/v1/incomes/{id}` — somente `EXPECTED`.
- `POST /api/v1/incomes/{id}/receipts` — recebimento (`amount`, `date`, `accountId`); resposta é o movimento.
- `GET /api/v1/incomes/{id}/movements` — histórico paginado.
- `POST /api/v1/incomes/{id}/movements/{movementId}/reverse` — estorno de movimento `ACTIVE`.
- `POST /api/v1/incomes/{id}/cancel` — somente `EXPECTED` sem RECEIPT `ACTIVE`.

`dateType` e `overdue` **não** existem em `GET /incomes` (pertencem a `GET /receivables`). A UI de Receitas não inventa atraso nem `dateType`. Sem rota `/incomes/:id`. Sem feature Contas a Receber neste bloco. Acréscimo (`POST /accruals`) fora da UI deste bloco.

Estados: `loading` | `loaded` | `empty` | `error`. Paginação server-side. Filtros disparam nova listagem. Detalhe, recebimento, cancelamento e estorno em painéis na mesma rota. `VALIDATION_ERROR.fields` nos controles; regras por `error.code`.


# 96K. Contas a pagar (Fase 21 — Bloco B11)

A página `/payables` é lazy-loaded sob o AuthGuard do MainLayout. A navegação passa a incluir Contas a pagar neste bloco.

Feature em `frontend/src/app/features/payables/`: `PayablesPage` → `PayablesService` → `HttpClient`. Apoio: `CategoriesService` (categorias `EXPENSE` para nomes) e `AccountsService` (nomes de conta). Visão de leitura; sem CRUD; sem recálculo de remaining/overdue.

Contrato utilizado:

- `GET /api/v1/payables` — único endpoint. Paginação oficial (`items`, `page`, `size`, `totalItems`, `totalPages`; default `page=0`, `size=20`). Totais oficiais do universo filtrado: `totalRemaining`, `totalOriginal`, `totalPaid`.
- Linha: `INSTALLMENT` (parcela ACCOUNT/NONE com remaining > 0) ou `INVOICE` (fatura SCHEDULED/OPEN/CLOSED com remaining > 0).
- Filtros oficiais usados na UI: `startDate`, `endDate`, `year`+`month`, `includeWithoutDueDate`, `status`, `overdue`, `withoutCreditCard`, `categoryId`, `responsibleType`, `search`, `sort`, `direction`.
- `overdue` exibido a partir do booleano oficial (não comparado com a data do navegador).
- Período usa `due_date` da linha.

Não existem `GET /payables/{id}` nem escritas. Sem painel de pagamento nesta visão (ações financeiras permanecem em `/expenses` e, no futuro, faturas). Filtro `creditCardId` omitido na UI até existir feature de cartões. Sem rota `/payables/:id`. Detalhe usa os dados da listagem.

Estados: `loading` | `loaded` | `empty` | `error`. Empty sem ação de criação. Paginação server-side.


# 96L. Cartões (Fase 21 — Bloco C1)

A página `/credit-cards` é lazy-loaded sob o AuthGuard do MainLayout. A navegação passa a incluir Cartões neste bloco. Sem rota `/credit-cards/:id`.

Feature em `frontend/src/app/features/credit-cards/`: `CreditCardsPage` → `CreditCardsService` → `HttpClient`. Listagem em cards visuais (não tabela e não mockup de plástico). Detalhe, criação e edição em painel na mesma rota. Sem faturas, compras, créditos ou pagamentos de fatura.

Contrato utilizado:

- `GET /api/v1/credit-cards` — array (sem paginação). Filtro oficial `holderName` (omitido quando vazio). Sem filtro client-side.
- `GET /api/v1/credit-cards/{id}`
- `POST /api/v1/credit-cards`
- `PUT /api/v1/credit-cards/{id}`
- `POST /api/v1/credit-cards/{id}/deactivate`
- `POST /api/v1/credit-cards/{id}/activate`
- `GET /api/v1/credit-cards/{id}/limit` — `creditLimit`, `usedLimit`, `availableLimit` oficiais; a UI não recalcula. `availableLimit` pode ser negativo.

`lastFourDigits` é opcional. Visualização `•••• 1234` somente quando houver valor. Sem PAN, CVC ou validade. Ativar/desativar exigem confirmação; cartões inativos permanecem na listagem.

Estados: `loading` | `loaded` | `empty` | `error`. Escape fecha confirmação, depois formulário, depois detalhe; não dispara ação financeira.


# 96M. Integração Cartões ↔ Despesas (Fase 21 — Bloco C2)

A página `/expenses` passa a criar despesas `CREDIT_CARD`. `ExpensesPage` consome `CreditCardsService.list()` (`GET /api/v1/credit-cards`, sem query). O contrato não possui filtro `active`; a UI oferece somente cartões com `active === true`. Falha do catálogo de cartões é explícita e não mascara `of([])`; não derruba os fluxos `ACCOUNT`/`NONE`. Cartão inativo permanece visível no histórico/detalhe quando o `creditCardId` já existir.

Payload de criação `CREDIT_CARD`: `paymentMethod`, `creditCardId` e demais campos cadastrais oficiais; sem `accountId`. `ACCOUNT`/`NONE` não enviam `creditCardId`. Edição de despesa `CREDIT_CARD` continua bloqueada (`canEditExpense`). Sem faturas, créditos ou compras como feature.

# 96N. Faturas (Fase 21 — Bloco C3)

A página `/invoices` é lazy-loaded sob o AuthGuard do MainLayout. A navegação inclui Faturas ao lado de Cartões. Sem rota `/invoices/:id` e sem `/credit-cards/:id/invoices` como rota de UI.

Feature em `frontend/src/app/features/invoices/`: `InvoicesPage` → `InvoicesService` → `HttpClient`. O catálogo de cartões reutiliza `CreditCardsService.list()` (ativos e inativos, consulta histórica). Não há `CatalogService`. C1 permanece em `/credit-cards`.

Contrato utilizado:

- `GET /api/v1/credit-cards/{cardId}/invoices` — array (sem paginação). Filtros oficiais `year`, `month` e `status`. Cartão é obrigatório para consultar; a UI não dispara N+1 em todos os cartões.
- `GET /api/v1/invoices/{id}`
- `GET /api/v1/invoices/{id}/items` — parcelas (`expense_installments`) do ciclo, não a despesa inteira.

`totalAmount`, `paidAmount` e `remainingAmount` são os oficiais da API; a UI não recalcula. Status de fatura apresentados: `SCHEDULED`, `OPEN`, `CLOSED`, `PAID` e `SETTLED_BY_AGREEMENT` (este último só como leitura; sem UI de acordo). Sem `PARTIALLY_PAID` de fatura. Sem lógica de atraso derivada de datas na fatura. O campo `overdue` do item é apresentado quando a API o envia.

Fora deste bloco: pagamentos, ajustes, créditos, acordos, fechamento manual, PDF e C4.

Estados: cartões e faturas com `loading` / `loaded` / `empty` / `error` distintos; idle até selecionar cartão. Escape fecha o painel de detalhe.

# 96O. Pagamento de faturas (Fase 21 — Bloco C4)

A página `/invoices` passa a registrar e estornar pagamentos da fatura no painel de detalhe. Sem rota `/invoices/:id`. Sem ajustes, créditos, acordos, renegociação ou fechamento manual.

`InvoicesPage` → `InvoicesService` (`listPayments`, `createPayment`, `reversePayment`) → `HttpClient`. Contas via `AccountsService.list()` (sem recálculo de saldo). Data padrão do formulário: dia civil `America/Sao_Paulo` (`todayIsoDate`).

Contrato utilizado:

- `GET /api/v1/invoices/{id}/payments` — array de `InvoicePaymentResponse`
- `POST /api/v1/invoices/{id}/payments` — `accountId`, `amount` (`0 < amount <= remainingAmount`), `paymentDate`, `notes` opcional (omitido quando vazio)
- `POST /api/v1/invoices/{invoiceId}/payments/{paymentId}/reverse` — não DELETE

Pagamento permitido em `OPEN` e `CLOSED`. Proibido em `SCHEDULED`, `PAID` e `SETTLED_BY_AGREEMENT`. Estorno somente pagamento `ACTIVE` e fatura não terminal. Após pagar/estornar: recarregar fatura, pagamentos e itens; painel permanece aberto; valores oficiais da API. `OPEN` com `remainingAmount = 0` não vira `PAID` no frontend.

# 96P. Ajustes de fatura (Fase 21 — Bloco C5)

A página `/invoices` registra, lista e reverte ajustes da fatura no mesmo painel de detalhe. Sem rota `/invoices/:id/adjustments`. Sem créditos, acordos, renegociação ou fechamento manual. Sem C6/C7/B12.

`InvoicesPage` → `InvoicesService` (`listAdjustments`, `createAdjustment`, `reverseAdjustment`) → `HttpClient`.

Contrato utilizado:

- `GET /api/v1/invoices/{id}/adjustments` — array de `InvoiceAdjustmentResponse`
- `POST /api/v1/invoices/{id}/adjustments` — `type` (`DISCOUNT` / `SURCHARGE`), `amount` (`> 0`), `reason` obrigatório
- `POST /api/v1/invoices/{invoiceId}/adjustments/{adjustmentId}/reverse` — não DELETE

Tipos e status oficiais do backend. `SURCHARGE` bloqueado na UI quando `remainingAmount <= 0`. Ajustes permitidos enquanto a fatura não é `PAID` nem `SETTLED_BY_AGREEMENT`. Após criar/reverter: recarregar fatura, ajustes e itens (parcelas atualizadas pelo backend); **não** recarregar pagamentos; painel permanece aberto; valores oficiais da API. Sem cálculo local de remaining/status.

# 96Q. Créditos de cartão (Fase 21 — Bloco C6)

A página `/credit-cards` consulta e cria créditos no painel de detalhe do cartão selecionado. Sem rota `/credits`. Sem aplicação manual, reverse, edição ou exclusão. Sem C7/B12.

`CreditCardsPage` → `CreditCardsService` (`listCredits`, `createCredit`) → `HttpClient`.

Contrato utilizado:

- `GET /api/v1/credit-cards/{id}/credits` — array de `CreditCardCreditResponse` (ordem do backend)
- `POST /api/v1/credit-cards/{id}/credits` — `amount` (`> 0`), `reason` obrigatório; origem `MANUAL` definida pelo backend

Response oficial: `id`, `creditCardId`, `amount`, `remainingAmount`, `reason`, `origin` (`MANUAL` / `CARD_PURCHASE_REFUND`), `expenseId` (nullable), `createdAt`. Sem status de domínio. Classificação visual Disponível/Utilizado a partir de `remainingAmount`. Soma dos `remainingAmount` é agregação de apresentação. Após criar: recarregar créditos e `GET .../limit`; não somar crédito ao limite; não afirmar aplicação a fatura.

# 97. Dashboard

Dashboard deve apresentar:

- cards;
- gráficos;
- tabelas;
- indicadores.


# 98. Gráficos

Biblioteca oficial:

Apache ECharts


# 99. Regra

Não criar gráficos manualmente com SVG complexo sem necessidade.

Não utilizar Chart.js / ng2-charts na V1.


# 100. Datas

Frontend não deve recalcular regras financeiras de fechamento de cartão.


# 101. Backend

Backend deve ser responsável pelos cálculos financeiros.


# 102. Frontend

Frontend deve apresentar o resultado recebido da API.


# 103. API

Responses devem ser consistentes.


# 104. Paginação

Listagens grandes devem suportar paginação.


# 105. Ordenação

Quando necessário, API deve permitir:

sort;


# 106. Filtros

Listagens devem permitir filtros relevantes.


Exemplo:

GET /api/v1/expenses?startDate=2026-08-01&endDate=2026-08-31


# 107. Busca

Busca textual deve ser implementada quando realmente necessária.


# 108. Banco

Criar índices para consultas importantes.


# 109. Índices

Priorizar índices em:

user_id;

due_date;

status;

credit_card_id;

account_id;

category_id;


quando aplicável.


# 110. Performance

Não otimizar prematuramente.


# 111. Performance

Primeiro:

correção.


Depois:

clareza.


Depois:

performance.


# 112. Testes

Testes automatizados são obrigatórios.


# 113. Backend

Utilizar:

JUnit


# 114. Backend

Utilizar:

Mockito


quando mocks forem realmente necessários.


# 115. Backend

Utilizar:

Spring Boot Test


# 116. Testes unitários

Devem testar regras de negócio isoladamente.


# 117. Exemplo

ExpenseServiceTest


deve testar:

R$ 100

3 parcelas


resultado:

33,34

33,33

33,33


Não criar `ParcelamentoService` nem `ParcelamentoServiceTest`. A geração e as regras de parcelas ficam em `ExpenseService`.


# 118. Testes de integração

Devem testar:

Controller

Service

Repository


quando necessário.


# 119. Banco em testes

Preferir banco real em container para testes de integração.


# 120. Testcontainers

Utilizar:

Testcontainers


para testes que dependam do PostgreSQL.


# 121. Regra

Não substituir todos os testes de PostgreSQL por banco H2 apenas por facilidade.


# 122. Frontend

Utilizar ferramenta de testes recomendada pela versão do Angular adotada.


# 123. Testes frontend

Priorizar:

services;

componentes críticos;

formulários;

fluxos financeiros importantes.


# 124. Testes E2E

Não são obrigatórios para toda a aplicação na V1.


Podem ser adicionados posteriormente.


# 125. Cobertura

Não buscar uma porcentagem artificial de cobertura.


# 126. Prioridade

Testar principalmente:

regras financeiras.


# 127. Docker

PostgreSQL deve ser executado via Docker.


# 128. PostgreSQL

Configuração local deve possuir:

database;

user;

password;

port;


# 129. Segurança

Credenciais não devem ser commitadas.


# 130. Environment

Utilizar:

.env


quando apropriado.


# 131. Git

Não versionar:

.env


# 132. Git

Não versionar:

logs;

builds;

node_modules;

target;

arquivos temporários.


# 133. Cursor

O Cursor será utilizado para desenvolvimento assistido por IA.


# 134. GitHub

O GitHub pessoal não será conectado ao Cursor.


# 135. Git

Commits serão realizados pelo VSCode.


# 136. Regra

O Cursor não deve tentar:

configurar;

alterar;

ou utilizar credenciais do GitHub.


# 137. Git

O projeto deve continuar funcionando normalmente independentemente da IDE utilizada.


# 138. IDE

Não criar configurações obrigatórias específicas do Cursor.


# 139. Documentação

Toda decisão arquitetural importante deve ser documentada.


# 140. ADR

Quando houver uma decisão técnica relevante e difícil de reverter:

criar um ADR.


# 141. Exemplo

docs/adr/001-escolha-do-banco.md


# 142. IA

A IA deve consultar:

AGENTS.md

e

documentação em docs/


antes de implementar funcionalidades relevantes.


# 143. IA

A IA não deve modificar arquitetura sem justificar a alteração.


# 144. IA

Antes de adicionar uma nova dependência:

verificar se a funcionalidade pode ser resolvida com recursos existentes.


# 145. IA

Não instalar bibliotecas apenas porque são populares.


# 146. IA

Toda dependência deve ter justificativa.


# 147. Arquitetura

Não utilizar microserviços.


# 148. Arquitetura

A V1 será um:

monólito modular.


# 149. Monólito

Backend:

uma aplicação Spring Boot.


# 150. Banco

Um PostgreSQL.


# 151. Frontend

Uma aplicação Angular.


# 152. Comunicação

HTTP/REST.


# 153. Futuro

A arquitetura deve permitir crescimento sem obrigar a adoção imediata de microserviços.


# 154. Regra

Não criar:

Kafka;

RabbitMQ;

Redis;

Kubernetes;


na V1.


# 155. Cache

Não implementar cache antes de existir necessidade real.


# 156. Mensageria

Não implementar mensageria na V1.


# 157. Observabilidade

Logging básico é obrigatório.


Monitoramento avançado pode ficar para o futuro.


# 158. CI/CD

Não é necessário implementar pipeline completo na V1.


# 159. Deploy

V1:

execução local.


# 160. Configuração

Separar:

development;

test;

futuramente production.


# 161. Profiles

Spring Profiles podem ser utilizados.


Exemplo:

application.yml

application-test.yml


# 162. Configuração

Nunca hardcodar:

senha do banco;

JWT secret;

credenciais externas.


# 163. API

Endpoints devem possuir nomes consistentes.


# 164. REST

Utilizar substantivos no plural.


Exemplo:

/accounts

/expenses

/incomes

/payables

/receivables

/transfers

/credit-cards

/invoices

/financial-goals


# 165. Regra

Evitar endpoints excessivamente verbosos.


# 166. Operações especiais

Operações financeiras específicas podem utilizar endpoints orientados à ação quando fizer sentido.


Exemplo:

POST /invoices/{id}/payments

POST /expenses/{id}/pay

POST /expenses/{id}/refund

Fechamento de fatura **não** é ação normal do usuário (`POST /invoices/{id}/close` não é operação funcional da Fase 9). Scheduler Spring (RN096A).


# 167. Pagamento

Pagamento deve ser tratado como operação financeira própria.


# 168. Estorno

Estorno deve ser tratado como operação própria.

Em receitas, estorno canônico (`POST /incomes/{id}/movements/{movementId}/reverse`) desfaz o RECEIPT (`RECEIVED` → `EXPECTED` quando remaining > 0) e **não** cancela a duplicata. Endpoints legados da Fase 6 (`/receive`, `/reverse`) foram **removidos** (**D74-A**).

Cancelamento de receita (`POST /incomes/{id}/cancel`) é operação distinta (`EXPECTED` → `CANCELLED`).

Em despesas (Fase 7), pagamento é `POST /expenses/{id}/pay`; cancelamento é `POST /expenses/{id}/cancel` (`OPEN` → `CANCELLED`); estorno é `POST /expenses/{id}/refund` (`PARTIALLY_PAID` / `PAID` → `REFUNDED`). `POST /payments/{id}/reverse` **não** pertence à Fase 7; entra no contrato da Fase 8.

Na Fase 9, despesa `CREDIT_CARD` usa os mesmos paths de cancel/refund; o refund exige `settlement` `CARD_CREDIT` ou `ACCOUNT` (RN117). Para `ACCOUNT`/`NONE`, `settlement` no mesmo DTO é proibido (**400**, `BUSINESS_RULE_VIOLATION`, `SETTLEMENT_NOT_ALLOWED`).


# 169. Transferência

Transferência deve ser tratada como operação própria.


# 170. Parcelamento

Criação de parcelamento deve ser uma operação de negócio.


# 171. Regra

Não permitir que o frontend monte manualmente dezenas de requisições para criar parcelas.


# 172. Backend

O backend deve receber:

valor;

quantidade de parcelas (`installmentCount`; omitido = 1);

data de vencimento da primeira parcela (`dueDate`);


e gerar as parcelas (valores e vencimentos mensais). Cartão **não** é input da Fase 8 (RN243).


# 173. Banco

Operação deve ser transacional.


# 174. API

Criar endpoints de domínio claros.


# 175. Exemplo

POST /expenses


Na Fase 7 cria despesa simples (`OPEN`, parcela 1/1 interna). Parcelamento funcional (N>1), payments por parcela, adjustments e reverse estão **implementados na Fase 8** (`docs/25` §47).


# 176. Resultado

Backend retorna:

despesa;

parcelas;


# 177. Regra

A API deve evitar múltiplas fontes de verdade.

Totais de fatura (`totalAmount`, `paidAmount`, `remainingAmount`) são calculados pelo backend a partir de parcelas, pagamentos de fatura, alocações, créditos aplicados e ajustes de fatura. Não são colunas. Limite usado/disponível do cartão também é derivado.

`invoice_id` vive na parcela, não na despesa. Modelo: `docs/23-modelo-de-dados.md`.


# 178. Fonte de verdade

Banco de dados é fonte de verdade dos dados persistidos.

Integridade de ownership: FKs compostas `(referenced_id, user_id)` além do filtro de `user_id` nas queries.


# 179. Fonte de verdade

Backend é fonte de verdade das regras.


# 180. Fonte de verdade

Frontend é responsável pela apresentação e interação.


# 181. Arquitetura final da V1

Frontend:

Angular 22.x


Backend:

Java 25 LTS + Spring Boot 4.1.x


Pacote:

br.com.financialcontrol


Persistência:

Spring Data JPA + Hibernate


Banco:

PostgreSQL 18


Migration:

Flyway


Segurança:

Spring Security + JWT Access Token (HS256) + Argon2id


Documentação:

springdoc-openapi / Swagger


PDF:

OpenPDF


Gráficos:

Apache ECharts


Testes:

JUnit 5 + Mockito + AssertJ + Spring Boot Test + Testcontainers


Infraestrutura:

Docker / Docker Compose (PostgreSQL)


# 182. Princípio final

A arquitetura deve ser suficientemente profissional para servir como base de aprendizado e evolução, mas suficientemente simples para que um desenvolvedor consiga entender todo o sistema.


# 183. Regra final

Se uma decisão técnica aumentar significativamente a complexidade sem benefício claro para a V1:

não implementar.


# 184. Regra final

Primeiro construir:

uma base correta.


Depois:

uma base testada.


Depois:

uma base fácil de evoluir.


Somente então:

adicionar funcionalidades.