import { isRecord } from '../../core/errors/api-error';
import {
  parseInvoice,
  parseInvoiceAdjustment,
  parseInvoicePayment,
} from '../invoices/invoices-parse';
import {
  AdjustmentStatus,
  AdjustmentType,
  CardReportCreditApplication,
  CardReportItem,
  CardReportPurchase,
  CardReportPurchaseInstallment,
  CardReportResponse,
  CardReportSummary,
  CashFlowFlowType,
  CashFlowHistorical,
  CashFlowItem,
  CashFlowProjected,
  CashFlowResponse,
  CashFlowType,
  CategoryReportItem,
  CategoryReportResponse,
  CategoryType,
  ExpenseReportInstallment,
  ExpenseReportItem,
  ExpenseReportOrigin,
  ExpenseReportResponse,
  ExpenseReportSummary,
  ExpenseStatus,
  IncomeReportItem,
  IncomeReportResponse,
  IncomeReportSummary,
  IncomeStatus,
  InvoiceReportAllocation,
  InvoiceReportAllocationType,
  InvoiceReportCategoryGroup,
  InvoiceReportHeader,
  InvoiceReportPurchase,
  InvoiceReportResponse,
  InvoiceReportResponsibleGroup,
  InvoiceStatus,
  PaymentMethod,
  ProjectionMonth,
  ProjectionQuarter,
  ProjectionSummary,
  ReportDateType,
  ReportInstallmentAdjustment,
  ReportNature,
  ReportPeriod,
  ResponsibleReportItem,
  ResponsibleReportResponse,
  ResponsibleType,
} from './reports.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const YEAR_MONTH = /^\d{4}-\d{2}$/;

const EXPENSE_STATUSES = new Set<ExpenseStatus>([
  'OPEN',
  'PARTIALLY_PAID',
  'PAID',
  'CANCELLED',
  'REFUNDED',
]);
const PAYMENT_METHODS = new Set<PaymentMethod>(['ACCOUNT', 'CREDIT_CARD', 'NONE']);
const RESPONSIBLE_TYPES = new Set<ResponsibleType>([
  'MINE',
  'GIULIA',
  'EDERSON',
  'ELISIANE',
  'OTHER',
]);
const INCOME_STATUSES = new Set<IncomeStatus>(['EXPECTED', 'RECEIVED', 'CANCELLED']);
const CATEGORY_TYPES = new Set<CategoryType>(['INCOME', 'EXPENSE']);
const DATE_TYPES = new Set<ReportDateType>(['EXPECTED', 'RECEIVED']);
const NATURES = new Set<ReportNature>(['EXPENSE', 'INCOME', 'BOTH']);
const ORIGINS = new Set<ExpenseReportOrigin>(['PURCHASE', 'AGREEMENT']);
const FLOW_TYPES = new Set<CashFlowFlowType>(['HISTORICAL', 'PROJECTED', 'BOTH']);
const CASH_FLOW_TYPES = new Set<CashFlowType>([
  'INCOME_RECEIPT',
  'EXPENSE_PAYMENT',
  'INVOICE_PAYMENT',
  'CARD_PURCHASE_REFUND',
  'TRANSFER_IN',
  'TRANSFER_OUT',
  'BALANCE_ADJUSTMENT',
]);
const ALLOCATION_TYPES = new Set<InvoiceReportAllocationType>([
  'PAYMENT',
  'INVOICE_ADJUSTMENT',
  'CREDIT',
  'SETTLEMENT',
]);
const INVOICE_STATUSES = new Set<InvoiceStatus>([
  'SCHEDULED',
  'OPEN',
  'CLOSED',
  'PAID',
  'SETTLED_BY_AGREEMENT',
]);
const ADJUSTMENT_TYPES = new Set<AdjustmentType>(['DISCOUNT', 'SURCHARGE']);
const ADJUSTMENT_STATUSES = new Set<AdjustmentStatus>(['ACTIVE', 'REVERSED']);

export function parseExpenseReport(body: unknown): ExpenseReportResponse | null {
  if (!isRecord(body)) {
    return null;
  }
  const period = parsePeriod(body['period']);
  const items = parseList(body['items'], parseExpenseItem);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);
  const summary = parseExpenseSummary(body['summary']);
  if (
    period == null ||
    items == null ||
    page == null ||
    size == null ||
    totalItems == null ||
    totalPages == null ||
    summary == null
  ) {
    return null;
  }
  return { period, items, page, size, totalItems, totalPages, summary };
}

export function parseIncomeReport(body: unknown): IncomeReportResponse | null {
  if (!isRecord(body)) {
    return null;
  }
  const period = parsePeriod(body['period']);
  const dateType = parseEnum(body['dateType'], DATE_TYPES);
  const items = parseList(body['items'], parseIncomeItem);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);
  const summary = parseIncomeSummary(body['summary']);
  if (
    period == null ||
    dateType == null ||
    items == null ||
    page == null ||
    size == null ||
    totalItems == null ||
    totalPages == null ||
    summary == null
  ) {
    return null;
  }
  return { period, dateType, items, page, size, totalItems, totalPages, summary };
}

