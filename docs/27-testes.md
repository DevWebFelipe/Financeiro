# Testes — Financial Control

## 0. Hierarquia e stack de testes

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

Backend: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL 18).

Frontend: framework oficial do Angular 22.x.

E2E: Playwright (Chromium) — Fase 22 **CONCLUÍDA / APROVADA COM RESSALVAS**. Sem Cypress. Sem CI/CD nesta fase.

PDF: OpenPDF. Gráficos: Apache ECharts.


## 1. Objetivo

Este documento define a estratégia de testes do Financial Control.

O objetivo é garantir:

- funcionamento correto;
- integridade financeira;
- isolamento entre usuários;
- segurança;
- estabilidade da API;
- previsibilidade dos cálculos;
- proteção contra regressões.


# 2. Princípio

Toda regra financeira crítica deve possuir teste automatizado.


Regra indefinida não deve ter teste que cristalize uma suposição.

```text
TESTE NÃO DEFINIDO → REGRA NÃO DEFINIDA → IMPLEMENTAÇÃO BLOQUEADA
```

Pendências oficiais (`AGENTS.md` §28.3 / `docs/23` §269): `payments.type` (269.1); edição de parcela **já em fatura** (269.2.7 deferido). O **269.3 (rateio)** e o **269.4 (estorno no cartão)** estão **fechados** (RN247, RN117). A edição ACCOUNT/NONE, reverse de payment e adjustments HTTP da Fase 8 estão implementados — manter os testes verdes.

Depois da decisão: documentação → teste → implementação.


# 3. Regra

Código não deve ser considerado concluído apenas porque:

- compila;
- inicia;
- endpoint responde;
- tela funciona.


Uma funcionalidade somente será considerada concluída quando seus testes relevantes estiverem implementados e passando.


# 4. Tipos de teste

A aplicação utilizará:

- testes unitários;
- testes de integração;
- testes de API;
- testes de segurança;
- testes de persistência;
- testes end-to-end quando justificável.


# 5. Testes unitários

Testes unitários devem validar regras isoladas.


Exemplos:

- cálculo de parcelas;
- arredondamento;
- cálculo de saldo;
- determinação de fatura;
- cálculo de projeção;
- cálculo de percentual de meta.


# 6. Testes de integração

Devem validar integração entre:

- service;
- repository;
- PostgreSQL;
- transações.


# 7. Testes de API

Devem validar:

- endpoint;
- autenticação;
- autorização;
- validação;
- status HTTP;
- request;
- response.


# 8. Testes de segurança

Devem validar principalmente:

- isolamento por usuário;
- autenticação;
- autorização;
- acesso indevido;
- manipulação de IDs.


# 9. Testes financeiros

Operações financeiras devem possuir testes específicos.


# 10. Precisão monetária

Nunca utilizar:

float;

double;


para representar dinheiro nos cálculos.


# 11. Java

Utilizar:

BigDecimal


para valores monetários.


# 12. Testes de arredondamento

Devem existir testes para:

- valores exatos;
- valores com meio centavo;
- valores com várias casas;
- parcelamentos;
- diferenças de arredondamento.


# 13. Exemplo

Valor:

100.00


3 parcelas:

33.34

33.33

33.33


A soma deve resultar exatamente em:

100.00


# 14. Teste

A soma das parcelas nunca pode apresentar diferença residual.


# 15. Testes de parcelas

Testar:

1 parcela;

2 parcelas;

3 parcelas;

12 parcelas;

24 parcelas;

parcelas com valores diferentes.


# 16. Testes de parcelas diferentes

Exemplo:

Total:

1000.00


Parcelas:

300.00

300.00

400.00


Deve ser aceito quando a soma for correta.


# 17. Teste de valor incorreto

Exemplo:

Total:

1000.00


Parcelas:

300.00

300.00

300.00


Deve ser rejeitado.


# 18. Teste de edição

Parcela `OPEN`: `amount` e `due_date` por operação da parcela; soma deve permanecer igual a `expenses.total_amount`; senão rejeitar + rollback. Sem redistribuição. Não editar `PARTIALLY_PAID` / `PAID` / `CANCELLED` / `REFUNDED`.


# 19. Teste

Parcela paga não deve ser alterada silenciosamente.


# 20. Testes de status

Testar transições válidas e inválidas.


# 21. Despesas

Status persistidos:

OPEN

PARTIALLY_PAID

PAID

CANCELLED

REFUNDED


OVERDUE é derivado (não persistido). **1/1:** status OPEN ou PARTIALLY_PAID e dueDate < hoje em `America/Sao_Paulo`. **N>1:** a despesa é overdue quando existe pelo menos uma parcela overdue segundo RN241. A API expõe `overdue` (boolean). PAID, CANCELLED e REFUNDED nunca são overdue.


# 22. Receita

Estados relevantes:

EXPECTED — duplicata ativa, não recebida

RECEIVED — recebimento efetivado

CANCELLED — duplicata inutilizada

Cancelamento (`EXPECTED` → `CANCELLED`) e estorno (`RECEIVED` → `EXPECTED`) são operações diferentes. Não existe status `REVERSED`.


# 23. Fatura

Status persistidos:

OPEN

CLOSED

PARTIALLY_PAID

PAID


OVERDUE é derivado da data de vencimento (não persistido).


# 24. Testes de transição (Fase 7)

Testar:

OPEN -> PAID (pagamento integral)

OPEN -> CANCELLED

OPEN -> PARTIALLY_PAID

PARTIALLY_PAID -> PAID

PARTIALLY_PAID -> REFUNDED

PAID -> REFUNDED


Não testar `OPEN -> REFUNDED` como transição válida. `OPEN` não se estorna (RN214).


# 25. Testes inválidos

Testar transições que devem ser rejeitadas.

Exemplos da Fase 7:

PAID -> OPEN

OPEN -> REFUNDED

PARTIALLY_PAID -> CANCELLED

PAID -> CANCELLED

CANCELLED -> OPEN

REFUNDED -> OPEN

segundo refund sobre REFUNDED

PUT sobre PARTIALLY_PAID, PAID, CANCELLED ou REFUNDED.


# 26. Testes de pagamento

Testar:

pagamento integral;

pagamento parcial;

múltiplos pagamentos;

pagamento acima do valor devido;

pagamento zero;

pagamento negativo.


# 27. Pagamento integral

Despesa:

1000


Pagamento:

1000


Resultado:

PAID


# 28. Pagamento parcial

Despesa:

1000


Pagamento:

400


Resultado:

PARTIALLY_PAID


Saldo:

600


# 29. Múltiplos pagamentos

Despesa:

1000


Pagamento 1:

300


Pagamento 2:

300


Pagamento 3:

400


Resultado:

PAID


# 30. Pagamento excedente

Despesa:

1000


Pagamento:

1001


Deve ser rejeitado.


# 31. Pagamento concorrente

Dois pagamentos simultâneos não podem ultrapassar o valor devido.


# 32. Teste

Despesa:

1000


Duas requisições simultâneas:

700

700


Resultado permitido:

somente uma combinação que não ultrapasse 1000.


# 33. Testes de saldo

Testar:

saldo positivo;

saldo zero;

saldo insuficiente;

recusa de operação normal que geraria saldo negativo (transferência, pagamento de despesa, pagamento de fatura);

estorno de receita mesmo quando o saldo resultante for negativo;

estorno de despesa restaura o saldo (pagamentos de despesa `REFUNDED` deixam de ser subtraídos) e **não** usa a exceção de saldo negativo da receita;

transferências;

pagamento de despesa sem saldo;

pagamento de fatura limitado ao saldo da conta.


# 34. Transferência (Fase 14)

Contrato: `docs/24` §19.5. Status: `CONCLUÍDA E APROVADA`.

Resultado final factual da suíte no encerramento da Fase 14:

- Total: 374
- Passaram: 374
- Falharam: 0
- `mvn test` = PASS
- `mvn verify` = PASS
- Flyway / Testcontainers = PASS

Grupos principais de cobertura adicionados: RN010A; backfill V28; concorrência; payment × transfer; transfer A→B × B→A; duas saídas simultâneas; acerto; saldo; inativação; schema V28. Classes: `Phase14ApiTest`, `Phase14Rn010aApiTest`, `Phase14ConcurrencyExtraTest`, `TransferConcurrencyTest`, `SchemaContractTest`.

Pendências classificadas pela reauditoria como **COBERTURA / NÃO BLOQUEANTES** (não reabrem a Fase 14): cobertura adicional de reverse de pagamento de fatura; cobertura adicional de refund ACCOUNT; algumas combinações de corrida `PUT` initial-balance × primeira movimentação; inconsistência menor de `markInitialBalanceLocked` no fluxo de antecipação de Agreement (o payment ainda bloqueia RN010A).

Cobertura de API implementada em `Phase14ApiTest`:

conta A (`BANK_ACCOUNT`) -> conta B (`BANK_ACCOUNT`).

Rejeitar `CASH` como origem ou destino.

Rejeitar transferência futura.

Aceitar transferência retroativa.

Reversão `ACTIVE` → `REVERSED` com checagem de saldo na conta debitada.

Sem desreversão.


# 35. Transferência

Após:

A:

1000


Transferência:

300


B:

500


Resultado:

A:

700


B:

800


# 36. Patrimônio

Transferência não deve alterar patrimônio total.


