export type ProjectionEventType = 'INCOME' | 'EXPENSE' | 'CREDIT_CARD_INVOICE' | 'TRANSFER';

export type ProjectionDirection = 'IN' | 'OUT';

export type ProjectionAccountAssignment = 'UNASSIGNED';

export type ProjectionPeriodMode = 'default' | 'range' | 'yearMonth' | 'fromToday';

export interface ProjectionSummary {
  readonly currentBalance: number;
  readonly projectedFinalBalance: number;
  readonly projectedIncome: number;
  readonly projectedExpense: number;
  readonly projectedNetCashFlow: number;
  readonly minimumProjectedBalance: number;
  readonly minimumProjectedBalanceDate: string;
  readonly reservedAmount: number;
  readonly availableProjectedBalance: number;
}

export interface ProjectionMonth {
  readonly period: string;
  readonly openingBalance: number;
  readonly totalIncome: number;
  readonly totalExpense: number;
  readonly netCashFlow: number;
  readonly closingBalance: number;
  readonly minimumProjectedBalance: number;
  readonly minimumProjectedBalanceDate: string;
  readonly negative: boolean;
  readonly reservedAmount: number;
  readonly availableProjectedBalance: number;
}

export interface ProjectionQuarter {
  readonly period: string;
  readonly months: readonly string[];
  readonly totalIncome: number;
  readonly totalExpense: number;
  readonly netCashFlow: number;
  readonly openingBalance: number;
  readonly closingBalance: number;
}

export interface ProjectionEvent {
  readonly date: string | null;
  readonly type: ProjectionEventType;
  readonly description: string;
  readonly amount: number;
  readonly direction: ProjectionDirection;
  readonly sourceId: string;
  readonly sourceType: ProjectionEventType;
  readonly overdue: boolean;
  readonly accountAssignment: ProjectionAccountAssignment;
}

export interface ProjectionEventPage {
  readonly items: readonly ProjectionEvent[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

export interface ProjectionResponse {
  readonly startDate: string;
  readonly endDate: string;
  readonly summary: ProjectionSummary;
  readonly months: readonly ProjectionMonth[];
  readonly quarters: readonly ProjectionQuarter[];
  readonly events: ProjectionEventPage;
  readonly undatedEvents: readonly ProjectionEvent[];
}

export interface ProjectionQueryParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly year?: number;
  readonly month?: number;
  readonly months?: number;
  readonly accountId?: string;
  readonly page?: number;
  readonly size?: number;
}

export const PROJECTION_PERIOD_MODE_OPTIONS: readonly {
  value: ProjectionPeriodMode;
  label: string;
}[] = [
  { value: 'default', label: 'Próximos 12 meses' },
  { value: 'range', label: 'Intervalo de datas' },
  { value: 'yearMonth', label: 'Mês/ano' },
  { value: 'fromToday', label: 'A partir de hoje por N meses' },
];

export const PROJECTION_MONTHS_COUNT_OPTIONS: readonly number[] = [
  1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
];
