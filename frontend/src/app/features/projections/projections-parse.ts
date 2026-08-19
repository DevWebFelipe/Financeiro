import { isRecord } from '../../core/errors/api-error';
import {
  ProjectionAccountAssignment,
  ProjectionDirection,
  ProjectionEvent,
  ProjectionEventPage,
  ProjectionEventType,
  ProjectionMonth,
  ProjectionQuarter,
  ProjectionResponse,
  ProjectionSummary,
} from './projections.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const YEAR_MONTH = /^\d{4}-\d{2}$/;

const EVENT_TYPES: ReadonlySet<ProjectionEventType> = new Set([
  'INCOME',
  'EXPENSE',
  'CREDIT_CARD_INVOICE',
  'TRANSFER',
]);

export function parseProjectionResponse(body: unknown): ProjectionResponse | null {
  if (!isRecord(body)) {
    return null;
  }

  const startDate = parseIsoDate(body['startDate']);
  const endDate = parseIsoDate(body['endDate']);
  const summary = parseSummary(body['summary']);
  const months = parseList(body['months'], parseMonth);
  const quarters = parseList(body['quarters'], parseQuarter);
  const events = parseEventPage(body['events']);
  const undatedEvents = parseList(body['undatedEvents'], parseEvent);

  if (
    startDate == null ||
    endDate == null ||
    summary == null ||
    months == null ||
    quarters == null ||
    events == null ||
    undatedEvents == null
  ) {
    return null;
  }

  return {
    startDate,
    endDate,
    summary,
    months,
    quarters,
    events,
    undatedEvents,
  };
}

export function parseProjectionEvent(value: unknown): ProjectionEvent | null {
  return parseEvent(value);
}

function parseSummary(value: unknown): ProjectionSummary | null {
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

function parseMonth(value: unknown): ProjectionMonth | null {
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

function parseQuarter(value: unknown): ProjectionQuarter | null {
  if (!isRecord(value)) {
    return null;
  }

  const period = typeof value['period'] === 'string' ? value['period'] : null;
  const months = parseList(value['months'], parseYearMonth);
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

function parseEventPage(value: unknown): ProjectionEventPage | null {
  if (!isRecord(value)) {
    return null;
  }

  const items = parseList(value['items'], parseEvent);
  const page = parseCount(value['page']);
  const size = parseCount(value['size']);
  const totalItems = parseLongCount(value['totalItems']);
  const totalPages = parseCount(value['totalPages']);

  if (items == null || page == null || size == null || totalItems == null || totalPages == null) {
    return null;
  }

  return { items, page, size, totalItems, totalPages };
}

function parseEvent(value: unknown): ProjectionEvent | null {
  if (!isRecord(value)) {
    return null;
  }

  if (value['date'] != null && parseIsoDate(value['date']) == null) {
    return null;
  }

  const date = parseIsoDate(value['date']);
  const type = parseEventType(value['type']);
  const description = typeof value['description'] === 'string' ? value['description'] : null;
  const amount = parseMoney(value['amount']);
  const direction = parseDirection(value['direction']);
  const sourceId = parseId(value['sourceId']);
  const sourceType = parseEventType(value['sourceType']);
  const overdue = typeof value['overdue'] === 'boolean' ? value['overdue'] : null;
  const accountAssignment = parseAccountAssignment(value['accountAssignment']);

  if (
    type == null ||
    description == null ||
    amount == null ||
    direction == null ||
    sourceId == null ||
    sourceType == null ||
    overdue == null ||
    accountAssignment == null
  ) {
    return null;
  }

  return {
    date,
    type,
    description,
    amount,
    direction,
    sourceId,
    sourceType,
    overdue,
    accountAssignment,
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

function parseYearMonth(value: unknown): string | null {
  if (typeof value !== 'string' || !YEAR_MONTH.test(value)) {
    return null;
  }
  const month = Number(value.slice(5, 7));
  if (!Number.isInteger(month) || month < 1 || month > 12) {
    return null;
  }
  return value;
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseEventType(value: unknown): ProjectionEventType | null {
  return typeof value === 'string' && EVENT_TYPES.has(value as ProjectionEventType)
    ? (value as ProjectionEventType)
    : null;
}

function parseDirection(value: unknown): ProjectionDirection | null {
  return value === 'IN' || value === 'OUT' ? value : null;
}

function parseAccountAssignment(value: unknown): ProjectionAccountAssignment | null {
  return value === 'UNASSIGNED' ? value : null;
}
