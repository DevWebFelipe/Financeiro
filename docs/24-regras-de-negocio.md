# Regras de Negócio — Financial Control

## 0. Hierarquia

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

Este é o documento canônico das regras financeiras detalhadas.


## 1. Objetivo

Este documento define as regras de negócio da aplicação Financial Control.

As regras aqui descritas representam o comportamento esperado do sistema.

Regras financeiras devem ser implementadas no backend.

O frontend pode auxiliar na validação e experiência do usuário, mas nunca deve ser a única camada responsável por garantir uma regra financeira.


# 2. Usuário

## RN001 — Isolamento de usuários

Todo dado financeiro pertence a um usuário.

Um usuário nunca pode visualizar, alterar ou excluir dados pertencentes a outro usuário.

O modelo físico impede referência cruzada (ex.: despesa do usuário A com categoria do usuário B) via `user_id` e FKs compostas `(referenced_id, user_id)`. Detalhe: `docs/23-modelo-de-dados.md` seções 264–266.


## RN002 — Identificação do usuário

O backend deve obter o ID do usuário autenticado através do contexto de segurança.

Nunca confiar em um userId enviado pelo frontend para determinar o proprietário de uma operação.

Incorreto: `GET /expenses?userId=...` aceito como dono dos dados.

Correto: `GET /expenses` — o backend determina o usuário pelo contexto autenticado.


## RN003 — Email

O email do usuário deve ser único.

Antes de persistir ou autenticar, o e-mail é normalizado: `trim` seguido de lowercase.

A constraint `UNIQUE (email)` da tabela `users` permanece a garantia no banco.


## RN004 — Usuário desativado

Usuário desativado (`active = false`) não pode autenticar.

Login de usuário desativado, inexistente ou com senha incorreta responde **401** com a mensagem genérica `Credenciais inválidas.`

Um Access Token de usuário desativado não autoriza endpoints protegidos.


# 3. Contas

## RN005 — Conta pertence ao usuário

Toda conta deve possuir um único proprietário.


## RN006 — Tipos de conta

A V1 suporta:

BANK_ACCOUNT

CASH


## RN007 — Conta ativa

Somente contas ativas podem ser utilizadas em novas operações.


## RN007A — Inativação com saldo (Fase 14)

Uma conta com saldo derivado diferente de zero **não** pode ser inativada.

Antes de desativar (`POST /accounts/{id}/deactivate`):

1. calcular o saldo atual da conta;
2. se `saldo != 0,00` → rejeitar (**400**, `BUSINESS_RULE_VIOLATION`);
3. se `saldo == 0,00` → permitir.

A regra é aplicada no backend. Contas inativas não participam de novas operações financeiras (RN007).


## RN008 — Conta com histórico

Uma conta que possui movimentações não deve ser excluída fisicamente.


## RN009 — Conta desativada

Uma conta desativada continua aparecendo no histórico.


## RN010 — Saldo inicial (Fase 14)

Toda conta começa conceitualmente com saldo inicial `R$ 0,00`.

Na criação (`POST /accounts`), `initialBalance` é **opcional**. Omitido ⇒ `0,00`. Se informado, o valor informado é persistido.

Após criar a conta, o usuário pode definir/alterar o saldo inicial **somente enquanto a conta ainda não tiver nenhuma movimentação financeira efetiva** (RN010A), via o endpoint oficial único:

```text
PUT /api/v1/accounts/{id}/initial-balance
```

Não criar outros endpoints equivalentes. O `PUT /accounts/{id}` cadastral **não** altera `initial_balance`.

Depois que existir a **primeira** movimentação financeira efetiva (RN010A):

- o saldo inicial não pode mais ser definido;
- o saldo inicial não pode mais ser alterado;
- isso permanece verdadeiro mesmo se a movimentação for posteriormente cancelada, revertida ou estornada.

Correções posteriores usam **Acerto de Saldos** (RN204 / `BALANCE_ADJUSTMENT`), nunca edição recorrente de `initial_balance`.

`initial_balance` é ponto de partida da linha temporal — não é saldo corrente persistido nem mecanismo de correção.


## RN010A — Primeira movimentação (Fase 14)

"Primeira movimentação" é o primeiro fato financeiro efetivo da conta.

**Contam** como movimentação (bloqueiam definitivamente o saldo inicial):

1. Receita `RECEIVED`;
2. Payment `ACTIVE` de despesa que produza saída da conta;
3. Pagamento `ACTIVE` de fatura que produza saída da conta;
4. Refund de compra no cartão que produza entrada na conta (`settlement = ACCOUNT`);
5. Transferência `ACTIVE`;
6. Acerto de Saldos `ACTIVE` (`BALANCE_ADJUSTMENT`).

Uma vez que qualquer desses fatos tenha ocorrido, a conta passa a ser considerada "já movimentada" **definitivamente**, mesmo que o fato seja depois cancelado, revertido ou estornado. Cancelamento/reversão **não** reabre a possibilidade de definir ou alterar o saldo inicial.

**Não contam** (não bloqueiam o saldo inicial):

- criação da conta;
- alterações cadastrais;
- definição/alteração do próprio saldo inicial;
- criação de despesa `OPEN` sem pagamento;
- criação de receita `EXPECTED` sem recebimento;
- criação de parcela;
- criação de fatura;
- operações exclusivamente relacionadas ao cartão que não movimentem o saldo da conta.

A regra baseia-se em movimentação financeira efetiva da conta, não na mera existência de registros relacionados.


## RN011 — Saldo

O saldo da conta é derivado das movimentações financeiras efetivas, tendo o saldo inicial como ponto de partida.

Não utilizar um campo `current_balance` como fonte de verdade.

O saldo deve refletir:

saldo inicial;

receitas efetivamente recebidas;

despesas efetivamente realizadas (payments `ACTIVE`);

pagamentos de fatura `ACTIVE`;

devoluções ACCOUNT de compra no cartão (quando aplicável);

transferências `ACTIVE` (entrada/saída);

acertos de saldo `ACTIVE` (`BALANCE_ADJUSTMENT`).

Conceitualmente:

```text
Saldo em uma data =
saldo inicial
+ receitas efetivamente recebidas até a data
− payments ACTIVE de despesas não CANCELLED/REFUNDED até a data
− pagamentos de fatura ACTIVE até a data
+ devoluções ACCOUNT de compra no cartão (quando aplicáveis à data)
+ transferências ACTIVE de entrada até a data
− transferências ACTIVE de saída até a data
+ acertos de saldo ACTIVE (adjustment_amount) até a data
```

A partir da Fase 7, "despesas efetivamente realizadas" significa a soma dos `payments.amount` da conta cuja despesa **não** está em `CANCELLED` nem `REFUNDED` (RN216).

A Fase 8 **emenda** essa fórmula: somente payments **`ACTIVE`** entram na soma; payments **`REVERSED`** não movimentam saldo; adjustments de **parcela/fatura** **não** movimentam saldo da conta (RN240). Refund da despesa continua excluindo os payments daquela despesa.

A Fase 9 inclui pagamentos de fatura `ACTIVE` e devoluções ACCOUNT (RN117 / RN240).

A **Fase 14** inclui transferências `ACTIVE` e acertos de saldo `ACTIVE` (`BALANCE_ADJUSTMENT`). Transferências/acertos `REVERSED` não movimentam saldo. Contrato: §19.5.

**Acerto de Saldos** (nome conceitual oficial) / identificador técnico `BALANCE_ADJUSTMENT` é fato próprio de conciliação (RN204). Não é receita, despesa, transferência, payment nem adjustment de parcela/fatura. Tabela oficial: `account_balance_adjustments`.


## RN012 — Saldo negativo

A V1 não permite que operações financeiras normais deixem a conta com saldo negativo.

Inclui: transferências (criação e reversão), acertos de saldo (criação e reversão quando o efeito for de saída), pagamento de despesas e pagamento de fatura (limitado ao saldo da conta).

Esta regra **não** se aplica ao estorno de receita recebida. O estorno é operação de correção (RN200), não despesa, não pagamento, não transferência e não consumo normal de saldo.

Esta exceção **não** se estende ao pagamento nem ao estorno de despesa. Pagamento de despesa não pode deixar saldo negativo (RN076A). O estorno de despesa (RN214, RN215) desfaz débitos já registrados e, portanto, devolve valor à conta; não usa a permissão de saldo negativo da Fase 6.

O cancelamento de receita prevista (RN207) também não consome saldo: parte de `EXPECTED`, que já não alterava o saldo. O cancelamento de despesa `OPEN` (RN213) igualmente não consome saldo.


## RN013 — Pagamento

Um pagamento pode reduzir o saldo da conta.


## RN014 — Receita recebida

Uma receita recebida aumenta o saldo da conta informada.


## RN015 — Receita prevista

Uma receita prevista não altera o saldo atual.


# 4. Transferências

Contrato completo da Fase 14: §19.5. Resumo das regras canônicas:


## RN016 — Transferência

Uma transferência representa movimentação de saldo entre duas contas **do mesmo usuário autenticado**.

Não é receita. Não é despesa. É fato financeiro próprio.

Status persistidos: `ACTIVE`, `REVERSED`. Somente `ACTIVE` produz efeito financeiro.


## RN016A — Contas participantes (Fase 14)

Somente contas `BANK_ACCOUNT` participam de transferências.

`CASH` **não** participa. Cartões de crédito **não** participam.

Origem e destino devem estar **ativas**.


## RN017 — Contas diferentes

Conta origem e conta destino devem ser diferentes. Não existe transferência de uma conta para ela mesma.


## RN018 — Valor

Transferência deve possuir valor maior que zero.


## RN019 — Saldo na criação

A conta origem deve possuir saldo suficiente (`saldo da origem >= valor`). Transferência não pode provocar saldo negativo.


## RN019A — Saldo na reversão (Fase 14)

Na reversão, a conta que sofrerá o **débito** do movimento inverso deve possuir saldo suficiente (`>= valor`).

A reversão **não** restaura um snapshot antigo de saldo; aplica o movimento inverso.


## RN020 — Atomicidade

Débito e crédito da transferência devem ocorrer na mesma transação: ou todos os efeitos são aplicados, ou nenhum.


## RN021 — Patrimônio

Transferências não alteram o patrimônio total do usuário.


## RN022 — Receita/despesa

Transferências não devem ser contabilizadas como receita ou despesa.


## RN022A — Data financeira (Fase 14)

Uma transferência possui uma única `transfer_date` (data financeira), compartilhada por origem e destino.

Calendário: `America/Sao_Paulo`.

**Retroativa:** permitida (data financeira passada).

**Futura:** **não** permitida. A Fase 14 não agenda transferências.


## RN022B — Reversão e imutabilidade (Fase 14)

Transferência **não** é editável. Correção = reverter + criar nova.

Reversão: `ACTIVE` → `REVERSED`; mantém o registro; remove efeito financeiro; não pode ser revertida novamente (não existe "desreversão").

Detalhe: §19.5 / RN255–RN258.


# 5. Cartões

## RN023 — Cartão pertence ao usuário

Cada cartão pertence a um usuário.


## RN024 — Cartão ativo

Somente cartões ativos podem receber novas compras.


## RN025 — Cartão desativado

Cartão desativado permanece disponível para consulta histórica.

Inativação **não** exclui o cartão. Cartão com histórico financeiro **não** deve ser excluído. Cartão pode ser reativado.

Inativo: não aceita novas compras nem novos movimentos de compra. Faturas, pagamentos, créditos e histórico existentes continuam válidos e acessíveis.


## RN025A — Titular

`holderName` é textual e informativo. Não precisa ser o usuário autenticado. É permitido titular de outra pessoa. O titular deve ser utilizável em filtros/consultas.


## RN025B — Dados do plástico

Não armazenar PAN completo, CVC/CVV, senha nem validade física do cartão. Não criar `expiration_date` para representar validade do plástico.

`last_four_digits` **pode** existir e **não** é obrigatório.


## RN026 — Limite

`credit_limit` é o limite contratado **persistido**.

`used_limit` e `available_limit` **não** são persistidos. São sempre calculados.

O limite usado considera o valor ainda não liquidado das compras/parcelas do cartão. Uma compra de R$ 1.000 em 10× R$ 100 consome R$ 1.000. Cada parcela liquidada (pagamento de fatura rateado, crédito aplicado ou equivalente) libera o respectivo valor. Pagamento antecipado libera proporcionalmente.

Crédito de cartão **não** aumenta o limite contratado nem o disponível.

Alterar `credit_limit` é permitido (aumentar ou diminuir), inclusive para valor abaixo do já utilizado. Nesse caso `available_limit` pode ficar negativo. Não bloquear a alteração por isso.


## RN027 — Limite não é saldo

O limite do cartão não deve ser contabilizado como dinheiro disponível na conta.


## RN028 — Compra no cartão

Uma compra no cartão não reduz imediatamente o saldo bancário.


## RN029 — Comprometimento

Uma compra no cartão aumenta o comprometimento do cartão.


## RN029A — Limite disponível — SUPERADA

**SUPERADA** pelo contrato da Fase 9.

A redação anterior recusava compra acima do limite disponível. Essa recusa **não** vale mais.

A compra no cartão **deve ser permitida** mesmo que ultrapasse o limite disponível (`available_limit` negativo). O backend **não** bloqueia por limite insuficiente. Eventual alerta visual é responsabilidade futura da apresentação e não faz parte da Fase 9.

Exemplo (comportamento vigente):

Limite: R$ 5.000,00

Comprometido: R$ 4.500,00

Disponível: R$ 500,00

Compra: R$ 600,00

Resultado: compra **aceita**; disponível passa a R$ −100,00.


## RN030 — Estorno

Um estorno reduz o comprometimento correspondente.


# 6. Categorias

## RN031 — Categoria pertence ao usuário

Cada categoria pertence a um usuário.


## RN032 — Tipos

Categorias podem ser:

INCOME

EXPENSE


## RN033 — Categoria ativa

Somente categorias ativas devem ser utilizadas em novos lançamentos.


## RN034 — Categoria histórica

Categorias utilizadas em operações históricas não devem ser excluídas fisicamente.


## RN034A — Unicidade de categoria

O usuário não pode possuir duas categorias com o mesmo `type` e o mesmo `name`.

A unicidade considera `user_id + type + name`, independentemente de `active`.

O nome é normalizado com `trim` antes da persistência e da comparação. A comparação de unicidade é case-insensitive (`Mercado`, ` mercado` e `MERCADO` são o mesmo nome).

Categorias de tipos diferentes podem ter o mesmo nome (`EXPENSE` Mercado e `INCOME` Mercado).


# 7. Responsável

## RN035 — Responsável

A despesa pode possuir responsável.


Valores padronizados:

MINE

GIULIA

EDERSON

ELISIANE

OTHER


## RN036 — Outro responsável

Quando:

OTHER


o sistema deve permitir informar texto livre.


## RN037 — Responsável não é usuário

O responsável financeiro não representa necessariamente um usuário do sistema.


## RN038 — Responsável

O responsável é utilizado principalmente para classificação e relatórios.


# 8. Receitas

O registro em `incomes` é a duplicata (título a receber). Não existe entidade separada.

Cancelamento e estorno são operações diferentes (RN198, RN200, RN207).


## RN039 — Receita

Uma receita deve possuir:

descrição;

valor;

categoria;

data prevista;

status.


## RN040 — Valor

Valor de receita deve ser maior que zero.


## RN041 — Receita esperada

Status:

EXPECTED


Duplicata ativa, ainda não recebida. Não altera o saldo da conta.

`account_id` e `received_date` são nulos. Não representa movimentação financeira efetivada.

É também o estado após o estorno de um recebimento (RN200): a duplicata permanece ativa e pode ser recebida novamente.


## RN042 — Receita recebida

Status:

RECEIVED


O recebimento baixa a duplicata e gera a movimentação financeira correspondente (`EXPECTED` → `RECEIVED`).

