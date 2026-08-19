import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { Account } from '../accounts/accounts.models';
import { AccountsService } from '../accounts/accounts.service';
import { todayIsoDate } from '../expenses/today-iso-date';
import { Transfer } from './transfers.models';
import { TransfersPage } from './transfers-page';
import { TransfersService } from './transfers.service';

const TRANSFER_ID = '01900000-0000-7000-8000-000000000070';
const SOURCE_ID = '01900000-0000-7000-8000-000000000003';
const DEST_ID = '01900000-0000-7000-8000-000000000004';

function transfer(overrides: Partial<Transfer> = {}): Transfer {
  return {
    id: TRANSFER_ID,
    sourceAccountId: SOURCE_ID,
    destinationAccountId: DEST_ID,
    amount: 500,
    transferDate: '2026-08-10',
    description: 'Aluguel',
    status: 'ACTIVE',
    createdAt: '2026-08-10T15:00:00Z',
    ...overrides,
  };
}

function account(overrides: Partial<Account> = {}): Account {
  return {
    id: SOURCE_ID,
    name: 'Conta corrente',
    type: 'BANK_ACCOUNT',
    active: true,
    initialBalance: 0,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

const accounts = (): Account[] => [
  account(),
  account({ id: DEST_ID, name: 'Poupança', type: 'CASH', active: false }),
];

const loadError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/transfers',
};

describe('TransfersPage', () => {
  let list: ReturnType<typeof vi.fn>;
  let get: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let reverse: ReturnType<typeof vi.fn>;
  let listAccounts: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    list = vi.fn();
    get = vi.fn();
    create = vi.fn();
    reverse = vi.fn();
    listAccounts = vi.fn().mockReturnValue(of(accounts()));

    await TestBed.configureTestingModule({
      imports: [TransfersPage],
      providers: [
        {
          provide: TransfersService,
          useValue: { list, get, create, reverse },
        },
        {
          provide: AccountsService,
          useValue: { list: listAccounts },
        },
      ],
    }).compileComponents();
  });

  it('shows a loading state without placeholder rows', () => {
    list.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando transferências.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });

  it('renders official transfer data after load', async () => {
    list.mockReturnValue(of([transfer()]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Transferências');
    expect(text).toContain('Conta corrente');
    expect(text).toContain('Poupança');
    expect(text).toContain('Ativa');
  });

  it('shows an empty state with a real create action', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma transferência cadastrada.');
    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nova transferência');
  });

  it('shows an error state and retries the transfers load', async () => {
    list.mockReturnValueOnce(throwError(() => loadError)).mockReturnValueOnce(of([transfer()]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as transferências.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Ativa');
  });

  it('keeps the transfer list when the accounts load fails', async () => {
    listAccounts.mockReturnValue(throwError(() => loadError));
    list.mockReturnValue(of([transfer()]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ativa');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('lists every account returned by AccountsService without local eligibility filters', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();

    const options = Array.from(
      fixture.nativeElement.querySelectorAll(
        '#transfer-source option',
      ) as NodeListOf<HTMLOptionElement>,
    ).map((option) => option.textContent?.trim());
    expect(options).toContain('Conta corrente');
    expect(options).toContain('Poupança');
  });

  it('reloads with official server-side filters', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onStartDateFilterChange('2026-08-01');
    fixture.componentInstance.onEndDateFilterChange('2026-08-31');
    fixture.componentInstance.onAccountFilterChange(SOURCE_ID);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith({
      startDate: '2026-08-01',
      endDate: '2026-08-31',
      accountId: SOURCE_ID,
    });
    expect(list.mock.calls.at(-1)?.[0]).not.toHaveProperty('page');
    expect(list.mock.calls.at(-1)?.[0]).not.toHaveProperty('size');
    expect(list.mock.calls.at(-1)?.[0]).not.toHaveProperty('status');
  });

  it('requires origin, destination, amount and date before submitting', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.transferForm.patchValue({ transferDate: '' });
    fixture.detectChanges();
    await fixture.componentInstance.submitTransferForm();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.componentInstance.fieldError('sourceAccountId')).toBe('Campo obrigatório.');
    expect(fixture.componentInstance.fieldError('destinationAccountId')).toBe('Campo obrigatório.');
    expect(fixture.componentInstance.fieldError('amount')).toBe('Campo obrigatório.');
    expect(fixture.componentInstance.fieldError('transferDate')).toBe('Campo obrigatório.');
  });

  it('rejects origin equal to destination', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.transferForm.patchValue({
      sourceAccountId: SOURCE_ID,
      destinationAccountId: SOURCE_ID,
      amount: 10,
      transferDate: '2026-08-10',
    });
    await fixture.componentInstance.submitTransferForm();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.componentInstance.fieldError('destinationAccountId')).toBe(
      'Origem e destino devem ser contas diferentes.',
    );
  });

  it('rejects amount that exceeds 17 integer digits or 2 decimal places', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.transferForm.patchValue({
      sourceAccountId: SOURCE_ID,
      destinationAccountId: DEST_ID,
      amount: 1.234,
      transferDate: '2026-08-10',
    });
    await fixture.componentInstance.submitTransferForm();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.componentInstance.fieldError('amount')).toBe(
      'O valor deve ter no máximo 17 dígitos inteiros e 2 decimais.',
    );
  });

  it('creates a transfer, omits blank description and reloads the list', async () => {
    list.mockReturnValueOnce(of([])).mockReturnValueOnce(of([transfer({ description: null })]));
    create.mockReturnValue(of(transfer({ description: null })));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.transferForm.patchValue({
      sourceAccountId: SOURCE_ID,
      destinationAccountId: DEST_ID,
      amount: 500,
      transferDate: '2026-08-10',
      description: '   ',
    });
    await fixture.componentInstance.submitTransferForm();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(create).toHaveBeenCalledWith({
      sourceAccountId: SOURCE_ID,
      destinationAccountId: DEST_ID,
      amount: 500,
      transferDate: '2026-08-10',
    });
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('description');
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('userId');
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('status');
    expect(list).toHaveBeenCalledTimes(2);
  });

  it('defaults the transfer date to the America/Sao_Paulo civil date', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();

    expect(fixture.componentInstance.transferForm.controls.transferDate.value).toBe(todayIsoDate());
  });

  it('blocks a second submit while the first is in flight', async () => {
    list.mockReturnValue(of([]));
    create.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.transferForm.patchValue({
      sourceAccountId: SOURCE_ID,
      destinationAccountId: DEST_ID,
      amount: 500,
      transferDate: '2026-08-10',
      description: 'Aluguel',
    });

    void fixture.componentInstance.submitTransferForm();
    expect(fixture.componentInstance.submitting()).toBe(true);
    await fixture.componentInstance.submitTransferForm();

    expect(create).toHaveBeenCalledTimes(1);
  });

  it('hides reverse for REVERSED transfers', async () => {
    list.mockReturnValue(of([transfer({ status: 'REVERSED' })]));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).map((button) => button.textContent?.trim());
    expect(buttons).not.toContain('Estornar');
    expect(fixture.nativeElement.textContent).toContain('Estornada');
  });

  it('opens detail with GET /transfers/{id}', async () => {
    list.mockReturnValue(of([transfer()]));
    get.mockReturnValue(of(transfer({ description: 'Aluguel atualizado' })));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openDetail(transfer());
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(get).toHaveBeenCalledWith(TRANSFER_ID);
    expect(fixture.nativeElement.textContent).toContain('Aluguel atualizado');
  });

  it('confirms reverse and reloads the list', async () => {
    list
      .mockReturnValueOnce(of([transfer()]))
      .mockReturnValueOnce(of([transfer({ status: 'REVERSED' })]));
    reverse.mockReturnValue(of(transfer({ status: 'REVERSED' })));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openReverseConfirm(transfer());
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Estornar transferência');

    await fixture.componentInstance.confirmReverse();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(reverse).toHaveBeenCalledWith(TRANSFER_ID);
    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Estornada');
  });

  it('closes reverse confirm before the form and the detail on Escape', async () => {
    list.mockReturnValue(of([transfer()]));
    get.mockReturnValue(of(transfer()));
    const fixture = TestBed.createComponent(TransfersPage);
    fixture.detectChanges();
    await fixture.whenStable();

    await fixture.componentInstance.openDetail(transfer());
    fixture.componentInstance.formMode.set('create');
    fixture.componentInstance.panelMode.set('reverse-confirm');
    fixture.detectChanges();

    fixture.componentInstance.onEscape();
    fixture.detectChanges();
    expect(fixture.componentInstance.panelMode()).toBe('detail');
    expect(fixture.componentInstance.formMode()).toBe('create');

    fixture.componentInstance.onEscape();
    fixture.detectChanges();
    expect(fixture.componentInstance.formMode()).toBe('closed');
    expect(fixture.componentInstance.panelMode()).toBe('detail');

    fixture.componentInstance.onEscape();
    fixture.detectChanges();
    expect(fixture.componentInstance.panelMode()).toBe('closed');
  });
});
