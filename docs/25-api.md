# API — Financial Control

## 0. Hierarquia e convenções

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

- Prefixo: `/api/v1`
- REST + JSON + DTOs
- OpenAPI / springdoc-openapi
- PDF: OpenPDF


## 1. Objetivo

Este documento define o contrato inicial da API REST do Financial Control.

Backend:

Java

Frontend:

Angular

Banco:

PostgreSQL


# 2. Arquitetura

A API seguirá o padrão:

REST


Formato:

JSON


Comunicação:

HTTP/HTTPS


# 3. Base URL

Desenvolvimento:

```text
http://localhost:8080/api/v1
```

Prefixo oficial da API:

```text
/api/v1
```


# 4. Versionamento

A API deve utilizar versionamento.


V1:

/api/v1


Exemplo:

/api/v1/expenses


# 5. Content-Type

Requests e responses JSON devem utilizar:

application/json


# 6. Autenticação

A API utiliza JWT Access Token (HS256).

Transporte: `Authorization: Bearer <token>`.

Senhas: Argon2id.

Identidade: claim `sub` = UUID do usuário, obtido pelo backend a partir do SecurityContext.

Nunca confiar em `userId` enviado pelo cliente.


## Fase 3 — implementado

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`
- `PUT /api/v1/users/me/password`
- Access Token JWT (HS256, 30 minutos, `expiresIn` em segundos = 1800)


## Fase 3 — não implementado

- Refresh Token
- `POST /api/v1/auth/refresh`
- Logout no backend
- OAuth / login social
- MFA
- roles
- rate limiting
- frontend de autenticação


# 6.1 Cadastro

Endpoint público:

`POST /api/v1/auth/register`

Request:

```json
{
  "name": "Nome do Usuário",
  "email": "usuario@email.com",
  "password": "senha"
}
```

Regras:

- `name` obrigatório, 1–255 caracteres (após trim);
- `email` obrigatório, formato válido, normalizado (`trim` + lowercase) antes de persistir;
- `password` obrigatório, 8–128 caracteres;
- usuário criado como `active = true`;
- UUID v7 gerado pela aplicação;
- senha persistida somente como hash Argon2id;
- não realiza auto-login;
- e-mail duplicado: **409 Conflict** (`code`: `CONFLICT`);
- validação: **400** (`code`: `VALIDATION_ERROR`).

Response **201 Created**:

```json
{
  "id": "uuid",
  "name": "Nome do Usuário",
  "email": "usuario@email.com",
  "active": true,
  "createdAt": "2026-08-13T12:00:00Z",
  "updatedAt": "2026-08-13T12:00:00Z"
}
```

Nunca retorna `password` nem `passwordHash`.


# 7. Login

Endpoint público:

`POST /api/v1/auth/login`

Request:

```json
{
  "email": "usuario@email.com",
  "password": "senha"
}
```

O e-mail é normalizado (`trim` + lowercase) antes da busca.

Response **200 OK**:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

`expiresIn` é a validade do Access Token em segundos (30 minutos).

Não retorna `refreshToken`, perfil, `password` nem `passwordHash`.

E-mail inexistente, senha incorreta ou usuário desativado: **401 Unauthorized**, mensagem genérica `Credenciais inválidas.`, `code`: `UNAUTHORIZED`. A API não distingue esses casos.


# 7.1 Refresh

Não implementado na Fase 3.

Não existe `POST /api/v1/auth/refresh` nesta versão da API.


# 8. Usuário autenticado

O backend identifica o usuário através do JWT (`sub`) e do SecurityContext.

Nunca confiar em `userId` enviado pelo frontend.


# 9. Perfil

Endpoint protegido (Bearer):

`GET /api/v1/users/me`

Retorna somente o usuário autenticado, no mesmo formato do cadastro.

Sem token, token inválido, expirado, usuário inexistente ou desativado: **401** (`code`: `UNAUTHORIZED`, mensagem `Não autenticado.`).


# 10. Atualização de perfil

Endpoint protegido (Bearer):

`PUT /api/v1/users/me`

Request (substituição dos campos permitidos):

```json
{
  "name": "Novo Nome",
  "email": "novo@email.com"
}
```

Campos permitidos: `name`, `email`.

Campos rejeitados (propriedades desconhecidas no DTO → **400**): `id`, `active`, `password`, `passwordHash`, `createdAt`, `updatedAt`, `userId`.

E-mail duplicado de outro usuário: **409 Conflict**.


# 11. Alteração de senha

Endpoint protegido (Bearer):

`PUT /api/v1/users/me/password`

Request:

```json
{
  "currentPassword": "...",
  "newPassword": "..."
}
```

- `newPassword`: 8–128 caracteres;
- senha atual incorreta: **401**, `Credenciais inválidas.`;
- sucesso: **204 No Content**, sem corpo.


# 12. Health Check

Endpoint:

GET /api/v1/health


Response:

{
  "status": "UP"
}


# 13. Contas


## Fase 4 — implementado

- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `POST /api/v1/accounts`
- `PUT /api/v1/accounts/{id}`
- `POST /api/v1/accounts/{id}/deactivate`
- `POST /api/v1/accounts/{id}/activate`
- `GET /api/v1/accounts/{id}/balance`

Todos os endpoints de contas exigem JWT Bearer. Sem token, token inválido, expirado ou usuário desativado: **401** (`code`: `UNAUTHORIZED`).

O proprietário é sempre o usuário autenticado (claim `sub`). Propriedades desconhecidas no JSON — inclusive `userId`, `id`, `active`, `createdAt` e `updatedAt` — são rejeitadas (**400**, `VALIDATION_ERROR`).

Conta de outro usuário ou UUID inexistente: **404** (`code`: `NOT_FOUND`, mensagem `Conta não encontrada.`). A API não distingue esses casos, para não vazar existência do recurso.


## Fora do escopo da Fase 14

- `GET /api/v1/accounts/{id}/statement` — extrato unificado **fora do escopo da Fase 14** e não implementado. O modelo permanece compatível com representação futura.


Endpoint:

GET /api/v1/accounts


Lista todas as contas do usuário autenticado (ativas e desativadas), em array JSON, ordenadas por `createdAt` crescente.

Não utiliza paginação nesta fase: o volume típico de contas pessoais não justifica o contrato paginado.

Response **200**:

```json
[
  {
    "id": "uuid",
    "name": "Nubank",
    "type": "BANK_ACCOUNT",
    "initialBalance": 1500.00,
    "active": true,
    "createdAt": "2026-08-13T12:00:00Z",
    "updatedAt": "2026-08-13T12:00:00Z"
  }
]
```

Não inclui `userId`.


# 14. Conta

Endpoint:

GET /api/v1/accounts/{id}


Retorna uma conta específica do usuário autenticado, inclusive se estiver desativada (histórico).

UUID inválido no path: **400**.


# 15. Criar conta

Endpoint:

POST /api/v1/accounts


Request:

```json
{
  "name": "Nubank",
  "type": "BANK_ACCOUNT",
  "initialBalance": 1500.00
}
```

Regras:

- `name` obrigatório, 1–255 caracteres (após trim);
- `type` obrigatório; somente `BANK_ACCOUNT` ou `CASH`;
- `initialBalance` **opcional** (contrato Fase 14 / RN010). Omitido ⇒ `0,00`. Se informado: número decimal JSON; no máximo 17 dígitos inteiros e 2 casas decimais; persistido como `NUMERIC(19,2)` e normalizado no backend com `RoundingMode.HALF_UP`, escala 2;
- a presença de `initialBalance` na criação **não** impede definição/alteração posterior via `PUT /accounts/{id}/initial-balance`, enquanto a conta ainda não tiver movimentação (RN010A);
- conta criada como `active = true`;
- UUID v7 gerado pela aplicação;
- o `userId` não é aceito no request.

Exemplos válidos:

```json
{ "name": "Nubank", "type": "BANK_ACCOUNT" }
```

⇒ `initialBalance = 0.00`

```json
{ "name": "Nubank", "type": "BANK_ACCOUNT", "initialBalance": 2000.00 }
```

Response **201 Created**: mesmo formato de `GET /api/v1/accounts/{id}`.

**Nota de implementação:** a Fase 14 tornou `initialBalance` opcional e aplica o default `0,00` quando o campo é omitido.


# 16. Atualizar conta

Endpoint:

PUT /api/v1/accounts/{id}


Request (substituição dos campos permitidos):

```json
{
  "name": "Nubank PJ",
  "type": "CASH"
}
```

Campos permitidos: `name`, `type`.

Campos rejeitados (propriedades desconhecidas no DTO → **400**): `id`, `userId`, `initialBalance`, `active`, `createdAt`, `updatedAt`.

O saldo inicial **não** é alterável por este `PUT`. Definição/alteração controlada do saldo inicial (enquanto sem movimentação): contrato da Fase 14 — endpoint dedicado abaixo (RN010). Após a primeira movimentação, correção = Acerto de Saldos.

O estado ativo/inativo não é alterável por PUT; usar os endpoints de desativar e reativar.


# 17. Desativar conta

Endpoint:

POST /api/v1/accounts/{id}/deactivate


Desativação lógica. Não excluir fisicamente. Não existe `DELETE /api/v1/accounts/{id}`.

A conta permanece persistida e consultável. Somente contas ativas poderão ser utilizadas em novas operações financeiras (RN007).

**Fase 14 (RN007A; emendada Fase 15 / RN274):** rejeitar se o **saldo financeiro total** for diferente de `0,00` **ou** se existir **valor reservado em metas** (`reservedAmount > 0`). Somente `totalBalance == 0` **e** `reservedAmount == 0` permitem inativação.

Response **200** com a conta (`active = false`). A operação é idempotente quando já inativa e elegível.


# 18. Reativar conta

Endpoint:

POST /api/v1/accounts/{id}/activate


Reativa uma conta do usuário autenticado.

Response **200** com a conta (`active = true`). A operação é idempotente.


# 19. Saldo da conta

Endpoint:

GET /api/v1/accounts/{id}/balance


Response:

```json
{
  "accountId": "...",
  "totalBalance": 10000.00,
  "reservedAmount": 6000.00,
  "availableBalance": 4000.00,
  "balance": 10000.00
}
```

- `totalBalance` — saldo financeiro total (RN240);
- `reservedAmount` — soma dos `currentAmount` das metas `ACTIVE` ou `COMPLETED` vinculadas à conta (Fase 15 / RN265);
- `availableBalance` — `totalBalance − reservedAmount` (RN276);
- `balance` — alias legado de `totalBalance` (compatibilidade retroativa).

Na Fase 4 o saldo derivado é igual ao `initialBalance` (neste caso `totalBalance = availableBalance`, `reservedAmount = 0`). A partir da Fase 6, receitas `RECEIVED` passam a somar. A partir da Fase 7, pagamentos de despesas cuja despesa **não** está `CANCELLED` nem `REFUNDED` passam a subtrair (RN216). A Fase 8 **emenda** a fórmula (RN240): somente payments **`ACTIVE`** entram no subtraendo. A Fase 9 inclui pagamentos de fatura `ACTIVE` e devoluções ACCOUNT. A **Fase 14** inclui transferências `ACTIVE` e acertos de saldo `ACTIVE` (`BALANCE_ADJUSTMENT`). A **Fase 15** adiciona `reservedAmount` e `availableBalance`; contribuições/resgates de meta **não** alteram `totalBalance`. Não existe coluna `current_balance`. Este endpoint é **leitura derivada**; não exige lock pessimista da conta. Saldo as-of-date é capacidade **interna** da Fase 14 (não obrigatório expor `date` neste GET nesta fase).


# 20. Extrato da conta

Endpoint previsto (futuro; **fora da Fase 14**):

GET /api/v1/accounts/{id}/statement


Query parameters:

startDate

endDate

page

size

Não implementado. Dependência: contrato futuro de extrato unificado. O modelo deve permanecer compatível.


# 21. Cartões

Contrato da **Fase 9** (implementado).

Endpoint:

GET /api/v1/credit-cards


Filtro oficial: `holderName` (titular; textual; não precisa ser o usuário).


# 22. Cartão

Endpoint:

GET /api/v1/credit-cards/{id}


# 23. Criar cartão

Endpoint:

POST /api/v1/credit-cards


Request:

{
  "name": "Nubank",
  "holderName": "Ederson",
  "lastFourDigits": "1234",
  "creditLimit": 5000.00,
  "closingDay": 10,
  "dueDay": 20
}


`lastFourDigits` é **opcional**. Não enviar PAN, CVC nem validade. Cartão nasce `active = true`. Não existe `DELETE` de cartão com histórico.


# 24. Atualizar cartão


# 24. Atualizar cartão

Endpoint:

PUT /api/v1/credit-cards/{id}


# 25. Desativar cartão

Endpoint:

POST /api/v1/credit-cards/{id}/deactivate


# 26. Reativar cartão

Endpoint:

POST /api/v1/credit-cards/{id}/activate


# 27. Limite do cartão

Endpoint:

GET /api/v1/credit-cards/{id}/limit


Response:

```json
{
  "creditLimit": 5000.00,
  "usedLimit": 1500.00,
  "availableLimit": 3500.00
}
```

`creditLimit` persistido. `usedLimit` e `availableLimit` **derivados** (não colunas). `availableLimit` pode ser negativo. Crédito de cartão não entra neste cálculo como aumento de limite.


# 27A. Créditos do cartão

Endpoints da Fase 9 (**implementados**):

- `GET /api/v1/credit-cards/{id}/credits`
- `POST /api/v1/credit-cards/{id}/credits` — crédito manual (`amount`, `reason` obrigatório)

### GET — shape oficial

Response **200**: **array** de créditos (sem envelope).

Cada item inclui, entre outros campos oficiais do DTO, `remainingAmount` (saldo ainda não aplicado daquele crédito).

O **saldo disponível total** de créditos do cartão **não** é campo da response. É **derivado** na leitura:

```text
SUM(remainingAmount) de todos os créditos do cartão
```

Não há response com `availableAmount` + `credits`. Não persistir saldo total de créditos.

Aplicação automática: sem endpoint de “aplicar crédito”. FIFO dos créditos (`created_at` ASC, `id` ASC). Faturas elegíveis (`OPEN` ou `CLOSED`, remaining > 0) por `due_date` ASC depois `id` ASC. Não movimenta conta. Não cria fatura. Detalhe: RN246.


# 28. Categorias


## Fase 5 — implementado

- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `POST /api/v1/categories/{id}/deactivate`

Todos os endpoints de categorias exigem JWT Bearer. Sem token, token inválido, expirado ou usuário desativado: **401** (`code`: `UNAUTHORIZED`).

O proprietário é sempre o usuário autenticado (claim `sub`). Propriedades desconhecidas no JSON — inclusive `userId`, `id`, `active`, `createdAt` e `updatedAt` — são rejeitadas (**400**, `VALIDATION_ERROR`).

Categoria de outro usuário ou UUID inexistente: **404** (`code`: `NOT_FOUND`, mensagem `Categoria não encontrada.`). A API não distingue esses casos, para não vazar existência do recurso.

Não existe `GET /api/v1/categories/{id}` nesta fase. Não existe `POST /api/v1/categories/{id}/activate`. Não existe `DELETE /api/v1/categories/{id}`.


## Fase 5 — não implementado

- uso de categoria em receitas ou despesas — pertence às fases dos respectivos domínios;
- reativação de categoria;
- categorias padrão / seeds;
- subcategorias.


Endpoint:

GET /api/v1/categories


Query (opcionais, combináveis):

type

active

Exemplos: `?type=EXPENSE`, `?type=INCOME`, `?active=true`, `?active=false`, `?type=EXPENSE&active=true`.

Lista somente as categorias do usuário autenticado, em array JSON, ordenadas por `createdAt` crescente. Sem filtro `active`, retorna ativas e desativadas. Sem paginação nesta fase.

Response **200**:

```json
[
  {
    "id": "uuid",
    "name": "Mercado",
    "type": "EXPENSE",
    "active": true,
    "createdAt": "2026-08-14T12:00:00Z",
    "updatedAt": "2026-08-14T12:00:00Z"
  }
]
```

Não inclui `userId`. Lista vazia: `[]`.


# 29. Criar categoria

Endpoint:

POST /api/v1/categories


Request:

```json
{
  "name": "Mercado",
  "type": "EXPENSE"
}
```

Regras:

- `name` obrigatório, 1–255 caracteres (após trim);
- `type` obrigatório; somente `INCOME` ou `EXPENSE`;
- categoria criada como `active = true`;
- UUID v7 gerado pela aplicação;
- o `userId` não é aceito no request;
- unicidade: `user_id + type + name` (case-insensitive; independente de `active`). Duplicidade: **409 Conflict** (`code`: `CONFLICT`).

Response **201 Created**: mesmo formato de um item da listagem.


# 30. Atualizar categoria

Endpoint:

PUT /api/v1/categories/{id}


Request (substituição dos campos permitidos):

```json
{
  "name": "Moradia",
  "type": "EXPENSE"
}
```

Campos permitidos: `name`, `type`.

Campos rejeitados (propriedades desconhecidas no DTO → **400**): `id`, `userId`, `active`, `createdAt`, `updatedAt`.

O estado ativo/inativo não é alterável por PUT; usar o endpoint de desativar.

A combinação final `user_id + type + name` permanece única (mesmas regras da criação). Duplicidade: **409**.


# 31. Desativar categoria

Endpoint:

POST /api/v1/categories/{id}/deactivate


Desativação lógica. Não excluir fisicamente. Não existe `DELETE /api/v1/categories/{id}`.

A categoria permanece persistida e consultável (inclusive com `?active=false`). A combinação `user_id + type + name` continua exclusiva. Somente categorias ativas poderão ser utilizadas em novos lançamentos das fases posteriores (RN033).

Response **200** com a categoria (`active = false`). A operação é idempotente.


# 32. Receitas

Contrato vigente — Fase 17 **`CONCLUÍDA E APROVADA`** (`docs/24` §19.9 / `docs/25` §67A).

**Status:** Parte 2 **implementada**. Decisões D73–D94 **fechadas** e **implementadas**.

O registro em `incomes` é a duplicata. Cancelamento (`POST /cancel`) e estorno de movimentação (`POST /movements/{id}/reverse`) são operações diferentes.

Endpoints canônicos de movimentação: `POST /accruals`, `POST /receipts`, `GET /movements`, `POST /movements/{movementId}/reverse` (**D74-A** — legado `/receive` e `/reverse` **removidos**).

Detalhe completo dos endpoints de movimentação: §67A.

Endpoint:

GET /api/v1/incomes


Filtros:

startDate

endDate

status

categoryId

accountId

page

size

`responsibleType` e `responsibleName` fazem parte do contrato **implementado** (**D89** / RN306). `POST` / `PUT` aceitam; `GET` devolve.


Response de item (criação, consulta, listagem, edição e ações):

```json
{
  "id": "...",
  "categoryId": "...",
  "accountId": "...",
  "description": "Salário",
  "amount": 5400.00,
  "expectedDate": "2026-08-05",
  "receivedDate": null,
  "status": "EXPECTED",
  "notes": "",
  "createdAt": "...",
  "updatedAt": "..."
}
```

`accountId` e `receivedDate` são `null` em `EXPECTED` na criação. Novos recebimentos **não** preenchem o cabeçalho — a conta e a data ficam em `income_movements` (**D76-A**). Colunas legadas permanecem para backfill/transição.

Na criação e no `PUT`, propriedades desconhecidas no JSON — inclusive `userId`, `id`, `status`, `accountId`, `receivedDate`, `createdAt` e `updatedAt` — são rejeitadas (**400**, `VALIDATION_ERROR`).

**Parte 2 (D89 — implementado):** `POST` / `PUT` aceitam `responsibleType` / `responsibleName`; `GET /incomes` e `GET /incomes/{id}` os devolvem.


# 33. Receita

Endpoint:

GET /api/v1/incomes/{id}

Isolamento: **404** se a receita não existir ou não for do usuário autenticado. UUID inválido no path: **400**.


# 34. Criar receita

Endpoint:

POST /api/v1/incomes


Request:

```json
{
  "categoryId": "...",
  "description": "Salário",
  "amount": 5400.00,
  "expectedDate": "2026-08-05",
  "notes": ""
}
```

Regras vigentes (Fase 17):

- criação resulta em `EXPECTED`, com `accountId` e `receivedDate` nulos;
- conta e data de recebimento entram em `POST /receipts` (`income_movements`);
- categoria obrigatória, do usuário, ativa e do tipo `INCOME` (RN031, RN033);
- valor > 0;
- `responsibleType` / `responsibleName` opcionais (**D89** / RN306 — **implementado**).

**Histórico Fase 6 (superado):** a conta e a data entravam somente em `POST /receive` (endpoint **removido** — **D74-A**).

Response **201**.


# 35. Atualizar receita

Endpoint:

PUT /api/v1/incomes/{id}

Somente receita `EXPECTED`. Campos do contrato de criação (substituição dos campos permitidos).

Receita `RECEIVED`: rejeitar (**400**, `BUSINESS_RULE_VIOLATION`). Correção: estornar → editar → receber novamente (RN202).

Receita `CANCELLED`: rejeitar nesta fase.

**Parte 2 (D93 / D79 — implementado):** PUT continua só em `EXPECTED` (inclui responsável). Se `RECEIVED` voltar a `EXPECTED` por acréscimo, o PUT volta a ser permitido nas regras de `EXPECTED`. Sem exceção para editar responsável em `RECEIVED`. Se existir **qualquer** linha em `income_movements` (inclusive `REVERSED`), alterar `amount` é rejeitado. Demais campos cadastrais não financeiros de `EXPECTED` seguem as regras vigentes.


# 36. Receber receita — **SUPERADO (Fase 6 — removido na Parte 2)**

**Histórico Fase 6.** Endpoint removido: `POST /api/v1/incomes/{id}/receive`.

Caminho canônico: `POST /api/v1/incomes/{id}/receipts` (§67A).


# 36.1 Estornar receita — **SUPERADO (Fase 6 — removido na Parte 2)**

**Histórico Fase 6.** Endpoint removido: `POST /api/v1/incomes/{id}/reverse`.

Caminho canônico: `POST /api/v1/incomes/{id}/movements/{movementId}/reverse` (§67A). Estorno de RECEIPT mantém RN200 (**D80-A**).


# 37. Cancelar receita

Endpoint:

POST /api/v1/incomes/{id}/cancel

Somente `EXPECTED` → `CANCELLED`.

O cancelamento inutiliza a duplicata. O registro permanece para histórico. Não representa mais receita pendente. Não pode ser recebida nesta fase.

Não há efeito financeiro a desfazer (`EXPECTED` não alterava o saldo).

Não tratar este endpoint como estorno. Estorno de recebimento = `POST /movements/{movementId}/reverse` (§67A).

Não há `RECEIVED` → `CANCELLED`. Sem reativação de receita cancelada.

**Parte 2 (D73 — implementado):** cancelar somente sem RECEIPT `ACTIVE`. Sem estorno automático.


# 38. Despesas — Contrato da Fase 7

Contrato da Fase 7. Não implementar cartão, fatura, ciclo nem parcelamento funcional nesta fase.

A Fase 7 opera despesas simples (`ACCOUNT` e `NONE`). Internamente toda despesa possui parcela 1/1 (`expense_installments`); `payments.installment_id` continua obrigatório. `installmentCount` é propriedade oficialmente aceita na criação (omitido = 1). O consumidor da Fase 7 **não** informa `installmentId` nas operações desta fase. A geração N>1 é o contrato da Fase 8 (seção 47).

`CREDIT_CARD` está fora do contrato operacional da Fase 7 e da Fase 8. Entra na **Fase 9**. O contrato da Fase 8 (parcelas N>1, pagamento por parcela, reverse, adjustments) está na seção 47; os endpoints HTTP da tabela da seção 47 — incluindo adjustments — estão **implementados**.

Todos os endpoints de despesa e pagamento exigem JWT Bearer. Sem token, token inválido, expirado ou usuário desativado: **401** (`UNAUTHORIZED`). Proprietário = usuário autenticado. Recurso de outro usuário ou UUID inexistente: **404** (`NOT_FOUND`). UUID inválido no path: **400**. Propriedades JSON desconhecidas — inclusive `userId`, `id`, `status`, `overdue`, `installmentId`, `createdAt`, `updatedAt`, `installments` — são rejeitadas (**400**, `VALIDATION_ERROR`). Na Fase 7/8, `creditCardId` também é desconhecida. Na **Fase 9**, `creditCardId` é propriedade oficial quando `paymentMethod = CREDIT_CARD` (obrigatório nesse caso; cartão ativo do usuário). `installmentCount` **não** é propriedade desconhecida: é propriedade oficialmente aceita na criação (omitido = 1; se informado, deve ser `> 0`). `FAIL_ON_UNKNOWN_PROPERTIES` continua rejeitando o que não pertence ao contrato.


Endpoint:

GET /api/v1/expenses


Filtros:

startDate — inclusive; a despesa entra no intervalo quando **pelo menos uma** parcela tem `due_date` no intervalo (RN226). Em 1/1 isso equivale ao `dueDate` da despesa.

endDate — inclusive; mesma regra de `startDate` (datas das parcelas, não somente `expenses.due_date`)

status — status persistido (`OPEN`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED`); não filtrar por `OVERDUE`

