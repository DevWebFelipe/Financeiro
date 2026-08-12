# Regras de Negócio — Financial Control

## 1. Objetivo

Este documento define as regras funcionais e financeiras do sistema Financial Control.

Estas regras devem ser consideradas fonte de verdade para:

- backend;
- frontend;
- banco de dados;
- testes automatizados;
- relatórios;
- projeções;
- dashboards.

Quando houver conflito entre código e este documento, a regra deve ser revisada antes da implementação.


# 2. Princípio fundamental

O sistema deve distinguir:

1. compromisso financeiro;
2. pagamento realizado;
3. movimentação bancária efetiva.

Uma despesa criada não significa necessariamente que dinheiro saiu da conta.

Uma compra no cartão não significa que dinheiro saiu da conta.

O dinheiro sai da conta quando ocorre o pagamento correspondente.


# 3. Usuário

Todo usuário deve possuir:

- nome;
- email;
- senha;
- status ativo/inativo.


O email deve ser único.


# 4. Isolamento de dados

Um usuário só pode visualizar ou alterar dados pertencentes a ele.

Exemplo:

Usuário A não pode consultar:

- contas do usuário B;
- despesas do usuário B;
- cartões do usuário B;
- receitas do usuário B;
- metas do usuário B.


Essa regra deve ser aplicada no backend.


# 5. Contas financeiras

Uma conta representa um local onde o dinheiro efetivamente está armazenado.

Exemplos:

- Banco A;
- Banco B;
- Conta conjunta;
- Carteira pessoal;
- Poupança.


# 6. Saldo inicial

Toda conta pode possuir:

initial_balance


Esse valor representa o saldo existente antes do início do controle pelo sistema.


# 7. Saldo atual

O saldo atual deve ser calculado com base em:

saldo inicial
+
entradas efetivadas
-
saídas efetivadas


Transferências devem ser consideradas de acordo com a conta.


# 8. Conta pessoal

Uma conta do tipo:

PERSONAL_WALLET


pode representar dinheiro em espécie.


Não existe necessidade de uma entidade separada para dinheiro físico.


# 9. Receita

Uma receita possui:

- descrição;
- categoria;
- valor;
- data;
- conta;
- status.


# 10. Receita pendente

Uma receita:

PENDING


é esperada, mas ainda não recebida.


Ela:

- aparece como receita prevista;
- não aumenta o saldo da conta.


# 11. Receita recebida

Quando uma receita for recebida:

status = RECEIVED


O valor passa a fazer parte do saldo da conta.


# 12. Receita cancelada

Uma receita cancelada:

- não entra no saldo;
- não deve aparecer como receita efetivada;
- permanece registrada.


# 13. Despesa

Uma despesa possui:

- descrição;
- categoria;
- valor;
- data;
- responsável;
- forma de pagamento;
- status.


# 14. Responsável da despesa

Os valores iniciais são:

MINE
GIULIA
EDERSON
ELISIANE


Representação na interface:

MINE = Meu
GIULIA = Giulia
EDERSON = Ederson
ELISIANE = Elisiane


# 15. Responsável não é usuário

O campo responsável não representa necessariamente uma conta de usuário.

Exemplo:

O usuário Felipe pode cadastrar uma despesa:

responsible = GIULIA


Isso significa que a despesa é atribuída à Giulia para fins de controle.


# 16. Despesa sem cartão

Uma despesa sem cartão possui:

credit_card = null


Ela pode possuir uma conta diretamente.


Exemplo:

Conta:
Banco A

Despesa:
Energia elétrica

Valor:
R$ 250

Pagamento:
Banco A


# 17. Despesa com cartão

Uma despesa feita no cartão possui:

credit_card != null


A compra inicialmente não reduz o saldo bancário.


# 18. Compra no cartão

Exemplo:

Compra:
R$ 1.000

Cartão:
Cartão A

Data:
10/08


O sistema deve:

1. registrar a despesa;
2. gerar a parcela;
3. associar a parcela à fatura correta;
4. aumentar o comprometimento do cartão;
5. não reduzir o saldo bancário naquele momento.


# 19. Cartão de crédito

Cada cartão possui:

- nome;
- titular;
- limite;
- dia de fechamento;
- dia de vencimento.


# 20. Titular do cartão

O titular pode ser:

- usuário;
- outra pessoa.


Exemplo:

Cartão do sogro.

O sistema não precisa criar uma conta de usuário para o sogro.


# 21. Limite do cartão

O limite disponível é conceitualmente:

limite total
-
valor comprometido


O valor comprometido deve considerar compras ainda não liberadas pelo pagamento das respectivas faturas.


# 22. Limite após compra

Exemplo:

Limite:
R$ 5.000

Compra:
R$ 1.000


Novo limite disponível:

R$ 4.000


# 23. Limite após pagamento

Exemplo:

Limite:
R$ 5.000

Valor comprometido:
R$ 2.000

Pagamento de fatura:
R$ 1.000


O comprometimento deve ser reduzido conforme a regra de pagamento da fatura.


# 24. Fechamento do cartão

Cada cartão possui:

closing_day


O fechamento define o ciclo da fatura.


# 25. Vencimento do cartão

Cada cartão possui:

due_day


O vencimento define quando a fatura deve ser paga.


# 26. Fatura

Cada fatura pertence a:

- um cartão;
- um mês;
- um ano.


Exemplo:

Cartão A
Agosto/2026


# 27. Fatura aberta

Uma fatura aberta:

- pode receber novas compras;
- pode receber parcelas;
- possui total recalculável.


# 28. Fatura fechada

Quando chega a data de fechamento:

- a fatura é encerrada;
- novas compras pertencem à próxima fatura;
- o valor da fatura fica definido.


# 29. Regra de fechamento

A regra exata deve considerar o dia de fechamento e a data da compra.

Exemplo:

Cartão fecha dia 10.

Compra dia 09:

entra na fatura que fecha dia 10.


Compra dia 10:

deve seguir uma regra consistente definida pelo sistema.


A implementação deve evitar ambiguidade.

Preferência:

compras realizadas até o fechamento pertencem ao ciclo atual.


# 30. Compra após fechamento

Cartão fecha dia 10.

Compra dia 11.

A compra deve entrar na próxima fatura.


# 31. Fatura vencida

Se:

today > due_date

e:

paid_amount < total_amount


a fatura deve ser considerada:

OVERDUE


# 32. Pagamento de fatura

O pagamento de fatura representa saída real de dinheiro de uma conta.


# 33. Pagamento integral

Exemplo:

Fatura:
R$ 2.000

Pagamento:
R$ 2.000


Resultado:

paid_amount = 2000

status = PAID


A saída bancária deve ser:

R$ 2.000


# 34. Pagamento parcial

Exemplo:

Fatura:
R$ 2.000

Pagamento:
R$ 1.200


Resultado:

paid_amount = 1200

status = PARTIALLY_PAID


Saldo restante:

R$ 800


# 35. Segundo pagamento da fatura

Fatura:

R$ 2.000

Primeiro pagamento:

R$ 1.200


Segundo pagamento:

R$ 800


Resultado:

paid_amount = 2000

status = PAID


# 36. Pagamento acima da fatura

Na V1:

não permitir pagamento superior ao valor restante da fatura.


# 37. Pagamento parcial recorrente

Uma fatura pode possuir vários pagamentos.


# 38. Registro do pagamento

Todo pagamento deve identificar:

- conta de origem;
- valor;
- data.


# 39. Saída bancária

Quando um pagamento for confirmado:

uma movimentação financeira deve ser criada.


# 40. Compra no cartão não é saída bancária

Exemplo:

Compra:

R$ 500

Cartão:

Cartão A


Não criar saída de:

R$ 500

na conta bancária.


# 41. Pagamento da fatura é saída bancária

Quando a fatura for paga:

criar saída de:

valor efetivamente pago.


# 42. Despesa sem cartão paga

Exemplo:

Conta de internet:

R$ 150


Pagamento:

R$ 150


O sistema deve:

1. registrar pagamento;
2. criar saída financeira;
3. atualizar saldo.


# 43. Despesa sem cartão pendente

Uma despesa pendente:

- aparece em contas a pagar;
- não reduz saldo.


