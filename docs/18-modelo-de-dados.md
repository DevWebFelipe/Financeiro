# Modelo de Dados — Financial Control

## 1. Objetivo

Este documento define o modelo conceitual e lógico inicial do banco de dados do Financial Control.

Banco:

PostgreSQL


ORM:

Spring Data JPA / Hibernate


Migrations:

Flyway


Identificador:

UUID


# 2. Princípios

O banco deve:

- preservar histórico financeiro;
- garantir integridade referencial;
- impedir dados financeiros órfãos;
- separar compromisso financeiro de movimentação efetiva;
- permitir pagamentos parciais;
- permitir parcelamentos;
- permitir projeções futuras;
- permitir múltiplas contas por usuário;
- permitir múltiplos cartões por usuário.


# 3. Entidades principais

V1:

User

Account

Category

Income

Expense

ExpenseInstallment

ExpensePayment

CreditCard

CreditCardInvoice

CreditCardInvoicePayment

Transfer

Goal

GoalContribution


# 4. Relacionamentos principais

User

possui:

N Accounts

N Categories

N Incomes

N Expenses

N CreditCards

N Transfers

N Goals


# 5. User

Tabela:

users


Campos:

id UUID PK

name VARCHAR(150) NOT NULL

email VARCHAR(255) NOT NULL UNIQUE

password_hash VARCHAR(255) NOT NULL

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 6. User

O email deve ser único.


# 7. User

Senha nunca deve ser armazenada em texto puro.


# 8. User

O frontend nunca recebe:

password_hash


# 9. User ownership

Toda entidade financeira pertencente ao usuário deve possuir:

user_id UUID NOT NULL


# 10. Account

Tabela:

accounts


Campos:

id UUID PK

user_id UUID NOT NULL

name VARCHAR(150) NOT NULL

type VARCHAR(30) NOT NULL

initial_balance NUMERIC(15,2) NOT NULL DEFAULT 0

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 11. Account FK

accounts.user_id

REFERENCES users.id


# 12. Account types

Valores iniciais:

BANK_ACCOUNT

SAVINGS_ACCOUNT

CASH


# 13. Cash

CASH representa uma carteira de dinheiro físico.


Não haverá entidade separada para dinheiro em espécie na V1.


# 14. Account balance

O saldo atual não deve ser livremente alterado.


O sistema deve calcular o saldo através das movimentações financeiras.


# 15. Initial balance

initial_balance representa o saldo existente no momento de criação da conta.


# 16. Account deactivation

Conta desativada:

active = false


Não excluir histórico.


# 17. Category

Tabela:

categories


Campos:

id UUID PK

user_id UUID NOT NULL

name VARCHAR(100) NOT NULL

type VARCHAR(20) NOT NULL

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 18. Category type

Valores:

EXPENSE

INCOME


# 19. Category

Categorias pertencem ao usuário.


# 20. Category uniqueness

O usuário não deve possuir duas categorias com o mesmo nome e mesmo tipo.


# 21. Income

Tabela:

incomes


Campos:

id UUID PK

user_id UUID NOT NULL

account_id UUID NOT NULL

category_id UUID

description VARCHAR(255) NOT NULL

amount NUMERIC(15,2) NOT NULL

income_date DATE NOT NULL

status VARCHAR(30) NOT NULL

responsible VARCHAR(30) NOT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 22. Income status

Valores iniciais:

EXPECTED

RECEIVED

CANCELLED


# 23. Income

Uma receita recebida deve estar vinculada a uma conta.


# 24. Income

Receita prevista pode existir sem movimentação efetiva na conta.


# 25. Income

Quando recebida:

deve gerar movimentação financeira.


# 26. Expense

Tabela:

expenses


Campos:

id UUID PK

user_id UUID NOT NULL

category_id UUID

account_id UUID

credit_card_id UUID

description VARCHAR(255) NOT NULL

amount NUMERIC(15,2) NOT NULL

expense_date DATE NOT NULL