Representa entrada financeira real.

`account_id` e `received_date` são obrigatórios.


## RN043 — Conta

Uma receita recebida deve estar vinculada à conta que recebeu o dinheiro.

Após o estorno, `account_id` volta a `null`. O próximo recebimento informa novamente a conta.


## RN044 — Data de recebimento

Receitas recebidas devem possuir data de recebimento.

Após o estorno, `received_date` volta a `null`. O próximo recebimento informa novamente a data.


## RN045 — Receita cancelada

O cancelamento inutiliza a duplicata (`EXPECTED` → `CANCELLED`).

Após o cancelamento:

- o registro permanece para fins históricos;
- o status passa a `CANCELLED`;
- a receita não representa mais duplicata pendente;
- não entra na projeção futura;
- não participa do saldo efetivo;
- não pode ser recebida posteriormente nesta fase.

O cancelamento **não** é estorno de recebimento e **não** deve ser tratado como reversão de `RECEIVED`.

Nesta fase o cancelamento parte somente de `EXPECTED`. Não há impacto financeiro, porque `EXPECTED` já não alterava o saldo.


## RN046 — Receita recebida

Receita já recebida não pode voltar silenciosamente para EXPECTED.

A correção explícita é o estorno (`POST /api/v1/incomes/{id}/reverse`), que retorna a duplicata a `EXPECTED` (ativa, não cancelada).

Não usar cancelamento para desfazer um recebimento.


## RN198 — Transições de status de receita

Cancelamento e estorno **não são a mesma operação**.

Ciclo oficial:

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

Transições permitidas:

```text
EXPECTED
   ├── receive ──► RECEIVED
   └── cancel  ──► CANCELLED

RECEIVED
   └── reverse ──► EXPECTED
```

Cancelar inutiliza a duplicata. Estornar desfaz o recebimento e mantém a duplicata ativa como não recebida.

Não existe status `REVERSED` para receitas. Os status oficiais continuam `EXPECTED`, `RECEIVED` e `CANCELLED`. O registro em `incomes` é a duplicata; não existe entidade separada.


## RN199 — Transições não permitidas de receita

Nesta fase, as seguintes transições são rejeitadas:

```text
RECEIVED  → CANCELLED
CANCELLED → EXPECTED
CANCELLED → RECEIVED
RECEIVED  → RECEIVED via receive
EXPECTED  → EXPECTED via reverse
CANCELLED → CANCELLED via cancel
```

Não existe operação de reativação de receita cancelada nesta fase.

O caminho composto `RECEIVED` → reverse → `EXPECTED` → cancel → `CANCELLED` já é possível pela composição das operações definidas (estornar e, em seguida, cancelar a duplicata prevista). Isso não é cancelamento direto de receita recebida.

**DECISÃO PENDENTE DO DESENVOLVEDOR:** cancelamento direto de receita já `RECEIVED` (sem estornar antes). A Fase 6 rejeita `RECEIVED` → `CANCELLED`. Não está definido se, em fase posterior, essa transição passará a existir, nem se o único caminho continuará sendo estornar e depois cancelar. Não implementar `RECEIVED` → `CANCELLED` até decisão explícita.


## RN200 — Estorno de receita recebida

O estorno **não cancela** a duplicata. Ele desfaz o recebimento e os efeitos financeiros correspondentes.

Transição:

```text
RECEIVED
   │
   └── ESTORNO
          ↓
      EXPECTED
```

e não:

```text
RECEIVED
   │
   └── ESTORNO
          ↓
      CANCELLED
```

O estorno de uma receita em `RECEIVED`:

1. desfaz o impacto financeiro produzido pelo recebimento original (conta e valor daquele recebimento);
2. altera o status para `EXPECTED`;
3. limpa `account_id` (`null`);
4. limpa `received_date` (`null`).

Após o estorno, a duplicata:

- continua ativa;
- continua existindo (o registro não é apagado);
- volta a representar uma receita não recebida;
- pode ser editada e recebida novamente.

Não cria despesa, receita negativa, status `REFUNDED`, status `REVERSED` nem status `CANCELLED`.

O próximo recebimento (`POST /receive`) deve informar novamente `accountId` e `receivedDate`. Não reutilizar automaticamente a conta anterior.

O estorno não deve ser bloqueado apenas porque o saldo resultante da conta ficará negativo. Esta possibilidade de saldo negativo é exceção à regra das operações normais (RN012).

Exemplo (correção com saldo não negativo):

```text
Conta: R$ 10.000
Receita recebida: +R$ 5.400
Saldo: R$ 15.400

Após estorno:
Saldo: R$ 10.000
status: EXPECTED
account_id: null
received_date: null

Se recebida novamente por R$ 5.500:
Saldo: R$ 15.500
```

Exemplo (correção com saldo negativo):

```text
Saldo atual: R$ 200
Recebimento anterior: +R$ 1.000
(o valor já foi utilizado em outras movimentações)

Estorno: −R$ 1.000
Saldo resultante: −R$ 800
```

O estorno continua permitido. Não se deve impedir a correção para preservar artificialmente um saldo não negativo.


## RN201 — Atomicidade do estorno de receita

O estorno é operação financeira atômica.

Conceitualmente:

```text
validar receita
↓
validar estado RECEIVED
↓
identificar a conta que recebeu o valor
↓
desfazer o impacto financeiro
↓
alterar status para EXPECTED
↓
limpar account_id
↓
limpar received_date
↓
commit
```

Se qualquer etapa falhar, toda a operação sofre rollback.


## RN202 — Edição de receita

`EXPECTED`: pode ser editada dentro dos campos permitidos pelo contrato da API.

`RECEIVED`: não pode sofrer edição que altere silenciosamente uma movimentação financeira já realizada. Para corrigir:

```text
RECEIVED → reverse → EXPECTED → PUT → EXPECTED → receive → RECEIVED
```

`CANCELLED`: não deve ser editada nem recebida nesta fase. Cancelamento inutiliza a duplicata; não há reativação nesta fase.


## RN203 — Receitas sem responsável (Fase 6)

Receitas não utilizam responsável nesta fase.

A API, as regras de negócio e os testes da Fase 6 não expõem nem utilizam `responsibleType` / `responsibleName`.

As colunas `responsible_type` e `responsible_name` permanecem no modelo físico. Não remover.

`responsible_type` é nullable (migration V16). Não persistir valor artificial (ex.: `MINE`) só para satisfazer o antigo `NOT NULL`.

O CHECK de valores válidos (`MINE`, `GIULIA`, `EDERSON`, `ELISIANE`, `OTHER`) permanece. Não alterar esses valores.

`responsible_name` permanece e é compatível com a ausência de responsável.

Esta regra não altera o uso de responsável em despesas (RN035–RN038).


## RN207 — Cancelamento de receita prevista

O cancelamento inutiliza a duplicata. Não desfaz um recebimento, porque só se aplica a receita ainda não recebida.

Transição:

```text
EXPECTED → CANCELLED
```

Endpoint: `POST /api/v1/incomes/{id}/cancel`.

Após o cancelamento (RN045):

- o registro permanece para histórico;
- o status é `CANCELLED`;
- a duplicata deixa de ser receita pendente;
- não pode ser recebida nesta fase;
- não há efeito financeiro a desfazer (`EXPECTED` não alterava o saldo).

O cancelamento **não** limpa dados de recebimento por analogia com o estorno: em `EXPECTED`, `account_id` e `received_date` já são nulos.

Não tratar `cancel` como sinônimo de `reverse`.


# 9. Despesas

## RN047 — Despesa

Uma despesa deve possuir:

descrição;

valor;

categoria;

data;

forma de pagamento;

status.


## RN048 — Valor

Valor da despesa deve ser maior que zero.


## RN049 — Forma de pagamento

Formas permitidas:

ACCOUNT

CREDIT_CARD

NONE


## RN050 — ACCOUNT

Quando a forma for `ACCOUNT`, a despesa deve possuir `account_id` obrigatório.

Na Fase 7, `ACCOUNT` **não** significa despesa já paga. A criação resulta em `OPEN`, sem linha em `payments` e sem alteração de saldo (RN208). O pagamento é operação posterior (`POST /api/v1/expenses/{id}/pay`).


## RN051 — CREDIT_CARD

Quando a forma for:

CREDIT_CARD


a despesa deve possuir cartão.


## RN052 — NONE

Quando a forma for `NONE`:

- `credit_card_id` é nulo;
- `account_id` é nulo na despesa (permanece nulo após o pagamento);
- a forma original `NONE` não muda quando o pagamento informa a conta.

A conta efetivamente usada fica em `payments.account_id` (RN209).


## RN053 — Conta e cartão

Uma despesa não deve possuir simultaneamente conta e cartão como forma principal de pagamento.


## RN054 — Despesa aberta

OPEN representa obrigação ainda não quitada.


## RN055 — Despesa vencida (derivada)

OVERDUE NÃO deve ser armazenado como status principal.

**1/1 (Fase 7 e Fase 8):** a despesa é overdue quando status é `OPEN` ou `PARTIALLY_PAID` e `dueDate` < data atual (`America/Sao_Paulo`) — RN218.

**N>1 (Fase 8):** a despesa é overdue quando existe **pelo menos uma** parcela overdue segundo RN241. Não usar somente `expenses.due_date`.

A interface poderá apresentar "VENCIDA" sem alterar o status persistido. Não existe coluna nem valor de status `OVERDUE`.

Status oficiais persistidos: OPEN, PARTIALLY_PAID, PAID, CANCELLED, REFUNDED.


## RN056 — Pagamento parcial

Quando parte da despesa for paga:

PARTIALLY_PAID


## RN057 — Pagamento integral

Quando o valor devido for totalmente pago:

PAID


## RN058 — Cancelamento

Uma despesa cancelada:

não representa obrigação ativa;

não deve aparecer como conta a pagar;

não deve participar da projeção futura.

Na Fase 7 o cancelamento parte somente de `OPEN` (RN213). Não há pagamentos a desfazer.


## RN059 — Histórico

Despesa cancelada continua armazenada. Não há exclusão física.


## RN060 — Estorno

Despesa estornada permanece armazenada. As linhas de `payments` **não** são apagadas (RN215).


## RN061 — Estorno

Despesa estornada (`REFUNDED`) não deve continuar representando obrigação financeira ativa.

Não volta a `OPEN`. Não replica o estorno de receita (`RECEIVED` → `EXPECTED`).


# 10. Parcelamento

## RN062 — Parcelamento

Uma despesa pode possuir uma ou várias parcelas.


## RN063 — Parcela mínima

Toda despesa deve possuir pelo menos uma parcela.


## RN064 — Quantidade

Quantidade de parcelas deve ser maior que zero.

Definida na criação (RN223). Não alterável depois.


## RN065 — Valores

Cada parcela possui seu próprio valor.


## RN066 — Parcelas diferentes

O sistema deve permitir valores diferentes para parcelas diferentes.


## RN067 — Soma

Ao criar um parcelamento, a soma das parcelas deve ser igual ao valor total da despesa.

A mesma invariável vale após edição cadastral de parcela `OPEN` (RN227). Payments, adjustments, reverse, refund e cancel **não** alteram `installment.amount` nem recalculam `expenses.total_amount`.

Não existe regra de valor mínimo por parcela (`amount > 0` em cada linha). Se a divisão em centavos, com residual na primeira (RN068), produzir uma ou mais parcelas `0.00`, isso é **permitido** desde que a soma continue igual a `expenses.total_amount`. Não rejeitar o parcelamento só porque alguma parcela resultou em `0.00`.


## RN068 — Arredondamento

Diferenças de centavos devem ser ajustadas na **primeira** parcela.

A redação anterior (“última parcela absorve o residual”) está **substituída**. O exemplo canônico (RN069) permanece: R$ 100 / 3 → 33,34 + 33,33 + 33,33.

Exemplo de total mínimo: R$ 0,01 / 3 → `0,01` + `0,00` + `0,00` (soma = total; sem perda de centavo).


## RN069 — Exemplo

R$ 100 em 3 parcelas:

R$ 33,34

R$ 33,33

R$ 33,33


## RN070 — Parcela paga

Uma parcela `PAID` não pode ser alterada cadastralmente (`amount` / `due_date`).

Parcelas `PARTIALLY_PAID`, `CANCELLED` e `REFUNDED` também não podem ser alteradas dessa forma (RN227).


## RN071 — Parcela aberta

Uma parcela `OPEN` pode ter `amount` e `due_date` alterados por operação própria da parcela, desde que a soma de todos os `expense_installments.amount` permaneça igual a `expenses.total_amount` (RN227).

Essa alteração é cadastral: não é payment, adjustment, refund nem reverse.


## RN072 — Parcela cancelada

Parcela cancelada não participa de compromissos futuros.


# 11. Pagamentos

## RN073 — Pagamento

Pagamento representa saída financeira real.


## RN074 — Conta

Todo pagamento deve indicar a conta utilizada (`payments.account_id`).

Na Fase 7:

- `NONE`: a conta é informada no `POST /pay` e gravada só em `payments.account_id`;
- `ACCOUNT`: a RN210 exigia a mesma conta da despesa.

A RN210 foi **SUPERADA** na Fase 8 (RN228): `expenses.account_id` é preferência; `payments.account_id` pode ser outra conta do mesmo usuário.


## RN075 — Valor

Pagamento deve ser maior que zero.


## RN076 — Limite

A soma dos payments **ACTIVE** não pode ultrapassar o valor devido da parcela (obrigação líquida: original + surcharges ACTIVE − discounts ACTIVE).


## RN076A — Saldo da conta

Pagamento de despesa não pode exceder o saldo disponível da conta utilizada.

Não permitir saldo negativo.


## RN077 — Pagamento parcial

Uma despesa pode possuir múltiplos pagamentos.


## RN078 — Exemplo

Despesa:

R$ 1.000


Pagamento 1:

R$ 300


Pagamento 2:

R$ 700


Resultado:

PAID


## RN079 — Pagamento

Pagamento reduz o saldo da conta utilizada.


## RN080 — Atomicidade

Alteração da despesa/parcela e movimentação da conta devem ocorrer na mesma transação.


# 12. Faturas

## RN081 — Fatura

Cada cartão possui ciclos de faturamento.


## RN082 — Ciclo

Cada ciclo deve possuir:

data de fechamento;

data de vencimento.


## RN083 — Unicidade

Um cartão não pode possuir duas faturas para o mesmo ciclo.


## RN084 — Compra

Compra no cartão deve ser vinculada à fatura correspondente.


## RN085 — Compra parcelada

Uma compra parcelada pode gerar parcelas em várias faturas.

A despesa original permanece uma só.

Cada parcela (`expense_installments`) é vinculada à fatura do respectivo ciclo via `invoice_id`.

`expenses` não possui `invoice_id`.


## RN086 — Fatura

Uma fatura pode conter parcelas de várias despesas.

Os itens da fatura são `expense_installments`, não `expenses`.


## RN087 — Fatura aberta

OPEN permite novas compras cujo ciclo (data da compra × `closing_day`) pertence a essa fatura.

O ciclo **não** se determina pela mera existência de uma fatura OPEN. Se o fechamento automático estiver atrasado, a data da compra continua mandando (RN095).


## RN088 — Fatura fechada

CLOSED não deve receber novas compras do ciclo fechado. Novas compras pertencem ao próximo ciclo. Fatura CLOSED não reabre.


## RN089 — Fatura vencida (derivada)

OVERDUE NÃO é status persistido da fatura.

Status persistidos da fatura: SCHEDULED, OPEN, CLOSED, PAID (Fase 9); SETTLED_BY_AGREEMENT (Fase 13 — §19.4).

`PARTIALLY_PAID` **não** é status de fatura (redação anterior **SUPERADA**).

