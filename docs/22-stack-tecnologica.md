# Stack Tecnológica — Financial Control

## 1. Objetivo

Este documento define a **stack oficial** do Financial Control.

A IA e o desenvolvimento devem seguir esta stack.

Não substituir tecnologias sem justificativa, atualização da documentação e alinhamento com `AGENTS.md`.

Hierarquia: `AGENTS.md` → `docs/20–28` → `README.md`.


# 2. Princípio

Priorizar: tecnologias modernas e estáveis, boa documentação, aprendizado, integração e manutenção.


# 3. Stack oficial — Backend

| Tecnologia | Versão / definição |
|------------|-------------------|
| Java | **25 LTS** |
| Spring Boot | **4.1.x** |
| Build | **Maven** (`pom.xml`) |
| Spring Web | API REST |
| Spring Data JPA | Persistência |
| Hibernate | Implementação JPA |
| Spring Security | Segurança |
| JWT | Access Token + Refresh Token |
| Hash de senha | **Argon2id** |
| Validação | Jakarta Bean Validation |
| Migrations | Flyway |
| OpenAPI | springdoc-openapi |
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

Comandos esperados: `mvn test`, `mvn clean verify`.


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
- Flyway para todo schema oficial
- `spring.jpa.hibernate.ddl-auto=validate`
- Nunca `ddl-auto=create` / `update` como fonte do schema


# 9. Segurança e autenticação

- Spring Security
- JWT: Access Token + Refresh Token
- A implementação detalhada do refresh será definida na fase de autenticação; a arquitetura deve estar preparada
- Senhas: **Argon2id** apenas (nunca texto puro; nunca MD5/SHA como hash de senha)
- Não usar BCrypt na V1


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
- MapStruct: opcional; mapeamento manual aceitável quando simples


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
- Node.js LTS compatível com Angular 22.x
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
| SGBD | PostgreSQL **18** |
| IDs | UUID |
| Dinheiro | NUMERIC(**19,2**) |
| Java | BigDecimal |
| Datas financeiras | DATE / LocalDate |
| Timestamps | TIMESTAMPTZ / Instant |
| Timezone app | **America/Sao_Paulo** |
| Moeda V1 | **BRL** |


# 22. Timezone e datas

- Timezone da aplicação: `America/Sao_Paulo`
- Datas de vencimento/fechamento: LocalDate
- Eventos de sistema: Instant + TIMESTAMPTZ
- Não depender do timezone do navegador para regras financeiras


# 23. Infraestrutura

- Docker + Docker Compose
- PostgreSQL em container no desenvolvimento (`docker-compose.yml`)
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

- Git + GitHub
- Commits manuais pelo desenvolvedor
- IA não executa push nem assume acesso ao GitHub
- `.gitignore` e `.cursorignore` respeitados


# 30. Stack consolidada V1

```text
Java 25 LTS
Spring Boot 4.1.x
Maven
Spring Web / Data JPA / Hibernate
Spring Security + JWT (Access + Refresh)
Argon2id
Jakarta Validation
Flyway
springdoc-openapi
OpenPDF
JUnit 5 / Mockito / AssertJ / Testcontainers
Spotless (Google Java Format)

Angular 22.x
TypeScript strict
Standalone / Signals / Services
Reactive Forms / HttpClient / Interceptors / Guards
Angular Material / Material Icons
Apache ECharts
ESLint / Prettier / npm

PostgreSQL 18
UUID / NUMERIC(19,2) / TIMESTAMPTZ
America/Sao_Paulo / BRL

Docker + Docker Compose
Pacote: br.com.financialcontrol
API: /api/v1
```


# 31. Regra final

Se a IA quiser alterar qualquer item desta stack:

1. explicar o motivo;
2. apresentar alternativa;
3. avaliar impacto;
4. atualizar `AGENTS.md`, este documento e o README;
5. somente então implementar.