categoryId

accountId — `expenses.account_id` (despesas `ACCOUNT`; despesas `NONE` não entram neste filtro, mesmo após pagamento)

responsibleType

paymentMethod — Fase 7/8: `ACCOUNT` ou `NONE`. Fase 9: também `CREDIT_CARD`

page — padrão `0`

size — padrão `20`

Ordenação fixa: `createdAt` crescente (mesmo padrão da Fase 6). Não há `sort`/`direction` nesta fase. Fase 7/8: sem filtro `creditCardId`. Fase 9: filtro `creditCardId` permitido.


Response de listagem paginada: `items`, `page`, `size`, `totalItems`, `totalPages`. Não expor `content` / `totalElements` do Spring Data.


Response de item (criação, consulta, listagem, edição e ações):

```json
{
  "id": "...",
  "categoryId": "...",
  "accountId": "...",
  "creditCardId": null,
  "description": "Aluguel",
  "totalAmount": 1500.00,
  "expenseDate": "2026-08-01",
  "dueDate": "2026-08-05",
  "paymentMethod": "ACCOUNT",
  "status": "OPEN",
  "responsibleType": "MINE",
  "responsibleName": null,
  "barcode": null,
  "notes": null,
  "overdue": false,
  "installmentId": "...",
  "createdAt": "...",
  "updatedAt": "..."
}
```

