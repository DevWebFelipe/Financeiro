# API REST — Financial Control

## 1. Objetivo

Este documento define o contrato inicial da API REST do Financial Control.

Backend:

Java + Spring Boot

Formato:

JSON

Comunicação:

HTTP/HTTPS

Autenticação:

JWT

Banco:

PostgreSQL


# 2. Princípios

A API deve:

- utilizar REST;
- possuir URLs previsíveis;
- utilizar HTTP status codes corretamente;
- validar entradas;
- retornar erros padronizados;
- respeitar o isolamento por usuário;
- utilizar DTOs;
- nunca expor Entities JPA diretamente.


# 3. URL base

Durante desenvolvimento:

/api


Exemplo:

http://localhost:8080/api


A porta poderá ser alterada através de configuração.


# 4. Versionamento

A V1 utilizará:

/api/v1


Exemplo:

GET /api/v1/accounts


Não utilizar versionamento baseado em query string.


# 5. Content-Type

Requests e responses JSON devem utilizar:

application/json


# 6. Autenticação

A maioria dos endpoints exigirá:

Authorization: Bearer <JWT>


Endpoints públicos:

POST /api/v1/auth/register
POST /api/v1/auth/login


# 7. Registro

Endpoint:

POST /api/v1/auth/register


Request:

{
  "name": "Felipe",
  "email": "felipe@example.com",
  "password": "senha"
}


Response:

201 Created


Não retornar a senha.


# 8. Login

Endpoint:

POST /api/v1/auth/login


Request:

{
  "email": "felipe@example.com",
  "password": "senha"
}


Response:

200 OK


Exemplo:

{
  "token": "...",
  "expiresAt": "..."
}


# 9. Usuário atual

Endpoint:

GET /api/v1/users/me


Retorna dados do usuário autenticado.


Não permitir informar user_id para consultar outro usuário.


# 10. Atualização de usuário

Endpoint:

PUT /api/v1/users/me


Permitir alteração de dados não sensíveis.


# 11. Contas

Endpoint:

GET /api/v1/accounts


Lista contas do usuário autenticado.


# 12. Criar conta

POST /api/v1/accounts


Request:

{
  "name": "Banco A",
  "type": "CHECKING",
  "initialBalance": 2500.00
}


Response:

201 Created


# 13. Buscar conta

GET /api/v1/accounts/{id}


Só pode retornar conta pertencente ao usuário autenticado.


# 14. Atualizar conta

PUT /api/v1/accounts/{id}


# 15. Desativar conta

DELETE /api/v1/accounts/{id}


Para entidades financeiras, preferir desativação/soft delete.


# 16. Saldo da conta

GET /api/v1/accounts/{id}/balance


Response conceitual:

{
  "accountId": "...",
  "balance": 3500.00
}


O valor deve ser calculado pelo backend.


# 17. Transações da conta

GET /api/v1/accounts/{id}/transactions


Filtros:

from
to
type


Exemplo:

GET /api/v1/accounts/{id}/transactions?from=2026-08-01&to=2026-08-31


# 18. Categorias

GET /api/v1/categories


# 19. Criar categoria

POST /api/v1/categories


Request:

{
  "name": "Mercado",
  "type": "EXPENSE",
  "parentId": null
}


# 20. Buscar categoria

GET /api/v1/categories/{id}


# 21. Atualizar categoria

PUT /api/v1/categories/{id}


# 22. Desativar categoria

DELETE /api/v1/categories/{id}


# 23. Categorias padrão

O sistema poderá possuir categorias padrão.

Elas devem ser criadas por seed.


# 24. Receitas

GET /api/v1/incomes


Filtros:

from
to
status
categoryId
accountId


# 25. Criar receita

POST /api/v1/incomes


Request:

{
  "description": "Salário",
  "amount": 5400.00,
  "incomeDate": "2026-08-05",
  "accountId": "...",
  "categoryId": "...",
  "status": "RECEIVED"
}


# 26. Buscar receita

GET /api/v1/incomes/{id}


# 27. Atualizar receita

PUT /api/v1/incomes/{id}


# 28. Receber receita

POST /api/v1/incomes/{id}/receive


Esse endpoint deve efetivar uma receita pendente.


# 29. Cancelar receita

POST /api/v1/incomes/{id}/cancel


Não excluir fisicamente.


# 30. Despesas

GET /api/v1/expenses


Filtros:

