import { parseProjectionEvent, parseProjectionResponse } from './projections-parse';

const SOURCE_ID = '01900000-0000-7000-8000-000000000050';

function summaryBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    currentBalance: 1000,
    projectedFinalBalance: -3000,
    projectedIncome: 1000,
    projectedExpense: 5000,
    projectedNetCashFlow: -4000,
    minimumProjectedBalance: -3000,
    minimumProjectedBalanceDate: '2026-08-25',
    reservedAmount: 200,
    availableProjectedBalance: -3200,
    ...overrides,
  };
}

function monthBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    period: '2026-08',
    openingBalance: 1000,
    totalIncome: 1000,
    totalExpense: 5000,
    netCashFlow: -4000,
    closingBalance: -3000,
    minimumProjectedBalance: -3000,
    minimumProjectedBalanceDate: '2026-08-25',
    negative: true,
    reservedAmount: 200,
    availableProjectedBalance: -3200,
    ...overrides,
  };
}

function quarterBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    period: '2026-Q4',
    months: ['2026-10', '2026-11', '2026-12'],
    totalIncome: 15,
    totalExpense: 3,
    netCashFlow: 12,
    openingBalance: 0,
    closingBalance: 12,
    ...overrides,
  };
}

function eventBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    date: '2026-08-20',
    type: 'EXPENSE',
    description: 'Aluguel',
    amount: 1500,
    direction: 'OUT',
    sourceId: SOURCE_ID,
    sourceType: 'EXPENSE',
    overdue: false,
    accountAssignment: 'UNASSIGNED',
    ...overrides,
  };
}

function responseBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    startDate: '2026-08-17',
    endDate: '2026-08-31',
    summary: summaryBody(),
    months: [monthBody()],
    quarters: [quarterBody()],
    events: {
      items: [eventBody()],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    },
    undatedEvents: [eventBody({ date: null, description: 'Sem vencimento' })],
    ...overrides,
  };
}

describe('projections parse', () => {
  it('parses a nested official envelope including events pagination', () => {
    const parsed = parseProjectionResponse(responseBody());

    expect(parsed).toMatchObject({
      startDate: '2026-08-17',
      endDate: '2026-08-31',
      summary: {
        projectedNetCashFlow: -4000,
        reservedAmount: 200,
      },
      events: { page: 0, size: 20, totalItems: 1, totalPages: 1 },
    });
    expect(parsed?.months[0]?.negative).toBe(true);
    expect(parsed?.quarters[0]?.months).toEqual(['2026-10', '2026-11', '2026-12']);
    expect(parsed?.events.items[0]?.type).toBe('EXPENSE');
    expect(parsed?.undatedEvents[0]?.date).toBeNull();
  });

  it('parses an event with a nullable date without fabricating a value', () => {
    const parsed = parseProjectionEvent(eventBody({ date: null }));
    expect(parsed?.date).toBeNull();
    expect(parsed?.description).toBe('Aluguel');
  });

  it('accepts official event enums', () => {
    expect(
      parseProjectionEvent(eventBody({ type: 'INCOME', sourceType: 'INCOME', direction: 'IN' })),
    ).not.toBeNull();
    expect(
      parseProjectionEvent(
        eventBody({ type: 'CREDIT_CARD_INVOICE', sourceType: 'CREDIT_CARD_INVOICE' }),
      ),
    ).not.toBeNull();
    expect(
      parseProjectionEvent(eventBody({ type: 'TRANSFER', sourceType: 'TRANSFER' })),
    ).not.toBeNull();
  });

  it('rejects invalid event type, direction and accountAssignment', () => {
    expect(parseProjectionEvent(eventBody({ type: 'PAYMENT' }))).toBeNull();
    expect(parseProjectionEvent(eventBody({ direction: 'DEBIT' }))).toBeNull();
    expect(parseProjectionEvent(eventBody({ accountAssignment: 'ASSIGNED' }))).toBeNull();
    expect(parseProjectionEvent(eventBody({ sourceType: 'INVOICE' }))).toBeNull();
  });

  it('rejects an invalid or datetime event date', () => {
    expect(parseProjectionEvent(eventBody({ date: '20/08/2026' }))).toBeNull();
    expect(parseProjectionEvent(eventBody({ date: '2026-08-20T00:00:00' }))).toBeNull();
  });

  it('rejects a nested month, quarter or events page that breaks the contract', () => {
    expect(
      parseProjectionResponse(responseBody({ months: [monthBody({ negative: 'yes' })] })),
    ).toBeNull();
    expect(
      parseProjectionResponse(responseBody({ quarters: [quarterBody({ months: ['2026-13'] })] })),
    ).toBeNull();
    expect(
      parseProjectionResponse(
        responseBody({
          events: { items: [eventBody()], page: 0, size: 20, totalItems: 1 },
        }),
      ),
    ).toBeNull();
  });

  it('rejects an envelope missing undatedEvents or summary money fields', () => {
    const withoutUndated = responseBody();
    delete withoutUndated['undatedEvents'];
    expect(parseProjectionResponse(withoutUndated)).toBeNull();

    expect(
      parseProjectionResponse(
        responseBody({ summary: summaryBody({ reservedAmount: undefined }) }),
      ),
    ).toBeNull();
    expect(parseProjectionResponse({ startDate: '2026-08-17' })).toBeNull();
    expect(parseProjectionResponse(null)).toBeNull();
  });

  it('parses empty months, quarters and event lists', () => {
    const parsed = parseProjectionResponse(
      responseBody({
        months: [],
        quarters: [],
        events: { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 },
        undatedEvents: [],
      }),
    );

    expect(parsed?.months).toEqual([]);
    expect(parsed?.quarters).toEqual([]);
    expect(parsed?.events.items).toEqual([]);
    expect(parsed?.undatedEvents).toEqual([]);
  });
});
