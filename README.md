# Financial Control

Sistema de controle financeiro pessoal multiusuário, com foco em organização financeira, planejamento futuro e aprendizado de tecnologias modernas.

Cada usuário possui suas próprias contas, cartões, receitas, despesas, metas e demais informações financeiras.

---

## Hierarquia da documentação

```text
AGENTS.md
    ↓
docs/20–28 (decisões funcionais e técnicas oficiais)
    ↓
docs/CODING_STANDARDS.md (convenções de código)
    ↓
.cursor/rules/*.mdc (instruções operacionais para o Cursor)
```

- `AGENTS.md` — autoridade máxima; regras para a IA e desenvolvimento por etapas
- `docs/20`–`docs/28` — especificação completa
- `docs/CODING_STANDARDS.md` — convenções de código; não pode contradizer os documentos superiores
- `.cursor/rules/*.mdc` — instruções operacionais; não criam decisões novas
- Este README — visão geral e execução local; não é fonte de decisões técnicas

Em conflito: corrigir a documentação até ficar consistente. Não manter duas decisões sobre o mesmo tema.

Documentação ativa em `docs/`:

| Documento | Conteúdo |
|-----------|----------|
| `20-fluxos-financeiros.md` | Fluxos financeiros |
| `21-arquitetura-do-sistema.md` | Arquitetura |
| `22-stack-tecnologica.md` | Stack oficial |
| `23-modelo-de-dados.md` | Modelo de dados |
| `24-regras-de-negocio.md` | Regras de negócio |
| `25-api.md` | API REST |
| `26-seguranca.md` | Segurança |
| `27-testes.md` | Estratégia de testes |
| `28-roadmap.md` | Roadmap por fases |
| `CODING_STANDARDS.md` | Convenções de código |

Se ainda existirem `docs/01`–`docs/19`, são **históricos/obsoletos** e não devem ser usados.

---

## Objetivo

Permitir controle financeiro pessoal completo, inicialmente enxuto e sólido:

- receitas e despesas;
- contas bancárias e dinheiro em espécie (`CASH`);
- cartões, faturas e parcelamentos;
- pagamentos e pagamentos parciais;
- transferências entre contas;
- metas e projeções;
- contas a pagar / a receber;
- dashboard, gráficos e relatórios;
- exportação PDF (ex.: despesas em cartão de terceiro por responsável).

---

## Stack oficial

### Backend

- Java 25 LTS
- Spring Boot 4.1.x
- Maven 3.9.x (≥ 3.9.12); Maven Wrapper no backend
- Spring Web, Spring Data JPA, Hibernate
- Spring Security, JWT Access Token (HS256; Refresh Token não implementado na Fase 3)
- Argon2id (`Argon2PasswordEncoder` + BouncyCastle)
- Jakarta Bean Validation
- Flyway
- springdoc-openapi
- JUnit 5, Mockito, AssertJ, Testcontainers
- OpenPDF

### Frontend

- Angular 22.x
- Node.js 22.x LTS (≥ 22.22.3); 24.x ≥ 24.15.0 aceito
- npm empacotado com o Node.js
- TypeScript strict
- Standalone Components, Signals, Services
- Reactive Forms, HttpClient, Interceptors, Route Guards
- Angular Material, Material Icons
- Apache ECharts
- ESLint, Prettier

### Banco

- PostgreSQL 18 (`postgres:18-alpine` via Docker)
- UUID v7 gerado pela aplicação
- NUMERIC(19,2) / BigDecimal; `RoundingMode.HALF_UP`
- TIMESTAMPTZ / `Instant` em UTC; `DATE` / `LocalDate` para calendário financeiro
- Calendário financeiro: timezone `America/Sao_Paulo`
- Flyway; Hibernate `ddl-auto=validate`

### Infraestrutura

- Docker Engine ≥ 24 / Docker Desktop / Compose V2 ≥ 2.24
- PostgreSQL no desenvolvimento via Docker Compose
- Backend e frontend podem rodar fora do Docker inicialmente

### Convenções

- Pacote Java: `br.com.financialcontrol`
- API: `/api/v1`
- Moeda V1: BRL

Detalhes: `docs/22-stack-tecnologica.md`.

---

## Fora da V1

