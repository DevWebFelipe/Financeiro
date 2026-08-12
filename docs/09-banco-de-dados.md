# Modelo de Banco de Dados — Financial Control

## 1. Objetivo

Este documento define o modelo lógico inicial do banco de dados do Financial Control.

Banco:

PostgreSQL

Identificadores:

UUID

Valores financeiros:

NUMERIC / DECIMAL

O modelo deve priorizar:

- integridade;
- rastreabilidade;
- consistência;
- ausência de duplicidade financeira;
- possibilidade de evolução;
- isolamento por usuário.


# 2. Regra fundamental

Toda entidade pertencente a um usuário deve possuir relacionamento com:

users.id

através de:

user_id


# 3. Identificadores

Todas as entidades principais devem utilizar:

UUID


O banco deverá gerar UUID quando apropriado.

O backend também poderá gerar UUID antes da persistência.


# 4. Auditoria

Entidades importantes devem possuir:

created_at
updated_at


Quando necessário:

created_by
updated_by


Na V1, created_by e updated_by não são obrigatórios para todas as entidades.


# 5. Soft Delete

Registros financeiros não devem ser fisicamente apagados quando isso destruiria histórico.

Quando apropriado, utilizar:

deleted_at


ou status de cancelamento/estorno.


# 6. Usuários

Tabela:

users


Campos:

id UUID PRIMARY KEY

name VARCHAR(150) NOT NULL

email VARCHAR(255) NOT NULL UNIQUE

password_hash VARCHAR(255) NOT NULL

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 7. Email

O email deve possuir índice UNIQUE.

A comparação de email deve ser feita de maneira consistente.

A implementação poderá utilizar normalização para lowercase.


# 8. Contas

Tabela:

accounts


Representa:

- conta corrente;
- conta poupança;
- carteira pessoal;
- outras contas financeiras.


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

name VARCHAR(150) NOT NULL

type VARCHAR(30) NOT NULL

initial_balance NUMERIC(19,4) NOT NULL DEFAULT 0

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL

deleted_at TIMESTAMP NULL


FK:

accounts.user_id
    ->
users.id


# 9. Tipos de conta

Valores iniciais:

CHECKING
SAVINGS
PERSONAL_WALLET
OTHER


A aplicação poderá apresentar nomes amigáveis em português.


# 10. Conta pessoal

Uma conta:

PERSONAL_WALLET


pode ser utilizada para representar dinheiro em espécie.

Não é necessário criar uma entidade específica chamada:

cash


# 11. Categorias

Tabela:

categories


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

name VARCHAR(150) NOT NULL

type VARCHAR(20) NOT NULL

parent_id UUID NULL

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL

deleted_at TIMESTAMP NULL


# 12. Categoria pai

parent_id referencia:

categories.id


Permite:

Alimentação
    Mercado
    Restaurante
    Lanche


# 13. Tipo de categoria

Valores:

INCOME
EXPENSE


Uma categoria deve representar apenas um tipo.


# 14. Responsável

A responsabilidade financeira não será uma entidade de usuário na V1.

Utilizar enum lógico:

MINE
GIULIA
EDERSON
ELISIANE


O banco poderá armazenar:

MINE
GIULIA
EDERSON
ELISIANE


A camada de apresentação poderá exibir:

Meu
Giulia
Ederson
Elisiane


# 15. Receitas

Tabela:

incomes


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

account_id UUID NOT NULL

category_id UUID NOT NULL

description VARCHAR(255) NOT NULL

amount NUMERIC(19,4) NOT NULL

income_date DATE NOT NULL

status VARCHAR(30) NOT NULL

notes TEXT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL

deleted_at TIMESTAMP NULL


# 16. Status de receita

Valores iniciais:

PENDING
RECEIVED
CANCELLED


# 17. Despesas

Tabela:

expenses


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

account_id UUID NULL

category_id UUID NOT NULL

credit_card_id UUID NULL

