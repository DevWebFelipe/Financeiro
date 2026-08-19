import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { PayablesService } from './payables.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const ITEM_ID = '01900000-0000-7000-8000-000000000030';

function itemBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: ITEM_ID,
    type: 'INSTALLMENT',
    expenseId: '01900000-0000-7000-8000-000000000010',
    creditCardId: null,
    categoryId: '01900000-0000-7000-8000-000000000002',
    accountId: '01900000-0000-7000-8000-000000000003',
    paymentMethod: 'ACCOUNT',
    name: 'Aluguel',
    purchaseDate: '2026-08-01',
    dueDate: '2026-08-10',
    originalAmount: 1500,
    paidAmount: 500,
    remainingAmount: 1000,
    status: 'PARTIALLY_PAID',
    overdue: false,
    responsibleType: 'MINE',
    responsibleName: null,
    ...overrides,
  };
}

function pageBody(items: Record<string, unknown>[] = [itemBody()]): Record<string, unknown> {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: items.length > 0 ? 1 : 0,
    totalRemaining: items.reduce((sum, item) => sum + Number(item['remainingAmount'] ?? 0), 0),
    totalOriginal: items.reduce((sum, item) => sum + Number(item['originalAmount'] ?? 0), 0),
    totalPaid: items.reduce((sum, item) => sum + Number(item['paidAmount'] ?? 0), 0),
  };
}

describe('PayablesService', () => {
  let service: PayablesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PayablesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /payables with default pagination', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/payables') &&
        req.params.get('page') === '0' &&
        req.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    request.flush(pageBody());

    const page = await pending;
    expect(page.items).toHaveLength(1);
    expect(page.items[0]?.name).toBe('Aluguel');
    expect(page.totalRemaining).toBe(1000);
  });

  it('requests GET /payables with official filters', async () => {
    const pending = firstValueFrom(
      service.list({
        status: 'OPEN',
        overdue: true,
        categoryId: '01900000-0000-7000-8000-000000000002',
        responsibleType: 'MINE',
        startDate: '2026-08-01',
        endDate: '2026-08-31',
        year: 2026,
        month: 8,
        withoutCreditCard: true,
        includeWithoutDueDate: true,
        search: 'Aluguel',
        sort: 'remainingAmount',
        direction: 'desc',
        page: 1,
        size: 10,
      }),
    );
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/payables') &&
        req.params.get('status') === 'OPEN' &&
        req.params.get('overdue') === 'true' &&
        req.params.get('categoryId') === '01900000-0000-7000-8000-000000000002' &&
        req.params.get('responsibleType') === 'MINE' &&
        req.params.get('startDate') === '2026-08-01' &&
        req.params.get('endDate') === '2026-08-31' &&
        req.params.get('year') === '2026' &&
        req.params.get('month') === '8' &&
        req.params.get('withoutCreditCard') === 'true' &&
        req.params.get('includeWithoutDueDate') === 'true' &&
        req.params.get('search') === 'Aluguel' &&
        req.params.get('sort') === 'remainingAmount' &&
        req.params.get('direction') === 'desc' &&
        req.params.get('page') === '1' &&
        req.params.get('size') === '10',
    );
    expect(request.request.method).toBe('GET');
    request.flush(pageBody([]));
    await expect(pending).resolves.toMatchObject({ items: [], totalRemaining: 0 });
  });

  it('does not send creditCardId or unknown params', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/payables') &&
        req.params.get('page') === '0' &&
        req.params.get('size') === '20',
    );
    expect(request.request.params.get('creditCardId')).toBeNull();
    expect(request.request.params.keys().sort()).toEqual(['page', 'size']);
    request.flush(pageBody());
    await pending;
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
          req.url === api('/payables') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush(
        {
          timestamp: '2026-08-19T15:00:00Z',
          status: 400,
          code: 'VALIDATION_ERROR',
          message: 'Dados inválidos.',
          path: '/api/v1/payables',
        },
        { status: 400, statusText: 'Bad Request' },
      );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('rejects a page response that does not match the payables contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/payables') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush({ items: [{ id: ITEM_ID }] });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
