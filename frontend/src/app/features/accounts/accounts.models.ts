export type AccountType = string;

export type WritableAccountType = 'BANK_ACCOUNT' | 'CASH';

export interface Account {
  readonly id: string;
  readonly name: string;
  readonly type: AccountType;
  readonly initialBalance: number;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AccountBalance {
  readonly accountId: string;
  readonly totalBalance: number;
  readonly reservedAmount: number;
  readonly availableBalance: number;
}

export interface AccountWithBalance {
  readonly account: Account;
  readonly balance: AccountBalance;
}

export interface CreateAccountRequest {
  readonly name: string;
  readonly type: WritableAccountType;
  readonly initialBalance?: number;
}

export interface UpdateAccountRequest {
  readonly name: string;
  readonly type: WritableAccountType;
}

export const ACCOUNT_TYPE_OPTIONS: readonly {
  readonly value: WritableAccountType;
  readonly label: string;
}[] = [
  { value: 'BANK_ACCOUNT', label: 'Conta bancária' },
  { value: 'CASH', label: 'Dinheiro' },
];
