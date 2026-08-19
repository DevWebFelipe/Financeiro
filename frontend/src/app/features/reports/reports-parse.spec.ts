import {
  parseCardReport,
  parseCashFlowReport,
  parseCategoryReport,
  parseExpenseReport,
  parseIncomeReport,
  parseInvoiceReport,
  parseResponsibleReport,
} from './reports-parse';

const EXPENSE_ID = '01900000-0000-7000-8000-000000000010';
const INSTALLMENT_ID = '01900000-0000-7000-8000-000000000011';
const CATEGORY_ID = '01900000-0000-7000-8000-000000000002';
const INCOME_ID = '01900000-0000-7000-8000-000000000020';
const CARD_ID = '01900000-0000-7000-8000-000000000040';
const INVOICE_ID = '01900000-0000-7000-8000-000000000050';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';

const period = { startDate: '2026-08-01', endDate: '2026-08-31' };

function expenseSummary(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    periodOriginal: 1500,
    periodDiscount: 0,
    periodSurcharge: 0,
    periodObligation: 1500,
    periodPaid: 500,
    periodRemaining: 1000,
    ...overrides,
  };
}

function invoiceBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: INVOICE_ID,
    creditCardId: CARD_ID,
    referenceYear: 2026,
    referenceMonth: 8,
    closingDate: '2026-08-10',
    dueDate: '2026-08-20',
    status: 'CLOSED',
    totalAmount: 2000,
    paidAmount: 1200,
    remainingAmount: 800,
    createdAt: '2026-08-10T12:00:00Z',
    updatedAt: '2026-08-10T12:00:00Z',
    ...overrides,
  };
}

function paymentBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: '01900000-0000-7000-8000-000000000060',
    invoiceId: INVOICE_ID,
    accountId: ACCOUNT_ID,
    amount: 500,
    paymentDate: '2026-08-12',
    notes: null,
    status: 'ACTIVE',
    createdAt: '2026-08-12T12:00:00Z',
    ...overrides,
  };
}

function invoiceAdjustmentBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: '01900000-0000-7000-8000-000000000061',
    invoiceId: INVOICE_ID,
    type: 'DISCOUNT',
    amount: 10,
    reason: 'Ajuste',
    status: 'ACTIVE',
    createdAt: '2026-08-12T12:00:00Z',
    ...overrides,
  };
}