export function parseCategoryReport(body: unknown): CategoryReportResponse | null {
  if (!isRecord(body)) {
    return null;
  }
  const period = parsePeriod(body['period']);
  const dateType = parseEnum(body['dateType'], DATE_TYPES);
  const items = parseList(body['items'], parseCategoryItem);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);
  const summary = parseCategorySummary(body['summary']);
  if (
    period == null ||
    dateType == null ||
    items == null ||
    page == null ||
    size == null ||
    totalItems == null ||
    totalPages == null ||
    summary == null
  ) {
    return null;
  }
  return { period, dateType, items, page, size, totalItems, totalPages, summary };
}

export function parseResponsibleReport(body: unknown): ResponsibleReportResponse | null {
  if (!isRecord(body)) {
    return null;
  }
  const period = parsePeriod(body['period']);
  const nature = parseEnum(body['nature'], NATURES);
  const dateType = body['dateType'] == null ? null : parseEnum(body['dateType'], DATE_TYPES);
  const items = parseList(body['items'], parseResponsibleItem);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);
  const summary = parseResponsibleSummary(body['summary']);
  if (
    period == null ||
    nature == null ||
    dateType === undefined ||
    (body['dateType'] != null && dateType == null) ||
    items == null ||
    page == null ||
    size == null ||
    totalItems == null ||
    totalPages == null ||
    summary == null
  ) {
    return null;
  }
  return {
    period,
    nature,
    dateType,
    items,
    page,
    size,
    totalItems,
    totalPages,
    summary,
  };
}

export function parseCardReport(body: unknown): CardReportResponse | null {
  if (!isRecord(body)) {
    return null;
  }
  const period = parsePeriod(body['period']);
  const items = parseList(body['items'], parseCardItem);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);
  const summary = parseCardSummary(body['summary']);
  if (
    period == null ||
    items == null ||
    page == null ||
    size == null ||
    totalItems == null ||
    totalPages == null ||
    summary == null
  ) {
    return null;
  }
  return { period, items, page, size, totalItems, totalPages, summary };
}

export function parseCashFlowReport(body: unknown): CashFlowResponse | null {
  if (!isRecord(body)) {
    return null;
  }
  const period = parsePeriod(body['period']);
  const flowType = parseEnum(body['flowType'], FLOW_TYPES);
  const accountId = parseNullableId(body['accountId']);
  if (period == null || flowType == null || accountId === undefined) {
    return null;
  }

  let historical: CashFlowHistorical | undefined;
  if (Object.prototype.hasOwnProperty.call(body, 'historical') && body['historical'] != null) {
    const parsed = parseHistorical(body['historical']);
    if (parsed == null) {
      return null;
    }
    historical = parsed;
  }

  let projected: CashFlowProjected | undefined;
  if (Object.prototype.hasOwnProperty.call(body, 'projected') && body['projected'] != null) {
    const parsed = parseProjected(body['projected']);
    if (parsed == null) {
      return null;
    }
    projected = parsed;
  }

  return { period, flowType, accountId, historical, projected };
}

export function parseInvoiceReport(body: unknown): InvoiceReportResponse | null {
  if (!isRecord(body)) {
    return null;
  }
  const invoiceId = parseId(body['invoiceId']);
  const card = parseInvoiceCard(body['card']);
  const invoice = parseInvoiceHeader(body['invoice']);
  const purchases = parseList(body['purchases'], parseInvoicePurchase);
  const byCategory = parseList(body['byCategory'], parseCategoryGroup);
  const byResponsible = parseList(body['byResponsible'], parseResponsibleGroup);
  const installmentAdjustments = parseList(
    body['installmentAdjustments'],
    parseInstallmentAdjustment,
  );
  const invoiceAdjustments = parseReuseList(body['invoiceAdjustments'], parseInvoiceAdjustment);
  const credits = parseList(body['credits'], parseCreditApplication);
  const payments = parseReuseList(body['payments'], parseInvoicePayment);
  const allocations = parseList(body['allocations'], parseAllocation);
  if (
    invoiceId == null ||
    card == null ||
    invoice == null ||
    purchases == null ||
    byCategory == null ||
    byResponsible == null ||
    installmentAdjustments == null ||
    invoiceAdjustments == null ||
    credits == null ||
    payments == null ||
    allocations == null
  ) {
    return null;
  }
  return {
    invoiceId,
    card,
    invoice,
    purchases,
    byCategory,
    byResponsible,
    installmentAdjustments,
    invoiceAdjustments,
    credits,
    payments,
    allocations,
  };
}

