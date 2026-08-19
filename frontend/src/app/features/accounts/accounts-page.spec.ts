import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { AccountsPage } from './accounts-page';
import { Account, AccountWithBalance } from './accounts.models';
import { AccountsService } from './accounts.service';

const ACCOUNT_ID = '01900000-0000-7000-8000-000000000001';

function account(overrides: Partial<Account> = {}): Account {
  return {
    id: ACCOUNT_ID,
    name: 'Nubank',
    type: 'BANK_ACCOUNT',
    initialBalance: 1500,
    active: true,
    createdAt: '2026-08-13T12:00:00Z',
    updatedAt: '2026-08-13T12:00:00Z',
    ...overrides,
  };
}

function item(
  overrides: {
    account?: Partial<Account>;
    balance?: Partial<AccountWithBalance['balance']>;
  } = {},
): AccountWithBalance {
  const resolved = account(overrides.account);
  return {
    account: resolved,
    balance: {
      accountId: resolved.id,
      totalBalance: 10000,
      reservedAmount: 200,
      availableBalance: 9800,
      ...overrides.balance,
    },
  };
}

const loadError: ApiError = {
  timestamp: '2026-08-18T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/accounts',
};

describe('AccountsPage', () => {
  let listWithBalances: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let update: ReturnType<typeof vi.fn>;
  let deactivate: ReturnType<typeof vi.fn>;
  let activate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    listWithBalances = vi.fn();
    create = vi.fn();
    update = vi.fn();
    deactivate = vi.fn();
    activate = vi.fn();
    await TestBed.configureTestingModule({
      imports: [AccountsPage],
      providers: [
        {
          provide: AccountsService,
          useValue: { listWithBalances, create, update, deactivate, activate },
        },
      ],
    }).compileComponents();
  });

  it('shows a loading state without currency placeholders', () => {
    listWithBalances.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando contas.');
    expect(text).not.toContain('R$');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
  });

  it('renders official account data and balances after load', async () => {
    listWithBalances.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Contas');
    expect(text).toContain('Nubank');
    expect(text).toContain('Conta bancária');
    expect(text).toContain('Ativa');
    expect(text).toMatch(/R\$\s*1\.500,00/);
    expect(text).toMatch(/R\$\s*10\.000,00/);
    expect(text).toMatch(/R\$\s*200,00/);
    expect(text).toMatch(/R\$\s*9\.800,00/);
    expect(text).toContain('Saldo total');
    expect(text).toContain('Saldo reservado');
    expect(text).toContain('Saldo disponível');
    expect(text).not.toContain('Saldo legado');
  });

  it('shows an empty state with a real create action', async () => {
    listWithBalances.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma conta cadastrada.');
    const createButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Nova conta'));
    expect(createButton).toBeTruthy();
    createButton?.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Saldo inicial (opcional)');
  });

  it('shows an error state and retries the accounts load', async () => {
    listWithBalances
      .mockReturnValueOnce(throwError(() => loadError))
      .mockReturnValueOnce(of([item()]));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as contas.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listWithBalances).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Nubank');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('labels inactive accounts and offers reactivation', async () => {
    listWithBalances.mockReturnValue(
      of([
        item({
          account: { active: false },
          balance: { totalBalance: 0, reservedAmount: 0, availableBalance: 0 },
        }),
      ]),
    );
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Inativa');
    expect(text).toContain('Reativar');
    expect(text).not.toContain('Desativar');
    expect(fixture.nativeElement.querySelector('tr.is-inactive')).not.toBeNull();
  });

  it('hides deactivate when official balances are not zero', async () => {
    listWithBalances.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Editar');
    expect(fixture.nativeElement.textContent).not.toContain('Desativar');
  });

  it('deactivates an eligible account and reloads', async () => {
    const eligible = item({
      balance: { totalBalance: 0, reservedAmount: 0, availableBalance: 0 },
    });
    listWithBalances.mockReturnValueOnce(of([eligible])).mockReturnValueOnce(
      of([
        item({
          account: { active: false },
          balance: { totalBalance: 0, reservedAmount: 0, availableBalance: 0 },
        }),
      ]),
    );
    deactivate.mockReturnValue(of(account({ active: false })));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((candidate) => candidate.textContent?.includes('Desativar'));
    button?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(deactivate).toHaveBeenCalledWith(ACCOUNT_ID);
    expect(listWithBalances).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Inativa');
  });

  it('shows a contextual business error when deactivate is rejected', async () => {
    const eligible = item({
      balance: { totalBalance: 0, reservedAmount: 0, availableBalance: 0 },
    });
    listWithBalances.mockReturnValue(of([eligible]));
    deactivate.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-18T15:00:00Z',
        status: 400,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Não é permitido desativar conta com saldo diferente de zero.',
        path: `/api/v1/accounts/${ACCOUNT_ID}/deactivate`,
      })),
    );
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((candidate) => candidate.textContent?.includes('Desativar'));
    button?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain(
      'Não é possível desativar esta conta enquanto houver saldo total ou valor reservado diferente de zero.',
    );
    expect(text).not.toContain('Não é permitido desativar conta com saldo diferente de zero.');
    expect(text).not.toContain(`/api/v1/accounts/${ACCOUNT_ID}/deactivate`);
  });

  it('requires name and type before submitting a new account', async () => {
    listWithBalances.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Informe o nome.');
    expect(fixture.nativeElement.textContent).toContain('Selecione o tipo.');
  });

  it('creates an account and reloads the list', async () => {
    listWithBalances.mockReturnValueOnce(of([])).mockReturnValueOnce(of([item()]));
    create.mockReturnValue(of(account()));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({
      name: 'Nubank',
      type: 'BANK_ACCOUNT',
      initialBalance: null,
    });
    fixture.detectChanges();
    await fixture.componentInstance.submit();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(create).toHaveBeenCalledWith({ name: 'Nubank', type: 'BANK_ACCOUNT' });
    expect(listWithBalances).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Nubank');
  });

  it('sends optional initialBalance on create when the user provides it', async () => {
    listWithBalances.mockReturnValue(of([]));
    create.mockReturnValue(of(account()));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({
      name: 'Nubank',
      type: 'BANK_ACCOUNT',
      initialBalance: 1500,
    });
    await fixture.componentInstance.submit();

    expect(create).toHaveBeenCalledWith({
      name: 'Nubank',
      type: 'BANK_ACCOUNT',
      initialBalance: 1500,
    });
  });

  it('binds validation field errors to the corresponding controls', async () => {
    listWithBalances.mockReturnValue(of([]));
    create.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-18T15:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Dados inválidos.',
        path: '/api/v1/accounts',
        fields: { name: 'O nome é obrigatório.' },
      })),
    );
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({
      name: 'Nubank',
      type: 'CASH',
      initialBalance: null,
    });
    fixture.detectChanges();
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('O nome é obrigatório.');
    expect(fixture.nativeElement.textContent).not.toContain('/api/v1/accounts');
  });

  it('updates an account with name and type only', async () => {
    listWithBalances
      .mockReturnValueOnce(of([item()]))
      .mockReturnValueOnce(of([item({ account: { name: 'Nubank PJ', type: 'CASH' } })]));
    update.mockReturnValue(of(account({ name: 'Nubank PJ', type: 'CASH' })));
    const fixture = TestBed.createComponent(AccountsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const row = item();
    fixture.componentInstance.openEdit(row);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#account-initial-balance')).toBeNull();

    fixture.componentInstance.form.patchValue({ name: 'Nubank PJ', type: 'CASH' });
    await fixture.componentInstance.submit();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(update).toHaveBeenCalledWith(ACCOUNT_ID, { name: 'Nubank PJ', type: 'CASH' });
  });

  it('does not inject HttpClient in the page', () => {
    listWithBalances.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(AccountsPage);
    expect(fixture.componentInstance).toBeTruthy();
    expect(listWithBalances).toHaveBeenCalledTimes(1);
  });
});