Não introduzir sem decisão futura explícita: Redis, Kafka, RabbitMQ, Kubernetes, microsserviços, GraphQL, NgRx, Zod, Tailwind, Bootstrap, H2, SQLite, CI/CD obrigatório, integração bancária, investimentos, importação de extratos, notificações, PWA, dark mode, deploy em produção, contas compartilhadas.

---

## Arquitetura

```text
Angular 22
   |
   | HTTP / REST /api/v1
   v
Spring Boot 4.1
   |
   | Controller → Service → Repository
   | JPA / Flyway
   v
PostgreSQL 18
```

Monólito modular. Backend é autoridade das regras. Frontend apresenta e valida para UX.

Fluxo HTTP do backend: Controller → Service → Repository. O Controller não acessa o Repository. Toda fronteira HTTP usa DTOs.

---

## Isolamento de usuários

> Nenhum usuário pode consultar, alterar ou excluir dados financeiros de outro usuário.

O backend obtém o usuário do contexto de segurança (`SecurityContext`). Nunca confiar em `userId` enviado pelo frontend.

---

## Autenticação (Fase 3)

Implementado: cadastro, login, Access Token JWT, perfil autenticado e alteração de senha.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/health` | público | `200` `{ "status": "UP" }` |
| `POST` | `/api/v1/auth/register` | público | `201` perfil (sem login automático) |
| `POST` | `/api/v1/auth/login` | público | `200` `{ accessToken, tokenType: "Bearer", expiresIn: 1800 }` |
| `GET` | `/api/v1/users/me` | Bearer | `200` perfil |
| `PUT` | `/api/v1/users/me` | Bearer | `200`; apenas `name` e `email` |
| `PUT` | `/api/v1/users/me/password` | Bearer | `204` |

Regras em vigor:

- Access Token JWT, algoritmo **HS256**, validade **30 minutos** (`expiresIn`: 1800 segundos);
- transporte: `Authorization: Bearer <token>`;
- identidade: claim `sub` = UUID do usuário, lido pelo backend a partir do SecurityContext;
- senhas com **Argon2id**; nunca texto puro;
- e-mail normalizado (trim + minúsculas) antes de persistir e autenticar;
- usuário desativado não autentica (`401` `"Credenciais inválidas."`, mesma mensagem de e-mail/senha inválidos);
- campos JSON desconhecidos na API são rejeitados (`400`);
- e-mail duplicado no cadastro: `409`.

Não implementado na Fase 3: Refresh Token, logout no backend, OAuth, MFA, roles, rate limiting, frontend de autenticação.

Detalhes: `docs/25-api.md` e `docs/26-seguranca.md`.

---

## Contas (Fase 4)

Implementado: criar, listar, consultar, editar (`name` e `type`), desativar, reativar e consultar saldo. Tipos V1: `BANK_ACCOUNT` e `CASH`.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/accounts` | Bearer | `200` array das contas do usuário |
| `GET` | `/api/v1/accounts/{id}` | Bearer | `200`; `404` se não for do usuário |
| `POST` | `/api/v1/accounts` | Bearer | `201` |
| `PUT` | `/api/v1/accounts/{id}` | Bearer | `200`; apenas `name` e `type` |
| `POST` | `/api/v1/accounts/{id}/deactivate` | Bearer | `200` desativação lógica |
| `POST` | `/api/v1/accounts/{id}/activate` | Bearer | `200` |
| `GET` | `/api/v1/accounts/{id}/balance` | Bearer | `200` `{ accountId, totalBalance, reservedAmount, availableBalance, balance }` (Fase 15 contrato; `balance` = alias de `totalBalance`) |

O saldo financeiro total é derivado (RN240; sem `current_balance`). A Fase 15 adiciona `reservedAmount` e `availableBalance`. Extrato (`/statement`) não foi implementado.

Não existe `DELETE` de conta.

---

## Categorias (Fase 5)

