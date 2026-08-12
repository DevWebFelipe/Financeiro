# Regras de Negócio — Financial Control

## 1. Objetivo

Este documento define as regras de negócio financeiras da V1 do Financial Control.

Estas regras possuem prioridade sobre decisões de implementação.

Se existir conflito entre:

- frontend;
- backend;
- banco de dados;
- comportamento esperado;

este documento deve ser utilizado como referência para determinar o comportamento correto.


# 2. Princípio fundamental

O sistema deve separar claramente:

1. obrigação financeira;
2. pagamento;
3. movimentação bancária;
4. cartão de crédito;
5. fatura;
6. parcela;
7. projeção.


Uma operação não deve ser contabilizada duas vezes.


# 3. Usuários

O sistema suporta múltiplos usuários.

Todo dado financeiro deve pertencer a um usuário.


O usuário autenticado será identificado através do JWT.


# 4. Isolamento de usuários

Um usuário nunca pode visualizar ou alterar dados pertencentes a outro usuário.


O backend nunca deve confiar em um userId enviado pelo frontend.


# 5. Identificadores

Todos os IDs principais devem utilizar UUID.


# 6. Valores monetários

Todos os valores financeiros devem utilizar precisão decimal.


Não utilizar:

float

double


para cálculos financeiros.


# 7. Valor positivo

Valores de:

- receitas;
- despesas;
- pagamentos;
- estornos;
- transferências;
- metas;

devem ser maiores que zero quando representarem uma operação financeira.


# 8. Receita

Uma receita representa dinheiro que o usuário espera ou recebeu.


Exemplos:

- salário;
- freelance;
- venda;
- outros recebimentos.


# 9. Receita pendente

Uma receita PENDING:

- aparece nas projeções;
- não altera saldo atual;
- não cria movimentação financeira efetiva.


# 10. Receita recebida

Uma receita RECEIVED:

- representa dinheiro efetivamente recebido;
- deve possuir conta de destino;
- cria movimentação financeira;
- aumenta o saldo da conta.


# 11. Receita cancelada

Uma receita CANCELLED:

- não deve aumentar saldo;
- não deve aparecer como receita efetiva;
- permanece registrada no histórico.


# 12. Despesa

Uma despesa representa uma obrigação financeira.


Uma despesa pode estar:

- pendente;
- parcialmente paga;
- paga;
- estornada;
- parcialmente estornada;
- cancelada.


# 13. Despesa pendente

Uma despesa PENDING:

- representa obrigação;
- aparece em contas a pagar;
- não reduz saldo bancário;
- pode possuir vencimento;
- pode possuir boleto;
- pode possuir cartão.


# 14. Despesa sem cartão

Quando uma despesa não possui cartão:

credit_card_id = NULL


Ela poderá ser:

- paga imediatamente;
- registrada para pagamento futuro.


# 15. Despesa sem cartão paga imediatamente

Exemplo:

Mercado:

R$ 100


Conta:

Banco A


Ao registrar como paga:

- criar pagamento;
- criar saída financeira;
- reduzir saldo.


# 16. Despesa sem cartão futura

Exemplo:

Conta de internet:

R$ 150


Vencimento:

20/08


Enquanto não paga:

- status PENDING;
- aparece em contas a pagar;
- não reduz saldo.


# 17. Pagamento de despesa

O pagamento é uma operação financeira separada da despesa.


Uma despesa pode possuir vários pagamentos.


# 18. Pagamento integral

Se:

valor pago acumulado = valor da despesa


então:

status = PAID


# 19. Pagamento parcial

Se:

0 < valor pago acumulado < valor da despesa


então:

status = PARTIALLY_PAID


# 20. Valor restante

Sempre:

remaining_amount =
expense_amount
-
total_paid
+
total_refunded


A fórmula exata deverá considerar a semântica do estorno e será centralizada no domínio.


# 21. Múltiplos pagamentos

Uma despesa pode possuir:

Pagamento 1
Pagamento 2
Pagamento 3
...


A soma dos pagamentos representa o total efetivamente pago.


# 22. Pagamento acima do valor

A V1 não permite pagamento superior ao saldo restante da obrigação.


A API deve rejeitar o pagamento.


# 23. Conta utilizada no pagamento

Quando uma despesa sem cartão for paga:

o pagamento deve informar a conta utilizada.


Exemplo:

Banco A


# 24. Conta não utilizada antes do pagamento

Uma despesa pendente não deve reduzir saldo.


# 25. Cartão de crédito

