# Requisitos Funcionais — Financial Control

## 1. Objetivo

O Financial Control é um sistema de controle financeiro pessoal multiusuário.

O objetivo da V1 é fornecer uma base sólida para que o usuário possa:

- controlar receitas;
- controlar despesas;
- controlar contas bancárias;
- controlar dinheiro em espécie através de uma conta pessoal;
- controlar cartões de crédito;
- controlar faturas;
- controlar compras parceladas;
- controlar parcelamentos de faturas;
- realizar transferências entre contas;
- criar metas financeiras;
- visualizar projeções financeiras;
- acompanhar os dados através de dashboards e gráficos;
- exportar relatórios financeiros.

A aplicação será inicialmente executada localmente.

A arquitetura deve permitir evolução futura sem adicionar complexidade desnecessária à V1.


## 2. Usuários

### RF-001 — Cadastro de usuário

O sistema deve permitir o cadastro de usuários.

Cada usuário deve possuir:

- identificador UUID;
- nome;
- e-mail;
- senha armazenada de forma segura;
- data de criação;
- data de atualização.

O e-mail deve ser único.


### RF-002 — Autenticação

O sistema deve permitir que o usuário faça login.

A autenticação será baseada em:

- e-mail;
- senha;
- JWT.

Após autenticar, o usuário poderá acessar somente os seus próprios dados financeiros.


### RF-003 — Isolamento de dados

Todos os dados financeiros devem estar vinculados direta ou indiretamente ao usuário.

Um usuário nunca poderá visualizar ou alterar:

- contas;
- cartões;
- faturas;
- receitas;
- despesas;
- categorias;
- movimentações;
- metas;

pertencentes a outro usuário.

O backend é responsável por garantir esse isolamento.

O frontend nunca deve ser considerado uma camada de segurança.


## 3. Contas

### RF-004 — Cadastro de conta

O sistema deve permitir o cadastro de múltiplas contas para cada usuário.

Exemplos:

- Banco A;
- Banco B;
- Poupança;
- Conta pessoal;
- Carteira;
- Conta utilizada para dinheiro em espécie.

Uma conta deve possuir, no mínimo:

- nome;
- tipo;
- saldo inicial;
- status;
- usuário;
- data de criação;
- data de atualização.


### RF-005 — Tipos de conta

A V1 deve suportar pelo menos:

- CONTA_CORRENTE;
- POUPANCA;
- CONTA_PESSOAL.

A estrutura deve permitir novos tipos futuramente.


### RF-006 — Saldo da conta

O sistema deve permitir consultar:

- saldo inicial;
- entradas;
- saídas;
- saldo atual.

O saldo não deve ser tratado como um valor arbitrário que o usuário altera livremente.

O saldo deve ser consequência das movimentações financeiras.


### RF-007 — Histórico de conta

O usuário deve conseguir visualizar as movimentações de uma conta.

Cada movimentação deve apresentar informações suficientes para identificar:

- data;
- descrição;
- tipo;
- valor;
- origem;
- destino, quando for transferência;
- saldo resultante, quando aplicável.


## 4. Transferências

### RF-008 — Transferência entre contas

O sistema deve permitir transferir dinheiro de uma conta para outra.

Exemplo:

Conta A:
R$ 1.000,00

Conta B:
+ R$ 1.000,00

A transferência não deve ser considerada:

- receita;
- despesa;
- lucro;
- prejuízo.


### RF-009 — Histórico de transferência

Uma transferência deve possuir:

- conta origem;
- conta destino;
- valor;
- data;
- descrição opcional;
- usuário;
- identificador da operação.

A operação deve manter a relação entre origem e destino.


## 5. Categorias

### RF-010 — Categorias financeiras

O sistema deve permitir categorias para:

- receitas;
- despesas.

Exemplos de despesas:

Moradia
    - Aluguel
    - Energia
    - Água
    - Internet

Alimentação
    - Mercado
    - Restaurante
    - Lanche

Transporte
    - Combustível
    - Manutenção

Exemplos de receitas:

- Salário;
- Freelance;
- Venda;
- Outros.


### RF-011 — Subcategorias

Uma categoria poderá possuir uma categoria pai.

Exemplo:

Alimentação
    - Mercado
    - Restaurante
    - Lanche

A estrutura deve permitir subcategorias sem exigir múltiplas tabelas específicas.


### RF-012 — Desativação de categoria

Categorias não devem ser fisicamente excluídas quando já estiverem sendo utilizadas por registros financeiros.

Deve ser possível desativar uma categoria.

Categorias desativadas não devem aparecer como opção para novos lançamentos, mas devem continuar aparecendo no histórico.


## 6. Receitas

### RF-013 — Cadastro de receita

