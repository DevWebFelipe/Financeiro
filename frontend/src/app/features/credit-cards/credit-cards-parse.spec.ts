import {
  parseCreditCard,
  parseCreditCardCredit,
  parseCreditCardCreditList,
  parseCreditCardLimit,
  parseCreditCardList,
} from './credit-cards-parse';

const CARD_ID = '01900000-0000-7000-8000-000000000040';

function creditResponse(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: CARD_ID,
    creditCardId: CARD_ID,
    amount: 100,
    remainingAmount: 40,
    reason: 'Ajuste comercial',
    origin: 'MANUAL',
    expenseId: null,
    createdAt: '2026-08-20T12:00:00Z',
    ...overrides,
  };
}

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

  it('parses MANUAL and CARD_PURCHASE_REFUND credits including nullable expenseId', () => {
    const manual = parseCreditCardCredit(creditResponse());
    expect(manual).toEqual(
      expect.objectContaining({
        amount: 100,
        remainingAmount: 40,
        origin: 'MANUAL',
        expenseId: null,
      }),
    );

    const refund = parseCreditCardCredit(
      creditResponse({
        amount: 50,
        remainingAmount: 0,
        reason: 'Estorno',
        origin: 'CARD_PURCHASE_REFUND',
        expenseId: CARD_ID,
      }),
    );
    expect(refund?.origin).toBe('CARD_PURCHASE_REFUND');
    expect(refund?.expenseId).toBe(CARD_ID);
    expect(refund?.remainingAmount).toBe(0);
  });

  it('parses a credit list and rejects invented origins', () => {
    expect(
      parseCreditCardCreditList([creditResponse({ amount: 10, remainingAmount: 10 })]),
    ).toHaveLength(1);
    expect(parseCreditCardCreditList({ items: [] })).toBeNull();
    expect(parseCreditCardCredit(creditResponse({ origin: 'PROMO' }))).toBeNull();
  });

  it('rejects a credit when a required field is missing or invalid', () => {
    expect(parseCreditCardCredit(creditResponse({ id: '' }))).toBeNull();
    expect(parseCreditCardCredit(creditResponse({ creditCardId: '' }))).toBeNull();
    expect(parseCreditCardCredit(creditResponse({ amount: '100' }))).toBeNull();
    expect(parseCreditCardCredit(creditResponse({ remainingAmount: undefined }))).toBeNull();
    expect(parseCreditCardCredit(creditResponse({ reason: '' }))).toBeNull();
    expect(parseCreditCardCredit(creditResponse({ origin: undefined }))).toBeNull();
    expect(parseCreditCardCredit(creditResponse({ createdAt: '' }))).toBeNull();
  });
});