due_date DATE

payment_method VARCHAR(30) NOT NULL

status VARCHAR(30) NOT NULL

responsible VARCHAR(30) NOT NULL

boleto_number VARCHAR(255)

parent_expense_id UUID

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 27. Expense account

account_id pode ser NULL quando a despesa for realizada no cartão de crédito e ainda não houver saída da conta.


# 28. Expense credit card

credit_card_id pode ser preenchido quando:

payment_method = CREDIT_CARD


# 29. Expense payment methods

Valores iniciais:

ACCOUNT

CREDIT_CARD

NONE


# 30. Payment method ACCOUNT

Representa uma despesa paga ou a pagar diretamente através de uma conta.


# 31. Payment method CREDIT_CARD

Representa uma compra realizada no cartão.


# 32. Payment method NONE

Representa uma obrigação financeira ainda não associada diretamente a uma conta ou cartão.


Exemplo:

lanche no escritório para pagar no final do mês.


# 33. Expense status

Valores:

OPEN

PARTIALLY_PAID

PAID

REFUNDED

CANCELLED


# 34. Expense

Uma despesa cancelada permanece armazenada.


# 35. Expense

Uma despesa estornada permanece armazenada.


# 36. Expense

Despesa cancelada não deve continuar impactando projeções financeiras.


# 37. Expense

Despesa estornada não deve continuar impactando o valor devido.


# 38. Parent expense

parent_expense_id pode representar uma relação com outra despesa.


Será utilizado especialmente para operações como:

parcelamento de cartão.


# 39. Expense installment

Tabela:

expense_installments


Campos:

id UUID PK

expense_id UUID NOT NULL

installment_number INTEGER NOT NULL

amount NUMERIC(15,2) NOT NULL

due_date DATE NOT NULL

status VARCHAR(30) NOT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 40. Installment

Uma despesa sem parcelamento pode possuir uma única parcela.


# 41. Installment

Despesa parcelada possui:

N parcelas.


# 42. Installment numbering

A numeração começa em:

1


# 43. Installment example

Despesa:

R$ 1.200,00


12 parcelas:


1/12

2/12

3/12

...

12/12


# 44. Installment values

Não assumir que:

amount / number_of_installments


é sempre o valor final de cada parcela.


# 45. Installment rounding

A geração automática deve distribuir centavos corretamente.


Exemplo:

100 / 3


pode gerar:

33,34

33,33

33,33


# 46. Installment editing

Cada parcela deve possuir valor próprio.


# 47. Installment

O total das parcelas deve ser rastreável em relação ao valor original da despesa.


# 48. Installment status

Valores iniciais:

OPEN

PARTIALLY_PAID

PAID

CANCELLED


# 49. Expense payment

Tabela:

expense_payments


Campos:

id UUID PK

expense_id UUID NOT NULL

installment_id UUID

account_id UUID NOT NULL

amount NUMERIC(15,2) NOT NULL

payment_date DATE NOT NULL

created_at TIMESTAMP NOT NULL


# 50. Expense payment

Um pagamento deve possuir:

despesa;

conta;

valor;

data.


# 51. Installment payment

installment_id pode ser utilizado quando o pagamento estiver relacionado diretamente a uma parcela.


# 52. Partial payment

Uma despesa pode possuir vários pagamentos.


Exemplo:

Despesa:

500


Pagamento 1:

200


Pagamento 2:

300


Total:

500


Status:

PAID


# 53. Payment sum

O total pago deve ser:

SUM(expense_payments.amount)


# 54. Expense remaining amount

Saldo:

expense.amount - totalPaid


# 55. Expense

Nunca alterar:

expense.amount


simplesmente porque um pagamento foi realizado.


# 56. Payment history

Pagamentos devem permanecer registrados.


# 57. Refund

Estorno não deve apagar pagamento histórico.


# 58. Credit card

Tabela:

credit_cards


Campos:

id UUID PK

user_id UUID NOT NULL