Fatura não `PAID` após o vencimento pode ser apresentada como vencida na UI (derivado de dueDate e status ≠ PAID). `SCHEDULED` não se apresenta como vencida.


## RN090 — Pagamento parcial da fatura — SUPERADA a transição de status

**SUPERADA** a regra de que pagamento parcial gera `PARTIALLY_PAID`.

Pagamento parcial **não** altera o status da fatura.

Exemplos:

- OPEN com remaining R$ 1.000; pagamento R$ 200 → continua OPEN (remaining R$ 800);
- OPEN com remaining R$ 0 → continua OPEN até o fechamento;
- CLOSED com remaining > 0; pagamento parcial → continua CLOSED;
- CLOSED com remaining = 0 → PAID.


## RN091 — Fatura paga

`PAID` exige **as duas** condições:

1. a fatura já foi fechada (não está OPEN nem SCHEDULED);
2. remaining = 0 (100% liquidado).

OPEN + remaining 0 **não** é PAID.

`PAID` é terminal. Nada pode alterar uma fatura PAID (novo pagamento, ajuste, compra, reabertura, alteração de histórico). Benefício financeiro posterior usa crédito do cartão.


## RN091A — Estados da fatura

Fluxo oficial:

```text
SCHEDULED → OPEN → CLOSED → PAID
CLOSED → SETTLED_BY_AGREEMENT (Fase 13)
```

No fechamento de uma OPEN:

- remaining > 0 → CLOSED;
- remaining = 0 → PAID.

No máximo **uma** fatura OPEN por cartão (unique parcial na migration da Fase 9).

Faturas futuras de compra parcelada nascem SCHEDULED. Quando o ciclo correspondente iniciar: SCHEDULED → OPEN.

Não usar `invoice_id` nulo para representar fatura futura. A parcela vincula-se à fatura na criação.


# 13. Fechamento de cartão

## RN092 — Data de fechamento

A data de fechamento determina o ciclo da compra, em conjunto com a **data da compra**, no calendário `America/Sao_Paulo`.


## RN092A — Alteração de dias do cartão

`closingDay` e `dueDay` podem ser alterados. A alteração vale somente para **ciclos futuros**.

Não reescrever faturas já existentes, parcelas já vinculadas, datas históricas nem pagamentos históricos.


## RN093 — Compra antes do fechamento

Compra realizada **antes** do dia de fechamento deve pertencer ao ciclo atual.

O dia do fechamento não faz parte deste caso (ver RN095).

Exemplo: fechamento dia 10; compra dia 09 → ciclo que fecha dia 10.


## RN094 — Compra após fechamento

Compra realizada após o dia de fechamento deve pertencer ao próximo ciclo.

Exemplo: fechamento dia 10; compra dia 11 → próximo ciclo.


## RN095 — Dia do fechamento

Compra realizada exatamente no dia do fechamento pertence à **próxima fatura**.

Exemplo:

Fechamento: dia 10

Compra: 10/08

Resultado: a compra entra no próximo ciclo (não no que fecha em 10/08).


## RN096 — Horário

Timestamps de sistema são persistidos em UTC.

Regras de calendário financeiro (dia da compra vs dia de fechamento, "hoje", atraso) utilizam `America/Sao_Paulo`.

O frontend não deve usar o timezone do navegador para essa decisão.


# 14. Vencimento de cartão

## RN096A — Fechamento automático

Fechamento de fatura **não** é operação funcional normal do usuário.

Adotar scheduler Spring, idempotente. O processo deve:

- abrir faturas SCHEDULED cujo ciclo iniciou — o ciclo inicia na `closing_date` da fatura anterior do mesmo cartão (nesse dia as compras já pertencem a este ciclo, RN095); a primeira fatura do cartão nasce OPEN;
- fechar faturas OPEN cuja `closing_date` chegou (remaining > 0 → CLOSED; remaining = 0 → PAID);
- marcar CLOSED com remaining = 0 como PAID;
- poder executar de novo sem duplicar efeitos.

Não reabrir fatura fechada. Não fechar novamente fatura já fechada. Não alterar PAID.

Se o scheduler atrasar, o ciclo da compra continua baseado na data (RN095), não no status OPEN.


## RN097 — Dia de vencimento

O cartão possui dia configurado para vencimento.


## RN098 — Mês sem o dia

Se o mês não possuir o dia configurado, utilizar o **último dia daquele mês**.

Exemplo:

Dia configurado: 31

Fevereiro (não bissexto): 28/02

Abril: 30/04


## RN099 — Regra

RN095, RN098 e RN099B devem ser cobertas por testes automatizados.


# 15. Pagamento de fatura

## RN099A — Vencimento da parcela no cartão

A primeira parcela de uma compra `CREDIT_CARD` recebe como vencimento a `due_date` da fatura à qual a compra pertence.

Exemplo: compra 11/08; fechamento dia 10; vencimento da fatura dia 20 → a compra pertence à fatura seguinte; primeira parcela vence em 20/09.

As demais parcelas seguem as respectivas faturas futuras (cada uma com o due da sua fatura).

Para `CREDIT_CARD`, o `dueDate` informado na criação da despesa **não** define o vencimento das parcelas. O backend calcula. `expenses.due_date` persistido = vencimento da primeira parcela.


## RN099B — `due_date` da fatura quando `due_day` ≤ `closing_day`

A `due_date` da fatura é calculada a partir da `closing_date` já determinada (RN098 no dia de fechamento) e do `due_day` do cartão **no momento em que a fatura nasce**. Não recalcular se o cartão mudar depois (RN092A).

Regra:

A comparação usa os **dias configurados** do cartão (`due_day` e `closing_day`), não o dia-do-mês efetivo de `closing_date` após RN098.

- se `due_day` > `closing_day`: `due_date` cai no **mesmo mês** da `closing_date`, no dia `due_day` (se o mês não tiver esse dia: último dia do mês — RN098);
- se `due_day` ≤ `closing_day`: `due_date` cai no **mês seguinte** ao da `closing_date`, no dia `due_day` (idem RN098).

Exemplos:

- fechamento dia 10, vencimento dia 20, ciclo que fecha 10/09 → 20 > 10 → due 20/09;
- fechamento dia 25, vencimento dia 5, ciclo que fecha 25/08 → 5 ≤ 25 → due 05/09;
- fechamento dia 10, vencimento dia 10, ciclo que fecha 10/09 → 10 ≤ 10 → due 10/10;
- fechamento dia 31, vencimento dia 31, ciclo que fecha 28/02 (não bissexto) → 31 ≤ 31 → mês seguinte → due 31/03;
- fechamento dia 31, vencimento dia 31, ciclo que fecha 31/01 → 31 ≤ 31 → mês seguinte → due 28/02 (RN098);
- fechamento dia 31, vencimento dia 5, ciclo que fecha 31/01 → 5 ≤ 31 → due 05/02.


## RN100 — Pagamento

Pagamento de fatura representa saída real de dinheiro da conta.

Pode ser integral, parcial, múltiplo e antecipado. Antecipado é pagamento normal enquanto a fatura está OPEN. Não existe tipo separado de “pagamento antecipado”.


## RN101 — Não duplicação

Pagamento da fatura não cria uma nova despesa de consumo.


## RN102 — Exemplo

Compras:

R$ 1.000


Pagamento da fatura:

R$ 1.000


Despesa de consumo continua:

R$ 1.000


Não:

R$ 2.000


## RN103 — Pagamento parcial

Pagamento parcial é permitido. Não altera o status da fatura (RN090).


## RN104 — Saldo

Se fatura:

R$ 2.000


e pagamento:

R$ 1.200


remaining:

R$ 800


O pagamento não pode exceder o remaining da fatura.


## RN105 — Conta

Pagamento de fatura deve indicar a conta utilizada. Qualquer conta **ativa** do usuário. Não precisa ser a conta originalmente associada à despesa.


## RN105A — Saldo da conta no pagamento de fatura

Pagamento parcial é permitido.

O valor pago não pode exceder o saldo disponível da conta utilizada.

Exemplo:

Saldo da conta: R$ 500,00

Fatura: R$ 1.000,00

Pagamento: R$ 500,00

Resultado: saldo da conta R$ 0,00; fatura com R$ 500,00 restantes.


## RN106 — Atomicidade

Pagamento, rateio (alocações), atualização de remaining/status das parcelas, liberação de limite e débito da conta devem ocorrer na mesma transação.


## RN106A — Sem `payments` da despesa

Pagamento de fatura usa `credit_card_invoice_payments`. **Não** cria linha em `payments`.

Despesa `CREDIT_CARD` **não** pode ser paga por `POST /expenses/{id}/pay` nem por `POST /expenses/{expenseId}/installments/{installmentId}/payments`. A liquidação é exclusivamente pela fatura.


## RN106B — Reverse de pagamento de fatura

Pagamento de fatura não se apaga. Reverse é operação explícita: o pagamento passa a `REVERSED` e deixa de participar do remaining, do saldo da conta e do limite. As alocações daquele pagamento permanecem persistidas e deixam de participar dos cálculos. Não usar DELETE.

Proibido se a fatura estiver `PAID` (nada altera PAID).


## RN247 — Rateio do pagamento da fatura

O pagamento da fatura é um valor **global** do ciclo. Não pertence a uma única compra.

Rateio proporcional ao **saldo aberto** (`remaining`) de cada parcela da fatura. Nunca usar o `amount` original como base se a parcela já estiver parcialmente liquidada.

Somente parcelas com `remaining > 0` participam.

Algoritmo oficial:

1. selecionar parcelas com remaining > 0;
2. ordenar por remaining ASC;
3. empate: `due_date` ASC, depois `id` da parcela ASC (UUID v7 = ordem de criação; estável);
4. proporção de cada parcela sobre a soma dos remainings;
5. arredondar cada share para escala 2 com `RoundingMode.HALF_UP`;
6. limitar cada share ao remaining da própria parcela e ao leftover;
7. residual final na **última** parcela da ordenação (necessariamente uma das maiores bases; no empate, a de `due_date`/`id` maiores);
8. invariantes: soma dos rateios = valor efetivamente rateado; nenhum rateio > remaining; nenhum remaining negativo.

Implementação em Java com `BigDecimal`. Não usar `double`. Não copiar a classe Delphi `TRateioValor`.

O resultado é **fato histórico** persistido: `credit_card_invoice_payment` → alocação → `expense_installment`. Não usar `payments`. Não persistir `paid_amount` na parcela.

Remaining da parcela (Fase 9, cartão):

```text
obligation (RN231)
− pagamentos próprios ACTIVE permitidos pela regra (ACCOUNT/NONE)
− SUM(alocações de pagamentos de fatura ACTIVE)
− SUM(alocações de créditos aplicados)
− efeitos de alocação de ajustes de fatura ACTIVE
```

Despesa `CREDIT_CARD` não possui pagamento próprio via endpoint de despesa.


## RN247A — Ajuste de fatura e rateio

Ajuste de fatura (`DISCOUNT` / `SURCHARGE`) exige `reason`. Participa do **mesmo** algoritmo de rateio RN247 (remaining aberto, ASC, desempate `due_date` ASC depois `id` ASC, residual na última).

Não criar ajuste em fatura `PAID`.

Ajuste negativo (`DISCOUNT`) não pode ultrapassar o remaining disponível. Excedente **não** vira crédito automaticamente.

`SURCHARGE` exige fatura não `PAID` **e** `remaining > 0`. Se `remaining = 0`, rejeitar por regra de negócio: não persistir ajuste sem efeito financeiro; não criar dívida futura sem rateio; não inventar semântica de rateio distinta do RN247. HTTP **400**, `code = BUSINESS_RULE_VIOLATION`, constante `SURCHARGE_REQUIRES_REMAINING`, mensagem `"O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto."`.

Juros/multa de atraso são ajustes (`SURCHARGE` + `reason`), não cálculo automático.


# 16. Parcelamento de fatura

**Status:** domínio da **Fase 13**. Contrato oficial: **§19.4** — `CONCLUÍDA E APROVADA`.

A tabela física `credit_card_invoice_installments` (V13) existe no schema e está **SUPERADA** como contrato de negócio (**D4=A**). Novas tabelas de Agreement na implementação.

## RN107 — Parcelamento — SUPERADA pela Fase 13 (§19.4)

Redação anterior: “uma fatura parcialmente paga pode ter seu saldo parcelado.”

**Vigente (decisão funcional Fase 13):** somente fatura **`CLOSED`** com `remaining > 0` pode ser negociada/parcelada. Fatura `OPEN` **não** pode. Parcelamento parcial do remaining (com entrada) é permitido. Detalhe: §19.4.


## RN108 — Saldo parcelado — SUPERADA em parte pela Fase 13 (§19.4)

Na **nova negociação**, o valor financiado é `invoiceRemaining − entryAmount`. Na **renegociação**, o financed inclui também o líquido dos futuros (`anticipatedFuturesNetAmount`) — RN254. Em ambos, `contractedTotal` **não** precisa igualar o financed (custo adicional); deve ser `>= financedAmount`. Ver RN113 / §19.4.


## RN109 — Exemplo — SUPERADA pela Fase 13 (§19.4)

O exemplo oficial de parcelamento com entrada, total contratado e custo adicional está em §19.4 (não o exemplo R$ 2.000 / R$ 1.200 / R$ 800 com soma das parcelas = saldo).


## RN110 — Parcelamento diferente

Parcelamento/negociação de fatura é diferente de compra parcelada.

**COMPRA ORIGINAL ≠ PARCELA DE NEGOCIAÇÃO (Agreement).**

As compras/parcelas originais (`expense_installments` do ciclo) continuam representando as obrigações de consumo. A negociação liquida a fatura e cria **nova** obrigação (Agreement) com parcelas próprias. Não transformar compras originais em parcelas do Agreement.


## RN111 — Parcelamento — SUPERADA pela Fase 13 (§19.4)

O plano de parcelas pertence ao **Agreement** + despesa `CREDIT_CARD` associada (`expense_installments`). Campos/statuses: §19.4. V13 sem uso de negócio.


## RN112 — Valores das parcelas do Agreement

Na Fase 13 (**D9=A**): todas as parcelas do plano têm o **mesmo** `installmentAmount`. Valores diferentes por parcela **não** fazem parte do request oficial desta fase.


## RN113 — Soma — SUPERADA pela Fase 13 (§19.4)

Redação anterior: “a soma das parcelas deve representar o valor parcelado.”

**Vigente:** **não** exigir `installmentCount × installmentAmount == financedAmount` nem `SUM(parcelas) == financedAmount`.

**Vigente (emenda):** exigir `contractedTotal >= financedAmount`; se menor → **400** `BUSINESS_RULE_VIOLATION`.

O banco pode informar parcelas cujo total contratado seja **maior** que o saldo negociado. O sistema registra:

- entrada (paga imediatamente);
- saldo efetivamente negociado (`financedAmount` — nova negociação: `invoiceRemaining − entryAmount`; renegociação: RN254);
- total contratado (`installmentCount × installmentAmount`);
- custo adicional = total contratado − financedAmount;
- percentual de acréscimo derivado para exibição/relatórios.

IOF, composição bancária de juros e plano de contas **fora** da Fase 13.


# 17. Cancelamentos e estornos

Cancelamento e estorno **não são sinônimos**.

Em receitas: cancelar inutiliza a duplicata (`EXPECTED` → `CANCELLED`); estornar desfaz o recebimento (`RECEIVED` → `EXPECTED`). Em despesas: `CANCELLED` anula a obrigação; `REFUNDED` registra que a despesa ocorreu e depois foi revertida.


## RN114 — Cancelamento

CANCELLED significa que a operação foi anulada.

Em receitas, `CANCELLED` inutiliza a duplicata prevista (RN045, RN207). Não é o destino do estorno de recebimento.


## RN115 — Estorno

REFUNDED significa que a despesa ocorreu e posteriormente foi revertida.

