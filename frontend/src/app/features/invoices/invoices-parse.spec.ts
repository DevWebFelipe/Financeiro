import {
  parseInvoice,
  parseInvoiceAdjustment,
  parseInvoiceAdjustmentList,
  parseInvoiceAgreement,
  parseInvoiceAgreementList,
  parseInvoiceItem,
  parseInvoiceItemList,
  parseInvoiceList,
  parseInvoicePayment,
  parseInvoicePaymentList,
} from './invoices-parse';

const INVOICE_ID = '01900000-0000-7000-8000-000000000050';
const CARD_ID = '01900000-0000-7000-8000-000000000040';
const ITEM_ID = '01900000-0000-7000-8000-000000000051';
const EXPENSE_ID = '01900000-0000-7000-8000-000000000010';

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
    installmentNumber: 2,
    totalInstallments: 3,
    amount: 333.34,
    remainingAmount: 333.34,
    dueDate: '2026-08-20',
    status: 'OPEN',
    overdue: false,
    createdAt: '2026-08-01T12:00:00Z',
    updatedAt: '2026-08-01T12:00:00Z',
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
    installments: [
      {
        id: ITEM_ID,
        expenseId: EXPENSE_ID,
        installmentNumber: 1,
        totalInstallments: 2,
        amount: 420,
        remainingAmount: 420,
        dueDate: '2026-09-20',
        status: 'OPEN',
        invoiceId: null,
        createdAt: '2026-08-20T12:00:00Z',
        updatedAt: '2026-08-20T12:00:00Z',
      },
    ],
    ...overrides,
  };
}