Implementado: criar, listar (filtros `type` e `active`), editar (`name` e `type`) e desativar. Tipos V1: `INCOME` e `EXPENSE`.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/categories` | Bearer | `200` array das categorias do usuário |
| `POST` | `/api/v1/categories` | Bearer | `201` |
| `PUT` | `/api/v1/categories/{id}` | Bearer | `200`; apenas `name` e `type` |
| `POST` | `/api/v1/categories/{id}/deactivate` | Bearer | `200` desativação lógica |

Unicidade: `user_id + type + name`, case-insensitive, independente de `active`. Duplicidade: `409`. O nome é persistido após `trim`, com a capitalização informada.

Não existe `GET` por id, reativação nem `DELETE` de categoria nesta fase.

---

## Receitas (Fase 17 — Parte 2)

**CONCLUÍDA E APROVADA** (`docs/24` §19.9 / `docs/25` §32 / §67A). Movimentações em `income_movements`; remaining derivado; saldo via RECEIPT ACTIVE. Endpoints legados `/receive` e `/reverse` **removidos** (**D74-A**).

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/incomes` | Bearer | `200` página |
| `GET` | `/api/v1/incomes/{id}` | Bearer | `200`; `404` se não for do usuário |
| `POST` | `/api/v1/incomes` | Bearer | `201` (`EXPECTED`; inclui `responsibleType`/`responsibleName` opcionais) |
| `PUT` | `/api/v1/incomes/{id}` | Bearer | `200`; somente `EXPECTED` |
| `POST` | `/api/v1/incomes/{id}/accruals` | Bearer | `201` (ACCRUAL) |
| `POST` | `/api/v1/incomes/{id}/receipts` | Bearer | `201` (RECEIPT) |
| `GET` | `/api/v1/incomes/{id}/movements` | Bearer | `200` página |
| `POST` | `/api/v1/incomes/{id}/movements/{movementId}/reverse` | Bearer | `200` (REVERSED) |
| `POST` | `/api/v1/incomes/{id}/cancel` | Bearer | `200` (`CANCELLED`; somente `EXPECTED` sem RECEIPT ACTIVE) |

Estorno de movimentação e cancelamento são operações distintas. Detalhes: `docs/25-api.md` §32 e §67A.

**Histórico Fase 6 (removidos):** `POST /incomes/{id}/receive`, `POST /incomes/{id}/reverse`.

---

## Despesas (Fase 7)

Implementado: despesas simples `ACCOUNT` e `NONE`, sem cartão e sem parcelamento funcional. Toda despesa nasce `OPEN`, com parcela interna 1/1; o cliente não informa `installmentId` no pagamento.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/expenses` | Bearer | `200` página (`items`, `page`, `size`, `totalItems`, `totalPages`) |
| `POST` | `/api/v1/expenses` | Bearer | `201` (`OPEN`; sem payment) |
| `GET` | `/api/v1/expenses/{id}` | Bearer | `200`; `404` se não for do usuário |
| `PUT` | `/api/v1/expenses/{id}` | Bearer | `200`; somente `OPEN` |
| `POST` | `/api/v1/expenses/{id}/pay` | Bearer | `200` (`PARTIALLY_PAID` ou `PAID`) |
| `POST` | `/api/v1/expenses/{id}/cancel` | Bearer | `200` (`CANCELLED`; somente `OPEN`) |
| `POST` | `/api/v1/expenses/{id}/refund` | Bearer | `200` (`REFUNDED`; somente `PARTIALLY_PAID` ou `PAID`) |
| `GET` | `/api/v1/expenses/{id}/payments` | Bearer | `200` array; histórico permanece após `REFUNDED` |
| `GET` | `/api/v1/payments/{id}` | Bearer | `200`; `404` se não for do usuário |

`CREDIT_CARD` e faturas estão **implementados na Fase 9**. Valores oficiais de `payments.type` continuam indefinidos. Parcelas N>1, reverse de payment e adjustments estão **implementados na Fase 8**. Detalhes: `docs/25-api.md`.

---

## Parcelamento e adjustments (Fase 8)

Implementado: despesas parceladas (`installmentCount`), pagamento por parcela, `payments.status` (`ACTIVE` / `REVERSED`), reverse de payment, adjustments `DISCOUNT` / `SURCHARGE` (create/list/reverse), refund misto, overdue N>1, filtro de listagem pelas datas das parcelas.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/expenses/{id}/installments` | Bearer | `200` array |
| `GET` | `/api/v1/expenses/{expenseId}/installments/{installmentId}` | Bearer | `200` |
| `PUT` | `/api/v1/expenses/{expenseId}/installments/{installmentId}` | Bearer | `200`; somente parcela `OPEN` |
| `POST` | `/api/v1/expenses/{expenseId}/installments/{installmentId}/payments` | Bearer | `200` |
| `POST` | `/api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments` | Bearer | `201` |
| `GET` | `/api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments` | Bearer | `200` array (ACTIVE + REVERSED) |
| `POST` | `.../adjustments/{adjustmentId}/reverse` | Bearer | `200` |
| `POST` | `/api/v1/payments/{id}/reverse` | Bearer | `200` |

