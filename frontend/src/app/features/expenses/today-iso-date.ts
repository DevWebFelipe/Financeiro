const FINANCIAL_TIME_ZONE = 'America/Sao_Paulo';

export function todayIsoDate(now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: FINANCIAL_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);

  const year = findPart(parts, 'year');
  const month = findPart(parts, 'month');
  const day = findPart(parts, 'day');
  return `${year}-${month}-${day}`;
}

function findPart(parts: Intl.DateTimeFormatPart[], type: Intl.DateTimeFormatPartTypes): string {
  return parts.find((part) => part.type === type)?.value ?? '';
}
