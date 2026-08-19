# Roadmap — Financial Control

## 0. Hierarquia

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

A IA não deve implementar fases futuras sem autorização explícita.


## 1. Objetivo

Este documento define a ordem de desenvolvimento do Financial Control.

O projeto será desenvolvido de forma incremental.

Nenhuma fase deve tentar implementar todo o sistema de uma única vez.


# 2. Princípio

Cada etapa deve:

1. possuir objetivo claro;
2. possuir escopo limitado;
3. possuir critérios de conclusão;
4. possuir testes;
5. ser validada antes da próxima etapa.


# 3. Regra principal

A IA não deve implementar fases futuras sem autorização explícita.


# 4. Regra

Antes de iniciar uma fase:

1. ler AGENTS.md;
2. ler documentação relacionada;
3. analisar código existente;
4. verificar dependências;
5. apresentar plano da fase;
6. aguardar aprovação quando solicitado.


# 5. Fase 0 — Planejamento

Status:

CONCLUÍDA


Objetivo:

Definir requisitos e arquitetura inicial.


Documentos:

AGENTS.md

docs/20-fluxos-financeiros.md até docs/28-roadmap.md

docs/CODING_STANDARDS.md

README.md


Resultado esperado:

Base documental do projeto consolidada (hierarquia AGENTS → docs/20–28 → CODING_STANDARDS → .cursor/rules).


# 6. Fase 1 — Estrutura inicial

Status:

CONCLUÍDA


Objetivo:

Criar a estrutura base do projeto.


Escopo:

- backend Java;
- frontend Angular;
- Docker;
- PostgreSQL;
- Git;
- configurações;
- documentação.


Não implementar funcionalidades financeiras ainda.


# 7. Fase 1 — Backend

Criar:

- projeto Java;
- estrutura de pacotes;
- configuração;
- dependências;
- health check;
- conexão com PostgreSQL.


# 8. Fase 1 — Frontend

Criar:

- projeto Angular;
- estrutura inicial;
- configuração;
- layout base;
- roteamento.


Não implementar dashboard financeiro ainda.


# 9. Fase 1 — Docker

Criar:

- PostgreSQL;
- backend;
- frontend quando aplicável.


Objetivo:

Permitir execução local padronizada.


# 10. Fase 1 — Git

Criar:

.gitignore

.cursorignore


Garantir que arquivos sensíveis não sejam versionados.


# 11. Critério de conclusão da Fase 1

Deve ser possível:

1. subir PostgreSQL;
2. iniciar backend;
3. iniciar frontend;
4. acessar health check;
5. conectar backend ao banco;
6. executar projeto localmente.


# 12. Fase 2 — Banco de dados

Status:

CONCLUÍDA


Objetivo:

Criar estrutura inicial do PostgreSQL.


Bloqueios oficiais (não migrar nem implementar a parte dependente até decisão):

- `payments.type` (`docs/23` §269.1);
- edição de parcela **já vinculada a fatura** × `expenses.total_amount` (`docs/23` §269.2.7) — DEFERIDA.

O antigo bloqueio 269.3 (rateio) está **fechado** na Fase 9. O antigo bloqueio 269.4 (estorno no cartão) está **fechado** na Fase 9 (RN117).

Governança: `AGENTS.md` seção 28. O modelo já consolidado permanece fonte de verdade.


Escopo:

- migrations;
- extensões necessárias;
- tabelas;
- constraints;
- índices;
- relacionamentos.


# 13. Fase 2

Prioridade:

users

accounts

categories

credit_cards

incomes

expenses

expense_installments

payments

transfers

credit_card_invoices

credit_card_invoice_payments

credit_card_invoice_installments

financial_goals

goal_contributions


# 14. Banco

Não criar tabelas desnecessárias para funcionalidades futuras.


# 15. Banco

Preparar arquitetura para evolução sem implementar funcionalidades futuras.


# 16. Fase 2 — Migrations

Utilizar ferramenta de migration apropriada ao stack escolhido.


# 17. Fase 2 — Seeds

Criar dados iniciais somente quando necessário.


# 18. Critério de conclusão

Banco deve:

- subir;
- executar migrations;
- possuir constraints;
- possuir relacionamentos;
- permitir rollback quando suportado pela estratégia;
- possuir estrutura documentada.


# 19. Fase 3 — Autenticação

Status:

CONCLUÍDA


Objetivo:

Implementar:

- cadastro;
- login;
- senha;
- JWT Access Token;
- autenticação;
- autorização por identidade/ownership.


# 20. Fase 3

Implementado:

POST /api/v1/auth/register

POST /api/v1/auth/login

GET /api/v1/users/me

PUT /api/v1/users/me

PUT /api/v1/users/me/password


Não implementado nesta fase: refresh token, logout backend, OAuth, MFA, roles, rate limiting, frontend de autenticação.


# 21. Fase 3 — Segurança

Implementar:

- password hashing;
- JWT;
- proteção de endpoints;
- isolamento por usuário.


# 22. Fase 3 — Testes

Testar:

- login;
- senha;
- JWT;
- usuário desativado;
- acesso sem autenticação;
- acesso entre usuários.


# 23. Critério de conclusão

Usuário consegue:

1. criar conta;
2. fazer login;
3. obter token;
4. acessar perfil;
5. acessar somente seus dados.


# 24. Fase 4 — Contas

Status:

CONCLUÍDA


Objetivo:

Implementar contas financeiras.


# 25. Fase 4

Implementar:

- criar conta;
- editar;
- listar;
- consultar;
- ativar;
- desativar;
- saldo.


# 26. Fase 4

Tipos:

BANK_ACCOUNT

CASH


# 27. Fase 4

Implementar:

saldo inicial;

saldo derivado estruturado como `initial_balance` + entradas − saídas.

Movimentações reais (receitas, despesas, transferências) **não** são implementadas nesta fase. Pertencem às fases dos respectivos domínios. A Fase 4 não cria ledger paralelo nem movimentação artificial.


# 28. Testes

Testar:

- saldo;
- entrada;
- saída;
- conta desativada;
- isolamento.


# 29. Critério de conclusão

Usuário consegue cadastrar todas as contas pessoais necessárias.


# 30. Fase 5 — Categorias

Status:

CONCLUÍDA


Objetivo:

Implementar categorias de receitas e despesas.


# 31. Fase 5

Implementar:

- criar;
- editar;
- listar;
- desativar.


# 32. Critério

Categorias devem possuir tipo:

INCOME

EXPENSE


# 33. Critério

Categorias desativadas não podem ser utilizadas em novos lançamentos.

Este critério é da regra RN033 e será aplicado nas fases de receitas e despesas. A Fase 5 não implementa lançamentos.


# 34. Fase 6 — Receitas

Status:

CONCLUÍDA

A documentação oficial da Fase 6 foi refinada antes da implementação: cancelamento inutiliza a duplicata (`EXPECTED` → `CANCELLED`); estorno desfaz o recebimento e mantém a duplicata ativa (`RECEIVED` → `EXPECTED`; limpa `account_id` e `received_date`; pode deixar saldo negativo); responsável fora desta fase (`responsible_type` nullable); saldo derivado; acerto de saldos como conceito futuro (hoje: Fase 14 §19.5 — `CONCLUÍDA E APROVADA`).

Objetivo:

Implementar controle de receitas.


# 35. Fase 6

Implementar:

- criação;
- listagem;
- consulta por ID;
- edição;
- recebimento;
- estorno;
- cancelamento;
- validações;
- ownership;
- integração com categoria;
- integração com conta;
- impacto no saldo por recebimento e estorno;
- testes;
- documentação.


Não implementar nesta fase:

- despesas;
- transferências;
- acerto de saldos (`BALANCE_ADJUSTMENT` — hoje Fase 14);
- relatórios;
- dashboard;
- frontend;
- responsável em receitas.


# 36. Estados

EXPECTED

RECEIVED

CANCELLED


# 37. Fase 6

Implementar vínculo com:

- categoria;
- conta.

Não utilizar `responsibleType` / `responsibleName` na API, nas regras nem nos testes desta fase. As colunas físicas em `incomes` permanecem; `responsible_type` é nullable.


# 38. Testes

Testar:

- receita prevista;
- receita recebida;
- receita cancelada (`EXPECTED` → `CANCELLED`; duplicata inutilizada; sem impacto no saldo);
- estorno (`RECEIVED` → `EXPECTED`; duplicata permanece ativa e pode ser recebida novamente);
- que estorno não resulta em `CANCELLED`;
- transições inválidas (incluindo rejeição de `RECEIVED` → `CANCELLED`);
- impacto no saldo (recebimento e estorno);
- isolamento;
- categoria `INCOME` ativa.


# 39. Critério de conclusão

Usuário consegue lançar, consultar, editar (quando `EXPECTED`), receber, estornar e cancelar receitas, com impacto correto no saldo.

Estorno e cancelamento devem comportar-se como operações distintas: estornar volta a `EXPECTED`; cancelar inutiliza em `CANCELLED`.


# 40. Fase 7 — Despesas simples

Status:

CONCLUÍDA

Despesas `ACCOUNT` e `NONE`; parcela interna 1/1; pagamento, cancelamento e refund; saldo derivado (RN216); `overdue` derivado; ownership; testes (`ExpenseServiceTest`, `ExpenseApiTest`); API `/api/v1/expenses` e `GET /api/v1/payments/{id}`. Contrato RN208–RN221 permanece fechado.

Objetivo:

Implementar despesas sem parcelamento funcional e sem cartão, reutilizando `expenses`, `expense_installments` (parcela 1/1) e `payments`.


# 41. Fase 7

Formas operacionais:

ACCOUNT

NONE


CREDIT_CARD, faturas e parcelas N>1: fora desta fase.


# 42. Fase 7

Implementar:

- criação (`OPEN`; sem payment; sem alteração de saldo);
- edição somente em `OPEN`;
- consulta e listagem paginada;
- pagamento (`POST /api/v1/expenses/{id}/pay`);
- cancelamento (`OPEN` → `CANCELLED`);
- estorno (`PARTIALLY_PAID` / `PAID` → `REFUNDED`);
- responsável;
- boleto (`barcode`);
- categoria `EXPENSE`;
- vencimento;
- `overdue` derivado na API;
- impacto no saldo (RN216);
- testes;
- documentação.

Não implementar nesta fase:

- cartão / fatura / ciclo;
- parcelamento funcional (N>1) nem CRUD de parcelas;
- `POST /payments/{id}/reverse` (entra na Fase 8);
- valores de `payments.type`;
- frontend.


# 43. Fase 7 — Regras fechadas

- `ACCOUNT` exige conta de referência e nasce `OPEN`; na Fase 7 o pagamento usa a mesma conta (RN210); essa restrição é **SUPERADA** na Fase 8 (RN228);
- `NONE` mantém `account_id` nulo; a conta do pagamento fica só em `payments`;
- parcela 1/1 interna; a API não exige `installmentId` no pagamento;
- cancelar só `OPEN`; estornar só `PARTIALLY_PAID`/`PAID`; estorno não apaga `payments` e não volta a `OPEN`;
- pagamento não deixa saldo negativo; estorno de despesa não usa a exceção de saldo negativo da receita.


# 44. Estados

Status persistidos:

OPEN

PARTIALLY_PAID

PAID

CANCELLED

REFUNDED


OVERDUE: derivado (não persistido); API expõe `overdue`.


# 45. Testes

Ver `docs/27-testes.md` §47.1. Incluir criação ACCOUNT/NONE, parcela 1/1, pagamentos (integral, parcial, múltiplos, excedente, saldo insuficiente, conta errada), cancelamento, refund, overdue, isolamento.


# 46. Critério de conclusão

Usuário consegue controlar despesas simples sem cartão, com impacto correto no saldo, sem conhecer parcela 1/1.


# 46.1 Observações de manutenção (não bloqueantes)

A Fase 7 está concluída. Os itens abaixo **não** são pendências da fase, **não** reabrem RN208–RN221 e **não** criam fase intermediária:

- lock pessimista da conta para dois pagamentos concorrentes em despesas diferentes da mesma conta;
- testes adicionais de filtros de data (`startDate` / `endDate`);
- teste dedicado de PUT `ACCOUNT` ↔ `NONE`;
- teste dedicado de PUT `CREDIT_CARD`;
- teste dedicado de conta inválida/inativa na criação `ACCOUNT`;
- testes adicionais de autenticação 401;
- possível redução de N+1 na listagem de despesas.

Permanecem fora da Fase 7: cartão, fatura e ciclo; valores oficiais de `payments.type`. Reverse de payment e parcelamento N>1 pertencem à **Fase 8**.

O item de lock pessimista da conta **não** foi promovido a requisito na Fase 8 para `GET` de saldo (RN240): a leitura continua derivada sem `SELECT FOR UPDATE` da conta.


# 47. Fase 8 — Parcelamento de despesas

Status:

IMPLEMENTAÇÃO CONCLUÍDA (pendência deliberada restante: JSON aninhado completo da despesa N>1; itens §269.1 / fatura / rateio / cartão permanecem deferidos)


Objetivo:

Implementar despesas parceladas (N≥1), pagamento por parcela, adjustments, reverse de payment/adjustment e refund misto, reutilizando `expenses` / `expense_installments` / `payments`.

**Status:** concluído.

# 48. Fase 8 — Escopo

Escopo implementado:

- `installmentCount` (omitido = 1);
- geração automática de valores (residual na **primeira** parcela) e vencimentos mensais (dia-base);
- edição cadastral de parcela `OPEN` (`amount`, `due_date`) com soma = total;
- pagamento por parcela; múltiplos payments; contas diferentes (RN228);
- `payments.status` `ACTIVE` / `REVERSED` (não usar `payments.type`);
- adjustments `DISCOUNT` / `SURCHARGE` com status `ACTIVE` / `REVERSED`;
- reverse de payment (`POST /payments/{id}/reverse`) e reverse de adjustment (`POST .../installments/{installmentId}/adjustments/{adjustmentId}/reverse`);
- HTTP create/list/reverse de adjustment (`docs/25` §47);
- status agregado persistido da despesa;
- cancelamento só `OPEN`; refund misto; parcela `OPEN` em despesa `REFUNDED` só consulta;
- overdue da parcela (derivado) e overdue da despesa N>1 (pelo menos uma parcela RN241);
- listagem `startDate`/`endDate` pelas datas das parcelas;
- `UNIQUE (expense_id, installment_number)` em migration V17;
- fatos necessários a relatórios futuros (sem implementar relatórios).


# 49. Fase 8 — Fora do escopo

Não implementar nesta fase:

- `CREDIT_CARD`, fatura, ciclo, `invoice_id`, rateio (§269.3);
- valores oficiais de `payments.type` (§269.1);
- dashboard, gráficos, PDF, relatórios de apresentação;
- frontend;
- refund individual de parcela;
- novas categorias de adjustment além de `DISCOUNT` e `SURCHARGE`.


# 50. Fase 8 — Regras fechadas

Ver `docs/24` seção 19.2 (RN222–RN245) e `docs/25` seção 47.

Invariáveis: soma das parcelas = `total_amount`; payment/adjustment/reverse/refund/cancel **não** alteram o total; quantidade imutável; `expenses.due_date` = primeira parcela. Exceção cadastral: `PUT` de despesa `OPEN` 1/1 pode alterar o total (RN217/RN245), **desde que** o `obligation` resultante permaneça válido (RN231). Overdue N>1: pelo menos uma parcela RN241. Parcelas `0.00` permitidas se forem consequência da divisão em centavos (RN067/RN068). `GET` de saldo: leitura derivada sem lock de conta (RN240).


# 50.1 Fase 8 — Ressalvas de auditoria (fechadas)

Decisões documentais pós-auditoria (sem inventar produto novo):

1. **Lock de conta / GET saldo:** mantido o padrão Fase 7 — leitura derivada; sem `SELECT FOR UPDATE` só para GET; escrita protegida pelos locks das entidades financeiras (RN240, RN244).
2. **PUT 1/1 + DISCOUNT ACTIVE:** permitido se `obligation` resultante `>= 0` (e `>=` payments ACTIVE); caso contrário rejeição + rollback; adjustments não são alterados (RN231, RN245).
3. **Parcelas 0,00:** permitidas quando inevitáveis na divisão; sem mínimo artificial por parcela (RN067, RN068).


# 51. Fase 8 — Dependências

Fases 0–7. Contas, categorias, receitas, despesas 1/1, JWT, Flyway v16, saldo derivado.


# 52. Testes

Ver `docs/27-testes.md` (Fase 8). Incluir geração 1/3/12, residual na primeira, dia-base 31, soma, edição OPEN, pagamento por parcela, contas diferentes, overpayment, discount/surcharge HTTP, reverse payment/adjustment, refund misto, overdue, isolamento, concorrência.


# 53. Critério de conclusão

Usuário consegue criar despesa parcelada (compromissos futuros gerados), pagar parcela a parcela, registrar desconto/acréscimo via HTTP, reverter payment e adjustment, refundir a despesa, com saldo e status corretos — sem cartão e sem relatórios de apresentação.

**Critério atendido.** Ressalvas de auditoria (lock de conta no GET de saldo; PUT 1/1 × obligation; parcelas 0,00) estão **fechadas** em `docs/24` (RN067/RN068, RN231, RN240, RN244, RN245) e `docs/28` §50.1.

A Fase 9 tem contrato oficial expandido documentado. **Implementação concluída e fase fechada.**


# 54. Fase 9 — Cartões de crédito (fase expandida)

Status:

CONCLUÍDA

Objetivo:

Implementar o domínio de cartão de crédito conforme o contrato consolidado: cadastro, limite, compras, ciclo, faturas, pagamento, rateio, créditos, ajustes, fechamento automático, reverse de pagamentos de fatura.

As antigas Fases 10 (compras), 11 (faturas) e 12 (pagamento de fatura) deste roadmap foram **absorvidas** pela Fase 9. Não existe mais uma Fase 9 restrita a “somente cadastro de cartão”.


# 55. Fase 9 — Inclui

- cadastro, edição, ativação e desativação de cartões (`holderName` filtrável; `last_four_digits` opcional);
- limite persistido; used/available derivados (available pode ser negativo; RN029A SUPERADA);
- compras `CREDIT_CARD` (cartão ativo; acima do limite permitida);
- parcelamento com todas as parcelas na criação; cada uma com `invoice_id`;
- ciclo pela data da compra × `closing_day` (`America/Sao_Paulo`; RN095);
- faturas `SCHEDULED` → `OPEN` → `CLOSED` → `PAID`; no máximo uma OPEN por cartão;
- fechamento automático (Spring `@Scheduled`, idempotente);
- pagamento de fatura (integral, parcial, múltiplo, antecipado) sem usar `payments`;
- rateio persistido (RN247);
- liberação de limite proporcional;
- créditos de cartão (FIFO dos créditos; faturas elegíveis por `due_date` ASC, `id` ASC);
- `due_date` da fatura (RN099B);
- ajustes de parcela e de fatura com `reason`;
- reverse de pagamento de fatura;
- cancelamento de compra `OPEN` e refund com `settlement` `CARD_CREDIT` / `ACCOUNT` (RN117).