function parseExpenseItem(value: unknown): ExpenseReportItem | null {
  if (!isRecord(value)) {
    return null;
  }
  const id = parseId(value['id']);
  const description = parseRequiredString(value['description']);
  const expenseDate = parseIsoDate(value['expenseDate']);
  const paymentMethod = parseEnum(value['paymentMethod'], PAYMENT_METHODS);
  const status = parseEnum(value['status'], EXPENSE_STATUSES);
  const categoryId = parseNullableId(value['categoryId']);
  const accountId = parseNullableId(value['accountId']);
  const creditCardId = parseNullableId(value['creditCardId']);
  const responsibleType = parseNullableEnum(value['responsibleType'], RESPONSIBLE_TYPES);
  const responsibleName = parseNullableString(value['responsibleName']);
  const origin = parseEnum(value['origin'], ORIGINS);
  const periodOriginal = parseMoney(value['periodOriginal']);
  const periodDiscount = parseMoney(value['periodDiscount']);
  const periodSurcharge = parseMoney(value['periodSurcharge']);
  const periodObligation = parseMoney(value['periodObligation']);
  const periodPaid = parseMoney(value['periodPaid']);
  const periodRemaining = parseMoney(value['periodRemaining']);
  const installments = parseList(value['installments'], parseExpenseInstallment);
  if (
    id == null ||
    description == null ||
    expenseDate == null ||
    paymentMethod == null ||
    status == null ||
    categoryId === undefined ||
    accountId === undefined ||
    creditCardId === undefined ||
    responsibleType === undefined ||
    responsibleName === undefined ||
    origin == null ||
    periodOriginal == null ||
    periodDiscount == null ||
    periodSurcharge == null ||
    periodObligation == null ||
    periodPaid == null ||
    periodRemaining == null ||
    installments == null
  ) {
    return null;
  }
  return {
    id,
    description,
    expenseDate,
    paymentMethod,
    status,
    categoryId,
    accountId,
    creditCardId,
    responsibleType,
    responsibleName,
    origin,
    periodOriginal,
    periodDiscount,
    periodSurcharge,
    periodObligation,
    periodPaid,
    periodRemaining,
    installments,
  };
}

function parseExpenseInstallment(value: unknown): ExpenseReportInstallment | null {
  if (!isRecord(value)) {
    return null;
  }
  const id = parseId(value['id']);
  const installmentNumber = parsePositiveCount(value['installmentNumber']);
  const totalInstallments = parsePositiveCount(value['totalInstallments']);
  const dueDate = parseIsoDate(value['dueDate']);
  const original = parseMoney(value['original']);
  const discount = parseMoney(value['discount']);
  const surcharge = parseMoney(value['surcharge']);
  const obligation = parseMoney(value['obligation']);
  const paid = parseMoney(value['paid']);
  const remaining = parseMoney(value['remaining']);
  const status = parseEnum(value['status'], EXPENSE_STATUSES);
  if (
    id == null ||
    installmentNumber == null ||
    totalInstallments == null ||
    dueDate == null ||
    original == null ||
    discount == null ||
    surcharge == null ||
    obligation == null ||
    paid == null ||
    remaining == null ||
    status == null
  ) {
    return null;
  }
  return {
    id,
    installmentNumber,
    totalInstallments,
    dueDate,
    original,
    discount,
    surcharge,
    obligation,
    paid,
    remaining,
    status,
  };
}

function parseIncomeItem(value: unknown): IncomeReportItem | null {
  if (!isRecord(value)) {
    return null;
  }
  const id = parseId(value['id']);
  const description = parseRequiredString(value['description']);
  const status = parseEnum(value['status'], INCOME_STATUSES);
  const categoryId = parseNullableId(value['categoryId']);
  const responsibleType = parseNullableEnum(value['responsibleType'], RESPONSIBLE_TYPES);
  const responsibleName = parseNullableString(value['responsibleName']);
  const expectedDate = parseIsoDate(value['expectedDate']);
  const amount = parseMoney(value['amount']);
  const accruedAmount = parseMoney(value['accruedAmount']);
  const receivedAmount = parseMoney(value['receivedAmount']);
  const remainingAmount = parseMoney(value['remainingAmount']);
  const periodReceivedAmount = parseOptionalMoney(value['periodReceivedAmount']);
  if (
    id == null ||
    description == null ||
    status == null ||
    categoryId === undefined ||
    responsibleType === undefined ||
    responsibleName === undefined ||
    expectedDate == null ||
    amount == null ||
    accruedAmount == null ||
    receivedAmount == null ||
    remainingAmount == null ||
    periodReceivedAmount === null
  ) {
    return null;
  }
  return {
    id,
    description,
    status,
    categoryId,
    responsibleType,
    responsibleName,
    expectedDate,
    amount,
    accruedAmount,
    receivedAmount,
    remainingAmount,
    ...(periodReceivedAmount !== undefined ? { periodReceivedAmount } : {}),
  };
}