# 44. Contas a pagar

A tela de contas a pagar deve permitir visualizar:

- descrição;
- categoria;
- valor;
- vencimento;
- responsável;
- boleto;
- status.


# 45. Número do boleto

Quando existir:

boleto_number


deve aparecer na tela de contas a pagar.


O objetivo é permitir copiar o número rapidamente para pagamento.


# 46. Pagamento de conta

Ao pagar uma conta:

o usuário deve poder informar:

- conta utilizada;
- valor pago;
- data do pagamento.


# 47. Pagamento parcial de despesa

Uma despesa pode ser paga parcialmente.


Exemplo:

Despesa:
R$ 1.000

Pagamento:
R$ 600


Saldo:

R$ 400


Status:

PARTIALLY_PAID


# 48. Pagamento posterior

O usuário pode realizar outro pagamento:

R$ 400


A despesa passa para:

PAID


# 49. Parcelamento de compra

Ao cadastrar uma despesa parcelada:

o usuário informa:

- valor total;
- quantidade de parcelas;
- data da compra;
- cartão;
- categoria;
- responsável.


# 50. Geração das parcelas

O sistema deve gerar automaticamente todas as parcelas futuras.


Exemplo:

Valor:

R$ 1.200

Parcelas:

12


Gerar:

1/12
2/12
3/12
...
12/12


# 51. Valor das parcelas

Inicialmente:

R$ 100 por parcela.


# 52. Diferença de arredondamento

Quando a divisão não for exata:

a diferença deve ser distribuída de forma controlada.


Exemplo:

R$ 100 / 3


Resultado:

33,33
33,33
33,34


A soma deve sempre resultar em:

R$ 100,00


# 53. Alteração individual de parcela

O usuário deve poder alterar uma parcela futura.


Exemplo:

Parcela 1:

R$ 100

Parcela 2:

R$ 100

Parcela 3:

R$ 150


O sistema deve permitir.


# 54. Alteração de parcela

Quando uma parcela for alterada:

o sistema deve validar o impacto no total do parcelamento.


Não permitir inconsistência silenciosa.


# 55. Parcelas futuras

Parcelas futuras devem aparecer no planejamento financeiro.


Exemplo:

Hoje:

Agosto/2026


Compra:

12x R$ 100


O sistema deve permitir visualizar compromissos de:

Setembro
Outubro
Novembro
...
Julho/2027


# 56. Projeção

A projeção financeira deve considerar:

- receitas futuras;
- despesas futuras;
- parcelas futuras;
- compromissos de cartão;
- contas pendentes.


# 57. Projeção não altera saldo

Uma projeção não cria movimentação bancária.


# 58. Saldo projetado

Conceitualmente:

saldo atual
+
receitas previstas
-
despesas previstas


# 59. Cartão na projeção

Compras parceladas no cartão devem ser consideradas nos meses das respectivas faturas.


# 60. Exemplo de projeção

Saldo atual:

R$ 5.000


Receita prevista:

R$ 4.000


Despesas previstas:

R$ 2.000


Parcelas futuras:

R$ 1.000


Saldo projetado:

R$ 6.000


A fórmula exata deve considerar datas e não apenas totais.


# 61. Visão mensal

O sistema deve permitir selecionar:

- mês;
- ano.


E visualizar:

Receitas
Despesas
Saldo
Cartões
Faturas
Projeções


# 62. Dashboard

O dashboard deve apresentar informações relevantes de forma visual.


# 63. Indicadores

Indicadores iniciais:

- saldo total;
- receitas do mês;
- despesas do mês;
- saldo mensal;
- contas a pagar;
- faturas abertas;
- faturas vencidas;
- valor comprometido futuro.


# 64. Gráfico de receitas e despesas

O sistema deve permitir visualizar receitas e despesas por período.


# 65. Gráfico por categoria

Permitir visualizar:

despesas por categoria.


Exemplo:

Alimentação
30%

Moradia
25%

Transporte
15%


# 66. Gráfico de evolução

Permitir visualizar evolução mensal:

Receitas
Despesas
Saldo


# 67. Transferência

Uma transferência ocorre entre duas contas.