Uma compra no cartão representa uma obrigação com a administradora do cartão.


Ela não representa uma saída imediata da conta bancária.


# 26. Compra no cartão

Ao criar compra no cartão:

- criar despesa;
- associar cartão;
- criar parcela(s);
- associar parcela(s) à(s) fatura(s);
- atualizar comprometimento.


Não reduzir saldo bancário.


# 27. Cartão do usuário

O cartão pertence ao usuário que o cadastrou.


# 28. Titular do cartão

O cartão pode possuir um titular textual.


Exemplos:

Felipe

Giulia

Ederson

Elisiane


O titular não precisa ser um usuário do sistema.


# 29. Responsável pela despesa

A despesa possui um responsável.


Valores V1:

MINE

GIULIA

EDERSON

ELISIANE


# 30. Diferença entre titular e responsável

Titular do cartão:

quem possui o cartão.


Responsável:

quem realizou/assume a despesa.


Exemplo:

Cartão titular:

Ederson


Responsável:

MINE


Isso significa:

compra realizada por Felipe no cartão do sogro.


# 31. Responsável não representa usuário

O campo responsável não substitui:

user_id.


user_id representa o proprietário dos dados.


responsible representa a pessoa relacionada à despesa.


# 32. Compra parcelada

Uma compra parcelada deve gerar automaticamente todas as parcelas futuras.


Isso é obrigatório na V1.


# 33. Objetivo do parcelamento

O usuário deve conseguir visualizar compromissos futuros.


Exemplo:

Compra:

R$ 1.200

12x


O sistema deve conhecer as 12 parcelas desde o momento da compra.


# 34. Número de parcelas

Quantidade mínima:

1


Quantidade deve ser inteira.


# 35. Valor total

A soma inicial das parcelas deve corresponder ao valor total da compra.


# 36. Divisão monetária

Quando o valor não puder ser dividido igualmente:

distribuir os centavos de forma que a soma seja exatamente igual ao valor original.


Exemplo:

R$ 100 / 3


Resultado:

R$ 33,33
R$ 33,33
R$ 33,34


# 37. Valor individual da parcela

Cada parcela possui seu próprio valor.


Isso permite alteração manual futura.


# 38. Alteração de parcela

O usuário pode editar parcelas futuras quando permitido.


Exemplo:

Parcela 5:

R$ 100


Alterar para:

R$ 150


# 39. Parcela paga

Uma parcela já paga não deve ser alterada normalmente.


Correções futuras deverão utilizar operação específica.


# 40. Parcela futura

Parcelas futuras podem ser editadas de acordo com as regras da aplicação.


# 41. Soma após edição

A alteração de parcelas não deve quebrar a integridade do plano.


O sistema deve informar claramente:

valor original;

valor atual;

diferença.


A política de alteração do total será definida pelo fluxo específico de edição.


# 42. Fatura

A fatura representa o compromisso do cartão em determinado ciclo.


# 43. Ciclo da fatura

Cada cartão possui:

dia de fechamento;

dia de vencimento.


# 44. Uma fatura por ciclo

Para um cartão:

ano + mês


deve existir somente uma fatura.


# 45. Compra antes do fechamento

Exemplo:

fechamento:

10


compra:

09


A compra pertence à fatura que será fechada no dia 10.


# 46. Compra depois do fechamento

Exemplo:

fechamento:

10


compra:

11


A compra pertence ao próximo ciclo.


# 47. Compra no dia do fechamento

V1:

uma compra realizada no dia do fechamento pertence ao ciclo que está sendo fechado.


Essa regra deve ser aplicada consistentemente.


# 48. Data da compra

A data da compra é diferente da data de vencimento da fatura.


# 49. Vencimento da fatura

A fatura possui sua própria data de vencimento.


# 50. Fechamento da fatura

Uma fatura OPEN:

pode receber novas compras.


Uma fatura CLOSED:

não deve receber novas compras normalmente.


# 51. Fechamento

Ao fechar uma fatura:

- recalcular total;
- validar parcelas;
- impedir novos lançamentos no ciclo;
- alterar status para CLOSED.


# 52. Pagamento da fatura

Pagar a fatura representa uma saída da conta bancária.


Somente nesse momento o dinheiro sai da conta.


# 53. Pagamento integral da fatura

Exemplo:

Fatura:

R$ 2.000


Pagamento:

R$ 2.000


Resultado:

status = PAID


# 54. Pagamento parcial da fatura

Exemplo:

Fatura:

R$ 2.000


Pagamento:

R$ 1.200


Resultado:

pago:

R$ 1.200


restante:

R$ 800


status:

PARTIALLY_PAID


# 55. Segundo pagamento

Se o usuário pagar posteriormente:

R$ 800


resultado:

fatura totalmente paga.


# 56. Vários pagamentos

Uma fatura pode possuir vários pagamentos.


Exemplo:

500
+
500
+
300
+
200


Total:

R$ 1.500


# 57. Conta utilizada para pagar cartão

O usuário escolhe qual conta bancária será utilizada.


O cartão não precisa estar associado à conta.


# 58. Fatura paga parcialmente

Uma fatura parcialmente paga continua representando obrigação financeira.


O saldo restante deve continuar aparecendo no planejamento.


# 59. Pagamento parcial não é estorno

Pagamento parcial:

reduz dívida porque houve pagamento.


Estorno:

reduz obrigação porque a compra foi revertida.


São operações diferentes.


# 60. Estorno

Uma compra pode ser estornada.


O registro original deve permanecer.


# 61. Estorno integral

Compra:

R$ 500


Estorno:

R$ 500


Resultado:

valor líquido da compra:

R$ 0


Status apropriado:

REFUNDED


# 62. Estorno parcial

Compra:

R$ 500


Estorno:

R$ 200


Resultado líquido:

R$ 300


Status:

PARTIALLY_REFUNDED


# 63. Múltiplos estornos

Uma despesa pode possuir vários estornos.


A soma não pode exceder o valor disponível para estorno.


# 64. Estorno de compra ainda não paga

Se a compra ainda estiver na fatura:

o estorno reduz o compromisso da fatura.


# 65. Estorno de compra de fatura já paga

Se a fatura já foi paga:

o estorno representa devolução financeira.


Deve existir entrada financeira na conta apropriada quando o dinheiro efetivamente retornar.


# 66. Estorno sem retorno financeiro

A implementação deve diferenciar:

estorno contábil da obrigação

e:

entrada financeira efetivamente recebida.


Não assumir automaticamente que todo estorno já foi recebido em conta.


# 67. Cancelamento

Cancelar não significa apagar.


O registro permanece.


# 68. Despesa cancelada

Uma despesa CANCELLED:

- não deve aparecer como obrigação ativa;
- não deve reduzir saldo;
- não deve entrar na projeção;
- deve permanecer no histórico.


# 69. Exclusão física

Não excluir fisicamente operações financeiras importantes.


# 70. Contas a pagar

Contas a pagar devem considerar obrigações com:

remaining_amount > 0


e status compatível.


# 71. Conta vencida

Uma obrigação está vencida quando:

due_date < data atual

e:

remaining_amount > 0.


# 72. OVERDUE

OVERDUE pode ser tratado como estado derivado.


Não é obrigatório armazenar.


# 73. Boleto

O sistema deve permitir armazenar o número do boleto.


O campo deve ser texto.


O usuário deve poder copiar o número facilmente no frontend.


# 74. Projeções

Projeções representam compromissos conhecidos no futuro.


Não são movimentações reais.


# 75. Projeção não altera saldo

Consultar uma projeção nunca deve alterar:

saldo;

fatura;

despesa;

receita.


# 76. Saldo atual

Saldo atual representa dinheiro efetivamente disponível nas contas.


Deve considerar movimentações financeiras efetivas.


# 77. Saldo projetado

Saldo projetado representa:

saldo atual

+
receitas futuras esperadas

-
despesas futuras esperadas

-
compromissos futuros.


# 78. Receita futura

Receita PENDING pode entrar na projeção.


# 79. Despesa futura

Despesa PENDING pode entrar na projeção.


# 80. Compra futura no cartão

Parcelas futuras devem entrar na projeção.


# 81. Fatura futura

Valores de parcelas futuras devem ser considerados nos respectivos ciclos.


# 82. Não duplicar cartão

Uma compra no cartão não deve aparecer como:

despesa futura

e novamente como:

fatura futura.


O sistema deve possuir uma regra clara de agregação.


# 83. Compromisso de cartão

Para planejamento:

o compromisso deve ser associado à fatura correspondente.


A projeção deve utilizar a parcela/fatura.


# 84. Transferência

Transferência movimenta dinheiro entre contas do mesmo usuário.


# 85. Transferência não é receita

Transferência não aumenta patrimônio.


# 86. Transferência não é despesa

Transferência não reduz patrimônio.