Regras do item:

- `accountId` é obrigatório em `ACCOUNT` e `null` em `NONE` (permanece `null` após o pagamento);
- `responsibleName` é obrigatório quando `responsibleType = OTHER`; nos demais casos é `null` ou omitível na entrada;
- `barcode` é opcional (número de boleto; o sistema não gera boletos);
- `overdue` é derivado (RN218): em 1/1, `true` se status é `OPEN` ou `PARTIALLY_PAID` e `dueDate` < hoje em `America/Sao_Paulo`; em N>1, `true` se existe pelo menos uma parcela overdue segundo RN241. Não usar somente `expenses.due_date` para N>1. `PAID`, `CANCELLED` e `REFUNDED` nunca são overdue;
- `installmentId` identifica a parcela interna 1/1 (rastreabilidade); a Fase 7 não expõe CRUD de parcelas;
- a lista de pagamentos **não** vem neste response; usar `GET /api/v1/expenses/{id}/payments`.


# 39. Criar despesa

Endpoint:

POST /api/v1/expenses


Request (`ACCOUNT`):

```json
{
  "categoryId": "...",
  "description": "Aluguel",
  "totalAmount": 1500.00,
  "expenseDate": "2026-08-01",
  "dueDate": "2026-08-05",
  "paymentMethod": "ACCOUNT",
  "accountId": "...",
  "responsibleType": "MINE",
  "barcode": "123456789",
  "notes": ""
}
```


Request (`NONE`):

```json
{
  "categoryId": "...",
  "description": "Internet",
  "totalAmount": 120.00,
  "expenseDate": "2026-08-01",
  "dueDate": "2026-08-10",
  "paymentMethod": "NONE",
  "responsibleType": "OTHER",
  "responsibleName": "Condomínio",
  "barcode": "23791...",
  "notes": ""
}
```

Regras da Fase 7:

- status inicial sempre `OPEN`;
- criação **não** gera `payments` e **não** altera saldo;
- o backend cria a parcela 1/1 (`installmentNumber = 1`, `totalInstallments = 1`, `amount = totalAmount`, `dueDate` da despesa, `invoiceId` nulo);
- `paymentMethod` somente `ACCOUNT` ou `NONE`; `CREDIT_CARD` é rejeitado (**400**, `BUSINESS_RULE_VIOLATION`);
- `ACCOUNT`: `accountId` obrigatório; conta do usuário e ativa;
- `NONE`: `accountId` ausente ou `null`; se enviado com valor, rejeitar;
- categoria obrigatória, do usuário, ativa, tipo `EXPENSE`;
- `totalAmount` > 0;
- `responsibleType` obrigatório (`MINE`, `GIULIA`, `EDERSON`, `ELISIANE`, `OTHER`);
- `OTHER` exige `responsibleName`;
- `barcode` e `notes` opcionais; em branco são persistidos como `null` (mesmo padrão da Fase 6);
- `installmentCount` é propriedade oficialmente aceita na criação: omitido = `1`; se informado, deve ser `> 0`. Não é propriedade desconhecida. A geração N>1 é o contrato da Fase 8 (seção 47); o código da Fase 7 ainda opera 1/1.

Response **201**.


# 40. Consultar despesa

Endpoint:

GET /api/v1/expenses/{id}

Isolamento: **404** se a despesa não existir ou não for do usuário autenticado.


# 41. Atualizar despesa

Endpoint:

PUT /api/v1/expenses/{id}

Somente despesa `OPEN`. Body: os mesmos campos cadastrais do contrato de criação (substituição), **exceto** `installmentCount` — a quantidade de parcelas é imutável; enviar `installmentCount` no `PUT` é propriedade inválida (**400**).

A parcela 1/1 é atualizada para permanecer consistente (`amount`, `due_date`). Este `PUT` é cadastral, não financeiro (RN217, RN245). Enquanto a despesa estiver `OPEN` e for 1/1, o total pode ser alterado. N>1: não redistribuir; quantidade imutável; `amount` de parcela segue RN227.

`PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED`: rejeitar (**400**, `BUSINESS_RULE_VIOLATION`). Correção após pagamento: `POST /refund` (terminal `REFUNDED`) e, se necessário, nova despesa.


# 42. Pagar despesa

Endpoint:

POST /api/v1/expenses/{id}/pay


Request:

```json
{
  "accountId": "...",
  "amount": 200.00,
  "paymentDate": "2026-08-10",
  "notes": ""
}
```

O consumidor **não** envia `installmentId`. O Service localiza a parcela 1/1, cria o `payment` associado e atualiza status da parcela e da despesa.

Despesa `CREDIT_CARD` **não** pode ser paga por este endpoint nem por `POST .../installments/{installmentId}/payments`. Liquidação somente via `POST /api/v1/invoices/{id}/payments`.

Regras:

- somente `OPEN` ou `PARTIALLY_PAID`;
- `amount` > 0; soma dos pagamentos da despesa + `amount` ≤ valor devido;
- não pode deixar o saldo da conta negativo;
- `NONE`: `accountId` obrigatório (conta do usuário, ativa); **não** preenche `expenses.account_id`; `payment_method` permanece `NONE`;
- `ACCOUNT` (Fase 7 implementada): `accountId` opcional; se omitido, usa `expenses.account_id`; se informado, deve ser igual a `expenses.account_id`; conta diferente: **400**. Esta restrição é a RN210, **SUPERADA** no contrato da Fase 8 (RN228);
- atômico; lock pessimista na despesa e na parcela 1/1 antes de somar e inserir;
- `payments.type` permanece `null`.

Response **200** com a despesa atualizada (`PARTIALLY_PAID` ou `PAID`).


# 43. Cancelar despesa

Endpoint:

POST /api/v1/expenses/{id}/cancel

Somente `OPEN` → `CANCELLED`. Sem efeito de saldo. Despesa e parcela 1/1 passam a `CANCELLED`.

Rejeitar `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED`.


# 44. Estornar despesa

Endpoint:

POST /api/v1/expenses/{id}/refund

Somente `PARTIALLY_PAID` ou `PAID` → `REFUNDED`.

Não volta a `OPEN`. Não apaga `payments`. O saldo deixa de subtrair esses pagamentos.

Rejeitar `OPEN`, `CANCELLED` e `REFUNDED` (inclusive segundo refund).

Não há `POST /api/v1/payments/{id}/reverse` **nesta fase (Fase 7)**. O reverse entra no contrato da Fase 8 (seção 47).

### Fase 9 — despesa `CREDIT_CARD`

O mesmo path. Body **obrigatório**:

```json
{
  "settlement": "CARD_CREDIT"
}
```

ou

```json
{
  "settlement": "ACCOUNT",
  "accountId": "..."
}
```

`settlement` valores oficiais: `CARD_CREDIT`, `ACCOUNT`. `ACCOUNT` exige `accountId` de conta ativa do usuário. Propriedades JSON realmente desconhecidas: **400** (`VALIDATION_ERROR`).

Para despesas `ACCOUNT` ou `NONE`: o body permanece vazio como na Fase 7/8. O campo `settlement` é **aceito estruturalmente** pelo DTO compartilhado do endpoint (necessário para `CREDIT_CARD` na Fase 9), porém sua **utilização é proibida**. Enviar `settlement` **não** é propriedade desconhecida: a API rejeita por regra de negócio — **400**, `code = BUSINESS_RULE_VIOLATION`, mensagem/regra `SETTLEMENT_NOT_ALLOWED`.

Efeitos em `CREDIT_CARD`: RN117. Fatura `PAID` não muda. Pagamentos de fatura não são revertidos.


# 45. Pagamentos da despesa

Endpoint:

GET /api/v1/expenses/{id}/payments

Lista os pagamentos da despesa (array; inclui os originais após `REFUNDED` e após reverse). Não inclui `type`. Inclui `status` (`ACTIVE` / `REVERSED`) — estado do payment na Fase 8 (`payments.status`; não usar `payments.type`).


Item:

```json
{
  "id": "...",
  "expenseId": "...",
  "installmentId": "...",
  "accountId": "...",
  "amount": 200.00,
  "paymentDate": "2026-08-10",
  "status": "ACTIVE",
  "notes": null,
  "createdAt": "..."
}
```

`status` valores oficiais: `ACTIVE`, `REVERSED`. Payment novo: `ACTIVE`. Após `POST /api/v1/payments/{id}/reverse`: `REVERSED`. O fato permanece no histórico.

# 46. Consultar pagamento

Endpoint:

GET /api/v1/payments/{id}

Isolamento: **404** se o pagamento não existir ou não for do usuário autenticado.


# 47. Contrato da Fase 8 — Parcelas, payments, adjustments e reverse

**Implementação concluída** para parcelas, pagamento por parcela, reverse de payment e adjustments (domínio + HTTP). O contrato HTTP de adjustment está nesta seção. A Fase 7 permanece compatível para 1/1 (`POST /expenses/{id}/pay`).

O campo `installmentId` singular na response da despesa **não** representa uma despesa N>1. A consulta das parcelas é pelos endpoints `GET .../installments` (lista) e `GET .../installments/{installmentId}` (detalhe da parcela, com `amount`, `remainingAmount` derivado, `dueDate`, `status`, `overdue`). O JSON aninhado completo da despesa N>1 (array de parcelas + payments/adjustments embutidos no GET da despesa) **não** foi fechado nesta consolidação — não inventar schema além do já previsto pelos endpoints de parcela e de adjustment.

