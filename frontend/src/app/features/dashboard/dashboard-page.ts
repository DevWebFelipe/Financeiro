import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, EMPTY, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { accountTypeLabel, formatBrl, formatIsoDate, formatQuarter } from './dashboard-format';
import { DashboardProjectionChart } from './dashboard-projection-chart';
import { DashboardResponse } from './dashboard.models';
import { DashboardService } from './dashboard.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [DashboardProjectionChart],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
})
export class DashboardPage {
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly data = signal<DashboardResponse | null>(null);
  readonly error = signal<ApiError | null>(null);

  readonly hasAccounts = computed(() => (this.data()?.accounts.length ?? 0) > 0);
  readonly hasCreditCards = computed(() => (this.data()?.creditCards.length ?? 0) > 0);
  readonly isEmpty = computed(() => {
    const snapshot = this.data();
    return snapshot != null && snapshot.accounts.length === 0 && snapshot.creditCards.length === 0;
  });

  readonly formatBrl = formatBrl;
  readonly formatIsoDate = formatIsoDate;
  readonly formatQuarter = formatQuarter;
  readonly accountTypeLabel = accountTypeLabel;

  constructor() {
    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.status.set('loading');
          this.data.set(null);
          this.error.set(null);
          return this.dashboardService.getDashboard().pipe(
            catchError((error: unknown) => {
              this.error.set(isApiError(error) ? error : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dashboard) => {
        this.data.set(dashboard);
        this.status.set('loaded');
      });
  }

  retry(): void {
    this.reload.next();
  }
}