Receitas não possuem status `REFUNDED`. O estorno de receita recebida segue RN198 e RN200: `RECEIVED` → `EXPECTED`. Não usa `CANCELLED`.


## RN116 — Histórico

Cancelamentos e estornos não devem apagar o registro original.


## RN117 — Estorno no cartão

Estorno/cancelamento de compra `CREDIT_CARD` ajusta o comprometimento conforme a situação financeira real. Não apagar histórico. Não reverter pagamentos de fatura mistos (rateio com outras compras). Não reconstruir faturas retroativamente.

Elegibilidade HTTP **igual** à das despesas `ACCOUNT`/`NONE` (RN236 / RN237), com status da despesa segundo RN235 (remaining das parcelas via fatura, não via `payments`):

- `OPEN` → `POST /api/v1/expenses/{id}/cancel` (sem body de `settlement`);
- `PARTIALLY_PAID` ou `PAID` → `POST /api/v1/expenses/{id}/refund` com `settlement` obrigatório;
- `CANCELLED` / `REFUNDED` → rejeitar.

**Valor liquidado** desta compra (não inclui ajustes de fatura; estes já estão na obligation):

```text
bankLiquidated   = SUM(alocações ACTIVE de pagamentos de fatura nas parcelas desta despesa)
creditLiquidated = SUM(alocações ACTIVE de créditos nas parcelas desta despesa)
totalLiquidated  = bankLiquidated + creditLiquidated
```

Invariável: despesa `OPEN` implica `totalLiquidated = 0`. Se `PARTIALLY_PAID`/`PAID` e `totalLiquidated = 0` (somente desconto de fatura), o refund ainda exige `settlement`; nenhum crédito e nenhum movimento bancário são gerados.

### Cancelamento (`OPEN`)

Efeitos: despesa `CANCELLED`; parcelas em faturas **não** `PAID` → `CANCELLED`; remaining das faturas `OPEN`/`CLOSED` recalculado (soma dos remainings); limite liberado; sem crédito novo; sem movimento bancário. Fatura `PAID` não é alterada.

### Refund (`PARTIALLY_PAID` ou `PAID`) — opções de `settlement`

1. `CARD_CREDIT` — gera crédito de cartão de `totalLiquidated` (`reason` do sistema: `estorno da compra`). Não movimenta conta. Se `totalLiquidated = 0`, não cria crédito.
2. `ACCOUNT` — exige `accountId` de conta **ativa** do usuário. Devolve `bankLiquidated` à conta como **fato persistido de devolução** (não é receita; não é linha em `incomes`; não reverte o pagamento da fatura; entra na fórmula de saldo — RN240). A parte `creditLiquidated` é **sempre** restaurada como crédito de cartão (esse valor nunca saiu da conta). Se `bankLiquidated = 0`, não há movimento bancário. Se `creditLiquidated = 0`, não cria crédito.

Efeitos comuns às duas opções:

- despesa → `REFUNDED` (não volta a `OPEN`);
- parcelas com liquidação, em fatura **não** `PAID` → `REFUNDED`;
- parcelas sem liquidação, em fatura **não** `PAID` → `CANCELLED`;
- parcelas em fatura `PAID` **não mudam de status** (fatura `PAID` é imutável); o benefício é só crédito e/ou devolução à conta;
- pagamentos de fatura e suas alocações **permanecem** (não reverse);
- aplicações de crédito já feitas **permanecem**; o novo crédito (se houver) é fato adicional e entra na aplicação automática RN246 na mesma transação;
- limite: compromissos não liquidados são liberados; o já liquidado já tinha sido liberado;
- remaining operacional da fatura `OPEN`/`CLOSED` = soma dos remainings das parcelas não `CANCELLED`/`REFUNDED`; se `CLOSED` e remaining = 0 → `PAID` na mesma transação (idempotente com o scheduler);
- após estorno, `total_amount − paid_amount` da fatura **pode divergir** de `remaining_amount`: `remaining_amount` é a dívida operacional; `paid_amount` continua sendo a soma dos pagamentos `ACTIVE` da fatura (histórico, não reescrito).

Proibido: refund de `OPEN`/`CANCELLED`/`REFUNDED`; cancel de `PARTIALLY_PAID`/`PAID`; `ACCOUNT` com conta inativa ou de outro usuário; alterar fatura `PAID`; usar `/pay` da despesa para liquidar `CREDIT_CARD`.

### Refund `ACCOUNT` / `NONE` e o campo `settlement`

O path `POST /api/v1/expenses/{id}/refund` é compartilhado. O DTO do request aceita estruturalmente `settlement` (e `accountId` quando aplicável) porque a Fase 9 exige esses campos para `CREDIT_CARD`.

Para despesas `ACCOUNT` ou `NONE`:

- o body permanece vazio como na Fase 7/8;
- `settlement` **não** é propriedade JSON desconhecida;
- a utilização de `settlement` é **proibida**;
- resposta: HTTP **400**, `code = BUSINESS_RULE_VIOLATION`, regra/mensagem `SETTLEMENT_NOT_ALLOWED`.

Não tratar esse caso como `VALIDATION_ERROR` por propriedade desconhecida.


## RN118 — Estorno

A lógica de estorno deve evitar duplicação de crédito ou redução indevida de dívida.


# 18. Boleto

## RN119 — Boleto

Despesa pode possuir número de boleto.


## RN120 — Boleto

Número de boleto deve ser opcional.


## RN121 — Boleto

O número deve poder ser copiado para facilitar o pagamento.


# 19. Despesas sem cartão

## RN122 — NONE

Despesas sem cartão podem permanecer abertas.

`expenses.account_id` permanece `null`. O pagamento posterior não altera `payment_method` nem preenche `expenses.account_id`.


## RN123 — Conta a pagar

Despesas NONE devem aparecer em contas a pagar quando estiverem abertas.


## RN124 — Pagamento

Quando despesas `NONE` forem pagas, o usuário informa a conta utilizada no `POST /pay`. Essa conta é gravada em `payments.account_id`.


## RN125 — Histórico

A forma original da despesa deve permanecer rastreável (`payment_method` não muda no pagamento).


# 19.1 Contrato da Fase 7 — Despesas simples

A Fase 7 implementa despesas **sem parcelamento funcional** e **sem cartão**. Formas operacionais: `ACCOUNT` e `NONE`.

O modelo físico já existente (`expenses`, `expense_installments`, `payments`) é reutilizado. Não criar tabela, coluna, enum, CHECK nem migration para esta fase.

`CREDIT_CARD`, faturas e parcelas N>1 permanecem **fora da Fase 7**. Cartão entra na Fase 9.


## RN208 — ACCOUNT na criação

`paymentMethod = ACCOUNT`:

- `account_id` obrigatório (conta do usuário, ativa);
- status inicial `OPEN`;
- **não** cria `payments`;
- **não** altera saldo;
- a parcela interna 1/1 nasce `OPEN`.

Ter conta associada não significa estar paga. Exemplo: aluguel com conta corrente e vencimento futuro permanece `OPEN` até o `POST /pay`.


## RN209 — NONE na criação e no pagamento

`paymentMethod = NONE`:

- `account_id` da despesa é `null` na criação e **permanece** `null` após o pagamento;
- `credit_card_id` é `null`;
- criação não altera saldo e não cria `payments`;
- no pagamento, `payments.account_id` identifica a conta usada;
- `payment_method` continua `NONE`.


## RN210 — Pagamento de despesa ACCOUNT (Fase 7 — SUPERADA na Fase 8)

**Vigência:** contrato da Fase 7. **SUPERADA** por RN228 na Fase 8.

Texto histórico da Fase 7:

Para despesa `ACCOUNT`, todo pagamento deve usar a mesma conta da despesa:

```text
payments.account_id == expenses.account_id
```

Rejeitar pagamento com conta diferente. Se o request omitir `accountId`, o backend utiliza `expenses.account_id`. Se informar `accountId`, o valor deve coincidir.

A implementação atual (Fase 7) ainda aplica esta regra. A implementação da Fase 8 deve deixá-la de aplicar e seguir RN228.


## RN211 — Parcela 1/1 (Fase 7)

Toda despesa possui pelo menos uma parcela (RN063, docs/23 §§185–186).

Na Fase 7 o backend cria internamente:

```text
installment_number = 1
total_installments = 1
amount = expenses.total_amount
due_date = expenses.due_date
invoice_id = null
status = OPEN
```

A API **não** expõe CRUD de parcelas nem campo `installments` na criação. O consumidor não precisa conhecer o ciclo de parcelamento.

A resposta da despesa inclui `installmentId` apenas para rastreabilidade. Pagamentos usam `POST /api/v1/expenses/{id}/pay`; o Service localiza a parcela única.

A Fase 8 expõe parcelas N>1, pagamento por parcela, edição cadastral de parcela `OPEN` e reverse de payment (seção 19.2). A Fase 7 não implementa esses comportamentos: o `PUT` da despesa `OPEN` atualiza a parcela 1/1 em conjunto com `total_amount` e `due_date` (RN217). Isso permanece o comportamento 1/1; não é a edição independente da Fase 8 (RN227).


## RN212 — Pagamento da Fase 7

Endpoint: `POST /api/v1/expenses/{id}/pay`.

Regras:

- somente despesas `OPEN` ou `PARTIALLY_PAID`;
- valor > 0;
- soma dos pagamentos da despesa (parcela 1/1) + novo valor ≤ valor devido (`expense_installments.amount`, igual a `total_amount` na Fase 7);
- conta do usuário, ativa;
- pagamento não pode deixar o saldo da conta negativo;
- operação atômica (`@Transactional`);
- lock pessimista (`PESSIMISTIC_WRITE`) na despesa e na parcela 1/1 **antes** de somar pagamentos e inserir o novo;
- ownership pelo SecurityContext; recurso de outro usuário: 404;
- internamente: inserir `payments` com `installment_id` da parcela 1/1; `payments.type` permanece `null` (sem significado nesta fase).

Status após o pagamento (despesa e parcela 1/1, juntos):

```text
OPEN → PARTIALLY_PAID   (0 < pago < devido)
OPEN → PAID             (pago = devido)
PARTIALLY_PAID → PAID   (pago acumulado = devido)
```

Múltiplos pagamentos parciais são permitidos enquanto a soma não ultrapassar o devido.

Rejeitar pagamento de `PAID`, `CANCELLED` ou `REFUNDED`.


## RN213 — Cancelamento de despesa (Fase 7)

Endpoint: `POST /api/v1/expenses/{id}/cancel`.

Permitido somente:

```text
OPEN → CANCELLED
```

Rejeitar cancelamento de `PARTIALLY_PAID`, `PAID`, `CANCELLED` e `REFUNDED`.

Despesa parcialmente paga ou paga não se “cancela”: o caminho de correção financeira é o estorno (RN214).

Efeitos:

- despesa e parcela 1/1 passam a `CANCELLED`;
- não existem pagamentos a desfazer (não havia `payments`);
- saldo inalterado;
- registro permanece.

Não há reativação nesta fase.


## RN214 — Estorno de despesa (Fase 7)

Endpoint: `POST /api/v1/expenses/{id}/refund`.

O estorno reverte movimentação financeira já realizada. **Não** é cancelamento e **não** replica `Income.reverse()`.

Permitido:

```text
PARTIALLY_PAID → REFUNDED
PAID → REFUNDED
```

Rejeitar estorno de `OPEN`, `CANCELLED` e `REFUNDED` (incluindo segundo `refund` sobre a mesma despesa).

Resultado terminal: `REFUNDED`. A despesa **não** volta a `OPEN`.

A parcela 1/1 também passa a `REFUNDED`. **Este efeito na parcela aplica-se somente à despesa 1/1 da Fase 7.**

Para N>1 aplicar **exclusivamente** RN237: despesa → `REFUNDED`; parcela com payment `ACTIVE` → `REFUNDED`; parcela sem payment `ACTIVE` → permanece `OPEN` (somente consultável). Nenhuma parcela sem payment ACTIVE é marcada `REFUNDED` só porque a despesa foi refundada.

Não existe `POST /api/v1/payments/{id}/reverse` **na Fase 7**. O estorno da Fase 7 é da despesa inteira (todos os pagamentos da parcela 1/1).

A previsão de “fase futura” para reverse por pagamento foi **SUPERADA**: o reverse entra no contrato da Fase 8 (RN238). Não usar `payments.type`. Não replicar `Income.reverse()`.


## RN215 — Histórico de pagamentos no estorno

O estorno **não apaga** linhas de `payments`.

Identificação dos pagamentos originais: todas as linhas de `payments` da despesa (parcela 1/1), identificadas por `expense_id` / `installment_id`.

Registro da reversão: o fato de estorno é o status `REFUNDED` da despesa e da parcela. Não se cria pagamento negativo. Não se usa `payments.type` (valores oficiais inexistentes — docs/23 §269.1). Não se adiciona coluna de estorno nesta fase.

Recálculo do saldo: pagamentos cuja despesa está `REFUNDED` (ou `CANCELLED`) **não** entram na soma que reduz o saldo (RN216). O efeito financeiro do débito original deixa de ser considerado; o saldo da conta volta ao valor que teria sem aqueles pagamentos.

Duplo estorno: despesa já `REFUNDED` rejeita novo `refund`.

Atomicidade: a mesma transação valida o status, aplica `REFUNDED` na despesa e na parcela e confirma. Falha em qualquer etapa implica rollback. Lock pessimista na despesa e na parcela.

Consulta `GET /expenses/{id}/payments` continua devolvendo os pagamentos originais após o estorno.


## RN216 — Saldo da conta a partir da Fase 7

Fórmula da Fase 7 (código atual):

```text
saldo atual =
  initial_balance
+ SUM(incomes.amount WHERE status = RECEIVED AND account_id = conta)
− SUM(payments.amount WHERE account_id = conta
      AND a despesa do pagamento NÃO está em CANCELLED nem REFUNDED)
```

Pagamento de despesa reduz o saldo.

Estorno de despesa (`REFUNDED`) desfaz o efeito desses pagamentos no saldo (deixam de ser subtraídos).

A Fase 8 **emenda** esta fórmula (RN240): o subtraendo restringe-se a payments **`ACTIVE`**. Payments **`REVERSED`** não entram. Adjustments de parcela não entram. Refund continua excluindo os payments da despesa `REFUNDED`. A **Fase 14** inclui transferências `ACTIVE` e acertos de saldo `ACTIVE` (`BALANCE_ADJUSTMENT` / RN204) - ver §19.5 e RN240 emendada. Não persistir `current_balance`.


## RN217 — Edição de despesa (Fase 7)

Somente despesa `OPEN` pode ser editada (`PUT /api/v1/expenses/{id}`).

Campos do contrato de criação (substituição): `categoryId`, `description`, `totalAmount`, `expenseDate`, `dueDate`, `paymentMethod`, `accountId`, `responsibleType`, `responsibleName`, `barcode`, `notes`. `installmentCount` **não** faz parte do `PUT` (quantidade imutável).

Regras no `PUT` de `OPEN`:

- `paymentMethod` somente `ACCOUNT` ou `NONE`;
- `ACCOUNT` exige `accountId` (conta ativa do usuário); `NONE` exige `accountId` ausente/nulo;
- troca `ACCOUNT` ↔ `NONE` é permitida enquanto não houver pagamento, respeitando a regra de conta;
- categoria `EXPENSE`, ativa, do usuário;
- valor > 0;
- a parcela 1/1 é atualizada (`amount`, `due_date`) para permanecer igual à despesa.

Rejeitar `PUT` de `PARTIALLY_PAID`, `PAID`, `CANCELLED` e `REFUNDED`. Não alterar silenciosamente valor, conta ou forma depois que existir pagamento. Correção de despesa já paga: `refund` (terminal `REFUNDED`) e nova despesa, se necessário.

Não alterar `status`, `id`, `userId`, `createdAt` via body. Propriedades desconhecidas: 400.

