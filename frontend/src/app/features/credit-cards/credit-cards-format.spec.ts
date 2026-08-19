import { creditCardStatusLabel, formatLastFourDigits } from './credit-cards-format';

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
});
