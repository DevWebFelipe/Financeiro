import { AccountType } from './accounts.models';

export function accountTypeLabel(type: AccountType): string {
  if (type === 'CASH') {
    return 'Dinheiro';
  }
  if (type === 'BANK_ACCOUNT') {
    return 'Conta bancária';
  }
  return type;
}

export function accountStatusLabel(active: boolean): string {
  return active ? 'Ativa' : 'Inativa';
}

export function canDeactivateAccount(
  active: boolean,
  totalBalance: number,
  reservedAmount: number,
): boolean {
  return active && totalBalance === 0 && reservedAmount === 0;
}
