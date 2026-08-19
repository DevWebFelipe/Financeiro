import { TestBed } from '@angular/core/testing';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { todayIsoDate } from '../expenses/today-iso-date';
import {
  CreateGoalContributionResult,
  CreateGoalRedemptionResult,
  FinancialGoal,
  FinancialGoalPage,
  GoalContribution,
  GoalRedemption,
} from './financial-goals.models';
import { FinancialGoalsPage } from './financial-goals-page';
import { FinancialGoalsService } from './financial-goals.service';

const GOAL_ID = '01900000-0000-7000-8000-000000000050';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const CONTRIBUTION_ID = '01900000-0000-7000-8000-000000000051';
const REDEMPTION_ID = '01900000-0000-7000-8000-000000000052';

function goal(overrides: Partial<FinancialGoal> = {}): FinancialGoal {
  return {
    id: GOAL_ID,
    accountId: ACCOUNT_ID,
    name: 'Viagem Chile',
    description: 'Férias de julho',
    targetAmount: 400,
    targetDate: '2026-12-20',
    status: 'ACTIVE',
    currentAmount: 100,
    progressPercent: 12.5,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function page(
  items: FinancialGoal[] = [goal()],
  totalPages = items.length > 0 ? 1 : 0,
): FinancialGoalPage {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages,
  };
}

function contribution(overrides: Partial<GoalContribution> = {}): GoalContribution {
  return {
    id: CONTRIBUTION_ID,
    goalId: GOAL_ID,
    amount: 100,
    contributionDate: '2026-08-17',
    notes: 'Primeiro aporte',
    createdAt: '2026-08-17T12:00:00Z',
    ...overrides,
  };
}

function redemption(overrides: Partial<GoalRedemption> = {}): GoalRedemption {
  return {
    id: REDEMPTION_ID,
    goalId: GOAL_ID,
    amount: 20,
    redemptionDate: '2026-08-18',
    notes: null,
    createdAt: '2026-08-18T12:00:00Z',
    ...overrides,
  };
}

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
  path: '/api/v1/financial-goals',
};

