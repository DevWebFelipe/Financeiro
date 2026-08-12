# API — Financial Control

## 1. Objetivo

Este documento define o contrato inicial da API REST do Financial Control.

A API será implementada em:

Java
+
Spring Boot
+
Spring Security
+
JWT


A API será consumida pelo frontend Angular.


# 2. Princípios

A API deve:

- ser RESTful;
- possuir versionamento;
- utilizar JSON;
- possuir autenticação;
- respeitar isolamento por usuário;
- possuir respostas consistentes;
- possuir validação;
- possuir tratamento padronizado de erros.


# 3. Base URL

Desenvolvimento local:

http://localhost:8080


API:

/api/v1


Exemplo:

http://localhost:8080/api/v1


# 4. Autenticação

A autenticação será baseada em JWT.


Endpoints públicos:

POST /api/v1/auth/register

POST /api/v1/auth/login


Os demais endpoints exigem autenticação.


# 5. Header

Requisições autenticadas utilizarão:

Authorization: Bearer <token>


# 6. Usuário autenticado

O backend deve identificar o usuário através do JWT.


Nunca confiar no:

idUsuario


enviado pelo frontend.


O usuário autenticado deve ser obtido através do contexto de segurança.


# 7. Isolamento

Toda entidade pertencente a um usuário deve possuir relação com o usuário.


Exemplo:

Expense

deve estar relacionada a:

User


# 8. Regra de segurança

Um usuário nunca pode:

consultar;

editar;

excluir;

pagar;

estornar;


dados pertencentes a outro usuário.


# 9. UUID

IDs serão UUID.


Exemplo:

{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}


# 10. Formato de resposta

Respostas de sucesso devem ser simples e previsíveis.


Exemplo:

{
  "id": "...",
  "description": "...",
  "amount": 100.00
}


# 11. Erros

Formato padrão:

{
  "timestamp": "2026-08-12T14:30:00-03:00",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Existem dados inválidos.",
  "path": "/api/v1/expenses",
  "errors": [
    {
      "field": "amount",
      "message": "O valor deve ser maior que zero."
    }
  ]
}


# 12. Códigos de erro

Exemplos:

VALIDATION_ERROR

UNAUTHORIZED

FORBIDDEN

NOT_FOUND

CONFLICT

BUSINESS_RULE_ERROR

INTERNAL_ERROR


# 13. HTTP

Utilizar:

200 OK

201 CREATED

204 NO CONTENT

400 BAD REQUEST

401 UNAUTHORIZED

403 FORBIDDEN

404 NOT FOUND

409 CONFLICT

422 UNPROCESSABLE ENTITY

500 INTERNAL SERVER ERROR


# 14. Auth

## POST /auth/register

Cria um novo usuário.


Request:

{
  "name": "Felipe",
  "email": "felipe@email.com",
  "password": "senha"
}


Response:

201 Created


{
  "id": "...",
  "name": "Felipe",
  "email": "felipe@email.com"
}


Não retornar senha.


# 15. Auth

## POST /auth/login

Realiza autenticação.


Request:

{
  "email": "felipe@email.com",
  "password": "senha"
}


Response:

200 OK


{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "...",
    "name": "Felipe",
    "email": "felipe@email.com"
  }
}


# 16. User

## GET /users/me

Retorna o usuário autenticado.


Response:

{
  "id": "...",
  "name": "Felipe",
  "email": "felipe@email.com"
}


# 17. User

## PUT /users/me

Atualiza dados do usuário.


Não permitir alteração arbitrária de:

id;

password hash;

dados internos.


# 18. User password

## PUT /users/me/password

Altera a senha.


Request:

{
  "currentPassword": "...",
  "newPassword": "..."
}


# 19. Accounts

## GET /accounts

Lista contas do usuário.


Exemplos:

- Banco A;
- Banco B;
- Conta conjunta;
- Carteira pessoal.


Query parameters possíveis:

page

size

sort

active


# 20. Accounts