Não incluir: parcelamento/negociação de fatura (Fase 13 — §19.4 / §269.5); relatórios/PDF; frontend financeiro; Refresh Token; `payments.type`; auditoria genérica; edição de parcela já em fatura (§269.2.7); `POST /invoices/{id}/close`.


# 56. Fase 9 — Status da fatura

Status persistidos:

SCHEDULED

OPEN

CLOSED

PAID

`PARTIALLY_PAID` **não** é status de fatura.

OVERDUE: derivado da data de vencimento (não persistido).


# 57. Fase 9 — Testes

Além dos testes de cadastro de cartão:

- compra antes / no dia / depois do fechamento (RN095);
- dia 31 em mês curto (RN098);
- compra parcelada com faturas SCHEDULED;
- uma OPEN por cartão;
- pagamento integral, parcial, múltiplo, antecipado;
- rateio (soma exata, remaining como base, ASC, empate `due_date`/`id`, residual na última);
- compra acima do limite aceita;
- pagamento reduz saldo da conta; compra não reduz;
- crédito FIFO; faturas por `due_date` ASC; crédito não movimenta conta;
- `due_date` RN099B;
- cancel/refund RN117 (`CARD_CREDIT` e `ACCOUNT`);
- fechamento idempotente; PAID imutável;
- isolamento e concorrência.


# 58. Critério de conclusão da Fase 9

Usuário consegue cadastrar cartão, lançar compra (inclusive parcelada e acima do limite), ver faturas e itens, pagar fatura (parcial/antecipado) com rateio e limite corretos, usar crédito e ajustes, cancelar/estornar compra no cartão (RN117), e o fechamento automático respeita o ciclo — sem parcelar saldo de fatura e sem relatórios.

**Critério atendido.** Fase 9 — **FECHADA E APROVADA**. RN246, RN247, RN247A (`SURCHARGE` exige remaining > 0; **400** / `BUSINESS_RULE_VIOLATION` / `SURCHARGE_REQUIRES_REMAINING`), settlement (`SETTLEMENT_NOT_ALLOWED`), credits (array + `remainingAmount`) e auditoria final de conformidade confirmados. Fora da Fase 9 (permanecem fora): parcelamento/negociação de fatura (Fase 13 — `CONCLUÍDA E APROVADA`); PDF/relatórios; frontend financeiro; Refresh Token; `payments.type`; auditoria genérica; edição cadastral de parcela já em fatura (§269.2.7); `POST /invoices/{id}/close`.


# 59–72. Fases 10–12 — ABSORVIDAS

As seções anteriores “Fase 10 — Compras”, “Fase 11 — Faturas” e “Fase 12 — Pagamento de fatura” deixam de ser fases futuras independentes. O conteúdo está no contrato da Fase 9 (`docs/24` §19.3).


# 73. Fase 13 — Parcelamento, Negociação e Renegociação de Fatura

Status:

`CONCLUÍDA E APROVADA`

Objetivo:

Permitir parcelar/negociar remaining de fatura **fechada**; distinguir nova negociação de renegociação (RN254); registrar Agreement, entrada, custo adicional; antecipação individual de parcelas; histórico completo.

Base (D1–D11) e emenda de renegociação (financed consolidado, `anticipatedFuturesNetAmount`, desconto × incorporação, `contractedTotal >= financedAmount`) **implementadas, testadas e aprovadas**.


# 74. Fase 13 — Escopo contratado

- somente `CLOSED` + remaining > 0;
- `SETTLED_BY_AGREEMENT` (D1); settlement fact (D2) = `invoiceSettlementAmount`;
- expense `CREDIT_CARD` + parcelas iguais; V13 SUPERADO (D3, D4, D9);
- API `/api/v1/invoices/.../agreements|renegotiations` (D5);
- nova negociação: `financedAmount = invoiceRemaining − entryAmount`;
- renegociação: RN254 (`financedAmount = (invoiceRemaining − entry) + anticipatedFuturesNetAmount`);
- `entryAmount == remaining` → 400 (D6);
- `settled` só em anticipate (D7);
- renegociação: todos `ACTIVE` do cartão (D8);
- `CANCELLED` reservado (D10);
- `used_limit` compromete contractedTotal (D11);
- testes L01–L36 com critérios de emenda (`docs/27`) — cobertos.

Detalhe: `docs/24` §19.4.


# 75. Fase 13 — Domínio separado

Parcelamento/negociação de fatura ≠ compra parcelada. V13 sem uso de negócio.


# 76. Fase 13 — Próximo passo

Fase 13 **concluída**. Fase 14 — Transferências, Acerto de Saldos e Saldo Inicial — **CONCLUÍDA E APROVADA**.


# 77. Fase 14 — Transferências, Acerto de Saldos e Saldo Inicial

**Status:** `CONCLUÍDA E APROVADA`.

Percurso: implementação → auditoria (`APROVADO COM CORREÇÕES`) → correções → reauditoria (`APROVADO`). Encerramento: `CONCLUÍDA E APROVADA / IMPLEMENTAÇÃO E AUDITORIA ENCERRADAS`.

Objetivo:

Transferências entre contas `BANK_ACCOUNT`, Acerto de Saldos (`BALANCE_ADJUSTMENT` / tabela `account_balance_adjustments`), regras de saldo inicial (RN010 / RN010A) e inativação com saldo zero (RN007A).

Contrato oficial: `docs/24` §19.5.


# 78. Fase 14 — Escopo

Implementado:

- transferência (criar, listar, consultar, reverter);
- somente `BANK_ACCOUNT`; CASH excluído de transferências;
- data financeira (retroativa ok; futura não);
- status `ACTIVE` / `REVERSED`;
- listagem MVP **sem** filtro de `status` (pode retornar ACTIVE e REVERSED; status no recurso);
- Acerto de Saldos (criar, listar, consultar, reverter) — tabela `account_balance_adjustments`;
- saldo as-of-date interno;
- saldo inicial: `initialBalance` opcional no `POST /accounts` (default `0,00`); alteração só via `PUT /accounts/{id}/initial-balance` até a primeira movimentação (RN010A);
- bloqueio de inativação com saldo ≠ 0 (RN007A);
- extensão da fórmula de saldo (RN240).

Fora do escopo: transferências futuras/agendadas; entre usuários; extrato unificado `/statement`; ledger genérico.


# 79. Fase 14 — Atomicidade e saldo

Transferência e acerto devem ser atômicos. Sem saldo negativo nas operações normais (criação e reversão com efeito de saída).


# 80. Testes

Resultado final da suíte no encerramento: Total 374; passaram 374; falharam 0; `mvn test` = PASS; `mvn verify` = PASS.

Cobertura implementada em `Phase14ApiTest`, `Phase14Rn010aApiTest`, `Phase14ConcurrencyExtraTest`, `TransferConcurrencyTest` e `SchemaContractTest`:

- saldo e patrimônio;
- BANK_ACCOUNT vs CASH;
- saldo insuficiente (criação e reversão);
- retroativo / futuro;
- rollback;
- isolamento;
- acerto as-of-date;
- saldo inicial após primeira movimentação (mesmo revertida) — RN010A;
- backfill V28 / schema V28;
- concorrência (payment × transfer; transfer A→B × B→A; duas saídas simultâneas);
- inativação com saldo ≠ 0.

Detalhe: `docs/27-testes.md` §§34–40C.


# 81. Critério de conclusão

Usuário consegue movimentar dinheiro entre `BANK_ACCOUNT` próprias, reverter, conciliar via acerto e gerir saldo inicial conforme RN010 / RN010A — com testes e docs alinhados.

**Critério atendido. Fase 14 — CONCLUÍDA E APROVADA.**


# 82. Fase 15 — Metas

**Status:** `CONCLUÍDA E APROVADA`.

Objetivo:

Implementar metas financeiras como reservas (caixinhas) vinculadas a uma conta.

Contrato oficial: `docs/24` §19.6 / `docs/25` §54E.

Migration: **V29** (`account_id` em `financial_goals`; `goal_redemptions`; remoção de `account_id` em `goal_contributions`).

**Observação operacional (V29):** em banco local já populado, a V29 falha se existirem metas sem contribuição (não há `account_id` para backfill). Ambientes novos (Flyway do zero / Testcontainers) aplicam a V29 normalmente. Não altera regra de negócio; não inventar estratégia de migração sem decisão explícita.


# 83. Fase 15 — Escopo

Implementar:

- criação de meta vinculada a conta (`BANK_ACCOUNT` / `CASH`);
- edição cadastral (`ACTIVE`);
- contribuição (`goal_contributions`);
- resgate (`goal_redemptions`);
- conclusão explícita (`COMPLETED`); resgate permitido também em `COMPLETED`;
- cancelamento (`CANCELLED`, somente com reservado zero);
- acompanhamento (`currentAmount`, `progressPercent` derivados);
- extensão de saldo da conta (`totalBalance`, `reservedAmount`, `availableBalance`).

Fora do escopo: frontend, dashboard, projeções, reverse de contribuição/resgate, transferência entre metas, pagamento de despesa pela meta, ledger genérico.


# 84. Fase 15 — Regras fechadas

