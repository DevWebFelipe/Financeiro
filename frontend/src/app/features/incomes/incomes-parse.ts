import { isRecord } from '../../core/errors/api-error';
import { Income, IncomeMovement, IncomeMovementPage, IncomePage } from './incomes.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function parseIncomePage(body: unknown): IncomePage | null {
  if (!isRecord(body)) {
    return null;
  }

  const items = parseIncomeList(body['items']);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);

  if (items == null || page == null || size == null || totalItems == null || totalPages == null) {
    return null;
  }

  return { items, page, size, totalItems, totalPages };
}

export function parseIncomeList(body: unknown): Income[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: Income[] = [];
  for (const item of body) {
    const parsed = parseIncome(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseIncome(value: unknown): Income | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const categoryId = parseId(value['categoryId']);
  const accountId = parseNullableId(value['accountId']);
  const description = typeof value['description'] === 'string' ? value['description'] : null;
  const amount = parseMoney(value['amount']);
  const expectedDate = parseIsoDate(value['expectedDate']);
  const receivedDate = parseNullableIsoDate(value['receivedDate']);
  const status = parseString(value['status']);
  const responsibleType = parseNullableString(value['responsibleType']);
  const responsibleName = parseNullableString(value['responsibleName']);
  const notes = parseNullableString(value['notes']);
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    categoryId == null ||
    description == null ||
    amount == null ||
    expectedDate == null ||
    status == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return {
    id,
    categoryId,
    accountId,
    description,
    amount,
    expectedDate,
    receivedDate,
    status: status as Income['status'],
    responsibleType: (responsibleType as Income['responsibleType']) ?? null,
    responsibleName,
    notes,
    createdAt,
    updatedAt,
  };
}

export function parseIncomeMovementPage(body: unknown): IncomeMovementPage | null {
  if (!isRecord(body)) {
    return null;
  }

  const items = parseIncomeMovementList(body['items']);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);

  if (items == null || page == null || size == null || totalItems == null || totalPages == null) {
    return null;
  }

  return { items, page, size, totalItems, totalPages };
}

export function parseIncomeMovementList(body: unknown): IncomeMovement[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: IncomeMovement[] = [];
  for (const item of body) {
    const parsed = parseIncomeMovement(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseIncomeMovement(value: unknown): IncomeMovement | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const incomeId = parseId(value['incomeId']);
  const type = parseString(value['type']);
  const status = parseString(value['status']);
  const amount = parseMoney(value['amount']);
  const movementDate = parseIsoDate(value['movementDate']);
  const accountId = parseNullableId(value['accountId']);
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);
  const reversedAt = parseNullableInstant(value['reversedAt']);

  if (
    id == null ||
    incomeId == null ||
    type == null ||
    status == null ||
    amount == null ||
    movementDate == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return {
    id,
    incomeId,
    type: type as IncomeMovement['type'],
    status: status as IncomeMovement['status'],
    amount,
    movementDate,
    accountId,
    createdAt,
    updatedAt,
    reversedAt,
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

function parseNullableIsoDate(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  return parseIsoDate(value);
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseNullableInstant(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  return parseInstant(value);
}
