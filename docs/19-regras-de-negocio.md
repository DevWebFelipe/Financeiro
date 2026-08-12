# Regras de Negócio — Financial Control

## 1. Objetivo

Este documento define as regras financeiras e comportamentais do sistema.

Estas regras são obrigatórias.

A implementação deve respeitar estas regras independentemente de:

- frontend;
- backend;
- banco de dados;
- API;
- relatórios.


# 2. Princípio fundamental

O sistema deve representar a realidade financeira.

Não deve simplesmente alterar números para produzir o resultado desejado.


# 3. Usuário

Cada usuário possui seus próprios dados financeiros.


# 4. Isolamento

Um usuário nunca pode visualizar ou modificar:

- contas;
- receitas;
- despesas;
- cartões;
- faturas;
- transferências;
- metas;


de outro usuário.


# 5. User ID

O frontend nunca define o:

user_id


para operações autenticadas.


O backend obtém o usuário através do contexto de autenticação.


# 6. Segurança

Mesmo que o usuário altere manualmente uma requisição HTTP:

o backend deve impedir acesso a dados de outro usuário.


# 7. Contas

Um usuário pode possuir várias contas.


Exemplos:

- Nubank;
- Itaú;
- Caixa;
- conta conjunta;
- carteira pessoal.


# 8. Conta pessoal

Uma conta do tipo:

CASH


pode ser utilizada para representar dinheiro em espécie.


# 9. Conta inicial

Ao criar uma conta, o usuário pode informar:

initialBalance


Esse valor representa o saldo existente antes do início do controle pelo sistema.


# 10. Saldo

O saldo atual não deve ser alterado manualmente.


# 11. Saldo

O saldo deve ser consequência das movimentações financeiras registradas.


# 12. Conta desativada

Uma conta desativada:

- não pode receber novas operações;
- continua disponível no histórico;
- continua sendo considerada em relatórios históricos.


# 13. Receita

Uma receita representa dinheiro que entra para o usuário.


# 14. Receita prevista

Uma receita pode ser cadastrada como:

EXPECTED


Ela representa dinheiro esperado.


# 15. Receita recebida

Quando o dinheiro efetivamente entra na conta:

status:

RECEIVED


e deve existir movimentação financeira correspondente.


# 16. Receita

Não permitir marcar:

RECEIVED


sem registrar a entrada financeira correspondente.


# 17. Receita cancelada

Receita cancelada permanece no banco.


Não deve mais impactar:

- projeções;
- saldo esperado;
- contas a receber.


# 18. Origem da receita

Toda receita deve possuir uma identificação de origem.


Exemplos:

- salário;
- freelance;
- meta;
- outros.


# 19. Categorias

Categorias de receitas e despesas são independentes.


Exemplo:

Categoria:

Salário


Tipo:

INCOME


# 20. Categoria

Categoria desativada não pode ser utilizada em novos lançamentos.


Histórico continua preservado.


# 21. Despesa

Uma despesa representa uma obrigação financeira.


# 22. Despesa aberta

Uma despesa:

OPEN


ainda não foi paga.


# 23. Despesa paga

Uma despesa só pode se tornar:

PAID


quando o valor devido tiver sido efetivamente pago.


# 24. Regra

Não permitir simplesmente:

PUT expense.status = PAID


sem registrar pagamento.


# 25. Pagamento

Todo pagamento deve possuir:

- despesa;
- conta;
- valor;
- data.


# 26. Pagamento parcial

Uma despesa pode ser parcialmente paga.


Exemplo:

Despesa:

R$ 500


Pagamento:

R$ 200


Saldo:

R$ 300


Status:

PARTIALLY_PAID


# 27. Pagamento restante

Posteriormente:

Pagamento:

R$ 300


Status:

PAID


# 28. Pagamento maior

Não permitir pagamento superior ao valor restante sem uma regra explícita.


# 29. Múltiplos pagamentos

Uma despesa pode possuir vários pagamentos.


# 30. Histórico de pagamentos

Pagamentos nunca devem ser apagados para "corrigir" o saldo.


Se houver erro:

deve existir uma operação de correção/estorno apropriada.


# 31. Despesa sem cartão

Uma despesa pode ser:

- paga imediatamente;
- ficar em aberto para pagamento futuro.


# 32. Despesa em conta

Quando:

paymentMethod = ACCOUNT


a despesa está relacionada a uma conta.


# 33. Despesa em cartão

Quando:

paymentMethod = CREDIT_CARD


deve existir:

credit_card_id


# 34. Despesa sem forma definida

Quando:

paymentMethod = NONE


a despesa representa obrigação ainda não associada a cartão ou conta.


# 35. Exemplo

Lanche no escritório:

R$ 50


Pagamento:

final do mês


Pode ser:

paymentMethod = NONE


até o pagamento.


# 36. Boleto

Uma despesa pode possuir:

boletoNumber


# 37. Boleto

O número do boleto serve para facilitar o pagamento posterior.


# 38. Boleto

O número do boleto não altera o valor da despesa.


# 39. Data de vencimento

Despesas abertas podem possuir:

dueDate


# 40. Contas a pagar

Toda despesa não cancelada e ainda não totalmente paga deve aparecer em:

Contas a pagar.


# 41. Contas a pagar

Devem ser ordenadas prioritariamente por:

dueDate


# 42. Despesas vencidas

Se:

dueDate < today


e saldo > 0:


a despesa deve ser considerada vencida.


# 43. Estorno

Estorno é diferente de cancelamento.


# 44. Cancelamento

Cancelamento representa uma operação que deixou de existir antes de sua efetivação financeira.


# 45. Estorno

Estorno representa uma operação que ocorreu e posteriormente foi revertida.


# 46. Cancelamento

Despesa cancelada:

- permanece no banco;
- não deve impactar saldo;
- não deve impactar projeções;
- não deve consumir limite do cartão.


# 47. Estorno

Despesa estornada:

- permanece no banco;
- mantém histórico;
- não deve permanecer como valor devido;
- deve reverter seu impacto financeiro correspondente.


# 48. Histórico

Não utilizar exclusão física para remover uma despesa financeira histórica.


# 49. Parcelamento

Uma despesa pode possuir:

1 ou mais parcelas.


# 50. Despesa sem parcelamento

Uma despesa não parcelada possui:

1 parcela.


# 51. Despesa parcelada

Uma despesa parcelada deve possuir:

N parcelas.


# 52. Parcelas

Cada parcela possui:

- número;
- valor;
- vencimento;
- status;
- valor pago.


# 53. Parcelas futuras

Todas as parcelas futuras devem ser conhecidas imediatamente após a criação da despesa parcelada.


# 54. Projeção

Parcelas futuras devem entrar na projeção financeira.


# 55. Exemplo

Compra:

R$ 1.200

12x


O sistema deve saber desde o primeiro momento que existirão compromissos futuros.


# 56. Valores diferentes

As parcelas não precisam possuir o mesmo valor.


# 57. Exemplo

Parcela 1:

R$ 100


Parcela 2:

R$ 100


Parcela 3:

R$ 105


Todos os valores devem ser suportados.


# 58. Alteração de parcela

O usuário pode alterar uma parcela individual.


# 59. Parcela paga

Parcela já paga não pode ser alterada livremente.


# 60. Parcela cancelada

Parcela cancelada não deve impactar projeções.


# 61. Soma das parcelas

A soma das parcelas deve ser coerente com a despesa.


# 62. Alteração de parcela

Se a alteração fizer a soma das parcelas diferente da despesa:

o sistema deve:

- rejeitar;
- ou atualizar a despesa através de operação explícita.


A V1 deve preferir rejeitar alterações inconsistentes.


# 63. Arredondamento

A geração de parcelas deve distribuir centavos corretamente.


# 64. Cartões

Um usuário pode possuir vários cartões.


# 65. Cartão

Cartão deve possuir:

- nome;
- últimos 4 dígitos;
- limite;
- fechamento;
- vencimento.


# 66. Segurança do cartão

Nunca armazenar:

- número completo;
- CVV;
- senha.


# 67. Compra no cartão

Uma compra no cartão representa:

compromisso futuro.


# 68. Compra no cartão

A compra não reduz imediatamente o saldo bancário.


# 69. Compra no cartão

A compra aumenta a dívida do cartão.


# 70. Fatura

As compras do cartão são agrupadas em faturas.


# 71. Fechamento

O dia de fechamento determina em qual fatura uma compra será incluída.


# 72. Exemplo

Cartão:

fecha dia 10.


Compra:

09/08


Entra na fatura que fecha em:

10/08.


# 73. Exemplo

Cartão:

fecha dia 10.


Compra:

11/08


Entra na próxima fatura.


# 74. Vencimento

A fatura possui:

dueDate


# 75. Fatura aberta

Fatura ainda em ciclo pode receber novas compras.


# 76. Fatura fechada

