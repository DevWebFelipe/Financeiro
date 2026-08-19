import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { catchError, EMPTY, firstValueFrom, of, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { IsoDatePipe } from '../../shared/pipes/iso-date.pipe';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { todayIsoDate } from '../expenses/today-iso-date';
import { canReverseTransfer, transferStatusLabel } from './transfers-format';
import { CreateTransferRequest, Transfer, TransferListParams } from './transfers.models';
import { TransfersService } from './transfers.service';

type FormMode = 'closed' | 'create';
type PanelMode = 'closed' | 'detail' | 'reverse-confirm';

@Component({
  selector: 'app-transfers-page',
  imports: [ReactiveFormsModule, EmptyState, ErrorState, BrlCurrencyPipe, IsoDatePipe],
  templateUrl: './transfers-page.html',
  styleUrl: './transfers-page.css',
})
export class TransfersPage {
  private readonly transfersService = inject(TransfersService);
  private readonly accountsService = inject(AccountsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reload = new Subject<void>();

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly transfers = signal<Transfer[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly formMode = signal<FormMode>('closed');
  readonly panelMode = signal<PanelMode>('closed');
  readonly selectedTransfer = signal<Transfer | null>(null);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly accounts = signal<Account[]>([]);

  readonly accountFilter = signal('');
  readonly startDateFilter = signal('');
  readonly endDateFilter = signal('');

  readonly isEmpty = computed(() => this.status() === 'loaded' && this.transfers().length === 0);
  readonly hasFilters = computed(
    () =>
      this.accountFilter() !== '' || this.startDateFilter() !== '' || this.endDateFilter() !== '',
  );

  readonly transferForm = this.formBuilder.nonNullable.group(
    {
      sourceAccountId: ['', Validators.required],
      destinationAccountId: ['', Validators.required],
      amount: this.formBuilder.control<number | null>(null, [
        Validators.required,
        Validators.min(0.01),
        moneyDigitsValidator,
      ]),
      transferDate: [todayIsoDate(), Validators.required],
      description: ['', Validators.maxLength(255)],
    },
    { validators: distinctAccountsValidator },
  );

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
          this.status.set('loading');
          this.transfers.set([]);
          this.error.set(null);
          this.actionError.set(null);
          return this.transfersService.list(this.listParams()).pipe(
            catchError((loadError: unknown) => {
              this.error.set(isApiError(loadError) ? loadError : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((transfers) => {
        this.transfers.set(transfers);
        this.status.set('loaded');
      });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.submitting()) {
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

  onAccountFilterChange(value: string): void {
    this.accountFilter.set(value);
    this.reload.next();
  }

  onStartDateFilterChange(value: string): void {
    this.startDateFilter.set(value);
    this.reload.next();
  }

  onEndDateFilterChange(value: string): void {
    this.endDateFilter.set(value);
    this.reload.next();
  }

  accountName(accountId: string): string {
    return this.accounts().find((item) => item.id === accountId)?.name ?? '—';
  }

  statusLabel(status: Transfer['status']): string {
    return transferStatusLabel(status);
  }

  canReverseItem(transfer: Transfer): boolean {
    return canReverseTransfer(transfer);
  }

  openCreate(): void {
    this.closePanels();
    this.resetTransferForm();
    this.formMode.set('create');
  }

  async openDetail(transfer: Transfer): Promise<void> {
    this.closeFormPanels();
    this.panelMode.set('detail');
    this.selectedTransfer.set(transfer);
    this.detailError.set(null);
    this.detailLoading.set(true);

    try {
      const detail = await firstValueFrom(this.transfersService.get(transfer.id));
      this.selectedTransfer.set(detail);
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da transferência.');
    } finally {
      this.detailLoading.set(false);
    }
  }

  closeDetail(): void {
    if (this.panelMode() === 'detail') {
      this.panelMode.set('closed');
    }
    this.selectedTransfer.set(null);
    this.detailError.set(null);
  }

  openReverseConfirm(transfer: Transfer): void {
    if (!this.canReverseItem(transfer)) {
      return;
    }
    this.closeFormPanels();
    this.selectedTransfer.set(transfer);
    this.panelMode.set('reverse-confirm');
  }

  closeReverseConfirm(): void {
    if (this.selectedTransfer() != null) {
      this.panelMode.set('detail');
      return;
    }
    this.panelMode.set('closed');
  }

  closeForm(): void {
    this.resetTransferForm();
    this.formMode.set('closed');
    this.formError.set(null);
    if (this.panelMode() === 'closed') {
      this.selectedTransfer.set(null);
    }
  }

  async submitTransferForm(): Promise<void> {
    if (this.transferForm.invalid || this.submitting()) {
      this.transferForm.markAllAsTouched();
      return;
    }

    if (this.formMode() !== 'create') {
      return;
    }

    const raw = this.transferForm.getRawValue();
    if (raw.amount == null) {
      return;
    }

    const description = raw.description.trim();
    const request: CreateTransferRequest = {
      sourceAccountId: raw.sourceAccountId,
      destinationAccountId: raw.destinationAccountId,
      amount: raw.amount,
      transferDate: raw.transferDate,
      ...(description !== '' ? { description } : {}),
    };

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      await firstValueFrom(this.transfersService.create(request));
      this.closeForm();
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError, this.transferForm);
    } finally {
      this.submitting.set(false);
    }
  }

  async confirmReverse(): Promise<void> {
    const transfer = this.selectedTransfer();
    if (transfer == null || this.submitting() || !this.canReverseItem(transfer)) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.formError.set(null);

    try {
      await firstValueFrom(this.transfersService.reverse(transfer.id));
      this.panelMode.set('closed');
      this.selectedTransfer.set(null);
      this.reload.next();
    } catch (mutationError: unknown) {
      this.handleMutationError(mutationError);
    } finally {
      this.submitting.set(false);
    }
  }

  fieldError(
    controlName:
      'sourceAccountId' | 'destinationAccountId' | 'amount' | 'transferDate' | 'description',
  ): string | null {
    if (
      (controlName === 'sourceAccountId' || controlName === 'destinationAccountId') &&
      this.transferForm.hasError('sameAccount') &&
      (this.transferForm.controls.sourceAccountId.touched ||
        this.transferForm.controls.destinationAccountId.touched)
    ) {
      return 'Origem e destino devem ser contas diferentes.';
    }
    return this.controlError(this.transferForm, controlName);
  }

  private listParams(): TransferListParams {
    const accountId = this.accountFilter();
    const startDate = this.startDateFilter();
    const endDate = this.endDateFilter();

    return {
      ...(accountId !== '' ? { accountId } : {}),
      ...(startDate !== '' ? { startDate } : {}),
      ...(endDate !== '' ? { endDate } : {}),
    };
  }

  private closePanels(): void {
    this.closeFormPanels();
    this.panelMode.set('closed');
    this.selectedTransfer.set(null);
    this.detailError.set(null);
  }

  private closeFormPanels(): void {
    if (this.formMode() !== 'closed') {
      this.closeForm();
    }
  }

  private resetTransferForm(): void {
    this.transferForm.reset({
      sourceAccountId: '',
      destinationAccountId: '',
      amount: null,
      transferDate: todayIsoDate(),
      description: '',
    });
    this.formError.set(null);
  }

  private handleMutationError(error: unknown, form: FormGroup = this.transferForm): void {
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
      this.setMutationMessage(
        'Esta operação não é permitida para a transferência no estado atual.',
      );
      return;
    }

    if (error.code === 'NOT_FOUND') {
      this.setMutationMessage('Transferência não encontrada.');
      return;
    }

    this.setMutationMessage('Não foi possível concluir a operação.');
  }

  private setMutationMessage(message: string): void {
    if (this.formMode() !== 'closed' || this.panelMode() === 'reverse-confirm') {
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

  private controlError(form: FormGroup, controlName: string): string | null {
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
    if (control.hasError('digits')) {
      return 'O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.';
    }
    if (control.hasError('maxlength')) {
      return 'Texto muito longo.';
    }
    return null;
  }
}

function moneyDigitsValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value == null || value === '') {
    return null;
  }
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return { digits: true };
  }
  const [integerPart, fractionPart = ''] = String(value).split('.');
  const integerDigits = integerPart.replace('-', '').replace(/^0+(?=\d)/, '');
  if (integerDigits.length > 17 || fractionPart.length > 2) {
    return { digits: true };
  }
  return null;
}

function distinctAccountsValidator(control: AbstractControl): ValidationErrors | null {
  const source = control.get('sourceAccountId')?.value;
  const destination = control.get('destinationAccountId')?.value;
  if (typeof source !== 'string' || typeof destination !== 'string') {
    return null;
  }
  if (source === '' || destination === '') {
    return null;
  }
  return source === destination ? { sameAccount: true } : null;
}