function parseCategoryItem(value: unknown): CategoryReportItem | null {
  if (!isRecord(value)) {
    return null;
  }
  const categoryId = parseId(value['categoryId']);
  const name = parseRequiredString(value['name']);
  const type = parseEnum(value['type'], CATEGORY_TYPES);
  const active = typeof value['active'] === 'boolean' ? value['active'] : null;
  const periodOriginal = parseOptionalMoney(value['periodOriginal']);
  const periodDiscount = parseOptionalMoney(value['periodDiscount']);
  const periodSurcharge = parseOptionalMoney(value['periodSurcharge']);
  const periodObligation = parseOptionalMoney(value['periodObligation']);
  const periodPaid = parseOptionalMoney(value['periodPaid']);
  const periodRemaining = parseOptionalMoney(value['periodRemaining']);
  const amount = parseOptionalMoney(value['amount']);
  const accruedAmount = parseOptionalMoney(value['accruedAmount']);
  const receivedAmount = parseOptionalMoney(value['receivedAmount']);
  const remainingAmount = parseOptionalMoney(value['remainingAmount']);
  const periodReceivedAmount = parseOptionalMoney(value['periodReceivedAmount']);
  if (
    categoryId == null ||
    name == null ||
    type == null ||
    active == null ||
    periodOriginal === null ||
    periodDiscount === null ||
    periodSurcharge === null ||
    periodObligation === null ||
    periodPaid === null ||
    periodRemaining === null ||
    amount === null ||
    accruedAmount === null ||
    receivedAmount === null ||
    remainingAmount === null ||
    periodReceivedAmount === null
  ) {
    return null;
  }
  return {
    categoryId,
    name,
    type,
    active,
    ...(periodOriginal !== undefined ? { periodOriginal } : {}),
    ...(periodDiscount !== undefined ? { periodDiscount } : {}),
    ...(periodSurcharge !== undefined ? { periodSurcharge } : {}),
    ...(periodObligation !== undefined ? { periodObligation } : {}),
    ...(periodPaid !== undefined ? { periodPaid } : {}),
    ...(periodRemaining !== undefined ? { periodRemaining } : {}),
    ...(amount !== undefined ? { amount } : {}),
    ...(accruedAmount !== undefined ? { accruedAmount } : {}),
    ...(receivedAmount !== undefined ? { receivedAmount } : {}),
    ...(remainingAmount !== undefined ? { remainingAmount } : {}),
    ...(periodReceivedAmount !== undefined ? { periodReceivedAmount } : {}),
  };
}

function parseResponsibleItem(value: unknown): ResponsibleReportItem | null {
  if (!isRecord(value)) {
    return null;
  }
  const key = parseRequiredString(value['key']);
  const responsibleType = parseNullableEnum(value['responsibleType'], RESPONSIBLE_TYPES);
  const responsibleName = parseNullableString(value['responsibleName']);
  const expense = value['expense'] == null ? undefined : parseExpenseSummary(value['expense']);
  const income = value['income'] == null ? undefined : parseIncomeSummary(value['income']);
  if (
    key == null ||
    responsibleType === undefined ||
    responsibleName === undefined ||
    expense === null ||
    income === null
  ) {
    return null;
  }
  return {
    key,
    responsibleType,
    responsibleName,
    ...(expense !== undefined ? { expense } : {}),
    ...(income !== undefined ? { income } : {}),
  };
}

function parseCardItem(value: unknown): CardReportItem | null {
  if (!isRecord(value)) {
    return null;
  }
  const creditCardId = parseId(value['creditCardId']);
  const name = parseRequiredString(value['name']);
  const holderName = parseRequiredString(value['holderName']);
  const lastFourDigits = parseNullableString(value['lastFourDigits']);
  const active = typeof value['active'] === 'boolean' ? value['active'] : null;
  const summary = parseCardSummary(value['summary']);
  const purchases = parseList(value['purchases'], parseCardPurchase);
  const invoices = parseReuseList(value['invoices'], parseInvoice);
  const payments = parseReuseList(value['payments'], parseInvoicePayment);
  const credits = parseList(value['credits'], parseCreditApplication);
  const installmentAdjustments = parseList(
    value['installmentAdjustments'],
    parseInstallmentAdjustment,
  );
  const invoiceAdjustments = parseReuseList(value['invoiceAdjustments'], parseInvoiceAdjustment);
  if (
    creditCardId == null ||
    name == null ||
    holderName == null ||
    lastFourDigits === undefined ||
    active == null ||
    summary == null ||
    purchases == null ||
    invoices == null ||
    payments == null ||
    credits == null ||
    installmentAdjustments == null ||
    invoiceAdjustments == null
  ) {
    return null;
  }
  return {
    creditCardId,
    name,
    holderName,
    lastFourDigits,
    active,
    summary,
    purchases,
    invoices,
    payments,
    credits,
    installmentAdjustments,
    invoiceAdjustments,
  };
}

