import { todayIsoDate } from './today-iso-date';

describe('todayIsoDate', () => {
  it('returns YYYY-MM-DD for the civil date in America/Sao_Paulo', () => {
    expect(todayIsoDate(new Date('2026-08-19T15:00:00.000Z'))).toBe('2026-08-19');
  });

  it('keeps the previous Sao Paulo day when UTC has already crossed midnight', () => {
    expect(todayIsoDate(new Date('2026-08-19T02:30:00.000Z'))).toBe('2026-08-18');
  });

  it('switches to the next Sao Paulo day at local midnight', () => {
    expect(todayIsoDate(new Date('2026-08-19T03:00:00.000Z'))).toBe('2026-08-19');
  });
});
