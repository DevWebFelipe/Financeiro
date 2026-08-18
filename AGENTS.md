# AGENTS.md — Financial Control

## 1. Objetivo

Este projeto é um sistema de controle financeiro pessoal multiusuário chamado Financial Control.

O sistema será desenvolvido inicialmente para execução local e terá como objetivo fornecer uma base sólida, organizada, testável e extensível para controle financeiro pessoal.

O projeto também possui finalidade educacional. As decisões técnicas devem ser explicadas quando forem relevantes, permitindo que o desenvolvedor compreenda não apenas "o que fazer", mas também "por que fazer".

---

## 2. Hierarquia da documentação

A documentação do projeto segue esta hierarquia de autoridade:

```text
AGENTS.md
 ↓
docs/20–28 — decisões funcionais e técnicas oficiais do projeto
 ↓
docs/CODING_STANDARDS.md — convenções gerais de código e organização
 ↓
.cursor/rules/*.mdc — instruções operacionais para o Cursor
```

Significado:

- `AGENTS.md` — regras que a IA deve seguir durante o desenvolvimento; autoridade máxima;
- `docs/20`–`docs/28` — especificação detalhada (arquitetura, stack, modelo, regras, API, segurança, testes, roadmap);
- `docs/CODING_STANDARDS.md` — convenções de código, nomenclatura e organização; não pode contradizer `AGENTS.md` nem `docs/20–28`;
- `.cursor/rules/*.mdc` — instruções operacionais para o Cursor; **não criam decisões arquiteturais novas**; não podem contradizer documentos superiores;
- `README.md` — visão geral do projeto; não é fonte de decisões técnicas.

Regras inferiores não podem contradizer regras superiores.

Quando uma regra técnica ainda não estiver decidida nos documentos superiores, a IA NÃO deve inventar uma decisão silenciosamente. Deve parar e usar: **DECISÃO PENDENTE DO DESENVOLVEDOR**.

Em caso de conflito:

1. identificar o conflito;
2. corrigir a documentação conflitante;
3. deixar todos os documentos consistentes.

Não manter duas decisões diferentes sobre a mesma questão.

Documentação ativa:

- `docs/20-fluxos-financeiros.md`
- `docs/21-arquitetura-do-sistema.md`
- `docs/22-stack-tecnologica.md`
- `docs/23-modelo-de-dados.md`
- `docs/24-regras-de-negocio.md`
- `docs/25-api.md`
- `docs/26-seguranca.md`
- `docs/27-testes.md`
- `docs/28-roadmap.md`
- `docs/CODING_STANDARDS.md`

Se existirem arquivos `docs/01`–`docs/19` no repositório, considerá-los **obsoletos/históricos**. A IA NÃO deve usá-los como fonte de verdade.

**DECISÃO PENDENTE DO DESENVOLVEDOR:** remover fisicamente `docs/01`–`docs/19` do repositório (ainda presentes no disco na consolidação; esta etapa não pôde alterá-los por restrição de escopo).

---

## 3. Regra principal de desenvolvimento

O projeto DEVE ser desenvolvido por etapas.

NUNCA implementar o sistema inteiro de uma única vez.

Cada etapa deve seguir este fluxo:

1. analisar o estado atual do projeto;
2. analisar a documentação relacionada à etapa;
3. implementar somente o escopo definido;
4. executar o build;
5. executar os testes;
6. corrigir problemas encontrados;
7. atualizar a documentação quando necessário;
8. apresentar um resumo das alterações;
9. informar eventuais decisões ou problemas encontrados;
10. aguardar autorização para iniciar a próxima etapa.

Se uma decisão importante de negócio não estiver definida, a IA deve parar e perguntar antes de implementar.

---

## 4. Stack tecnológica oficial

### Backend

- Java 25 LTS
- Spring Boot 4.1.x
- Maven 3.9.x (≥ 3.9.12); o backend possui Maven Wrapper
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT Access Token (HS256, 30 minutos, Bearer) — implementado na Fase 3
- Refresh Token — **não implementado na Fase 3**
- Argon2id (hash de senhas)
- Jakarta Bean Validation
- Flyway
- springdoc-openapi
- JUnit 5
- Mockito
- AssertJ
- Testcontainers
- OpenPDF

### Frontend

- Angular 22.x
- Node.js 22.x LTS (≥ 22.22.3); 24.x ≥ 24.15.0 aceito
- npm (empacotado com o Node.js utilizado)
- TypeScript strict
- Standalone Components
- Signals
- Services
- Reactive Forms
- Angular HttpClient
- HTTP Interceptors
- Route Guards
- Angular Material
- Material Icons
- Apache ECharts
- ESLint
- Prettier

### Banco

- PostgreSQL 18 (`postgres:18-alpine` via Docker)
- UUID v7 gerado pela aplicação; coluna `UUID` no banco (sem default de geração misturado)
- NUMERIC(19,2) / BigDecimal para valores monetários
- Percentuais armazenados como fração (5,25% = `0.0525`)
- TIMESTAMPTZ / `Instant` para instantes absolutos (persistidos em UTC)
- `DATE` / `LocalDate` para datas financeiras
- Calendário financeiro: timezone `America/Sao_Paulo`
- Booleanos de estado: coluna `active` (Java: `isActive`)
- Flyway (único responsável pelo schema)
- Hibernate `ddl-auto=validate`

### Infraestrutura

- Docker Engine ≥ 24; no Windows, Docker Desktop
- Docker Compose V2 ≥ 2.24 (`docker compose`)
- PostgreSQL em container no desenvolvimento (`postgres:18-alpine`)
- backend/frontend podem executar fora do Docker inicialmente
- Dockerização completa da aplicação poderá ser feita posteriormente
- Git ≥ 2.39

Environment Contract completo: `docs/22-stack-tecnologica.md` (seção 30). Diagnóstico: `scripts/check-environment.ps1`.

### Convenções