function parseCardPurchase(value: unknown): CardReportPurchase | null {
  if (!isRecord(value)) {
    return null;
  }
  const expenseId = parseId(value['expenseId']);
  const description = parseRequiredString(value['description']);
  const expenseDate = parseIsoDate(value['expenseDate']);
  const original = parseMoney(value['original']);
  const responsibleType = parseNullableEnum(value['responsibleType'], RESPONSIBLE_TYPES);
  const responsibleName = parseNullableString(value['responsibleName']);
  const status = parseEnum(value['status'], EXPENSE_STATUSES);
  const totalInstallments = parsePositiveCount(value['totalInstallments']);
  const installments = parseList(value['installments'], parseCardPurchaseInstallment);
  if (
    expenseId == null ||
    description == null ||
    expenseDate == null ||
    original == null ||
    responsibleType === undefined ||
    responsibleName === undefined ||
    status == null ||
    totalInstallments == null ||
    installments == null
  ) {
    return null;
  }
  return {
    expenseId,
    description,
    expenseDate,
    original,
    responsibleType,
    responsibleName,
    status,
    totalInstallments,
    installments,
  };
}

function parseCardPurchaseInstallment(value: unknown): CardReportPurchaseInstallment | null {
  if (!isRecord(value)) {
    return null;
  }
  const installmentNumber = parsePositiveCount(value['installmentNumber']);
  const dueDate = parseIsoDate(value['dueDate']);
  const amount = parseMoney(value['amount']);
  if (installmentNumber == null || dueDate == null || amount == null) {
    return null;
  }
  return { installmentNumber, dueDate, amount };
}

function parseHistorical(value: unknown): CashFlowHistorical | null {
  if (!isRecord(value)) {
    return null;
  }
  const openingBalance = parseOptionalMoney(value['openingBalance']);
  const closingBalance = parseOptionalMoney(value['closingBalance']);
  const items = parseList(value['items'], parseCashFlowItem);
  const page = parseCount(value['page']);
  const size = parseCount(value['size']);
  const totalItems = parseLongCount(value['totalItems']);
  const totalPages = parseCount(value['totalPages']);
  const summary = parseCashFlowSummary(value['summary']);
  if (
    openingBalance === null ||
    closingBalance === null ||
    items == null ||
    page == null ||
    size == null ||
    totalItems == null ||
    totalPages == null ||
    summary == null
  ) {
    return null;
  }
  return {
    ...(openingBalance !== undefined ? { openingBalance } : {}),
    ...(closingBalance !== undefined ? { closingBalance } : {}),
    items,
    page,
    size,
    totalItems,
    totalPages,
    summary,
  };
}

function parseProjected(value: unknown): CashFlowProjected | null {
  if (!isRecord(value)) {
    return null;
  }
  if (value['empty'] === true) {
    return { empty: true };
  }
  const summary = parseProjectionSummary(value['summary']);
  const months = parseList(value['months'], parseProjectionMonth);
  const quarters = parseList(value['quarters'], parseProjectionQuarter);
  if (summary == null || months == null || quarters == null) {
    return null;
  }
  return { summary, months, quarters };
}

function parseCashFlowItem(value: unknown): CashFlowItem | null {
  if (!isRecord(value)) {
    return null;
  }
  const id = parseId(value['id']);
  const type = parseEnum(value['type'], CASH_FLOW_TYPES);
  const date = parseIsoDate(value['date']);
  const amount = parseMoney(value['amount']);
  const accountId = parseNullableId(value['accountId']);
  const description = parseRequiredString(value['description']);
  if (
    id == null ||
    type == null ||
    date == null ||
    amount == null ||
    accountId === undefined ||
    description == null
  ) {
    return null;
  }
  return { id, type, date, amount, accountId, description };
}

function parseInvoiceCard(value: unknown): InvoiceReportResponse['card'] | null {
  if (!isRecord(value)) {
    return null;
  }
  const name = parseRequiredString(value['name']);
  const holderName = parseRequiredString(value['holderName']);
  const lastFourDigits = parseNullableString(value['lastFourDigits']);
  if (name == null || holderName == null || lastFourDigits === undefined) {
    return null;
  }
  return { name, holderName, lastFourDigits };
}