O sistema deve permitir cadastrar receitas.

Uma receita deve possuir, no mínimo:

- descrição;
- valor;
- origem/categoria;
- data;
- conta;
- status;
- usuário;
- data de criação;
- data de atualização.


### RF-014 — Origem da receita

A receita deve possuir uma origem.

Exemplos:

- salário;
- freelance;
- venda;
- outros.

A origem poderá utilizar o sistema de categorias.


### RF-015 — Status da receita

A V1 deve possuir os seguintes estados:

PREVISTA
RECEBIDA
CANCELADA

PREVISTA:

A receita ainda não foi recebida.

Ela deve participar das projeções futuras.

Não deve alterar o saldo real da conta.

RECEBIDA:

A receita foi efetivamente recebida.

Deve gerar uma movimentação de entrada.

CANCELADA:

A receita foi anulada.

Deve permanecer no histórico, mas não deve participar dos cálculos financeiros ativos.


### RF-016 — Recebimento de receita prevista

Uma receita prevista deve poder ser marcada como recebida.

Ao realizar essa operação:

- alterar o status;
- registrar a conta;
- criar a movimentação de entrada;
- atualizar o saldo resultante.


## 7. Despesas

### RF-017 — Cadastro de despesa

O sistema deve permitir cadastrar despesas.

Uma despesa deve possuir, no mínimo:

- descrição;
- valor;
- categoria;
- data;
- vencimento;
- número do boleto, quando aplicável;
- conta, quando aplicável;
- cartão, quando aplicável;
- responsável;
- status;
- usuário;
- data de criação;
- data de atualização.


### RF-018 — Despesa sem cartão

Uma despesa pode ser cadastrada sem cartão.

Exemplo:

Internet
R$ 120,00
Vencimento: 10/08
Boleto: 123456789

Nesse momento ela pode permanecer:

PENDENTE

sem uma conta de pagamento definida.


### RF-019 — Número do boleto

A despesa deve permitir armazenar o número do boleto.

O campo deve ser opcional.

O objetivo é permitir que o usuário copie o número do boleto diretamente do sistema quando for realizar o pagamento.

O sistema não precisa gerar boletos na V1.


### RF-020 — Status da despesa

A V1 deve possuir:

PENDENTE
PAGA
ESTORNADA
CANCELADA

PENDENTE:

A despesa ainda não foi paga.

Deve aparecer em:

Contas a pagar

e participar das projeções.

PAGA:

A despesa foi efetivamente paga.

Deve gerar uma saída financeira.

ESTORNADA:

A despesa existiu, mas posteriormente foi revertida.

O registro original deve permanecer.

CANCELADA:

A despesa foi anulada antes de produzir efeitos financeiros definitivos ou foi corrigida pelo usuário.

O registro deve permanecer no histórico.


## 8. Responsável pela despesa

### RF-021 — Responsável

Cada despesa poderá possuir um responsável.

Na V1 serão utilizados os seguintes valores:

0 — Meu
1 — Giulia
2 — Ederson
3 — Elisiane

Essa informação não deve criar novos usuários no sistema.

É apenas uma classificação da despesa.


### RF-022 — Relatórios por responsável

O sistema deve permitir filtrar despesas por responsável.

Exemplo:

Cartão do Ederson
Responsável: Meu

Resultado:

Mercado       R$ 500,00
Amazon        R$ 300,00
Restaurante   R$ 120,00

Total          R$ 920,00


## 9. Pagamento de despesas

### RF-023 — Pagamento de despesa

Uma despesa pendente deve poder ser paga.

No momento do pagamento o usuário deve informar:

- conta utilizada;
- valor efetivamente pago;
- data do pagamento.

Por padrão, o valor pago deve corresponder ao valor da despesa.


### RF-024 — Pagamento de despesa

Ao pagar uma despesa:

1. validar a despesa;
2. validar a conta;
3. registrar a movimentação de saída;
4. alterar o status para PAGA;
5. manter o histórico da operação.


## 10. Cartões de crédito

### RF-025 — Cadastro de cartão

O sistema deve permitir múltiplos cartões por usuário.

Um cartão deve possuir:

- nome/apelido;
- titular;
- limite;
- dia de fechamento;
- dia de vencimento;
- status;
- usuário;
- data de criação;
- data de atualização.


### RF-026 — Titular do cartão

O titular do cartão será armazenado como texto.

Exemplos:

- Felipe;
- Giulia;
- Ederson;
- Elisiane.

Não é necessário criar um usuário do sistema para cada titular.


### RF-027 — Limite do cartão

O sistema deve armazenar o limite do cartão.

A V1 deve permitir visualizar:

- limite total;
- valor utilizado;
- limite disponível.

