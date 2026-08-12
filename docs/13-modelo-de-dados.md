# Modelo de Dados — Financial Control

## 1. Objetivo

Este documento define o modelo conceitual e lógico inicial do banco de dados do Financial Control.

Banco:

PostgreSQL

Identificadores:

UUID

Todas as entidades que representam dados do usuário devem possuir relacionamento com o usuário autenticado, direta ou indiretamente.


# 2. Princípios do modelo

O banco deve priorizar:

- integridade;
- consistência financeira;
- rastreabilidade;
- simplicidade;
- normalização adequada;
- possibilidade de evolução.

Não criar tabelas apenas para antecipar funcionalidades futuras.

Não criar microserviços ou bancos separados.


# 3. Convenções

## 3.1 Primary Key

Todas as tabelas principais devem utilizar:

UUID


Exemplo:

id UUID PRIMARY KEY


# 3.2 UUID

A geração de UUID deve ser realizada de forma consistente.

A estratégia definitiva será definida durante a implementação do PostgreSQL.


# 3.3 Nomes

Tabelas:

snake_case


Exemplo:

credit_cards


Colunas:

snake_case


Exemplo:

created_at


# 3.4 Datas

Campos de data:

DATE


Exemplo:

expense_date


# 3.5 Data e hora

Campos de auditoria:

TIMESTAMP WITH TIME ZONE


Exemplo:

created_at


# 3.6 Valores monetários

Não utilizar:

FLOAT

ou:

DOUBLE


Para valores financeiros.

Preferência:

NUMERIC


Exemplo:

NUMERIC(15,2)


# 3.7 Quantidades

Quando necessário:

NUMERIC


# 3.8 Booleanos

Utilizar:

BOOLEAN


# 4. Auditoria básica

Entidades relevantes devem possuir:

created_at
updated_at


Quando aplicável:

created_by
updated_by


A necessidade de created_by e updated_by será avaliada conforme a entidade.


# 5. Usuários

Tabela:

users


Campos conceituais:

id
name
email
password_hash
active
created_at
updated_at


## Restrições

email deve ser único.


# 6. Contas financeiras

Tabela:

accounts


Campos:

id
user_id
name
type
initial_balance
active
created_at
updated_at


Relacionamento:

accounts.user_id -> users.id


# 7. Tipos de conta

Exemplos:

CHECKING

SAVINGS

PERSONAL_WALLET


A V1 não precisa possuir muitos tipos.


# 8. Saldo de conta

O saldo atual não deve depender somente de um campo manualmente atualizado.

A arquitetura deve permitir derivar o saldo através das movimentações financeiras.


Pode existir um campo materializado/cache no futuro.


A V1 deve priorizar consistência.


# 9. Movimentações financeiras

Tabela conceitual:

financial_transactions


Essa tabela representa movimentações efetivas de dinheiro.


Tipos:

INCOME

EXPENSE

TRANSFER_IN

TRANSFER_OUT


Campos conceituais:

id
user_id
account_id
type
amount
transaction_date
description
reference_type
reference_id
created_at


# 10. Regra das movimentações

Somente movimentações efetivas devem aparecer em:

financial_transactions


Uma despesa pendente não gera movimentação.

Uma compra no cartão não gera movimentação bancária no momento da compra.

Uma receita pendente não gera movimentação.


# 11. Receitas

Tabela:

incomes


Campos:

id
user_id
description
amount
income_date
due_date
account_id
category_id
status
notes
created_at
updated_at


Relacionamentos:

user_id -> users.id

account_id -> accounts.id

category_id -> categories.id


# 12. Status de receita

Sugestão:

PENDING

RECEIVED

CANCELLED


# 13. Categorias

Tabela:

categories


Campos:

id
user_id
name
type
parent_id
active
created_at
updated_at


Relacionamento:

user_id -> users.id

parent_id -> categories.id


# 14. Tipo de categoria

EXPENSE

INCOME


# 15. Categorias padrão

O sistema poderá possuir categorias padrão.


Uma alternativa futura é utilizar:

user_id NULL


para categorias globais.


A decisão final será tomada durante a implementação.


# 16. Despesas

Tabela:

expenses


Campos conceituais:

id
user_id
description
amount
expense_date
due_date
category_id
account_id
credit_card_id
responsible
status
boleto_number
notes
created_at
updated_at


Relacionamentos:

user_id -> users.id

category_id -> categories.id

account_id -> accounts.id

credit_card_id -> credit_cards.id


# 17. Responsável pela despesa

A V1 utilizará valores controlados.


Valores:

MINE

GIULIA

EDERSON

ELISIANE


O sistema deve permitir posteriormente transformar isso em cadastro.


# 18. Status da despesa

A V1 deve contemplar pelo menos:

PENDING

