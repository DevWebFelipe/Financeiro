import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { ProjectionsPage } from './projections-page';
import { ProjectionsService } from './projections.service';
import {
  ProjectionEvent,
  ProjectionMonth,
  ProjectionQuarter,
  ProjectionResponse,
  ProjectionSummary,
} from './projections.models';

const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const SOURCE_ID = '01900000-0000-7000-8000-000000000050';

const account = (): Account => ({
  id: ACCOUNT_ID,
  name: 'Conta corrente',
  type: 'BANK_ACCOUNT',
  active: true,
  initialBalance: 0,
  createdAt: '2026-08-14T12:00:00Z',
  updatedAt: '2026-08-14T12:00:00Z',
});

function event(overrides: Partial<ProjectionEvent> = {}): ProjectionEvent {
  return {
    date: '2026-08-20',
    type: 'EXPENSE',
    description: 'Aluguel',
    amount: 1500,
    direction: 'OUT',
    sourceId: SOURCE_ID,
    sourceType: 'EXPENSE',
    overdue: false,
    accountAssignment: 'UNASSIGNED',
    ...overrides,
  };
}

function summary(overrides: Partial<ProjectionSummary> = {}): ProjectionSummary {
  return {
    currentBalance: 1000,
    projectedFinalBalance: -3000,
    projectedIncome: 1000,
    projectedExpense: 5000,
    projectedNetCashFlow: -4000,
    minimumProjectedBalance: -3000,
    minimumProjectedBalanceDate: '2026-08-25',
    reservedAmount: 200,
    availableProjectedBalance: -3200,
    ...overrides,
  };
}

function month(overrides: Partial<ProjectionMonth> = {}): ProjectionMonth {
  return {
    period: '2026-08',
    openingBalance: 1000,
    totalIncome: 1000,
    totalExpense: 5000,
    netCashFlow: -4000,
    closingBalance: -3000,
    minimumProjectedBalance: -3000,
    minimumProjectedBalanceDate: '2026-08-25',
    negative: true,
    reservedAmount: 200,
    availableProjectedBalance: -3200,
    ...overrides,
  };
}

function quarter(overrides: Partial<ProjectionQuarter> = {}): ProjectionQuarter {
  return {
    period: '2026-Q4',
    months: ['2026-10', '2026-11', '2026-12'],
    totalIncome: 15,
    totalExpense: 3,
    netCashFlow: 12,
    openingBalance: 0,
    closingBalance: 12,
    ...overrides,
  };
}

function projection(overrides: Partial<ProjectionResponse> = {}): ProjectionResponse {
  const events = overrides.events;
  return {
    startDate: '2026-08-17',
    endDate: '2026-08-31',
    summary: summary(),
    months: [month()],
    quarters: [quarter()],
    events: events ?? {
      items: [event()],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    },
    undatedEvents: [event({ date: null, description: 'Sem vencimento' })],
    ...overrides,
  };
}

const loadError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/projections',
};