- Pacote Java: `br.com.financialcontrol`
- Pacotes de domínio no plural, alinhados ao modelo real: `accounts`, `expenses`, `incomes`, `transfers`, `balance_adjustments`, `payments`, `credit_cards`, `credit_card_invoices`, `financial_goals`
- Pacote de consulta da Fase 16 (`CONCLUÍDA E APROVADA`): `payables` — visão de leitura; **sem** tabela/entidade JPA
- Pacote de consulta da Fase 17 (`CONCLUÍDA E APROVADA`): `receivables` — visão de leitura `GET /api/v1/receivables`; **sem** tabela/entidade JPA; contrato `docs/24` §19.8 / §19.9. Parte 2: tabela `income_movements` (V30); movimentações no domínio `incomes`; decisões D73–D94 **fechadas** e **implementadas**. **Não** recriar o endpoint da Parte 1. **Não** criar tabela `receivables`.
- Pacote de consulta da Fase 18 (`docs/24` §19.10 — **CONCLUÍDA E APROVADA**): `projections` — visão derivada `GET /api/v1/projections`; **sem** tabela/entidade JPA; D95–D204 **fechadas** e **implementadas**. **Não** criar tabela `projections`.
- Pacote de consulta da Fase 19 (`docs/24` §19.11 — **CONCLUÍDA E APROVADA**): `dashboard` — visão derivada `GET /api/v1/dashboard`; **sem** tabela/entidade JPA; D282–D289 **fechadas** e **implementadas**. **Não** criar tabela `dashboard`. **Não** criar `GET /dashboard/monthly` nem `GET /projections/monthly`.
- Não criar módulo genérico `transactions` para agrupar operações financeiras diferentes
- API: `/api/v1`
- Moeda V1: BRL
- Nunca usar float ou double para valores financeiros
- Arredondamento financeiro V1: `RoundingMode.HALF_UP`, escala 2
- Formato de erro da API: o definido em `docs/25-api.md` (não RFC 7807 nesta etapa)
- Paginação da API: `items`, `totalItems`, `totalPages` (não expor `Page` do Spring Data)

Detalhes: `docs/22-stack-tecnologica.md`.

---

## 5. Tecnologias excluídas da V1

Não introduzir na V1 sem decisão futura explícita:

- Redis
- Kafka
- RabbitMQ
- Kubernetes
- microsserviços
- GraphQL
- NgRx
- Zod
- Tailwind
- Bootstrap
- H2
- SQLite
- CI/CD obrigatório
- integração bancária
- investimentos
- importação automática de extratos
- notificações
- PWA
- dark mode

---

## 6. Banco de dados

O banco oficial é PostgreSQL 18.

Regras:

- UUID v7 gerado pela aplicação; o banco armazena `UUID` e não gera o identificador;
- não misturar geração na aplicação com `DEFAULT`, `uuid_generate_v4()` ou `@GeneratedValue`;
- NUMERIC(19,2) para valores monetários; nunca `NUMERIC(19,4)` para dinheiro da V1;
- BigDecimal no Java; `RoundingMode.HALF_UP`; escala 2;
- percentuais como fração (`0.0525` = 5,25%);
- coluna booleana de estado: `active` (não `is_active`);
- Foreign Keys e constraints;
- índices quando necessários;
- Flyway para migrations; nomes no plural da tabela (`V1__create_accounts.sql`);
- Hibernate `ddl-auto=validate`; nunca `update` / `create` como fonte do schema;
- nunca alterar uma migration já executada;
- alterações posteriores via novas migrations;
- fonte de verdade do saldo: movimentações financeiras (não um `current_balance` independente).

---

## 7. Arquitetura

Aplicação monolítica modular. Não criar microsserviços na V1.

Fluxo padrão do backend:

```text
Controller
    ↓
Service
    ↓
Repository
```

Controller **não** acessa Repository diretamente. Mesmo leituras simples passam pelo Service do módulo.

Isso **não** significa criar um UseCase por operação. O padrão é **Service por módulo** (`AccountService`, `ExpenseService`, `TransferService`). O Service pode ser pequeno.

`*UseCase` só existe quando a operação for um caso de negócio nomeado, atômico e suficientemente complexo (ex.: `TransferMoneyUseCase`, se a orquestração justificar). Não criar `CreateExpenseUseCase`, `GetExpenseUseCase`, `ListExpenseUseCase`.

Toda fronteira HTTP usa DTOs. Entidades JPA nunca são expostas pela API. Não criar DTOs duplicados que representam o mesmo contrato.

Não introduzir MapStruct na Fase 1. Mapeamento manual é aceitável quando pequeno e claro. Não criar `*Mapper` automaticamente para cada entidade.

Não criar interface + `*Impl` para toda classe. Repositories Spring Data continuam sendo interfaces do framework. Não criar DAO adicional.

Não criar `common/`, `utils/`, `helpers/` ou `managers/` genéricos sem responsabilidade compartilhada real.

Organização por domínio real, pacotes no plural. Não criar pastas de domínio vazias na Fase 1 só para antecipar o futuro.

O backend é a autoridade final sobre as regras de negócio.

Frontend: validações para UX; nenhuma regra crítica só no frontend.

Frontend organizado por features (`core`, `shared`, `features/*`). A estrutura de uma feature cresce conforme a necessidade; não criar `pages/`, `components/`, `services/` e `models/` automaticamente para cada feature.

---

## 8. Multiusuário e isolamento (regra fundamental de segurança)

O sistema é multiusuário.

Todo dado financeiro deve estar relacionado ao usuário (`userId` / IdUsuario).

O backend deve obter o usuário autenticado a partir do contexto de segurança.

Nunca confiar em um `userId` enviado pelo frontend para determinar o proprietário.

Incorreto: `GET /expenses?userId=...` aceito como dono dos dados.

Correto: `GET /expenses` — o backend determina o usuário pelo contexto autenticado.

Queries e operações de persistência devem filtrar pelo usuário autenticado.

Um usuário nunca pode consultar, alterar ou excluir dados pertencentes a outro usuário.

O modelo físico impede referência cruzada entre usuários (despesa A + categoria B, etc.) via `user_id` e FKs compostas. Detalhe: `docs/23-modelo-de-dados.md` seções 264–266.

---

## 9. Segurança

- Senhas nunca em texto puro; hash com **Argon2id**.
- Autenticação: Spring Security + JWT Access Token (HS256, 30 minutos, Bearer).
- Refresh Token **não** foi implementado na Fase 3. Não implementar sem autorização explícita.
- Segredos apenas em variáveis de ambiente; nunca versionar credenciais.

Detalhes: `docs/26-seguranca.md`.

---

## 10. Valores monetários

- Java: `BigDecimal`
- PostgreSQL: `NUMERIC(19,2)`
- Nunca float/double
- `RoundingMode.HALF_UP` em todos os cálculos financeiros da V1; nenhum Service escolhe outro modo
- Valores monetários normalizados para escala 2 quando aplicável
- Parcelamentos: residual de centavos absorvido pela **primeira** parcela
- Percentuais: fração (`0.0525` = 5,25%)

---

## 11. Regras financeiras fundamentais

### 11.1 Receitas

O registro em `incomes` é a duplicata (título a receber). Não existe entidade separada.

Cancelamento e estorno **não são a mesma operação**.