## POST /accounts

Cria conta.


Request:

{
  "name": "Nubank",
  "type": "BANK_ACCOUNT",
  "initialBalance": 1000.00
}


# 21. Account type

Tipos iniciais:

BANK_ACCOUNT

SAVINGS_ACCOUNT

CASH


# 22. Accounts

## GET /accounts/{id}

Detalhes da conta.


# 23. Accounts

## PUT /accounts/{id}

Atualiza dados cadastrais.


Não permitir alterar saldo diretamente através desse endpoint.


# 24. Accounts

## POST /accounts/{id}/deactivate

Desativa uma conta.


Não excluir histórico financeiro.


# 25. Account balance

Saldo deve ser calculado através das movimentações financeiras.


Evitar permitir alteração arbitrária do saldo.


# 26. Categories

## GET /categories

Lista categorias.


Filtros:

type

active


# 27. Category type

Tipos:

EXPENSE

INCOME


# 28. Categories

## POST /categories

Cria categoria.


Request:

{
  "name": "Mercado",
  "type": "EXPENSE"
}


# 29. Categories

## GET /categories/{id}


# 30. Categories

## PUT /categories/{id}


# 31. Categories

## POST /categories/{id}/deactivate


# 32. Incomes

## GET /incomes

Lista receitas.


Filtros:

startDate

endDate

categoryId

accountId

responsible

status


# 33. Incomes

## POST /incomes

Cria receita.


Request conceitual:

{
  "description": "Salário",
  "amount": 5400.00,
  "date": "2026-08-05",
  "accountId": "...",
  "categoryId": "...",
  "responsible": "MINE"
}


# 34. Income source

A receita deve permitir identificar sua origem.


Exemplos:

Salário

Freelance

Meta

Outros


Pode utilizar categoria para representar a origem.


# 35. Incomes

## GET /incomes/{id}


# 36. Incomes

## PUT /incomes/{id}


# 37. Incomes

## POST /incomes/{id}/cancel

Cancela receita.


Não remover fisicamente do banco.


# 38. Expenses

## GET /expenses

Lista despesas.


Filtros:

startDate

endDate

categoryId

accountId

creditCardId

status

responsible

paymentMethod


# 39. Expenses

## POST /expenses

Cria despesa.


Exemplo:

{
  "description": "Supermercado",
  "amount": 350.00,
  "date": "2026-08-10",
  "dueDate": "2026-08-10",
  "categoryId": "...",
  "responsible": "MINE",
  "paymentMethod": "CREDIT_CARD",
  "creditCardId": "..."
}


# 40. Expense without card

Também deve ser possível:

{
  "paymentMethod": "ACCOUNT"
}


# 41. Expense boleto

Uma despesa pode possuir:

boletoNumber


Exemplo:

{
  "boletoNumber": "836600000..."
}


# 42. Expense due date

Despesas sem pagamento imediato devem possuir:

dueDate


# 43. Expense status

Status iniciais:

OPEN

PAID

PARTIALLY_PAID

REFUNDED

CANCELLED


# 44. Expense

Não utilizar DELETE para remover uma despesa financeira já registrada.


# 45. Cancel expense

## POST /expenses/{id}/cancel

Cancela uma despesa.


A despesa permanece no banco.


# 46. Refund

## POST /expenses/{id}/refund

Registra estorno.


O estorno deve preservar o histórico original.


# 47. Payment

## POST /expenses/{id}/payments

Registra pagamento.


Request:

{
  "amount": 100.00,
  "accountId": "...",
  "paymentDate": "2026-08-12"
}


# 48. Partial payment

O sistema deve permitir pagamentos parciais.


Exemplo:

Despesa:

500


Pagamento:

300


Saldo restante:

200


Status:

PARTIALLY_PAID


# 49. Full payment

Quando:

totalPaid >= amount


a despesa deve ser considerada:

PAID


# 50. Payment validation

Não permitir pagamento maior que o saldo devido sem regra explícita.


