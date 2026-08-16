# Stack Tecnológica — Financial Control

## 1. Objetivo

Este documento define a **stack oficial** do Financial Control.

A IA e o desenvolvimento devem seguir esta stack.

Não substituir tecnologias sem justificativa, atualização da documentação e alinhamento com `AGENTS.md`.

Hierarquia: `AGENTS.md` → `docs/20–28` → `docs/CODING_STANDARDS.md` → `.cursor/rules/*.mdc`.


# 2. Princípio

Priorizar: tecnologias modernas e estáveis, boa documentação, aprendizado, integração e manutenção.


# 3. Stack oficial — Backend

| Tecnologia | Versão / definição |
|------------|-------------------|
| Java | **25 LTS** |
| Spring Boot | **4.1.x** |
| Build | **Maven 3.9.x** (≥ **3.9.12**; Wrapper no backend) |
| Spring Web | API REST |
| Spring Data JPA | Persistência |
| Hibernate | Implementação JPA |
| Spring Security | Segurança |
| JWT | Access Token HS256 (Fase 3). Refresh Token: não implementado nesta fase |
| Hash de senha | **Argon2id** (`Argon2PasswordEncoder` + BouncyCastle) |
| Validação | Jakarta Bean Validation |
| Migrations | Flyway |
| OpenAPI | springdoc-openapi **3.0.2** (linha 3.x / Spring Boot 4) |
| PDF | **OpenPDF** |
| Testes | JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers |


# 4. Java

Utilizar Java **25 LTS**.

Não alterar a versão sem decisão documentada e atualização de `AGENTS.md` / este documento.


# 5. Spring Boot

Utilizar Spring Boot **4.1.x**.

Dependências oficiais via starters compatíveis com essa linha.


# 6. Maven

Ferramenta de build oficial. Arquivo principal: `pom.xml`.

Linha oficial: **Maven 3.9.x**, mínimo **3.9.12**.

Não utilizar Maven 4 nesta fase do projeto.

O backend utilizará **Maven Wrapper** (`mvnw` / `mvnw.cmd`) para garantir reprodutibilidade da versão do Maven. A criação efetiva do wrapper ocorrerá quando o projeto Spring Boot for criado; a versão do wrapper deverá respeitar a política 3.9.x (≥ 3.9.12).

Comandos esperados: `mvn test`, `mvn clean verify` (ou `.\mvnw` / `.\mvnw.cmd` após o wrapper existir).


# 7. Pacote Java

Namespace oficial:

```text
br.com.financialcontrol
```

Não utilizar `com.example` nem `com.financialcontrol`.


# 8. Persistência

- Spring Data JPA + Hibernate
- PostgreSQL **18**
- Driver JDBC oficial PostgreSQL
- UUID v7 gerado pela aplicação; coluna `UUID` sem default de geração
- Flyway para todo schema oficial; nomes de migration no plural (`V1__create_accounts.sql`)
- `spring.jpa.hibernate.ddl-auto=validate`
- Nunca `ddl-auto=create` / `update` como fonte do schema


# 9. Segurança e autenticação

- Spring Security (stateless)
- JWT Access Token: **HS256**, biblioteca **Nimbus JOSE JWT** (`nimbus-jose-jwt` 10.3)
- Claims: `sub` (UUID), `iat`, `exp`; validade 30 minutos
- Refresh Token: **não implementado na Fase 3**
- Senhas: **Argon2id** apenas (nunca texto puro; nunca MD5/SHA como hash de senha)
- Não usar BCrypt na V1
- OpenAPI: `springdoc-openapi-starter-webmvc-ui` **3.0.2** (linha 3.x, Spring Boot 4)


# 10. Validação e API

- Jakarta Bean Validation nos DTOs de entrada
- Regras de negócio nos services
- springdoc-openapi + Swagger UI no desenvolvimento
- Prefixo oficial: `/api/v1`
- DTOs na API; não expor entidades JPA


# 11. PDF

Biblioteca oficial: **OpenPDF**.

Geração no backend, sob demanda, com isolamento por usuário.


# 12. Lombok e MapStruct

- Lombok: permitido com uso restrito (evitar `@SneakyThrows` e excessos)
- MapStruct: **não** introduzir na Fase 1 apenas por convenção
- Mapeamento manual é aceitável quando for pequeno e claro
- Não criar `*Mapper` automaticamente para cada entidade


# 13. Testes backend

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- Testcontainers com **PostgreSQL real**
- Não usar H2 como substituto padrão do PostgreSQL em testes de persistência


# 14. Formatação backend

Solução oficial de formatação automatizável:

**Spotless** com **Google Java Format**

Deve ser documentada no projeto na Fase 1 e executável via Maven.

Não adicionar outras ferramentas de qualidade sem necessidade.


# 15. Stack oficial — Frontend

