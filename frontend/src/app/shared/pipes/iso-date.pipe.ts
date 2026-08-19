import { Pipe, PipeTransform } from '@angular/core';

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

export function formatIsoDate(value: string): string {
  const match = ISO_DATE.exec(value);
  if (match == null) {
    return value;
  }
  return `${match[3]}/${match[2]}/${match[1]}`;
}

@Pipe({
  name: 'isoDate',
})
export class IsoDatePipe implements PipeTransform {
  transform(value: string): string {
    return formatIsoDate(value);
  }
}
