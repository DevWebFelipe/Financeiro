# Fluxos Financeiros — Financial Control

## 0. Hierarquia

`AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`

Este documento detalha fluxos. Regras canônicas: `docs/24-regras-de-negocio.md`.


## 1. Objetivo

Este documento descreve os principais fluxos financeiros da V1.

Os fluxos servem como:

- referência funcional;
- referência para implementação;
- referência para testes;
- referência para validação do comportamento do sistema.


# 2. Princípio

Cada fluxo deve representar uma operação financeira real.

O sistema não deve criar ou remover dinheiro artificialmente.


# 3. Fluxo — Cadastro de usuário

## Entrada

Nome:

Felipe


Email:

felipe@email.com


Senha:

senha segura


## Resultado

Usuário criado.


O sistema gera:

UUID


# 4. Fluxo — Cadastro de conta

Usuário cadastra:

Nome:

Nubank


Tipo:

BANK_ACCOUNT


Saldo inicial:

R$ 2.500,00


## Resultado

Conta criada.


Saldo inicial:

R$ 2.500,00


# 5. Fluxo — Segunda conta

Usuário cadastra:

Itaú


Saldo inicial:

R$ 1.000,00


## Resultado

Usuário possui:

Nubank:

R$ 2.500


Itaú:

R$ 1.000


Total:

R$ 3.500


# 6. Fluxo — Cadastro de carteira

Usuário cadastra:

Carteira


Tipo:

CASH


Saldo inicial:

R$ 100


## Resultado

Dinheiro físico passa a ser controlado como uma conta.


# 7. Fluxo — Cadastro de cartão

Usuário cadastra:

Cartão A


Últimos 4 dígitos:

1234


Limite:

R$ 5.000


Fechamento:

10


Vencimento:

17


# 8. Fluxo — Segundo cartão

Usuário cadastra:

Cartão B


Limite:

R$ 8.000


Fechamento:

20


Vencimento:

27


# 9. Fluxo — Cadastro de categoria

Categoria:

Mercado


Tipo:

EXPENSE


# 10. Fluxo — Cadastro de categoria

Categoria:

Salário


Tipo:

INCOME


# 11. Fluxo — Receita salarial

No 5º dia útil do mês:

usuário lança:

Descrição:

Salário


Valor:

R$ 5.400,00


Categoria:

Salário


Conta:

Nubank


Responsável:

MINE


Data:

data de recebimento


Status:

RECEIVED


# 12. Resultado

Conta Nubank aumenta:

R$ 5.400


A receita aparece no histórico.


# 13. Fluxo — Receita futura

Usuário sabe que receberá:

Freelance:

R$ 1.000


Mas ainda não recebeu.


Cadastra:

status:

EXPECTED


# 14. Resultado

A receita:

não aumenta o saldo atual;


mas aparece na projeção.


# 15. Fluxo — Recebimento do freelance

Quando o dinheiro entra:

usuário registra recebimento.


Conta:

Nubank


Valor:

R$ 1.000


## Resultado

Receita:

RECEIVED


Saldo aumenta:

R$ 1.000


# 16. Fluxo — Despesa de luz

Usuário recebe conta de luz:

R$ 180


Vencimento:

10/08


Forma:

NONE


Status:

OPEN


Boleto:

123456789


# 17. Resultado

A despesa aparece em:

Contas a pagar.


Não reduz o saldo bancário.


# 18. Fluxo — Pagamento da luz

No dia do pagamento:

Conta:

Nubank


Valor:

R$ 180


Data:

08/08


## Resultado

Despesa:

PAID


Nubank:

- R$ 180


Pagamento registrado.


# 19. Fluxo — Despesa paga parcialmente

Conta:

R$ 500


Usuário paga:

R$ 200


## Resultado

Despesa:

PARTIALLY_PAID


Valor restante:

R$ 300


Conta:

- R$ 200


# 20. Fluxo — Pagamento restante

Usuário paga:

R$ 300


## Resultado

Despesa:

PAID


Saldo restante:

R$ 0


# 21. Fluxo — Compra à vista

Usuário compra supermercado:

R$ 300


Conta:

Nubank


Data:

10/08


Forma:

ACCOUNT


## Resultado

Despesa:

PAID


Nubank:

- R$ 300


# 22. Fluxo — Compra no cartão

Usuário compra supermercado:

R$ 600


Cartão:

Cartão A


Responsável:

MINE


