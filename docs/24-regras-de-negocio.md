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


## RN008 — Conta com histórico

Uma conta que possui movimentações não deve ser excluída fisicamente.


## RN009 — Conta desativada

Uma conta desativada continua aparecendo no histórico.


## RN010 — Saldo inicial

Toda conta pode possuir um saldo inicial.


## RN011 — Saldo

O saldo da conta é derivado das movimentações financeiras efetivas, tendo o saldo inicial como ponto de partida.

Não utilizar um campo `current_balance` como fonte de verdade.

O saldo deve refletir:

saldo inicial;

receitas efetivamente recebidas;

despesas efetivamente realizadas;

transferências de entrada;

transferências de saída;

ajustes de saldo.

Conceitualmente:

```text
Saldo em uma data =
saldo inicial
+ receitas efetivamente recebidas
− despesas efetivamente realizadas
+ transferências de entrada
− transferências de saída
+ ajustes de saldo
```

A partir da Fase 7, “despesas efetivamente realizadas” significa a soma dos `payments.amount` da conta cuja despesa **não** está em `CANCELLED` nem `REFUNDED` (RN216).

As demais operações (transferências, ajustes) participam desse cálculo quando o respectivo domínio for implementado.

Ajuste de saldo é movimentação própria de conciliação (RN204). Não é receita nem despesa. A funcionalidade de ajuste não pertence à Fase 6 nem à Fase 7 (RN206).


## RN012 — Saldo negativo

A V1 não permite que operações financeiras normais deixem a conta com saldo negativo.

Inclui: transferências, pagamento de despesas e pagamento de fatura (limitado ao saldo da conta).

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

## RN016 — Transferência

Uma transferência representa movimentação entre duas contas do mesmo usuário.


## RN017 — Contas diferentes

Conta origem e conta destino devem ser diferentes.


## RN018 — Valor

Transferência deve possuir valor maior que zero.


## RN019 — Saldo

A conta origem deve possuir saldo suficiente.


## RN020 — Atomicidade

Débito e crédito da transferência devem ocorrer na mesma transação.


## RN021 — Patrimônio

Transferências não alteram o patrimônio total do usuário.


## RN022 — Receita/despesa

Transferências não devem ser contabilizadas como receita ou despesa.


# 5. Cartões

## RN023 — Cartão pertence ao usuário

Cada cartão pertence a um usuário.


## RN024 — Cartão ativo

Somente cartões ativos podem receber novas compras.


## RN025 — Cartão desativado

Cartão desativado permanece disponível para consulta histórica.


## RN026 — Limite

O limite do cartão representa o limite de crédito disponível contratado.


## RN027 — Limite não é saldo

O limite do cartão não deve ser contabilizado como dinheiro disponível na conta.


## RN028 — Compra no cartão

Uma compra no cartão não reduz imediatamente o saldo bancário.


## RN029 — Comprometimento

Uma compra no cartão aumenta o comprometimento do cartão.


## RN029A — Limite disponível

Uma compra não pode ultrapassar o limite disponível do cartão.

Exemplo:

Limite: R$ 5.000,00

Comprometido: R$ 4.500,00

Disponível: R$ 500,00

Compra: R$ 600,00

Resultado: compra recusada.

Esta regra deve ser validada no backend.


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

Uma despesa é considerada vencida quando:

- status é OPEN ou PARTIALLY_PAID; e
- dueDate < data atual (timezone da aplicação).

A interface poderá apresentar "VENCIDA" sem alterar o status persistido.

A API da Fase 7 expõe o derivado `overdue` (boolean) na resposta da despesa (RN218). Não existe coluna nem valor de status `OVERDUE`.

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


## RN065 — Valores

Cada parcela possui seu próprio valor.


## RN066 — Parcelas diferentes

O sistema deve permitir valores diferentes para parcelas diferentes.


## RN067 — Soma

Ao criar um parcelamento, a soma das parcelas deve ser igual ao valor total da despesa.


## RN068 — Arredondamento

Diferenças de centavos devem ser ajustadas na última parcela.


## RN069 — Exemplo

R$ 100 em 3 parcelas:

R$ 33,34

R$ 33,33

R$ 33,33


## RN070 — Parcela paga

Uma parcela totalmente paga não pode ser alterada diretamente.


## RN071 — Parcela aberta

Uma parcela OPEN pode ser alterada conforme as regras da aplicação.


## RN072 — Parcela cancelada

Parcela cancelada não participa de compromissos futuros.


# 11. Pagamentos

## RN073 — Pagamento

Pagamento representa saída financeira real.


## RN074 — Conta

Todo pagamento deve indicar a conta utilizada.

Na Fase 7:

- `NONE`: a conta é informada no `POST /pay` e gravada só em `payments.account_id`;
- `ACCOUNT`: a conta do pagamento **deve** ser a mesma de `expenses.account_id` (RN210).


## RN075 — Valor

Pagamento deve ser maior que zero.


## RN076 — Limite

A soma dos pagamentos não pode ultrapassar o valor devido.


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