Exemplo:

Banco A:

- R$ 2.000


Transferir:

R$ 500


Banco B:

+ R$ 500

Banco A:

- R$ 500


# 68. Transferência não contabiliza como receita

Não aumentar a receita mensal.


# 69. Transferência não contabiliza como despesa

Não aumentar a despesa mensal.


# 70. Transferência entre contas

As duas contas devem pertencer ao mesmo usuário na V1.


# 71. Transferência inválida

Não permitir:

conta origem = conta destino.


# 72. Metas financeiras

O usuário pode criar metas.


Exemplos:

- Viagem;
- Reserva;
- Presente;
- Casa;
- Carro.


# 73. Meta

Uma meta possui:

- nome;
- descrição;
- valor objetivo;
- valor atual;
- prazo opcional;
- status.


# 74. Meta ativa

Uma meta ativa aparece no dashboard.


# 75. Meta concluída

Quando:

current_amount >= target_amount


a meta pode ser marcada como:

COMPLETED


# 76. Meta cancelada

Uma meta cancelada permanece registrada para histórico.


# 77. Estorno

Uma despesa estornada não deve ser apagada.


# 78. Estorno total

Exemplo:

Compra:

R$ 500


Estorno:

R$ 500


Resultado líquido:

R$ 0


# 79. Estorno parcial

Exemplo:

Compra:

R$ 500


Estorno:

R$ 200


Resultado líquido:

R$ 300


# 80. Estorno de compra no cartão

O estorno deve reduzir o compromisso financeiro correspondente.


# 81. Estorno após pagamento

Caso uma despesa já tenha sido paga e posteriormente estornada:

o sistema deve registrar a entrada financeira correspondente ao estorno.


Essa regra deve preservar a rastreabilidade.


# 82. Cancelamento

Cancelamento significa que a operação deixou de ser válida.

Não deve apagar o registro.


# 83. Cancelamento de despesa

Uma despesa:

CANCELLED


não deve:

- aparecer como despesa efetiva;
- reduzir saldo;
- aumentar comprometimento do cartão.


# 84. Estorno x cancelamento

CANCELLED:

operação invalidada.


REFUNDED:

operação ocorreu, mas posteriormente houve devolução de dinheiro.


# 85. Relatório de cartão de terceiros

O sistema deve permitir filtrar despesas por:

cartão
e
responsável.


Exemplo:

Cartão:
Ederson


Responsável:

MINE


Resultado:

todas as despesas do usuário Felipe no cartão de Ederson.


# 86. Exportação

A V1 deve permitir exportação de relatório das despesas de cartão.


Formato inicial sugerido:

PDF


O relatório deve conter:

- titular do cartão;
- período;
- descrição;
- data;
- parcela;
- valor;
- categoria;
- responsável;
- total.


# 87. Relatório para conferência

O relatório deve facilitar a conferência com o titular do cartão.


# 88. Filtro de despesas

As despesas devem poder ser filtradas por:

- período;
- categoria;
- cartão;
- responsável;
- status;
- conta;
- descrição.


# 89. Ordenação

Permitir ordenar por:

- data;
- vencimento;
- valor;
- descrição.


# 90. Busca

A busca textual deve permitir pesquisar por:

descrição.


# 91. Exclusão

Não excluir fisicamente operações financeiras relevantes.


# 92. Exclusão lógica

Quando o usuário desejar remover uma entidade cadastral:

usar:

deleted_at

quando aplicável.


# 93. Operações financeiras

Para operações financeiras:

preferir mudança de status.


Exemplo:

CANCELLED


em vez de:

DELETE


# 94. Histórico

O sistema deve preservar informações suficientes para entender o que aconteceu.


# 95. Auditoria futura

A arquitetura deve permitir futuramente implementar:

- histórico de alterações;
- usuário responsável pela alteração;
- data da alteração;
- valor anterior;
- valor novo.


Não é obrigatório implementar auditoria detalhada na V1.


# 96. Integridade

Nenhuma operação deve criar:

- saldo negativo indevido;
- pagamento duplicado;
- fatura duplicada;
- parcela duplicada;
- transferência duplicada;
- movimentação duplicada.