Data:

05/08


Categoria:

Mercado


Forma:

CREDIT_CARD


# 23. Resultado

A conta bancária:

não é reduzida.


O cartão:

recebe comprometimento de:

R$ 600


# 23.1 Fluxo — Compra acima do limite disponível

Limite do cartão:

R$ 5.000,00


Comprometido:

R$ 4.500,00


Disponível:

R$ 500,00


Tentativa de compra:

R$ 600,00


Resultado:

compra recusada pelo backend.


# 24. Fluxo — Compra parcelada

Usuário compra:

Televisão


Valor:

R$ 2.400


Cartão:

Cartão A


Parcelamento:

12x


Data:

05/08


# 25. Resultado

Sistema cria:

1 despesa


e:

12 parcelas


Cada parcela:

R$ 200


# 26. Resultado futuro

Agosto:

R$ 200


Setembro:

R$ 200


Outubro:

R$ 200


Novembro:

R$ 200


Dezembro:

R$ 200


e assim por diante.


# 27. Fluxo — Parcelas com valores diferentes

Compra:

R$ 1.000


Parcelas:

5


Valores:

200

180

220

190

210


O sistema deve permitir.


# 28. Regra

O usuário pode editar cada parcela individualmente.


# 29. Fluxo — Compra no dia do fechamento

Cartão:

fecha dia 10


Compra:

10/08


Regra oficial (RN095):

compra no dia do fechamento pertence à próxima fatura.


# 30. Fluxo — Compra depois do fechamento

Cartão fecha:

10/08


Compra:

11/08


Deve entrar no próximo ciclo.


# 31. Fluxo — Compra antes do fechamento

Cartão fecha:

10/08


Compra:

09/08


Deve entrar no ciclo que fecha em:

10/08


# 32. Fluxo — Fatura

Cartão A:

fecha dia 10


vence dia 17


Durante o ciclo são feitas compras:


Mercado:

R$ 600


Internet:

R$ 150


Restaurante:

R$ 100


Total:

R$ 850


# 33. Resultado

Fatura:

R$ 850


# 34. Fluxo — Fatura fechada

Quando chega o fechamento:

a fatura passa para:

CLOSED


Novas compras devem ir para a próxima fatura.


# 35. Fluxo — Fatura vencida

Fatura:

R$ 850


Vencimento:

17/08


Se chegar:

18/08


e ainda houver saldo:

A UI pode apresentar como VENCIDA.

O status persistido permanece OPEN, CLOSED ou PARTIALLY_PAID conforme o ciclo.

OVERDUE é derivado (não persistido).


# 36. Fluxo — Pagamento integral da fatura

Fatura:

R$ 850


Conta:

Nubank


Pagamento:

R$ 850


# 37. Resultado

Fatura:

PAID


Conta:

- R$ 850


Saldo da fatura:

R$ 0


# 38. Fluxo — Pagamento parcial da fatura

Fatura:

R$ 2.000


Usuário paga:

R$ 1.200


# 39. Resultado

Fatura:

PARTIALLY_PAID


Pago:

R$ 1.200


Restante:

R$ 800


# 40. Regra

O sistema não deve considerar os R$ 800 como pagos.


# 41. Fluxo — Parcelamento do restante da fatura

Fatura:

R$ 2.000


Pago:

R$ 1.200


Saldo:

R$ 800


Usuário decide parcelar:

R$ 800


em:

4 parcelas


# 42. Resultado

Criar operação:

PARCELAMENTO_CARTAO


Parcelas:

1/4

2/4

3/4

4/4


# 43. Valores do parcelamento

O usuário pode definir valores diferentes.


Exemplo:

200

200

200

200


ou:

150

200

200

250


# 44. Regra

A soma deve representar:

R$ 800


# 45. Fluxo — Parcelamento com juros

Se futuramente houver suporte a juros:

o sistema poderá representar:

principal;

juros;

total.


## V1

Não implementar cálculo avançado de juros.


Mas não criar estrutura que impeça evolução futura.


# 46. Fluxo — Transferência entre contas

Usuário possui:

Nubank:

R$ 2.000


Itaú:

R$ 500


Transfere:

R$ 300


Nubank → Itaú


# 47. Resultado

Nubank:

R$ 1.700


Itaú:

R$ 800


Patrimônio total:

R$ 2.500


# 48. Regra

Transferência não aparece como:

receita;


nem:

despesa.


