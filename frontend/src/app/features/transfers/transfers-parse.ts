import { isRecord } from '../../core/errors/api-error';
import { Transfer, TransferStatus } from './transfers.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function parseTransferList(body: unknown): Transfer[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: Transfer[] = [];
  for (const item of body) {
    const parsed = parseTransfer(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseTransfer(value: unknown): Transfer | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const sourceAccountId = parseId(value['sourceAccountId']);
  const destinationAccountId = parseId(value['destinationAccountId']);
  const amount = parseMoney(value['amount']);
  const transferDate = parseIsoDate(value['transferDate']);
  const description = parseNullableString(value['description']);
  const status = parseStatus(value['status']);
  const createdAt = parseInstant(value['createdAt']);

  if (
    id == null ||
    sourceAccountId == null ||
    destinationAccountId == null ||
    amount == null ||
    transferDate == null ||
    status == null ||
    createdAt == null
  ) {
    return null;
  }

  return {
    id,
    sourceAccountId,
    destinationAccountId,
    amount,
    transferDate,
    description,
    status,
    createdAt,
  };
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseNullableString(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  return typeof value === 'string' ? value : null;
}

function parseMoney(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseIsoDate(value: unknown): string | null {
  return typeof value === 'string' && ISO_DATE.test(value) ? value : null;
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseStatus(value: unknown): TransferStatus | null {
  return value === 'ACTIVE' || value === 'REVERSED' ? value : null;
}
