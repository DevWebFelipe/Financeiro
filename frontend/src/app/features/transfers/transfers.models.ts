export type TransferStatus = 'ACTIVE' | 'REVERSED';

export interface Transfer {
  readonly id: string;
  readonly sourceAccountId: string;
  readonly destinationAccountId: string;
  readonly amount: number;
  readonly transferDate: string;
  readonly description: string | null;
  readonly status: TransferStatus;
  readonly createdAt: string;
}

export interface TransferListParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly accountId?: string;
}

export interface CreateTransferRequest {
  readonly sourceAccountId: string;
  readonly destinationAccountId: string;
  readonly amount: number;
  readonly transferDate: string;
  readonly description?: string;
}