`installmentCount` na criação é propriedade **oficialmente aceita** (não é propriedade desconhecida): se omitido, `1`; se informado, deve ser `> 0`. Propriedades realmente desconhecidas continuam **400** (`FAIL_ON_UNKNOWN_PROPERTIES`). Propriedade JSON `installments` como array de criação manual pelo cliente **não** é o modo oficial (o backend gera as parcelas) e permanece rejeitada.

Listagem `GET /api/v1/expenses`: `startDate`/`endDate` consideram as datas das parcelas (`expense_installments.due_date`). A despesa entra no intervalo quando pelo menos uma parcela tem `due_date` no intervalo. Em N=1 o efeito é equivalente ao `dueDate` atual.

## Endpoints previstos

| Método | Endpoint | Finalidade | Auth |
|---|---|---|---|
| `GET` | `/api/v1/expenses/{id}/installments` | Listar parcelas da despesa | Bearer |
| `GET` | `/api/v1/expenses/{expenseId}/installments/{installmentId}` | Consultar parcela | Bearer |
| `PUT` | `/api/v1/expenses/{expenseId}/installments/{installmentId}` | Edição cadastral `OPEN` (`amount`, `due_date`) | Bearer |
| `POST` | `/api/v1/expenses/{expenseId}/installments/{installmentId}/payments` | Pagar a parcela identificada | Bearer |
| `POST` | `/api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments` | Criar adjustment (`DISCOUNT` / `SURCHARGE`) | Bearer |
| `GET` | `/api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments` | Histórico de adjustments da parcela | Bearer |
| `POST` | `/api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments/{adjustmentId}/reverse` | `ACTIVE` → `REVERSED` | Bearer |
| `POST` | `/api/v1/payments/{id}/reverse` | Payment `ACTIVE` → `REVERSED` | Bearer |
| `POST` | `/api/v1/expenses/{id}/pay` | **Somente 1/1** (legado Fase 7) | Bearer |

Para N>1, `POST /expenses/{id}/pay` **não** escolhe parcela implicitamente. A API deve exigir a identificação da parcela.

## Adjustments — contrato HTTP (fechado)

Adjustment pertence à **parcela** (`expense_installments` 1:N), não à despesa como recurso raiz. Paths aninhados sob a parcela seguem o mesmo padrão de `.../installments/{installmentId}/payments`. Reverse de payment permanece em `/api/v1/payments/{id}/reverse` porque `payments` já é recurso de primeiro nível; reverse de adjustment usa a cadeia aninhada para validar `expenseId` + `installmentId` + `adjustmentId` (ownership composto), análogo ao `PUT` da parcela.

Não existem campos oficiais de subclassificação (`subtype`, juros vs multa vs tarifa como enum). A partir da Fase 9, `reason` (texto) é **obrigatório** em todo adjustment (parcela ou fatura) — RN232 emendada.

### Criar adjustment

```text
POST /api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments
```

Auth: Bearer. Response **201 Created**.

Request:

```json
{
  "type": "DISCOUNT",
  "amount": 10.00,
  "reason": "Desconto concedido"
}
```

- `type` obrigatório: somente `DISCOUNT` ou `SURCHARGE`.
- `amount` obrigatório: `> 0` (escala monetária do projeto).
- `reason` obrigatório (Fase 9): texto de justificativa. Sem enum de motivos.
- Propriedades desconhecidas: **400** `VALIDATION_ERROR`.
- Não enviar: `userId`, `id`, `status`, `createdAt`, `expenseId`, `installmentId`.

Response (fato persistido):

```json
{
  "id": "...",
  "expenseId": "...",
  "installmentId": "...",
  "type": "DISCOUNT",
  "amount": 10.00,
  "reason": "Desconto concedido",
  "status": "ACTIVE",
  "createdAt": "..."
}
```

Não expor `userId`. Não existe `reversedAt` no modelo físico — não inventar. Novo fato: `status = ACTIVE`.

Regras:

- ownership: usuário autenticado; `expenseId` / `installmentId` / cadeia composta; cross-user ou inexistente → **404** `NOT_FOUND` (sem distinguir);
- despesa **não** `CANCELLED` nem `REFUNDED`;
- parcela **não** `CANCELLED` nem `REFUNDED` (RN237: parcela `OPEN` de despesa `REFUNDED` não recebe adjustment);
- fatura do ciclo **não** `PAID` (ajuste de parcela em item de fatura PAID é bloqueado);
- `reason` obrigatório;
- não altera `installment.amount`, `expenses.total_amount` nem saldo de conta;
- altera `obligation` / `remaining` derivados; recalcula status persistido da parcela e da despesa na mesma transação;
- `obligation` resultante deve permanecer `>= 0` e `>= SUM(active payments)`; caso contrário **400** `BUSINESS_RULE_VIOLATION` + rollback;
- atômico; locks: despesa → parcela → consultar fatos ACTIVE → persistir (RN244).

Erros (padrão existente):

| Situação | HTTP | code |
|---|---|---|
| JSON inválido / amount ≤ 0 / type inválido / propriedades desconhecidas | 400 | `VALIDATION_ERROR` |
| despesa/parcela terminal; obligation inválida; parcela não elegível | 400 | `BUSINESS_RULE_VIOLATION` |
| não autenticado | 401 | `UNAUTHORIZED` |
| expense/installment inexistente ou de outro usuário | 404 | `NOT_FOUND` |

### Listar adjustments

```text
GET /api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments
```

Auth: Bearer. Response **200**.

Retorna **array JSON** (mesmo padrão de `GET /expenses/{id}/payments` — **sem** envelope `{ "items": ... }` e **sem** paginação neste histórico).

Inclui fatos `ACTIVE` **e** `REVERSED`. Ordenação: `createdAt ASC`, `id ASC`.

Cada item usa o mesmo shape da response de criação (`id`, `expenseId`, `installmentId`, `type`, `amount`, `status`, `createdAt`).

Ownership / 404: iguais à criação. Consulta do histórico **permanece** permitida se a despesa estiver `CANCELLED` ou `REFUNDED` (fatos não são apagados).

### Reverse de adjustment

```text
POST /api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments/{adjustmentId}/reverse
```

Auth: Bearer. Response **200**. Body: **vazio** (sem `reason` nem outros campos) — mesmo padrão de `POST /payments/{id}/reverse`.

Comportamento: `ACTIVE` → `REVERSED`. Não apaga; não altera `amount`, `type` nem `createdAt`; não cria adjustment compensatório. Remove o efeito do fato no `obligation` / `remaining`; recalcula status da parcela e da despesa. Não movimenta saldo de conta.

Permitido somente se:

- adjustment do usuário, da parcela e da despesa informadas;
- `status = ACTIVE`;
- despesa **não** `CANCELLED` nem `REFUNDED` (RN239).

Proibido: já `REVERSED`; despesa terminal; ownership inválido (**404**).

Se o reverse deixar `obligation` inválida face aos payments ACTIVE (ex.: reverter `SURCHARGE` necessário à cobertura dos payments), **400** `BUSINESS_RULE_VIOLATION` + rollback (RN231).

Locks: despesa → parcela → adjustment → validar → persistir.

Response: mesmo shape do fato, com `"status": "REVERSED"`.

| Situação | HTTP | code |
|---|---|---|
| já REVERSED; despesa CANCELLED/REFUNDED; obligation inválida | 400 | `BUSINESS_RULE_VIOLATION` |
| não autenticado | 401 | `UNAUTHORIZED` |
| expense/installment/adjustment inexistente ou de outro usuário / adjustment não pertence à parcela | 404 | `NOT_FOUND` |

### Obligation, saldo e RN234

```text
obligation = installment.amount + ACTIVE SURCHARGES − ACTIVE DISCOUNTS
remaining  = obligation − ACTIVE PAYMENTS
```

Adjustment **nunca** entra no saldo da conta (RN232, RN240). Somente payments `ACTIVE` válidos.

RN234 (payment + adjustment no mesmo ato): permanece regra de domínio. **Não** há endpoint HTTP composto nesta consolidação; cada `POST .../adjustments` e cada pagamento são atômicos isoladamente. Endpoint composto, se necessário no futuro, exige decisão explícita.

## Regras HTTP resumidas

- Auth, 401, 404, UUID inválido, propriedades desconhecidas: iguais à Fase 7.
- `PUT` da parcela: só `OPEN`; soma dos amounts = `total_amount` ou 400 + rollback; não `PARTIALLY_PAID`/`PAID`/`CANCELLED`/`REFUNDED`.
- Payment: amount > 0; ≤ remaining (RN231); `payments.status = ACTIVE`; `payments.account_id` do usuário, ativa; **não** precisa igualar `expenses.account_id` (RN228).
- Reverse de payment: rejeitar se já `REVERSED` ou se a despesa estiver `REFUNDED`/`CANCELLED`. Reverse de adjustment: mesma filosofia (RN239) — path na tabela acima.
- `payments.type` continua sem valores; não enviar estado em `type`. O estado do payment é `status` (`ACTIVE` / `REVERSED`).
- Cartão / `invoice_id` / faturas: fora.

A edição de parcela × total da despesa para ACCOUNT/NONE está **fechada** (docs/23 §269.2). Parcela já em fatura permanece **DEFERIDA**.


# 52. Transferências (Fase 14 — implementado)

**Status:** contrato oficial `docs/24` §19.5 — `CONCLUÍDA E APROVADA`.

Endpoint:

GET /api/v1/transfers


Filtros oficiais do MVP da Fase 14:

startDate

endDate

accountId (origem ou destino)

**Não** há filtro de `status` no MVP. A listagem pode retornar transferências `ACTIVE` e `REVERSED`. O `status` continua no recurso de cada item. Não criar parâmetro de filtro por antecipação.


Auth: Bearer. Ownership: somente transferências do usuário autenticado.


# 53. Criar transferência

Endpoint:

POST /api/v1/transfers


Request:

```json
{
  "sourceAccountId": "...",
  "destinationAccountId": "...",
  "amount": 500.00,
  "transferDate": "2026-08-10",
  "description": "Transferência"
}
```

Regras (contrato):

- origem e destino: `BANK_ACCOUNT`, ativas, do usuário autenticado, distintas;
- `CASH` e cartões: rejeitar;
- `amount > 0`;
- `transferDate` não futura (`America/Sao_Paulo`); retroativa permitida;
- saldo da origem >= amount;
- status inicial: `ACTIVE`;
- atômica.

Response **201**: transferência criada (incluir `id`, contas, `amount`, `transferDate`, `description`, `status`, `createdAt`).


# 54. Transferência

Endpoint:

GET /api/v1/transfers/{id}


Auth: Bearer. **404** se não existir ou não for do usuário.


# 54A. Reverter transferência

Endpoint:

POST /api/v1/transfers/{id}/reverse


Body: vazio.

Comportamento: `ACTIVE` → `REVERSED`. Exige saldo suficiente na conta debitada pelo movimento inverso. Já `REVERSED` → **400** `BUSINESS_RULE_VIOLATION`. Não apaga. Não edita amount/datas.

Response **200**: transferência com `status = REVERSED`.


# 54B. Definir / alterar saldo inicial (Fase 14 — implementado)

Endpoint oficial (único para definição/alteração após a criação):

PUT /api/v1/accounts/{id}/initial-balance


Request:

```json
{
  "initialBalance": 2500.00
}
```

Regras (RN010 / RN010A):

- conta do usuário autenticado;
- permitido somente se a conta **nunca** teve movimentação financeira efetiva (RN010A);
- após a primeira movimentação (mesmo cancelada/revertida/estornada): **400** `BUSINESS_RULE_VIOLATION`;
- valor normalizado `NUMERIC(19,2)` / `HALF_UP`;
- não usar `PUT /accounts/{id}` (cadastral) para saldo inicial;
- não criar outros endpoints equivalentes.

Response **200**: conta atualizada.


# 54C. Acerto de Saldos — listar / criar (Fase 14 — implementado)

Identificador técnico: `BALANCE_ADJUSTMENT`.