from
to
status
categoryId
accountId
creditCardId
responsible


# 31. Criar despesa

POST /api/v1/expenses


Request conceitual:

{
  "description": "Mercado",
  "amount": 350.00,
  "expenseDate": "2026-08-12",
  "dueDate": "2026-08-12",
  "categoryId": "...",
  "accountId": "...",
  "creditCardId": null,
  "responsible": "MINE",
  "boletoNumber": null
}


# 32. Criar despesa no cartão

POST /api/v1/expenses


Exemplo:

{
  "description": "Compra",
  "amount": 1200.00,
  "expenseDate": "2026-08-12",
  "categoryId": "...",
  "creditCardId": "...",
  "responsible": "MINE"
}


# 33. Criar despesa parcelada

Endpoint:

POST /api/v1/expenses/installment


Request:

{
  "description": "Compra notebook",
  "totalAmount": 3600.00,
  "installmentCount": 12,
  "expenseDate": "2026-08-12",
  "categoryId": "...",
  "creditCardId": "...",
  "responsible": "MINE"
}


O backend deve:

1. criar despesa;
2. criar plano;
3. gerar parcelas;
4. associar parcelas às faturas;
5. executar tudo em uma transação.


# 34. Buscar despesa

GET /api/v1/expenses/{id}


# 35. Atualizar despesa

PUT /api/v1/expenses/{id}


Alterações devem respeitar regras de negócio.


# 36. Cancelar despesa

POST /api/v1/expenses/{id}/cancel


Não apagar.


# 37. Estornar despesa

POST /api/v1/expenses/{id}/refund


Request:

{
  "amount": 100.00,
  "refundDate": "2026-08-20",
  "description": "Estorno parcial"
}


# 38. Pagamento de despesa

POST /api/v1/expenses/{id}/payments


Request:

{
  "accountId": "...",
  "amount": 300.00,
  "paymentDate": "2026-08-12"
}


# 39. Parcelas da despesa

GET /api/v1/expenses/{id}/installments


# 40. Atualizar parcela

PUT /api/v1/installments/{id}


Request:

{
  "amount": 150.00,
  "dueDate": "2026-10-10"
}


# 41. Cartões

GET /api/v1/credit-cards


# 42. Criar cartão

POST /api/v1/credit-cards


Request:

{
  "name": "Cartão Ederson",
  "holderName": "Ederson",
  "creditLimit": 5000.00,
  "closingDay": 10,
  "dueDay": 20
}


# 43. Buscar cartão

GET /api/v1/credit-cards/{id}


# 44. Atualizar cartão

PUT /api/v1/credit-cards/{id}


# 45. Desativar cartão

DELETE /api/v1/credit-cards/{id}


# 46. Limite do cartão

GET /api/v1/credit-cards/{id}/limit


Response:

{
  "creditLimit": 5000.00,
  "used": 1200.00,
  "available": 3800.00
}


# 47. Faturas do cartão

GET /api/v1/credit-cards/{id}/invoices


Filtros:

year
month
status


# 48. Buscar fatura

GET /api/v1/invoices/{id}


# 49. Fechar fatura

POST /api/v1/invoices/{id}/close


O endpoint deve validar se a fatura pode ser fechada.


# 50. Reabrir fatura

POST /api/v1/invoices/{id}/reopen


Somente se a regra de negócio permitir.


# 51. Parcelas da fatura

GET /api/v1/invoices/{id}/installments


# 52. Pagamentos da fatura

GET /api/v1/invoices/{id}/payments


# 53. Pagar fatura

POST /api/v1/invoices/{id}/payments


Request:

{
  "accountId": "...",
  "amount": 1200.00,
  "paymentDate": "2026-08-20"
}


# 54. Fatura em aberto

GET /api/v1/invoices/open


Retorna faturas abertas do usuário.


# 55. Faturas vencidas

GET /api/v1/invoices/overdue


Retorna faturas vencidas.


# 56. Contas a pagar

GET /api/v1/payables


Filtros:

from
to
status
categoryId
responsible


Deve retornar despesas pendentes e demais compromissos que representem contas a pagar.


# 57. Transferências

GET /api/v1/transfers


# 58. Criar transferência

POST /api/v1/transfers


Request:

{
  "sourceAccountId": "...",
  "destinationAccountId": "...",
  "amount": 500.00,
  "transferDate": "2026-08-12",
  "description": "Transferência"
}


A operação deve ocorrer em uma única transação.