name VARCHAR(150) NOT NULL

last_four_digits VARCHAR(4)

credit_limit NUMERIC(15,2) NOT NULL

closing_day INTEGER NOT NULL

due_day INTEGER NOT NULL

active BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 59. Credit card

Não armazenar número completo do cartão.


# 60. Credit card

Não armazenar:

CVV;

senha;

código de segurança.


# 61. Credit card closing day

closing_day deve estar entre:

1 e 31


# 62. Credit card due day

due_day deve estar entre:

1 e 31


# 63. Credit card invoice

Tabela:

credit_card_invoices


Campos:

id UUID PK

user_id UUID NOT NULL

credit_card_id UUID NOT NULL

reference_year INTEGER NOT NULL

reference_month INTEGER NOT NULL

closing_date DATE NOT NULL

due_date DATE NOT NULL

total_amount NUMERIC(15,2) NOT NULL DEFAULT 0

status VARCHAR(30) NOT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 64. Invoice

Uma fatura pertence a:

um usuário;

um cartão.


# 65. Invoice

A fatura representa o compromisso financeiro gerado pelo cartão.


# 66. Invoice status

Valores:

OPEN

CLOSED

PARTIALLY_PAID

PAID

OVERDUE


# 67. Invoice total

O total da fatura deve ser derivado das despesas vinculadas à fatura.


O sistema pode armazenar total_amount para performance e consulta, mas deve manter consistência com os itens.


# 68. Invoice expense relation

A V1 deve possuir uma forma explícita de relacionar despesas à fatura.


# 69. Invoice item

Para permitir histórico e controle adequado, será criada:

credit_card_invoice_items


Campos:

id UUID PK

invoice_id UUID NOT NULL

expense_id UUID NOT NULL

amount NUMERIC(15,2) NOT NULL

created_at TIMESTAMP NOT NULL


# 70. Invoice item

O valor do item representa quanto da despesa foi atribuído àquela fatura.


# 71. Invoice item

Uma despesa de cartão pode aparecer em uma fatura específica.


# 72. Future installments

Cada parcela de uma compra parcelada no cartão pode aparecer em uma fatura diferente.


# 73. Invoice item

A relação deverá permitir:

despesa;

parcela;

fatura.


Portanto, a tabela deve possuir também:

installment_id UUID


# 74. Invoice item final

Campos:

id UUID PK

invoice_id UUID NOT NULL

expense_id UUID NOT NULL

installment_id UUID

amount NUMERIC(15,2) NOT NULL

created_at TIMESTAMP NOT NULL


# 75. Invoice payment

Tabela:

credit_card_invoice_payments


Campos:

id UUID PK

invoice_id UUID NOT NULL

account_id UUID NOT NULL

amount NUMERIC(15,2) NOT NULL

payment_date DATE NOT NULL

created_at TIMESTAMP NOT NULL


# 76. Invoice payment

O pagamento da fatura representa saída real da conta bancária.


# 77. Invoice partial payment

Uma fatura pode possuir vários pagamentos.


# 78. Invoice remaining

Saldo da fatura:

total_amount - SUM(payments)


# 79. Invoice paid

Quando:

totalPaid >= totalAmount


status:

PAID


# 80. Invoice partial

Quando:

0 < totalPaid < totalAmount


status:

PARTIALLY_PAID


# 81. Invoice unpaid

Quando:

totalPaid = 0


e ainda não vencida:

OPEN

ou:

CLOSED


conforme ciclo da fatura.


# 82. Invoice overdue

Se:

dueDate < currentDate

e saldo > 0:


status:

OVERDUE


# 83. Card purchase

Compra com cartão:

não reduz diretamente o saldo da conta.


Ela aumenta:

compromisso da fatura.


# 84. Invoice payment

Pagamento da fatura:

reduz saldo da conta;

reduz dívida da fatura.


# 85. Credit card available limit

Limite disponível:

creditLimit - committedAmount


# 86. Committed amount