Fora da Fase 8: semântica de `payments.type`, JSON aninhado completo da despesa N>1, endpoint composto payment+adjustment. Cartão, fatura e rateio estão no contrato da **Fase 9** (**CONCLUÍDA** / **APROVADA**).

---

## Metas (Fase 15)

Implementado: meta vinculada a uma conta (`BANK_ACCOUNT` / `CASH`); contribuição; resgate em `ACTIVE` e `COMPLETED`; conclusão manual; cancelamento com `currentAmount = 0`. `currentAmount` e `progressPercent` são derivados. Contribuição/resgate não alteram `totalBalance`.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `POST` | `/api/v1/financial-goals` | Bearer | `201` (`ACTIVE`) |
| `GET` | `/api/v1/financial-goals` | Bearer | `200` página (`items`, `page`, `size`, `totalItems`, `totalPages`) |
| `GET` | `/api/v1/financial-goals/{id}` | Bearer | `200`; `404` se não for do usuário |
| `PUT` | `/api/v1/financial-goals/{id}` | Bearer | `200`; somente `ACTIVE` |
| `POST` | `/api/v1/financial-goals/{id}/contributions` | Bearer | `201` |
| `GET` | `/api/v1/financial-goals/{id}/contributions` | Bearer | `200` array |
| `POST` | `/api/v1/financial-goals/{id}/redemptions` | Bearer | `201` |
| `GET` | `/api/v1/financial-goals/{id}/redemptions` | Bearer | `200` array |
| `POST` | `/api/v1/financial-goals/{id}/complete` | Bearer | `200` (`COMPLETED`) |
| `POST` | `/api/v1/financial-goals/{id}/cancel` | Bearer | `200` (`CANCELLED`; exige reservado zero) |

Não existe `DELETE` nem reverse de contribuição/resgate. Detalhes: `docs/25-api.md` §54E.

---

## Contas a pagar (Fase 16)

**CONCLUÍDA E APROVADA** (`docs/24` §19.7 / `docs/25` §66). Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes).

Visão derivada. Sem tabela `payables`. Endpoint:

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/payables` | Bearer | `200` página + `totalRemaining`, `totalOriginal`, `totalPaid` |

Linha = parcela ACCOUNT/NONE com remaining > 0 **ou** fatura com remaining > 0. Cartão não é somado de novo como despesa/parcela. "Mês atual" = mês selecionado no filtro.

---

## Contas a receber (Fase 17)

**CONCLUÍDA E APROVADA** (`docs/24` §19.8 / §19.9 / `docs/25` §67 / §67A). Parte 1: visão derivada. Parte 2: movimentações (`income_movements`), item/resumo evoluídos (D77-A / D92-B), filtros D78-A / D88 / D94.

Visão derivada de `Income`. Sem tabela `receivables`. Endpoint:

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/receivables` | Bearer | `200` página + `summary` (`futureAmount`, `overdueAmount`, `totalReceivableAmount`, `receivedAmount`) |

Data: `expectedDate` (obrigatória; sem alias `dueDate`). Padrão: somente `EXPECTED`. `OVERDUE` derivado. Resumo no mesmo GET, respeitando filtros. Sem `GET /receivables/{id}`. Sem escrita neste recurso.

---

## Regras financeiras (resumo)

