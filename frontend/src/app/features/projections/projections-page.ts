import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, EMPTY, of, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { IsoDatePipe } from '../../shared/pipes/iso-date.pipe';
import { YearMonthPipe } from '../../shared/pipes/year-month.pipe';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import {
  formatProjectionQuarter,
  projectionAccountAssignmentLabel,
  projectionDirectionLabel,
  projectionEventTypeLabel,
  projectionOverdueLabel,
  undatedEventDateLabel,
} from './projections-format';
import {
  PROJECTION_MONTHS_COUNT_OPTIONS,
  PROJECTION_PERIOD_MODE_OPTIONS,
  ProjectionEvent,
  ProjectionPeriodMode,
  ProjectionQueryParams,
  ProjectionResponse,
} from './projections.models';
import { ProjectionsService } from './projections.service';

const PAGE_SIZE = 20;
const MAX_MONTHS = 12;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

@Component({
  selector: 'app-projections-page',
  imports: [EmptyState, ErrorState, BrlCurrencyPipe, IsoDatePipe, YearMonthPipe],
  templateUrl: './projections-page.html',
  styleUrl: './projections-page.css',
})
export class ProjectionsPage {
  private readonly projectionsService = inject(ProjectionsService);
  private readonly accountsService = inject(AccountsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly data = signal<ProjectionResponse | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly accounts = signal<Account[]>([]);

  readonly pageIndex = signal(0);
  readonly periodMode = signal<ProjectionPeriodMode>('default');
  readonly accountFilter = signal('');
  readonly startDateFilter = signal('');
  readonly endDateFilter = signal('');
  readonly yearMonthFilter = signal('');
  readonly monthsCount = signal('');

  readonly periodModeOptions = PROJECTION_PERIOD_MODE_OPTIONS;
  readonly monthsCountOptions = PROJECTION_MONTHS_COUNT_OPTIONS;
  readonly undatedDateLabel = undatedEventDateLabel();
  readonly formatQuarter = formatProjectionQuarter;

  readonly summary = computed(() => this.data()?.summary ?? null);
  readonly months = computed(() => this.data()?.months ?? []);
  readonly quarters = computed(() => this.data()?.quarters ?? []);
  readonly events = computed(() => this.data()?.events.items ?? []);
  readonly undatedEvents = computed(() => this.data()?.undatedEvents ?? []);
  readonly totalItems = computed(() => this.data()?.events.totalItems ?? 0);
  readonly totalPages = computed(() => this.data()?.events.totalPages ?? 0);
  readonly hasFilters = computed(
    () => this.periodMode() !== 'default' || this.accountFilter() !== '',
  );
  readonly isEmpty = computed(
    () =>
      this.status() === 'loaded' &&
      this.months().length === 0 &&
      this.events().length === 0 &&
      this.undatedEvents().length === 0,
  );
  readonly eventsEmpty = computed(
    () => this.status() === 'loaded' && this.events().length === 0 && !this.isEmpty(),
  );
  readonly canGoPrev = computed(() => this.pageIndex() > 0);
  readonly canGoNext = computed(() => {
    const total = this.totalPages();
    return total > 0 && this.pageIndex() < total - 1;
  });
  readonly periodHint = computed(() => this.periodValidationMessage());

  constructor() {
    this.accountsService
      .list()
      .pipe(
        catchError(() => of([] as Account[])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((accounts) => this.accounts.set(accounts));

    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() => {
          const params = this.queryParams();
          if (params == null) {
            this.status.set('loaded');
            this.error.set(null);
            return EMPTY;
          }
          this.status.set('loading');
          this.error.set(null);
          return this.projectionsService.get(params).pipe(
            catchError((loadError: unknown) => {
              this.error.set(isApiError(loadError) ? loadError : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((projection) => {
        this.data.set(projection);
        this.status.set('loaded');
      });
  }

  retry(): void {
    this.reload.next();
  }

  goToPage(page: number): void {
    if (page < 0 || (this.totalPages() > 0 && page >= this.totalPages())) {
      return;
    }
    this.pageIndex.set(page);
    this.reload.next();
  }

  onPeriodModeChange(value: ProjectionPeriodMode): void {
    this.periodMode.set(value);
    if (value === 'default') {
      this.startDateFilter.set('');
      this.endDateFilter.set('');
      this.yearMonthFilter.set('');
      this.monthsCount.set('');
    }
    if (value === 'fromToday' && this.monthsCount() === '') {
      this.monthsCount.set('3');
    }
    if (value !== 'fromToday' && value !== 'yearMonth') {
      this.monthsCount.set('');
    }
    this.resetAndReload();
  }

  onAccountFilterChange(value: string): void {
    this.accountFilter.set(value);
    this.resetAndReload();
  }

  onStartDateFilterChange(value: string): void {
    this.startDateFilter.set(value);
    this.resetAndReload();
  }

  onEndDateFilterChange(value: string): void {
    this.endDateFilter.set(value);
    this.resetAndReload();
  }

  onYearMonthFilterChange(value: string): void {
    this.yearMonthFilter.set(value);
    this.resetAndReload();
  }

  onMonthsCountChange(value: string): void {
    this.monthsCount.set(value);
    this.resetAndReload();
  }

  eventTypeLabel(type: ProjectionEvent['type']): string {
    return projectionEventTypeLabel(type);
  }

  directionLabel(direction: ProjectionEvent['direction']): string {
    return projectionDirectionLabel(direction);
  }

  assignmentLabel(assignment: ProjectionEvent['accountAssignment']): string {
    return projectionAccountAssignmentLabel(assignment);
  }

  overdueLabel(overdue: boolean): string {
    return projectionOverdueLabel(overdue);
  }

  eventKey(event: ProjectionEvent, index: number): string {
    return `${event.sourceType}:${event.sourceId}:${event.date ?? 'undated'}:${index}`;
  }

  private resetAndReload(): void {
    this.pageIndex.set(0);
    this.reload.next();
  }

  private queryParams(): ProjectionQueryParams | null {
    if (this.periodValidationMessage() != null) {
      return null;
    }

    const accountId = this.accountFilter();
    const base: ProjectionQueryParams = {
      page: this.pageIndex(),
      size: PAGE_SIZE,
      ...(accountId !== '' ? { accountId } : {}),
    };

    switch (this.periodMode()) {
      case 'default':
        return base;
      case 'range':
        return {
          ...base,
          startDate: this.startDateFilter(),
          endDate: this.endDateFilter(),
        };
      case 'yearMonth': {
        const yearMonth = this.parseYearMonth(this.yearMonthFilter());
        if (yearMonth == null) {
          return null;
        }
        const months = this.parseMonthsCount(this.monthsCount());
        return {
          ...base,
          year: yearMonth.year,
          month: yearMonth.month,
          ...(months != null ? { months } : {}),
        };
      }
      case 'fromToday': {
        const months = this.parseMonthsCount(this.monthsCount());
        if (months == null) {
          return null;
        }
        return { ...base, months };
      }
    }
  }

  private periodValidationMessage(): string | null {
    const mode = this.periodMode();
    if (mode === 'range') {
      const start = this.startDateFilter();
      const end = this.endDateFilter();
      if (start === '' || end === '') {
        return 'Informe a data inicial e a data final.';
      }
      if (!ISO_DATE.test(start) || !ISO_DATE.test(end) || start > end) {
        return 'O intervalo de datas é inválido.';
      }
      if (this.monthSpan(start, end) > MAX_MONTHS) {
        return 'O intervalo não pode ultrapassar 12 meses.';
      }
      return null;
    }
    if (mode === 'yearMonth') {
      if (this.parseYearMonth(this.yearMonthFilter()) == null) {
        return 'Informe o mês e o ano.';
      }
      const months = this.monthsCount();
      if (months !== '' && this.parseMonthsCount(months) == null) {
        return 'A quantidade de meses deve ser de 1 a 12.';
      }
      return null;
    }
    if (mode === 'fromToday' && this.parseMonthsCount(this.monthsCount()) == null) {
      return 'Informe a quantidade de meses (1 a 12).';
    }
    return null;
  }

  private parseYearMonth(value: string): { year: number; month: number } | null {
    const match = /^(\d{4})-(\d{2})$/.exec(value);
    if (match == null) {
      return null;
    }
    const year = Number(match[1]);
    const month = Number(match[2]);
    if (!Number.isInteger(year) || !Number.isInteger(month) || month < 1 || month > 12) {
      return null;
    }
    return { year, month };
  }

  private parseMonthsCount(value: string): number | null {
    if (value === '') {
      return null;
    }
    const months = Number(value);
    if (!Number.isInteger(months) || months < 1 || months > MAX_MONTHS) {
      return null;
    }
    return months;
  }

  private monthSpan(startDate: string, endDate: string): number {
    const start = this.parseYearMonth(startDate.slice(0, 7));
    const end = this.parseYearMonth(endDate.slice(0, 7));
    if (start == null || end == null) {
      return MAX_MONTHS + 1;
    }
    return (end.year - start.year) * 12 + (end.month - start.month) + 1;
  }
}