O comprometimento deve considerar compras válidas ainda não quitadas conforme regra do cartão.


# 87. Cancelled expense

Despesa cancelada não deve consumir limite.


# 88. Refunded expense

Despesa estornada deve liberar o compromisso correspondente.


# 89. Card installment

Uma compra de:

R$ 1.200


em:

12 parcelas


gera:

1 despesa;

12 parcelas;

itens de fatura conforme cada ciclo.


# 90. Invoice generation

A fatura deve ser determinada pelo:

cartão;

vencimento da parcela;

data de fechamento.


# 91. Example invoice

Cartão:

fecha dia 10;

vence dia 17.


Compra:

09/08


deve pertencer à fatura que fecha em:

10/08


Compra:

11/08


deve pertencer à próxima fatura.


# 92. Transfer

Tabela:

transfers


Campos:

id UUID PK

user_id UUID NOT NULL

source_account_id UUID NOT NULL

destination_account_id UUID NOT NULL

amount NUMERIC(15,2) NOT NULL

transfer_date DATE NOT NULL

description VARCHAR(255)

status VARCHAR(30) NOT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 93. Transfer status

Valores:

COMPLETED

CANCELLED


# 94. Transfer

source_account_id

e:

destination_account_id


devem ser diferentes.


# 95. Transfer

Transferência não é:

income;

expense.


# 96. Transfer

Uma transferência representa movimentação entre ativos do próprio usuário.


# 97. Goal

Tabela:

goals


Campos:

id UUID PK

user_id UUID NOT NULL

name VARCHAR(150) NOT NULL

target_amount NUMERIC(15,2) NOT NULL

current_amount NUMERIC(15,2) NOT NULL DEFAULT 0

target_date DATE

status VARCHAR(30) NOT NULL

created_at TIMESTAMP NOT NULL

updated_at TIMESTAMP NOT NULL


# 98. Goal status

Valores:

ACTIVE

COMPLETED

CANCELLED


# 99. Goal contribution

Tabela:

goal_contributions


Campos:

id UUID PK

goal_id UUID NOT NULL

account_id UUID NOT NULL

amount NUMERIC(15,2) NOT NULL

contribution_date DATE NOT NULL

created_at TIMESTAMP NOT NULL


# 100. Goal contribution

Uma contribuição representa dinheiro separado para uma meta.


# 101. Goal

current_amount deve refletir as contribuições válidas.


# 102. Goal completion

Quando:

current_amount >= target_amount


a meta pode ser marcada:

COMPLETED


# 103. Responsible

Na V1 não haverá tabela de responsáveis.


Será utilizado:

VARCHAR


Valores:

MINE

GIULIA

EDERSON

ELISIANE


# 104. Responsible

A aplicação deve permitir que o usuário selecione os nomes amigáveis.


# 105. Responsible

O backend deve validar os valores permitidos.


# 106. Future responsible

No futuro poderá existir tabela própria.


A V1 não implementará isso.


# 107. Audit timestamps

Entidades principais devem possuir:

created_at

updated_at


# 108. Soft delete

Entidades financeiras não devem possuir exclusão física quando isso destruir histórico.


# 109. Cancel

Cancelar altera:

status


e mantém:

registro;

valores;

datas;

relações.


# 110. Foreign keys

Todas as relações relevantes devem possuir:

FOREIGN KEY


# 111. Foreign key ownership

O backend deve validar também:

user_id


para impedir acesso cruzado.


# 112. Unique constraints

Devem existir constraints quando fizer sentido.


Exemplo:

users.email


# 113. Indexes

Criar índices para consultas frequentes.


Principalmente:

user_id

expense_date

due_date

status

credit_card_id

account_id


# 114. Composite indexes

Podem ser utilizados.


Exemplo conceitual:

(user_id, expense_date)


# 115. Foreign key indexes

Foreign keys importantes devem possuir índices quando necessário.


# 116. Monetary type

Utilizar:

NUMERIC(15,2)


