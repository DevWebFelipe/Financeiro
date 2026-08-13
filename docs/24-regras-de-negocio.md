# Regras de Negócio — Financial Control

## 0. Hierarquia

`AGENTS.md` → `docs/20–28` → `README.md`

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


## RN002 — Identificação do usuário

O backend deve obter o ID do usuário autenticado através do contexto de segurança.

Nunca confiar em um userId enviado pelo frontend para determinar o proprietário de uma operação.


## RN003 — Email

O email do usuário deve ser único.


## RN004 — Usuário desativado

Usuário desativado não pode autenticar ou realizar novas operações financeiras.


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

O saldo da conta deve refletir:

saldo inicial;

entradas;

saídas.


## RN012 — Saldo negativo

A V1 não permite que operações financeiras normais deixem a conta com saldo negativo.

Inclui: transferências, pagamento de despesas e pagamento de fatura (limitado ao saldo da conta).


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


não altera o saldo da conta.


## RN042 — Receita recebida

Status:

RECEIVED


representa entrada financeira real.


## RN043 — Conta

Uma receita recebida deve estar vinculada à conta que recebeu o dinheiro.


## RN044 — Data de recebimento

Receitas recebidas devem possuir data de recebimento.


## RN045 — Receita cancelada

Receita cancelada não entra na projeção futura.


## RN046 — Receita recebida

Receita já recebida não pode voltar silenciosamente para EXPECTED.


Deve existir operação explícita de correção.


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

Quando a forma for:

ACCOUNT


a despesa deve possuir conta.


## RN051 — CREDIT_CARD

Quando a forma for:

CREDIT_CARD


a despesa deve possuir cartão.


## RN052 — NONE

Quando a forma for:

NONE


a despesa não possui cartão.


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


## RN059 — Histórico

Despesa cancelada continua armazenada.


## RN060 — Estorno

Despesa estornada permanece armazenada.


## RN061 — Estorno

Despesa estornada não deve continuar representando obrigação financeira ativa.


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


## RN086 — Fatura

Uma fatura pode conter parcelas de várias despesas.


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

O sistema deve utilizar timezone definido pela aplicação (`America/Sao_Paulo`) para evitar inconsistências.


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


# 17. Estornos

## RN114 — Cancelamento

CANCELLED significa que a operação foi anulada.


## RN115 — Estorno

REFUNDED significa que a operação ocorreu e posteriormente foi revertida.


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


## RN123 — Conta a pagar

Despesas NONE devem aparecer em contas a pagar quando estiverem abertas.


## RN124 — Pagamento

Quando forem pagas:

o usuário informa a conta utilizada.


## RN125 — Histórico

A forma original da despesa deve permanecer rastreável.


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


## RN166 — Estorno

Quando a operação ocorreu e depois foi revertida:

utilizar REFUNDED.


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

Definir explicitamente o rounding mode utilizado em cada cálculo financeiro.


# 33. Calendário

## RN179 — Datas

Datas financeiras devem utilizar calendário do Brasil.


## RN180 — Timezone

Timezone da aplicação deve ser definido explicitamente.


## RN181 — LocalDate

Operações que representam somente uma data devem preferencialmente utilizar:

LocalDate


# 34. Dados derivados

## RN182 — Valores derivados

Valores como:

saldo;

total da fatura;

saldo restante;

percentual da meta;


podem ser derivados.


## RN183 — Fonte de verdade

A aplicação deve evitar múltiplas fontes de verdade para o mesmo valor.


# 35. Regras de consistência

## RN184 — Fatura

Total da fatura deve corresponder às parcelas/despesas vinculadas.


## RN185 — Pagamentos

Valor pago não pode ultrapassar o valor devido.


## RN186 — Parcelamento

Soma das parcelas deve corresponder ao valor parcelado.


## RN187 — Meta

Valor acumulado deve corresponder às contribuições.


# 36. Usuários

## RN188 — Segurança

Nenhuma regra de negócio pode permitir acesso cruzado entre usuários.


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


# 41. Regra final

O backend é a autoridade sobre:

- saldo;
- parcelas;
- faturas;
- pagamentos;
- transferências;
- projeções;
- regras financeiras.


# 42. Regra final

O frontend é responsável por:

- apresentar;
- solicitar;
- validar experiência;
- organizar informações.


# 43. Regra final

Nenhuma regra financeira crítica deve depender exclusivamente do frontend.


# 44. Regra final

Toda regra crítica deve possuir teste automatizado.


# 45. Critério de aceitação

Uma funcionalidade financeira somente estará concluída quando:

1. regra estiver documentada;
2. backend implementar a regra;
3. testes cobrirem a regra;
4. API estiver documentada;
5. frontend consumir corretamente;
6. fluxo estiver validado.


# 46. Regra final

Em caso de conflito entre código e documentação:

o código não deve ser simplesmente considerado correto.


O conflito deve ser identificado e resolvido.


# 47. Regra final

A documentação deve permanecer atualizada conforme o sistema evoluir.