Este `PUT` é cadastral, **não** é operação financeira. Payment, adjustment, reverse, refund e cancel **não** alteram o total original.

Exceção cadastral 1/1: enquanto a despesa estiver `OPEN` e for 1/1, o `PUT` pode alterar `totalAmount` (e a parcela única, para a soma continuar igual) — RN245. N>1: não redistribuir automaticamente; quantidade imutável; alteração de `amount` de parcela segue RN227.


## RN218 — OVERDUE derivado na API

`OVERDUE` não é status persistido nem valor de CHECK. Timezone: `America/Sao_Paulo`.

**Despesa 1/1** (Fase 7 e Fase 8):

```text
overdue = true quando
  status IN (OPEN, PARTIALLY_PAID)
  AND expenses.dueDate < hoje
```

Equivale à única parcela (RN241). `PAID`, `CANCELLED` e `REFUNDED` nunca são overdue.

**Despesa N>1** (Fase 8):

```text
overdue = true quando existe pelo menos uma parcela overdue segundo RN241
```

Não determinar overdue de N>1 somente por `expenses.due_date`.

A resposta HTTP da despesa inclui `overdue` (boolean). Não criar coluna.


## RN219 — Concorrência de pagamento

Dois `POST /pay` simultâneos não podem fazer a soma ultrapassar o devido (RN167, RN168).

A verificação `pago_acumulado + novo_pagamento <= devido` ocorre **dentro** da transação, após o lock pessimista da despesa e da parcela 1/1.


## RN220 — CREDIT_CARD fora da Fase 7

A Fase 7 rejeita `paymentMethod = CREDIT_CARD` na criação e na edição. Cartão, fatura e ciclo pertencem à Fase 9.

O enum/CHECK físico `CREDIT_CARD` permanece no banco; a API da Fase 7 não o opera.


## RN221 — payments.type na Fase 7

O campo `payments.type` permanece sem valores oficiais. Na Fase 7 é persistido como `null`. Não criar enum, CHECK, constante nem regra sobre `type`. Pendência: docs/23 §269.1.

O estado ACTIVE/REVERSED do payment **não** usa `type`. Na Fase 8 o estado é `payments.status` (RN230).


# 19.2 Contrato da Fase 8 — Parcelamento, payments, adjustments e reverse

Contrato oficial da Fase 8. **Implementado.**

A Fase 8 não é só geração de parcelas. Abrange: despesas parceladas; geração determinística; vencimentos mensais; edição cadastral de parcela `OPEN`; pagamento por parcela; múltiplos payments; payments em contas diferentes; discounts e surcharges; reverse de payment e de adjustment; status persistido de payment e de adjustment; status agregado persistido da despesa; cancelamento; refund (incluindo refund misto); overdue da parcela; preservação de fatos; preparação dos fatos para relatórios futuros.

Fora da Fase 8: `CREDIT_CARD`, fatura, ciclo, rateio, valores de `payments.type`, dashboards, gráficos, PDF e relatórios de apresentação. Cartão, fatura, ciclo e rateio passam ao contrato da **Fase 9**.

Modelo conceitual:

```text
EXPENSE (obrigação)
  └── INSTALLMENTS (partes da obrigação)
        ├── PAYMENTS (movimentação financeira)
        └── ADJUSTMENTS (alteração da obrigação)
```

Obrigação ≠ movimentação financeira ≠ ajuste da obrigação.

O código da Fase 7 (parcela 1/1) permanece válido quando `installmentCount` é 1. Regras da Fase 7 que assumem exclusivamente `installmentNumber = 1` são generalizadas abaixo. Regras SUPERADAS estão marcadas (RN210, reverse “futuro”, fórmula RN216 sem `payments.status`).


## RN222 — Escopo e fatos da Fase 8

A implementação da Fase 8 reutiliza `expenses`, `expense_installments` e `payments`. Introduz adjustments da parcela e `payments.status`. Não cria colunas derivadas (`paid_amount`, `remaining_amount`, `discount_total`, `surcharge_total`, `current_balance`, `early_payment_savings`). `status` de despesa, parcela, payment e adjustment é persistido.

Relatórios futuros devem poder calcular, a partir dos fatos: total original; descontos; acréscimos; valor pago; saldo; payments por conta; facts revertidos; valores por período e categoria. A Fase 8 não implementa as telas nem os endpoints de relatório.


## RN223 — Quantidade de parcelas

A quantidade é definida na criação (`installmentCount`). Se omitido: `installmentCount = 1` (compatível com a Fase 7). Se informado: deve ser `> 0`. Não é propriedade JSON desconhecida.

Após a criação: não alterar a quantidade; não redistribuir automaticamente; não criar nem excluir parcelas automaticamente.


## RN224 — Geração dos valores

Na criação, a soma dos `expense_installments.amount` deve ser exatamente `expenses.total_amount`.

Residual de centavos: **primeira** parcela. `RoundingMode.HALF_UP`, escala 2.

Exemplo: R$ 1.000,00 / 3 → 333,34 + 333,33 + 333,33.


## RN225 — Vencimentos mensais

A primeira parcela usa a `dueDate` informada na criação. As seguintes são mensais. A data da compra (`expense_date`) **não** participa do vencimento.

Dia-base = dia da `dueDate` original. Se o mês não tiver esse dia, usar o último dia **daquele** mês. Não carregar o dia ajustado para os meses seguintes.

Exemplo, dia-base 31: 31/01, 28/02, 31/03, 30/04, 31/05.


## RN226 — `expenses.due_date`

É o vencimento da **primeira** parcela. Os vencimentos reais ficam em `expense_installments.due_date`. Em 1/1 os dois coincidem. Não usar `expenses.due_date` como vencimento de todas as parcelas.

Na listagem `GET /expenses`, `startDate`/`endDate` consideram as datas das parcelas: a despesa entra no intervalo quando **pelo menos uma** parcela tem `due_date` no intervalo. Em 1/1 o efeito é equivalente a filtrar por `expenses.due_date`.


## RN227 — Edição cadastral da parcela

Somente parcela `OPEN`. Operação própria da parcela (não é o `PUT` da despesa N>1).

Campos permitidos: `amount`, `due_date`.

Alterar `amount` só se `SUM(all installment.amount) = expenses.total_amount`. Caso contrário: rejeitar, rollback, nenhum valor parcial.

Sem redistribuição automática. Alterar `due_date` de uma parcela não altera as demais.

`PAID`, `PARTIALLY_PAID`, `CANCELLED` e `REFUNDED` não podem ser editadas assim.

Essa edição não é payment, adjustment, refund nem reverse.


## RN228 — Conta de referência (substitui RN210)

`expenses.account_id` é conta de **referência/preferência** quando aplicável (`ACCOUNT`). **Não** obriga os payments a usar a mesma conta.

Cada payment tem `payments.account_id` da conta que movimentou o dinheiro. A conta deve pertencer ao usuário autenticado e estar ativa.

Uma despesa ou uma parcela pode ter payments de contas diferentes.

RN210 (Fase 7) está **SUPERADA** por esta regra.


## RN229 — Pagamento por parcela

Payment pertence obrigatoriamente a uma parcela (`payments.installment_id`). Uma parcela pode ter múltiplos payments. Payment não altera `installment.amount` nem `expenses.total_amount`.

N>1: o pagamento deve identificar a parcela. `POST /api/v1/expenses/{id}/pay` permanece para **1/1**. Para N>1 não escolher parcela implicitamente.

Overpayment (valor > remaining da parcela) é rejeitado antes de persistir.


## RN230 — `payments.status`

Não usar `payments.type` para estado.

Coluna de contrato: `payments.status`. Valores oficiais: `ACTIVE`, `REVERSED`.

Novo payment: `ACTIVE`. Reverse: `ACTIVE` → `REVERSED`. `REVERSED` permanece no histórico; não se edita; não se exclui; não movimenta saldo. `ACTIVE` participa do saldo.

`payments.type` permanece sem semântica na Fase 8 (§269.1).


## RN231 — Obrigação e saldo da parcela

`installment.amount` é o valor **original**.

```text
obligation =
    installment.amount
  + SUM(active surcharges)
  − SUM(active discounts)

remaining =
    obligation
  − SUM(active payments)
```

Somente fatos `ACTIVE`. Remaining nunca negativo. Payment não pode exceder remaining.

**Invariável de obligation:** `obligation` não pode ser negativa. Criar `DISCOUNT` (ou reverter `SURCHARGE`) que deixaria `obligation < 0`, ou `obligation < SUM(active payments)`, deve ser rejeitado com rollback. Não “corrigir” silenciosamente o total, o `installment.amount` nem o adjustment.

**Alteração cadastral e obligation (Fase 8):** o `PUT` da despesa `OPEN` 1/1 (RN217 / RN245) e a edição cadastral de `amount` de parcela `OPEN` (RN227) continuam cadastrais — não apagam nem redistribuem adjustments. Porém, após a alteração, o `obligation` resultante **deve** permanecer `>= 0` e `>= SUM(active payments)`. Se a alteração produzir `obligation` inválida, rejeitar a operação com rollback integral. Nenhum adjustment é alterado, removido ou compensado automaticamente.


## RN232 — Adjustment

Relacionamento: `expense_installments` 1:N adjustments.

Adjustment altera a **obrigação**. Não movimenta saldo de conta. Não é payment. Nunca representar desconto como payment negativo.

Tipos oficiais iniciais: `DISCOUNT` (reduz), `SURCHARGE` (aumenta). O `amount` do adjustment é sempre positivo.

Desconto por antecipação = `DISCOUNT`. Acréscimo por atraso = `SURCHARGE`. Não criar enumeração específica por motivo (juros vs multa vs tarifa vs outros). A partir da Fase 9 (**concluída**), o request HTTP inclui `type`, `amount` e **`reason` obrigatório** (texto). A ausência de `reason` na Fase 8 está **SUPERADA** pelo contrato vigente.

Ajuste de **parcela** altera a obligation da parcela (RN231). Ajuste de **fatura** participa do rateio (RN247A).

Tabela física: plural snake_case, filha da parcela, com `user_id` e FK composta de ownership. O nome exato segue a convenção do projeto na migration da implementação; não criar módulo genérico `adjustments` desligado de `expenses`.

**Criação (elegibilidade):** despesa **não** `CANCELLED`/`REFUNDED`; parcela **não** `CANCELLED`/`REFUNDED`. **Não** se exige parcela `OPEN`: `PARTIALLY_PAID` e `PAID` podem receber adjustment enquanto a despesa estiver ativa **e**, se a parcela tiver `invoice_id`, a fatura **não** estiver `PAID`, desde que a invariável de `obligation` (RN231) seja respeitada. Parcela `OPEN` de despesa `REFUNDED` não recebe adjustment (RN237). Fatura `PAID`: nenhum ajuste (parcela ou fatura).

HTTP: `POST` / `GET` em `/api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments` — detalhe em `docs/25` seção 47.


## RN233 — Status do adjustment

Fato histórico. Não apagar nem editar o valor. Estados persistidos: `ACTIVE`, `REVERSED`. Nasce `ACTIVE`. Reverse: `ACTIVE` → `REVERSED`. `REVERSED` não participa da obrigação e não pode ser revertido de novo.


## RN234 — Payment e adjustment atômicos

Quando a operação exigir adjustment + payment no mesmo ato (ex.: DISCOUNT 40 + PAYMENT 550), ambos na mesma transação. Não persistir metade.

**HTTP:** não há endpoint composto nesta consolidação. Cada `POST .../adjustments` e cada pagamento são atômicos isoladamente. Endpoint composto, se necessário no futuro, exige decisão explícita.


## RN235 — Status persistido da parcela e da despesa

Parcela e despesa mantêm status persistido. Recalcular e gravar na mesma transação do fato. Não depender só de calcular o status em cada SELECT.

Agregação da despesa (estados financeiros normais):

- todas as parcelas `OPEN` → despesa `OPEN`;
- alguma quitada e alguma pendente → `PARTIALLY_PAID`;
- todas quitadas → `PAID`.

Parcela **quitada** = `remaining = 0`. Em `CREDIT_CARD`, o remaining vem das alocações de fatura/crédito/ajuste (RN247), não de `payments` da despesa.

Terminais: `CANCELLED`, `REFUNDED` (conforme RN236, RN237 e RN117).


## RN236 — Cancelamento (Fase 8)

Somente despesa `OPEN` → `CANCELLED`. Não movimenta dinheiro. Em `ACCOUNT`/`NONE`: todas as parcelas (necessariamente `OPEN`, sem payment) passam a `CANCELLED` na mesma transação.

`PARTIALLY_PAID` e `PAID` **não** podem ser canceladas. Havendo movimentação financeira, a operação é refund.

Em `CREDIT_CARD`, o cancelamento de `OPEN` segue RN117 (parcelas em fatura não `PAID` → `CANCELLED`; sem crédito; sem movimento bancário).


## RN237 — Refund misto

Refund é da despesa inteira. Sem refund individual de parcela.

Em `ACCOUNT`/`NONE` (Fase 8): havendo payment **ACTIVE**, despesa → `REFUNDED`. Parcelas com payment ativo → `REFUNDED`. Parcelas sem payment → permanecem `OPEN` **somente como histórico**. Payments não são apagados nem editados. A despesa `REFUNDED` não produz efeito no saldo da conta (os payments deixam de ser subtraídos).

Parcela `OPEN` de despesa `REFUNDED` (`ACCOUNT`/`NONE`): não recebe payment; não recebe adjustment; não é alterada; não é cancelada; apenas consulta. Nenhuma parcela sem payment `ACTIVE` é marcada `REFUNDED` só porque a despesa foi refundada.

Em `CREDIT_CARD`, o refund segue RN117 (`settlement` `CARD_CREDIT` ou `ACCOUNT`). Não usa `payments` da despesa.


## RN238 — Reverse de payment

Endpoint: `POST /api/v1/payments/{id}/reverse`.

`ACTIVE` → `REVERSED`. Já `REVERSED`: rejeitar. Não apagar. Desfaz o efeito daquele payment no saldo da conta e no remaining da parcela. Atualizar status da parcela e da despesa na mesma transação.

Não altera `installment.amount` nem `expenses.total_amount`.

**Proibido** se a despesa estiver `REFUNDED` ou `CANCELLED`.

A menção documental anterior a reverse como “fase futura” está **SUPERADA**.


## RN239 — Reverse de adjustment

Endpoint:

```text
POST /api/v1/expenses/{expenseId}/installments/{installmentId}/adjustments/{adjustmentId}/reverse
```

Body: vazio. Response: fato com `status = REVERSED` (mesmo shape da criação; sem `reversedAt` — campo inexistente no modelo).

`ACTIVE` → `REVERSED`. Já `REVERSED`: rejeitar. Não apaga o adjustment nem altera `amount`, `type` ou `createdAt`; o histórico permanece. Remove o efeito da obrigação. Atualizar status da parcela e da despesa quando necessário. Não movimenta saldo de conta.

Mesma filosofia de RN238: permitido somente enquanto a despesa estiver ativa. **Proibido** se a despesa estiver `CANCELLED` ou `REFUNDED`.

Ownership: adjustment do usuário e da parcela/despesa do path; mismatch → **404** (sem distinguir). A consulta do histórico (`GET .../adjustments`) permanece permitida após terminal da despesa.


## RN240 — Saldo da conta (emenda da RN216)

```text
saldo atual =
  initial_balance
+ SUM(incomes.amount WHERE status = RECEIVED AND account_id = conta)
+ SUM(devoluções ACCOUNT de compra no cartão - RN117 - nesta conta)
− SUM(payments.amount WHERE account_id = conta
      AND payments.status = ACTIVE
      AND a despesa do pagamento NÃO está em CANCELLED nem REFUNDED)
− SUM(credit_card_invoice_payments.amount WHERE account_id = conta
      AND status = ACTIVE)
+ SUM(transfers.amount WHERE destination_account_id = conta AND status = ACTIVE)
− SUM(transfers.amount WHERE source_account_id = conta AND status = ACTIVE)
+ SUM(account_balance_adjustments.adjustment_amount WHERE account_id = conta AND status = ACTIVE)
```

