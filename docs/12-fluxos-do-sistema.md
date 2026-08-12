# Fluxos do Sistema — Financial Control

## 1. Objetivo

Este documento descreve os principais fluxos funcionais do Financial Control.

O objetivo é permitir que backend, frontend, banco de dados e testes sejam implementados seguindo os mesmos comportamentos.

Cada fluxo deve ser considerado uma unidade funcional.

Sempre que possível, um fluxo deve possuir:

- regra de negócio;
- operação de banco;
- endpoint;
- interface;
- testes automatizados.


# 2. Fluxo de cadastro de usuário

## Entrada

Usuário informa:

- nome;
- email;
- senha.

## Processo

1. validar nome;
2. validar email;
3. verificar se email já existe;
4. validar senha;
5. gerar hash da senha;
6. criar usuário;
7. persistir;
8. retornar dados públicos.

## Não fazer

Nunca armazenar senha em texto puro.

## Resultado

Usuário criado.


# 3. Fluxo de login

## Entrada

- email;
- senha.

## Processo

1. localizar usuário;
2. verificar se está ativo;
3. validar senha;
4. gerar JWT;
5. retornar token.

## Falha

Credenciais inválidas:

HTTP 401.


# 4. Fluxo de criação de conta

## Entrada

- nome;
- tipo;
- saldo inicial.

## Processo

1. validar dados;
2. associar ao usuário autenticado;
3. criar conta;
4. registrar saldo inicial;
5. salvar.

## Resultado

Conta disponível para utilização.


# 5. Fluxo de criação de conta pessoal

Exemplo:

Nome:

Carteira


Tipo:

PERSONAL_WALLET


A conta funciona como representação de dinheiro em espécie.


# 6. Fluxo de criação de categoria

## Entrada

- nome;
- tipo;
- categoria pai opcional.

## Processo

1. validar;
2. verificar duplicidade;
3. associar ao usuário;
4. criar categoria.


# 7. Fluxo de criação de receita recebida

Exemplo:

Salário:

R$ 5.400


Conta:

Banco A


## Processo

1. criar receita;
2. marcar como RECEIVED;
3. registrar entrada financeira;
4. atualizar saldo da conta.

## Resultado

Saldo aumenta em:

R$ 5.400.


# 8. Fluxo de criação de receita pendente

Exemplo:

Freelance:

R$ 1.000


Ainda não recebido.

## Processo

1. criar receita;
2. status = PENDING;
3. não criar entrada efetiva;
4. não alterar saldo.


# 9. Fluxo de recebimento de receita

Receita:

R$ 1.000


Status:

PENDING


Usuário confirma recebimento.

## Processo

1. validar receita;
2. informar conta;
3. registrar recebimento;
4. criar entrada financeira;
5. alterar status para RECEIVED;
6. atualizar saldo.

Tudo deve ocorrer em uma transação.


# 10. Fluxo de cancelamento de receita

## Processo

1. localizar receita;
2. validar possibilidade de cancelamento;
3. alterar status;
4. não apagar registro.

Se já houver entrada financeira, o cancelamento deve ser tratado como operação de reversão apropriada, e não simplesmente mudar o status.


# 11. Fluxo de criação de despesa sem cartão

Exemplo:

Internet:

R$ 150


## Entrada

- descrição;
- valor;
- vencimento;
- categoria;
- responsável;
- conta;
- boleto opcional.

## Processo

1. validar;
2. criar despesa;
3. status = PENDING;
4. não reduzir saldo.

## Resultado

A despesa aparece em:

Contas a pagar.


# 12. Fluxo de pagamento de despesa

Despesa:

R$ 150


Conta:

Banco A


## Processo

1. localizar despesa;
2. validar status;
3. validar valor;
4. registrar pagamento;
5. criar saída financeira;
6. atualizar status;
7. atualizar saldo.

## Resultado

Saldo reduz:

R$ 150.


# 13. Fluxo de pagamento parcial de despesa

Despesa:

R$ 1.000


Pagamento:

R$ 600


## Processo

1. registrar pagamento de R$ 600;
2. criar saída de R$ 600;
3. atualizar valor pago;
4. calcular saldo restante;
5. status = PARTIALLY_PAID.

