import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, EMPTY, firstValueFrom, of, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { IsoDatePipe } from '../../shared/pipes/iso-date.pipe';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { todayIsoDate } from '../expenses/today-iso-date';
import {
  canCancelFinancialGoal,
  canCompleteFinancialGoal,
  canContributeToFinancialGoal,
  canEditFinancialGoal,
  canRedeemFromFinancialGoal,
  financialGoalProgressLabel,
  financialGoalStatusLabel,
} from './financial-goals-format';
import {
  CreateFinancialGoalRequest,
  CreateGoalContributionRequest,
  CreateGoalRedemptionRequest,
  FINANCIAL_GOAL_STATUS_OPTIONS,
  FinancialGoal,
  FinancialGoalListParams,
  FinancialGoalPage,
  FinancialGoalStatusFilter,
  GoalContribution,
  GoalRedemption,
  UpdateFinancialGoalRequest,
} from './financial-goals.models';
import { FinancialGoalsService } from './financial-goals.service';

type FormMode = 'closed' | 'create' | 'edit' | 'contribute' | 'redeem';
type PanelMode = 'closed' | 'detail' | 'cancel-confirm' | 'complete-confirm';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-financial-goals-page',
  imports: [ReactiveFormsModule, EmptyState, ErrorState, BrlCurrencyPipe, IsoDatePipe],
  templateUrl: './financial-goals-page.html',
  styleUrl: './financial-goals-page.css',
})
export class FinancialGoalsPage {
  private readonly financialGoalsService = inject(FinancialGoalsService);
  private readonly accountsService = inject(AccountsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reload = new Subject<void>();

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly pageData = signal<FinancialGoalPage | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly formMode = signal<FormMode>('closed');
  readonly panelMode = signal<PanelMode>('closed');
  readonly selectedGoal = signal<FinancialGoal | null>(null);
  readonly contributions = signal<GoalContribution[]>([]);
  readonly redemptions = signal<GoalRedemption[]>([]);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);
  readonly historyError = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly accounts = signal<Account[]>([]);

  readonly pageIndex = signal(0);
  readonly statusFilter = signal<FinancialGoalStatusFilter>('');

  readonly statusOptions = FINANCIAL_GOAL_STATUS_OPTIONS;

  readonly goals = computed(() => this.pageData()?.items ?? []);
  readonly totalItems = computed(() => this.pageData()?.totalItems ?? 0);
  readonly totalPages = computed(() => this.pageData()?.totalPages ?? 0);
  readonly isEmpty = computed(() => this.status() === 'loaded' && this.goals().length === 0);
  readonly hasFilters = computed(() => this.statusFilter() !== '');
  readonly canGoPrev = computed(() => this.pageIndex() > 0);
  readonly canGoNext = computed(() => {
    const total = this.totalPages();
    return total > 0 && this.pageIndex() < total - 1;
  });
  readonly activeAccounts = computed(() => this.accounts().filter((account) => account.active));

  readonly goalForm = this.formBuilder.nonNullable.group({
    accountId: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    targetAmount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    targetDate: [''],
  });

  readonly movementForm = this.formBuilder.nonNullable.group({
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    date: [todayIsoDate(), Validators.required],
    notes: [''],
  });

