import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, EMPTY, firstValueFrom, forkJoin, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { creditCardStatusLabel, formatLastFourDigits } from './credit-cards-format';
import {
  CreateCreditCardRequest,
  CreditCard,
  CreditCardLimit,
  CreditCardListParams,
  CreditCardWithLimit,
  UpdateCreditCardRequest,
} from './credit-cards.models';
import { CreditCardsService } from './credit-cards.service';

type FormMode = 'closed' | 'create' | 'edit';
type PanelMode = 'closed' | 'detail' | 'deactivate-confirm' | 'activate-confirm';

@Component({
  selector: 'app-credit-cards-page',
  imports: [ReactiveFormsModule, EmptyState, ErrorState, BrlCurrencyPipe],
  templateUrl: './credit-cards-page.html',
  styleUrl: './credit-cards-page.css',
})
export class CreditCardsPage {
  private readonly creditCardsService = inject(CreditCardsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reload = new Subject<void>();
  private pendingAction: 'create' | 'update' | 'deactivate' | 'activate' | null = null;

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly cards = signal<CreditCardWithLimit[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly formMode = signal<FormMode>('closed');
  readonly panelMode = signal<PanelMode>('closed');
  readonly selectedCard = signal<CreditCard | null>(null);
  readonly selectedLimit = signal<CreditCardLimit | null>(null);
  readonly pendingCard = signal<CreditCardWithLimit | null>(null);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly holderNameFilter = signal('');
  private readonly apiFieldErrors = signal<Record<string, string>>({});

  readonly isEmpty = computed(() => this.status() === 'loaded' && this.cards().length === 0);
  readonly hasFilters = computed(() => this.holderNameFilter() !== '');
  readonly emptyMessage = computed(() =>
    this.hasFilters()
      ? 'Nenhum cartão corresponde ao filtro informado.'
      : 'Nenhum cartão cadastrado.',
  );
  readonly statusLabel = creditCardStatusLabel;
  readonly lastFourLabel = formatLastFourDigits;

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    holderName: ['', [Validators.required, Validators.maxLength(255)]],
    lastFourDigits: ['', [Validators.maxLength(4), Validators.pattern(/^(\d{4})?$/)]],
    creditLimit: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    closingDay: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(31),
    ]),
    dueDay: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(31),
    ]),
  });

  constructor() {
    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.status.set('loading');
          this.cards.set([]);
          this.error.set(null);
          this.actionError.set(null);
          return this.creditCardsService.listWithLimits(this.listParams()).pipe(
            catchError((error: unknown) => {
              this.error.set(isApiError(error) ? error : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((cards) => {
        this.cards.set(cards);
        this.status.set('loaded');
        this.refreshOpenDetail(cards);
      });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.submitting()) {
      return;
    }
    if (this.panelMode() === 'deactivate-confirm' || this.panelMode() === 'activate-confirm') {
      this.closeConfirm();
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

  onHolderNameFilterChange(value: string): void {
    this.holderNameFilter.set(value.trim());
    this.reload.next();
  }

  clearHolderNameFilter(): void {
    this.holderNameFilter.set('');
    this.reload.next();
  }

  openCreate(): void {
    this.closePanels();
    this.resetForm();
    this.formMode.set('create');
    this.editingId.set(null);
  }

  openEdit(item: CreditCardWithLimit): void {
    this.closePanels();
    this.resetForm();
    this.form.patchValue({
      name: item.card.name,
      holderName: item.card.holderName,
      lastFourDigits: item.card.lastFourDigits ?? '',
      creditLimit: item.card.creditLimit,
      closingDay: item.card.closingDay,
      dueDay: item.card.dueDay,
    });
    this.formMode.set('edit');
    this.editingId.set(item.card.id);
  }

  closeForm(): void {
    this.resetForm();
    this.formMode.set('closed');
    this.editingId.set(null);
  }

  async openDetail(item: CreditCardWithLimit): Promise<void> {
    this.closeForm();
    this.pendingCard.set(null);
    this.panelMode.set('detail');
    this.selectedCard.set(item.card);
    this.selectedLimit.set(item.limit);
    this.detailError.set(null);
    this.detailLoading.set(true);

    try {
      const detail = await firstValueFrom(
        forkJoin({
          card: this.creditCardsService.get(item.card.id),
          limit: this.creditCardsService.getLimit(item.card.id),
        }),
      );
      this.selectedCard.set(detail.card);
      this.selectedLimit.set(detail.limit);
    } catch (error: unknown) {
      this.detailError.set(
        isApiError(error)
          ? 'Não foi possível carregar os detalhes do cartão.'
          : 'Não foi possível carregar os detalhes do cartão.',
      );
    } finally {
      this.detailLoading.set(false);
    }
  }

  closeDetail(): void {
    this.panelMode.set('closed');
    this.selectedCard.set(null);
    this.selectedLimit.set(null);
    this.detailError.set(null);
    this.detailLoading.set(false);
  }

  openDeactivateConfirm(item: CreditCardWithLimit): void {
    if (this.submitting() || !item.card.active) {
      return;
    }
    this.closeForm();
    this.pendingCard.set(item);
    this.panelMode.set('deactivate-confirm');
  }

  openActivateConfirm(item: CreditCardWithLimit): void {
    if (this.submitting() || item.card.active) {
      return;
    }
    this.closeForm();
    this.pendingCard.set(item);
    this.panelMode.set('activate-confirm');
  }

  closeConfirm(): void {
    const keepDetail = this.selectedCard() != null;
    this.pendingCard.set(null);
    this.formError.set(null);
    this.panelMode.set(keepDetail ? 'detail' : 'closed');
  }

  async confirmDeactivate(): Promise<void> {
    const item = this.pendingCard();
    if (item == null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);
    this.pendingAction = 'deactivate';
    try {
      await firstValueFrom(this.creditCardsService.deactivate(item.card.id));
      this.pendingCard.set(null);
      this.panelMode.set(this.selectedCard() != null ? 'detail' : 'closed');
      this.reload.next();
    } catch (error: unknown) {
      this.handleMutationError(error);
    } finally {
      this.submitting.set(false);
      this.pendingAction = null;
    }
  }

  async confirmActivate(): Promise<void> {
    const item = this.pendingCard();
    if (item == null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);
    this.pendingAction = 'activate';
    try {
      await firstValueFrom(this.creditCardsService.activate(item.card.id));
      this.pendingCard.set(null);
      this.panelMode.set(this.selectedCard() != null ? 'detail' : 'closed');
      this.reload.next();
    } catch (error: unknown) {
      this.handleMutationError(error);
    } finally {
      this.submitting.set(false);
      this.pendingAction = null;
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting() || this.formMode() === 'closed') {
      this.form.markAllAsTouched();
      return;
    }

    const mode = this.formMode();
    const raw = this.form.getRawValue();
    if (raw.creditLimit == null || raw.closingDay == null || raw.dueDay == null) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.toWriteRequest(raw, raw.creditLimit, raw.closingDay, raw.dueDay);
    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);
    this.apiFieldErrors.set({});

    try {
      if (mode === 'create') {
        this.pendingAction = 'create';
        await firstValueFrom(this.creditCardsService.create(request));
      } else {
        const cardId = this.editingId();
        if (cardId == null) {
          return;
        }
        this.pendingAction = 'update';
        await firstValueFrom(this.creditCardsService.update(cardId, request));
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

  fieldError(
    controlName: 'name' | 'holderName' | 'lastFourDigits' | 'creditLimit' | 'closingDay' | 'dueDay',
  ): string | null {
    const apiMessage = this.apiFieldErrors()[controlName];
    if (apiMessage != null) {
      return apiMessage;
    }
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
        return 'O nome é obrigatório.';
      }
      if (controlName === 'holderName') {
        return 'O titular é obrigatório.';
      }
      if (controlName === 'creditLimit') {
        return 'O limite é obrigatório.';
      }
      if (controlName === 'closingDay') {
        return 'O dia de fechamento é obrigatório.';
      }
      if (controlName === 'dueDay') {
        return 'O dia de vencimento é obrigatório.';
      }
    }
    if (control.hasError('maxlength')) {
      if (controlName === 'holderName') {
        return 'O titular deve ter no máximo 255 caracteres.';
      }
      if (controlName === 'lastFourDigits') {
        return 'Os últimos dígitos devem ter no máximo 4 caracteres.';
      }
      return 'O nome deve ter no máximo 255 caracteres.';
    }
    if (control.hasError('pattern')) {
      return 'Informe exatamente 4 dígitos.';
    }
    if (control.hasError('min') || control.hasError('max')) {
      if (controlName === 'creditLimit') {
        return 'O limite deve ser maior que zero.';
      }
      if (controlName === 'closingDay') {
        return 'O dia de fechamento deve ser entre 1 e 31.';
      }
      if (controlName === 'dueDay') {
        return 'O dia de vencimento deve ser entre 1 e 31.';
      }
    }
    return null;
  }

  private listParams(): CreditCardListParams {
    const holderName = this.holderNameFilter();
    return holderName === '' ? {} : { holderName };
  }

  private toWriteRequest(
    raw: {
      name: string;
      holderName: string;
      lastFourDigits: string;
    },
    creditLimit: number,
    closingDay: number,
    dueDay: number,
  ): CreateCreditCardRequest | UpdateCreditCardRequest {
    const lastFourDigits = raw.lastFourDigits.trim();
    const request = {
      name: raw.name.trim(),
      holderName: raw.holderName.trim(),
      creditLimit,
      closingDay,
      dueDay,
    };
    return lastFourDigits === '' ? request : { ...request, lastFourDigits };
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
      this.setMutationMessage('A operação não pôde ser concluída pelas regras do cartão.');
      return;
    }

    this.setMutationMessage('Não foi possível concluir a operação.');
  }

  private setMutationMessage(message: string): void {
    if (this.formMode() !== 'closed') {
      this.formError.set(message);
      return;
    }
    if (this.panelMode() === 'deactivate-confirm' || this.panelMode() === 'activate-confirm') {
      this.formError.set(message);
      return;
    }
    this.actionError.set(message);
  }

  private applyFieldErrors(fields: Record<string, string>): void {
    this.apiFieldErrors.set(fields);
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
    this.form.reset({
      name: '',
      holderName: '',
      lastFourDigits: '',
      creditLimit: null,
      closingDay: null,
      dueDay: null,
    });
    this.formError.set(null);
    this.apiFieldErrors.set({});
  }

  private closePanels(): void {
    this.panelMode.set('closed');
    this.selectedCard.set(null);
    this.selectedLimit.set(null);
    this.pendingCard.set(null);
    this.detailError.set(null);
    this.detailLoading.set(false);
  }

  private refreshOpenDetail(cards: CreditCardWithLimit[]): void {
    const selected = this.selectedCard();
    if (selected == null || this.panelMode() === 'closed') {
      return;
    }
    const updated = cards.find((item) => item.card.id === selected.id);
    if (updated == null) {
      return;
    }
    this.selectedCard.set(updated.card);
    this.selectedLimit.set(updated.limit);
  }
}
