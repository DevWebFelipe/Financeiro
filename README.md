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
- Spring Security, JWT Access Token (HS256; Refresh Token não implementado na Fase 3)
- Argon2id (`Argon2PasswordEncoder` + BouncyCastle)
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
   | Controller → Service → Repository
   | JPA / Flyway
   v
PostgreSQL 18
```

Monólito modular. Backend é autoridade das regras. Frontend apresenta e valida para UX.

Fluxo HTTP do backend: Controller → Service → Repository. O Controller não acessa o Repository. Toda fronteira HTTP usa DTOs.

---

## Isolamento de usuários

> Nenhum usuário pode consultar, alterar ou excluir dados financeiros de outro usuário.

O backend obtém o usuário do contexto de segurança (`SecurityContext`). Nunca confiar em `userId` enviado pelo frontend.

---

## Autenticação (Fase 3)

Implementado: cadastro, login, Access Token JWT, perfil autenticado e alteração de senha.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/health` | público | `200` `{ "status": "UP" }` |
| `POST` | `/api/v1/auth/register` | público | `201` perfil (sem login automático) |
| `POST` | `/api/v1/auth/login` | público | `200` `{ accessToken, tokenType: "Bearer", expiresIn: 1800 }` |
| `GET` | `/api/v1/users/me` | Bearer | `200` perfil |
| `PUT` | `/api/v1/users/me` | Bearer | `200`; apenas `name` e `email` |
| `PUT` | `/api/v1/users/me/password` | Bearer | `204` |

Regras em vigor:

- Access Token JWT, algoritmo **HS256**, validade **30 minutos** (`expiresIn`: 1800 segundos);
- transporte: `Authorization: Bearer <token>`;
- identidade: claim `sub` = UUID do usuário, lido pelo backend a partir do SecurityContext;
- senhas com **Argon2id**; nunca texto puro;
- e-mail normalizado (trim + minúsculas) antes de persistir e autenticar;
- usuário desativado não autentica (`401` `"Credenciais inválidas."`, mesma mensagem de e-mail/senha inválidos);
- campos JSON desconhecidos na API são rejeitados (`400`);
- e-mail duplicado no cadastro: `409`.

Não implementado na Fase 3: Refresh Token, logout no backend, OAuth, MFA, roles, rate limiting, frontend de autenticação.

Detalhes: `docs/25-api.md` e `docs/26-seguranca.md`.

---

## Contas (Fase 4)

Implementado: criar, listar, consultar, editar (`name` e `type`), desativar, reativar e consultar saldo. Tipos V1: `BANK_ACCOUNT` e `CASH`.

| Método | Caminho | Autenticação | Resposta |
|--------|---------|--------------|----------|
| `GET` | `/api/v1/accounts` | Bearer | `200` array das contas do usuário |
| `GET` | `/api/v1/accounts/{id}` | Bearer | `200`; `404` se não for do usuário |
| `POST` | `/api/v1/accounts` | Bearer | `201` |
| `PUT` | `/api/v1/accounts/{id}` | Bearer | `200`; apenas `name` e `type` |
| `POST` | `/api/v1/accounts/{id}/deactivate` | Bearer | `200` desativação lógica |
| `POST` | `/api/v1/accounts/{id}/activate` | Bearer | `200` |
| `GET` | `/api/v1/accounts/{id}/balance` | Bearer | `200` `{ accountId, balance }` |

Na Fase 4 o saldo consultado é o saldo inicial. Não existe `current_balance` persistido. Extrato (`/statement`) não foi implementado: depende de movimentações reais das fases seguintes.

Não existe `DELETE` de conta.

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

### Início rápido (Windows)

Na raiz do projeto:

```powershell
powershell.exe -ExecutionPolicy Bypass -File ".\scripts\start.ps1"
```

O script valida o ambiente, garante o `.env` local (gera `JWT_SECRET` se o arquivo ainda não existir), sobe o PostgreSQL, o backend e o frontend.

Para encerrar:

```powershell
powershell.exe -ExecutionPolicy Bypass -File ".\scripts\stop.ps1"
```

O arquivo `.env` **não é versionado**. O modelo é `.env.example`.

### Pré-requisitos

Versões oficiais: `docs/22-stack-tecnologica.md` (Environment Contract).

- Git ≥ 2.39
- Docker Desktop (Engine ≥ 24, daemon em execução)
- Docker Compose V2 ≥ 2.24 (`docker compose`)
- Node.js 22.x LTS ≥ 22.22.3 (preferencial) ou 24.x ≥ 24.15.0
- npm empacotado com o Node.js (não atualizar com `npm install -g npm@latest`)
- Java 25 LTS (JDK, com `javac`)
- Maven 3.9.x ≥ 3.9.12; o backend já possui Maven Wrapper (`backend/mvnw` / `backend/mvnw.cmd`)
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

- `.env.example` — modelo das variáveis (sem segredo real)
- `.env` — local, **não versionar** (já está no `.gitignore`)

O backend **exige** `JWT_SECRET` com no mínimo 32 bytes. Não há default em `application.yml`. O valor de exemplo em `.env.example` não deve ser usado em produção.

`scripts/start.ps1` cria `.env` a partir de `.env.example` e gera um `JWT_SECRET` local quando o arquivo ainda não existe. Para carregar o `.env` em uma sessão manual:

```powershell
. .\scripts\import-dotenv.ps1
```

Outras variáveis típicas: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_EXPIRATION_MINUTES` (padrão 30), `CORS_ALLOWED_ORIGINS`, `APP_PORT`.

### Backend

Com o PostgreSQL no ar e o `.env` configurado, na raiz do projeto:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
```

Ou, de forma equivalente:

```powershell
. .\scripts\import-dotenv.ps1
cd backend
.\mvnw.cmd spring-boot:run
```

- Health: `http://localhost:8080/api/v1/health`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend

O scaffold Angular da Fase 1 existe em `frontend/`. A UI de autenticação **não** foi implementada na Fase 3.

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
│   ├── check-environment.ps1
│   ├── import-dotenv.ps1
│   ├── run-backend.ps1
│   ├── start.ps1
│   └── stop.ps1
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
- JWT Access Token (HS256, 30 minutos, Bearer)
- Refresh Token **não** implementado na Fase 3
- Isolamento por usuário via SecurityContext
- Secrets fora do código (`JWT_SECRET` obrigatório)
- Validação no backend (Jakarta Bean Validation + regras de negócio)
- Sem dados sensíveis desnecessários em logs

Ver `docs/26-seguranca.md`.

---

## Testes

Unitários, integração, API e segurança. Testcontainers com PostgreSQL. Ver `docs/27-testes.md`.

---

## Status

```text
Fase 0 — Planejamento — CONCLUÍDA
Fase 1 — Fundação / estrutura inicial — CONCLUÍDA
Fase 2 — Persistência / modelo de dados — CONCLUÍDA
Fase 3 — Autenticação e segurança — CONCLUÍDA
Fase 4 — Contas — CONCLUÍDA
```

Estado atual do backend (Fases 1–4): Spring Boot **4.1.0**, Java **25**, Maven Wrapper, PostgreSQL **18**, Flyway, Spring Security, JWT Access Token HS256, Argon2id, Jakarta Bean Validation, Testcontainers, OpenAPI/Swagger, fluxo Controller → Service → Repository, domínio de contas.

Próxima fase: **Fase 5 — Categorias**. Não iniciar sem autorização explícita.

Não implementar Refresh Token, logout, OAuth, MFA, roles, rate limiting, frontend de autenticação nem módulos financeiros da Fase 5+ sem autorização.
