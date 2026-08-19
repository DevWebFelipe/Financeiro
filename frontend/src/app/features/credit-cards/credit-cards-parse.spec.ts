import { parseCreditCard, parseCreditCardLimit, parseCreditCardList } from './credit-cards-parse';

const CARD_ID = '01900000-0000-7000-8000-000000000040';

function cardBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: CARD_ID,
    name: 'Nubank',
    holderName: 'Ederson',
    lastFourDigits: '1234',
    creditLimit: 5000,
    closingDay: 10,
    dueDay: 20,
    active: true,
    createdAt: '2026-08-13T12:00:00Z',
    updatedAt: '2026-08-13T12:00:00Z',
    ...overrides,
  };
}

describe('credit-cards-parse', () => {
  it('parses a valid credit card list', () => {
    const parsed = parseCreditCardList([cardBody(), cardBody({ lastFourDigits: null })]);
    expect(parsed).toHaveLength(2);
    expect(parsed?.[0]?.lastFourDigits).toBe('1234');
    expect(parsed?.[1]?.lastFourDigits).toBeNull();
  });

  it('returns null for a non-array list', () => {
    expect(parseCreditCardList({ id: CARD_ID })).toBeNull();
  });

  it('returns null when a list item is incomplete', () => {
    expect(parseCreditCardList([cardBody({ creditLimit: '5000' })])).toBeNull();
  });

  it('parses official limit fields including a negative availableLimit', () => {
    const parsed = parseCreditCardLimit({
      creditLimit: 5000,
      usedLimit: 6200,
      availableLimit: -1200,
    });
    expect(parsed).toEqual({
      creditLimit: 5000,
      usedLimit: 6200,
      availableLimit: -1200,
    });
  });

  it('rejects a limit payload that omits availableLimit', () => {
    expect(parseCreditCardLimit({ creditLimit: 5000, usedLimit: 100 })).toBeNull();
  });
});
