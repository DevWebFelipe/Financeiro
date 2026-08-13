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

Endpoint:

GET /api/v1/accounts


Lista as contas do usuário autenticado.


# 14. Conta

Endpoint:

GET /api/v1/accounts/{id}


Retorna uma conta específica.


# 15. Criar conta

Endpoint:

POST /api/v1/accounts


Request:

{
  "name": "Nubank",
  "type": "BANK_ACCOUNT",
  "initialBalance": 1500.00
}


# 16. Atualizar conta

Endpoint:

PUT /api/v1/accounts/{id}


# 17. Desativar conta

Endpoint:

POST /api/v1/accounts/{id}/deactivate


Não excluir fisicamente.


# 18. Reativar conta

Endpoint:

POST /api/v1/accounts/{id}/activate


# 19. Saldo da conta

Endpoint:

GET /api/v1/accounts/{id}/balance


Response:

{
  "accountId": "...",
  "balance": 1500.00
}


# 20. Extrato da conta

Endpoint:

GET /api/v1/accounts/{id}/statement


Query parameters:

startDate

endDate

page

size


# 21. Cartões

Endpoint:

GET /api/v1/credit-cards


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

{
  "creditLimit": 5000.00,
  "usedLimit": 1500.00,
  "availableLimit": 3500.00
}


# 28. Categorias

Endpoint:

GET /api/v1/categories


Query:

type

active


# 29. Criar categoria

Endpoint:

POST /api/v1/categories


Request:

{
  "name": "Mercado",
  "type": "EXPENSE"
}


# 30. Atualizar categoria

Endpoint:

PUT /api/v1/categories/{id}


# 31. Desativar categoria

Endpoint:

POST /api/v1/categories/{id}/deactivate


# 32. Receitas

Endpoint:

GET /api/v1/incomes


Filtros:

startDate

endDate

status

categoryId

accountId

responsibleType

page

size


# 33. Receita

Endpoint:

GET /api/v1/incomes/{id}


# 34. Criar receita

Endpoint:

POST /api/v1/incomes


Request:

{
  "categoryId": "...",
  "accountId": "...",
  "description": "Salário",
  "amount": 5400.00,
  "expectedDate": "2026-08-05",
  "responsibleType": "MINE",
  "notes": ""
}


# 35. Atualizar receita

Endpoint:

PUT /api/v1/incomes/{id}


# 36. Receber receita

Endpoint:

POST /api/v1/incomes/{id}/receive


Request:

{
  "accountId": "...",
  "receivedDate": "2026-08-05"
}


# 37. Cancelar receita

Endpoint:

POST /api/v1/incomes/{id}/cancel


# 38. Despesas

Endpoint:

GET /api/v1/expenses


Filtros:

startDate

endDate

status

categoryId

accountId

creditCardId

responsibleType

paymentMethod

page

size


# 39. Criar despesa

Endpoint:

POST /api/v1/expenses


Request:

{
  "categoryId": "...",
  "description": "Mercado",
  "totalAmount": 500.00,
  "expenseDate": "2026-08-10",
  "dueDate": "2026-08-10",
  "paymentMethod": "CREDIT_CARD",
  "creditCardId": "...",
  "installments": 5,
  "responsibleType": "MINE",
  "notes": ""
}


# 40. Despesa sem cartão

Request:

{
  "categoryId": "...",
  "description": "Internet",
  "totalAmount": 120.00,
  "expenseDate": "2026-08-01",
  "dueDate": "2026-08-10",
  "paymentMethod": "NONE",
  "responsibleType": "MINE"
}


# 41. Despesa em conta

Request:

{
  "categoryId": "...",
  "description": "Aluguel",
  "totalAmount": 1500.00,
  "expenseDate": "2026-08-01",
  "dueDate": "2026-08-05",
  "paymentMethod": "ACCOUNT",
  "accountId": "...",
  "responsibleType": "MINE"
}


# 42. Despesa

Endpoint:

GET /api/v1/expenses/{id}


# 43. Atualizar despesa

Endpoint:

PUT /api/v1/expenses/{id}


# 44. Cancelar despesa

Endpoint:

POST /api/v1/expenses/{id}/cancel


# 45. Estornar despesa

Endpoint:

POST /api/v1/expenses/{id}/refund


