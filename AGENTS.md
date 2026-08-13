# AGENTS.md — Financial Control

## 1. Objetivo

Este projeto é um sistema de controle financeiro pessoal multiusuário chamado Financial Control.

O sistema será desenvolvido inicialmente para execução local e terá como objetivo fornecer uma base sólida, organizada, testável e extensível para controle financeiro pessoal.

O projeto também possui finalidade educacional. As decisões técnicas devem ser explicadas quando forem relevantes, permitindo que o desenvolvedor compreenda não apenas "o que fazer", mas também "por que fazer".

---

## 2. Hierarquia da documentação

A documentação do projeto segue esta hierarquia:

```text
AGENTS.md
    ↓
documentação ativa em docs/20–28
    ↓
README.md
```

Significado:

- `AGENTS.md` — regras que a IA deve seguir durante o desenvolvimento;
- `docs/20`–`docs/28` — especificação detalhada (arquitetura, stack, modelo, regras, API, segurança, testes, roadmap);
- `README.md` — visão geral do projeto.

Em caso de conflito:

1. identificar o conflito;
2. corrigir a documentação conflitante;
3. deixar todos os documentos consistentes.

Não manter duas decisões diferentes sobre a mesma questão.

Documentação ativa:

- `docs/20-fluxos-financeiros.md`
- `docs/21-arquitetura-do-sistema.md`
- `docs/22-stack-tecnologica.md`
- `docs/23-modelo-de-dados.md`
- `docs/24-regras-de-negocio.md`
- `docs/25-api.md`
- `docs/26-seguranca.md`
- `docs/27-testes.md`
- `docs/28-roadmap.md`

Se existirem arquivos `docs/01`–`docs/19` no repositório, considerá-los **obsoletos/históricos**. A IA NÃO deve usá-los como fonte de verdade.

**DECISÃO PENDENTE DO DESENVOLVEDOR:** remover fisicamente `docs/01`–`docs/19` do repositório (ainda presentes no disco na consolidação; esta etapa não pôde alterá-los por restrição de escopo).

---

## 3. Regra principal de desenvolvimento

O projeto DEVE ser desenvolvido por etapas.

NUNCA implementar o sistema inteiro de uma única vez.

Cada etapa deve seguir este fluxo:

1. analisar o estado atual do projeto;
2. analisar a documentação relacionada à etapa;
3. implementar somente o escopo definido;
4. executar o build;
5. executar os testes;
6. corrigir problemas encontrados;
7. atualizar a documentação quando necessário;
8. apresentar um resumo das alterações;
9. informar eventuais decisões ou problemas encontrados;
10. aguardar autorização para iniciar a próxima etapa.

Se uma decisão importante de negócio não estiver definida, a IA deve parar e perguntar antes de implementar.

---

## 4. Stack tecnológica oficial

### Backend

- Java 25 LTS
- Spring Boot 4.1.x
- Maven
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT (Access Token + Refresh Token)
- Argon2id (hash de senhas)
- Jakarta Bean Validation
- Flyway
- springdoc-openapi
- JUnit 5
- Mockito
- AssertJ
- Testcontainers
- OpenPDF

### Frontend

- Angular 22.x
- TypeScript strict
- Standalone Components
- Signals
- Services
- Reactive Forms
- Angular HttpClient
- HTTP Interceptors
- Route Guards
- Angular Material
- Material Icons
- Apache ECharts
- ESLint
- Prettier
- npm

### Banco

- PostgreSQL 18
- UUID
- NUMERIC(19,2) / BigDecimal
- TIMESTAMPTZ para timestamps
- LocalDate para datas financeiras
- timezone America/Sao_Paulo
- Flyway

### Infraestrutura

- Docker
- Docker Compose
- PostgreSQL em container no desenvolvimento
- backend/frontend podem executar fora do Docker inicialmente
- Dockerização completa da aplicação poderá ser feita posteriormente

### Convenções

- Pacote Java: `br.com.financialcontrol`
- API: `/api/v1`
- Moeda V1: BRL
- Nunca usar float ou double para valores financeiros

Detalhes: `docs/22-stack-tecnologica.md`.

---

## 5. Tecnologias excluídas da V1

Não introduzir na V1 sem decisão futura explícita:

- Redis
- Kafka
- RabbitMQ
- Kubernetes
- microsserviços
- GraphQL
- NgRx
- Zod
- Tailwind
- Bootstrap
- H2
- SQLite
- CI/CD obrigatório
- integração bancária
- investimentos
- importação automática de extratos
- notificações
- PWA
- dark mode

---

## 6. Banco de dados

O banco oficial é PostgreSQL 18.

Regras:

- UUID como identificador das entidades;
- NUMERIC(19,2) para valores monetários;
- BigDecimal no Java;
- Foreign Keys e constraints;
- índices quando necessários;
- Flyway para migrations;
- Hibernate `ddl-auto=validate`;
- nunca alterar uma migration já executada;
- alterações posteriores via novas migrations;
- fonte de verdade do saldo: movimentações financeiras (não um `current_balance` independente).

---

## 7. Arquitetura

Aplicação monolítica modular. Não criar microsserviços na V1.

Backend:

- Controller;
- DTO;
- Service;
- regras de negócio;
- Repository;
- Entity.

Organização inicial deve facilitar separação por domínio. Estrutura definitiva na Fase 1.

O backend é a autoridade final sobre as regras de negócio.

Frontend: validações para UX; nenhuma regra crítica só no frontend.

Não expor entidades JPA na API. Usar DTOs.

Frontend organizado por features (`core`, `shared`, `features/*`).

---

## 8. Multiusuário e isolamento (regra fundamental de segurança)

O sistema é multiusuário.

Todo dado financeiro deve estar relacionado ao usuário (`userId` / IdUsuario).

O backend deve obter o usuário autenticado a partir do contexto de segurança.

Nunca confiar em um `userId` enviado pelo frontend para determinar o proprietário.

Um usuário nunca pode consultar, alterar ou excluir dados pertencentes a outro usuário.

---

## 9. Segurança

- Senhas nunca em texto puro; hash com **Argon2id**.
- Autenticação: Spring Security + JWT.
- Access Token + Refresh Token (fluxo detalhado na implementação da autenticação; arquitetura deve estar preparada).
- Segredos apenas em variáveis de ambiente; nunca versionar credenciais.

Detalhes: `docs/26-seguranca.md`.

---

## 10. Valores monetários

- Java: `BigDecimal`
- PostgreSQL: `NUMERIC(19,2)`
- Nunca float/double
- Arredondamentos financeiros explícitos e determinísticos

---

## 11. Regras financeiras fundamentais

### 11.1 Receitas

- `EXPECTED` — não altera saldo; participa de projeções
- `RECEIVED` — gera entrada financeira
- Receitas canceladas não participam de projeções

### 11.2 Despesas — status oficiais

- `OPEN`
- `PARTIALLY_PAID`
- `PAID`
- `CANCELLED`
- `REFUNDED`

`OVERDUE` **não** é status persistido. É derivado quando status é `OPEN` ou `PARTIALLY_PAID` e `dueDate` < data atual. A UI pode exibir "VENCIDA".

Formas de pagamento: `ACCOUNT`, `CREDIT_CARD`, `NONE`.

`NONE` = despesa sem cartão (pode permanecer aberta até o pagamento).

### 11.3 Cancelamento e estorno

Sem exclusão física. `CANCELLED` / `REFUNDED` preservam histórico e não impactam saldo, projeções, totais, gráficos nem contas a pagar.

### 11.4 Compra no cartão

Não reduz saldo bancário imediatamente; aumenta comprometimento; **não pode ultrapassar o limite disponível** (validação no backend).

### 11.5 Pagamento da fatura

Gera saída na conta escolhida; **não** cria nova despesa de consumo; despesas originais permanecem; pagamento parcial permitido; valor não pode exceder saldo disponível da conta.

### 11.6 Transferência

Operação própria, atômica: saída na origem + entrada no destino. Não é receita nem despesa. Contas diferentes. Sem saldo insuficiente.

### 11.7 Saldo negativo

Operações normais não permitem saldo negativo (transferências, pagamento de despesas, pagamento de fatura limitado ao saldo da conta).

### 11.8 Contas

Tipos oficiais: `BANK_ACCOUNT`, `CASH`.

Não usar: `CHECKING`, `SAVINGS`, `PERSONAL_WALLET`, `OTHER`.

`CASH` = dinheiro em espécie (ex.: Carteira Felipe). Sem entidade separada de carteira.

### 11.9 Saldo

Fonte de verdade: movimentações. Saldo derivado delas. Cache/`current_balance` só se mantido transacionalmente consistente com as movimentações — nunca duas fontes independentes.

---

## 12. Responsáveis pelas despesas

Valores oficiais:

- `MINE`
- `GIULIA`
- `EDERSON`
- `ELISIANE`
- `OTHER` (permite descrição textual)

Não são usuários do sistema. Apenas classificação para controle e prestação de contas.

---

## 13. Cartões de crédito

Campos essenciais incluem:

- nome/apelido;
- `holderName` (titular textual — não precisa ser usuário);
- limite;
- limite disponível / comprometimento;
- dia de fechamento;
- dia de vencimento;
- status;
- usuário proprietário do registro.