# 37. Transferência

Conta origem e destino devem ser diferentes.


# 38. Transferência

Valor zero deve ser rejeitado.


# 39. Transferência

Valor negativo deve ser rejeitado.


# 40. Transferência

Saldo insuficiente deve ser rejeitado (criação e reversão).


# 40A. Acerto de Saldos (Fase 14)

Testar:

- usuário informa `reportedBalance`; sistema calcula diferença;
- `reportedBalance < 0` rejeitado;
- acerto retroativo (as-of-date);
- acerto futuro rejeitado;
- múltiplos acertos independentes;
- reversão com efeito inverso e checagem de saldo;
- `BANK_ACCOUNT` e `CASH` permitidos; cartão não.


# 40B. Saldo inicial (Fase 14)

Testar:

- criar conta sem `initialBalance` ⇒ `0,00`;
- criar conta com `initialBalance` informado;
- `PUT /accounts/{id}/initial-balance` sem movimentações;
- rejeitar após primeira movimentação efetiva (RN010A);
- rejeitar mesmo se a movimentação for depois cancelada/revertida/estornada;
- não bloquear por despesa OPEN / receita EXPECTED / fatos só de cartão sem movimento na conta;
- correção posterior via acerto.


# 40C. Inativação com saldo (Fase 14; emendada Fase 15)

Testar:

- `totalBalance != 0` → rejeitar deactivate;
- `reservedAmount > 0` (mesmo com `totalBalance == 0`) → rejeitar deactivate;
- `totalBalance == 0` **e** `reservedAmount == 0` → permitir.


# 40D. Metas financeiras (Fase 15)

**Status:** `CONCLUÍDA E APROVADA`. Contrato: `docs/24` §19.6 / `docs/25` §54E.

Classes: `FinancialGoalApiTest`, `FinancialGoalConcurrencyTest`, `GoalProgressTest`, `GoalReservationFoundationTest`.

**Cadastro e edição**

- criar meta vinculada a `BANK_ACCOUNT` e a `CASH`;
- rejeitar conta inativa ou de outro usuário;
- editar `name`, `description`, `targetAmount`, `targetDate` em `ACTIVE`;
- rejeitar edição em `COMPLETED` / `CANCELLED`;
- rejeitar `targetAmount <= 0`;
- permitir nomes duplicados no mesmo usuário.

**Contribuição**

- contribuição válida reduz `availableBalance`, não altera `totalBalance`;
- contribuição usando exatamente todo o `availableBalance`;
- contribuição acima do objetivo (`progressPercent > 100%`);
- rejeitar contribuição em `COMPLETED` / `CANCELLED`;
- rejeitar contribuição com `availableBalance` insuficiente;
- rejeitar data futura;
- rejeitar `amount <= 0`.

**Resgate**

- resgate em `ACTIVE` → permitido;
- resgate parcial em `ACTIVE` → permitido;
- resgate total em `ACTIVE` → permitido;
- resgate em `COMPLETED` → permitido;
- resgate parcial em `COMPLETED` → permitido;
- resgate total em `COMPLETED` → permitido;
- resgate em `CANCELLED` → rejeitado;
- resgate **não** altera `status` da meta;
- após resgate total de `COMPLETED`, status permanece `COMPLETED`;
- resgate aumenta `availableBalance`, não altera `totalBalance`;
- rejeitar resgate > `currentAmount`;
- rejeitar `redemptionDate` futura;
- resgate retorna sempre à conta vinculada (sem escolha de conta).

**Derivados**

- `currentAmount = SUM(contributions) − SUM(redemptions)`;
- `progressPercent` com `HALF_UP`, escala 2, sem teto (`33.33%`, `16.67%`, `100%`, `110%`);
- meta `ACTIVE` com `currentAmount = 0` após resgate total;
- `COMPLETED` + `currentAmount > 0` é válido;
- `COMPLETED` + `currentAmount = 0` é válido;
- `COMPLETED` + `progressPercent = 0%` após resgate total — status permanece `COMPLETED`.

**Conclusão e cancelamento**

- concluir abaixo de 100%, em 100% e acima de 100%;
- concluir com `currentAmount = 0`;
- concluir **não** altera automaticamente ao atingir 100% por contribuição;
- cancelar com `currentAmount = 0` (somente `ACTIVE`);
- rejeitar cancelar com reservado > 0;
- rejeitar `COMPLETED` → `CANCELLED`;
- `COMPLETED` bloqueia contribuição e edição; **permite** resgate;
- `CANCELLED` bloqueia contribuição, resgate e edição;
- `COMPLETED` não pode voltar para `ACTIVE`.

**Saldo e conta**

- `GET /accounts/{id}/balance` expõe `totalBalance`, `reservedAmount`, `availableBalance`;
- operações existentes (pay, transfer, invoice pay) respeitam `availableBalance`;
- contribuição conta como primeira movimentação (RN010A);
- rejeitar inativação com reservado > 0.

**Segurança, paginação e concorrência**

- isolamento entre usuários (404) em GET e mutações (contribuir, resgatar, completar, cancelar);
- 401 sem autenticação;
- `page < 0` e `size < 1` → **400** `BUSINESS_RULE_VIOLATION`;
- dois aportes concorrentes não ultrapassam `availableBalance`;
- dois resgates concorrentes não ultrapassam `currentAmount`;
- aporte e resgate concorrentes na mesma meta mantêm invariantes de saldo;
- duas metas na mesma conta disputando `availableBalance` são serializadas pelo lock da conta.


# 40E. Contas a pagar (Fase 16)

Contrato: `docs/24` §19.7 / `docs/25` §66.

**Status:** `CONCLUÍDA E APROVADA`. Classe: `PayablesApiTest` (14 testes HTTP + regras da visão). Suíte informada no fechamento: 425 testes, `mvn test` / `mvn verify` / Spotless com sucesso. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes).

**Não** testar regra indefinida. **Não** persistir remaining. **Não** criar tabela `payables`.

**Elegibilidade ACCOUNT/NONE**

- parcela `OPEN` / `PARTIALLY_PAID` com remaining > 0 aparece;
- 1/1 aparece como uma linha INSTALLMENT (parcela interna);
- N>1: uma linha por parcela com remaining > 0; o período usa o `due_date` **da parcela**, não RN226;
- pagamento parcial: `remainingAmount` (não `originalAmount` / `totalAmount` da despesa);
- remaining 0 não aparece e não aumenta `totalRemaining`;
- `CANCELLED` não aparece;
- `REFUNDED` não aparece;
- NONE aberta aparece (RN123);
- ACCOUNT aberta aparece.

**Cartão e dupla contagem (obrigatório)**

- fatura `SCHEDULED` / `OPEN` / `CLOSED` com remaining > 0 aparece como `INVOICE`;
- despesa `CREDIT_CARD` não aparece como linha;
- parcela de cartão não aparece como linha;
- soma despesa/parcela de cartão + fatura **nunca** ocorre;
- fatura remaining 0 (`PAID`, `SETTLED_BY_AGREEMENT`, OPEN remaining 0) não aumenta total;
- settlement de Agreement reduz remaining da fatura na visão sem regra paralela;
- a nova obrigação do Agreement entra só via faturas futuras (remaining > 0), não junto com a fatura liquidada.

**Período**

- sem período: inclui futuras elegíveis;
- `year`+`month` = mês **selecionado** (outubro/2026 consultável em agosto/2026);
- `startDate`/`endDate` no `due_date` da linha;
- 12× R$ 100: setembro devolve **uma** parcela (~100 remaining), não R$ 1.200;
- `includeWithoutDueDate` documentado; no modelo vigente todas as linhas têm `due_date`.

**Overdue**

- derivado; sem coluna/status persistido;
- parcela: remaining > 0 e `due_date` < hoje (`America/Sao_Paulo`);
- fatura: remaining > 0, `OPEN`/`CLOSED`, `due_date` < hoje;
- `SCHEDULED` nunca overdue;
- filtro `overdue=true|false` independente de `status`.

**Filtros**

- `status` múltiplo (`OPEN,PARTIALLY_PAID`);
- `creditCardId`;
- `withoutCreditCard=true`;
- `categoryId` e `responsibleType` só restringem INSTALLMENT; faturas não são excluídas só por esses filtros;
- `search` em descrição/notas/boleto (INSTALLMENT) e nome do cartão (INVOICE);
- combinação de filtros (interseção).

**Ordenação e paginação**

- `sort` nos campos oficiais + `direction`;
- desempate `id ASC`;
- `size` default 20, máximo 100; `page < 0` / `size < 1` / `size > 100` → 400;
- totais (`totalRemaining`, `totalOriginal`, `totalPaid`, `totalItems`) do **universo filtrado**, não da página (obrigatório).

**Fora da visão**

- transferência não entra;
- acerto de saldo não entra;
- saldo inicial não entra;
- `reservedAmount` de meta não entra.

**Isolamento e auth (obrigatório)**

- usuário B nunca recebe linhas do usuário A;
- 401 sem Bearer.

**Remaining**

- payables não inventa fórmula; usa remaining oficial de parcela e de fatura (ajustes, créditos, settlement já refletidos).


# 40F. Contas a receber (Fase 17 — Parte 1)

Contrato: `docs/24` §19.8 / `docs/25` §67.

