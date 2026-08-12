# Ciclo de Cartão, Fatura e Parcelamento — Financial Control

## 1. Objetivo

Este documento define o comportamento do sistema para:

- compras no cartão;
- compras parceladas;
- parcelas;
- faturas;
- fechamento;
- vencimento;
- pagamentos;
- pagamentos parciais;
- parcelamento de fatura;
- estornos;
- cancelamentos.

Cartões de crédito representam uma das partes mais importantes do Financial Control.

As regras deste documento devem ser implementadas de forma consistente no backend.


# 2. Conceitos

O sistema diferencia claramente:

Compra
Parcela
Fatura
Pagamento
Conta bancária

Esses conceitos não devem ser tratados como a mesma coisa.


# 3. Compra

Uma compra representa uma obrigação financeira assumida pelo usuário.

Exemplo:

01/08/2026

Compra:
Mercado

Valor:
R$ 500,00

Cartão:
Cartão A


Essa compra não representa saída imediata da conta bancária.


# 4. Compra à vista

Uma compra à vista no cartão possui:

1 compra
1 parcela

Exemplo:

Valor:
R$ 500,00

Parcela:
1/1

A parcela será vinculada à fatura correspondente.


# 5. Compra parcelada

Exemplo:

Compra:
R$ 1.200,00

Parcelamento:
12x

O sistema deve criar:

1 InstallmentPlan

12 Installments


Exemplo:

1/12
2/12
3/12
...
12/12


# 6. Valor das parcelas

O sistema deve permitir valores diferentes entre parcelas.

Exemplo:

Compra:
R$ 1.000,00

Parcelas:

1/3 = R$ 300,00
2/3 = R$ 300,00
3/3 = R$ 400,00


A soma das parcelas deve ser:

R$ 1.000,00


# 7. Alteração de parcela

Parcelas futuras podem ter seus valores alterados.

Exemplo:

Original:

1/3 = R$ 333,33
2/3 = R$ 333,33
3/3 = R$ 333,34

Usuário altera:

3/3 = R$ 350,00


O sistema deve identificar que o parcelamento sofreu alteração.

A alteração não deve apagar o histórico original quando isso for necessário para auditoria futura.


# 8. Regra de fechamento

Cada cartão possui:

closing_day

Exemplo:

closing_day = 10


A compra será direcionada à fatura conforme sua data e o ciclo do cartão.


# 9. Regra para compra antes do fechamento

Se o cartão fecha no dia 10:

Compra:
09/08

A compra pertence ao ciclo que fecha em:

10/08


# 10. Regra para compra depois do fechamento

Se o cartão fecha no dia 10:

Compra:
11/08

A compra pertence ao próximo ciclo.


# 11. Regra para compra no dia do fechamento

Compra realizada exatamente no dia de fechamento deve seguir uma regra determinística.

Regra V1:

A compra realizada no próprio dia do fechamento pertence à fatura que está sendo fechada.

Exemplo:

Fechamento:
10/08

Compra:
10/08

Resultado:

Fatura fechada em 10/08.


Essa regra deverá ser aplicada de forma consistente.


# 12. Horário

A V1 trabalha prioritariamente com datas.

Não será necessário considerar horário da compra para determinar o ciclo.

A data da compra será suficiente.


# 13. Fatura

Uma fatura representa o conjunto de obrigações do ciclo de um cartão.

Exemplo:

Cartão:
Cartão A

Ciclo:
11/07 até 10/08

Vencimento:
15/08


# 14. Fatura aberta

Uma fatura aberta:

- ainda pode receber compras;
- pode ter seu valor alterado;
- não foi finalizada.


# 15. Fatura fechada

Uma fatura fechada:

- não deve receber novas compras;
- possui ciclo encerrado;
- aguarda pagamento.


# 16. Vencimento

O vencimento é definido pelo cartão.

Exemplo:

closing_day:
10

due_day:
15


Fatura referente ao ciclo encerrado em 10/08:

Vencimento:
15/08


# 17. Mês com quantidade diferente de dias

O sistema deve tratar corretamente cartões configurados com dias como:

29
30
31

Quando o mês não possuir o dia configurado, deve ser aplicada uma regra determinística.

Regra V1:

Utilizar o último dia válido do mês.

Exemplo:

closing_day:
31

Fevereiro:

fechamento no último dia de fevereiro.


# 18. Fatura automática

O sistema deve conseguir determinar a fatura de uma compra sem exigir que o usuário escolha manualmente a fatura.

O usuário escolhe:

Cartão

O sistema determina:

Fatura


# 19. Fatura futura

