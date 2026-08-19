import { isRecord } from '../../core/errors/api-error';
import { PayableItem, PayablePage } from './payables.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function parsePayablePage(body: unknown): PayablePage | null {
  if (!isRecord(body)) {
    return null;
  }

  const items = parsePayableList(body['items']);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);
  const totalRemaining = parseMoney(body['totalRemaining']);
  const totalOriginal = parseMoney(body['totalOriginal']);
  const totalPaid = parseMoney(body['totalPaid']);

  if (
    items == null ||
    page == null ||
    size == null ||
    totalItems == null ||
    totalPages == null ||
    totalRemaining == null ||
    totalOriginal == null ||
    totalPaid == null
  ) {
    return null;
  }

  return {
    items,
    page,
    size,
    totalItems,
    totalPages,
    totalRemaining,
    totalOriginal,
    totalPaid,
  };
}

export function parsePayableList(body: unknown): PayableItem[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: PayableItem[] = [];
  for (const item of body) {
    const parsed = parsePayableItem(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parsePayableItem(value: unknown): PayableItem | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const type = parseString(value['type']);
  const expenseId = parseNullableId(value['expenseId']);
  const creditCardId = parseNullableId(value['creditCardId']);
  const categoryId = parseNullableId(value['categoryId']);
  const accountId = parseNullableId(value['accountId']);
  const paymentMethod = parseNullableString(value['paymentMethod']);
  const name = typeof value['name'] === 'string' ? value['name'] : null;
  if (value['purchaseDate'] != null && parseIsoDate(value['purchaseDate']) == null) {
    return null;
  }
  if (value['dueDate'] != null && parseIsoDate(value['dueDate']) == null) {
    return null;
  }
  const purchaseDate = parseIsoDate(value['purchaseDate']);
  const dueDate = parseIsoDate(value['dueDate']);
  const originalAmount = parseMoney(value['originalAmount']);
  const paidAmount = parseMoney(value['paidAmount']);
  const remainingAmount = parseMoney(value['remainingAmount']);
  const status = parseString(value['status']);
  const overdue = typeof value['overdue'] === 'boolean' ? value['overdue'] : null;
  const responsibleType = parseNullableString(value['responsibleType']);
  const responsibleName = parseNullableString(value['responsibleName']);

  if (
    id == null ||
    type == null ||
    name == null ||
    originalAmount == null ||
    paidAmount == null ||
    remainingAmount == null ||
    status == null ||
    overdue == null
  ) {
    return null;
  }

  return {
    id,
    type: type as PayableItem['type'],
    expenseId,
    creditCardId,
    categoryId,
    accountId,
    paymentMethod: (paymentMethod as PayableItem['paymentMethod']) ?? null,
    name,
    purchaseDate,
    dueDate,
    originalAmount,
    paidAmount,
    remainingAmount,
    status,
    overdue,
    responsibleType: (responsibleType as PayableItem['responsibleType']) ?? null,
    responsibleName,
  };
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseNullableId(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  return parseId(value);
}

function parseString(value: unknown): string | null {
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

function parseCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : null;
}

function parseLongCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : null;
}

function parseIsoDate(value: unknown): string | null {
  return typeof value === 'string' && ISO_DATE.test(value) ? value : null;
}