**Status:** Parte 1 **CONCLUÍDA E APROVADA**. Auditoria: **APROVADA COM RESSALVAS** (não bloqueantes). Endpoint: `GET /api/v1/receivables`. Classe: `ReceivablesApiTest` — **35/35**. `Clock` fixo em `2026-08-17T15:00:00Z` / hoje financeiro `2026-08-17` em `America/Sao_Paulo`; override de bean apenas neste teste.

Regressão no fechamento: `IncomeApiTest` 15/15; `PayablesApiTest` 14/14; `mvn verify` **460/460**; BUILD SUCCESS.

**Não** criar infraestrutura especial de concorrência na Parte 1 (D30). A suíte da Parte 2 está em §40G (**implementada**).

Melhorias futuras de teste (não são falhas da Parte 1): token inválido/expirado; período só com `startDate` ou só com `endDate`; fronteira UTC/São Paulo à meia-noite; `categoryId`/`accountId` de outro usuário.

**Não** testar a Parte 2 nesta suíte da Parte 1. A suíte da Parte 2 é §40G (**implementada**). **Não** persistir remaining. **Não** criar tabela `receivables`. **Não** testar `dueDate` como alias. **Não** testar receita sem `expectedDate`.

**Elegibilidade**

- `EXPECTED` futura aparece na consulta padrão;
- `EXPECTED` com `expectedDate` < hoje (`America/Sao_Paulo`) é `overdue=true`;
- `EXPECTED` com `expectedDate >= hoje` é `overdue=false`;
- `RECEIVED` **não** aparece no padrão; aparece com `status=RECEIVED`;
- `CANCELLED` nunca aparece nem no resumo;
- estorno `RECEIVED` → `EXPECTED` recoloca a linha e reclassifica por `expectedDate` vs hoje.

**Período e dateType**

- período inclusivo sobre `expectedDate` quando `dateType=EXPECTED`;
- período sobre `receivedDate` quando `dateType=RECEIVED`;
- período sem `dateType` → 400;
- `status=EXPECTED` + `dateType=RECEIVED` → 400 **na Parte 1** (D88 / Parte 2: passa a ser permitido — §40G);
- `status=RECEIVED` + `dateType=EXPECTED` → 400;
- `status=RECEIVED` + `overdue` → 400;
- sem datas: operacional (abertas `EXPECTED`).

**Filtros**

- `status`, `overdue`, `categoryId`, `accountId`, `responsibleType`, `responsibleName`;
- `status=EXPECTED&accountId=<id>` → 200 vazio possível (não 400);
- combinação por interseção;
- query param desconhecido → 400;
- `year` / `month` / `search` **não** existem (desconhecidos → 400).

**Item e resumo**

- item **não** contém `remainingAmount` nem `receivedAmount`;
- item contém `expectedDate`, `overdue`, `responsibleType`, `responsibleName` (estes dois podem ser nulos até a evolução de Income);
- `summary` do universo filtrado: `futureAmount`, `overdueAmount`, `totalReceivableAmount` (= soma dos dois), `receivedAmount`;
- consulta padrão: `receivedAmount = 0.00` **na Parte 1** (D92-B / Parte 2: pode ser > 0 por RECEIPT parciais no universo EXPECTED — §40G);
- `status=RECEIVED`: totais a receber `0.00` e `receivedAmount` das filtradas.

**Ordenação e paginação**

- `sort` só nos campos oficiais + `direction`; padrão `expectedDate ASC`; desempate `id ASC`;
- `size` default 20, máximo 100; `page < 0` / `size < 1` / `size > 100` → 400.

**Isolamento e auth**

- usuário B nunca recebe linhas do usuário A;
- 401 sem Bearer.

**Consulta**

- filtros/ordenação/paginação no banco (contrato RN302). Não copiar a filtragem em memória de payables.

**Responsável**

- a visão filtra/retorna as colunas existentes;
- enquanto Income não gravar responsável, filtros por responsável podem devolver vazio;
- a evolução do `POST`/`PUT /incomes` para gravar responsável está **implementada** (Parte 2 / `docs/24` §19.9 / RN306 / §40G).


# 40G. Contas a receber — Fase 17 Parte 2

Contrato: `docs/24` §19.9 / `docs/25` §67A.

**Status:** **`CONCLUÍDA E APROVADA`**. Decisões D73–D94 **fechadas** e **implementadas**. Suíte validada: **481** testes; 0 falhas; `mvn verify` BUILD SUCCESS.

Classes: `IncomeMovementsApiTest`, `IncomeBalanceAsOfIntegrationTest`, `V30IncomeMovementsBackfillTest`; `ReceivablesApiTest` (D88, D92-B, D94); `SchemaContractTest` (`income_movements`). Reutilizar `Clock` / timezone `America/Sao_Paulo`. Concorrência: locks conforme `docs/24` §19.9.

## Testes adicionados (pós-auditoria)

- `ReceivablesApiTest`: D88 (`EXPECTED` + `dateType=RECEIVED`; RECEIPT ACTIVE/REVERSED histórico); D92-B (parcial + `summary.receivedAmount`).
- `IncomeBalanceAsOfIntegrationTest`: RN240 as-of por `movement_date`; reverse excluído do saldo.
- `V30IncomeMovementsBackfillTest`: backfill D83 (SQL da V30); duplicação; EXPECTED/CANCELLED; validação de dados inválidos.
- `IncomeMovementsApiTest`: reverse concorrente (recomendado).

## Sort `receivedDate` (D76)

O sort `receivedDate` em `/receivables` usa `incomes.received_date` (legado). Novos RECEIPTs **não** preenchem o cabeçalho; o teste de sort usa `containsExactlyInAnyOrder` quando aplicável.

## Acréscimos

- acréscimo simples (não movimenta conta);
- múltiplos acréscimos;
- acréscimo após recebimento parcial;
- acréscimo após `RECEIVED` (`RECEIVED` → `EXPECTED`);
- acréscimo em receita `CANCELLED` → 400;
- data futura → 400;
- data retroativa (inclusive `< expectedDate`) → permitido;
- POST **201** + objeto da movimentação;
- POST equivalente duas vezes cria dois fatos (não idempotente).

## Recebimentos

- recebimento parcial (status permanece `EXPECTED`);
- duas contas para a mesma receita;
- recebimento integral (remaining = 0 → `RECEIVED`);
- over-receipt → 400;
- recebimento com remaining = 0 → 400;
- recebimento sem conta → 400;
- conta de outro usuário;
- conta inativa no create;
- saldo da conta após RECEIPT (`+ valor`);
- data futura → 400;
- data retroativa → permitido;
- POST **201** + objeto da movimentação.

## Status e cancelamento (D73)

- `EXPECTED` → `RECEIVED` ao zerar;
- `EXPECTED` permanece `EXPECTED` após baixa parcial;
- `RECEIVED` → `EXPECTED` após novo acréscimo;
- `EXPECTED` sem RECEIPT ACTIVE → cancelar → `CANCELLED`;
- `EXPECTED` com RECEIPT ACTIVE → cancelar → 400;
- `EXPECTED` apenas com RECEIPT REVERSED → cancelar → `CANCELLED`;
- `RECEIVED` → cancelar → 400;
- `RECEIVED` + acréscimo → `EXPECTED` com RECEIPT ACTIVE → cancelar → 400;
- `CANCELLED` → cancelar → 400;
- receita `CANCELLED` sem novas movimentações (ACCRUAL / RECEIPT);
- sem estorno automático no cancelamento;
- histórico permanece após cancelar.

## Estornos

- estorno de RECEIPT (conta do próprio RECEIPT, remaining, status) → **200** + movimento `REVERSED`;
- estorno de ACCRUAL;
- estorno de ACCRUAL que deixaria remaining < 0 → 400;
- tentar estornar duas vezes → 400;
- saldo da conta após reverse (RN200: pode ficar negativo; não exige conta ativa);
- retorno do remaining e do status `RECEIVED` → `EXPECTED`;
- reverse após `CANCELLED` de fato já `REVERSED` → 400;
- se existir ACTIVE inconsistente em `CANCELLED` → rejeitar até saneamento.

## Histórico (`GET /incomes/{id}/movements`)

- fatos permanecem; reverse não apaga;
- sem linha artificial para `incomes.amount` (D75-A);
- paginação: `items`, `page`, `size`, `totalItems`, `totalPages`;
- ordenação: `movementDate ASC`, `id ASC`;
- datas, valores, conta de cada RECEIPT.

## Cadastro / responsável (D89 / D93 / D79)

- responsável em POST;
- responsável em PUT `EXPECTED`;
- responsável em GET listagem e GET por id;
- `OTHER` com nome / sem nome → 400;
- PUT só `EXPECTED`; `RECEIVED` não edita (nem responsável);
- `amount` editado após qualquer movimento (inclusive REVERSED) → 400;
- `amount` em `EXPECTED` sem movimentações → permitido.

## Segurança e contrato HTTP

- 401 sem Bearer;
- ownership (receita / conta de outro usuário → 404);
- UUID inválido → 400;
- propriedades desconhecidas → 400.

## Visão `GET /receivables`

- item aditivo: `amount` original, `accruedAmount`, `receivedAmount`, `remainingAmount`;
- `CANCELLED` continua fora;
- overdue da obrigação (`expectedDate`), não da movimentação;
- `status=EXPECTED` + `dateType=RECEIVED` permitido;
- `status=RECEIVED` + `dateType=EXPECTED` → 400;
- `status=RECEIVED` + `overdue` → 400;
- resumo com recebimento parcial no universo EXPECTED (`receivedAmount` > 0; `futureAmount`/`overdueAmount` = remaining);
- agregação no banco, não em memória.

