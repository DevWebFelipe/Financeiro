import { Pipe, PipeTransform } from '@angular/core';

const YEAR_MONTH = /^(\d{4})-(\d{2})$/;

const monthFormat = new Intl.DateTimeFormat('pt-BR', {
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

export function formatYearMonth(value: string): string {
  const match = YEAR_MONTH.exec(value);
  if (match == null) {
    return value;
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  if (month < 1 || month > 12) {
    return value;
  }

  return monthFormat.format(new Date(Date.UTC(year, month - 1, 1)));
}

@Pipe({
  name: 'yearMonth',
})
export class YearMonthPipe implements PipeTransform {
  transform(value: string): string {
    return formatYearMonth(value);
  }
}
