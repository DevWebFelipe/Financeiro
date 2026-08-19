const currencyFormat = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

const progressFormat = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatBrl(value: number): string {
  return currencyFormat.format(value);
}

export function formatProgressPercent(value: number): string {
  return `${progressFormat.format(value)}%`;
}