## RN240 / RN010A / backfill

- RN240 após backfill (saldo preservado; sem zerar nem duplicar);
- RN010A após RECEIPT parcial (qualquer RECEIPT, inclusive REVERSED);
- backfill das `RECEIVED` históricas;
- limitação: não reconstruir já estornadas sem dados no cabeçalho.

## Concorrência

- remaining 100; A recebe 70 e B recebe 50; B deve ver remaining 30 e ser rejeitado;
- nunca calcular remaining antes do lock da Income.

## Regressão obrigatória na implementação

Os 35 testes da Parte 1 (`ReceivablesApiTest`); `IncomeApiTest`; `PayablesApiTest`; `mvn verify`. Atualizar somente o que a evolução aditiva exigir.


# 40H. Projeções (Fase 18 — implementada)

Contrato: `docs/24` §19.10 / `docs/25` §68.

**Status:** **CONCLUÍDA E APROVADA**. D95–D204 **fechadas** e **implementadas**. Classes: `ProjectionApiTest`, `ProjectionCalculatorTest`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes).

Timezone / relógio: `America/Sao_Paulo`; `Clock` injetável (`asOfDate` interno). Reutilizar o padrão da Fase 17 (ex.: `2026-08-17`).

Não persistir projeção. Não criar tabela. Não testar recorrência, cenários, IA, filtro de cartão/categoria/responsável, `asOfDate` público, `GET /projections/monthly` nem frontend.

## Cenários mínimos (implementados)

1. saldo atual + receita futura;
2. saldo atual + despesa futura;
3. receita + despesa;
4. parcelas;
5. parcela parcialmente paga (somente remaining);
6. descontos;
7. surcharge;
8. reversão;
9. fatura;
10. fatura parcialmente paga;
11. fatura com crédito (remaining oficial; sem recálculo);
12. Agreement (fatura liquidada não entra; obrigação nova pelas faturas futuras);
13. receita vencida (entra no primeiro período);
14. despesa vencida (entra no primeiro período);
15. cancelamento (não entra);
16. refund (não gera entrada futura artificial);
17. transferências (histórico no saldo; consolidado inalterado; sem evento futuro);
18. múltiplas contas (consolidado);
19. metas (`reservedAmount` não reduz `closingBalance`; `availableProjectedBalance`);
20. saldo negativo (200 válido; `negative`; mínimo e data);
21. múltiplos meses (encadeamento opening/closing);
22. trimestre calendário (meses + total);
23. 12 meses (default e teto; > 12 → 400);
24. isolamento por usuário.

## D201–D204 (obrigatório)

1. evento sem conta determinada participa do consolidado;
2. evento sem conta determinada **não** é atribuído artificialmente à conta filtrada (`UNASSIGNED` no detalhe);
3. período totalmente no passado → **400** (não 200 vazio; não deslocar horizonte);
4. evento sem data **não** altera `closingBalance`;
5. evento sem data **não** altera `projectedFinalBalance`;
6. evento sem data aparece em grupo separado quando a resposta expõe eventos (`ProjectionCalculatorTest`; no modelo vigente `expected_date` / `due_date` são obrigatórias, então `undatedEvents` da API tende a vazio);
7. `includeEvents` **não** é parâmetro da V1 (query desconhecida → **400**).

## Não duplicação (categoria explícita, obrigatória)

- compra de cartão **não** duplicada com a fatura;
- pagamento já realizado **não** projetado novamente;
- receita já recebida **não** projetada novamente;
- transferência **não** duplicada no consolidado;
- eventos já incorporados ao saldo atual **não** contabilizados novamente.

## HTTP / contrato

- 401 sem Bearer;
- query desconhecida → 400 (inclui `includeEvents`);
- filtros de período conflitantes → 400;
- período inteiramente no passado → 400;
- série mensal sem paginação; eventos no contrato normal, com proteção de volume;
- DTOs próprios; nunca entidade JPA.

## Cálculo

- unitário de `ProjectionCalculator` (fórmula, meses parciais, mínimo);
- API/integração sobre remaining oficiais (não reimplementar RN231 / fatura / Fase 17);
- agregação no banco; sem N+1; sem lock pessimista.

## Regressão obrigatória na implementação

Suíte: **509** testes; 0 falhas; `mvn test` e `mvn verify` BUILD SUCCESS. Não alterar regras das Fases 0–17 para “facilitar” a projeção.


# 40I. Dashboard (Fase 19 — concluída e aprovada)

Contrato: `docs/24` §19.11 / `docs/25` §71.

**Status:** **CONCLUÍDA E APROVADA**. D282–D289 **fechadas** e **implementadas**. Classe: `DashboardApiTest`. Sem tabela. Sem frontend. Sem `GET /dashboard/monthly`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes).

Timezone / relógio: o mesmo da Fase 18 (`America/Sao_Paulo`; Clock injetável; `2026-08-17`).

## Cenários mínimos (implementados)

1. 401 sem Bearer;
2. query desconhecida (`includeEvents`, `accountId`) → 400;
3. período inteiramente no passado / `year` xor `month` / `months=13` → 400;
4. default 12 meses; sem fatos: saldo = inicial; payables/receivables 0;
5. saldo e `projection.summary` coincidem com `GET /accounts/{id}/balance` e `GET /projections`;
6. envelope **sem** `events` / `undatedEvents`;
7. receita parcial + reverse de RECEIPT (D289);
8. cancelamento de receita/despesa não entra;
9. parcelas ACCOUNT vs fatura CREDIT_CARD (sem dupla contagem);
10. overdue de payables (parcela) sem seção nova na projeção;
11. transferência: consolidado inalterado; sem fluxo IN/OUT;
12. meta: `reservedAmount` não reduz `totalBalance`;
13. isolamento por usuário.

## Regressão

`ProjectionApiTest` e `ProjectionCalculatorTest` permanecem verdes (overdue no 1º período; D201; paginação de eventos; teto 12 meses).

Suíte: **519** testes; 0 falhas; `mvn test` e `mvn verify` BUILD SUCCESS.


# 40J. Relatórios (Fase 20 — concluída e aprovada)

Contrato: `docs/24` §19.12 / `docs/25` §76.

**Status:** **CONCLUÍDA E APROVADA**. D-F20-01 a D-F20-16 **fechadas** e **implementadas**. Classe: `ReportsApiTest` (**59** métodos; JSON + PDF; isolamento incluso). Sem tabela. Sem frontend. OpenPDF 3.0.5. Auditoria final: **APROVADA COM RESSALVAS** (ressalva exclusivamente documental/status, corrigida na etapa de fechamento; não bloqueante). Suíte: **578** testes; `mvn test` e `mvn verify` BUILD SUCCESS (Spotless limpo).

Timezone: `America/Sao_Paulo` (Clock injetável, mesmo padrão das Fases 18–19).

A cobertura inclui autenticação, ownership, parâmetros, períodos, paginação, ordenação, Agreement, cartões, fatura, fluxo de caixa e D-F20-01 a D-F20-16. A auditoria considerou a cobertura essencialmente completa. Ressalva de baixa prioridade (não bloqueante): não há teste explícito de 404 para os paths SUPERADOS (`expenses-by-category` etc.); esses paths **não** existem no `ReportsController`.

## Cenários mínimos (implementados)

