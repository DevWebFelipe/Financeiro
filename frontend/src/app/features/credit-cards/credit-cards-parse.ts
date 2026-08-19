import { isRecord } from '../../core/errors/api-error';
import {
  CreditCard,
  CreditCardCredit,
  CreditCardCreditOrigin,
  CreditCardLimit,
} from './credit-cards.models';

export function parseCreditCardList(body: unknown): CreditCard[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const cards: CreditCard[] = [];
  for (const item of body) {
    const parsed = parseCreditCard(item);
    if (parsed == null) {
      return null;
    }
    cards.push(parsed);
  }
  return cards;
}

export function parseCreditCard(value: unknown): CreditCard | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const name = typeof value['name'] === 'string' ? value['name'] : null;
  const holderName = typeof value['holderName'] === 'string' ? value['holderName'] : null;
  const lastFourDigits = parseNullableString(value['lastFourDigits']);
  const creditLimit = parseMoney(value['creditLimit']);
  const closingDay = parseDay(value['closingDay']);
  const dueDay = parseDay(value['dueDay']);
  const active = typeof value['active'] === 'boolean' ? value['active'] : null;
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    name == null ||
    holderName == null ||
    creditLimit == null ||
    closingDay == null ||
    dueDay == null ||
    active == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return {
    id,
    name,
    holderName,
    lastFourDigits,
    creditLimit,
    closingDay,
    dueDay,
    active,
    createdAt,
    updatedAt,
  };
}

export function parseCreditCardLimit(value: unknown): CreditCardLimit | null {
  if (!isRecord(value)) {
    return null;
  }

  const creditLimit = parseMoney(value['creditLimit']);
  const usedLimit = parseMoney(value['usedLimit']);
  const availableLimit = parseMoney(value['availableLimit']);

  if (creditLimit == null || usedLimit == null || availableLimit == null) {
    return null;
  }

  return { creditLimit, usedLimit, availableLimit };
}

export function parseCreditCardCreditList(body: unknown): CreditCardCredit[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const credits: CreditCardCredit[] = [];
  for (const item of body) {
    const parsed = parseCreditCardCredit(item);
    if (parsed == null) {
      return null;
    }
    credits.push(parsed);
  }
  return credits;
}

export function parseCreditCardCredit(value: unknown): CreditCardCredit | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const creditCardId = parseId(value['creditCardId']);
  const amount = parseMoney(value['amount']);
  const remainingAmount = parseMoney(value['remainingAmount']);
  const reason =
    typeof value['reason'] === 'string' && value['reason'].length > 0 ? value['reason'] : null;
  const origin = parseOrigin(value['origin']);
  const expenseId = parseOptionalId(value['expenseId']);
  const createdAt = parseInstant(value['createdAt']);

  if (
    id == null ||
    creditCardId == null ||
    amount == null ||
    remainingAmount == null ||
    reason == null ||
    origin == null ||
    expenseId === undefined ||
    createdAt == null
  ) {
    return null;
  }

  return {
    id,
    creditCardId,
    amount,
    remainingAmount,
    reason,
    origin,
    expenseId,
    createdAt,
  };
}

function parseOrigin(value: unknown): CreditCardCreditOrigin | null {
  return value === 'MANUAL' || value === 'CARD_PURCHASE_REFUND' ? value : null;
}

function parseOptionalId(value: unknown): string | null | undefined {
  if (value == null) {
    return null;
  }
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseNullableString(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseMoney(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseDay(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 && value <= 31
    ? value
    : null;
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}