Somente payments `ACTIVE` de despesa `ACCOUNT`/`NONE` (despesa não `CANCELLED`/`REFUNDED`) e pagamentos `ACTIVE` de fatura movimentam o saldo da conta no sentido de saída por esses domínios. `REVERSED` não movimenta. Adjustments de **parcela/fatura** não movimentam saldo de conta. Crédito de cartão **não** movimenta conta. Refund de despesa `ACCOUNT`/`NONE` remove o efeito dos payments daquela despesa. Refund `CREDIT_CARD` com `settlement = ACCOUNT` **soma** o fato de devolução (`bankLiquidated`); **não** reverte os pagamentos da fatura.

**Fase 14:** transferências `ACTIVE` e acertos de saldo `ACTIVE` (`BALANCE_ADJUSTMENT` / tabela `account_balance_adjustments`) entram na fórmula; `REVERSED` não entra. Sem `current_balance` persistido.

**Leitura do saldo (`GET /accounts/{id}/balance`):** é cálculo derivado sob demanda (saldo atual). O contrato da Fase 14 exige capacidade **interna** de saldo **as-of-date** para acerto retroativo; **não** obriga expor data nesse GET nesta fase. O contrato **não** exige `SELECT FOR UPDATE` da conta apenas para essa leitura. A proteção contra saldo negativo (RN076A) e a concorrência de escrita (RN244) aplicam-se às **operações** que criam/reverterem facts financeiros (pay, reverse, transfer, balance adjustment, etc.), com locks nas entidades financeiras envolvidas, no padrão das fases anteriores. Não introduzir lock explícito de conta só para GET de saldo sem decisão arquitetural futura.


## RN241 — Overdue da parcela

Não persistir `OVERDUE`. Condição derivada:

```text
status da parcela IN (OPEN, PARTIALLY_PAID)
AND remaining > 0
AND due_date < reference_date
AND despesa NOT IN (CANCELLED, REFUNDED)
```

`PAID`, `CANCELLED` e `REFUNDED` nunca são overdue. Timezone: `America/Sao_Paulo`. A API operacional usa a data atual como `reference_date`. Recortes históricos explícitos ficam para relatórios futuros.

Overdue da **despesa** N>1: `true` quando existe pelo menos uma parcela overdue segundo esta regra (RN218). Não usar somente `expenses.due_date`.


## RN242 — Integridade de parcelas

Na implementação: `UNIQUE (expense_id, installment_number)` em **nova** migration. Não alterar V8–V10. UUID v7; `NUMERIC(19,2)`; Flyway; `ddl-auto=validate`; ownership; FKs compostas; sem `ON DELETE CASCADE` nas relações financeiras atuais; `payments.installment_id` obrigatório.


## RN243 — Cartão fora da Fase 8

Na Fase 8: rejeitar `CREDIT_CARD` como na Fase 7 (RN220). Não criar faturas, não preencher `invoice_id`, não implementar ciclo nem rateio.

A Fase 9 **implementa** cartão, fatura, ciclo e rateio. A pergunta do §269.2 sobre edição cadastral de parcela **já em fatura** permanece **DEFERIDA**.


## RN244 — Concorrência (Fase 8)

Operações sobre a mesma parcela são transacionais. Dois pagamentos concorrentes não ultrapassam o remaining. Proteger consistência entre payments, adjustments, status da parcela e da despesa. Proteger a conta contra saldo negativo (RN076A). Não enfraquecer locking para “passar teste”.

Locks previstos nas operações de escrita sobre a parcela: despesa → parcela → consultar fatos ACTIVE → persistir. Em reverse de adjustment: despesa → parcela → adjustment → validar `ACTIVE` → persistir `REVERSED`. O `GET` de saldo da conta permanece leitura derivada sem lock de conta (RN240). Race teórica entre pagamentos de **despesas diferentes** na mesma conta sem lock de conta é a mesma ressalva não bloqueante já registrada na Fase 7 (`docs/28` §46.1); não reabre decisão nesta fase.


## RN245 — `total_amount` após a criação

`expenses.total_amount` é o original histórico. **Não** é operação financeira. Payment, adjustment, reverse, refund e cancel **não** o alteram.

Exceção cadastral já existente (não é fato financeiro):

- enquanto a despesa estiver `OPEN` **e** for 1/1, o `PUT` da despesa (RN217) pode alterar o total (e a parcela única, para a soma continuar igual);
- N>1: quantidade imutável; sem redistribuição automática; alteração de `amount` de parcela segue RN227 (`SUM = total_amount`).

Quando existirem adjustments `ACTIVE` na parcela 1/1, o `PUT` que altera o total deve respeitar a invariável de `obligation` (RN231): rejeitar se o total novo produzir `obligation < 0` (ou `obligation < SUM(active payments)`). Não alterar, apagar nem redistribuir adjustments para “fazer fechar”.


# 19.3 Contrato da Fase 9 — Cartões de crédito (fase expandida)

Contrato oficial da Fase 9. **Implementado** e **concluído** (ver `docs/28`).

A Fase 9 absorve o que o roadmap anterior distribuía entre as Fases 9–12, mais as decisões desta consolidação: cadastro e manutenção de cartões; limite derivado; compras `CREDIT_CARD`; parcelamento vinculado a faturas; ciclo; status `SCHEDULED`/`OPEN`/`CLOSED`/`PAID`; fechamento automático (scheduler Spring); pagamento de fatura; rateio persistido; liberação de limite; créditos de cartão; ajustes com `reason`; reverse de pagamentos de fatura; cancelamento/estorno de compra no cartão (RN117 / §269.4 **fechado**).

Fora da Fase 9: parcelamento/negociação/renegociação de fatura (**Fase 13**, §19.4 — `CONCLUÍDA E APROVADA`); relatórios/PDF; frontend financeiro; Refresh Token; `payments.type`; auditoria genérica; edição de parcela já em fatura (§269.2.7); `POST /invoices/{id}/close`.

A RN029A (recusar compra acima do limite) está **SUPERADA**. `PARTIALLY_PAID` como status de **fatura** está **SUPERADO**. O §269.3 (rateio) está **fechado** (RN247). O §269.4 (estorno de compra no cartão já liquidada) está **fechado** (RN117).

Decisões finais da Fase 9 (fechadas e implementadas):

1. cancelamento vs refund de compra no cartão, opções `CARD_CREDIT` / `ACCOUNT` e efeitos — RN117;
2. ordem das faturas na aplicação automática de créditos (FIFO dos créditos inalterado) — RN246;
3. mês do `due_date` da fatura quando `due_day` ≤ `closing_day` — RN099B;
4. desempate do rateio: `due_date` ASC, depois `id` da parcela ASC — RN247 passo 3;
5. refund `ACCOUNT`/`NONE` + `settlement` → **400** `BUSINESS_RULE_VIOLATION` (`SETTLEMENT_NOT_ALLOWED`) — não é propriedade desconhecida;
6. `SURCHARGE` de fatura exige `remaining > 0` — RN247A (**400**, `BUSINESS_RULE_VIOLATION`, `SURCHARGE_REQUIRES_REMAINING`);
7. `GET /credit-cards/{id}/credits` → array com `remainingAmount` por crédito; total disponível = `SUM(remainingAmount)` (derivado; sem envelope).


## RN246 — Crédito de cartão

Crédito pertence ao **cartão**, não à conta bancária e não à fatura.

- não movimenta conta;
- não aumenta `credit_limit` nem `available_limit`;
- não cria fatura;
- não expira automaticamente;
- pode ser parcial;
- nunca fica negativo;
- possui histórico (não apagar);
- pode originar-se de estorno de compra no cartão (`settlement = CARD_CREDIT`, ou a parte `creditLiquidated` de `ACCOUNT`) ou de lançamento manual;
- crédito manual exige `amount` e `reason`.

Aplicação: **automática**, na mesma transação da operação que cria remaining liquidável ou crédito disponível (pagamento de fatura, crédito novo, fechamento, estorno que gera crédito). O usuário não escolhe qual crédito aplicar.

**FIFO dos créditos:** créditos disponíveis (ainda não totalmente aplicados), do mais antigo para o mais novo (`created_at` ASC, empate `id` ASC).

**Ordem das faturas elegíveis:** status `OPEN` ou `CLOSED`, `remaining > 0`. `SCHEDULED`, `PAID` e `SETTLED_BY_AGREEMENT` não são elegíveis. Ordenar por `due_date` ASC, depois `id` ASC.

Percorrer créditos em FIFO; para cada crédito, percorrer faturas na ordem acima até esgotar o crédito ou as faturas. Cada aplicação usa o rateio RN247 sobre a fatura alvo. Crédito maior que o remaining da fatura: fatura remaining 0; sobra segue para a próxima fatura elegível ou permanece crédito. Nunca remaining negativo. Nunca cria fatura.

Se o crédito **já foi utilizado** (qualquer aplicação ACTIVE), a origem **não** pode ser desfeita. Não reconstruir faturas retroativamente.

Na Fase 9 **não há** reverse de crédito nem reverse do refund da despesa `CREDIT_CARD` (`REFUNDED` é terminal). Crédito não aplicado permanece disponível até ser consumido pela aplicação automática.


## RN248 — Concorrência da Fase 9

Operações críticas (compra no cartão, pagamento de fatura, aplicação de crédito, fechamento, ajuste de fatura, reverse, cancel/refund RN117) são transacionais. Locks pessimistas nas entidades envolvidas (cartão, fatura, parcelas, conta, créditos). Evitar dupla liquidação e duplo fechamento. Idempotência do scheduler (RN096A).


# 19.4 Contrato da Fase 13 — Parcelamento, Negociação e Renegociação de Fatura

**Status:** `CONCLUÍDA E APROVADA`.

A base da Fase 13 (D1–D11) e a **emenda de consolidação da renegociação** (RN254) estão implementadas, testadas e auditadas.

Decisões D1–D11 **fechadas** (2026-08-16) — **não reabertas**. Autoridade: `AGENTS.md` §28 → esta seção → `docs/23` §269.5 → API/testes.

---

## 19.4.1 Objetivo

Controle pessoal de:

- parcelamento de fatura **fechada**;
- **nova negociação** (Agreement da fatura atual, sem antecipar Agreements anteriores);
- **renegociação** (consolidação da fatura atual + futuros líquidos dos Agreements `ACTIVE` do cartão + novo Agreement) — RN254;
- entrada imediata em conta;
- nova obrigação `expenses` `CREDIT_CARD` + parcelas em faturas futuras;
- antecipação individual de parcelas de Agreement (pagamento parcial / quitação com desconto);
- histórico completo (fatura → Agreement → renegociação → …).

Fora do escopo: IOF, composição bancária de juros, plano de contas, DRE, contabilização bancária, cálculo “real” de juros do emissor, cancelamento de Agreement (`CANCELLED` reservado — D10).

---

## 19.4.2 Conceito fundamental

```text
COMPRA ORIGINAL  ≠  PARCELA DE NEGOCIAÇÃO (Agreement)
```

Parcelar **não** transforma compras/parcelas originais. A fatura é liquidada pela negociação; nasce **nova** obrigação (`expenses` + `expense_installments`) ligada ao Agreement. Distinção obrigatória em modelo, API, histórico e relatórios (RN110).

---

## 19.4.3 Elegibilidade, entrada e plano (comum)

- Somente fatura **`CLOSED`** com `remaining > 0` (`invoiceRemaining`).
- `OPEN` / `SCHEDULED` / `PAID` / `SETTLED_BY_AGREEMENT` → rejeitar.
- Entrada: paga imediatamente na conta; **não** é parcela 0/N; **não** reentra no Agreement.
- `0 <= entryAmount < invoiceRemaining`.
- `entryAmount == invoiceRemaining` → **400** `BUSINESS_RULE_VIOLATION` (usar `POST /api/v1/invoices/{id}/payments`) — **D6=A**.
- `entryAmount > invoiceRemaining` → rejeitar.
- Plano (**D9=A**): `installmentCount` (> 0) + `installmentAmount` (> 0); parcelas **iguais**; `due_date` pelo ciclo (1ª na **próxima** fatura).
- `contractedTotal = installmentCount × installmentAmount`.
- **Invariante (nova negociação e renegociação):** `contractedTotal >= financedAmount`. Se `contractedTotal < financedAmount` → **400** `BUSINESS_RULE_VIOLATION` (razão específica, ex. mensagem alinhada a `AGREEMENT_CONTRACTED_TOTAL_BELOW_FINANCED_AMOUNT`). Garante `additionalCost >= 0`.
- Custo adicional = `contractedTotal − financedAmount`; % de acréscimo = fração `additionalCost / financedAmount` (exibição).
- RN113 **SUPERADA** (contracted pode ser `>` financed; **não** pode ser `<`).

### financedAmount — duas fórmulas

| Operação | Fórmula |
| --- | --- |
| **Nova negociação** | `financedAmount = invoiceRemaining − entryAmount` |
| **Renegociação** | `financedAmount = (invoiceRemaining − entryAmount) + anticipatedFuturesNetAmount` (RN254) |

**Não** aplicar a fórmula da nova negociação à renegociação.

Exemplo canônico (nova negociação): remaining 1.000; entrada 400; financed 600; 10 × 120 → contracted 1.200; custo 600; acréscimo 100%.

---

## 19.4.4 Primeira parcela na próxima fatura

Parcela 1/N do Agreement **não** pertence à fatura negociada. Entra na **próxima** fatura do cartão (ciclo seguinte), como `expense_installments.invoice_id` da despesa do Agreement.

---

## 19.4.5 Pagamento de fatura aberta (Fase 9 reafirmado)

- `OPEN` + remaining 0 → continua `OPEN` até fechamento → `PAID`.
- `PayInvoiceRequest` **sem** `settled`.
- Reutilizar `POST /api/v1/invoices/{id}/payments`.

---

## 19.4.6 Status da fatura e settlement (D1=A, D2=A)

### Status persistidos

`SCHEDULED` | `OPEN` | `CLOSED` | `PAID` | `SETTLED_BY_AGREEMENT`

- `PAID` / `SETTLED_BY_AGREEMENT`: terminais; imutáveis.
- Ambos fora de elegibilidade de crédito (RN246) e de alteração.

### Settlement da fatura

Além da entrada (`credit_card_invoice_payments`), o valor **`invoiceSettlementAmount = invoiceRemaining − entryAmount`** é liquidado por um **fato de settlement** (D2) rateado às parcelas da **fatura atual** (espírito RN247), **sem** movimentar conta.

- Na **nova negociação**: `invoiceSettlementAmount = financedAmount`.
- Na **renegociação**: `invoiceSettlementAmount` liquida **só a fatura**; o trecho dos futuros líquidos entra no `financedAmount` do Agreement **sem** passar pelo settlement da fatura (RN254).

Após entrada + settlement da fatura: remaining da fatura = 0 → `SETTLED_BY_AGREEMENT`. Compras originais **não** são reescritas como parcelas do Agreement; remainings do ciclo caem a 0 via alocações de entrada + settlement.

---

## 19.4.7 Natureza da obrigação e limite (D3=A, D4=A, D11)

### Obrigação (D3=A)

Na mesma transação:

1. pagamento de entrada (`credit_card_invoice_payments`) + rateio;
2. fato de settlement de `invoiceSettlementAmount` + rateio;
3. fatura → `SETTLED_BY_AGREEMENT`;
4. (renegociação) tratamento dos futuros — RN254;
5. cabeçalho **Agreement** (histórico/vínculos);
6. nova `expenses` com `payment_method = CREDIT_CARD`, `total_amount = contractedTotal`, cartão da fatura;
7. `expense_installments` 1..N (valores iguais a `installmentAmount`), 1ª na próxima fatura.

