export type AccountType = 'BANK_ACCOUNT' | 'CASH';

export interface DashboardBalance {
  readonly totalBalance: number;
  readonly reservedAmount: number;
  readonly availableBalance: number;
}

export interface DashboardProjectionSummary {
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

export interface DashboardProjectionMonth {
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

export interface DashboardProjectionQuarter {
  readonly period: string;
  readonly months: readonly string[];
  readonly totalIncome: number;
  readonly totalExpense: number;
  readonly netCashFlow: number;
  readonly openingBalance: number;
  readonly closingBalance: number;
}

export interface DashboardProjection {
  readonly summary: DashboardProjectionSummary;
  readonly months: readonly DashboardProjectionMonth[];
  readonly quarters: readonly DashboardProjectionQuarter[];
}

export interface DashboardPayables {
  readonly totalRemaining: number;
  readonly installmentRemaining: number;
  readonly invoiceRemaining: number;
  readonly overdueRemaining: number;
  readonly overdueInstallmentRemaining: number;
  readonly overdueInvoiceRemaining: number;
  readonly openCount: number;
  readonly overdueCount: number;
}

export interface DashboardReceivables {
  readonly futureAmount: number;
  readonly overdueAmount: number;
  readonly totalReceivableAmount: number;
  readonly receivedAmount: number;
}

export interface DashboardAccount {
  readonly id: string;
  readonly name: string;
  readonly type: AccountType;
  readonly totalBalance: number;
  readonly reservedAmount: number;
  readonly availableBalance: number;
}

export interface DashboardCreditCard {
  readonly id: string;
  readonly name: string;
  readonly creditLimit: number;
  readonly usedLimit: number;
  readonly availableLimit: number;
  readonly invoiceRemaining: number;
  readonly overdueInvoiceRemaining: number;
}

export interface DashboardResponse {
  readonly asOfDate: string;
  readonly startDate: string;
  readonly endDate: string;
  readonly balance: DashboardBalance;
  readonly projection: DashboardProjection;
  readonly payables: DashboardPayables;
  readonly receivables: DashboardReceivables;
  readonly accounts: readonly DashboardAccount[];
  readonly creditCards: readonly DashboardCreditCard[];
}