# 59. Buscar transferência

GET /api/v1/transfers/{id}


# 60. Metas

GET /api/v1/goals


# 61. Criar meta

POST /api/v1/goals


Request:

{
  "name": "Viagem",
  "description": "Viagem de férias",
  "targetAmount": 10000.00,
  "targetDate": "2027-01-15"
}


# 62. Buscar meta

GET /api/v1/goals/{id}


# 63. Atualizar meta

PUT /api/v1/goals/{id}


# 64. Concluir meta

POST /api/v1/goals/{id}/complete


# 65. Cancelar meta

POST /api/v1/goals/{id}/cancel


# 66. Dashboard

GET /api/v1/dashboard


O endpoint deve retornar dados resumidos para o dashboard.


# 67. Dashboard mensal

GET /api/v1/dashboard/monthly


Parâmetros:

year
month


Exemplo:

GET /api/v1/dashboard/monthly?year=2026&month=8


# 68. Resposta do dashboard

Exemplo conceitual:

{
  "balance": 5000.00,
  "income": 5400.00,
  "expenses": 2300.00,
  "pending": 1200.00,
  "creditCardCommitment": 1800.00,
  "projectedBalance": 4100.00
}


# 69. Gráfico de receitas e despesas

GET /api/v1/dashboard/income-expense


Parâmetros:

from
to


# 70. Despesas por categoria

GET /api/v1/dashboard/expenses-by-category


Parâmetros:

from
to


# 71. Evolução mensal

GET /api/v1/dashboard/monthly-evolution


Parâmetros:

months


Exemplo:

months=12


# 72. Projeção

GET /api/v1/projections


Parâmetros:

from
to


# 73. Projeção mensal

GET /api/v1/projections/monthly


Parâmetros:

year
month


# 74. Comprometimentos futuros

GET /api/v1/projections/commitments


Parâmetros:

from
to


# 75. Relatório do cartão

GET /api/v1/reports/credit-card


Filtros:

creditCardId
responsible
from
to


# 76. Exportação do relatório

GET /api/v1/reports/credit-card/export


Parâmetros:

creditCardId
responsible
from
to
format


Formato inicial:

PDF


# 77. Content-Type PDF

A resposta deve utilizar:

application/pdf


# 78. Paginação

Endpoints de listagem devem suportar paginação quando necessário.


Parâmetros:

page
size
sort


Exemplo:

GET /api/v1/expenses?page=0&size=20&sort=expenseDate,desc


# 79. Paginação

A página inicial será:

0


# 80. Tamanho padrão

Preferência:

20


# 81. Tamanho máximo

A API deve possuir limite máximo para evitar consultas excessivamente grandes.


Exemplo:

100


# 82. Filtros

Filtros devem ser combináveis.


Exemplo:

GET /api/v1/expenses?status=PENDING&creditCardId=...&responsible=MINE


# 83. Ordenação

Permitir somente campos conhecidos.

Não montar ORDER BY diretamente com texto arbitrário.


# 84. Erros

A API deve possuir formato padronizado.


Exemplo:

{
  "timestamp": "2026-08-12T14:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Dados inválidos",
  "path": "/api/v1/expenses",
  "details": []
}


# 85. Erro de validação

Exemplo:

{
  "field": "amount",
  "message": "O valor deve ser maior que zero"
}


# 86. Erro de autenticação

HTTP:

401


# 87. Erro de autorização

HTTP:

403


# 88. Não encontrado

HTTP:

404


# 89. Conflito

HTTP:

409


Exemplo:

Tentativa de fechar fatura já fechada.


# 90. Erro interno

HTTP:

500


Não retornar stack trace ao usuário.


# 91. Transações

Endpoints que alteram múltiplas entidades devem utilizar transação no Service.


# 92. Idempotência

Operações financeiras críticas devem ser avaliadas quanto à possibilidade de execução duplicada.


Futuro suporte:

Idempotency-Key


# 93. DTOs

Cada endpoint deve utilizar DTOs específicos quando necessário.


Não reutilizar um DTO apenas para economizar classes se isso tornar o contrato confuso.


# 94. Response DTO

Responses devem retornar somente informações necessárias.


Não retornar:

password_hash

ou informações internas.


# 95. UUID

IDs devem ser retornados como strings UUID no JSON.


# 96. Datas

Datas devem utilizar:

YYYY-MM-DD


Exemplo:

2026-08-12