Agreement **não** é cadastro CRUD genérico. Ligação obrigatória Agreement ↔ expense ↔ fatura de origem.

### Modelo legado V13 (D4=A)

`credit_card_invoice_installments` está **SUPERADO** como contrato de negócio. Novas tabelas de Agreement (+ settlement). V13 permanece no schema sem uso de negócio na Fase 13.

### Limite (D11 = total contratado)

- remaining das compras do ciclo liberado (entrada + settlement) → reduz `used_limit`;
- na renegociação, futuros encerrados também deixam de comprometer;
- a nova despesa do Agreement compromete o **`contractedTotal`** (soma dos remainings das novas parcelas), **não** o `financedAmount`.

RN029A inalterada (limite negativo permitido).

---

## 19.4.8 Nova negociação vs renegociação (D8=A)

### Nova negociação — `POST /api/v1/invoices/{invoiceId}/agreements`

- Negocia **somente** a fatura atual.
- **Não** antecipa Agreements anteriores.
- Agreements `ACTIVE` do cartão continuam `ACTIVE`.
- Coexistência permitida.
- `financedAmount = invoiceRemaining − entryAmount`.

### Renegociação — `POST /api/v1/invoices/{invoiceId}/renegotiations`

Detalhe determinístico: **RN254**.

Resumo:

- Escopo (**D8=A**): **todos** os Agreements `ACTIVE` do **mesmo cartão**.
- Request inclui `anticipatedFuturesNetAmount` (líquido dos futuros informado pelo banco). **Sem** lista de `agreementId`s.
- Parcela já na fatura atual: **uma vez** (entrada + settlement); **não** duplicar.
- Futuros: desconto financeiro (`futuresDiscountAmount`) ≠ incorporação do líquido (`anticipatedFuturesNetAmount`).
- `financedAmount = (invoiceRemaining − entryAmount) + anticipatedFuturesNetAmount`.
- Settlement da fatura = `invoiceSettlementAmount = invoiceRemaining − entryAmount` apenas.
- Encerra Agreements elegíveis como `RENEGOTIATED`.
- Cria novo Agreement + expense + parcelas; 1ª na próxima fatura.
- `contractedTotal >= financedAmount` obrigatório.

---

## 19.4.9 Antecipação de parcela de Agreement, `settled` e desconto (D7=B)

`settled` **não** entra em:

- `PayInvoiceRequest`;
- `POST /expenses/.../payments` de despesas `ACCOUNT`/`NONE` (Fase 8 inalterada neste ponto).

`settled` entra **somente** na antecipação de parcelas da obrigação do Agreement (endpoint dedicado — ver §19.4.12 / `docs/25`).

Regras:

- pagamento parcial (`settled=false` ou omitido) → remaining cai; status `PARTIALLY_PAID` se remaining > 0;
- remaining 0 pelo total pago → `PAID` mesmo se `settled=false`;
- `settled=true` e `amount < remaining` → payment do valor + `DISCOUNT` automático da diferença → `PAID`;
- usuário **não** informa `discount`;
- % desconto derivado para exibição;
- `amount > remaining` → rejeitar (sem crédito automático);
- parcela já `PAID` → rejeitar.

Como a despesa do Agreement é `CREDIT_CARD`, a liquidação **ordinária** das parcelas continua via pagamento da fatura em que elas aparecem (RN106A). A antecipação (D7) é operação **explícita** (conta + valor [+ settled]) sobre parcela de Agreement ainda em aberto, distinta do pay genérico de despesa ACCOUNT e **distinta** da consolidação em lote da renegociação (RN254).

---

## 19.4.10 Estados do Agreement

| Status | Significado | Fase 13 |
| --- | --- | --- |
| `ACTIVE` | obrigações abertas | sim |
| `COMPLETED` | todas encerradas normalmente | sim |
| `RENEGOTIATED` | consolidado em nova renegociação | sim |
| `CANCELLED` | reservado | **sem** operação nesta fase (**D10=A**) |

Nunca apagar Agreement.

---

## 19.4.11 Ownership, concorrência e atomicidade

- Ownership pelo usuário autenticado; FKs compostas; conta de entrada/antecipação do mesmo usuário e ativa.
- `@Transactional` + locks pessimistas (fatura, cartão, conta, Agreements/despesa/parcelas) — estende RN248.
- Concorrência: uma negociação/renegociação por fatura vence; sem Agreements duplicados nem saldo inconsistente.
- Rollback completo se qualquer etapa falhar.

---

## 19.4.12 API oficial (D5=A)

| Método | Path | Operação |
| --- | --- | --- |
| `POST` | `/api/v1/invoices/{invoiceId}/agreements` | Nova negociação |
| `POST` | `/api/v1/invoices/{invoiceId}/renegotiations` | Renegociação |
| `GET` | `/api/v1/invoices/{invoiceId}/agreements` | Agreements da fatura |
| `GET` | `/api/v1/agreements/{agreementId}` | Detalhe do Agreement |
| `POST` | `/api/v1/agreements/{agreementId}/installments/{installmentId}/anticipate` | Antecipar parcela do Agreement (`amount`, `accountId`, `paymentDate`, `settled?`) |

Request mínima — nova negociação: `entryAmount`, `accountId`, `entryPaymentDate`, `installmentCount`, `installmentAmount`.

Request mínima — renegociação: os mesmos **+** `anticipatedFuturesNetAmount` (quando há futuros; senão `0`). **Sem** lista de `agreementId`s. Detalhe: `docs/25`.

Response do Agreement deve permitir reconstruir: fatura de origem, entrada, financed, contractedTotal, custo/%, expenseId, status, cadeia (origem/renegociação), parcelas; na renegociação, idealmente também original/desconto/líquido dos futuros (conceitual).

Legado `POST/GET /api/v1/invoices/{id}/installments` (body lista amount/dueDate): **obsoleto**; não implementar.

Pagamento de fatura: `POST /api/v1/invoices/{id}/payments` (inalterado).

---

## RN249 — Agreement e fatura `SETTLED_BY_AGREEMENT`

Somente fatura `CLOSED` com remaining > 0 pode gerar Agreement. Resultado terminal da fatura: `SETTLED_BY_AGREEMENT`. Entrada via `credit_card_invoice_payments`. Settlement da fatura = `invoiceSettlementAmount` (D2). Nova despesa `CREDIT_CARD` com `total_amount = contractedTotal`. V13 sem uso de negócio.


## RN250 — Nova negociação vs renegociação

Nova negociação: não antecipa Agreements `ACTIVE`; `financedAmount = invoiceRemaining − entryAmount`.

Renegociação: consolida fatura + futuros líquidos (RN254); marca anteriores `RENEGOTIATED`; cria novo Agreement. Sem `agreementIds` no request. Parcela da fatura atual não é contada duas vezes.


## RN251 — Antecipação de parcela de Agreement

Endpoint de antecipação (§19.4.12). `settled` só nesse fluxo. Desconto automático quando `settled=true` e amount < remaining. Não alterar pay de fatura nem pay ACCOUNT/NONE. Distinto da consolidação em lote da renegociação (RN254).


## RN252 — Limite após Agreement

Liquidação libera remaining das parcelas do ciclo (e futuros encerrados na renegociação). O novo Agreement compromete o **contractedTotal** no `used_limit`. `financedAmount` **não** define o comprometimento do limite.


## RN253 — Concorrência e atomicidade da Fase 13

Negociação, renegociação e antecipação são atômicas e com lock pessimista. Falha → rollback completo.


## RN254 — Consolidação da renegociação (emenda)

Regra determinística da renegociação. **Implementada e aprovada** (Fase 13 `CONCLUÍDA E APROVADA`).

### Grandezas

| Conceito | Definição |
| --- | --- |
| `invoiceRemaining` | Remaining da fatura atual antes da entrada |
| `futureOriginalAmount` | Soma dos remainings das parcelas **futuras** elegíveis de todos os Agreements `ACTIVE` do cartão, **excluindo** parcelas da fatura atual (e excluindo `CANCELLED` / `REFUNDED` / `PAID`) |
| `anticipatedFuturesNetAmount` | Valor líquido dos futuros informado pelo banco (request); `0 ≤ net ≤ futureOriginalAmount` |
| `futuresDiscountAmount` | `futureOriginalAmount − anticipatedFuturesNetAmount` |
| `consolidatedAmount` | `invoiceRemaining + anticipatedFuturesNetAmount` |
| `invoiceSettlementAmount` | `invoiceRemaining − entryAmount` |
| `financedAmount` | `consolidatedAmount − entryAmount` ≡ `(invoiceRemaining − entryAmount) + anticipatedFuturesNetAmount` |
| `contractedTotal` | `installmentCount × installmentAmount` |

### Desconto financeiro × incorporação

```text
futureOriginalAmount
    ↓  desconto financeiro do banco (futuresDiscountAmount)
anticipatedFuturesNetAmount
    ↓  incorporação / transferência para a nova obrigação
entra no financedAmount / consolidação;
parcelas antigas deixam de comprometer o limite (remaining → 0)
```

- **`futuresDiscountAmount`:** desconto **financeiro** concedido pelo banco.
- **`anticipatedFuturesNetAmount`:** dívida **líquida** incorporada — **não** é desconto financeiro.
- Relatórios futuros **não** devem somar os dois como “dois descontos financeiros”.
- Se a implementação física usar adjustment para zerar remaining das parcelas antigas após o desconto, a **semântica de negócio** da parte líquida é **liquidação/transferência**, não segundo desconto financeiro (reasons/fatos distintos).

### Settlement

Somente a fatura atual, valor `invoiceSettlementAmount`. Sem Payment sintético. Futuros não passam pelo settlement da fatura.

### Invariante

`contractedTotal >= financedAmount`; caso contrário **400** `BUSINESS_RULE_VIOLATION`.

### Exemplo oficial

Janeiro: fatura 1.500; entrada 500; Agreement #1 = 10×200.

Fevereiro: compras 1.000 + parcela 1/10 (200) → fatura 1.200; futuros #1 = 1.800; banco `anticipatedFuturesNetAmount = 900` → `futuresDiscountAmount = 900`.