# 49. Fluxo — Transferência para pagar empréstimo

Usuário transfere dinheiro:

Conta A → Conta B


Depois paga uma despesa pela Conta B.


As duas operações devem ser independentes.


# 50. Fluxo — Cancelamento de despesa

Usuário cadastrou:

Internet:

R$ 100


Mas a cobrança foi cancelada.


Usuário marca:

CANCELLED


# 51. Resultado

Despesa continua no banco.


Não aparece mais como:

conta a pagar;


nem:

compromisso futuro.


# 52. Fluxo — Estorno

Usuário comprou:

Produto:

R$ 500


no cartão.


A compra foi efetivada.


Posteriormente:

produto devolvido.


# 53. Resultado

Despesa:

REFUNDED


Histórico continua disponível.


O compromisso deve ser revertido.


# 54. Diferença

CANCELLED:

operação anulada.


REFUNDED:

operação ocorreu e depois foi revertida.


# 55. Fluxo — Responsável

Usuário compra:

Mercado:

R$ 300


Responsável:

GIULIA


# 56. Resultado

Relatório pode mostrar:

Giulia:

R$ 300


# 57. Fluxo — Cartão do sogro

Usuário cadastra:

Cartão Ederson


Limite:

R$ 5.000


# 58. Compra

Compra:

R$ 500


Cartão:

Cartão Ederson


Responsável:

MINE


# 59. Resultado

A compra aparece na fatura do cartão.


No relatório:

Responsável:

MINE


# 60. Fluxo — Fatura do sogro

Fatura:

R$ 2.000


Itens:

Felipe:

R$ 800


Giulia:

R$ 500


Outros:

R$ 700


# 61. Relatório

Usuário solicita:

"Gerar relatório da fatura"


O sistema gera documento contendo:

cartão;

período;

vencimento;

despesas;

categorias;

responsáveis;

valores;

total.


# 62. Objetivo do relatório

Permitir enviar a fatura para o proprietário do cartão.


# 63. Fluxo — Meta

Usuário cria:

Meta:

Viagem


Objetivo:

R$ 5.000


Data:

20/12


# 64. Resultado

Meta:

ACTIVE


Valor acumulado:

R$ 0


# 65. Fluxo — Contribuição para meta

Usuário separa:

R$ 500


Conta:

Nubank


Meta:

Viagem


# 66. Resultado

Meta:

R$ 500


Conta:

- R$ 500


# 67. Fluxo — Meta concluída

Meta:

R$ 5.000


Acumulado:

R$ 5.000


Status:

COMPLETED


# 68. Fluxo — Dashboard

Ao abrir o sistema:

o usuário deve visualizar resumo financeiro.


# 69. Dashboard

Informações mínimas:

Saldo total;

Receitas do mês;

Despesas do mês;

Despesas futuras;

Faturas abertas;

Faturas vencendo;

Comprometimento futuro;

Metas.


# 70. Dashboard

Saldo total deve representar contas do usuário.


# 71. Dashboard

Cartões não devem ser tratados como contas bancárias.


# 72. Dashboard

O valor das faturas não deve ser subtraído duas vezes.


# 73. Fluxo — Projeção mensal

Usuário seleciona:

Dezembro/2026


Sistema calcula:

receitas previstas;

despesas previstas;

parcelas;

faturas;

compromissos.


# 74. Resultado

Usuário visualiza:

Receitas previstas:

R$ 6.000


Despesas comprometidas:

R$ 4.000


Saldo projetado:

R$ 2.000


# 75. Regra

Saldo projetado não significa saldo bancário atual.


# 76. Fluxo — Projeção de cartão

Usuário possui:

Cartão A:

R$ 800 comprometidos em dezembro


Cartão B:

R$ 600 comprometidos em dezembro


# 77. Resultado

Sistema mostra:

Cartão A:

R$ 800


Cartão B:

R$ 600


Total:

R$ 1.400


# 78. Fluxo — Contas sem cartão

Usuário possui:

Internet:

R$ 150


Luz:

R$ 200


Aluguel:

R$ 1.000


Nenhuma usa cartão.


# 79. Resultado

Contas sem cartão:

R$ 1.350


# 80. Fluxo — Resumo mensal

Para determinado mês:

Cartão A:

R$ 1.000


Cartão B:

R$ 800


Sem cartão:

R$ 1.200


Total de compromissos:

R$ 3.000


# 81. Fluxo — Mês seguinte