description VARCHAR(255) NOT NULL

amount NUMERIC(19,4) NOT NULL

expense_date DATE NOT NULL

due_date DATE NULL

status VARCHAR(30) NOT NULL

responsible VARCHAR(30) NOT NULL

boleto_number VARCHAR(255) NULL

notes TEXT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL

deleted_at TIMESTAMP NULL


# 18. Regra de conta da despesa

account_id pode ser NULL.

Quando a despesa estiver no cartão:

credit_card_id != NULL

e:

account_id = NULL

até que exista pagamento real da fatura.


# 19. Regra de despesa sem cartão

Quando a despesa for sem cartão:

credit_card_id = NULL


A despesa poderá possuir:

account_id

quando já estiver vinculada a uma conta.


# 20. Regra de consistência

Uma despesa não deve possuir simultaneamente:

credit_card_id

e

account_id


como forma de pagamento primária na V1.

A compra no cartão somente gera saída bancária quando a fatura for paga.


# 21. Status de despesa

Valores iniciais:

PENDING
PAID
PARTIALLY_PAID
CANCELLED
REFUNDED
PARTIALLY_REFUNDED


A lista poderá evoluir conforme as regras de negócio.


# 22. Regra de estorno

Estorno não deve apagar a despesa.

A operação deve permanecer registrada.


# 23. Cartões

Tabela:

credit_cards


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

name VARCHAR(150) NOT NULL

holder_name VARCHAR(150) NULL

credit_limit NUMERIC(19,4) NOT NULL

closing_day SMALLINT NOT NULL

due_day SMALLINT NOT NULL

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL

deleted_at TIMESTAMP NULL


# 24. Dias do cartão

closing_day:

1 a 31


due_day:

1 a 31


O banco deve possuir CHECK constraints para impedir valores inválidos.


# 25. Regra de limite

credit_limit deve ser:

>= 0


# 26. Parcelamento

Tabela:

installment_plans


Representa uma operação de parcelamento.


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

expense_id UUID NOT NULL

total_amount NUMERIC(19,4) NOT NULL

installment_count INTEGER NOT NULL

type VARCHAR(30) NOT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 27. Tipos de parcelamento

Valores:

PURCHASE
INVOICE_REFINANCING


PURCHASE:

parcelamento originado de uma compra.


INVOICE_REFINANCING:

parcelamento originado de saldo de fatura.


# 28. Parcelas

Tabela:

installments


Campos:

id UUID PRIMARY KEY

installment_plan_id UUID NOT NULL

installment_number INTEGER NOT NULL

amount NUMERIC(19,4) NOT NULL

due_date DATE NOT NULL

status VARCHAR(30) NOT NULL

invoice_id UUID NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 29. Número da parcela

installment_number começa em:

1


Exemplo:

1/12
2/12
...
12/12


# 30. Unicidade da parcela

Deve existir constraint:

UNIQUE (
    installment_plan_id,
    installment_number
)


# 31. Valor da parcela

amount deve ser:

> 0


# 32. Soma das parcelas

A soma das parcelas de um parcelamento deve corresponder ao:

installment_plans.total_amount


Exceto quando uma alteração posterior de negócio representar ajuste explícito.

O backend deve garantir a consistência.


# 33. Alteração de parcela

Parcelas futuras podem ter valores alterados.

A alteração deve atualizar a representação do parcelamento quando necessário.

O sistema não deve permitir que:

soma das parcelas

e

total do plano

fiquem inconsistentes sem uma regra explícita.


# 34. Histórico de alteração

A arquitetura deve permitir futuramente registrar histórico de alteração de parcelas.

Exemplo:

installment_history


Não é obrigatório implementar essa tabela na V1 se a auditoria básica for suficiente.


# 35. Faturas

Tabela:

invoices


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

credit_card_id UUID NOT NULL

reference_year INTEGER NOT NULL

reference_month INTEGER NOT NULL