# 117. Monetary Java

Utilizar:

BigDecimal


# 118. Floating point

Não utilizar:

float;

double;


para valores monetários.


# 119. Dates

Utilizar:

DATE


para datas sem horário.


# 120. Timestamps

Utilizar:

TIMESTAMP


para:

created_at;

updated_at.


# 121. Timezone

O sistema deve possuir estratégia consistente de timezone.


# 122. Database naming

Tabelas:

snake_case


Exemplo:

credit_card_invoices


# 123. Database columns

snake_case


Exemplo:

created_at


# 124. Java naming

Java:

camelCase


# 125. UUID generation

UUID deve ser gerado pelo backend ou banco de maneira consistente.


# 126. Nullability

Campos obrigatórios devem possuir:

NOT NULL


# 127. Amount validation

Valores monetários devem ser:

>= 0


# 128. Positive amounts

Operações financeiras de entrada/saída devem validar:

amount > 0


# 129. Zero

Valores zero não devem ser aceitos em:

despesas;

receitas;

pagamentos;

transferências;

contribuições.


# 130. Expense installments

A soma das parcelas deve ser coerente com o valor da despesa.


# 131. Installment modifications

Ao alterar uma parcela:

o sistema deve recalcular o estado financeiro relacionado quando necessário.


# 132. Paid installment

Parcela já paga não deve ser alterada livremente.


# 133. Expense status

O status deve ser consequência das operações sempre que possível.


Não permitir que o frontend escolha arbitrariamente:

PAID


sem registrar pagamento.


# 134. Invoice status

O status da fatura deve ser derivado do ciclo e pagamentos.


# 135. Account balance

O saldo deve considerar:

initial_balance

+

incomes recebidos

-

expenses pagas

-

invoice payments

-

transferências de saída

+

transferências de entrada


# 136. Credit card purchase

Compra de cartão não entra diretamente no cálculo do saldo bancário.


# 137. Credit card invoice payment

Pagamento da fatura entra como saída da conta.


# 138. Projection

Projeções devem considerar compromissos futuros.


# 139. Projection

Uma despesa futura não deve ser confundida com pagamento realizado.


# 140. Financial movement

A V1 pode utilizar as entidades de negócio como fonte das movimentações.


Uma entidade genérica:

financial_transactions


pode ser adicionada posteriormente.


# 141. Financial transaction

Não é obrigatório criar uma tabela genérica de movimentações na primeira versão.


# 142. Motivo

Evitar complexidade prematura.


# 143. Future architecture

O modelo deve permitir futuramente uma camada unificada de:

financial_transactions


sem quebrar os domínios atuais.


# 144. Card refinancing

Quando uma fatura não puder ser totalmente paga:

o saldo restante poderá originar uma despesa:

PARCELAMENTO_CARTAO


# 145. Card refinancing

Essa despesa deve possuir:

parent_expense_id


apontando para a origem quando aplicável.


# 146. Refinancing installments

O parcelamento deve possuir suas próprias:

expense_installments


# 147. Refinancing

Os valores das parcelas podem ser diferentes.


# 148. Refinancing

A dívida original não deve simplesmente desaparecer.


O histórico da fatura permanece.


# 149. Refinancing

O valor refinanciado não deve ser contabilizado duas vezes como dívida atual.


# 150. Refinancing

A implementação detalhada será definida durante a implementação da funcionalidade.


# 151. Important rule

Não implementar o refinanciamento apenas como:

editar valor da fatura.


Deve existir histórico da operação.


# 152. Refund

Estorno de compra no cartão deve:

preservar despesa;

registrar estorno;

ajustar compromisso da fatura;

preservar histórico.


# 153. Cancel

Cancelamento deve ser diferente de:

refund.


# 154. Cancel

Cancelamento:

operação considerada inválida antes de efetivação.


# 155. Refund

Estorno:

operação que existiu e posteriormente foi revertida.


# 156. Payment

Pagamento:

operação financeira efetiva.


