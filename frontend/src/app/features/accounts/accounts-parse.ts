import { isRecord } from '../../core/errors/api-error';
import { Account, AccountBalance } from './accounts.models';

export function parseAccountList(body: unknown): Account[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const accounts: Account[] = [];
  for (const item of body) {
    const parsed = parseAccount(item);
    if (parsed == null) {
      return null;
    }
    accounts.push(parsed);
  }
  return accounts;
}

export function parseAccount(value: unknown): Account | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const name = typeof value['name'] === 'string' ? value['name'] : null;
  const type = parseAccountType(value['type']);
  const initialBalance = parseMoney(value['initialBalance']);
  const active = typeof value['active'] === 'boolean' ? value['active'] : null;
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    name == null ||
    type == null ||
    initialBalance == null ||
    active == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return { id, name, type, initialBalance, active, createdAt, updatedAt };
}

export function parseAccountBalance(value: unknown): AccountBalance | null {
  if (!isRecord(value)) {
    return null;
  }

  const accountId = parseId(value['accountId']);
  const totalBalance = parseMoney(value['totalBalance']);
  const reservedAmount = parseMoney(value['reservedAmount']);
  const availableBalance = parseMoney(value['availableBalance']);

  if (
    accountId == null ||
    totalBalance == null ||
    reservedAmount == null ||
    availableBalance == null
  ) {
    return null;
  }

  return { accountId, totalBalance, reservedAmount, availableBalance };
}

function parseAccountType(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseMoney(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}