Usuário deseja saber:

"Quanto vou pagar em setembro?"


O sistema deve considerar:

parcelas de cartão;

despesas abertas;

contas previstas;

parcelamentos.


# 82. Fluxo — Presente

Usuário deseja saber:

"Quanto posso gastar com presentes em dezembro?"


O sistema deve permitir visualizar:

receitas previstas;

compromissos já existentes;

saldo projetado.


# 83. Regra

O sistema não deve decidir automaticamente quanto o usuário pode gastar.


Deve apresentar os dados para que o usuário tome a decisão.


# 84. Fluxo — Nova despesa futura

Usuário sabe que pagará:

IPTU:

R$ 500


em dezembro.


Cadastra a despesa com:

dueDate = dezembro


# 85. Resultado

A despesa aparece na projeção de dezembro.


Não reduz o saldo atual.


# 86. Fluxo — Despesa futura paga

Quando o usuário pagar:

registra pagamento.


A despesa passa:

OPEN


para:

PAID


# 87. Fluxo — Receita prevista cancelada

Usuário esperava:

Freelance:

R$ 1.000


Mas o trabalho foi cancelado.


Marca:

CANCELLED


# 88. Resultado

A receita deixa de impactar a projeção.


# 89. Fluxo — Conta vencida

Conta:

R$ 400


Vencimento:

10/08


Hoje:

15/08


Não paga.


# 90. Resultado

Status persistido:

OPEN


Apresentação na UI:

VENCIDA (derivado: dueDate < hoje)


Saldo devido:

R$ 400


# 91. Fluxo — Pagamento parcial de conta vencida

Usuário paga:

R$ 200


# 92. Resultado

Status:

PARTIALLY_PAID


Saldo:

R$ 200


# 93. Fluxo — Pagamento final

Usuário paga:

R$ 200


# 94. Resultado

Status:

PAID


# 95. Fluxo — Alteração de parcela futura

Compra:

R$ 1.200


12x


Usuário percebe que a parcela 5 foi cadastrada incorretamente.


Altera:

parcela 5:

R$ 120


# 96. Resultado

Somente a parcela 5 deve ser alterada.


# 97. Fluxo — Tentativa de alterar parcela paga

Parcela:

PAID


Usuário tenta alterar valor.


# 98. Resultado

Operação deve ser rejeitada.


O sistema deve solicitar operação de correção apropriada.


# 99. Fluxo — Usuário A tentando acessar usuário B

Usuário A faz requisição para acessar:

expenseId


pertencente ao usuário B.


# 100. Resultado

Backend deve rejeitar.


Não revelar dados do usuário B.


# 101. Fluxo — Usuário A tentando alterar conta do usuário B

Mesma regra:

acesso negado.


# 102. Fluxo — Usuário A tentando pagar despesa do usuário B

A operação deve ser rejeitada.


# 103. Fluxo — Exclusão

Usuário tenta excluir uma despesa já paga.


# 104. Resultado

O sistema não deve apagar o registro.


Deve oferecer:

cancelamento;

estorno;

ou operação de correção adequada.


# 105. Fluxo — Exclusão de categoria

Categoria possui histórico.


Não deve ser fisicamente apagada.


Pode ser desativada.


# 106. Fluxo — Exclusão de cartão

Cartão possui faturas históricas.


Não deve ser fisicamente apagado.


Pode ser desativado.


# 107. Fluxo — Exclusão de conta

Conta possui movimentações.


Não deve ser fisicamente apagada.


Pode ser desativada.


# 108. Fluxo — Criação de despesa parcelada

Entrada:

Valor:

R$ 1.000


Parcelas:

3


Data:

05/08


# 109. Resultado esperado

Parcela 1:

R$ 333,34


Parcela 2:

R$ 333,33


Parcela 3:

R$ 333,33


Total:

R$ 1.000


# 110. Fluxo — Arredondamento

Entrada:

R$ 100


3 parcelas


Resultado:

33,34

33,33

33,33


# 111. Regra

Nunca perder ou criar centavos durante parcelamento.


# 112. Fluxo — Compra parcelada no cartão

Valor:

R$ 1.000


5x


Cartão A


Data:

05/08


# 113. Resultado

Parcela 1:

R$ 200


Parcela 2:

R$ 200


Parcela 3:

R$ 200


Parcela 4:

R$ 200


Parcela 5:

R$ 200


# 114. Projeção