# 157. Historical integrity

Nunca remover dados financeiros apenas para alterar o saldo visual.


# 158. Database constraints

Sempre que possível:

integridade deve existir no banco.


# 159. Business rules

Regras complexas devem existir no backend.


# 160. Frontend

Frontend não deve ser responsável por garantir integridade financeira.


# 161. Transactions

Operações multi-entidade devem ser transacionais.


# 162. Expense creation

Criação de despesa parcelada deve ser:

ATOMIC


# 163. Invoice payment

Pagamento de fatura deve ser:

ATOMIC


# 164. Transfer

Transferência deve ser:

ATOMIC


# 165. Goal contribution

Contribuição de meta deve ser:

ATOMIC


# 166. Migration

Todas as tabelas devem ser criadas via Flyway.


# 167. Migration order

As migrations devem respeitar dependências.


Exemplo:

users

antes de:

accounts


# 168. Initial migration

A primeira migration deverá criar:

users;

accounts;

categories;

incomes;

expenses;

expense_installments;

expense_payments;

credit_cards;

credit_card_invoices;

credit_card_invoice_items;

credit_card_invoice_payments;

transfers;

goals;

goal_contributions.


# 169. Seed

Dados iniciais de desenvolvimento podem ser criados separadamente.


# 170. Production

Nunca inserir dados financeiros pessoais reais via migration.


# 171. Test data

Testes devem utilizar dados fictícios.


# 172. Referential integrity

Não permitir registros apontando para entidades inexistentes.


# 173. User deletion

Usuários não devem ser fisicamente excluídos na V1.


# 174. Historical data

Histórico financeiro deve permanecer disponível.


# 175. Future

O modelo pode futuramente receber:

investments;

recurring_expenses;

bank_statements;

open_finance;

notifications;

attachments;

audit_logs.


Nenhuma dessas entidades será implementada na V1.


# 176. Regra

Não criar tabelas futuras somente para "deixar preparado".


# 177. Simplicidade

O modelo deve ser o menor modelo capaz de representar corretamente os requisitos da V1.


# 178. Regra financeira fundamental

O sistema deve distinguir claramente:

1. compromisso financeiro;
2. pagamento;
3. movimentação entre contas;
4. projeção.


# 179. Exemplo

Compra de:

R$ 1.000


no cartão:

Não reduz conta bancária imediatamente.


# 180. Exemplo

Pagamento da fatura:

R$ 1.000


Reduz conta bancária.


# 181. Exemplo

Transferência:

Conta A:

-500


Conta B:

+500


Não altera patrimônio total.


# 182. Exemplo

Despesa aberta:

R$ 300


Não reduz saldo da conta.


Mas aparece:

contas a pagar.


# 183. Exemplo

Despesa paga:

R$ 300


Reduz saldo da conta.


# 184. Exemplo

Despesa cancelada:

R$ 300


Não deve mais impactar o planejamento.


# 185. Exemplo

Despesa estornada:

R$ 300


O histórico permanece.


O impacto financeiro é revertido.


# 186. Exemplo

Despesa parcelada:

R$ 1.200


12 parcelas.


O sistema deve saber:

quanto já venceu;

quanto já foi pago;

quanto ainda falta;

quanto será devido em cada mês.


# 187. Exemplo

Compra no cartão em agosto:

R$ 1.200

12 parcelas.


A projeção de setembro, outubro, novembro etc. deve considerar as parcelas correspondentes.


# 188. Exemplo

Fatura:

R$ 2.000


Pagamento:

R$ 1.200


Saldo:

R$ 800


A fatura permanece:

PARTIALLY_PAID


# 189. Exemplo

Os R$ 800 restantes podem posteriormente ser transformados em:

PARCELAMENTO_CARTAO


sem apagar o histórico da fatura original.


# 190. Regra final

O banco deve preservar a história do dinheiro.

Nunca resolver um problema financeiro simplesmente apagando ou sobrescrevendo o registro original.