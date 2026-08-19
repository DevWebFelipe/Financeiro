import { Invoice, InvoiceAdjustment, InvoicePayment } from '../invoices/invoices.models';

export type ReportType =
  'expenses' | 'incomes' | 'categories' | 'responsibles' | 'cards' | 'cash-flow' | 'invoices';

export type SortDirection = 'asc' | 'desc';

export type ExpenseStatus = 'OPEN' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED' | 'REFUNDED';
export type PaymentMethod = 'ACCOUNT' | 'CREDIT_CARD' | 'NONE';
export type ResponsibleType = 'MINE' | 'GIULIA' | 'EDERSON' | 'ELISIANE' | 'OTHER';
export type IncomeStatus = 'EXPECTED' | 'RECEIVED' | 'CANCELLED';
export type CategoryType = 'INCOME' | 'EXPENSE';
export type ReportDateType = 'EXPECTED' | 'RECEIVED';
export type ReportNature = 'EXPENSE' | 'INCOME' | 'BOTH';
export type ExpenseReportOrigin = 'PURCHASE' | 'AGREEMENT';
export type CashFlowFlowType = 'HISTORICAL' | 'PROJECTED' | 'BOTH';
export type CashFlowType =
  | 'INCOME_RECEIPT'
  | 'EXPENSE_PAYMENT'
  | 'INVOICE_PAYMENT'
  | 'CARD_PURCHASE_REFUND'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'BALANCE_ADJUSTMENT';
export type InvoiceReportAllocationType =
  'PAYMENT' | 'INVOICE_ADJUSTMENT' | 'CREDIT' | 'SETTLEMENT';
export type InvoiceStatus = 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'PAID' | 'SETTLED_BY_AGREEMENT';
export type AdjustmentType = 'DISCOUNT' | 'SURCHARGE';
export type AdjustmentStatus = 'ACTIVE' | 'REVERSED';

export type ExpenseReportSortField =
  | 'dueDate'
  | 'expenseDate'
  | 'description'
  | 'status'
  | 'createdAt'
  | 'periodObligation'
  | 'periodRemaining';

export type IncomeReportSortField =
  'expectedDate' | 'description' | 'amount' | 'status' | 'createdAt' | 'receivedAmount';

export type CategoryReportSortField = 'name' | 'type';
export type ResponsibleReportSortField = 'responsibleType' | 'responsibleName';
export type CardReportSortField = 'name' | 'holderName';
export type CashFlowSortField = 'date' | 'amount' | 'type';

export interface ReportPeriod {
  readonly startDate: string;
  readonly endDate: string;
}

export interface ExpenseReportSummary {
  readonly periodOriginal: number;
  readonly periodDiscount: number;
  readonly periodSurcharge: number;
  readonly periodObligation: number;
  readonly periodPaid: number;
  readonly periodRemaining: number;
}

export interface IncomeReportSummary {
  readonly amount?: number;
  readonly accruedAmount?: number;
  readonly receivedAmount?: number;
  readonly remainingAmount?: number;
  readonly periodReceivedAmount?: number;
}

export interface ExpenseReportInstallment {
  readonly id: string;
  readonly installmentNumber: number;
  readonly totalInstallments: number;
  readonly dueDate: string;
  readonly original: number;
  readonly discount: number;
  readonly surcharge: number;
  readonly obligation: number;
  readonly paid: number;
  readonly remaining: number;
  readonly status: ExpenseStatus;
}

export interface ExpenseReportItem {
  readonly id: string;
  readonly description: string;
  readonly expenseDate: string;
  readonly paymentMethod: PaymentMethod;
  readonly status: ExpenseStatus;
  readonly categoryId: string | null;
  readonly accountId: string | null;
  readonly creditCardId: string | null;
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
  readonly origin: ExpenseReportOrigin;
  readonly periodOriginal: number;
  readonly periodDiscount: number;
  readonly periodSurcharge: number;
  readonly periodObligation: number;
  readonly periodPaid: number;
  readonly periodRemaining: number;
  readonly installments: readonly ExpenseReportInstallment[];
}

