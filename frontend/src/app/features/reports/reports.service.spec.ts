import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { parseContentDispositionFilename, ReportsService } from './reports.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);
const EXPENSE_ID = '01900000-0000-7000-8000-000000000010';
const CATEGORY_ID = '01900000-0000-7000-8000-000000000002';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const INVOICE_ID = '01900000-0000-7000-8000-000000000050';

const period = { startDate: '2026-08-01', endDate: '2026-08-31' };

function expenseSummary(): Record<string, unknown> {
  return {
    periodOriginal: 1500,
    periodDiscount: 0,
    periodSurcharge: 0,
    periodObligation: 1500,
    periodPaid: 500,
    periodRemaining: 1000,
  };
}

function expenseReportBody(): Record<string, unknown> {
  return {
    period,
    items: [
      {
        id: EXPENSE_ID,
        description: 'Aluguel',
        expenseDate: '2026-08-01',
        paymentMethod: 'ACCOUNT',
        status: 'PARTIALLY_PAID',
        categoryId: CATEGORY_ID,
        accountId: ACCOUNT_ID,
        creditCardId: null,
        responsibleType: 'MINE',
        responsibleName: null,
        origin: 'PURCHASE',
        ...expenseSummary(),
        installments: [],
      },
    ],
    page: 0,
    size: 20,
    totalItems: 1,
    totalPages: 1,
    summary: expenseSummary(),
  };
}

