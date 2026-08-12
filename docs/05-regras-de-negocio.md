# Regras de Negócio — Financial Control

## 1. Objetivo

Este documento define as regras de negócio do Financial Control.

As regras aqui descritas devem ser consideradas a fonte principal para implementação das funcionalidades financeiras.

Quando houver conflito entre uma implementação e este documento, a regra de negócio deve prevalecer.

Caso uma regra esteja ambígua ou incompleta, a IA não deve inventar um comportamento.

Deve:

1. identificar a ambiguidade;
2. explicar o problema;
3. apresentar uma proposta;
4. solicitar aprovação antes de alterar a regra.


# 2. Usuários

## RN-001 — Identificação

Todo usuário possui um UUID único.

## RN-002 — Isolamento

Todos os dados financeiros devem estar vinculados a um usuário.

Um usuário nunca pode consultar, alterar ou excluir logicamente dados pertencentes a outro usuário.

## RN-003 — Autenticação

O usuário autenticado é a fonte oficial do `userId`.

O frontend não pode determinar livremente o usuário proprietário de um registro.


# 3. Contas

## RN-004 — Conta pertence ao usuário

Toda conta pertence a exatamente um usuário.

## RN-005 — Múltiplas contas

Um usuário pode possuir várias contas.

Exemplo:

- Banco A;
- Banco B;
- Poupança;
- Carteira pessoal.

## RN-006 — Conta pessoal

Uma conta do tipo `CONTA_PESSOAL` poderá ser utilizada para representar dinheiro em espécie.

Não será criado um módulo separado para dinheiro em espécie na V1.

## RN-007 — Conta inativa

Uma conta pode ser desativada.

Uma conta inativa não deve aparecer como opção para novas movimentações.

O histórico da conta deve permanecer disponível.

## RN-008 — Saldo inicial

Toda conta pode possuir um saldo inicial.

O saldo inicial representa a situação financeira da conta no momento em que ela foi cadastrada.

## RN-009 — Saldo atual

O saldo atual deve ser calculado considerando:

saldo inicial
+
entradas
-
saídas

Transferências devem afetar corretamente as contas de origem e destino.

## RN-010 — Saldo não deve depender de valor manual

O sistema não deve depender de um campo de saldo atualizado manualmente como única fonte da verdade.

As movimentações financeiras devem ser a fonte principal.


# 4. Categorias

## RN-011 — Categorias por usuário

Cada usuário pode possuir suas próprias categorias.

## RN-012 — Tipo de categoria

Uma categoria deve ser classificada como:

- INCOME;
- EXPENSE.

## RN-013 — Categoria pai

Uma categoria pode possuir outra categoria como pai.

Exemplo:

Alimentação
- Mercado
- Restaurante
- Lanche

## RN-014 — Categoria em uso

Uma categoria que já possui lançamentos não deve ser fisicamente excluída.

Ela deve ser desativada.

## RN-015 — Categoria desativada

Categorias desativadas não devem aparecer para novos lançamentos.

Lançamentos históricos continuam vinculados à categoria.


# 5. Receitas

## RN-016 — Cadastro de receita

Uma receita deve possuir:

- descrição;
- valor;
- categoria;
- conta;
- data;
- status.

## RN-017 — Receita prevista

Uma receita com status `PREVISTA` não altera o saldo real da conta.

Ela deve participar das projeções.

## RN-018 — Receita recebida

Quando uma receita for marcada como `RECEBIDA`:

- deve ser registrada uma entrada financeira;
- o saldo da conta deve ser afetado.

## RN-019 — Receita cancelada

Uma receita cancelada não deve afetar o saldo.

O histórico deve permanecer.

## RN-020 — Alteração de receita recebida

Uma receita já recebida não deve ser simplesmente alterada de forma que gere inconsistência no histórico financeiro.

Caso a alteração altere o valor efetivamente recebido, deve existir uma estratégia de ajuste financeiro.


# 6. Despesas

## RN-021 — Cadastro

Uma despesa deve possuir pelo menos:

- descrição;
- valor;
- categoria;
- data;
- responsável;
- status.