## Resultado

Restante:

R$ 400.


# 14. Fluxo de segundo pagamento

Despesa:

R$ 1.000

Pago:

R$ 600

Restante:

R$ 400


Usuário paga:

R$ 400.


## Resultado

Pago:

R$ 1.000

Restante:

R$ 0

Status:

PAID


# 15. Fluxo de criação de compra no cartão

Exemplo:

Mercado:

R$ 500


Cartão:

Cartão A


## Processo

1. validar cartão;
2. validar limite;
3. criar despesa;
4. associar cartão;
5. identificar fatura;
6. criar parcela;
7. adicionar valor à fatura;
8. atualizar comprometimento do cartão.

## Não fazer

Não retirar R$ 500 da conta bancária.


# 16. Fluxo de compra à vista no cartão

Exemplo:

Compra:

R$ 500


Parcelas:

1


Criar apenas:

1/1


A compra pertence à fatura correspondente.


# 17. Fluxo de compra parcelada

Exemplo:

Compra:

R$ 1.200


Parcelas:

12


## Processo

1. validar valor;
2. validar quantidade;
3. validar cartão;
4. identificar ciclo inicial;
5. calcular parcelas;
6. gerar parcelas;
7. identificar faturas;
8. criar/associar faturas;
9. salvar tudo em transação.

## Resultado

12 parcelas.


# 18. Exemplo de parcelamento

Compra:

12/08/2026


Valor:

R$ 1.200


Parcelas:

12


Gerar:

Parcela 1:
R$ 100

Parcela 2:
R$ 100

Parcela 3:
R$ 100

...

Parcela 12:
R$ 100


# 19. Parcelamento com valor não divisível

Exemplo:

R$ 100 / 3


Gerar:

R$ 33,33
R$ 33,33
R$ 33,34


A soma deve ser:

R$ 100,00.


# 20. Regra de geração das parcelas

A primeira parcela deve receber a data de vencimento determinada pela fatura correspondente.

As parcelas seguintes devem acompanhar os ciclos seguintes do cartão.


# 21. Fluxo de identificação da fatura

Dada:

- data da compra;
- dia de fechamento;
- dia de vencimento.

Determinar:

fatura correspondente.


# 22. Compra antes do fechamento

Cartão fecha dia:

10


Compra:

09/08


A compra pertence à fatura que fecha em:

10/08.


# 23. Compra após fechamento

Cartão fecha:

10


Compra:

11/08


A compra pertence à próxima fatura.


# 24. Compra no dia do fechamento

O comportamento deve ser consistente com a regra definida no documento de regras de negócio.

V1:

considerar a compra como pertencente ao ciclo que está sendo fechado.


# 25. Fluxo de criação de fatura

Uma fatura deve possuir:

- cartão;
- período;
- data de fechamento;
- data de vencimento;
- status;
- total.

Não devem existir duas faturas para o mesmo cartão e mesmo ciclo.


# 26. Fluxo de fechamento de fatura

## Processo

1. localizar fatura;
2. validar status;
3. impedir novos lançamentos;
4. recalcular total;
5. marcar como CLOSED;
6. salvar.

Tudo deve ser consistente em transação.


# 27. Fatura aberta

Pode receber:

- compras;
- parcelas;
- estornos.


# 28. Fatura fechada

Não deve receber novas compras.

Novas compras devem ser direcionadas para a próxima fatura.


# 29. Fluxo de pagamento integral da fatura

Fatura:

R$ 2.000


Pagamento:

R$ 2.000


## Processo

1. localizar fatura;
2. validar status;
3. validar conta;
4. validar valor;
5. criar pagamento;
6. criar saída financeira;
7. atualizar paid_amount;
8. status = PAID;
9. atualizar saldo;
10. atualizar comprometimento do cartão.

Tudo em uma transação.


# 30. Fluxo de pagamento parcial da fatura

Fatura:

R$ 2.000


Pagamento:

R$ 1.200


## Resultado

Pago:

R$ 1.200


Restante:

R$ 800


Status:

PARTIALLY_PAID


O saldo bancário deve reduzir:

R$ 1.200.


