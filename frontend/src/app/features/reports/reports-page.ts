import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, EMPTY, Observable, of, Subject, switchMap, tap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { IsoDatePipe } from '../../shared/pipes/iso-date.pipe';
import { YearMonthPipe } from '../../shared/pipes/year-month.pipe';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { Category } from '../categories/categories.models';
import { CategoriesService } from '../categories/categories.service';
import { CreditCard } from '../credit-cards/credit-cards.models';
import { CreditCardsService } from '../credit-cards/credit-cards.service';
import {
  allocationTypeLabel,
  cashFlowTypeLabel,
  categoryTypeLabel,
  dateTypeLabel,
  expenseStatusLabel,
  formatQuarter,
  incomeStatusLabel,
  invoiceStatusLabel,
  natureLabel,
  originLabel,
  paymentMethodLabel,
  responsibleTypeLabel,
} from './reports-format';
import { ReportsService } from './reports.service';
import {
  CARD_SORT_OPTIONS,
  CASH_FLOW_SORT_OPTIONS,
  CATEGORY_SORT_OPTIONS,
  CardReportParams,
  CardReportResponse,
  CashFlowFlowType,
  CashFlowProjectedData,
  CashFlowReportParams,
  CashFlowResponse,
  CategoryReportParams,
  CategoryReportResponse,
  DATE_TYPE_OPTIONS,
  EXPENSE_SORT_OPTIONS,
  EXPENSE_STATUS_OPTIONS,
  ExpenseReportParams,
  ExpenseReportResponse,
  ExpenseReportSortField,
  ExpenseStatus,
  FLOW_TYPE_OPTIONS,
  INCOME_SORT_OPTIONS,
  INCOME_STATUS_OPTIONS,
  IncomeReportParams,
  IncomeReportResponse,
  IncomeReportSortField,
  IncomeStatus,
  InvoiceReportParams,
  InvoiceReportResponse,
  NATURE_OPTIONS,
  PAYMENT_METHOD_OPTIONS,
  PaymentMethod,
  REPORT_TYPE_OPTIONS,
  RESPONSIBLE_SORT_OPTIONS,
  RESPONSIBLE_TYPE_OPTIONS,
  ReportDateType,
  ReportNature,
  ReportType,
  ResponsibleReportParams,
  ResponsibleReportResponse,
  ResponsibleType,
  SortDirection,
} from './reports.models';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-reports-page',
  imports: [EmptyState, ErrorState, BrlCurrencyPipe, IsoDatePipe, YearMonthPipe],
  templateUrl: './reports-page.html',
  styleUrl: './reports-page.css',
})
export class ReportsPage {
  private readonly reportsService = inject(ReportsService);
  private readonly categoriesService = inject(CategoriesService);
  private readonly accountsService = inject(AccountsService);
  private readonly creditCardsService = inject(CreditCardsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();

  readonly status = signal<'idle' | 'loading' | 'loaded' | 'error'>('idle');
  readonly error = signal<ApiError | null>(null);
  readonly pdfError = signal<ApiError | null>(null);

  readonly expenseReport = signal<ExpenseReportResponse | null>(null);
  readonly incomeReport = signal<IncomeReportResponse | null>(null);
  readonly categoryReport = signal<CategoryReportResponse | null>(null);
  readonly responsibleReport = signal<ResponsibleReportResponse | null>(null);
  readonly cardReport = signal<CardReportResponse | null>(null);
  readonly cashFlowReport = signal<CashFlowResponse | null>(null);
  readonly invoiceReport = signal<InvoiceReportResponse | null>(null);

  readonly categories = signal<Category[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly creditCards = signal<CreditCard[]>([]);

  readonly reportType = signal<ReportType>('expenses');
  readonly pageIndex = signal(0);
  readonly startDate = signal('');
  readonly endDate = signal('');
  readonly sortField = signal('');
  readonly sortDirection = signal<SortDirection>('asc');

  readonly expenseStatus = signal<'' | ExpenseStatus>('');
  readonly categoryId = signal('');
  readonly accountId = signal('');
  readonly creditCardId = signal('');
  readonly responsibleType = signal<'' | ResponsibleType>('');
  readonly responsibleName = signal('');
  readonly paymentMethod = signal<'' | PaymentMethod>('');
  readonly dateType = signal<ReportDateType>('EXPECTED');
  readonly incomeStatus = signal<'' | IncomeStatus>('');
  readonly nature = signal<ReportNature>('BOTH');
  readonly flowType = signal<CashFlowFlowType>('BOTH');
  readonly invoiceId = signal('');

  readonly reportTypeOptions = REPORT_TYPE_OPTIONS;
  readonly expenseStatusOptions = EXPENSE_STATUS_OPTIONS;
  readonly incomeStatusOptions = INCOME_STATUS_OPTIONS;
  readonly paymentMethodOptions = PAYMENT_METHOD_OPTIONS;
  readonly responsibleOptions = RESPONSIBLE_TYPE_OPTIONS;
  readonly dateTypeOptions = DATE_TYPE_OPTIONS;
  readonly natureOptions = NATURE_OPTIONS;
  readonly flowTypeOptions = FLOW_TYPE_OPTIONS;
  readonly expenseSortOptions = EXPENSE_SORT_OPTIONS;
  readonly incomeSortOptions = INCOME_SORT_OPTIONS;
  readonly categorySortOptions = CATEGORY_SORT_OPTIONS;
  readonly responsibleSortOptions = RESPONSIBLE_SORT_OPTIONS;
  readonly cardSortOptions = CARD_SORT_OPTIONS;
  readonly cashFlowSortOptions = CASH_FLOW_SORT_OPTIONS;

  readonly usesPeriod = computed(() => this.reportType() !== 'invoices');
  readonly usesDateType = computed(() => {
    const type = this.reportType();
    if (type === 'incomes' || type === 'categories') {
      return true;
    }
    return type === 'responsibles' && this.nature() !== 'EXPENSE';
  });
  readonly usesPagination = computed(() => this.reportType() !== 'invoices');
  readonly totalPages = computed(() => this.currentTotalPages());
  readonly totalItems = computed(() => this.currentTotalItems());
  readonly canGoPrev = computed(() => this.pageIndex() > 0);
  readonly canGoNext = computed(() => {
    const total = this.totalPages();
    return total > 0 && this.pageIndex() < total - 1;
  });
  readonly isEmpty = computed(() => this.status() === 'loaded' && this.currentIsEmpty());
  readonly projected = computed((): CashFlowProjectedData | null => {
    const projected = this.cashFlowReport()?.projected;
    if (projected == null || projected.empty === true) {
      return null;
    }
    return projected;
  });
  readonly projectedEmpty = computed(() => this.cashFlowReport()?.projected?.empty === true);

  readonly expenseStatusLabel = expenseStatusLabel;
  readonly incomeStatusLabel = incomeStatusLabel;
  readonly paymentMethodLabel = paymentMethodLabel;
  readonly responsibleTypeLabel = responsibleTypeLabel;
  readonly originLabel = originLabel;
  readonly dateTypeLabel = dateTypeLabel;
  readonly natureLabel = natureLabel;
  readonly cashFlowTypeLabel = cashFlowTypeLabel;
  readonly invoiceStatusLabel = invoiceStatusLabel;
  readonly allocationTypeLabel = allocationTypeLabel;
  readonly categoryTypeLabel = categoryTypeLabel;
  readonly formatQuarter = formatQuarter;

  constructor() {
    this.categoriesService
      .list()
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

    this.creditCardsService
      .list()
      .pipe(
        catchError(() => of([] as CreditCard[])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((cards) => this.creditCards.set(cards));

    this.reload
      .pipe(
        switchMap(() => {
          this.status.set('loading');
          this.error.set(null);
          return this.loadCurrent().pipe(
            catchError((loadError: unknown) => {
              this.error.set(isApiError(loadError) ? loadError : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.status.set('loaded');
      });
  }

  consult(): void {
    if (this.reportType() === 'invoices' && this.invoiceId().trim().length === 0) {
      return;
    }
    if (!this.hasValidPeriod()) {
      return;
    }
    this.pageIndex.set(0);
    this.reload.next();
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

  downloadPdf(): void {
    if (this.reportType() === 'invoices' && this.invoiceId().trim().length === 0) {
      return;
    }
    if (!this.hasValidPeriod()) {
      return;
    }
    this.pdfError.set(null);
    this.pdfRequest()
      .pipe(
        catchError((loadError: unknown) => {
          this.pdfError.set(isApiError(loadError) ? loadError : null);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onReportTypeChange(value: ReportType): void {
    this.reportType.set(value);
    this.sortField.set('');
    this.pageIndex.set(0);
    this.status.set('idle');
    this.error.set(null);
    this.pdfError.set(null);
    this.clearReports();
  }

  onStartDateChange(value: string): void {
    this.startDate.set(value);
  }

  onEndDateChange(value: string): void {
    this.endDate.set(value);
  }

  onSortFieldChange(value: string): void {
    this.sortField.set(value);
  }

  onSortDirectionChange(value: SortDirection): void {
    this.sortDirection.set(value);
  }

  onExpenseStatusChange(value: '' | ExpenseStatus): void {
    this.expenseStatus.set(value);
  }

  onCategoryIdChange(value: string): void {
    this.categoryId.set(value);
  }

  onAccountIdChange(value: string): void {
    this.accountId.set(value);
  }

  onCreditCardIdChange(value: string): void {
    this.creditCardId.set(value);
  }

  onResponsibleTypeChange(value: '' | ResponsibleType): void {
    this.responsibleType.set(value);
  }

  onResponsibleNameChange(value: string): void {
    this.responsibleName.set(value);
  }

  onPaymentMethodChange(value: '' | PaymentMethod): void {
    this.paymentMethod.set(value);
  }

  onDateTypeChange(value: ReportDateType): void {
    this.dateType.set(value);
  }

  onIncomeStatusChange(value: '' | IncomeStatus): void {
    this.incomeStatus.set(value);
  }

  onNatureChange(value: ReportNature): void {
    this.nature.set(value);
  }

  onFlowTypeChange(value: CashFlowFlowType): void {
    this.flowType.set(value);
  }

  onInvoiceIdChange(value: string): void {
    this.invoiceId.set(value.trim());
  }

  private loadCurrent(): Observable<unknown> {
    switch (this.reportType()) {
      case 'expenses':
        return this.reportsService
          .listExpenses(this.expenseParams())
          .pipe(tap((report) => this.expenseReport.set(report)));
      case 'incomes':
        return this.reportsService
          .listIncomes(this.incomeParams())
          .pipe(tap((report) => this.incomeReport.set(report)));
      case 'categories':
        return this.reportsService
          .listCategories(this.categoryParams())
          .pipe(tap((report) => this.categoryReport.set(report)));
      case 'responsibles':
        return this.reportsService
          .listResponsibles(this.responsibleParams())
          .pipe(tap((report) => this.responsibleReport.set(report)));
      case 'cards':
        return this.reportsService
          .listCards(this.cardParams())
          .pipe(tap((report) => this.cardReport.set(report)));
      case 'cash-flow':
        return this.reportsService
          .listCashFlow(this.cashFlowParams())
          .pipe(tap((report) => this.cashFlowReport.set(report)));
      case 'invoices':
        return this.reportsService
          .getInvoice(this.invoiceId().trim(), this.invoiceParams())
          .pipe(tap((report) => this.invoiceReport.set(report)));
    }
  }

  private pdfRequest(): Observable<void> {
    switch (this.reportType()) {
      case 'expenses':
        return this.reportsService.downloadExpensesPdf(this.expenseParams());
      case 'incomes':
        return this.reportsService.downloadIncomesPdf(this.incomeParams());
      case 'categories':
        return this.reportsService.downloadCategoriesPdf(this.categoryParams());
      case 'responsibles':
        return this.reportsService.downloadResponsiblesPdf(this.responsibleParams());
      case 'cards':
        return this.reportsService.downloadCardsPdf(this.cardParams());
      case 'cash-flow':
        return this.reportsService.downloadCashFlowPdf(this.cashFlowParams());
      case 'invoices':
        return this.reportsService.downloadInvoicePdf(
          this.invoiceId().trim(),
          this.invoiceParams(),
        );
    }
  }

  private expenseParams(): ExpenseReportParams {
    const status = this.expenseStatus();
    const paymentMethod = this.paymentMethod();
    return {
      ...this.periodParams(),
      ...this.pagingParams(),
      ...(status === '' ? {} : { status }),
      ...(this.categoryId() !== '' ? { categoryId: this.categoryId() } : {}),
      ...(this.accountId() !== '' ? { accountId: this.accountId() } : {}),
      ...(this.creditCardId() !== '' ? { creditCardId: this.creditCardId() } : {}),
      ...this.responsibleParamsSlice(),
      ...(paymentMethod === '' ? {} : { paymentMethod }),
      ...this.sortParams<ExpenseReportSortField>(),
    };
  }

  private incomeParams(): IncomeReportParams {
    const status = this.incomeStatus();
    return {
      dateType: this.dateType(),
      ...this.periodParams(),
      ...this.pagingParams(),
      ...(status === '' ? {} : { status }),
      ...(this.categoryId() !== '' ? { categoryId: this.categoryId() } : {}),
      ...(this.accountId() !== '' ? { accountId: this.accountId() } : {}),
      ...this.responsibleParamsSlice(),
      ...this.sortParams<IncomeReportSortField>(),
    };
  }

  private categoryParams(): CategoryReportParams {
    return {
      dateType: this.dateType(),
      ...this.periodParams(),
      ...this.pagingParams(),
      ...this.sortParams(),
    };
  }

  private responsibleParams(): ResponsibleReportParams {
    return {
      nature: this.nature(),
      ...(this.nature() !== 'EXPENSE' ? { dateType: this.dateType() } : {}),
      ...this.periodParams(),
      ...this.pagingParams(),
      ...this.sortParams(),
    };
  }

  private cardParams(): CardReportParams {
    return {
      ...this.periodParams(),
      ...this.pagingParams(),
      ...(this.creditCardId() !== '' ? { creditCardId: this.creditCardId() } : {}),
      ...this.sortParams(),
    };
  }

  private cashFlowParams(): CashFlowReportParams {
    return {
      flowType: this.flowType(),
      ...this.periodParams(),
      ...this.pagingParams(),
      ...(this.accountId() !== '' ? { accountId: this.accountId() } : {}),
      ...this.sortParams(),
    };
  }

  private invoiceParams(): InvoiceReportParams {
    return {
      ...this.responsibleParamsSlice(),
    };
  }

  private hasValidPeriod(): boolean {
    if (!this.usesPeriod()) {
      return true;
    }
    const start = this.startDate();
    const end = this.endDate();
    if (start === '' && end === '') {
      return true;
    }
    return start !== '' && end !== '' && start <= end;
  }

  private periodParams(): { startDate?: string; endDate?: string } {
    const start = this.startDate();
    const end = this.endDate();
    if (start === '' || end === '') {
      return {};
    }
    return { startDate: start, endDate: end };
  }

  private pagingParams(): { page: number; size: number } {
    return { page: this.pageIndex(), size: PAGE_SIZE };
  }

  private sortParams<T extends string>(): { sort?: T; direction?: SortDirection } {
    const sort = this.sortField();
    if (sort === '') {
      return {};
    }
    return { sort: sort as T, direction: this.sortDirection() };
  }

  private responsibleParamsSlice(): {
    responsibleType?: ResponsibleType;
    responsibleName?: string;
  } {
    const responsibleType = this.responsibleType();
    return {
      ...(responsibleType === '' ? {} : { responsibleType }),
      ...(this.responsibleName() !== '' ? { responsibleName: this.responsibleName() } : {}),
    };
  }

  private currentTotalPages(): number {
    switch (this.reportType()) {
      case 'expenses':
        return this.expenseReport()?.totalPages ?? 0;
      case 'incomes':
        return this.incomeReport()?.totalPages ?? 0;
      case 'categories':
        return this.categoryReport()?.totalPages ?? 0;
      case 'responsibles':
        return this.responsibleReport()?.totalPages ?? 0;
      case 'cards':
        return this.cardReport()?.totalPages ?? 0;
      case 'cash-flow':
        return this.cashFlowReport()?.historical?.totalPages ?? 0;
      case 'invoices':
        return 0;
    }
  }

  private currentTotalItems(): number {
    switch (this.reportType()) {
      case 'expenses':
        return this.expenseReport()?.totalItems ?? 0;
      case 'incomes':
        return this.incomeReport()?.totalItems ?? 0;
      case 'categories':
        return this.categoryReport()?.totalItems ?? 0;
      case 'responsibles':
        return this.responsibleReport()?.totalItems ?? 0;
      case 'cards':
        return this.cardReport()?.totalItems ?? 0;
      case 'cash-flow':
        return this.cashFlowReport()?.historical?.totalItems ?? 0;
      case 'invoices':
        return 0;
    }
  }

  private currentIsEmpty(): boolean {
    switch (this.reportType()) {
      case 'expenses':
        return (this.expenseReport()?.items.length ?? 0) === 0;
      case 'incomes':
        return (this.incomeReport()?.items.length ?? 0) === 0;
      case 'categories':
        return (this.categoryReport()?.items.length ?? 0) === 0;
      case 'responsibles':
        return (this.responsibleReport()?.items.length ?? 0) === 0;
      case 'cards':
        return (this.cardReport()?.items.length ?? 0) === 0;
      case 'cash-flow': {
        const report = this.cashFlowReport();
        const historicalEmpty = (report?.historical?.items.length ?? 0) === 0;
        const projected = report?.projected;
        const projectedEmpty =
          projected == null ||
          projected.empty === true ||
          !('months' in projected) ||
          projected.months.length === 0;
        return historicalEmpty && projectedEmpty;
      }
      case 'invoices':
        return false;
    }
  }

  private clearReports(): void {
    this.expenseReport.set(null);
    this.incomeReport.set(null);
    this.categoryReport.set(null);
    this.responsibleReport.set(null);
    this.cardReport.set(null);
    this.cashFlowReport.set(null);
    this.invoiceReport.set(null);
  }
}