describe('ReportsService', () => {
  let service: ReportsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('requests GET /reports/expenses with default pagination and no extra params', async () => {
    const pending = firstValueFrom(service.listExpenses({ page: 0, size: 20 }));
    const request = httpTesting.expectOne((req) => req.url === api('/reports/expenses'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['page', 'size']);
    request.flush(expenseReportBody());
    await expect(pending).resolves.toMatchObject({ totalItems: 1 });
  });

  it('requests GET /reports/expenses with official filters', async () => {
    const pending = firstValueFrom(
      service.listExpenses({
        startDate: '2026-08-01',
        endDate: '2026-08-31',
        status: 'OPEN',
        categoryId: CATEGORY_ID,
        accountId: ACCOUNT_ID,
        creditCardId: '01900000-0000-7000-8000-000000000040',
        responsibleType: 'OTHER',
        responsibleName: 'João',
        paymentMethod: 'ACCOUNT',
        sort: 'periodObligation',
        direction: 'desc',
        page: 1,
        size: 10,
      }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/reports/expenses'));
    expect(request.request.params.keys().sort()).toEqual([
      'accountId',
      'categoryId',
      'creditCardId',
      'direction',
      'endDate',
      'page',
      'paymentMethod',
      'responsibleName',
      'responsibleType',
      'size',
      'sort',
      'startDate',
      'status',
    ]);
    request.flush({ ...expenseReportBody(), items: [], totalItems: 0, totalPages: 0 });
    await pending;
  });

  it('requests GET /reports/incomes with required dateType', async () => {
    const pending = firstValueFrom(
      service.listIncomes({ dateType: 'EXPECTED', page: 0, size: 20 }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/reports/incomes'));
    expect(request.request.params.get('dateType')).toBe('EXPECTED');
    expect(request.request.params.keys().sort()).toEqual(['dateType', 'page', 'size']);
    request.flush({
      period,
      dateType: 'EXPECTED',
      items: [],
      page: 0,
      size: 20,
      totalItems: 0,
      totalPages: 0,
      summary: { amount: 0, accruedAmount: 0, receivedAmount: 0, remainingAmount: 0 },
    });
    await pending;
  });

  it('does not send dateType on responsibles when nature is EXPENSE', async () => {
    const pending = firstValueFrom(
      service.listResponsibles({ nature: 'EXPENSE', page: 0, size: 20 }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/reports/responsibles'));
    expect(request.request.params.get('nature')).toBe('EXPENSE');
    expect(request.request.params.get('dateType')).toBeNull();
    expect(request.request.params.keys().sort()).toEqual(['nature', 'page', 'size']);
    request.flush({
      period,
      nature: 'EXPENSE',
      items: [],
      page: 0,
      size: 20,
      totalItems: 0,
      totalPages: 0,
      summary: { expense: expenseSummary() },
    });
    await pending;
  });

  it('sends dateType on responsibles when nature is BOTH', async () => {
    const pending = firstValueFrom(
      service.listResponsibles({ nature: 'BOTH', dateType: 'RECEIVED', page: 0, size: 20 }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/reports/responsibles'));
    expect(request.request.params.get('dateType')).toBe('RECEIVED');
    request.flush({
      period,
      nature: 'BOTH',
      dateType: 'RECEIVED',
      items: [],
      page: 0,
      size: 20,
      totalItems: 0,
      totalPages: 0,
      summary: {},
    });
    await pending;
  });

  it('requests GET /reports/categories with required dateType', async () => {
    const pending = firstValueFrom(
      service.listCategories({ dateType: 'RECEIVED', page: 0, size: 20 }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/reports/categories'));
    expect(request.request.params.keys().sort()).toEqual(['dateType', 'page', 'size']);
    request.flush({
      period,
      dateType: 'RECEIVED',
      items: [],
      page: 0,
      size: 20,
      totalItems: 0,
      totalPages: 0,
      summary: {
        expense: expenseSummary(),
        income: { periodReceivedAmount: 0 },
      },
    });
    await pending;
  });

  it('requests GET /reports/cards without unknown params', async () => {
    const pending = firstValueFrom(service.listCards({ page: 0, size: 20 }));
    const request = httpTesting.expectOne((req) => req.url === api('/reports/cards'));
    expect(request.request.params.keys().sort()).toEqual(['page', 'size']);
    request.flush({
      period,
      items: [],
      page: 0,
      size: 20,
      totalItems: 0,
      totalPages: 0,
      summary: { purchaseAmount: 0, invoiceAmount: 0, paidAmount: 0, creditAmount: 0 },
    });
    await pending;
  });

  it('requests GET /reports/cash-flow with pagination for historical items', async () => {
    const pending = firstValueFrom(service.listCashFlow({ flowType: 'BOTH', page: 2, size: 10 }));
    const request = httpTesting.expectOne((req) => req.url === api('/reports/cash-flow'));
    expect(request.request.params.keys().sort()).toEqual(['flowType', 'page', 'size']);
    request.flush({
      period,
      flowType: 'BOTH',
      accountId: null,
      historical: {
        items: [],
        page: 2,
        size: 10,
        totalItems: 0,
        totalPages: 0,
        summary: { totalIn: 0, totalOut: 0, net: 0 },
      },
      projected: { empty: true },
    });
    await pending;
  });

  it('requests GET /reports/invoices/{id} without page or size', async () => {
    const pending = firstValueFrom(service.getInvoice(INVOICE_ID, { responsibleType: 'MINE' }));
    const request = httpTesting.expectOne(
      (req) => req.url === api(`/reports/invoices/${INVOICE_ID}`),
    );
    expect(request.request.params.keys().sort()).toEqual(['responsibleType']);
    expect(request.request.params.get('page')).toBeNull();
    expect(request.request.params.get('size')).toBeNull();
    request.flush({
      invoiceId: INVOICE_ID,
      card: { name: 'Nubank', holderName: 'Ederson', lastFourDigits: null },
      invoice: {
        referenceYear: 2026,
        referenceMonth: 8,
        closingDate: '2026-08-10',
        dueDate: '2026-08-20',
        status: 'OPEN',
        totalAmount: 0,
        paidAmount: 0,
        remainingAmount: 0,
      },
      purchases: [],
      byCategory: [],
      byResponsible: [],
      installmentAdjustments: [],
      invoiceAdjustments: [],
      credits: [],
      payments: [],
      allocations: [],
    });
    await pending;
  });

  it('downloads expenses PDF without page or size and uses Content-Disposition filename', async () => {
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:report');
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi.fn();
    const originalCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
      const element = originalCreate(tag);
      if (tag === 'a') {
        Object.defineProperty(element, 'click', { value: click });
      }
      return element;
    });

    const pending = firstValueFrom(
      service.downloadExpensesPdf({
        status: 'OPEN',
        page: 3,
        size: 20,
      }),
    );
    const request = httpTesting.expectOne((req) => req.url === api('/reports/expenses/pdf'));
    expect(request.request.responseType).toBe('blob');
    expect(request.request.params.get('status')).toBe('OPEN');
    expect(request.request.params.get('page')).toBeNull();
    expect(request.request.params.get('size')).toBeNull();
    request.flush(new Blob(['%PDF'], { type: 'application/pdf' }), {
      headers: {
        'Content-Disposition':
          'attachment; filename="relatorio-despesas-2026-08-01_2026-08-31.pdf"',
      },
    });
    await pending;
    expect(createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalled();
  });

  it('downloads incomes, categories, responsibles, cards and cash-flow PDFs without page or size', async () => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:x');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);

    const cases: Array<{
      call: () => ReturnType<ReportsService[keyof ReportsService]>;
      path: string;
    }> = [
      {
        call: () => service.downloadIncomesPdf({ dateType: 'EXPECTED', page: 1, size: 20 }),
        path: '/reports/incomes/pdf',
      },
      {
        call: () => service.downloadCategoriesPdf({ dateType: 'RECEIVED', page: 1, size: 20 }),
        path: '/reports/categories/pdf',
      },
      {
        call: () => service.downloadResponsiblesPdf({ nature: 'EXPENSE', page: 1, size: 20 }),
        path: '/reports/responsibles/pdf',
      },
      {
        call: () => service.downloadCardsPdf({ page: 1, size: 20 }),
        path: '/reports/cards/pdf',
      },
      {
        call: () => service.downloadCashFlowPdf({ flowType: 'HISTORICAL', page: 1, size: 20 }),
        path: '/reports/cash-flow/pdf',
      },
    ];

    for (const item of cases) {
      const pending = firstValueFrom(item.call() as ReturnType<ReportsService['downloadCardsPdf']>);
      const request = httpTesting.expectOne((req) => req.url === api(item.path));
      expect(request.request.params.get('page')).toBeNull();
      expect(request.request.params.get('size')).toBeNull();
      request.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
      await pending;
    }
  });

  it('downloads invoice PDF without page or size', async () => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:x');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const pending = firstValueFrom(
      service.downloadInvoicePdf(INVOICE_ID, { responsibleName: 'João' }),
    );
    const request = httpTesting.expectOne(
      (req) => req.url === api(`/reports/invoices/${INVOICE_ID}/pdf`),
    );
    expect(request.request.params.keys().sort()).toEqual(['responsibleName']);
    expect(request.request.params.get('page')).toBeNull();
    request.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
    await pending;
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.listExpenses({ page: 0, size: 20 })).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );
    httpTesting
      .expectOne((req) => req.url === api('/reports/expenses'))
      .flush(
        {
          timestamp: '2026-08-19T15:00:00Z',
          status: 400,
          code: 'VALIDATION_ERROR',
          message: 'Dados inválidos.',
          path: '/api/v1/reports/expenses',
        },
        { status: 400, statusText: 'Bad Request' },
      );
    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('parses Content-Disposition filenames', () => {
    expect(
      parseContentDispositionFilename(
        'attachment; filename="relatorio-despesas-2026-08-01_2026-08-31.pdf"',
        'fallback.pdf',
      ),
    ).toBe('relatorio-despesas-2026-08-01_2026-08-31.pdf');
    expect(parseContentDispositionFilename(null, 'fallback.pdf')).toBe('fallback.pdf');
  });
});