function parseInvoiceHeader(value: unknown): InvoiceReportHeader | null {
  if (!isRecord(value)) {
    return null;
  }
  const referenceYear = parseYear(value['referenceYear']);
  const referenceMonth = parseMonth(value['referenceMonth']);
  const closingDate = parseIsoDate(value['closingDate']);
  const dueDate = parseIsoDate(value['dueDate']);
  const status = parseEnum(value['status'], INVOICE_STATUSES);
  const totalAmount = parseMoney(value['totalAmount']);
  const paidAmount = parseMoney(value['paidAmount']);
  const remainingAmount = parseMoney(value['remainingAmount']);
  if (
    referenceYear == null ||
    referenceMonth == null ||
    closingDate == null ||
    dueDate == null ||
    status == null ||
    totalAmount == null ||
    paidAmount == null ||
    remainingAmount == null
  ) {
    return null;
  }
  return {
    referenceYear,
    referenceMonth,
    closingDate,
    dueDate,
    status,
    totalAmount,
    paidAmount,
    remainingAmount,
  };
}

function parseInvoicePurchase(value: unknown): InvoiceReportPurchase | null {
  if (!isRecord(value)) {
    return null;
  }
  const expenseId = parseId(value['expenseId']);
  const description = parseRequiredString(value['description']);
  const expenseDate = parseIsoDate(value['expenseDate']);
  const original = parseMoney(value['original']);
  const categoryName = parseRequiredString(value['categoryName']);
  const responsibleType = parseNullableEnum(value['responsibleType'], RESPONSIBLE_TYPES);
  const responsibleName = parseNullableString(value['responsibleName']);
  const installmentNumber = parsePositiveCount(value['installmentNumber']);
  const totalInstallments = parsePositiveCount(value['totalInstallments']);
  const discount = parseMoney(value['discount']);
  const surcharge = parseMoney(value['surcharge']);
  if (
    expenseId == null ||
    description == null ||
    expenseDate == null ||
    original == null ||
    categoryName == null ||
    responsibleType === undefined ||
    responsibleName === undefined ||
    installmentNumber == null ||
    totalInstallments == null ||
    discount == null ||
    surcharge == null
  ) {
    return null;
  }
  return {
    expenseId,
    description,
    expenseDate,
    original,
    categoryName,
    responsibleType,
    responsibleName,
    installmentNumber,
    totalInstallments,
    discount,
    surcharge,
  };
}

function parseCategoryGroup(value: unknown): InvoiceReportCategoryGroup | null {
  if (!isRecord(value)) {
    return null;
  }
  const name = parseRequiredString(value['name']);
  const original = parseMoney(value['original']);
  if (name == null || original == null) {
    return null;
  }
  return { name, original };
}

function parseResponsibleGroup(value: unknown): InvoiceReportResponsibleGroup | null {
  if (!isRecord(value)) {
    return null;
  }
  const responsibleType = parseNullableEnum(value['responsibleType'], RESPONSIBLE_TYPES);
  const responsibleName = parseNullableString(value['responsibleName']);
  const original = parseMoney(value['original']);
  if (responsibleType === undefined || responsibleName === undefined || original == null) {
    return null;
  }
  return { responsibleType, responsibleName, original };
}

function parseAllocation(value: unknown): InvoiceReportAllocation | null {
  if (!isRecord(value)) {
    return null;
  }
  const id = parseId(value['id']);
  const type = parseEnum(value['type'], ALLOCATION_TYPES);
  const sourceId = parseId(value['sourceId']);
  const installmentId = parseId(value['installmentId']);
  const amount = parseMoney(value['amount']);
  const createdAt = parseInstant(value['createdAt']);
  if (
    id == null ||
    type == null ||
    sourceId == null ||
    installmentId == null ||
    amount == null ||
    createdAt == null
  ) {
    return null;
  }
  return { id, type, sourceId, installmentId, amount, createdAt };
}

function parseCreditApplication(value: unknown): CardReportCreditApplication | null {
  if (!isRecord(value)) {
    return null;
  }
  const id = parseId(value['id']);
  const creditId = parseId(value['creditId']);
  const invoiceId = parseId(value['invoiceId']);
  const installmentId = parseId(value['installmentId']);
  const amount = parseMoney(value['amount']);
  const createdAt = parseInstant(value['createdAt']);
  if (
    id == null ||
    creditId == null ||
    invoiceId == null ||
    installmentId == null ||
    amount == null ||
    createdAt == null
  ) {
    return null;
  }
  return { id, creditId, invoiceId, installmentId, amount, createdAt };
}

function parseInstallmentAdjustment(value: unknown): ReportInstallmentAdjustment | null {
  if (!isRecord(value)) {
    return null;
  }
  const id = parseId(value['id']);
  const expenseId = parseId(value['expenseId']);
  const installmentId = parseId(value['installmentId']);
  const type = parseEnum(value['type'], ADJUSTMENT_TYPES);
  const amount = parseMoney(value['amount']);
  const reason = parseRequiredString(value['reason']);
  const status = parseEnum(value['status'], ADJUSTMENT_STATUSES);
  const createdAt = parseInstant(value['createdAt']);
  if (
    id == null ||
    expenseId == null ||
    installmentId == null ||
    type == null ||
    amount == null ||
    reason == null ||
    status == null ||
    createdAt == null
  ) {
    return null;
  }
  return { id, expenseId, installmentId, type, amount, reason, status, createdAt };
}

