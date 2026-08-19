import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { InvoicesService } from './invoices.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const CARD_ID = '01900000-0000-7000-8000-000000000040';
const INVOICE_ID = '01900000-0000-7000-8000-000000000050';
const ITEM_ID = '01900000-0000-7000-8000-000000000051';
const EXPENSE_ID = '01900000-0000-7000-8000-000000000010';
const PAYMENT_ID = '01900000-0000-7000-8000-000000000052';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const ADJUSTMENT_ID = '01900000-0000-7000-8000-000000000053';

function invoiceBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: INVOICE_ID,
    creditCardId: CARD_ID,
    referenceYear: 2026,
    referenceMonth: 8,
    closingDate: '2026-08-10',
    dueDate: '2026-08-20',
    status: 'CLOSED',
    totalAmount: 1000,
    paidAmount: 100,
    remainingAmount: 800,
    createdAt: '2026-08-10T12:00:00Z',
    updatedAt: '2026-08-10T12:00:00Z',
    ...overrides,
  };
}

function itemBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: ITEM_ID,
    expenseId: EXPENSE_ID,
    installmentNumber: 1,
    totalInstallments: 1,
    amount: 800,
    remainingAmount: 800,
    dueDate: '2026-08-20',
    status: 'OPEN',
    overdue: false,
    createdAt: '2026-08-01T12:00:00Z',
    updatedAt: '2026-08-01T12:00:00Z',
    ...overrides,
  };
}

function paymentBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: PAYMENT_ID,
    invoiceId: INVOICE_ID,
    accountId: ACCOUNT_ID,
    amount: 500,
    paymentDate: '2026-08-20',
    notes: null,
    status: 'ACTIVE',
    createdAt: '2026-08-20T12:00:00Z',
    ...overrides,
  };
}

function adjustmentBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: ADJUSTMENT_ID,
    invoiceId: INVOICE_ID,
    type: 'DISCOUNT',
    amount: 100,
    reason: 'Correção de cobrança',
    status: 'ACTIVE',
    createdAt: '2026-08-20T12:00:00Z',
    ...overrides,
  };
}

function agreementBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: ITEM_ID,
    creditCardId: CARD_ID,
    sourceInvoiceId: INVOICE_ID,
    expenseId: EXPENSE_ID,
    status: 'ACTIVE',
    entryAmount: 0,
    financedAmount: 800,
    installmentCount: 2,
    installmentAmount: 420,
    contractedTotal: 840,
    additionalCost: 40,
    additionalCostPercent: 0.05,
    createdAt: '2026-08-20T12:00:00Z',
    supersededByAgreementId: null,
    installments: [],
    ...overrides,
  };
}

