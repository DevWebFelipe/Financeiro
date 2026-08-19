import { Pipe, PipeTransform } from '@angular/core';

const currencyFormat = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

export function formatBrl(value: number): string {
  return currencyFormat.format(value);
}

@Pipe({
  name: 'brlCurrency',
})
export class BrlCurrencyPipe implements PipeTransform {
  transform(value: number): string {
    return formatBrl(value);
  }
}
