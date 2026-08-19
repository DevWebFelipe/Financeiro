import { Component, computed, input } from '@angular/core';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { YearMonthPipe } from '../../shared/pipes/year-month.pipe';
import { DashboardProjectionMonth } from './dashboard.models';

@Component({
  selector: 'app-dashboard-projection-chart',
  imports: [BrlCurrencyPipe, YearMonthPipe],
  templateUrl: './dashboard-projection-chart.html',
  styleUrl: './dashboard-projection-chart.css',
})
export class DashboardProjectionChart {
  readonly months = input.required<readonly DashboardProjectionMonth[]>();

  readonly scale = computed(() => {
    let max = 0;
    for (const month of this.months()) {
      max = Math.max(max, month.totalIncome, month.totalExpense);
    }
    return max;
  });

  barPercent(value: number): number {
    const scale = this.scale();
    if (scale <= 0) {
      return 0;
    }
    return (value / scale) * 100;
  }
}