# 97. Idempotência

Operações financeiras críticas devem considerar risco de execução duplicada.

Especialmente:

- pagamento;
- transferência;
- estorno.


A estratégia de idempotência poderá ser implementada conforme a necessidade da API.


# 98. Concorrência

O backend deve considerar que duas requisições simultâneas podem tentar alterar:

- fatura;
- pagamento;
- limite;
- parcela.


Operações críticas devem utilizar transações.


# 99. Transações

Operações financeiras que alteram múltiplas tabelas devem utilizar transação de banco.


Exemplo:

Pagamento de fatura:

1. validar fatura;
2. validar valor;
3. criar pagamento;
4. criar vínculo com fatura;
5. criar movimentação;
6. atualizar status;
7. confirmar transação.


Se qualquer etapa falhar:

toda a operação deve ser revertida.


# 100. Pagamento de fatura parcial

Fluxo:

1. selecionar fatura;
2. informar conta;
3. informar valor;
4. validar valor restante;
5. registrar pagamento;
6. registrar saída;
7. atualizar paid_amount;
8. atualizar status.


# 101. Pagamento integral

Fluxo:

1. validar valor;
2. registrar pagamento;
3. registrar saída;
4. atualizar fatura;
5. status = PAID.


# 102. Pagamento de despesa

Fluxo:

1. selecionar despesa;
2. selecionar conta;
3. informar valor;
4. validar saldo/regra;
5. registrar pagamento;
6. registrar saída;
7. atualizar status.


# 103. Transferência

Fluxo:

1. selecionar origem;
2. selecionar destino;
3. informar valor;
4. validar contas;
5. registrar transferência;
6. saída na origem;
7. entrada no destino.


Tudo deve ocorrer na mesma transação.


# 104. Consistência

Não deve existir:

transferência registrada

sem suas movimentações correspondentes.


# 105. Consistência de fatura

Não deve existir:

invoice.paid_amount

diferente da soma dos seus pagamentos.


O valor oficial deve ser derivado ou mantido de forma consistente através de transação.


# 106. Consistência de despesa

Não deve existir:

soma dos pagamentos > valor devido.


# 107. Consistência de estorno

Não permitir:

soma dos estornos > valor original.


# 108. Valores

Todos os valores monetários devem utilizar:

BigDecimal


no Java.


# 109. Arredondamento

Nunca utilizar:

double

ou:

float


para valores financeiros.


# 110. Arredondamento

Quando uma operação exigir arredondamento:

utilizar regra explicitamente definida.

Preferência inicial:

HALF_UP


A regra poderá ser revisada caso alguma operação fiscal/financeira exija comportamento diferente.


# 111. Casas decimais

Interface:

2 casas


Banco:

4 casas


Backend:

BigDecimal


# 112. Cálculos

Cálculos financeiros críticos devem ser realizados no backend.


# 113. Frontend

O frontend deve apresentar resultados.

Não deve ser a fonte de verdade financeira.


# 114. Erros

Erros de regra de negócio devem retornar respostas HTTP apropriadas.


Exemplos:

400:
dados inválidos.


404:
registro não encontrado.


409:
conflito de negócio.


401:
não autenticado.


403:
sem permissão.


# 115. Mensagens

Mensagens retornadas pela API devem ser claras.


Exemplo:

"Não é possível pagar uma fatura com valor superior ao saldo pendente."


# 116. Valores futuros

Parcelas futuras devem existir no banco.

Não recalcular toda a história a cada consulta.


# 117. Parcelas

Ao criar um parcelamento:

todas as parcelas previstas devem ser persistidas.


# 118. Fatura futura

Se a fatura correspondente ainda não existir:

ela deve ser criada quando necessário.


A estratégia exata deve ser definida na implementação.


# 119. Planejamento de dezembro

Exemplo:

Em agosto:

Compra:
R$ 1.200
12 parcelas


O sistema deve permitir visualizar:

Dezembro:

parcela correspondente.


# 120. Planejamento

O sistema deve conseguir responder:

"Quanto já está comprometido em cada mês futuro?"


# 121. Comprometimento futuro

Deve considerar:

- parcelas;
- contas pendentes;
- receitas previstas.


# 122. Receita recorrente

A V1 não precisa implementar um mecanismo sofisticado de recorrência.

Porém, a arquitetura não deve impedir futura implementação.


# 123. Despesa recorrente

A V1 não precisa de recorrência automática para contas como:

- luz;
- internet;
- aluguel.


O usuário poderá lançar manualmente.


# 124. Evolução futura

A arquitetura deve permitir posteriormente adicionar:

- recorrências;
- investimentos;
- importação bancária;
- notificações;
- múltiplos perfis;
- permissões;
- relatórios avançados.


Não implementar agora.


# 125. Regra para IA

A IA não deve criar comportamento financeiro não especificado.

Quando uma regra não estiver definida:

1. identificar a ambiguidade;
2. explicar o impacto;
3. propor alternativas;
4. aguardar decisão.


# 126. Regra de segurança

Nunca simplificar uma regra financeira alterando o significado do dinheiro.

Exemplo:

Não transformar:

pagamento parcial de fatura

em:

despesa nova

sem uma decisão explícita do usuário.


# 127. Regra específica: parcelamento de cartão

Quando o usuário não conseguir pagar a fatura integral e decidir parcelar o saldo:

isso será tratado como uma nova operação financeira:

INVOICE_REFINANCING


A V1 deve permitir registrar essa operação como um parcelamento específico.


# 128. Parcelamento da fatura

Exemplo:

Fatura:

R$ 2.000


Pagamento:

R$ 1.000


Saldo:

R$ 1.000


O usuário poderá criar:

Parcelamento de cartão:

R$ 1.000


com:

N parcelas.


# 129. Parcelamento de cartão

O parcelamento da fatura não deve apagar a fatura original.

A fatura original deve permanecer com seu histórico.


# 130. Relação com fatura

O parcelamento deve possuir referência à operação de origem.


# 131. Parcelas do refinanciamento

Cada parcela deve possuir:

- número;
- valor;
- vencimento;
- status.


# 132. Valores diferentes

O usuário deve poder editar individualmente o valor das parcelas do parcelamento.


# 133. Exemplo

Parcelamento:

R$ 1.000


Parcelas:

1:
R$ 200

2:
R$ 200

3:
R$ 250

4:
R$ 350


Total:

R$ 1.000


# 134. Regra de soma

O sistema deve garantir:

soma das parcelas = valor refinanciado


# 135. Fatura após refinanciamento

O comportamento exato da fatura após o refinanciamento deverá ser definido na implementação funcional da V1.

A regra deve evitar:

- cobrança duplicada;
- saldo duplicado;
- comprometimento duplicado.


# 136. Princípio para refinanciamento

O valor refinanciado não pode continuar sendo cobrado como dívida original e também como nova dívida sem ajuste de saldo.


# 137. Cenário principal

O sistema deve suportar:

1. compra;
2. parcela;
3. fatura;
4. pagamento parcial;
5. saldo restante;
6. refinanciamento;
7. parcelas futuras;
8. pagamento dessas parcelas.


# 138. Regra de projeção do refinanciamento

As parcelas do refinanciamento devem aparecer nas projeções futuras.


# 139. Dashboard

O dashboard deve permitir ao usuário compreender:

"Quanto posso gastar?"


sem confundir:

saldo atual

com:

saldo realmente disponível considerando compromissos futuros.


# 140. Conceito de disponível

A V1 poderá apresentar dois valores:

Saldo atual

e

Saldo projetado.


Não assumir que os dois significam a mesma coisa.


# 141. Saldo atual

Representa:

dinheiro efetivamente disponível nas contas.


# 142. Saldo projetado

Representa:

dinheiro esperado após compromissos conhecidos.


# 143. Transparência

Quando um valor for projetado:

a interface deve indicar claramente que é uma projeção.


# 144. Regra final

O sistema deve sempre priorizar:

rastreabilidade
+
consistência
+
clareza.


O usuário deve conseguir responder:

"O que aconteceu com meu dinheiro?"

e:

"O que vai acontecer com meu dinheiro?"

sem precisar confiar em cálculos ocultos.