function parsePeriod(value: unknown): ReportPeriod | null {
  if (!isRecord(value)) {
    return null;
  }
  const startDate = parseIsoDate(value['startDate']);
  const endDate = parseIsoDate(value['endDate']);
  if (startDate == null || endDate == null) {
    return null;
  }
  return { startDate, endDate };
}

function parseExpenseSummary(value: unknown): ExpenseReportSummary | null {
  if (!isRecord(value)) {
    return null;
  }
  const periodOriginal = parseMoney(value['periodOriginal']);
  const periodDiscount = parseMoney(value['periodDiscount']);
  const periodSurcharge = parseMoney(value['periodSurcharge']);
  const periodObligation = parseMoney(value['periodObligation']);
  const periodPaid = parseMoney(value['periodPaid']);
  const periodRemaining = parseMoney(value['periodRemaining']);
  if (
    periodOriginal == null ||
    periodDiscount == null ||
    periodSurcharge == null ||
    periodObligation == null ||
    periodPaid == null ||
    periodRemaining == null
  ) {
    return null;
  }
  return {
    periodOriginal,
    periodDiscount,
    periodSurcharge,
    periodObligation,
    periodPaid,
    periodRemaining,
  };
}

function parseIncomeSummary(value: unknown): IncomeReportSummary | null {
  if (!isRecord(value)) {
    return null;
  }
  const amount = parseOptionalMoney(value['amount']);
  const accruedAmount = parseOptionalMoney(value['accruedAmount']);
  const receivedAmount = parseOptionalMoney(value['receivedAmount']);
  const remainingAmount = parseOptionalMoney(value['remainingAmount']);
  const periodReceivedAmount = parseOptionalMoney(value['periodReceivedAmount']);
  if (
    amount === null ||
    accruedAmount === null ||
    receivedAmount === null ||
    remainingAmount === null ||
    periodReceivedAmount === null
  ) {
    return null;
  }
  return {
    ...(amount !== undefined ? { amount } : {}),
    ...(accruedAmount !== undefined ? { accruedAmount } : {}),
    ...(receivedAmount !== undefined ? { receivedAmount } : {}),
    ...(remainingAmount !== undefined ? { remainingAmount } : {}),
    ...(periodReceivedAmount !== undefined ? { periodReceivedAmount } : {}),
  };
}

function parseCategorySummary(value: unknown): CategoryReportResponse['summary'] | null {
  if (!isRecord(value)) {
    return null;
  }
  const expense = parseExpenseSummary(value['expense']);
  const income = parseIncomeSummary(value['income']);
  if (expense == null || income == null) {
    return null;
  }
  return { expense, income };
}

function parseResponsibleSummary(value: unknown): ResponsibleReportResponse['summary'] | null {
  if (!isRecord(value)) {
    return null;
  }
  const expense = value['expense'] == null ? undefined : parseExpenseSummary(value['expense']);
  const income = value['income'] == null ? undefined : parseIncomeSummary(value['income']);
  if (expense === null || income === null) {
    return null;
  }
  return {
    ...(expense !== undefined ? { expense } : {}),
    ...(income !== undefined ? { income } : {}),
  };
}

function parseCardSummary(value: unknown): CardReportSummary | null {
  if (!isRecord(value)) {
    return null;
  }
  const purchaseAmount = parseMoney(value['purchaseAmount']);
  const invoiceAmount = parseMoney(value['invoiceAmount']);
  const paidAmount = parseMoney(value['paidAmount']);
  const creditAmount = parseMoney(value['creditAmount']);
  if (
    purchaseAmount == null ||
    invoiceAmount == null ||
    paidAmount == null ||
    creditAmount == null
  ) {
    return null;
  }
  return { purchaseAmount, invoiceAmount, paidAmount, creditAmount };
}

function parseCashFlowSummary(value: unknown): CashFlowHistorical['summary'] | null {
  if (!isRecord(value)) {
    return null;
  }
  const totalIn = parseMoney(value['totalIn']);
  const totalOut = parseMoney(value['totalOut']);
  const net = parseMoney(value['net']);
  if (totalIn == null || totalOut == null || net == null) {
    return null;
  }
  return { totalIn, totalOut, net };
}