```text
CRIAR RECEITA
      ↓
   EXPECTED
    ↙     ↘
RECEBER   CANCELAR
   ↓          ↓
RECEIVED   CANCELLED
   ↓
ESTORNAR
   ↓
EXPECTED
```

- `EXPECTED` — duplicata ativa, ainda não recebida; não altera saldo; participa de projeções; `account_id` e `received_date` nulos
- `RECEIVED` — recebimento efetivo; gera entrada financeira; `account_id` e `received_date` obrigatórios
- `CANCELLED` — cancelamento inutiliza a duplicata; o registro permanece para histórico; não é mais receita pendente; não pode ser recebida; não participa de projeções nem do saldo efetivo
- Recebimento: `POST /incomes/{id}/receipts` (RECEIPT ACTIVE; remaining = 0 → `RECEIVED`)
- Cancelamento: `EXPECTED` → `CANCELLED` (`POST /incomes/{id}/cancel`); somente sem RECEIPT ACTIVE (**D73**)
- Estorno de recebimento: `POST /incomes/{id}/movements/{movementId}/reverse` (RECEIPT ACTIVE → REVERSED); **não cancela** a duplicata; `EXPECTED`/`RECEIVED` seguem o remaining; histórico permanece em `income_movements`
- Acréscimo: `POST /incomes/{id}/accruals` (ACCRUAL ACTIVE; não movimenta conta; `RECEIVED` + acréscimo reabre `EXPECTED`)
- `POST /incomes/{id}/receive` e `POST /incomes/{id}/reverse` (**legado Fase 6**) foram **removidos** na Parte 2 (**D74-A**)
- Receita `RECEIVED` não deve ser editada de forma que altere silenciosamente a movimentação já realizada; correção = estornar → editar → receber novamente (o próximo receive informa conta e data de novo)
- Receitas aceitam `responsibleType` / `responsibleName` no POST/PUT/GET de Income (**D89** / RN306 — Parte 2 implementada). Colunas físicas permanecem nullable conforme RN035–RN038.
- Contas a receber na V1 (Fase 17 — `CONCLUÍDA E APROVADA` — contrato `docs/24` §19.8 / §19.9 / `docs/25` §67 / §67A): visão derivada `GET /api/v1/receivables`; linha = `Income` `EXPECTED` ou `RECEIVED`; `CANCELLED` fora; `overdue` derivado de `EXPECTED` + `expectedDate` < hoje; sem tabela `receivables`; sem alias `dueDate`. Parte 2: tabela `income_movements` (ACCRUAL / RECEIPT; ACTIVE / REVERSED); remaining derivado; saldo usa SUM(RECEIPT ACTIVE); backfill D83 na V30; cancelar somente sem RECEIPT ACTIVE (**D73**). Decisões D73–D94 **fechadas** e **implementadas**. D88/D94: `dateType=RECEIVED` considera qualquer RECEIPT histórico; `receivedAmount`, `remaining`, saldo e `accountId` em receivables usam somente RECEIPT ACTIVE.
- **D73 (fechado):** cancelamento direto `RECEIVED` → `CANCELLED` **não** existe. A Fase 6 rejeita. A Parte 2 mantém a rejeição: se houver RECEIPT ACTIVE, primeiro estornar a movimentação.

### 11.2 Despesas — status oficiais

- `OPEN`
- `PARTIALLY_PAID`
- `PAID`
- `CANCELLED`
- `REFUNDED`

`OVERDUE` **não** é status persistido. **1/1:** a API expõe `overdue` da despesa quando status é `OPEN`/`PARTIALLY_PAID` e `dueDate` < hoje em `America/Sao_Paulo`. **N>1:** `overdue` da despesa é `true` quando existe pelo menos uma parcela overdue segundo RN241 (`OPEN`/`PARTIALLY_PAID`, `remaining > 0`, `due_date` < hoje, despesa não `CANCELLED`/`REFUNDED`). Não usar somente `expenses.due_date` para N>1. A UI pode exibir "VENCIDA".

Formas de pagamento: `ACCOUNT`, `CREDIT_CARD`, `NONE`.

Contrato da Fase 7 (`ACCOUNT` e `NONE` apenas) — implementado:

- criação sempre `OPEN`; não gera `payments`; não altera saldo;
- `ACCOUNT` exige `account_id` de referência; `NONE` mantém `expenses.account_id` nulo; `payment_method` não muda no pagamento;
- internamente parcela 1/1; a API da Fase 7 não exige `installmentId` no pagamento;
- `CREDIT_CARD` fora desta fase;
- `payments.type` permanece sem valores oficiais (`null`).

A RN210 da Fase 7 (pagamento `ACCOUNT` obrigatoriamente na mesma conta da despesa) foi **SUPERADA** pelo contrato da Fase 8: `expenses.account_id` é preferência; a conta efetivamente movimentada é `payments.account_id` e pode diferir. Detalhe: `docs/24` RN228.

Contrato da Fase 8 (**implementado**): despesas parceladas, pagamento por parcela, `payments.status` (`ACTIVE` / `REVERSED`), adjustments (`DISCOUNT` / `SURCHARGE`) com HTTP create/list/reverse (`docs/25` §47), reverse de payment e de adjustment, refund misto. Cartão e fatura entram na **Fase 9**. Relatórios de apresentação permanecem fora. Detalhe: `docs/24` seção 19.2.

Contrato da Fase 9 (**implementado** e **concluído**): fase expandida de cartões de crédito — cadastro, limite, compra `CREDIT_CARD`, ciclo, faturas (`SCHEDULED` / `OPEN` / `CLOSED` / `PAID`), fechamento automático, pagamento de fatura com rateio persistido (desempate RN247), créditos de cartão (FIFO + ordem de faturas RN246), `due_date` da fatura (RN099B), ajustes com `reason`, reverse de pagamentos de fatura, cancelamento/estorno de compra no cartão (RN117). Fora da Fase 9: parcelamento/negociação/renegociação de fatura (**Fase 13**, `docs/24` §19.4 / RN254 — `CONCLUÍDA E APROVADA`; status de fatura inclui `SETTLED_BY_AGREEMENT`), relatórios/PDF, frontend financeiro, Refresh Token, `payments.type`, auditoria genérica, edição cadastral de parcela já em fatura (§269.2.7), `POST /invoices/{id}/close`. Detalhe da Fase 9: `docs/24` seção 19.3 e `docs/28`.

### 11.3 Cancelamento e estorno

Cancelamento e estorno **não são sinônimos**.

Sem exclusão física. Em despesas:

- cancelar: somente `OPEN` → `CANCELLED` (`POST /expenses/{id}/cancel`); sem impacto de saldo; `PARTIALLY_PAID` e `PAID` **não** se cancelam (usar refund). Em `CREDIT_CARD`, o cancelamento de `OPEN` segue RN117 (sem crédito; sem movimento bancário);
- estornar a despesa: `PARTIALLY_PAID` ou `PAID` → `REFUNDED` (`POST /expenses/{id}/refund`); **não** volta a `OPEN`; **não** apaga `payments`; o saldo deixa de subtrair esses pagamentos; refund é da despesa inteira (Fase 8: parcelas com payment ativo → `REFUNDED`; sem payment → `OPEN` somente para consulta). Em `CREDIT_CARD`, o refund exige `settlement`: `CARD_CREDIT` (crédito de cartão = valor liquidado) ou `ACCOUNT` (devolve à conta só o que saiu dela; a parte paga com crédito de cartão volta como crédito) — RN117;
- reverse de payment: `POST /api/v1/payments/{id}/reverse` entra na **Fase 8** (`ACTIVE` → `REVERSED`); não apaga o fato; não é permitido se a despesa estiver `REFUNDED` ou `CANCELLED`. A menção anterior a “fase futura” está **SUPERADA**;
- reverse de adjustment: `POST /api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments/{adjustmentId}/reverse` (RN239) — `ACTIVE` → `REVERSED`; não apaga o fato; proibido se a despesa estiver `CANCELLED` ou `REFUNDED`; create/list em `.../installments/{installmentId}/adjustments` (`docs/25` §47);
- a partir da Fase 9, todo adjustment (parcela ou fatura) exige `reason`; tipos continuam `DISCOUNT` / `SURCHARGE` (RN232 emendada);
- ajuste de fatura `SURCHARGE` exige `remaining > 0` (RN247A); `remaining = 0` → **400**, `BUSINESS_RULE_VIOLATION`, `SURCHARGE_REQUIRES_REMAINING` (`"O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto."`);
- em `POST /expenses/{id}/refund`, para despesas `ACCOUNT`/`NONE`, o campo `settlement` é aceito estruturalmente pelo DTO compartilhado, porém proibido: **400**, `BUSINESS_RULE_VIOLATION`, `SETTLEMENT_NOT_ALLOWED` (não é propriedade desconhecida);
- despesa `CREDIT_CARD` **não** se liquida por `POST /expenses/{id}/pay` nem por pagamento de parcela: a liquidação é só via fatura (`docs/25` faturas);
- `CANCELLED` / `REFUNDED` não impactam saldo, projeções, totais, gráficos nem contas a pagar.

Não copiar o estorno de receita (`RECEIVED` → `EXPECTED`) para despesa. Reverse de payment **não** usa `payments.type` e **não** replica `Income.reverse()`.

Em receitas (ver 11.1):

- cancelar inutiliza a duplicata (`EXPECTED` → `CANCELLED`);
- estornar desfaz o recebimento e mantém a duplicata ativa (`RECEIVED` → `EXPECTED`).

Receitas não possuem `REFUNDED` nem `REVERSED`.

Não utilizar `DELETE` HTTP como operação padrão para dados financeiros. Preferir ações explícitas (`POST /expenses/{id}/pay`, `POST /expenses/{id}/cancel`, `POST /expenses/{id}/refund`, `POST /incomes/{id}/accruals`, `POST /incomes/{id}/receipts`, `POST /incomes/{id}/movements/{movementId}/reverse`, `POST /incomes/{id}/cancel`, `POST /payments/{id}/reverse`, `POST .../adjustments/{id}/reverse`, na Fase 9 endpoints de fatura/crédito, e na Fase 14 `POST /transfers/{id}/reverse` e reverse de acerto de saldos — contrato §19.5). `DELETE` só pode existir para recurso não financeiro com regra explícita. Cartão com histórico financeiro **não** se exclui (inativar).

### 11.4 Compra no cartão

Não reduz saldo bancário imediatamente; aumenta o comprometimento (limite usado derivado).

A RN029A (recusar compra acima do limite disponível) está **SUPERADA**. A compra **deve ser permitida** mesmo que `available_limit` fique negativo. Eventual alerta visual é da camada de apresentação (fora da Fase 9) e **não** bloqueia o backend.

Cartão inativo não aceita novas compras. Histórico permanece. Compra no dia do fechamento pertence ao **próximo** ciclo (RN095). O ciclo usa a **data da compra** e o `closing_day` do cartão em `America/Sao_Paulo` — não o fato de existir fatura `OPEN` (o fechamento automático pode estar atrasado).

Para `CREDIT_CARD`, o `dueDate` enviado na criação da despesa **não** define o vencimento das parcelas. O backend usa o vencimento da fatura de cada ciclo (RN099B: se `due_day` > `closing_day`, due no mesmo mês da `closing_date`; se `due_day` ≤ `closing_day`, no mês seguinte; RN098 se o mês não tiver o dia). `expenses.due_date` persistido = vencimento da primeira parcela (due da fatura do ciclo da compra).

### 11.5 Pagamento da fatura

Gera saída na conta escolhida; **não** cria nova despesa de consumo; **não** cria linha em `payments` (tabela da despesa `ACCOUNT`/`NONE`). Despesas originais permanecem. Pagamento parcial, múltiplo e antecipado (enquanto `OPEN`) são o mesmo tipo de pagamento.

Valor não pode exceder o remaining da fatura nem o saldo disponível da conta. Qualquer conta **ativa** do usuário; não precisa ser a conta da despesa.

O pagamento é rateado entre as parcelas com `remaining > 0` daquela fatura (proporcional ao remaining, ordenação remaining ASC, empate `due_date` ASC depois `id` ASC, residual na última). O rateio é fato persistido (`credit_card_invoice_payment` → alocação → `expense_installment`). Libera limite na mesma proporção.

Créditos de cartão aplicam-se automaticamente: FIFO dos créditos; faturas elegíveis (`OPEN`/`CLOSED` com remaining > 0) por `due_date` ASC depois `id` ASC (RN246). `GET /credit-cards/{id}/credits` retorna **array** de créditos com `remainingAmount` por crédito; o saldo disponível total é derivado (`SUM(remainingAmount)`), sem envelope.

Pagamento parcial **não** altera o status da fatura (`OPEN` permanece `OPEN`; `CLOSED` permanece `CLOSED` até remaining = 0 → `PAID`).

### 11.6 Transferência

Operação própria, atômica: saída na origem + entrada no destino. Não é receita nem despesa.

