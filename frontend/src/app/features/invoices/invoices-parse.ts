import { isRecord } from '../../core/errors/api-error';
import {
  Invoice,
  InvoiceAdjustment,
  InvoiceAdjustmentStatus,
  InvoiceAdjustmentType,
  InvoiceAgreement,
  InvoiceAgreementInstallment,
  InvoiceAgreementInstallmentStatus,
  InvoiceAgreementStatus,
  InvoiceItem,
  InvoicePayment,
  InvoiceStatus,
} from './invoices.models';

const INVOICE_STATUSES = new Set<InvoiceStatus>([
  'SCHEDULED',
  'OPEN',
  'CLOSED',
  'PAID',
  'SETTLED_BY_AGREEMENT',
]);

export function parseInvoiceList(body: unknown): Invoice[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const invoices: Invoice[] = [];
  for (const item of body) {
    const parsed = parseInvoice(item);
    if (parsed == null) {
      return null;
    }
    invoices.push(parsed);
  }
  return invoices;
}

export function parseInvoice(value: unknown): Invoice | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const creditCardId = parseId(value['creditCardId']);
  const referenceYear = parseYear(value['referenceYear']);
  const referenceMonth = parseMonth(value['referenceMonth']);
  const closingDate = parseIsoDate(value['closingDate']);
  const dueDate = parseIsoDate(value['dueDate']);
  const status = parseInvoiceStatus(value['status']);
  const totalAmount = parseMoney(value['totalAmount']);
  const paidAmount = parseMoney(value['paidAmount']);
  const remainingAmount = parseMoney(value['remainingAmount']);
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    creditCardId == null ||
    referenceYear == null ||
    referenceMonth == null ||
    closingDate == null ||
    dueDate == null ||
    status == null ||
    totalAmount == null ||
    paidAmount == null ||
    remainingAmount == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return {
    id,
    creditCardId,
    referenceYear,
    referenceMonth,
    closingDate,
    dueDate,
    status,
    totalAmount,
    paidAmount,
    remainingAmount,
    createdAt,
    updatedAt,
  };
}

export function parseInvoiceItemList(body: unknown): InvoiceItem[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: InvoiceItem[] = [];
  for (const item of body) {
    const parsed = parseInvoiceItem(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseInvoiceItem(value: unknown): InvoiceItem | null {
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
  const status =
    typeof value['status'] === 'string' && value['status'].length > 0 ? value['status'] : null;
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
    status,
    overdue,
    createdAt,
    updatedAt,
  };
}

export function parseInvoicePaymentList(body: unknown): InvoicePayment[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const payments: InvoicePayment[] = [];
  for (const item of body) {
    const parsed = parseInvoicePayment(item);
    if (parsed == null) {
      return null;
    }
    payments.push(parsed);
  }
  return payments;
}

export function parseInvoicePayment(value: unknown): InvoicePayment | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const invoiceId = parseId(value['invoiceId']);
  const accountId = parseId(value['accountId']);
  const amount = parseMoney(value['amount']);
  const paymentDate = parseIsoDate(value['paymentDate']);
  const notes = parseNullableString(value['notes']);
  const status = parsePaymentStatus(value['status']);
  const createdAt = parseInstant(value['createdAt']);

  if (
    id == null ||
    invoiceId == null ||
    accountId == null ||
    amount == null ||
    paymentDate == null ||
    notes === undefined ||
    status == null ||
    createdAt == null
  ) {
    return null;
  }

  return {
    id,
    invoiceId,
    accountId,
    amount,
    paymentDate,
    notes,
    status,
    createdAt,
  };
}

export function parseInvoiceAdjustmentList(body: unknown): InvoiceAdjustment[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const adjustments: InvoiceAdjustment[] = [];
  for (const item of body) {
    const parsed = parseInvoiceAdjustment(item);
    if (parsed == null) {
      return null;
    }
    adjustments.push(parsed);
  }
  return adjustments;
}

export function parseInvoiceAdjustment(value: unknown): InvoiceAdjustment | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const invoiceId = parseId(value['invoiceId']);
  const type = parseAdjustmentType(value['type']);
  const amount = parseMoney(value['amount']);
  const reason = parseRequiredString(value['reason']);
  const status = parseAdjustmentStatus(value['status']);
  const createdAt = parseInstant(value['createdAt']);

  if (
    id == null ||
    invoiceId == null ||
    type == null ||
    amount == null ||
    reason == null ||
    status == null ||
    createdAt == null
  ) {
    return null;
  }

  return {
    id,
    invoiceId,
    type,
    amount,
    reason,
    status,
    createdAt,
  };
}