Fatura fechada não deve receber novas compras normalmente.


# 77. Fatura paga

Fatura paga:

não possui saldo devido.


# 78. Fatura parcialmente paga

Fatura pode ser parcialmente paga.


Exemplo:

Total:

R$ 2.000


Pago:

R$ 1.200


Restante:

R$ 800


Status:

PARTIALLY_PAID


# 79. Fatura em atraso

Se:

dueDate passou

e:

remainingAmount > 0


status:

OVERDUE


# 80. Pagamento da fatura

Pagamento da fatura deve:

- registrar pagamento;
- registrar conta utilizada;
- reduzir saldo da conta;
- reduzir saldo devido da fatura.


# 81. Pagamento da fatura

Pagamento da fatura é uma saída financeira real.


# 82. Compra no cartão

Compra no cartão não deve ser contabilizada novamente como saída bancária.


# 83. Regra anti-duplicidade

Não fazer:

Compra cartão:

-100


e depois:

Pagamento fatura:

-100


como duas saídas da mesma conta.


Somente o pagamento da fatura reduz a conta bancária.


# 84. Limite do cartão

O limite disponível deve considerar compromissos ainda existentes.


# 85. Cancelamento de compra

Compra cancelada deve deixar de consumir limite.


# 86. Estorno de compra

Compra estornada deve liberar o comprometimento correspondente.


# 87. Fatura

Uma fatura deve possuir:

- total;
- total pago;
- saldo restante.


# 88. Total da fatura

O total da fatura deve ser baseado nos itens válidos da fatura.


# 89. Fatura

Não permitir que o usuário altere manualmente:

totalAmount


sem operação de negócio correspondente.


# 90. Pagamento parcial da fatura

Pagamento parcial deve preservar:

valor original;

valor pago;

saldo restante.


# 91. Parcelamento de cartão

Quando o usuário não conseguir pagar toda a fatura:

o saldo restante permanece como dívida.


# 92. Parcelamento de cartão

A V1 permitirá representar o saldo refinanciado através de uma despesa:

PARCELAMENTO_CARTAO


# 93. Parcelamento cartão

O parcelamento deve possuir:

- despesa original;
- parcelas;
- valores individuais;
- vencimentos.


# 94. Parcelamento cartão

As parcelas podem possuir valores diferentes.


# 95. Histórico do parcelamento

A fatura original não deve ser apagada.


# 96. Histórico

O sistema deve permitir identificar:

quanto era a fatura;

quanto foi pago;

quanto ficou pendente;

quanto foi refinanciado.


# 97. Evitar duplicidade

O saldo refinanciado não pode ser contado simultaneamente como:

dívida da fatura;

e nova dívida.


# 98. Regra

Ao implementar o refinanciamento:

o estado da dívida original deve ser atualizado de maneira consistente.


# 99. Transferências

Uma transferência movimenta dinheiro entre duas contas.


# 100. Transferência

Uma transferência possui:

conta origem;

conta destino;

valor;

data.


# 101. Transferência

Conta origem e destino devem ser diferentes.


# 102. Transferência

Transferência não é:

receita.


# 103. Transferência

Transferência não é:

despesa.


# 104. Patrimônio

Transferência não altera patrimônio total.


# 105. Exemplo

Conta A:

R$ 5.000


Transferência:

R$ 1.000


Conta A:

R$ 4.000


Conta B:

+R$ 1.000


Total:

continua R$ 5.000.


# 106. Transferência

A operação deve ser atômica.


# 107. Transferência

Nunca permitir:

debitar origem;

falhar antes de creditar destino.


# 108. Cancelamento de transferência

Se permitido:

a transferência original permanece registrada.


# 109. Cancelamento

O cancelamento deve reverter o impacto financeiro da transferência.


# 110. Metas

Usuário pode criar metas financeiras.


# 111. Meta

Meta possui:

- nome;
- valor objetivo;
- valor acumulado;
- data objetivo;
- status.


# 112. Contribuição

Contribuição representa dinheiro destinado à meta.


# 113. Meta

Contribuição deve possuir uma conta de origem.


# 114. Meta

O valor acumulado deve refletir contribuições válidas.


# 115. Meta concluída

Quando:

currentAmount >= targetAmount


a meta pode ser concluída.


# 116. Meta cancelada

Meta cancelada permanece no histórico.


# 117. Projeção

A projeção financeira é uma das funções principais do sistema.


# 118. Projeção

Deve considerar:

- receitas futuras;
- despesas abertas;
- parcelas futuras;
- faturas futuras;
- pagamentos previstos.


