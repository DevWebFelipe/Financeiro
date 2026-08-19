export function creditCardStatusLabel(active: boolean): string {
  return active ? 'Ativo' : 'Inativo';
}

export function formatLastFourDigits(lastFourDigits: string | null): string | null {
  if (lastFourDigits == null || lastFourDigits.length === 0) {
    return null;
  }
  return `•••• ${lastFourDigits}`;
}

export function creditOriginLabel(origin: string): string {
  switch (origin) {
    case 'MANUAL':
      return 'Crédito manual';
    case 'CARD_PURCHASE_REFUND':
      return 'Estorno de compra';
    default:
      return origin;
  }
}

export function creditAvailabilityLabel(remainingAmount: number): string {
  return remainingAmount > 0 ? 'Disponível' : 'Utilizado';
}

export function sumRemainingCredits(credits: readonly { remainingAmount: number }[]): number {
  return credits.reduce((total, credit) => total + credit.remainingAmount, 0);
}

export function formatCreditInstantDate(value: string): string {
  const instant = new Date(value);
  if (Number.isNaN(instant.getTime())) {
    return value;
  }

  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(instant);

  const year = parts.find((part) => part.type === 'year')?.value ?? '';
  const month = parts.find((part) => part.type === 'month')?.value ?? '';
  const day = parts.find((part) => part.type === 'day')?.value ?? '';
  return `${day}/${month}/${year}`;
}
