import { isRecord } from '../../core/errors/api-error';
import { CreditCard, CreditCardLimit } from './credit-cards.models';

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