Somente contas `BANK_ACCOUNT` ativas do mesmo usuário; origem ≠ destino; `CASH` e cartões excluídos. Valor positivo; sem saldo insuficiente (criação e reversão). Status: `ACTIVE` / `REVERSED`. Retroativa permitida; futura não. Não editável — correção = reverter + criar nova. Listagem MVP sem filtro de `status` (pode retornar ACTIVE e REVERSED).

Contrato: `docs/24` §19.5. Status da Fase 14: `CONCLUÍDA E APROVADA`.

### 11.7 Saldo negativo

Operações normais não permitem saldo negativo (transferências e suas reversões, acertos de saldo e suas reversões quando o efeito for de saída, pagamento de despesas, pagamento de fatura limitado ao saldo da conta).

Estorno de receita recebida é correção: não é bloqueado se o saldo resultante for negativo (**D80** / RN200: reverse de RECEIPT usa a conta do movimento; conta pode estar inativa).

Estorno de despesa **não** herda essa exceção: devolve o valor dos payments **ACTIVE** à fórmula de saldo (RN216, emendada na Fase 8 por RN240) e não autoriza pagamento que deixe saldo negativo. Reverse de payment (`REVERSED`) e refund da despesa `ACCOUNT`/`NONE` também retiram o efeito daqueles payments do saldo. Refund `CREDIT_CARD` com `settlement = ACCOUNT` **soma** a devolução (`bankLiquidated`) e **não** reverte pagamentos de fatura (RN117).

### 11.8 Contas

Tipos oficiais: `BANK_ACCOUNT`, `CASH`.

Não usar: `CHECKING`, `SAVINGS`, `PERSONAL_WALLET`, `OTHER`.

`CASH` = dinheiro em espécie (ex.: Carteira Felipe). Sem entidade separada de carteira.

Conta com saldo financeiro total ≠ `0,00` **ou** valor reservado em metas > `0,00` **não** pode ser inativada (RN007A / RN274 — Fase 15 emenda Fase 14).

Saldo inicial (RN010 / RN010A / Fase 14): começa em `0,00`; `initialBalance` opcional na criação; definição/alteração só via `PUT /accounts/{id}/initial-balance` enquanto a conta nunca tiver tido movimentação efetiva; após a primeira movimentação (mesmo cancelada/revertida), correção = Acerto de Saldos. **Limitação histórica inevitável:** incomes totalmente revertidos **antes** da V28 podem ser indetectáveis no backfill, porque a Fase 6 remove o vínculo com a conta. Isso não é bug da Fase 14.

### 11.9 Saldo

Fonte de verdade: movimentações. Saldo derivado delas, a partir do saldo inicial.

**Saldo financeiro total** (`totalBalance`): fórmula RN240 — saldo inicial + SUM(RECEIPT ACTIVE na conta) − payments ACTIVE − pagamentos de fatura ACTIVE + devoluções ACCOUNT + transferências ACTIVE (entrada − saída) + acertos de saldo ACTIVE. Contribuições/resgates de meta **não** alteram o total. RN010A consulta qualquer RECEIPT histórico na conta (ACTIVE ou REVERSED) (**D91-A**).

**Fase 15 (`CONCLUÍDA E APROVADA`):** cada conta expõe também **valor reservado em metas** (`reservedAmount`) e **saldo disponível** (`availableBalance = totalBalance − reservedAmount`). Contribuição classifica dinheiro como reservado (reduz disponível); resgate devolve à conta vinculada. Operações financeiras normais validam **saldo disponível**, não o total. Contrato: `docs/24` §19.6.

A partir da Fase 8 (RN240): o subtraendo de despesas `ACCOUNT`/`NONE` usa somente payments `ACTIVE` de despesas não `CANCELLED`/`REFUNDED`. A partir da Fase 9, o saldo também subtrai pagamentos `ACTIVE` de fatura (`credit_card_invoice_payments`) na conta utilizada e **soma** devoluções `ACCOUNT` de compra no cartão (RN117). A Fase 14 inclui transferências e acertos `ACTIVE`. Crédito de cartão **não** movimenta conta. `GET /accounts/{id}/balance` é leitura derivada; as-of-date é capacidade interna (Fase 14). **Exceção as-of (RN263):** `card_purchase_account_refunds` usa `created_at`.

Cache/`current_balance` só se mantido transacionalmente consistente com as movimentações — nunca duas fontes independentes.

**Acerto de Saldos** (nome conceitual; técnico `BALANCE_ADJUSTMENT`; tabela `account_balance_adjustments`) é fato próprio de conciliação (não é receita, despesa, transferência nem adjustment de parcela/fatura). Contrato: `docs/24` §19.5. Status: `CONCLUÍDA E APROVADA`. Não criar entidade genérica `Transaction`. Extrato unificado `/statement` fora da Fase 14.

---

## 12. Responsáveis pelas despesas

Valores oficiais:

- `MINE`
- `GIULIA`
- `EDERSON`
- `ELISIANE`
- `OTHER` (permite descrição textual)

Não são usuários do sistema. Apenas classificação para controle e prestação de contas.

Receitas não utilizam responsável na Fase 6. Esta seção não muda as regras de responsável das despesas.

---

## 13. Cartões de crédito

Campos essenciais incluem:

- nome/apelido;
- `holderName` (titular textual — não precisa ser o usuário autenticado; filtrável);
- `last_four_digits` (opcional; **não** obrigatório);
- limite contratado (`credit_limit`, persistido);
- limite usado / disponível (derivados; **não** persistir `used_limit` / `available_limit`);
- dia de fechamento;
- dia de vencimento;
- `active` (nasce ativo; inativar/reativar; inativar impede novas compras);
- usuário proprietário do registro.

Não armazenar PAN completo, CVC, senha nem validade do plástico. Não criar `expiration_date` para validade física.

Não excluir cartão que possua histórico financeiro.

Um cartão pode ser usado por diferentes responsáveis nas despesas.

Limite usado: valor ainda não liquidado das compras/parcelas do cartão. Compra de R$ 1.000 em 10× R$ 100 consome R$ 1.000; cada parcela liquidada (inclusive via rateio de pagamento/crédito/ajuste) libera o valor correspondente. Crédito de cartão **não** aumenta o limite. Alterar `credit_limit` é permitido mesmo que fique abaixo do usado (`available_limit` negativo).

Alterar `closingDay` / `dueDay` vale só para **ciclos futuros**. Não reescrever faturas, parcelas nem pagamentos já existentes.

Regras de ciclo:

- compra no dia do fechamento pertence à próxima fatura (RN095);
- dia configurado inexistente no mês → último dia daquele mês (RN098);
- `due_date` da fatura: se `due_day` > `closing_day`, mesmo mês da `closing_date`; se `due_day` ≤ `closing_day`, mês seguinte (RN099B).

