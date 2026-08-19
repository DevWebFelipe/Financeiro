import { AccountType } from './dashboard.models';

export function formatQuarter(period: string): string {
  const match = /^(\d{4})-Q([1-4])$/.exec(period);
  if (match == null) {
    return period;
  }
  return `${match[2]}º trimestre de ${match[1]}`;
}

export function accountTypeLabel(type: AccountType): string {
  return type === 'CASH' ? 'Dinheiro' : 'Conta bancária';
}
