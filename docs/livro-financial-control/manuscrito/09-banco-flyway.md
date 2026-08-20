# PARTE VIII — Persistência e evolução do banco

# Capítulo 9 — PostgreSQL, modelo físico e Flyway

Este capítulo descreve o estado físico encontrado no repositório em agosto de
2026. O modelo é controlado por trinta migrations Flyway, de V1 a V30, e contém
27 tabelas. As visões de contas a pagar, contas a receber, projeções, dashboard
e relatórios não possuem tabelas próprias.

> **ARQUITETURA — Uma única fonte para o schema**
>
> O Flyway cria e evolui o banco. O Hibernate usa
> `spring.jpa.hibernate.ddl-auto=validate`: ele verifica o mapeamento, mas não
> cria nem corrige tabelas. Isso impede que o comportamento do banco varie
> conforme a inicialização da aplicação.

## 9.1 PostgreSQL no Financial Control

O banco oficial é PostgreSQL 18. No desenvolvimento, o Compose usa a imagem
`postgres:18-alpine`. A escolha aparece simultaneamente em
`docker-compose.yml`, no contrato de ambiente e nos testes Testcontainers.

As correspondências principais são:

| Conceito | PostgreSQL | Java |
|---|---|---|
| Identificador | `UUID` | `UUID` |
| Dinheiro | `NUMERIC(19,2)` | `BigDecimal` |
| Instante absoluto | `TIMESTAMPTZ` | `Instant` |
| Data financeira | `DATE` | `LocalDate` |
| Estado booleano | `BOOLEAN`, coluna `active` | `boolean`, propriedade `isActive` |

### UUID v7

Os identificadores normais são gerados na aplicação por
`br.com.financialcontrol.UuidV7`. As entidades não usam `@GeneratedValue`, e as
colunas não têm `DEFAULT` gerador. O projeto evita duas autoridades concorrentes
para IDs.

A V30 usa `uuidv7()` do PostgreSQL somente durante o backfill histórico de
`income_movements`. Isso não muda a política das operações normais.

### Valores monetários

Dinheiro nunca usa `float` ou `double`. O banco fixa duas casas decimais e os
Services normalizam valores com `RoundingMode.HALF_UP`.

Exemplo de parcelamento de R$ 100,00 em três partes:

```text
base = 100,00 / 3, arredondado para baixo = 33,33
primeira parcela = 100,00 - (33,33 × 2) = 33,34
demais parcelas = 33,33
soma = 100,00
```

O residual fica na primeira parcela. Há uma divergência importante para valores
muito pequenos, documentada em 9.11.

### Datas e timezone

`Instant` representa eventos absolutos e é persistido em UTC. `LocalDate`
representa vencimentos, datas de compra, recebimento e pagamento. As regras de
calendário usam `America/Sao_Paulo`; não dependem do timezone do navegador.

## 9.2 Como o Flyway evolui o schema

As migrations ficam em:

```text
backend/src/main/resources/db/migration/
```

Elas são aplicadas em ordem numérica. Uma migration executada não é reescrita:
mudanças posteriores usam uma nova versão.

| Versão | Marco principal |
|---|---|
| V1 | `users` |
| V2–V5 | contas, categorias, cartões e metas |
| V6 | receitas |
| V7 | faturas no modelo inicial |
| V8–V10 | despesas, parcelas e pagamentos |
| V11–V14 | transferências, pagamentos de fatura, tabela posteriormente superada e contribuições |
| V15–V16 | unicidade de categorias e responsável nullable em receitas |
| V17 | status de payment e ajustes de parcela |
| V18–V25 | cartões/faturas da Fase 9 e status `SETTLED_BY_AGREEMENT` |
| V26–V27 | Agreements, settlements e alocações |
| V28 | transferências, acertos e trava de saldo inicial |
| V29 | vínculo de meta à conta e resgates |
| V30 | movimentações de receita e backfill |

### Evolução de status sem apagar história

A V7 criou o primeiro CHECK de fatura. A V19 substituiu esse CHECK pelo conjunto
`SCHEDULED`, `OPEN`, `CLOSED` e `PAID`; a V25 adicionou
`SETTLED_BY_AGREEMENT`. A migration antiga permanece como registro da evolução,
mas o contrato efetivo é o CHECK final.

### Valores derivados não viram colunas

O schema não persiste:

- saldo atual, reservado ou disponível da conta;
- total pago ou remaining da parcela;
- totais de fatura;
- limite usado e disponível do cartão;
- valor acumulado e percentual de meta;
- resultados de payables, receivables, projections, dashboard ou reports.

Persistir esses valores criaria uma segunda fonte sujeita a dessincronização.
Fatos — pagamentos, alocações, créditos e movimentos — são persistidos; totais
são calculados.