# 46. Parcelas

Endpoint:

GET /api/v1/expenses/{id}/installments


# 47. Parcela

Endpoint:

GET /api/v1/expenses/{expenseId}/installments/{installmentId}


# 48. Atualizar parcela

Endpoint:

PUT /api/v1/expenses/{expenseId}/installments/{installmentId}


Permitir alterar:

amount

dueDate


quando a regra de negócio permitir.


# 49. Pagamento de parcela

Endpoint:

POST /api/v1/expenses/{expenseId}/installments/{installmentId}/payments


Request:

{
  "accountId": "...",
  "amount": 200.00,
  "paymentDate": "2026-08-10",
  "notes": ""
}


# 50. Pagamentos

Endpoint:

GET /api/v1/expenses/{id}/payments


# 51. Pagamento

Endpoint:

GET /api/v1/payments/{id}


# 52. Transferências

Endpoint:

GET /api/v1/transfers


Filtros:

startDate

endDate

accountId


# 53. Criar transferência

Endpoint:

POST /api/v1/transfers


Request:

{
  "sourceAccountId": "...",
  "destinationAccountId": "...",
  "amount": 500.00,
  "transferDate": "2026-08-10",
  "description": "Transferência"
}


# 54. Transferência

Endpoint:

GET /api/v1/transfers/{id}


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


`totalAmount`, `paidAmount` e `remainingAmount` são derivados na leitura.

Não são colunas de `credit_card_invoices`. Fórmulas: `docs/23-modelo-de-dados.md` seção 263.


# 59. Compras da fatura

Endpoint:

GET /api/v1/invoices/{id}/items


Retorna as parcelas (`expense_installments`) pertencentes à fatura (`invoice_id`).

Não retorna a despesa inteira como se ela pertencesse a um único ciclo.

Uma despesa parcelada pode ter parcelas em outras faturas.


# 60. Fechar fatura

Endpoint:

POST /api/v1/invoices/{id}/close


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


# 64. Parcelamento de fatura

Endpoint:

POST /api/v1/invoices/{id}/installments


Request:

{
  "installments": [
    {
      "amount": 450.00,
      "dueDate": "2026-09-20"
    },
    {
      "amount": 450.00,
      "dueDate": "2026-10-20"
    }
  ]
}


# 65. Parcelas da fatura

Endpoint:

GET /api/v1/invoices/{id}/installments


# 66. Contas a pagar

Endpoint:

GET /api/v1/payables


Filtros:

startDate

endDate

status

categoryId

responsibleType


# 67. Contas a receber

Endpoint:

GET /api/v1/receivables


Filtros:

startDate

endDate

status

categoryId


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

POST /api/v1/expenses/{id}/cancel

POST /api/v1/payments/{id}/reverse

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

Códigos usados na Fase 3:

- `VALIDATION_ERROR` — 400
- `UNAUTHORIZED` — 401
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

pagamento de fatura.


# 117. Pagamento de fatura

Fluxo:

1. validar fatura;
2. validar conta;
3. validar valor;
4. verificar saldo;
5. criar pagamento (`credit_card_invoice_payments`);
6. recalcular totais derivados e atualizar o status persistido da fatura;
7. registrar a saída na conta;
8. confirmar transação.


Não gravar `total_amount` / `paid_amount` / `remaining_amount` como colunas. O status (`PARTIALLY_PAID` / `PAID`) é persistido.


# 118. Transferência

Fluxo:

1. validar conta origem;
2. validar conta destino;
3. validar saldo;
4. criar transferência;
5. debitar origem;
6. creditar destino;
7. confirmar transação.


# 119. Compra no cartão

Fluxo:

1. validar cartão;
2. determinar ciclo;
3. localizar/criar fatura;
4. criar despesa;
5. criar parcelas;
6. associar cada parcela à fatura do respectivo ciclo (`expense_installments.invoice_id`);
7. atualizar comprometimento;
8. confirmar transação.


# 120. Pagamento de despesa

Fluxo:

1. validar despesa;
2. validar parcela;
3. validar conta;
4. validar valor;
5. verificar saldo;
6. criar pagamento;
7. atualizar parcela;
8. atualizar despesa;
9. registrar saída;
10. confirmar transação.


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