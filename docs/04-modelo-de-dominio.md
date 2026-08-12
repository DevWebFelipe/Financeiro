# Modelo de Domínio — Financial Control

## 1. Objetivo

Este documento define as principais entidades do Financial Control, seus atributos, relacionamentos e responsabilidades.

Este documento será utilizado como referência para:

- modelo do banco PostgreSQL;
- entidades JPA;
- DTOs;
- regras de negócio;
- migrations;
- APIs;
- testes.

O modelo deve priorizar integridade financeira e clareza.


## 2. Princípios

### 2.1 Identificadores

As entidades principais utilizarão UUID.

### 2.2 Auditoria básica

As entidades relevantes possuirão:

- created_at;
- updated_at.

### 2.3 Usuário

Dados financeiros devem pertencer a um usuário.

### 2.4 Histórico

Registros financeiros não devem ser apagados fisicamente quando já tiverem produzido efeitos financeiros.


# 3. User

Representa o usuário do sistema.

## Campos

- id;
- name;
- email;
- password_hash;
- created_at;
- updated_at.

## Regras

- email deve ser único;
- password_hash nunca deve conter senha em texto puro;
- usuário pode possuir várias contas;
- usuário pode possuir vários cartões;
- usuário pode possuir receitas;
- usuário pode possuir despesas;
- usuário pode possuir categorias;
- usuário pode possuir metas.


# 4. Account

Representa uma conta financeira pertencente ao usuário.

## Campos

- id;
- user_id;
- name;
- type;
- initial_balance;
- active;
- created_at;
- updated_at.

## Tipos

- CONTA_CORRENTE;
- POUPANCA;
- CONTA_PESSOAL.

## Exemplos

Felipe pode possuir:

Conta Santander
Conta Nubank
Poupança
Carteira

A conta pessoal pode representar dinheiro em espécie.


## Relacionamentos

User 1:N Account


# 5. Category

Representa categorias de receitas ou despesas.

## Campos

- id;
- user_id;
- name;
- type;
- parent_id;
- active;
- created_at;
- updated_at.

## Tipos

- INCOME;
- EXPENSE.

## Hierarquia

Uma categoria pode possuir uma categoria pai.

Exemplo:

Alimentação
    |
    +--- Mercado
    +--- Restaurante
    +--- Lanche


## Relacionamentos

User 1:N Category

Category 1:N Category


# 6. Income

Representa uma receita.

## Campos

- id;
- user_id;
- category_id;
- account_id;
- description;
- amount;
- income_date;
- status;
- created_at;
- updated_at.

## Status

- PREVISTA;
- RECEBIDA;
- CANCELADA.

## Regras

Uma receita prevista não altera o saldo.

Uma receita recebida gera uma movimentação de entrada.

Uma receita cancelada permanece no histórico.


## Relacionamentos

User 1:N Income

Category 1:N Income

Account 1:N Income


# 7. Expense

Representa uma despesa.

## Campos

- id;
- user_id;
- category_id;
- account_id;
- credit_card_id;
- description;
- amount;
- expense_date;
- due_date;
- boleto_number;
- responsible;
- status;
- created_at;
- updated_at.

## Status

- PENDENTE;
- PAGA;
- ESTORNADA;
- CANCELADA.

## Responsible

Valores atuais:

0 - Meu
1 - Giulia
2 - Ederson
3 - Elisiane

Esse campo representa uma classificação e não um usuário.


## Regras

Uma despesa pode:

- possuir conta;
- possuir cartão;
- não possuir nenhum dos dois inicialmente.

Quando associada a cartão, a despesa deverá estar relacionada a uma fatura através de sua parcela/item de fatura.

Uma despesa pendente participa das contas a pagar e projeções.

Uma despesa paga gera movimentação de saída.

Uma despesa estornada permanece no histórico.

Uma despesa cancelada permanece no histórico.


# 8. CreditCard

Representa um cartão de crédito.

## Campos

