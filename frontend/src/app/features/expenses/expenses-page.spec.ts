import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { Category } from '../categories/categories.models';
import { CategoriesService } from '../categories/categories.service';
import { CreditCard } from '../credit-cards/credit-cards.models';
import { CreditCardsService } from '../credit-cards/credit-cards.service';
import { Expense, ExpenseInstallment, ExpensePage } from './expenses.models';
import { ExpensesPage } from './expenses-page';
import { ExpensesService } from './expenses.service';
import { todayIsoDate } from './today-iso-date';

const EXPENSE_ID = '01900000-0000-7000-8000-000000000010';
const CATEGORY_ID = '01900000-0000-7000-8000-000000000002';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const INSTALLMENT_ID = '01900000-0000-7000-8000-000000000011';
const CARD_ID = '01900000-0000-7000-8000-000000000040';
const CARD_ID_INACTIVE = '01900000-0000-7000-8000-000000000041';
const CARD_ID_NO_DIGITS = '01900000-0000-7000-8000-000000000042';

function expense(overrides: Partial<Expense> = {}): Expense {
  return {
    id: EXPENSE_ID,
    categoryId: CATEGORY_ID,
    accountId: ACCOUNT_ID,
    creditCardId: null,
    description: 'Mercado',
    totalAmount: 150.5,
    expenseDate: '2026-08-01',
    dueDate: '2026-08-10',
    paymentMethod: 'ACCOUNT',
    status: 'OPEN',
    responsibleType: 'MINE',
    responsibleName: null,
    barcode: null,
    notes: null,
    overdue: false,
    installmentId: INSTALLMENT_ID,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function page(items: Expense[] = [expense()]): ExpensePage {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: items.length > 0 ? 1 : 0,
  };
}

function installment(overrides: Partial<ExpenseInstallment> = {}): ExpenseInstallment {
  return {
    id: INSTALLMENT_ID,
    expenseId: EXPENSE_ID,
    installmentNumber: 1,
    totalInstallments: 1,
    amount: 150.5,
    remainingAmount: 150.5,
    dueDate: '2026-08-10',
    status: 'OPEN',
    overdue: false,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

const category = (): Category => ({
  id: CATEGORY_ID,
  name: 'Alimentação',
  type: 'EXPENSE',
  active: true,
  createdAt: '2026-08-14T12:00:00Z',
  updatedAt: '2026-08-14T12:00:00Z',
});

const account = (): Account => ({
  id: ACCOUNT_ID,
  name: 'Conta corrente',
  type: 'BANK_ACCOUNT',
  active: true,
  initialBalance: 0,
  createdAt: '2026-08-14T12:00:00Z',
  updatedAt: '2026-08-14T12:00:00Z',
});

function creditCard(overrides: Partial<CreditCard> = {}): CreditCard {
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

const loadError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/expenses',
};

describe('ExpensesPage', () => {
  let list: ReturnType<typeof vi.fn>;
  let get: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let update: ReturnType<typeof vi.fn>;
  let cancel: ReturnType<typeof vi.fn>;
  let pay: ReturnType<typeof vi.fn>;
  let payInstallment: ReturnType<typeof vi.fn>;
  let refund: ReturnType<typeof vi.fn>;
  let listInstallments: ReturnType<typeof vi.fn>;
  let listCategories: ReturnType<typeof vi.fn>;
  let listAccounts: ReturnType<typeof vi.fn>;
  let listCreditCards: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    list = vi.fn();
    get = vi.fn();
    create = vi.fn();
    update = vi.fn();
    cancel = vi.fn();
    pay = vi.fn();
    payInstallment = vi.fn();
    refund = vi.fn();
    listInstallments = vi.fn();
    listCategories = vi.fn().mockReturnValue(of([category()]));
    listAccounts = vi.fn().mockReturnValue(of([account()]));
    listCreditCards = vi.fn().mockReturnValue(
      of([
        creditCard(),
        creditCard({
          id: CARD_ID_INACTIVE,
          name: 'Inter',
          holderName: 'Giulia',
          lastFourDigits: '9999',
          active: false,
        }),
        creditCard({
          id: CARD_ID_NO_DIGITS,
          name: 'C6',
          holderName: 'Elisiane',
          lastFourDigits: null,
        }),
      ]),
    );

    await TestBed.configureTestingModule({
      imports: [ExpensesPage],
      providers: [
        {
          provide: ExpensesService,
          useValue: {
            list,
            get,
            create,
            update,
            cancel,
            pay,
            payInstallment,
            refund,
            listInstallments,
          },
        },
        {
          provide: CategoriesService,
          useValue: { list: listCategories },
        },
        {
          provide: AccountsService,
          useValue: { list: listAccounts },
        },
        {
          provide: CreditCardsService,
          useValue: { list: listCreditCards },
        },
      ],
    }).compileComponents();
  });

  it('shows a loading state without placeholder rows', () => {
    list.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando despesas.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });

  it('renders official expense data after load', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Despesas');
    expect(text).toContain('Mercado');
    expect(text).toContain('Alimentação');
    expect(text).toContain('Aberta');
  });

  it('shows an empty state with a real create action', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma despesa cadastrada.');
    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nova despesa');
  });

  it('shows an error state and retries the expenses load', async () => {
    list.mockReturnValueOnce(throwError(() => loadError)).mockReturnValueOnce(of(page()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as despesas.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Mercado');
  });

  it('reloads with official server-side filters', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onStatusFilterChange('OPEN');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'OPEN', page: 0 }));
  });

  it('creates an expense through the form', async () => {
    list.mockReturnValue(of(page([])));
    create.mockReturnValue(of(expense()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'ACCOUNT',
      accountId: ACCOUNT_ID,
      responsibleType: 'MINE',
    });
    await fixture.componentInstance.submitExpenseForm();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        description: 'Farmácia',
        totalAmount: 80,
        paymentMethod: 'ACCOUNT',
        accountId: ACCOUNT_ID,
      }),
    );
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('creditCardId');
    expect(list).toHaveBeenCalledTimes(2);
  });

  it('maps VALIDATION_ERROR fields to form controls', async () => {
    list.mockReturnValue(of(page([])));
    create.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Dados inválidos.',
        path: '/api/v1/expenses',
        fields: { description: 'A descrição é obrigatória.' },
      })),
    );
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'X',
      totalAmount: 10,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-10',
      paymentMethod: 'ACCOUNT',
      accountId: ACCOUNT_ID,
      responsibleType: 'MINE',
    });
    await fixture.componentInstance.submitExpenseForm();
    fixture.detectChanges();

    expect(fixture.componentInstance.expenseFieldError('description')).toBe(
      'A descrição é obrigatória.',
    );
  });

  it('shows BUSINESS_RULE_VIOLATION feedback on pay', async () => {
    list.mockReturnValue(of(page()));
    listInstallments.mockReturnValue(of([installment()]));
    pay.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Operação não permitida.',
        path: '/api/v1/expenses/.../pay',
      })),
    );
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openPay(expense());
    fixture.detectChanges();
    fixture.componentInstance.payForm.patchValue({
      accountId: ACCOUNT_ID,
      amount: 150.5,
      paymentDate: '2026-08-10',
    });
    await fixture.componentInstance.submitPay();
    fixture.detectChanges();

    expect(fixture.componentInstance.formError()).toContain('não é permitida');
  });

  it('defaults paymentDate to the America/Sao_Paulo civil date', async () => {
    list.mockReturnValue(of(page()));
    listInstallments.mockReturnValue(of([installment()]));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openPay(expense());
    fixture.detectChanges();

    expect(fixture.componentInstance.payForm.controls.paymentDate.value).toBe(todayIsoDate());
  });

  it('hides pay action for credit card expenses', async () => {
    list.mockReturnValue(of(page([expense({ paymentMethod: 'CREDIT_CARD' })])));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).map((button) => button.textContent?.trim());
    expect(buttons).not.toContain('Pagar');
  });

  it('hides edit action for OPEN credit card expenses', async () => {
    list.mockReturnValue(of(page([expense({ paymentMethod: 'CREDIT_CARD', status: 'OPEN' })])));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).map((button) => button.textContent?.trim());
    expect(buttons).not.toContain('Editar');
  });

  it('loads expense categories and accounts for the form', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listCategories).toHaveBeenCalledWith({ type: 'EXPENSE', active: true });
    expect(listAccounts).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Alimentação');
    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nova despesa');
    expect(fixture.nativeElement.textContent).toContain('Conta corrente');
  });

  it('shows an error state when categories fail to load', async () => {
    list.mockReturnValue(of(page()));
    listCategories.mockReturnValue(throwError(() => loadError));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as categorias e contas.');
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Nova despesa');
    expect(fixture.componentInstance.status()).toBe('error');
  });

  it('shows an error state when accounts fail to load', async () => {
    list.mockReturnValue(of(page()));
    listAccounts.mockReturnValue(throwError(() => loadError));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as categorias e contas.');
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Nova despesa');
    expect(fixture.componentInstance.status()).toBe('error');
  });

  it('retries catalog and expense loads after a catalog failure', async () => {
    list.mockReturnValue(of(page()));
    listCategories
      .mockReturnValueOnce(throwError(() => loadError))
      .mockReturnValue(of([category()]));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listCategories).toHaveBeenCalledTimes(2);
    expect(listAccounts).toHaveBeenCalledTimes(2);
    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Mercado');
    expect(fixture.nativeElement.textContent).toContain('Alimentação');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('closes the create form on Escape without submitting', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-form-title')).not.toBeNull();

    pressEscape(fixture);

    expect(fixture.nativeElement.querySelector('#expense-form-title')).toBeNull();
    expect(create).not.toHaveBeenCalled();
  });

  it('still closes the create form from the cancel button', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    const cancel = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.trim() === 'Cancelar');
    cancel?.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#expense-form-title')).toBeNull();
    expect(create).not.toHaveBeenCalled();
  });

  it('closes the edit form on Escape without submitting', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openEdit(expense());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-form-title')?.textContent).toContain(
      'Editar despesa',
    );

    pressEscape(fixture);

    expect(fixture.nativeElement.querySelector('#expense-form-title')).toBeNull();
    expect(update).not.toHaveBeenCalled();
  });

  it('closes the pay form on Escape without paying', async () => {
    list.mockReturnValue(of(page()));
    listInstallments.mockReturnValue(of([installment()]));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openPay(expense());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-pay-title')).not.toBeNull();

    pressEscape(fixture);

    expect(fixture.nativeElement.querySelector('#expense-pay-title')).toBeNull();
    expect(pay).not.toHaveBeenCalled();
    expect(payInstallment).not.toHaveBeenCalled();
  });

  it('closes the refund form on Escape without refunding', async () => {
    const paid = expense({ status: 'PAID' });
    list.mockReturnValue(of(page([paid])));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openRefund(paid);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-refund-title')).not.toBeNull();

    pressEscape(fixture);

    expect(fixture.nativeElement.querySelector('#expense-refund-title')).toBeNull();
    expect(refund).not.toHaveBeenCalled();
  });

  it('closes the detail panel on Escape', async () => {
    list.mockReturnValue(of(page()));
    get.mockReturnValue(of(expense()));
    listInstallments.mockReturnValue(of([installment()]));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openDetail(expense());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-detail-title')).not.toBeNull();

    pressEscape(fixture);

    expect(fixture.nativeElement.querySelector('#expense-detail-title')).toBeNull();
  });

  it('closes cancel confirmation on Escape without cancelling', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCancelConfirm(expense());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-cancel-title')).not.toBeNull();

    pressEscape(fixture);

    expect(fixture.nativeElement.querySelector('#expense-cancel-title')).toBeNull();
    expect(cancel).not.toHaveBeenCalled();
  });

  it('hides the card field and omits creditCardId for ACCOUNT and NONE', async () => {
    list.mockReturnValue(of(page([])));
    create.mockReturnValue(of(expense({ paymentMethod: 'NONE', accountId: null })));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'ACCOUNT',
      accountId: ACCOUNT_ID,
      responsibleType: 'MINE',
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-credit-card')).toBeNull();

    fixture.componentInstance.expenseForm.patchValue({ paymentMethod: 'NONE', accountId: '' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-credit-card')).toBeNull();

    await fixture.componentInstance.submitExpenseForm();
    expect(create.mock.calls[0]?.[0]).toEqual(expect.objectContaining({ paymentMethod: 'NONE' }));
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('creditCardId');
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('accountId');
  });

  it('shows active cards for CREDIT_CARD and sends creditCardId', async () => {
    list.mockReturnValue(of(page([])));
    create.mockReturnValue(
      of(expense({ paymentMethod: 'CREDIT_CARD', accountId: null, creditCardId: CARD_ID })),
    );
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'CREDIT_CARD',
      responsibleType: 'MINE',
    });
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('#expense-credit-card') as HTMLSelectElement;
    expect(select).not.toBeNull();
    const options = Array.from(select.querySelectorAll('option')).map(
      (option) => option.textContent ?? '',
    );
    expect(options.join(' ')).toContain('Nubank — Ederson •••• 1234');
    const withoutDigits = options.find((option) => option.includes('C6'));
    expect(withoutDigits).toContain('C6 — Elisiane');
    expect(withoutDigits).not.toContain('••••');
    expect(options.join(' ')).not.toContain('Inter');
    expect(options.join(' ')).not.toContain('9999');

    fixture.componentInstance.expenseForm.patchValue({ creditCardId: CARD_ID });
    await fixture.componentInstance.submitExpenseForm();

    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        paymentMethod: 'CREDIT_CARD',
        creditCardId: CARD_ID,
      }),
    );
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('accountId');
  });

  it('does not submit CREDIT_CARD without a selected card', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'CREDIT_CARD',
      responsibleType: 'MINE',
    });
    await fixture.componentInstance.submitExpenseForm();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.componentInstance.expenseFieldError('creditCardId')).toBe('Campo obrigatório.');
  });

  it('clears creditCardId and validators when switching from CREDIT_CARD to ACCOUNT or NONE', async () => {
    list.mockReturnValue(of(page([])));
    create.mockReturnValue(of(expense()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'CREDIT_CARD',
      creditCardId: CARD_ID,
      responsibleType: 'MINE',
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-credit-card')).not.toBeNull();
    expect(fixture.componentInstance.expenseForm.controls.creditCardId.value).toBe(CARD_ID);

    fixture.componentInstance.expenseForm.patchValue({
      paymentMethod: 'ACCOUNT',
      accountId: ACCOUNT_ID,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-credit-card')).toBeNull();
    expect(fixture.componentInstance.expenseForm.controls.creditCardId.value).toBe('');
    expect(fixture.componentInstance.expenseForm.controls.creditCardId.validator).toBeNull();

    fixture.componentInstance.expenseForm.patchValue({
      paymentMethod: 'CREDIT_CARD',
      creditCardId: CARD_ID,
    });
    fixture.componentInstance.expenseForm.patchValue({ paymentMethod: 'NONE' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-credit-card')).toBeNull();
    expect(fixture.componentInstance.expenseForm.controls.creditCardId.value).toBe('');

    await fixture.componentInstance.submitExpenseForm();
    expect(create.mock.calls[0]?.[0]).toEqual(expect.objectContaining({ paymentMethod: 'NONE' }));
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('creditCardId');
  });

  it('shows an explicit error when credit cards fail to load and still allows ACCOUNT create', async () => {
    list.mockReturnValue(of(page([])));
    listCreditCards.mockReturnValue(throwError(() => loadError));
    create.mockReturnValue(of(expense()));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.status()).toBe('loaded');
    fixture.componentInstance.openCreate();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'CREDIT_CARD',
      responsibleType: 'MINE',
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os cartões.');

    await fixture.componentInstance.submitExpenseForm();
    expect(create).not.toHaveBeenCalled();

    fixture.componentInstance.expenseForm.patchValue({
      paymentMethod: 'ACCOUNT',
      accountId: ACCOUNT_ID,
    });
    await fixture.componentInstance.submitExpenseForm();
    expect(create).toHaveBeenCalledWith(expect.objectContaining({ paymentMethod: 'ACCOUNT' }));
  });

  it('retries the credit-card catalog without failing ACCOUNT/NONE', async () => {
    list.mockReturnValue(of(page([])));
    listCreditCards
      .mockReturnValueOnce(throwError(() => loadError))
      .mockReturnValueOnce(of([creditCard()]));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.expenseForm.patchValue({ paymentMethod: 'CREDIT_CARD' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os cartões.');

    fixture.componentInstance.retryCreditCards();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listCreditCards).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('#expense-credit-card')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Nubank — Ederson •••• 1234');
  });

  it('treats an empty active-card catalog as empty, not as a technical error', async () => {
    list.mockReturnValue(of(page([])));
    listCreditCards.mockReturnValue(
      of([creditCard({ id: CARD_ID_INACTIVE, active: false, name: 'Inter' })]),
    );
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.expenseForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Farmácia',
      totalAmount: 80,
      expenseDate: '2026-08-01',
      dueDate: '2026-08-15',
      paymentMethod: 'CREDIT_CARD',
      responsibleType: 'MINE',
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhum cartão ativo disponível.');
    expect(fixture.nativeElement.querySelector('#expense-credit-card')).toBeNull();
    await fixture.componentInstance.submitExpenseForm();
    expect(create).not.toHaveBeenCalled();
  });

  it('keeps CREDIT_CARD expenses non-editable', async () => {
    const cardExpense = expense({
      paymentMethod: 'CREDIT_CARD',
      status: 'OPEN',
      creditCardId: CARD_ID,
    });
    list.mockReturnValue(of(page([cardExpense])));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.canEditItem(cardExpense)).toBe(false);
    fixture.componentInstance.openEdit(cardExpense);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#expense-form-title')).toBeNull();
  });

  it('shows historical CREDIT_CARD expenses in detail including an inactive card', async () => {
    const historical = expense({
      paymentMethod: 'CREDIT_CARD',
      creditCardId: CARD_ID_INACTIVE,
      accountId: null,
    });
    list.mockReturnValue(of(page([historical])));
    get.mockReturnValue(of(historical));
    listInstallments.mockReturnValue(of([installment()]));
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Mercado');
    expect(fixture.nativeElement.textContent).toContain('Cartão');

    await fixture.componentInstance.openDetail(historical);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Detalhes da despesa');
    expect(fixture.nativeElement.textContent).toContain('Inter — Giulia •••• 9999');
  });
});

function pressEscape(fixture: { detectChanges(): void }): void {
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
  fixture.detectChanges();
}
