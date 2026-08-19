import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, EMPTY, firstValueFrom, forkJoin, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { IsoDatePipe } from '../../shared/pipes/iso-date.pipe';
import { AccountsService } from '../accounts/accounts.service';
import { Account } from '../accounts/accounts.models';
import { CategoriesService } from '../categories/categories.service';
import { Category } from '../categories/categories.models';
import {
  canCancelExpense,
  canEditExpense,
  canPayExpense,
  canPayInstallment,
  canRefundExpense,
  expenseStatusLabel,
  isSingleInstallment,
  paymentMethodLabel,
  responsibleTypeLabel,
} from './expenses-format';
import {
  CreateExpenseRequest,
  EXPENSE_STATUS_OPTIONS,
  Expense,
  ExpenseInstallment,
  ExpenseListParams,
  ExpensePage,
  ExpenseStatusFilter,
  PAYMENT_METHOD_OPTIONS,
  PaymentMethod,
  PaymentMethodFilter,
  PayExpenseRequest,
  RefundExpenseRequest,
  RefundSettlement,
  RESPONSIBLE_TYPE_OPTIONS,
  ResponsibleType,
  ResponsibleTypeFilter,
  UpdateExpenseRequest,
  WritablePaymentMethod,
} from './expenses.models';
import { ExpensesService } from './expenses.service';

type FormMode = 'closed' | 'create' | 'edit' | 'pay' | 'refund';
type PanelMode = 'closed' | 'detail' | 'cancel-confirm';

const PAGE_SIZE = 20;

function todayIsoDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

@Component({
  selector: 'app-expenses-page',
  imports: [ReactiveFormsModule, EmptyState, ErrorState, BrlCurrencyPipe, IsoDatePipe],
  templateUrl: './expenses-page.html',
  styleUrl: './expenses-page.css',
})
export class ExpensesPage {
  private readonly expensesService = inject(ExpensesService);
  private readonly categoriesService = inject(CategoriesService);
  private readonly accountsService = inject(AccountsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reload = new Subject<void>();

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly pageData = signal<ExpensePage | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly formMode = signal<FormMode>('closed');
  readonly panelMode = signal<PanelMode>('closed');
  readonly selectedExpense = signal<Expense | null>(null);
  readonly installments = signal<ExpenseInstallment[]>([]);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly categories = signal<Category[]>([]);
  readonly accounts = signal<Account[]>([]);

  readonly pageIndex = signal(0);
  readonly statusFilter = signal<ExpenseStatusFilter>('');
  readonly paymentMethodFilter = signal<PaymentMethodFilter>('');
  readonly responsibleFilter = signal<ResponsibleTypeFilter>('');
  readonly categoryFilter = signal('');
  readonly accountFilter = signal('');
  readonly startDateFilter = signal('');
  readonly endDateFilter = signal('');

  readonly statusOptions = EXPENSE_STATUS_OPTIONS;
  readonly paymentMethodOptions = PAYMENT_METHOD_OPTIONS;
  readonly responsibleOptions = RESPONSIBLE_TYPE_OPTIONS;

  readonly expenses = computed(() => this.pageData()?.items ?? []);
  readonly totalItems = computed(() => this.pageData()?.totalItems ?? 0);
  readonly totalPages = computed(() => this.pageData()?.totalPages ?? 0);
  readonly isEmpty = computed(() => this.status() === 'loaded' && this.expenses().length === 0);
  readonly hasFilters = computed(
    () =>
      this.statusFilter() !== '' ||
      this.paymentMethodFilter() !== '' ||
      this.responsibleFilter() !== '' ||
      this.categoryFilter() !== '' ||
      this.accountFilter() !== '' ||
      this.startDateFilter() !== '' ||
      this.endDateFilter() !== '',
  );
  readonly canGoPrev = computed(() => this.pageIndex() > 0);
  readonly canGoNext = computed(() => {
    const total = this.totalPages();
    return total > 0 && this.pageIndex() < total - 1;
  });

  readonly activeAccounts = computed(() => this.accounts().filter((account) => account.active));

  readonly expenseForm = this.formBuilder.nonNullable.group({
    categoryId: ['', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(255)]],
    totalAmount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    expenseDate: ['', Validators.required],
    dueDate: ['', Validators.required],
    paymentMethod: this.formBuilder.nonNullable.control<WritablePaymentMethod | ''>(
      '',
      Validators.required,
    ),
    accountId: [''],
    responsibleType: this.formBuilder.nonNullable.control<ResponsibleType | ''>(
      '',
      Validators.required,
    ),
    responsibleName: [''],
    barcode: [''],
    notes: [''],
    installmentCount: this.formBuilder.control<number | null>(null, [Validators.min(1)]),
  });

