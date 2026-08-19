import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { TransfersService } from './transfers.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const TRANSFER_ID = '01900000-0000-7000-8000-000000000070';
const SOURCE_ID = '01900000-0000-7000-8000-000000000003';
const DEST_ID = '01900000-0000-7000-8000-000000000004';

function transferBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: TRANSFER_ID,
    sourceAccountId: SOURCE_ID,
    destinationAccountId: DEST_ID,
    amount: 500,
    transferDate: '2026-08-10',
    description: 'Transferência',
    status: 'ACTIVE',
    createdAt: '2026-08-10T15:00:00Z',
    ...overrides,
  };
}

describe('TransfersService', () => {
  let service: TransfersService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TransfersService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /transfers as an array without page or size', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(api('/transfers'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    expect(request.request.params.get('page')).toBeNull();
    expect(request.request.params.get('size')).toBeNull();
    request.flush([transferBody()]);

    const items = await pending;
    expect(items).toHaveLength(1);
    expect(items[0]?.amount).toBe(500);
  });

  it('requests GET /transfers with official filters only', async () => {
    const pending = firstValueFrom(
      service.list({
        startDate: '2026-08-01',
        endDate: '2026-08-31',
        accountId: SOURCE_ID,
      }),
    );
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/transfers') &&
        req.params.get('startDate') === '2026-08-01' &&
        req.params.get('endDate') === '2026-08-31' &&
        req.params.get('accountId') === SOURCE_ID,
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('status')).toBeNull();
    expect(request.request.params.get('page')).toBeNull();
    expect(request.request.params.get('size')).toBeNull();
    expect(request.request.params.get('sort')).toBeNull();
    expect(request.request.params.get('search')).toBeNull();
    request.flush([]);
    await expect(pending).resolves.toEqual([]);
  });

  it('gets transfer detail with GET /transfers/{id}', async () => {
    const pending = firstValueFrom(service.get(TRANSFER_ID));
    const request = httpTesting.expectOne(api(`/transfers/${TRANSFER_ID}`));
    expect(request.request.method).toBe('GET');
    request.flush(transferBody());
    await expect(pending).resolves.toMatchObject({ id: TRANSFER_ID, status: 'ACTIVE' });
  });

  it('creates a transfer with POST /transfers and exact payload fields', async () => {
    const body = {
      sourceAccountId: SOURCE_ID,
      destinationAccountId: DEST_ID,
      amount: 500,
      transferDate: '2026-08-10',
      description: 'Aluguel',
    };
    const pending = firstValueFrom(service.create(body));
    const request = httpTesting.expectOne(api('/transfers'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.body).not.toHaveProperty('userId');
    expect(request.request.body).not.toHaveProperty('status');
    expect(request.request.body).not.toHaveProperty('createdAt');
    request.flush(transferBody({ description: 'Aluguel' }), {
      status: 201,
      statusText: 'Created',
    });
    await expect(pending).resolves.toMatchObject({ description: 'Aluguel' });
  });

  it('omits description from POST /transfers when the request does not include it', async () => {
    const body = {
      sourceAccountId: SOURCE_ID,
      destinationAccountId: DEST_ID,
      amount: 80,
      transferDate: '2026-08-10',
    };
    const pending = firstValueFrom(service.create(body));
    const request = httpTesting.expectOne(api('/transfers'));
    expect(request.request.body).toEqual(body);
    expect(request.request.body).not.toHaveProperty('description');
    request.flush(transferBody({ amount: 80, description: null }), {
      status: 201,
      statusText: 'Created',
    });
    await expect(pending).resolves.toMatchObject({ amount: 80, description: null });
  });

  it('reverses a transfer with POST /transfers/{id}/reverse and empty body', async () => {
    const pending = firstValueFrom(service.reverse(TRANSFER_ID));
    const request = httpTesting.expectOne(api(`/transfers/${TRANSFER_ID}/reverse`));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush(transferBody({ status: 'REVERSED' }));
    await expect(pending).resolves.toMatchObject({ status: 'REVERSED' });
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/transfers')).flush(
      {
        timestamp: '2026-08-19T15:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: '/api/v1/transfers',
      },
      { status: 500, statusText: 'Server Error' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('rejects a list response that does not match the transfers contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/transfers')).flush({ items: [transferBody()] });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