| Grandeza | Valor |
| --- | --- |
| consolidatedAmount | 2.100 |
| entryAmount | 500 |
| financedAmount | **1.600** |
| invoiceSettlementAmount | 700 |
| contractedTotal (#2) | 3.200 |
| additionalCost | 1.600 |
| used_limit (novo Agreement) | **3.200** (D11) |

Março: sem parcela 2/10 do #1; parcela 1/10 do #2 = 320; compras novas normais.


## 19.4.13 Decisões D1–D11 — FECHADAS (não reabertas pela emenda)

| ID | Decisão |
| --- | --- |
| D1 | **A** — status `SETTLED_BY_AGREEMENT` (terminal) |
| D2 | **A** — fato de settlement (sem Payment sintético); na renegociação o settlement cobre `invoiceSettlementAmount` |
| D3 | **A** — `expenses` `CREDIT_CARD` + parcelas + Agreement |
| D4 | **A** — V13 SUPERADO |
| D5 | **A** — paths sob `/api/v1/invoices/...` |
| D6 | **A** — `entryAmount == invoiceRemaining` → 400 |
| D7 | **B** — `settled` só em anticipate |
| D8 | **A** — todos `ACTIVE` do cartão |
| D9 | **A** — count × amount iguais |
| D10 | **A** — `CANCELLED` reservado |
| D11 | **contractedTotal** no `used_limit` |

A emenda altera **somente** a semântica detalhada da **renegociação** (consolidação / financedAmount / futuros), não reabre D1–D11.


## 19.4.14 Testes

Cenários L01–L36 permanecem a base. A emenda exige cobertura de L13–L16/L36, `financedAmount` consolidado, `contractedTotal >= financedAmount` e desconto × incorporação — cobertos em `CreditCardInvoiceAgreementPhase13ApiTest`. Detalhe: `docs/27-testes.md`.


# 19.5 Contrato da Fase 14 — Transferências, Acerto de Saldos e Saldo Inicial

**Status:** `CONTRATO FECHADO / IMPLEMENTAÇÃO PENDENTE`.

Não implementar código, migrations nem testes de implementação até autorização explícita.

Autoridade: `AGENTS.md` §28 → esta seção → `docs/23` (modelo) → `docs/25` (API) → `docs/27` / `docs/28`.

---

## 19.5.1 Escopo

**Inclui:**

1. Transferências entre contas (`BANK_ACCOUNT`);
2. Reversão de transferências (`ACTIVE` → `REVERSED`);
3. Transferências retroativas;
4. Regras de saldo e atomicidade das transferências;
5. Acerto de Saldos (`BALANCE_ADJUSTMENT` / tabela `account_balance_adjustments`);
6. Reversão de Acerto de Saldos;
7. Acertos retroativos;
8. Regras temporais dos acertos;
9. Saldo inicial (RN010 / RN010A);
10. Encerramento do saldo inicial após a primeira movimentação;
11. Inativação de conta com saldo ≠ 0 (RN007A);
12. Extensão do cálculo de saldo (RN240 / RN011);
13. Cálculo interno de saldo as-of-date.

**Fora do escopo:**

- transferências futuras/agendadas;
- transferência entre usuários;
- extrato unificado / `GET /accounts/{id}/statement`;
- planejamento financeiro futuro;
- ledger genérico / entidade `Transaction` única.

---

## 19.5.2 Transferências

### Conceito

Fato próprio: debita origem, credita destino, patrimônio consolidado inalterado. Um único registro lógico (não duas transferências).

### Contas

Somente `BANK_ACCOUNT` ativas do usuário autenticado; origem ≠ destino; `CASH` e cartões excluídos (RN016A).

### Valor e atomicidade

`amount > 0`; uma origem; um destino; saldo origem suficiente; sem saldo negativo; operação atômica (RN018–RN020).

### Data

Uma `transfer_date` financeira (`America/Sao_Paulo`). Retroativa permitida. Futura proibida (RN022A).

### Status

`ACTIVE` | `REVERSED`. Somente `ACTIVE` entra no saldo.

### Listagem (MVP)

Filtros oficiais: `startDate`, `endDate`, `accountId` (origem ou destino). **Não** há filtro de `status` no MVP. A listagem pode retornar `ACTIVE` e `REVERSED`. O `status` continua no recurso de cada item.

### Reversão

Não editável. `POST .../reverse`: mantém registro; `ACTIVE` → `REVERSED`; efeito inverso; exige saldo suficiente na conta debitada pela reversão; sem "desreversão" (RN019A, RN022B).

### Histórico lógico

Origem: saída + id/nome do destino. Destino: entrada + id/nome da origem. Extrato unificado fora da fase.


## RN255 — Transferência ACTIVE no saldo

Transferência `ACTIVE`:

- origem: − `amount`;
- destino: + `amount`.

`REVERSED`: zero efeito no saldo.


## RN256 — Transferência futura proibida

`transfer_date` não pode ser posterior à data atual em `America/Sao_Paulo`.


## RN257 — Reversão de transferência

`ACTIVE` → `REVERSED`. Já `REVERSED` → rejeitar. Não apaga. Não cria fato compensatório separado. Aplica movimento inverso com checagem de saldo (RN019A).


## RN258 — Concorrência da transferência

Criação e reversão são `@Transactional` com locks pessimistas nas contas envolvidas (padrão das fases anteriores). Falha → rollback completo.

---

## 19.5.3 Acerto de Saldos (`BALANCE_ADJUSTMENT`)

### Nome

- Conceitual: **Acerto de Saldos**
- Técnico: `BALANCE_ADJUSTMENT`
- Tabela oficial: **`account_balance_adjustments`**

Não reutilizar o termo genérico `ADJUSTMENT` (já usado em parcela/fatura). Não usar nomes ambíguos (`adjustments`, `expense_installment_adjustments`).

### Fluxo

Usuário informa `reported_balance` (saldo real ≥ 0). Sistema calcula:

```text
adjustment_amount = reported_balance − calculated_balance
```

Pode ser positivo, zero ou negativo. Não informar a diferença diretamente.

### Contas

`BANK_ACCOUNT` e `CASH` **ativas**. Cartões não participam. Conta com saldo calculado 0,00 pode receber acerto.

### Persistência do fato (não é saldo da conta)

Campos do fato: `calculated_balance`, `reported_balance`, `adjustment_amount`, data financeira, `account_id`, `user_id`, `status`, timestamps conforme padrão.

Esses valores **não** constituem `current_balance` da conta.

### Status

`ACTIVE` | `REVERSED`. Somente `ACTIVE` no saldo.

### Temporal

Retroativo permitido (usa saldo as-of-date). Futuro proibido. Múltiplos acertos independentes. Não editável; correção = reverter + novo acerto.

### Reversão

Efeito inverso de `adjustment_amount`; exige saldo suficiente quando o inverso for saída; sem desreversão.


## RN259 — Acerto de Saldos

Fato de conciliação. Não é receita, despesa, transferência, payment nem adjustment de parcela/fatura.


## RN260 — Saldo real do acerto

`reported_balance >= 0`. O acerto não pode provocar saldo negativo da conta.


## RN261 — Acerto as-of-date

Na criação, `calculated_balance` = saldo da conta **até** a data financeira do acerto (inclusive fatos elegíveis com data ≤ data do acerto). Movimentações posteriores permanecem intactas e passam a incorporar o acerto na linha temporal.


## RN262 — Reversão de acerto

`ACTIVE` → `REVERSED`. Já `REVERSED` → rejeitar. Remove efeito financeiro; histórico permanece.


## RN263 — Saldo as-of-date (capacidade interna)

A Fase 14 exige cálculo interno de saldo até uma data financeira. Uso principal: acerto retroativo. Expor data em `GET /accounts/{id}/balance` **não** é obrigatório nesta fase.

---

## 19.5.4 Saldo inicial

Ver RN010 / RN010A.

API oficial única de definição/alteração após a criação:

```text
PUT /api/v1/accounts/{id}/initial-balance
```

`POST /accounts`: `initialBalance` **opcional**; omitido ⇒ `0,00`. A presença na criação **não** impede alteração posterior via PUT enquanto não houver movimentação efetiva (RN010A).

---

## 19.5.5 Inativação

Ver RN007A.

---

## 19.5.6 Arquitetura

Manter domínio modular. **Não** criar `Transaction` / ledger genérico. Pacote: `balance_adjustments` (tabela `account_balance_adjustments`). Cálculo de saldo agrega fatos por domínio (RN240).

---

## 19.5.7 Critério de conclusão da implementação (futuro)

Usuário consegue: transferir entre `BANK_ACCOUNT` próprias; reverter transferência; registrar/reverter acerto; definir saldo inicial só antes da primeira movimentação; inativar apenas conta com saldo zero; saldo derivado coerente com RN240.


# 20. Metas

## RN126 — Meta

Meta possui:

nome;

valor alvo;

data alvo opcional;

status.


## RN127 — Valor

Valor alvo deve ser maior que zero.


## RN128 — Contribuição

Contribuição deve possuir valor maior que zero.


## RN129 — Conta

Contribuição deve indicar a conta de origem.


## RN130 — Saldo

A contribuição reduz o dinheiro disponível na conta.


## RN131 — Despesa

Contribuição para meta não é despesa de consumo.


## RN132 — Meta concluída

Quando:

current_amount >= target_amount


a meta pode ser marcada como:

COMPLETED


# 21. Projeções

## RN133 — Projeção

Projeções são calculadas a partir dos dados existentes.


## RN134 — Receita futura

Receitas EXPECTED participam da projeção.


## RN135 — Receita recebida

Receitas RECEIVED representam histórico realizado.


## RN136 — Despesa futura

Despesas OPEN participam da projeção conforme seu vencimento.


## RN137 — Parcela futura

Parcelas OPEN participam da projeção.


## RN138 — Fatura

Compromissos futuros de cartão participam da projeção.


## RN139 — Cancelamento

Despesas CANCELLED não participam da projeção.

Receitas CANCELLED também não participam da projeção (RN045). Receita estornada volta a `EXPECTED` e, portanto, volta a participar da projeção enquanto permanecer prevista.


## RN140 — Estorno

Despesas REFUNDED não devem representar compromisso futuro ativo.


# 22. Saldo projetado

## RN141 — Saldo projetado

Saldo projetado é diferente do saldo atual.


## RN142 — Fórmula conceitual

Saldo projetado:

saldo atual

+

receitas previstas

-

compromissos futuros


## RN143 — Projeção

O sistema não deve considerar limite de cartão como dinheiro disponível.


## RN144 — Projeção

O sistema não deve considerar uma receita EXPECTED como dinheiro disponível atualmente.


# 23. Dashboard

## RN145 — Saldo total

Saldo total é a soma dos saldos das contas ativas.


## RN146 — Cartões

Cartões não entram no saldo bancário.


## RN147 — Faturas

Faturas não devem ser subtraídas duas vezes.


## RN148 — Receitas

Dashboard deve diferenciar:

recebidas;

previstas.


## RN149 — Despesas

Dashboard deve diferenciar:

pagas;

abertas;

futuras.


# 24. Relatórios

## RN150 — Relatórios

Relatórios devem respeitar o usuário autenticado.


## RN151 — Fatura

Relatório de fatura deve apresentar:

cartão;

período;

fechamento;

vencimento;

despesas;

categorias;

responsáveis;

valores;

total.


## RN152 — Responsável

Relatório pode agrupar despesas por responsável.


## RN153 — Categoria

Relatório pode agrupar despesas por categoria.


## RN154 — Conta

Relatório pode agrupar movimentações por conta.


# 25. Exportação

## RN155 — Exportação

A exportação da fatura deve representar os dados da fatura selecionada.


## RN156 — Exportação

O usuário deve poder utilizar o relatório para conferência com o proprietário do cartão.


## RN157 — V1

Formato inicial de exportação:

PDF


## RN158 — Futuro

CSV e Excel podem ser adicionados posteriormente.


# 26. Busca

## RN159 — Busca

Busca textual deve respeitar o usuário autenticado.


## RN160 — Busca

Busca pode considerar:

descrição;

categoria;

observações;

número do boleto.


# 27. Alterações

## RN161 — Alteração

Operações abertas podem ser alteradas conforme regras específicas.


## RN162 — Histórico

Operações já pagas não devem ser alteradas silenciosamente.


## RN163 — Correção

Correções após pagamento devem preservar o histórico financeiro.


# 28. Exclusão

## RN164 — Exclusão física

Exclusão física de operações financeiras não deve ser utilizada como operação normal.


## RN165 — Cancelamento

Quando a operação não deve mais ser considerada:

utilizar CANCELLED.

Em receitas, isso inutiliza a duplicata prevista (`EXPECTED` → `CANCELLED`). Não usar cancelamento para desfazer um recebimento já efetivado.


## RN166 — Estorno

Quando a despesa ocorreu e depois foi revertida:

utilizar REFUNDED.

Esta regra aplica-se a despesas. O estorno de receita recebida não utiliza `REFUNDED` nem cria `REVERSED`; a transição oficial é `RECEIVED` → `EXPECTED` (RN198, RN200). A duplicata permanece ativa como não recebida.


# 29. Concorrência

## RN167 — Concorrência

Operações financeiras críticas devem possuir proteção contra execução simultânea inconsistente.


## RN168 — Pagamento duplicado

O sistema deve impedir que duas requisições simultâneas façam o mesmo pagamento duas vezes.


## RN169 — Transferência duplicada

O sistema deve evitar duplicação de transferência em caso de requisições repetidas.


# 30. Idempotência

## RN170 — Operações críticas

Quando necessário, operações críticas poderão utilizar mecanismo de idempotência.


## RN171 — V1

A necessidade de idempotência deve ser avaliada por operação.


Não implementar complexidade desnecessária.


# 31. Integridade financeira

## RN172 — Valores

Valores monetários devem utilizar precisão decimal.


## RN173 — Nunca usar

double

float


para cálculo monetário.


## RN174 — Backend

Cálculos financeiros devem ocorrer no backend.


# 32. Arredondamento

## RN175 — Parcelamento

Parcelamentos devem preservar o valor total.


## RN176 — Arredondamento

Diferença de centavos deve ser aplicada de forma determinística.


## RN177 — Regra

Utilizar:

BigDecimal


no Java.


## RN178 — Regra

O rounding mode oficial da V1 é:

`RoundingMode.HALF_UP`

Valores monetários devem ser normalizados para escala 2 quando aplicável.

Nenhum Service pode escolher outro `RoundingMode`.

Em parcelamentos, o residual de centavos é absorvido pela **primeira** parcela (RN068, RN224).


# 33. Calendário

## RN179 — Datas

Datas financeiras devem utilizar calendário do Brasil.


## RN180 — Timezone

Timestamps de sistema são persistidos em UTC.

Regras de calendário financeiro ("hoje", vencimento, fechamento, atraso, ciclos) utilizam `America/Sao_Paulo`.

O frontend não deve usar o timezone do navegador para decidir regras financeiras.


## RN181 — LocalDate

Operações que representam somente uma data devem preferencialmente utilizar:

LocalDate


# 34. Dados derivados

## RN182 — Valores derivados

Valores como:

saldo da conta;

total da fatura;

valor pago da fatura;

saldo restante da fatura;

acumulado da meta;

percentual da meta;

comprometimento / limite disponível do cartão;


são derivados. Não são colunas persistidas na V1.

Fórmulas e fatos persistidos: `docs/23-modelo-de-dados.md` (seções 194–199 e 263).


## RN183 — Fonte de verdade

A aplicação deve evitar múltiplas fontes de verdade para o mesmo valor.

Fatos da fatura: parcelas vinculadas, pagamentos da fatura, alocações de rateio, créditos aplicados e ajustes de fatura.

Totais da fatura são calculados a partir desses fatos. Remaining da fatura = soma dos remainings das parcelas do ciclo (excluindo parcelas `CANCELLED` e `REFUNDED`). Não persistir `total_amount`, `paid_amount`, `remaining_amount`, `used_limit` nem `available_limit`.


# 35. Regras de consistência

## RN184 — Fatura

Total da fatura deve corresponder às parcelas vinculadas (`expense_installments` com aquele `invoice_id`), excluindo `CANCELLED` e `REFUNDED`.

Não utilizar o `total_amount` da despesa inteira: uma compra parcelada atravessa várias faturas (RN085).


## RN185 — Pagamentos

Valor pago não pode ultrapassar o valor devido.


## RN186 — Parcelamento

Soma das parcelas deve corresponder ao valor parcelado.


## RN187 — Meta

Valor acumulado (`current_amount`) é derivado da soma das contribuições.

Não persistir acumulado como fonte independente.


# 36. Usuários

## RN188 — Segurança

Nenhuma regra de negócio pode permitir acesso cruzado entre usuários.

A identidade do usuário autenticado vem exclusivamente do SecurityContext (`AuthenticatedUser`). Nunca de `userId` no JSON, query ou path de `/users/me`.


## RN194 — Política de senha

A senha deve ter entre 8 e 128 caracteres. Não há regra adicional de complexidade na V1 desta fase.

Somente o hash Argon2id é persistido.


## RN195 — Perfil

`PUT /api/v1/users/me` altera somente `name` e `email` do usuário autenticado.

Não é permitido alterar `id`, `active`, senha, `passwordHash`, `createdAt` ou `updatedAt` por esse endpoint.


## RN196 — Alteração de senha

`PUT /api/v1/users/me/password` exige a senha atual. Senha atual incorreta: **401** com `Credenciais inválidas.` Sucesso: **204 No Content**.


# 37. API

## RN189 — Validação

Toda regra recebida pela API deve ser validada no backend.


## RN190 — Erro

Quando uma regra de negócio for violada:

não retornar HTTP 500.


Deve retornar erro de negócio apropriado.


# 38. Mensagens

## RN191 — Erros

Mensagens devem ser claras para o usuário.


## RN192 — Segurança

Mensagens não devem revelar:

SQL;

stack trace;

informações internas;

dados de outros usuários.


# 39. Auditoria futura

## RN193 — Auditoria

O sistema poderá futuramente registrar:

quem alterou;

quando alterou;

valor anterior;

valor novo.


## RN194 — V1

Auditoria detalhada não é obrigatória na primeira versão.


# 40. Regras de evolução

## RN195 — Novas regras

Toda nova regra financeira deve ser documentada antes ou junto da implementação.


## RN196 — IA

A IA não deve inventar comportamento financeiro.


## RN197 — Dúvida

Quando houver ambiguidade:

1. identificar a regra;
2. explicar a dúvida;
3. apresentar alternativas;
4. solicitar decisão.


# 41. Acerto de Saldos (`BALANCE_ADJUSTMENT`)

Nome conceitual oficial: **Acerto de Saldos**. Identificador técnico: `BALANCE_ADJUSTMENT`. Tabela oficial: `account_balance_adjustments`.

Não confundir com adjustments de parcela/fatura (`DISCOUNT` / `SURCHARGE`).


## RN204 — Acerto de Saldos

Um acerto de saldos é um fato financeiro cuja única finalidade é reconciliar o saldo calculado pelo sistema com o saldo real da conta.

O usuário informa o saldo real (`reported_balance`). O sistema calcula `adjustment_amount = reported_balance − calculated_balance` (pode ser positivo, zero ou negativo).

Altera o saldo derivado. Deve persistir o fato com `calculated_balance`, `reported_balance`, `adjustment_amount`, data financeira e `status` (`ACTIVE` / `REVERSED`) para histórico/auditoria.

Esses campos **não** são fonte de verdade de saldo da conta.

Contrato completo: **Fase 14** (`docs/24` §19.5 / RN259–RN263).


## RN205 — Acerto não é receita nem despesa

Acerto de saldos não é receita, despesa, transferência, payment nem adjustment de parcela/fatura.

Não deve ser lançado como despesa (acerto negativo) nem como receita (acerto positivo). Isso distorceria relatórios financeiros.

Futuramente, um relatório poderá apresentar receitas, despesas, transferências, acertos, movimentação líquida e saldo de forma separada.


## RN206 — Acerto fora da Fase 6 e da Fase 7

A funcionalidade de acerto de saldos não pertence à Fase 6 nem à Fase 7.

Contrato oficial e implementação: **Fase 14** (`docs/24` §19.5). Status documental: `CONTRATO FECHADO / IMPLEMENTAÇÃO PENDENTE`.

Não criar endpoint, DTO, entidade, service, controller, migration nem testes de implementação sem autorização explícita da Fase 14.


# 42. Regra final

O backend é a autoridade sobre:

- saldo;
- parcelas;
- faturas;
- pagamentos;
- transferências;
- projeções;
- regras financeiras.


# 43. Regra final

O frontend é responsável por:

- apresentar;
- solicitar;
- validar experiência;
- organizar informações.


# 44. Regra final

Nenhuma regra financeira crítica deve depender exclusivamente do frontend.


# 45. Regra final

Toda regra crítica deve possuir teste automatizado.


# 46. Critério de aceitação

Uma funcionalidade financeira somente estará concluída quando:

1. regra estiver documentada;
2. backend implementar a regra;
3. testes cobrirem a regra;
4. API estiver documentada;
5. frontend consumir corretamente;
6. fluxo estiver validado.


# 47. Regra final

Em caso de conflito entre código e documentação:

o código não deve ser simplesmente considerado correto.


O conflito deve ser identificado e resolvido.


# 48. Regra final

A documentação deve permanecer atualizada conforme o sistema evoluir.