  constructor() {
    this.accountsService
      .list()
      .pipe(
        catchError(() => of([] as Account[])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((accounts) => {
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
          return this.financialGoalsService.list(this.listParams()).pipe(
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
    if (this.panelMode() === 'complete-confirm') {
      this.closeCompleteConfirm();
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

  onStatusFilterChange(value: FinancialGoalStatusFilter): void {
    this.statusFilter.set(value);
    this.pageIndex.set(0);
    this.reload.next();
  }

  accountName(accountId: string): string {
    return this.accounts().find((item) => item.id === accountId)?.name ?? '—';
  }

  statusLabel(status: FinancialGoal['status']): string {
    return financialGoalStatusLabel(status);
  }

  progressLabel(progressPercent: number): string {
    return financialGoalProgressLabel(progressPercent);
  }

  canEditItem(goal: FinancialGoal): boolean {
    return canEditFinancialGoal(goal);
  }

  canContributeItem(goal: FinancialGoal): boolean {
    return canContributeToFinancialGoal(goal);
  }

  canRedeemItem(goal: FinancialGoal): boolean {
    return canRedeemFromFinancialGoal(goal);
  }

  canCompleteItem(goal: FinancialGoal): boolean {
    return canCompleteFinancialGoal(goal);
  }

  canCancelItem(goal: FinancialGoal): boolean {
    return canCancelFinancialGoal(goal);
  }

  openCreate(): void {
    this.closePanels();
    this.resetGoalForm();
    this.formMode.set('create');
    this.editingId.set(null);
  }

  openEdit(goal: FinancialGoal): void {
    if (!this.canEditItem(goal)) {
      return;
    }
    this.closePanels();
    this.resetGoalForm();
    this.goalForm.patchValue({
      accountId: goal.accountId,
      name: goal.name,
      description: goal.description ?? '',
      targetAmount: goal.targetAmount,
      targetDate: goal.targetDate ?? '',
    });
    this.formMode.set('edit');
    this.editingId.set(goal.id);
  }

  async openDetail(goal: FinancialGoal): Promise<void> {
    this.closeFormPanels();
    this.panelMode.set('detail');
    this.selectedGoal.set(goal);
    this.contributions.set([]);
    this.redemptions.set([]);
    this.detailError.set(null);
    this.historyError.set(null);
    this.detailLoading.set(true);

    try {
      const detail = await firstValueFrom(this.financialGoalsService.get(goal.id));
      this.selectedGoal.set(detail);
    } catch {
      this.detailError.set('Não foi possível atualizar os detalhes da meta.');
    } finally {
      this.detailLoading.set(false);
    }

    await this.reloadHistories(goal.id);
  }

  retryHistories(): void {
    const goal = this.selectedGoal();
    if (goal == null) {
      return;
    }
    void this.reloadHistories(goal.id);
  }

  closeDetail(): void {
    if (this.panelMode() === 'detail') {
      this.panelMode.set('closed');
    }
    this.selectedGoal.set(null);
    this.contributions.set([]);
    this.redemptions.set([]);
    this.detailError.set(null);
    this.historyError.set(null);
  }

  openCancelConfirm(goal: FinancialGoal): void {
    if (!this.canCancelItem(goal)) {
      return;
    }
    this.closeFormPanels();
    this.selectedGoal.set(goal);
    this.panelMode.set('cancel-confirm');
  }

  closeCancelConfirm(): void {
    if (this.panelMode() === 'cancel-confirm') {
      this.panelMode.set('closed');
      this.selectedGoal.set(null);
    }
  }

  openCompleteConfirm(goal: FinancialGoal): void {
    if (!this.canCompleteItem(goal)) {
      return;
    }
    this.closeFormPanels();
    this.selectedGoal.set(goal);
    this.panelMode.set('complete-confirm');
  }

  closeCompleteConfirm(): void {
    if (this.panelMode() === 'complete-confirm') {
      this.panelMode.set('closed');
      this.selectedGoal.set(null);
    }
  }

  openContribute(goal: FinancialGoal): void {
    if (!this.canContributeItem(goal)) {
      return;
    }
    this.closePanels();
    this.resetMovementForm();
    this.selectedGoal.set(goal);
    this.formMode.set('contribute');
  }

  openRedeem(goal: FinancialGoal): void {
    if (!this.canRedeemItem(goal)) {
      return;
    }
    this.closePanels();
    this.resetMovementForm();
    this.selectedGoal.set(goal);
    this.formMode.set('redeem');
  }

  closeForm(): void {
    this.resetGoalForm();
    this.resetMovementForm();
    this.formMode.set('closed');
    this.editingId.set(null);
    this.formError.set(null);
    if (this.panelMode() === 'closed') {
      this.selectedGoal.set(null);
    }
  }

  async submitGoalForm(): Promise<void> {
    if (this.goalForm.invalid || this.submitting()) {
      this.goalForm.markAllAsTouched();
      return;
    }

    const mode = this.formMode();
    if (mode !== 'create' && mode !== 'edit') {
      return;
    }

    const raw = this.goalForm.getRawValue();
    if (raw.targetAmount == null) {
      return;
    }

    const description = raw.description.trim();
    const targetDate = raw.targetDate.trim();

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      if (mode === 'create') {
        if (raw.accountId === '') {
          this.goalForm.controls.accountId.markAsTouched();
          return;
        }
        const request: CreateFinancialGoalRequest = {
          accountId: raw.accountId,
          name: raw.name.trim(),
          targetAmount: raw.targetAmount,
          ...(description !== '' ? { description } : {}),
          ...(targetDate !== '' ? { targetDate } : {}),
        };
        await firstValueFrom(this.financialGoalsService.create(request));
      } else {
        const goalId = this.editingId();
        if (goalId == null) {
          return;
        }
        const request: UpdateFinancialGoalRequest = {
          name: raw.name.trim(),
          targetAmount: raw.targetAmount,
          ...(description !== '' ? { description } : {}),
          ...(targetDate !== '' ? { targetDate } : {}),
        };
        await firstValueFrom(this.financialGoalsService.update(goalId, request));
      }
      this.closeForm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.goalForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async submitContribute(): Promise<void> {
    if (this.movementForm.invalid || this.submitting()) {
      this.movementForm.markAllAsTouched();
      return;
    }

    const goal = this.selectedGoal();
    if (goal == null || !this.canContributeItem(goal)) {
      return;
    }

    const request = this.contributionRequest();
    if (request == null) {
      this.movementForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      const result = await firstValueFrom(this.financialGoalsService.contribute(goal.id, request));
      this.selectedGoal.set(result.goal);
      this.panelMode.set('detail');
      this.closeForm();
      this.reload.next();
      await this.reloadHistories(result.goal.id);
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.movementForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async submitRedeem(): Promise<void> {
    if (this.movementForm.invalid || this.submitting()) {
      this.movementForm.markAllAsTouched();
      return;
    }

    const goal = this.selectedGoal();
    if (goal == null || !this.canRedeemItem(goal)) {
      return;
    }

    const request = this.redemptionRequest();
    if (request == null) {
      this.movementForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      const result = await firstValueFrom(this.financialGoalsService.redeem(goal.id, request));
      this.selectedGoal.set(result.goal);
      this.panelMode.set('detail');
      this.closeForm();
      this.reload.next();
      await this.reloadHistories(result.goal.id);
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.movementForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async confirmComplete(): Promise<void> {
    const goal = this.selectedGoal();
    if (goal == null || this.submitting() || !this.canCompleteItem(goal)) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.formError.set(null);

    try {
      await firstValueFrom(this.financialGoalsService.complete(goal.id));
      this.closeCompleteConfirm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError);
    } finally {
      this.submitting.set(false);
    }
  }

  async confirmCancel(): Promise<void> {
    const goal = this.selectedGoal();
    if (goal == null || this.submitting() || !this.canCancelItem(goal)) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.formError.set(null);

    try {
      await firstValueFrom(this.financialGoalsService.cancel(goal.id));
      this.closeCancelConfirm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError);
    } finally {
      this.submitting.set(false);
    }
  }

  goalFieldError(controlName: 'accountId' | 'name' | 'targetAmount' | 'targetDate'): string | null {
    return this.fieldError(this.goalForm, controlName);
  }

  movementFieldError(controlName: 'amount' | 'date'): string | null {
    return this.fieldError(this.movementForm, controlName);
  }

  private listParams(): FinancialGoalListParams {
    const status = this.statusFilter();
    return {
      page: this.pageIndex(),
      size: PAGE_SIZE,
      ...(status !== '' ? { status } : {}),
    };
  }

  private async reloadHistories(goalId: string): Promise<void> {
    this.historyError.set(null);

    const [contributionsResult, redemptionsResult] = await Promise.allSettled([
      firstValueFrom(this.financialGoalsService.listContributions(goalId)),
      firstValueFrom(this.financialGoalsService.listRedemptions(goalId)),
    ]);

    if (contributionsResult.status === 'fulfilled') {
      this.contributions.set(contributionsResult.value);
    } else {
      this.contributions.set([]);
    }

    if (redemptionsResult.status === 'fulfilled') {
      this.redemptions.set(redemptionsResult.value);
    } else {
      this.redemptions.set([]);
    }

    if (contributionsResult.status === 'rejected' || redemptionsResult.status === 'rejected') {
      this.historyError.set('Não foi possível carregar o histórico da meta.');
    }
  }

  private contributionRequest(): CreateGoalContributionRequest | null {
    const raw = this.movementForm.getRawValue();
    if (raw.amount == null || raw.date === '') {
      return null;
    }
    const notes = raw.notes.trim();
    return {
      amount: raw.amount,
      contributionDate: raw.date,
      ...(notes !== '' ? { notes } : {}),
    };
  }

  private redemptionRequest(): CreateGoalRedemptionRequest | null {
    const raw = this.movementForm.getRawValue();
    if (raw.amount == null || raw.date === '') {
      return null;
    }
    const notes = raw.notes.trim();
    return {
      amount: raw.amount,
      redemptionDate: raw.date,
      ...(notes !== '' ? { notes } : {}),
    };
  }

  private closePanels(): void {
    this.closeFormPanels();
    this.panelMode.set('closed');
    this.selectedGoal.set(null);
    this.contributions.set([]);
    this.redemptions.set([]);
    this.detailError.set(null);
    this.historyError.set(null);
  }

  private closeFormPanels(): void {
    if (this.formMode() !== 'closed') {
      this.closeForm();
    }
  }

  private resetGoalForm(): void {
    this.goalForm.reset({
      accountId: '',
      name: '',
      description: '',
      targetAmount: null,
      targetDate: '',
    });
    this.formError.set(null);
  }

  private resetMovementForm(): void {
    this.movementForm.reset({
      amount: null,
      date: todayIsoDate(),
      notes: '',
    });
    this.formError.set(null);
  }

  private handleMutationError(error: unknown, form: FormGroup = this.goalForm): void {
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
      this.setMutationMessage('Esta operação não é permitida para a meta no estado atual.');
      return;
    }

    if (error.code === 'NOT_FOUND') {
      this.setMutationMessage('Meta não encontrada.');
      return;
    }

    this.setMutationMessage('Não foi possível concluir a operação.');
  }

  private setMutationMessage(message: string): void {
    if (
      this.formMode() !== 'closed' ||
      this.panelMode() === 'cancel-confirm' ||
      this.panelMode() === 'complete-confirm'
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
