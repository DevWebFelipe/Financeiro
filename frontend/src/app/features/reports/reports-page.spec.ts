import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { Category } from '../categories/categories.models';
import { CategoriesService } from '../categories/categories.service';
import { CreditCard } from '../credit-cards/credit-cards.models';
import { CreditCardsService } from '../credit-cards/credit-cards.service';
import { ReportsPage } from './reports-page';
import { ReportsService } from './reports.service';
import { ExpenseReportResponse, IncomeReportResponse } from './reports.models';

const CATEGORY_ID = '01900000-0000-7000-8000-000000000002';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const CARD_ID = '01900000-0000-7000-8000-000000000040';

const period = { startDate: '2026-08-01', endDate: '2026-08-31' };

function expenseReport(overrides: Partial<ExpenseReportResponse> = {}): ExpenseReportResponse {
  return {
    period,
    items: [
      {
        id: '01900000-0000-7000-8000-000000000010',
        description: 'Aluguel',
        expenseDate: '2026-08-01',
        paymentMethod: 'ACCOUNT',
        status: 'PARTIALLY_PAID',
        categoryId: CATEGORY_ID,
        accountId: ACCOUNT_ID,
        creditCardId: null,
        responsibleType: 'MINE',
        responsibleName: null,
        origin: 'PURCHASE',
        periodOriginal: 1500,
        periodDiscount: 0,
        periodSurcharge: 0,
        periodObligation: 1500,
        periodPaid: 500,
        periodRemaining: 1000,
        installments: [],
      },
    ],
    page: 0,
    size: 20,
    totalItems: 1,
    totalPages: 2,
    summary: {
      periodOriginal: 1500,
      periodDiscount: 0,
      periodSurcharge: 0,
      periodObligation: 1500,
      periodPaid: 500,
      periodRemaining: 1000,
    },
    ...overrides,
  };
}

function incomeReport(): IncomeReportResponse {
  return {
    period,
    dateType: 'EXPECTED',
    items: [],
    page: 0,
    size: 20,
    totalItems: 0,
    totalPages: 0,
    summary: { amount: 0, accruedAmount: 0, receivedAmount: 0, remainingAmount: 0 },
  };
}

const category = (): Category => ({
  id: CATEGORY_ID,
  name: 'Moradia',
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

const card = (): CreditCard => ({
  id: CARD_ID,
  name: 'Nubank',
  holderName: 'Ederson',
  lastFourDigits: '1234',
  creditLimit: 5000,
  closingDay: 10,
  dueDay: 20,
  active: true,
  createdAt: '2026-08-14T12:00:00Z',
  updatedAt: '2026-08-14T12:00:00Z',
});

const loadError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/reports/expenses',
};

describe('ReportsPage', () => {
  let listExpenses: ReturnType<typeof vi.fn>;
  let listIncomes: ReturnType<typeof vi.fn>;
  let downloadExpensesPdf: ReturnType<typeof vi.fn>;
  let listResponsibles: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    listExpenses = vi.fn();
    listIncomes = vi.fn();
    downloadExpensesPdf = vi.fn().mockReturnValue(of(undefined));
    listResponsibles = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ReportsPage],
      providers: [
        {
          provide: ReportsService,
          useValue: {
            listExpenses,
            listIncomes,
            listCategories: vi.fn(),
            listResponsibles,
            listCards: vi.fn(),
            listCashFlow: vi.fn(),
            getInvoice: vi.fn(),
            downloadExpensesPdf,
            downloadIncomesPdf: vi.fn(),
            downloadCategoriesPdf: vi.fn(),
            downloadResponsiblesPdf: vi.fn(),
            downloadCardsPdf: vi.fn(),
            downloadCashFlowPdf: vi.fn(),
            downloadInvoicePdf: vi.fn(),
          },
        },
        {
          provide: CategoriesService,
          useValue: { list: vi.fn().mockReturnValue(of([category()])) },
        },
        {
          provide: AccountsService,
          useValue: { list: vi.fn().mockReturnValue(of([account()])) },
        },
        {
          provide: CreditCardsService,
          useValue: { list: vi.fn().mockReturnValue(of([card()])) },
        },
      ],
    }).compileComponents();
  });

  it('does not load a report until Consultar is used', () => {
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    expect(listExpenses).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Relatórios');
  });

  it('shows a loading state without placeholder amounts', () => {
    listExpenses.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    fixture.componentInstance.consult();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando relatório.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(text).not.toContain('R$ 0,00');
  });

  it('renders official expense numbers after consult', async () => {
    listExpenses.mockReturnValue(of(expenseReport()));
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    fixture.componentInstance.consult();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Aluguel');
    expect(text).toContain('R$');
    expect(listExpenses).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20 }));
  });

  it('shows empty state after a successful empty consult', async () => {
    listExpenses.mockReturnValue(of(expenseReport({ items: [], totalItems: 0, totalPages: 0 })));
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    fixture.componentInstance.consult();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'Nenhum resultado para os filtros selecionados.',
    );
  });

  it('shows an error state and retries the report load', async () => {
    listExpenses
      .mockReturnValueOnce(throwError(() => loadError))
      .mockReturnValueOnce(of(expenseReport()));
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    fixture.componentInstance.consult();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar o relatório.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listExpenses).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Aluguel');
  });

  it('switches type-specific filters and calls the matching endpoint', async () => {
    listIncomes.mockReturnValue(of(incomeReport()));
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();

    fixture.componentInstance.onReportTypeChange('incomes');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#filter-date-type')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#filter-expense-status')).toBeNull();

    fixture.componentInstance.consult();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(listExpenses).not.toHaveBeenCalled();
    expect(listIncomes).toHaveBeenCalledWith(
      expect.objectContaining({ dateType: 'EXPECTED', page: 0, size: 20 }),
    );
  });

  it('does not send dateType for responsibles when nature is EXPENSE', async () => {
    listResponsibles.mockReturnValue(
      of({
        period,
        nature: 'EXPENSE',
        dateType: null,
        items: [],
        page: 0,
        size: 20,
        totalItems: 0,
        totalPages: 0,
        summary: {},
      }),
    );
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    fixture.componentInstance.onReportTypeChange('responsibles');
    fixture.componentInstance.onNatureChange('EXPENSE');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#filter-date-type')).toBeNull();

    fixture.componentInstance.consult();
    await fixture.whenStable();
    expect(listResponsibles).toHaveBeenCalledWith(
      expect.objectContaining({ nature: 'EXPENSE', page: 0, size: 20 }),
    );
    expect(listResponsibles.mock.calls[0]?.[0]?.dateType).toBeUndefined();
  });

  it('paginates the current report without changing filters', async () => {
    listExpenses.mockReturnValue(of(expenseReport()));
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    fixture.componentInstance.onExpenseStatusChange('OPEN');
    fixture.componentInstance.consult();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.goToPage(1);
    await fixture.whenStable();
    expect(listExpenses).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'OPEN', page: 1, size: 20 }),
    );
  });

  it('downloads PDF with current filters and without page size from the page call', async () => {
    listExpenses.mockReturnValue(of(expenseReport()));
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    fixture.componentInstance.onExpenseStatusChange('PAID');
    fixture.componentInstance.consult();
    await fixture.whenStable();

    fixture.componentInstance.downloadPdf();
    expect(downloadExpensesPdf).toHaveBeenCalledWith(expect.objectContaining({ status: 'PAID' }));
  });
});