function parseProjectionSummary(value: unknown): ProjectionSummary | null {
  if (!isRecord(value)) {
    return null;
  }
  const currentBalance = parseMoney(value['currentBalance']);
  const projectedFinalBalance = parseMoney(value['projectedFinalBalance']);
  const projectedIncome = parseMoney(value['projectedIncome']);
  const projectedExpense = parseMoney(value['projectedExpense']);
  const projectedNetCashFlow = parseMoney(value['projectedNetCashFlow']);
  const minimumProjectedBalance = parseMoney(value['minimumProjectedBalance']);
  const minimumProjectedBalanceDate = parseIsoDate(value['minimumProjectedBalanceDate']);
  const reservedAmount = parseMoney(value['reservedAmount']);
  const availableProjectedBalance = parseMoney(value['availableProjectedBalance']);
  if (
    currentBalance == null ||
    projectedFinalBalance == null ||
    projectedIncome == null ||
    projectedExpense == null ||
    projectedNetCashFlow == null ||
    minimumProjectedBalance == null ||
    minimumProjectedBalanceDate == null ||
    reservedAmount == null ||
    availableProjectedBalance == null
  ) {
    return null;
  }
  return {
    currentBalance,
    projectedFinalBalance,
    projectedIncome,
    projectedExpense,
    projectedNetCashFlow,
    minimumProjectedBalance,
    minimumProjectedBalanceDate,
    reservedAmount,
    availableProjectedBalance,
  };
}

function parseProjectionMonth(value: unknown): ProjectionMonth | null {
  if (!isRecord(value)) {
    return null;
  }
  const period = parseYearMonth(value['period']);
  const openingBalance = parseMoney(value['openingBalance']);
  const totalIncome = parseMoney(value['totalIncome']);
  const totalExpense = parseMoney(value['totalExpense']);
  const netCashFlow = parseMoney(value['netCashFlow']);
  const closingBalance = parseMoney(value['closingBalance']);
  const minimumProjectedBalance = parseMoney(value['minimumProjectedBalance']);
  const minimumProjectedBalanceDate = parseIsoDate(value['minimumProjectedBalanceDate']);
  const negative = typeof value['negative'] === 'boolean' ? value['negative'] : null;
  const reservedAmount = parseMoney(value['reservedAmount']);
  const availableProjectedBalance = parseMoney(value['availableProjectedBalance']);
  if (
    period == null ||
    openingBalance == null ||
    totalIncome == null ||
    totalExpense == null ||
    netCashFlow == null ||
    closingBalance == null ||
    minimumProjectedBalance == null ||
    minimumProjectedBalanceDate == null ||
    negative == null ||
    reservedAmount == null ||
    availableProjectedBalance == null
  ) {
    return null;
  }
  return {
    period,
    openingBalance,
    totalIncome,
    totalExpense,
    netCashFlow,
    closingBalance,
    minimumProjectedBalance,
    minimumProjectedBalanceDate,
    negative,
    reservedAmount,
    availableProjectedBalance,
  };
}

function parseProjectionQuarter(value: unknown): ProjectionQuarter | null {
  if (!isRecord(value)) {
    return null;
  }
  const period = parseRequiredString(value['period']);
  const months = parseList(value['months'], parseYearMonth);
  const totalIncome = parseMoney(value['totalIncome']);
  const totalExpense = parseMoney(value['totalExpense']);
  const netCashFlow = parseMoney(value['netCashFlow']);
  const openingBalance = parseMoney(value['openingBalance']);
  const closingBalance = parseMoney(value['closingBalance']);
  if (
    period == null ||
    months == null ||
    totalIncome == null ||
    totalExpense == null ||
    netCashFlow == null ||
    openingBalance == null ||
    closingBalance == null
  ) {
    return null;
  }
  return { period, months, totalIncome, totalExpense, netCashFlow, openingBalance, closingBalance };
}

function parseList<T>(value: unknown, parseItem: (item: unknown) => T | null): T[] | null {
  if (!Array.isArray(value)) {
    return null;
  }
  const items: T[] = [];
  for (const item of value) {
    const parsed = parseItem(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

function parseReuseList<T>(value: unknown, parseItem: (item: unknown) => T | null): T[] | null {
  return parseList(value, parseItem);
}

function parseEnum<T extends string>(value: unknown, allowed: Set<T>): T | null {
  return typeof value === 'string' && allowed.has(value as T) ? (value as T) : null;
}

function parseNullableEnum<T extends string>(
  value: unknown,
  allowed: Set<T>,
): T | null | undefined {
  if (value == null) {
    return null;
  }
  return parseEnum(value, allowed) ?? undefined;
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseNullableId(value: unknown): string | null | undefined {
  if (value == null) {
    return null;
  }
  return parseId(value) ?? undefined;
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

function parseMoney(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseOptionalMoney(value: unknown): number | undefined | null {
  if (value == null) {
    return undefined;
  }
  return parseMoney(value);
}

function parseCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : null;
}

function parseLongCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : null;
}

function parsePositiveCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 ? value : null;
}

function parseYear(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 ? value : null;
}

function parseMonth(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 && value <= 12
    ? value
    : null;
}

function parseIsoDate(value: unknown): string | null {
  return typeof value === 'string' && ISO_DATE.test(value) ? value : null;
}

function parseYearMonth(value: unknown): string | null {
  return typeof value === 'string' && YEAR_MONTH.test(value) ? value : null;
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}
