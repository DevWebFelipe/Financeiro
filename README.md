# Financial Control

Sistema de controle financeiro pessoal desenvolvido com foco em organização financeira, planejamento futuro e aprendizado de tecnologias modernas.

O sistema será multiusuário, permitindo que cada usuário possua suas próprias contas, cartões, receitas, despesas, metas e demais informações financeiras.

---

## Objetivo

O Financial Control tem como objetivo permitir um controle financeiro pessoal completo, porém inicialmente enxuto e sólido.

O sistema deverá permitir principalmente:

- controlar receitas;
- controlar despesas;
- controlar contas bancárias;
- controlar cartões de crédito;
- controlar faturas;
- controlar despesas parceladas;
- controlar pagamentos parciais;
- realizar transferências entre contas;
- controlar metas financeiras;
- visualizar contas a pagar;
- visualizar contas a receber;
- realizar projeções financeiras;
- visualizar informações através de gráficos;
- gerar relatórios;
- exportar informações de faturas.

---

## Stack

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- OpenAPI / Swagger
- JUnit
- Mockito
- Testcontainers

### Frontend

- Angular
- TypeScript
- RxJS

Tecnologias adicionais deverão ser avaliadas antes de serem adicionadas ao projeto.

---

## Banco de dados

O banco utilizado será:

PostgreSQL

Cada informação financeira deverá possuir isolamento por usuário.

A regra fundamental é:

> Nenhum usuário pode acessar ou alterar dados financeiros pertencentes a outro usuário.

---

## Arquitetura

A arquitetura inicial seguirá:

```text
Angular
   |
   | HTTP / REST
   v
Spring Boot
   |
   | JPA / Repository
   v
PostgreSQL
```

---

## Execução local

### Pré-requisitos

Para desenvolvimento local, será necessário possuir:

- Git
- Docker
- Docker Compose
- Node.js
- npm
- Java
- IDE ou editor de código

---

## PostgreSQL

O PostgreSQL será executado através do Docker.

Subir o banco:

```bash
docker compose up -d
```

Verificar containers:

```bash
docker compose ps
```

Visualizar logs:

```bash
docker compose logs -f postgres
```

Parar containers:

```bash
docker compose down
```

---

## Variáveis de ambiente

O projeto utiliza variáveis de ambiente.

O arquivo:

```text
.env.example
```

contém as variáveis esperadas.

O arquivo:

```text
.env
```

é local e não deve ser versionado.

---

## Estrutura inicial

```text
financial-control/
|
├── AGENTS.md
├── README.md
├── .gitignore
├── .cursorignore
├── .env.example
├── docker-compose.yml
|
├── docs/
|
├── backend/
|
└── frontend/
```

A estrutura definitiva poderá ser ajustada durante a implementação da Fase 1.

---

## Desenvolvimento

O desenvolvimento será realizado utilizando o Cursor.

O GitHub pessoal não será conectado ao Cursor.

O fluxo será:

```text
Cursor
  |
  v
Desenvolvimento
  |
  v
VSCode
  |
  v
Revisão
  |
  v
Commit
  |
  v
GitHub
```

---

## Git

Os commits devem ser pequenos e relacionados a uma alteração específica.

Exemplos:

```text
feat: add account management

feat: add expense installments

feat: add credit card invoices

test: add invoice payment tests

fix: correct installment rounding
```

Antes de realizar um commit:

1. revisar o diff;
2. executar testes;
3. verificar arquivos sensíveis;
4. verificar documentação;
5. confirmar que o escopo está correto.

---

## Documentação

A documentação do projeto está localizada em:

```text
docs/
```

O arquivo:

```text
AGENTS.md
```

define as regras que devem ser seguidas pela IA durante o desenvolvimento.

---

## Desenvolvimento orientado por fases

O projeto será desenvolvido incrementalmente.

A IA não deve implementar todo o sistema de uma única vez.

O roadmap está definido em:

```text
docs/28-roadmap.md
```

Cada fase deve:

1. possuir escopo definido;
2. possuir implementação;
3. possuir testes;
4. ser validada;
5. somente então permitir o início da próxima fase.

---

## Segurança

O sistema trabalha com informações financeiras.

Por isso:

- senhas nunca serão armazenadas em texto puro;
- dados financeiros serão isolados por usuário;
- JWT será utilizado para autenticação;
- endpoints privados exigirão autenticação;
- secrets não serão versionados;
- validações serão realizadas no backend;
- operações financeiras serão protegidas contra inconsistências;
- SQL Injection deve ser evitado;
- dados financeiros não devem aparecer desnecessariamente em logs.

---

## Testes

Testes automatizados são obrigatórios para regras críticas.

Serão utilizados:

- testes unitários;
- testes de integração;
- testes de API;
- testes de segurança;
- testes de persistência;
- testes E2E quando necessário.

