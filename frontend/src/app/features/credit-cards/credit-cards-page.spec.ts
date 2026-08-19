import { TestBed } from '@angular/core/testing';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { CreditCardsPage } from './credit-cards-page';
import {
  CreditCard,
  CreditCardCredit,
  CreditCardLimit,
  CreditCardWithLimit,
} from './credit-cards.models';
import { CreditCardsService } from './credit-cards.service';

const CARD_ID = '01900000-0000-7000-8000-000000000040';

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

function limit(overrides: Partial<CreditCardLimit> = {}): CreditCardLimit {
  return {
    creditLimit: 5000,
    usedLimit: 1500,
    availableLimit: 3500,
    ...overrides,
  };
}

function item(
  overrides: { card?: Partial<CreditCard>; limit?: Partial<CreditCardLimit> } = {},
): CreditCardWithLimit {
  const resolved = card(overrides.card);
  return {
    card: resolved,
    limit: limit({ creditLimit: resolved.creditLimit, ...overrides.limit }),
  };
}

function credit(overrides: Partial<CreditCardCredit> = {}): CreditCardCredit {
  return {
    id: '01900000-0000-7000-8000-000000000060',
    creditCardId: CARD_ID,
    amount: 100,
    remainingAmount: 40,
    reason: 'Ajuste comercial',
    origin: 'MANUAL',
    expenseId: null,
    createdAt: '2026-08-20T12:00:00Z',
    ...overrides,
  };
}

const loadError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/credit-cards',
};

function buttonByText(root: HTMLElement, label: string): HTMLButtonElement | undefined {
  return Array.from(root.querySelectorAll('button')).find((candidate) =>
    candidate.textContent?.includes(label),
  );
}

function pressEscape(): void {
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
}

