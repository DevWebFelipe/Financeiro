export type InvoiceStatus = 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'PAID' | 'SETTLED_BY_AGREEMENT';

export type InvoiceStatusFilter = '' | InvoiceStatus;

export interface Invoice {
  readonly id: string;
  readonly creditCardId: string;
  readonly referenceYear: number;
  readonly referenceMonth: number;
  readonly closingDate: string;
  readonly dueDate: string;
  readonly status: InvoiceStatus;
  readonly totalAmount: number;
  readonly paidAmount: number;
  readonly remainingAmount: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface InvoiceItem {
  readonly id: string;
  readonly expenseId: string;
  readonly installmentNumber: number;
  readonly totalInstallments: number;
  readonly amount: number;
  readonly remainingAmount: number;
  readonly dueDate: string;
  readonly status: string;
  readonly overdue: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface InvoiceListParams {
  readonly year?: number;
  readonly month?: number;
  readonly status?: InvoiceStatus;
}

export type InvoicePaymentStatus = 'ACTIVE' | 'REVERSED';

export interface InvoicePayment {
  readonly id: string;
  readonly invoiceId: string;
  readonly accountId: string;
  readonly amount: number;
  readonly paymentDate: string;
  readonly notes: string | null;
  readonly status: InvoicePaymentStatus;
  readonly createdAt: string;
}

export interface PayInvoiceRequest {
  readonly accountId: string;
  readonly amount: number;
  readonly paymentDate: string;
  readonly notes?: string;
}

export type InvoiceAdjustmentType = 'DISCOUNT' | 'SURCHARGE';

export type InvoiceAdjustmentStatus = 'ACTIVE' | 'REVERSED';

export interface InvoiceAdjustment {
  readonly id: string;
  readonly invoiceId: string;
  readonly type: InvoiceAdjustmentType;
  readonly amount: number;
  readonly reason: string;
  readonly status: InvoiceAdjustmentStatus;
  readonly createdAt: string;
}

export interface CreateInvoiceAdjustmentRequest {
  readonly type: InvoiceAdjustmentType;
  readonly amount: number;
  readonly reason: string;
}

export const INVOICE_ADJUSTMENT_TYPE_OPTIONS: readonly {
  value: InvoiceAdjustmentType;
  label: string;
}[] = [
  { value: 'DISCOUNT', label: 'Desconto' },
  { value: 'SURCHARGE', label: 'Acréscimo' },
];

export const INVOICE_STATUS_OPTIONS: readonly { value: InvoiceStatus; label: string }[] = [
  { value: 'SCHEDULED', label: 'Agendada' },
  { value: 'OPEN', label: 'Aberta' },
  { value: 'CLOSED', label: 'Fechada' },
  { value: 'PAID', label: 'Paga' },
  { value: 'SETTLED_BY_AGREEMENT', label: 'Liquidada por acordo' },
];

export const INVOICE_MONTH_OPTIONS: readonly { value: number; label: string }[] = [
  { value: 1, label: 'Janeiro' },
  { value: 2, label: 'Fevereiro' },
  { value: 3, label: 'Março' },
  { value: 4, label: 'Abril' },
  { value: 5, label: 'Maio' },
  { value: 6, label: 'Junho' },
  { value: 7, label: 'Julho' },
  { value: 8, label: 'Agosto' },
  { value: 9, label: 'Setembro' },
  { value: 10, label: 'Outubro' },
  { value: 11, label: 'Novembro' },
  { value: 12, label: 'Dezembro' },
];
