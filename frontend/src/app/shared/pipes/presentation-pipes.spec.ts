import { BrlCurrencyPipe } from './brl-currency.pipe';
import { IsoDatePipe } from './iso-date.pipe';
import { YearMonthPipe } from './year-month.pipe';

describe('shared presentation pipes', () => {
  const brlCurrency = new BrlCurrencyPipe();
  const isoDate = new IsoDatePipe();
  const yearMonth = new YearMonthPipe();

  it('formats BRL in pt-BR without changing the numeric value', () => {
    expect(brlCurrency.transform(9800)).toMatch(/R\$\s*9\.800,00/);
    expect(brlCurrency.transform(0)).toMatch(/R\$\s*0,00/);
  });

  it('formats domain DATE strings without using the local timezone', () => {
    expect(isoDate.transform('2026-08-17')).toBe('17/08/2026');
    expect(isoDate.transform('2026-01-01')).toBe('01/01/2026');
  });

  it('returns the original DATE string when the format is not YYYY-MM-DD', () => {
    expect(isoDate.transform('2026-08-17T00:00:00')).toBe('2026-08-17T00:00:00');
    expect(isoDate.transform('17/08/2026')).toBe('17/08/2026');
  });

  it('formats year-month periods in pt-BR using UTC calendar parts', () => {
    expect(yearMonth.transform('2026-08').toLowerCase()).toContain('2026');
    expect(yearMonth.transform('2026-08').toLowerCase()).toMatch(/ago/);
  });

  it('returns the original period when it is not YYYY-MM', () => {
    expect(yearMonth.transform('2026-13')).toBe('2026-13');
    expect(yearMonth.transform('2026-Q4')).toBe('2026-Q4');
  });
});