1. 401 sem Bearer em todos os GETs JSON e PDF;
2. query desconhecida → 400 `VALIDATION_ERROR`;
3. só `startDate` ou só `endDate` → 400 `VALIDATION_ERROR`;
4. `startDate` > `endDate` → 400 `VALIDATION_ERROR`;
5. horizonte > 12 meses calendário → 400 `VALIDATION_ERROR`;
6. default = mês calendário corrente;
7. `page < 0` / `size < 1` / `size > 100` → 400 `BUSINESS_RULE_VIOLATION` no JSON; PDF ignora `page`/`size` mesmo inválidos;
8. `summary` independente da página;
9. recorte de despesa por `due_date` da parcela; N>1 não duplica a despesa no `items[]`; `summary` não usa `expenses.total_amount` integral;
10. CREDIT_CARD: `periodRemaining` da **parcela**, não da fatura; compra não entra no caixa;
11. `CANCELLED`/`REFUNDED` em `items[]` e fora do `summary` efetivo;
12. ACCOUNT/NONE `REFUNDED` não cria entrada de caixa; `bankLiquidated` só quando o fato existir;
13. Agreement (D-F20-02): do `summary` saem **somente** parcelas com `invoice_id` da fatura `SETTLED_BY_AGREEMENT` (não a despesa inteira); despesa do Agreement não é item da fatura liquidada; parcelas futuras da original permanecem;
14. `dateType` obrigatório em incomes/categories e em responsibles quando `INCOME`/`BOTH`; ausência → 400; `nature=EXPENSE` + `dateType` → 400 (D-F20-15); `dateType` ≠ `status`;
15. `RECEIVED`: `periodReceivedAmount` = RECEIPT ACTIVE no período; `receivedAmount` permanece o oficial §19.9; não copiar D94;
16. fluxo: fatos de hoje no histórico; remaining aberto na projeção; o mesmo fato não nos dois lados;
17. `flowType=PROJECTED` + período inteiro no passado → 400 `VALIDATION_ERROR` (D202);
18. `BOTH` + período inteiro no passado → 200, `"projected": { "empty": true }`; **não** chamar `ProjectionService`;
19. período inteiro no futuro → `historical` sem `openingBalance`/`closingBalance` artificiais;
20. `openingBalance` as-of `startDate − 1`; `closingBalance` as-of `min(endDate, hoje)`; invariante `opening + líquido = closing`; **não** usar `availableBalance` como saldo principal;
21. datas oficiais do caixa (`movement_date`, `payment_date`, `created_at` RN263, `transfer_date`, `adjustment_date`);
22. transferências: líquido consolidado 0; não são receita/despesa;
23. metas e ACCRUAL fora do caixa;
24. `invoiceId` de outro usuário / inexistente → 404; filtro `accountId`/`categoryId` alheio → 200 vazio; fatura existente + filtro de responsável sem compras → 200 com cabeçalho oficial e listas vazias (D-F20-03);
25. filtro de responsável da fatura é **opcional**; restringe compras do JSON/PDF; **não** recalcula totais do cabeçalho;
26. PDF: `Content-Type: application/pdf`; universo igual ao JSON; sem UUID/`creditLimit`/notas no PDF da fatura;
27. isolamento: usuário A não lê relatório nem PDF do usuário B;
28. paths SUPERADOS (`expenses-by-category` etc.) **não** existem;
29. item de despesa só `period*` (D-F20-07); CREDIT_CARD `periodPaid = periodObligation − periodRemaining` (D-F20-05);
30. `accountId` em receitas = D78-A qualquer `dateType` (D-F20-08);
31. cartões: fatura por `closingDate`; `purchases[]` = uma compra; crédito por `created_at` da aplicação; blocos de ajuste separados (D-F20-04, D-F20-10, D-F20-11, D-F20-13).

Não alterar o comportamento de `ProjectionApiTest` / `DashboardApiTest` para “facilitar” relatórios.


# 41. Atomicidade

Se uma etapa da transferência falhar:

nenhuma parte da transferência deve ser persistida.


# 42. Teste de rollback

Simular falha durante transferência.


Resultado:

nenhum débito;

nenhum crédito.


# 43. Receitas

Testar:

criação;

edição em `EXPECTED`;

recebimento (`EXPECTED` → `RECEIVED`);

estorno (`RECEIVED` → `EXPECTED`);

cancelamento (`EXPECTED` → `CANCELLED`);

rejeição de edição em `RECEIVED`;

rejeição de transições inválidas;

que estorno **não** resulta em `CANCELLED`;

que cancelamento **não** é tratado como estorno.


A escrita de responsável no cadastro de Income está **implementada** (Parte 2 / `docs/24` §19.9 / RN306 / §40G).


# 44. Receita esperada

EXPECTED não altera saldo.


# 45. Receita recebida

RECEIVED deve aumentar saldo.


# 46. Receita recebida

Deve possuir conta de destino.


# 46.1 Estorno de receita

Após `RECEIVED` → `reverse` → `EXPECTED`:

o status deve ser `EXPECTED`, **não** `CANCELLED`;

o saldo deve voltar ao valor anterior ao recebimento daquela receita (pode ficar negativo);

`accountId` deve ser `null`;

`receivedDate` deve ser `null`;

a duplicata permanece ativa;

a receita pode ser editada e recebida novamente, informando outra vez a conta e a data.

O estorno desfaz a movimentação original (conta e valor do recebimento), não dados posteriormente alterados.

Não rejeitar o estorno só porque o saldo resultante é negativo.

Se qualquer etapa falhar, nenhuma alteração de saldo nem de status deve persistir.


# 46.2 Transições inválidas de receita

Rejeitar:

`RECEIVED` → `CANCELLED`;

`CANCELLED` → `EXPECTED`;

`CANCELLED` → `RECEIVED`;

`receive` sobre receita já `RECEIVED`;

`reverse` sobre receita `EXPECTED` ou `CANCELLED`;

`cancel` sobre receita `RECEIVED` ou `CANCELLED`;

`PUT` sobre receita `RECEIVED` ou `CANCELLED`.


Não criar teste que autorize `RECEIVED` → `CANCELLED`. Essa transição é rejeitada na Fase 6 e permanece **DECISÃO PENDENTE** para fases posteriores.


# 47. Receita cancelada

Após `EXPECTED` → `cancel` → `CANCELLED`:

o registro permanece (não é apagado);

o status é `CANCELLED`;

não deve aparecer como receita futura;

não participa do saldo efetivo;

não altera o saldo;

não pode ser recebida nesta fase;

não pode ser editada nesta fase.

O cancelamento não deve produzir os efeitos do estorno (não há movimentação financeira a desfazer).


# 47.1 Fase 7 — Despesas simples

Contrato: `docs/25` (Fase 7) e RN208–RN221. Não testar `CREDIT_CARD`, parcelas N>1, faturas nem `payments.type`.

Padrão: `ExpenseServiceTest` (unidade) + `ExpenseApiTest` (API + Testcontainers), no mesmo estilo de `IncomeServiceTest` / `IncomeApiTest`. Clock injetável para `overdue`.


## Criação

- `ACCOUNT` nasce `OPEN`; exige `accountId`; não cria `payments`; não altera saldo; gera parcela 1/1 (`installmentNumber = 1`, `totalInstallments = 1`, `amount = totalAmount`);
- `NONE` nasce `OPEN`; `accountId` nulo; rejeitar `accountId` informado; não cria `payments`; gera parcela 1/1;
- `CREDIT_CARD` rejeitado;
- ownership: `userId` do SecurityContext;
- categoria `EXPENSE` ativa do usuário;
- categoria `INCOME`, inativa ou de outro usuário: rejeitar (404 se de outro usuário);
- `responsibleType` obrigatório; `OTHER` exige `responsibleName`;
- `barcode` opcional persistido;
- UUID v7 no id da despesa e da parcela.


## Pagamento (`POST /expenses/{id}/pay`)

- pagamento integral: `OPEN` → `PAID`; saldo reduzido; parcela 1/1 `PAID`;
- pagamento parcial: `OPEN` → `PARTIALLY_PAID`;
- múltiplos pagamentos até quitar: último leva a `PAID`; soma = total;
- pagamento acima do devido: rejeitado;
- valor zero ou negativo: rejeitado;
- saldo insuficiente: rejeitado (não deixa saldo negativo);
- conta inexistente / de outro usuário: 404;
- conta inativa: 400;
- `ACCOUNT` paga com conta diferente de `expenses.account_id`: rejeitado **na Fase 7** (RN210). O contrato da Fase 8 **SUPERA** essa restrição (RN228) — os testes da Fase 8 devem passar a aceitar contas diferentes do mesmo usuário;
- `ACCOUNT` sem `accountId` no body: usa a conta da despesa;
- `NONE` paga com conta válida: `payments.account_id` preenchido; `expenses.account_id` permanece `null`; `paymentMethod` permanece `NONE`;
- `PAID` / `CANCELLED` / `REFUNDED`: pagamento rejeitado;
- dois pagamentos concorrentes não ultrapassam o devido;
- `payments.type` permanece `null`.


## Cancelamento

- `OPEN` → `CANCELLED`; parcela 1/1 `CANCELLED`; saldo inalterado; registro permanece;
- `PARTIALLY_PAID` rejeitado;
- `PAID` rejeitado;
- `CANCELLED` rejeitado;
- `REFUNDED` rejeitado.


## Refund

- `PARTIALLY_PAID` → `REFUNDED`;
- `PAID` → `REFUNDED`;
- `OPEN` rejeitado;
- `CANCELLED` rejeitado;
- `REFUNDED` rejeitado (duplo refund);
- saldo restaurado (pagamentos deixam de ser subtraídos);
- linhas de `payments` permanecem e continuam listáveis;
- despesa não volta a `OPEN`;
- parcela 1/1 também `REFUNDED`.


## Edição (`PUT`)

- `OPEN`: campos do contrato alteráveis; parcela 1/1 acompanha `totalAmount` e `dueDate`;
- `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED`: rejeitar.


## Overdue

- `OPEN` com `dueDate` ≥ hoje: `overdue = false`;
- `OPEN` com `dueDate` < hoje: `overdue = true`;
- `PARTIALLY_PAID` vencida: `overdue = true`;
- `PAID` nunca overdue;
- `CANCELLED` nunca overdue;
- `REFUNDED` nunca overdue;
- “hoje” em `America/Sao_Paulo`, não no timezone do JVM/navegador.


## Segurança

- usuário B não lê, edita, paga, cancela nem estorna despesa de A: **404**;
- listagem de B não inclui itens de A;
- categoria de outro usuário na criação: **404**;
- conta de outro usuário no pagamento: **404**;
- pagamento de outro usuário: **404**;
- sem token: **401**.


# 47.2 Fase 8 — Parcelamento, payments, adjustments e reverse

Contrato: `docs/24` seção 19.2 e `docs/25` seção 47. Domínio e HTTP de adjustment estão implementados; os cenários abaixo devem permanecer verdes. Não testar `CREDIT_CARD`, faturas, `payments.type` nem relatórios de apresentação.

Cenários obrigatórios:

- `installmentCount` omitido = 1 (regressão Fase 7);
- 3 e 12 parcelas; soma = total; residual na **primeira** (1000/3 → 333,34 + 333,33 + 333,33);
- dia-base 31: 31/01, 28/02, 31/03 (não carregar 28);
- `expenses.due_date` = primeira parcela;
- quantidade imutável após criação;
- PUT parcela `OPEN` com soma correta vs rejeição + rollback;
- PUT parcela `PAID` / `PARTIALLY_PAID` rejeitado;
- pagamento por parcela; múltiplos payments; overpayment rejeitado;
- payments da mesma parcela em contas diferentes do usuário;
- `POST /expenses/{id}/pay` só 1/1; N>1 exige identificação da parcela;
- `payments.status` ACTIVE; reverse → REVERSED; segundo reverse rejeitado;
- reverse após REFUNDED/CANCELLED rejeitado;
- payment REVERSED não movimenta saldo da conta;
- DISCOUNT + PAYMENT atômicos (domínio); SURCHARGE + PAYMENT atômicos (domínio); adjustment não movimenta conta;
- refund misto: parcelas com payment ACTIVE → REFUNDED; sem payment → OPEN bloqueada (sem pay/adjust/edit/cancel);
- cancel só OPEN; PARTIALLY_PAID e PAID rejeitados;
- status agregado da despesa persistido;
- overdue da parcela: remaining > 0, OPEN/PARTIALLY_PAID, due_date < hoje, despesa não CANCELLED/REFUNDED (RN241);
- overdue da despesa N>1: true se pelo menos uma parcela estiver overdue segundo RN241 (não usar somente expenses.due_date);
- listagem startDate/endDate: despesa no intervalo se pelo menos uma parcela tiver due_date no intervalo;
- UNIQUE (expense_id, installment_number);
- isolamento 404; concorrência na mesma parcela;
- `payments.type` permanece sem valores oficiais;
- **cenários documentados pós-auditoria (cobrir na implementação/ajuste se ainda não verdes):**
  - A) 1/1 `OPEN` + `DISCOUNT` ACTIVE + `PUT` que produziria `obligation < 0` → rejeição + rollback (RN231 / RN245);
  - B) 1/1 `OPEN` + `DISCOUNT` ACTIVE + `PUT` com `obligation` resultante válida → alteração permitida;
  - C) parcelamento `0,01` / 3 → `0,01` + `0,00` + `0,00`, soma = total (RN067 / RN068);
  - D) `GET` saldo derivado sem exigir lock explícito de conta para leitura (RN240).

**HTTP Adjustment** (`docs/25` §47) — cenários implementados (manter verdes):

CREATE (`POST .../installments/{installmentId}/adjustments`):

1. DISCOUNT válido → 201 + `status ACTIVE`;
2. SURCHARGE válido → 201;
3. `amount` ≤ 0 rejeitado (400);
4. `type` inválido / propriedade desconhecida rejeitado (400);
5. ownership inválido / UUID de outro usuário → 404;
6. parcela não elegível (`CANCELLED`/`REFUNDED` da parcela) → 400;
7. despesa `REFUNDED` / `CANCELLED` → 400;
8. (opcional regressão) parcela `PARTIALLY_PAID`/`PAID` com obligation válida pode receber adjustment (não exige `OPEN`).

GET (`GET .../adjustments`):

9. retorna ACTIVE + REVERSED;
10. ordenação `createdAt ASC`, `id ASC`;
11. ownership → 404; histórico consultável após terminal da despesa.

REVERSE (`POST .../adjustments/{adjustmentId}/reverse`):

12. ACTIVE → REVERSED (200); body vazio;
13. já REVERSED → 400;
14. despesa REFUNDED → 400;
15. despesa CANCELLED → 400;
16. ownership / adjustment de outra parcela → 404;
17. histórico preservado (mesmo `id`, `amount`, `type`, `createdAt`).

FINANCEIRO:

18. DISCOUNT reduz obligation / remaining derivado;
19. SURCHARGE aumenta obligation;
20. reverse restaura obligation anterior;
21. adjustment não altera saldo de conta.

CONCORRÊNCIA:

22. dois creates concorrentes respeitam lock / obligation;
23. dois reverses concorrentes: apenas um aplica `REVERSED`.


# 48. Testes de cartão

Contrato da Fase 9 (`docs/24` §19.3). **Implementado** e **concluído**.

Testar:

criação;

edição (`closingDay`/`dueDay` não reescrevem faturas existentes);

desativação (bloqueia compra; não exclui);

reativação;

filtro por `holderName`;

`lastFourDigits` omitido;

limite derivado (available negativo permitido);

compras (acima do limite aceita; cartão inativo recusada);

ciclo RN095 / RN098;

faturas `SCHEDULED` → `OPEN` → `CLOSED` → `PAID`;

uma OPEN por cartão;

rateio RN247 (remaining ASC; empate `due_date` ASC, `id` ASC; residual na última);

crédito FIFO e ordem de faturas (`due_date` ASC, `id` ASC); crédito manual com `reason`;

`due_date` da fatura RN099B (`due_day` ≤ `closing_day` → mês seguinte);

cancelamento `OPEN` e refund com `settlement` `CARD_CREDIT` / `ACCOUNT` (RN117);

refund `ACCOUNT`/`NONE` com `settlement` → 400 `BUSINESS_RULE_VIOLATION` (`SETTLEMENT_NOT_ALLOWED`);

ajuste de fatura: `DISCOUNT` limitado ao remaining; `SURCHARGE` exige remaining > 0 (RN247A);

`GET /credit-cards/{id}/credits` retorna array; `remainingAmount` por crédito; total disponível = soma derivada;

pagamento de fatura não cria `payments`;

fechamento idempotente do scheduler;

PAID imutável.


# 49. Limite

Testar:

limite total;

limite utilizado;

limite disponível.


# 50. Exemplo

Limite:

5000


Utilizado:

1500


Disponível:

3500


# 50.1 Compra acima do limite

Limite: 5000

Comprometido: 4500

Disponível: 500

Compra: 600

Resultado esperado: compra **aceita** (RN029A **SUPERADA**). `availableLimit` = −100. Não recusar no backend.


# 51. Teste

Compra no cartão não deve reduzir saldo bancário imediatamente.


# 52. Teste

Compra no cartão deve aumentar comprometimento do cartão.


# 53. Testes de fechamento

Testar compras:

antes do fechamento;

no dia do fechamento;

depois do fechamento.


# 54. Exemplo

Fechamento:

10


Compra:

09


Deve pertencer ao ciclo que fecha no dia 10.


# 55. Exemplo

Fechamento:

10


Compra:

11


Deve pertencer ao próximo ciclo.


# 56. Regra

Compra exatamente no dia do fechamento (RN095) deve ir para a próxima fatura.

Exemplo: fechamento dia 10; compra 10/08 → próximo ciclo.


# 57. Testes de vencimento e fechamento (RN098)

Testar dia configurado 31 em:

mês de 28 dias → 28;

mês de 29 dias → 29;

mês de 30 dias → 30;

mês de 31 dias → 31.


# 58. Exemplo

Cartão:

fechamento 10;

vencimento 20.


Testar transição entre meses.


# 59. Faturas

Testar:

criação;

abertura;

fechamento;

pagamento;

pagamento parcial;

vencimento;

parcelamento.


# 60. Fatura

As parcelas (`expense_installments`) devem aparecer na fatura correta (`invoice_id`).


# 61. Fatura

Uma compra parcelada deve ter cada parcela na fatura do respectivo ciclo.

A despesa original não pertence a uma única fatura.

Testar também que `totalAmount` da fatura é a soma das parcelas do ciclo (derivado), não o total da despesa.


# 62. Exemplo

Compra:

1200


12 parcelas:

100


Deve gerar:

12 parcelas.


# 63. Teste

A soma das parcelas deve ser:

1200


# 64. Pagamento de fatura

Testar:

pagamento integral;

pagamento parcial;

múltiplos pagamentos.


# 65. Pagamento de fatura

Pagamento não deve criar uma nova despesa de consumo.


# 66. Exemplo

Compra:

1000


Pagamento da fatura:

1000


Despesa total:

1000


e não:

2000


# 67. Pagamento parcial

Fatura:

2000


Pagamento:

1200


Saldo:

800


# 68. Testes de parcelamento / negociação de fatura (Fase 13)

**Status:** cenários L01–L36 **contratados e cobertos** em `CreditCardInvoiceAgreementPhase13ApiTest` (inclui emenda RN254 e cenário oficial Jan→Fev→Mar). Contrato: `docs/24` §19.4 / RN254 (`CONCLUÍDA E APROVADA`).

Mínimo contratado:

