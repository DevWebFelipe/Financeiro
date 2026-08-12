# API REST — Financial Control

## 1. Objetivo

Este documento define o contrato inicial da API REST do Financial Control.

O backend será desenvolvido em Java.

O frontend será desenvolvido em Angular.

A comunicação entre frontend e backend será realizada através de HTTP/HTTPS utilizando JSON.

A API deve seguir princípios REST.

---

# 2. Base URL

Durante o desenvolvimento local:

http://localhost:8080/api

A porta poderá ser alterada através de configuração.

Não utilizar URLs fixas diretamente no código.

---

# 3. Versionamento

A API deverá ser preparada para versionamento.

Formato:

/api/v1

Exemplo:

GET /api/v1/accounts

Mesmo que a primeira versão seja a única atualmente existente, o projeto deve permitir evolução futura.

---

# 4. Formato

## 4.1 Request

Dados enviados pela API devem utilizar JSON.

## 4.2 Response

Dados retornados pela API devem utilizar JSON.

## 4.3 Content-Type

Requests e responses JSON devem utilizar:

application/json

---

# 5. Autenticação

A API deverá possuir autenticação baseada em token.

A tecnologia específica poderá ser definida durante a implementação.

Uma opção inicial:

JWT

O usuário autenticado será identificado pelo backend.

O frontend não deve enviar livremente um userId esperando que o backend confie nele.

---

# 6. Regra de segurança

Nunca confiar em:

userId

enviado pelo frontend para determinar o proprietário de um recurso.

O backend deve obter o usuário autenticado a partir do contexto de autenticação.

---

# 7. Respostas HTTP

Utilizar códigos HTTP apropriados.

## 200

Operação realizada com sucesso.

## 201

Recurso criado.

## 204

Operação realizada sem conteúdo de retorno.

## 400

Requisição inválida.

## 401

Usuário não autenticado.

## 403

Usuário autenticado, mas sem permissão.

## 404

Recurso não encontrado.

## 409

Conflito de estado.

Exemplo:

Tentativa de cadastrar email já existente.

## 422

Dados semanticamente inválidos, quando apropriado.

## 500

Erro interno inesperado.

---

# 8. Formato de erro

As respostas de erro devem possuir formato padronizado.

Exemplo:

{
  "timestamp": "2026-08-12T14:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Dados inválidos.",
  "path": "/api/v1/accounts",
  "fieldErrors": [
    {
      "field": "name",
      "message": "Nome é obrigatório."
    }
  ]
}

O formato pode ser refinado durante a implementação.

O importante é que todos os endpoints utilizem uma estrutura consistente.

---

# 9. Paginação

Endpoints de listagem devem ser preparados para paginação.

Exemplo:

GET /api/v1/expenses?page=0&size=20

Response:

{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}

---

# 10. Ordenação

Listagens devem permitir ordenação quando fizer sentido.

Exemplo:

GET /api/v1/expenses?sort=dueDate,asc

---

# 11. Filtros

Listagens devem aceitar filtros através de query parameters.

Exemplo:

GET /api/v1/expenses?status=PENDENTE

---

# 12. Datas

Datas devem utilizar formato ISO-8601.

Exemplo:

2026-08-12

Para data e hora:

2026-08-12T14:30:00Z

---

# 13. Valores monetários

Valores monetários devem ser retornados como número decimal.

Exemplo:

{
  "amount": 1500.50
}

O backend deve utilizar BigDecimal.

O frontend não deve utilizar floating point para realizar cálculos financeiros críticos.

---

# 14. Authentication

## POST /api/v1/auth/register

Cria um usuário.

Request:

{
  "name": "Felipe",
  "email": "felipe@example.com",
  "password": "senha"
}

Response:

201

{
  "id": "uuid",
  "name": "Felipe",
  "email": "felipe@example.com"
}

A senha nunca deve ser retornada.

---

# 15. Login

## POST /api/v1/auth/login

Request:

{
  "email": "felipe@example.com",
  "password": "senha"
}

Response:

200

{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "name": "Felipe",
    "email": "felipe@example.com"
  }
}

---

# 16. Usuário atual

## GET /api/v1/auth/me

Retorna os dados do usuário autenticado.

Response:

{
  "id": "uuid",
  "name": "Felipe",
  "email": "felipe@example.com"
}

---

# 17. Contas

## GET /api/v1/accounts

Lista as contas do usuário autenticado.

