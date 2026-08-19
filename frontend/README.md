# FinancialControl

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.3.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Styles

Visual tokens, Inter (local), reset and base styles live in `src/styles/` and are loaded from `src/styles.css`. Use the CSS custom properties for systemic decisions. Do not add one-off token variables for every CSS value.

## API

The API base URL is centralized in `src/app/core/config/`. Development uses `http://localhost:8080/api/v1`. Production uses `/api/v1`. HTTP errors are normalized to `ApiError` in `src/app/core/errors/`.

Authentication lives in `src/app/core/auth/`. The JWT is stored only in `sessionStorage`. Login is `/login`; registration is `/register` and does not auto-login.

The authenticated shell lives in `src/app/layout/`. After login, the app opens `/dashboard` inside MainLayout (header, collapsible desktop sidebar, mobile navigation drawer). The Dashboard page consumes `GET /api/v1/dashboard` and does not recalculate balances. Accounts (`/accounts`) consume `GET /api/v1/accounts` plus official `GET /api/v1/accounts/{id}/balance`; the client does not recompute balances. Credit cards (`/credit-cards`) consume `GET /api/v1/credit-cards` with optional `holderName`, plus official `GET /api/v1/credit-cards/{id}/limit` and `GET /api/v1/credit-cards/{id}/credits`; manual credits use `POST /api/v1/credit-cards/{id}/credits` (`amount`, `reason`); the client does not recompute limits, does not apply credits to invoices, and does not load invoices in this feature. Invoices (`/invoices`) consume `GET /api/v1/credit-cards/{cardId}/invoices` plus official `GET /api/v1/invoices/{id}`, `GET /api/v1/invoices/{id}/items`, `GET /api/v1/invoices/{id}/payments`, `POST /api/v1/invoices/{id}/payments`, `POST /api/v1/invoices/{invoiceId}/payments/{paymentId}/reverse`, `GET /api/v1/invoices/{id}/adjustments`, `POST /api/v1/invoices/{id}/adjustments`, `POST /api/v1/invoices/{invoiceId}/adjustments/{adjustmentId}/reverse`, `GET /api/v1/invoices/{id}/agreements`, `POST /api/v1/invoices/{id}/agreements`, `POST /api/v1/invoices/{id}/renegotiations`, `GET /api/v1/agreements/{id}`, and `POST /api/v1/agreements/{agreementId}/installments/{installmentId}/anticipate`; the client does not recompute totals, remaining, financedAmount or additional cost and does not call credit endpoints. C7 (agreements / renegotiation on `/invoices`) is concluded / approved with reservations. Categories (`/categories`) consume `GET /api/v1/categories` with optional official filters `type` and `active`. Expenses (`/expenses`) consume paginated `GET /api/v1/expenses` with official filters and financial actions (pay, cancel, refund) per backend contract; creation supports `ACCOUNT`, `NONE`, and `CREDIT_CARD` (`creditCardId` of an active card). Incomes (`/incomes`) consume paginated `GET /api/v1/incomes` with official filters (`startDate`/`endDate` on `expectedDate`, `status`, `categoryId`, `accountId`); receipt uses `POST /incomes/{id}/receipts`. Payables (`/payables`) consume paginated `GET /api/v1/payables` as a derived read-only view (installments and invoices with remaining greater than zero); the client does not recompute remaining or overdue. Transfers (`/transfers`) consume `GET /api/v1/transfers` as an array with official `startDate`/`endDate`/`accountId`, create via `POST /transfers`, and reverse via `POST /transfers/{id}/reverse`. Goals (`/goals`) consume paginated `GET /api/v1/financial-goals` plus official contribution, redemption, complete and cancel endpoints; `currentAmount` and `progressPercent` are displayed as returned. Projections (`/projections`) consume read-only `GET /api/v1/projections` with official period params and event pagination; summary, months, quarters, `negative` and `overdue` are official. Reports (`/reports`) is a single page with a report-type selector; each type uses its own `GET /api/v1/reports/*` JSON contract and official PDF download (`GET .../pdf`). B12 (transfers, goals, projections, reports) is concluded / approved with reservations. Shared UI lives in `src/app/shared/` (`ErrorState`, `EmptyState`, and presentation pipes for BRL and domain dates). Do not put feature domain, HTTP, or Auth there.

The frontend architecture remains `core` / `shared` / `layout` / `features`, with feature HTTP flow `Page → FeatureService → HttpClient → interceptors → API`. The backend is authoritative for financial rules and values. Frontend formatters are presentation-only, and response parsers must not silently repair invalid API payloads.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

Tests that display official financial values should compare the UI with API responses instead of duplicating backend formulas.

## Running end-to-end tests

E2E uses Playwright (Chromium only). The suite lives in `e2e/` and talks to the real backend — not mocks.

Prerequisites: PostgreSQL (Docker Compose) and the Spring Boot API must be up (`GET http://localhost:8080/api/v1/health` → `{ "status": "UP" }`).

From the repository root:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run-e2e.ps1
```

From `frontend/`, with the stack already running:

```bash
npx ng e2e
# or
npx playwright test
```

HTML report: `frontend/playwright-report/`. Firefox/WebKit are not configured. Playwright remains the official E2E tool and Chromium remains required. CI/CD remains outside Phase 23 scope.

Phase 22 status: **CONCLUÍDA / APROVADA COM RESSALVAS**. Reservations **AUD-F22-A1** to **AUD-F22-A4** are non-blocking and were not corrected in the documentation wrap-up.

## Phase 23 quality status

**F23 — DECISÕES APROVADAS / CONSOLIDAÇÃO DOCUMENTAL**.

Phase 23 will audit architecture, services, contracts, parsers, formatters, forms, routes, UI states, authentication/session, basic security, dependencies, unit tests, Playwright E2E, responsiveness, accessibility, performance, scripts, and documentation. It does not add features, financial rules, endpoints, migrations, dependencies, CI/CD, redesign, or C8.

This status records approved decisions only. No Phase 23 audit, correction, test run, or approval is claimed. The next step is **F23 — AUDITORIA INICIAL / MAPA TÉCNICO**. Full contract: `../docs/28-roadmap.md` §112; architecture: `../docs/21-arquitetura-do-sistema.md` §185; tests: `../docs/27-testes.md` §178.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