---

## 14. Faturas

Status persistidos da fatura: `SCHEDULED`, `OPEN`, `CLOSED`, `PAID` (Fase 9); `SETTLED_BY_AGREEMENT` (Fase 13 — liquidação por Agreement; terminal).

`PARTIALLY_PAID` **não** é status de fatura (a menção anterior está **SUPERADA**). Pagamento parcial não muda o status da fatura.

`OVERDUE` derivado da data de vencimento (não persistido).

Fluxo: `SCHEDULED` → `OPEN` → `CLOSED` → `PAID` **ou** `CLOSED` → `SETTLED_BY_AGREEMENT`. No máximo **uma** fatura `OPEN` por cartão. Faturas futuras de parcelamento nascem `SCHEDULED` (parcela já vinculada; **não** usar `invoice_id` nulo).

`PAID` exige fatura já fechada **e** remaining = 0 por pagamentos/créditos/ajustes. `OPEN` com remaining 0 **não** é `PAID` até o fechamento. `PAID` e `SETTLED_BY_AGREEMENT` são terminais: nada altera a fatura.

Campos persistidos: cartão, período, fechamento, vencimento, status.

Valor total, valor pago e saldo restante são **derivados** (não colunas). Remaining da fatura = soma dos remainings das parcelas do ciclo. Detalhe: `docs/23-modelo-de-dados.md`.

Itens da fatura: `expense_installments` (`invoice_id` na parcela, nunca na despesa). Uma compra parcelada atravessa várias faturas (RN085).

Pagamento parcial permitido. Parcelamento/negociação do **saldo restante** da fatura é operação **separada** (**Fase 13**, §19.4 / RN254 — `CONCLUÍDA E APROVADA`), **fora da Fase 9**, e **não** apaga/modifica compras originais. V13 `credit_card_invoice_installments` está **SUPERADO** (§269.5); modelo oficial = Agreement + settlement + expense `CREDIT_CARD`. Nova negociação: `financedAmount = invoiceRemaining − entryAmount`. Renegociação: `financedAmount = (invoiceRemaining − entryAmount) + anticipatedFuturesNetAmount`; desconto financeiro dos futuros ≠ incorporação do líquido (RN254). `contractedTotal >= financedAmount`. `used_limit` compromete o **contractedTotal**.

Fechamento **não** é ação normal do usuário. Scheduler Spring, idempotente: abre `SCHEDULED` cujo ciclo iniciou; fecha `OPEN` cuja `closing_date` chegou (`remaining > 0` → `CLOSED`; `remaining = 0` → `PAID`); `CLOSED` com remaining 0 → `PAID`. Não reabrir. Não fechar de novo. Não alterar `PAID` nem `SETTLED_BY_AGREEMENT`.

---

## 15. Parcelamentos

Gerar automaticamente todas as parcelas futuras.

`installmentCount` omitido na criação = 1 (compatível com a Fase 7). Se informado, deve ser `> 0`. A quantidade **não** pode ser alterada depois.

Parcelas podem ter valores diferentes; a soma deve ser exatamente `expenses.total_amount` na criação e após edição cadastral de parcela `OPEN`.

Arredondamento determinístico; residual de centavos na **primeira** parcela. Sem perda de centavos. Não há mínimo contratual por parcela: `0.00` é permitido se for consequência inevitável da divisão (ex.: `0,01` / 3).

`expenses.due_date` é o vencimento da primeira parcela. Em `ACCOUNT`/`NONE`, demais vencimentos: mensais, dia-base da `dueDate` original; mês sem aquele dia → último dia daquele mês (sem “carregar” o dia ajustado). Em `CREDIT_CARD`, cada parcela vence na `due_date` da fatura do respectivo ciclo (calculada pelo backend).

Pagamento de N>1 `ACCOUNT`/`NONE` é **por parcela**. `POST /expenses/{id}/pay` permanece para 1/1 `ACCOUNT`/`NONE`. Despesa `CREDIT_CARD` não usa esses endpoints de pagamento.

Edição cadastral de parcela `OPEN`: `amount` e `due_date`; alterar `amount` exige que a soma continue igual ao total; sem redistribuição automática.

Payment, adjustment, reverse, refund e cancel **não** alteram `expenses.total_amount`. Exceção cadastral (não é fato financeiro): enquanto a despesa estiver `OPEN` e for 1/1, o `PUT` da despesa (RN217) pode alterar o total — desde que o `obligation` resultante permaneça válido (RN231 / RN245). N>1: quantidade imutável; sem redistribuição automática; alteração de `amount` de parcela segue RN227.

Em compra no cartão (Fase 9), cada parcela referencia a fatura do respectivo ciclo (`expense_installments.invoice_id`) desde a criação. A despesa original não possui `invoice_id`. Cartão permanece **fora da Fase 8** (implementação na Fase 9).

---

## 16. Número do boleto

Campo opcional na despesa, para cópia no pagamento. O sistema não gera boletos.

---

## 17. Metas, projeções, relatórios e gráficos

- Metas na V1 (Fase 15 — contrato `docs/24` §19.6): reserva vinculada a uma conta; nome, valor alvo, acumulado derivado (`contributions − redemptions`), data alvo opcional, progresso derivado (`HALF_UP`, escala 2), status (`ACTIVE`/`COMPLETED`/`CANCELLED`); contribuição, resgate (inclusive em `COMPLETED`), conclusão manual e cancelamento (com reservado zero, somente `ACTIVE`).
- Contas a pagar na V1 (Fase 16 — contrato `docs/24` §19.7 / `docs/25` §66 — `CONCLUÍDA E APROVADA`): visão derivada `GET /api/v1/payables`; linha = parcela ACCOUNT/NONE com remaining > 0 **ou** fatura com remaining > 0; **sem** tabela `payables`. `reservedAmount` de meta **não** é conta a pagar.
- Contas a receber na V1 (Fase 17 — **`CONCLUÍDA E APROVADA`** — contrato `docs/24` §19.8 / §19.9 / `docs/25` §67 / §67A): visão derivada `GET /api/v1/receivables`; movimentações em `income_movements`; D73–D94 implementadas; escrita de responsável (**D89**).
- Projeções na V1 (Fase 18 — contrato `docs/24` §19.10 / `docs/25` §68 — **CONCLUÍDA E APROVADA**): visão derivada de fluxo de caixa `GET /api/v1/projections`; saldo atual + entradas futuras − saídas futuras; remaining oficial; fatura (não compra) como saída de cartão; sem persistência; sem recorrência; D95–D204 **fechadas** e **implementadas**.
- Dashboard na V1 (Fase 19 — contrato `docs/24` §19.11 / `docs/25` §71 — **CONCLUÍDA E APROVADA**): visão derivada `GET /api/v1/dashboard`; reutiliza saldo RN240, payables, receivables e `ProjectionService`; sem eventos no envelope; sem frontend; D282–D289 **fechadas** e **implementadas**. **Não** criar tabela `dashboard`.
- PDF: **OpenPDF** (ex.: relatório por responsável em cartão de terceiro).
- Gráficos: **Apache ECharts**.

