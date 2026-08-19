import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  catchError,
  EMPTY,
  firstValueFrom,
  forkJoin,
  of,
  startWith,
  Subject,
  switchMap,
} from 'rxjs';
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
  canCancelIncome,
  canEditIncome,
  canReceiveIncome,
  canReverseMovement,
  incomeMovementStatusLabel,
  incomeMovementTypeLabel,
  incomeStatusLabel,
  responsibleTypeLabel,
} from './incomes-format';
import {
  CreateIncomeReceiptRequest,
  CreateIncomeRequest,
  INCOME_STATUS_OPTIONS,
  Income,
  IncomeListParams,
  IncomeMovement,
  IncomePage,
  IncomeStatusFilter,
  RESPONSIBLE_TYPE_OPTIONS,
  ResponsibleType,
  UpdateIncomeRequest,
} from './incomes.models';
import { IncomesService } from './incomes.service';

type FormMode = 'closed' | 'create' | 'edit' | 'receive';
type PanelMode = 'closed' | 'detail' | 'cancel-confirm' | 'reverse-confirm';

const PAGE_SIZE = 20;

function todayIsoDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

@Component({
  selector: 'app-incomes-page',
  imports: [ReactiveFormsModule, EmptyState, ErrorState, BrlCurrencyPipe, IsoDatePipe],
  templateUrl: './incomes-page.html',
  styleUrl: './incomes-page.css',
})
export class IncomesPage {
  private readonly incomesService = inject(IncomesService);
  private readonly categoriesService = inject(CategoriesService);
  private readonly accountsService = inject(AccountsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reload = new Subject<void>();

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly pageData = signal<IncomePage | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly formMode = signal<FormMode>('closed');
  readonly panelMode = signal<PanelMode>('closed');
  readonly selectedIncome = signal<Income | null>(null);
  readonly selectedMovement = signal<IncomeMovement | null>(null);
  readonly movements = signal<IncomeMovement[]>([]);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly categories = signal<Category[]>([]);
  readonly accounts = signal<Account[]>([]);

  readonly pageIndex = signal(0);
  readonly statusFilter = signal<IncomeStatusFilter>('');
  readonly categoryFilter = signal('');
  readonly accountFilter = signal('');
  readonly startDateFilter = signal('');
  readonly endDateFilter = signal('');

  readonly statusOptions = INCOME_STATUS_OPTIONS;
  readonly responsibleOptions = RESPONSIBLE_TYPE_OPTIONS;

  readonly incomes = computed(() => this.pageData()?.items ?? []);
  readonly totalItems = computed(() => this.pageData()?.totalItems ?? 0);
  readonly totalPages = computed(() => this.pageData()?.totalPages ?? 0);
  readonly isEmpty = computed(() => this.status() === 'loaded' && this.incomes().length === 0);
  readonly hasFilters = computed(
    () =>
      this.statusFilter() !== '' ||
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

  readonly incomeForm = this.formBuilder.nonNullable.group({
    categoryId: ['', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(255)]],
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    expectedDate: ['', Validators.required],
    notes: [''],
    responsibleType: this.formBuilder.nonNullable.control<ResponsibleType | ''>(''),
    responsibleName: [''],
  });

  readonly receiveForm = this.formBuilder.nonNullable.group({
    accountId: ['', Validators.required],
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    date: [todayIsoDate(), Validators.required],
  });

  constructor() {
    forkJoin({
      categories: this.categoriesService.list({ type: 'INCOME', active: true }),
      accounts: this.accountsService.list(),
    })
      .pipe(
        catchError(() => of({ categories: [] as Category[], accounts: [] as Account[] })),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ categories, accounts }) => {
        this.categories.set(categories);
        this.accounts.set(accounts);
      });

    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.status.set('loading');
          this.pageData.set(null);
          this.error.set(null);
          this.actionError.set(null);
          return this.incomesService.list(this.listParams()).pipe(
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
    if (this.submitting()) {
      return;
    }
    if (this.panelMode() === 'cancel-confirm') {
      this.closeCancelConfirm();
      return;
    }
    if (this.panelMode() === 'reverse-confirm') {
      this.closeReverseConfirm();
      return;
    }
    if (this.formMode() !== 'closed') {
      this.closeForm();
      return;
    }
    if (this.panelMode() === 'detail') {
      this.closeDetail();
    }
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

  onStatusFilterChange(value: IncomeStatusFilter): void {
    this.statusFilter.set(value);
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

  statusLabel(status: Income['status']): string {
    return incomeStatusLabel(status);
  }

  movementTypeLabel(type: IncomeMovement['type']): string {
    return incomeMovementTypeLabel(type);
  }

  movementStatusLabel(status: IncomeMovement['status']): string {
    return incomeMovementStatusLabel(status);
  }

  responsibleLabel(type: ResponsibleType | null): string {
    return responsibleTypeLabel(type);
  }

  canEditItem(income: Income): boolean {
    return canEditIncome(income);
  }

  canCancelItem(income: Income): boolean {
    return canCancelIncome(income);
  }

  canReceiveItem(income: Income): boolean {
    return canReceiveIncome(income);
  }

  canReverseItem(movement: IncomeMovement, income: Income): boolean {
    return canReverseMovement(movement, income);
  }

  openCreate(): void {
    this.closePanels();
    this.resetIncomeForm();
    this.formMode.set('create');
    this.editingId.set(null);
  }

  openEdit(income: Income): void {
    if (!this.canEditItem(income)) {
      return;
    }
    this.closePanels();
    this.resetIncomeForm();
    this.incomeForm.patchValue({
      categoryId: income.categoryId,
      description: income.description,
      amount: income.amount,
      expectedDate: income.expectedDate,
      notes: income.notes ?? '',
      responsibleType: income.responsibleType ?? '',
      responsibleName: income.responsibleName ?? '',
    });
    this.formMode.set('edit');
    this.editingId.set(income.id);
  }

  async openDetail(income: Income): Promise<void> {
    this.closeFormPanels();
    this.panelMode.set('detail');
    this.selectedIncome.set(income);
    this.movements.set([]);
    this.detailError.set(null);
    this.detailLoading.set(true);

    try {
      const result = await firstValueFrom(
        forkJoin({
          detail: this.incomesService.get(income.id),
          movements: this.incomesService.listMovements(income.id),
        }),
      );
      this.selectedIncome.set(result.detail);
      this.movements.set(result.movements.items);
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da receita.');
    } finally {
      this.detailLoading.set(false);
    }
  }

  closeDetail(): void {
    if (this.panelMode() === 'detail') {
      this.panelMode.set('closed');
    }
    this.selectedIncome.set(null);
    this.movements.set([]);
    this.detailError.set(null);
  }

  openCancelConfirm(income: Income): void {
    if (!this.canCancelItem(income)) {
      return;
    }
    this.closeFormPanels();
    this.selectedIncome.set(income);
    this.panelMode.set('cancel-confirm');
  }

  closeCancelConfirm(): void {
    if (this.panelMode() === 'cancel-confirm') {
      this.panelMode.set('closed');
      this.selectedIncome.set(null);
    }
  }

  openReverseConfirm(movement: IncomeMovement, income: Income): void {
    if (!this.canReverseItem(movement, income)) {
      return;
    }
    this.selectedMovement.set(movement);
    this.selectedIncome.set(income);
    this.panelMode.set('reverse-confirm');
  }

  closeReverseConfirm(): void {
    this.selectedMovement.set(null);
    if (this.selectedIncome() != null) {
      this.panelMode.set('detail');
      return;
    }
    this.panelMode.set('closed');
  }

  openReceive(income: Income): void {
    if (!this.canReceiveItem(income)) {
      return;
    }
    this.closePanels();
    this.resetReceiveForm();
    this.selectedIncome.set(income);
    this.formMode.set('receive');
    this.receiveForm.patchValue({
      accountId: this.activeAccounts()[0]?.id ?? '',
      amount: income.amount,
      date: todayIsoDate(),
    });
  }

  closeForm(): void {
    this.resetIncomeForm();
    this.resetReceiveForm();
    this.formMode.set('closed');
    this.editingId.set(null);
    this.formError.set(null);
    if (this.panelMode() === 'closed') {
      this.selectedIncome.set(null);
    }
  }

  async submitIncomeForm(): Promise<void> {
    if (this.incomeForm.invalid || this.submitting()) {
      this.incomeForm.markAllAsTouched();
      return;
    }

    const mode = this.formMode();
    if (mode !== 'create' && mode !== 'edit') {
      return;
    }

    const raw = this.incomeForm.getRawValue();
    if (raw.amount == null) {
      return;
    }

    if (raw.responsibleType === 'OTHER' && raw.responsibleName.trim() === '') {
      this.incomeForm.controls.responsibleName.markAsTouched();
      this.incomeForm.controls.responsibleName.setErrors({ required: true });
      return;
    }

    const request: CreateIncomeRequest | UpdateIncomeRequest = {
      categoryId: raw.categoryId,
      description: raw.description.trim(),
      amount: raw.amount,
      expectedDate: raw.expectedDate,
      ...(raw.notes.trim() !== '' ? { notes: raw.notes.trim() } : {}),
      ...(raw.responsibleType !== '' ? { responsibleType: raw.responsibleType } : {}),
      ...(raw.responsibleName.trim() !== '' ? { responsibleName: raw.responsibleName.trim() } : {}),
    };

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      if (mode === 'create') {
        await firstValueFrom(this.incomesService.create(request));
      } else {
        const incomeId = this.editingId();
        if (incomeId == null) {
          return;
        }
        await firstValueFrom(this.incomesService.update(incomeId, request));
      }
      this.closeForm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.incomeForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async submitReceive(): Promise<void> {
    if (this.receiveForm.invalid || this.submitting()) {
      this.receiveForm.markAllAsTouched();
      return;
    }

    const income = this.selectedIncome();
    if (income == null) {
      return;
    }

    const raw = this.receiveForm.getRawValue();
    if (raw.amount == null || raw.accountId === '') {
      this.receiveForm.markAllAsTouched();
      return;
    }

    const request: CreateIncomeReceiptRequest = {
      amount: raw.amount,
      date: raw.date,
      accountId: raw.accountId,
    };

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      await firstValueFrom(this.incomesService.createReceipt(income.id, request));
      this.closeForm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.receiveForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async confirmCancel(): Promise<void> {
    const income = this.selectedIncome();
    if (income == null || this.submitting() || !this.canCancelItem(income)) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.formError.set(null);

    try {
      await firstValueFrom(this.incomesService.cancel(income.id));
      this.closeCancelConfirm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError);
    } finally {
      this.submitting.set(false);
    }
  }

  async confirmReverse(): Promise<void> {
    const income = this.selectedIncome();
    const movement = this.selectedMovement();
    if (income == null || movement == null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.formError.set(null);

    try {
      await firstValueFrom(this.incomesService.reverseMovement(income.id, movement.id));
      this.selectedMovement.set(null);
      this.formMode.set('closed');
      this.reload.next();
      await this.openDetail(income);
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError);
    } finally {
      this.submitting.set(false);
    }
  }

  incomeFieldError(
    controlName: 'categoryId' | 'description' | 'amount' | 'expectedDate' | 'responsibleName',
  ): string | null {
    return this.fieldError(this.incomeForm, controlName);
  }

  receiveFieldError(controlName: 'accountId' | 'amount' | 'date'): string | null {
    return this.fieldError(this.receiveForm, controlName);
  }

  showResponsibleNameField(): boolean {
    return this.incomeForm.controls.responsibleType.value === 'OTHER';
  }

  private listParams(): IncomeListParams {
    const status = this.statusFilter();
    const categoryId = this.categoryFilter();
    const accountId = this.accountFilter();
    const startDate = this.startDateFilter();
    const endDate = this.endDateFilter();

    return {
      page: this.pageIndex(),
      size: PAGE_SIZE,
      ...(status !== '' ? { status } : {}),
      ...(categoryId !== '' ? { categoryId } : {}),
      ...(accountId !== '' ? { accountId } : {}),
      ...(startDate !== '' ? { startDate } : {}),
      ...(endDate !== '' ? { endDate } : {}),
    };
  }

  private closePanels(): void {
    this.closeFormPanels();
    this.panelMode.set('closed');
    this.selectedIncome.set(null);
    this.selectedMovement.set(null);
    this.movements.set([]);
    this.detailError.set(null);
  }

  private closeFormPanels(): void {
    if (this.formMode() !== 'closed') {
      this.closeForm();
    }
  }

  private resetIncomeForm(): void {
    this.incomeForm.reset({
      categoryId: '',
      description: '',
      amount: null,
      expectedDate: '',
      notes: '',
      responsibleType: '',
      responsibleName: '',
    });
    this.formError.set(null);
  }

  private resetReceiveForm(): void {
    this.receiveForm.reset({
      accountId: '',
      amount: null,
      date: todayIsoDate(),
    });
    this.formError.set(null);
  }

  private handleMutationError(error: unknown, form: FormGroup = this.incomeForm): void {
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
      this.setMutationMessage('Esta operação não é permitida para a receita no estado atual.');
      return;
    }

    if (error.code === 'NOT_FOUND') {
      this.setMutationMessage('Receita não encontrada.');
      return;
    }

    this.setMutationMessage('Não foi possível concluir a operação.');
  }

  private setMutationMessage(message: string): void {
    if (
      this.formMode() !== 'closed' ||
      this.panelMode() === 'cancel-confirm' ||
      this.panelMode() === 'reverse-confirm'
    ) {
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
}
