import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { ExpensesService } from './expenses.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const EXPENSE_ID = '01900000-0000-7000-8000-000000000010';
const INSTALLMENT_ID = '01900000-0000-7000-8000-000000000011';

function expenseBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: EXPENSE_ID,
    categoryId: '01900000-0000-7000-8000-000000000002',
    accountId: '01900000-0000-7000-8000-000000000003',
    creditCardId: null,
    description: 'Mercado',
    totalAmount: 150.5,
    expenseDate: '2026-08-01',
    dueDate: '2026-08-10',
    paymentMethod: 'ACCOUNT',
    status: 'OPEN',
    responsibleType: 'MINE',
    responsibleName: null,
    barcode: null,
    notes: null,
    overdue: false,
    installmentId: INSTALLMENT_ID,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function pageBody(items: Record<string, unknown>[] = [expenseBody()]): Record<string, unknown> {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: 1,
  };
}

function installmentBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: INSTALLMENT_ID,
    expenseId: EXPENSE_ID,
    installmentNumber: 1,
    totalInstallments: 1,
    amount: 150.5,
    remainingAmount: 150.5,
    dueDate: '2026-08-10',
    status: 'OPEN',
    overdue: false,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

describe('ExpensesService', () => {
  let service: ExpensesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ExpensesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /expenses with default pagination', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/expenses') &&
        req.params.get('page') === '0' &&
        req.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    request.flush(pageBody());

    const page = await pending;
    expect(page.items).toHaveLength(1);
    expect(page.items[0]?.description).toBe('Mercado');
  });

  it('requests GET /expenses with official filters', async () => {
    const pending = firstValueFrom(
      service.list({
        status: 'OPEN',
        categoryId: '01900000-0000-7000-8000-000000000002',
        accountId: '01900000-0000-7000-8000-000000000003',
        paymentMethod: 'ACCOUNT',
        responsibleType: 'MINE',
        startDate: '2026-08-01',
        endDate: '2026-08-31',
        page: 1,
        size: 10,
      }),
    );
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/expenses') &&
        req.params.get('status') === 'OPEN' &&
        req.params.get('categoryId') === '01900000-0000-7000-8000-000000000002' &&
        req.params.get('accountId') === '01900000-0000-7000-8000-000000000003' &&
        req.params.get('paymentMethod') === 'ACCOUNT' &&
        req.params.get('responsibleType') === 'MINE' &&
        req.params.get('startDate') === '2026-08-01' &&
        req.params.get('endDate') === '2026-08-31' &&
        req.params.get('page') === '1' &&
        req.params.get('size') === '10',
    );
    expect(request.request.method).toBe('GET');
    request.flush(pageBody([]));
    await expect(pending).resolves.toMatchObject({ items: [], page: 0 });
  });

  it('gets expense detail with GET /expenses/{id}', async () => {
    const pending = firstValueFrom(service.get(EXPENSE_ID));
    const request = httpTesting.expectOne(api(`/expenses/${EXPENSE_ID}`));
    expect(request.request.method).toBe('GET');
    request.flush(expenseBody());
    await expect(pending).resolves.toMatchObject({ id: EXPENSE_ID, status: 'OPEN' });
  });

  it('creates expense with POST /expenses', async () => {
    const body = {
      categoryId: '01900000-0000-7000-8000-000000000002',
      description: 'Mercado',
      totalAmount: 150.5,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-10',
      paymentMethod: 'ACCOUNT' as const,
      accountId: '01900000-0000-7000-8000-000000000003',
      responsibleType: 'MINE' as const,
    };
    const pending = firstValueFrom(service.create(body));
    const request = httpTesting.expectOne(api('/expenses'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush(expenseBody(), { status: 201, statusText: 'Created' });
    await expect(pending).resolves.toMatchObject({ description: 'Mercado' });
  });

  it('creates a CREDIT_CARD expense with creditCardId and without accountId', async () => {
    const body = {
      categoryId: '01900000-0000-7000-8000-000000000002',
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'CREDIT_CARD' as const,
      creditCardId: '01900000-0000-7000-8000-000000000040',
      responsibleType: 'MINE' as const,
    };
    const pending = firstValueFrom(service.create(body));
    const request = httpTesting.expectOne(api('/expenses'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.body).not.toHaveProperty('accountId');
    request.flush(
      expenseBody({
        paymentMethod: 'CREDIT_CARD',
        accountId: null,
        creditCardId: body.creditCardId,
      }),
      { status: 201, statusText: 'Created' },
    );
    await expect(pending).resolves.toMatchObject({
      paymentMethod: 'CREDIT_CARD',
      creditCardId: body.creditCardId,
    });
  });

  it('updates expense with PUT /expenses/{id}', async () => {
    const body = {
      categoryId: '01900000-0000-7000-8000-000000000002',
      description: 'Mercado atualizado',
      totalAmount: 160,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-10',
      paymentMethod: 'ACCOUNT' as const,
      accountId: '01900000-0000-7000-8000-000000000003',
      responsibleType: 'MINE' as const,
    };
    const pending = firstValueFrom(service.update(EXPENSE_ID, body));
    const request = httpTesting.expectOne(api(`/expenses/${EXPENSE_ID}`));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(body);
    request.flush(expenseBody({ description: 'Mercado atualizado', totalAmount: 160 }));
    await expect(pending).resolves.toMatchObject({ description: 'Mercado atualizado' });
  });

  it('cancels expense with POST /expenses/{id}/cancel', async () => {
    const pending = firstValueFrom(service.cancel(EXPENSE_ID));
    const request = httpTesting.expectOne(api(`/expenses/${EXPENSE_ID}/cancel`));
    expect(request.request.method).toBe('POST');
    request.flush(expenseBody({ status: 'CANCELLED' }));
    await expect(pending).resolves.toMatchObject({ status: 'CANCELLED' });
  });

  it('pays 1/1 expense with POST /expenses/{id}/pay', async () => {
    const payRequest = {
      accountId: '01900000-0000-7000-8000-000000000003',
      amount: 150.5,
      paymentDate: '2026-08-10',
    };
    const pending = firstValueFrom(service.pay(EXPENSE_ID, payRequest));
    const request = httpTesting.expectOne(api(`/expenses/${EXPENSE_ID}/pay`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payRequest);
    request.flush(expenseBody({ status: 'PAID' }));
    await expect(pending).resolves.toMatchObject({ status: 'PAID' });
  });

  it('pays installment with POST /expenses/{id}/installments/{installmentId}/payments', async () => {
    const payRequest = {
      accountId: '01900000-0000-7000-8000-000000000003',
      amount: 50,
      paymentDate: '2026-08-10',
    };
    const pending = firstValueFrom(service.payInstallment(EXPENSE_ID, INSTALLMENT_ID, payRequest));
    const request = httpTesting.expectOne(
      api(`/expenses/${EXPENSE_ID}/installments/${INSTALLMENT_ID}/payments`),
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payRequest);
    request.flush(expenseBody({ status: 'PARTIALLY_PAID' }));
    await expect(pending).resolves.toMatchObject({ status: 'PARTIALLY_PAID' });
  });

  it('refunds expense with POST /expenses/{id}/refund', async () => {
    const pending = firstValueFrom(service.refund(EXPENSE_ID));
    const request = httpTesting.expectOne(api(`/expenses/${EXPENSE_ID}/refund`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush(expenseBody({ status: 'REFUNDED' }));
    await expect(pending).resolves.toMatchObject({ status: 'REFUNDED' });
  });

  it('lists installments with GET /expenses/{id}/installments', async () => {
    const pending = firstValueFrom(service.listInstallments(EXPENSE_ID));
    const request = httpTesting.expectOne(api(`/expenses/${EXPENSE_ID}/installments`));
    expect(request.request.method).toBe('GET');
    request.flush([installmentBody()]);
    const installments = await pending;
    expect(installments).toHaveLength(1);
    expect(installments[0]?.remainingAmount).toBe(150.5);
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
          req.url === api('/expenses') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush(
        {
          timestamp: '2026-08-19T15:00:00Z',
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Erro interno.',
          path: '/api/v1/expenses',
        },
        { status: 500, statusText: 'Server Error' },
      );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('rejects a page response that does not match the expenses contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/expenses') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush({ items: [{ id: EXPENSE_ID }] });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