Filtros possíveis:

- active;
- type.

---

## GET /api/v1/accounts/{id}

Retorna uma conta específica.

O backend deve garantir que ela pertença ao usuário autenticado.

---

## POST /api/v1/accounts

Cria uma conta.

Request:

{
  "name": "Nubank",
  "type": "CONTA_CORRENTE",
  "initialBalance": 1000.00
}

---

## PUT /api/v1/accounts/{id}

Atualiza uma conta.

---

## PATCH /api/v1/accounts/{id}/status

Ativa ou desativa uma conta.

Request:

{
  "active": false
}

---

## GET /api/v1/accounts/{id}/balance

Retorna o saldo calculado da conta.

Response:

{
  "accountId": "uuid",
  "initialBalance": 1000.00,
  "currentBalance": 1500.00
}

---

# 18. Categorias

## GET /api/v1/categories

Lista categorias.

Filtros:

- type;
- active;
- parentId.

---

## GET /api/v1/categories/{id}

Retorna categoria.

---

## POST /api/v1/categories

Cria categoria.

Request:

{
  "name": "Mercado",
  "type": "EXPENSE",
  "parentId": "uuid"
}

---

## PUT /api/v1/categories/{id}

Atualiza categoria.

---

## PATCH /api/v1/categories/{id}/status

Ativa ou desativa categoria.

---

# 19. Receitas

## GET /api/v1/incomes

Lista receitas.

Filtros:

- startDate;
- endDate;
- status;
- categoryId;
- accountId.

---

## GET /api/v1/incomes/{id}

Retorna receita.

---

## POST /api/v1/incomes

Cria receita.

Request:

{
  "description": "Salário",
  "amount": 5400.00,
  "incomeDate": "2026-08-05",
  "categoryId": "uuid",
  "accountId": "uuid",
  "status": "RECEIVED"
}

Os nomes finais dos enums devem ser padronizados durante a implementação.

---

## PUT /api/v1/incomes/{id}

Atualiza receita.

---

## PATCH /api/v1/incomes/{id}/status

Altera status da receita.

---

# 20. Despesas

## GET /api/v1/expenses

Lista despesas.

Filtros:

- startDate;
- endDate;
- status;
- categoryId;
- accountId;
- creditCardId;
- responsible;
- hasCreditCard.

---

## GET /api/v1/expenses/{id}

Retorna uma despesa.

---

## POST /api/v1/expenses

Cria uma despesa.

Request básico:

{
  "description": "Internet",
  "amount": 120.00,
  "expenseDate": "2026-08-10",
  "dueDate": "2026-08-15",
  "categoryId": "uuid",
  "accountId": "uuid",
  "responsible": "MINE",
  "status": "PENDING",
  "boletoNumber": "..."
}

---

# 21. Despesa no cartão

A criação de despesa no cartão deve permitir informar:

- creditCardId;
- amount;
- expenseDate;
- numberOfInstallments;
- responsible;
- categoryId;
- description.

Exemplo:

POST /api/v1/expenses

{
  "description": "Notebook",
  "amount": 3000.00,
  "expenseDate": "2026-08-12",
  "categoryId": "uuid",
  "creditCardId": "uuid",
  "responsible": "MINE",
  "numberOfInstallments": 10
}

O backend deve:

1. validar o cartão;
2. determinar a fatura;
3. criar o parcelamento;
4. criar as parcelas;
5. associar as parcelas às faturas;
6. retornar o resultado.

Toda a operação deve ser transacional.

---

# 22. Despesa parcelada

## POST /api/v1/expenses/installment

Endpoint especializado para criação de parcelamento, caso a implementação considere isso mais claro que utilizar POST /expenses.

A decisão final entre:

POST /expenses

ou

POST /expenses/installment

deve priorizar simplicidade da API.

Não duplicar regras de negócio em dois endpoints.

---

# 23. Parcelamento

## GET /api/v1/installments

Lista parcelas.

Filtros:

- installmentPlanId;
- status;
- startDate;
- endDate;
- creditCardId.

---

## GET /api/v1/installments/{id}

Retorna uma parcela.

---

## PUT /api/v1/installments/{id}

Permite alterar uma parcela futura quando permitido pelas regras de negócio.

Exemplo:

{
  "amount": 450.00,
  "dueDate": "2026-12-10"
}

---

# 24. Cartões

## GET /api/v1/credit-cards

Lista cartões.