| ID | Cenário |
| --- | --- |
| L01 | parcelamento válido |
| L02 | parcelamento sem entrada (`entryAmount = 0`) |
| L03 | entrada igual ao saldo → 400 |
| L04 | entrada superior ao saldo |
| L05 | tentativa em fatura aberta |
| L06 | pagamento integral em fatura aberta (status permanece OPEN) |
| L07 | ausência de campo “quitado”/`settled` em pagamento de fatura |
| L08 | fechamento posterior de fatura zerada → PAID |
| L09 | compras parceladas originais não são transformadas |
| L10 | nova obrigação (expense CREDIT_CARD do Agreement) é independente |
| L11 | nova negociação não antecipa Agreements anteriores |
| L12 | coexistência de Agreements ACTIVE |
| L13 | renegociação automática (todos ACTIVE; sem lista de ids); `anticipatedFuturesNetAmount` no request |
| L14 | futuros: `futureOriginalAmount` → desconto financeiro → `anticipatedFuturesNetAmount` (incorporação ≠ segundo desconto) |
| L15 | encerramento automático `RENEGOTIATED` |
| L16 | entrada + settlement fatura (`invoiceSettlementAmount`) + financed consolidado (RN254) |
| L17 | antecipação individual com desconto automático (`settled=true`) |
| L18 | percentual de desconto derivado (antecipação individual) |
| L19 | pagamento parcial (antecipação) |
| L20 | segundo pagamento completa obrigação |
| L21 | quitação com desconto |
| L22 | pagamento superior ao saldo |
| L23 | pagamento de parcela já quitada |
| L24 | limite negativo permitido |
| L25 | histórico da fatura parcelada |
| L26 | histórico de renegociação (exemplo oficial Jan→Fev→Mar) |
| L27 | ownership da fatura |
| L28 | ownership do Agreement |
| L29 | ownership da conta de pagamento |
| L30 | concorrência em negociação |
| L31 | concorrência em pagamento/antecipação |
| L32 | rollback completo |
| L33 | imutabilidade `SETTLED_BY_AGREEMENT` |
| L34 | 1ª parcela na próxima fatura |
| L35 | contractedTotal > financedAmount (e rejeição se contractedTotal < financedAmount → 400) |
| L36 | renegociação sem duplicar parcela da fatura atual; financed = (invoiceRemaining − entry) + anticipatedFuturesNetAmount |


# 69. Exemplo (legado pré-Agreement)

O exemplo “saldo 800 → duas parcelas 400+400 cuja soma = 800” ficou **SUPERADO** como regra geral (RN113). O exemplo canônico da Fase 13 admite total contratado **maior** que o financed (ex.: negociado 600, 10×120 = 1.200). Exemplo oficial de **renegociação** consolidada: `docs/24` RN254 (financed 1.600; contracted 3.200).


# 69. Exemplo (legado pré-Agreement)

O exemplo “saldo 800 → duas parcelas 400+400 cuja soma = 800” ficou **SUPERADO** como regra geral (RN113). O exemplo canônico da Fase 13 admite total contratado **maior** que o financed (ex.: negociado 600, 10×120 = 1.200).


# 70. Testes de estorno

Testar:

compra aberta;

compra fechada;

compra parcelada;

compra já parcialmente paga.


# 71. Estorno

Estorno não deve apagar o registro original.


# 72. Cancelamento

Cancelamento não deve apagar o registro original.


# 73. Teste

Após cancelamento:

não aparece em contas a pagar (`GET /api/v1/payables` — `docs/27` §40E);

não participa da projeção;

continua disponível no histórico.


# 74. Estorno

Após estorno de **despesa**:

não representa obrigação financeira ativa.


Esta seção aplica-se a despesas / compras. Não se aplica ao estorno de receita: após `RECEIVED` → `EXPECTED`, a duplicata permanece ativa como receita não recebida e pode ser recebida novamente (seção 46.1).


# 75. Testes de boleto

Testar:

com número;

sem número;

número inválido;

alteração do número.


# 76. Testes de responsável

Valores:

MINE;

GIULIA;

EDERSON;

ELISIANE;

OTHER.


# 77. Outro responsável

Quando:

OTHER


deve permitir descrição.


# 78. Testes de categoria

Testar:

criação (`INCOME` e `EXPENSE`);

categoria inicia ativa;

validações de `name` e `type`;

ownership pelo usuário autenticado;

rejeição de `userId` e demais campos não permitidos;

unicidade `user_id + type + name` (case-insensitive; independente de `active`);

mesmo nome em tipos diferentes permitido;

listagem isolada por usuário e filtros `type` / `active`;

edição de nome e tipo com reaplicação da unicidade;

desativação lógica (sem exclusão física; idempotente);

isolamento cross-user (404).

Uso em despesas e receitas: fases posteriores.


# 79. Categoria

Categoria desativada não deve ser utilizada em novos lançamentos.


# 80. Testes de contas

Testar:

criação;

saldo inicial;

entrada;

saída;

transferência;

desativação.


# 81. Conta

Conta com histórico não deve ser excluída fisicamente.


# 82. Isolamento

Criar:

usuário A;

usuário B.


# 83. Teste

Criar despesa para usuário A.


Usuário B tentando consultar:

deve falhar.


# 84. Teste

Usuário B tentando alterar despesa de A:

deve falhar.


# 85. Teste

Usuário B tentando cancelar despesa de A:

deve falhar.


# 86. Teste

Usuário B tentando consultar fatura de A:

deve falhar.


# 87. Teste

Usuário B tentando consultar conta de A:

deve falhar.


# 88. Teste

Usuário B tentando realizar transferência usando conta de A:

deve falhar.


# 89. UserId

Enviar userId de outro usuário no request não deve alterar o proprietário da operação.


# 90. Autenticação

Testar endpoint protegido:

sem JWT.


Resultado:

401


# 91. JWT inválido

Testar:

JWT inválido.


Resultado:

401


# 92. JWT expirado

Testar:

JWT expirado.


Resultado:

401


# 93. Login

Testar:

credenciais corretas;

senha incorreta;

email inexistente;

usuário desativado.


# 94. Senha

Senha nunca deve aparecer:

em response;

em logs;

em banco em texto puro.


A Fase 3 cobre autenticação (`/auth`, `/users/me`). A Fase 4 cobre IDOR de contas (`/accounts`). Testes de IDOR dos demais recursos financeiros (despesa/fatura/etc.) permanecem para as fases dos respectivos módulos.


# 95. Validação

Testar:

campos obrigatórios;

valores negativos;

UUID inválido;

datas inválidas;

enum inválido;

strings excessivamente grandes.


# 96. API

Endpoints devem possuir testes de:

201;

200;

400;

401;

403;

404;

409.


# 97. Testes de persistência

Testar:

criação;

atualização;

consulta;

relacionamentos;

constraints.


# 98. Banco

Testar constraints importantes.


Exemplos:

unique;

foreign key;

not null;

FK composta de ownership (`referenced_id`, `user_id`);

rejeição de despesa com `category_id` / `account_id` / `credit_card_id` de outro usuário;

ausência de coluna `invoice_id` em `expenses`;

ausência de colunas `total_amount` / `paid_amount` / `remaining_amount` em `credit_card_invoices`.


# 99. Transações

Operações financeiras compostas devem possuir testes de rollback.


# 100. Rollback

Se ocorrer erro durante:

pagamento;

transferência;

compra;

pagamento de fatura;


nenhuma parte parcial deve permanecer persistida.


# 101. Projeções

Testar:

receitas futuras;

despesas futuras;

parcelas futuras;

faturas futuras;

cancelamentos;

estornos.


# 102. Projeção

Receita EXPECTED deve participar.

Inclui receita que voltou a `EXPECTED` após estorno. Essa duplicata permanece ativa e prevista.


# 103. Projeção

Receita CANCELLED não deve participar.

Cancelamento inutiliza a duplicata. Não confundir com estorno.


# 104. Projeção

Despesa OPEN deve participar.


# 105. Projeção

Despesa CANCELLED não deve participar.


# 106. Projeção

Despesa REFUNDED não deve representar compromisso futuro ativo.


# 107. Projeção

Transferência não deve alterar resultado financeiro projetado.


# 108. Projeção mensal

Testar:

agosto;

setembro;

outubro;

dezembro.


# 109. Exemplo

Compra parcelada em agosto:

12 parcelas.


A projeção de dezembro deve incluir a parcela correspondente.


# 110. Dashboard

Testar:

saldo;

receitas;

despesas;

faturas;

contas a pagar.


# 111. Dashboard

Os valores exibidos devem corresponder aos dados persistidos.


# 112. Relatórios

Contrato: `docs/27` §40J / `docs/24` §19.12. Implementado na Fase 20 (`ReportsApiTest`).


# 113. PDF

Contrato: `docs/27` §40J. Biblioteca: OpenPDF 3.0.5. Isolamento por usuário. Universo igual ao JSON. `page`/`size` ignorados.

Usuário A não pode gerar PDF da fatura do usuário B.


# 114. Teste

(Reservado — isolamento de PDF coberto em §40J item 27.)


# 115. Testes de regressão

Toda correção de bug financeiro deve gerar teste que reproduza o problema.


# 116. Regra

Bug corrigido sem teste de regressão não deve ser considerado completamente corrigido.


# 117. Testes de fronteira

Devem ser testados valores:

0;

0.01;

0.99;

1.00;

999.99;

1000.00;

valores grandes.


# 118. Datas

Testar:

primeiro dia do mês;

último dia do mês;

virada de ano;

ano bissexto.


# 119. Timezone

Testar operações próximas da meia-noite quando timestamps forem utilizados.


# 120. Concorrência

Operações que alteram saldo devem possuir testes de concorrência quando tecnicamente aplicável.


# 121. Performance

Na V1 não é necessário criar testes de carga complexos.


# 122. Performance

Entretanto, queries principais devem ser avaliadas para evitar problemas óbvios de performance.


# 123. Testes de API

Preferir testes automatizados executáveis localmente.


# 124. CI