- meta ≠ despesa; meta ≠ conta independente;
- `totalBalance` (RN240) inalterado por contribuição/resgate;
- `availableBalance = totalBalance − reservedAmount`;
- `targetAmount` não é teto; progresso pode exceder 100%;
- conclusão manual; não automática ao atingir 100%;
- resgate permitido em `ACTIVE` e `COMPLETED`; resgate **não** altera status;
- cancelamento exige `currentAmount = 0` (somente `ACTIVE`); **não** permitir `COMPLETED` → `CANCELLED`;
- contribuição/resgate imutáveis nesta fase (sem reverse);
- RN010A inclui contribuição/resgate;
- inativação de conta bloqueada se `reservedAmount > 0` (RN274);
- schema: `account_id` em `financial_goals`; tabela `goal_redemptions`; `account_id` removido de `goal_contributions` (migration **V29**).

Detalhe: `docs/24` §19.6 (RN264–RN280).


# 85. Testes

Ver `docs/27-testes.md` §40D. Classes: `FinancialGoalApiTest`, `FinancialGoalConcurrencyTest`, `GoalProgressTest`, `GoalReservationFoundationTest`.


# 85A. Critério de conclusão — Fase 15

Usuário consegue: criar meta vinculada a conta; contribuir e resgatar (inclusive em `COMPLETED`); concluir e cancelar conforme RN264–RN280; consultar `totalBalance`, `reservedAmount` e `availableBalance`; operações existentes respeitam saldo disponível.

**Critério atendido. Fase 15 — CONCLUÍDA E APROVADA.**


# 86. Fase 16 — Contas a pagar

**Status:** `CONCLUÍDA E APROVADA`.

Objetivo:

Visão consolidada de leitura das obrigações de saída já existentes.

Contrato oficial: `docs/24` §19.7 / `docs/25` §66 / `docs/27` §40E.

Endpoint implementado: `GET /api/v1/payables`.

**Não** criar tabela `payables`, migration, remaining persistido, status novo nem escritas em `/payables`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes; não reabrem a fase).


# 87. Fase 16 — Escopo

Implementado:

- `GET /api/v1/payables`;
- linha = parcela ACCOUNT/NONE com remaining > 0 **ou** fatura `SCHEDULED`/`OPEN`/`CLOSED` com remaining > 0;
- pacote `payables` (Controller → Service → consultas; sem entidade JPA).

Fora do escopo: frontend, dashboard, projeções, `GET /payables/{id}`, POST/PUT/DELETE, transferências, acertos, saldo inicial, metas.


# 88. Fase 16 — Regras fechadas

- visão derivada; remaining/overdue/totais derivados;
- período = `due_date` da linha (**não** RN226);
- "mês atual" = mês **selecionado** no filtro (`year`/`month`), não o mês do relógio (outubro/2026 consultável em agosto/2026);
- cartão só como fatura (sem dupla contagem);
- remaining 0 nunca entra;
- `OVERDUE` derivado; filtro `overdue` separado de `status`;
- `status` múltiplo; `withoutCreditCard`; categoria/responsável só em ACCOUNT/NONE;
- envelope com `totalRemaining`, `totalOriginal`, `totalPaid` do universo filtrado;
- ordenação `sort`+`direction` + desempate `id`;
- paginação `page`/`size` (default 20, máx. 100);
- isolamento JWT.

Detalhe: `docs/24` §19.7 (RN281–RN292).


# 89. Critério

Usuário deve conseguir responder:

"Quanto tenho para pagar?"

e:

"Quanto tenho para pagar neste período?"

sem duplicar cartão, sem usar total no lugar de remaining, sem quitadas/metas/transferências, sem violar isolamento.

**Critério atendido. Fase 16 — CONCLUÍDA E APROVADA.**

Testes: `docs/27` §40E (`PayablesApiTest`, 14 testes). Suíte no fechamento: 425 testes; `mvn test` / `mvn verify` / Spotless com sucesso.


# 90. Fase 17 — Contas a receber

**Status da Parte 1:** **CONCLUÍDA E APROVADA** (`docs/24` §19.8 / `docs/25` §67 / `docs/27` §40F). Auditoria: **APROVADA COM RESSALVAS** (não bloqueantes).

**Fase 17 — CONCLUÍDA E APROVADA** (Parte 1 + Parte 2). Decisões D73–D94 **fechadas** e **implementadas**; migration V30; 481 testes verdes.

Objetivo da Parte 1:

Visão consolidada de leitura das duplicatas de receita já existentes (`Income`).


# 91. Fase 17 — Parte 1

Exibir, via `GET /api/v1/receivables` (pacote `receivables`):

- receitas `EXPECTED` futuras;
- receitas `EXPECTED` vencidas (`overdue` derivado);
- receitas `RECEIVED` quando solicitadas.

**Não** utilizar `/api/v1/accounts-receivable`. **Não** criar alias `dueDate`. `expectedDate` permanece obrigatória. `CANCELLED` fica fora. Consulta padrão: somente `EXPECTED`. Resumo no mesmo GET, respeitando filtros. Filtros/ordenação/paginação no banco. `size` máximo 100. Sem `year` / `month` / `search`. Sem escrita em `/receivables`.

Fora da Parte 1: frontend, dashboard, PDF, baixas/movimentações (Parte 2 — **implementada**). A escrita de responsável em Income foi absorvida pelo contrato da Parte 2 (RN306 / `docs/24` §19.9) e **está implementada**.


# 92. Critério da Parte 1

Usuário deve conseguir responder:

"Quanto tenho para receber?"

incluindo futuras, vencidas, total a receber, recebidas quando solicitadas, filtros, ordenação, paginação, ownership, dados agregados, testes e documentação.

**Critério atendido. Fase 17 — Parte 1 — CONCLUÍDA E APROVADA.**

Testes: `docs/27` §40F (`ReceivablesApiTest`, 35/35). Regressão no fechamento: `IncomeApiTest` 15/15; `PayablesApiTest` 14/14; suíte `mvn verify` 460/460.

A Parte 2 **não** reduz `amount` / `receivedAmount` / `remainingAmount` na duplicata como fonte de verdade. Contrato: `docs/24` §19.9. **`CONCLUÍDA E APROVADA`**.


# 92A. Fase 17 — Parte 2

**Status:** **`CONCLUÍDA E APROVADA`**. Decisões D73–D94 **fechadas** e **implementadas**. Reauditoria: **APROVADA COM RESSALVAS** (documentação consolidada nesta etapa).

Objetivo: acréscimos, recebimentos parciais, histórico, estorno por movimentação, escrita de responsável em Income, evolução da visão `GET /receivables`. **Implementado.**

Não criar tabela `receivables`. Migration V30 (`income_movements`) **executada**.

A Fase 18 — Projeções (`docs/28` §93) está **CONCLUÍDA E APROVADA**.

Fora da Parte 2: frontend, dashboard, PDF, Excel, projeções, over-receipt, patrimônio por responsável.


# 92B. Critério da Parte 2 (implementado)

Usuário consegue:

- lançar acréscimos sem mover conta;
- receber parcialmente e integralmente (remaining = 0 fecha `RECEIVED`);
- reabrir `RECEIVED` com novo acréscimo;
- estornar movimentação individual;
- consultar histórico;
- informar responsável no cadastro;
- ver a visão de contas a receber coerente com remaining / recebimentos válidos.

**Critério atendido. Fase 17 — Parte 2 — CONCLUÍDA E APROVADA.**

Testes: `docs/27` §40G. Suíte: **481** testes; 0 falhas; `mvn verify` BUILD SUCCESS.

**Fase 17 inteira — CONCLUÍDA E APROVADA.**


# 93. Fase 18 — Projeções

**Status:** **CONCLUÍDA E APROVADA**. D95–D204 **fechadas** e **implementadas**. Endpoint `GET /api/v1/projections`. Sem tabela `projections`. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes; não reabrem a fase). Collection Postman atualizada (`GET /api/v1/projections`).

Objetivo:

Visão derivada de fluxo de caixa: saldo projetado a partir do saldo real atual, com composição do fluxo. Contrato: `docs/24` §19.10 / `docs/25` §68 / `docs/27` §40H.

Não persistir. Sem tabela `projections`. Sem frontend. Sem recorrência. Sem cenários.


# 94. Fase 18 — Períodos

Permitir consultar, no mesmo `GET /api/v1/projections`:

- mês calendário;
- trimestre calendário (meses + total);
- vários meses (máximo 12; default 12).

Granularidade V1: mensal. Sem projeção diária. Sem `GET /projections/monthly`.


# 95. Fase 18 — Composição

Considerar:

- saldo atual (não reconstruir o mês desde o dia 1);
- receitas futuras `EXPECTED` (remaining; vencidas entram imediatamente);
- despesas/parcelas ACCOUNT/NONE (remaining);
- faturas `SCHEDULED`/`OPEN`/`CLOSED` com remaining > 0;
- obrigações futuras de Agreement via domínio existente;
- transferências somente no saldo atual / efeito consolidado nulo;
- metas: `reservedAmount` e `availableProjectedBalance` (não são caixa).


# 96. Fase 18 — Exclusões

Não considerar:

- limite de cartão como dinheiro;
- compra no cartão como saída de conta (a fatura é a saída);
- despesas `CANCELLED` / `REFUNDED` como compromisso futuro;
- receitas `CANCELLED` ou já recebidas;
- fatos já incorporados ao saldo atual;
- filtros de categoria, responsável ou cartão.


# 97. Critério

Usuário deve conseguir responder:

"Quanto dinheiro provavelmente terei em dezembro?"

("Provavelmente" = fatos conhecidos e ainda relevantes — não probabilidade estatística.)

**Critério atendido.** Especificação D95–D204 e implementação backend **CONCLUÍDA E APROVADA**. Auditoria final: **APROVADA COM RESSALVAS** (D203 estrutural; remaining via serviços oficiais; `UNASSIGNED`; débito técnico de rota inexistente). Testes: `docs/27` §40H (`ProjectionApiTest`, `ProjectionCalculatorTest`). Suíte: **509** testes; `mvn verify` BUILD SUCCESS.


