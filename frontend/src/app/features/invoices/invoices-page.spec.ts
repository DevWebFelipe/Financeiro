import { TestBed } from '@angular/core/testing';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { CreditCard } from '../credit-cards/credit-cards.models';
import { CreditCardsService } from '../credit-cards/credit-cards.service';
import { todayIsoDate } from '../expenses/today-iso-date';
import { InvoicesPage } from './invoices-page';
import {
  Invoice,
  InvoiceAdjustment,
  InvoiceAgreement,
  InvoiceItem,
  InvoicePayment,
} from './invoices.models';
import { InvoicesService } from './invoices.service';

const CARD_ID = '01900000-0000-7000-8000-000000000040';
const INACTIVE_CARD_ID = '01900000-0000-7000-8000-000000000041';
const INVOICE_ID = '01900000-0000-7000-8000-000000000050';
const ITEM_ID = '01900000-0000-7000-8000-000000000051';
const EXPENSE_ID = '01900000-0000-7000-8000-000000000010';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const PAYMENT_ID = '01900000-0000-7000-8000-000000000052';
const ADJUSTMENT_ID = '01900000-0000-7000-8000-000000000053';
const AGREEMENT_ID = '01900000-0000-7000-8000-000000000070';
const AGREEMENT_INSTALLMENT_ID = '01900000-0000-7000-8000-000000000071';

function card(overrides: Partial<CreditCard> = {}): CreditCard {
  return {
    id: CARD_ID,
    name: 'Nubank',
    holderName: 'Ederson',
    lastFourDigits: '1234',
    creditLimit: 5000,
    closingDay: 10,
    dueDay: 20,
    active: true,
    createdAt: '2026-08-13T12:00:00Z',
    updatedAt: '2026-08-13T12:00:00Z',
    ...overrides,
  };
}

function invoice(overrides: Partial<Invoice> = {}): Invoice {
  return {
    id: INVOICE_ID,
    creditCardId: CARD_ID,
    referenceYear: 2026,
    referenceMonth: 8,
    closingDate: '2026-08-10',
    dueDate: '2026-08-20',
    status: 'CLOSED',
    totalAmount: 1000,
    paidAmount: 100,
    remainingAmount: 800,
    createdAt: '2026-08-10T12:00:00Z',
    updatedAt: '2026-08-10T12:00:00Z',
    ...overrides,
  };
}

function item(overrides: Partial<InvoiceItem> = {}): InvoiceItem {
  return {
    id: ITEM_ID,
    expenseId: EXPENSE_ID,
    installmentNumber: 2,
    totalInstallments: 3,
    amount: 333.34,
    remainingAmount: 333.34,
    dueDate: '2026-08-20',
    status: 'OPEN',
    overdue: false,
    createdAt: '2026-08-01T12:00:00Z',
    updatedAt: '2026-08-01T12:00:00Z',
    ...overrides,
  };
}

function account(overrides: Partial<Account> = {}): Account {
  return {
    id: ACCOUNT_ID,
    name: 'Conta corrente',
    type: 'BANK_ACCOUNT',
    initialBalance: 0,
    active: true,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function payment(overrides: Partial<InvoicePayment> = {}): InvoicePayment {
  return {
    id: PAYMENT_ID,
    invoiceId: INVOICE_ID,
    accountId: ACCOUNT_ID,
    amount: 500,
    paymentDate: '2026-08-20',
    notes: null,
    status: 'ACTIVE',
    createdAt: '2026-08-20T12:00:00Z',
    ...overrides,
  };
}

function adjustment(overrides: Partial<InvoiceAdjustment> = {}): InvoiceAdjustment {
  return {
    id: ADJUSTMENT_ID,
    invoiceId: INVOICE_ID,
    type: 'DISCOUNT',
    amount: 100,
    reason: 'Correção de cobrança',
    status: 'ACTIVE',
    createdAt: '2026-08-20T12:00:00Z',
    ...overrides,
  };
}

function agreement(overrides: Partial<InvoiceAgreement> = {}): InvoiceAgreement {
  return {
    id: AGREEMENT_ID,
    creditCardId: CARD_ID,
    sourceInvoiceId: INVOICE_ID,
    expenseId: EXPENSE_ID,
    status: 'ACTIVE',
    entryAmount: 0,
    financedAmount: 800,
    installmentCount: 2,
    installmentAmount: 420,
    contractedTotal: 840,
    additionalCost: 40,
    additionalCostPercent: 0.05,
    createdAt: '2026-08-20T12:00:00Z',
    supersededByAgreementId: null,
    installments: [
      {
        id: AGREEMENT_INSTALLMENT_ID,
        expenseId: EXPENSE_ID,
        installmentNumber: 1,
        totalInstallments: 2,
        amount: 420,
        remainingAmount: 420,
        dueDate: '2026-09-20',
        status: 'OPEN',
        invoiceId: INVOICE_ID,
        createdAt: '2026-08-20T12:00:00Z',
        updatedAt: '2026-08-20T12:00:00Z',
      },
    ],
    ...overrides,
  };
}

const cardsError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/credit-cards',
};

const invoicesError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: `/api/v1/credit-cards/${CARD_ID}/invoices`,
};