closing_date DATE NOT NULL

due_date DATE NOT NULL

status VARCHAR(30) NOT NULL

total_amount NUMERIC(19,4) NOT NULL DEFAULT 0

paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 36. Unicidade da fatura

Um cartão não pode possuir duas faturas para o mesmo:

reference_year
reference_month


Constraint:

UNIQUE (
    credit_card_id,
    reference_year,
    reference_month
)


# 37. Status de fatura

Valores iniciais:

OPEN
CLOSED
PARTIALLY_PAID
PAID
OVERDUE


# 38. Fatura aberta

OPEN:

- aceita novas parcelas;
- pode ter valor alterado.


# 39. Fatura fechada

CLOSED:

- ciclo encerrado;
- não aceita novas compras normais;
- aguarda pagamento.


# 40. Fatura parcialmente paga

PARTIALLY_PAID:

paid_amount > 0

e:

paid_amount < total_amount


# 41. Fatura paga

PAID:

paid_amount = total_amount


# 42. Fatura vencida

Uma fatura não paga após due_date deve ser considerada vencida.

A aplicação poderá atualizar status automaticamente.

A regra deve evitar depender exclusivamente de uma tarefa agendada.


# 43. Associação parcela/fatura

installments.invoice_id

referencia:

invoices.id


Uma parcela pode pertencer a uma única fatura.


# 44. Integridade

Uma parcela associada a uma fatura deve pertencer ao mesmo usuário da fatura.

O backend deve garantir essa regra.


# 45. Pagamentos

Tabela:

payments


Representa pagamento real.


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

account_id UUID NOT NULL

amount NUMERIC(19,4) NOT NULL

payment_date DATE NOT NULL

description VARCHAR(255) NULL

created_at TIMESTAMP NOT NULL


# 46. Pagamento de fatura

Tabela de relacionamento:

invoice_payments


Campos:

id UUID PRIMARY KEY

invoice_id UUID NOT NULL

payment_id UUID NOT NULL

amount NUMERIC(19,4) NOT NULL


Essa tabela permite que:

uma fatura possua vários pagamentos;

um pagamento seja associado a uma fatura.


# 47. Regra de pagamento

O valor registrado em invoice_payments deve ser positivo.


# 48. Pagamento de despesa

Uma despesa sem cartão pode possuir pagamentos.

Tabela:

expense_payments


Campos:

id UUID PRIMARY KEY

expense_id UUID NOT NULL

payment_id UUID NOT NULL

amount NUMERIC(19,4) NOT NULL


# 49. Pagamento real

Todo payment representa saída real de dinheiro da conta.

Portanto:

payment
    ->
account


# 50. Movimentações financeiras

Tabela:

financial_transactions


Representa movimentos efetivos de dinheiro nas contas.


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

account_id UUID NOT NULL

type VARCHAR(30) NOT NULL

amount NUMERIC(19,4) NOT NULL

transaction_date DATE NOT NULL

description VARCHAR(255) NOT NULL

reference_type VARCHAR(50) NULL

reference_id UUID NULL

created_at TIMESTAMP NOT NULL


# 51. Tipos de movimentação

Valores iniciais:

INCOME
EXPENSE
TRANSFER_IN
TRANSFER_OUT


# 52. Regra fundamental

financial_transactions representa dinheiro que efetivamente entrou ou saiu da conta.

Uma compra no cartão não cria financial_transaction.

O pagamento da fatura cria financial_transaction.


# 53. Receita recebida

Quando uma receita é efetivamente recebida:

Income
    ->
Payment/FinancialTransaction

A modelagem exata pode utilizar diretamente financial_transaction para receitas recebidas.


# 54. Despesa paga

Quando uma despesa sem cartão é paga:

Expense
    ->
Payment
    ->
FinancialTransaction


# 55. Fatura paga

Quando uma fatura é paga:

Invoice
    ->
InvoicePayment
    ->
Payment
    ->