Se uma compra parcelada gerar parcelas futuras, as faturas futuras devem ser criadas ou determinadas de forma que o sistema consiga projetar os valores.

A implementação pode escolher entre:

- criar todas as faturas antecipadamente;
- criar as faturas sob demanda;

desde que o comportamento externo seja equivalente e as projeções funcionem corretamente.


# 20. Regra de parcela e fatura

Cada parcela deve pertencer a uma única fatura.

Exemplo:

Compra:
12x

Parcela 1:
Fatura Agosto

Parcela 2:
Fatura Setembro

Parcela 3:
Fatura Outubro


# 21. Projeção do cartão

O usuário deve conseguir visualizar compromissos futuros.

Exemplo:

Agosto:
R$ 1.500

Setembro:
R$ 1.100

Outubro:
R$ 800

Novembro:
R$ 600

Dezembro:
R$ 400


Esses valores devem considerar as parcelas futuras conhecidas.


# 22. Limite do cartão

O limite do cartão representa o crédito total disponibilizado.

Exemplo:

Limite:
R$ 5.000


O sistema deve ser capaz de apresentar:

- limite total;
- limite comprometido;
- limite disponível.


# 23. Limite comprometido

O limite comprometido deve considerar compras ainda não quitadas conforme as regras financeiras do cartão.

Na V1, o cálculo deve considerar principalmente:

- compras abertas;
- parcelas futuras;
- valores ainda devidos.

Pagamentos de fatura devem reduzir o valor comprometido conforme apropriado.


# 24. Limite disponível

Conceitualmente:

limite disponível =
limite total
-
limite comprometido


Nunca permitir que o cálculo utilize valores negativos sem que isso seja explicitamente representado como excesso de limite.


# 25. Pagamento da fatura

O pagamento da fatura representa saída real de dinheiro.

Exemplo:

Fatura:
R$ 2.000

Conta:
Santander

Pagamento:
R$ 2.000


Resultado:

Conta Santander:
- R$ 2.000

Fatura:
PAGA


# 26. Pagamento parcial

O usuário pode pagar somente parte da fatura.

Exemplo:

Fatura:
R$ 2.000

Pagamento:
R$ 500

Resultado:

Pago:
R$ 500

Restante:
R$ 1.500

Status:
PARCIALMENTE_PAGA


# 27. Múltiplos pagamentos

Uma fatura pode receber vários pagamentos.

Exemplo:

Fatura:
R$ 2.000

Pagamento 1:
R$ 500

Pagamento 2:
R$ 500

Pagamento 3:
R$ 1.000


Resultado:

Total pago:
R$ 2.000

Status:
PAGA


# 28. Pagamento acima do valor da fatura

A V1 não deve permitir pagamento superior ao saldo devido sem uma regra explícita para crédito excedente.

Por segurança:

pagamento > saldo restante

deve ser rejeitado inicialmente.


# 29. Pagamento em conta

Todo pagamento real de fatura deve indicar:

- conta;
- valor;
- data.


# 30. Fatura vencida

Se a data atual ultrapassar o vencimento e a fatura não estiver totalmente paga:

Status:

VENCIDA

Caso tenha pagamento parcial:

VENCIDA

ou estado equivalente que preserve:

- valor total;
- valor pago;
- saldo restante.


A representação exata poderá ser refinada na implementação.


# 31. Parcelamento de fatura

O parcelamento de fatura ocorre quando o usuário não consegue pagar o saldo integral.

Exemplo:

Fatura:
R$ 2.000

Pagamento:
R$ 500

Saldo:
R$ 1.500

Usuário decide parcelar:

R$ 1.500 em 3x


O sistema deve registrar essa operação sem apagar a fatura original.


# 32. Parcelamento de saldo

O valor parcelado deve representar o saldo que foi efetivamente transformado em parcelamento.

Não assumir automaticamente que o valor é igual ao valor original da fatura.


# 33. Taxas do parcelamento

A V1 não possui um módulo completo de juros e taxas bancárias.

Caso seja necessário registrar um valor final diferente do saldo parcelado, o sistema deve permitir que as parcelas possuam seus próprios valores.

Exemplo:

Saldo parcelado:
R$ 1.500

Parcelas:

1/3 = R$ 550
2/3 = R$ 550
3/3 = R$ 550

Total:
R$ 1.650


O acréscimo de R$ 150 pode ser representado através dos valores das parcelas.

Uma modelagem detalhada de juros poderá ser criada futuramente.


# 34. Identificação do parcelamento de fatura

As parcelas de refinanciamento devem ser identificáveis.

Exemplo:

Tipo do parcelamento:

INVOICE_REFINANCING


Isso permite distinguir:

Compra normal
versus
Parcelamento de fatura.


# 35. Fatura após parcelamento

A fatura original deve continuar existindo no histórico.

O saldo transformado em parcelamento não deve continuar sendo cobrado como se fosse uma obrigação independente da mesma fatura.

O mecanismo exato de representação deve garantir que o saldo não seja contabilizado duas vezes.


# 36. Projeção após parcelamento

Depois do parcelamento:

Fatura atual:
saldo original convertido conforme operação


Meses futuros:
parcelas do refinanciamento


A projeção deve considerar somente o compromisso válido em cada período.


# 37. Estorno

Um estorno representa a reversão total ou parcial de uma compra.

Exemplo:

Compra:
R$ 500

Estorno:
R$ 500


Resultado:

Impacto líquido:
R$ 0


# 38. Estorno antes do fechamento

Se a compra for estornada antes do fechamento da fatura:

A fatura deve refletir o estorno.

O resultado líquido deve ser:

Compra:
+ R$ 500

Estorno:
- R$ 500

Total:
R$ 0


# 39. Estorno depois do fechamento

Se a fatura já estiver fechada, o estorno deve permanecer identificável.

O sistema deve registrar o crédito/estorno sem alterar silenciosamente o histórico da fatura original.


# 40. Estorno após pagamento

Se uma compra for estornada depois que a fatura já foi paga, o sistema deverá representar o crédito resultante.

Esse crédito poderá ser utilizado para abater faturas futuras conforme evolução futura do sistema.

Na V1, o importante é preservar corretamente o histórico e o valor do crédito.


# 41. Estorno parcial

A arquitetura deve permitir futuramente:

Compra:
R$ 500

Estorno:
R$ 200

Resultado líquido:
R$ 300


A implementação completa do estorno parcial não é obrigatória na primeira versão.


# 42. Cancelamento

Cancelamento é diferente de estorno.

Cancelamento:

A operação é invalidada antes de produzir ou consolidar seus efeitos financeiros.

Estorno:

A operação existiu e posteriormente foi revertida.


# 43. Compra cancelada

Uma compra cancelada:

- permanece no histórico;
- não deve continuar comprometendo a projeção;
- não deve permanecer como obrigação válida.


# 44. Parcela cancelada

Quando uma parcela for cancelada, o sistema deve garantir que ela não continue sendo contabilizada como obrigação futura.


# 45. Fatura cancelada

A V1 não precisa de um fluxo completo de cancelamento de fatura.

Caso seja necessário no futuro, a operação deverá preservar o histórico.


# 46. Competência

A competência financeira deve ser baseada nas datas reais da operação.

Para cartão:

A competência da compra é determinada pela fatura.

Para conta:

A competência da movimentação é determinada pela data da movimentação.


# 47. Dashboard de cartões

O dashboard deve conseguir apresentar:

- limite total;
- limite utilizado;
- limite disponível;
- fatura atual;
- próxima fatura;
- valor de faturas futuras;
- valor vencido;
- valor pago;
- valor restante.


# 48. Visão mensal

O usuário deve conseguir selecionar um mês.

Exemplo:

Dezembro/2026


O sistema deverá conseguir mostrar:

Cartões:

Cartão A:
R$ 1.200

Cartão B:
R$ 800

Despesas sem cartão:
R$ 600

Receitas:
R$ 5.000

Saldo projetado:
R$ 2.400


# 49. Responsável

Cada compra/despesa deve possuir responsável.

Exemplo:

Compra:
R$ 300

Responsável:
Ederson


Ao exportar a fatura:

Filtro:
Ederson


Resultado:

somente aquela compra.


# 50. Compra compartilhada

A V1 não implementará divisão de uma compra entre múltiplos responsáveis.

Exemplo:

Compra:
R$ 300

Felipe:
R$ 150

Giulia:
R$ 150

Esse cenário fica para uma evolução futura.

Na V1 uma despesa possui apenas um responsável.


# 51. Compra sem cartão

Uma compra sem cartão não participa de fatura.

Exemplo:

Mercado:
R$ 500

Forma:
Conta bancária

Resultado:

Despesa:
R$ 500

Pagamento:
Conta X

FinancialTransaction:
SAIDA R$ 500


# 52. Compra sem cartão e pendente

Exemplo:

Lanche no escritório:
R$ 100

Vencimento:
31/08

Status:
PENDENTE

Resultado:

Não altera saldo.

Aparece em contas a pagar.

Participa da projeção.


# 53. Pagamento de despesa pendente

Quando o usuário pagar:

Despesa:
PAGA

Payment:
criado

FinancialTransaction:
SAIDA

Saldo:
reduzido.


