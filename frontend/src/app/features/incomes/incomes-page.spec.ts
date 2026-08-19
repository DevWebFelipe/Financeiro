import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { Category } from '../categories/categories.models';
import { CategoriesService } from '../categories/categories.service';
import { Income, IncomeMovement, IncomeMovementPage, IncomePage } from './incomes.models';
import { IncomesPage } from './incomes-page';
import { IncomesService } from './incomes.service';

const INCOME_ID = '01900000-0000-7000-8000-000000000020';
const CATEGORY_ID = '01900000-0000-7000-8000-000000000002';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const MOVEMENT_ID = '01900000-0000-7000-8000-000000000021';

function income(overrides: Partial<Income> = {}): Income {
  return {
    id: INCOME_ID,
    categoryId: CATEGORY_ID,
    accountId: null,
    description: 'Salário',
    amount: 5400,
    expectedDate: '2026-08-05',
    receivedDate: null,
    status: 'EXPECTED',
    responsibleType: null,
    responsibleName: null,
    notes: null,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function page(items: Income[] = [income()]): IncomePage {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: items.length > 0 ? 1 : 0,
  };
}

function movement(overrides: Partial<IncomeMovement> = {}): IncomeMovement {
  return {
    id: MOVEMENT_ID,
    incomeId: INCOME_ID,
    type: 'RECEIPT',
    status: 'ACTIVE',
    amount: 5400,
    movementDate: '2026-08-05',
    accountId: ACCOUNT_ID,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    reversedAt: null,
    ...overrides,
  };
}

function movementPage(items: IncomeMovement[] = [movement()]): IncomeMovementPage {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: 1,
  };
}

const category = (): Category => ({
  id: CATEGORY_ID,
  name: 'Salário',
  type: 'INCOME',
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
  path: '/api/v1/incomes',
};

describe('IncomesPage', () => {
  let list: ReturnType<typeof vi.fn>;
  let get: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let update: ReturnType<typeof vi.fn>;
  let cancel: ReturnType<typeof vi.fn>;
  let createReceipt: ReturnType<typeof vi.fn>;
  let listMovements: ReturnType<typeof vi.fn>;
  let reverseMovement: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    list = vi.fn();
    get = vi.fn();
    create = vi.fn();
    update = vi.fn();
    cancel = vi.fn();
    createReceipt = vi.fn();
    listMovements = vi.fn();
    reverseMovement = vi.fn();

    await TestBed.configureTestingModule({
      imports: [IncomesPage],
      providers: [
        {
          provide: IncomesService,
          useValue: {
            list,
            get,
            create,
            update,
            cancel,
            createReceipt,
            listMovements,
            reverseMovement,
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
      ],
    }).compileComponents();
  });

  it('shows a loading state without placeholder rows', () => {
    list.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando receitas.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });

  it('renders official income data after load', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Receitas');
    expect(text).toContain('Salário');
    expect(text).toContain('Esperada');
  });

  it('shows an empty state with a real create action', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma receita cadastrada.');
    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nova receita');
  });

  it('shows an error state and retries the incomes load', async () => {
    list.mockReturnValueOnce(throwError(() => loadError)).mockReturnValueOnce(of(page()));
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as receitas.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Salário');
  });

  it('reloads with official server-side filters', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onStatusFilterChange('EXPECTED');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'EXPECTED', page: 0 }));
  });

  it('creates an income through the form', async () => {
    list.mockReturnValue(of(page([])));
    create.mockReturnValue(of(income()));
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.incomeForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'Freelance',
      amount: 1200,
      expectedDate: '2026-08-20',
    });
    await fixture.componentInstance.submitIncomeForm();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        description: 'Freelance',
        amount: 1200,
        expectedDate: '2026-08-20',
      }),
    );
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
        path: '/api/v1/incomes',
        fields: { description: 'A descrição é obrigatória.' },
      })),
    );
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.incomeForm.patchValue({
      categoryId: CATEGORY_ID,
      description: 'X',
      amount: 10,
      expectedDate: '2026-08-05',
    });
    await fixture.componentInstance.submitIncomeForm();
    fixture.detectChanges();

    expect(fixture.componentInstance.incomeFieldError('description')).toBe(
      'A descrição é obrigatória.',
    );
  });

  it('shows BUSINESS_RULE_VIOLATION feedback on receive', async () => {
    list.mockReturnValue(of(page()));
    createReceipt.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Operação não permitida.',
        path: '/api/v1/incomes/.../receipts',
      })),
    );
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openReceive(income());
    fixture.detectChanges();
    fixture.componentInstance.receiveForm.patchValue({
      accountId: ACCOUNT_ID,
      amount: 5400,
      date: '2026-08-05',
    });
    await fixture.componentInstance.submitReceive();
    fixture.detectChanges();

    expect(fixture.componentInstance.formError()).toContain('não é permitida');
  });

  it('hides receive and edit actions for cancelled incomes', async () => {
    list.mockReturnValue(of(page([income({ status: 'CANCELLED' })])));
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).map((button) => button.textContent?.trim());
    expect(buttons).not.toContain('Receber');
    expect(buttons).not.toContain('Editar');
    expect(buttons).not.toContain('Cancelar');
  });

  it('opens detail and lists official movements', async () => {
    list.mockReturnValue(of(page()));
    get.mockReturnValue(of(income()));
    listMovements.mockReturnValue(of(movementPage()));
    const fixture = TestBed.createComponent(IncomesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openDetail(income());
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(get).toHaveBeenCalledWith(INCOME_ID);
    expect(listMovements).toHaveBeenCalledWith(INCOME_ID);
    expect(fixture.nativeElement.textContent).toContain('Recebimento');
  });
});
