import { canEditExpense } from './expenses-format';
import { Expense } from './expenses.models';

function expense(overrides: Partial<Expense> = {}): Expense {
  return {
    id: '01900000-0000-7000-8000-000000000010',
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
    installmentId: '01900000-0000-7000-8000-000000000011',
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

describe('canEditExpense', () => {
  it('allows editing ACCOUNT expenses that are OPEN', () => {
    expect(canEditExpense(expense({ paymentMethod: 'ACCOUNT', status: 'OPEN' }))).toBe(true);
  });

  it('allows editing NONE expenses that are OPEN', () => {
    expect(
      canEditExpense(expense({ paymentMethod: 'NONE', accountId: null, status: 'OPEN' })),
    ).toBe(true);
  });

  it('does not allow editing CREDIT_CARD expenses even when OPEN', () => {
    expect(canEditExpense(expense({ paymentMethod: 'CREDIT_CARD', status: 'OPEN' }))).toBe(false);
  });

  it('does not allow editing expenses that are not OPEN', () => {
    expect(canEditExpense(expense({ status: 'PARTIALLY_PAID' }))).toBe(false);
    expect(canEditExpense(expense({ status: 'PAID' }))).toBe(false);
    expect(canEditExpense(expense({ status: 'CANCELLED' }))).toBe(false);
    expect(canEditExpense(expense({ status: 'REFUNDED' }))).toBe(false);
  });
});
