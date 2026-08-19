import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { CreditCardsService } from './credit-cards.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const CARD_ID = '01900000-0000-7000-8000-000000000040';
const CARD_ID_B = '01900000-0000-7000-8000-000000000041';

function cardBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
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

function limitBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    creditLimit: 5000,
    usedLimit: 1500,
    availableLimit: 3500,
    ...overrides,
  };
}

describe('CreditCardsService', () => {
  let service: CreditCardsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CreditCardsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /credit-cards without holderName when the filter is empty', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(api('/credit-cards'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([cardBody()]);

    const cards = await pending;
    expect(cards).toHaveLength(1);
    expect(cards[0]?.name).toBe('Nubank');
    expect(cards[0]?.holderName).toBe('Ederson');
  });

  it('sends holderName as a server-side query param', async () => {
    const pending = firstValueFrom(service.list({ holderName: 'Ederson' }));
    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === api('/credit-cards') && candidate.params.get('holderName') === 'Ederson',
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual(['holderName']);
    request.flush([cardBody()]);
    await pending;
  });

  it('requests GET /credit-cards/{id}', async () => {
    const pending = firstValueFrom(service.get(CARD_ID));
    const request = httpTesting.expectOne(api(`/credit-cards/${CARD_ID}`));
    expect(request.request.method).toBe('GET');
    request.flush(cardBody());
    await expect(pending).resolves.toMatchObject({ id: CARD_ID, name: 'Nubank' });
  });

  it('requests GET /credit-cards/{id}/limit and keeps a negative availableLimit', async () => {
    const pending = firstValueFrom(service.getLimit(CARD_ID));
    const request = httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/limit`));
    expect(request.request.method).toBe('GET');
    request.flush(limitBody({ usedLimit: 6200, availableLimit: -1200 }));

    const limit = await pending;
    expect(limit.creditLimit).toBe(5000);
    expect(limit.usedLimit).toBe(6200);
    expect(limit.availableLimit).toBe(-1200);
  });

  it('loads official limits in parallel after GET /credit-cards', async () => {
    const pending = firstValueFrom(service.listWithLimits());
    httpTesting
      .expectOne(api('/credit-cards'))
      .flush([cardBody(), cardBody({ id: CARD_ID_B, name: 'Inter', lastFourDigits: null })]);

    const firstLimit = httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/limit`));
    const secondLimit = httpTesting.expectOne(api(`/credit-cards/${CARD_ID_B}/limit`));
    firstLimit.flush(limitBody());
    secondLimit.flush(limitBody({ creditLimit: 2000, usedLimit: 0, availableLimit: 2000 }));

    const items = await pending;
    expect(items).toHaveLength(2);
    expect(items[0]?.card.name).toBe('Nubank');
    expect(items[0]?.limit.availableLimit).toBe(3500);
    expect(items[1]?.card.name).toBe('Inter');
    expect(items[1]?.limit.creditLimit).toBe(2000);
  });

  it('does not request limits when GET /credit-cards returns an empty array', async () => {
    const pending = firstValueFrom(service.listWithLimits());
    httpTesting.expectOne(api('/credit-cards')).flush([]);
    await expect(pending).resolves.toEqual([]);
  });

  it('creates a card with POST /credit-cards and omits empty lastFourDigits', async () => {
    const pending = firstValueFrom(
      service.create({
        name: 'Nubank',
        holderName: 'Ederson',
        creditLimit: 5000,
        closingDay: 10,
        dueDay: 20,
      }),
    );
    const request = httpTesting.expectOne(api('/credit-cards'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      name: 'Nubank',
      holderName: 'Ederson',
      creditLimit: 5000,
      closingDay: 10,
      dueDay: 20,
    });
    expect(request.request.body).not.toHaveProperty('pan');
    expect(request.request.body).not.toHaveProperty('cvc');
    expect(request.request.body).not.toHaveProperty('expirationDate');
    request.flush(cardBody(), { status: 201, statusText: 'Created' });
    await expect(pending).resolves.toMatchObject({ name: 'Nubank', active: true });
  });

  it('updates a card with PUT /credit-cards/{id}', async () => {
    const pending = firstValueFrom(
      service.update(CARD_ID, {
        name: 'Nubank PJ',
        holderName: 'Ederson',
        lastFourDigits: '4321',
        creditLimit: 8000,
        closingDay: 5,
        dueDay: 15,
      }),
    );
    const request = httpTesting.expectOne(api(`/credit-cards/${CARD_ID}`));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      name: 'Nubank PJ',
      holderName: 'Ederson',
      lastFourDigits: '4321',
      creditLimit: 8000,
      closingDay: 5,
      dueDay: 15,
    });
    request.flush(cardBody({ name: 'Nubank PJ', lastFourDigits: '4321' }));
    await expect(pending).resolves.toMatchObject({ name: 'Nubank PJ', lastFourDigits: '4321' });
  });

  it('deactivates with POST /credit-cards/{id}/deactivate', async () => {
    const pending = firstValueFrom(service.deactivate(CARD_ID));
    const request = httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/deactivate`));
    expect(request.request.method).toBe('POST');
    request.flush(cardBody({ active: false }));
    await expect(pending).resolves.toMatchObject({ active: false });
  });

  it('activates with POST /credit-cards/{id}/activate', async () => {
    const pending = firstValueFrom(service.activate(CARD_ID));
    const request = httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/activate`));
    expect(request.request.method).toBe('POST');
    request.flush(cardBody({ active: true }));
    await expect(pending).resolves.toMatchObject({ active: true });
  });

  it('does not call invoice or credit endpoints', async () => {
    const pending = firstValueFrom(service.listWithLimits());
    httpTesting.expectOne(api('/credit-cards')).flush([cardBody()]);
    httpTesting.expectOne(api(`/credit-cards/${CARD_ID}/limit`)).flush(limitBody());
    await pending;
    httpTesting.verify();
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/credit-cards')).flush(
      {
        timestamp: '2026-08-19T15:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: '/api/v1/credit-cards',
      },
      { status: 500, statusText: 'Server Error' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('rejects a list response that does not match the credit-cards contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/credit-cards')).flush({ id: CARD_ID });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