Sistema deve mostrar cada parcela no mês correspondente.


# 115. Fluxo — Compra cancelada antes da fatura

Compra:

R$ 500


Cartão A


Depois:

CANCELLED


# 116. Resultado

Não deve permanecer como compromisso.


# 117. Fluxo — Compra estornada após fatura

Compra:

R$ 500


Fatura já fechada.


Posteriormente:

REFUNDED


# 118. Resultado

Histórico permanece.


O valor devido deve ser ajustado conforme a regra de estorno.


# 119. Fluxo — Fatura parcialmente paga e estorno

Fatura:

R$ 2.000


Pago:

R$ 1.000


Depois uma compra de:

R$ 500


é estornada.


# 120. Resultado

O saldo da fatura deve ser recalculado corretamente.


Não simplesmente subtrair:

R$ 500


de um valor sem considerar os pagamentos já realizados.


# 121. Fluxo — Várias contas

Usuário possui:

Nubank:

R$ 3.000


Itaú:

R$ 2.000


Caixa:

R$ 1.000


# 122. Dashboard

Saldo total:

R$ 6.000


# 123. Fluxo — Transferência

Nubank:

-1.000


Itaú:

+1.000


Saldo total:

continua:

R$ 6.000


# 124. Fluxo — Pagamento cartão

Fatura:

R$ 1.000


Pagamento:

Nubank


# 125. Resultado

Saldo total diminui:

R$ 1.000


porque agora houve saída real.


# 126. Fluxo — Receita

Salário:

R$ 5.400


Recebido em:

Nubank


# 127. Resultado

Saldo total aumenta:

R$ 5.400


# 128. Fluxo — Planejamento

Usuário consulta:

Agosto


Deve conseguir visualizar:

realizado;

previsto;

comprometido.


# 129. Fluxo — Comparação

Usuário consulta:

Setembro


Deve visualizar:

receitas previstas;

despesas previstas;

parcelas;

faturas;


mesmo que ainda não exista saldo bancário correspondente.


# 130. Fluxo — Relatório mensal

Usuário seleciona:

Agosto/2026


Sistema apresenta:

Receitas;

Despesas;

Transferências;

Faturas;

Pagamentos;

Saldo.


# 131. Regra

Transferências devem aparecer separadamente.


# 132. Regra

Transferências não devem distorcer receitas/despesas.


# 133. Fluxo — Filtro por responsável

Usuário seleciona:

MINE


Sistema mostra somente operações relacionadas ao responsável.


# 134. Fluxo — Filtro por cartão

Usuário seleciona:

Cartão A


Sistema mostra:

compras;

parcelas;

faturas;


relacionadas ao cartão.


# 135. Fluxo — Filtro por conta

Usuário seleciona:

Nubank


Sistema mostra:

receitas;

pagamentos;

transferências;

movimentações;


relacionadas à conta.


# 136. Fluxo — Filtro por categoria

Usuário seleciona:

Mercado


Sistema mostra:

despesas da categoria.


# 137. Fluxo — Filtro por período

Usuário seleciona:

01/08/2026

até:

31/08/2026


Sistema retorna operações do período conforme o significado da consulta.


# 138. Fluxo — Busca

Usuário pesquisa:

"Netflix"


Sistema deve encontrar despesas relacionadas.


# 139. Fluxo — Dashboard mensal

Usuário seleciona:

Agosto/2026


Dashboard mostra:

receitas;

despesas;

saldo;

categorias;

cartões;

contas.


# 140. Fluxo — Gráfico de despesas

Sistema apresenta despesas agrupadas por categoria.


Exemplo:

Mercado:

R$ 1.000


Casa:

R$ 500


Lazer:

R$ 300


# 141. Fluxo — Gráfico mensal

Sistema pode apresentar:

Janeiro:

R$ 3.000


Fevereiro:

R$ 3.500


Março:

R$ 2.800


# 142. Fluxo — Gráfico cartão

Sistema apresenta:

Cartão A:

R$ 1.200


Cartão B:

R$ 800


# 143. Fluxo — Gráfico receitas x despesas

Exemplo:

Receitas:

R$ 6.000


Despesas:

R$ 4.000


Saldo:

R$ 2.000


# 144. Fluxo — Meta

Meta:

Viagem


Objetivo:

R$ 5.000


Acumulado:

R$ 2.000


Sistema apresenta:

40%


# 145. Fluxo — Projeção