## 9.3 Inventário das 27 tabelas

### Cadastro e identidade

- `users`
- `accounts`
- `categories`
- `credit_cards`

### Receitas

- `incomes`
- `income_movements`

### Despesas

- `expenses`
- `expense_installments`
- `expense_installment_adjustments`
- `payments`

### Cartões, faturas e créditos

- `credit_card_invoices`
- `credit_card_invoice_payments`
- `credit_card_invoice_payment_allocations`
- `credit_card_credits`
- `credit_card_credit_applications`
- `credit_card_invoice_adjustments`
- `credit_card_invoice_adjustment_allocations`
- `card_purchase_account_refunds`
- `credit_card_invoice_installments` — tabela legada, sem uso operacional atual

### Agreements

- `credit_card_invoice_agreements`
- `credit_card_invoice_agreement_settlements`
- `credit_card_invoice_agreement_settlement_allocations`

### Movimentação entre contas

- `transfers`
- `account_balance_adjustments`

### Metas

- `financial_goals`
- `goal_contributions`
- `goal_redemptions`

## 9.4 Relacionamentos principais

```text
users
 ├─ accounts
 ├─ categories
 ├─ credit_cards ─ credit_card_invoices
 ├─ incomes ─ income_movements ─ accounts
 ├─ expenses ─ expense_installments
 │                         ├─ payments ─ accounts
 │                         └─ credit_card_invoices
 ├─ transfers ─ accounts (origem e destino)
 └─ financial_goals ─ accounts
                      ├─ goal_contributions
                      └─ goal_redemptions
```

Uma despesa parcelada pertence à tabela `expenses`, mas cada parcela é um fato
em `expense_installments`. Em compras no cartão, a parcela — e não a despesa —
aponta para a fatura. Assim, uma compra em dez vezes pode atravessar dez ciclos.

## 9.5 Constraints e índices

O banco protege invariantes que não devem depender somente da aplicação.

| Tipo | Exemplos |
|---|---|
| Chave primária | `id` UUID |
| Unicidade de ownership | `UNIQUE (id, user_id)` |
| CHECK de status | despesas, receitas, faturas, payments, movements |
| CHECK de alvo | coerência entre `payment_method`, conta e cartão |
| Unicidade de parcela | `(expense_id, installment_number)` |
| Uma fatura aberta | índice parcial único por cartão |
| Categoria sem duplicata lógica | índice por usuário, tipo e `lower(name)` |

Índices de `user_id`, datas e FKs sustentam as consultas multiusuário. Índice
não substitui regra de ownership: ele melhora busca; a FK composta preserva
integridade.

## 9.6 Ownership em duas camadas

O Service sempre consulta com o `userId` extraído do principal autenticado:

```java
accountRepository.findByIdAndUserId(accountId, authenticatedUser.userId())
```

O banco repete a proteção em relacionamentos:

```sql
FOREIGN KEY (expense_id, user_id)
    REFERENCES expenses (id, user_id)
```

Isso impede que uma falha futura no Service grave uma parcela do usuário A em
uma despesa do usuário B.

> **SEGURANÇA — Por que retornar 404**
>
> Consultas por `(id, user_id)` tratam um recurso alheio como inexistente. Além
> de isolar dados, isso evita confirmar a existência de IDs pertencentes a outro
> usuário.

O JPA normalmente mapeia a coluna de relação simples. A FK composta permanece
no Flyway. Essa separação mantém o mapeamento legível sem enfraquecer o modelo
físico.

## 9.7 Entities e enums

Há 27 classes `@Entity`, uma para cada tabela. Não existem entidades JPA para
payables, receivables, projections, dashboard e reports, porque esses recursos
são visões calculadas.

Enums Java acompanham os CHECKs do banco:

- conta: `BANK_ACCOUNT`, `CASH`;
- receita: `EXPECTED`, `RECEIVED`, `CANCELLED`;
- despesa: `OPEN`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED`;
- payment: `ACTIVE`, `REVERSED`;
- fatura: `SCHEDULED`, `OPEN`, `CLOSED`, `PAID`,
  `SETTLED_BY_AGREEMENT`;
- meta: `ACTIVE`, `COMPLETED`, `CANCELLED`.

Duas exceções são intencionais:

1. `payments.type` continua `String`, sem enum ou CHECK, porque seus valores
   oficiais permanecem pendentes;
2. `credit_card_invoice_installments.status` pertence ao modelo superado e não
   ganhou novo contrato.

## 9.8 Visões derivadas

| Recurso | Implementação | Tabela própria |
|---|---|---|
| Payables | `PayablesService` | não |
| Receivables | `ReceivablesService` | não |
| Projections | `ProjectionService` | não |
| Dashboard | `DashboardService` | não |
| Reports | `ReportsService` | não |

Uma linha de payable pode representar parcela ACCOUNT/NONE ou fatura com
remaining positivo. Receivables deriva receitas e movimentações. Projeções
combinam saldo atual e eventos futuros. Dashboard agrega serviços existentes.
Reports calcula visões e, quando solicitado, produz PDF com OpenPDF.

## 9.9 Testes do contrato físico

Os testes usam PostgreSQL real em Testcontainers.

| Teste | O que protege |
|---|---|
| `SchemaContractTest` | tabelas, CHECKs e ausência de colunas derivadas |
| `OwnershipAndPersistenceTest` | FKs compostas e isolamento |
| `Phase8PersistenceContractTest` | schema de parcelas, payments e adjustments |
| `V30IncomeMovementsBackfillTest` | migração e backfill de movimentos |

O `SchemaContractTest` passou durante a auditoria. Isso comprova alinhamento com
o contrato que o próprio teste expressa; não prova que toda regra documental
esteja correta. A seção seguinte mostra por quê.

## 9.10 Como criar uma nova migration

1. Confirmar que a regra de negócio está decidida.
2. Escolher a próxima versão; nunca renomear ou editar uma migration aplicada.
3. Criar tabela ou `ALTER TABLE` com tipos, nulabilidade e constraints.
4. Incluir `user_id` e ownership composto quando o dado for financeiro.
5. Não persistir um valor definido como derivado.
6. Atualizar a Entity de forma compatível.
7. Criar teste de schema e teste de integração do comportamento.
8. Executar `.\mvnw.cmd test` e `.\mvnw.cmd verify`.

> **NÃO FAÇA**
>
> Não use `ddl-auto=update`, não adicione `DEFAULT` gerador de UUID, não remova
> FK composta para simplificar JPA e não crie tabela para uma visão calculada.

## 9.11 Divergências encontradas na auditoria

Estas divergências pertencem ao estado auditado. Não foram corrigidas durante a
criação do livro.

### INC-01 — Parcela de R$ 0,00

**PENDENTE — severidade alta.**

A documentação e `ExpenseService.splitInstallmentAmounts` permitem parcelas
zero quando inevitáveis. O teste
`ExpenseFinancialPhase8Test.shouldSplitVerySmallTotalAcrossInstallmentsWithoutLosingCents`
confirma:

```text
R$ 0,01 em 3× → 0,01; 0,00; 0,00
```

Porém, a V9 contém:

```sql
CONSTRAINT ck_expense_installments_amount CHECK (amount > 0)
```

Persistir esse exemplo falharia. O teste cobre o algoritmo, não o INSERT.

### INC-02 — Motivo do ajuste de parcela

**PENDENTE — severidade média.**

RN232 e `ExpenseService` exigem `reason`. Na V23, porém, a coluna de ajuste de
parcela é nullable e o CHECK aceita `NULL`. Ajuste de fatura, na mesma migration,
usa `NOT NULL`.

### INC-03 — Cabeçalho legado de receita

**DECIDIDO como legado; exposição ainda pendente de harmonização.**

Novos RECEIPTs são gravados em `income_movements` e não preenchem
`incomes.account_id` ou `received_date`. Algumas respostas ainda leem esses
campos do cabeçalho, podendo retornar nulos apesar de existir RECEIPT ativo.

### INC-04 — Filtro de `/incomes` por conta

**PENDENTE.**

`IncomeRepository.searchByUser` filtra a conta do cabeçalho legado.
`searchReceivables` usa corretamente `EXISTS` em RECEIPT ativo. Os dois recursos
podem responder de modo diferente para receitas criadas após a V30.

### INC-05 — Tabela superada ainda presente

**Dívida conhecida.**

`credit_card_invoice_installments` foi superada por Agreement + settlement +
despesa CREDIT_CARD. Tabela, Entity e Repository permanecem; Services não a
usam. Sua remoção exigiria migration e decisão sobre histórico.

### INC-06 — Lista documental de FKs incompleta

**PENDENTE documental.**

`docs/23` §265 não enumera várias FKs introduzidas nas fases 9–17, embora elas
existam nas migrations e sejam protegidas por testes.

### INC-07 — Backfill da V29

**PENDENTE para bases legadas.**

A V29 obtém `financial_goals.account_id` de contribuições anteriores e depois
aplica `NOT NULL`. Uma meta histórica sem contribuição não teria conta para o
backfill. Novas metas não sofrem esse problema porque a criação exige conta.

### INC-08 — Bloqueios oficiais respeitados

**PENDENTE por governança, sem defeito de implementação conhecido.**

- `payments.type`: coluna sem valores oficiais, enum ou CHECK;
- edição cadastral de parcela já vinculada a fatura: decisão deferida.

Esses pontos não devem receber implementação provisória.