## RN-022 — Responsável

O responsável pode ser:

0 - Meu
1 - Giulia
2 - Ederson
3 - Elisiane

O responsável é apenas uma classificação financeira.

Não representa necessariamente um usuário do sistema.

## RN-023 — Despesa sem cartão

Uma despesa pode ser cadastrada sem cartão.

Exemplos:

- conta de luz;
- internet;
- mercado pago em dinheiro;
- almoço;
- lanche no escritório;
- boleto.

## RN-024 — Despesa com conta

Uma despesa que será paga diretamente de uma conta bancária pode possuir uma conta associada.

## RN-025 — Despesa com cartão

Uma despesa de cartão deve possuir um cartão associado.

A compra não gera saída bancária imediatamente.

## RN-026 — Despesa pendente

Uma despesa pendente:

- não afeta o saldo real;
- aparece nas contas a pagar;
- participa das projeções.

## RN-027 — Despesa paga

Quando uma despesa for paga:

- deve existir um pagamento;
- deve existir uma movimentação financeira de saída;
- o saldo da conta deve ser reduzido.

## RN-028 — Despesa estornada

Uma despesa estornada:

- permanece no histórico;
- deixa de representar um compromisso financeiro válido;
- deve possuir registro do estorno quando necessário;
- não deve continuar impactando projeções como despesa válida.

## RN-029 — Despesa cancelada

Uma despesa cancelada:

- permanece no banco;
- não deve impactar saldo;
- não deve aparecer como despesa pendente;
- deve permanecer disponível no histórico.

## RN-030 — Não utilizar DELETE físico

Não excluir fisicamente despesas que já tenham sido registradas financeiramente.

## RN-031 — Boleto

Uma despesa que possua boleto pode armazenar:

`boleto_number`

Esse campo serve para facilitar o pagamento posteriormente.

O número do boleto deve poder ser copiado pela interface.


# 7. Parcelamentos

## RN-032 — Criação de parcelamento

Quando uma compra for parcelada, o sistema deve criar todas as parcelas futuras conhecidas.

Exemplo:

Compra de R$ 1.200 em 12x.

Devem existir 12 parcelas.

## RN-033 — Parcelas futuras

As parcelas futuras devem estar disponíveis imediatamente para projeções.

Isso permite responder:

"Quanto já tenho comprometido em dezembro?"

## RN-034 — Número da parcela

Cada parcela deve possuir:

- número;
- quantidade total;
- valor;
- vencimento;
- status.

Exemplo:

1/12
2/12
3/12

## RN-035 — Valores diferentes

O sistema deve permitir alterar individualmente o valor das parcelas.

Exemplo:

1/3 = R$ 100
2/3 = R$ 120
3/3 = R$ 150

## RN-036 — Soma das parcelas

A soma das parcelas deve ser conhecida e validada.

O sistema não deve assumir que todas as parcelas possuem o mesmo valor.

## RN-037 — Alteração de parcela

Uma parcela futura pode ter seu valor alterado.

A alteração deve preservar o histórico da operação.

## RN-038 — Parcela paga

Uma parcela paga deve possuir um pagamento associado quando o pagamento representar uma movimentação financeira real.


# 8. Cartão de crédito

## RN-039 — Cadastro

Um cartão deve possuir:

- nome;
- titular;
- limite;
- dia de fechamento;
- dia de vencimento;
- status.

## RN-040 — Titular

O titular é um texto livre.

Exemplos:

- Felipe;
- Giulia;
- Ederson;
- Elisiane.

Não é necessário criar cadastro de titular na V1.

## RN-041 — Múltiplos cartões

Um usuário pode possuir vários cartões.

## RN-042 — Cartão inativo

Um cartão inativo não deve aparecer para novas compras.

O histórico deve permanecer.


# 9. Compras no cartão

## RN-043 — Compra não é pagamento

Uma compra no cartão não representa uma saída bancária imediata.

Ela representa uma obrigação futura.

## RN-044 — Compra à vista

Uma compra à vista no cartão deve entrar na fatura correspondente.

Exemplo:

Compra:
R$ 100

Fatura:
R$ 100

Conta bancária:
sem alteração no momento da compra.

## RN-045 — Compra parcelada

Uma compra parcelada deve gerar parcelas futuras.

Essas parcelas devem ser distribuídas pelas faturas correspondentes.

## RN-046 — Projeção

As parcelas futuras devem aparecer nas projeções das respectivas competências.


# 10. Faturas

## RN-047 — Fatura por cartão

Cada fatura pertence a um cartão.

## RN-048 — Competência

A fatura deve possuir mês e ano de referência.

## RN-049 — Fechamento

Cada cartão possui dia de fechamento.

Uma compra realizada antes ou depois do fechamento deve ser direcionada para a fatura correspondente conforme a regra de fechamento.

## RN-050 — Vencimento

Cada cartão possui dia de vencimento.

A fatura deve possuir uma data de vencimento calculada a partir da configuração do cartão e da competência.

## RN-051 — Fatura aberta

Enquanto estiver aberta, a fatura pode receber novas compras.

## RN-052 — Fatura fechada

Depois do fechamento, novas compras devem ser direcionadas para a próxima fatura.

## RN-053 — Fatura paga

Uma fatura somente deve ser considerada totalmente paga quando:

valor pago >= valor devido

## RN-054 — Fatura parcialmente paga

Se:

0 < valor pago < valor devido

o status deve ser:

`PARCIALMENTE_PAGA`

## RN-055 — Fatura não paga

Se nenhuma parte tiver sido paga e o vencimento tiver passado:

`VENCIDA`

## RN-056 — Fatura em aberto

Uma fatura aberta ainda pode receber novas compras.


# 11. Pagamento de fatura

## RN-057 — Pagamento integral

Quando uma fatura for paga integralmente:

- registrar pagamento;
- registrar saída na conta;
- atualizar valor pago;
- marcar fatura como paga.

## RN-058 — Pagamento parcial

Quando uma fatura for paga parcialmente:

- registrar o valor efetivamente pago;
- registrar saída na conta;
- atualizar valor pago;
- manter saldo restante;
- manter status parcialmente pago.

## RN-059 — Conta de pagamento

O pagamento da fatura deve indicar de qual conta o dinheiro saiu.

## RN-060 — Histórico

O sistema deve preservar os pagamentos realizados.

Não sobrescrever silenciosamente pagamentos anteriores.

## RN-061 — Múltiplos pagamentos

Uma fatura pode possuir mais de um pagamento.

Exemplo:

Fatura:
R$ 2.000

Pagamento 1:
R$ 1.000

Pagamento 2:
R$ 500

Saldo:
R$ 500


# 12. Parcelamento de fatura

## RN-062 — Motivação

O parcelamento de fatura existe para situações em que o usuário não consegue pagar o valor integral.

## RN-063 — Preservação

O parcelamento não deve apagar as compras originais.

Devem permanecer:

- compras;
- fatura;
- pagamentos;
- saldo;
- parcelamento.

## RN-064 — Saldo parcelado

O valor utilizado para o parcelamento deve corresponder ao saldo restante da fatura, conforme a operação realizada.

## RN-065 — Parcelas

O parcelamento deve gerar parcelas futuras.

## RN-066 — Projeção

As parcelas do parcelamento devem participar das projeções futuras.

## RN-067 — Identificação

As parcelas de parcelamento de fatura devem ser identificáveis como diferentes de compras normais.


# 13. Estornos

## RN-068 — Estorno não é exclusão

Uma compra estornada deve permanecer no histórico.

## RN-069 — Estorno parcial

A arquitetura deve permitir futuramente estornos parciais.

Não é obrigatório implementar a funcionalidade de estorno parcial na V1.

## RN-070 — Impacto na fatura

Um estorno deve reduzir o impacto financeiro da compra correspondente.

## RN-071 — Histórico do estorno

Deve ser possível identificar que determinada operação foi estornada.


# 14. Cancelamentos

## RN-072 — Cancelamento

Uma despesa pode ser cancelada antes de produzir efeitos financeiros definitivos.

