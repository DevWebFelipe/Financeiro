import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { FinancialGoalsService } from './financial-goals.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const GOAL_ID = '01900000-0000-7000-8000-000000000050';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const CONTRIBUTION_ID = '01900000-0000-7000-8000-000000000051';
const REDEMPTION_ID = '01900000-0000-7000-8000-000000000052';

function goalBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: GOAL_ID,
    accountId: ACCOUNT_ID,
    name: 'Viagem Chile',
    description: 'Férias de julho',
    targetAmount: 5000,
    targetDate: '2026-12-20',
    status: 'ACTIVE',
    currentAmount: 500,
    progressPercent: 10,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function pageBody(items: Record<string, unknown>[] = [goalBody()]): Record<string, unknown> {
  return {
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: 1,
  };
}

function contributionBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: CONTRIBUTION_ID,
    goalId: GOAL_ID,
    amount: 500,
    contributionDate: '2026-08-17',
    notes: 'Primeiro aporte',
    createdAt: '2026-08-17T12:00:00Z',
    ...overrides,
  };
}

function redemptionBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: REDEMPTION_ID,
    goalId: GOAL_ID,
    amount: 200,
    redemptionDate: '2026-08-18',
    notes: null,
    createdAt: '2026-08-18T12:00:00Z',
    ...overrides,
  };
}

describe('FinancialGoalsService', () => {
  let service: FinancialGoalsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FinancialGoalsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /financial-goals with default pagination and no sort', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/financial-goals') &&
        req.params.get('page') === '0' &&
        req.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('sort')).toBeNull();
    request.flush(pageBody());

    const page = await pending;
    expect(page.items).toHaveLength(1);
    expect(page.items[0]?.name).toBe('Viagem Chile');
  });

  it('requests GET /financial-goals with official status filter', async () => {
    const pending = firstValueFrom(service.list({ status: 'ACTIVE', page: 1, size: 20 }));
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/financial-goals') &&
        req.params.get('status') === 'ACTIVE' &&
        req.params.get('page') === '1' &&
        req.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    request.flush(pageBody([]));
    await expect(pending).resolves.toMatchObject({ items: [], page: 0 });
  });

  it('gets goal detail with GET /financial-goals/{id}', async () => {
    const pending = firstValueFrom(service.get(GOAL_ID));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}`));
    expect(request.request.method).toBe('GET');
    request.flush(goalBody());
    await expect(pending).resolves.toMatchObject({ id: GOAL_ID, status: 'ACTIVE' });
  });

  it('creates a goal with POST /financial-goals', async () => {
    const body = {
      accountId: ACCOUNT_ID,
      name: 'Viagem Chile',
      targetAmount: 5000,
      targetDate: '2026-12-20',
    };
    const pending = firstValueFrom(service.create(body));
    const request = httpTesting.expectOne(api('/financial-goals'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush(goalBody(), { status: 201, statusText: 'Created' });
    await expect(pending).resolves.toMatchObject({ name: 'Viagem Chile' });
  });

  it('updates a goal with PUT /financial-goals/{id} without accountId', async () => {
    const body = { name: 'Viagem atualizada', targetAmount: 6000 };
    const pending = firstValueFrom(service.update(GOAL_ID, body));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}`));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(body);
    expect(request.request.body).not.toHaveProperty('accountId');
    request.flush(goalBody({ name: 'Viagem atualizada', targetAmount: 6000 }));
    await expect(pending).resolves.toMatchObject({ name: 'Viagem atualizada' });
  });

  it('creates a contribution and parses contribution plus goal', async () => {
    const body = { amount: 500, contributionDate: '2026-08-17' };
    const pending = firstValueFrom(service.contribute(GOAL_ID, body));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}/contributions`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush(
      { contribution: contributionBody(), goal: goalBody({ currentAmount: 500 }) },
      { status: 201, statusText: 'Created' },
    );
    const result = await pending;
    expect(result.contribution.amount).toBe(500);
    expect(result.goal.currentAmount).toBe(500);
  });

  it('lists contributions as an array preserving order', async () => {
    const pending = firstValueFrom(service.listContributions(GOAL_ID));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}/contributions`));
    expect(request.request.method).toBe('GET');
    request.flush([contributionBody({ id: 'c1' }), contributionBody({ id: 'c2' })]);
    const items = await pending;
    expect(items.map((item) => item.id)).toEqual(['c1', 'c2']);
  });

  it('creates a redemption and parses redemption plus goal', async () => {
    const body = { amount: 200, redemptionDate: '2026-08-18' };
    const pending = firstValueFrom(service.redeem(GOAL_ID, body));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}/redemptions`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush(
      { redemption: redemptionBody(), goal: goalBody({ currentAmount: 300 }) },
      { status: 201, statusText: 'Created' },
    );
    const result = await pending;
    expect(result.redemption.amount).toBe(200);
    expect(result.goal.currentAmount).toBe(300);
  });

  it('lists redemptions as an array', async () => {
    const pending = firstValueFrom(service.listRedemptions(GOAL_ID));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}/redemptions`));
    expect(request.request.method).toBe('GET');
    request.flush([redemptionBody()]);
    const items = await pending;
    expect(items).toHaveLength(1);
  });

  it('completes a goal with POST /financial-goals/{id}/complete', async () => {
    const pending = firstValueFrom(service.complete(GOAL_ID));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}/complete`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush(goalBody({ status: 'COMPLETED' }));
    await expect(pending).resolves.toMatchObject({ status: 'COMPLETED' });
  });

  it('cancels a goal with POST /financial-goals/{id}/cancel', async () => {
    const pending = firstValueFrom(service.cancel(GOAL_ID));
    const request = httpTesting.expectOne(api(`/financial-goals/${GOAL_ID}/cancel`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush(goalBody({ status: 'CANCELLED' }));
    await expect(pending).resolves.toMatchObject({ status: 'CANCELLED' });
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/financial-goals') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush(
        {
          timestamp: '2026-08-19T15:00:00Z',
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'Erro interno.',
          path: '/api/v1/financial-goals',
        },
        { status: 500, statusText: 'Server Error' },
      );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('rejects a page response that does not match the contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting
      .expectOne(
        (req) =>
          req.url === api('/financial-goals') &&
          req.params.get('page') === '0' &&
          req.params.get('size') === '20',
      )
      .flush({ items: [{ id: GOAL_ID }] });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