---

## GET /api/v1/credit-cards/{id}

Retorna cartão.

---

## POST /api/v1/credit-cards

Cria cartão.

Request:

{
  "name": "Cartão A",
  "holderName": "Ederson",
  "creditLimit": 5000.00,
  "closingDay": 10,
  "dueDay": 15
}

---

## PUT /api/v1/credit-cards/{id}

Atualiza cartão.

---

## PATCH /api/v1/credit-cards/{id}/status

Ativa ou desativa cartão.

---

# 25. Limite do cartão

## GET /api/v1/credit-cards/{id}/limit

Response:

{
  "creditLimit": 5000.00,
  "usedLimit": 1800.00,
  "availableLimit": 3200.00
}

O cálculo deve ser realizado pelo backend.

---

# 26. Faturas

## GET /api/v1/credit-cards/{creditCardId}/invoices

Lista faturas de um cartão.

Filtros:

- year;
- month;
- status.

---

## GET /api/v1/invoices/{id}

Retorna uma fatura.

---

## GET /api/v1/invoices/{id}/items

Lista itens da fatura.

---

## GET /api/v1/invoices/{id}/summary

Retorna resumo.

Exemplo:

{
  "totalAmount": 2000.00,
  "paidAmount": 500.00,
  "remainingAmount": 1500.00,
  "status": "PARTIALLY_PAID"
}

---

# 27. Fechamento de fatura

## POST /api/v1/invoices/{id}/close

Fecha uma fatura.

O backend deve:

1. validar que a fatura pode ser fechada;
2. impedir novas compras;
3. consolidar os valores;
4. atualizar o status;
5. registrar timestamps apropriados.

A operação deve ser transacional.

---

# 28. Pagamento de fatura

## POST /api/v1/invoices/{id}/payments

Registra pagamento.

Request:

{
  "accountId": "uuid",
  "amount": 500.00,
  "paymentDate": "2026-08-12"
}

O backend deve:

1. validar a fatura;
2. validar a conta;
3. validar o valor;
4. registrar pagamento;
5. registrar saída financeira;
6. atualizar a fatura;
7. recalcular status.

Toda a operação deve ser transacional.

---

# 29. Pagamentos da fatura

## GET /api/v1/invoices/{id}/payments

Lista pagamentos da fatura.

---

# 30. Pagamento de despesa

## POST /api/v1/expenses/{id}/payments

Registra pagamento de despesa sem cartão.

Request:

{
  "accountId": "uuid",
  "amount": 120.00,
  "paymentDate": "2026-08-15"
}

Resultado:

Expense
    ->
Payment
    ->
FinancialTransaction


---

# 31. Transferências

## GET /api/v1/transfers

Lista transferências.

---

## GET /api/v1/transfers/{id}

Retorna transferência.

---

## POST /api/v1/transfers

Cria transferência.

Request:

{
  "sourceAccountId": "uuid",
  "destinationAccountId": "uuid",
  "amount": 1000.00,
  "transferDate": "2026-08-12",
  "description": "Transferência para pagamento"
}

A operação deve ser transacional.

---

# 32. Metas

## GET /api/v1/goals

Lista metas.

---

## GET /api/v1/goals/{id}

Retorna meta.

---

## POST /api/v1/goals

Cria meta.

Request:

{
  "name": "Viagem",
  "description": "Reserva para viagem",
  "targetAmount": 5000.00,
  "targetDate": "2027-01-01"
}

---

## PUT /api/v1/goals/{id}

Atualiza meta.

---

## PATCH /api/v1/goals/{id}/status

Altera status.

---

# 33. Dashboard

## GET /api/v1/dashboard

Retorna resumo financeiro.

Exemplo:

{
  "totalBalance": 5000.00,
  "income": 5400.00,
  "expenses": 2500.00,
  "creditCardDebt": 1800.00,
  "upcomingExpenses": 1200.00
}

O formato poderá evoluir conforme a implementação do frontend.

---

# 34. Dashboard mensal

## GET /api/v1/dashboard/monthly

Parâmetros:

year
month

Exemplo:

GET /api/v1/dashboard/monthly?year=2026&month=12

Deve retornar dados como:

- receitas;
- despesas;
- despesas por categoria;
- cartões;
- faturas;
- projeção;
- saldo projetado.

---

# 35. Projeções

## GET /api/v1/projections

Retorna projeções financeiras.

Parâmetros:

startDate
endDate

Exemplo:

GET /api/v1/projections?startDate=2026-08-01&endDate=2026-12-31

---

# 36. Projeção mensal

## GET /api/v1/projections/monthly

Exemplo:

GET /api/v1/projections/monthly?year=2026&month=12

Response conceitual:

{
  "month": "2026-12",
  "expectedIncome": 5000.00,
  "expectedExpenses": 1200.00,
  "expectedCreditCards": 900.00,
  "projectedBalance": 2900.00
}

---

# 37. Contas a pagar

## GET /api/v1/payables

Lista despesas pendentes.

Filtros:

- startDate;
- endDate;
- categoryId;
- responsible;
- creditCardId;
- accountId.

---

# 38. Relatório de fatura

## GET /api/v1/invoices/{id}/report

Retorna dados necessários para exportação.

Parâmetros:

responsible

Exemplo:

GET /api/v1/invoices/{id}/report?responsible=MINE

---

# 39. Exportação

A API deverá futuramente permitir exportação.

Formatos inicialmente considerados:

- PDF;
- CSV.

A implementação pode começar com apenas um formato.

A arquitetura deve permitir adicionar outros formatos posteriormente.

---

# 40. Relatório CSV

## GET /api/v1/invoices/{id}/report/export?format=csv

Deve retornar arquivo CSV.

O relatório deve respeitar filtros.

---

# 41. Relatório PDF

## GET /api/v1/invoices/{id}/report/export?format=pdf

Deve retornar PDF.

O PDF deve possuir informações suficientes para conferência.

---

# 42. Transações

A API não deve permitir que o frontend crie diretamente FinancialTransaction em operações normais.

Exemplo:

Não criar:

POST /api/v1/transactions

para pagamento comum.

Em vez disso:

POST /api/v1/invoices/{id}/payments

O backend cria a movimentação.

Isso reduz o risco de inconsistência.

---

# 43. Movimentações

## GET /api/v1/transactions

Pode existir endpoint de consulta.

Filtros:

- accountId;
- type;
- startDate;
- endDate.

Esse endpoint é somente para consulta na V1.

---

# 44. Filtros financeiros

Endpoints de consulta devem permitir combinar filtros quando fizer sentido.

Exemplo:

GET /api/v1/expenses
    ?startDate=2026-08-01
    &endDate=2026-08-31
    &responsible=MINE
    &status=PENDING


# 45. DTOs

O backend não deve expor diretamente entidades JPA como contrato da API.

Utilizar DTOs.

Exemplo:

Entity:

ExpenseEntity

DTO:

ExpenseResponse

Request:

CreateExpenseRequest

Update:

UpdateExpenseRequest


# 46. Validação

Requests devem possuir validação.

Exemplos:

- campos obrigatórios;
- valores positivos;
- UUID válido;
- datas válidas;
- quantidade de parcelas válida;
- dia de fechamento válido;
- dia de vencimento válido.

---

# 47. Regras de negócio no backend

O frontend pode realizar validações para melhorar a experiência.

Entretanto:

A validação definitiva deve existir no backend.

Nunca confiar exclusivamente no Angular.


# 48. Transações de banco

Endpoints que alteram múltiplas entidades devem utilizar transações.

Especialmente:

- compra parcelada;
- pagamento;
- pagamento de fatura;
- transferência;
- parcelamento de fatura;
- estorno.


# 49. Idempotência

Operações financeiras críticas devem ser protegidas contra duplicidade.

A estratégia exata poderá utilizar:

- idempotency key;
- identificador único;
- controle transacional;
- ou combinação dessas estratégias.

A solução deve ser definida durante implementação.


# 50. Swagger / OpenAPI

A API deve possuir documentação OpenAPI.

A documentação deve permitir:

- visualizar endpoints;
- visualizar parâmetros;
- visualizar schemas;
- testar endpoints;
- autenticar via JWT.

A documentação deve permanecer atualizada.


# 51. OpenAPI

A implementação deve gerar documentação automaticamente a partir dos contratos do backend sempre que possível.

Evitar manter documentação duplicada manualmente.


# 52. Organização dos endpoints

Os endpoints devem seguir uma nomenclatura consistente.

Utilizar substantivos.

Preferir:

GET /expenses

em vez de:

GET /getExpenses


# 53. HTTP verbs

Utilizar:

GET
POST
PUT
PATCH
DELETE

quando apropriado.