Ela apenas muda onde o dinheiro está.


# 87. Transferência deve ser atômica

Origem e destino devem ser atualizados na mesma transação.


Se uma etapa falhar:

tudo deve sofrer rollback.


# 88. Transferência entre contas

Permitido:

Banco A -> Banco B


Permitido:

Banco A -> Carteira


Permitido:

Carteira -> Banco B


# 89. Transferência entre usuários

V1:

não permitir.


# 90. Conta pessoal

Uma conta pode representar dinheiro em espécie.


Exemplo:

Carteira


# 91. Múltiplas contas

O sistema deve permitir várias contas por usuário.


Exemplo:

Banco A

Banco B

Carteira

Poupança


# 92. Saldo inicial

Ao criar conta:

o usuário pode informar saldo inicial.


Esse valor deve ser registrado de maneira rastreável.


# 93. Correção de saldo

Não permitir alteração arbitrária de saldo sem operação correspondente.


Futuramente poderá existir:

ajuste de saldo.


V1:

não implementar ajuste livre sem registro.


# 94. Metas

Uma meta representa um objetivo financeiro.


Exemplo:

Viagem:

R$ 10.000


# 95. Meta na V1

A meta pode possuir:

valor objetivo;

valor atual;

data objetivo.


# 96. Meta não altera saldo

Criar uma meta não movimenta dinheiro.


# 97. Meta concluída

Uma meta pode ser marcada como COMPLETED.


# 98. Meta cancelada

Uma meta pode ser marcada como CANCELLED.


# 99. Relatório de cartão

O relatório deve permitir filtrar:

- cartão;
- período;
- responsável.


# 100. Relatório do cartão do sogro

Exemplo:

Cartão:

Ederson


Responsável:

MINE


O relatório deve mostrar somente despesas correspondentes ao filtro.


# 101. Conteúdo do relatório

O relatório deve possuir:

- descrição;
- data;
- categoria;
- responsável;
- parcela;
- valor;
- total.


# 102. PDF

A geração do PDF deve ocorrer no backend.


# 103. Responsável "MINE"

Representa despesas do usuário proprietário do sistema.


Não representa necessariamente:

user_id.


# 104. Responsável "GIULIA"

Representa despesas relacionadas à esposa.


# 105. Responsável "EDERSON"

Representa despesas relacionadas ao sogro.


# 106. Responsável "ELISIANE"

Representa despesas relacionadas à sogra.


# 107. Futuro cadastro de pessoas

A arquitetura deve permitir futuramente substituir os valores fixos por um cadastro de pessoas.


Não implementar na V1.


# 108. Categorias

Cada despesa deve possuir uma categoria.


Exemplos:

Mercado

Moradia

Energia

Internet

Combustível

Lazer

Alimentação

Saúde

Educação

Outros


# 109. Categoria de receita

Receitas também podem possuir categoria/origem.


Exemplos:

Salário

Freelance

Outros


# 110. Categoria pai

Categorias podem possuir hierarquia.


Exemplo:

Alimentação

- Mercado

- Restaurantes

- Lanches


# 111. Exclusão de categoria

Não excluir categoria que possua histórico.


Preferir:

active = false.


# 112. Alteração de categoria

Alterar nome da categoria não deve modificar o histórico financeiro passado de forma inesperada.


# 113. Data de vencimento

Despesas futuras devem possuir due_date quando houver vencimento conhecido.


# 114. Data da despesa

expense_date representa quando a despesa ocorreu.


# 115. Data de pagamento

payment_date representa quando o dinheiro efetivamente saiu da conta.


# 116. Diferença entre datas

Exemplo:

Compra:

10/08


Vencimento:

20/08


Pagamento:

19/08


São três eventos distintos.


# 117. Compra no cartão

Para compra no cartão:

expense_date = data da compra


A saída bancária ocorrerá na data do pagamento da fatura.


# 118. Parcelas

Cada parcela deve possuir:

- número;
- valor;
- vencimento;
- fatura;
- status.


# 119. Parcela 1/12

O frontend deve apresentar:

1/12


# 120. Parcela 12/12

O frontend deve apresentar:

12/12


# 121. Parcelas futuras

Todas devem existir no banco.


Não depender de cálculo em tempo real para saber que uma compra possui parcelas futuras.


# 122. Alteração de parcela futura

Ao alterar:

Parcela 5:


o sistema deve atualizar:

- parcela;
- fatura;
- projeção;
- relatórios.


# 123. Fechamento

