import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { AccountsService } from './accounts.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const ACCOUNT_ID = '01900000-0000-7000-8000-000000000001';
const ACCOUNT_ID_B = '01900000-0000-7000-8000-000000000002';

function accountBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: ACCOUNT_ID,
    name: 'Nubank',
    type: 'BANK_ACCOUNT',
    initialBalance: 1500,
    active: true,
    createdAt: '2026-08-13T12:00:00Z',
    updatedAt: '2026-08-13T12:00:00Z',
    ...overrides,
  };
}

function balanceBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    accountId: ACCOUNT_ID,
    totalBalance: 10000,
    reservedAmount: 200,
    availableBalance: 9800,
    balance: 10000,
    ...overrides,
  };
}

describe('AccountsService', () => {
  let service: AccountsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AccountsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /accounts without query params and returns the official array', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(api('/accounts'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([accountBody()]);

    const accounts = await pending;
    expect(accounts).toHaveLength(1);
    expect(accounts[0]?.name).toBe('Nubank');
    expect(accounts[0]?.type).toBe('BANK_ACCOUNT');
    expect(accounts[0]?.active).toBe(true);
    expect(accounts[0]?.initialBalance).toBe(1500);
  });

  it('keeps an unknown account type as presentation data', async () => {
    const pending = firstValueFrom(service.list());
    httpTesting.expectOne(api('/accounts')).flush([accountBody({ type: 'WALLET' })]);
    const accounts = await pending;
    expect(accounts[0]?.type).toBe('WALLET');
  });

  it('requests GET /accounts/{id}/balance and uses official totals without the legacy alias', async () => {
    const pending = firstValueFrom(service.getBalance(ACCOUNT_ID));
    const request = httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID}/balance`));
    expect(request.request.method).toBe('GET');
    request.flush(balanceBody());

    const balance = await pending;
    expect(balance.accountId).toBe(ACCOUNT_ID);
    expect(balance.totalBalance).toBe(10000);
    expect(balance.reservedAmount).toBe(200);
    expect(balance.availableBalance).toBe(9800);
    expect(balance).not.toHaveProperty('balance');
  });

  it('loads balances in parallel after GET /accounts', async () => {
    const pending = firstValueFrom(service.listWithBalances());
    httpTesting
      .expectOne(api('/accounts'))
      .flush([
        accountBody(),
        accountBody({ id: ACCOUNT_ID_B, name: 'Carteira', type: 'CASH', initialBalance: 0 }),
      ]);

    const firstBalance = httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID}/balance`));
    const secondBalance = httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID_B}/balance`));
    expect(firstBalance.request.method).toBe('GET');
    expect(secondBalance.request.method).toBe('GET');
    firstBalance.flush(balanceBody());
    secondBalance.flush(
      balanceBody({
        accountId: ACCOUNT_ID_B,
        totalBalance: 80,
        reservedAmount: 0,
        availableBalance: 80,
        balance: 80,
      }),
    );

    const items = await pending;
    expect(items).toHaveLength(2);
    expect(items[0]?.account.name).toBe('Nubank');
    expect(items[0]?.balance.availableBalance).toBe(9800);
    expect(items[1]?.account.name).toBe('Carteira');
    expect(items[1]?.balance.totalBalance).toBe(80);
  });

  it('does not request balances when GET /accounts returns an empty array', async () => {
    const pending = firstValueFrom(service.listWithBalances());
    httpTesting.expectOne(api('/accounts')).flush([]);
    await expect(pending).resolves.toEqual([]);
  });

  it('fails the combined load when any balance request fails', async () => {
    const pending = firstValueFrom(service.listWithBalances()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/accounts')).flush([accountBody()]);
    httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID}/balance`)).flush(
      {
        timestamp: '2026-08-18T15:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: `/api/v1/accounts/${ACCOUNT_ID}/balance`,
      },
      { status: 500, statusText: 'Server Error' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('creates an account with POST /accounts and omits optional initialBalance', async () => {
    const pending = firstValueFrom(service.create({ name: 'Nubank', type: 'BANK_ACCOUNT' }));
    const request = httpTesting.expectOne(api('/accounts'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Nubank', type: 'BANK_ACCOUNT' });
    request.flush(accountBody(), { status: 201, statusText: 'Created' });

    await expect(pending).resolves.toMatchObject({ name: 'Nubank', active: true });
  });

  it('creates an account sending initialBalance when provided', async () => {
    const pending = firstValueFrom(
      service.create({ name: 'Nubank', type: 'BANK_ACCOUNT', initialBalance: 1500 }),
    );
    const request = httpTesting.expectOne(api('/accounts'));
    expect(request.request.body).toEqual({
      name: 'Nubank',
      type: 'BANK_ACCOUNT',
      initialBalance: 1500,
    });
    request.flush(accountBody());
    await pending;
  });

  it('updates name and type with PUT /accounts/{id}', async () => {
    const pending = firstValueFrom(service.update(ACCOUNT_ID, { name: 'Nubank PJ', type: 'CASH' }));
    const request = httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID}`));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Nubank PJ', type: 'CASH' });
    request.flush(accountBody({ name: 'Nubank PJ', type: 'CASH' }));
    await expect(pending).resolves.toMatchObject({ name: 'Nubank PJ', type: 'CASH' });
  });

  it('deactivates with POST /accounts/{id}/deactivate', async () => {
    const pending = firstValueFrom(service.deactivate(ACCOUNT_ID));
    const request = httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID}/deactivate`));
    expect(request.request.method).toBe('POST');
    request.flush(accountBody({ active: false }));
    await expect(pending).resolves.toMatchObject({ active: false });
  });

  it('activates with POST /accounts/{id}/activate', async () => {
    const pending = firstValueFrom(service.activate(ACCOUNT_ID));
    const request = httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID}/activate`));
    expect(request.request.method).toBe('POST');
    request.flush(accountBody({ active: true }));
    await expect(pending).resolves.toMatchObject({ active: true });
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/accounts')).flush(
      {
        timestamp: '2026-08-18T15:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: '/api/v1/accounts',
      },
      { status: 500, statusText: 'Server Error' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
    if (isApiError(error)) {
      expect(error.status).toBe(500);
      expect(error.code).toBe('INTERNAL_ERROR');
    }
  });

  it('propagates validation errors from POST /accounts', async () => {
    const pending = firstValueFrom(service.create({ name: '', type: 'BANK_ACCOUNT' })).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/accounts')).flush(
      {
        timestamp: '2026-08-18T15:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Dados inválidos.',
        path: '/api/v1/accounts',
        fields: { name: 'O nome é obrigatório.' },
      },
      { status: 400, statusText: 'Bad Request' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
    if (isApiError(error)) {
      expect(error.code).toBe('VALIDATION_ERROR');
      expect(error.fields?.['name']).toBe('O nome é obrigatório.');
    }
  });

  it('rejects a list response that does not match the accounts contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/accounts')).flush({ id: ACCOUNT_ID });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });

  it('rejects a balance response that omits official totalBalance', async () => {
    const pending = firstValueFrom(service.getBalance(ACCOUNT_ID)).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api(`/accounts/${ACCOUNT_ID}/balance`)).flush({
      accountId: ACCOUNT_ID,
      reservedAmount: 0,
      availableBalance: 10,
      balance: 10,
    });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