# 51. Installments

Despesas parceladas devem permitir:

numberOfInstallments


Exemplo:

{
  "amount": 1200.00,
  "installments": 12
}


# 52. Installment generation

Ao criar uma despesa parcelada:

o sistema deve gerar automaticamente as parcelas futuras.


# 53. Installment

Cada parcela deve possuir:

- número;
- valor;
- vencimento;
- status;
- valor pago;
- valor restante.


# 54. Installment amount

Não assumir que todas as parcelas possuem o mesmo valor.


# 55. Installment editing

Deve ser possível alterar o valor individual de uma parcela.


Exemplo:

Parcela 1:

100


Parcela 2:

100


Parcela 3:

105


# 56. Installment endpoint

## GET /expenses/{id}/installments

Lista parcelas da despesa.


# 57. Installment update

## PUT /installments/{id}

Atualiza uma parcela.


Request:

{
  "amount": 105.00,
  "dueDate": "2026-10-10"
}


# 58. Installment restrictions

Alterações em parcelas já pagas devem possuir regras específicas.


# 59. Future projection

Parcelas futuras devem entrar automaticamente nas projeções.


# 60. Credit cards

## GET /credit-cards

Lista cartões.


# 61. Credit cards

## POST /credit-cards

Cria cartão.


Request:

{
  "name": "Nubank",
  "lastFourDigits": "1234",
  "creditLimit": 5000.00,
  "closingDay": 10,
  "dueDay": 17
}


# 62. Credit card

Campos principais:

name

lastFourDigits

creditLimit

closingDay

dueDay

active


# 63. Credit card

Não armazenar:

número completo;

CVV;

senha;

dados bancários sensíveis.


# 64. Credit card

## GET /credit-cards/{id}


# 65. Credit card

## PUT /credit-cards/{id}


# 66. Credit card

## POST /credit-cards/{id}/deactivate


# 67. Credit card invoice

## GET /credit-card-invoices

Lista faturas.


Filtros:

creditCardId

referenceMonth

status


# 68. Invoice

Uma fatura deve possuir:

- cartão;
- período;
- data de fechamento;
- data de vencimento;
- valor total;
- valor pago;
- valor restante;
- status.


# 69. Invoice status

Inicialmente:

OPEN

CLOSED

PARTIALLY_PAID

PAID

OVERDUE


# 70. Invoice

## GET /credit-card-invoices/{id}

Detalhes da fatura.


# 71. Invoice expenses

## GET /credit-card-invoices/{id}/expenses

Lista despesas pertencentes à fatura.


# 72. Invoice payment

## POST /credit-card-invoices/{id}/payments

Registra pagamento.


Request:

{
  "amount": 1000.00,
  "accountId": "...",
  "paymentDate": "2026-08-12"
}


# 73. Partial invoice payment

Permitir pagamento parcial.


Exemplo:

Fatura:

2000


Pagamento:

1200


Saldo:

800


Status:

PARTIALLY_PAID


# 74. Full invoice payment

Quando o valor devido for quitado:

status:

PAID


# 75. Invoice residual debt

Caso uma fatura não seja totalmente paga:

o saldo restante deve continuar registrado.


# 76. Credit card debt refinancing

A V1 deve permitir representar a dívida restante através de uma despesa específica de:

PARCELAMENTO_CARTAO


Essa funcionalidade deve ser implementada respeitando o histórico da fatura original.


# 77. Card installment refinancing

As parcelas do parcelamento do cartão podem possuir valores diferentes.


# 78. Invoice export

## GET /credit-card-invoices/{id}/export

Gera relatório da fatura.


Formato inicial:

PDF


# 79. Invoice export

O relatório deve permitir visualizar:

- cartão;
- período;
- vencimento;
- total;
- despesas;
- categorias;
- responsável;
- valor de cada despesa;
- total por responsável.


# 80. Responsible

Despesas podem possuir responsável.


Valores iniciais:

MINE