describe('reports parse', () => {
  it('parses an expense report including installments', () => {
    const parsed = parseExpenseReport({
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
          installments: [
            {
              id: INSTALLMENT_ID,
              installmentNumber: 1,
              totalInstallments: 1,
              dueDate: '2026-08-10',
              original: 1500,
              discount: 0,
              surcharge: 0,
              obligation: 1500,
              paid: 500,
              remaining: 1000,
              status: 'PARTIALLY_PAID',
            },
          ],
        },
      ],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
      summary: expenseSummary(),
    });

    expect(parsed?.items[0]?.description).toBe('Aluguel');
    expect(parsed?.items[0]?.installments[0]?.remaining).toBe(1000);
    expect(parsed?.summary.periodObligation).toBe(1500);
  });

  it('parses an income report and keeps periodReceivedAmount optional', () => {
    const withPeriod = parseIncomeReport({
      period,
      dateType: 'RECEIVED',
      items: [
        {
          id: INCOME_ID,
          description: 'Salário',
          status: 'RECEIVED',
          categoryId: CATEGORY_ID,
          responsibleType: 'MINE',
          responsibleName: null,
          expectedDate: '2026-08-05',
          amount: 5400,
          accruedAmount: 0,
          receivedAmount: 5400,
          remainingAmount: 0,
          periodReceivedAmount: 5400,
        },
      ],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
      summary: { periodReceivedAmount: 5400 },
    });
    expect(withPeriod?.summary.periodReceivedAmount).toBe(5400);
    expect(withPeriod?.summary.amount).toBeUndefined();
    expect(withPeriod?.items[0]?.periodReceivedAmount).toBe(5400);

    const expected = parseIncomeReport({
      period,
      dateType: 'EXPECTED',
      items: [
        {
          id: INCOME_ID,
          description: 'Salário',
          status: 'EXPECTED',
          categoryId: CATEGORY_ID,
          responsibleType: null,
          responsibleName: null,
          expectedDate: '2026-08-05',
          amount: 5400,
          accruedAmount: 0,
          receivedAmount: 0,
          remainingAmount: 5400,
        },
      ],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
      summary: { amount: 5400, accruedAmount: 0, receivedAmount: 0, remainingAmount: 5400 },
    });
    expect(expected?.items[0]?.periodReceivedAmount).toBeUndefined();
    expect(expected?.summary.amount).toBe(5400);
  });

  it('parses category items with only the official money fields present', () => {
    const parsed = parseCategoryReport({
      period,
      dateType: 'EXPECTED',
      items: [
        {
          categoryId: CATEGORY_ID,
          name: 'Moradia',
          type: 'EXPENSE',
          active: true,
          ...expenseSummary(),
        },
        {
          categoryId: '01900000-0000-7000-8000-000000000022',
          name: 'Salário',
          type: 'INCOME',
          active: true,
          amount: 5400,
          accruedAmount: 0,
          receivedAmount: 0,
          remainingAmount: 5400,
        },
      ],
      page: 0,
      size: 20,
      totalItems: 2,
      totalPages: 1,
      summary: {
        expense: expenseSummary(),
        income: { amount: 5400, accruedAmount: 0, receivedAmount: 0, remainingAmount: 5400 },
      },
    });
    expect(parsed?.items[0]?.periodObligation).toBe(1500);
    expect(parsed?.items[0]?.amount).toBeUndefined();
    expect(parsed?.items[1]?.amount).toBe(5400);
    expect(parsed?.items[1]?.periodObligation).toBeUndefined();
  });

  it('parses a responsible report with nullable dateType', () => {
    const parsed = parseResponsibleReport({
      period,
      nature: 'EXPENSE',
      items: [
        {
          key: 'MINE',
          responsibleType: 'MINE',
          responsibleName: null,
          expense: expenseSummary(),
        },
      ],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
      summary: { expense: expenseSummary() },
    });
    expect(parsed?.dateType).toBeNull();
    expect(parsed?.items[0]?.expense?.periodObligation).toBe(1500);
    expect(parsed?.items[0]?.income).toBeUndefined();
  });

  it('parses a card report reusing invoice, payment and adjustment shapes', () => {
    const parsed = parseCardReport({
      period,
      items: [
        {
          creditCardId: CARD_ID,
          name: 'Nubank',
          holderName: 'Ederson',
          lastFourDigits: '1234',
          active: true,
          summary: {
            purchaseAmount: 12000,
            invoiceAmount: 2000,
            paidAmount: 500,
            creditAmount: 0,
          },
          purchases: [
            {
              expenseId: EXPENSE_ID,
              description: 'Notebook',
              expenseDate: '2026-08-02',
              original: 12000,
              responsibleType: 'MINE',
              responsibleName: null,
              status: 'OPEN',
              totalInstallments: 12,
              installments: [{ installmentNumber: 1, dueDate: '2026-08-20', amount: 1000 }],
            },
          ],
          invoices: [invoiceBody()],
          payments: [paymentBody()],
          credits: [],
          installmentAdjustments: [],
          invoiceAdjustments: [invoiceAdjustmentBody()],
        },
      ],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
      summary: {
        purchaseAmount: 12000,
        invoiceAmount: 2000,
        paidAmount: 500,
        creditAmount: 0,
      },
    });
    expect(parsed?.items[0]?.invoices[0]?.id).toBe(INVOICE_ID);
    expect(parsed?.items[0]?.payments[0]?.amount).toBe(500);
    expect(parsed?.items[0]?.invoiceAdjustments[0]?.reason).toBe('Ajuste');
  });

  it('parses cash-flow historical optional balances and empty projected', () => {
    const emptyProjected = parseCashFlowReport({
      period,
      flowType: 'BOTH',
      accountId: null,
      historical: {
        items: [],
        page: 0,
        size: 20,
        totalItems: 0,
        totalPages: 0,
        summary: { totalIn: 0, totalOut: 0, net: 0 },
      },
      projected: { empty: true },
    });
    expect(emptyProjected?.historical?.openingBalance).toBeUndefined();
    expect(emptyProjected?.projected).toEqual({ empty: true });

    const withBalances = parseCashFlowReport({
      period,
      flowType: 'HISTORICAL',
      accountId: ACCOUNT_ID,
      historical: {
        openingBalance: 1000,
        closingBalance: 4900,
        items: [
          {
            id: INCOME_ID,
            type: 'INCOME_RECEIPT',
            date: '2026-08-05',
            amount: 5400,
            accountId: ACCOUNT_ID,
            description: 'Salário',
          },
        ],
        page: 0,
        size: 20,
        totalItems: 1,
        totalPages: 1,
        summary: { totalIn: 5400, totalOut: 1500, net: 3900 },
      },
    });
    expect(withBalances?.historical?.openingBalance).toBe(1000);
    expect(withBalances?.projected).toBeUndefined();
  });

  it('parses an invoice report without inventing extra fields', () => {
    const parsed = parseInvoiceReport({
      invoiceId: INVOICE_ID,
      card: { name: 'Cartão Ederson', holderName: 'Ederson', lastFourDigits: '1234' },
      invoice: {
        referenceYear: 2026,
        referenceMonth: 8,
        closingDate: '2026-08-10',
        dueDate: '2026-08-20',
        status: 'CLOSED',
        totalAmount: 2000,
        paidAmount: 1200,
        remainingAmount: 800,
      },
      purchases: [
        {
          expenseId: EXPENSE_ID,
          description: 'Mercado',
          expenseDate: '2026-08-02',
          original: 200,
          categoryName: 'Alimentação',
          responsibleType: 'MINE',
          responsibleName: null,
          installmentNumber: 1,
          totalInstallments: 1,
          discount: 0,
          surcharge: 0,
        },
      ],
      byCategory: [{ name: 'Alimentação', original: 200 }],
      byResponsible: [{ responsibleType: 'MINE', responsibleName: null, original: 200 }],
      installmentAdjustments: [],
      invoiceAdjustments: [],
      credits: [],
      payments: [],
      allocations: [
        {
          id: '01900000-0000-7000-8000-000000000070',
          type: 'PAYMENT',
          sourceId: '01900000-0000-7000-8000-000000000060',
          installmentId: INSTALLMENT_ID,
          amount: 100,
          createdAt: '2026-08-12T12:00:00Z',
        },
      ],
    });
    expect(parsed?.invoice.remainingAmount).toBe(800);
    expect(parsed?.allocations[0]?.type).toBe('PAYMENT');
  });

  it('rejects an expense report missing official fields', () => {
    expect(parseExpenseReport({ items: [] })).toBeNull();
  });
});