| Tema | Regra |
|------|--------|
| Contas | Tipos: `BANK_ACCOUNT`, `CASH` |
| Categorias | Tipos: `INCOME`, `EXPENSE`; unicidade `user + type + name` (case-insensitive, inclusive inativas) |
| Responsável | `MINE`, `GIULIA`, `EDERSON`, `ELISIANE`, `OTHER` (+ texto) — não são usuários |
| Despesa status | `OPEN`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED` |
| Vencida | Derivada (`OVERDUE` não persistido) |
| Cartão | `holderName` textual (filtrável; não precisa ser o usuário); `last_four_digits` opcional; não armazenar PAN/CVC/validade; inativar não exclui; compra não reduz saldo bancário; compra acima do limite **permitida** (RN029A **SUPERADA**); `used_limit`/`available_limit` derivados (available pode ser negativo); crédito de cartão não aumenta limite; compra no dia do fechamento vai para a próxima fatura; dia inexistente no mês → último dia do mês; `due_date` da fatura: `due_day` > `closing_day` → mesmo mês; `due_day` ≤ `closing_day` → mês seguinte (RN099B) |
| Fatura | `SCHEDULED`, `OPEN`, `CLOSED`, `PAID` (`PARTIALLY_PAID` **não** é status de fatura); no máximo uma `OPEN` por cartão; futuras de parcelamento = `SCHEDULED`; pagamento parcial não muda status; `PAID` só após fechamento e remaining 0; pagamento não cria despesa nova nem linha em `payments` |
| Parcelas | Soma = total; residual na **primeira** parcela; quantidade imutável após criação; `ACCOUNT`/`NONE`: vencimentos mensais (dia-base); `CREDIT_CARD`: vencimento = due da fatura do ciclo; edição somente parcela `OPEN` sem fatura (RN227); pagamento por parcela só `ACCOUNT`/`NONE`; `payments.status` ACTIVE/REVERSED; adjustments DISCOUNT/SURCHARGE com `reason` (Fase 9); reverse de payment/adjustment |
| Crédito de cartão | Pertence ao cartão; não movimenta conta; não cria fatura; FIFO dos créditos; faturas elegíveis por `due_date` ASC depois `id` ASC; nunca negativo; manual exige `reason`; `GET .../credits` = array com `remainingAmount` por crédito; total disponível = `SUM(remainingAmount)` (derivado) |
| Ajuste de fatura | `DISCOUNT` / `SURCHARGE` com `reason`; `SURCHARGE` exige remaining da fatura > 0 |
| Refund | `CREDIT_CARD` exige `settlement`; `ACCOUNT`/`NONE` + `settlement` → 400 `BUSINESS_RULE_VIOLATION` (`SETTLEMENT_NOT_ALLOWED`) |
| Transferência | Atômica; só `BANK_ACCOUNT`; status `ACTIVE`/`REVERSED`; não é receita/despesa; sem saldo insuficiente (criação e reversão); listagem MVP sem filtro de status |
| Acerto de Saldos | Fato `BALANCE_ADJUSTMENT` (tabela `account_balance_adjustments`); usuário informa saldo real; diferença calculada; `BANK_ACCOUNT` e `CASH` |
| Saldo inicial | Opcional na criação (default `0,00`); alteração só via `PUT .../initial-balance` até a primeira movimentação efetiva (RN010A) |
| Saldo | Derivado: **total** (RN240), **reservado em metas** e **disponível** (Fase 15); operações normais usam disponível; sem `current_balance` |
| Metas (Fase 15) | Reserva vinculada a conta; contribuição; resgate em `ACTIVE` e `COMPLETED`; conclusão manual; cancelamento só com `currentAmount = 0`; `currentAmount` e `progressPercent` derivados |
| Contas a pagar (Fase 16) | Visão derivada `GET /api/v1/payables` (`CONCLUÍDA E APROVADA`); linha = parcela ACCOUNT/NONE ou fatura, remaining > 0; sem tabela; cartão só como fatura |
| Contas a receber (Fase 17) | **`CONCLUÍDA E APROVADA`**: visão `GET /api/v1/receivables`; `income_movements` (V30); D73–D94 implementadas; sem tabela `receivables` |
| Receita | `EXPECTED` / `RECEIVED` / `CANCELLED`; acréscimos (ACCRUAL) e recebimentos (RECEIPT) em `income_movements`; remaining derivado; cancelar somente sem RECEIPT ACTIVE (**D73**); estorno por movimentação (`/movements/{id}/reverse`); responsável (**D89**) implementado; cabeçalho `account_id`/`received_date` legado (**D76**) |
| Despesa (Fase 7) | `ACCOUNT` e `NONE`; criação `OPEN` sem payment; parcela 1/1 interna; `POST /expenses/{id}/pay`; cancelar só `OPEN`; estornar `PARTIALLY_PAID`/`PAID` → `REFUNDED`; `overdue` derivado; cartão fora. A RN210 (payment na mesma conta) foi **SUPERADA** no contrato da Fase 8. |
| Pagamentos | Sem saldo negativo em operações normais; fatura parcial limitada ao saldo da conta |
| PDF / gráficos | OpenPDF / Apache ECharts |

Detalhes: `docs/24-regras-de-negocio.md`.

---

## Execução local

### Início rápido (Windows)

Na raiz do projeto:

```powershell
powershell.exe -ExecutionPolicy Bypass -File ".\scripts\start.ps1"
```

O script valida o ambiente, garante o `.env` local (gera `JWT_SECRET` se o arquivo ainda não existir), sobe o PostgreSQL, o backend e o frontend.

Para encerrar:

```powershell
powershell.exe -ExecutionPolicy Bypass -File ".\scripts\stop.ps1"
```

O arquivo `.env` **não é versionado**. O modelo é `.env.example`.

### Pré-requisitos

Versões oficiais: `docs/22-stack-tecnologica.md` (Environment Contract).

- Git ≥ 2.39
- Docker Desktop (Engine ≥ 24, daemon em execução)
- Docker Compose V2 ≥ 2.24 (`docker compose`)
- Node.js 22.x LTS ≥ 22.22.3 (preferencial) ou 24.x ≥ 24.15.0
- npm empacotado com o Node.js (não atualizar com `npm install -g npm@latest`)
- Java 25 LTS (JDK, com `javac`)
- Maven 3.9.x ≥ 3.9.12; o backend já possui Maven Wrapper (`backend/mvnw` / `backend/mvnw.cmd`)
- IDE ou editor

### Diagnóstico do ambiente (Windows)

Somente leitura; não instala nem altera o sistema:

```powershell
.\scripts\check-environment.ps1
```

Se a política de execução bloquear o script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-environment.ps1
```