- id;
- user_id;
- name;
- holder_name;
- credit_limit;
- closing_day;
- due_day;
- active;
- created_at;
- updated_at.

## Exemplos

Nubank Felipe
Itaú Giulia
Cartão Ederson

## Regras

Um usuário pode possuir vários cartões.

Um cartão pertence a somente um usuário.

O titular é armazenado como texto.


## Relacionamentos

User 1:N CreditCard

CreditCard 1:N Invoice


# 9. Invoice

Representa uma fatura de cartão.

## Campos

- id;
- credit_card_id;
- reference_month;
- reference_year;
- closing_date;
- due_date;
- total_amount;
- paid_amount;
- status;
- created_at;
- updated_at.

## Status

- ABERTA;
- FECHADA;
- PAGA;
- PARCIALMENTE_PAGA;
- VENCIDA.


## Regras

Uma fatura pertence a um cartão.

Uma fatura possui vários itens.

O total da fatura deve ser consequência de seus itens e ajustes financeiros.

O valor pago representa quanto foi efetivamente pago.

O saldo restante é:

total_amount - paid_amount


# 10. InstallmentPlan

Representa um parcelamento.

Pode representar:

- compra parcelada;
- parcelamento de fatura.

## Campos

- id;
- user_id;
- type;
- description;
- total_amount;
- total_installments;
- created_at;
- updated_at.

## Tipos

- CREDIT_CARD_PURCHASE;
- INVOICE_REFINANCING.


## Regras

Um parcelamento possui várias parcelas.

Cada parcela possui valor próprio.

Não assumir que:

total_amount / total_installments

seja necessariamente o valor de cada parcela.


# 11. Installment

Representa uma parcela individual.

## Campos

- id;
- installment_plan_id;
- installment_number;
- amount;
- due_date;
- status;
- expense_id;
- invoice_id;
- created_at;
- updated_at.

## Status

A definição completa poderá ser refinada durante a implementação.

Inicialmente considerar:

- PENDENTE;
- PAGA;
- ESTORNADA;
- CANCELADA.

## Relacionamentos

InstallmentPlan 1:N Installment

Installment poderá estar associada a:

- Expense;
- Invoice.


# 12. InvoiceItem

Representa um item de uma fatura.

## Campos

- id;
- invoice_id;
- expense_id;
- installment_id;
- type;
- description;
- amount;
- created_at;
- updated_at.

## Tipos

- PURCHASE;
- REFUND;
- CREDIT;
- ADJUSTMENT.

## Regras

Um item pertence a uma fatura.

O item deve permitir identificar sua origem.

Exemplo:

Compra de R$ 500:

InvoiceItem
    type = PURCHASE
    amount = 500


Estorno de R$ 500:

InvoiceItem
    type = REFUND
    amount = -500


# 13. Payment

Representa um pagamento financeiro.

Essa entidade deve ser utilizada quando for necessário registrar formalmente o pagamento de:

- despesa;
- fatura;
- parcela.

## Campos

- id;
- user_id;
- account_id;
- expense_id;
- invoice_id;
- installment_id;
- amount;
- payment_date;
- created_at;
- updated_at.

## Regras

O pagamento deve possuir uma conta de origem.

Uma operação de pagamento deve gerar uma movimentação financeira.


# 14. Transaction

Representa uma movimentação financeira efetiva.

O nome da tabela pode ser definido durante a implementação para evitar conflito com conceitos do banco/framework.

Uma alternativa aceitável é:

financial_transaction


## Campos

- id;
- user_id;
- account_id;
- type;
- amount;
- transaction_date;
- description;
- reference_type;
- reference_id;
- transfer_id;
- created_at;
- updated_at.

## Tipos

- ENTRADA;
- SAIDA;
- TRANSFERENCIA.

## Regras

Somente movimentações efetivas devem gerar Transactions.

Projeções não geram Transactions.

Despesas pendentes não geram Transactions.

Receitas previstas não geram Transactions.

Compras no cartão não geram imediatamente uma saída bancária.


# 15. Transfer

Representa uma transferência entre contas.