OPEN permite novas compras pertencentes ao ciclo.


## RN088 — Fatura fechada

CLOSED não deve receber novas compras do ciclo fechado.


## RN089 — Fatura vencida (derivada)

OVERDUE NÃO é status persistido da fatura.

Status persistidos: OPEN, CLOSED, PARTIALLY_PAID, PAID.

Fatura não quitada após o vencimento pode ser apresentada como vencida na UI (derivado de dueDate e status ≠ PAID).


## RN090 — Fatura parcialmente paga

Pagamento parcial gera:

PARTIALLY_PAID


## RN091 — Fatura paga

Quando o valor devido for totalmente quitado:

PAID


# 13. Fechamento de cartão

## RN092 — Data de fechamento

A data de fechamento determina o ciclo da compra.


## RN093 — Compra antes do fechamento

Compra realizada **antes** do dia de fechamento deve pertencer ao ciclo atual.

O dia do fechamento não faz parte deste caso (ver RN095).


## RN094 — Compra após fechamento

Compra realizada após o dia de fechamento deve pertencer ao próximo ciclo.


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

## RN097 — Dia de vencimento

O cartão possui dia configurado para vencimento.


## RN098 — Mês sem o dia

Se o mês não possuir o dia configurado, utilizar o **último dia daquele mês**.

Exemplo:

Dia configurado: 31

Fevereiro (não bissexto): 28/02

Abril: 30/04


## RN099 — Regra

RN095 e RN098 devem ser cobertas por testes automatizados.


# 15. Pagamento de fatura

## RN100 — Pagamento

Pagamento de fatura representa saída real de dinheiro da conta.


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

Pagamento parcial é permitido.


## RN104 — Saldo

Se fatura:

R$ 2.000


e pagamento:

R$ 1.200


saldo:

R$ 800


## RN105 — Conta

Pagamento de fatura deve indicar a conta utilizada.


## RN105A — Saldo da conta no pagamento de fatura

Pagamento parcial é permitido.

O valor pago não pode exceder o saldo disponível da conta utilizada.

Exemplo:

Saldo da conta: R$ 500,00

Fatura: R$ 1.000,00

Pagamento: R$ 500,00

Resultado: saldo da conta R$ 0,00; fatura com R$ 500,00 restantes.


## RN106 — Atomicidade

Pagamento e atualização da fatura devem ocorrer na mesma transação.


# 16. Parcelamento de fatura

## RN107 — Parcelamento

Uma fatura parcialmente paga pode ter seu saldo parcelado.


## RN108 — Saldo parcelado

Somente o saldo restante deve ser parcelado.


## RN109 — Exemplo

Fatura:

R$ 2.000


Pago:

R$ 1.200


Saldo:

R$ 800


Parcelamento:

R$ 800


## RN110 — Parcelamento diferente

Parcelamento de fatura é diferente de compra parcelada.


## RN111 — Parcelamento

Cada parcela possui:

número;

total;

valor;

vencimento;

status.


## RN112 — Valores diferentes

Parcelas do parcelamento de fatura podem possuir valores diferentes.


## RN113 — Soma

A soma das parcelas deve representar o valor parcelado.


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

Estorno de compra no cartão deve ajustar o comprometimento correspondente.


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

`CREDIT_CARD`, faturas e parcelas N>1 permanecem para fases posteriores.


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


## RN210 — Pagamento de despesa ACCOUNT

Para despesa `ACCOUNT`, todo pagamento deve usar a mesma conta da despesa:

```text
payments.account_id == expenses.account_id
```

Rejeitar pagamento com conta diferente. Se o request omitir `accountId`, o backend utiliza `expenses.account_id`. Se informar `accountId`, o valor deve coincidir.


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

A Fase 8 poderá expor parcelas N>1. A Fase 7 não implementa edição independente de parcela (docs/23 §269.2 permanece bloqueada). Se a despesa `OPEN` for editada, a parcela 1/1 é atualizada para permanecer consistente com `total_amount` e `due_date` — isso não é edição de parcela futura da Fase 8.


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

A parcela 1/1 também passa a `REFUNDED`.

Não existe `POST /api/v1/payments/{id}/reverse` na Fase 7. Estorno é da despesa inteira (todos os pagamentos da parcela 1/1). O reverse por pagamento individual permanece previsto para fase futura (docs/23 §89).


## RN215 — Histórico de pagamentos no estorno

O estorno **não apaga** linhas de `payments`.

Identificação dos pagamentos originais: todas as linhas de `payments` da despesa (parcela 1/1), identificadas por `expense_id` / `installment_id`.

Registro da reversão: o fato de estorno é o status `REFUNDED` da despesa e da parcela. Não se cria pagamento negativo. Não se usa `payments.type` (valores oficiais inexistentes — docs/23 §269.1). Não se adiciona coluna de estorno nesta fase.

Recálculo do saldo: pagamentos cuja despesa está `REFUNDED` (ou `CANCELLED`) **não** entram na soma que reduz o saldo (RN216). O efeito financeiro do débito original deixa de ser considerado; o saldo da conta volta ao valor que teria sem aqueles pagamentos.