### PostgreSQL

```bash
docker compose up -d
docker compose ps
docker compose logs -f postgres
docker compose down
```

### Variáveis de ambiente

- `.env.example` — modelo das variáveis (sem segredo real)
- `.env` — local, **não versionar** (já está no `.gitignore`)

O backend **exige** `JWT_SECRET` com no mínimo 32 bytes. Não há default em `application.yml`. O valor de exemplo em `.env.example` não deve ser usado em produção.

`scripts/start.ps1` cria `.env` a partir de `.env.example` e gera um `JWT_SECRET` local quando o arquivo ainda não existe. Para carregar o `.env` em uma sessão manual:

```powershell
. .\scripts\import-dotenv.ps1
```

Outras variáveis típicas: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_EXPIRATION_MINUTES` (padrão 30), `CORS_ALLOWED_ORIGINS`, `APP_PORT`.

### Backend

Com o PostgreSQL no ar e o `.env` configurado, na raiz do projeto:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
```

Ou, de forma equivalente:

```powershell
. .\scripts\import-dotenv.ps1
cd backend
.\mvnw.cmd spring-boot:run
```

- Health: `http://localhost:8080/api/v1/health`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend

O scaffold Angular da Fase 1 existe em `frontend/`. A UI de autenticação **não** foi implementada na Fase 3.

### Estrutura

```text
financial-control/
├── AGENTS.md
├── README.md
├── .gitignore
├── .cursorignore
├── .env.example
├── docker-compose.yml
├── docs/
│   ├── 20–28
│   └── CODING_STANDARDS.md
├── .cursor/rules/
├── scripts/
│   ├── check-environment.ps1
│   ├── import-dotenv.ps1
│   ├── run-backend.ps1
│   ├── start.ps1
│   └── stop.ps1
├── backend/          (Fase 1+)
├── frontend/         (Fase 1+)
└── postman/          (collection e environment locais)
```

Portas típicas: Angular `4200`, Spring Boot `8080`, PostgreSQL `5432`.

---

## Desenvolvimento

Desenvolvimento assistido no Cursor. Commits e pushes manuais via VSCode/Git. A IA não deve executar push nem assumir acesso ao GitHub.

Fluxo por fases: ver `docs/28-roadmap.md`.

Cada fase: escopo definido → implementação → testes → validação → só então a próxima.

---

## Segurança

