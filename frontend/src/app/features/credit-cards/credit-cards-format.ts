export function creditCardStatusLabel(active: boolean): string {
  return active ? 'Ativo' : 'Inativo';
}

export function formatLastFourDigits(lastFourDigits: string | null): string | null {
  if (lastFourDigits == null || lastFourDigits.length === 0) {
    return null;
  }
  return `•••• ${lastFourDigits}`;
}