Duplo estorno: despesa já `REFUNDED` rejeita novo `refund`.

Atomicidade: a mesma transação valida o status, aplica `REFUNDED` na despesa e na parcela e confirma. Falha em qualquer etapa implica rollback. Lock pessimista na despesa e na parcela.

Consulta `GET /expenses/{id}/payments` continua devolvendo os pagamentos originais após o estorno.


## RN216 — Saldo da conta a partir da Fase 7

```text
saldo atual =
  initial_balance
+ SUM(incomes.amount WHERE status = RECEIVED AND account_id = conta)
− SUM(payments.amount WHERE account_id = conta
      AND a despesa do pagamento NÃO está em CANCELLED nem REFUNDED)
```

Pagamento de despesa reduz o saldo.

Estorno de despesa desfaz o efeito desses pagamentos no saldo (deixam de ser subtraídos).

Transferências e ajustes entram quando implementados. Não persistir `current_balance`.


## RN217 — Edição de despesa (Fase 7)

Somente despesa `OPEN` pode ser editada (`PUT /api/v1/expenses/{id}`).

Campos do contrato de criação (substituição): `categoryId`, `description`, `totalAmount`, `expenseDate`, `dueDate`, `paymentMethod`, `accountId`, `responsibleType`, `responsibleName`, `barcode`, `notes`.

Regras no `PUT` de `OPEN`:

- `paymentMethod` somente `ACCOUNT` ou `NONE`;
- `ACCOUNT` exige `accountId` (conta ativa do usuário); `NONE` exige `accountId` ausente/nulo;
- troca `ACCOUNT` ↔ `NONE` é permitida enquanto não houver pagamento, respeitando a regra de conta;
- categoria `EXPENSE`, ativa, do usuário;
- valor > 0;
- a parcela 1/1 é atualizada (`amount`, `due_date`) para permanecer igual à despesa.

Rejeitar `PUT` de `PARTIALLY_PAID`, `PAID`, `CANCELLED` e `REFUNDED`. Não alterar silenciosamente valor, conta ou forma depois que existir pagamento. Correção de despesa já paga: `refund` (terminal `REFUNDED`) e nova despesa, se necessário.

Não alterar `status`, `id`, `userId`, `createdAt` via body. Propriedades desconhecidas: 400.


## RN218 — OVERDUE derivado na API

`OVERDUE` não é status persistido nem valor de CHECK.

`overdue = true` quando:

```text
status IN (OPEN, PARTIALLY_PAID)
AND dueDate < data atual no timezone America/Sao_Paulo
```

Caso contrário, `overdue = false` (`PAID`, `CANCELLED`, `REFUNDED`, ou ainda no prazo).

A resposta HTTP da despesa inclui `overdue` (boolean). Não criar coluna.


## RN219 — Concorrência de pagamento

Dois `POST /pay` simultâneos não podem fazer a soma ultrapassar o devido (RN167, RN168).

A verificação `pago_acumulado + novo_pagamento <= devido` ocorre **dentro** da transação, após o lock pessimista da despesa e da parcela 1/1.


## RN220 — CREDIT_CARD fora da Fase 7

A Fase 7 rejeita `paymentMethod = CREDIT_CARD` na criação e na edição. Cartão, fatura e ciclo pertencem às Fases 9+.

O enum/CHECK físico `CREDIT_CARD` permanece no banco; a API da Fase 7 não o opera.


## RN221 — payments.type na Fase 7

O campo `payments.type` permanece sem valores oficiais. Na Fase 7 é persistido como `null`. Não criar enum, CHECK, constante nem regra. Pendência: docs/23 §269.1.


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

Em parcelamentos, o residual de centavos é absorvido pela última parcela.


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

Fatos da fatura: parcelas vinculadas e pagamentos da fatura.

Totais da fatura são calculados a partir desses fatos.


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


# 41. Ajuste de saldo

## RN204 — Ajuste de saldo

Um ajuste de saldo é uma movimentação cuja única finalidade é reconciliar o saldo calculado pelo sistema com o saldo real da conta.

Pode ser positivo ou negativo. Altera o saldo. Deve possuir histórico/auditoria quando implementado.


## RN205 — Ajuste não é receita nem despesa

Ajuste de saldo não é receita e não é despesa.

Não representa necessariamente uma operação econômica.

Não deve ser lançado como despesa (ajuste negativo) nem como receita (ajuste positivo). Isso distorceria relatórios financeiros.

Futuramente, um relatório poderá apresentar receitas, despesas, transferências, ajustes, movimentação líquida e saldo de forma separada.


## RN206 — Ajuste fora da Fase 6 e da Fase 7

A funcionalidade de ajuste de saldo não pertence à Fase 6 nem à Fase 7.

Não criar endpoint, DTO, entidade, service, controller, migration nem testes funcionais de ajuste nestas fases.

A arquitetura não pode impedir a futura existência de ajustes.


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