Tabela criada pela migration V28: **`account_balance_adjustments`**.

Não confundir com adjustments de parcela/fatura (`expense_installment_adjustments` / ajustes de fatura).

Endpoints implementados:

```text
GET  /api/v1/accounts/{accountId}/balance-adjustments
POST /api/v1/accounts/{accountId}/balance-adjustments
GET  /api/v1/accounts/{accountId}/balance-adjustments/{id}
```

Request de criação:

```json
{
  "reportedBalance": 1500.00,
  "adjustmentDate": "2026-08-14"
}
```

O cliente **não** envia `adjustmentAmount` nem `calculatedBalance`. O backend calcula:

```text
calculatedBalance = saldo as-of adjustmentDate
adjustmentAmount = reportedBalance − calculatedBalance
```

Regras:

- conta `BANK_ACCOUNT` ou `CASH` **ativa**;
- `reportedBalance >= 0`;
- `adjustmentDate` não futura;
- retroativa permitida;
- status inicial `ACTIVE`;
- o acerto não pode deixar o saldo da conta negativo;
- persistir no fato: `calculatedBalance`, `reportedBalance`, `adjustmentAmount`, data, status, timestamps.

Response **201**: fato do acerto (campos acima + `id`, `accountId`, `status`, `createdAt`).


# 54D. Reverter acerto de saldo

Endpoint:

POST /api/v1/accounts/{accountId}/balance-adjustments/{id}/reverse


Body: vazio.

`ACTIVE` → `REVERSED`. Exige saldo suficiente quando o efeito inverso for saída. Já `REVERSED` → **400**. Sem desreversão.


# 54E. Metas financeiras (Fase 15)

**Status:** contrato oficial `docs/24` §19.6 — **`CONCLUÍDA E APROVADA`**.

Base: `/api/v1/financial-goals`. Auth: Bearer. Ownership: somente recursos do usuário autenticado; mismatch → **404** (sem distinguir). Propriedades JSON desconhecidas → **400** `VALIDATION_ERROR` (`FAIL_ON_UNKNOWN_PROPERTIES`).

## Conceito

Meta = reserva (caixinha) vinculada a **uma** conta. Contribuição classifica dinheiro como reservado (reduz `availableBalance`, não altera `totalBalance`). Resgate devolve à **mesma** conta vinculada.

## Criar meta

```text
POST /api/v1/financial-goals
```

Request:

```json
{
  "accountId": "...",
  "name": "Viagem Chile",
  "description": "Férias de julho",
  "targetAmount": 5000.00,
  "targetDate": "2026-12-20"
}
```

Regras:

- `accountId` obrigatório; conta `BANK_ACCOUNT` ou `CASH` **ativa** do usuário;
- `targetAmount > 0`;
- `targetDate` opcional;
- `name` obrigatório; `description` opcional;
- status inicial: `ACTIVE`;
- **não** aceitar `userId`, `status`, `currentAmount`, `progressPercent`.
- nomes duplicados são permitidos (RN279).

Response **201**: meta com `id`, campos persistidos, `accountId`, `status`, `currentAmount` (`0.00`), `progressPercent` (`0.00`), timestamps.

## Listar metas

```text
GET /api/v1/financial-goals
```

Query: paginação padrão (`page` default `0`, `size` default `20`); filtro opcional `status` (`ACTIVE`, `COMPLETED`, `CANCELLED`). Ordenação: `createdAt` ASC.

`page < 0` ou `size < 1` → **400** `BUSINESS_RULE_VIOLATION`.

Response **200**: página (`items`, `page`, `size`, `totalItems`, `totalPages`); cada item inclui derivados `currentAmount`, `progressPercent`, `accountId`.

## Consultar meta

```text
GET /api/v1/financial-goals/{id}
```

Response **200** ou **404**.

## Editar meta

```text
PUT /api/v1/financial-goals/{id}
```

Somente meta `ACTIVE`. Request (substituição dos campos editáveis):

```json
{
  "name": "Viagem Chile",
  "description": null,
  "targetAmount": 6000.00,
  "targetDate": "2027-01-15"
}
```

Regras:

- `targetAmount > 0`;
- alterar `targetAmount` **não** altera contribuições/resgates/reservado;
- `accountId` **não** editável;
- `COMPLETED` / `CANCELLED` → **400** `BUSINESS_RULE_VIOLATION`.

Response **200**: meta atualizada com derivados recalculados.

## Contribuir

```text
POST /api/v1/financial-goals/{id}/contributions
```

Request:

```json
{
  "amount": 500.00,
  "contributionDate": "2026-08-17",
  "notes": "Primeiro aporte"
}
```

Regras:

- meta `ACTIVE`;
- `amount > 0`;
- `contributionDate` não futura (`America/Sao_Paulo`);
- `availableBalance` da conta vinculada `>= amount`;
- conta vinculada ativa;
- fato imutável; sem `DELETE`; sem reverse nesta fase.
- request **não** recebe `accountId`.

Response **201**:

```json
{
  "contribution": { "id": "...", "goalId": "...", "amount": 500.00, "contributionDate": "2026-08-17", "notes": "Primeiro aporte", "createdAt": "..." },
  "goal": { }
}
```

## Listar contribuições

```text
GET /api/v1/financial-goals/{id}/contributions
```

Response **200**: array JSON (sem envelope paginado), ordenado por `contributionDate` ASC, `createdAt` ASC, `id` ASC. Recurso de outro usuário → **404**.

## Resgatar

```text
POST /api/v1/financial-goals/{id}/redemptions
```

Request:

```json
{
  "amount": 200.00,
  "redemptionDate": "2026-08-17",
  "notes": null
}
```

Regras:

- meta `ACTIVE` **ou** `COMPLETED`;
- `amount > 0`; `amount <= currentAmount`;
- `redemptionDate` não futura;
- retorno **sempre** à conta vinculada; sem escolha de conta;
- resgate **não** altera `status` da meta;
- fato imutável; sem reverse nesta fase;
- rejeitar resgate em `CANCELLED`.
- request **não** recebe `accountId` (propriedade desconhecida → **400** `VALIDATION_ERROR`).

Response **201**:

```json
{
  "redemption": { "id": "...", "goalId": "...", "amount": 200.00, "redemptionDate": "2026-08-17", "notes": null, "createdAt": "..." },
  "goal": { }
}
```

## Listar resgates

```text
GET /api/v1/financial-goals/{id}/redemptions
```

Response **200**: array JSON (sem envelope paginado), ordenado por `redemptionDate` ASC, `createdAt` ASC, `id` ASC. Recurso de outro usuário → **404**.

## Concluir meta

```text
POST /api/v1/financial-goals/{id}/complete
```

Body: vazio.

Regras:

- somente `ACTIVE` → `COMPLETED`;
- permitido independentemente de `currentAmount` vs `targetAmount` (inclusive `currentAmount = 0`);
- terminal; sem reabertura nesta fase.

Response **200**: meta com `status = COMPLETED`.

## Cancelar meta

```text
POST /api/v1/financial-goals/{id}/cancel
```

Body: vazio.

Regras:

- somente `ACTIVE` → `CANCELLED`;
- exige `currentAmount = 0`; caso contrário **400** `BUSINESS_RULE_VIOLATION`;
- terminal.

Response **200**: meta com `status = CANCELLED`.

## Erros comuns

| Situação | HTTP | code |
|---|---|---|
| meta/ contribuição de outro usuário | 404 | `NOT_FOUND` |
| contribuição/resgate/edição em status inválido | 400 | `BUSINESS_RULE_VIOLATION` |
| contribuição em `COMPLETED` / `CANCELLED` | 400 | `BUSINESS_RULE_VIOLATION` |
| resgate em `CANCELLED` | 400 | `BUSINESS_RULE_VIOLATION` |
| edição em `COMPLETED` / `CANCELLED` | 400 | `BUSINESS_RULE_VIOLATION` |
| cancelamento de meta `COMPLETED` | 400 | `BUSINESS_RULE_VIOLATION` |
| saldo disponível insuficiente | 400 | `BUSINESS_RULE_VIOLATION` |
| resgate > currentAmount | 400 | `BUSINESS_RULE_VIOLATION` |
| cancelar com reservado > 0 | 400 | `BUSINESS_RULE_VIOLATION` |
| data futura | 400 | `BUSINESS_RULE_VIOLATION` |
| `targetAmount <= 0` / `amount <= 0` | 400 | `VALIDATION_ERROR` |
| `page < 0` / `size < 1` | 400 | `BUSINESS_RULE_VIOLATION` |
| propriedade JSON desconhecida (ex.: `accountId` no resgate) | 400 | `VALIDATION_ERROR` |
| conta inexistente ou de outro usuário na criação | 404 | `NOT_FOUND` |
| conta inativa na criação/contribuição | 400 | `BUSINESS_RULE_VIOLATION` |

**Não** criar `DELETE` de meta, contribuição ou resgate.


# 55. Faturas

Endpoint:

GET /api/v1/credit-cards/{cardId}/invoices


Filtros:

year

month

status


# 56. Fatura

Endpoint:

GET /api/v1/invoices/{id}


# 57. Fatura atual

Endpoint:

GET /api/v1/credit-cards/{cardId}/invoices/current


# 58. Fatura

Response conceitual:

{
  "id": "...",
  "creditCardId": "...",
  "referenceYear": 2026,
  "referenceMonth": 8,
  "closingDate": "2026-08-10",
  "dueDate": "2026-08-20",
  "status": "CLOSED",
  "totalAmount": 2000.00,
  "paidAmount": 1200.00,
  "remainingAmount": 800.00
}


`status` da fatura: `SCHEDULED` | `OPEN` | `CLOSED` | `PAID`. Não usar `PARTIALLY_PAID`.

`dueDate` calculado na criação da fatura (RN099B): se `due_day` > `closing_day`, mesmo mês da `closingDate`; se `due_day` ≤ `closing_day`, mês seguinte; RN098 se o mês não tiver o dia. Não recalcular se o cartão mudar depois.

`totalAmount`, `paidAmount` e `remainingAmount` são derivados na leitura.

Não são colunas de `credit_card_invoices`. Fórmulas: `docs/23-modelo-de-dados.md` seção 263.


# 59. Compras da fatura

Endpoint:

GET /api/v1/invoices/{id}/items


Retorna as parcelas (`expense_installments`) pertencentes à fatura (`invoice_id`).

Não retorna a despesa inteira como se ela pertencesse a um único ciclo.

Uma despesa parcelada pode ter parcelas em outras faturas.


# 60. Fechar fatura

Fechamento **não** é operação funcional normal do usuário. **Não** expor `POST /api/v1/invoices/{id}/close` como ação da API da Fase 9.

O backend fecha via scheduler Spring (RN096A), idempotente. A determinação do ciclo da compra usa a data, não o status OPEN.


# 61. Regra

Fechamento de fatura deve ser uma operação controlada pelo backend.


# 62. Pagamento de fatura

Endpoint:

POST /api/v1/invoices/{id}/payments


Request:

{
  "accountId": "...",
  "amount": 1200.00,
  "paymentDate": "2026-08-20",
  "notes": ""
}


# 63. Pagamentos da fatura

Endpoint:

GET /api/v1/invoices/{id}/payments


# 63A. Reverse de pagamento de fatura

Endpoint da Fase 9 (**implementado**):

POST /api/v1/invoices/{invoiceId}/payments/{paymentId}/reverse

Não DELETE. Proibido se a fatura estiver `PAID`.


# 63B. Ajustes de fatura

Endpoints da Fase 9 (**implementados**):

- `POST /api/v1/invoices/{id}/adjustments` — `type`, `amount`, `reason`
- `GET /api/v1/invoices/{id}/adjustments`
- `POST /api/v1/invoices/{invoiceId}/adjustments/{adjustmentId}/reverse`

Proibido em fatura `PAID`. Rateio RN247A.

`DISCOUNT`: não pode ultrapassar o remaining da fatura.