# 31. Fluxo de segundo pagamento da fatura

Fatura:

R$ 2.000

Pago:

R$ 1.200


Novo pagamento:

R$ 800


## Resultado

Pago:

R$ 2.000

Restante:

R$ 0

Status:

PAID


# 32. Fluxo de pagamento múltiplo

Uma fatura pode possuir:

Pagamento 1:
R$ 500

Pagamento 2:
R$ 700

Pagamento 3:
R$ 300


Total:

R$ 1.500


A soma dos pagamentos representa:

paid_amount.


# 33. Fluxo de pagamento da fatura com conta diferente

O usuário pode escolher qual conta bancária será utilizada para pagar a fatura.


Exemplo:

Cartão:

Cartão A


Pagamento:

Conta Banco B


Isso é permitido.


# 34. Fluxo de estorno

Compra:

R$ 500


Estorno:

R$ 500


## Processo

1. localizar despesa;
2. validar se pode ser estornada;
3. registrar estorno;
4. alterar status apropriado;
5. atualizar fatura;
6. atualizar comprometimento.


# 35. Fluxo de estorno parcial

Compra:

R$ 500


Estorno:

R$ 200


Resultado líquido:

R$ 300.


A compra permanece registrada.


# 36. Fluxo de estorno antes do pagamento da fatura

Compra:

R$ 500


Fatura:

R$ 500


Estorno:

R$ 500


Resultado:

fatura reduzida em:

R$ 500.


# 37. Fluxo de estorno depois do pagamento

Compra:

R$ 500


Fatura paga:

R$ 500


Depois:

estorno de R$ 500.


Nesse cenário:

o dinheiro do estorno deve voltar para a conta apropriada.


O sistema deve registrar uma entrada financeira.


# 38. Fluxo de cancelamento

Quando uma operação for cancelada:

não apagar.


Alterar status para:

CANCELLED


# 39. Cancelamento de compra no cartão

Se ainda não estiver paga:

- remover do comprometimento;
- não gerar nova saída;
- manter histórico.


# 40. Fluxo de transferência

Exemplo:

Conta A:

R$ 3.000


Transferência:

R$ 500


Conta B:

R$ 1.000


## Processo

1. validar origem;
2. validar destino;
3. validar valor;
4. verificar que são contas diferentes;
5. criar transferência;
6. criar saída na origem;
7. criar entrada no destino;
8. atualizar saldos.

Tudo em uma transação.


# 41. Transferência não é receita/despesa

A transferência não deve alterar:

total de receitas;

total de despesas.


Ela altera apenas os saldos das contas.


# 42. Transferência para pagamento

Exemplo:

Conta A:

R$ 5.000


Conta B:

R$ 500


Transferir:

R$ 1.000


A transferência não deve ser confundida com o pagamento posterior de uma despesa.


# 43. Fluxo de meta

Criar meta:

Viagem


Objetivo:

R$ 10.000


## Processo

1. validar;
2. criar meta;
3. definir status ACTIVE.


# 44. Atualização de meta

O valor atual pode ser alterado conforme o mecanismo definido na V1.


A implementação deve deixar claro se o valor é:

- apenas acompanhamento;
- ou derivado de movimentações financeiras.


A V1 pode utilizar acompanhamento manual.


# 45. Conclusão de meta

Quando:

current_amount >= target_amount


o sistema pode permitir:

COMPLETED


# 46. Cancelamento de meta

Alterar status para:

CANCELLED


Manter histórico.


# 47. Fluxo de contas a pagar

A tela deve consultar compromissos pendentes.


Exibir:

- descrição;
- valor;
- vencimento;
- categoria;
- responsável;
- boleto;
- origem.


# 48. Contas vencidas

Se:

due_date < today


e:

remaining_amount > 0


então:

OVERDUE.


# 49. Fluxo de boleto

Ao cadastrar uma despesa:

o usuário pode informar:

boletoNumber.


Esse número deve ficar disponível para cópia.


# 50. Fluxo de relatório de cartão

Usuário seleciona:

Cartão:
Ederson


Período:

01/08/2026
31/08/2026


Responsável:

MINE


## Processo