| Tecnologia | Definição |
|------------|-----------|
| Angular | **22.x** |
| Node.js | **22.x LTS** (≥ **22.22.3**); **24.x** ≥ **24.15.0** aceito |
| npm | Empacotado com o Node.js utilizado (não pinado à parte) |
| TypeScript | strict |
| Componentes | Standalone |
| Estado | Signals + Services (sem NgRx) |
| Forms | Reactive Forms |
| HTTP | HttpClient + Interceptors |
| Rotas | Route Guards |
| UI | Angular Material + Material Icons |
| Gráficos | **Apache ECharts** |
| Lint / format | ESLint + Prettier |
| Pacotes | npm (`package-lock.json` versionado) |


# 16. Angular

- Versão **22.x**
- Preferir recursos nativos
- Node.js **22.x LTS** (≥ **22.22.3**), linha preferencial
- Node.js **24.x** (≥ **24.15.0**) é linha compatível aceita (não é WARNING)
- Não utilizar Node.js Current (ex.: 26.x) como padrão de instalação
- npm: versão empacotada com o Node.js utilizado; não executar `npm install -g npm@latest` como procedimento oficial
- Porta padrão de desenvolvimento: `4200`
- Evitar `any` sem justificativa


# 17. Estado e validação frontend

- Sem NgRx na V1
- Sem Zod na V1
- Validação: Angular Validators
- Backend permanece autoridade final


# 18. Gráficos

Biblioteca oficial: **Apache ECharts** (integração Angular compatível com a versão 22.x).

Dados preparados pelo backend. Não calcular regras financeiras críticas só no gráfico.


# 19. CSS

CSS modular + CSS variables quando útil.

Não utilizar Bootstrap nem Tailwind na V1.


# 20. Testes frontend

Framework oficial recomendado pelo Angular 22.x no scaffold do projeto.

Priorizar: services, componentes críticos, formulários.

E2E: **Playwright** poderá ser introduzido posteriormente (não obrigatório no início da V1).


# 21. Stack oficial — Banco

| Item | Definição |
|------|-----------|
| SGBD | PostgreSQL **18** (imagem `postgres:18-alpine` via Docker) |
| IDs | UUID v7 gerado pela aplicação |
| Dinheiro | NUMERIC(**19,2**) |
| Percentual | Fração (`0.0525` = 5,25%) |
| Java | BigDecimal; `RoundingMode.HALF_UP`; escala 2 |
| Booleanos | coluna `active` / Java `isActive` |
| Datas financeiras | DATE / LocalDate (`America/Sao_Paulo`) |
| Timestamps | TIMESTAMPTZ / Instant (UTC) |
| Timezone app | **America/Sao_Paulo** (calendário financeiro) |
| Moeda V1 | **BRL** |


# 22. Timezone e datas

Persistência de instantes:

- `Instant` no Java
- `TIMESTAMP WITH TIME ZONE` no PostgreSQL
- valores persistidos em UTC

Calendário financeiro:

- timezone da aplicação: `America/Sao_Paulo`
- "hoje", vencimento, fechamento de fatura, atraso e ciclos usam esse calendário
- datas sem horário: `LocalDate` / `DATE`

O frontend não deve usar o timezone local do navegador para decidir regras financeiras.


# 23. Infraestrutura

- Docker Engine **≥ 24**; no Windows, **Docker Desktop** (versão atual/recomendada)
- Docker Compose **V2** (`docker compose`), mínimo **2.24**; o binário legado `docker-compose` não é requisito
- PostgreSQL **18** em container no desenvolvimento: imagem `postgres:18-alpine` em `docker-compose.yml`
- Não exigir PostgreSQL instalado diretamente no Windows
- Backend e frontend podem rodar fora do Docker inicialmente
- Dockerização completa da aplicação: posteriormente
- Volume Docker para dados; não commitado
- Usuário/database dedicados (não usar `postgres` como usuário da app em ambiente configurado)
- Portas típicas: app `8080`, Angular `4200`, Postgres `5432` (configuráveis)


# 24. Configuração

- `application.yml` + variáveis de ambiente
- Secrets nunca commitados
- CORS apenas para origens necessárias; configurável
- Profiles: development / test (production depois)


# 25. Tecnologias excluídas da V1

Não introduzir sem decisão futura explícita:

- Redis
- Kafka / RabbitMQ
- Kubernetes
- microsserviços
- GraphQL
- NgRx
- Zod
- Tailwind / Bootstrap
- H2 / SQLite (como banco oficial ou substituto padrão de testes de persistência)
- CI/CD obrigatório
- integração bancária
- investimentos
- importação automática de extratos
- notificações
- PWA
- dark mode
- Spring Cloud
- Elasticsearch
- event sourcing / CQRS

Exceção aprovada na Fase 9: Spring `@Scheduled` **somente** para abertura/fechamento de faturas (RN096A). Não é autorização para mensageria nem job platform.


