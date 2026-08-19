import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { DashboardResponse } from './dashboard.models';
import { DashboardService } from './dashboard.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

function validBody(): Record<string, unknown> {
  return {
    asOfDate: '2026-08-17',
    startDate: '2026-08-17',
    endDate: '2027-07-31',
    balance: {
      totalBalance: 10000,
      reservedAmount: 200,
      availableBalance: 9800,
    },
    projection: {
      summary: {
        currentBalance: 10000,
        projectedFinalBalance: 12000,
        projectedIncome: 4000,
        projectedExpense: 2000,
        projectedNetCashFlow: 2000,
        minimumProjectedBalance: 9800,
        minimumProjectedBalanceDate: '2026-08-17',
        reservedAmount: 200,
        availableProjectedBalance: 11800,
      },
      months: [
        {
          period: '2026-08',
          openingBalance: 10000,
          totalIncome: 400,
          totalExpense: 100,
          netCashFlow: 300,
          closingBalance: 10300,
          minimumProjectedBalance: 10000,
          minimumProjectedBalanceDate: '2026-08-17',
          negative: false,
          reservedAmount: 200,
          availableProjectedBalance: 10100,
        },
      ],
      quarters: [
        {
          period: '2026-Q4',
          months: ['2026-10', '2026-11', '2026-12'],
          totalIncome: 15,
          totalExpense: 3,
          netCashFlow: 12,
          openingBalance: 0,
          closingBalance: 12,
        },
      ],
    },
    payables: {
      totalRemaining: 290,
      installmentRemaining: 260,
      invoiceRemaining: 30,
      overdueRemaining: 30,
      overdueInstallmentRemaining: 30,
      overdueInvoiceRemaining: 0,
      openCount: 2,
      overdueCount: 1,
    },
    receivables: {
      futureAmount: 400,
      overdueAmount: 0,
      totalReceivableAmount: 400,
      receivedAmount: 0,
    },
    accounts: [
      {
        id: '01900000-0000-7000-8000-000000000001',
        name: 'Nubank',
        type: 'BANK_ACCOUNT',
        totalBalance: 10000,
        reservedAmount: 200,
        availableBalance: 9800,
      },
    ],
    creditCards: [
      {
        id: '01900000-0000-7000-8000-000000000002',
        name: 'Cartão',
        creditLimit: 2000,
        usedLimit: 180,
        availableLimit: 1820,
        invoiceRemaining: 180,
        overdueInvoiceRemaining: 0,
      },
    ],
  };
}

describe('DashboardService', () => {
  let service: DashboardService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /dashboard without query params and returns the official envelope', async () => {
    const pending = firstValueFrom(service.getDashboard());
    const request = httpTesting.expectOne(api('/dashboard'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush(validBody());

    const dashboard: DashboardResponse = await pending;
    expect(dashboard.asOfDate).toBe('2026-08-17');
    expect(dashboard.balance.availableBalance).toBe(9800);
    expect(dashboard.projection.months).toHaveLength(1);
    expect(dashboard.payables.overdueCount).toBe(1);
    expect(dashboard.receivables.totalReceivableAmount).toBe(400);
    expect(dashboard.accounts[0]?.name).toBe('Nubank');
    expect(dashboard.creditCards[0]?.usedLimit).toBe(180);
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.getDashboard()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/dashboard')).flush(
      {
        timestamp: '2026-08-17T15:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: '/api/v1/dashboard',
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

  it('rejects a response that does not match the dashboard contract', async () => {
    const pending = firstValueFrom(service.getDashboard()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/dashboard')).flush({ asOfDate: '2026-08-17' });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