# 98. Fase 19 — Dashboard

**Status:** **CONCLUÍDA E APROVADA**. D282–D289 **fechadas** e **implementadas**. Endpoint `GET /api/v1/dashboard`. Sem tabela `dashboard`. Sem frontend. Sem `GET /dashboard/monthly`. Sem `GET /projections/monthly`. Collection Postman atualizada. Testes: `DashboardApiTest`. Suíte: **519** testes; `mvn verify` BUILD SUCCESS. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes; não reabrem a fase). Sem correções obrigatórias.

Objetivo:

Visão geral financeira de leitura. Reutiliza a projeção da Fase 18. Contrato: `docs/24` §19.11 / `docs/25` §71 / `docs/27` §40I.


# 99. Dashboard

Exibir (backend):

- saldo total / reservado / disponível;
- receitas previstas (receivables);
- obrigações em aberto (payables: parcela vs fatura);
- faturas (remaining);
- contas a receber;
- projeção (summary + months + quarters; eventos em `GET /projections`).


# 100. Dashboard

Gráficos por categoria/cartão/responsável e `GET /api/v1/reports/*` **fora** desta fase (Fase 20 / frontend). O fluxo de caixa mensal do Dashboard usa `projection.months`.


# 101. Fase 20 — Relatórios

**Status:** **CONCLUÍDA E APROVADA**. D-F20-01 a D-F20-16 **fechadas** e **implementadas**. 7 endpoints JSON + 7 PDF. OpenPDF 3.0.5. Collection Postman atualizada (14 requests em `02 - Processos/Reports`). Sem tabela `reports`. Flyway permanece em V30. Testes: `ReportsApiTest` (59 métodos). Suíte: **578** testes; `mvn verify` BUILD SUCCESS. Auditoria final: **APROVADA COM RESSALVAS** (ressalva exclusivamente documental/status, corrigida na etapa de fechamento; não bloqueante; não reabre a fase). Sem correções funcionais obrigatórias.

Contrato: `docs/24` §19.12 / `docs/25` §76 / `docs/27` §40J.

Objetivo (atendido): relatórios derivados (despesas, receitas, categorias, responsáveis, cartões, fluxo de caixa, fatura) e exportação PDF (OpenPDF). Sem tabela `reports`. Sem reabrir Dashboard nem `GET /projections`.


# 102. Relatórios

Endpoints oficiais: `GET /api/v1/reports/expenses|incomes|categories|responsibles|cards|cash-flow` e `GET /api/v1/reports/invoices/{invoiceId}`, com variantes `/pdf`.

**SUPERADOS:** `expenses-by-category`, `income-by-category`, `expenses-by-responsible`, `expenses-by-card`.


# 103. Fase 20

Exportação inicial: PDF (OpenPDF). CSV/Excel fora. Frontend/gráficos fora.


# 104. Fase 20

Relatório de fatura adequado para enviar ao titular/responsável do cartão (filtro por responsável; PDF sem UUID/`creditLimit`/notas internas).


# 105. Fase 21 — Frontend completo

Objetivo:

Integrar todas as funcionalidades ao Angular.

Status: **CONCLUÍDA / APROVADA COM RESSALVAS**.

Bloco B1 — Design System base — **implementado** (Inter local, tokens visuais, reset/base, foco acessível, `prefers-reduced-motion`, responsividade estrutural). Sem componentes de layout ou features neste bloco.

Bloco B2 — Core HTTP, configuração e contrato de erros — **implementado** (URL da API por ambiente, HttpClient, interceptor de erros, `ApiError`, normalização segura). Sem AuthService, JWT storage, login ou features.

Bloco B3 — Autenticação, sessão e inicialização — **implementado** (AuthService, sessionStorage, interceptor Authorization, AuthGuard, login/registro mínimos). Sem layout, dashboard ou features financeiras.

Bloco B4 — App Shell, layout responsivo e navegação — **implementado** (MainLayout, Header, Sidebar colapsável, Drawer mobile, `/dashboard` como entrada autenticada). Dashboard funcional e features financeiras fora deste bloco.

Bloco B5 — Dashboard funcional — **implementado** (`GET /api/v1/dashboard`, estados loading/loaded/error, projeção mensal oficial, sem ECharts e sem CRUDs).

Bloco B6 — Shared UI foundation — **implementado** (EmptyState, ErrorState, pipes de BRL/DATE/mês). Sem Button, sem tabela/form/modal genéricos, sem features financeiras.

Bloco B7 — Contas (Accounts) — **implementado** (`/accounts` lazy-loaded, saldos oficiais via `GET /accounts/{id}/balance`, CRUD cadastral conforme contrato, sem recálculo de saldo).

Bloco B8 — Categorias (Categories) — **implementado** (`/categories` lazy-loaded, filtros oficiais `type`/`active`, CRUD cadastral conforme contrato, sem reativação).

Bloco B9 — Despesas (Expenses) — **implementado** (`/expenses` lazy-loaded, paginação e filtros oficiais, CRUD + pagamento/cancelamento/estorno conforme contrato, sem cartão na criação).

Bloco B10 — Receitas (Incomes) — **implementado** (`/incomes` lazy-loaded, paginação e filtros oficiais de `GET /incomes`, CRUD + recebimento/cancelamento/estorno de movimento conforme contrato). Sem Contas a Receber.

Bloco B11 — Contas a pagar (Payables) — **implementado** (`/payables` lazy-loaded, visão derivada `GET /api/v1/payables`, filtros e paginação oficiais, sem CRUD e sem `GET /payables/{id}`). Sem B12.

Bloco C1 — Cartões (Credit Cards) — **implementado** (`/credit-cards` lazy-loaded, cadastro + limites oficiais `GET /credit-cards/{id}/limit`, filtro `holderName`, ativar/desativar com confirmação). Sem faturas. Sem B12.

Bloco C2 — Integração Cartões ↔ Despesas — **implementado** (criação `CREDIT_CARD` com `creditCardId`; catálogo `GET /credit-cards` filtrado por `active` na UI; edição `CREDIT_CARD` continua bloqueada). Sem faturas. Sem B12.

Bloco C3 — Faturas — **implementado** (`/invoices` lazy-loaded, consulta por cartão `GET /credit-cards/{cardId}/invoices`, detalhe `GET /invoices/{id}`, parcelas `GET /invoices/{id}/items`). Sem pagamentos, ajustes, créditos ou acordos. Sem B12. Sem C4.

Bloco C4 — Pagamento de faturas — **implementado** (histórico `GET /invoices/{id}/payments`, pagamento parcial/integral `POST /invoices/{id}/payments`, reversão `POST .../payments/{paymentId}/reverse`). Sem ajustes, créditos, acordos, fechamento manual. Sem B12. Sem C5.

Bloco C5 — Ajustes de fatura — **implementado** (histórico `GET /invoices/{id}/adjustments`, criação `POST /invoices/{id}/adjustments`, reversão `POST .../adjustments/{adjustmentId}/reverse`). Sem créditos, acordos, fechamento manual. Sem B12. Sem C6. Sem C7.

Bloco C6 — Créditos de cartão — **implementado** (consulta `GET /credit-cards/{id}/credits`, criação manual `POST /credit-cards/{id}/credits`). Sem aplicação manual, reverse, edição ou exclusão. Sem B12. Sem C7.

Bloco C7 — Acordos / Renegociação — **CONCLUÍDA / APROVADA COM RESSALVAS** (histórico `GET /invoices/{id}/agreements`, criação `POST /invoices/{id}/agreements`, renegociação `POST /invoices/{id}/renegotiations`, detalhe `GET /agreements/{id}`, pagamento/antecipação `POST /agreements/{id}/installments/{installmentId}/anticipate`). Sem cancelamento, reverse, edição, exclusão ou rota `/agreements`. Sem B12. Ressalvas não bloqueantes: **AUD-C7-A1** (cobertura visual/integrada ainda não executada); **AUD-C7-A2** (refresh após mutations abrangente; eventual otimização futura); **AUD-C7-A3** (backend permanece autoridade financeira; o frontend não deve expandir regras financeiras locais).

Bloco B12 — Transferências + Metas + Projeções + Relatórios — **CONCLUÍDA / APROVADA COM RESSALVAS** (`/transfers`, `/goals`, `/projections`, `/reports`; contratos reais do backend; PDF oficial; sem recálculo financeiro no frontend). Transferências, metas, projeções e relatórios **concluídos**. Auditoria formal realizada (somente leitura). C1–C7 não reabertos. Ressalvas não bloqueantes: **AUD-B12-A1** (validação visual/browser não executada); **AUD-B12-A2** (avisos de jsdom no download de PDF em testes; contrato correto; experiência real no navegador não validada); **AUD-B12-A3** (validações financeiras permanecem no backend; o frontend não deve reproduzi-las).

Status dos blocos C (Fase 21 — Frontend):

```text
C1 — Cartões .................... CONCLUÍDO / APROVADO COM RESSALVAS
C2 — Despesas CC ................ CONCLUÍDO / APROVADO COM RESSALVAS
C3 — Faturas .................... CONCLUÍDO / APROVADO COM RESSALVAS
C4 — Pagamentos ................. CONCLUÍDO / APROVADO COM RESSALVAS
C5 — Ajustes .................... CONCLUÍDO / APROVADO COM RESSALVAS
C6 — Créditos ................... CONCLUÍDO / APROVADO COM RESSALVAS
C7 — Acordos / Renegociação ..... CONCLUÍDO / APROVADO COM RESSALVAS
B12 ............................. CONCLUÍDA / APROVADA COM RESSALVAS
```