GIULIA

EDERSON

ELISIANE


# 81. Responsible

Os nomes apresentados ao usuário podem ser:

Meu

Giulia

Ederson

Elisiane


# 82. Responsible

A V1 não precisa possuir cadastro de pessoas.


# 83. Future

Posteriormente pode existir tabela:

responsible


sem quebrar o modelo atual.


# 84. Transfers

## GET /transfers

Lista transferências.


# 85. Transfers

## POST /transfers

Cria transferência entre contas.


Request:

{
  "sourceAccountId": "...",
  "destinationAccountId": "...",
  "amount": 500.00,
  "date": "2026-08-12",
  "description": "Transferência"
}


# 86. Transfer

Uma transferência deve atualizar:

conta origem;

conta destino.


# 87. Transfer

A transferência não deve ser contabilizada como:

receita;

despesa.


# 88. Transfer

Deve existir uma única operação lógica de transferência.


# 89. Transfer cancellation

Se implementado:

POST /transfers/{id}/cancel


Deve preservar histórico.


# 90. Goals

## GET /goals

Lista metas.


# 91. Goals

## POST /goals

Cria meta.


Exemplo:

{
  "name": "Presentes de Natal",
  "targetAmount": 2000.00,
  "targetDate": "2026-12-15"
}


# 92. Goal

Campos:

name

targetAmount

currentAmount

targetDate

status


# 93. Goal contribution

## POST /goals/{id}/contributions

Registra contribuição.


Request:

{
  "amount": 200.00,
  "accountId": "...",
  "date": "2026-08-12"
}


# 94. Goal

A meta deve permitir acompanhar:

valor acumulado;

valor restante;

percentual;

prazo.


# 95. Dashboard

## GET /dashboard

Retorna dados resumidos para a tela principal.


# 96. Dashboard

Deve permitir informar o período.


Exemplo:

GET /dashboard?startDate=2026-08-01&endDate=2026-08-31


# 97. Dashboard

Informações iniciais:

- receitas;
- despesas;
- saldo;
- despesas abertas;
- despesas pagas;
- faturas;
- parcelas futuras;
- metas.


# 98. Dashboard

Também deve retornar dados para gráficos.


# 99. Dashboard charts

Exemplos:

despesas por categoria;

receitas x despesas;

despesas por cartão;

evolução mensal.


# 100. Projections

## GET /dashboard/projection

Retorna projeção financeira.


# 101. Projection

A projeção deve considerar:

- receitas previstas;
- despesas previstas;
- parcelas futuras;
- faturas;
- contas abertas.


# 102. Projection

Exemplo:

GET /dashboard/projection?startDate=2026-09-01&endDate=2026-12-31


# 103. Projection

O objetivo é responder perguntas como:

"Quanto já está comprometido em dezembro?"


# 104. Projection

Deve separar:

- confirmado;
- previsto;
- pago.


# 105. Accounts payable

## GET /reports/accounts-payable

Lista contas ainda não pagas.


Filtros:

dueDate;

category;

account;

responsible;

status.


# 106. Accounts receivable

## GET /reports/accounts-receivable

Lista receitas previstas/a receber.


# 107. Monthly summary

## GET /reports/monthly-summary

Retorna resumo mensal.


# 108. Monthly summary

Informações:

- receitas;
- despesas;
- saldo;
- despesas por categoria;
- despesas por cartão;
- receitas por origem.


# 109. Reports

Relatórios devem sempre respeitar:

userId.


# 110. Export

Relatórios poderão futuramente possuir:

PDF;

CSV;

Excel.


V1:

PDF onde houver necessidade.


# 111. Filtering

Filtros financeiros devem aceitar:

startDate

endDate


# 112. Pagination

Listagens devem aceitar:

page

size

sort


# 113. Example

GET /expenses?page=0&size=20&sort=date,desc


# 114. Search

Descrição pode permitir busca textual.


Exemplo:

GET /expenses?search=mercado


# 115. API