Usuário possui:

Receitas futuras:

R$ 6.000


Compromissos:

R$ 4.500


# 146. Resultado

Saldo projetado:

R$ 1.500


# 147. Regra

Saldo projetado é informativo.


Não representa garantia de disponibilidade.


# 148. Fluxo — Cenário completo

Usuário inicia o mês com:

Nubank:

R$ 1.000


Itaú:

R$ 2.000


# 149. Receitas

Salário:

R$ 5.400


Freelance:

R$ 800


Total de receitas:

R$ 6.200


# 150. Despesas

Luz:

R$ 180


Internet:

R$ 120


Mercado:

R$ 600


Aluguel:

R$ 1.200


Total:

R$ 2.100


# 151. Cartões

Cartão A:

R$ 1.000


Cartão B:

R$ 700


Total:

R$ 1.700


# 152. Resultado

Sistema deve permitir visualizar:

Saldo atual;

Despesas já pagas;

Contas em aberto;

Faturas;

Compromissos futuros;

Receitas futuras.


# 153. Cenário completo

Usuário consulta:

"Quanto já comprometi em setembro?"


Sistema deve somar:

parcelas;

contas futuras;

parcelamentos;

faturas aplicáveis.


# 154. Cenário completo

Usuário consulta:

"Quanto tenho hoje?"


Sistema deve considerar:

saldo das contas.


Não considerar simplesmente:

limite dos cartões.


# 155. Cenário completo

Usuário consulta:

"Quanto devo nos cartões?"


Sistema deve mostrar:

saldo das faturas/compromissos.


# 156. Cenário completo

Usuário consulta:

"Quanto devo pagar este mês?"


Sistema deve mostrar:

contas a pagar;

faturas;

parcelas;

demais obrigações relevantes.


# 157. Cenário completo

Usuário consulta:

"Quanto receberei este mês?"


Sistema deve mostrar:

receitas previstas;

receitas recebidas.


# 158. Cenário completo

Usuário consulta:

"Quanto gastei este mês?"


O sistema deve diferenciar:

despesas realizadas;

compromissos futuros.


# 159. Cenário completo

Usuário consulta:

"Quanto gastei no cartão?"


Deve apresentar compras/parcelas conforme período selecionado.


# 160. Cenário completo

Usuário consulta:

"Quanto gastei com mercado?"


Sistema agrupa por:

categoria.


# 161. Cenário completo

Usuário consulta:

"Quanto a Giulia gastou?"


Sistema filtra:

responsible = GIULIA


# 162. Cenário completo

Usuário consulta:

"Quanto está no cartão do Ederson?"


Sistema filtra:

credit_card


# 163. Cenário completo

Usuário consulta:

"Quanto preciso pagar para o Ederson?"


Sistema deve permitir visualizar o total da fatura e, quando aplicável, separar por responsável.


# 164. Regra

O sistema não assume que toda despesa de um cartão de terceiro pertence ao proprietário do cartão.


# 165. Fluxo — Correção

Usuário lançou:

Mercado:

R$ 500


Mas deveria ser:

R$ 450


Se ainda não houver pagamento:

pode corrigir conforme regras da despesa.


# 166. Fluxo — Correção após pagamento

Se já foi pago:

não alterar silenciosamente o histórico.


Deve utilizar operação de correção.


# 167. Regra final

Todos os fluxos acima devem possuir testes automatizados quando a funcionalidade correspondente for implementada.


# 168. Testes

Cada fluxo crítico deve ter:

Given

When

Then


# 169. Exemplo

Given:

fatura de R$ 2.000


When:

usuário paga R$ 1.200


Then:

fatura = PARTIALLY_PAID

saldo = R$ 800


# 170. Exemplo

Given:

compra de R$ 1.000 em 5x


When:

compra criada


Then:

5 parcelas devem existir.


# 171. Exemplo

Given:

conta A = R$ 1.000

conta B = R$ 500


When:

transferência de R$ 200


Then:

conta A = R$ 800

conta B = R$ 700


# 172. Exemplo

Given:

compra no cartão = R$ 500


When:

compra criada


Then:

saldo bancário não muda.


# 173. Exemplo

Given:

fatura = R$ 500


When:

fatura paga pela conta A


Then:

saldo da conta A diminui R$ 500.


# 174. Critério final

A implementação da V1 somente deve ser considerada correta quando os fluxos críticos estiverem funcionando e testados.