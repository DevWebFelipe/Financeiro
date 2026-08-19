import { isRecord } from '../../core/errors/api-error';
import { Expense, ExpenseInstallment, ExpensePage } from './expenses.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function parseExpensePage(body: unknown): ExpensePage | null {
  if (!isRecord(body)) {
    return null;
  }

  const items = parseExpenseList(body['items']);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);

  if (items == null || page == null || size == null || totalItems == null || totalPages == null) {
    return null;
  }

  return { items, page, size, totalItems, totalPages };
}

export function parseExpenseList(body: unknown): Expense[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: Expense[] = [];
  for (const item of body) {
    const parsed = parseExpense(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseExpense(value: unknown): Expense | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const categoryId = parseId(value['categoryId']);
  const accountId = parseNullableId(value['accountId']);
  const creditCardId = parseNullableId(value['creditCardId']);
  const description = typeof value['description'] === 'string' ? value['description'] : null;
  const totalAmount = parseMoney(value['totalAmount']);
  const expenseDate = parseIsoDate(value['expenseDate']);
  const dueDate = parseIsoDate(value['dueDate']);
  const paymentMethod = parseString(value['paymentMethod']);
  const status = parseString(value['status']);
  const responsibleType = parseString(value['responsibleType']);
  const responsibleName = parseNullableString(value['responsibleName']);
  const barcode = parseNullableString(value['barcode']);
  const notes = parseNullableString(value['notes']);
  const overdue = typeof value['overdue'] === 'boolean' ? value['overdue'] : null;
  const installmentId = parseId(value['installmentId']);
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    categoryId == null ||
    description == null ||
    totalAmount == null ||
    expenseDate == null ||
    dueDate == null ||
    paymentMethod == null ||
    status == null ||
    responsibleType == null ||
    overdue == null ||
    installmentId == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return {
    id,
    categoryId,
    accountId,
    creditCardId,
    description,
    totalAmount,
    expenseDate,
    dueDate,
    paymentMethod: paymentMethod as Expense['paymentMethod'],
    status: status as Expense['status'],
    responsibleType: responsibleType as Expense['responsibleType'],
    responsibleName,
    barcode,
    notes,
    overdue,
    installmentId,
    createdAt,
    updatedAt,
  };
}

export function parseInstallment(value: unknown): ExpenseInstallment | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const expenseId = parseId(value['expenseId']);
  const installmentNumber = parseCount(value['installmentNumber']);
  const totalInstallments = parseCount(value['totalInstallments']);
  const amount = parseMoney(value['amount']);
  const remainingAmount = parseMoney(value['remainingAmount']);
  const dueDate = parseIsoDate(value['dueDate']);
  const status = parseString(value['status']);
  const overdue = typeof value['overdue'] === 'boolean' ? value['overdue'] : null;
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    expenseId == null ||
    installmentNumber == null ||
    totalInstallments == null ||
    amount == null ||
    remainingAmount == null ||
    dueDate == null ||
    status == null ||
    overdue == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return {
    id,
    expenseId,
    installmentNumber,
    totalInstallments,
    amount,
    remainingAmount,
    dueDate,
    status: status as ExpenseInstallment['status'],
    overdue,
    createdAt,
    updatedAt,
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

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}
