import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { routes } from './app.routes';
import { AUTH_TOKEN_KEY } from './core/auth/auth-token.store';
import { AuthService } from './core/auth/auth.service';
import { joinApiUrl } from './core/config/api-config';
import { environment } from './core/config/environment';
import { provideCoreHttp } from './core/http/provide-core-http';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const user = {
  id: '01900000-0000-7000-8000-000000000001',
  name: 'Ada Lovelace',
  email: 'ada@example.com',
  active: true,
  createdAt: '2026-08-13T12:00:00Z',
  updatedAt: '2026-08-13T12:00:00Z',
};

describe('app routes', () => {
  let router: Router;
  let auth: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideCoreHttp(), provideHttpClientTesting()],
    });
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('keeps login and register public', async () => {
    await auth.initialize();
    await router.navigateByUrl('/login');
    expect(router.url).toBe('/login');

    await router.navigateByUrl('/register');
    expect(router.url).toBe('/register');
  });

  it('protects /dashboard and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/dashboard');
    expect(router.url).toBe('/login?returnUrl=%2Fdashboard');
  });

  it('protects /accounts and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/accounts');
    expect(router.url).toBe('/login?returnUrl=%2Faccounts');
  });

  it('protects /credit-cards and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/credit-cards');
    expect(router.url).toBe('/login?returnUrl=%2Fcredit-cards');
  });

  it('protects /invoices and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/invoices');
    expect(router.url).toBe('/login?returnUrl=%2Finvoices');
  });

  it('protects /categories and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/categories');
    expect(router.url).toBe('/login?returnUrl=%2Fcategories');
  });

  it('protects /expenses and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/expenses');
    expect(router.url).toBe('/login?returnUrl=%2Fexpenses');
  });

  it('protects /incomes and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/incomes');
    expect(router.url).toBe('/login?returnUrl=%2Fincomes');
  });

  it('protects /payables and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/payables');
    expect(router.url).toBe('/login?returnUrl=%2Fpayables');
  });

  it('protects /transfers and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/transfers');
    expect(router.url).toBe('/login?returnUrl=%2Ftransfers');
  });

  it('protects /goals and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/goals');
    expect(router.url).toBe('/login?returnUrl=%2Fgoals');
  });

  it('protects /projections and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/projections');
    expect(router.url).toBe('/login?returnUrl=%2Fprojections');
  });

  it('protects /reports and sends unauthenticated users to login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/reports');
    expect(router.url).toBe('/login?returnUrl=%2Freports');
  });

  it('uses /dashboard as the authenticated entry from /', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/');
    expect(router.url).toBe('/dashboard');
    expect(TestBed.inject(Title).getTitle()).toBe('Dashboard — Financeiro');
  });

  it('allows an authenticated user to open /dashboard', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/dashboard');
    expect(router.url).toBe('/dashboard');
  });

  it('allows an authenticated user to open lazy-loaded /accounts', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/accounts');
    expect(router.url).toBe('/accounts');
    expect(TestBed.inject(Title).getTitle()).toBe('Contas — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /credit-cards', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/credit-cards');
    expect(router.url).toBe('/credit-cards');
    expect(TestBed.inject(Title).getTitle()).toBe('Cartões — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /invoices', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/invoices');
    expect(router.url).toBe('/invoices');
    expect(TestBed.inject(Title).getTitle()).toBe('Faturas — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /categories', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/categories');
    expect(router.url).toBe('/categories');
    expect(TestBed.inject(Title).getTitle()).toBe('Categorias — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /expenses', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/expenses');
    expect(router.url).toBe('/expenses');
    expect(TestBed.inject(Title).getTitle()).toBe('Despesas — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /incomes', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/incomes');
    expect(router.url).toBe('/incomes');
    expect(TestBed.inject(Title).getTitle()).toBe('Receitas — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /payables', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/payables');
    expect(router.url).toBe('/payables');
    expect(TestBed.inject(Title).getTitle()).toBe('Contas a pagar — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /transfers', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/transfers');
    expect(router.url).toBe('/transfers');
    expect(TestBed.inject(Title).getTitle()).toBe('Transferências — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /goals', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/goals');
    expect(router.url).toBe('/goals');
    expect(TestBed.inject(Title).getTitle()).toBe('Metas — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /projections', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/projections');
    expect(router.url).toBe('/projections');
    expect(TestBed.inject(Title).getTitle()).toBe('Projeções — Financeiro');
  });

  it('allows an authenticated user to open lazy-loaded /reports', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/reports');
    expect(router.url).toBe('/reports');
    expect(TestBed.inject(Title).getTitle()).toBe('Relatórios — Financeiro');
  });

  it('redirects authenticated guests away from login to the dashboard', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await router.navigateByUrl('/login');
    expect(router.url).toBe('/dashboard');
  });
});