function parsePaymentStatus(value: unknown): 'ACTIVE' | 'REVERSED' | null {
  return value === 'ACTIVE' || value === 'REVERSED' ? value : null;
}

function parseAdjustmentType(value: unknown): InvoiceAdjustmentType | null {
  return value === 'DISCOUNT' || value === 'SURCHARGE' ? value : null;
}

function parseAdjustmentStatus(value: unknown): InvoiceAdjustmentStatus | null {
  return value === 'ACTIVE' || value === 'REVERSED' ? value : null;
}

function parseRequiredString(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseNullableString(value: unknown): string | null | undefined {
  if (value == null) {
    return null;
  }
  return typeof value === 'string' ? value : undefined;
}

function parseInvoiceStatus(value: unknown): InvoiceStatus | null {
  return typeof value === 'string' && INVOICE_STATUSES.has(value as InvoiceStatus)
    ? (value as InvoiceStatus)
    : null;
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseYear(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 ? value : null;
}

function parseMonth(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 && value <= 12
    ? value
    : null;
}

function parseCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 ? value : null;
}

function parseMoney(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseIsoDate(value: unknown): string | null {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : null;
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

export function parseInvoiceAgreementList(body: unknown): InvoiceAgreement[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const agreements: InvoiceAgreement[] = [];
  for (const item of body) {
    const parsed = parseInvoiceAgreement(item);
    if (parsed == null) {
      return null;
    }
    agreements.push(parsed);
  }
  return agreements;
}

export function parseInvoiceAgreement(value: unknown): InvoiceAgreement | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const creditCardId = parseId(value['creditCardId']);
  const sourceInvoiceId = parseId(value['sourceInvoiceId']);
  const expenseId = parseId(value['expenseId']);
  const status = parseAgreementStatus(value['status']);
  const entryAmount = parseMoney(value['entryAmount']);
  const financedAmount = parseMoney(value['financedAmount']);
  const installmentCount = parseCount(value['installmentCount']);
  const installmentAmount = parseMoney(value['installmentAmount']);
  const contractedTotal = parseMoney(value['contractedTotal']);
  const additionalCost = parseMoney(value['additionalCost']);
  const additionalCostPercent = parseMoney(value['additionalCostPercent']);
  const createdAt = parseInstant(value['createdAt']);
  const supersededByAgreementId = parseOptionalId(value['supersededByAgreementId']);
  const installments = parseAgreementInstallmentList(value['installments']);

  if (
    id == null ||
    creditCardId == null ||
    sourceInvoiceId == null ||
    expenseId == null ||
    status == null ||
    entryAmount == null ||
    financedAmount == null ||
    installmentCount == null ||
    installmentAmount == null ||
    contractedTotal == null ||
    additionalCost == null ||
    additionalCostPercent == null ||
    createdAt == null ||
    supersededByAgreementId === undefined ||
    installments == null
  ) {
    return null;
  }

  return {
    id,
    creditCardId,
    sourceInvoiceId,
    expenseId,
    status,
    entryAmount,
    financedAmount,
    installmentCount,
    installmentAmount,
    contractedTotal,
    additionalCost,
    additionalCostPercent,
    createdAt,
    supersededByAgreementId,
    installments,
  };
}

function parseAgreementInstallmentList(body: unknown): InvoiceAgreementInstallment[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const installments: InvoiceAgreementInstallment[] = [];
  for (const item of body) {
    const parsed = parseAgreementInstallment(item);
    if (parsed == null) {
      return null;
    }
    installments.push(parsed);
  }
  return installments;
}

function parseAgreementInstallment(value: unknown): InvoiceAgreementInstallment | null {
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
  const status = parseAgreementInstallmentStatus(value['status']);
  const invoiceId = parseOptionalId(value['invoiceId']);
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
    invoiceId === undefined ||
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
    status,
    invoiceId,
    createdAt,
    updatedAt,
  };
}

function parseAgreementStatus(value: unknown): InvoiceAgreementStatus | null {
  return value === 'ACTIVE' ||
    value === 'COMPLETED' ||
    value === 'RENEGOTIATED' ||
    value === 'CANCELLED'
    ? value
    : null;
}

function parseAgreementInstallmentStatus(value: unknown): InvoiceAgreementInstallmentStatus | null {
  return value === 'OPEN' ||
    value === 'PARTIALLY_PAID' ||
    value === 'PAID' ||
    value === 'CANCELLED' ||
    value === 'REFUNDED'
    ? value
    : null;
}

function parseOptionalId(value: unknown): string | null | undefined {
  if (value == null) {
    return null;
  }
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}