function buttonByText(root: HTMLElement, label: string): HTMLButtonElement | undefined {
  return Array.from(root.querySelectorAll('button')).find((candidate) =>
    candidate.textContent?.includes(label),
  );
}

function pressEscape(): void {
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
}

function optionValues(select: HTMLSelectElement | null): string[] {
  return Array.from(select?.querySelectorAll('option') ?? []).map((option) => option.value);
}

describe('InvoicesPage', () => {
  let listCards: ReturnType<typeof vi.fn>;
  let listAccounts: ReturnType<typeof vi.fn>;
  let listByCard: ReturnType<typeof vi.fn>;
  let get: ReturnType<typeof vi.fn>;
  let listItems: ReturnType<typeof vi.fn>;
  let listPayments: ReturnType<typeof vi.fn>;
  let createPayment: ReturnType<typeof vi.fn>;
  let reversePayment: ReturnType<typeof vi.fn>;
  let listAdjustments: ReturnType<typeof vi.fn>;
  let createAdjustment: ReturnType<typeof vi.fn>;
  let reverseAdjustment: ReturnType<typeof vi.fn>;
  let listAgreements: ReturnType<typeof vi.fn>;
  let createAgreement: ReturnType<typeof vi.fn>;
  let createRenegotiation: ReturnType<typeof vi.fn>;
  let getAgreement: ReturnType<typeof vi.fn>;
  let anticipateInstallment: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    listCards = vi.fn().mockReturnValue(of([card()]));
    listAccounts = vi.fn().mockReturnValue(of([account()]));
    listByCard = vi.fn();
    get = vi.fn();
    listItems = vi.fn();
    listPayments = vi.fn().mockReturnValue(of([]));
    createPayment = vi.fn();
    reversePayment = vi.fn();
    listAdjustments = vi.fn().mockReturnValue(of([]));
    createAdjustment = vi.fn();
    reverseAdjustment = vi.fn();
    listAgreements = vi.fn().mockReturnValue(of([]));
    createAgreement = vi.fn();
    createRenegotiation = vi.fn();
    getAgreement = vi.fn();
    anticipateInstallment = vi.fn();

    await TestBed.configureTestingModule({
      imports: [InvoicesPage],
      providers: [
        { provide: CreditCardsService, useValue: { list: listCards } },
        { provide: AccountsService, useValue: { list: listAccounts } },
        {
          provide: InvoicesService,
          useValue: {
            listByCard,
            get,
            listItems,
            listPayments,
            createPayment,
            reversePayment,
            listAdjustments,
            createAdjustment,
            reverseAdjustment,
            listAgreements,
            createAgreement,
            createRenegotiation,
            getAgreement,
            anticipateInstallment,
          },
        },
      ],
    }).compileComponents();
  });

  it('loads the page title and waits for a card before querying invoices', async () => {
    listCards.mockReturnValue(of([card()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Faturas');
    expect(fixture.nativeElement.textContent).toContain(
      'Selecione um cartão para consultar as faturas.',
    );
    expect(listCards).toHaveBeenCalledTimes(1);
    expect(listByCard).not.toHaveBeenCalled();
  });

  it('includes inactive cards in the historical filter', async () => {
    listCards.mockReturnValue(
      of([card(), card({ id: INACTIVE_CARD_ID, name: 'Inter', active: false })]),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('#filter-card') as HTMLSelectElement | null;
    expect(optionValues(select)).toEqual(['', CARD_ID, INACTIVE_CARD_ID]);
    expect(fixture.nativeElement.textContent).toContain('(inativo)');
  });

  it('queries invoices for the selected card', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([invoice()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listByCard).toHaveBeenCalledWith(CARD_ID, {});
    expect(fixture.nativeElement.textContent).toContain('Nubank');
    expect(fixture.nativeElement.textContent).toContain('Fechada');
  });

  it('sends year, month and status filters to the card invoices endpoint', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.componentInstance.onYearFilterChange('2026');
    fixture.componentInstance.onMonthFilterChange('8');
    fixture.componentInstance.onStatusFilterChange('OPEN');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(listByCard).toHaveBeenLastCalledWith(CARD_ID, {
      year: 2026,
      month: 8,
      status: 'OPEN',
    });
  });

  it('shows an empty state when the selected card has no invoices', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Nenhuma fatura encontrada para este cartão.',
    );
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('shows an error state when the card catalog fails and retries only cards', async () => {
    listCards.mockReturnValueOnce(throwError(() => cardsError)).mockReturnValueOnce(of([card()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os cartões.');
    expect(listByCard).not.toHaveBeenCalled();

    buttonByText(fixture.nativeElement, 'Tentar novamente')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listCards).toHaveBeenCalledTimes(2);
    expect(listByCard).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Selecione um cartão para consultar as faturas.',
    );
  });

  it('shows an error state when invoices fail and retries only invoices', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard
      .mockReturnValueOnce(throwError(() => invoicesError))
      .mockReturnValueOnce(of([invoice()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar as faturas.');
    expect(listCards).toHaveBeenCalledTimes(1);

    buttonByText(fixture.nativeElement, 'Tentar novamente')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listByCard).toHaveBeenCalledTimes(2);
    expect(listCards).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Fechada');
  });

  it('presents official amounts without recalculating remaining', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([invoice()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const formattedRemaining = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(800);
    const derived = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(900);
    expect(fixture.nativeElement.textContent).toContain(formattedRemaining);
    expect(fixture.nativeElement.textContent).not.toContain(derived);
  });

  it('presents official invoice statuses including SETTLED_BY_AGREEMENT', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([invoice({ status: 'SETTLED_BY_AGREEMENT' })]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Liquidada por acordo');
    expect(fixture.nativeElement.textContent).not.toContain('Parcialmente paga');
  });

  it('opens the detail panel, loads the invoice and its installments', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([invoice()]));
    get.mockReturnValue(of(invoice()));
    listItems.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const details = buttonByText(fixture.nativeElement, 'Detalhes');
    expect(details).toBeTruthy();
    await fixture.componentInstance.openDetail(invoice());
    fixture.detectChanges();

    expect(get).toHaveBeenCalledWith(INVOICE_ID);
    expect(listItems).toHaveBeenCalledWith(INVOICE_ID);
    expect(listPayments).toHaveBeenCalledWith(INVOICE_ID);
    expect(listAdjustments).toHaveBeenCalledWith(INVOICE_ID);
    expect(listAgreements).toHaveBeenCalledWith(INVOICE_ID);
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).toContain('Parcelas desta fatura');
    expect(fixture.nativeElement.textContent).toContain('2/3');
  });

  it('keeps invoice detail visible when items fail', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([invoice()]));
    get.mockReturnValue(of(invoice()));
    listItems.mockReturnValue(throwError(() => invoicesError));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await fixture.componentInstance.openDetail(invoice());
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).toContain(
      'Não foi possível carregar as parcelas da fatura.',
    );
    const formattedRemaining = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(800);
    expect(fixture.nativeElement.textContent).toContain(formattedRemaining);
  });

  it('closes the detail panel with Escape', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(of([invoice()]));
    get.mockReturnValue(of(invoice()));
    listItems.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await fixture.componentInstance.openDetail(invoice());
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');

    pressEscape();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Detalhes da fatura');
  });

  it('does not expose payment on PAID invoices and does not expose credits', async () => {
    listCards.mockReturnValue(of([card()]));
    listByCard.mockReturnValue(
      of([invoice({ status: 'PAID', remainingAmount: 0, paidAmount: 1000 })]),
    );
    get.mockReturnValue(of(invoice({ status: 'PAID', remainingAmount: 0, paidAmount: 1000 })));
    listItems.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await fixture.componentInstance.openDetail(
      invoice({ status: 'PAID', remainingAmount: 0, paidAmount: 1000 }),
    );
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).not.toContain('Registrar pagamento');
    expect(fixture.nativeElement.querySelector('#invoice-pay-amount')).toBeNull();
    expect(text).not.toContain('Estornar');
    expect(fixture.nativeElement.querySelector('#invoice-pay-notes')).toBeNull();
    expect(text).not.toContain('Registrar ajuste');
    expect(fixture.nativeElement.querySelector('#invoice-adjust-amount')).toBeNull();
    expect(text).not.toContain('Registrar crédito');
    expect(text).toContain('Ajustes da fatura');
  });

  it('shows a loading state without currency placeholders while cards load', () => {
    listCards.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(InvoicesPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando cartões.');
    expect(text).not.toContain('R$');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
  });

  async function openInvoice(
    fixture: ReturnType<typeof TestBed.createComponent<InvoicesPage>>,
    value: Invoice = invoice(),
  ): Promise<void> {
    listByCard.mockReturnValue(of([value]));
    get.mockReturnValue(of(value));
    listItems.mockReturnValue(of([item()]));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(value);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('shows the payment form for OPEN and CLOSED invoices', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture, invoice({ status: 'OPEN' }));
    expect(fixture.nativeElement.querySelector('#invoice-pay-amount')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Registrar pagamento');

    fixture.componentInstance.closeDetail();
    await openInvoice(fixture, invoice({ status: 'CLOSED' }));
    expect(fixture.nativeElement.querySelector('#invoice-pay-amount')).not.toBeNull();
  });

  it('hides payment for SCHEDULED and SETTLED_BY_AGREEMENT', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture, invoice({ status: 'SCHEDULED' }));
    expect(fixture.nativeElement.querySelector('#invoice-pay-amount')).toBeNull();

    fixture.componentInstance.closeDetail();
    await openInvoice(fixture, invoice({ status: 'SETTLED_BY_AGREEMENT' }));
    expect(fixture.nativeElement.querySelector('#invoice-pay-amount')).toBeNull();
  });

  it('defaults paymentDate to America/Sao_Paulo and amount to remainingAmount', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    expect(fixture.componentInstance.payForm.controls.paymentDate.value).toBe(todayIsoDate());
    expect(fixture.componentInstance.payForm.controls.amount.value).toBe(800);
  });

  it('rejects zero, negative and above-remaining amounts without calling createPayment', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    const page = fixture.componentInstance;

    page.payForm.patchValue({ amount: 0 });
    await page.submitPay();
    page.payForm.patchValue({ amount: -10 });
    await page.submitPay();
    page.payForm.patchValue({ amount: 801 });
    await page.submitPay();

    expect(createPayment).not.toHaveBeenCalled();
  });

  it('requires accountId and paymentDate', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    const page = fixture.componentInstance;
    page.payForm.patchValue({ accountId: '', paymentDate: '' });
    await page.submitPay();
    expect(createPayment).not.toHaveBeenCalled();
    expect(page.payFieldError('accountId')).toBe('Campo obrigatório.');
    expect(page.payFieldError('paymentDate')).toBe('Campo obrigatório.');
  });

  it('submits a partial payment without notes and reloads official data with the panel open', async () => {
    const afterPay = invoice({ paidAmount: 600, remainingAmount: 300 });
    createPayment.mockReturnValue(of(payment({ amount: 500 })));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    get.mockReturnValue(of(afterPay));
    listItems.mockReturnValue(of([item()]));
    listPayments.mockReturnValue(of([payment({ amount: 500 })]));

    fixture.componentInstance.payForm.patchValue({
      accountId: ACCOUNT_ID,
      amount: 500,
      paymentDate: '2026-08-20',
      notes: '  ',
    });
    await fixture.componentInstance.submitPay();
    fixture.detectChanges();

    expect(createPayment).toHaveBeenCalledWith(INVOICE_ID, {
      accountId: ACCOUNT_ID,
      amount: 500,
      paymentDate: '2026-08-20',
    });
    expect(createPayment.mock.calls[0]?.[1]).not.toHaveProperty('notes');
    expect(get).toHaveBeenCalledTimes(2);
    expect(listPayments).toHaveBeenCalledTimes(2);
    expect(listItems).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    const remaining = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(300);
    expect(fixture.nativeElement.textContent).toContain(remaining);
  });

  it('submits the remaining amount as a full payment', async () => {
    createPayment.mockReturnValue(of(payment({ amount: 800 })));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    get.mockReturnValue(of(invoice({ paidAmount: 900, remainingAmount: 0, status: 'CLOSED' })));
    await fixture.componentInstance.submitPay();
    expect(createPayment).toHaveBeenCalledWith(
      INVOICE_ID,
      expect.objectContaining({ amount: 800, accountId: ACCOUNT_ID }),
    );
  });

  it('shows payment history, empty history and retry after error', async () => {
    listPayments.mockReturnValueOnce(throwError(() => invoicesError)).mockReturnValue(of([]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    expect(fixture.nativeElement.textContent).toContain(
      'Não foi possível carregar os pagamentos da fatura.',
    );
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');

    buttonByText(fixture.nativeElement, 'Tentar novamente')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(listPayments).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain(
      'Nenhum pagamento registrado nesta fatura.',
    );

    listPayments.mockReturnValue(of([payment()]));
    await fixture.componentInstance.retryPayments();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Conta corrente');
    expect(fixture.nativeElement.textContent).toContain('Ativo');
  });

  it('shows API error when payment fails and does not close the panel', async () => {
    createPayment.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Saldo insuficiente.',
        path: `/api/v1/invoices/${INVOICE_ID}/payments`,
      })),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    await fixture.componentInstance.submitPay();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Saldo insuficiente.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('asks for confirmation before reverse and Escape cancels without calling reverse', async () => {
    listPayments.mockReturnValue(of([payment()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openReverseConfirm(payment());
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Estornar pagamento');

    pressEscape();
    fixture.detectChanges();
    expect(reversePayment).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar estorno');
  });

  it('cancels reverse confirmation without posting', async () => {
    listPayments.mockReturnValue(of([payment()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openReverseConfirm(payment());
    fixture.detectChanges();
    fixture.componentInstance.closeReverseConfirm();
    fixture.detectChanges();
    expect(reversePayment).not.toHaveBeenCalled();
  });

  it('reverses an ACTIVE payment and reloads official data', async () => {
    listPayments.mockReturnValue(of([payment()]));
    reversePayment.mockReturnValue(of(payment({ status: 'REVERSED' })));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    const after = invoice({ paidAmount: 100, remainingAmount: 800 });
    get.mockReturnValue(of(after));
    listPayments.mockReturnValue(of([payment({ status: 'REVERSED' })]));

    fixture.componentInstance.openReverseConfirm(payment());
    await fixture.componentInstance.confirmReverse();
    fixture.detectChanges();

    expect(reversePayment).toHaveBeenCalledWith(INVOICE_ID, PAYMENT_ID);
    expect(get).toHaveBeenCalledTimes(2);
    expect(listPayments).toHaveBeenCalledTimes(2);
    expect(listItems).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('shows reverse error without closing the panel', async () => {
    listPayments.mockReturnValue(of([payment()]));
    reversePayment.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Fatura paga não pode ser alterada.',
        path: `/api/v1/invoices/${INVOICE_ID}/payments/${PAYMENT_ID}/reverse`,
      })),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openReverseConfirm(payment());
    await fixture.componentInstance.confirmReverse();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Fatura paga não pode ser alterada.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('does not reverse a REVERSED payment', async () => {
    listPayments.mockReturnValue(of([payment({ status: 'REVERSED' })]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openReverseConfirm(payment({ status: 'REVERSED' }));
    expect(fixture.componentInstance.paymentPendingReverse()).toBeNull();
  });

  it('does not confirm payment or reverse with Escape', async () => {
    createPayment.mockReturnValue(of(payment()));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    pressEscape();
    fixture.detectChanges();
    expect(createPayment).not.toHaveBeenCalled();
    expect(reversePayment).not.toHaveBeenCalled();
    expect(createAdjustment).not.toHaveBeenCalled();
    expect(reverseAdjustment).not.toHaveBeenCalled();
  });

  it('loads adjustments when opening detail and shows an empty state', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    expect(listAdjustments).toHaveBeenCalledWith(INVOICE_ID);
    expect(fixture.nativeElement.textContent).toContain('Ajustes da fatura');
    expect(fixture.nativeElement.textContent).toContain('Nenhum ajuste registrado.');
    expect(fixture.nativeElement.querySelector('#invoice-adjust-amount')).not.toBeNull();
    expect(fixture.componentInstance.adjustForm.controls.amount.value).toBeNull();
  });

  it('shows adjustment loading separately from invoice detail', async () => {
    listAdjustments.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(InvoicesPage);
    listByCard.mockReturnValue(of([invoice()]));
    get.mockReturnValue(of(invoice()));
    listItems.mockReturnValue(of([item()]));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.onCardFilterChange(CARD_ID);
    fixture.detectChanges();
    await fixture.whenStable();

    void fixture.componentInstance.openDetail(invoice());
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).toContain('Carregando ajustes da fatura.');
  });

  it('keeps invoice detail visible when adjustments fail and retries only adjustments', async () => {
    listAdjustments.mockReturnValueOnce(throwError(() => invoicesError)).mockReturnValue(of([]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    expect(fixture.nativeElement.textContent).toContain(
      'Não foi possível carregar os ajustes da fatura.',
    );
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).toContain('Parcelas desta fatura');
    expect(fixture.nativeElement.textContent).toContain('Pagamentos da fatura');

    buttonByText(fixture.nativeElement, 'Tentar novamente')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(listAdjustments).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Nenhum ajuste registrado.');
  });

  it('creates a DISCOUNT without extra fields and reloads official data', async () => {
    createAdjustment.mockReturnValue(of(adjustment()));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    const after = invoice({ totalAmount: 900, paidAmount: 100, remainingAmount: 700 });
    get.mockReturnValue(of(after));
    listItems.mockReturnValue(of([item({ remainingAmount: 233.34 })]));
    listAdjustments.mockReturnValue(of([adjustment()]));
    const paymentsBefore = listPayments.mock.calls.length;

    fixture.componentInstance.adjustForm.patchValue({
      type: 'DISCOUNT',
      amount: 100,
      reason: 'Correção de cobrança',
    });
    await fixture.componentInstance.submitAdjust();
    fixture.detectChanges();

    expect(createAdjustment).toHaveBeenCalledWith(INVOICE_ID, {
      type: 'DISCOUNT',
      amount: 100,
      reason: 'Correção de cobrança',
    });
    expect(createAdjustment.mock.calls[0]?.[1]).not.toHaveProperty('notes');
    expect(createAdjustment.mock.calls[0]?.[1]).not.toHaveProperty('status');
    expect(get).toHaveBeenCalledTimes(2);
    expect(listAdjustments).toHaveBeenCalledTimes(2);
    expect(listItems).toHaveBeenCalledTimes(2);
    expect(listPayments.mock.calls.length).toBe(paymentsBefore);
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).toContain('Desconto');
    expect(fixture.nativeElement.textContent).toContain('Correção de cobrança');
    const remaining = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(700);
    expect(fixture.nativeElement.textContent).toContain(remaining);
    expect(fixture.componentInstance.adjustForm.controls.amount.value).toBeNull();
    expect(fixture.componentInstance.adjustForm.controls.reason.value).toBe('');
  });

  it('creates a SURCHARGE when remaining is greater than zero', async () => {
    createAdjustment.mockReturnValue(
      of(adjustment({ type: 'SURCHARGE', amount: 25, reason: 'Juros' })),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    get.mockReturnValue(of(invoice({ remainingAmount: 825 })));
    listAdjustments.mockReturnValue(
      of([adjustment({ type: 'SURCHARGE', amount: 25, reason: 'Juros' })]),
    );

    fixture.componentInstance.adjustForm.patchValue({
      type: 'SURCHARGE',
      amount: 25,
      reason: 'Juros',
    });
    await fixture.componentInstance.submitAdjust();

    expect(createAdjustment).toHaveBeenCalledWith(INVOICE_ID, {
      type: 'SURCHARGE',
      amount: 25,
      reason: 'Juros',
    });
  });

  it('rejects empty, zero and negative amounts without calling createAdjustment', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    const page = fixture.componentInstance;
    page.adjustForm.patchValue({ type: 'DISCOUNT', reason: 'Correção' });

    page.adjustForm.patchValue({ amount: null });
    await page.submitAdjust();
    page.adjustForm.patchValue({ amount: 0 });
    await page.submitAdjust();
    page.adjustForm.patchValue({ amount: -10 });
    await page.submitAdjust();

    expect(createAdjustment).not.toHaveBeenCalled();
  });

  it('rejects empty and whitespace reason without calling createAdjustment', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    const page = fixture.componentInstance;
    page.adjustForm.patchValue({ type: 'DISCOUNT', amount: 10, reason: '' });
    await page.submitAdjust();
    expect(page.adjustFieldError('reason')).toBe('Campo obrigatório.');

    page.adjustForm.patchValue({ reason: '   ' });
    await page.submitAdjust();
    expect(createAdjustment).not.toHaveBeenCalled();
  });

  it('hides SURCHARGE and blocks submit when remainingAmount is not greater than zero', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture, invoice({ remainingAmount: 0, paidAmount: 1000, status: 'OPEN' }));
    const typeSelect = fixture.nativeElement.querySelector(
      '#invoice-adjust-type',
    ) as HTMLSelectElement | null;
    expect(optionValues(typeSelect)).toEqual(['DISCOUNT']);

    fixture.componentInstance.adjustForm.patchValue({
      type: 'SURCHARGE',
      amount: 10,
      reason: 'Juros',
    });
    await fixture.componentInstance.submitAdjust();
    fixture.detectChanges();
    expect(createAdjustment).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'O acréscimo só pode ser aplicado quando a fatura possui saldo em aberto.',
    );
  });

  it('shows API error when adjustment creation fails and keeps the panel open', async () => {
    createAdjustment.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'O desconto não pode ultrapassar o restante da fatura.',
        path: `/api/v1/invoices/${INVOICE_ID}/adjustments`,
      })),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.adjustForm.patchValue({
      type: 'DISCOUNT',
      amount: 900,
      reason: 'Correção',
    });
    await fixture.componentInstance.submitAdjust();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'O desconto não pode ultrapassar o restante da fatura.',
    );
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('shows Reverter for ACTIVE adjustments and hides it for REVERSED', async () => {
    listAdjustments.mockReturnValue(
      of([
        adjustment(),
        adjustment({ id: '01900000-0000-7000-8000-000000000054', status: 'REVERSED' }),
      ]),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    expect(fixture.nativeElement.textContent).toContain('Ativo');
    expect(fixture.nativeElement.textContent).toContain('Estornado');
    const reverseButtons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).filter((button) => button.textContent?.includes('Reverter'));
    expect(reverseButtons).toHaveLength(1);
  });

  it('asks for confirmation before reversing an adjustment and Escape cancels without posting', async () => {
    listAdjustments.mockReturnValue(of([adjustment()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAdjustReverseConfirm(adjustment());
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reverter ajuste');

    pressEscape();
    fixture.detectChanges();
    expect(reverseAdjustment).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar reversão');
  });

  it('cancels adjustment reverse confirmation without posting', async () => {
    listAdjustments.mockReturnValue(of([adjustment()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAdjustReverseConfirm(adjustment());
    fixture.detectChanges();
    fixture.componentInstance.closeAdjustReverseConfirm();
    fixture.detectChanges();
    expect(reverseAdjustment).not.toHaveBeenCalled();
  });

  it('does not reverse a REVERSED adjustment', async () => {
    listAdjustments.mockReturnValue(of([adjustment({ status: 'REVERSED' })]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAdjustReverseConfirm(adjustment({ status: 'REVERSED' }));
    expect(fixture.componentInstance.adjustmentPendingReverse()).toBeNull();
  });

  it('reverses an ACTIVE adjustment and reloads official data without reloading payments', async () => {
    listAdjustments.mockReturnValue(of([adjustment()]));
    reverseAdjustment.mockReturnValue(of(adjustment({ status: 'REVERSED' })));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    const paymentsBefore = listPayments.mock.calls.length;
    get.mockReturnValue(of(invoice({ remainingAmount: 900 })));
    listAdjustments.mockReturnValue(of([adjustment({ status: 'REVERSED' })]));
    listItems.mockReturnValue(of([item()]));

    fixture.componentInstance.openAdjustReverseConfirm(adjustment());
    await fixture.componentInstance.confirmAdjustReverse();
    fixture.detectChanges();

    expect(reverseAdjustment).toHaveBeenCalledWith(INVOICE_ID, ADJUSTMENT_ID);
    expect(get).toHaveBeenCalledTimes(2);
    expect(listAdjustments).toHaveBeenCalledTimes(2);
    expect(listItems).toHaveBeenCalledTimes(2);
    expect(listPayments.mock.calls.length).toBe(paymentsBefore);
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.nativeElement.textContent).toContain('Estornado');
  });

  it('shows reverse error without closing the panel or changing the adjustment locally', async () => {
    listAdjustments.mockReturnValue(of([adjustment()]));
    reverseAdjustment.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Fatura paga não pode ser alterada.',
        path: `/api/v1/invoices/${INVOICE_ID}/adjustments/${ADJUSTMENT_ID}/reverse`,
      })),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAdjustReverseConfirm(adjustment());
    await fixture.componentInstance.confirmAdjustReverse();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Fatura paga não pode ser alterada.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
    expect(fixture.componentInstance.adjustments()[0]?.status).toBe('ACTIVE');
  });

  it('does not expose adjustment form on SETTLED_BY_AGREEMENT', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture, invoice({ status: 'SETTLED_BY_AGREEMENT' }));
    expect(fixture.nativeElement.querySelector('#invoice-adjust-amount')).toBeNull();
  });

  it('prevents duplicate adjustment submits while a request is in flight', async () => {
    const pending = new Subject<InvoiceAdjustment>();
    createAdjustment.mockReturnValue(pending.asObservable());
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.adjustForm.patchValue({
      type: 'DISCOUNT',
      amount: 10,
      reason: 'Correção',
    });
    const first = fixture.componentInstance.submitAdjust();
    const second = fixture.componentInstance.submitAdjust();
    await second;
    expect(createAdjustment).toHaveBeenCalledTimes(1);
    pending.next(adjustment());
    pending.complete();
    await first;
  });

  it('loads agreements in the invoice detail and shows empty state', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    expect(listAgreements).toHaveBeenCalledWith(INVOICE_ID);
    expect(fixture.nativeElement.textContent).toContain('Acordos / Renegociação');
    expect(fixture.nativeElement.textContent).toContain('Nenhum acordo registrado nesta fatura.');
    expect(buttonByText(fixture.nativeElement, 'Nova negociação')).toBeTruthy();
    expect(buttonByText(fixture.nativeElement, 'Nova renegociação')).toBeTruthy();
  });

  it('offers negotiation only for CLOSED invoices with remaining greater than zero', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture, invoice({ status: 'OPEN' }));
    expect(buttonByText(fixture.nativeElement, 'Nova negociação')).toBeUndefined();

    fixture.componentInstance.closeDetail();
    await openInvoice(fixture, invoice({ status: 'PAID', remainingAmount: 0 }));
    expect(buttonByText(fixture.nativeElement, 'Nova negociação')).toBeUndefined();

    fixture.componentInstance.closeDetail();
    await openInvoice(fixture, invoice({ status: 'SETTLED_BY_AGREEMENT', remainingAmount: 0 }));
    expect(buttonByText(fixture.nativeElement, 'Nova negociação')).toBeUndefined();
  });

  it('creates a negotiation with the official payload after confirmation', async () => {
    createAgreement.mockReturnValue(of(agreement()));
    listAgreements.mockReturnValueOnce(of([])).mockReturnValue(of([agreement()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAgreementForm('negotiate');
    fixture.componentInstance.agreementForm.patchValue({
      entryAmount: 0,
      accountId: ACCOUNT_ID,
      entryPaymentDate: todayIsoDate(),
      installmentCount: 2,
      installmentAmount: 420,
    });
    fixture.componentInstance.prepareAgreementConfirm();
    expect(createAgreement).not.toHaveBeenCalled();
    await fixture.componentInstance.confirmAgreement();
    fixture.detectChanges();

    expect(createAgreement).toHaveBeenCalledWith(INVOICE_ID, {
      entryAmount: 0,
      accountId: ACCOUNT_ID,
      entryPaymentDate: todayIsoDate(),
      installmentCount: 2,
      installmentAmount: 420,
    });
    expect(createAgreement.mock.calls[0]?.[1]).not.toHaveProperty('financedAmount');
    expect(createAgreement.mock.calls[0]?.[1]).not.toHaveProperty('origin');
    expect(listAgreements).toHaveBeenCalledTimes(2);
    expect(listPayments).toHaveBeenCalled();
    expect(listAdjustments).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Acordo criado com sucesso.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('rejects entry equal to remaining without posting', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAgreementForm('negotiate');
    fixture.componentInstance.agreementForm.patchValue({
      entryAmount: 800,
      accountId: ACCOUNT_ID,
      installmentCount: 1,
      installmentAmount: 10,
    });
    fixture.componentInstance.prepareAgreementConfirm();
    expect(createAgreement).not.toHaveBeenCalled();
  });

  it('creates a renegotiation with anticipatedFuturesNetAmount only', async () => {
    createRenegotiation.mockReturnValue(of(agreement()));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAgreementForm('renegotiate');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#invoice-agreement-futures')).not.toBeNull();
    fixture.componentInstance.agreementForm.patchValue({
      entryAmount: 100,
      accountId: ACCOUNT_ID,
      entryPaymentDate: todayIsoDate(),
      installmentCount: 2,
      installmentAmount: 400,
      anticipatedFuturesNetAmount: 50,
    });
    fixture.componentInstance.prepareAgreementConfirm();
    await fixture.componentInstance.confirmAgreement();

    expect(createRenegotiation).toHaveBeenCalledWith(INVOICE_ID, {
      entryAmount: 100,
      accountId: ACCOUNT_ID,
      entryPaymentDate: todayIsoDate(),
      installmentCount: 2,
      installmentAmount: 400,
      anticipatedFuturesNetAmount: 50,
    });
    expect(createRenegotiation.mock.calls[0]?.[1]).not.toHaveProperty('agreementIds');
  });

  it('pays an agreement installment partially without notes or invented totals', async () => {
    anticipateInstallment.mockReturnValue(of(agreement({ status: 'ACTIVE' })));
    getAgreement.mockReturnValue(of(agreement()));
    listAgreements.mockReturnValue(of([agreement()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openInstallmentPay(agreement(), agreement().installments[0]!);
    fixture.componentInstance.installmentPayForm.patchValue({
      accountId: ACCOUNT_ID,
      amount: 100,
      paymentDate: todayIsoDate(),
      settled: false,
      notes: '  ',
    });
    await fixture.componentInstance.prepareInstallmentPay();

    expect(anticipateInstallment).toHaveBeenCalledWith(AGREEMENT_ID, AGREEMENT_INSTALLMENT_ID, {
      accountId: ACCOUNT_ID,
      amount: 100,
      paymentDate: todayIsoDate(),
      settled: false,
    });
    expect(anticipateInstallment.mock.calls[0]?.[2]).not.toHaveProperty('notes');
    expect(getAgreement).toHaveBeenCalledWith(AGREEMENT_ID);
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('asks confirmation before settling an installment', async () => {
    anticipateInstallment.mockReturnValue(of(agreement({ status: 'COMPLETED' })));
    getAgreement.mockReturnValue(of(agreement({ status: 'COMPLETED' })));
    listAgreements.mockReturnValue(of([agreement()]));
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openInstallmentPay(agreement(), agreement().installments[0]!);
    fixture.componentInstance.installmentPayForm.patchValue({
      accountId: ACCOUNT_ID,
      amount: 100,
      paymentDate: todayIsoDate(),
      settled: true,
    });
    fixture.componentInstance.prepareInstallmentPay();
    expect(anticipateInstallment).not.toHaveBeenCalled();
    await fixture.componentInstance.confirmInstallmentSettle();
    expect(anticipateInstallment).toHaveBeenCalledWith(
      AGREEMENT_ID,
      AGREEMENT_INSTALLMENT_ID,
      expect.objectContaining({ settled: true, amount: 100 }),
    );
  });

  it('closes the agreement form with Escape without posting', async () => {
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAgreementForm('negotiate');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#invoice-agreement-entry')).not.toBeNull();
    pressEscape();
    fixture.detectChanges();
    expect(createAgreement).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('#invoice-agreement-entry')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('shows API error on agreement create and keeps the panel open', async () => {
    createAgreement.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'A fatura já possui negociação.',
        path: `/api/v1/invoices/${INVOICE_ID}/agreements`,
      })),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAgreementForm('negotiate');
    fixture.componentInstance.agreementForm.patchValue({
      entryAmount: 0,
      accountId: ACCOUNT_ID,
      installmentCount: 1,
      installmentAmount: 10,
    });
    fixture.componentInstance.prepareAgreementConfirm();
    await fixture.componentInstance.confirmAgreement();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('A fatura já possui negociação.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes da fatura');
  });

  it('prevents duplicate agreement submits while a request is in flight', async () => {
    const pending = new Subject<InvoiceAgreement>();
    createAgreement.mockReturnValue(pending.asObservable());
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    fixture.componentInstance.openAgreementForm('negotiate');
    fixture.componentInstance.agreementForm.patchValue({
      entryAmount: 0,
      accountId: ACCOUNT_ID,
      installmentCount: 1,
      installmentAmount: 10,
    });
    fixture.componentInstance.prepareAgreementConfirm();
    const first = fixture.componentInstance.confirmAgreement();
    const second = fixture.componentInstance.confirmAgreement();
    await second;
    expect(createAgreement).toHaveBeenCalledTimes(1);
    pending.next(agreement());
    pending.complete();
    await first;
  });

  it('presents CANCELLED and RENEGOTIATED statuses from the API without a cancel action', async () => {
    listAgreements.mockReturnValue(
      of([
        agreement({ status: 'CANCELLED' }),
        agreement({
          id: '01900000-0000-7000-8000-000000000072',
          status: 'RENEGOTIATED',
          supersededByAgreementId: AGREEMENT_ID,
        }),
      ]),
    );
    const fixture = TestBed.createComponent(InvoicesPage);
    await openInvoice(fixture);
    expect(fixture.nativeElement.textContent).toContain('Cancelado');
    expect(fixture.nativeElement.textContent).toContain('Renegociado');
    expect(fixture.nativeElement.textContent).toContain('Renegociado por outro acordo.');
    expect(buttonByText(fixture.nativeElement, 'Cancelar acordo')).toBeUndefined();
  });
});