describe('invoices-parse', () => {
  it('parses official invoice fields without deriving remaining', () => {
    const parsed = parseInvoice(invoiceBody());
    expect(parsed?.totalAmount).toBe(1000);
    expect(parsed?.paidAmount).toBe(100);
    expect(parsed?.remainingAmount).toBe(800);
    expect(parsed?.status).toBe('CLOSED');
  });

  it('parses SETTLED_BY_AGREEMENT as an official invoice status', () => {
    expect(parseInvoice(invoiceBody({ status: 'SETTLED_BY_AGREEMENT' }))?.status).toBe(
      'SETTLED_BY_AGREEMENT',
    );
  });

  it('rejects PARTIALLY_PAID as an invoice status', () => {
    expect(parseInvoice(invoiceBody({ status: 'PARTIALLY_PAID' }))).toBeNull();
  });

  it('parses an invoice list', () => {
    const parsed = parseInvoiceList([invoiceBody(), invoiceBody({ id: ITEM_ID, status: 'OPEN' })]);
    expect(parsed).toHaveLength(2);
  });

  it('returns null for a non-array invoice list', () => {
    expect(parseInvoiceList({ id: INVOICE_ID })).toBeNull();
  });

  it('parses installment items without inventing description', () => {
    const parsed = parseInvoiceItem(itemBody());
    expect(parsed).toEqual(
      expect.objectContaining({
        installmentNumber: 2,
        totalInstallments: 3,
        amount: 333.34,
        overdue: false,
      }),
    );
    expect(parsed).not.toHaveProperty('description');
  });

  it('parses an item list', () => {
    expect(parseInvoiceItemList([itemBody()])).toHaveLength(1);
  });

  it('returns null when overdue is missing from an item', () => {
    const body = itemBody();
    delete body['overdue'];
    expect(parseInvoiceItem(body)).toBeNull();
  });

  it('parses an invoice payment including nullable notes', () => {
    const parsed = parseInvoicePayment({
      id: ITEM_ID,
      invoiceId: INVOICE_ID,
      accountId: CARD_ID,
      amount: 500,
      paymentDate: '2026-08-20',
      notes: null,
      status: 'ACTIVE',
      createdAt: '2026-08-20T12:00:00Z',
    });
    expect(parsed?.amount).toBe(500);
    expect(parsed?.notes).toBeNull();
    expect(parsed?.status).toBe('ACTIVE');
  });

  it('parses a payment list', () => {
    expect(
      parseInvoicePaymentList([
        {
          id: ITEM_ID,
          invoiceId: INVOICE_ID,
          accountId: CARD_ID,
          amount: 500,
          paymentDate: '2026-08-20',
          notes: 'PIX',
          status: 'REVERSED',
          createdAt: '2026-08-20T12:00:00Z',
        },
      ]),
    ).toHaveLength(1);
  });

  it('parses DISCOUNT and SURCHARGE adjustments with official statuses', () => {
    const discount = parseInvoiceAdjustment({
      id: ITEM_ID,
      invoiceId: INVOICE_ID,
      type: 'DISCOUNT',
      amount: 100,
      reason: 'Correção de cobrança',
      status: 'ACTIVE',
      createdAt: '2026-08-20T12:00:00Z',
    });
    expect(discount).toEqual(
      expect.objectContaining({
        type: 'DISCOUNT',
        amount: 100,
        reason: 'Correção de cobrança',
        status: 'ACTIVE',
      }),
    );

    const surcharge = parseInvoiceAdjustment({
      id: ITEM_ID,
      invoiceId: INVOICE_ID,
      type: 'SURCHARGE',
      amount: 25.5,
      reason: 'Juros',
      status: 'REVERSED',
      createdAt: '2026-08-20T12:00:00Z',
    });
    expect(surcharge?.type).toBe('SURCHARGE');
    expect(surcharge?.status).toBe('REVERSED');
  });

  it('parses an adjustment list and rejects an invalid payload', () => {
    expect(
      parseInvoiceAdjustmentList([
        {
          id: ITEM_ID,
          invoiceId: INVOICE_ID,
          type: 'DISCOUNT',
          amount: 10,
          reason: 'Ajuste',
          status: 'ACTIVE',
          createdAt: '2026-08-20T12:00:00Z',
        },
      ]),
    ).toHaveLength(1);
    expect(parseInvoiceAdjustmentList({ items: [] })).toBeNull();
    expect(
      parseInvoiceAdjustment({
        id: ITEM_ID,
        invoiceId: INVOICE_ID,
        type: 'CREDIT',
        amount: 10,
        reason: 'Ajuste',
        status: 'ACTIVE',
        createdAt: '2026-08-20T12:00:00Z',
      }),
    ).toBeNull();
  });

  it('parses agreements with official statuses, nullable supersededByAgreementId and installment invoiceId', () => {
    const body = agreementBody();
    const parsed = parseInvoiceAgreement(body);
    expect(parsed?.status).toBe('ACTIVE');
    expect(parsed?.entryAmount).toBe(0);
    expect(parsed?.additionalCostPercent).toBe(0.05);
    expect(parsed?.supersededByAgreementId).toBeNull();
    expect(parsed?.installments[0]?.invoiceId).toBeNull();
    expect(parsed?.installments[0]?.remainingAmount).toBe(420);

    expect(parseInvoiceAgreement(agreementBody({ status: 'COMPLETED' }))?.status).toBe('COMPLETED');
    expect(parseInvoiceAgreement(agreementBody({ status: 'CANCELLED' }))?.status).toBe('CANCELLED');
    expect(
      parseInvoiceAgreement(
        agreementBody({ status: 'RENEGOTIATED', supersededByAgreementId: ITEM_ID }),
      )?.supersededByAgreementId,
    ).toBe(ITEM_ID);
  });

  it('parses an agreement list and rejects unknown status or missing required fields', () => {
    expect(parseInvoiceAgreementList([agreementBody()])).toHaveLength(1);
    expect(parseInvoiceAgreementList({ items: [] })).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ status: 'OPEN' }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ id: '' }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ creditCardId: '' }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ sourceInvoiceId: '' }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ expenseId: '' }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ entryAmount: '0' }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ financedAmount: undefined }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ installmentCount: 0 }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ installmentAmount: undefined }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ contractedTotal: undefined }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ additionalCost: undefined }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ additionalCostPercent: undefined }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ createdAt: '' }))).toBeNull();
    expect(parseInvoiceAgreement(agreementBody({ installments: null }))).toBeNull();
  });
});