As regras de testes estão documentadas em:

```text
docs/27-testes.md
```

---

## Dinheiro

Valores monetários devem utilizar:

```text
BigDecimal
```

no backend.

Não utilizar:

```text
float
double
```

para representar valores financeiros.

---

## Parcelamentos

Despesas parceladas devem gerar automaticamente as parcelas futuras.

Exemplo:

Uma compra realizada em agosto em 12 parcelas deverá gerar compromissos futuros até o período correspondente.

As parcelas poderão possuir valores diferentes, desde que:

```text
soma das parcelas = valor total
```

---

## Cartões

O sistema deverá controlar:

- cartões;
- limite;
- fechamento;
- vencimento;
- compras;
- parcelas;
- faturas;
- pagamentos;
- pagamentos parciais;
- parcelamento de saldo.

---

## Faturas

Uma fatura poderá ser:

- aberta;
- fechada;
- vencida;
- parcialmente paga;
- paga;
- cancelada.

O pagamento da fatura não deve duplicar a despesa original.

---

## Estornos e cancelamentos

Despesas não devem ser apagadas fisicamente quando houver necessidade de removê-las do controle financeiro.

Devem existir estados apropriados, como:

```text
CANCELLED
REFUNDED
```

O registro histórico deve ser preservado.

---

## Transferências

Transferências entre contas devem ser tratadas como operações financeiras próprias.

Exemplo:

```text
Conta A
   |
   | Transferência
   v
Conta B
```

Transferências não devem ser contabilizadas como receita ou despesa.

---

## Metas

O sistema terá suporte a metas financeiras na V1.

Uma meta deverá permitir controlar:

- valor alvo;
- valor acumulado;
- data alvo;
- progresso;
- situação.

---

## Projeções

O sistema deverá permitir visualizar compromissos financeiros futuros.

As projeções devem considerar:

- receitas futuras;
- despesas futuras;
- parcelas;
- faturas;
- compromissos conhecidos.

Despesas canceladas e receitas canceladas não devem participar das projeções.

---

## Responsável pela despesa

Despesas poderão possuir um responsável/prestação de contas:

```text
0 - Meu
1 - Giulia
2 - Ederson
3 - Elisiane
4 - Outro
```

Quando o responsável for:

```text
Outro
```

será permitido informar uma descrição textual.

Isso será utilizado principalmente para facilitar o controle de despesas realizadas no cartão de terceiros.

---

## Relatórios

O sistema deverá permitir gerar relatórios.

Um dos principais casos de uso será:

> Gerar uma relação das despesas realizadas no cartão de outra pessoa para conferência e pagamento.

A V1 deverá possuir exportação em PDF.

---

## Dashboard

O dashboard deverá apresentar informações como:

- saldo total;
- receitas;
- despesas;
- contas a pagar;
- contas a receber;
- faturas;
- projeções.

Também deverá possuir gráficos.

Exemplos:

- despesas por categoria;
- despesas por cartão;
- despesas por responsável;
- fluxo de caixa;
- receitas x despesas.

---

## V1

A primeira versão deverá permitir:

- usuários;
- contas;
- categorias;
- receitas;
- despesas;
- parcelamentos;
- cartões;
- faturas;
- pagamentos;
- pagamentos parciais;
- parcelamento de fatura;
- transferências;
- metas;
- contas a pagar;
- contas a receber;
- projeções;
- dashboard;
- gráficos;
- relatórios;
- exportação PDF.

---

## Funcionalidades futuras

Não fazem parte da V1:

- investimentos;
- importação automática de extratos bancários;
- notificações;
- integração bancária;
- deploy;
- CI/CD completo;
- PWA;
- dark mode;
- auditoria avançada.

Essas funcionalidades poderão ser implementadas posteriormente.

---

## Objetivo de aprendizado

Este projeto também será utilizado como projeto de aprendizado.

Sempre que uma tecnologia importante for introduzida, deve ser possível entender:

- o que ela faz;
- por que foi escolhida;
- como funciona;
- onde está sendo utilizada;
- quais alternativas existem.

A IA não deve simplesmente gerar código sem explicação quando uma decisão técnica importante estiver sendo tomada.

---

## Regra para IA

Antes de implementar qualquer funcionalidade:

1. ler `AGENTS.md`;
2. consultar a documentação relacionada;
3. verificar o estado atual do projeto;
4. identificar dependências;
5. propor uma abordagem;
6. implementar somente o escopo solicitado;
7. criar ou atualizar testes;
8. executar testes;
9. revisar o resultado;
10. informar os arquivos alterados.

A IA não deve implementar funcionalidades futuras sem autorização.

---

## Status

Projeto em:

```text
Planejamento / Pré-implementação
```

Próxima etapa:

```text
Fase 1 — Estrutura inicial
```