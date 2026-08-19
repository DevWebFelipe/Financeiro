import { parseIncome, parseIncomeMovement, parseIncomePage } from './incomes-parse';

const INCOME_ID = '01900000-0000-7000-8000-000000000020';
const MOVEMENT_ID = '01900000-0000-7000-8000-000000000021';

function incomeBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: INCOME_ID,
    categoryId: '01900000-0000-7000-8000-000000000002',
    accountId: null,
    description: 'Salário',
    amount: 5400,
    expectedDate: '2026-08-05',
    receivedDate: null,
    status: 'EXPECTED',
    responsibleType: null,
    responsibleName: null,
    notes: null,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

describe('incomes parse', () => {
  it('parses a valid income page', () => {
    const parsed = parseIncomePage({
      items: [incomeBody()],
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
    expect(parsed?.items[0]?.description).toBe('Salário');
    expect(parsed?.items[0]?.status).toBe('EXPECTED');
  });

  it('parses a valid income with optional responsible fields', () => {
    const parsed = parseIncome(incomeBody({ responsibleType: 'MINE' }));
    expect(parsed).toMatchObject({
      id: INCOME_ID,
      amount: 5400,
      expectedDate: '2026-08-05',
      responsibleType: 'MINE',
    });
  });

  it('parses a valid movement', () => {
    const parsed = parseIncomeMovement({
      id: MOVEMENT_ID,
      incomeId: INCOME_ID,
      type: 'RECEIPT',
      status: 'ACTIVE',
      amount: 5400,
      movementDate: '2026-08-05',
      accountId: '01900000-0000-7000-8000-000000000003',
      createdAt: '2026-08-14T12:00:00Z',
      updatedAt: '2026-08-14T12:00:00Z',
      reversedAt: null,
    });

    expect(parsed).toMatchObject({
      type: 'RECEIPT',
      status: 'ACTIVE',
      amount: 5400,
    });
  });

  it('rejects an income page missing required fields', () => {
    expect(parseIncomePage({ items: [incomeBody()], page: 0 })).toBeNull();
  });

  it('rejects an income with invalid expectedDate', () => {
    expect(parseIncome(incomeBody({ expectedDate: '05/08/2026' }))).toBeNull();
  });
});
