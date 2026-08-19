import { AccountType } from './dashboard.models';

const currencyFormat = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

const monthFormat = new Intl.DateTimeFormat('pt-BR', {
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

export function formatBrl(value: number): string {
  return currencyFormat.format(value);
}

export function formatIsoDate(isoDate: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate);
  if (match == null) {
    return isoDate;
  }
  return `${match[3]}/${match[2]}/${match[1]}`;
}

export function formatYearMonth(period: string): string {
  const match = /^(\d{4})-(\d{2})$/.exec(period);
  if (match == null) {
    return period;
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  if (month < 1 || month > 12) {
    return period;
  }

  return monthFormat.format(new Date(Date.UTC(year, month - 1, 1)));
}

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
