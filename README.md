# Financial Control

Sistema de controle financeiro pessoal multiusuário, com foco em organização financeira, planejamento futuro e aprendizado de tecnologias modernas.

Cada usuário possui suas próprias contas, cartões, receitas, despesas, metas e demais informações financeiras.

---

## Hierarquia da documentação

```text
AGENTS.md
    ↓
docs/20–28 (decisões funcionais e técnicas oficiais)
    ↓
docs/CODING_STANDARDS.md (convenções de código)
    ↓
.cursor/rules/*.mdc (instruções operacionais para o Cursor)
```

- `AGENTS.md` — autoridade máxima; regras para a IA e desenvolvimento por etapas
- `docs/20`–`docs/28` — especificação completa
- `docs/CODING_STANDARDS.md` — convenções de código; não pode contradizer os documentos superiores
- `.cursor/rules/*.mdc` — instruções operacionais; não criam decisões novas
- Este README — visão geral e execução local; não é fonte de decisões técnicas

Em conflito: corrigir a documentação até ficar consistente. Não manter duas decisões sobre o mesmo tema.

Documentação ativa em `docs/`:

| Documento | Conteúdo |
|-----------|----------|
| `20-fluxos-financeiros.md` | Fluxos financeiros |
| `21-arquitetura-do-sistema.md` | Arquitetura |
| `22-stack-tecnologica.md` | Stack oficial |
| `23-modelo-de-dados.md` | Modelo de dados |
| `24-regras-de-negocio.md` | Regras de negócio |
| `25-api.md` | API REST |
| `26-seguranca.md` | Segurança |
| `27-testes.md` | Estratégia de testes |
| `28-roadmap.md` | Roadmap por fases |
| `CODING_STANDARDS.md` | Convenções de código |

Se ainda existirem `docs/01`–`docs/19`, são **históricos/obsoletos** e não devem ser usados.

---

## Objetivo

Permitir controle financeiro pessoal completo, inicialmente enxuto e sólido:

- receitas e despesas;
- contas bancárias e dinheiro em espécie (`CASH`);
- cartões, faturas e parcelamentos;
- pagamentos e pagamentos parciais;
- transferências entre contas;
- metas e projeções;
- contas a pagar / a receber;
- dashboard, gráficos e relatórios;
- exportação PDF (ex.: despesas em cartão de terceiro por responsável).

---

## Stack oficial

### Backend

- Java 25 LTS
- Spring Boot 4.1.x
- Maven 3.9.x (≥ 3.9.12); Maven Wrapper no backend
- Spring Web, Spring Data JPA, Hibernate
- Spring Security, JWT (Access + Refresh Token)
- Argon2id
- Jakarta Bean Validation
- Flyway
- springdoc-openapi
- JUnit 5, Mockito, AssertJ, Testcontainers
- OpenPDF

### Frontend

- Angular 22.x
- Node.js 22.x LTS (≥ 22.22.3); 24.x ≥ 24.15.0 aceito
- npm empacotado com o Node.js
- TypeScript strict
- Standalone Components, Signals, Services
- Reactive Forms, HttpClient, Interceptors, Route Guards
- Angular Material, Material Icons
- Apache ECharts
- ESLint, Prettier

### Banco

- PostgreSQL 18 (`postgres:18-alpine` via Docker)
- UUID v7 gerado pela aplicação
- NUMERIC(19,2) / BigDecimal; `RoundingMode.HALF_UP`
- TIMESTAMPTZ / `Instant` em UTC; `DATE` / `LocalDate` para calendário financeiro
- Calendário financeiro: timezone `America/Sao_Paulo`
- Flyway; Hibernate `ddl-auto=validate`

### Infraestrutura

- Docker Engine ≥ 24 / Docker Desktop / Compose V2 ≥ 2.24
- PostgreSQL no desenvolvimento via Docker Compose
- Backend e frontend podem rodar fora do Docker inicialmente

### Convenções

- Pacote Java: `br.com.financialcontrol`
- API: `/api/v1`
- Moeda V1: BRL

Detalhes: `docs/22-stack-tecnologica.md`.

---

## Fora da V1

Não introduzir sem decisão futura explícita: Redis, Kafka, RabbitMQ, Kubernetes, microsserviços, GraphQL, NgRx, Zod, Tailwind, Bootstrap, H2, SQLite, CI/CD obrigatório, integração bancária, investimentos, importação de extratos, notificações, PWA, dark mode, deploy em produção, contas compartilhadas.

