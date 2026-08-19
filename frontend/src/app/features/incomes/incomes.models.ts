export type IncomeStatus = 'EXPECTED' | 'RECEIVED' | 'CANCELLED';

export type IncomeMovementType = 'ACCRUAL' | 'RECEIPT';

export type IncomeMovementStatus = 'ACTIVE' | 'REVERSED';

export type ResponsibleType = 'MINE' | 'GIULIA' | 'EDERSON' | 'ELISIANE' | 'OTHER';

export interface Income {
  readonly id: string;
  readonly categoryId: string;
  readonly accountId: string | null;
  readonly description: string;
  readonly amount: number;
  readonly expectedDate: string;
  readonly receivedDate: string | null;
  readonly status: IncomeStatus;
  readonly responsibleType: ResponsibleType | null;
  readonly responsibleName: string | null;
  readonly notes: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface IncomePage {
  readonly items: Income[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

export interface IncomeMovement {
  readonly id: string;
  readonly incomeId: string;
  readonly type: IncomeMovementType;
  readonly status: IncomeMovementStatus;
  readonly amount: number;
  readonly movementDate: string;
  readonly accountId: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly reversedAt: string | null;
}

export interface IncomeMovementPage {
  readonly items: IncomeMovement[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

export interface IncomeListParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly status?: IncomeStatus;
  readonly categoryId?: string;
  readonly accountId?: string;
  readonly page?: number;
  readonly size?: number;
}

export interface CreateIncomeRequest {
  readonly categoryId: string;
  readonly description: string;
  readonly amount: number;
  readonly expectedDate: string;
  readonly notes?: string;
  readonly responsibleType?: ResponsibleType;
  readonly responsibleName?: string;
}

export interface UpdateIncomeRequest {
  readonly categoryId: string;
  readonly description: string;
  readonly amount: number;
  readonly expectedDate: string;
  readonly notes?: string;
  readonly responsibleType?: ResponsibleType;
  readonly responsibleName?: string;
}

export interface CreateIncomeReceiptRequest {
  readonly amount: number;
  readonly date: string;
  readonly accountId: string;
}

export type IncomeStatusFilter = '' | IncomeStatus;

export const INCOME_STATUS_OPTIONS: readonly { value: IncomeStatus; label: string }[] = [
  { value: 'EXPECTED', label: 'Esperada' },
  { value: 'RECEIVED', label: 'Recebida' },
  { value: 'CANCELLED', label: 'Cancelada' },
];

export const RESPONSIBLE_TYPE_OPTIONS: readonly { value: ResponsibleType; label: string }[] = [
  { value: 'MINE', label: 'Minha' },
  { value: 'GIULIA', label: 'Giulia' },
  { value: 'EDERSON', label: 'Ederson' },
  { value: 'ELISIANE', label: 'Elisiane' },
  { value: 'OTHER', label: 'Outro' },
];
