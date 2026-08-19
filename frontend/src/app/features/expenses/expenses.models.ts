export type ExpenseStatus = 'OPEN' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED' | 'REFUNDED';

export type PaymentMethod = 'ACCOUNT' | 'CREDIT_CARD' | 'NONE';

export type WritablePaymentMethod = 'ACCOUNT' | 'NONE';

export type ResponsibleType = 'MINE' | 'GIULIA' | 'EDERSON' | 'ELISIANE' | 'OTHER';

export type RefundSettlement = 'CARD_CREDIT' | 'ACCOUNT';

export interface Expense {
  readonly id: string;
  readonly categoryId: string;
  readonly accountId: string | null;
  readonly creditCardId: string | null;
  readonly description: string;
  readonly totalAmount: number;
  readonly expenseDate: string;
  readonly dueDate: string;
  readonly paymentMethod: PaymentMethod;
  readonly status: ExpenseStatus;
  readonly responsibleType: ResponsibleType;
  readonly responsibleName: string | null;
  readonly barcode: string | null;
  readonly notes: string | null;
  readonly overdue: boolean;
  readonly installmentId: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ExpensePage {
  readonly items: Expense[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

export interface ExpenseInstallment {
  readonly id: string;
  readonly expenseId: string;
  readonly installmentNumber: number;
  readonly totalInstallments: number;
  readonly amount: number;
  readonly remainingAmount: number;
  readonly dueDate: string;
  readonly status: ExpenseStatus;
  readonly overdue: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ExpenseListParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly status?: ExpenseStatus;
  readonly categoryId?: string;
  readonly accountId?: string;
  readonly responsibleType?: ResponsibleType;
  readonly paymentMethod?: PaymentMethod;
  readonly page?: number;
  readonly size?: number;
}

export interface CreateExpenseRequest {
  readonly categoryId: string;
  readonly description: string;
  readonly totalAmount: number;
  readonly expenseDate: string;
  readonly dueDate: string;
  readonly paymentMethod: PaymentMethod;
  readonly accountId?: string;
  readonly responsibleType: ResponsibleType;
  readonly responsibleName?: string;
  readonly barcode?: string;
  readonly notes?: string;
  readonly installmentCount?: number;
}

export interface UpdateExpenseRequest {
  readonly categoryId: string;
  readonly description: string;
  readonly totalAmount: number;
  readonly expenseDate: string;
  readonly dueDate: string;
  readonly paymentMethod: PaymentMethod;
  readonly accountId?: string;
  readonly responsibleType: ResponsibleType;
  readonly responsibleName?: string;
  readonly barcode?: string;
  readonly notes?: string;
}

export interface PayExpenseRequest {
  readonly accountId?: string;
  readonly amount: number;
  readonly paymentDate: string;
  readonly notes?: string;
}

export interface RefundExpenseRequest {
  readonly settlement?: RefundSettlement;
  readonly accountId?: string;
}

export type ExpenseStatusFilter = '' | ExpenseStatus;
export type PaymentMethodFilter = '' | PaymentMethod;
export type ResponsibleTypeFilter = '' | ResponsibleType;

export const EXPENSE_STATUS_OPTIONS: readonly { value: ExpenseStatus; label: string }[] = [
  { value: 'OPEN', label: 'Aberta' },
  { value: 'PARTIALLY_PAID', label: 'Parcialmente paga' },
  { value: 'PAID', label: 'Paga' },
  { value: 'CANCELLED', label: 'Cancelada' },
  { value: 'REFUNDED', label: 'Estornada' },
];

export const PAYMENT_METHOD_OPTIONS: readonly { value: PaymentMethod; label: string }[] = [
  { value: 'ACCOUNT', label: 'Conta' },
  { value: 'NONE', label: 'Sem conta' },
];

export const RESPONSIBLE_TYPE_OPTIONS: readonly { value: ResponsibleType; label: string }[] = [
  { value: 'MINE', label: 'Minha' },
  { value: 'GIULIA', label: 'Giulia' },
  { value: 'EDERSON', label: 'Ederson' },
  { value: 'ELISIANE', label: 'Elisiane' },
  { value: 'OTHER', label: 'Outro' },
];