A estrutura deve permitir futuramente executar testes automaticamente em CI/CD.


# 125. GitHub Actions

Pode ser implementado posteriormente.


# 126. Cobertura

Cobertura de código deve ser acompanhada.


# 127. Meta de cobertura

Não buscar 100% de cobertura artificialmente.


# 128. Prioridade

Maior prioridade para:

- regras financeiras;
- segurança;
- transações;
- cálculos;
- persistência.


# 129. Cobertura

Código de infraestrutura simples pode possuir menor cobertura quando não houver valor adicional significativo.


# 130. Testes frágeis

Evitar testes excessivamente acoplados à implementação interna.


# 131. Testes

Preferir validar comportamento.


# 132. Exemplo

Testar:

"saldo final é 1000"


em vez de testar:

"método X chamou método Y exatamente duas vezes"


quando isso não for requisito.


# 133. Testes unitários

Devem ser rápidos.


# 134. Testes de integração

Podem ser mais lentos, mas devem ser executáveis localmente.


# 135. PostgreSQL

Testes de integração devem utilizar PostgreSQL real ou ambiente equivalente quando a regra depender de comportamento específico do PostgreSQL.


# 136. Testcontainers

É recomendado avaliar:

Testcontainers


para testes de integração com PostgreSQL.


# 137. V1

Se Testcontainers for utilizado:

o teste deve iniciar PostgreSQL isolado.


# 138. Banco de testes

Nunca utilizar o banco financeiro pessoal real para testes automatizados.


# 139. Dados

Testes devem criar seus próprios dados.


# 140. Isolamento

Cada teste deve evitar depender do resultado de outro teste.


# 141. Determinismo

Testes devem ser determinísticos.


# 142. Data atual

Evitar depender diretamente da data atual do sistema.


# 143. Clock

Quando necessário, utilizar clock controlável/testável.


# 144. Exemplo

Não depender diretamente de:

LocalDate.now()


em regras que precisam de testes determinísticos.


# 145. Fixtures

Fixtures devem ser simples e legíveis.


# 146. Test Builders

Builders ou factories podem ser utilizados quando reduzirem complexidade real.

Não criar Factory/Builder para cada entidade ou para objetos simples que cabem em poucas linhas no próprio teste.

Não criar testes de concorrência antes que exista comportamento concorrente real a proteger.


# 147. Nomenclatura

Nomes dos testes devem explicar o comportamento esperado.


# 148. Exemplo

deveRejeitarPagamentoQuandoValorUltrapassaSaldoDaFatura


# 149. Testes

Testes devem ser organizados por domínio.


Exemplo:

account;

expense;

invoice;

payment;

transfer.


# 150. Estrutura

A estrutura dos testes deve acompanhar a arquitetura do projeto quando isso melhorar a navegação.


# 151. Teste financeiro

Sempre que possível, utilizar valores explícitos e fáceis de conferir.


# 152. Exemplo

1000.00


em vez de valores aleatórios.


# 153. Teste

Evitar testes excessivamente complexos.


# 154. Teste

Um teste deve possuir uma intenção clara.


# 155. Teste de erro

Testar também mensagens/códigos de erro quando fizerem parte do contrato da API.


# 156. OpenAPI

Quando o contrato mudar:

testes e documentação devem ser atualizados.


# 157. Frontend

A V1 deve possuir testes para componentes e serviços críticos do Angular.


# 158. Frontend

Priorizar:

- serviços HTTP;
- guards;
- autenticação;
- formulários financeiros;
- cálculos exibidos;
- componentes críticos.


# 159. Frontend

Não é necessário testar cada detalhe visual.


# 160. E2E

A Fase 22 introduziu Playwright no frontend (`frontend/e2e/`, `npx ng e2e` / `npx playwright test`). **Status:** **CONCLUÍDA / APROVADA COM RESSALVAS**.

Os testes E2E validam fluxos no navegador contra backend Spring e PostgreSQL reais (Angular + JWT + API). Autenticação real: sem JWT fixo, sem bypass de guard/interceptor. Isolamento multiusuário exercitado. Não substituem testes unitários nem de API.


# 161. Fluxos E2E implementados (F22)

1. Registro, login, rota protegida, sessão, logout e novo login.
2. Isolamento multiusuário (UI + `GET` direto de recurso alheio → 404).
3. Conta → receita → recebimento → despesa → pagamento → saldo oficial.
4. Transferência entre contas `BANK_ACCOUNT` e reversão.
5. Cartão → compra → fatura → ajuste → crédito (aplicação automática) → pagamento → limite oficial.
6. Fatura `CLOSED` → acordo → parcelas → antecipação/quitação.
7. Meta → contribuição → resgate (`currentAmount` / `progressPercent` oficiais).
8. Dashboard e projeções após movimentações reais.
9. Relatórios representativos (despesas, receitas, fluxo de caixa, fatura) e PDF real.
10. Validação visual desktop (1366×768) e mobile (390×844).

Não há dezenas de combinações de filtro; a cobertura paramétrica permanece nos testes de API.


# 162. E2E — execução

Pré-requisito: PostgreSQL (Docker Compose) e backend no ar (`GET /api/v1/health` → `UP`).

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run-e2e.ps1
```

Ou, com o stack já iniciado, em `frontend/`: `npx ng e2e` / `npx playwright test`.

Browser obrigatório: Chromium. Firefox/WebKit opcionais, não configurados. Retries: 0.

Usuários e dados são criados pelo próprio teste (e-mails `@example.test`). Não há JWT fixo nem bypass de guard/interceptor.

Fechamento de fatura: não existe `POST /invoices/{id}/close`. O helper de acordo simula o scheduler oficial (`closeDueInvoices`) via SQL somente quando `closingDate` ≤ hoje em `America/Sao_Paulo`.


# 163. Estratégia

Começar pelos testes de domínio e API.


# 164. Testes

A sequência recomendada para cada funcionalidade:

1. regra;
2. teste;
3. implementação;
4. integração;
5. API;
6. frontend.


# 165. TDD

Não é obrigatório utilizar TDD estrito em todas as funcionalidades.


# 166. Entretanto

Para regras financeiras críticas:

escrever o teste antes ou junto da implementação é altamente recomendado.


# 167. Regra

A IA deve criar testes junto com a funcionalidade.


# 168. Regra

A IA não deve simplesmente afirmar que uma funcionalidade foi testada.


Os testes devem existir no código.


# 169. Regra

Depois de implementar:

executar testes.


# 170. Regra

Se teste falhar:

investigar causa.


Não simplesmente remover ou enfraquecer o teste.


# 171. Regra

Não alterar teste apenas para fazer o código passar sem avaliar a regra de negócio.


# 172. Regra

Quando um teste revelar conflito entre:

código;

documentação;

regra;


a inconsistência deve ser apresentada.


# 173. Regra

A IA deve solicitar decisão quando a regra não estiver definida.


# 174. Qualidade

Uma funcionalidade é considerada pronta quando:

- testes unitários relevantes passam;
- testes de integração relevantes passam;
- testes de API passam;
- regras de segurança passam;
- documentação está atualizada.


# 175. Critério financeiro

Nenhuma operação financeira crítica deve ser entregue sem testes automatizados.


# 176. Regra final

Os testes são parte do produto.


Não são uma etapa opcional posterior.


# 177. Fase 22 — E2E Playwright

**Status:** CONCLUÍDA / APROVADA COM RESSALVAS. Auditoria formal realizada (somente leitura).

Ferramenta: `@playwright/test` + `playwright-ng-schematics` (integração recomendada pelo Angular CLI para `ng e2e`). Chromium obrigatório. Firefox/WebKit não configurados. Stack real: PostgreSQL + backend Spring + frontend Angular.

Suíte de referência auditada: **11** testes Chromium, **11** aprovados, **0** falhos, **0** ignorados. Relatório HTML em `frontend/playwright-report/`.

A suíte unitária do frontend permanece **60** arquivos / **584** testes (Vitest). Builds `ng build` e `ng build --configuration development` passaram.

Backend e migrations **não** foram alterados. Pendências oficiais de negócio (`payments.type`; §269.2.7) e ressalvas C3–C7/B12 **não** foram reabertas. C8 **não** foi implementada.

Ressalvas da auditoria (não bloqueantes; **não** corrigidas nesta consolidação):

- **AUD-F22-A1 — INFORMATIVO:** o teste E2E do fluxo financeiro core possui uma asserção simples baseada em receita - despesa. O valor oficial utilizado pela aplicação continua vindo da API. A orientação permanece: testes futuros devem preferir comparar UI com valores oficiais da API em vez de reproduzir fórmulas financeiras.
- **AUD-F22-A2 — BAIXO:** a cobertura E2E dos relatórios JSON não percorre individualmente todos os tipos de relatório. A suíte cobre os principais fluxos e os contratos internos permanecem cobertos pelos testes da aplicação.
- **AUD-F22-A3 — INFORMATIVO:** a validação visual da F22 é estrutural/funcional em desktop e mobile, não uma suíte de visual regression pixel-level com snapshots.
- **AUD-F22-A4 — INFORMATIVO:** o helper de fechamento de fatura utilizado nos E2E prepara um cenário que, em produção, seria fechado pelo scheduler, pois o contrato atual não possui endpoint manual de fechamento. O helper permanece somente como fixture de teste e não representa uma operação disponível na aplicação.