# 54. Boleto

Quando uma despesa possuir boleto:

boleto_number

deve ser armazenado.

A interface deverá permitir:

- visualizar;
- copiar.


# 55. Relatório de fatura

O relatório de fatura deve permitir:

- selecionar cartão;
- selecionar fatura;
- selecionar responsável;
- visualizar total;
- visualizar despesas filtradas;
- exportar.


# 56. Conferência

O relatório deve facilitar conferência com o titular do cartão.

Exemplo:

Cartão:
Ederson

Fatura:
Agosto/2026

Responsável:
Meu


Resultado:

Descrição
Data
Categoria
Parcela
Valor


# 57. Regra de duplicidade

Uma compra não pode aparecer duas vezes na mesma fatura sem que existam dois registros financeiros reais.

Uma parcela deve possuir uma única associação principal à fatura.


# 58. Regra de consistência

O total da fatura deve ser compatível com a soma dos seus itens financeiros.

Quando existirem:

- créditos;
- estornos;
- ajustes;

eles devem participar do cálculo conforme seu tipo.


# 59. Regra de arredondamento

Todos os cálculos monetários devem utilizar BigDecimal.

O arredondamento deve ser explícito.

Não utilizar double para cálculos financeiros.


# 60. Transações

As seguintes operações devem ser transacionais:

- criação de compra parcelada;
- pagamento de despesa;
- pagamento de fatura;
- pagamento parcial;
- parcelamento de fatura;
- transferência;
- estorno quando envolver múltiplas entidades.


# 61. Concorrência

O sistema deve impedir situações como:

Dois pagamentos simultâneos consumirem o mesmo saldo de fatura.


A estratégia exata de locking será definida durante a implementação quando necessário.


# 62. Idempotência

Operações críticas de pagamento devem possuir proteção contra duplicidade.

Uma repetição acidental da mesma requisição não deve gerar dois pagamentos.


# 63. Histórico

O sistema deve permitir reconstruir a história financeira de uma operação.

Exemplo:

Compra
    ->
Parcela
    ->
Fatura
    ->
Pagamento
    ->
Estorno


# 64. Regra de não duplicidade

Um valor financeiro não pode ser contabilizado duas vezes.

Exemplo incorreto:

Compra no cartão:
R$ 500

FinancialTransaction:
SAIDA R$ 500

antes do pagamento da fatura.


Isso não deve ocorrer.

A saída bancária somente ocorre no pagamento da fatura.


# 65. Projeção de dezembro

Exemplo:

Estamos em agosto.

O usuário possui:

Cartão A:
R$ 500 em setembro
R$ 400 em outubro
R$ 300 em novembro
R$ 200 em dezembro

Cartão B:
R$ 700 em dezembro

Despesas pendentes:
R$ 300 em dezembro

A projeção de dezembro deve considerar:

Cartão A:
R$ 200

Cartão B:
R$ 700

Despesas:
R$ 300

Total comprometido:
R$ 1.200


# 66. Projeção com receitas

Se houver:

Receita prevista:
R$ 5.000

Compromissos:
R$ 1.200

Saldo projetado:

R$ 3.800


# 67. Projeção não altera saldo

A projeção é apenas uma visão calculada.

Ela não deve criar:

- FinancialTransaction;
- Payment;
- Expense;
- Income.

# 68. Regra de implementação

Toda regra de cartão/fatura deve possuir testes automatizados.

Especialmente:

- compra antes do fechamento;
- compra no fechamento;
- compra depois do fechamento;
- mudança de mês;
- fevereiro;
- parcelamento;
- parcelas futuras;
- valores diferentes;
- pagamento integral;
- pagamento parcial;
- múltiplos pagamentos;
- parcelamento de saldo;
- estorno;
- cancelamento.


# 69. Regra de alteração

Caso a implementação revele que alguma regra acima é insuficiente:

A IA deve:

1. parar a implementação daquela regra;
2. explicar o caso;
3. propor solução;
4. atualizar este documento após aprovação;
5. somente então implementar.


# 70. Objetivo

O objetivo deste modelo é garantir que o sistema responda corretamente às perguntas práticas do usuário:

"Quanto estou devendo?"

"Quanto tenho no cartão?"

"Quanto tenho para pagar este mês?"

"Quanto já está comprometido em dezembro?"

"Quanto ainda posso gastar?"

"Quanto da fatura é meu?"

"Quanto devo para o titular do cartão?"

"Quanto já paguei?"

"Quanto ainda falta pagar?"

"Se eu parcelar o saldo, quanto terei nos próximos meses?"

As respostas devem ser baseadas em dados financeiros consistentes e rastreáveis.