# 119. Projeção

Não considerar:

- despesas canceladas;
- despesas estornadas;
- receitas canceladas.


# 120. Projeção

O sistema deve diferenciar:

REALIZADO

PREVISTO

COMPROMETIDO


# 121. Realizado

Representa dinheiro que efetivamente entrou ou saiu.


# 122. Previsto

Representa operação esperada.


# 123. Comprometido

Representa obrigação financeira já assumida.


# 124. Exemplo

Compra parcelada no cartão:

É:

COMPROMETIDA


Mas ainda não é:

SAÍDA BANCÁRIA


# 125. Exemplo

Fatura fechada:

É:

COMPROMETIDA


Pagamento da fatura:

torna-se:

REALIZADO


# 126. Exemplo

Salário esperado:

PREVISTO


Quando recebido:

REALIZADO


# 127. Dashboard

Dashboard deve mostrar informações financeiras sem alterar dados.


# 128. Dashboard

Deve permitir visualizar:

- receitas;
- despesas;
- saldo;
- contas;
- cartões;
- faturas;
- projeções;
- metas.


# 129. Gráficos

Os gráficos devem utilizar dados calculados pelo backend.


# 130. Gráficos

Não duplicar regras financeiras no frontend apenas para produzir gráficos.


# 131. Relatórios

Relatórios devem utilizar os mesmos critérios das regras financeiras.


# 132. Responsável

Uma despesa pode ser associada a:

MINE

GIULIA

EDERSON

ELISIANE


# 133. Responsável

O responsável não representa necessariamente o proprietário da conta ou cartão.


# 134. Exemplo

Cartão do sogro:

responsible = MINE


A despesa continua pertencendo ao usuário do sistema.


# 135. Cartão de terceiros

O sistema deve permitir cadastrar um cartão utilizado pelo usuário mesmo que o cartão pertença a outra pessoa.


# 136. Exemplo

Cartão:

Ederson


Despesas:

MINE

GIULIA


O cartão continua pertencendo ao contexto financeiro do usuário.


# 137. Relatório cartão terceiro

O usuário deve poder gerar relatório da fatura.


# 138. Relatório

O relatório deve permitir separar despesas por responsável.


# 139. Exemplo

Fatura:

R$ 2.000


Meu:

R$ 800


Giulia:

R$ 500


Outros:

R$ 700


# 140. Exportação

A exportação deve manter os valores exatamente iguais aos dados do sistema.


# 141. PDF

O PDF da fatura deve apresentar:

- cartão;
- período;
- vencimento;
- despesas;
- responsável;
- categoria;
- valor;
- total.


# 142. Contas a pagar

Deve listar:

despesas abertas;

despesas parcialmente pagas;

parcelas futuras;


conforme filtro selecionado.


# 143. Contas a pagar

Não incluir:

canceladas;

estornadas;

totalmente pagas.


# 144. Vencimento

Contas devem poder ser ordenadas por:

- vencimento;
- valor;
- categoria;
- responsável.


# 145. Planejamento mensal

Usuário deve conseguir visualizar:

quanto já está comprometido em determinado mês.


# 146. Exemplo

Agosto:

R$ 2.000


Setembro:

R$ 3.500


Outubro:

R$ 3.000


O sistema deve conseguir chegar nesses valores através dos lançamentos futuros.


# 147. Planejamento

Parcelas futuras devem aparecer automaticamente.


# 148. Planejamento

Não exigir que o usuário cadastre novamente cada parcela como uma nova despesa.


# 149. Parcelamento

Criar a despesa parcelada uma única vez.


O sistema gera as parcelas.


# 150. Edição

Alterações devem respeitar o estado atual da parcela.


# 151. Parcela futura

Pode ser alterada.


# 152. Parcela paga

Possui restrições maiores de alteração.


# 153. Correção financeira

Quando uma operação já realizada estiver errada:

não sobrescrever silenciosamente.


Registrar operação de correção apropriada.


# 154. Histórico

O sistema deve preservar:

- datas;
- valores;
- pagamentos;
- estornos;
- cancelamentos.


# 155. Exclusão

Exclusão física deve ser excepcional.


# 156. Exclusão lógica

Quando necessário:

status;

active;

cancelled_at;


ou estratégia equivalente.


# 157. Integridade

O backend deve validar todas as regras.


# 158. Banco

O banco deve garantir:

foreign keys;

not null;

unique;

check constraints;


quando apropriado.


# 159. Frontend