## Campos

- id;
- user_id;
- source_account_id;
- destination_account_id;
- amount;
- transfer_date;
- description;
- created_at;
- updated_at.

## Regras

A conta origem e a conta destino devem ser diferentes.

A transferência deve pertencer ao usuário autenticado.

Uma transferência gera:

- saída na conta origem;
- entrada na conta destino.

As duas movimentações devem possuir referência à mesma transferência.


# 16. Goal

Representa uma meta financeira.

## Campos

- id;
- user_id;
- name;
- description;
- target_amount;
- current_amount;
- target_date;
- status;
- created_at;
- updated_at.

## Status

- ATIVA;
- CONCLUIDA;
- CANCELADA.

## Regras

Uma meta pertence a um usuário.

O progresso deve ser calculado com base nos valores armazenados.


# 17. GoalContribution

A arquitetura deve permitir futuramente registrar contribuições individuais para metas.

A entidade pode ser preparada conceitualmente, mas NÃO precisa ser implementada na primeira versão se não houver necessidade.

Possível estrutura futura:

- id;
- goal_id;
- account_id;
- amount;
- contribution_date;
- description;
- created_at;
- updated_at.

Essa entidade NÃO faz parte obrigatória da V1.


# 18. Relacionamento geral

Modelo conceitual:

User
 |
 +--- Account
 |
 +--- Category
 |
 +--- Income
 |
 +--- Expense
 |
 +--- CreditCard
 |       |
 |       +--- Invoice
 |               |
 |               +--- InvoiceItem
 |
 +--- InstallmentPlan
 |       |
 |       +--- Installment
 |
 +--- Payment
 |
 +--- FinancialTransaction
 |
 +--- Transfer
 |
 +--- Goal


# 19. Relacionamentos principais

User

1:N Account

1:N Category

1:N Income

1:N Expense

1:N CreditCard

1:N InstallmentPlan

1:N Payment

1:N FinancialTransaction

1:N Transfer

1:N Goal


CreditCard

1:N Invoice


Invoice

1:N InvoiceItem


InstallmentPlan

1:N Installment


Category

1:N Category


Account

1:N Payment

1:N FinancialTransaction


# 20. Dependências conceituais

## Receita

User
    |
    +--- Income
            |
            +--- Category
            |
            +--- Account
            |
            +--- FinancialTransaction


## Despesa sem cartão

User
    |
    +--- Expense
            |
            +--- Category
            |
            +--- Payment
                    |
                    +--- Account
                            |
                            +--- FinancialTransaction


## Compra no cartão

User
    |
    +--- Expense
            |
            +--- CreditCard
                    |
                    +--- Invoice
                            |
                            +--- InvoiceItem


## Compra parcelada

User
    |
    +--- InstallmentPlan
            |
            +--- Installment
                    |
                    +--- Expense
                    |
                    +--- Invoice
                            |
                            +--- InvoiceItem


## Pagamento de fatura

Invoice
    |
    +--- Payment
            |
            +--- Account
                    |
                    +--- FinancialTransaction


## Transferência

Transfer
    |
    +--- Account origem
    |
    +--- Account destino
    |
    +--- FinancialTransaction
            |
            +--- saída
            +--- entrada


# 21. Integridade

O banco deve garantir:

- Foreign Keys válidas;
- valores monetários não negativos quando aplicável;
- dias de fechamento válidos;
- dias de vencimento válidos;
- e-mail único;
- relacionamentos válidos;
- identificadores únicos.


# 22. Exclusão

Entidades financeiras não devem ser fisicamente excluídas quando já possuírem histórico financeiro.

Exemplos:

Expense
Invoice
Installment
Payment
FinancialTransaction
Transfer

Devem permanecer registradas.

Utilizar status quando necessário.


# 23. Conta e cartão

Conta bancária e cartão de crédito são conceitos diferentes.

Uma conta representa dinheiro disponível.

Um cartão representa crédito utilizado que posteriormente será pago através de uma conta.

Não misturar esses conceitos no modelo.