  readonly payForm = this.formBuilder.nonNullable.group({
    installmentId: [''],
    accountId: ['', Validators.required],
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    paymentDate: [todayIsoDate(), Validators.required],
    notes: [''],
  });

  readonly refundForm = this.formBuilder.nonNullable.group({
    settlement: this.formBuilder.nonNullable.control<RefundSettlement | ''>(''),
    accountId: [''],
  });

  constructor() {
    forkJoin({
      categories: this.categoriesService.list({ type: 'EXPENSE', active: true }),
      accounts: this.accountsService.list(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ categories, accounts }) => {
          this.categories.set(categories);
          this.accounts.set(accounts);
        },
      });

    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.status.set('loading');
          this.pageData.set(null);
          this.error.set(null);
          this.actionError.set(null);
          return this.expensesService.list(this.listParams()).pipe(
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

  onStatusFilterChange(value: ExpenseStatusFilter): void {
    this.statusFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  onPaymentMethodFilterChange(value: PaymentMethodFilter): void {
    this.paymentMethodFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  onResponsibleFilterChange(value: ResponsibleTypeFilter): void {
    this.responsibleFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  onCategoryFilterChange(value: string): void {
    this.categoryFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  onAccountFilterChange(value: string): void {
    this.accountFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  onStartDateFilterChange(value: string): void {
    this.startDateFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  onEndDateFilterChange(value: string): void {
    this.endDateFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  categoryName(categoryId: string): string {
    return this.categories().find((item) => item.id === categoryId)?.name ?? '—';
  }

  accountName(accountId: string | null): string {
    if (accountId == null) {
      return '—';
    }
    return this.accounts().find((item) => item.id === accountId)?.name ?? '—';
  }

  statusLabel(status: Expense['status']): string {
    return expenseStatusLabel(status);
  }

  methodLabel(method: PaymentMethod): string {
    return paymentMethodLabel(method);
  }

  responsibleLabel(type: ResponsibleType): string {
    return responsibleTypeLabel(type);
  }

  canEditItem(expense: Expense): boolean {
    return canEditExpense(expense);
  }

  canCancelItem(expense: Expense): boolean {
    return canCancelExpense(expense);
  }

  canPayItem(expense: Expense): boolean {
    return canPayExpense(expense);
  }

  canRefundItem(expense: Expense): boolean {
    return canRefundExpense(expense);
  }

  canPayInstallmentItem(installment: ExpenseInstallment, expense: Expense): boolean {
    return canPayInstallment(installment, expense);
  }

  openCreate(): void {
    this.closePanels();
    this.resetExpenseForm();
    this.formMode.set('create');
    this.editingId.set(null);
  }

  openEdit(expense: Expense): void {
    if (!this.canEditItem(expense)) {
      return;
    }
    this.closePanels();
    this.resetExpenseForm();
    const writableMethod = this.toWritablePaymentMethod(expense.paymentMethod);
    this.expenseForm.patchValue({
      categoryId: expense.categoryId,
      description: expense.description,
      totalAmount: expense.totalAmount,
      expenseDate: expense.expenseDate,
      dueDate: expense.dueDate,
      paymentMethod: writableMethod ?? '',
      accountId: expense.accountId ?? '',
      responsibleType: expense.responsibleType,
      responsibleName: expense.responsibleName ?? '',
      barcode: expense.barcode ?? '',
      notes: expense.notes ?? '',
      installmentCount: null,
    });
    this.formMode.set('edit');
    this.editingId.set(expense.id);
  }

  async openDetail(expense: Expense): Promise<void> {
    this.closeFormPanels();
    this.panelMode.set('detail');
    this.selectedExpense.set(expense);
    this.installments.set([]);
    this.detailError.set(null);
    this.detailLoading.set(true);

    try {
      const result = await firstValueFrom(
        forkJoin({
          detail: this.expensesService.get(expense.id),
          installments: this.expensesService.listInstallments(expense.id),
        }),
      );
      this.selectedExpense.set(result.detail);
      this.installments.set(result.installments);
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da despesa.');
    } finally {
      this.detailLoading.set(false);
    }
  }

  closeDetail(): void {
    this.panelMode.set('closed');
    this.selectedExpense.set(null);
    this.installments.set([]);
    this.detailError.set(null);
  }

  openCancelConfirm(expense: Expense): void {
    if (!this.canCancelItem(expense)) {
      return;
    }
    this.closeFormPanels();
    this.selectedExpense.set(expense);
    this.panelMode.set('cancel-confirm');
  }

  closeCancelConfirm(): void {
    if (this.formMode() === 'closed' && this.panelMode() === 'cancel-confirm') {
      this.selectedExpense.set(null);
    }
    this.panelMode.set('closed');
  }

  async openPay(expense: Expense): Promise<void> {
    if (!this.canPayItem(expense)) {
      return;
    }
    this.closePanels();
    this.resetPayForm();
    this.selectedExpense.set(expense);
    this.formMode.set('pay');

    try {
      const installments = await firstValueFrom(this.expensesService.listInstallments(expense.id));
      this.installments.set(installments);
      const defaultAccountId = expense.accountId ?? this.activeAccounts()[0]?.id ?? '';
      const defaultInstallment =
        installments.find((item) => item.remainingAmount > 0) ?? installments[0];
      this.payForm.patchValue({
        installmentId: isSingleInstallment(installments) ? '' : (defaultInstallment?.id ?? ''),
        accountId: defaultAccountId,
        amount: defaultInstallment?.remainingAmount ?? expense.totalAmount,
        paymentDate: todayIsoDate(),
      });
    } catch {
      this.formError.set('Não foi possível preparar o pagamento.');
      this.formMode.set('closed');
      this.selectedExpense.set(null);
    }
  }

  openRefund(expense: Expense): void {
    if (!this.canRefundItem(expense)) {
      return;
    }
    this.closePanels();
    this.refundForm.reset({ settlement: '', accountId: '' });
    this.selectedExpense.set(expense);
    this.formMode.set('refund');
  }

  closeForm(): void {
    this.resetExpenseForm();
    this.resetPayForm();
    this.refundForm.reset({ settlement: '', accountId: '' });
    this.formMode.set('closed');
    this.editingId.set(null);
    this.formError.set(null);
    if (this.panelMode() === 'closed') {
      this.selectedExpense.set(null);
      this.installments.set([]);
    }
  }

  async submitExpenseForm(): Promise<void> {
    if (this.expenseForm.invalid || this.submitting()) {
      this.expenseForm.markAllAsTouched();
      return;
    }

    const mode = this.formMode();
    if (mode !== 'create' && mode !== 'edit') {
      return;
    }

    const raw = this.expenseForm.getRawValue();
    if (raw.paymentMethod === '' || raw.responsibleType === '') {
      this.expenseForm.markAllAsTouched();
      return;
    }

    if (raw.paymentMethod === 'ACCOUNT' && raw.accountId === '') {
      this.expenseForm.controls.accountId.markAsTouched();
      this.expenseForm.controls.accountId.setErrors({ required: true });
      return;
    }

    if (raw.responsibleType === 'OTHER' && raw.responsibleName.trim() === '') {
      this.expenseForm.controls.responsibleName.markAsTouched();
      this.expenseForm.controls.responsibleName.setErrors({ required: true });
      return;
    }

    if (raw.totalAmount == null) {
      return;
    }

    const baseRequest = {
      categoryId: raw.categoryId,
      description: raw.description.trim(),
      totalAmount: raw.totalAmount,
      expenseDate: raw.expenseDate,
      dueDate: raw.dueDate,
      paymentMethod: raw.paymentMethod,
      responsibleType: raw.responsibleType,
      ...(raw.accountId !== '' ? { accountId: raw.accountId } : {}),
      ...(raw.responsibleName.trim() !== '' ? { responsibleName: raw.responsibleName.trim() } : {}),
      ...(raw.barcode.trim() !== '' ? { barcode: raw.barcode.trim() } : {}),
      ...(raw.notes.trim() !== '' ? { notes: raw.notes.trim() } : {}),
    };

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      if (mode === 'create') {
        const request: CreateExpenseRequest = {
          ...baseRequest,
          ...(raw.installmentCount != null && raw.installmentCount > 1
            ? { installmentCount: raw.installmentCount }
            : {}),
        };
        await firstValueFrom(this.expensesService.create(request));
      } else {
        const expenseId = this.editingId();
        if (expenseId == null) {
          return;
        }
        await firstValueFrom(
          this.expensesService.update(expenseId, baseRequest as UpdateExpenseRequest),
        );
      }
      this.closeForm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.expenseForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async submitPay(): Promise<void> {
    if (this.payForm.invalid || this.submitting()) {
      this.payForm.markAllAsTouched();
      return;
    }

    const expense = this.selectedExpense();
    if (expense == null) {
      return;
    }

    const raw = this.payForm.getRawValue();
    if (raw.amount == null || raw.accountId === '') {
      this.payForm.markAllAsTouched();
      return;
    }

    const request: PayExpenseRequest = {
      accountId: raw.accountId,
      amount: raw.amount,
      paymentDate: raw.paymentDate,
      ...(raw.notes.trim() !== '' ? { notes: raw.notes.trim() } : {}),
    };

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      const installmentItems = this.installments();
      if (isSingleInstallment(installmentItems)) {
        await firstValueFrom(this.expensesService.pay(expense.id, request));
      } else {
        if (raw.installmentId === '') {
          this.payForm.controls.installmentId.markAsTouched();
          this.payForm.controls.installmentId.setErrors({ required: true });
          return;
        }
        await firstValueFrom(
          this.expensesService.payInstallment(expense.id, raw.installmentId, request),
        );
      }
      this.closeForm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.payForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async submitRefund(): Promise<void> {
    const expense = this.selectedExpense();
    if (expense == null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      if (expense.paymentMethod === 'CREDIT_CARD') {
        const raw = this.refundForm.getRawValue();
        if (raw.settlement === '') {
          this.refundForm.controls.settlement.markAsTouched();
          return;
        }
        if (raw.settlement === 'ACCOUNT' && raw.accountId === '') {
          this.refundForm.controls.accountId.markAsTouched();
          this.refundForm.controls.accountId.setErrors({ required: true });
          return;
        }
        const request: RefundExpenseRequest = {
          settlement: raw.settlement,
          ...(raw.settlement === 'ACCOUNT' ? { accountId: raw.accountId } : {}),
        };
        await firstValueFrom(this.expensesService.refund(expense.id, request));
      } else {
        await firstValueFrom(this.expensesService.refund(expense.id));
      }
      this.closeForm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.refundForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async confirmCancel(): Promise<void> {
    const expense = this.selectedExpense();
    if (expense == null || this.submitting() || !this.canCancelItem(expense)) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);

    try {
      await firstValueFrom(this.expensesService.cancel(expense.id));
      this.closeCancelConfirm();
      this.closeDetail();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError);
    } finally {
      this.submitting.set(false);
    }
  }

  expenseFieldError(
    controlName:
      | 'categoryId'
      | 'description'
      | 'totalAmount'
      | 'expenseDate'
      | 'dueDate'
      | 'paymentMethod'
      | 'accountId'
      | 'responsibleType'
      | 'responsibleName'
      | 'installmentCount',
  ): string | null {
    return this.fieldError(this.expenseForm, controlName);
  }

  payFieldError(
    controlName: 'installmentId' | 'accountId' | 'amount' | 'paymentDate',
  ): string | null {
    return this.fieldError(this.payForm, controlName);
  }

  refundFieldError(controlName: 'settlement' | 'accountId'): string | null {
    return this.fieldError(this.refundForm, controlName);
  }

  showAccountField(): boolean {
    return this.expenseForm.controls.paymentMethod.value === 'ACCOUNT';
  }

  showResponsibleNameField(): boolean {
    return this.expenseForm.controls.responsibleType.value === 'OTHER';
  }

  showInstallmentCountField(): boolean {
    return this.formMode() === 'create';
  }

  showPayInstallmentField(): boolean {
    return this.formMode() === 'pay' && !isSingleInstallment(this.installments());
  }

  showRefundSettlementFields(): boolean {
    return this.selectedExpense()?.paymentMethod === 'CREDIT_CARD';
  }

  isOverdue(expense: Expense): boolean {
    return expense.overdue;
  }

  private listParams(): ExpenseListParams {
    const status = this.statusFilter();
    const paymentMethod = this.paymentMethodFilter();
    const responsibleType = this.responsibleFilter();
    const categoryId = this.categoryFilter();
    const accountId = this.accountFilter();
    const startDate = this.startDateFilter();
    const endDate = this.endDateFilter();

    return {
      page: this.pageIndex(),
      size: PAGE_SIZE,
      ...(status !== '' ? { status } : {}),
      ...(paymentMethod !== '' ? { paymentMethod } : {}),
      ...(responsibleType !== '' ? { responsibleType } : {}),
      ...(categoryId !== '' ? { categoryId } : {}),
      ...(accountId !== '' ? { accountId } : {}),
      ...(startDate !== '' ? { startDate } : {}),
      ...(endDate !== '' ? { endDate } : {}),
    };
  }

  private closePanels(): void {
    this.closeFormPanels();
    this.closeDetail();
    this.closeCancelConfirm();
  }

  private closeFormPanels(): void {
    if (this.formMode() !== 'closed') {
      this.closeForm();
    }
  }

  private resetExpenseForm(): void {
    this.expenseForm.reset({
      categoryId: '',
      description: '',
      totalAmount: null,
      expenseDate: '',
      dueDate: '',
      paymentMethod: '',
      accountId: '',
      responsibleType: '',
      responsibleName: '',
      barcode: '',
      notes: '',
      installmentCount: null,
    });
    this.formError.set(null);
  }

  private resetPayForm(): void {
    this.payForm.reset({
      installmentId: '',
      accountId: '',
      amount: null,
      paymentDate: todayIsoDate(),
      notes: '',
    });
    this.formError.set(null);
  }

  private handleMutationError(error: unknown, form: FormGroup = this.expenseForm): void {
    if (!isApiError(error)) {
      this.setMutationMessage('Não foi possível concluir a operação.');
      return;
    }

    if (error.code === 'VALIDATION_ERROR' && error.fields != null) {
      this.applyFieldErrors(form, error.fields);
      if (this.formError() == null && this.unmappedFieldCount(form, error.fields) > 0) {
        this.formError.set('Revise os dados informados.');
      }
      return;
    }

    if (error.code === 'VALIDATION_ERROR') {
      this.formError.set('Revise os dados informados.');
      return;
    }

    if (error.status === 403) {
      this.setMutationMessage('Você não tem permissão para esta operação.');
      return;
    }

    if (error.code === 'BUSINESS_RULE_VIOLATION') {
      this.setMutationMessage('Esta operação não é permitida para a despesa no estado atual.');
      return;
    }

    if (error.code === 'NOT_FOUND') {
      this.setMutationMessage('Despesa não encontrada.');
      return;
    }

    this.setMutationMessage('Não foi possível concluir a operação.');
  }

  private setMutationMessage(message: string): void {
    if (this.formMode() !== 'closed' || this.panelMode() === 'cancel-confirm') {
      this.formError.set(message);
      return;
    }
    if (this.panelMode() === 'detail') {
      this.detailError.set(message);
      return;
    }
    this.actionError.set(message);
  }

  private applyFieldErrors(form: FormGroup, fields: Record<string, string>): void {
    for (const [key, message] of Object.entries(fields)) {
      const control = form.get(key);
      if (control == null) {
        continue;
      }
      control.setErrors({ ...control.errors, api: message });
      control.markAsTouched();
    }
  }

  private unmappedFieldCount(form: FormGroup, fields: Record<string, string>): number {
    return Object.keys(fields).filter((key) => form.get(key) == null).length;
  }

  private fieldError(form: FormGroup, controlName: string): string | null {
    const control = form.get(controlName);
    if (control == null || !control.touched || control.valid) {
      return null;
    }
    if (control.hasError('api')) {
      const apiError = control.getError('api');
      return typeof apiError === 'string' ? apiError : null;
    }
    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }
    if (control.hasError('min')) {
      return 'Informe um valor maior que zero.';
    }
    if (control.hasError('maxlength')) {
      return 'Texto muito longo.';
    }
    return null;
  }

  private toWritablePaymentMethod(method: PaymentMethod): WritablePaymentMethod | null {
    return method === 'ACCOUNT' || method === 'NONE' ? method : null;
  }
}