PARTIALLY_PAID

PAID

REFUNDED

PARTIALLY_REFUNDED

CANCELLED


Pode existir:

OVERDUE


caso seja conveniente como status derivado.


Preferência:

OVERDUE ser calculado pela regra de vencimento em vez de armazenado, quando possível.


# 19. Boleto

O campo:

boleto_number


deve aceitar texto.


Não utilizar tipo numérico.


Motivo:

códigos podem possuir:

- zeros à esquerda;
- separadores;
- caracteres.


# 20. Pagamentos

Tabela:

expense_payments


Campos:

id
expense_id
account_id
amount
payment_date
notes
created_at


Relacionamentos:

expense_id -> expenses.id

account_id -> accounts.id


# 21. Regra dos pagamentos

Uma despesa pode possuir:

0
1
ou vários pagamentos.


O valor pago deve ser calculável pela soma dos pagamentos.


Não confiar somente em um campo manual:


paid_amount


Caso exista campo materializado, ele deve ser mantido de forma consistente.


# 22. Estornos

Tabela:

expense_refunds


Campos:

id
expense_id
amount
refund_date
description
created_at


Relacionamento:

expense_id -> expenses.id


# 23. Regra de estorno

Uma despesa pode possuir:

0
1
ou vários estornos.


Valor disponível para estorno:

valor original
-
valor já estornado.


# 24. Cartões

Tabela:

credit_cards


Campos:

id
user_id
name
holder_name
credit_limit
closing_day
due_day
active
created_at
updated_at


Relacionamento:

user_id -> users.id


# 25. Titular do cartão

O campo:

holder_name


é textual.


Exemplo:

Ederson


Não criar cadastro separado para titulares na V1.


# 26. Dias do cartão

closing_day:

1 a 31


due_day:

1 a 31


O backend deve validar valores válidos.


# 27. Faturas

Tabela:

credit_card_invoices


Campos:

id
credit_card_id
reference_year
reference_month
closing_date
due_date
status
created_at
updated_at


Relacionamento:

credit_card_id -> credit_cards.id


# 28. Unicidade da fatura

Deve existir somente uma fatura por:

credit_card_id
+
reference_year
+
reference_month


Criar constraint UNIQUE.


# 29. Status da fatura

OPEN

CLOSED

PARTIALLY_PAID

PAID

OVERDUE

CANCELLED


OVERDUE pode ser derivado conforme regras.


# 30. Parcelamentos

Tabela:

installment_plans


Campos:

id
user_id
expense_id
total_amount
installment_count
created_at
updated_at


Relacionamentos:

user_id -> users.id

expense_id -> expenses.id


# 31. Regra do parcelamento

Uma despesa parcelada possui:

1 installment_plan


O plano possui:

N installments


# 32. Parcelas

Tabela:

installments


Campos:

id
installment_plan_id
invoice_id
installment_number
amount
due_date
status
created_at
updated_at


Relacionamentos:

installment_plan_id -> installment_plans.id

invoice_id -> credit_card_invoices.id


# 33. Unicidade da parcela

Dentro de um plano:

installment_number


deve ser único.


Constraint:

UNIQUE(installment_plan_id, installment_number)


# 34. Status da parcela

PENDING

PAID

PARTIALLY_PAID

CANCELLED

REFUNDED


A necessidade de todos os estados será validada durante implementação.


# 35. Valor das parcelas

As parcelas devem possuir valor próprio.


Não assumir que:

total_amount / installment_count


seja sempre suficiente.


Isso é importante porque:

R$ 100 / 3


resulta em:

33,33
33,33
33,34


# 36. Edição de parcelas

Cada parcela deve poder possuir valor diferente.


Exemplo:

1:
R$ 100

2:
R$ 100

3:
R$ 120


A soma deve ser controlada pelas regras de negócio.


# 37. Relação despesa -> cartão

Uma despesa pode possuir:

credit_card_id NULL


Quando:

NULL

significa:

não foi paga/comprada com cartão.


# 38. Relação despesa -> conta

Para despesas sem cartão:

account_id


representa a conta utilizada no pagamento, quando a operação já foi efetivada.


Para despesas pendentes:

account_id pode permanecer NULL até o pagamento.


# 39. Compra no cartão

Para uma compra no cartão:

credit_card_id != NULL


A conta bancária não deve ser utilizada como conta de pagamento imediato.


O pagamento ocorre quando a fatura é paga.


# 40. Transferências

Tabela:

transfers


Campos:

id
user_id
source_account_id
destination_account_id
amount
transfer_date
description
created_at


Relacionamentos:

user_id -> users.id

source_account_id -> accounts.id

destination_account_id -> accounts.id


# 41. Regra de transferência

source_account_id

deve ser diferente de:

destination_account_id