describe('ProjectionsPage', () => {
  let get: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    get = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ProjectionsPage],
      providers: [
        { provide: ProjectionsService, useValue: { get } },
        {
          provide: AccountsService,
          useValue: { list: vi.fn().mockReturnValue(of([account()])) },
        },
      ],
    }).compileComponents();
  });

  it('shows a loading state without placeholder amounts', () => {
    get.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando projeções.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(text).not.toContain('R$');
    expect(get).toHaveBeenCalledWith({ page: 0, size: 20 });
  });

  it('renders official summary, months, quarters, events and undated rows', async () => {
    get.mockReturnValue(of(projection()));
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Projeções');
    expect(text).toContain('Resumo');
    expect(text).toMatch(/R\$\s*1\.000,00/);
    expect(text).toContain('Negativo');
    expect(text).toContain('4º trimestre de 2026');
    expect(text).toContain('Aluguel');
    expect(text).toContain('Despesa');
    expect(text).toContain('Saída');
    expect(text).toContain('Sem vencimento');
    expect(text).toContain('Sem data');
    expect(text).not.toContain('2026-13-01');
  });

  it('shows official overdue from the API without inferring it from dates', async () => {
    get.mockReturnValue(
      of(
        projection({
          events: {
            items: [event({ overdue: true, date: '2026-08-01' })],
            page: 0,
            size: 20,
            totalItems: 1,
            totalPages: 1,
          },
          undatedEvents: [],
        }),
      ),
    );
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Vencido');
  });

  it('shows empty state without a create action', async () => {
    get.mockReturnValue(
      of(
        projection({
          months: [],
          quarters: [],
          events: { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 },
          undatedEvents: [],
        }),
      ),
    );
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhum evento projetado no horizonte.');
    expect(fixture.nativeElement.textContent).not.toContain('Nova');
  });

  it('shows an error state and retries the projections load', async () => {
    get.mockReturnValueOnce(throwError(() => loadError)).mockReturnValueOnce(of(projection()));
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as projeções.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(get).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Aluguel');
  });

  it('reloads with optional accountId and default period params only', async () => {
    get.mockReturnValue(of(projection({ undatedEvents: [] })));
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onAccountFilterChange(ACCOUNT_ID);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(get).toHaveBeenLastCalledWith({ page: 0, size: 20, accountId: ACCOUNT_ID });
  });

  it('maps date interval without mixing year, month or months', async () => {
    get.mockReturnValue(of(projection({ undatedEvents: [] })));
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onPeriodModeChange('range');
    fixture.componentInstance.onStartDateFilterChange('2026-08-01');
    fixture.componentInstance.onEndDateFilterChange('2026-08-31');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(get).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      startDate: '2026-08-01',
      endDate: '2026-08-31',
    });
    const last = get.mock.calls.at(-1)?.[0] as Record<string, unknown>;
    expect(last['year']).toBeUndefined();
    expect(last['month']).toBeUndefined();
    expect(last['months']).toBeUndefined();
  });

  it('maps month/year with optional months count', async () => {
    get.mockReturnValue(of(projection({ undatedEvents: [] })));
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onPeriodModeChange('yearMonth');
    fixture.componentInstance.onYearMonthFilterChange('2026-08');
    fixture.componentInstance.onMonthsCountChange('3');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(get).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      year: 2026,
      month: 8,
      months: 3,
    });
    const last = get.mock.calls.at(-1)?.[0] as Record<string, unknown>;
    expect(last['startDate']).toBeUndefined();
    expect(last['endDate']).toBeUndefined();
  });

  it('maps from-today as months only', async () => {
    get.mockReturnValue(of(projection({ undatedEvents: [] })));
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onPeriodModeChange('fromToday');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(get).toHaveBeenLastCalledWith({ page: 0, size: 20, months: 3 });
    const last = get.mock.calls.at(-1)?.[0] as Record<string, unknown>;
    expect(last['startDate']).toBeUndefined();
    expect(last['year']).toBeUndefined();
  });

  it('does not request an invalid date range longer than 12 months', async () => {
    get.mockReturnValue(of(projection({ undatedEvents: [] })));
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    const callsAfterLoad = get.mock.calls.length;

    fixture.componentInstance.onPeriodModeChange('range');
    fixture.componentInstance.onStartDateFilterChange('2026-01-01');
    fixture.componentInstance.onEndDateFilterChange('2027-02-01');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(get.mock.calls.length).toBe(callsAfterLoad);
    expect(fixture.nativeElement.textContent).toContain(
      'O intervalo não pode ultrapassar 12 meses.',
    );
  });

  it('re-requests the same period params when changing the events page', async () => {
    get.mockReturnValue(
      of(
        projection({
          undatedEvents: [],
          events: {
            items: [event()],
            page: 0,
            size: 20,
            totalItems: 40,
            totalPages: 2,
          },
        }),
      ),
    );
    const fixture = TestBed.createComponent(ProjectionsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.onPeriodModeChange('fromToday');
    fixture.componentInstance.onMonthsCountChange('6');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.goToPage(1);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(get).toHaveBeenLastCalledWith({ page: 1, size: 20, months: 6 });
    expect(fixture.nativeElement.textContent).toContain('Página 2 de 2');
  });
});