O cálculo deve considerar os lançamentos relevantes do cartão.


### RF-028 — Fechamento da fatura

Cada cartão deve possuir um dia de fechamento.

O sistema deve determinar em qual fatura uma compra deve entrar de acordo com a data da compra e as regras de fechamento.


### RF-029 — Vencimento da fatura

Cada cartão deve possuir um dia de vencimento.

A fatura deve apresentar a data de vencimento correspondente.


## 11. Faturas

### RF-030 — Faturas de cartão

O sistema deve criar e controlar faturas para os cartões.

Uma fatura deve possuir:

- cartão;
- período;
- data de fechamento;
- data de vencimento;
- valor total;
- valor pago;
- saldo restante;
- status.


### RF-031 — Status da fatura

A V1 poderá utilizar:

ABERTA
FECHADA
PAGA
PARCIALMENTE_PAGA
VENCIDA

A definição final das transições entre estados deve ser implementada conforme as regras de negócio.


### RF-032 — Itens da fatura

Uma fatura deve possuir seus itens individualmente.

Um item pode representar:

- compra;
- parcela;
- estorno;
- crédito;
- ajuste.

Isso evita tratar a fatura simplesmente como um número acumulado.


## 12. Compras no cartão

### RF-033 — Compra no cartão

Ao cadastrar uma despesa com cartão:

- associar a despesa ao cartão;
- determinar a fatura correspondente;
- criar o item da fatura;
- considerar o valor no limite utilizado.

A compra não deve gerar imediatamente uma saída em conta bancária.


### RF-034 — Pagamento da fatura

A saída bancária acontece quando a fatura é paga.

Exemplo:

Compra:
R$ 500,00

Conta:
Nenhuma alteração.

Pagamento da fatura:
R$ 500,00

Conta:
- R$ 500,00


## 13. Parcelamentos

### RF-035 — Compra parcelada

O usuário deve poder informar:

- valor;
- quantidade de parcelas;
- cartão;
- data da compra.

O sistema deve criar automaticamente as parcelas futuras.


### RF-036 — Parcelas futuras

Cada parcela deve possuir:

- número da parcela;
- valor;
- vencimento;
- status;
- referência ao parcelamento;
- referência à fatura correspondente.

As parcelas futuras devem aparecer nas projeções.


### RF-037 — Valores diferentes

O sistema deve permitir que cada parcela tenha valor próprio.

Exemplo:

Parcela 1 — R$ 180,00
Parcela 2 — R$ 200,00
Parcela 3 — R$ 210,00
Parcela 4 — R$ 210,00

Não assumir que:

valor_total / quantidade

seja sempre suficiente.


## 14. Parcelamento de fatura

### RF-038 — Pagamento parcial da fatura

O usuário poderá pagar somente parte da fatura.

Exemplo:

Fatura: R$ 2.000,00
Valor pago: R$ 1.000,00
Saldo: R$ 1.000,00

A fatura deve permanecer parcialmente paga.


### RF-039 — Parcelamento do saldo

O saldo restante de uma fatura poderá ser transformado em parcelamento.

Exemplo:

Saldo:
R$ 1.000,00

Parcelamento:
5 parcelas

O sistema deve criar as parcelas futuras.

As despesas originais da fatura não devem ser excluídas.


### RF-040 — Valor individual das parcelas da fatura

O usuário deve poder editar individualmente o valor das parcelas do parcelamento.

Isso é necessário porque parcelamentos de cartão podem possuir juros e valores diferentes.


## 15. Estornos

### RF-041 — Estorno de despesa

Uma despesa que já existe poderá ser estornada.

O sistema deve:

- manter a despesa original;
- alterar seu status para ESTORNADA;
- registrar os efeitos financeiros necessários.


### RF-042 — Estorno de compra no cartão

Quando uma compra no cartão for estornada:

- a compra original deve permanecer registrada;
- o lançamento deve ser identificado como estornado;
- a fatura deve receber o crédito/ajuste correspondente;
- o limite utilizado deve ser recalculado conforme a regra definida.


### RF-043 — Estorno de despesa já paga

Caso uma despesa já paga seja estornada, o sistema deve registrar o retorno financeiro correspondente.

Exemplo:

Despesa:
R$ 500,00

Pagamento:
- R$ 500,00

Estorno:
+ R$ 500,00

O histórico original deve permanecer.


## 16. Cancelamentos

### RF-044 — Cancelamento

O usuário deve poder cancelar operações que ainda possam ser anuladas.

O sistema não deve apagar fisicamente o registro.

O registro deve permanecer disponível para histórico.


## 17. Movimentações financeiras

### RF-045 — Movimentação

O sistema deve registrar movimentações financeiras efetivas.