1. consultar despesas;
2. filtrar;
3. calcular total;
4. gerar relatório;
5. disponibilizar PDF.


# 51. Relatório

O PDF deve conter:

- título;
- cartão;
- titular;
- período;
- responsável;
- lista de despesas;
- data;
- descrição;
- categoria;
- parcela;
- valor;
- total.


# 52. Fluxo do dashboard

Ao abrir o sistema:

1. identificar usuário;
2. consultar saldo;
3. consultar receitas;
4. consultar despesas;
5. consultar contas a pagar;
6. consultar cartões;
7. consultar compromissos;
8. calcular projeção;
9. retornar resumo.


# 53. Dashboard mensal

Usuário escolhe:

Agosto/2026


O sistema deve mostrar somente informações relevantes ao período.


# 54. Fluxo de projeção

Usuário seleciona:

Setembro/2026


O sistema deve identificar:

- receitas previstas;
- despesas previstas;
- parcelas de cartão;
- faturas;
- refinanciamentos;
- outros compromissos.


# 55. Projeção de cartão

Compra:

12x R$ 100


Se a parcela 4 estiver em novembro:

Novembro deve considerar:

R$ 100


# 56. Projeção de refinanciamento

Fatura:

R$ 2.000


Pagamento:

R$ 1.000


Refinanciamento:

R$ 1.000


4 parcelas:

R$ 250


A projeção deve considerar:

R$ 250

em cada mês correspondente.


# 57. Fluxo de saldo projetado

Para cada período:

saldo inicial do período

+
receitas previstas

-
despesas previstas

-
compromissos previstos


Resultado:

saldo projetado.


# 58. Não confundir saldo

O sistema deve mostrar separadamente:

Saldo atual

e:

Saldo projetado.


# 59. Fluxo de lançamento mensal

Cenário:

Durante agosto:


Usuário registra:

- mercado;
- luz;
- internet;
- combustível;
- lazer;
- compras parceladas.


Algumas possuem cartão.

Outras não.


O sistema deve organizar automaticamente.


# 60. Fluxo de análise mensal

No início de setembro:

usuário seleciona:

Agosto.


Visualiza:

- receitas;
- despesas;
- cartões;
- contas;
- transferências;
- saldo.


# 61. Fluxo de análise do cartão

Usuário seleciona:

Cartão A


Sistema mostra:

fatura atual;
faturas futuras;
parcelas;
valor comprometido;
limite disponível.


# 62. Fluxo de planejamento futuro

Usuário seleciona:

Dezembro.


Sistema mostra:

Receitas esperadas

Despesas previstas

Parcelas de cartões

Contas previstas

Refinanciamentos


E apresenta:

Saldo projetado.


# 63. Fluxo de decisão financeira

O sistema deve permitir responder:

"Quanto já tenho comprometido em dezembro?"


Deve considerar todos os compromissos conhecidos.


# 64. Fluxo de lançamento de salário

Exemplo:

Dia 5:

Salário:

R$ 5.400


Conta:

Banco A


Usuário cadastra como:

RECEIVED


Saldo aumenta.


# 65. Fluxo de freelance

Exemplo:

Freelance:

R$ 800


Ainda não recebido.


Cadastrar:

PENDING.


Quando receber:

POST /incomes/{id}/receive


Selecionar conta.


Saldo aumenta.


# 66. Fluxo de pagamento com dinheiro em espécie

Conta:

Carteira


Despesa:

R$ 50


Pagamento:

R$ 50


O saldo da carteira reduz.


# 67. Fluxo de compra no cartão do sogro

Exemplo:

Compra:

R$ 300


Cartão:

Ederson


Responsável:

MINE


A despesa deve ser identificável no relatório.


# 68. Fluxo de conferência com sogro

1. selecionar cartão Ederson;
2. selecionar período;
3. selecionar responsável MINE;
4. gerar relatório;
5. enviar PDF;
6. conferir valores.


# 69. Fluxo de despesas da esposa

Exemplo:

Cartão:

Giulia


Responsável:

GIULIA


O sistema deve permitir identificar essas despesas separadamente.


# 70. Fluxo de despesas compartilhadas

A V1 não precisa possuir rateio automático.


Uma despesa pode possuir apenas um responsável.