FinancialTransaction


# 56. Transferências

Tabela:

transfers


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

source_account_id UUID NOT NULL

destination_account_id UUID NOT NULL

amount NUMERIC(19,4) NOT NULL

transfer_date DATE NOT NULL

description VARCHAR(255) NULL

created_at TIMESTAMP NOT NULL


# 57. Regra de transferência

source_account_id
e
destination_account_id

devem ser diferentes.


# 58. Transferência e saldo

Uma transferência gera:

TRANSFER_OUT

na conta origem.

E:

TRANSFER_IN

na conta destino.


# 59. Transferência não é receita

Transferência entre contas do mesmo usuário não deve ser contabilizada como:

receita.


# 60. Transferência não é despesa

Transferência entre contas do mesmo usuário não deve ser contabilizada como:

despesa.


# 61. Metas

Tabela:

goals


Campos:

id UUID PRIMARY KEY

user_id UUID NOT NULL

name VARCHAR(150) NOT NULL

description TEXT NULL

target_amount NUMERIC(19,4) NOT NULL

target_date DATE NULL

current_amount NUMERIC(19,4) NOT NULL DEFAULT 0

status VARCHAR(30) NOT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL

deleted_at TIMESTAMP NULL


# 62. Status de meta

Valores:

ACTIVE
COMPLETED
CANCELLED


# 63. Valor da meta

target_amount:

> 0


current_amount:

>= 0


# 64. Estornos

A V1 deve preservar a operação original.

Uma estrutura dedicada poderá ser:

refunds


Campos conceituais:

id
user_id
expense_id
amount
refund_date
description
created_at


# 65. Estorno parcial

A arquitetura deve permitir futuramente:

expense.amount = 500

refund.amount = 200


Saldo líquido:

300


# 66. Estorno total

expense.amount = 500

refund.amount = 500


Saldo líquido:

0


# 67. Cancelamento

Cancelamento não deve apagar fisicamente a despesa.

O status deve indicar:

CANCELLED


# 68. Boleto

boleto_number pertence à despesa.

Não é necessário criar tabela específica na V1.


# 69. Responsável

O campo:

responsible

deve existir nas despesas.


# 70. Cartão do sogro

Um cartão pode possuir:

holder_name = Ederson


O usuário autenticado continua sendo o proprietário do cadastro do cartão.


# 71. Exemplo

Usuário:

Felipe


Cartão:

Cartão Ederson


holder_name:

Ederson


Compra:

R$ 100


responsible:

MINE


Isso permite gerar relatório:

"Despesas do Felipe no cartão do Ederson."


# 72. Relacionamentos principais

users
    |
    +-- accounts
    |
    +-- categories
    |
    +-- incomes
    |
    +-- expenses
    |
    +-- credit_cards
    |
    +-- invoices
    |
    +-- installment_plans
    |
    +-- goals
    |
    +-- transfers
    |
    +-- payments
    |
    +-- financial_transactions


# 73. Relação de despesa

expenses
    |
    +-- category
    |
    +-- credit_card
    |
    +-- installment_plan
    |
    +-- payments


# 74. Relação de cartão

credit_cards
    |
    +-- invoices
            |
            +-- installments
            |
            +-- invoice_payments
                    |
                    +-- payments


# 75. Relação de pagamento

payments
    |
    +-- invoice_payments
    |
    +-- expense_payments
    |
    +-- financial_transactions


# 76. Regra de duplicidade financeira

Uma única saída bancária não deve ser representada duas vezes.

Exemplo incorreto:

Pagamento de fatura:

financial_transaction:
- R$ 1.000

E também:

expense:
- R$ 1.000
financial_transaction:
- R$ 1.000


Isso duplicaria o gasto.


# 77. Regra do cartão

Compra:

Expense

Parcela:

Installment

Fatura:

Invoice

Pagamento:

Payment

Saída:

FinancialTransaction


Somente a última etapa altera o saldo bancário.