# 26. Arquitetura tecnológica

Não utilizar na V1:

- microsserviços
- mensageria
- cache distribuído

Monólito modular: uma app Spring Boot + uma app Angular + um PostgreSQL.


# 27. Qualidade

Backend:

- Spotless (Google Java Format)
- `mvn clean verify`

Frontend:

- `npm run lint`
- `npm run format` (ou equivalente)
- `npm test`
- `npm run build`


# 28. Dependências

Antes de adicionar biblioteca:

1. problema real;
2. solução nativa existente?;
3. manutenção e documentação;
4. licença;
5. atualizar este documento e `AGENTS.md`.


# 29. Git / GitHub / Cursor

- Git **≥ 2.39** (não fixar patch exato; recomendado: versão atual do Git for Windows)
- GitHub
- Commits manuais pelo desenvolvedor
- IA não executa push nem assume acesso ao GitHub
- `.gitignore` e `.cursorignore` respeitados


# 30. Environment Contract

Este contrato define as versões e políticas oficiais do ambiente de desenvolvimento Windows.

Diagnóstico somente leitura: `scripts/check-environment.ps1`.

Não instalar, atualizar nem modificar o ambiente a partir do script de diagnóstico.

| Tecnologia | Regra oficial |
|------------|---------------|
| Java/JDK | **25 LTS** (`javac` obrigatório) |
| Spring Boot | **4.1.x** |
| Angular | **22.x** |
| Node.js | **22.x LTS**, mínimo **22.22.3** (linha preferencial) |
| Node.js 24 | Aceito quando **≥ 24.15.0** (não é WARNING) |
| npm | Versão empacotada pelo Node.js utilizado |
| Maven | **3.9.x**, mínimo **3.9.12** |
| Maven Wrapper | Será utilizado no backend (criado com o projeto Spring Boot) |
| Git | **≥ 2.39** (recomendado: Git for Windows atual) |
| Docker Engine | **≥ 24** (daemon em execução) |
| Docker Desktop | Versão atual/recomendada no Windows |
| Docker Compose | **V2**, mínimo **2.24** (`docker compose`) |
| PostgreSQL | **18-alpine** via Docker Compose |

Node.js:

```text
Node 22 >= 22.22.3  → OK (preferencial)
Node 24 >= 24.15.0  → OK (compatível aceito)
Node 22 < 22.22.3   → incompatível
Node 24 < 24.15.0   → incompatível
Node 20 ou inferior → incompatível
Node 26             → não é padrão (Current; não LTS ainda)
```

Não utilizar Node.js Current como padrão de instalação.

npm: não fixar versão independente; não usar `npm install -g npm@latest` como procedimento oficial.

Maven 4 não deve ser utilizado nesta fase. Enquanto o wrapper não existir, o Maven instalado no PATH deve ser 3.9.x ≥ 3.9.12. Quando o wrapper existir, ele será a fonte preferencial da versão do projeto.

Docker:

```text
Docker não instalado              → ERROR
Docker instalado + daemon parado  → ERROR
Docker instalado + daemon ativo   → OK (Engine ≥ 24)
```

PostgreSQL local no Windows não é obrigatório. A fonte oficial é a imagem `postgres:18-alpine` em `docker-compose.yml`.


# 31. Stack consolidada V1

```text
Java 25 LTS
Spring Boot 4.1.x
Maven 3.9.x (≥ 3.9.12) + Maven Wrapper no backend
Spring Web / Data JPA / Hibernate
Spring Security + JWT Access Token (HS256)
Argon2id
Jakarta Validation
Flyway
springdoc-openapi
OpenPDF
JUnit 5 / Mockito / AssertJ / Testcontainers
Spotless (Google Java Format)

Angular 22.x
Node.js 22.x LTS (≥ 22.22.3); 24.x ≥ 24.15.0 aceito
npm empacotado com o Node.js
TypeScript strict
Standalone / Signals / Services
Reactive Forms / HttpClient / Interceptors / Guards
Angular Material / Material Icons
Apache ECharts
ESLint / Prettier

PostgreSQL 18 (postgres:18-alpine via Docker)
UUID v7 (app) / NUMERIC(19,2) / TIMESTAMPTZ UTC
Calendário financeiro: America/Sao_Paulo / BRL
RoundingMode.HALF_UP / percentual como fração

Docker Engine ≥ 24 / Docker Desktop
Docker Compose V2 ≥ 2.24
Git ≥ 2.39
Pacote: br.com.financialcontrol
API: /api/v1
```


# 32. Regra final

Se a IA quiser alterar qualquer item desta stack:

1. explicar o motivo;
2. apresentar alternativa;
3. avaliar impacto;
4. atualizar `AGENTS.md`, este documento e o README;
5. somente então implementar.