Funcionalidade futura:

divisão de despesas.


# 71. Fluxo de edição de parcela

Usuário acessa:

Compra parcelada


Seleciona:

Parcela 5


Altera:

R$ 100

para:

R$ 150


## Processo

1. validar parcela;
2. verificar se está editável;
3. atualizar valor;
4. recalcular total do parcelamento;
5. atualizar fatura;
6. atualizar projeção.


# 72. Regra de parcela paga

Parcelas já pagas não devem ser alteradas livremente.


Se for necessário corrigir:

utilizar operação específica.


# 73. Regra de parcela futura

Parcelas futuras podem ser editadas conforme regras de negócio.


# 74. Fluxo de alteração de valor

Se uma parcela for alterada:

o sistema deve atualizar todos os agregados relacionados.


Exemplo:

Parcela:

R$ 100


Alterada para:

R$ 150


Devem refletir:

- fatura;
- cartão;
- projeção;
- relatórios.


# 75. Fluxo de cancelamento de parcela

Se permitido:

marcar parcela como CANCELLED.


Não apagar.


# 76. Fluxo de cancelamento de parcelamento

Caso seja necessário futuramente:

cancelar parcelas futuras.

Parcelas já pagas devem permanecer no histórico.


# 77. Fluxo de exclusão

O usuário não deve apagar operações financeiras relevantes.


A interface deve oferecer:

Cancelar

ou:

Estornar.


# 78. Fluxo de auditoria futura

A arquitetura deve permitir posteriormente registrar:

- quem alterou;
- quando;
- operação;
- valor anterior;
- valor novo.


Não implementar completamente na V1.


# 79. Fluxo de erro

Se uma operação falhar:

1. retornar erro;
2. não deixar dados parcialmente gravados;
3. manter banco consistente.


# 80. Fluxo transacional

Operações críticas devem utilizar:

BEGIN

operações

COMMIT


ou:

ROLLBACK


Exemplo:

Pagamento de fatura.


# 81. Fluxo de concorrência

Se duas requisições tentarem pagar a mesma fatura simultaneamente:

o sistema deve impedir pagamento duplicado.


# 82. Fluxo de segurança

Toda requisição autenticada deve:

1. validar JWT;
2. identificar usuário;
3. aplicar user_id;
4. consultar somente dados permitidos.


# 83. Fluxo de isolamento

Usuário A tenta acessar:

/expenses/{id-do-usuario-B}


Resultado:

não deve receber os dados.


# 84. Fluxo frontend

O Angular deve:

1. chamar API;
2. receber DTO;
3. apresentar dados;
4. enviar comandos;
5. atualizar tela.


Regras financeiras não devem ser duplicadas desnecessariamente no frontend.


# 85. Fluxo de loading

Durante requisições:

mostrar estado de carregamento adequado.


# 86. Fluxo de erro frontend

Se API retornar erro:

exibir mensagem amigável.


Não mostrar:

stack trace;

detalhes internos;

SQL.


# 87. Fluxo de formulário

Antes de enviar:

validar campos básicos.


Mas o backend sempre deve validar novamente.


# 88. Fluxo de confirmação

Operações críticas devem pedir confirmação.

Exemplos:

- cancelar despesa;
- estornar;
- pagar fatura;
- transferir dinheiro.


# 89. Fluxo de pagamento

Antes de confirmar:

mostrar:

Valor

Conta

Destino

Data


Usuário confirma.


# 90. Fluxo de transferência

Antes de confirmar:

mostrar:

Origem

Destino

Valor


Usuário confirma.


# 91. Fluxo de estorno

Antes de confirmar:

mostrar:

Despesa

Valor original

Valor já estornado

Valor disponível para estorno

Valor do novo estorno


# 92. Fluxo de refinanciamento

Antes de confirmar:

mostrar:

Valor original

Valor pago

Saldo restante

Quantidade de parcelas

Valor das parcelas


# 93. Fluxo de projeção

A projeção deve ser somente leitura.


Ela não deve criar movimentações.


# 94. Fluxo de relatório

Relatório deve utilizar os mesmos dados da aplicação.


Não duplicar regra de cálculo apenas para o PDF.


