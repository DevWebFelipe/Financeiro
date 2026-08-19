import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { IncomesService } from './incomes.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const INCOME_ID = '01900000-0000-7000-8000-000000000020';
const MOVEMENT_ID = '01900000-0000-7000-8000-000000000021';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';

function incomeBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: INCOME_ID,
    categoryId: '01900000-0000-7000-8000-000000000002',
    accountId: null,
    description: 'Salário',
    amount: 5400,
    expectedDate: '2026-08-05',
    receivedDate: null,
    status: 'EXPECTED',
    responsibleType: null,
    responsibleName: null,
    notes: null,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function pageBody(items: Record<string, unknown>[] = [incomeBody()]): Record<string, unknown> {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: 1,
  };
}

function movementBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: MOVEMENT_ID,
    incomeId: INCOME_ID,
    type: 'RECEIPT',
    status: 'ACTIVE',
    amount: 5400,
    movementDate: '2026-08-05',
    accountId: ACCOUNT_ID,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    reversedAt: null,
    ...overrides,
  };
}

describe('IncomesService', () => {
  let service: IncomesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(IncomesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /incomes with default pagination', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/incomes') &&
        req.params.get('page') === '0' &&
        req.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    request.flush(pageBody());

    const page = await pending;
    expect(page.items).toHaveLength(1);
    expect(page.items[0]?.description).toBe('Salário');
  });

  it('requests GET /incomes with official filters', async () => {
    const pending = firstValueFrom(
      service.list({
        status: 'EXPECTED',
        categoryId: '01900000-0000-7000-8000-000000000002',
        accountId: ACCOUNT_ID,
        startDate: '2026-08-01',
        endDate: '2026-08-31',
        page: 1,
        size: 10,
      }),
    );
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/incomes') &&
        req.params.get('status') === 'EXPECTED' &&
        req.params.get('categoryId') === '01900000-0000-7000-8000-000000000002' &&
        req.params.get('accountId') === ACCOUNT_ID &&
        req.params.get('startDate') === '2026-08-01' &&
        req.params.get('endDate') === '2026-08-31' &&
        req.params.get('page') === '1' &&
        req.params.get('size') === '10',
    );
    expect(request.request.method).toBe('GET');
    request.flush(pageBody([]));
    await expect(pending).resolves.toMatchObject({ items: [], page: 0 });
  });

  it('gets income detail with GET /incomes/{id}', async () => {
    const pending = firstValueFrom(service.get(INCOME_ID));
    const request = httpTesting.expectOne(api(`/incomes/${INCOME_ID}`));
    expect(request.request.method).toBe('GET');
    request.flush(incomeBody());
    await expect(pending).resolves.toMatchObject({ id: INCOME_ID, status: 'EXPECTED' });
  });

  it('creates income with POST /incomes', async () => {
    const body = {
      categoryId: '01900000-0000-7000-8000-000000000002',
      description: 'Salário',
      amount: 5400,
      expectedDate: '2026-08-05',
    };
    const pending = firstValueFrom(service.create(body));
    const request = httpTesting.expectOne(api('/incomes'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush(incomeBody(), { status: 201, statusText: 'Created' });
    await expect(pending).resolves.toMatchObject({ description: 'Salário' });
  });

  it('updates income with PUT /incomes/{id}', async () => {
    const body = {
      categoryId: '01900000-0000-7000-8000-000000000002',
      description: 'Salário atualizado',
      amount: 5500,
      expectedDate: '2026-08-05',
    };
    const pending = firstValueFrom(service.update(INCOME_ID, body));
    const request = httpTesting.expectOne(api(`/incomes/${INCOME_ID}`));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(body);
    request.flush(incomeBody({ description: 'Salário atualizado', amount: 5500 }));
    await expect(pending).resolves.toMatchObject({ description: 'Salário atualizado' });
  });

  it('cancels income with POST /incomes/{id}/cancel', async () => {
    const pending = firstValueFrom(service.cancel(INCOME_ID));
    const request = httpTesting.expectOne(api(`/incomes/${INCOME_ID}/cancel`));
    expect(request.request.method).toBe('POST');
    request.flush(incomeBody({ status: 'CANCELLED' }));
    await expect(pending).resolves.toMatchObject({ status: 'CANCELLED' });
  });

  it('creates a receipt with POST /incomes/{id}/receipts', async () => {
    const receipt = { amount: 5400, date: '2026-08-05', accountId: ACCOUNT_ID };
    const pending = firstValueFrom(service.createReceipt(INCOME_ID, receipt));
    const request = httpTesting.expectOne(api(`/incomes/${INCOME_ID}/receipts`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(receipt);
    request.flush(movementBody(), { status: 201, statusText: 'Created' });
    await expect(pending).resolves.toMatchObject({ type: 'RECEIPT', status: 'ACTIVE' });
  });

  it('lists movements with GET /incomes/{id}/movements', async () => {
    const pending = firstValueFrom(service.listMovements(INCOME_ID));
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api(`/incomes/${INCOME_ID}/movements`) &&
        req.params.get('page') === '0' &&
        req.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    request.flush({
      items: [movementBody()],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });
    const page = await pending;
    expect(page.items).toHaveLength(1);
    expect(page.items[0]?.type).toBe('RECEIPT');
  });

  it('reverses a movement with POST /incomes/{id}/movements/{movementId}/reverse', async () => {
    const pending = firstValueFrom(service.reverseMovement(INCOME_ID, MOVEMENT_ID));
    const request = httpTesting.expectOne(
      api(`/incomes/${INCOME_ID}/movements/${MOVEMENT_ID}/reverse`),
    );
    expect(request.request.method).toBe('POST');
    request.flush(movementBody({ status: 'REVERSED', reversedAt: '2026-08-15T12:00:00Z' }));
    await expect(pending).resolves.toMatchObject({ status: 'REVERSED' });
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/incomes') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush(
        {
          timestamp: '2026-08-19T15:00:00Z',
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Erro interno.',
          path: '/api/v1/incomes',
        },
        { status: 500, statusText: 'Server Error' },
      );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('rejects a page response that does not match the incomes contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/incomes') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush({ items: [{ id: INCOME_ID }] });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