# 24. Fatura e despesa

Despesa representa o compromisso financeiro.

Fatura representa a consolidação das compras do cartão.

Não utilizar a fatura como substituta da despesa.


# 25. Fatura e pagamento

O pagamento da fatura é uma operação diferente da compra.

Exemplo:

Compra:

R$ 500,00

Fatura:

R$ 500,00

Pagamento:

R$ 500,00

Conta:

- R$ 500,00


# 26. Projeções

Projeções não devem criar registros em FinancialTransaction.

A projeção deve ser calculada a partir de:

- receitas previstas;
- despesas pendentes;
- parcelas futuras;
- faturas futuras;
- parcelamentos.


# 27. Histórico

O histórico financeiro deve ser preservado.

Não utilizar:

DELETE FROM expenses

para "apagar" uma despesa que já existiu.

Utilizar:

status

ou operações de estorno/cancelamento.


# 28. Modelo preparado para evolução

O modelo deve permitir futura implementação de:

- investimentos;
- importação bancária;
- múltiplas moedas;
- contas compartilhadas;
- usuários compartilhados;
- notificações;
- auditoria completa;
- anexos;
- comprovantes;
- recorrências.

Essas funcionalidades não devem ser implementadas na V1.


# 29. Regra importante sobre recorrência

Despesas recorrentes, como:

- internet;
- energia;
- aluguel;
- assinaturas;

podem ser implementadas futuramente.

A V1 não precisa possuir um sistema completo de recorrência.

Entretanto, a arquitetura não deve impedir sua implementação futura.


# 30. Regra importante sobre saldo

O saldo de uma conta deve ser derivado das movimentações financeiras.

O campo initial_balance representa apenas o saldo inicial informado pelo usuário.

Movimentações posteriores devem determinar o saldo atual.


# 31. Regra importante sobre projeção

Uma projeção deve responder perguntas como:

"Quanto provavelmente terei disponível em dezembro?"

ou:

"Quanto já tenho comprometido no cartão em dezembro?"

A projeção deve considerar compromissos futuros conhecidos.


# 32. Regra importante sobre cartão

Uma compra parcelada deve comprometer o limite conforme as regras reais do cartão.

O cálculo definitivo do limite utilizado deverá considerar:

- compras abertas;
- parcelas futuras;
- pagamentos;
- estornos;
- ajustes.

A regra detalhada deverá ser validada durante a implementação do módulo de cartões.


# 33. Regra importante sobre estorno

Estorno não é exclusão.

O usuário deve conseguir identificar:

- compra original;
- data;
- valor;
- estorno;
- impacto na fatura.


# 34. Regra importante sobre pagamento parcial

Pagamento parcial não significa que a fatura foi paga.

Exemplo:

Fatura:
R$ 2.000

Pagamento:
R$ 500

Status:
PARCIALMENTE_PAGA

Saldo:
R$ 1.500


# 35. Regra importante sobre parcelamento de fatura

O parcelamento do saldo da fatura deve ser uma operação posterior.

Não modificar silenciosamente as compras originais.

O sistema deve preservar:

- compras originais;
- fatura original;
- pagamento parcial;
- saldo;
- parcelamento;
- parcelas futuras.


# 36. Regra de evolução do modelo

Antes de criar uma nova entidade, verificar se:

1. ela representa um conceito real do domínio;
2. seus dados possuem ciclo de vida próprio;
3. existe necessidade de relacionamento independente;
4. ela melhora a clareza do modelo.

Evitar criar tabelas apenas para representar detalhes que poderiam ser atributos simples.


# 37. Estado atual

Este modelo representa a proposta inicial do domínio da V1.

Durante a implementação, caso seja identificada uma inconsistência:

1. interromper a implementação daquela parte;
2. documentar o problema;
3. apresentar a proposta de alteração;
4. aguardar aprovação.

O modelo de domínio deve ser alterado conscientemente, pois ele impactará:

- banco;
- migrations;
- entidades;
- API;
- frontend;
- testes.