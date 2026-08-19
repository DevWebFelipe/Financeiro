import { parseExpense, parseExpensePage, parseInstallment } from './expenses-parse';

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

describe('expenses parse', () => {
  it('parses a valid expense page', () => {
    const parsed = parseExpensePage({
      items: [expenseBody()],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });

    expect(parsed).toMatchObject({
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });
    expect(parsed?.items[0]?.description).toBe('Mercado');
  });

  it('parses a valid expense', () => {
    const parsed = parseExpense(expenseBody());
    expect(parsed).toMatchObject({
      id: EXPENSE_ID,
      totalAmount: 150.5,
      expenseDate: '2026-08-01',
      status: 'OPEN',
      overdue: false,
    });
  });

  it('parses a valid installment', () => {
    const parsed = parseInstallment({
      id: INSTALLMENT_ID,
      expenseId: EXPENSE_ID,
      installmentNumber: 1,
      totalInstallments: 3,
      amount: 50,
      remainingAmount: 50,
      dueDate: '2026-08-10',
      status: 'OPEN',
      overdue: false,
      createdAt: '2026-08-14T12:00:00Z',
      updatedAt: '2026-08-14T12:00:00Z',
    });

    expect(parsed).toMatchObject({
      installmentNumber: 1,
      totalInstallments: 3,
      remainingAmount: 50,
    });
  });

  it('rejects an expense page missing required fields', () => {
    expect(parseExpensePage({ items: [expenseBody()], page: 0 })).toBeNull();
  });

  it('rejects an expense with invalid dates', () => {
    expect(parseExpense(expenseBody({ dueDate: '10/08/2026' }))).toBeNull();
  });
});