Tipos principais:

ENTRADA
SAIDA
TRANSFERENCIA


### RF-046 — Origem da movimentação

Quando aplicável, a movimentação deve manter referência à origem.

Exemplos:

Receita recebida
    -> ENTRADA

Despesa paga
    -> SAIDA

Pagamento de fatura
    -> SAIDA

Transferência
    -> TRANSFERENCIA


## 18. Metas financeiras

### RF-047 — Cadastro de meta

O usuário deve poder criar metas.

Uma meta pode possuir:

- nome;
- descrição;
- valor objetivo;
- valor acumulado;
- prazo;
- status;
- usuário.


### RF-048 — Progresso da meta

O sistema deve calcular o progresso da meta.

Exemplo:

Objetivo: R$ 10.000
Atual:    R$ 4.000

Progresso:
40%


### RF-049 — Status da meta

Estados previstos:

ATIVA
CONCLUIDA
CANCELADA


## 19. Projeções financeiras

### RF-050 — Projeção mensal

O sistema deve permitir consultar o cenário financeiro futuro.

Exemplo:

Agosto
Setembro
Outubro
Novembro
Dezembro
Janeiro


### RF-051 — Elementos da projeção

A projeção deve considerar, conforme aplicável:

- receitas previstas;
- despesas pendentes;
- parcelas futuras;
- faturas futuras;
- parcelamentos de faturas;
- demais compromissos futuros existentes na V1.


### RF-052 — Projeção não altera saldo

Uma projeção é apenas uma visão calculada.

Ela não deve criar movimentações financeiras reais.


## 20. Dashboard

### RF-053 — Dashboard financeiro

O sistema deve possuir um dashboard.

O dashboard deve apresentar inicialmente:

- saldo total;
- receitas do período;
- despesas do período;
- resultado do período;
- despesas por categoria;
- receitas por origem;
- gastos por cartão;
- faturas abertas;
- contas a pagar;
- metas;
- projeções futuras.


## 21. Gráficos

### RF-054 — Gráficos financeiros

O sistema deve apresentar os dados financeiros de maneira visual.

Exemplos:

- despesas por categoria;
- receitas por origem;
- despesas por cartão;
- evolução mensal;
- receitas x despesas;
- progresso das metas.

Os gráficos devem ter finalidade informativa e não apenas estética.


## 22. Contas a pagar

### RF-055 — Contas a pagar

O sistema deve possuir uma visão consolidada das despesas pendentes.

Deve permitir visualizar:

- descrição;
- valor;
- vencimento;
- número do boleto;
- categoria;
- responsável;
- cartão, quando aplicável;
- status.


### RF-056 — Filtro de vencimento

A visão de contas a pagar deve permitir filtros por:

- período;
- vencimento;
- categoria;
- responsável;
- conta;
- cartão;
- status.


## 23. Relatórios

### RF-057 — Relatório de fatura

O sistema deve permitir gerar relatório de uma fatura.

O relatório deve permitir filtrar por responsável.

Exemplo:

Cartão: Cartão do Ederson
Fatura: Agosto/2026
Responsável: Meu


### RF-058 — Exportação

A V1 deve permitir exportar o relatório para um formato apropriado.

O formato inicial pode ser definido durante a implementação, priorizando uma solução simples e útil para compartilhamento.


## 24. Histórico

### RF-059 — Preservação de histórico

Registros financeiros relevantes não devem ser apagados fisicamente.

Exemplos:

- despesas;
- receitas;
- pagamentos;
- estornos;
- cancelamentos;
- transferências;
- faturas;
- parcelas.


## 25. Auditoria básica

### RF-060 — Timestamps

As entidades relevantes devem possuir:

- createdAt;
- updatedAt.

A V1 não precisa possuir um sistema completo de auditoria ou event sourcing.

A arquitetura deve permitir evolução futura caso seja necessário.


## 26. Funcionalidades fora da V1

As seguintes funcionalidades não fazem parte da primeira versão:

- investimentos;
- importação automática de extratos bancários;
- integração com bancos;
- notificações de vencimento;
- compartilhamento familiar;
- contas compartilhadas;
- deploy em produção;
- integrações externas;
- automações financeiras avançadas.

Essas funcionalidades devem permanecer como possíveis extensões futuras.


## 27. Prioridade da V1

A prioridade de implementação deve ser:

1. autenticação e usuários;
2. contas;
3. categorias;
4. receitas;
5. despesas;
6. movimentações;
7. cartões;
8. faturas;
9. parcelamentos;
10. pagamentos;
11. estornos;
12. transferências;
13. metas;
14. projeções;
15. dashboard;
16. relatórios.

A implementação deve seguir o roadmap definido em:

docs/09-roadmap.md