Depois de fechada:

a fatura não deve sofrer alterações normais.


Operações excepcionais como estorno podem alterar seus valores conforme regras específicas.


# 124. Reabertura

A V1 pode permitir reabertura apenas em situações controladas.


Não disponibilizar reabertura irrestrita ao usuário.


# 125. Fatura vencida

Se:

due_date < today

e:

remaining_amount > 0


a fatura deve ser apresentada como vencida.


# 126. Fatura parcialmente paga e vencida

Uma fatura pode estar:

PARTIALLY_PAID


e também estar vencida por regra temporal.


A implementação deve evitar conflito entre estado financeiro e estado temporal.


# 127. Estado derivado

Sempre que possível:

estado temporal deve ser calculado.


Exemplo:

OVERDUE.


# 128. Projeção mensal

Para cada mês futuro:

calcular:

saldo inicial;

receitas;

despesas;

cartões;

compromissos;

saldo projetado.


# 129. Projeção de dezembro

Exemplo:

Saldo atual:

R$ 2.000


Receitas futuras:

R$ 5.400


Despesas:

R$ 2.000


Cartões:

R$ 1.500


Saldo projetado:

R$ 3.900


A fórmula final deve utilizar somente compromissos não duplicados.


# 130. Projeção conservadora

O sistema deve diferenciar:

receitas esperadas

de:

dinheiro já disponível.


O dashboard deve deixar isso visualmente claro.


# 131. Receitas recorrentes

V1:

não implementar mecanismo automático de recorrência.


O usuário poderá lançar manualmente suas receitas mensais.


# 132. Despesas recorrentes

V1:

não implementar geração automática de recorrências.


Parcelamentos de cartão são diferentes de recorrências.


# 133. Parcelamento não é recorrência

Uma compra parcelada possui:

origem única;

quantidade definida de parcelas.


# 134. Pagamento de fatura não é despesa adicional

Quando uma fatura é paga:

não criar uma nova despesa.


O pagamento apenas liquida compromissos já registrados.


# 135. Pagamento de fatura

O pagamento gera:

saída financeira na conta.


Não gerar:

nova despesa.


# 136. Compra no cartão

A compra gera:

despesa/obrigação.


Não gerar:

saída financeira bancária.


# 137. Conta bancária

A conta bancária representa dinheiro disponível.


# 138. Cartão

O cartão representa crédito utilizado.


Não confundir:

saldo bancário

com:

limite de crédito.


# 139. Limite do cartão

Limite disponível:

credit_limit

-
commitment


O cálculo deve considerar:

compras e parcelas ainda comprometidas.


# 140. Pagamento da fatura

Pagamento reduz:

comprometimento financeiro do cartão.


E reduz:

saldo bancário.


# 141. Estorno

Estorno reduz:

comprometimento da compra.


Se houver devolução efetiva:

aumenta saldo da conta.


# 142. Cancelamento

Cancelamento remove a obrigação ativa sem apagar histórico.


# 143. Histórico

Operações financeiras devem permanecer consultáveis.


# 144. Auditoria futura

A V1 não precisa possuir auditoria completa.


Porém:

as entidades devem possuir created_at e updated_at.


# 145. Concorrência

Duas operações simultâneas não podem gerar:

- pagamento duplicado;
- transferência duplicada;
- estorno duplicado.


# 146. Transações

Operações que envolvem múltiplas alterações devem ser transacionais.


Exemplos:

- criar parcelamento;
- pagar fatura;
- pagar despesa;
- transferência;
- estorno.


# 147. Rollback

Se qualquer etapa de uma operação transacional falhar:

nenhuma alteração parcial deve permanecer.


# 148. Integridade de parcelas

A soma inicial das parcelas deve ser igual ao total da compra.


# 149. Integridade de pagamentos

A soma dos pagamentos não pode ultrapassar o valor devido.


# 150. Integridade de estornos

A soma dos estornos não pode ultrapassar o valor da obrigação estornável.


# 151. Integridade de faturas

Uma parcela deve pertencer a uma fatura compatível com o cartão da compra.


# 152. Integridade de usuário

Todas as relações financeiras devem permanecer dentro do mesmo usuário.


# 153. Dados históricos

Não modificar retroativamente eventos financeiros apenas para corrigir uma visualização.


Correções devem gerar operações apropriadas quando necessário.


# 154. Princípio de imutabilidade

Eventos financeiros já efetivados devem ser tratados como históricos.


Exemplo:

Pagamento realizado.