describe('FinancialGoalsPage', () => {
  let list: ReturnType<typeof vi.fn>;
  let get: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let update: ReturnType<typeof vi.fn>;
  let contribute: ReturnType<typeof vi.fn>;
  let listContributions: ReturnType<typeof vi.fn>;
  let redeem: ReturnType<typeof vi.fn>;
  let listRedemptions: ReturnType<typeof vi.fn>;
  let complete: ReturnType<typeof vi.fn>;
  let cancel: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    list = vi.fn();
    get = vi.fn();
    create = vi.fn();
    update = vi.fn();
    contribute = vi.fn();
    listContributions = vi.fn();
    redeem = vi.fn();
    listRedemptions = vi.fn();
    complete = vi.fn();
    cancel = vi.fn();

    await TestBed.configureTestingModule({
      imports: [FinancialGoalsPage],
      providers: [
        {
          provide: FinancialGoalsService,
          useValue: {
            list,
            get,
            create,
            update,
            contribute,
            listContributions,
            redeem,
            listRedemptions,
            complete,
            cancel,
          },
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
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando metas.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });

  it('renders official goal amounts and progress after load', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Metas');
    expect(text).toContain('Viagem Chile');
    expect(text).toContain('Ativa');
    expect(text).toContain('12,50%');
    expect(text).not.toContain('25,00%');
  });

  it('shows an empty state with a real create action', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma meta cadastrada.');
    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nova meta');
  });

  it('shows an error state and retries the goals load', async () => {
    list.mockReturnValueOnce(throwError(() => loadError)).mockReturnValueOnce(of(page()));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as metas.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Viagem Chile');
  });

  it('reloads with official server-side status filter and page 0', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onStatusFilterChange('ACTIVE');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'ACTIVE', page: 0, size: 20 }),
    );
  });

  it('paginates with official page and size 20', async () => {
    list.mockReturnValue(of(page([goal()], 3)));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.goToPage(1);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 20 }));
  });

  it('requires name and targetAmount before submitting a new goal', async () => {
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    await fixture.componentInstance.submitGoalForm();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.componentInstance.goalFieldError('name')).toBe('Campo obrigatório.');
    expect(fixture.componentInstance.goalFieldError('targetAmount')).toBe('Campo obrigatório.');
  });

  it('creates a goal through the form omitting blank optional fields', async () => {
    list.mockReturnValue(of(page([])));
    create.mockReturnValue(of(goal()));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.goalForm.patchValue({
      accountId: ACCOUNT_ID,
      name: '  Reserva  ',
      description: '   ',
      targetAmount: 1200,
      targetDate: '',
    });
    await fixture.componentInstance.submitGoalForm();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(create).toHaveBeenCalledWith({
      accountId: ACCOUNT_ID,
      name: 'Reserva',
      targetAmount: 1200,
    });
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
        path: '/api/v1/financial-goals',
        fields: { name: 'O nome é obrigatório.' },
      })),
    );
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.goalForm.patchValue({
      accountId: ACCOUNT_ID,
      name: 'X',
      targetAmount: 10,
    });
    await fixture.componentInstance.submitGoalForm();
    fixture.detectChanges();

    expect(fixture.componentInstance.goalFieldError('name')).toBe('O nome é obrigatório.');
  });

  it('prevents duplicate create submits while a request is in flight', async () => {
    const pending = new Subject<FinancialGoal>();
    create.mockReturnValue(pending.asObservable());
    list.mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.goalForm.patchValue({
      accountId: ACCOUNT_ID,
      name: 'Reserva',
      targetAmount: 100,
    });

    const first = fixture.componentInstance.submitGoalForm();
    const second = fixture.componentInstance.submitGoalForm();
    await second;
    expect(create).toHaveBeenCalledTimes(1);
    pending.next(goal());
    pending.complete();
    await first;
  });

  it('hides edit and contribute for completed goals and keeps redeem', async () => {
    list.mockReturnValue(of(page([goal({ status: 'COMPLETED' })])));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).map((button) => button.textContent?.trim());
    expect(buttons).not.toContain('Editar');
    expect(buttons).not.toContain('Contribuir');
    expect(buttons).not.toContain('Concluir');
    expect(buttons).not.toContain('Cancelar');
    expect(buttons).toContain('Resgatar');
  });

  it('opens cancel confirmation for ACTIVE goals without checking currentAmount locally', async () => {
    list.mockReturnValue(of(page([goal({ currentAmount: 80 })])));
    cancel.mockReturnValue(of(goal({ status: 'CANCELLED', currentAmount: 80 })));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCancelConfirm(goal({ currentAmount: 80 }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Cancelar meta');
    await fixture.componentInstance.confirmCancel();
    expect(cancel).toHaveBeenCalledWith(GOAL_ID);
    expect(list).toHaveBeenCalledTimes(2);
  });

  it('opens complete confirmation and reloads after success', async () => {
    list.mockReturnValue(of(page()));
    complete.mockReturnValue(of(goal({ status: 'COMPLETED' })));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCompleteConfirm(goal());
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Concluir meta');
    await fixture.componentInstance.confirmComplete();
    expect(complete).toHaveBeenCalledWith(GOAL_ID);
    expect(list).toHaveBeenCalledTimes(2);
  });

  it('opens detail with official fields and histories', async () => {
    list.mockReturnValue(of(page()));
    get.mockReturnValue(of(goal()));
    listContributions.mockReturnValue(of([contribution()]));
    listRedemptions.mockReturnValue(of([redemption()]));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openDetail(goal());
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(get).toHaveBeenCalledWith(GOAL_ID);
    expect(listContributions).toHaveBeenCalledWith(GOAL_ID);
    expect(listRedemptions).toHaveBeenCalledWith(GOAL_ID);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Detalhes da meta');
    expect(text).toContain('12,50%');
    expect(text).toContain('Primeiro aporte');
  });

  it('keeps goal detail when history load fails', async () => {
    list.mockReturnValue(of(page()));
    get.mockReturnValue(of(goal({ currentAmount: 100, progressPercent: 12.5, targetAmount: 400 })));
    listContributions.mockReturnValue(throwError(() => loadError));
    listRedemptions.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openDetail(goal());
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Viagem Chile');
    expect(text).toContain('12,50%');
    expect(text).toContain('Não foi possível carregar o histórico da meta.');
    expect(fixture.componentInstance.selectedGoal()?.name).toBe('Viagem Chile');
  });

  it('defaults contribution date to the America/Sao_Paulo civil date', async () => {
    list.mockReturnValue(of(page()));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openContribute(goal());
    fixture.detectChanges();

    expect(fixture.componentInstance.movementForm.controls.date.value).toBe(todayIsoDate());
  });

  it('contributes and reloads the returned goal plus histories', async () => {
    const updated = goal({ currentAmount: 150, progressPercent: 37.5 });
    const result: CreateGoalContributionResult = {
      contribution: contribution({ amount: 50 }),
      goal: updated,
    };
    list.mockReturnValue(of(page()));
    contribute.mockReturnValue(of(result));
    get.mockReturnValue(of(updated));
    listContributions.mockReturnValue(of([contribution({ amount: 50 })]));
    listRedemptions.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openContribute(goal());
    fixture.detectChanges();
    fixture.componentInstance.movementForm.patchValue({
      amount: 50,
      date: '2026-08-17',
      notes: '  ',
    });
    await fixture.componentInstance.submitContribute();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(contribute).toHaveBeenCalledWith(GOAL_ID, {
      amount: 50,
      contributionDate: '2026-08-17',
    });
    expect(fixture.componentInstance.selectedGoal()?.currentAmount).toBe(150);
    expect(listContributions).toHaveBeenCalledWith(GOAL_ID);
    expect(listRedemptions).toHaveBeenCalledWith(GOAL_ID);
    expect(list).toHaveBeenCalledTimes(2);
  });

  it('redeems without computing a local redeemable amount', async () => {
    const updated = goal({ currentAmount: 80, progressPercent: 20 });
    const result: CreateGoalRedemptionResult = {
      redemption: redemption({ amount: 20 }),
      goal: updated,
    };
    list.mockReturnValue(of(page()));
    redeem.mockReturnValue(of(result));
    listContributions.mockReturnValue(of([]));
    listRedemptions.mockReturnValue(of([redemption()]));
    const fixture = TestBed.createComponent(FinancialGoalsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openRedeem(goal());
    fixture.detectChanges();
    fixture.componentInstance.movementForm.patchValue({
      amount: 20,
      date: '2026-08-18',
    });
    await fixture.componentInstance.submitRedeem();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(redeem).toHaveBeenCalledWith(GOAL_ID, {
      amount: 20,
      redemptionDate: '2026-08-18',
    });
    expect(fixture.componentInstance.selectedGoal()?.currentAmount).toBe(80);
  });
});
