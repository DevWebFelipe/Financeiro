import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { ProjectionsService } from './projections.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const SOURCE_ID = '01900000-0000-7000-8000-000000000050';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';

function eventBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    date: '2026-08-20',
    type: 'EXPENSE',
    description: 'Aluguel',
    amount: 1500,
    direction: 'OUT',
    sourceId: SOURCE_ID,
    sourceType: 'EXPENSE',
    overdue: false,
    accountAssignment: 'UNASSIGNED',
    ...overrides,
  };
}

function responseBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    startDate: '2026-08-17',
    endDate: '2026-08-31',
    summary: {
      currentBalance: 1000,
      projectedFinalBalance: -3000,
      projectedIncome: 1000,
      projectedExpense: 5000,
      projectedNetCashFlow: -4000,
      minimumProjectedBalance: -3000,
      minimumProjectedBalanceDate: '2026-08-25',
      reservedAmount: 200,
      availableProjectedBalance: -3200,
    },
    months: [
      {
        period: '2026-08',
        openingBalance: 1000,
        totalIncome: 1000,
        totalExpense: 5000,
        netCashFlow: -4000,
        closingBalance: -3000,
        minimumProjectedBalance: -3000,
        minimumProjectedBalanceDate: '2026-08-25',
        negative: true,
        reservedAmount: 200,
        availableProjectedBalance: -3200,
      },
    ],
    quarters: [],
    events: {
      items: [eventBody()],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    },
    undatedEvents: [],
    ...overrides,
  };
}

describe('ProjectionsService', () => {
  let service: ProjectionsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectionsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /projections with default page and size only', async () => {
    const pending = firstValueFrom(service.get());
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/projections') &&
        req.params.get('page') === '0' &&
        req.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['page', 'size']);
    request.flush(responseBody());

    const projection = await pending;
    expect(projection.summary.projectedExpense).toBe(5000);
    expect(projection.events.items).toHaveLength(1);
  });

  it('sends startDate and endDate only for a date interval', async () => {
    const pending = firstValueFrom(
      service.get({ startDate: '2026-08-01', endDate: '2026-08-31', page: 0, size: 20 }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/projections'));
    expect(request.request.params.keys().sort()).toEqual(['endDate', 'page', 'size', 'startDate']);
    expect(request.request.params.get('startDate')).toBe('2026-08-01');
    expect(request.request.params.get('endDate')).toBe('2026-08-31');
    request.flush(responseBody());
    await pending;
  });

  it('sends year and month together, with optional months count', async () => {
    const pending = firstValueFrom(
      service.get({ year: 2026, month: 8, months: 3, page: 1, size: 10 }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/projections'));
    expect(request.request.params.keys().sort()).toEqual([
      'month',
      'months',
      'page',
      'size',
      'year',
    ]);
    expect(request.request.params.get('year')).toBe('2026');
    expect(request.request.params.get('month')).toBe('8');
    expect(request.request.params.get('months')).toBe('3');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');
    request.flush(responseBody());
    await pending;
  });

  it('sends months only for a horizon from today', async () => {
    const pending = firstValueFrom(service.get({ months: 6, page: 0, size: 20 }));
    const request = httpTesting.expectOne((req) => req.url === api('/projections'));
    expect(request.request.params.keys().sort()).toEqual(['months', 'page', 'size']);
    expect(request.request.params.get('months')).toBe('6');
    request.flush(responseBody());
    await pending;
  });

  it('sends optional accountId without empty optional params', async () => {
    const pending = firstValueFrom(service.get({ accountId: ACCOUNT_ID }));
    const request = httpTesting.expectOne((req) => req.url === api('/projections'));
    expect(request.request.params.keys().sort()).toEqual(['accountId', 'page', 'size']);
    expect(request.request.params.get('accountId')).toBe(ACCOUNT_ID);
    expect(request.request.params.get('includeEvents')).toBeNull();
    expect(request.request.params.get('startDate')).toBeNull();
    expect(request.request.params.get('year')).toBeNull();
    expect(request.request.params.get('months')).toBeNull();
    request.flush(responseBody());
    await pending;
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.get()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/projections') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush(
        {
          timestamp: '2026-08-19T15:00:00Z',
          status: 400,
          code: 'VALIDATION_ERROR',
          message: 'Dados inválidos.',
          path: '/api/v1/projections',
        },
        { status: 400, statusText: 'Bad Request' },
      );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('rejects a response that does not match the projections contract', async () => {
    const pending = firstValueFrom(service.get()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/projections') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush({ startDate: '2026-08-17' });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