# 42. Movimentações da transferência

Uma transferência efetivada deve gerar:

TRANSFER_OUT

na origem


e:

TRANSFER_IN

no destino.


# 43. Transferência e receitas/despesas

Transferências não devem ser contabilizadas como:

receitas

ou:

despesas.


# 44. Metas

Tabela:

financial_goals


Campos:

id
user_id
name
description
target_amount
current_amount
target_date
status
created_at
updated_at


Relacionamento:

user_id -> users.id


# 45. Status de meta

ACTIVE

COMPLETED

CANCELLED


# 46. Meta na V1

A V1 pode utilizar:

current_amount


como acompanhamento manual.


Não é obrigatório criar movimentações específicas de meta.


# 47. Refinanciamento de cartão

A V1 deve deixar a arquitetura preparada para refinanciamento.


Não é necessário implementar um modelo definitivo complexo nesta primeira etapa.


Porém:

o modelo não deve impedir a criação futura de uma obrigação derivada de uma fatura.


# 48. Possível modelo futuro de obrigação

Futuramente poderá existir:

financial_obligations


para representar:

- refinanciamentos;
- empréstimos;
- outras dívidas.


Não implementar agora.


# 49. Relação usuário

Toda entidade pertencente ao usuário deve permitir determinar o proprietário do dado.


Exemplo:

expenses.user_id


# 50. Regra de segurança

Nunca confiar em:

user_id


enviado pelo frontend.


O backend deve utilizar o usuário autenticado.


# 51. Integridade referencial

Foreign Keys devem ser utilizadas.


Não depender somente da aplicação para garantir relacionamentos.


# 52. Exclusão

Evitar:

ON DELETE CASCADE


em entidades financeiras.


Motivo:

a exclusão de um registro pai não deve apagar histórico financeiro inesperadamente.


# 53. Soft delete

Entidades relevantes podem possuir:

active


ou:

deleted_at


A estratégia deve ser consistente.


# 54. Histórico financeiro

Operações financeiras importantes não devem ser fisicamente apagadas.


Preferir:

CANCELLED

REFUNDED

ou outros estados.


# 55. Índices

Criar índices para consultas frequentes.


Prioridade:

user_id

account_id

category_id

credit_card_id

status

expense_date

due_date


# 56. Índices compostos

Avaliar índices compostos como:

(user_id, expense_date)

(user_id, due_date)

(user_id, status)

(credit_card_id, reference_year, reference_month)


# 57. Índice de faturas

A constraint:

credit_card_id
+
reference_year
+
reference_month


deve possuir índice único.


# 58. Precisão monetária

Todos os cálculos financeiros devem utilizar precisão decimal.


Não utilizar floating point para regra financeira.


# 59. Arredondamento

O arredondamento deve ser realizado explicitamente.


Não depender de comportamento implícito de floating point.


# 60. Regra de soma de parcelas

A soma das parcelas deve corresponder ao valor total da compra.


Exceção:

alteração manual posterior das parcelas.


Nesse caso, o sistema deve registrar a alteração e aplicar as regras de negócio definidas.


# 61. Integridade do parcelamento

Não permitir:

installment_number > installment_count


# 62. Integridade do pagamento

Não permitir pagamento:

<= 0


# 63. Integridade do estorno

Não permitir estorno:

<= 0


# 64. Integridade do estorno

Não permitir estornar mais que:

valor da despesa
-
valor já estornado.


# 65. Integridade da fatura

Não permitir pagamento maior que o saldo devido sem uma regra explícita de crédito/excesso.


V1:

bloquear pagamento acima do saldo restante.


# 66. Integridade da transferência

Não permitir:

amount <= 0


# 67. Integridade da transferência

Não permitir transferência entre contas de usuários diferentes.


# 68. Integridade de contas

Uma conta pertence a um usuário.


Um usuário não pode utilizar diretamente uma conta de outro usuário.


# 69. Integridade de cartões

Um cartão pertence a um usuário.


Um usuário não pode utilizar diretamente um cartão de outro usuário.


# 70. Integridade de categorias

Uma categoria pertence a um usuário.


Uma despesa deve utilizar categoria pertencente ao mesmo usuário.


# 71. Integridade de fatura

Uma parcela só pode pertencer a uma fatura do mesmo cartão da despesa associada.


Essa regra deve ser garantida pela aplicação e, quando possível, pelo modelo.


# 72. Integridade de datas

Não permitir:

closing_day inválido.

due_day inválido.


# 73. Integridade de valores

Não permitir valores negativos para:

- receitas;
- despesas;
- pagamentos;
- estornos;
- transferências;
- limites;
- metas.


# 74. Valores zero

Valores zero devem ser tratados explicitamente.


Preferência:

bloquear operações financeiras com valor zero.