# 95. Fluxo de atualização do dashboard

Após operação financeira:

o frontend deve atualizar os dados relevantes.


Exemplo:

Após pagamento:

- saldo;
- contas a pagar;
- fatura;
- projeção.


# 96. Fluxo de atualização após transferência

Após transferência:

- saldo da origem;
- saldo do destino;
- histórico.


# 97. Fluxo de atualização após estorno

Após estorno:

- despesa;
- fatura;
- comprometimento;
- saldo, quando aplicável;
- projeção.


# 98. Fluxo de atualização após cancelamento

Após cancelamento:

- status;
- saldo, se aplicável;
- comprometimento;
- projeção.


# 99. Fluxo de consistência

Toda operação financeira deve responder:

Qual foi o efeito?

Onde foi registrado?

Qual saldo foi alterado?

Qual compromisso foi alterado?


# 100. Fluxo completo de exemplo

## Cenário

Usuário compra:

R$ 1.200

em 12 parcelas.

Cartão:

Cartão A.

## Etapa 1

Criar despesa.


## Etapa 2

Criar plano de parcelamento.


## Etapa 3

Criar 12 parcelas.


## Etapa 4

Identificar 12 faturas.


## Etapa 5

Atualizar comprometimento.


## Etapa 6

Usuário fecha a primeira fatura.


## Etapa 7

Usuário paga parcialmente:

R$ 80.


## Etapa 8

Fatura fica:

PARTIALLY_PAID.


## Etapa 9

Usuário decide refinanciar o restante.


## Etapa 10

Criar operação de refinanciamento.


## Etapa 11

Criar parcelas futuras.


## Etapa 12

Atualizar projeção.


## Etapa 13

No mês seguinte:

usuário visualiza:

- nova fatura;
- parcela do refinanciamento;
- demais compras;
- projeção.


# 101. Fluxo mensal real

Durante o mês:

1. registrar receitas;
2. registrar despesas;
3. registrar compras;
4. associar cartões;
5. acompanhar contas;
6. acompanhar faturas;
7. realizar pagamentos;
8. registrar transferências;
9. acompanhar metas.


No fechamento do mês:

10. analisar receitas;
11. analisar despesas;
12. analisar cartões;
13. analisar categorias;
14. analisar projeção futura.


# 102. Fluxo de planejamento

Usuário deseja comprar presentes em dezembro.


Ele consulta:

Dezembro.


O sistema mostra:

Receitas previstas
+
Despesas previstas
+
Parcelas
+
Faturas
+
Refinanciamentos


E apresenta:

saldo projetado.


O usuário utiliza essa informação para decidir quanto pode gastar.


# 103. Princípio fundamental dos fluxos

Nenhum fluxo financeiro deve:

- apagar histórico;
- criar dinheiro inexistente;
- contabilizar transferência como receita/despesa;
- contabilizar compra de cartão como saída bancária imediata;
- duplicar pagamento;
- duplicar parcela;
- duplicar fatura.


# 104. Regra para implementação por IA

Cada fluxo deve ser implementado separadamente.

A IA deve:

1. ler as regras;
2. identificar entidades envolvidas;
3. implementar backend;
4. implementar testes;
5. documentar API;
6. implementar frontend;
7. testar integração;
8. somente então avançar para o próximo fluxo.


# 105. Regra de progresso

Não implementar funcionalidades futuras apenas porque a arquitetura permite.


Exemplo:

Se investimentos não fazem parte da V1:

não criar módulo de investimentos.


# 106. Regra de mudanças

Se durante a implementação surgir uma necessidade não prevista:

a IA deve:

1. interromper a implementação daquela decisão;
2. explicar o problema;
3. apresentar alternativas;
4. solicitar decisão.


# 107. Critério de conclusão de um fluxo

Um fluxo só será considerado concluído quando possuir:

- backend;
- persistência;
- validação;
- testes;
- documentação;
- frontend, quando aplicável;
- tratamento de erros.


# 108. Regra final

O sistema deve sempre preservar a pergunta:

"Onde está o dinheiro?"

E também:

"Qual compromisso financeiro já assumi?"

Essas duas perguntas são a base de todo o sistema.