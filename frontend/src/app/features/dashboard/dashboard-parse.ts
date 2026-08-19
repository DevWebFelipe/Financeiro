import { isRecord } from '../../core/errors/api-error';
import {
  AccountType,
  DashboardAccount,
  DashboardBalance,
  DashboardCreditCard,
  DashboardPayables,
  DashboardProjection,
  DashboardProjectionMonth,
  DashboardProjectionQuarter,
  DashboardProjectionSummary,
  DashboardReceivables,
  DashboardResponse,
} from './dashboard.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const YEAR_MONTH = /^\d{4}-\d{2}$/;

export function parseDashboardResponse(body: unknown): DashboardResponse | null {
  if (!isRecord(body)) {
    return null;
  }

  const asOfDate = parseIsoDate(body['asOfDate']);
  const startDate = parseIsoDate(body['startDate']);
  const endDate = parseIsoDate(body['endDate']);
  const balance = parseBalance(body['balance']);
  const projection = parseProjection(body['projection']);
  const payables = parsePayables(body['payables']);
  const receivables = parseReceivables(body['receivables']);
  const accounts = parseList(body['accounts'], parseAccount);
  const creditCards = parseList(body['creditCards'], parseCreditCard);

  if (
    asOfDate == null ||
    startDate == null ||
    endDate == null ||
    balance == null ||
    projection == null ||
    payables == null ||
    receivables == null ||
    accounts == null ||
    creditCards == null
  ) {
    return null;
  }

  return {
    asOfDate,
    startDate,
    endDate,
    balance,
    projection,
    payables,
    receivables,
    accounts,
    creditCards,
  };
}

function parseBalance(value: unknown): DashboardBalance | null {
  if (!isRecord(value)) {
    return null;
  }

  const totalBalance = parseMoney(value['totalBalance']);
  const reservedAmount = parseMoney(value['reservedAmount']);
  const availableBalance = parseMoney(value['availableBalance']);
  if (totalBalance == null || reservedAmount == null || availableBalance == null) {
    return null;
  }

  return { totalBalance, reservedAmount, availableBalance };
}

function parseProjection(value: unknown): DashboardProjection | null {
  if (!isRecord(value)) {
    return null;
  }

  const summary = parseProjectionSummary(value['summary']);
  const months = parseList(value['months'], parseProjectionMonth);
  const quarters = parseList(value['quarters'], parseProjectionQuarter);
  if (summary == null || months == null || quarters == null) {
    return null;
  }

  return { summary, months, quarters };
}

function parseProjectionSummary(value: unknown): DashboardProjectionSummary | null {
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

function parseProjectionMonth(value: unknown): DashboardProjectionMonth | null {
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

function parseProjectionQuarter(value: unknown): DashboardProjectionQuarter | null {
  if (!isRecord(value)) {
    return null;
  }

  const period = typeof value['period'] === 'string' ? value['period'] : null;
  const months = parseStringList(value['months'], parseYearMonth);
  const totalIncome = parseMoney(value['totalIncome']);
  const totalExpense = parseMoney(value['totalExpense']);
  const netCashFlow = parseMoney(value['netCashFlow']);
  const openingBalance = parseMoney(value['openingBalance']);
  const closingBalance = parseMoney(value['closingBalance']);

  if (
    period == null ||
    period.length === 0 ||
    months == null ||
    totalIncome == null ||
    totalExpense == null ||
    netCashFlow == null ||
    openingBalance == null ||
    closingBalance == null
  ) {
    return null;
  }

  return {
    period,
    months,
    totalIncome,
    totalExpense,
    netCashFlow,
    openingBalance,
    closingBalance,
  };
}

function parsePayables(value: unknown): DashboardPayables | null {
  if (!isRecord(value)) {
    return null;
  }

  const totalRemaining = parseMoney(value['totalRemaining']);
  const installmentRemaining = parseMoney(value['installmentRemaining']);
  const invoiceRemaining = parseMoney(value['invoiceRemaining']);
  const overdueRemaining = parseMoney(value['overdueRemaining']);
  const overdueInstallmentRemaining = parseMoney(value['overdueInstallmentRemaining']);
  const overdueInvoiceRemaining = parseMoney(value['overdueInvoiceRemaining']);
  const openCount = parseCount(value['openCount']);
  const overdueCount = parseCount(value['overdueCount']);

  if (
    totalRemaining == null ||
    installmentRemaining == null ||
    invoiceRemaining == null ||
    overdueRemaining == null ||
    overdueInstallmentRemaining == null ||
    overdueInvoiceRemaining == null ||
    openCount == null ||
    overdueCount == null
  ) {
    return null;
  }

  return {
    totalRemaining,
    installmentRemaining,
    invoiceRemaining,
    overdueRemaining,
    overdueInstallmentRemaining,
    overdueInvoiceRemaining,
    openCount,
    overdueCount,
  };
}

function parseReceivables(value: unknown): DashboardReceivables | null {
  if (!isRecord(value)) {
    return null;
  }

  const futureAmount = parseMoney(value['futureAmount']);
  const overdueAmount = parseMoney(value['overdueAmount']);
  const totalReceivableAmount = parseMoney(value['totalReceivableAmount']);
  const receivedAmount = parseMoney(value['receivedAmount']);
  if (
    futureAmount == null ||
    overdueAmount == null ||
    totalReceivableAmount == null ||
    receivedAmount == null
  ) {
    return null;
  }

  return { futureAmount, overdueAmount, totalReceivableAmount, receivedAmount };
}

function parseAccount(value: unknown): DashboardAccount | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const name = typeof value['name'] === 'string' ? value['name'] : null;
  const type = parseAccountType(value['type']);
  const totalBalance = parseMoney(value['totalBalance']);
  const reservedAmount = parseMoney(value['reservedAmount']);
  const availableBalance = parseMoney(value['availableBalance']);

  if (
    id == null ||
    name == null ||
    type == null ||
    totalBalance == null ||
    reservedAmount == null ||
    availableBalance == null
  ) {
    return null;
  }

  return { id, name, type, totalBalance, reservedAmount, availableBalance };
}

function parseCreditCard(value: unknown): DashboardCreditCard | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const name = typeof value['name'] === 'string' ? value['name'] : null;
  const creditLimit = parseMoney(value['creditLimit']);
  const usedLimit = parseMoney(value['usedLimit']);
  const availableLimit = parseMoney(value['availableLimit']);
  const invoiceRemaining = parseMoney(value['invoiceRemaining']);
  const overdueInvoiceRemaining = parseMoney(value['overdueInvoiceRemaining']);

  if (
    id == null ||
    name == null ||
    creditLimit == null ||
    usedLimit == null ||
    availableLimit == null ||
    invoiceRemaining == null ||
    overdueInvoiceRemaining == null
  ) {
    return null;
  }

  return {
    id,
    name,
    creditLimit,
    usedLimit,
    availableLimit,
    invoiceRemaining,
    overdueInvoiceRemaining,
  };
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

function parseStringList(
  value: unknown,
  parseItem: (item: unknown) => string | null,
): string[] | null {
  return parseList(value, parseItem);
}

function parseMoney(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : null;
}

function parseIsoDate(value: unknown): string | null {
  return typeof value === 'string' && ISO_DATE.test(value) ? value : null;
}

function parseYearMonth(value: unknown): string | null {
  return typeof value === 'string' && YEAR_MONTH.test(value) ? value : null;
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseAccountType(value: unknown): AccountType | null {
  return value === 'BANK_ACCOUNT' || value === 'CASH' ? value : null;
}
