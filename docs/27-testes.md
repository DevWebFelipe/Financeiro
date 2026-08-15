# Testes — Financial Control

## 0. Hierarquia e stack de testes

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

Backend: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL 18).

Frontend: framework oficial do Angular 22.x.

E2E: Playwright posteriormente.

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

Pendências oficiais (`AGENTS.md` §28.3 / `docs/23` §269): `payments.type`; edição de parcela **já em fatura** (269.2 deferido); rateio do pagamento parcial da fatura. A edição ACCOUNT/NONE e o reverse de payment estão no contrato da Fase 8 — testar só após a implementação autorizada.

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


# 34. Transferência

Testar:

conta A -> conta B.


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

Saldo insuficiente deve ser rejeitado.


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


A Fase 6 não testa responsável em receitas (`responsibleType` / `responsibleName`).


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

Contrato: `docs/24` seção 19.2 e `docs/25` seção 47. **Não implementar nem escrever estes testes até a autorização da Fase 8.** Não testar `CREDIT_CARD`, faturas, `payments.type` nem relatórios de apresentação.

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
- DISCOUNT + PAYMENT atômicos; SURCHARGE + PAYMENT atômicos; adjustment não movimenta conta;
- reverse de adjustment; segundo reverse rejeitado;
- refund misto: parcelas com payment ACTIVE → REFUNDED; sem payment → OPEN bloqueada (sem pay/adjust/edit/cancel);
- cancel só OPEN; PARTIALLY_PAID e PAID rejeitados;
- status agregado da despesa persistido;
- overdue da parcela: remaining > 0, OPEN/PARTIALLY_PAID, due_date < hoje, despesa não CANCELLED/REFUNDED (RN241);
- overdue da despesa N>1: true se pelo menos uma parcela estiver overdue segundo RN241 (não usar somente expenses.due_date);
- listagem startDate/endDate: despesa no intervalo se pelo menos uma parcela tiver due_date no intervalo;
- reverse de adjustment: ACTIVE → REVERSED; rejeitar se despesa CANCELLED/REFUNDED;
- UNIQUE (expense_id, installment_number);
- isolamento 404; concorrência na mesma parcela;
- `payments.type` permanece sem valores oficiais.


# 48. Testes de cartão

Testar:

criação;

edição;

desativação;

reativação;

limite;

compras;

faturas.


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


# 50.1 Recusa por limite

Limite: 5000

Comprometido: 4500

Disponível: 500

Compra: 600

Resultado esperado: compra recusada.


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


# 68. Testes de parcelamento de fatura

Testar:

saldo parcial;

parcelas iguais;

parcelas diferentes;

soma incorreta.


# 69. Exemplo

Saldo:

800


Parcelamento:

400

400


Resultado:

800


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

não aparece em contas a pagar;

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

Testar:

por categoria;

por cartão;

por responsável;

por conta;

fluxo de caixa.


# 113. PDF

Testar:

geração;

conteúdo;

isolamento por usuário.


# 114. Teste

Usuário A não pode gerar PDF da fatura do usuário B.


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

Testes end-to-end podem cobrir os principais fluxos.


# 161. Fluxos E2E prioritários

1. Login.
2. Criar conta.
3. Criar categoria.
4. Criar receita.
5. Criar despesa.
6. Criar compra parcelada.
7. Consultar fatura.
8. Pagar fatura.
9. Realizar transferência.
10. Consultar projeção.


# 162. E2E

Não implementar dezenas de fluxos E2E inicialmente.


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