Não utilizar POST para todas as operações simplesmente por conveniência.


# 54. DELETE

DELETE não deve ser utilizado para apagar registros financeiros históricos.

Quando uma operação financeira precisar ser "removida", utilizar:

- cancelamento;
- estorno;
- inativação;

conforme a regra de negócio.


# 55. Status

Enums utilizados na API devem possuir nomenclatura consistente.

A representação final deve ser definida durante implementação.

Não misturar:

PENDING
pendente
Pending

sem uma regra.


# 56. Responsável

A API deve aceitar os responsáveis:

MINE
GIULIA
EDERSON
ELISIANE

A representação numérica utilizada no banco pode ser diferente, desde que exista conversão consistente.

---

# 57. Paginação

Toda listagem potencialmente grande deve ser paginável.

Exceções podem existir para listas pequenas e controladas, como:

- responsáveis;
- tipos;
- categorias pequenas.

---

# 58. Ordenação padrão

As APIs devem possuir ordenação padrão previsível.

Exemplo:

Despesas:

dueDate ASC

Faturas:

referenceYear DESC
referenceMonth DESC

---

# 59. Segurança

Endpoints devem verificar:

1. autenticação;
2. autorização;
3. propriedade do recurso.

Exemplo:

GET /expenses/{id}

O backend deve verificar:

expense.userId == authenticatedUser.id


# 60. Erro de propriedade

Se um usuário tentar acessar recurso pertencente a outro usuário:

Não revelar informações sobre o recurso.

Pode retornar:

404 Not Found

para evitar exposição desnecessária.


# 61. Logs

A API deve possuir logs estruturados suficientes para diagnosticar problemas.

Não registrar:

- senhas;
- tokens;
- informações sensíveis desnecessárias.


# 62. Health Check

A API deve possuir endpoint para verificar disponibilidade.

Exemplo:

GET /actuator/health

O endpoint exato dependerá do framework escolhido.


# 63. Health Check do banco

O health check deve permitir verificar se a aplicação consegue se comunicar com o PostgreSQL.

---

# 64. CORS

Durante desenvolvimento local, o backend deverá permitir comunicação com o Angular.

Exemplo:

Angular:
http://localhost:4200

Backend:
http://localhost:8080

A configuração deve ser explícita.

Não utilizar:

Access-Control-Allow-Origin: *

em produção.

---

# 65. Configuração

URLs, portas, credenciais e outras configurações não devem ficar hardcoded.

Utilizar:

- environment variables;
- application configuration;
- Docker Compose.


# 66. Banco

A API deve utilizar PostgreSQL.

O acesso ao banco deve utilizar uma camada de persistência adequada ao Java.

A tecnologia exata será definida no documento de stack.


# 67. Migrations

Alterações de banco devem ser realizadas através de migrations.

Não depender de criação automática de schema em produção.

Durante desenvolvimento, migrations também devem ser utilizadas para manter histórico.


# 68. Testes

Endpoints críticos devem possuir testes automatizados.

Especialmente:

- autenticação;
- criação de despesa;
- compra parcelada;
- fechamento;
- pagamento;
- pagamento parcial;
- transferência;
- projeção.


# 69. Testes de autorização

Deve existir teste garantindo que:

Usuário A

não consegue acessar:

Dados do usuário B.


# 70. Testes financeiros

Os testes devem validar valores financeiros utilizando BigDecimal.

Nunca utilizar comparação baseada em double.


# 71. Contrato de API

Alterações no contrato da API devem ser conscientes.

Antes de alterar:

- request;
- response;
- endpoint;
- enum;

verificar impacto no Angular.

---

# 72. Regra de implementação incremental

A API não deve ser implementada inteira de uma vez.

A ordem inicial recomendada será:

1. Health Check
2. Banco
3. Autenticação
4. Usuários
5. Contas
6. Categorias
7. Receitas
8. Despesas simples
9. Pagamentos
10. Transferências
11. Cartões
12. Faturas
13. Compras parceladas
14. Projeções
15. Metas
16. Dashboard
17. Relatórios
18. Exportações

A ordem pode ser ajustada conforme dependências.


# 73. Regra final

O backend deve ser a fonte da verdade das regras financeiras.

O Angular deve consumir a API.

O Angular não deve duplicar regras financeiras complexas que possam gerar divergência com o backend.

Toda regra crítica deve ser validada no backend e coberta por testes automatizados.