export interface ExpenseReportResponse {
  readonly period: ReportPeriod;
  readonly items: readonly ExpenseReportItem[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly summary: ExpenseReportSummary;
}

export interface IncomeReportItem {
  readonly id: string;
  readonly description: string;
  readonly status: IncomeStatus;
  readonly categoryId: string | null;
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
  readonly expectedDate: string;
  readonly amount: number;
  readonly accruedAmount: number;
  readonly receivedAmount: number;
  readonly remainingAmount: number;
  readonly periodReceivedAmount?: number;
}

export interface IncomeReportResponse {
  readonly period: ReportPeriod;
  readonly dateType: ReportDateType;
  readonly items: readonly IncomeReportItem[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly summary: IncomeReportSummary;
}

export interface CategoryReportItem {
  readonly categoryId: string;
  readonly name: string;
  readonly type: CategoryType;
  readonly active: boolean;
  readonly periodOriginal?: number;
  readonly periodDiscount?: number;
  readonly periodSurcharge?: number;
  readonly periodObligation?: number;
  readonly periodPaid?: number;
  readonly periodRemaining?: number;
  readonly amount?: number;
  readonly accruedAmount?: number;
  readonly receivedAmount?: number;
  readonly remainingAmount?: number;
  readonly periodReceivedAmount?: number;
}

export interface CategoryReportResponse {
  readonly period: ReportPeriod;
  readonly dateType: ReportDateType;
  readonly items: readonly CategoryReportItem[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly summary: {
    readonly expense: ExpenseReportSummary;
    readonly income: IncomeReportSummary;
  };
}

export interface ResponsibleReportItem {
  readonly key: string;
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
  readonly expense?: ExpenseReportSummary;
  readonly income?: IncomeReportSummary;
}

export interface ResponsibleReportResponse {
  readonly period: ReportPeriod;
  readonly nature: ReportNature;
  readonly dateType: ReportDateType | null;
  readonly items: readonly ResponsibleReportItem[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly summary: {
    readonly expense?: ExpenseReportSummary;
    readonly income?: IncomeReportSummary;
  };
}

export interface CardReportSummary {
  readonly purchaseAmount: number;
  readonly invoiceAmount: number;
  readonly paidAmount: number;
  readonly creditAmount: number;
}

export interface CardReportPurchaseInstallment {
  readonly installmentNumber: number;
  readonly dueDate: string;
  readonly amount: number;
}

export interface CardReportPurchase {
  readonly expenseId: string;
  readonly description: string;
  readonly expenseDate: string;
  readonly original: number;
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
  readonly status: ExpenseStatus;
  readonly totalInstallments: number;
  readonly installments: readonly CardReportPurchaseInstallment[];
}

export interface CardReportCreditApplication {
  readonly id: string;
  readonly creditId: string;
  readonly invoiceId: string;
  readonly installmentId: string;
  readonly amount: number;
  readonly createdAt: string;
}

export interface ReportInstallmentAdjustment {
  readonly id: string;
  readonly expenseId: string;
  readonly installmentId: string;
  readonly type: AdjustmentType;
  readonly amount: number;
  readonly reason: string;
  readonly status: AdjustmentStatus;
  readonly createdAt: string;
}

export interface CardReportItem {
  readonly creditCardId: string;
  readonly name: string;
  readonly holderName: string;
  readonly lastFourDigits: string | null;
  readonly active: boolean;
  readonly summary: CardReportSummary;
  readonly purchases: readonly CardReportPurchase[];
  readonly invoices: readonly Invoice[];
  readonly payments: readonly InvoicePayment[];
  readonly credits: readonly CardReportCreditApplication[];
  readonly installmentAdjustments: readonly ReportInstallmentAdjustment[];
  readonly invoiceAdjustments: readonly InvoiceAdjustment[];
}

export interface CardReportResponse {
  readonly period: ReportPeriod;
  readonly items: readonly CardReportItem[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly summary: CardReportSummary;
}

export interface CashFlowItem {
  readonly id: string;
  readonly type: CashFlowType;
  readonly date: string;
  readonly amount: number;
  readonly accountId: string | null;
  readonly description: string;
}

export interface CashFlowHistorical {
  readonly openingBalance?: number;
  readonly closingBalance?: number;
  readonly items: readonly CashFlowItem[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly summary: {
    readonly totalIn: number;
    readonly totalOut: number;
    readonly net: number;
  };
}

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

export interface CashFlowProjectedEmpty {
  readonly empty: true;
}

export interface CashFlowProjectedData {
  readonly empty?: boolean;
  readonly summary: ProjectionSummary;
  readonly months: readonly ProjectionMonth[];
  readonly quarters: readonly ProjectionQuarter[];
}

export type CashFlowProjected = CashFlowProjectedEmpty | CashFlowProjectedData;

export interface CashFlowResponse {
  readonly period: ReportPeriod;
  readonly flowType: CashFlowFlowType;
  readonly accountId: string | null;
  readonly historical?: CashFlowHistorical;
  readonly projected?: CashFlowProjected;
}

export interface InvoiceReportCard {
  readonly name: string;
  readonly holderName: string;
  readonly lastFourDigits: string | null;
}

export interface InvoiceReportHeader {
  readonly referenceYear: number;
  readonly referenceMonth: number;
  readonly closingDate: string;
  readonly dueDate: string;
  readonly status: InvoiceStatus;
  readonly totalAmount: number;
  readonly paidAmount: number;
  readonly remainingAmount: number;
}

export interface InvoiceReportPurchase {
  readonly expenseId: string;
  readonly description: string;
  readonly expenseDate: string;
  readonly original: number;
  readonly categoryName: string;
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
  readonly installmentNumber: number;
  readonly totalInstallments: number;
  readonly discount: number;
  readonly surcharge: number;
}

export interface InvoiceReportCategoryGroup {
  readonly name: string;
  readonly original: number;
}

export interface InvoiceReportResponsibleGroup {
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
  readonly original: number;
}

export interface InvoiceReportAllocation {
  readonly id: string;
  readonly type: InvoiceReportAllocationType;
  readonly sourceId: string;
  readonly installmentId: string;
  readonly amount: number;
  readonly createdAt: string;
}

export interface InvoiceReportResponse {
  readonly invoiceId: string;
  readonly card: InvoiceReportCard;
  readonly invoice: InvoiceReportHeader;
  readonly purchases: readonly InvoiceReportPurchase[];
  readonly byCategory: readonly InvoiceReportCategoryGroup[];
  readonly byResponsible: readonly InvoiceReportResponsibleGroup[];
  readonly installmentAdjustments: readonly ReportInstallmentAdjustment[];
  readonly invoiceAdjustments: readonly InvoiceAdjustment[];
  readonly credits: readonly CardReportCreditApplication[];
  readonly payments: readonly InvoicePayment[];
  readonly allocations: readonly InvoiceReportAllocation[];
}

export interface ExpenseReportParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly status?: ExpenseStatus;
  readonly categoryId?: string;
  readonly accountId?: string;
  readonly creditCardId?: string;
  readonly responsibleType?: ResponsibleType;
  readonly responsibleName?: string;
  readonly paymentMethod?: PaymentMethod;
  readonly sort?: ExpenseReportSortField;
  readonly direction?: SortDirection;
  readonly page?: number;
  readonly size?: number;
}

export interface IncomeReportParams {
  readonly dateType: ReportDateType;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly status?: IncomeStatus;
  readonly categoryId?: string;
  readonly accountId?: string;
  readonly responsibleType?: ResponsibleType;
  readonly responsibleName?: string;
  readonly sort?: IncomeReportSortField;
  readonly direction?: SortDirection;
  readonly page?: number;
  readonly size?: number;
}

export interface CategoryReportParams {
  readonly dateType: ReportDateType;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly sort?: CategoryReportSortField;
  readonly direction?: SortDirection;
  readonly page?: number;
  readonly size?: number;
}

export interface ResponsibleReportParams {
  readonly nature?: ReportNature;
  readonly dateType?: ReportDateType;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly sort?: ResponsibleReportSortField;
  readonly direction?: SortDirection;
  readonly page?: number;
  readonly size?: number;
}

export interface CardReportParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly creditCardId?: string;
  readonly sort?: CardReportSortField;
  readonly direction?: SortDirection;
  readonly page?: number;
  readonly size?: number;
}

export interface CashFlowReportParams {
  readonly flowType?: CashFlowFlowType;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly accountId?: string;
  readonly sort?: CashFlowSortField;
  readonly direction?: SortDirection;
  readonly page?: number;
  readonly size?: number;
}

export interface InvoiceReportParams {
  readonly responsibleType?: ResponsibleType;
  readonly responsibleName?: string;
}

export const REPORT_TYPE_OPTIONS: readonly { value: ReportType; label: string }[] = [
  { value: 'expenses', label: 'Despesas' },
  { value: 'incomes', label: 'Receitas' },
  { value: 'categories', label: 'Categorias' },
  { value: 'responsibles', label: 'Responsáveis' },
  { value: 'cards', label: 'Cartões' },
  { value: 'cash-flow', label: 'Fluxo de caixa' },
  { value: 'invoices', label: 'Fatura' },
];

export const EXPENSE_STATUS_OPTIONS: readonly { value: ExpenseStatus; label: string }[] = [
  { value: 'OPEN', label: 'Aberta' },
  { value: 'PARTIALLY_PAID', label: 'Parcialmente paga' },
  { value: 'PAID', label: 'Paga' },
  { value: 'CANCELLED', label: 'Cancelada' },
  { value: 'REFUNDED', label: 'Estornada' },
];

export const INCOME_STATUS_OPTIONS: readonly { value: IncomeStatus; label: string }[] = [
  { value: 'EXPECTED', label: 'Esperada' },
  { value: 'RECEIVED', label: 'Recebida' },
  { value: 'CANCELLED', label: 'Cancelada' },
];

export const PAYMENT_METHOD_OPTIONS: readonly { value: PaymentMethod; label: string }[] = [
  { value: 'ACCOUNT', label: 'Conta' },
  { value: 'CREDIT_CARD', label: 'Cartão' },
  { value: 'NONE', label: 'Sem conta' },
];

export const RESPONSIBLE_TYPE_OPTIONS: readonly { value: ResponsibleType; label: string }[] = [
  { value: 'MINE', label: 'Minha' },
  { value: 'GIULIA', label: 'Giulia' },
  { value: 'EDERSON', label: 'Ederson' },
  { value: 'ELISIANE', label: 'Elisiane' },
  { value: 'OTHER', label: 'Outro' },
];

export const DATE_TYPE_OPTIONS: readonly { value: ReportDateType; label: string }[] = [
  { value: 'EXPECTED', label: 'Competência (esperada)' },
  { value: 'RECEIVED', label: 'Recebimento' },
];

export const NATURE_OPTIONS: readonly { value: ReportNature; label: string }[] = [
  { value: 'BOTH', label: 'Despesas e receitas' },
  { value: 'EXPENSE', label: 'Somente despesas' },
  { value: 'INCOME', label: 'Somente receitas' },
];

export const FLOW_TYPE_OPTIONS: readonly { value: CashFlowFlowType; label: string }[] = [
  { value: 'BOTH', label: 'Histórico e projetado' },
  { value: 'HISTORICAL', label: 'Histórico' },
  { value: 'PROJECTED', label: 'Projetado' },
];

export const EXPENSE_SORT_OPTIONS: readonly { value: ExpenseReportSortField; label: string }[] = [
  { value: 'dueDate', label: 'Vencimento' },
  { value: 'expenseDate', label: 'Data da despesa' },
  { value: 'description', label: 'Descrição' },
  { value: 'status', label: 'Situação' },
  { value: 'createdAt', label: 'Criação' },
  { value: 'periodObligation', label: 'Obrigação do período' },
  { value: 'periodRemaining', label: 'Restante do período' },
];

export const INCOME_SORT_OPTIONS: readonly { value: IncomeReportSortField; label: string }[] = [
  { value: 'expectedDate', label: 'Data esperada' },
  { value: 'description', label: 'Descrição' },
  { value: 'amount', label: 'Valor' },
  { value: 'status', label: 'Situação' },
  { value: 'createdAt', label: 'Criação' },
  { value: 'receivedAmount', label: 'Recebido' },
];

export const CATEGORY_SORT_OPTIONS: readonly { value: CategoryReportSortField; label: string }[] = [
  { value: 'name', label: 'Nome' },
  { value: 'type', label: 'Tipo' },
];

export const RESPONSIBLE_SORT_OPTIONS: readonly {
  value: ResponsibleReportSortField;
  label: string;
}[] = [
  { value: 'responsibleType', label: 'Tipo de responsável' },
  { value: 'responsibleName', label: 'Nome do responsável' },
];

export const CARD_SORT_OPTIONS: readonly { value: CardReportSortField; label: string }[] = [
  { value: 'name', label: 'Nome' },
  { value: 'holderName', label: 'Titular' },
];

export const CASH_FLOW_SORT_OPTIONS: readonly { value: CashFlowSortField; label: string }[] = [
  { value: 'date', label: 'Data' },
  { value: 'amount', label: 'Valor' },
  { value: 'type', label: 'Tipo' },
];
