PATH: d:\Financeiro\README.md

NEW:
Fase 13 — Parcelamento / negociação / renegociação de fatura — CONCLUÍDA E APROVADA
Fase 14 — Transferências, Acerto de Saldos e Saldo Inicial — CONTRATO CONSOLIDADO / IMPLEMENTAÇÃO PENDENTE
```

Estado atual do backend (Fases 1–9 + 13): Spring Boot **4.1.0**, Java **25**, Maven Wrapper, PostgreSQL **18**, Flyway, Spring Security, JWT Access Token HS256, Argon2id, Jakarta Bean Validation, Testcontainers, OpenAPI/Swagger, fluxo Controller → Service → Repository, domínio de contas, categorias, receitas, despesas, parcelamento (Fase 8), cartões/faturas (Fase 9) e Agreements (Fase 13).

Fase 13 — **CONCLUÍDA E APROVADA** (`docs/24` §19.4 / RN254). **Fase 14** — Transferências, Acerto de Saldos e Saldo Inicial: contrato oficial em `docs/24` §19.5 / `docs/25` / `docs/28` — status **`CONTRATO CONSOLIDADO / IMPLEMENTAÇÃO PENDENTE`**. **Não implementar** a Fase 14 sem autorização explícita. Não implementar Refresh Token, `payments.type`, relatórios/PDF, frontend financeiro, auditoria genérica, extrato `/statement` nem `POST /invoices/{id}/close` sem autorização.

Não implementar Refresh Token, logout, OAuth, MFA, roles, rate limiting, frontend financeiro, relatórios/PDF, auditoria genérica nem `payments.type` sem autorização. A edição de parcela já em fatura (§269.2.7) permanece **deferida**. Fechados: §269.3, §269.4, **§269.5** (Fase 13 — `CONCLUÍDA E APROVADA`).