Não simplesmente editar o pagamento para mudar o valor.


Futuras versões poderão possuir operações de correção.


# 155. V1 simplificada

A V1 deve ser sólida, mas não precisa implementar:

- investimentos;
- Open Banking;
- importação de extratos;
- notificações;
- PIX;
- recorrências automáticas;
- rateio;
- cadastro de pessoas;
- auditoria avançada.


# 156. Preparação futura

A arquitetura pode ser preparada para essas funcionalidades.


Mas:

não criar implementação desnecessária.


# 157. Regra de refinanciamento da fatura

A situação abaixo é importante:

Fatura:

R$ 2.000


Pagamento:

R$ 1.000


Restante:

R$ 1.000


O usuário poderá futuramente transformar esse restante em um novo compromisso parcelado.


# 158. Refinanciamento na V1

O comportamento de refinanciamento deve ser implementado somente quando a funcionalidade correspondente for desenvolvida.


O modelo deve permanecer extensível.


# 159. Não inventar refinanciamento

O sistema não deve criar automaticamente um refinanciamento somente porque uma fatura foi parcialmente paga.


O usuário deve iniciar essa operação explicitamente.


# 160. Projeção de saldo restante

Enquanto uma fatura parcialmente paga possuir saldo restante:

esse saldo deve permanecer considerado no planejamento.


# 161. Conta a pagar de fatura

Uma fatura fechada e não totalmente paga deve aparecer como compromisso financeiro.


# 162. Fatura e despesa

O sistema deve permitir navegar:

fatura

-> parcelas

-> despesa original.


# 163. Despesa e parcelas

O sistema deve permitir navegar:

despesa

-> plano

-> parcelas

-> faturas.


# 164. Histórico de cartão

O usuário deve conseguir consultar:

compras;

parcelas;

faturas;

pagamentos;

estornos.


# 165. Dashboard

O dashboard deve apresentar pelo menos:

- saldo atual;
- receitas do período;
- despesas do período;
- contas a pagar;
- faturas;
- compromissos futuros;
- saldo projetado.


# 166. Gráficos

A V1 deve possuir gráficos para:

- receitas x despesas;
- despesas por categoria;
- evolução mensal.


# 167. Gráfico de receitas x despesas

Comparar valores efetivos do período.


Não misturar automaticamente projeções com valores realizados.


# 168. Gráfico por categoria

Mostrar distribuição das despesas por categoria.


# 169. Evolução mensal

Mostrar histórico mensal.


Exemplo:

Janeiro
Fevereiro
Março
...
Dezembro


# 170. Projeções no gráfico

Projeções devem possuir indicação visual clara de que são projeções.


# 171. Relatórios

Relatórios devem utilizar dados reais do banco.


# 172. Relatório do cartão

Deve permitir:

cartão;

período;

responsável.


# 173. Exportação

V1:

PDF.


Futuro:

Excel;

CSV.


# 174. Segurança

Toda operação deve validar:

autenticação;

autorização;

propriedade do recurso.


# 175. Validação dupla

Frontend valida para melhorar experiência.

Backend valida para garantir segurança e integridade.


# 176. Regra de autoridade

O backend é a autoridade final sobre:

- valores;
- status;
- saldos;
- parcelas;
- faturas;
- projeções.


# 177. Regra contra manipulação

O frontend não deve poder enviar:

saldo final;

limite disponível;

valor da fatura calculado;

valor pago acumulado;


como fonte de verdade.


Esses valores devem ser calculados pelo backend.


# 178. Regra contra duplicidade

O sistema deve impedir criação duplicada quando a mesma operação for enviada duas vezes.


# 179. Idempotência futura

Operações críticas poderão utilizar:

Idempotency-Key.


A arquitetura deve permitir essa evolução.


# 180. Regra de implementação

Quando uma regra financeira não estiver clara:

não assumir.


A IA deve:

1. identificar a ambiguidade;
2. explicar;
3. propor opções;
4. aguardar decisão.


# 181. Regra de alteração

Se uma nova funcionalidade exigir mudança nas regras:

atualizar este documento antes da implementação.


# 182. Regra final

O sistema deve sempre manter três conceitos separados:

DINHEIRO

COMPROMISSO

HISTÓRICO


DINHEIRO:

o que efetivamente entrou ou saiu das contas.


COMPROMISSO:

o que o usuário precisa pagar ou receber.


HISTÓRICO:

o que aconteceu e não deve ser apagado.


Esses conceitos são a base do Financial Control.