`SURCHARGE`: além de fatura não `PAID`, exige **remaining > 0**. Se `remaining = 0`, rejeitar por regra de negócio — **400**, `code = BUSINESS_RULE_VIOLATION`, constante `SURCHARGE_REQUIRES_REMAINING`, mensagem `"O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto."`. Não persistir ajuste sem efeito financeiro.


# 64. Parcelamento / negociação de fatura (Fase 13)

**Status:** `CONCLUÍDA E APROVADA` (`docs/24` §19.4 / RN254).

Invariante comum: `contractedTotal = installmentCount × installmentAmount` e `contractedTotal >= financedAmount`. Se `contractedTotal < financedAmount` → **400** `BUSINESS_RULE_VIOLATION` (razão específica, ex. alinhada a `AGREEMENT_CONTRACTED_TOTAL_BELOW_FINANCED_AMOUNT`).

## Nova negociação

`POST /api/v1/invoices/{invoiceId}/agreements`

Request:

```json
{
  "entryAmount": 400.00,
  "accountId": "…",
  "entryPaymentDate": "2026-02-10",
  "installmentCount": 10,
  "installmentAmount": 120.00
}
```

Regras: fatura `CLOSED` com remaining > 0; `0 <= entryAmount < invoiceRemaining`; `entryAmount == invoiceRemaining` → **400** `BUSINESS_RULE_VIOLATION`; **não** antecipa Agreements anteriores; `financedAmount = invoiceRemaining − entryAmount`; settlement da fatura = esse financed; parcelas iguais; 1ª na próxima fatura; fatura → `SETTLED_BY_AGREEMENT`; cria Agreement + expense `CREDIT_CARD` (`total_amount = contractedTotal`).

## Renegociação

`POST /api/v1/invoices/{invoiceId}/renegotiations`

Request:

```json
{
  "entryAmount": 500.00,
  "accountId": "…",
  "entryPaymentDate": "2026-02-10",
  "installmentCount": 10,
  "installmentAmount": 320.00,
  "anticipatedFuturesNetAmount": 900.00
}
```

- **Sem** lista de `agreementId`s (D8).
- `anticipatedFuturesNetAmount`: líquido dos futuros informado pelo banco; `0 ≤ net ≤ futureOriginalAmount` (soma dos remainings futuros dos Agreements `ACTIVE` do cartão, excluindo parcelas da fatura atual). Se não houver futuros, enviar `0`.
- `futuresDiscountAmount = futureOriginalAmount − anticipatedFuturesNetAmount` (desconto **financeiro**).
- `anticipatedFuturesNetAmount` é **incorporação** na nova obrigação — **não** é segundo desconto financeiro (RN254).
- `financedAmount = (invoiceRemaining − entryAmount) + anticipatedFuturesNetAmount`.
- Settlement da fatura = `invoiceSettlementAmount = invoiceRemaining − entryAmount` (não inclui o líquido dos futuros).
- Futuros: remaining → 0 após desconto financeiro + liquidação/transferência do líquido; parcela da fatura atual não duplicada.
- Exemplo oficial e demais regras: `docs/24` RN254.

## Consultas

- `GET /api/v1/invoices/{invoiceId}/agreements`
- `GET /api/v1/agreements/{agreementId}`

## Antecipação de parcela do Agreement

`POST /api/v1/agreements/{agreementId}/installments/{installmentId}/anticipate`

Request:

```json
{
  "accountId": "…",
  "amount": 150.00,
  "paymentDate": "2026-03-01",
  "settled": true
}
```

`settled` só neste fluxo (não no pay de fatura nem no pay ACCOUNT/NONE).

### Legado obsoleto

Não implementar: `POST/GET /api/v1/invoices/{id}/installments` com lista amount/dueDate.

Não confundir com `GET /api/v1/invoices/{id}/items` (Fase 9).


# 65. Consulta de parcelas de parcelamento de fatura (legado)

**Obsoleto.** Substituído por Agreements (§64).


# 66. Contas a pagar (Fase 16)

**Status:** contrato oficial `docs/24` §19.7 — `CONCLUÍDA E APROVADA`.

Endpoint único:

```text
GET /api/v1/payables
```

Auth: Bearer obrigatório. Sem token / token inválido / expirado / usuário desativado → **401** `UNAUTHORIZED`. Dono = usuário autenticado (JWT). Não existe `userId` na query. Recurso de outro usuário não é distinguido: filtros que não casam com o dono devolvem lista vazia (**200**), sem vazar existência.

Não existem `GET /payables/{id}`, POST, PUT, PATCH nem DELETE neste recurso.

Propriedades JSON não se aplicam (GET sem body). Query params desconhecidos: rejeitar (**400** `VALIDATION_ERROR`) — o mesmo rigor de `FAIL_ON_UNKNOWN_PROPERTIES` nas escritas, aplicado aos nomes de parâmetro não oficiais.

Visão derivada. Sem tabela `payables`. Remaining e totais **não** são colunas.

## Conceito

Linha elegível:

- parcela `ACCOUNT` / `NONE` com `remainingAmount > 0`; **ou**
- fatura `SCHEDULED` / `OPEN` / `CLOSED` com `remainingAmount > 0`.

Despesa `CREDIT_CARD` e parcela de cartão **não** são linhas. `remainingAmount = 0` nunca aparece.

## Query params

