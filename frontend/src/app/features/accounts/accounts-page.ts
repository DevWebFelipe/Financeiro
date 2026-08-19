import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, EMPTY, firstValueFrom, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { accountStatusLabel, accountTypeLabel, canDeactivateAccount } from './accounts-format';
import { AccountWithBalance, ACCOUNT_TYPE_OPTIONS, WritableAccountType } from './accounts.models';
import { AccountsService } from './accounts.service';

@Component({
  selector: 'app-accounts-page',
  imports: [ReactiveFormsModule, EmptyState, ErrorState, BrlCurrencyPipe],
  templateUrl: './accounts-page.html',
  styleUrl: './accounts-page.css',
})
export class AccountsPage {
  private readonly accountsService = inject(AccountsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reload = new Subject<void>();
  private pendingAction: 'create' | 'update' | 'deactivate' | 'activate' | null = null;

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly accounts = signal<AccountWithBalance[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly formMode = signal<'closed' | 'create' | 'edit'>('closed');
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  readonly isEmpty = computed(() => this.status() === 'loaded' && this.accounts().length === 0);
  readonly typeOptions = ACCOUNT_TYPE_OPTIONS;
  readonly accountTypeLabel = accountTypeLabel;
  readonly accountStatusLabel = accountStatusLabel;

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    type: this.formBuilder.nonNullable.control<WritableAccountType | ''>('', Validators.required),
    initialBalance: this.formBuilder.control<number | null>(null),
  });

  constructor() {
    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.status.set('loading');
          this.accounts.set([]);
          this.error.set(null);
          this.actionError.set(null);
          return this.accountsService.listWithBalances().pipe(
            catchError((error: unknown) => {
              this.error.set(isApiError(error) ? error : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((accounts) => {
        this.accounts.set(accounts);
        this.status.set('loaded');
      });
  }

  retry(): void {
    this.reload.next();
  }

  canDeactivateItem(item: AccountWithBalance): boolean {
    return canDeactivateAccount(
      item.account.active,
      item.balance.totalBalance,
      item.balance.reservedAmount,
    );
  }

  openCreate(): void {
    this.resetForm();
    this.formMode.set('create');
    this.editingId.set(null);
  }

  openEdit(item: AccountWithBalance): void {
    this.resetForm();
    const writableType = this.toWritableType(item.account.type);
    this.form.patchValue({
      name: item.account.name,
      type: writableType ?? '',
      initialBalance: null,
    });
    this.formMode.set('edit');
    this.editingId.set(item.account.id);
  }

  closeForm(): void {
    this.resetForm();
    this.formMode.set('closed');
    this.editingId.set(null);
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting() || this.formMode() === 'closed') {
      this.form.markAllAsTouched();
      return;
    }

    const mode = this.formMode();
    const { name, type, initialBalance } = this.form.getRawValue();
    if (type === '') {
      this.form.controls.type.markAsTouched();
      return;
    }

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      if (mode === 'create') {
        this.pendingAction = 'create';
        const request =
          initialBalance == null
            ? { name: name.trim(), type }
            : { name: name.trim(), type, initialBalance };
        await firstValueFrom(this.accountsService.create(request));
      } else {
        const accountId = this.editingId();
        if (accountId == null) {
          return;
        }
        this.pendingAction = 'update';
        await firstValueFrom(this.accountsService.update(accountId, { name: name.trim(), type }));
      }
      this.closeForm();
      this.reload.next();
    } catch (error: unknown) {
      this.handleMutationError(error);
    } finally {
      this.submitting.set(false);
      this.pendingAction = null;
    }
  }

  async deactivate(item: AccountWithBalance): Promise<void> {
    if (this.submitting() || !this.canDeactivateItem(item)) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.pendingAction = 'deactivate';
    try {
      await firstValueFrom(this.accountsService.deactivate(item.account.id));
      this.reload.next();
    } catch (error: unknown) {
      this.handleMutationError(error);
    } finally {
      this.submitting.set(false);
      this.pendingAction = null;
    }
  }

  async activate(item: AccountWithBalance): Promise<void> {
    if (this.submitting() || item.account.active) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.pendingAction = 'activate';
    try {
      await firstValueFrom(this.accountsService.activate(item.account.id));
      this.reload.next();
    } catch (error: unknown) {
      this.handleMutationError(error);
    } finally {
      this.submitting.set(false);
      this.pendingAction = null;
    }
  }

  fieldError(controlName: 'name' | 'type' | 'initialBalance'): string | null {
    const control = this.form.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('api')) {
      const apiError = control.getError('api');
      return typeof apiError === 'string' ? apiError : null;
    }
    if (control.hasError('required')) {
      if (controlName === 'name') {
        return 'Informe o nome.';
      }
      if (controlName === 'type') {
        return 'Selecione o tipo.';
      }
    }
    if (control.hasError('maxlength')) {
      return 'O nome deve ter no máximo 255 caracteres.';
    }
    return null;
  }

  private handleMutationError(error: unknown): void {
    if (!isApiError(error)) {
      this.setMutationMessage('Não foi possível concluir a operação.');
      return;
    }

    if (error.code === 'VALIDATION_ERROR' && error.fields != null) {
      this.applyFieldErrors(error.fields);
      if (this.formError() == null && this.unmappedFieldCount(error.fields) > 0) {
        this.formError.set('Revise os dados informados.');
      }
      return;
    }

    if (error.code === 'VALIDATION_ERROR') {
      this.formError.set('Revise os dados informados.');
      return;
    }

    if (error.status === 403) {
      this.actionError.set('Você não tem permissão para esta operação.');
      return;
    }

    if (error.code === 'BUSINESS_RULE_VIOLATION') {
      this.actionError.set(this.businessRuleMessage());
      return;
    }

    this.setMutationMessage('Não foi possível concluir a operação.');
  }

  private businessRuleMessage(): string {
    if (this.pendingAction === 'deactivate') {
      return 'Não é possível desativar esta conta enquanto houver saldo total ou valor reservado diferente de zero.';
    }
    return 'A operação não pôde ser concluída pelas regras da conta.';
  }

  private setMutationMessage(message: string): void {
    if (this.formMode() === 'closed') {
      this.actionError.set(message);
      return;
    }
    this.formError.set(message);
  }

  private applyFieldErrors(fields: Record<string, string>): void {
    for (const [key, message] of Object.entries(fields)) {
      const control = this.form.get(key);
      if (control == null) {
        continue;
      }
      control.setErrors({ ...control.errors, api: message });
      control.markAsTouched();
    }
  }

  private unmappedFieldCount(fields: Record<string, string>): number {
    return Object.keys(fields).filter((key) => this.form.get(key) == null).length;
  }

  private resetForm(): void {
    this.form.reset({ name: '', type: '', initialBalance: null });
    this.formError.set(null);
  }

  private toWritableType(type: string): WritableAccountType | null {
    return type === 'BANK_ACCOUNT' || type === 'CASH' ? type : null;
  }
}