# 75. Nome das colunas

Não utilizar palavras reservadas do PostgreSQL sem necessidade.


# 76. Migrations

O banco deve ser criado através de migrations.


Não depender de:

hibernate.ddl-auto=create


# 77. Hibernate

Durante desenvolvimento:

ddl-auto


não deve ser responsável pelo schema oficial.


Preferência:

validate


quando o schema estiver estabilizado.


# 78. Migrations

Toda alteração estrutural deve criar migration.


Exemplo:

V1__create_users.sql

V2__create_accounts.sql


A ferramenta de migration será definida na arquitetura.


# 79. Seed

Dados iniciais podem ser inseridos por migration ou mecanismo de seed controlado.


# 80. Dados iniciais

A V1 pode possuir:

categorias padrão.


Não criar usuários padrão com senha conhecida.


# 81. Ambiente de testes

Testes devem utilizar banco isolado.


Preferência:

PostgreSQL em container.


# 82. Ambiente de desenvolvimento

PostgreSQL deve rodar via Docker.


# 83. Ambiente local

Docker Compose poderá possuir:

postgres

backend

frontend


Quando necessário.


# 84. Schema

A V1 pode utilizar:

public


Não criar múltiplos schemas sem necessidade.


# 85. Relacionamentos principais

users
|
+-- accounts
|
+-- categories
|
+-- incomes
|
+-- expenses
|     |
|     +-- expense_payments
|     |
|     +-- expense_refunds
|     |
|     +-- installment_plans
|            |
|            +-- installments
|
+-- credit_cards
|     |
|     +-- credit_card_invoices
|            |
|            +-- installments
|
+-- transfers
|
+-- financial_goals
|
+-- financial_transactions


# 86. Regra importante

O modelo acima é conceitual.


Antes da implementação definitiva:

- revisar relacionamentos;
- revisar cardinalidade;
- revisar constraints;
- revisar índices;
- revisar regras financeiras.


# 87. Não duplicar informação

Evitar armazenar o mesmo valor em múltiplos lugares sem necessidade.


Exemplo:

Não armazenar simultaneamente:

expense.total_paid

e uma estrutura inconsistente de payments.


Se houver campo derivado/materializado:

deve existir estratégia clara para mantê-lo consistente.


# 88. Dados derivados

Os seguintes dados podem ser derivados:

- saldo;
- valor pago;
- valor restante;
- valor da fatura;
- limite disponível;
- valor estornado;
- saldo projetado.


A V1 deve priorizar cálculo consistente.


# 89. Performance futura

Se cálculos derivados se tornarem pesados:

poderemos introduzir:

- materialized views;
- campos calculados/materializados;
- tabelas de resumo;
- cache.


Não antecipar essa complexidade na V1.


# 90. Concorrência

Operações financeiras críticas devem considerar concorrência.


Exemplos:

- pagamento de fatura;
- pagamento de despesa;
- transferência;
- estorno.


Pode ser necessário utilizar:

transaction isolation;

optimistic locking;

ou pessimistic locking.


A estratégia deve ser escolhida durante implementação.


# 91. Optimistic Locking

Entidades que possam sofrer alterações concorrentes podem utilizar:

version


quando necessário.


# 92. Precisão temporal

O sistema deve considerar o timezone configurado para a aplicação.


A aplicação deve possuir timezone configurável.


# 93. Datas financeiras

Datas como:

expense_date
due_date
payment_date
income_date


representam datas de negócio.


Preferir:

DATE


quando não houver necessidade de horário.


# 94. Auditoria

created_at e updated_at devem ser preenchidos automaticamente pelo backend.


# 95. Integridade do usuário

Ao excluir/desativar usuário:

não apagar automaticamente histórico financeiro.


A estratégia de desativação deve preservar dados.


# 96. Email

email deve ser:

- obrigatório;
- único;
- normalizado para comparação.


# 97. Senha

password_hash deve armazenar somente hash seguro.


A tecnologia específica será definida na arquitetura.


# 98. Campos opcionais

Campos opcionais devem aceitar NULL quando semanticamente apropriado.


Não utilizar valores artificiais como:

""

ou:

0


para representar ausência de informação.


# 99. Evolução

O modelo deve permitir futuramente adicionar:

- investimentos;
- importação bancária;
- recorrências;
- notificações;
- rateio;
- obrigações;
- auditoria avançada.


Sem implementar essas funcionalidades agora.


# 100. Regra final

O banco deve representar a realidade financeira.

Uma operação deve poder responder:

Quem realizou?

O que aconteceu?

Quando aconteceu?

Qual foi o valor?

Qual conta foi afetada?

Qual cartão foi afetado?

Qual compromisso foi criado?

Qual é o estado atual?

E principalmente:

A informação histórica continua disponível?