- Argon2id para senhas
- JWT Access Token (HS256, 30 minutos, Bearer)
- Refresh Token **não** implementado na Fase 3
- Isolamento por usuário via SecurityContext
- Secrets fora do código (`JWT_SECRET` obrigatório)
- Validação no backend (Jakarta Bean Validation + regras de negócio)
- Sem dados sensíveis desnecessários em logs

Ver `docs/26-seguranca.md`.

---

## Testes

Unitários, integração, API e segurança. Testcontainers com PostgreSQL. Ver `docs/27-testes.md`.

Coleção Postman para testes manuais: `postman/Financial Control API.postman_collection.json` (instruções em `postman/README.md`). A collection acompanha as fases já implementadas (até Fase 19 — dashboard).

---

## Status

**Fase atual:** Fase 19 — Dashboard — **CONCLUÍDA E APROVADA**

**Próxima fase:** Fase 20 — Relatórios — **NÃO INICIADA**

```text
Fase 0 — Planejamento — CONCLUÍDA E APROVADA
Fase 1 — Fundação / estrutura inicial — CONCLUÍDA E APROVADA
Fase 2 — Persistência / modelo de dados — CONCLUÍDA E APROVADA
Fase 3 — Autenticação e segurança — CONCLUÍDA E APROVADA
Fase 4 — Contas — CONCLUÍDA E APROVADA
Fase 5 — Categorias — CONCLUÍDA E APROVADA
Fase 6 — Receitas — CONCLUÍDA E APROVADA
Fase 7 — Despesas — CONCLUÍDA E APROVADA
Fase 8 — Parcelamento de despesas — CONCLUÍDA E APROVADA
Fase 9 — Cartões / faturas (expandida) — CONCLUÍDA E APROVADA
Fase 10–12 — Absorvidas na Fase 9
Fase 13 — Parcelamento / negociação / renegociação de fatura — CONCLUÍDA E APROVADA
Fase 14 — Transferências, Acerto de Saldos e Saldo Inicial — CONCLUÍDA E APROVADA
Fase 15 — Metas — CONCLUÍDA E APROVADA
Fase 16 — Contas a pagar — CONCLUÍDA E APROVADA
Fase 17 — Contas a receber — CONCLUÍDA E APROVADA
Fase 18 — Projeções — CONCLUÍDA E APROVADA
Fase 19 — Dashboard — CONCLUÍDA E APROVADA
```

Estado atual do backend (Fases 1–9 + 13 + 14 + 15 + 16 + 17 + 18 + 19): Spring Boot **4.1.0**, Java **25**, Maven Wrapper, PostgreSQL **18**, Flyway, Spring Security, JWT Access Token HS256, Argon2id, Jakarta Bean Validation, Testcontainers, OpenAPI/Swagger, fluxo Controller → Service → Repository, domínio de contas, categorias, receitas (movimentações V30), despesas, parcelamento (Fase 8), cartões/faturas (Fase 9), Agreements (Fase 13), transferências e Acerto de Saldos (Fase 14), metas financeiras (Fase 15), contas a pagar (Fase 16), contas a receber (Fase 17), projeções (Fase 18), dashboard (Fase 19).

Fases 0–19. **Fase 19 — Dashboard:** **CONCLUÍDA E APROVADA** (`docs/24` §19.11; D282–D289). Endpoint `GET /api/v1/dashboard`. **Não** criar tabela `dashboard`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes). **Fase 18 — Projeções:** **CONCLUÍDA E APROVADA** (`docs/24` §19.10; D95–D204). Endpoint `GET /api/v1/projections`. **Não** criar tabela `projections`. D73–D94 implementadas; **não** recriar `GET /api/v1/receivables`; **não** criar tabela `receivables`. Não implementar Refresh Token, `payments.type`, relatórios/PDF, frontend financeiro, auditoria genérica, extrato `/statement` nem `POST /invoices/{id}/close` sem autorização.

Não implementar Refresh Token, logout, OAuth, MFA, roles, rate limiting, frontend financeiro, relatórios/PDF, auditoria genérica nem `payments.type` sem autorização. A edição de parcela já em fatura (§269.2.7) permanece **deferida**. Fechados: §269.3, §269.4, **§269.5** (Fase 13 — `CONCLUÍDA E APROVADA`).