C7 permanece **CONCLUÍDA / APROVADA COM RESSALVAS**. B12 está **CONCLUÍDA / APROVADA COM RESSALVAS** (auditoria formal realizada; ressalvas **AUD-B12-A1**, **AUD-B12-A2** e **AUD-B12-A3** não bloqueantes e não corrigidas nesta consolidação).


# 106. Frontend

Criar telas:

- login;
- dashboard;
- contas;
- cartões;
- categorias;
- receitas;
- despesas;
- faturas;
- transferências;
- metas;
- contas a pagar;
- contas a receber;
- projeções;
- relatórios.


# 107. Frontend

Priorizar experiência de uso.


# 108. Frontend

Formulários devem possuir:

- validação;
- mensagens claras;
- estados de carregamento;
- tratamento de erro.


# 109. Frontend

Não duplicar regras financeiras críticas.


# 110. Fase 22 — Testes E2E / integração e consolidação

**Status:** CONCLUÍDA / APROVADA COM RESSALVAS.

Objetivo:

Validar os principais fluxos através da aplicação completa (browser → Angular → JWT → API → PostgreSQL). Não é fase de novo domínio financeiro.

Implementado e auditado: Playwright (Chromium) em `frontend/e2e/`; autenticação real; dados E2E controlados; isolamento; fluxo core; transferência; cartão/fatura/crédito/pagamento; acordo; metas; dashboard/projeções; relatórios JSON + PDF; validação visual desktop/mobile.

Backend e migrations **não** foram alterados. Fora desta fase: C8, CI/CD, Cypress, alteração de regras financeiras, redesign. C8 permanece **não iniciada**.

Higiene: `docs/01`–`docs/19` já não estavam no disco. A pasta temporária `.tmp-phase14-extract/` (cópias da Fase 14) foi removida; a documentação oficial permanece em `docs/`.

Auditoria formal realizada (somente leitura). Ressalvas não bloqueantes (**não** corrigidas nesta consolidação): **AUD-F22-A1** (INFORMATIVO — asserção simples receita − despesa no fluxo core; a UI continua usando o valor oficial da API); **AUD-F22-A2** (BAIXO — E2E de relatórios JSON não percorre todos os tipos; contratos internos cobertos pelos testes da aplicação); **AUD-F22-A3** (INFORMATIVO — validação visual estrutural/funcional, sem visual regression pixel-level); **AUD-F22-A4** (INFORMATIVO — helper de fechamento de fatura é fixture de teste do scheduler; não existe endpoint manual de close).


# 111. Fluxos prioritários (cobertos na F22)

1. Login / registro / sessão / logout.
2. Cadastro de conta.
3. Cadastro de categoria.
4. Receita e recebimento.
5. Despesa e pagamento.
6. Compra no cartão.
7. Fatura, ajuste, crédito e pagamento.
8. Acordo / antecipação de parcela.
9. Transferência e reversão.
10. Metas, dashboard, projeções, relatórios e PDF.


# 112. Fase 23 — Qualidade

**Estado:** **DECISÕES APROVADAS / CONSOLIDAÇÃO DOCUMENTAL**.

A Fase 23 é a etapa de qualidade, consistência e endurecimento técnico posterior à conclusão do frontend da Fase 21 e à integração/E2E da Fase 22. Ela abrange transversalmente os blocos C1–C7 e B12 já encerrados, sem reabri-los funcionalmente. As ressalvas históricas de B12 e F22 entram como insumo para classificação durante a auditoria, não como correções automáticas.

A F23 não é fase funcional. C8 permanece futura, não iniciada e fora do escopo; a F23 apenas prepara uma base técnica mais estável para seu desenvolvimento posterior.

## 112.1 Objetivo e escopo

Dentro do escopo:

- auditoria estrutural e arquitetural;
- testes unitários, de integração e E2E;
- qualidade de código, contratos, parsers, formatters e formulários;
- autenticação, sessão e segurança técnica básica;
- dependências, scripts e documentação;
- responsividade, acessibilidade e performance básicas;
- correção mínima de problemas reais comprovados.

Fora do escopo:

- C8, novos módulos, funcionalidades, endpoints ou regras de negócio;
- novas modalidades de relatório e modo simplificado/detalhado sem contrato backend;
- redesign, reescrita arquitetural ou troca de Angular, Playwright ou banco;
- migrations, alteração de banco, CI/CD completo e deploy;
- refactors amplos, upgrades automáticos e otimizações prematuras.

## 112.2 Princípios

- O backend permanece autoridade sobre regras e valores financeiros; o frontend apresenta resultados oficiais e faz apenas validações de UX e cálculos estritamente visuais.
- Corrigir a menor superfície necessária, sem reabrir fases anteriores por conveniência.
- Não criar abstrações genéricas sem repetição significativa, comportamento comum e benefício claro.
- E2E valida comportamento observável na cadeia navegador → API → banco → resultado, sem acoplamento a detalhes internos do Angular.
- Testes financeiros do frontend devem comparar com contratos e respostas oficiais, não reproduzir fórmulas do backend.
- Qualidade é avaliada por risco e resultado; não por meta artificial de quantidade de testes ou cobertura.
- Decisão, execução e resultado de auditoria são registros distintos. Este contrato não antecipa resultados da F23.

## 112.3 Decisões aprovadas

### D-F23-01 — Objetivo formal da F23

A F23 é uma fase de qualidade, consistência e endurecimento técnico, não uma fase funcional. Inclui auditoria estrutural, testes, E2E, qualidade de código, dependências, documentação, scripts, segurança técnica básica e correção de problemas reais encontrados. Não inclui funcionalidades financeiras, endpoints, regras financeiras, telas de negócio ou módulos funcionais novos.

### D-F23-02 — Backend como autoridade

O frontend não deve reproduzir regras financeiras do backend, incluindo saldos, limites, parcelas, juros, descontos, créditos, acordos, progresso de metas, projeções e resultados de relatórios. Cálculos estritamente de apresentação permanecem permitidos.

### D-F23-03 — Auditoria arquitetural do frontend

Auditar a organização `core` / `shared` / `layout` / `features` e a cadeia `Page → FeatureService → HttpClient → interceptors → API`. Procurar dependências invertidas, HTTP direto em componentes, imports inadequados, services ou modelos duplicados, parsers inconsistentes, formatters desnecessários, abstrações genéricas sem valor e dependências indevidas entre features.

### D-F23-04 — Abstrações genéricas

Não extrair abstração apenas para reduzir duplicação. A extração exige repetição significativa, comportamento realmente comum, benefício claro de manutenção e preservação da clareza. Não criar artificialmente `BaseCrudService`, `GenericTable`, `GenericForm`, `GenericApiService` ou `GenericReportComponent`.

### D-F23-05 — Auditoria dos services

Auditar todos os FeatureServices quanto a endpoints, métodos HTTP, payloads, query params, headers, erros, tipos, parsing, retornos e ausência de lógica financeira, com atenção especial a C1–C7, B12 e helpers da F22 que interagem com a API.

### D-F23-06 — Parsers

Verificar rejeição de payload inválido, obrigatoriedade, enums, preservação de `null`, casts e correspondência entre DTO real e modelo frontend. Parser não pode corrigir silenciosamente resposta inválida.

### D-F23-07 — Formatters

Formatters são exclusivamente de apresentação. Podem formatar moeda, datas, labels, percentuais, enums e textos; não podem alterar valores, calcular regras financeiras, inferir estados de negócio ou substituir valores oficiais da API.

### D-F23-08 — Formulários

Auditar validações básicas de UX, obrigatoriedade, whitespace quando aplicável, valores obviamente inválidos e mensagens coerentes. O frontend não precisa reproduzir integralmente Bean Validation; o backend continua sendo autoridade.

### D-F23-09 — Tratamento de erros

Auditar `400`, `401`, `403`, `404`, `409`, `422` caso exista, `500` e indisponibilidade da API. Verificar mensagens, preservação do estado da tela, retry, ausência de loops, ausência de dados fictícios e ausência de stack trace exposto.

### D-F23-10 — Autenticação e sessão

Auditar login, logout, expiração JWT, interceptor, guard, armazenamento do token, tratamento de `401`, redirecionamento e limpeza de sessão. O mecanismo não será alterado sem necessidade comprovada.

### D-F23-11 — Segurança básica

Auditar armazenamento JWT, XSS, `innerHTML`, interpolação, URLs externas, downloads, secrets, `.env`, logs e credenciais hardcoded. A F23 não é um pentest completo.

### D-F23-12 — Dependências

Auditar `package.json`, `package-lock.json`, dependências diretas, transitivas relevantes, Angular, Playwright e ferramentas de teste. Identificar itens não utilizados, duplicados, desnecessários e vulnerabilidades conhecidas. Não atualizar automaticamente; upgrades relevantes exigem decisão própria.

### D-F23-13 — Playwright

Playwright permanece a ferramenta E2E oficial, com Chromium obrigatório nesta etapa. Não adicionar Cypress, Selenium ou ferramenta paralela.

### D-F23-14 — Suíte E2E

Auditar independência, dados únicos, isolamento, cleanup, estabilidade, seletores, waits, ausência de sleeps arbitrários, ausência de dependência de ordem, API real e autenticação real.

### D-F23-15 — Cobertura E2E

Não perseguir quantidade artificial. A cobertura deve priorizar riscos críticos, nesta ordem: autenticação, isolamento, saldo, pagamentos, transferências, cartões, faturas, acordos, metas, projeções e relatórios.

### D-F23-16 — E2E comportamental