Não permitir filtros que possibilitem acesso a outro usuário.


# 116. Transactions

Transações financeiras devem possuir histórico.


# 117. Audit

V1 não terá sistema completo de auditoria.


Mas eventos financeiros importantes devem permanecer registrados.


# 118. Soft delete

Entidades financeiras importantes não devem ser fisicamente removidas.


# 119. Cancellation

Cancelar significa:

manter registro;

alterar status;

preservar histórico.


# 120. Refund

Estorno significa:

manter operação original;

registrar evento de estorno;

ajustar impacto financeiro.


# 121. Payment

Pagamento significa:

registrar movimentação;

associar conta;

registrar valor;

registrar data.


# 122. Payment source

Toda saída financeira real deve estar associada a uma conta.


# 123. Credit card

Compra no cartão não representa imediatamente uma saída da conta bancária.


Ela representa:

compromisso com cartão.


# 124. Invoice payment

Somente o pagamento da fatura representa saída da conta bancária.


# 125. Account balance

Saldo de conta deve refletir movimentações efetivas.


# 126. Future expenses

Despesas futuras não devem reduzir saldo atual.


# 127. Projection

Despesas futuras devem afetar projeção.


# 128. Installments

Parcelas futuras devem afetar projeção.


# 129. Card invoice

Faturas futuras devem afetar projeção.


# 130. Partial payments

Pagamentos parciais devem refletir corretamente:

valor original;

valor pago;

saldo restante.


# 131. Money precision

Valores financeiros devem possuir precisão de duas casas na API.


# 132. JSON money

Exemplo:

{
  "amount": 125.50
}


# 133. Date format

Datas:

YYYY-MM-DD


# 134. DateTime

DateTime:

ISO-8601


# 135. Idempotência

Operações financeiras críticas devem ser avaliadas quanto à necessidade de idempotência.


# 136. Duplicate payment

O sistema deve evitar pagamentos duplicados acidentais.


# 137. Future

Uma estratégia mais completa de idempotency keys pode ser adicionada posteriormente.


# 138. API documentation

Todos os endpoints devem ser documentados no Swagger/OpenAPI.


# 139. OpenAPI

Documentar:

request;

response;

status;

erros;

autenticação.


# 140. Versioning

Qualquer breaking change deve resultar em revisão do contrato.


# 141. API implementation order

A implementação não deve começar pelo Dashboard.


# 142. Ordem sugerida

1. Auth
2. Users
3. Accounts
4. Categories
5. Incomes
6. Expenses
7. Installments
8. Credit Cards
9. Invoices
10. Payments
11. Transfers
12. Goals
13. Dashboard
14. Reports
15. Exportações


# 143. Regra

Cada etapa deve ser implementada e testada antes da próxima.


# 144. Frontend

O frontend não deve assumir endpoints que ainda não existem.


# 145. Backend

O backend não deve criar endpoints apenas porque podem ser úteis no futuro.


# 146. Contrato

Este documento é o contrato inicial.


Durante a implementação:

se uma regra exigir mudança:

1. identificar a necessidade;
2. atualizar este documento;
3. explicar a mudança;
4. implementar.


# 147. Regra final

A API deve representar eventos e operações financeiras reais.


Não criar endpoints simplesmente para manipular tabelas.


Exemplo:

não criar:

PUT /account/balance


Criar operações que representem o domínio.


# 148. Princípio

A API deve responder:

"o que aconteceu financeiramente?"


e não apenas:

"qual tabela quero alterar?"


# 149. Evolução

A API deve estar preparada para receber futuramente:

- investimentos;
- importação bancária;
- Open Finance;
- notificações;
- recorrências;
- relatórios avançados;
- múltiplas formas de parcelamento.


Essas funcionalidades não fazem parte da V1.


# 150. Regra final

Nenhuma funcionalidade futura deve ser implementada apenas porque existe previsão neste documento.

Somente requisitos aprovados para a V1 devem gerar código.