| Param | Obrigatório | Default | Semântica |
|---|---|---|---|
| `startDate` | não | omitido | `LocalDate` inclusive; `due_date` da **linha** ≥ `startDate`. **Não** RN226. |
| `endDate` | não | omitido | `LocalDate` inclusive; `due_date` da linha ≤ `endDate`. |
| `year` | não | omitido | ano do **mês selecionado**; exige `month` |
| `month` | não | omitido | 1–12; exige `year`. "Mês atual" = este par, **não** o mês do relógio. Ex.: hoje 2026-08-17 + `year=2026&month=10` → outubro/2026. |
| `includeWithoutDueDate` | não | `false` | com filtro temporal, inclui linhas sem `due_date` se `true` |
| `status` | não | omitido | um ou mais status persistidos da origem, separados por vírgula (D38). Ex.: `status=OPEN,PARTIALLY_PAID`. Valores: `OPEN`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED`, `SCHEDULED`, `CLOSED`, `SETTLED_BY_AGREEMENT`. Não existe status `OVERDUE`. |
| `overdue` | não | omitido | `true` \| `false`; separado de `status` |
| `creditCardId` | não | omitido | UUID do cartão; casa linhas `INVOICE` daquele cartão |
| `withoutCreditCard` | não | `false` | `true` = somente linhas INSTALLMENT (ACCOUNT/NONE) |
| `categoryId` | não | omitido | UUID; **somente** INSTALLMENT. Faturas **não** são filtradas por categoria (continuam no resultado se os demais filtros permitirem). |
| `responsibleType` | não | omitido | `MINE`, `GIULIA`, `EDERSON`, `ELISIANE`, `OTHER`; **somente** INSTALLMENT. Faturas não são filtradas por responsável. |
| `search` | não | omitido | texto simples (contém, case-insensitive). INSTALLMENT: `description`, `notes`, `barcode`. INVOICE: nome do cartão. |
| `sort` | não | `dueDate` | `name`, `purchaseDate`, `dueDate`, `originalAmount`, `remainingAmount`, `status`, `paidAmount` |
| `direction` | não | `asc` | `asc` \| `desc` (campo principal). Desempate **sempre** `id ASC`. |
| `page` | não | `0` | ≥ 0 |
| `size` | não | `20` | 1–100 |

Filtros combinam por interseção. `year` sem `month` (ou o inverso) → **400** `VALIDATION_ERROR`. `startDate` > `endDate` → **400** `VALIDATION_ERROR`. `sort` / `direction` / `status` / `overdue` / `month` inválidos → **400** `VALIDATION_ERROR`. `page < 0`, `size < 1` ou `size > 100` → **400** `BUSINESS_RULE_VIOLATION`.

Período: `due_date` da linha. Sem `startDate`/`endDate`/`year`/`month`: todas as elegíveis, inclusive futuras. Linha sem `due_date`: entra na consulta geral; com período, só se `includeWithoutDueDate=true`. No modelo vigente todas as parcelas e faturas têm `due_date`; o parâmetro não autoriza migration.

`creditCardId` + `withoutCreditCard=true` pode resultar vazio (combinação válida).

## Overdue

Derivado (`America/Sao_Paulo`). Não persistido.

- INSTALLMENT: `remainingAmount > 0`, possui `due_date`, `due_date` < hoje.
- INVOICE: `remainingAmount > 0`, possui `due_date`, status `OPEN` ou `CLOSED`, `due_date` < hoje. `SCHEDULED` nunca overdue.

## Response 200

```json
{
  "items": [
    {
      "id": "...",
      "type": "INSTALLMENT",
      "expenseId": "...",
      "creditCardId": null,
      "categoryId": "...",
      "accountId": "...",
      "paymentMethod": "ACCOUNT",
      "name": "Aluguel",
      "purchaseDate": "2026-08-01",
      "dueDate": "2026-08-10",
      "originalAmount": 1500.00,
      "paidAmount": 500.00,
      "remainingAmount": 1000.00,
      "status": "PARTIALLY_PAID",
      "overdue": false,
      "responsibleType": "MINE",
      "responsibleName": null
    },
    {
      "id": "...",
      "type": "INVOICE",
      "expenseId": null,
      "creditCardId": "...",
      "categoryId": null,
      "accountId": null,
      "paymentMethod": null,
      "name": "Nubank",
      "purchaseDate": "2026-08-10",
      "dueDate": "2026-08-20",
      "originalAmount": 2000.00,
      "paidAmount": 200.00,
      "remainingAmount": 1800.00,
      "status": "CLOSED",
      "overdue": false,
      "responsibleType": null,
      "responsibleName": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 2,
  "totalPages": 1,
  "totalRemaining": 2800.00,
  "totalOriginal": 3500.00,
  "totalPaid": 700.00
}
```

Regras do item:

- `type`: `INSTALLMENT` \| `INVOICE`;
- `id`: id da parcela ou da fatura;
- `name`: descrição da despesa ou nome do cartão;
- `purchaseDate`: `expense_date` da despesa ou `closing_date` da fatura;
- `originalAmount` / `paidAmount` / `remainingAmount`: `docs/24` §19.7.4 (fontes oficiais existentes; **não** nova fórmula);
- sem arrays de payments, adjustments ou parcelas filhas;
- valores monetários escala 2, `HALF_UP`.

`totalRemaining`, `totalOriginal` e `totalPaid` somam o **universo filtrado**, não só `items`. `totalItems` / `totalPages` idem (paginação clássica).

Lista vazia: **200**, `items: []`, totais `0.00`, `totalItems: 0`, `totalPages: 0`.

## Erros

| Situação | HTTP | `code` |
|---|---|---|
| sem Bearer / token inválido | 401 | `UNAUTHORIZED` |
| query param desconhecido ou valor inválido (UUID, enum, data, `year`/`month` incompleto, `startDate` > `endDate`) | 400 | `VALIDATION_ERROR` |
| `page < 0` / `size < 1` / `size > 100` | 400 | `BUSINESS_RULE_VIOLATION` |

Não usar 404 para lista vazia.

## Fora desta fase

Dashboard, projeções, frontend, escritas, `GET /payables/{id}`, migration, tabela `payables`.


# 67. Contas a receber (Fase 17 — Parte 1)

**Status:** `docs/24` §19.8 — Parte 1 **CONCLUÍDA E APROVADA**. Parte 2 **implementada** (`§67A`). A Fase 17 inteira **não** está concluída. Endpoint existente:

```text
GET /api/v1/receivables
```

Pacote: `receivables`. **Não** utilizar `/api/v1/accounts-receivable`.

Auth: Bearer obrigatório. Sem token / token inválido / expirado / usuário desativado → **401** `UNAUTHORIZED`. Dono = usuário autenticado (JWT). Não existe `userId` na query. Recurso de outro usuário não é distinguido: filtros que não casam com o dono devolvem lista vazia (**200**), sem vazar existência.

Não existem `GET /receivables/{id}`, POST, PUT, PATCH nem DELETE neste recurso. Receber / estornar / cancelar / editar permanecem em `/api/v1/incomes`.

Propriedades JSON não se aplicam (GET sem body). Query params desconhecidos: rejeitar (**400** `VALIDATION_ERROR`) — o mesmo rigor de payables.

Visão derivada de `Income`. Sem tabela `receivables`. Sem alias `dueDate`. `expectedDate` continua obrigatória no cadastro de Income.

## Conceito

Linha elegível: um `Income` `EXPECTED` ou `RECEIVED` do usuário autenticado.

`CANCELLED` nunca aparece. Sem `status`: somente `EXPECTED` (futuras + vencidas). `RECEIVED` só entra com `status=RECEIVED`.

`overdue` é derivado (`America/Sao_Paulo`): `status = EXPECTED` **e** `expectedDate` < hoje. Não persistido. Não existe status `OVERDUE`.

## Query params

| Param | Obrigatório | Default | Semântica |
|---|---|---|---|
| `startDate` | não | omitido | `LocalDate` inclusive; exige `dateType` quando presente |
| `endDate` | não | omitido | `LocalDate` inclusive; exige `dateType` quando presente |
| `dateType` | se houver período | omitido | `EXPECTED` (filtra `expectedDate`) \| `RECEIVED` (filtra `receivedDate`) |
| `status` | não | omitido = `EXPECTED` | um valor: `EXPECTED` \| `RECEIVED`. `CANCELLED` não é aceito |
| `overdue` | não | omitido | `true` \| `false`; separado de `status` |
| `categoryId` | não | omitido | UUID da categoria |
| `accountId` | não | omitido | UUID da conta. `EXPECTED` tem `accountId` nulo → conjunto vazio possível (**200**, não 400) |
| `responsibleType` | não | omitido | `MINE`, `GIULIA`, `EDERSON`, `ELISIANE`, `OTHER` |
| `responsibleName` | não | omitido | igualdade com `incomes.responsible_name` |
| `sort` | não | `expectedDate` | `amount`, `expectedDate`, `receivedDate`, `description`, `status`, `createdAt` |
| `direction` | não | `asc` | `asc` \| `desc`. Desempate **sempre** `id ASC` |
| `page` | não | `0` | ≥ 0 |
| `size` | não | `20` | 1–100 |

**Não** existem `year`, `month` nem `search`.

Filtros combinam por interseção quando compatíveis. `startDate` > `endDate` → **400** `VALIDATION_ERROR`. Período sem `dateType` → **400** `VALIDATION_ERROR`. `sort` / `direction` / `status` / `overdue` / `dateType` inválidos → **400** `VALIDATION_ERROR`. Combinações incompatíveis (D58 / RN298) **na Parte 1 implementada**: **400** `VALIDATION_ERROR`:

- `status=EXPECTED` (explícito ou padrão) + `dateType=RECEIVED`;
- `status=RECEIVED` + `dateType=EXPECTED`;
- `status=RECEIVED` + `overdue`.

**Parte 2 (D88 / D94 — implementado):** `status=EXPECTED` + `dateType=RECEIVED` **é permitido**. `dateType=RECEIVED` usa `movement_date` de qualquer RECEIPT (ACTIVE ou REVERSED — **D94**).

`page < 0`, `size < 1` ou `size > 100` → **400** `BUSINESS_RULE_VIOLATION`.

## Response 200

```json
{
  "items": [
    {
      "id": "...",
      "categoryId": "...",
      "accountId": null,
      "responsibleType": null,
      "responsibleName": null,
      "description": "Salário",
      "amount": 5400.00,
      "expectedDate": "2026-08-05",
      "receivedDate": null,
      "status": "EXPECTED",
      "overdue": false
    }
  ],
  "summary": {
    "futureAmount": 5400.00,
    "overdueAmount": 0.00,
    "totalReceivableAmount": 5400.00,
    "receivedAmount": 0.00
  },
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

Regras do item:

- `id`: id do `Income`;
- `amount`: valor da duplicata (`incomes.amount`); **não** expor `remainingAmount` nem `receivedAmount` no item **na Parte 1**;
- `expectedDate`: data prevista (obrigatória no cadastro; sem alias `dueDate`);
- `receivedDate`: preenchido só em `RECEIVED` **na Parte 1** (cabeçalho);
- `overdue`: derivado; `true` somente se `EXPECTED` e `expectedDate` < hoje;
- `responsibleType` / `responsibleName`: colunas de `incomes` (nulos enquanto a API de Income da Fase 6 não os gravar);
- valores monetários escala 2, `HALF_UP`.

**Parte 2 (D77-A / D92-B — implementado):** item aditivo — `amount` continua o original; `accruedAmount`, `receivedAmount` e `remainingAmount` derivados (não persistidos). `summary.receivedAmount` = soma dos RECEIPT ACTIVE do universo filtrado (pode ser > 0 no padrão EXPECTED). `futureAmount` / `overdueAmount` somam `remainingAmount`.

`summary.futureAmount` + `summary.overdueAmount` = `summary.totalReceivableAmount`. Os quatro campos do resumo somam o **universo filtrado**, não só `items` e não um global. `totalItems` / `totalPages` idem.

Lista vazia: **200**, `items: []`, resumo `0.00`, `totalItems: 0`, `totalPages: 0`.

## Erros

| Situação | HTTP | `code` |
|---|---|---|
| sem Bearer / token inválido | 401 | `UNAUTHORIZED` |
| query param desconhecido, valor inválido, período sem `dateType`, `startDate` > `endDate`, combinação incompatível | 400 | `VALIDATION_ERROR` |
| `page < 0` / `size < 1` / `size > 100` | 400 | `BUSINESS_RULE_VIOLATION` |

Não usar 404 para lista vazia.

## Fora desta parte

Dashboard, projeções, frontend, PDF/Excel, escritas neste recurso, `GET /receivables/{id}`, migration, tabela `receivables`, alias `dueDate`, `year`/`month`/`search`.

A Parte 2 (movimentações + escrita de responsável) está **implementada** em `docs/24` §19.9 / `docs/25` §67A. Evolução do item/resumo: D77-A / D92-B; filtros: D78-A / D88 / D94.


# 67A. Contas a receber — Fase 17 Parte 2 (implementada)

**Status:** `docs/24` §19.9 — **`CONCLUÍDA E APROVADA`**. Decisões D73–D94 **fechadas** e **implementadas**. Endpoints canônicos implementados; `/receive` e `/reverse` **removidos** (**D74-A**).

## Distinção

| Recurso | Papel |
|---|---|
| `/api/v1/incomes` | cadastro; responsável no POST/PUT/GET (**D89**) |
| `POST /api/v1/incomes/{id}/accruals` | acréscimo — **201** + movimento (**D90**) |
| `POST /api/v1/incomes/{id}/receipts` | recebimento — **201** + movimento |
| `GET /api/v1/incomes/{id}/movements` | histórico paginado — **200** |
| `POST /api/v1/incomes/{id}/movements/{movementId}/reverse` | estorno — **200** + movimento `REVERSED` |
| `/api/v1/receivables` | visão somente leitura (Parte 1 existente; totais evoluídos) |

Não misturar cadastro com operação financeira. Não criar escrita em `/receivables`. Não criar `GET /receivables/{id}`. POST de criação **não** é idempotente.

Legado Fase 6 (**D74-A** — **removido**): `POST /incomes/{id}/receive` e `POST /incomes/{id}/reverse`. Caminho canônico: §67A. Sem reverse em massa.

Request de acréscimo: `{ "amount", "date" }`. Request de recebimento: `{ "amount", "date", "accountId" }`. Response da movimentação inclui pelo menos: `id`, `incomeId`, `type` (`ACCRUAL` \| `RECEIPT`), `status` (`ACTIVE` \| `REVERSED`), `amount`, `movementDate`, `accountId` (nulo em ACCRUAL), `createdAt`, `updatedAt`, `reversedAt`.

`GET /movements`: envelope `items`, `page`, `size`, `totalItems`, `totalPages`. Ordenação: `movementDate ASC`, `id ASC`. Sem filtros extras nesta parte. O histórico **não** inclui linha para `incomes.amount` (**D75-A**).

Auth: Bearer. Ownership: **404**. Conta de outro usuário / inativa no RECEIPT: padrão de `/receive` (ativa no create). Reverse de RECEIPT: conta owned, não precisa ativa (**D80**). Datas futuras → **400** (**D82**). Recebimento > remaining, remaining = 0, `CANCELLED`, estorno já `REVERSED`, estorno de ACCRUAL com remaining < 0, cancelar com RECEIPT ACTIVE, `PUT amount` após qualquer movimento: **400** `BUSINESS_RULE_VIOLATION`.

`POST` / `PUT /incomes`: `responsibleType` / `responsibleName` (PUT só `EXPECTED`). `GET` os devolve.

## Visão `GET /api/v1/receivables` (evolução)

Item aditivo (**D77-A**): `amount` (original), `accruedAmount`, `receivedAmount`, `remainingAmount`. Não persistidos.

Resumo (**D92-B**): `futureAmount` / `overdueAmount` = soma de `remainingAmount`; `receivedAmount` = soma dos RECEIPT ACTIVE do universo filtrado (pode ser > 0 no padrão EXPECTED); `totalReceivableAmount` = future + overdue. Agregação no banco.

Filtros (**D78-A** / **D88** / **D94**): `dateType=RECEIVED` usa `movement_date` de **qualquer** RECEIPT (ACTIVE ou REVERSED); `accountId` = pelo menos um RECEIPT ACTIVE na conta; `EXPECTED` + `dateType=RECEIVED` permitido.

## Fora desta parte

Frontend, dashboard, PDF/Excel, projeções, over-receipt, tabela `receivables`, exclusão física.


# 68. Projeções

Endpoint:

GET /api/v1/projections


Query:

startDate

endDate


# 69. Projeção

Response conceitual:

{
  "startDate": "2026-09-01",
  "endDate": "2026-12-31",
  "openingBalance": 5000.00,
  "projectedIncome": 15000.00,
  "projectedExpenses": 12000.00,
  "projectedBalance": 8000.00
}


# 70. Projeção mensal

Endpoint:

GET /api/v1/projections/monthly


Query:

startDate

endDate


# 71. Dashboard

Endpoint:

GET /api/v1/dashboard


# 72. Dashboard

Deve fornecer informações suficientes para:

saldo;

receitas;

despesas;

faturas;

contas a pagar;

projeções.


# 73. Dashboard

Response conceitual:

{
  "balance": 5000.00,
  "income": {
    "received": 5400.00,
    "expected": 3000.00
  },
  "expenses": {
    "paid": 2000.00,
    "open": 1500.00
  },
  "creditCards": {
    "openInvoices": 2500.00
  }
}


# 74. Dashboard mensal

Endpoint:

GET /api/v1/dashboard/monthly


Query:

year

month


# 75. Gráficos

A API deve fornecer dados agregados necessários para gráficos.


# 76. Gastos por categoria

Endpoint:

GET /api/v1/reports/expenses-by-category


Query:

startDate

endDate


# 77. Gastos por cartão

Endpoint:

GET /api/v1/reports/expenses-by-card


Query:

startDate

endDate


# 78. Gastos por responsável

Endpoint:

GET /api/v1/reports/expenses-by-responsible


Query:

startDate

endDate


# 79. Receitas por categoria

Endpoint:

GET /api/v1/reports/income-by-category


Query:

startDate

endDate


# 80. Fluxo de caixa

Endpoint:

GET /api/v1/reports/cash-flow


Query:

startDate

endDate


# 81. Relatório de fatura

Endpoint:

GET /api/v1/reports/invoices/{invoiceId}


# 82. Exportação de fatura

Endpoint:

GET /api/v1/reports/invoices/{invoiceId}/pdf


Response:

application/pdf


# 83. Relatório

O PDF deve permitir conferência das despesas da fatura.

Biblioteca oficial: **OpenPDF**.

Deve permitir filtrar por responsável quando aplicável (ex.: cartão de terceiro).


# 84. Filtros

Endpoints de listagem devem possuir paginação quando houver possibilidade de grande volume de dados.


# 85. Paginação

Parâmetros:

page

size


# 86. Ordenação

Parâmetros:

sort

direction


Exemplo:

sort=dueDate

direction=asc


# 87. Padrão de paginação

A API deve retornar metadados de paginação.


Exemplo:

{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 100,
  "totalPages": 5
}

A API não deve expor o modelo `Page` do Spring Data.

Não utilizar `content` nem `totalElements` no contrato externo.


# 88. Respostas

Sucesso deve utilizar códigos HTTP apropriados.


# 89. POST

Criação:

201 Created


# 90. GET

Consulta:

200 OK


# 91. PUT

Substituição completa de recurso quando aplicável.

Resposta padrão: 200 OK com o recurso atualizado.

Não criar `PUT` apenas porque o verbo existe.


# 92. PATCH

Alteração parcial quando aplicável.

Resposta padrão: 200 OK com o recurso atualizado.

Ações de negócio nomeadas (cancelar, estornar, pagar) devem usar `POST /{recurso}/{id}/{acao}`, não PATCH genérico de status.


# 93. DELETE

Não utilizar DELETE como operação padrão para dados financeiros.

Preferir ações explícitas:

POST /api/v1/expenses/{id}/pay

POST /api/v1/expenses/{id}/cancel

POST /api/v1/expenses/{id}/refund

POST /api/v1/incomes/{id}/receipts

POST /api/v1/incomes/{id}/movements/{movementId}/reverse

POST /api/v1/incomes/{id}/cancel

**Histórico Fase 6 (removidos — D74-A):** `POST /api/v1/incomes/{id}/receive` e `POST /api/v1/incomes/{id}/reverse`.

`POST /api/v1/payments/{id}/reverse` entra na Fase 8 (RN238). Não faz parte da Fase 7.

DELETE somente pode existir para recurso não financeiro com regra explícita autorizando a remoção.


# 94. Erro de validação

HTTP:

400 Bad Request


# 95. Não autenticado

HTTP:

401 Unauthorized


# 96. Sem permissão

HTTP:

403 Forbidden


# 97. Registro inexistente

HTTP:

404 Not Found


# 98. Conflito

HTTP:

409 Conflict


# 99. Erro interno

HTTP:

500 Internal Server Error


# 100. Formato de erro

A API deve possuir formato padronizado.

Este é o contrato oficial de erro da V1.

Não substituir automaticamente pelo RFC 7807.

Não criar um segundo formato paralelo.


Exemplo:

{
  "timestamp": "2026-08-12T14:00:00Z",
  "status": 400,
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Não é possível realizar o pagamento.",
  "path": "/api/v1/invoices/..."
}

Códigos usados nas Fases 3, 4 e 5:

- `VALIDATION_ERROR` — 400
- `BUSINESS_RULE_VIOLATION` — 400
- `UNAUTHORIZED` — 401
- `NOT_FOUND` — 404
- `METHOD_NOT_ALLOWED` — 405
- `CONFLICT` — 409
- `INTERNAL_ERROR` — 500


# 101. Erros de validação

Exemplo:

{
  "timestamp": "...",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Dados inválidos.",
  "fields": {
    "amount": "O valor deve ser maior que zero."
  }
}


# 102. Segurança

Não retornar:

passwordHash;

tokens;

informações internas.


# 103. IDs

IDs devem ser UUID.


# 104. Datas

API deve utilizar formato:

YYYY-MM-DD


para datas sem horário.


# 105. Timestamps

Instantes devem utilizar:

ISO 8601


# 106. Valores

Valores monetários devem ser enviados como número decimal JSON.


Exemplo:

1500.50


# 107. Enum

Enums devem utilizar strings.


Exemplo:

"PAID"


e não:

3


# 108. Swagger

A API deve possuir documentação OpenAPI.


# 109. Swagger UI

Disponível no ambiente de desenvolvimento.


# 110. Contrato

Toda alteração pública da API deve atualizar:

OpenAPI;

documentação;

testes.


# 111. Validação

O backend deve validar:

UUID;

valores;

datas;

status;

relacionamentos;

permissões.


# 112. Segurança

Toda operação deve verificar se o recurso pertence ao usuário autenticado.


# 113. Exemplo

GET:

/api/v1/expenses/{id}


não pode retornar uma despesa pertencente a outro usuário.


# 114. Criação

Ao criar:

expense


o backend deve ignorar qualquer:

userId


enviado pelo cliente.


# 115. Usuário

O usuário é obtido através do contexto de autenticação.


# 116. Transações

Operações financeiras compostas devem utilizar transações.


Exemplo:

pagamento de fatura;

estorno de receita recebida.


# 116.1 Estorno de receita

O estorno **não** coloca a receita em `CANCELLED`. Transição: `RECEIVED` → `EXPECTED`.

Fluxo:

1. validar receita e ownership;
2. validar estado `RECEIVED`;
3. identificar a conta que recebeu o valor;
4. desfazer o impacto financeiro;
5. alterar status para `EXPECTED`;
6. limpar `accountId`;
7. limpar `receivedDate`;
8. confirmar transação.


Se qualquer etapa falhar, rollback completo.

A duplicata permanece ativa e pode ser recebida novamente.


# 116.2 Cancelamento de receita

O cancelamento inutiliza a duplicata. Não é estorno.

Fluxo:

1. validar receita e ownership;
2. validar estado `EXPECTED`;
3. alterar status para `CANCELLED`;
4. confirmar transação.


Não há impacto financeiro a desfazer. Não limpar `accountId` / `receivedDate` por analogia com o estorno: já são nulos em `EXPECTED`.

Rejeitar `cancel` sobre `RECEIVED` ou `CANCELLED` nesta fase.


# 117. Pagamento de fatura

Fluxo:

1. validar fatura;
2. validar conta;
3. validar valor;
4. verificar saldo;
5. criar pagamento (`credit_card_invoice_payments`);
6. persistir alocações do rateio nas parcelas;
7. recalcular remainings derivados; no fechamento (scheduler), não neste passo, transitar OPEN→CLOSED/PAID;
8. registrar a saída na conta;
9. aplicar créditos FIFO nas faturas elegíveis (RN246: créditos FIFO; faturas `due_date` ASC, `id` ASC);
10. confirmar transação.


Não gravar `total_amount` / `paid_amount` / `remaining_amount` como colunas. Pagamento parcial **não** transita a fatura para `PARTIALLY_PAID`. Status persistido: `SCHEDULED` / `OPEN` / `CLOSED` / `PAID`. Rateio persistido em alocações. Atualizar remaining das parcelas e o limite usado na mesma transação.


# 118. Transferência (Fase 14 — implementado)

Endpoint implementado: `POST /api/v1/transfers`.

Fluxo:

1. validar conta origem (`BANK_ACCOUNT`, ativa, do usuário);
2. validar conta destino (`BANK_ACCOUNT`, ativa, do usuário, distinta);
3. validar `amount > 0` e `transferDate` não futura;
4. validar saldo da origem;
5. criar transferência `ACTIVE` (um único fato);
6. efeitos no saldo derivado (origem −, destino +) na mesma transação;
7. confirmar.

Reversão: `POST /api/v1/transfers/{id}/reverse` — movimento inverso com checagem de saldo; `ACTIVE` → `REVERSED`.

Não há colunas de saldo a debitar/creditar: o saldo é derivado (RN240).


# 119. Compra no cartão

Fluxo:

1. validar cartão **ativo**;
2. determinar ciclo pela **data da compra** e `closing_day` (`America/Sao_Paulo`; RN095) — não pelo status OPEN;
3. localizar/criar fatura do ciclo (OPEN ou SCHEDULED); `due_date` da fatura nova segundo RN099B;
4. criar despesa `CREDIT_CARD` (`creditCardId`; `dueDate` do request não define vencimento das parcelas — RN099A / RN099B);
5. criar todas as parcelas; cada uma com `invoice_id` da fatura do respectivo ciclo;
6. faturas futuras nascem `SCHEDULED`;
7. atualizar comprometimento derivado (compra acima do limite **permitida**);
8. confirmar transação.

Não recusar por limite insuficiente. Não criar `payments`. Não reduzir saldo da conta.


# 120. Pagamento de despesa (Fase 7)

Endpoint: `POST /api/v1/expenses/{id}/pay`.

Fluxo:

1. validar despesa e ownership (lock pessimista);
2. validar status `OPEN` ou `PARTIALLY_PAID`;
3. localizar a parcela 1/1 (lock pessimista);
4. validar conta (ativa, do usuário). Na Fase 7, se `ACCOUNT`, igual a `expenses.account_id` (RN210). Na Fase 8 essa igualdade é **SUPERADA** (RN228);
5. validar valor > 0 e soma ≤ devido;
6. verificar que o saldo da conta não ficará negativo;
7. criar `payments` (`installment_id` da parcela 1/1, `type` nulo);
8. atualizar status da parcela e da despesa;
9. confirmar transação.

O saldo é derivado: não há coluna a debitar. A inserção do pagamento passa a entrar na fórmula da RN216.

Não exigir `installmentId` no request.


# 120.1 Cancelamento de despesa (Fase 7)

Endpoint: `POST /api/v1/expenses/{id}/cancel`.

Fluxo: validar `OPEN` → `CANCELLED` na despesa e na parcela 1/1. Sem `payments`. Sem efeito de saldo.


# 120.2 Estorno de despesa (Fase 7)

Endpoint: `POST /api/v1/expenses/{id}/refund`.

Fluxo:

1. validar despesa e ownership (lock pessimista);
2. validar status `PARTIALLY_PAID` ou `PAID`;
3. lock da parcela 1/1;
4. alterar despesa e parcela para `REFUNDED`;
5. confirmar transação.

Não apagar `payments`. O saldo deixa de subtrair esses pagamentos. Não voltar a `OPEN`. Não usar `Income.reverse()` como modelo.

Na Fase 9, despesa `CREDIT_CARD` usa o mesmo endpoint com body `settlement` (RN117). Não reverte pagamentos de fatura. Para `ACCOUNT`/`NONE`, `settlement` é estruturalmente conhecido no DTO e proibido por regra de negócio (**400**, `BUSINESS_RULE_VIOLATION`, `SETTLEMENT_NOT_ALLOWED`) — não é propriedade desconhecida.


# 121. Idempotência

Operações que possam ser repetidas acidentalmente devem ser avaliadas para suporte a:

Idempotency-Key


# 122. V1

Não implementar idempotência em todos os endpoints automaticamente.


Implementar somente onde houver justificativa.


# 123. CORS

Configurar CORS para permitir o frontend Angular no ambiente local.


# 124. Desenvolvimento

Frontend:

http://localhost:4200


Backend:

http://localhost:8080


# 125. CORS

Não utilizar:

allowOrigin = *


em produção.


# 126. Documentação

Swagger/OpenAPI deve ser a fonte técnica do contrato da API.


# 127. Documentação

O arquivo:

docs/25-api.md


representa o contrato funcional de alto nível.


# 128. OpenAPI

A especificação detalhada deverá ser gerada/atualizada pelo backend.


# 129. Regra

Não implementar todos os endpoints de uma vez.


# 130. Implementação

Endpoints devem ser implementados por domínio.


Ordem inicial:

1. Auth
2. Users
3. Accounts
4. Categories
5. Credit Cards
6. Incomes
7. Expenses
8. Installments
9. Payments
10. Transfers
10A. Balance Adjustments (Acerto de Saldos)
11. Invoices
12. Financial Goals
13. Projections
14. Dashboard
15. Reports


# 131. Regra

Antes de implementar um endpoint:

1. consultar regras de negócio;
2. consultar modelo de dados;
3. definir request;
4. definir response;
5. definir validações;
6. criar teste;
7. implementar;
8. documentar.


# 132. Regra final

A API deve ser previsível, consistente e orientada ao domínio financeiro.


# 133. Regra final

Não criar endpoints apenas para facilitar a implementação do frontend.


# 134. Regra final

Endpoints devem representar operações e recursos do domínio.


# 135. Regra final

O frontend deve consumir a API e não acessar o PostgreSQL diretamente.