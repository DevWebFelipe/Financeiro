import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { parseDashboardResponse } from './dashboard-parse';
import { DashboardPage } from './dashboard-page';
import { DashboardService } from './dashboard.service';

function validBody(overrides: Record<string, unknown> = {}): unknown {
  return {
    asOfDate: '2026-08-17',
    startDate: '2026-08-17',
    endDate: '2027-07-31',
    balance: {
      totalBalance: 10000,
      reservedAmount: 200,
      availableBalance: 9800,
    },
    projection: {
      summary: {
        currentBalance: 10000,
        projectedFinalBalance: 12000,
        projectedIncome: 4000,
        projectedExpense: 2000,
        projectedNetCashFlow: 2000,
        minimumProjectedBalance: 9800,
        minimumProjectedBalanceDate: '2026-08-17',
        reservedAmount: 200,
        availableProjectedBalance: 11800,
      },
      months: [
        {
          period: '2026-08',
          openingBalance: 10000,
          totalIncome: 400,
          totalExpense: 100,
          netCashFlow: 300,
          closingBalance: 10300,
          minimumProjectedBalance: 10000,
          minimumProjectedBalanceDate: '2026-08-17',
          negative: false,
          reservedAmount: 200,
          availableProjectedBalance: 10100,
        },
      ],
      quarters: [],
    },
    payables: {
      totalRemaining: 0,
      installmentRemaining: 0,
      invoiceRemaining: 0,
      overdueRemaining: 0,
      overdueInstallmentRemaining: 0,
      overdueInvoiceRemaining: 0,
      openCount: 0,
      overdueCount: 0,
    },
    receivables: {
      futureAmount: 0,
      overdueAmount: 0,
      totalReceivableAmount: 0,
      receivedAmount: 0,
    },
    accounts: [
      {
        id: '01900000-0000-7000-8000-000000000001',
        name: 'Nubank',
        type: 'BANK_ACCOUNT',
        totalBalance: 10000,
        reservedAmount: 200,
        availableBalance: 9800,
      },
    ],
    creditCards: [],
    ...overrides,
  };
}

function parsedDashboard(overrides: Record<string, unknown> = {}) {
  const parsed = parseDashboardResponse(validBody(overrides));
  if (parsed == null) {
    throw new Error('test fixture is invalid');
  }
  return parsed;
}

const loadError: ApiError = {
  timestamp: '2026-08-17T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/dashboard',
};

describe('DashboardPage', () => {
  let getDashboard: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    getDashboard = vi.fn();
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [{ provide: DashboardService, useValue: { getDashboard } }],
    }).compileComponents();
  });

  it('shows a loading state without currency placeholders', async () => {
    getDashboard.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando dashboard.');
    expect(text).not.toContain('R$');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
  });

  it('renders official values after load', async () => {
    getDashboard.mockReturnValue(of(parsedDashboard()));
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toMatch(/R\$\s*9\.800,00/);
    expect(text).toMatch(/R\$\s*10\.000,00/);
    expect(text).toContain('Nubank');
    expect(text).toContain('Posição em 17/08/2026');
    expect(text).toContain('Entradas');
    expect(text).toContain('Projeção mensal oficial');
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Dashboard');
  });

  it('shows an empty state when there are no accounts or cards', async () => {
    getDashboard.mockReturnValue(of(parsedDashboard({ accounts: [], creditCards: [] })));
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não há contas ativas neste momento.');
    expect(fixture.nativeElement.textContent).toMatch(/R\$\s*0,00/);
  });

  it('shows an error state and retries the dashboard load', async () => {
    getDashboard
      .mockReturnValueOnce(throwError(() => loadError))
      .mockReturnValueOnce(of(parsedDashboard()));
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar o dashboard.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getDashboard).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Nubank');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('does not inject HttpClient in the page', () => {
    getDashboard.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(DashboardPage);
    expect(fixture.componentInstance).toBeTruthy();
    expect(getDashboard).toHaveBeenCalledTimes(1);
  });
});