Um cartão pode ser usado por diferentes responsáveis nas despesas.

Regras de ciclo:

- compra no dia do fechamento pertence à próxima fatura (RN095);
- dia configurado inexistente no mês → último dia daquele mês (RN098).

---

## 14. Faturas

Status persistidos: `OPEN`, `CLOSED`, `PARTIALLY_PAID`, `PAID`.

`OVERDUE` derivado da data de vencimento.

Campos: cartão, período, fechamento, vencimento, valor total, valor pago, saldo restante, status.

Pagamento parcial permitido. Parcelamento do saldo restante é operação separada e **não** apaga/modifica compras originais.

---

## 15. Parcelamentos

Gerar automaticamente todas as parcelas futuras.

Parcelas podem ter valores diferentes; a soma deve ser exatamente o total.

Arredondamento determinístico; sem residual de centavos.

---

## 16. Número do boleto

Campo opcional na despesa, para cópia no pagamento. O sistema não gera boletos.

---

## 17. Metas, projeções, relatórios e gráficos

- Metas na V1: nome, valor alvo, acumulado, data alvo, progresso, situação.
- Projeções: receitas/despesas futuras, parcelas, faturas, compromissos; excluir `CANCELLED`/`REFUNDED` e receitas canceladas.
- PDF: **OpenPDF** (ex.: relatório por responsável em cartão de terceiro).
- Gráficos: **Apache ECharts**.

---

## 18. Testes

Obrigatórios para regras críticas. Backend: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL).

Priorizar: regras financeiras, parcelamentos, arredondamentos, faturas, pagamentos parciais, transferências, limite de cartão, isolamento, cancelamentos, estornos, autenticação/autorização.

Frontend: framework oficial do Angular 22.x. E2E Playwright posteriormente.

Detalhes: `docs/27-testes.md`.

---

## 19. API

REST em `/api/v1`. DTOs, validação, status HTTP adequados, erros padronizados, autenticação/autorização, OpenAPI/Swagger.

Não criar todos os endpoints antecipadamente. Detalhes: `docs/25-api.md`.

---

## 20. Frontend

Angular 22.x, TypeScript strict, Standalone, Signals, Services, Reactive Forms, Material, HttpClient, Interceptors, Guards, ESLint, Prettier.

Sem NgRx e sem Zod na V1.

Validação: Angular Validators no front; Jakarta Validation + regras de negócio no backend.

---

## 21. Qualidade de código

- Frontend: ESLint + Prettier
- Backend: Spotless (Google Java Format) — formatação consistente e automatizável

Não adicionar ferramentas de qualidade sem necessidade.

---

## 22. Docker

PostgreSQL via Docker Compose no desenvolvimento.

Backend e frontend podem rodar localmente fora do Docker.

Não criar configuração de produção complexa na V1.

---

## 23. Git e GitHub

Desenvolvimento no Cursor. Commits/pushes manuais pelo desenvolvedor (VSCode).

A IA NÃO deve presumir acesso ao GitHub nem executar push.

---

## 24. Gitignore

Nunca versionar: credenciais, `.env`, senhas, tokens, certificados privados, `node_modules`, `target`, `dist`, temporários, logs, gerados, configs pessoais de IDE.

---

## 25. Finalidade educacional

Em decisões técnicas importantes, explicar de forma proporcional: o quê, por quê, alternativas e adequação ao projeto.

---

## 26. Escopo da V1

Usuários, autenticação, contas, categorias, receitas, despesas, cartões, faturas, parcelamentos, pagamentos (incl. parciais), transferências, estornos, cancelamentos, metas, projeções, dashboard, gráficos, relatórios, exportação PDF, testes, Docker, PostgreSQL, migrations, Swagger/OpenAPI.

---

## 27. Fora da V1

Além das tecnologias excluídas (seção 5): deploy em produção, compartilhamento familiar, contas compartilhadas, automações bancárias, integrações externas.

---

## 28. Regra de parada

Parar e solicitar orientação quando:

- decisão de negócio não definida;
- conflito entre requisitos;
- mudança significativa de escopo;
- risco de perda de dados;
- regra financeira ambígua;
- testes sem correção segura;
- biblioteca não aprovada.

Não assumir decisões importantes de negócio. Usar: **DECISÃO PENDENTE DO DESENVOLVEDOR**.

---

## 29. Regra final

Construir uma aplicação organizada, segura, testável, compreensível, moderna, extensível, adequada ao aprendizado e com regras financeiras confiáveis.

Priorizar qualidade e clareza em vez de velocidade de implementação.