## RN-073 — Histórico

O cancelamento não deve apagar o registro.


# 15. Pagamentos de despesas

## RN-074 — Despesa paga

Quando uma despesa sem cartão for paga:

Despesa
    ->
Pagamento
    ->
FinancialTransaction SAIDA

## RN-075 — Conta

O pagamento deve informar a conta utilizada.

## RN-076 — Valor

O valor pago pode ser diferente do valor originalmente previsto quando o domínio permitir.

O sistema deve preservar o valor original e o valor efetivamente pago quando houver diferença relevante.


# 16. Transferências

## RN-077 — Transferência

Uma transferência move dinheiro entre duas contas do mesmo usuário.

## RN-078 — Contas diferentes

Conta origem e conta destino devem ser diferentes.

## RN-079 — Operação única

A transferência deve ser tratada como uma operação única.

## RN-080 — Efeito

Exemplo:

Transferência:
R$ 1.000

Conta A:
- R$ 1.000

Conta B:
+ R$ 1.000

## RN-081 — Atomicidade

A saída e a entrada devem ser registradas na mesma transação de banco.

Se uma falhar, ambas devem ser revertidas.


# 17. Projeções

## RN-082 — Objetivo

Projeções existem para permitir planejamento financeiro.

## RN-083 — Dados considerados

As projeções podem considerar:

- receitas previstas;
- despesas pendentes;
- parcelas futuras;
- faturas futuras;
- parcelamentos de fatura.

## RN-084 — Dados excluídos

Não considerar:

- despesas canceladas;
- despesas estornadas;
- receitas canceladas;
- operações inválidas.

## RN-085 — Saldo projetado

Conceitualmente:

saldo projetado =
saldo atual
+
receitas futuras
-
despesas futuras


## RN-086 — Cartões

Para cartões, considerar as parcelas futuras e os valores das faturas ainda não pagas.

## RN-087 — Competência

As projeções devem permitir visualizar valores por:

- mês;
- período.

Exemplo:

Agosto/2026
Setembro/2026
Outubro/2026
Novembro/2026
Dezembro/2026


# 18. Dashboard

## RN-088 — Visão geral

O dashboard deve apresentar informações úteis para tomada de decisão.

Inicialmente considerar:

- saldo total;
- saldo por conta;
- receitas do período;
- despesas do período;
- despesas por categoria;
- valores de cartões;
- faturas abertas;
- contas a pagar;
- projeções futuras;
- metas.


# 19. Contas a pagar

## RN-089 — Listagem

O sistema deve permitir visualizar despesas ainda não pagas.

## RN-090 — Informações

A listagem deve apresentar pelo menos:

- descrição;
- categoria;
- valor;
- vencimento;
- responsável;
- conta/cartão;
- número do boleto quando existir.

## RN-091 — Boleto

O usuário deve conseguir copiar o número do boleto sem precisar localizar fisicamente a conta.


# 20. Responsável pela despesa

## RN-092 — Classificação

Toda despesa deve possuir um responsável.

Valores:

0 - Meu
1 - Giulia
2 - Ederson
3 - Elisiane

## RN-093 — Relatórios

Relatórios devem permitir filtrar despesas por responsável.

Exemplo:

"Mostrar somente minhas despesas na fatura do Ederson."


# 21. Relatório de fatura

## RN-094 — Objetivo

Permitir exportar somente determinadas despesas de uma fatura.

## RN-095 — Filtro por responsável

O usuário deve poder escolher:

- Meu;
- Giulia;
- Ederson;
- Elisiane.

## RN-096 — Exemplo

Fatura do cartão do Ederson:

Total:
R$ 3.000

Despesas do Felipe:
R$ 1.200

O relatório deve permitir exportar os R$ 1.200 correspondentes.

## RN-097 — Conferência

O relatório deverá conter informações suficientes para conferência com o titular do cartão.

Considerar:

- descrição;
- data;
- valor;
- responsável;
- parcela;
- categoria.


# 22. Metas

## RN-098 — Cadastro

Uma meta deve possuir:

- nome;
- descrição;
- valor objetivo;
- prazo;
- status.

## RN-099 — Progresso

O sistema deve apresentar:

valor acumulado
/
valor objetivo

## RN-100 — Conclusão

Quando o objetivo for atingido, a meta poderá ser marcada como concluída.


# 23. Histórico

## RN-101 — Preservação

Operações financeiras relevantes devem permanecer no histórico.

## RN-102 — Status

Quando uma operação não representar mais um compromisso financeiro, utilizar status apropriado.

Exemplos:

- CANCELADA;
- ESTORNADA.


# 24. Auditoria básica

## RN-103 — Datas

Registros relevantes devem possuir:

created_at
updated_at

## RN-104 — Futuro

Uma auditoria detalhada de quem alterou cada campo poderá ser adicionada posteriormente.


# 25. Regras de valor

## RN-105 — Valores positivos

Valores monetários devem ser positivos em seus registros principais.

O tipo da operação determina se o valor representa entrada ou saída.

## RN-106 — Zero

Não permitir valores zero em operações financeiras que não façam sentido com valor zero.

## RN-107 — Precisão

Utilizar precisão adequada para valores monetários.

Java:

BigDecimal

PostgreSQL:

NUMERIC


# 26. Regras de data

## RN-108 — Data da despesa

A data da despesa representa quando a obrigação foi realizada.

## RN-109 — Data de vencimento

A data de vencimento representa quando a obrigação deve ser paga.

## RN-110 — Data de pagamento

A data de pagamento representa quando o dinheiro efetivamente saiu da conta.

Essas datas são conceitos diferentes e não devem ser confundidas.


# 27. Regra de integridade

## RN-111

Uma operação não deve produzir efeitos financeiros duplicados.

Exemplo:

Pagar uma despesa não pode gerar duas saídas financeiras por acidente.

## RN-112

Operações financeiras que envolvam múltiplas tabelas devem ser transacionais.


# 28. Idempotência

## RN-113

Operações críticas de pagamento devem ser projetadas para evitar duplicidade acidental.

Caso a API seja chamada duas vezes devido a retry, o sistema deve impedir que o mesmo pagamento seja registrado duas vezes quando isso for identificável.


# 29. Concorrência

## RN-114

O backend deve considerar que duas requisições podem ocorrer simultaneamente.

Operações de:

- pagamento;
- fechamento de fatura;
- alteração de saldo;
- transferência;

devem preservar a consistência.


# 30. Regra para edição

## RN-115

Dados financeiros futuros podem ser editados normalmente quando ainda não produziram efeito.

## RN-116

Dados históricos ou já pagos devem possuir tratamento mais restritivo.

O sistema não deve simplesmente alterar o passado sem preservar consistência financeira.


# 31. Regra para exclusão

## RN-117

O botão "Excluir" não deve necessariamente executar DELETE físico.

Para registros financeiros, "excluir" pode significar:

- cancelar;
- estornar;
- inativar.

A interface deve deixar claro o efeito da operação.


# 32. Regras futuras

As seguintes funcionalidades ficam explicitamente fora da V1:

- investimentos;
- importação de extratos;
- integração bancária;
- notificações;
- múltiplas moedas;
- recorrência avançada;
- auditoria completa;
- anexos;
- OCR;
- sincronização automática;
- aplicativo mobile nativo.


# 33. Regra de evolução

Quando uma nova funcionalidade for adicionada:

1. definir a regra de negócio;
2. atualizar este documento;
3. atualizar o modelo de domínio se necessário;
4. atualizar migrations;
5. implementar backend;
6. criar testes;
7. implementar frontend;
8. atualizar documentação.


# 34. Regra final

Nenhuma regra financeira deve ser implementada apenas por inferência.

Quando houver dúvida sobre comportamento financeiro, o sistema deve parar a implementação daquela regra e solicitar uma decisão.

A prioridade absoluta é:

1. preservar os dados;
2. preservar o histórico;
3. evitar duplicidade;
4. manter os cálculos financeiros corretos;
5. permitir auditoria e conferência;
6. manter o sistema simples.