describe('CreditCardsPage', () => {
  let listWithLimits: ReturnType<typeof vi.fn>;
  let get: ReturnType<typeof vi.fn>;
  let getLimit: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let update: ReturnType<typeof vi.fn>;
  let activate: ReturnType<typeof vi.fn>;
  let deactivate: ReturnType<typeof vi.fn>;
  let listCredits: ReturnType<typeof vi.fn>;
  let createCredit: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    listWithLimits = vi.fn();
    get = vi.fn();
    getLimit = vi.fn();
    create = vi.fn();
    update = vi.fn();
    activate = vi.fn();
    deactivate = vi.fn();
    listCredits = vi.fn().mockReturnValue(of([]));
    createCredit = vi.fn();

    await TestBed.configureTestingModule({
      imports: [CreditCardsPage],
      providers: [
        {
          provide: CreditCardsService,
          useValue: {
            listWithLimits,
            get,
            getLimit,
            create,
            update,
            activate,
            deactivate,
            listCredits,
            createCredit,
          },
        },
      ],
    }).compileComponents();
  });

  it('shows a loading state without currency placeholders', () => {
    listWithLimits.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando cartões.');
    expect(text).not.toContain('R$');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
  });

  it('renders official card data and limits after load', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Cartões');
    expect(text).toContain('Nubank');
    expect(text).toContain('Ederson');
    expect(text).toContain('•••• 1234');
    expect(text).toContain('Ativo');
    expect(text).toMatch(/R\$\s*5\.000,00/);
    expect(text).toMatch(/R\$\s*1\.500,00/);
    expect(text).toMatch(/R\$\s*3\.500,00/);
    expect(fixture.nativeElement.querySelector('#credit-card-pan')).toBeNull();
    expect(fixture.nativeElement.querySelector('#credit-card-cvc')).toBeNull();
    expect(text).not.toContain('Validade');
  });

  it('presents a negative availableLimit exactly as returned by the API', async () => {
    listWithLimits.mockReturnValue(
      of([item({ limit: { usedLimit: 6200, availableLimit: -1200 } })]),
    );
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const formatted = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(-1200);
    expect(fixture.nativeElement.textContent).toContain(formatted);
    expect(fixture.nativeElement.textContent).not.toContain('R$ 3.800,00');
  });

  it('omits last four digits when the API does not provide them', async () => {
    listWithLimits.mockReturnValue(of([item({ card: { lastFourDigits: null } })]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('••••');
  });

  it('shows an empty state with a real create action', async () => {
    listWithLimits.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhum cartão cadastrado.');
    const createButton = buttonByText(fixture.nativeElement, 'Novo cartão');
    expect(createButton).toBeTruthy();
    createButton?.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Novo cartão');
    expect(fixture.nativeElement.querySelector('#credit-card-name')).not.toBeNull();
  });

  it('shows load error and retries', async () => {
    listWithLimits
      .mockReturnValueOnce(throwError(() => loadError))
      .mockReturnValueOnce(of([item()]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os cartões.');
    expect(fixture.nativeElement.textContent).not.toContain('Nenhum cartão cadastrado.');

    buttonByText(fixture.nativeElement, 'Tentar novamente')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listWithLimits).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Nubank');
  });

  it('sends holderName to the service and clears it without client-side filtering', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listWithLimits).toHaveBeenCalledWith({});

    const input = fixture.nativeElement.querySelector('#filter-holder-name') as HTMLInputElement;
    input.value = 'Ederson';
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listWithLimits).toHaveBeenCalledWith({ holderName: 'Ederson' });

    buttonByText(fixture.nativeElement, 'Limpar filtro')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(listWithLimits).toHaveBeenLastCalledWith({});
  });

  it('opens detail, loads official data, and closes on Escape', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Detalhes')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(get).toHaveBeenCalledWith(CARD_ID);
    expect(getLimit).toHaveBeenCalledWith(CARD_ID);
    expect(listCredits).toHaveBeenCalledWith(CARD_ID);
    expect(fixture.nativeElement.textContent).toContain('Detalhes do cartão');
    expect(fixture.nativeElement.textContent).toContain('Dia de fechamento');
    expect(fixture.nativeElement.textContent).toContain('10');

    pressEscape();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#credit-card-detail-title')).toBeNull();
  });

  it('requires cadastral fields before submitting a new card', async () => {
    listWithLimits.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('O nome é obrigatório.');
    expect(fixture.nativeElement.textContent).toContain('O titular é obrigatório.');
    expect(fixture.nativeElement.textContent).toContain('O limite é obrigatório.');
    expect(fixture.nativeElement.textContent).toContain('O dia de fechamento é obrigatório.');
    expect(fixture.nativeElement.textContent).toContain('O dia de vencimento é obrigatório.');
  });

  it('creates a card without lastFourDigits and reloads the list', async () => {
    listWithLimits.mockReturnValueOnce(of([])).mockReturnValueOnce(of([item()]));
    create.mockReturnValue(of(card({ lastFourDigits: null })));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({
      name: 'Nubank',
      holderName: 'Ederson',
      lastFourDigits: '',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });
    fixture.detectChanges();
    await fixture.componentInstance.submit();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(create).toHaveBeenCalledWith({
      name: 'Nubank',
      holderName: 'Ederson',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('lastFourDigits');
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('pan');
    expect(create.mock.calls[0]?.[0]).not.toHaveProperty('cvc');
    expect(listWithLimits).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Nubank');
  });

  it('rejects incomplete lastFourDigits without inventing a PAN', async () => {
    listWithLimits.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({
      name: 'Nubank',
      holderName: 'Ederson',
      lastFourDigits: '12',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Informe exatamente 4 dígitos.');
    expect(fixture.nativeElement.querySelector('input[name="pan"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name="cvc"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('#credit-card-expiry')).toBeNull();
  });

  it('binds validation field errors to the corresponding controls', async () => {
    listWithLimits.mockReturnValue(of([]));
    create.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Dados inválidos.',
        path: '/api/v1/credit-cards',
        fields: { name: 'O nome é obrigatório.' },
      })),
    );
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({
      name: 'Nubank',
      holderName: 'Ederson',
      lastFourDigits: '',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('O nome é obrigatório.');
    expect(fixture.nativeElement.textContent).not.toContain('/api/v1/credit-cards');
  });

  it('shows a generic error when create fails', async () => {
    listWithLimits.mockReturnValue(of([]));
    create.mockReturnValue(throwError(() => loadError));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({
      name: 'Nubank',
      holderName: 'Ederson',
      lastFourDigits: '1234',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível concluir a operação.');
  });

  it('opens edit with existing data and updates the card', async () => {
    listWithLimits
      .mockReturnValueOnce(of([item()]))
      .mockReturnValueOnce(of([item({ card: { name: 'Nubank PJ', lastFourDigits: '4321' } })]));
    update.mockReturnValue(of(card({ name: 'Nubank PJ', lastFourDigits: '4321' })));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openEdit(item());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#credit-card-name')).not.toBeNull();
    expect(fixture.componentInstance.form.getRawValue()).toMatchObject({
      name: 'Nubank',
      holderName: 'Ederson',
      lastFourDigits: '1234',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });

    fixture.componentInstance.form.patchValue({ name: 'Nubank PJ', lastFourDigits: '4321' });
    await fixture.componentInstance.submit();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(update).toHaveBeenCalledWith(CARD_ID, {
      name: 'Nubank PJ',
      holderName: 'Ederson',
      lastFourDigits: '4321',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });
    expect(fixture.nativeElement.textContent).toContain('Nubank PJ');
  });

  it('shows a generic error when update fails', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    update.mockReturnValue(throwError(() => loadError));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openEdit(item());
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível concluir a operação.');
  });

  it('asks for confirmation before deactivating and keeps the card after success', async () => {
    listWithLimits
      .mockReturnValueOnce(of([item()]))
      .mockReturnValueOnce(of([item({ card: { active: false } })]));
    deactivate.mockReturnValue(of(card({ active: false })));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Desativar')?.click();
    fixture.detectChanges();
    expect(deactivate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Deseja desativar o cartão');

    buttonByText(fixture.nativeElement, 'Confirmar desativação')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(deactivate).toHaveBeenCalledWith(CARD_ID);
    expect(fixture.nativeElement.textContent).toContain('Nubank');
    expect(fixture.nativeElement.textContent).toContain('Inativo');
    expect(fixture.nativeElement.textContent).toContain('Ativar');
  });

  it('cancels deactivation confirmation without calling the service', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Desativar')?.click();
    fixture.detectChanges();
    buttonByText(fixture.nativeElement, 'Voltar')?.click();
    fixture.detectChanges();

    expect(deactivate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('#credit-card-deactivate-title')).toBeNull();
  });

  it('closes deactivation confirmation on Escape without deactivating', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Desativar')?.click();
    fixture.detectChanges();
    pressEscape();
    fixture.detectChanges();

    expect(deactivate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('#credit-card-deactivate-title')).toBeNull();
  });

  it('shows an error when deactivation fails', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    deactivate.mockReturnValue(throwError(() => loadError));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openDeactivateConfirm(item());
    fixture.detectChanges();
    await fixture.componentInstance.confirmDeactivate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível concluir a operação.');
  });

  it('asks for confirmation before activating and updates the status', async () => {
    const inactive = item({ card: { active: false } });
    listWithLimits.mockReturnValueOnce(of([inactive])).mockReturnValueOnce(of([item()]));
    activate.mockReturnValue(of(card({ active: true })));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Ativar')?.click();
    fixture.detectChanges();
    expect(activate).not.toHaveBeenCalled();

    buttonByText(fixture.nativeElement, 'Voltar')?.click();
    fixture.detectChanges();
    expect(activate).not.toHaveBeenCalled();

    buttonByText(fixture.nativeElement, 'Ativar')?.click();
    fixture.detectChanges();
    buttonByText(fixture.nativeElement, 'Confirmar ativação')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(activate).toHaveBeenCalledWith(CARD_ID);
    expect(fixture.nativeElement.textContent).toContain('Ativo');
  });

  it('shows an error when activation fails', async () => {
    const inactive = item({ card: { active: false } });
    listWithLimits.mockReturnValue(of([inactive]));
    activate.mockReturnValue(throwError(() => loadError));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.openActivateConfirm(inactive);
    await fixture.componentInstance.confirmActivate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível concluir a operação.');
  });

  it('does not inject HttpClient in the page', () => {
    listWithLimits.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(CreditCardsPage);
    expect(fixture.componentInstance).toBeTruthy();
    expect(listWithLimits).toHaveBeenCalledTimes(1);
  });

  it('loads credits in the card detail and shows an empty state', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.detectChanges();

    expect(listCredits).toHaveBeenCalledWith(CARD_ID);
    expect(fixture.nativeElement.textContent).toContain('Créditos do cartão');
    expect(fixture.nativeElement.textContent).toContain(
      'Nenhum crédito registrado para este cartão.',
    );
    expect(buttonByText(fixture.nativeElement, 'Adicionar crédito')).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('Aplicar crédito');
  });

  it('keeps card detail visible when credits fail and retries only credits', async () => {
    listCredits.mockReturnValueOnce(throwError(() => loadError)).mockReturnValue(of([]));
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Não foi possível carregar os créditos do cartão.',
    );
    expect(fixture.nativeElement.textContent).toContain('Detalhes do cartão');

    buttonByText(fixture.nativeElement, 'Tentar novamente')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(listCredits).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain(
      'Nenhum crédito registrado para este cartão.',
    );
  });

  it('presents remainingAmount = 0 credits as used history without hiding them', async () => {
    listCredits.mockReturnValue(
      of([
        credit(),
        credit({
          id: '01900000-0000-7000-8000-000000000061',
          remainingAmount: 0,
          origin: 'CARD_PURCHASE_REFUND',
          reason: 'Estorno',
        }),
      ]),
    );
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Crédito manual');
    expect(fixture.nativeElement.textContent).toContain('Estorno de compra');
    expect(fixture.nativeElement.textContent).toContain('Disponível');
    expect(fixture.nativeElement.textContent).toContain('Utilizado');
    expect(fixture.nativeElement.textContent).toContain('Ajuste comercial');
    const remaining = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(40);
    expect(fixture.nativeElement.textContent).toContain(remaining);
  });

  it('creates a manual credit without extra fields and reloads credits and limit', async () => {
    createCredit.mockReturnValue(of(credit({ remainingAmount: 100 })));
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.detectChanges();

    fixture.componentInstance.openCreditForm();
    fixture.detectChanges();
    getLimit.mockReturnValue(of(limit({ usedLimit: 1400, availableLimit: 3600 })));
    listCredits.mockReturnValue(of([credit({ remainingAmount: 100 })]));

    fixture.componentInstance.creditForm.patchValue({
      amount: 100,
      reason: '  Ajuste comercial  ',
    });
    await fixture.componentInstance.submitCredit();
    fixture.detectChanges();

    expect(createCredit).toHaveBeenCalledWith(CARD_ID, {
      amount: 100,
      reason: 'Ajuste comercial',
    });
    expect(createCredit.mock.calls[0]?.[0]).not.toBeUndefined();
    expect(createCredit.mock.calls[0]?.[1]).not.toHaveProperty('origin');
    expect(createCredit.mock.calls[0]?.[1]).not.toHaveProperty('expenseId');
    expect(listCredits).toHaveBeenCalledTimes(2);
    expect(getLimit.mock.calls.length).toBeGreaterThanOrEqual(2);
    expect(fixture.nativeElement.textContent).toContain('Crédito adicionado com sucesso.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes do cartão');
    expect(fixture.nativeElement.querySelector('#card-credit-amount')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('aplicados à fatura');
  });

  it('rejects empty, zero and negative credit amounts and whitespace reason', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.componentInstance.openCreditForm();

    const page = fixture.componentInstance;
    page.creditForm.patchValue({ amount: null, reason: 'Motivo' });
    await page.submitCredit();
    page.creditForm.patchValue({ amount: 0 });
    await page.submitCredit();
    page.creditForm.patchValue({ amount: -10 });
    await page.submitCredit();
    page.creditForm.patchValue({ amount: 10, reason: '   ' });
    await page.submitCredit();

    expect(createCredit).not.toHaveBeenCalled();
  });

  it('accepts credit amount at 17 integer digits and 2 fraction digits and rejects 18 integer digits', async () => {
    createCredit.mockReturnValue(of(credit()));
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.componentInstance.openCreditForm();

    const page = fixture.componentInstance;
    page.creditForm.patchValue({ amount: 10.12, reason: 'Motivo' });
    expect(page.creditForm.controls.amount.hasError('digits')).toBe(false);

    page.creditForm.patchValue({ amount: 10_000_000_000_000_000, reason: 'Motivo' });
    expect(page.creditForm.controls.amount.hasError('digits')).toBe(false);
    expect(page.creditForm.valid).toBe(true);
    await page.submitCredit();
    expect(createCredit).toHaveBeenCalledTimes(1);

    page.openCreditForm();
    page.creditForm.patchValue({ amount: 100000000000000000.0, reason: 'Motivo' });
    expect(page.creditForm.controls.amount.hasError('digits')).toBe(true);
    await page.submitCredit();
    expect(createCredit).toHaveBeenCalledTimes(1);
  });

  it('closes the credit form with Escape without posting and keeps the detail open', async () => {
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.componentInstance.openCreditForm();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#card-credit-amount')).not.toBeNull();

    pressEscape();
    fixture.detectChanges();
    expect(createCredit).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('#card-credit-amount')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Detalhes do cartão');
  });

  it('shows credits loading after card detail is ready without hanging on GET', async () => {
    listCredits.mockReturnValue(NEVER);
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    void fixture.componentInstance.openDetail(item());
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Carregando créditos do cartão.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes do cartão');
  });

  it('shows API error on credit create without closing the detail', async () => {
    createCredit.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'O valor deve ser maior que zero.',
        path: `/api/v1/credit-cards/${CARD_ID}/credits`,
      })),
    );
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.componentInstance.openCreditForm();
    fixture.componentInstance.creditForm.patchValue({ amount: 100, reason: 'Motivo' });
    await fixture.componentInstance.submitCredit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('O valor deve ser maior que zero.');
    expect(fixture.nativeElement.textContent).toContain('Detalhes do cartão');
  });

  it('prevents duplicate credit submits while a request is in flight', async () => {
    const pending = new Subject<CreditCardCredit>();
    createCredit.mockReturnValue(pending.asObservable());
    listWithLimits.mockReturnValue(of([item()]));
    get.mockReturnValue(of(card()));
    getLimit.mockReturnValue(of(limit()));
    const fixture = TestBed.createComponent(CreditCardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.componentInstance.openDetail(item());
    fixture.componentInstance.openCreditForm();
    fixture.componentInstance.creditForm.patchValue({ amount: 10, reason: 'Motivo' });

    const first = fixture.componentInstance.submitCredit();
    const second = fixture.componentInstance.submitCredit();
    await second;
    expect(createCredit).toHaveBeenCalledTimes(1);
    pending.next(credit());
    pending.complete();
    await first;
  });
});
