import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { Category } from '../categories/categories.models';
import { CategoriesService } from '../categories/categories.service';
import { PayableItem, PayablePage } from './payables.models';
import { PayablesPage } from './payables-page';
import { PayablesService } from './payables.service';

const ITEM_ID = '01900000-0000-7000-8000-000000000030';
const CATEGORY_ID = '01900000-0000-7000-8000-000000000002';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';

function item(overrides: Partial<PayableItem> = {}): PayableItem {
  return {
    id: ITEM_ID,
    type: 'INSTALLMENT',
    expenseId: '01900000-0000-7000-8000-000000000010',
    creditCardId: null,
    categoryId: CATEGORY_ID,
    accountId: ACCOUNT_ID,
    paymentMethod: 'ACCOUNT',
    name: 'Aluguel',
    purchaseDate: '2026-08-01',
    dueDate: '2026-08-10',
    originalAmount: 1500,
    paidAmount: 500,
    remainingAmount: 1000,
    status: 'PARTIALLY_PAID',
    overdue: false,
    responsibleType: 'MINE',
    responsibleName: null,
    ...overrides,
  };
}

function page(items: PayableItem[] = [item()]): PayablePage {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: items.length > 0 ? 1 : 0,
    totalRemaining: items.reduce((sum, row) => sum + row.remainingAmount, 0),
    totalOriginal: items.reduce((sum, row) => sum + row.originalAmount, 0),
    totalPaid: items.reduce((sum, row) => sum + row.paidAmount, 0),
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

const loadError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/payables',
};

describe('PayablesPage', () => {
  let list: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    list = vi.fn();
    await TestBed.configureTestingModule({
      imports: [PayablesPage],
      providers: [
        { provide: PayablesService, useValue: { list } },
        {
          provide: CategoriesService,
          useValue: { list: vi.fn().mockReturnValue(of([category()])) },
        },
        {
          provide: AccountsService,
          useValue: { list: vi.fn().mockReturnValue(of([account()])) },
        },
      ],
    }).compileComponents();
  });

  it('shows a loading state without placeholder amounts', () => {
    list.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando contas a pagar.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
    expect(text).not.toContain('R$ 0,00');
  });

  it('renders official payable data after load', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Contas a pagar');
    expect(text).toContain('Aluguel');
    expect(text).toContain('Parcela');
    expect(text).toContain('Parcialmente paga');
  });

  it('shows empty state without a create action', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma conta a pagar em aberto.');
    expect(fixture.nativeElement.textContent).not.toContain('Nova');
  });

  it('shows an error state and retries the payables load', async () => {
    list.mockReturnValueOnce(throwError(() => loadError)).mockReturnValueOnce(of(page()));
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as contas a pagar.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Aluguel');
  });

  it('reloads with official overdue and status filters', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onStatusFilterChange('OPEN');
    fixture.componentInstance.onOverdueFilterChange('true');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'OPEN', overdue: true, page: 0 }),
    );
  });

  it('sends year and month together from the month filter', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onYearMonthFilterChange('2026-10');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith(
      expect.objectContaining({ year: 2026, month: 10, page: 0 }),
    );
  });

  it('opens a detail panel from list data without extra requests', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openDetail(item());
    fixture.detectChanges();

    expect(list).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Detalhes da obrigação');
    expect(fixture.nativeElement.textContent).toContain('Parcela de despesa');
  });

  it('shows official overdue text without inferring from dates', async () => {
    list.mockReturnValue(of(page([item({ overdue: true, dueDate: '2026-08-01' })])));
    const fixture = TestBed.createComponent(PayablesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Vencida');
  });
});