O frontend pode impedir ações inválidas por UX.


Mas o backend deve sempre validar novamente.


# 160. Concorrência

Operações financeiras devem considerar concorrência.


# 161. Exemplo

Dois pagamentos simultâneos não podem resultar em pagamento superior ao saldo.


# 162. Transações

Operações financeiras críticas devem utilizar transações.


# 163. Idempotência

Operações de pagamento devem futuramente suportar proteção contra duplicidade.


# 164. V1

Uma estratégia simples de prevenção de duplicidade deve ser implementada quando o fluxo de pagamento for criado.


# 165. Arredondamento

Todos os cálculos financeiros devem utilizar:

BigDecimal


# 166. Arredondamento

Não utilizar:

double;

float;


para dinheiro.


# 167. Casas decimais

Valores monetários devem possuir:

2 casas decimais.


# 168. Soma

Somas devem preservar precisão até a etapa final.


# 169. Parcelamento

Arredondamento deve distribuir centavos corretamente.


# 170. Datas

Datas financeiras não devem depender do timezone do navegador.


# 171. Backend

Backend deve ser a fonte oficial para determinar:

faturas;

parcelas;

vencimentos;

projeções.


# 172. Frontend

Frontend apenas apresenta os resultados.


# 173. Fatura

O sistema deve ser capaz de responder:

"Quanto devo no cartão A?"


# 174. Fatura

Também:

"Quanto devo no cartão B?"


# 175. Projeção

E:

"Quanto já está comprometido em dezembro?"


# 176. Contas

E:

"Quanto tenho disponível em minhas contas?"


# 177. Planejamento

E:

"Quanto posso gastar sem comprometer minhas contas futuras?"


# 178. Limite

A V1 pode apresentar o comprometimento atual dos cartões.


# 179. Limite

Não precisa implementar análise avançada de crédito.


# 180. Limite

Compras futuras e parcelas devem ser consideradas no comprometimento.


# 181. V1

Não implementar:

investimentos;

Open Finance;

importação bancária;

notificações;

recorrências automáticas complexas;

multi-moeda;

criptomoedas.


# 182. Futuro

O modelo deve permitir evolução sem exigir reconstrução completa.


# 183. Não antecipar

Não implementar funcionalidades futuras somente para "deixar pronto".


# 184. Testabilidade

Toda regra importante deve possuir teste automatizado.


# 185. Testes prioritários

Especialmente:

- cálculo de parcelas;
- arredondamento;
- pagamento parcial;
- pagamento total;
- estorno;
- cancelamento;
- fatura;
- fechamento;
- vencimento;
- projeção;
- transferência;
- isolamento por usuário.


# 186. Cenário crítico

Compra parcelada:

R$ 1.000

10x


O sistema deve gerar:

10 parcelas.


# 187. Cenário crítico

Parcela:

R$ 100


Usuário paga:

R$ 50


Resultado:

PARTIALLY_PAID


Saldo:

R$ 50


# 188. Cenário crítico

Depois paga:

R$ 50


Resultado:

PAID


# 189. Cenário crítico

Fatura:

R$ 2.000


Paga:

R$ 1.000


Resultado:

PARTIALLY_PAID


# 190. Cenário crítico

Saldo:

R$ 1.000


Esse saldo não pode desaparecer.


# 191. Cenário crítico

Usuário cria parcelamento:

R$ 1.000


em:

5 parcelas


Cada parcela deve ser independente.


# 192. Cenário crítico

Usuário altera:

parcela 3:

R$ 210


As demais continuam com seus valores originais.


# 193. Cenário crítico

Compra cancelada:

não aparece como compromisso atual.


# 194. Cenário crítico

Compra estornada:

histórico permanece.


# 195. Cenário crítico

Transferência:

R$ 500


Conta A:

-500


Conta B:

+500


# 196. Cenário crítico

Transferência não deve aparecer como:

R$ 500 de receita.


# 197. Cenário crítico

Transferência não deve aparecer como:

R$ 500 de despesa.


# 198. Cenário crítico

Compra cartão:

R$ 500


Não reduzir saldo bancário.


# 199. Cenário crítico

Pagamento fatura:

R$ 500


Reduz saldo bancário.


# 200. Regra final

O sistema deve sempre responder corretamente às três perguntas:

1. Quanto dinheiro eu tenho agora?
2. Quanto dinheiro já está comprometido?
3. Quanto dinheiro provavelmente terei disponível no futuro?


Essas três informações nunca devem ser confundidas.