---

## 18. Testes

Obrigatórios para regras críticas. Backend: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL).

Priorizar: regras financeiras, parcelamentos, arredondamentos, faturas, pagamentos parciais, transferências, limite de cartão, isolamento, cancelamentos, estornos, autenticação/autorização.

Frontend: framework oficial do Angular 22.x. E2E Playwright posteriormente.

Detalhes: `docs/27-testes.md`.

---

## 19. API

REST em `/api/v1`. DTOs, validação, status HTTP adequados, erros padronizados, autenticação/autorização, OpenAPI/Swagger.

Métodos: `GET` leitura; `POST` criação ou ação de negócio; `PUT` substituição completa quando aplicável; `PATCH` alteração parcial quando aplicável. Não criar endpoint só porque o verbo existe.

Erros: formato de `docs/25-api.md` (`timestamp`, `status`, `code`, `message`, `path`, `fields` quando houver validação). Não adotar RFC 7807 nesta etapa. Não criar um segundo formato paralelo.

Paginação: `items`, `page`, `size`, `totalItems`, `totalPages`. Não expor `content` / `totalElements` do Spring Data `Page`.

Não criar todos os endpoints antecipadamente. Detalhes: `docs/25-api.md`.

---

## 20. Frontend

Angular 22.x, TypeScript strict, Standalone, Signals, Services, Reactive Forms, Material, HttpClient, Interceptors, Guards, ESLint, Prettier.

Sem NgRx e sem Zod na V1.

Sem `BaseComponent`, `GenericCrudService`, design system prematuro ou estado global por padrão.

Signals sob demanda. RxJS quando a API já for stream. Sem `*StateService` por feature.

O frontend não usa o timezone do navegador para decidir regras financeiras.

Validação: Angular Validators no front; Jakarta Validation + regras de negócio no backend.

---

## 21. Qualidade de código

- Frontend: ESLint + Prettier
- Backend: Spotless (Google Java Format) — formatação consistente e automatizável

Não adicionar ferramentas de qualidade sem necessidade.

---

## 22. Docker

PostgreSQL via Docker Compose no desenvolvimento (imagem `postgres:18-alpine`).

Docker Engine ≥ 24, daemon em execução. Compose V2 ≥ 2.24 (`docker compose`).

Backend e frontend podem rodar localmente fora do Docker.

Não criar configuração de produção complexa na V1.

---

## 23. Git e GitHub

Git ≥ 2.39 (recomendado: versão atual do Git for Windows).

Desenvolvimento no Cursor. Commits/pushes manuais pelo desenvolvedor (VSCode).

A IA NÃO deve presumir acesso ao GitHub nem executar push.

---

## 24. Gitignore

Nunca versionar: credenciais, `.env`, senhas, tokens, certificados privados, `node_modules`, `target`, `dist`, temporários, logs, gerados, configs pessoais de IDE.

---

## 25. Finalidade educacional

Em decisões técnicas importantes, explicar de forma proporcional: o quê, por quê, alternativas e adequação ao projeto.

---

## 26. Escopo da V1

Usuários, autenticação, contas, categorias, receitas, despesas, cartões, faturas, parcelamentos, pagamentos (incl. parciais), transferências, estornos, cancelamentos, metas, projeções, dashboard, gráficos, relatórios, exportação PDF, testes, Docker, PostgreSQL, migrations, Swagger/OpenAPI.

---

## 27. Fora da V1

Além das tecnologias excluídas (seção 5): deploy em produção, compartilhamento familiar, contas compartilhadas, automações bancárias, integrações externas.

---

## 28. Regra de parada e governança de lacunas

Parar e solicitar orientação quando:

- decisão de negócio não definida;
- conflito entre requisitos;
- mudança significativa de escopo;
- risco de perda de dados;
- regra financeira ambígua;
- testes sem correção segura;
- biblioteca não aprovada.

Não assumir decisões importantes de negócio. Usar: **DECISÃO PENDENTE DO DESENVOLVEDOR** / **PENDÊNCIA DE DECISÃO**.

Não criar automaticamente: UseCase por CRUD; interface + implementação para toda classe; DAO sobre Spring Data; Mapper para toda entidade; MapStruct sem necessidade; `common/` genérico; Domain Events; Specification; Strategy; Hexagonal Architecture; Clean Architecture por camadas artificiais; NgRx; `BaseComponent`; `GenericCrudService`.

Antes de criar classe, interface, service, mapper, componente, pasta ou abstração: existe responsabilidade real que justifique isso **agora**? Se não, não criar.

### 28.1 Regra máxima

Nenhuma lacuna de negócio pode ser preenchida por suposição técnica.

Não escolher automaticamente a alternativa mais simples, convencional ou conveniente.

Se a decisão puder alterar modelo, relacionamento, cálculo, status, persistência vs derivação, ownership, API, Service, migration, constraint, índice, enum, CHECK ou qualquer regra de negócio: **parar e solicitar decisão explícita**.

Não criar código provisório, migration “preparatória”, enum/CHECK com valores presumidos, nem alterar documentação para justificar decisão ainda não aprovada.

### 28.2 Hierarquia de fontes (conflito)

1. Decisão explícita do usuário
2. Regras de negócio oficiais (`docs/24`)
3. Modelo de dados oficial (`docs/23`)
4. Arquitetura oficial (`docs/21`)
5. API / documentação técnica (`docs/25` e correlatos)
6. `docs/CODING_STANDARDS.md`
7. Implementação existente
8. Inferência técnica

Implementação existente nunca justifica regra de negócio conflitante com a documentação oficial. Código pode estar errado ou desatualizado.

### 28.3 Pendências oficiais (bloqueadas)

Até decisão explícita, **não** implementar Flyway, entidade, enum, CHECK, teste ou regra dependente de:

1. `payments.type` — o campo existe no modelo; valores oficiais **não** existem. **Não** usar `type` para `ACTIVE`/`REVERSED`. O estado do payment na Fase 8 é a coluna **`payments.status`**. Pagamento de fatura **não** usa `payments` nem `payments.type`. Não criar enum, CHECK, constantes, validações nem regras sobre `type`.
2. Edição de parcela já vinculada a fatura × `expenses.total_amount` — a edição cadastral de parcela `OPEN` **sem fatura** (ACCOUNT/NONE) está **fechada** no contrato da Fase 8 (RN227): `total_amount` não muda; soma das parcelas deve continuar igual ao total; sem redistribuição; rollback se não fechar. A pergunta do §269.2 sobre parcela **já pertencente a uma fatura** permanece **DEFERIDA**.

**SUPERADO (Fase 13 D1–D11):** o antigo bloqueio §269.5. Decisões fechadas. Emenda de renegociação (RN254) **implementada e aprovada**. Status: `CONCLUÍDA E APROVADA`. Detalhe: `docs/24` §19.4 e `docs/23` §269.5.

**SUPERADO (Fase 14):** transferências, Acerto de Saldos (`BALANCE_ADJUSTMENT` / `account_balance_adjustments`), saldo inicial (RN010 / RN010A) e inativação com saldo zero (RN007A). Status: `CONCLUÍDA E APROVADA`. Detalhe: `docs/24` §19.5.

**Fase 15 — Metas:** contrato `docs/24` §19.6 / API `docs/25` §54E — **`CONCLUÍDA E APROVADA`**. Fora do escopo: frontend, dashboard, projeções, reverse de contribuição/resgate, DELETE, ledger genérico.

**Fase 16 — Contas a pagar:** contrato `docs/24` §19.7 / API `docs/25` §66 / testes `docs/27` §40E — `CONCLUÍDA E APROVADA`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes; não reabrem a fase). **Não** criar tabela `payables` nem persistir remaining. Fora do escopo: frontend, dashboard, projeções, escritas, `GET /payables/{id}`.

**Fase 17 — Contas a receber (Parte 1 + Parte 2):** `docs/24` §19.8 / §19.9 / `docs/25` §67 / §67A / `docs/27` §40F / §40G / `docs/23` §269.6 — **`CONCLUÍDA E APROVADA`**. Decisões D73–D94 **fechadas** e **implementadas**. Migration V30; rota existente `GET /api/v1/receivables` — **não** recriar. **Não** criar tabela `receivables`. **Fase 18 — Projeções:** **`CONCLUÍDA E APROVADA`** (`docs/24` §19.10; D95–D204 **fechadas** e **implementadas**). Endpoint `GET /api/v1/projections`. **Não** criar tabela `projections`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes). **Fase 19 — Dashboard:** **`CONCLUÍDA E APROVADA`** (`docs/24` §19.11; D282–D289). Endpoint `GET /api/v1/dashboard`. **Não** criar tabela `dashboard`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes). Suíte: **519** testes.

**SUPERADO (Fase 9):** o antigo item 269.3 (rateio). Rateio proporcional ao remaining, ordenação remaining ASC, empate `due_date` ASC depois `id` ASC, residual na última, persistido como alocação. Status da fatura **não** muda por pagamento parcial. Detalhe: `docs/23` §269.3 e `docs/24` RN247.

**SUPERADO (Fase 9):** o antigo item 269.4 (estorno de compra no cartão já liquidada). Opções HTTP: cancel se `OPEN`; refund com `settlement` `CARD_CREDIT` ou `ACCOUNT` se `PARTIALLY_PAID`/`PAID`. Detalhe: `docs/23` §269.4 e `docs/24` RN117.

Detalhe: `docs/23-modelo-de-dados.md` seção 269.

O restante do modelo já consolidado continua válido e é fonte de verdade.

### 28.4 Novas lacunas durante a implementação

1. Identificar: documento consultado, regra existente, o que falta, o que a decisão trava.
2. Não implementar a parte dependente.
3. Apresentar alternativas de negócio de forma neutra.
4. Aguardar aprovação.
5. Só então: atualizar documentação / RN / modelo / API / testes, e implementar.

### 28.5 Banco, derivados, ownership, JPA, testes

- Migration: se entidade, FK, nullable, CHECK, enum, índice, derivado vs persistido, cascade ou exclusão depender de decisão em aberto, **não criar a migration**.
- Não criar colunas para valores definidos como derivados (`total_amount` / `paid_amount` / `remaining_amount` da fatura; `used_limit` / `available_limit` do cartão; `current_amount` da meta; `paid_amount` / `remaining_amount` / `discount_total` / `surcharge_total` / `early_payment_savings` da despesa ou parcela; totais da visão Contas a Pagar; totais da visão Contas a Receber). A Fase 16 **não** cria tabela `payables`. A Fase 17 Parte 1 **não** cria tabela `receivables`. A Fase 17 Parte 2 **não** persiste remaining/recebido/acumulado em `incomes` (fonte = movimentações + valor original; `docs/24` §19.9). A evolução aditiva do item de `/receivables` (D77) **não** autoriza segunda fonte persistida. A Fase 18 **não** cria tabela `projections` nem persiste saldo/itens projetados (`docs/24` §19.10). A Fase 19 **não** cria tabela `dashboard` (`docs/24` §19.11). Otimização não autoriza segunda fonte de verdade. `status` de despesa, parcela, payment, adjustment, fatura e crédito **é** persistido por decisão explícita. Alocação de rateio **é** fato persistido (não é coluna derivada na parcela). Status da movimentação de receita (`ACTIVE` / `REVERSED`) **é** persistido na proposta da Parte 2.
- Ownership: FK composta `(referenced_id, user_id) → (parent.id, parent.user_id)`. Não trocar por FK simples só para facilitar o JPA. Service usa `user_id` do contexto autenticado; o banco também impede cruzamento.
- JPA não altera o modelo físico. Constraint no banco + mapeamento JPA compatível.
- Teste de regra indefinida é proibido (`TESTE NÃO DEFINIDO → REGRA NÃO DEFINIDA → IMPLEMENTAÇÃO BLOQUEADA`). Depois: decisão → documentação → teste → implementação.

### 28.6 O que a IA pode vs não pode decidir sozinha

Pode (semântica inalterada): nome de variável, organização de métodos, estrutura de pacote, mapper interno, detalhe que não muda contrato.

Não pode: significado de campo, cálculo, valor financeiro, status, pagamento, relacionamento, cardinalidade, persistido vs derivado, ownership, parcelamento, cancelamento/estorno, qualquer regra que altere o resultado financeiro ou o comportamento esperado pelo usuário.

Dúvida “posso assumir que funciona assim?” → **não, se for decisão de negócio**.

---

## 29. Regra final

Construir uma aplicação organizada, segura, testável, compreensível, moderna, extensível, adequada ao aprendizado e com regras financeiras confiáveis.

Priorizar qualidade e clareza em vez de velocidade de implementação.