# 78. Regra de projeção

A projeção deve utilizar:

- receitas previstas;
- despesas pendentes;
- parcelas futuras;
- faturas futuras;
- compromissos de cartão.


Não utilizar apenas financial_transactions.

financial_transactions representa passado/efetivado.

Projeção representa obrigações conhecidas futuras.


# 79. Saldo da conta

Saldo conceitual:

initial_balance
+
entradas efetivas
-
saídas efetivas


Transferências entre contas devem afetar:

origem:
-

destino:
+


# 80. Saldo não deve incluir

Compras no cartão ainda não pagas.

Parcelas futuras.

Faturas ainda não pagas.


# 81. Contas a pagar

Uma despesa:

PENDING


pode aparecer em contas a pagar.


Ela ainda não altera o saldo.


# 82. Pagamento parcial de despesa

Uma despesa pode possuir vários pagamentos.

Exemplo:

Despesa:
R$ 1.000

Pagamento 1:
R$ 400

Pagamento 2:
R$ 300

Saldo:
R$ 300


# 83. Status da despesa após pagamento parcial

PARTIALLY_PAID


# 84. Status da despesa após pagamento total

PAID


# 85. Regra de soma de pagamentos

Soma dos pagamentos da despesa não pode exceder:

expense.amount


exceto quando existir uma regra específica de crédito/ajuste.


# 86. Regra de soma de pagamentos de fatura

Soma de invoice_payments não pode exceder:

invoice.total_amount


na V1.


# 87. Índices

Criar índices para consultas frequentes.

Exemplos:

expenses(user_id)
expenses(user_id, expense_date)
expenses(user_id, due_date)
expenses(user_id, status)

incomes(user_id, income_date)

accounts(user_id)

credit_cards(user_id)

invoices(credit_card_id, due_date)

installments(installment_plan_id)

financial_transactions(account_id, transaction_date)


# 88. Índices compostos

Índices compostos devem ser criados baseados nas consultas reais.

Não criar dezenas de índices preventivamente.


# 89. Foreign Keys

Relacionamentos devem utilizar Foreign Keys reais.

Não depender apenas da aplicação para integridade.


# 90. ON DELETE

Evitar:

ON DELETE CASCADE

em entidades financeiras importantes quando isso puder apagar histórico acidentalmente.


# 91. Integridade financeira

Constraints devem proteger:

- valores negativos inválidos;
- dias inválidos;
- relações inexistentes;
- duplicidade de fatura;
- duplicidade de parcela.


# 92. Decimal

Preferência:

NUMERIC(19,4)


para valores financeiros.


# 93. Por que 4 casas?

Mesmo que a interface utilize 2 casas decimais, quatro casas fornecem margem para cálculos intermediários.

A apresentação continuará utilizando:

2 casas


# 94. Não utilizar MONEY

Não utilizar o tipo PostgreSQL:

MONEY


Utilizar:

NUMERIC


# 95. Enums PostgreSQL

Na V1, preferir:

VARCHAR + CHECK


em vez de PostgreSQL ENUM.

Motivo:

facilitar migrations e evolução dos valores.


# 96. Timestamps

Preferir:

TIMESTAMP WITH TIME ZONE


quando representar instante real.


# 97. Datas financeiras

Utilizar:

DATE


para:

- vencimento;
- fechamento;
- compra;
- pagamento;
- transferência.


# 98. UUID

Preferir geração de UUID compatível com PostgreSQL.

A estratégia final será definida durante a migration inicial.


# 99. Nomenclatura

Tabelas:

snake_case

Campos:

snake_case


Exemplo:

credit_cards
closing_day


# 100. Singular/plural

Tabelas devem utilizar plural.

Exemplo:

users
accounts
expenses
invoices


# 101. Chaves

PK:

id


FK:

<entity>_id


Exemplo:

user_id
account_id
credit_card_id


# 102. Schema

Inicialmente utilizar:

