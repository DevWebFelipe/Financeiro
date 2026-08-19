const FINANCIAL_TIME_ZONE = 'America/Sao_Paulo';

export function todayIsoDate(now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: FINANCIAL_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);
  return `${part(parts, 'year')}-${part(parts, 'month')}-${part(parts, 'day')}`;
}

export function formatIsoDatePt(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (match == null) {
    return value;
  }
  return `${match[3]}/${match[2]}/${match[1]}`;
}

export function shiftIsoDate(isoDate: string, days: number): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  const utc = Date.UTC(year ?? 0, (month ?? 1) - 1, (day ?? 1) + days);
  const shifted = new Date(utc);
  const y = shifted.getUTCFullYear();
  const m = String(shifted.getUTCMonth() + 1).padStart(2, '0');
  const d = String(shifted.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export interface CardCycleDates {
  readonly closingDay: number;
  readonly dueDay: number;
  readonly purchaseDate: string;
}

/**
 * Dates that make a CREDIT_CARD purchase belong to a cycle whose closing_date
 * is already on or before today in America/Sao_Paulo. The first invoice still
 * opens as OPEN; the official scheduler (or the E2E close helper) then closes it.
 */
export function pastCycleCardDates(today: string = todayIsoDate()): CardCycleDates {
  const day = Number(today.slice(8, 10));
  if (day >= 8) {
    const closingDay = day - 3;
    const purchaseDate = `${today.slice(0, 8)}${String(day - 5).padStart(2, '0')}`;
    return {
      closingDay,
      dueDay: Math.min(closingDay + 5, 28),
      purchaseDate,
    };
  }
  const previousMonth = shiftIsoDate(`${today.slice(0, 8)}01`, -1);
  return {
    closingDay: 28,
    dueDay: 5,
    purchaseDate: `${previousMonth.slice(0, 8)}20`,
  };
}

function part(parts: Intl.DateTimeFormatPart[], type: Intl.DateTimeFormatPartTypes): string {
  return parts.find((item) => item.type === type)?.value ?? '';
}