# 97. Timestamps

Timestamps devem utilizar ISO 8601.


Exemplo:

2026-08-12T14:30:00Z


# 98. Valores monetários

Valores monetários devem ser retornados como números decimais no contrato da API, desde que a serialização preserve precisão.


A decisão final de serialização deverá ser padronizada antes da implementação.


# 99. OpenAPI

Todos os endpoints devem ser documentados automaticamente através de OpenAPI.


# 100. Swagger

O Swagger deve permitir:

- visualizar endpoints;
- visualizar schemas;
- testar endpoints;
- informar JWT.


# 101. Segurança Swagger

Durante desenvolvimento local:

Swagger pode permanecer habilitado.


Em produção futura:

deverá ser avaliado.


# 102. CORS

Durante desenvolvimento:

Angular:

http://localhost:4200


Backend:

http://localhost:8080


O CORS deve permitir somente origens configuradas.


# 103. Não utilizar CORS aberto

Não utilizar:

*


como configuração permanente.


# 104. Autenticação JWT

O backend deve validar:

- assinatura;
- expiração;
- estrutura;
- usuário.


# 105. Endpoint público

Somente endpoints explicitamente definidos como públicos devem aceitar requests sem autenticação.


# 106. User ID

O frontend não deve enviar:

userId


para operações que podem inferi-lo do JWT.


Exemplo incorreto:

POST /expenses

{
  "userId": "..."
}


O backend deve obter o usuário autenticado do contexto de segurança.


# 107. Isolamento

Mesmo que um ID de outro usuário seja enviado:

o backend deve responder:

404

ou comportamento equivalente seguro.


Nunca permitir acesso cruzado.


# 108. Regras de autorização

Toda consulta deve aplicar:

user_id = authenticatedUser.id


# 109. Controllers

Controllers devem:

- receber request;
- validar;
- chamar service;
- retornar response.


Não devem implementar cálculos financeiros complexos.


# 110. Services

Services devem implementar regras de negócio.


# 111. Repositories

Repositories devem realizar persistência e consultas.


# 112. Queries

Consultas complexas devem ser criadas com:

Spring Data
JPQL
native query

quando realmente necessário.


# 113. Performance

Evitar:

N+1 queries


especialmente em:

- faturas;
- parcelas;
- dashboard;
- relatórios.


# 114. Dashboard

O dashboard não deve executar dezenas de queries desnecessárias.


Quando necessário, criar consultas agregadas específicas.


# 115. Relatórios

Relatórios devem ser gerados no backend.

O frontend apenas solicita e baixa/exibe o resultado.


# 116. PDF

O mecanismo de geração de PDF será definido durante a implementação do módulo de relatórios.

A biblioteca escolhida deve possuir licença compatível e manutenção adequada.


# 117. API futura

A arquitetura deve permitir futuramente adicionar:

- investimentos;
- importação bancária;
- notificações;
- recorrências;
- relatórios avançados.


# 118. Não implementar agora

Não implementar na V1:

- Open Banking;
- PIX automático;
- integração bancária;
- investimentos;
- notificações;
- microservices;
- GraphQL.


# 119. Regra para novos endpoints

Antes de criar um endpoint:

1. verificar se já existe;
2. avaliar se a operação é realmente necessária;
3. seguir padrão REST;
4. documentar;
5. criar testes.


# 120. Testes da API

Endpoints críticos devem possuir testes.


Prioridade:

1. autenticação;
2. despesas;
3. parcelamento;
4. faturas;
5. pagamentos;
6. transferências;
7. projeções.


# 121. Testes de isolamento

Devem existir testes garantindo:

Usuário A

não consegue acessar dados do:

Usuário B.


# 122. Testes financeiros

Devem testar:

- pagamento integral;
- pagamento parcial;
- múltiplos pagamentos;
- estorno;
- cancelamento;
- parcelamento;
- alteração de parcela;
- fechamento de fatura;
- transferência.


# 123. Testes de projeção

Devem verificar:

- parcelas futuras;
- receitas futuras;
- despesas futuras;
- faturas futuras;
- saldo projetado.


# 124. Contrato

Este documento representa o contrato inicial da API.

Alterações significativas devem ser documentadas antes de serem implementadas.


# 125. Regra final

A API deve ser previsível.

Dado:

request válido

deve produzir:

response previsível

e respeitar:

autenticação
+
autorização
+
validação
+
regra de negócio
+
integridade financeira.