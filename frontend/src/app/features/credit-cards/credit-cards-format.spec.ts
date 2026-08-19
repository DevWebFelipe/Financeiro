import {
  creditAvailabilityLabel,
  creditCardStatusLabel,
  creditOriginLabel,
  formatCreditInstantDate,
  formatLastFourDigits,
  sumRemainingCredits,
} from './credit-cards-format';

describe('credit-cards-format', () => {
  it('labels active and inactive cards without relying on color', () => {
    expect(creditCardStatusLabel(true)).toBe('Ativo');
    expect(creditCardStatusLabel(false)).toBe('Inativo');
  });

  it('formats last four digits only when a value exists', () => {
    expect(formatLastFourDigits('1234')).toBe('•••• 1234');
    expect(formatLastFourDigits(null)).toBeNull();
    expect(formatLastFourDigits('')).toBeNull();
  });

  it('labels official credit origins', () => {
    expect(creditOriginLabel('MANUAL')).toBe('Crédito manual');
    expect(creditOriginLabel('CARD_PURCHASE_REFUND')).toBe('Estorno de compra');
  });

  it('classifies availability visually from remainingAmount', () => {
    expect(creditAvailabilityLabel(0.01)).toBe('Disponível');
    expect(creditAvailabilityLabel(0)).toBe('Utilizado');
  });

  it('sums official remainingAmount values for presentation', () => {
    expect(sumRemainingCredits([{ remainingAmount: 40 }, { remainingAmount: 0 }])).toBe(40);
  });

  it('formats credit createdAt as a civil date in America/Sao_Paulo', () => {
    expect(formatCreditInstantDate('2026-08-20T12:00:00Z')).toBe('20/08/2026');
  });
});