describe('InvoicesService', () => {
  let service: InvoicesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InvoicesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('lists invoices by card without query params when filters are empty', async () => {
    const pending = firstValueFrom(service.listByCard(CARD_ID));
    const request = httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/invoices`));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([invoiceBody()]);

    const invoices = await pending;
    expect(invoices).toHaveLength(1);
    expect(invoices[0]?.remainingAmount).toBe(800);
  });

  it('sends year, month and status as official query params', async () => {
    const pending = firstValueFrom(
      service.listByCard(CARD_ID, { year: 2026, month: 8, status: 'OPEN' }),
    );
    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === api(`/credit-cards/${CARD_ID}/invoices`) &&
        candidate.params.get('year') === '2026' &&
        candidate.params.get('month') === '8' &&
        candidate.params.get('status') === 'OPEN',
    );
    expect(request.request.method).toBe('GET');
    request.flush([invoiceBody({ status: 'OPEN' })]);
    await pending;
  });

  it('loads a single invoice by id', async () => {
    const pending = firstValueFrom(service.get(INVOICE_ID));
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}`));
    expect(request.request.method).toBe('GET');
    request.flush(invoiceBody());

    const invoice = await pending;
    expect(invoice.id).toBe(INVOICE_ID);
    expect(invoice.totalAmount).toBe(1000);
  });

  it('loads invoice items as installments', async () => {
    const pending = firstValueFrom(service.listItems(INVOICE_ID));
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/items`));
    expect(request.request.method).toBe('GET');
    request.flush([itemBody()]);

    const items = await pending;
    expect(items).toHaveLength(1);
    expect(items[0]?.installmentNumber).toBe(1);
    expect(items[0]?.totalInstallments).toBe(1);
  });

  it('propagates API errors from the list endpoint', async () => {
    const pending = firstValueFrom(service.listByCard(CARD_ID));
    httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/invoices`)).flush(
      {
        timestamp: '2026-08-19T15:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: `/api/v1/credit-cards/${CARD_ID}/invoices`,
      },
      { status: 500, statusText: 'Server Error' },
    );

    try {
      await pending;
      throw new Error('expected listByCard to fail');
    } catch (error: unknown) {
      expect(isApiError(error)).toBe(true);
    }
  });

  it('rejects a list payload that does not match the contract', async () => {
    const pending = firstValueFrom(service.listByCard(CARD_ID));
    httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/invoices`)).flush({ items: [] });
    await expect(pending).rejects.toThrow('Invoices response did not match the expected contract.');
  });

  it('lists invoice payments', async () => {
    const pending = firstValueFrom(service.listPayments(INVOICE_ID));
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/payments`));
    expect(request.request.method).toBe('GET');
    request.flush([paymentBody()]);

    const payments = await pending;
    expect(payments).toHaveLength(1);
    expect(payments[0]?.amount).toBe(500);
    expect(payments[0]?.status).toBe('ACTIVE');
  });

  it('posts a payment with accountId, amount and paymentDate and omits empty notes', async () => {
    const pending = firstValueFrom(
      service.createPayment(INVOICE_ID, {
        accountId: ACCOUNT_ID,
        amount: 500,
        paymentDate: '2026-08-20',
      }),
    );
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/payments`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      accountId: ACCOUNT_ID,
      amount: 500,
      paymentDate: '2026-08-20',
    });
    expect(request.request.body).not.toHaveProperty('notes');
    request.flush(paymentBody());
    await pending;
  });

  it('posts notes when they are provided', async () => {
    const pending = firstValueFrom(
      service.createPayment(INVOICE_ID, {
        accountId: ACCOUNT_ID,
        amount: 500,
        paymentDate: '2026-08-20',
        notes: 'PIX',
      }),
    );
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/payments`));
    expect(request.request.body).toEqual({
      accountId: ACCOUNT_ID,
      amount: 500,
      paymentDate: '2026-08-20',
      notes: 'PIX',
    });
    request.flush(paymentBody({ notes: 'PIX' }));
    await pending;
  });

  it('reverses a payment with POST, not DELETE', async () => {
    const pending = firstValueFrom(service.reversePayment(INVOICE_ID, PAYMENT_ID));
    const request = httpTesting.expectOne(
      api(`/invoices/${INVOICE_ID}/payments/${PAYMENT_ID}/reverse`),
    );
    expect(request.request.method).toBe('POST');
    request.flush(paymentBody({ status: 'REVERSED' }));

    const reversed = await pending;
    expect(reversed.status).toBe('REVERSED');
  });

  it('lists invoice adjustments', async () => {
    const pending = firstValueFrom(service.listAdjustments(INVOICE_ID));
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/adjustments`));
    expect(request.request.method).toBe('GET');
    request.flush([adjustmentBody()]);

    const adjustments = await pending;
    expect(adjustments).toHaveLength(1);
    expect(adjustments[0]?.type).toBe('DISCOUNT');
    expect(adjustments[0]?.reason).toBe('Correção de cobrança');
  });

  it('posts an adjustment with type, amount and reason only', async () => {
    const pending = firstValueFrom(
      service.createAdjustment(INVOICE_ID, {
        type: 'DISCOUNT',
        amount: 100,
        reason: 'Correção de cobrança',
      }),
    );
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/adjustments`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      type: 'DISCOUNT',
      amount: 100,
      reason: 'Correção de cobrança',
    });
    request.flush(adjustmentBody());
    await pending;
  });

  it('reverses an adjustment with POST and an empty body, not DELETE', async () => {
    const pending = firstValueFrom(service.reverseAdjustment(INVOICE_ID, ADJUSTMENT_ID));
    const request = httpTesting.expectOne(
      api(`/invoices/${INVOICE_ID}/adjustments/${ADJUSTMENT_ID}/reverse`),
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush(adjustmentBody({ status: 'REVERSED' }));

    const reversed = await pending;
    expect(reversed.status).toBe('REVERSED');
  });

  it('propagates API errors from createAdjustment', async () => {
    const pending = firstValueFrom(
      service.createAdjustment(INVOICE_ID, {
        type: 'SURCHARGE',
        amount: 10,
        reason: 'Juros',
      }),
    );
    httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/adjustments`)).flush(
      {
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto.',
        path: `/api/v1/invoices/${INVOICE_ID}/adjustments`,
      },
      { status: 400, statusText: 'Bad Request' },
    );

    try {
      await pending;
      throw new Error('expected createAdjustment to fail');
    } catch (error: unknown) {
      expect(isApiError(error)).toBe(true);
    }
  });

  it('lists invoice agreements without query params', async () => {
    const pending = firstValueFrom(service.listAgreements(INVOICE_ID));
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/agreements`));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([agreementBody()]);
    const agreements = await pending;
    expect(agreements).toHaveLength(1);
    expect(agreements[0]?.financedAmount).toBe(800);
  });

  it('posts a new agreement with only contract fields', async () => {
    const pending = firstValueFrom(
      service.createAgreement(INVOICE_ID, {
        entryAmount: 0,
        accountId: ACCOUNT_ID,
        entryPaymentDate: '2026-08-20',
        installmentCount: 2,
        installmentAmount: 420,
      }),
    );
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/agreements`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      entryAmount: 0,
      accountId: ACCOUNT_ID,
      entryPaymentDate: '2026-08-20',
      installmentCount: 2,
      installmentAmount: 420,
    });
    expect(request.request.body).not.toHaveProperty('financedAmount');
    expect(request.request.body).not.toHaveProperty('creditCardId');
    request.flush(agreementBody(), { status: 201, statusText: 'Created' });
    await pending;
  });

  it('posts a renegotiation with anticipatedFuturesNetAmount', async () => {
    const pending = firstValueFrom(
      service.createRenegotiation(INVOICE_ID, {
        entryAmount: 100,
        accountId: ACCOUNT_ID,
        entryPaymentDate: '2026-08-20',
        installmentCount: 2,
        installmentAmount: 400,
        anticipatedFuturesNetAmount: 50,
      }),
    );
    const request = httpTesting.expectOne(api(`/invoices/${INVOICE_ID}/renegotiations`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      entryAmount: 100,
      accountId: ACCOUNT_ID,
      entryPaymentDate: '2026-08-20',
      installmentCount: 2,
      installmentAmount: 400,
      anticipatedFuturesNetAmount: 50,
    });
    expect(request.request.body).not.toHaveProperty('agreementIds');
    request.flush(agreementBody(), { status: 201, statusText: 'Created' });
    await pending;
  });

  it('loads an agreement by id', async () => {
    const pending = firstValueFrom(service.getAgreement(ITEM_ID));
    const request = httpTesting.expectOne(api(`/agreements/${ITEM_ID}`));
    expect(request.request.method).toBe('GET');
    request.flush(agreementBody());
    await pending;
  });

  it('posts installment anticipation with settled and omits empty notes', async () => {
    const pending = firstValueFrom(
      service.anticipateInstallment(ITEM_ID, ITEM_ID, {
        accountId: ACCOUNT_ID,
        amount: 100,
        paymentDate: '2026-08-20',
        settled: false,
      }),
    );
    const request = httpTesting.expectOne(
      api(`/agreements/${ITEM_ID}/installments/${ITEM_ID}/anticipate`),
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      accountId: ACCOUNT_ID,
      amount: 100,
      paymentDate: '2026-08-20',
      settled: false,
    });
    expect(request.request.body).not.toHaveProperty('notes');
    request.flush(agreementBody());
    await pending;
  });
});