---

## Arquitetura

```text
Angular 22
   |
   | HTTP / REST /api/v1
   v
Spring Boot 4.1
   |
   | JPA / Flyway
   v
PostgreSQL 18
```

Monólito modular. Backend é autoridade das regras. Frontend apresenta e valida para UX.

---

## Isolamento de usuários

> Nenhum usuário pode consultar, alterar ou excluir dados financeiros de outro usuário.

O backend obtém o usuário do contexto de segurança. Nunca confiar em `userId` enviado pelo frontend.

---

## Regras financeiras (resumo)

| Tema | Regra |
|------|--------|
| Contas | Tipos: `BANK_ACCOUNT`, `CASH` |
| Responsável | `MINE`, `GIULIA`, `EDERSON`, `ELISIANE`, `OTHER` (+ texto) — não são usuários |
| Despesa status | `OPEN`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `REFUNDED` |
| Vencida | Derivada (`OVERDUE` não persistido) |
| Cartão | `holderName` textual; compra não reduz saldo bancário; respeita limite disponível; compra no dia do fechamento vai para a próxima fatura; dia inexistente no mês → último dia do mês |
| Fatura | `OPEN`, `CLOSED`, `PARTIALLY_PAID`, `PAID`; pagamento não cria despesa nova |
| Parcelas | Valores podem diferir; soma = total; arredondamento determinístico |
| Transferência | Atômica; não é receita/despesa; sem saldo insuficiente |
| Saldo | Derivado de movimentações |
| Pagamentos | Sem saldo negativo; fatura parcial limitada ao saldo da conta |
| PDF / gráficos | OpenPDF / Apache ECharts |

Detalhes: `docs/24-regras-de-negocio.md`.

---

## Execução local

### Pré-requisitos

Versões oficiais: `docs/22-stack-tecnologica.md` (Environment Contract).

- Git ≥ 2.39
- Docker Desktop (Engine ≥ 24, daemon em execução)
- Docker Compose V2 ≥ 2.24 (`docker compose`)
- Node.js 22.x LTS ≥ 22.22.3 (preferencial) ou 24.x ≥ 24.15.0
- npm empacotado com o Node.js (não atualizar com `npm install -g npm@latest`)
- Java 25 LTS (JDK, com `javac`)
- Maven 3.9.x ≥ 3.9.12 (Maven Wrapper será criado com o backend)
- IDE ou editor

### Diagnóstico do ambiente (Windows)

Somente leitura; não instala nem altera o sistema:

```powershell
.\scripts\check-environment.ps1
```

Se a política de execução bloquear o script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-environment.ps1
```

### PostgreSQL

```bash
docker compose up -d
docker compose ps
docker compose logs -f postgres
docker compose down
```

### Variáveis de ambiente

- `.env.example` — modelo das variáveis
- `.env` — local, **não versionar**

### Estrutura

```text
financial-control/
├── AGENTS.md
├── README.md
├── .gitignore
├── .cursorignore
├── .env.example
├── docker-compose.yml
├── docs/
│   ├── 20–28
│   └── CODING_STANDARDS.md
├── .cursor/rules/
├── scripts/
│   └── check-environment.ps1
├── backend/          (Fase 1+)
└── frontend/         (Fase 1+)
```

Portas típicas: Angular `4200`, Spring Boot `8080`, PostgreSQL `5432`.

---

## Desenvolvimento

Desenvolvimento assistido no Cursor. Commits e pushes manuais via VSCode/Git. A IA não deve executar push nem assumir acesso ao GitHub.

Fluxo por fases: ver `docs/28-roadmap.md`.

Cada fase: escopo definido → implementação → testes → validação → só então a próxima.

---

## Segurança

- Argon2id para senhas
- JWT (Access + Refresh Token)
- Isolamento por usuário
- Secrets fora do código
- Validação no backend
- Sem dados sensíveis desnecessários em logs

Ver `docs/26-seguranca.md`.

---

## Testes

Unitários, integração, API e segurança. Testcontainers com PostgreSQL. Ver `docs/27-testes.md`.

---

## Status

```text
Fase 1 — Fundação (Environment Contract)
```

Etapa atual: diagnóstico do ambiente de desenvolvimento.

A IA não deve avançar para instalação automática (`setup-windows.ps1`) nem para o scaffold do backend/frontend sem autorização explícita.
