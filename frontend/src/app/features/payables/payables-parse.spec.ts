import { parsePayableItem, parsePayablePage } from './payables-parse';

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

describe('payables parse', () => {
  it('parses a valid payables page including official totals', () => {
    const parsed = parsePayablePage({
      items: [itemBody()],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
      totalRemaining: 1000,
      totalOriginal: 1500,
      totalPaid: 500,
    });

    expect(parsed).toMatchObject({
      page: 0,
      totalItems: 1,
      totalRemaining: 1000,
      totalOriginal: 1500,
      totalPaid: 500,
    });
    expect(parsed?.items[0]?.name).toBe('Aluguel');
  });

  it('parses an invoice line with nullable expense fields', () => {
    const parsed = parsePayableItem(
      itemBody({
        type: 'INVOICE',
        expenseId: null,
        creditCardId: '01900000-0000-7000-8000-000000000040',
        categoryId: null,
        accountId: null,
        paymentMethod: null,
        name: 'Nubank',
        status: 'CLOSED',
        responsibleType: null,
      }),
    );

    expect(parsed).toMatchObject({
      type: 'INVOICE',
      expenseId: null,
      paymentMethod: null,
      status: 'CLOSED',
    });
  });

  it('parses an empty page', () => {
    const parsed = parsePayablePage({
      items: [],
      page: 0,
      size: 20,
      totalItems: 0,
      totalPages: 0,
      totalRemaining: 0,
      totalOriginal: 0,
      totalPaid: 0,
    });

    expect(parsed?.items).toEqual([]);
    expect(parsed?.totalRemaining).toBe(0);
  });

  it('rejects a page missing official totals', () => {
    expect(
      parsePayablePage({
        items: [itemBody()],
        page: 0,
        size: 20,
        totalItems: 1,
        totalPages: 1,
      }),
    ).toBeNull();
  });

  it('rejects an item missing remainingAmount', () => {
    const body = itemBody();
    delete body['remainingAmount'];
    expect(parsePayableItem(body)).toBeNull();
  });

  it('rejects an item with invalid dueDate', () => {
    expect(parsePayableItem(itemBody({ dueDate: '10/08/2026' }))).toBeNull();
  });
});
