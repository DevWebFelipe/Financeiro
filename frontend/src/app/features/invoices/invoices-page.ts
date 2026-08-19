import { Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import type { AbstractControl, ValidationErrors } from '@angular/forms';
import { catchError, EMPTY, firstValueFrom, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { BrlCurrencyPipe } from '../../shared/pipes/brl-currency.pipe';
import { IsoDatePipe } from '../../shared/pipes/iso-date.pipe';
import { YearMonthPipe } from '../../shared/pipes/year-month.pipe';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { formatLastFourDigits } from '../credit-cards/credit-cards-format';
import { CreditCard } from '../credit-cards/credit-cards.models';
import { CreditCardsService } from '../credit-cards/credit-cards.service';
import { todayIsoDate } from '../expenses/today-iso-date';
import {
  canAdjustInvoice,
  canCreateInvoiceAgreement,
  canCreateInvoiceSurcharge,
  canPayAgreementInstallment,
  canPayInvoice,
  canReverseInvoiceAdjustment,
  canReverseInvoicePayment,
  formatAdditionalCostPercent,
  formatInvoiceInstantDate,
  invoiceAdjustmentStatusLabel,
  invoiceAdjustmentTypeLabel,
  invoiceAgreementStatusLabel,
  invoiceItemStatusLabel,
  invoicePaymentStatusLabel,
  invoicePeriodKey,
  invoiceStatusLabel,
} from './invoices-format';
import {
  AnticipateAgreementInstallmentRequest,
  CreateInvoiceAdjustmentRequest,
  CreateInvoiceAgreementRequest,
  CreateInvoiceRenegotiationRequest,
  INVOICE_ADJUSTMENT_TYPE_OPTIONS,
  INVOICE_MONTH_OPTIONS,
  INVOICE_STATUS_OPTIONS,
  Invoice,
  InvoiceAdjustment,
  InvoiceAdjustmentType,
  InvoiceAgreement,
  InvoiceAgreementInstallment,
  InvoiceItem,
  InvoiceListParams,
  InvoicePayment,
  InvoiceStatusFilter,
  PayInvoiceRequest,
} from './invoices.models';
import { InvoicesService } from './invoices.service';

@Component({
  selector: 'app-invoices-page',
  imports: [
    EmptyState,
    ErrorState,
    BrlCurrencyPipe,
    IsoDatePipe,
    YearMonthPipe,
    ReactiveFormsModule,
  ],
  templateUrl: './invoices-page.html',
  styleUrl: './invoices-page.css',
})
export class InvoicesPage {
  private readonly invoicesService = inject(InvoicesService);
  private readonly creditCardsService = inject(CreditCardsService);
  private readonly accountsService = inject(AccountsService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reloadCards = new Subject<void>();
  private readonly reloadInvoices = new Subject<void>();
  private readonly reloadAccounts = new Subject<void>();

  readonly cardsStatus = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly invoicesStatus = signal<'idle' | 'loading' | 'loaded' | 'error'>('idle');
  readonly accountsStatus = signal<'idle' | 'loading' | 'loaded' | 'error'>('idle');
  readonly cards = signal<CreditCard[]>([]);
  readonly invoices = signal<Invoice[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly cardsError = signal<ApiError | null>(null);
  readonly invoicesError = signal<ApiError | null>(null);

  readonly selectedCardId = signal('');
  readonly yearFilter = signal('');
  readonly monthFilter = signal('');
  readonly statusFilter = signal<InvoiceStatusFilter>('');

  readonly selectedInvoice = signal<Invoice | null>(null);
  readonly items = signal<InvoiceItem[]>([]);
  readonly payments = signal<InvoicePayment[]>([]);
  readonly adjustments = signal<InvoiceAdjustment[]>([]);
  readonly detailLoading = signal(false);
  readonly detailError = signal<string | null>(null);
  readonly itemsError = signal<string | null>(null);
  readonly itemsLoading = signal(false);
  readonly paymentsStatus = signal<'idle' | 'loading' | 'loaded' | 'error'>('idle');
  readonly paymentsError = signal<string | null>(null);
  readonly adjustmentsStatus = signal<'idle' | 'loading' | 'loaded' | 'error'>('idle');
  readonly adjustmentsError = signal<string | null>(null);
  readonly agreements = signal<InvoiceAgreement[]>([]);
  readonly agreementsStatus = signal<'idle' | 'loading' | 'loaded' | 'error'>('idle');
  readonly agreementsError = signal<string | null>(null);
  readonly payError = signal<string | null>(null);
  readonly adjustError = signal<string | null>(null);
  readonly reverseError = signal<string | null>(null);
  readonly adjustReverseError = signal<string | null>(null);
  readonly agreementFormError = signal<string | null>(null);
  readonly agreementSuccess = signal<string | null>(null);
  readonly installmentPayError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly paymentPendingReverse = signal<InvoicePayment | null>(null);
  readonly adjustmentPendingReverse = signal<InvoiceAdjustment | null>(null);
  readonly agreementFormMode = signal<'closed' | 'negotiate' | 'renegotiate'>('closed');
  readonly agreementConfirming = signal(false);
  readonly pendingInstallmentPay = signal<{
    agreement: InvoiceAgreement;
    installment: InvoiceAgreementInstallment;
  } | null>(null);
  readonly installmentSettleConfirming = signal(false);

  readonly payForm = this.formBuilder.nonNullable.group({
    accountId: ['', Validators.required],
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    paymentDate: [todayIsoDate(), Validators.required],
    notes: [''],
  });

  readonly adjustForm = this.formBuilder.nonNullable.group({
    type: this.formBuilder.control<InvoiceAdjustmentType>('DISCOUNT', {
      validators: [Validators.required],
    }),
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    reason: ['', Validators.required],
  });

  readonly agreementForm = this.formBuilder.nonNullable.group({
    entryAmount: this.formBuilder.control<number | null>(0, [
      Validators.required,
      Validators.min(0),
      moneyDigitsValidator,
    ]),
    accountId: ['', Validators.required],
    entryPaymentDate: [todayIsoDate(), Validators.required],
    installmentCount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      integerValidator,
    ]),
    installmentAmount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      moneyDigitsValidator,
    ]),
    anticipatedFuturesNetAmount: this.formBuilder.control<number | null>(0, [
      Validators.min(0),
      moneyDigitsValidator,
    ]),
  });

  readonly installmentPayForm = this.formBuilder.nonNullable.group({
    accountId: ['', Validators.required],
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    paymentDate: [todayIsoDate(), Validators.required],
    settled: [false],
    notes: [''],
  });

  readonly statusOptions = INVOICE_STATUS_OPTIONS;
  readonly monthOptions = INVOICE_MONTH_OPTIONS;
  readonly statusLabel = invoiceStatusLabel;
  readonly itemStatusLabel = invoiceItemStatusLabel;
  readonly paymentStatusLabel = invoicePaymentStatusLabel;
  readonly periodKey = invoicePeriodKey;
  readonly canPay = canPayInvoice;
  readonly canReversePayment = canReverseInvoicePayment;
  readonly canAdjust = canAdjustInvoice;
  readonly canReverseAdjustment = canReverseInvoiceAdjustment;
  readonly canCreateSurcharge = canCreateInvoiceSurcharge;
  readonly canCreateAgreement = canCreateInvoiceAgreement;
  readonly canPayInstallment = canPayAgreementInstallment;
  readonly agreementStatusLabel = invoiceAgreementStatusLabel;
  readonly additionalCostPercent = formatAdditionalCostPercent;
  readonly adjustmentTypeLabel = invoiceAdjustmentTypeLabel;
  readonly adjustmentStatusLabel = invoiceAdjustmentStatusLabel;
  readonly instantDate = formatInvoiceInstantDate;
  readonly adjustmentTypeOptions = INVOICE_ADJUSTMENT_TYPE_OPTIONS;

  readonly isCardsEmpty = computed(
    () => this.cardsStatus() === 'loaded' && this.cards().length === 0,
  );
  readonly isInvoicesEmpty = computed(
    () => this.invoicesStatus() === 'loaded' && this.invoices().length === 0,
  );
  readonly showInvoiceFilters = computed(
    () => this.cardsStatus() === 'loaded' && this.cards().length > 0,
  );
  readonly activeAccounts = computed(() => this.accounts().filter((account) => account.active));
  readonly isPaymentsEmpty = computed(
    () => this.paymentsStatus() === 'loaded' && this.payments().length === 0,
  );
  readonly isAdjustmentsEmpty = computed(
    () => this.adjustmentsStatus() === 'loaded' && this.adjustments().length === 0,
  );
  readonly isAgreementsEmpty = computed(
    () => this.agreementsStatus() === 'loaded' && this.agreements().length === 0,
  );

  constructor() {
    this.reloadCards
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.cardsStatus.set('loading');
          this.cards.set([]);
          this.cardsError.set(null);
          return this.creditCardsService.list().pipe(
            catchError((error: unknown) => {
              this.cardsError.set(isApiError(error) ? error : null);
              this.cardsStatus.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((cards) => {
        this.cards.set(cards);
        this.cardsStatus.set('loaded');
      });

    this.reloadAccounts
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.accountsStatus.set('loading');
          this.accounts.set([]);
          return this.accountsService.list().pipe(
            catchError(() => {
              this.accountsStatus.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((accounts) => {
        this.accounts.set(accounts);
        this.accountsStatus.set('loaded');
      });

    this.reloadInvoices
      .pipe(
        switchMap(() => {
          const cardId = this.selectedCardId();
          if (cardId === '') {
            this.invoices.set([]);
            this.invoicesStatus.set('idle');
            return EMPTY;
          }
          this.invoicesStatus.set('loading');
          this.invoices.set([]);
          this.invoicesError.set(null);
          return this.invoicesService.listByCard(cardId, this.listParams()).pipe(
            catchError((error: unknown) => {
              this.invoicesError.set(isApiError(error) ? error : null);
              this.invoicesStatus.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((invoices) => {
        this.invoices.set(invoices);
        this.invoicesStatus.set('loaded');
      });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.submitting()) {
      return;
    }
    if (this.installmentSettleConfirming()) {
      this.closeInstallmentSettleConfirm();
      return;
    }
    if (this.agreementConfirming()) {
      this.closeAgreementConfirm();
      return;
    }
    if (this.pendingInstallmentPay() != null) {
      this.closeInstallmentPay();
      return;
    }
    if (this.agreementFormMode() !== 'closed') {
      this.closeAgreementForm();
      return;
    }
    if (this.adjustmentPendingReverse() != null) {
      this.closeAdjustReverseConfirm();
      return;
    }
    if (this.paymentPendingReverse() != null) {
      this.closeReverseConfirm();
      return;
    }
    this.closeDetail();
  }

  retryCards(): void {
    this.reloadCards.next();
  }

  retryInvoices(): void {
    this.reloadInvoices.next();
  }

  retryAccounts(): void {
    this.reloadAccounts.next();
  }

  onCardFilterChange(value: string): void {
    this.selectedCardId.set(value);
    this.closeDetail();
    this.reloadInvoices.next();
  }

  onYearFilterChange(value: string): void {
    this.yearFilter.set(value.trim());
    this.reloadIfCardSelected();
  }

  onMonthFilterChange(value: string): void {
    this.monthFilter.set(value);
    this.reloadIfCardSelected();
  }

  onStatusFilterChange(value: InvoiceStatusFilter): void {
    this.statusFilter.set(value);
    this.reloadIfCardSelected();
  }

  cardLabel(card: CreditCard): string {
    const digits = formatLastFourDigits(card.lastFourDigits);
    const base = `${card.name} — ${card.holderName}`;
    const withDigits = digits == null ? base : `${base} ${digits}`;
    return card.active ? withDigits : `${withDigits} (inativo)`;
  }

  cardName(cardId: string): string {
    const card = this.cards().find((item) => item.id === cardId);
    return card == null ? '—' : this.cardLabel(card);
  }

  accountName(accountId: string): string {
    const account = this.accounts().find((item) => item.id === accountId);
    return account == null ? '—' : account.name;
  }

  async openDetail(invoice: Invoice): Promise<void> {
    this.selectedInvoice.set(invoice);
    this.items.set([]);
    this.payments.set([]);
    this.adjustments.set([]);
    this.agreements.set([]);
    this.detailError.set(null);
    this.itemsError.set(null);
    this.paymentsError.set(null);
    this.adjustmentsError.set(null);
    this.agreementsError.set(null);
    this.payError.set(null);
    this.adjustError.set(null);
    this.reverseError.set(null);
    this.adjustReverseError.set(null);
    this.agreementFormError.set(null);
    this.agreementSuccess.set(null);
    this.installmentPayError.set(null);
    this.paymentPendingReverse.set(null);
    this.adjustmentPendingReverse.set(null);
    this.detailLoading.set(true);
    this.itemsLoading.set(false);
    this.paymentsStatus.set('idle');
    this.adjustmentsStatus.set('idle');
    this.agreementsStatus.set('idle');
    this.closeAgreementForm();
    this.closeInstallmentPay();

    try {
      const detail = await firstValueFrom(this.invoicesService.get(invoice.id));
      this.selectedInvoice.set(detail);
      this.resetPayForm(detail);
      this.resetAdjustForm();
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da fatura.');
      this.detailLoading.set(false);
      return;
    }

    this.detailLoading.set(false);
    await Promise.all([
      this.reloadItems(invoice.id),
      this.reloadPayments(invoice.id),
      this.reloadAdjustments(invoice.id),
      this.reloadAgreements(invoice.id),
    ]);
  }

  closeDetail(): void {
    this.selectedInvoice.set(null);
    this.items.set([]);
    this.payments.set([]);
    this.adjustments.set([]);
    this.agreements.set([]);
    this.detailError.set(null);
    this.itemsError.set(null);
    this.paymentsError.set(null);
    this.adjustmentsError.set(null);
    this.agreementsError.set(null);
    this.payError.set(null);
    this.adjustError.set(null);
    this.reverseError.set(null);
    this.adjustReverseError.set(null);
    this.agreementFormError.set(null);
    this.agreementSuccess.set(null);
    this.installmentPayError.set(null);
    this.detailLoading.set(false);
    this.itemsLoading.set(false);
    this.paymentsStatus.set('idle');
    this.adjustmentsStatus.set('idle');
    this.agreementsStatus.set('idle');
    this.submitting.set(false);
    this.paymentPendingReverse.set(null);
    this.adjustmentPendingReverse.set(null);
    this.closeAgreementForm();
    this.closeInstallmentPay();
  }

  retryItems(): void {
    const invoice = this.selectedInvoice();
    if (invoice == null) {
      return;
    }
    void this.reloadItems(invoice.id);
  }

  retryPayments(): void {
    const invoice = this.selectedInvoice();
    if (invoice == null) {
      return;
    }
    void this.reloadPayments(invoice.id);
  }

  retryAdjustments(): void {
    const invoice = this.selectedInvoice();
    if (invoice == null) {
      return;
    }
    void this.reloadAdjustments(invoice.id);
  }

  retryAgreements(): void {
    const invoice = this.selectedInvoice();
    if (invoice == null) {
      return;
    }
    void this.reloadAgreements(invoice.id);
  }

  payFieldError(controlName: 'accountId' | 'amount' | 'paymentDate'): string | null {
    const control = this.payForm.get(controlName);
    if (control == null || !control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }
    if (control.hasError('min')) {
      return 'Informe um valor maior que zero.';
    }
    if (control.hasError('max')) {
      return 'O valor não pode ser maior que o restante da fatura.';
    }
    return null;
  }

  adjustFieldError(controlName: 'type' | 'amount' | 'reason'): string | null {
    const control = this.adjustForm.get(controlName);
    if (control == null || !control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }
    if (control.hasError('min')) {
      return 'Informe um valor maior que zero.';
    }
    return null;
  }

  async submitAdjust(): Promise<void> {
    const invoice = this.selectedInvoice();
    if (invoice == null || this.submitting() || !canAdjustInvoice(invoice.status)) {
      return;
    }

    const raw = this.adjustForm.getRawValue();
    if (raw.reason.trim() === '') {
      this.adjustForm.controls.reason.setErrors({ required: true });
    }
    if (this.adjustForm.invalid) {
      this.adjustForm.markAllAsTouched();
      return;
    }
    if (raw.amount == null || raw.type == null) {
      this.adjustForm.markAllAsTouched();
      return;
    }
    if (raw.type === 'SURCHARGE' && !canCreateInvoiceSurcharge(invoice.remainingAmount)) {
      this.adjustError.set(
        'O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto.',
      );
      return;
    }

    const request: CreateInvoiceAdjustmentRequest = {
      type: raw.type,
      amount: raw.amount,
      reason: raw.reason.trim(),
    };

    this.submitting.set(true);
    this.adjustError.set(null);
    try {
      await firstValueFrom(this.invoicesService.createAdjustment(invoice.id, request));
      await this.refreshAfterAdjustmentMutation(invoice.id);
    } catch (error: unknown) {
      this.adjustError.set(
        isApiError(error) ? error.message : 'Não foi possível registrar o ajuste.',
      );
    } finally {
      this.submitting.set(false);
    }
  }

  openAdjustReverseConfirm(adjustment: InvoiceAdjustment): void {
    const invoice = this.selectedInvoice();
    if (invoice == null || !canReverseInvoiceAdjustment(invoice.status, adjustment.status)) {
      return;
    }
    this.paymentPendingReverse.set(null);
    this.adjustReverseError.set(null);
    this.adjustmentPendingReverse.set(adjustment);
  }

  closeAdjustReverseConfirm(): void {
    this.adjustmentPendingReverse.set(null);
    this.adjustReverseError.set(null);
  }

  async confirmAdjustReverse(): Promise<void> {
    const invoice = this.selectedInvoice();
    const adjustment = this.adjustmentPendingReverse();
    if (invoice == null || adjustment == null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.adjustReverseError.set(null);
    try {
      await firstValueFrom(this.invoicesService.reverseAdjustment(invoice.id, adjustment.id));
      this.adjustmentPendingReverse.set(null);
      await this.refreshAfterAdjustmentMutation(invoice.id);
    } catch (error: unknown) {
      this.adjustReverseError.set(
        isApiError(error) ? error.message : 'Não foi possível reverter o ajuste.',
      );
    } finally {
      this.submitting.set(false);
    }
  }

  private async refreshAfterPaymentMutation(invoiceId: string): Promise<void> {
    try {
      const detail = await firstValueFrom(this.invoicesService.get(invoiceId));
      this.selectedInvoice.set(detail);
      this.resetPayForm(detail);
      this.resetAdjustForm();
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da fatura.');
      return;
    }

    await Promise.all([this.reloadItems(invoiceId), this.reloadPayments(invoiceId)]);
    this.reloadIfCardSelected();
  }

  private async refreshAfterAdjustmentMutation(invoiceId: string): Promise<void> {
    try {
      const detail = await firstValueFrom(this.invoicesService.get(invoiceId));
      this.selectedInvoice.set(detail);
      this.resetPayForm(detail);
      this.resetAdjustForm();
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da fatura.');
      return;
    }

    await Promise.all([this.reloadItems(invoiceId), this.reloadAdjustments(invoiceId)]);
    this.reloadIfCardSelected();
  }

  async submitPay(): Promise<void> {
    const invoice = this.selectedInvoice();
    if (invoice == null || this.submitting() || !canPayInvoice(invoice.status)) {
      return;
    }
    if (this.payForm.invalid) {
      this.payForm.markAllAsTouched();
      return;
    }

    const raw = this.payForm.getRawValue();
    if (raw.amount == null || raw.accountId === '') {
      this.payForm.markAllAsTouched();
      return;
    }

    const request: PayInvoiceRequest = {
      accountId: raw.accountId,
      amount: raw.amount,
      paymentDate: raw.paymentDate,
      ...(raw.notes.trim() !== '' ? { notes: raw.notes.trim() } : {}),
    };

    this.submitting.set(true);
    this.payError.set(null);
    try {
      await firstValueFrom(this.invoicesService.createPayment(invoice.id, request));
      await this.refreshAfterPaymentMutation(invoice.id);
    } catch (error: unknown) {
      this.payError.set(
        isApiError(error) ? error.message : 'Não foi possível registrar o pagamento.',
      );
    } finally {
      this.submitting.set(false);
    }
  }

  openReverseConfirm(payment: InvoicePayment): void {
    const invoice = this.selectedInvoice();
    if (invoice == null || !canReverseInvoicePayment(invoice.status, payment.status)) {
      return;
    }
    this.adjustmentPendingReverse.set(null);
    this.reverseError.set(null);
    this.paymentPendingReverse.set(payment);
  }

  closeReverseConfirm(): void {
    this.paymentPendingReverse.set(null);
    this.reverseError.set(null);
  }

  async confirmReverse(): Promise<void> {
    const invoice = this.selectedInvoice();
    const payment = this.paymentPendingReverse();
    if (invoice == null || payment == null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.reverseError.set(null);
    try {
      await firstValueFrom(this.invoicesService.reversePayment(invoice.id, payment.id));
      this.paymentPendingReverse.set(null);
      await this.refreshAfterPaymentMutation(invoice.id);
    } catch (error: unknown) {
      this.reverseError.set(
        isApiError(error) ? error.message : 'Não foi possível estornar o pagamento.',
      );
    } finally {
      this.submitting.set(false);
    }
  }

  private async reloadItems(invoiceId: string): Promise<void> {
    this.itemsError.set(null);
    this.itemsLoading.set(true);
    try {
      const items = await firstValueFrom(this.invoicesService.listItems(invoiceId));
      this.items.set(items);
    } catch {
      this.items.set([]);
      this.itemsError.set('Não foi possível carregar as parcelas da fatura.');
    } finally {
      this.itemsLoading.set(false);
    }
  }

  private async reloadPayments(invoiceId: string): Promise<void> {
    this.paymentsError.set(null);
    this.paymentsStatus.set('loading');
    this.payments.set([]);
    try {
      const payments = await firstValueFrom(this.invoicesService.listPayments(invoiceId));
      this.payments.set(payments);
      this.paymentsStatus.set('loaded');
    } catch {
      this.payments.set([]);
      this.paymentsError.set('Não foi possível carregar os pagamentos da fatura.');
      this.paymentsStatus.set('error');
    }
  }

  private async reloadAdjustments(invoiceId: string): Promise<void> {
    this.adjustmentsError.set(null);
    this.adjustmentsStatus.set('loading');
    this.adjustments.set([]);
    try {
      const adjustments = await firstValueFrom(this.invoicesService.listAdjustments(invoiceId));
      this.adjustments.set(adjustments);
      this.adjustmentsStatus.set('loaded');
    } catch {
      this.adjustments.set([]);
      this.adjustmentsError.set('Não foi possível carregar os ajustes da fatura.');
      this.adjustmentsStatus.set('error');
    }
  }

  private async reloadAgreements(invoiceId: string): Promise<void> {
    this.agreementsError.set(null);
    this.agreementsStatus.set('loading');
    this.agreements.set([]);
    try {
      const agreements = await firstValueFrom(this.invoicesService.listAgreements(invoiceId));
      this.agreements.set(agreements);
      this.agreementsStatus.set('loaded');
    } catch {
      this.agreements.set([]);
      this.agreementsError.set('Não foi possível carregar os acordos da fatura.');
      this.agreementsStatus.set('error');
    }
  }

  openAgreementForm(mode: 'negotiate' | 'renegotiate'): void {
    const invoice = this.selectedInvoice();
    if (
      invoice == null ||
      this.submitting() ||
      !canCreateInvoiceAgreement(invoice.status, invoice.remainingAmount)
    ) {
      return;
    }
    this.closeInstallmentPay();
    this.paymentPendingReverse.set(null);
    this.adjustmentPendingReverse.set(null);
    this.agreementSuccess.set(null);
    this.resetAgreementForm(invoice);
    this.agreementFormMode.set(mode);
    this.agreementConfirming.set(false);
  }

  closeAgreementForm(): void {
    this.agreementFormMode.set('closed');
    this.agreementConfirming.set(false);
    this.agreementFormError.set(null);
  }

  closeAgreementConfirm(): void {
    this.agreementConfirming.set(false);
  }

  agreementFieldError(
    controlName:
      | 'entryAmount'
      | 'accountId'
      | 'entryPaymentDate'
      | 'installmentCount'
      | 'installmentAmount'
      | 'anticipatedFuturesNetAmount',
  ): string | null {
    const control = this.agreementForm.get(controlName);
    if (control == null || !control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }
    if (control.hasError('min')) {
      return controlName === 'entryAmount' || controlName === 'anticipatedFuturesNetAmount'
        ? 'Informe um valor maior ou igual a zero.'
        : controlName === 'installmentCount'
          ? 'Informe uma quantidade maior ou igual a 1.'
          : 'Informe um valor maior que zero.';
    }
    if (control.hasError('max') || control.hasError('entryEqualsRemaining')) {
      return 'A entrada deve ser menor que o restante da fatura.';
    }
    if (control.hasError('integer')) {
      return 'Informe um número inteiro.';
    }
    if (control.hasError('digits')) {
      return 'O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.';
    }
    return null;
  }

  prepareAgreementConfirm(): void {
    const invoice = this.selectedInvoice();
    if (
      invoice == null ||
      this.submitting() ||
      this.agreementFormMode() === 'closed' ||
      !canCreateInvoiceAgreement(invoice.status, invoice.remainingAmount)
    ) {
      return;
    }
    this.syncAgreementEntryValidators(invoice.remainingAmount);
    if (this.agreementFormMode() === 'renegotiate') {
      this.agreementForm.controls.anticipatedFuturesNetAmount.setValidators([
        Validators.required,
        Validators.min(0),
        moneyDigitsValidator,
      ]);
    } else {
      this.agreementForm.controls.anticipatedFuturesNetAmount.setValidators([
        Validators.min(0),
        moneyDigitsValidator,
      ]);
    }
    this.agreementForm.controls.anticipatedFuturesNetAmount.updateValueAndValidity({
      emitEvent: false,
    });
    if (this.agreementForm.invalid) {
      this.agreementForm.markAllAsTouched();
      return;
    }
    this.agreementFormError.set(null);
    this.agreementConfirming.set(true);
  }

  async confirmAgreement(): Promise<void> {
    const invoice = this.selectedInvoice();
    const mode = this.agreementFormMode();
    if (
      invoice == null ||
      this.submitting() ||
      !this.agreementConfirming() ||
      mode === 'closed' ||
      this.agreementForm.invalid
    ) {
      return;
    }

    const raw = this.agreementForm.getRawValue();
    if (
      raw.entryAmount == null ||
      raw.accountId === '' ||
      raw.installmentCount == null ||
      raw.installmentAmount == null
    ) {
      this.agreementForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.agreementFormError.set(null);
    try {
      if (mode === 'negotiate') {
        const request: CreateInvoiceAgreementRequest = {
          entryAmount: raw.entryAmount,
          accountId: raw.accountId,
          entryPaymentDate: raw.entryPaymentDate,
          installmentCount: raw.installmentCount,
          installmentAmount: raw.installmentAmount,
        };
        await firstValueFrom(this.invoicesService.createAgreement(invoice.id, request));
      } else {
        if (raw.anticipatedFuturesNetAmount == null) {
          this.agreementForm.markAllAsTouched();
          this.submitting.set(false);
          return;
        }
        const request: CreateInvoiceRenegotiationRequest = {
          entryAmount: raw.entryAmount,
          accountId: raw.accountId,
          entryPaymentDate: raw.entryPaymentDate,
          installmentCount: raw.installmentCount,
          installmentAmount: raw.installmentAmount,
          anticipatedFuturesNetAmount: raw.anticipatedFuturesNetAmount,
        };
        await firstValueFrom(this.invoicesService.createRenegotiation(invoice.id, request));
      }
      this.closeAgreementForm();
      this.agreementSuccess.set('Acordo criado com sucesso.');
      await this.refreshAfterAgreementMutation(invoice.id);
    } catch (error: unknown) {
      this.agreementConfirming.set(false);
      this.agreementFormError.set(
        isApiError(error) ? error.message : 'Não foi possível registrar o acordo.',
      );
    } finally {
      this.submitting.set(false);
    }
  }

  openInstallmentPay(agreement: InvoiceAgreement, installment: InvoiceAgreementInstallment): void {
    if (this.submitting() || !canPayAgreementInstallment(installment.remainingAmount)) {
      return;
    }
    this.closeAgreementForm();
    this.paymentPendingReverse.set(null);
    this.adjustmentPendingReverse.set(null);
    this.installmentPayError.set(null);
    this.installmentSettleConfirming.set(false);
    this.pendingInstallmentPay.set({ agreement, installment });
    this.installmentPayForm.reset({
      accountId: this.activeAccounts()[0]?.id ?? '',
      amount: installment.remainingAmount,
      paymentDate: todayIsoDate(),
      settled: false,
      notes: '',
    });
    this.installmentPayForm.controls.amount.setValidators([
      Validators.required,
      Validators.min(0.01),
      Validators.max(installment.remainingAmount),
      moneyDigitsValidator,
    ]);
    this.installmentPayForm.controls.amount.updateValueAndValidity({ emitEvent: false });
  }

  closeInstallmentPay(): void {
    this.pendingInstallmentPay.set(null);
    this.installmentSettleConfirming.set(false);
    this.installmentPayError.set(null);
  }

  closeInstallmentSettleConfirm(): void {
    this.installmentSettleConfirming.set(false);
  }

  installmentPayFieldError(controlName: 'accountId' | 'amount' | 'paymentDate'): string | null {
    const control = this.installmentPayForm.get(controlName);
    if (control == null || !control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }
    if (control.hasError('min')) {
      return 'Informe um valor maior que zero.';
    }
    if (control.hasError('max')) {
      return 'O valor não pode ser maior que o restante da parcela.';
    }
    if (control.hasError('digits')) {
      return 'O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.';
    }
    return null;
  }

  async prepareInstallmentPay(): Promise<void> {
    const pending = this.pendingInstallmentPay();
    if (pending == null || this.submitting()) {
      return;
    }
    if (this.installmentPayForm.invalid) {
      this.installmentPayForm.markAllAsTouched();
      return;
    }
    this.installmentPayError.set(null);
    if (this.installmentPayForm.controls.settled.value) {
      this.installmentSettleConfirming.set(true);
      return;
    }
    await this.submitInstallmentPay();
  }

  async confirmInstallmentSettle(): Promise<void> {
    if (!this.installmentSettleConfirming()) {
      return;
    }
    await this.submitInstallmentPay();
  }

  private async submitInstallmentPay(): Promise<void> {
    const pending = this.pendingInstallmentPay();
    const invoice = this.selectedInvoice();
    if (pending == null || invoice == null || this.submitting()) {
      return;
    }
    const raw = this.installmentPayForm.getRawValue();
    if (raw.amount == null || raw.accountId === '') {
      this.installmentPayForm.markAllAsTouched();
      return;
    }

    const request: AnticipateAgreementInstallmentRequest = {
      accountId: raw.accountId,
      amount: raw.amount,
      paymentDate: raw.paymentDate,
      settled: raw.settled,
      ...(raw.notes.trim() !== '' ? { notes: raw.notes.trim() } : {}),
    };

    this.submitting.set(true);
    this.installmentPayError.set(null);
    try {
      await firstValueFrom(
        this.invoicesService.anticipateInstallment(
          pending.agreement.id,
          pending.installment.id,
          request,
        ),
      );
      this.closeInstallmentPay();
      await this.refreshAfterInstallmentPay(invoice.id, pending.agreement.id);
    } catch (error: unknown) {
      this.installmentSettleConfirming.set(false);
      this.installmentPayError.set(
        isApiError(error) ? error.message : 'Não foi possível registrar o pagamento da parcela.',
      );
    } finally {
      this.submitting.set(false);
    }
  }

  private async refreshAfterAgreementMutation(invoiceId: string): Promise<void> {
    try {
      const detail = await firstValueFrom(this.invoicesService.get(invoiceId));
      this.selectedInvoice.set(detail);
      this.invoices.update((items) => items.map((item) => (item.id === invoiceId ? detail : item)));
      this.resetPayForm(detail);
      this.resetAdjustForm();
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da fatura.');
      return;
    }

    await Promise.all([
      this.reloadItems(invoiceId),
      this.reloadPayments(invoiceId),
      this.reloadAdjustments(invoiceId),
      this.reloadAgreements(invoiceId),
    ]);
  }

  private async refreshAfterInstallmentPay(invoiceId: string, agreementId: string): Promise<void> {
    try {
      const [detail] = await Promise.all([
        firstValueFrom(this.invoicesService.get(invoiceId)),
        firstValueFrom(this.invoicesService.getAgreement(agreementId)),
      ]);
      this.selectedInvoice.set(detail);
      this.invoices.update((items) => items.map((item) => (item.id === invoiceId ? detail : item)));
      this.resetPayForm(detail);
      this.resetAdjustForm();
    } catch {
      this.detailError.set('Não foi possível carregar os detalhes da fatura.');
    }

    await Promise.all([
      this.reloadItems(invoiceId),
      this.reloadPayments(invoiceId),
      this.reloadAdjustments(invoiceId),
      this.reloadAgreements(invoiceId),
    ]);
  }

  private resetAgreementForm(invoice: Invoice): void {
    this.agreementForm.reset({
      entryAmount: 0,
      accountId: this.activeAccounts()[0]?.id ?? '',
      entryPaymentDate: todayIsoDate(),
      installmentCount: null,
      installmentAmount: null,
      anticipatedFuturesNetAmount: 0,
    });
    this.syncAgreementEntryValidators(invoice.remainingAmount);
    this.agreementFormError.set(null);
  }

  private syncAgreementEntryValidators(remainingAmount: number): void {
    this.agreementForm.controls.entryAmount.setValidators([
      Validators.required,
      Validators.min(0),
      Validators.max(remainingAmount),
      entryNotEqualRemainingValidator(remainingAmount),
      moneyDigitsValidator,
    ]);
    this.agreementForm.controls.entryAmount.updateValueAndValidity({ emitEvent: false });
  }

  private resetPayForm(invoice: Invoice): void {
    this.payForm.reset({
      accountId: this.activeAccounts()[0]?.id ?? '',
      amount: invoice.remainingAmount,
      paymentDate: todayIsoDate(),
      notes: '',
    });
    this.payForm.controls.amount.setValidators([
      Validators.required,
      Validators.min(0.01),
      Validators.max(invoice.remainingAmount),
    ]);
    this.payForm.controls.amount.updateValueAndValidity({ emitEvent: false });
  }

  private resetAdjustForm(): void {
    this.adjustForm.reset({
      type: 'DISCOUNT',
      amount: null,
      reason: '',
    });
    this.adjustError.set(null);
  }

  private reloadIfCardSelected(): void {
    if (this.selectedCardId() === '') {
      return;
    }
    this.reloadInvoices.next();
  }

  private listParams(): InvoiceListParams {
    const yearRaw = this.yearFilter();
    const year = yearRaw === '' ? undefined : Number(yearRaw);
    const monthRaw = this.monthFilter();
    const month = monthRaw === '' ? undefined : Number(monthRaw);
    const status = this.statusFilter();

    return {
      ...(year != null && Number.isInteger(year) ? { year } : {}),
      ...(month != null && Number.isInteger(month) ? { month } : {}),
      ...(status !== '' ? { status } : {}),
    };
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

function integerValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value == null || value === '') {
    return null;
  }
  return typeof value === 'number' && Number.isInteger(value) ? null : { integer: true };
}

function entryNotEqualRemainingValidator(remainingAmount: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value == null || typeof value !== 'number') {
      return null;
    }
    return value === remainingAmount ? { entryEqualsRemaining: true } : null;
  };
}
