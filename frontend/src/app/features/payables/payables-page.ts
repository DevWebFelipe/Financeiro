import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, EMPTY, of, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { IsoDatePipe } from '../../shared/pipes/iso-date.pipe';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { Category } from '../categories/categories.models';
import { CategoriesService } from '../categories/categories.service';
import {
  payableOriginLabel,
  payableStatusLabel,
  payableTypeLabel,
  paymentMethodLabel,
  responsibleTypeLabel,
} from './payables-format';
import {
  OverdueFilter,
  PAYABLE_SORT_OPTIONS,
  PAYABLE_STATUS_OPTIONS,
  PayableItem,
  PayableListParams,
  PayablePage,
  PayableSortField,
  PayableStatusFilter,
  RESPONSIBLE_TYPE_OPTIONS,
  ResponsibleType,
  SortDirection,
} from './payables.models';
import { PayablesService } from './payables.service';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-payables-page',
  imports: [EmptyState, ErrorState, BrlCurrencyPipe, IsoDatePipe],
  templateUrl: './payables-page.html',
  styleUrl: './payables-page.css',
})
export class PayablesPage {
  private readonly payablesService = inject(PayablesService);
  private readonly categoriesService = inject(CategoriesService);
  private readonly accountsService = inject(AccountsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly pageData = signal<PayablePage | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly selectedItem = signal<PayableItem | null>(null);

  readonly categories = signal<Category[]>([]);
  readonly accounts = signal<Account[]>([]);

  readonly pageIndex = signal(0);
  readonly statusFilter = signal<PayableStatusFilter>('');
  readonly overdueFilter = signal<OverdueFilter>('');
  readonly categoryFilter = signal('');
  readonly responsibleFilter = signal<'' | ResponsibleType>('');
  readonly startDateFilter = signal('');
  readonly endDateFilter = signal('');
  readonly yearMonthFilter = signal('');
  readonly searchFilter = signal('');
  readonly withoutCreditCard = signal(false);
  readonly includeWithoutDueDate = signal(false);
  readonly sortField = signal<PayableSortField | ''>('');
  readonly sortDirection = signal<SortDirection>('asc');

  readonly statusOptions = PAYABLE_STATUS_OPTIONS;
  readonly sortOptions = PAYABLE_SORT_OPTIONS;
  readonly responsibleOptions = RESPONSIBLE_TYPE_OPTIONS;

  readonly items = computed(() => this.pageData()?.items ?? []);
  readonly totalItems = computed(() => this.pageData()?.totalItems ?? 0);
  readonly totalPages = computed(() => this.pageData()?.totalPages ?? 0);
  readonly totalRemaining = computed(() => this.pageData()?.totalRemaining ?? null);
  readonly totalOriginal = computed(() => this.pageData()?.totalOriginal ?? null);
  readonly totalPaid = computed(() => this.pageData()?.totalPaid ?? null);
  readonly isEmpty = computed(() => this.status() === 'loaded' && this.items().length === 0);
  readonly hasFilters = computed(
    () =>
      this.statusFilter() !== '' ||
      this.overdueFilter() !== '' ||
      this.categoryFilter() !== '' ||
      this.responsibleFilter() !== '' ||
      this.startDateFilter() !== '' ||
      this.endDateFilter() !== '' ||
      this.yearMonthFilter() !== '' ||
      this.searchFilter() !== '' ||
      this.withoutCreditCard() ||
      this.includeWithoutDueDate(),
  );
  readonly showTotals = computed(
    () => this.status() === 'loaded' && (this.totalItems() > 0 || this.hasFilters()),
  );
  readonly canGoPrev = computed(() => this.pageIndex() > 0);
  readonly canGoNext = computed(() => {
    const total = this.totalPages();
    return total > 0 && this.pageIndex() < total - 1;
  });
  readonly hasPeriodFilter = computed(
    () =>
      this.startDateFilter() !== '' || this.endDateFilter() !== '' || this.yearMonthFilter() !== '',
  );

  constructor() {
    this.categoriesService
      .list({ type: 'EXPENSE' })
      .pipe(
        catchError(() => of([] as Category[])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((categories) => this.categories.set(categories));

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
          this.status.set('loading');
          this.pageData.set(null);
          this.error.set(null);
          return this.payablesService.list(this.listParams()).pipe(
            catchError((loadError: unknown) => {
              this.error.set(isApiError(loadError) ? loadError : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.pageData.set(page);
        this.status.set('loaded');
      });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeDetail();
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

  onStatusFilterChange(value: PayableStatusFilter): void {
    this.statusFilter.set(value);
    this.resetAndReload();
  }

  onOverdueFilterChange(value: OverdueFilter): void {
    this.overdueFilter.set(value);
    this.resetAndReload();
  }

  onCategoryFilterChange(value: string): void {
    this.categoryFilter.set(value);
    this.resetAndReload();
  }

  onResponsibleFilterChange(value: '' | ResponsibleType): void {
    this.responsibleFilter.set(value);
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

  onSearchFilterChange(value: string): void {
    this.searchFilter.set(value.trim());
    this.resetAndReload();
  }

  onWithoutCreditCardChange(checked: boolean): void {
    this.withoutCreditCard.set(checked);
    this.resetAndReload();
  }

  onIncludeWithoutDueDateChange(checked: boolean): void {
    this.includeWithoutDueDate.set(checked);
    this.resetAndReload();
  }

  onSortFieldChange(value: PayableSortField | ''): void {
    this.sortField.set(value);
    this.resetAndReload();
  }

  onSortDirectionChange(value: SortDirection): void {
    this.sortDirection.set(value);
    this.resetAndReload();
  }

  categoryName(categoryId: string | null): string {
    if (categoryId == null) {
      return '—';
    }
    return this.categories().find((item) => item.id === categoryId)?.name ?? '—';
  }

  accountName(accountId: string | null): string {
    if (accountId == null) {
      return '—';
    }
    return this.accounts().find((item) => item.id === accountId)?.name ?? '—';
  }

  typeLabel(type: PayableItem['type']): string {
    return payableTypeLabel(type);
  }

  statusLabel(status: string): string {
    return payableStatusLabel(status);
  }

  methodLabel(method: PayableItem['paymentMethod']): string {
    return paymentMethodLabel(method);
  }

  responsibleLabel(type: PayableItem['responsibleType']): string {
    return responsibleTypeLabel(type);
  }

  originLabel(item: PayableItem): string {
    return payableOriginLabel(item);
  }

  itemKey(item: PayableItem): string {
    return `${item.type}:${item.id}`;
  }

  openDetail(item: PayableItem): void {
    this.selectedItem.set(item);
  }

  closeDetail(): void {
    this.selectedItem.set(null);
  }

  private resetAndReload(): void {
    this.pageIndex.set(0);
    this.selectedItem.set(null);
    this.reload.next();
  }

  private listParams(): PayableListParams {
    const status = this.statusFilter();
    const overdue = this.overdueFilter();
    const categoryId = this.categoryFilter();
    const responsibleType = this.responsibleFilter();
    const startDate = this.startDateFilter();
    const endDate = this.endDateFilter();
    const yearMonth = this.yearMonthFilter();
    const search = this.searchFilter();
    const sort = this.sortField();
    const yearMonthParts = this.parseYearMonth(yearMonth);

    return {
      page: this.pageIndex(),
      size: PAGE_SIZE,
      ...(status !== '' ? { status } : {}),
      ...(overdue === 'true' ? { overdue: true } : overdue === 'false' ? { overdue: false } : {}),
      ...(categoryId !== '' ? { categoryId } : {}),
      ...(responsibleType !== '' ? { responsibleType } : {}),
      ...(startDate !== '' ? { startDate } : {}),
      ...(endDate !== '' ? { endDate } : {}),
      ...(yearMonthParts != null ? yearMonthParts : {}),
      ...(search !== '' ? { search } : {}),
      ...(this.withoutCreditCard() ? { withoutCreditCard: true } : {}),
      ...(this.includeWithoutDueDate() ? { includeWithoutDueDate: true } : {}),
      ...(sort !== '' ? { sort, direction: this.sortDirection() } : {}),
    };
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
}
