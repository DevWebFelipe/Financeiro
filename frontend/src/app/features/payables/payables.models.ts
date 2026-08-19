export type PayableItemType = 'INSTALLMENT' | 'INVOICE';

export type PayableOriginStatus =
  | 'OPEN'
  | 'PARTIALLY_PAID'
  | 'PAID'
  | 'CANCELLED'
  | 'REFUNDED'
  | 'SCHEDULED'
  | 'CLOSED'
  | 'SETTLED_BY_AGREEMENT';

export type PayableSortField =
  | 'name'
  | 'purchaseDate'
  | 'dueDate'
  | 'originalAmount'
  | 'remainingAmount'
  | 'status'
  | 'paidAmount';

export type SortDirection = 'asc' | 'desc';

export type PaymentMethod = 'ACCOUNT' | 'CREDIT_CARD' | 'NONE';

export type ResponsibleType = 'MINE' | 'GIULIA' | 'EDERSON' | 'ELISIANE' | 'OTHER';

export interface PayableItem {
  readonly id: string;
  readonly type: PayableItemType;
  readonly expenseId: string | null;
  readonly creditCardId: string | null;
  readonly categoryId: string | null;
  readonly accountId: string | null;
  readonly paymentMethod: PaymentMethod | null;
  readonly name: string;
  readonly purchaseDate: string | null;
  readonly dueDate: string | null;
  readonly originalAmount: number;
  readonly paidAmount: number;
  readonly remainingAmount: number;
  readonly status: string;
  readonly overdue: boolean;
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
}

export interface PayablePage {
  readonly items: PayableItem[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly totalRemaining: number;
  readonly totalOriginal: number;
  readonly totalPaid: number;
}

export interface PayableListParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly year?: number;
  readonly month?: number;
  readonly includeWithoutDueDate?: boolean;
  readonly status?: string;
  readonly overdue?: boolean;
  readonly withoutCreditCard?: boolean;
  readonly categoryId?: string;
  readonly responsibleType?: ResponsibleType;
  readonly search?: string;
  readonly sort?: PayableSortField;
  readonly direction?: SortDirection;
  readonly page?: number;
  readonly size?: number;
}

export type PayableStatusFilter = '' | PayableOriginStatus;
export type OverdueFilter = '' | 'true' | 'false';

export const PAYABLE_STATUS_OPTIONS: readonly {
  value: PayableOriginStatus;
  label: string;
}[] = [
  { value: 'OPEN', label: 'Em aberto' },
  { value: 'PARTIALLY_PAID', label: 'Parcialmente paga' },
  { value: 'PAID', label: 'Paga' },
  { value: 'SCHEDULED', label: 'Agendada' },
  { value: 'CLOSED', label: 'Fechada' },
  { value: 'SETTLED_BY_AGREEMENT', label: 'Liquidada por acordo' },
  { value: 'CANCELLED', label: 'Cancelada' },
  { value: 'REFUNDED', label: 'Estornada' },
];

export const PAYABLE_SORT_OPTIONS: readonly { value: PayableSortField; label: string }[] = [
  { value: 'dueDate', label: 'Vencimento' },
  { value: 'name', label: 'Nome' },
  { value: 'purchaseDate', label: 'Data da origem' },
  { value: 'originalAmount', label: 'Valor original' },
  { value: 'paidAmount', label: 'Valor pago' },
  { value: 'remainingAmount', label: 'Restante' },
  { value: 'status', label: 'Situação' },
];

export const RESPONSIBLE_TYPE_OPTIONS: readonly { value: ResponsibleType; label: string }[] = [
  { value: 'MINE', label: 'Minha' },
  { value: 'GIULIA', label: 'Giulia' },
  { value: 'EDERSON', label: 'Ederson' },
  { value: 'ELISIANE', label: 'Elisiane' },
  { value: 'OTHER', label: 'Outro' },
];
