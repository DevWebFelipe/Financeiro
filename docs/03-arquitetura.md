# Arquitetura — Financial Control

## 1. Objetivo

Este documento define a arquitetura técnica do Financial Control.

A arquitetura deve priorizar:

- simplicidade;
- organização;
- segurança;
- testabilidade;
- manutenção;
- aprendizado;
- possibilidade de evolução.

A V1 será um monólito modular.

Não utilizar microsserviços.


## 2. Visão geral

A aplicação será dividida em três partes principais:

Frontend:

Angular

Backend:

Java + Spring Boot

Banco de dados:

PostgreSQL


Fluxo principal:

Usuário
    |
    v
Angular
    |
    | HTTP/JSON
    v
Spring Boot REST API
    |
    v
Regras de negócio
    |
    v
Spring Data JPA / Hibernate
    |
    v
PostgreSQL


## 3. Frontend

O frontend será desenvolvido utilizando Angular e TypeScript.

Tecnologias principais:

- Angular;
- TypeScript;
- Angular Router;
- HttpClient;
- Reactive Forms;
- Signals.

A aplicação deverá utilizar Standalone Components.


## 4. Estrutura do frontend

A estrutura inicial deverá seguir organização por funcionalidades.

Exemplo:

src/
    app/
        core/
        shared/
        features/
            auth/
            dashboard/
            accounts/
            categories/
            incomes/
            expenses/
            cards/
            invoices/
            installments/
            transfers/
            goals/
            projections/
            reports/


## 5. Core

A pasta core deverá conter recursos globais da aplicação.

Exemplos:

- autenticação;
- interceptors;
- guards;
- serviços globais;
- configuração;
- modelos realmente compartilhados.

Não utilizar core como depósito genérico para qualquer código.


## 6. Shared

A pasta shared deverá conter componentes e recursos reutilizáveis.

Exemplos:

- botões;
- inputs;
- modais;
- tabelas;
- componentes de loading;
- componentes de mensagens;
- pipes reutilizáveis.

Não colocar regras específicas de negócio em shared.


## 7. Features

Cada funcionalidade importante deverá possuir seu próprio módulo lógico.

Exemplo:

features/expenses/

Dentro dela poderão existir:

- pages;
- components;
- services;
- models;
- forms.

A estrutura deve ser criada conforme a necessidade.

Não criar dezenas de arquivos vazios antecipadamente.


## 8. Backend

O backend será desenvolvido utilizando:

- Java 25;
- Spring Boot;
- Spring Web;
- Spring Data JPA;
- Hibernate;
- Spring Security;
- JWT;
- Bean Validation;
- Flyway;
- PostgreSQL.

A arquitetura será baseada em módulos de domínio.


## 9. Estrutura do backend

Estrutura inicial recomendada:

src/
    main/
        java/
            br/
                com/
                    financialcontrol/
                        FinancialControlApplication.java

                        config/

                        security/

                        common/

                        user/

                        account/

                        category/

                        income/

                        expense/

                        creditcard/

                        invoice/

                        installment/

                        transfer/

                        goal/

                        projection/

                        report/

        resources/
            application.yml
            db/
                migration/


A estrutura exata de packages poderá ser ajustada durante a implementação.


## 10. Organização dos módulos

Cada módulo deverá possuir suas próprias responsabilidades.

Exemplo:

expense/

    controller/
    dto/
    entity/
    repository/
    service/


Outro exemplo:

account/

    controller/
    dto/
    entity/
    repository/
    service/


Não criar uma estrutura global gigantesca como:

controller/
service/
repository/
entity/

contendo todas as funcionalidades do sistema.

A organização por domínio deve facilitar a manutenção.


## 11. Controller

Controllers são responsáveis por:

- receber requisições;
- validar entrada;
- chamar serviços;
- retornar respostas HTTP.

Controllers não devem conter regras de negócio complexas.


## 12. DTO

DTOs representam a comunicação da API.

Exemplos:

CreateExpenseRequest
UpdateExpenseRequest
ExpenseResponse

Não retornar entidades JPA diretamente.


## 13. Service

Services concentram as regras de negócio.

Exemplos:

- criar despesa;
- pagar despesa;
- criar compra parcelada;
- calcular fatura;
- pagar fatura;
- realizar transferência;
- estornar despesa.


## 14. Repository

Repositories são responsáveis pelo acesso aos dados.

Utilizar Spring Data JPA.

Repositories não devem possuir regras de negócio.


## 15. Entity

Entities representam o modelo persistido no PostgreSQL.

As entidades não devem ser utilizadas diretamente como contrato da API.