E2E valida comportamento observado pelo usuário na cadeia navegador → API → banco → resultado. Não depende de classes internas, métodos privados, detalhes Angular ou estrutura interna dos services.

### D-F23-17 — Suíte unitária

Auditar services, parsers, formatters, pages, guards, interceptors e componentes críticos. Identificar testes frágeis ou redundantes sem reescrever centenas de testes apenas por estilo.

### D-F23-18 — Autoridade do backend nos testes

Ao verificar valores financeiros oficiais, como `balance`, `remainingAmount`, limite, `currentAmount`, `progressPercent`, projeções e totais de relatórios, preferir comparação com a resposta da API. Não reproduzir fórmulas financeiras no frontend apenas para testá-lo.

### D-F23-19 — Contratos frontend

Criar ou fortalecer testes de contrato que garantam payloads exatos, sem campos adicionais indevidos. Dar atenção especial a créditos, ajustes, acordos, renegociações, relatórios, transferências e metas.

### D-F23-20 — Rotas

Auditar rotas públicas e protegidas, lazy loading, redirects, páginas inexistentes, navegação, refresh direto e coerência das features.

### D-F23-21 — Navegação

Auditar menu, ícones, labels, ordem, rotas, estado ativo, desktop e mobile. Corrigir somente inconsistências reais; pendências históricas podem ser corrigidas sem reabrir funcionalmente fases encerradas.

### D-F23-22 — Responsividade

Auditar desktop, tablet quando relevante e mobile, priorizando overflow, tabelas, formulários, navegação, painéis, PDFs/downloads, erros e botões inacessíveis. Não redesenhar o sistema.

### D-F23-23 — Acessibilidade

Realizar verificação básica de labels, inputs, botões, foco, teclado, Escape, contraste evidente, nome acessível e navegação básica por teclado. Não buscar certificação WCAG completa.

### D-F23-24 — Estados de UI

Auditar `loading`, `success`, `empty`, `error`, `retry`, `submitting` e `disabled`, especialmente após mutations. Nenhuma tela deve permanecer visualmente inconsistente após uma operação.

### D-F23-25 — Concorrência e duplo submit

Auditar duplo clique, múltiplos submits, requests e confirmações repetidas. Quando aplicável, marcar `submitting`, bloquear nova operação, executar o request e restaurar o estado em `finally`.

### D-F23-26 — Refresh após mutation

Após alteração, a UI deve refletir valores oficiais do backend. O refresh deve ser necessário, explícito e coerente, nunca indiscriminado.

### D-F23-27 — Performance

Auditar bundle, lazy loading, HTTP duplicado, loops, requests desnecessários, carregamento inicial, tabelas grandes, relatórios e PDF. Não realizar otimização prematura.

### D-F23-28 — Relatórios

Auditar `/reports`: parâmetros, paginação, ordenação, filtros, datas, enums, PDF, download, erros, loading, empty e mobile. Não criar tipos novos nem modo simplificado/detalhado sem contrato backend.

### D-F23-29 — Projeções

Auditar `/projections`: combinações de parâmetros, período, paginação, eventos, `undatedEvents`, `negative` e `overdue`. O frontend pode validar combinações simples já declaradas inválidas pelo contrato; o backend continua autoridade.

### D-F23-30 — Metas

Auditar `/goals`: paginação, status, contribuições, resgates, conclusão, cancelamento, valores oficiais e progresso. Não alterar regras financeiras das metas.

### D-F23-31 — Cartões e faturas

Realizar auditoria transversal de C1–C7 para integração, navegação, contratos, E2E e regressões. Não reabrir os blocos; corrigir apenas regressões ou bugs atuais claramente comprovados.

### D-F23-32 — Pendências históricas

Classificar cada pendência, com justificativa, como `CORRIGIR NA F23`, `ADIAR`, `INFORMATIVA` ou `ENCERRADA POR E2E`. A existência de uma pendência não autoriza correção automática.

### D-F23-33 — Documentação

Auditar `README.md`, `frontend/README.md`, `docs/21`, `docs/27`, `docs/28`, documentação de testes e roadmap para que descrevam o estado real do repositório sem se tornarem cópia do código.

### D-F23-34 — Scripts

Auditar `start.ps1`, `run-backend.ps1`, `run-e2e.ps1` e Docker Compose quanto a funcionamento, mensagens, pré-requisitos, caminhos, duplicações e compatibilidade Windows.

### D-F23-35 — Limpeza técnica

Remover somente temporários, código morto comprovado, imports inutilizados, artefatos esquecidos, scripts obsoletos e dependências comprovadamente não utilizadas. Não remover com base apenas em aparência.

### D-F23-36 — Backend

O backend só poderá ser alterado diante de bug real ou inconsistência que comprometa a qualidade. Não criar endpoints nem regras.

### D-F23-37 — Banco

Não criar migrations. O banco será usado somente para testes, validação, E2E e diagnóstico.

### D-F23-38 — CI/CD

Não implementar CI/CD completo, GitHub Actions, pipelines de deploy, ambientes remotos ou publicação automática. Estrutura futura pode ser preparada sem iniciar CI/CD.

### D-F23-39 — Métricas

Não estabelecer meta artificial, como 100% de coverage. Indicadores são testes, builds e E2E passando; ausência de regressões; contratos corretos; arquitetura consistente; ausência de bugs relevantes conhecidos; e documentação alinhada.

### D-F23-40 — Critério de aprovação

A F23 somente poderá ser aprovada quando a auditoria estiver concluída, problemas críticos corrigidos, unitários/E2E/builds passando, contratos preservados, backend íntegro, documentação consolidada e nenhuma regressão relevante conhecida. Ressalvas menores somente podem permanecer se registradas, classificadas, justificadas e não bloqueantes.

### D-F23-41 — Metodologia

Fluxo oficial:

```text
decisões
   ↓
consolidação documental
   ↓
auditoria inicial / mapa técnico
   ↓
implementação das correções
   ↓
testes
   ↓
auditoria final
   ↓
consolidação documental
   ↓
aprovação
```

Não implementar antes da consolidação documental.

### D-F23-42 — Fora do escopo

Permanecem fora: C8, módulos e regras financeiras novos, modalidades novas de relatório, modo simplificado/detalhado, CI/CD completo, deploy, redesign, migração de framework, troca de Angular, Playwright ou banco e reescrita arquitetural.

### D-F23-43 — C8

C8 permanece fora e não deve ser iniciada incidentalmente. A F23 apenas prepara a base técnica para seu desenvolvimento futuro.

### D-F23-44 — Correção mínima

Corrigir a menor superfície necessária. Evitar refactoring amplo, alteração de API, renomeações em massa, migrations e reorganizações desnecessárias.

### D-F23-45 — Regra contra regressão

Toda correção relevante deve possuir teste adequado à sua camada: bug de service → teste de service; parser → parser; page → page; E2E → E2E. Teste manual isolado não é suficiente.

### D-F23-46 — Auditoria final independente

Após a implementação haverá auditoria final independente de diff, código, testes, E2E, builds, documentação, dependências, backend, migrations e escopo. Implementação não equivale a aprovação.

### D-F23-47 — Consolidação final

Após a auditoria final, atualizar `docs/21`, `docs/27`, `docs/28`, `README.md` e `frontend/README.md` quando aplicável, registrando resultado, ressalvas, justificativas e próximo passo do roadmap.

### D-F23-48 — Encerramento

Estados finais oficiais: `APROVADA`, `APROVADA COM RESSALVAS` ou `REPROVADA / NECESSITA CORREÇÃO`. Não utilizar estados intermediários informais como “quase”, “parcialmente” ou “praticamente concluída”.

## 112.4 Registro de execução e auditoria

O registro de execução/auditoria deverá conter, em etapa posterior:

- mapa técnico e itens auditados;
- problemas encontrados e classificação das pendências;
- correções e testes associados;
- resultados de unitários, E2E, builds e verificações;
- auditoria final independente, ressalvas e decisão de aprovação.

Nenhum desses resultados foi obtido nesta consolidação. A F23 não está implementada, auditada nem aprovada.


# 113. Critérios de aprovação da F23

Aplicam-se integralmente D-F23-39, D-F23-40, D-F23-46 e D-F23-48. A aprovação formal ocorrerá somente após execução, testes, auditoria final independente e consolidação final.

O estado após esta etapa documental é:

```text
F23 — DECISÕES APROVADAS / CONSOLIDAÇÃO DOCUMENTAL
```

O próximo estado previsto é:

```text
F23 — AUDITORIA INICIAL / MAPA TÉCNICO
```


# 114. Relação com o roadmap

- Fases 21 e 22 permanecem encerradas com suas ressalvas históricas.
- C1–C7 e B12 serão auditados transversalmente, sem reabertura funcional.
- As ressalvas de B12 e F22 serão classificadas conforme D-F23-32.
- C8 permanece posterior, fora da F23 e não iniciada.
- F24 e etapas futuras não são iniciadas pela consolidação da F23.


# 115. Fase 24 — Documentação

Atualizar:

README.md

AGENTS.md

docs/


# 116. Fase 25 — Preparação futura

Somente após V1 estável considerar:

- investimentos;
- importação bancária;
- notificações;
- múltiplos formatos de exportação;
- dark mode;
- PWA;
- deploy;
- CI/CD;
- auditoria avançada.


# 117. Regra

Funcionalidades futuras não devem entrar na V1 apenas porque são tecnicamente interessantes.


# 118. Prioridade

A prioridade é:

estabilidade;

correção;

segurança;

aprendizado;

experiência de uso.


# 119. IA

A IA deve trabalhar somente na fase autorizada.


# 120. IA