public


Não criar múltiplos schemas sem necessidade.


# 103. Migrations

A estrutura inicial deve ser criada através de Flyway.

Exemplo:

V1__create_users.sql
V2__create_accounts.sql
V3__create_categories.sql
V4__create_credit_cards.sql
V5__create_expenses.sql


A ordem real deverá respeitar dependências.


# 104. Migration inicial

A primeira migration não deve tentar implementar todo o banco se isso dificultar entendimento.

Pode ser dividida em migrations pequenas e lógicas.


# 105. Seed

Dados iniciais poderão ser criados através de seed separado.

Exemplos:

categorias padrão.


Não inserir usuários reais.


# 106. Dados de exemplo

Dados de desenvolvimento devem ser claramente identificados como:

DEMO
TEST


# 107. Ambiente de teste

Testes automatizados podem criar dados temporários.

Não utilizar dados do ambiente real.


# 108. Segurança

Credenciais do banco não devem estar nas migrations.

Não armazenar:

password
secret
JWT key


em arquivos versionados.


# 109. Diagrama

O projeto deverá possuir futuramente um diagrama ER.

Arquivo sugerido:

docs/database-diagram.md


ou:

docs/database-diagram.png


# 110. Regra para IA

Antes de criar entidades JPA:

1. ler este documento;
2. ler as regras de negócio;
3. identificar conflitos;
4. propor ajustes;
5. aguardar aprovação se o ajuste alterar o modelo.


# 111. Regra de evolução

Se uma regra financeira não puder ser representada adequadamente no modelo:

Não criar uma solução improvisada.

Explicar:

- qual regra está faltando;
- qual tabela seria afetada;
- qual relacionamento seria criado;
- impacto nas consultas;
- impacto nas migrations.


# 112. Regra final

O banco deve representar fatos financeiros.

Não armazenar valores derivados sem necessidade.

Exemplo:

available_credit_limit


pode ser calculado.

Não deve necessariamente ser armazenado.

Da mesma forma:

projected_balance


deve ser calculado.

O banco deve armazenar os dados necessários para produzir esses valores corretamente.


# 113. Princípio de consistência

Sempre que possível:

dados fundamentais
    ->
cálculos derivados


e não:

dados fundamentais
    ->
cálculo
    ->
armazenamento duplicado


Isso reduz inconsistências.


# 114. Modelo conceitual final

USERS

    |
    +-- ACCOUNTS
    |
    +-- CATEGORIES
    |
    +-- INCOMES
    |
    +-- EXPENSES
    |       |
    |       +-- INSTALLMENT_PLANS
    |       |       |
    |       |       +-- INSTALLMENTS
    |       |
    |       +-- EXPENSE_PAYMENTS
    |
    +-- CREDIT_CARDS
    |       |
    |       +-- INVOICES
    |               |
    |               +-- INSTALLMENTS
    |               |
    |               +-- INVOICE_PAYMENTS
    |
    +-- PAYMENTS
    |
    +-- FINANCIAL_TRANSACTIONS
    |
    +-- TRANSFERS
    |
    +-- GOALS
    |
    +-- REFUNDS


# 115. Objetivo

O modelo deve permitir responder corretamente:

Quanto tenho em cada conta?

Quanto já recebi?

Quanto já gastei?

Quanto ainda devo?

Quanto tenho no cartão?

Quanto devo em cada fatura?

Quanto já paguei da fatura?

Quanto falta pagar?

Quanto está comprometido nos próximos meses?

Quanto terei de pagar em dezembro?

Quanto das despesas do cartão é meu?

Quanto devo ao titular do cartão?

Quais despesas foram estornadas?

Quais despesas foram canceladas?

Qual é meu saldo projetado?


# 116. Regra final de segurança

Nenhuma implementação deve sacrificar a integridade financeira para simplificar código.

Se houver conflito entre:

simplicidade de implementação

e

correção financeira

a correção financeira vence.