## 16. Regras de negócio financeiras

Operações financeiras importantes devem ser transacionais.

Utilizar:

@Transactional

quando uma operação envolver múltiplas alterações que precisam ser realizadas como uma única unidade.

Exemplo:

Pagamento de fatura:

1. validar fatura;
2. validar conta;
3. registrar pagamento;
4. registrar movimentação;
5. atualizar status da fatura.

Se uma etapa falhar, a operação deve ser revertida.


## 17. Autenticação

A autenticação utilizará JWT.

Fluxo:

1. usuário informa e-mail e senha;
2. backend valida credenciais;
3. backend gera JWT;
4. frontend armazena o token de forma adequada;
5. frontend envia o token nas requisições protegidas;
6. backend valida o token;
7. backend identifica o usuário autenticado.


## 18. Identificação do usuário

O usuário autenticado deverá ser obtido através do contexto de segurança do Spring.

Não confiar em:

idUsuario

enviado pelo frontend para determinar o proprietário de uma operação.

Exemplo incorreto:

POST /api/v1/expenses

{
    "idUsuario": "..."
}

O backend deve determinar o usuário através da autenticação.


## 19. Isolamento de dados

Consultas financeiras devem sempre considerar o usuário autenticado.

Exemplo conceitual:

INCORRETO:

findById(expenseId)

CORRETO:

findByIdAndUserId(expenseId, authenticatedUserId)

ou solução equivalente que garanta o isolamento.


## 20. Banco de dados

O banco será PostgreSQL.

O banco deverá ser executado inicialmente através de Docker Compose.


## 21. Flyway

Flyway será utilizado para controle das migrations.

Estrutura:

src/main/resources/db/migration/

Exemplo:

V1__create_users.sql
V2__create_accounts.sql
V3__create_categories.sql


As migrations devem ser pequenas e relacionadas a uma alteração lógica.


## 22. Relacionamentos

Todos os relacionamentos devem possuir Foreign Keys.

Exemplo:

expense.user_id
    -> users.id

expense.category_id
    -> categories.id


## 23. UUID

UUID será utilizado como identificador das entidades principais.

O backend deverá gerar os UUIDs de forma segura e consistente.

O frontend não deve ser responsável por gerar IDs de entidades persistidas, salvo quando houver motivo arquitetural explícito.


## 24. Datas

Utilizar:

LocalDate

quando o dado representa apenas uma data.

Exemplos:

- vencimento;
- data da compra;
- data da receita;
- data de fechamento.

Utilizar tipos de data/hora quando realmente houver necessidade de horário.


## 25. Valores monetários

Java:

BigDecimal

PostgreSQL:

NUMERIC

Nunca utilizar:

float
double

para valores financeiros.


## 26. Precisão financeira

Operações financeiras devem definir explicitamente:

- escala;
- arredondamento;
- comparação;
- soma.

Evitar utilizar operações matemáticas que possam introduzir erros de ponto flutuante.


## 27. Movimentações

Movimentações financeiras representam efeitos reais sobre contas.

Principais tipos:

ENTRADA
SAIDA
TRANSFERENCIA

Uma movimentação deve possuir referência à origem quando possível.

Exemplos:

Receita recebida
    -> ENTRADA

Despesa paga
    -> SAIDA

Pagamento de fatura
    -> SAIDA

Transferência
    -> TRANSFERENCIA


## 28. Saldo

O saldo de uma conta deve ser consequência das movimentações financeiras.

Não utilizar o saldo como única fonte de verdade.

Conceitualmente:

saldo atual =
saldo inicial
+ entradas
- saídas


## 29. Cartão de crédito

Compras no cartão não devem gerar imediatamente uma saída bancária.

Fluxo:

Compra
    |
    v
Despesa
    |
    v
Parcela
    |
    v
Fatura
    |
    v
Pagamento da fatura
    |
    v
Movimentação de saída


## 30. Parcelamentos

Ao criar uma compra parcelada, o backend deve criar as parcelas futuras dentro de uma transação.

Exemplo:

Compra:

R$ 1.200,00

Parcelas:

1/12
2/12
3/12
...
12/12


## 31. Valores diferentes nas parcelas

O sistema deve permitir valores individuais.

Exemplo:

Parcela 1:
R$ 100,00

Parcela 2:
R$ 110,00

Parcela 3:
R$ 115,00


A soma das parcelas deve ser validada conforme a regra definida para aquele parcelamento.

Não assumir divisão automática como regra universal.


## 32. Faturas

Uma fatura pertence a um cartão.

Uma fatura possui vários itens.

Conceitualmente:

Cartão
    |
    +--- Fatura
            |
            +--- Item
            +--- Item
            +--- Item


## 33. Fechamento de fatura

O sistema deve possuir uma regra centralizada para determinar a fatura de uma compra.

Essa regra não deve ser duplicada em múltiplos controllers ou componentes.

A lógica deve permanecer no domínio/backend.


## 34. Pagamento de fatura

O pagamento de uma fatura deve ser transacional.

Fluxo:

1. localizar fatura;
2. validar status;
3. validar conta;
4. validar valor;
5. registrar pagamento;
6. registrar movimentação;
7. atualizar valor pago;
8. atualizar saldo restante;
9. atualizar status.


## 35. Pagamento parcial

Uma fatura pode receber pagamento parcial.

Exemplo:

Valor da fatura:
R$ 2.000,00

Pagamento:
R$ 1.200,00

Saldo:
R$ 800,00


O sistema não deve marcar a fatura como totalmente paga.


## 36. Parcelamento de fatura

Quando o usuário transformar o saldo da fatura em parcelamento:

- manter a fatura original;
- manter os itens originais;
- registrar o pagamento realizado;
- registrar o parcelamento;
- criar parcelas futuras.

Não apagar informações históricas.


## 37. Estornos

Estornos devem preservar o registro original.

O sistema deve registrar o efeito contrário da operação original quando necessário.

Não utilizar DELETE físico como mecanismo de estorno.


## 38. Cancelamentos

Cancelamentos também não devem apagar registros financeiros relevantes.

Utilizar status apropriado.


## 39. Transferências

Transferências devem ser tratadas como uma operação única.

Uma transferência gera:

saída na conta origem

e

entrada na conta destino.

As duas operações devem possuir referência à mesma transferência.


## 40. Metas

Metas pertencem a um usuário.

O sistema deve permitir acompanhar:

- valor objetivo;
- valor acumulado;
- progresso;
- prazo;
- status.


## 41. Projeções

Projeções devem ser calculadas a partir de dados financeiros futuros.

Devem considerar:

- receitas previstas;
- despesas pendentes;
- parcelas futuras;
- faturas futuras;
- parcelamentos.

Projeções não devem criar movimentações reais.


## 42. Dashboard

O dashboard deve consumir endpoints específicos para indicadores.

Evitar carregar todas as despesas e receitas para o frontend e realizar todos os cálculos no navegador.

Cálculos financeiros relevantes devem ocorrer no backend.


## 43. Relatórios

Relatórios devem ser gerados pelo backend quando houver necessidade de:

- agregação;
- filtros;
- cálculos;
- exportação.

O frontend será responsável principalmente por solicitar e apresentar o resultado.


## 44. Exportação de fatura

A exportação da fatura deve permitir selecionar:

- cartão;
- fatura;
- responsável.

Exemplo:

Cartão:
Cartão do Ederson

Fatura:
Agosto/2026

Responsável:
Meu


O resultado deve conter somente as despesas correspondentes ao filtro solicitado.


## 45. Tratamento de erros

O backend deve possuir tratamento global de exceções.

Deve existir uma resposta padronizada.

Exemplo conceitual:

{
    "timestamp": "...",
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Dados inválidos",
    "path": "/api/v1/expenses",
    "errors": []
}


## 46. Validação

As validações devem existir no backend.

O frontend também poderá validar dados para melhorar a experiência.

Entretanto:

Frontend:
validação de experiência

Backend:
validação obrigatória


## 47. CORS

O backend deverá permitir requisições do frontend durante o desenvolvimento local.

A configuração deverá ser explícita.

Não utilizar:

allow all

de forma indiscriminada sem necessidade.


## 48. CORS em produção

Mesmo que a V1 seja local, a configuração deverá permitir futura separação entre ambientes.

Não espalhar URLs fixas pelo código.


## 49. Configuração

Utilizar arquivos de configuração do Spring Boot e variáveis de ambiente.

Exemplo:

DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
JWT_SECRET


## 50. Docker Compose

O Docker Compose deverá inicialmente fornecer:

postgres

Opcionalmente, conforme a evolução:

backend
frontend


Não adicionar serviços sem necessidade.


## 51. Desenvolvimento local

O projeto deve poder ser executado localmente de maneira previsível.

O README deverá explicar:

1. pré-requisitos;
2. como iniciar o PostgreSQL;
3. como executar o backend;
4. como executar o frontend;
5. como executar testes;
6. como acessar Swagger.


## 52. Testes

Os testes devem ser organizados por responsabilidade.

Exemplos:

unitários:

ExpenseServiceTest

integração:

ExpenseControllerIntegrationTest


## 53. Testcontainers

Testcontainers deverá ser utilizado nos testes que dependem de PostgreSQL real.

O objetivo é garantir que os testes sejam executados contra um banco semelhante ao utilizado pela aplicação.


## 54. Frontend e backend

O frontend não deve conhecer detalhes internos do banco de dados.

O contrato entre frontend e backend é a API REST.


## 55. Contratos

Quando necessário, os contratos da API devem ser documentados através de OpenAPI.

O frontend deve consumir os contratos definidos pela API.


## 56. Versionamento da API

A primeira versão será:

/api/v1


Alterações incompatíveis futuramente poderão utilizar:

/api/v2


## 57. Bibliotecas adicionais

Antes de adicionar qualquer biblioteca relevante, a IA deve explicar:

- problema;
- benefício;
- alternativa;
- custo;
- impacto.

Não adicionar bibliotecas simplesmente porque são populares.


## 58. Zod

A utilização de Zod no Angular não é obrigatória.

Antes de adicionar Zod, avaliar se Reactive Forms e validações do Angular são suficientes.

Caso Zod seja adotado posteriormente, documentar a decisão.


## 59. Gerenciamento de estado

A V1 não utilizará NgRx automaticamente.

Preferir:

- Signals;
- serviços;
- estado local.

NgRx ou solução equivalente somente deverá ser introduzido se a complexidade realmente justificar.


## 60. Bibliotecas de UI

Nenhuma biblioteca visual específica está definida inicialmente.

Antes de escolher uma biblioteca de componentes, comparar opções considerando:

- manutenção;
- acessibilidade;
- integração com Angular;
- documentação;
- personalização;
- tamanho;
- aprendizado.


## 61. Gráficos

O sistema deverá utilizar uma biblioteca de gráficos adequada ao Angular.

A biblioteca definitiva deverá ser escolhida antes da implementação do dashboard.

A escolha deve considerar:

- manutenção;
- documentação;
- compatibilidade com Angular;
- facilidade de uso;
- possibilidade de evolução.


## 62. Logs

Logs devem ser úteis para diagnóstico.

Nunca registrar:

- senhas;
- JWT;
- credenciais;
- secrets.

Evitar registrar dados financeiros desnecessários.


## 63. Auditoria

A V1 possuirá apenas auditoria básica através de:

- created_at;
- updated_at;
- status.

Uma solução completa de auditoria poderá ser adicionada futuramente.


## 64. Soft Delete

Registros financeiros importantes não devem ser fisicamente excluídos.

Quando necessário, utilizar status:

- CANCELADO;
- ESTORNADO;
- INATIVO.

Não implementar uma arquitetura genérica de soft delete para todas as entidades sem necessidade.


## 65. Responsáveis

O campo responsável das despesas será tratado como classificação.

Valores atuais:

0 - Meu
1 - Giulia
2 - Ederson
3 - Elisiane

A estrutura deve permitir futura evolução para uma entidade própria, mas isso não será implementado na V1.


## 66. Evolução

A arquitetura deve permitir futura implementação de:

- investimentos;
- importação de extratos;
- integrações bancárias;
- notificações;
- compartilhamento;
- múltiplas moedas;
- relatórios avançados.

Essas funcionalidades não devem interferir desnecessariamente na V1.


## 67. Regra de simplicidade

Não implementar abstrações apenas porque são consideradas boas práticas em sistemas corporativos.

Cada abstração deve resolver um problema real.

Evitar:

- excesso de interfaces;
- excesso de factories;
- excesso de patterns;
- generic repositories desnecessários;
- camadas sem responsabilidade clara.


## 68. Regra de aprendizado

Quando uma decisão arquitetural importante for tomada, a IA deve explicar resumidamente:

1. o que está sendo feito;
2. por que está sendo feito;
3. qual problema resolve;
4. quais alternativas existem;
5. por que a opção escolhida é adequada para este projeto.


## 69. Regra de implementação

A arquitetura descrita neste documento deve ser considerada a referência principal para implementação.

Caso a IA identifique necessidade de alteração arquitetural:

1. não implementar imediatamente;
2. explicar o problema;
3. apresentar a alteração proposta;
4. aguardar aprovação.

Alterações arquiteturais relevantes devem ser documentadas.


## 70. Objetivo final

A arquitetura deve resultar em um sistema:

- simples de entender;
- seguro;
- testável;
- modular;
- moderno;
- adequado para aprendizado;
- preparado para evolução;
- confiável para cálculos financeiros.

A prioridade é construir uma base sólida antes de adicionar funcionalidades avançadas.