Não implementar automaticamente a próxima fase ao terminar a atual.


# 121. IA

Ao terminar uma fase:

1. executar testes;
2. verificar build;
3. verificar documentação;
4. apresentar resumo;
5. listar arquivos alterados;
6. listar decisões;
7. aguardar autorização.


# 122. Git

Cada fase relevante deve resultar em commits organizados.


# 123. Commits

Preferir commits pequenos e semânticos.


# 124. Exemplos

feat: add account management

feat: add expense installments

feat: add credit card invoices

test: add invoice payment tests

fix: correct installment rounding


# 125. Git

Não fazer commits gigantes com dezenas de funcionalidades sem relação.


# 126. Cursor

O desenvolvimento será realizado no Cursor.


# 127. GitHub

Os commits serão realizados pelo VSCode.


# 128. GitHub

O Cursor não precisa possuir acesso à conta pessoal do GitHub.


# 129. Fluxo

Cursor:

desenvolvimento.


VSCode:

revisão;

commit;

push.


# 130. Regra

Antes do commit:

1. revisar diff;
2. executar testes;
3. verificar arquivos sensíveis;
4. verificar documentação;
5. confirmar escopo.


# 131. Consumo de IA

Evitar prompts gigantes durante implementação.


# 132. Estratégia

Solicitar:

uma funcionalidade;

um domínio;

ou uma etapa pequena.


# 133. Exemplo

Bom:

"Implemente o CRUD de contas seguindo AGENTS.md, docs de regras e testes."


Ruim:

"Implemente todo o sistema financeiro."


# 134. IA

Antes de gerar código:

a IA deve analisar a documentação existente.


# 135. IA

Se encontrar inconsistência:

não inventar solução silenciosamente.


# 136. IA

Deve informar:

- problema;
- impacto;
- alternativas;
- recomendação.


# 137. Aprovação

Mudanças arquiteturais relevantes exigem aprovação.


# 138. Arquitetura

Não alterar stack sem aprovação explícita.


# 139. Tecnologias

Não adicionar bibliotecas importantes sem justificar.


# 140. Dependências

Toda nova dependência deve responder:

1. Por que precisamos?
2. Existe alternativa nativa?
3. Qual o custo?
4. Qual o impacto?
5. É mantida?


# 141. Frontend

Não adicionar biblioteca somente para resolver problema simples que Angular já resolve.


# 142. Backend

Não adicionar framework adicional sem necessidade.


# 143. Banco

Não adicionar banco secundário na V1.


# 144. Arquitetura

V1:

Angular

↓

Java

↓

PostgreSQL


# 145. Docker

Docker deve facilitar o ambiente, não esconder problemas arquiteturais.


# 146. Desenvolvimento

A aplicação deve continuar compreensível para o desenvolvedor humano.


# 147. IA

Código gerado por IA deve ser revisável.


# 148. IA

Não aceitar código que o desenvolvedor não consiga explicar.


# 149. Aprendizado

Sempre que uma tecnologia importante for introduzida:

a IA deve explicar brevemente:

- o que é;
- por que está sendo usada;
- como funciona no projeto.


# 150. Aprendizado

A documentação não deve ser excessivamente acadêmica.


# 151. Objetivo

O projeto também será utilizado como projeto de aprendizado.


# 152. V1

V1 deve ser suficientemente completa para uso pessoal real.


# 153. V1

V1 não precisa ser uma plataforma financeira empresarial.


# 154. Critério de sucesso

A V1 deve permitir:

1. cadastrar usuários;
2. cadastrar contas;
3. cadastrar cartões;
4. cadastrar categorias;
5. cadastrar receitas;
6. cadastrar despesas;
7. parcelar despesas;
8. editar parcelas;
9. controlar faturas;
10. pagar faturas;
11. pagar parcialmente;
12. parcelar saldo de fatura;
13. realizar transferências;
14. cadastrar metas;
15. consultar contas a pagar;
16. consultar contas a receber;
17. projetar meses futuros;
18. visualizar dashboard;
19. gerar relatórios;
20. exportar fatura em PDF.


# 155. Critério de sucesso

O sistema deve responder claramente:

Quanto tenho?

Quanto vou receber?

Quanto tenho para pagar?

Quanto devo nos cartões?

Quanto terei para pagar nos próximos meses?

Quanto posso gastar?


# 156. Regra final

A V1 deve priorizar controle financeiro confiável.


# 157. Regra final

É preferível uma V1 menor e correta do que uma V1 grande e instável.


# 158. Regra final

Não avançar de fase sem validar a anterior.


# 159. Regra final

Documentação, código e testes devem evoluir juntos.


# 160. Regra final

O roadmap pode ser alterado conforme o projeto evoluir.


# 161. Regra

Alterações significativas no roadmap devem ser documentadas.


# 162. Fim da V1

Após a Fase 24:

V1 CONCLUÍDA


# 163. Pós-V1

Somente após a V1 estar estável avaliar novas funcionalidades.


# 164. Próxima etapa

Fases 0 a 9: **CONCLUÍDAS E APROVADAS**. Fases 10–12: **ABSORVIDAS NA FASE 9**. Fase 13: **CONCLUÍDA E APROVADA**. Fase 14: **CONCLUÍDA E APROVADA**. Fase 15: **CONCLUÍDA E APROVADA**. Fase 16: **CONCLUÍDA E APROVADA**. Fase 17: **CONCLUÍDA E APROVADA**. Fase 18: **CONCLUÍDA E APROVADA**. Fase 19: **CONCLUÍDA E APROVADA**. Fase 20: **CONCLUÍDA E APROVADA**. Fase 21: **CONCLUÍDA / APROVADA COM RESSALVAS**. Fase 22: **CONCLUÍDA / APROVADA COM RESSALVAS**. Fase 23: **DECISÕES APROVADAS / CONSOLIDAÇÃO DOCUMENTAL**.

**Fase 20 — Relatórios:** **CONCLUÍDA E APROVADA** (`docs/28` §101 / `docs/24` §19.12 / `docs/25` §76; D-F20-01 a D-F20-16 **implementadas**). 7 JSON + 7 PDF. OpenPDF 3.0.5. Collection atualizada (14 requests). Flyway V30; sem tabela `reports`. Testes: `ReportsApiTest`. Suíte: **578** testes. Auditoria final: **APROVADA COM RESSALVAS** (ressalva exclusivamente documental/status, corrigida na etapa de fechamento; não bloqueante). **Fase 21 — Frontend completo:** **CONCLUÍDA / APROVADA COM RESSALVAS**. **Fase 22 — E2E / consolidação:** **CONCLUÍDA / APROVADA COM RESSALVAS** (Playwright/Chromium; `frontend/e2e/`; `scripts/run-e2e.ps1`; auditoria formal; ressalvas **AUD-F22-A1** a **AUD-F22-A4** não bloqueantes e não corrigidas nesta consolidação). **Fase 23 — Qualidade:** **DECISÕES APROVADAS / CONSOLIDAÇÃO DOCUMENTAL**; ainda não auditada, implementada ou aprovada. C8 permanece **não iniciada** e fora da F23. Próxima etapa do roadmap: **F23 — AUDITORIA INICIAL / MAPA TÉCNICO**. Bloco B1 (Design System base) **implementado**. Bloco B2 (HTTP / erros) **implementado**. Bloco B3 (autenticação) **implementado**. Bloco B4 (App Shell) **implementado**. Bloco B5 (Dashboard funcional) **implementado**. Bloco B6 (Shared UI) **implementado**. Bloco B7 (Contas) **implementado**. Bloco B8 (Categorias) **implementado**. Bloco B9 (Despesas) **implementado**. Bloco B10 (Receitas) **implementado**. Bloco B11 (Contas a pagar) **implementado**. Blocos C1–C7 **implementados** (`CONCLUÍDO / APROVADO COM RESSALVAS`). B12 **CONCLUÍDA / APROVADA COM RESSALVAS**.

**Fase 19 — CONCLUÍDA E APROVADA.** Endpoint: `GET /api/v1/dashboard`. Sem tabela `dashboard`. D282–D289 **implementadas**. Testes: `DashboardApiTest`. Suíte: **519** testes. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes).

**Fase 18 — CONCLUÍDA E APROVADA.** Endpoint: `GET /api/v1/projections`. Sem tabela `projections`. D95–D204 **implementadas**. Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes). Testes: `ProjectionApiTest`, `ProjectionCalculatorTest`.

**Fase 17 — CONCLUÍDA E APROVADA** (Parte 1 + Parte 2). Endpoint: `GET /api/v1/receivables`. Sem tabela `receivables`. Parte 2: `income_movements` (V30); movimentações; D73–D94 **implementadas**. Escrita de responsável (**D89**) **implementada**.

Status da Fase 16: `CONCLUÍDA E APROVADA` (`docs/24` §19.7 / `docs/25` §66). Auditoria final: **APROVADA COM RESSALVAS** (não bloqueantes).

Status da Fase 15: `CONCLUÍDA E APROVADA` (`docs/24` §19.6 / `docs/25` §54E).

Status da Fase 14: `CONCLUÍDA E APROVADA` (`docs/24` §19.5).

Status da Fase 13: `CONCLUÍDA E APROVADA` (`docs/24` §19.4 / RN254; `docs/23` §269.5 D1–D11 FECHADO).

A IA não deve implementar Refresh Token, logout backend, frontend financeiro, `payments.type`, extrato `/statement` nem auditoria genérica sem autorização. Itens ainda deferidos: §269.1, §269.2.7. Fechados: §269.3, §269.4, **§269.5**. A Fase 20 (relatórios/PDF) está **CONCLUÍDA E APROVADA**.
