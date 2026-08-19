import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { isApiError } from '../../core/errors/api-error';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { CategoriesService } from './categories.service';

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const CATEGORY_ID = '01900000-0000-7000-8000-000000000001';

function categoryBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: CATEGORY_ID,
    name: 'Mercado',
    type: 'EXPENSE',
    active: true,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

describe('CategoriesService', () => {
  let service: CategoriesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCoreHttp(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoriesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('requests GET /categories without query params by default', async () => {
    const pending = firstValueFrom(service.list());
    const request = httpTesting.expectOne(api('/categories'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([categoryBody()]);

    const categories = await pending;
    expect(categories).toHaveLength(1);
    expect(categories[0]?.name).toBe('Mercado');
    expect(categories[0]?.type).toBe('EXPENSE');
    expect(categories[0]?.active).toBe(true);
  });

  it('requests GET /categories with official type and active filters', async () => {
    const pending = firstValueFrom(service.list({ type: 'INCOME', active: false }));
    const request = httpTesting.expectOne(
      (req) =>
        req.url === api('/categories') &&
        req.params.get('type') === 'INCOME' &&
        req.params.get('active') === 'false',
    );
    expect(request.request.method).toBe('GET');
    request.flush([categoryBody({ type: 'INCOME', active: false, name: 'Salário' })]);
    await expect(pending).resolves.toMatchObject([
      { name: 'Salário', type: 'INCOME', active: false },
    ]);
  });

  it('creates a category with POST /categories', async () => {
    const pending = firstValueFrom(service.create({ name: 'Mercado', type: 'EXPENSE' }));
    const request = httpTesting.expectOne(api('/categories'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Mercado', type: 'EXPENSE' });
    request.flush(categoryBody(), { status: 201, statusText: 'Created' });
    await expect(pending).resolves.toMatchObject({ name: 'Mercado', active: true });
  });

  it('updates name and type with PUT /categories/{id}', async () => {
    const pending = firstValueFrom(
      service.update(CATEGORY_ID, { name: 'Moradia', type: 'EXPENSE' }),
    );
    const request = httpTesting.expectOne(api(`/categories/${CATEGORY_ID}`));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Moradia', type: 'EXPENSE' });
    request.flush(categoryBody({ name: 'Moradia' }));
    await expect(pending).resolves.toMatchObject({ name: 'Moradia' });
  });

  it('deactivates with POST /categories/{id}/deactivate', async () => {
    const pending = firstValueFrom(service.deactivate(CATEGORY_ID));
    const request = httpTesting.expectOne(api(`/categories/${CATEGORY_ID}/deactivate`));
    expect(request.request.method).toBe('POST');
    request.flush(categoryBody({ active: false }));
    await expect(pending).resolves.toMatchObject({ active: false });
  });

  it('propagates ApiError from the HTTP interceptor', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/categories')).flush(
      {
        timestamp: '2026-08-19T15:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: '/api/v1/categories',
      },
      { status: 500, statusText: 'Server Error' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
  });

  it('propagates validation errors from POST /categories', async () => {
    const pending = firstValueFrom(service.create({ name: '', type: 'EXPENSE' })).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/categories')).flush(
      {
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Dados inválidos.',
        path: '/api/v1/categories',
        fields: { name: 'O nome é obrigatório.' },
      },
      { status: 400, statusText: 'Bad Request' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
    if (isApiError(error)) {
      expect(error.fields?.['name']).toBe('O nome é obrigatório.');
    }
  });

  it('propagates conflict errors from POST /categories', async () => {
    const pending = firstValueFrom(service.create({ name: 'Mercado', type: 'EXPENSE' })).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/categories')).flush(
      {
        timestamp: '2026-08-19T15:00:00Z',
        status: 409,
        code: 'CONFLICT',
        message: 'Já existe uma categoria com este nome e tipo.',
        path: '/api/v1/categories',
      },
      { status: 409, statusText: 'Conflict' },
    );

    const error = await pending;
    expect(isApiError(error)).toBe(true);
    if (isApiError(error)) {
      expect(error.code).toBe('CONFLICT');
    }
  });

  it('rejects a list response that does not match the categories contract', async () => {
    const pending = firstValueFrom(service.list()).then(
      () => {
        throw new Error('expected error');
      },
      (error: unknown) => error,
    );

    httpTesting.expectOne(api('/categories')).flush({ id: CATEGORY_ID });
    const error = await pending;
    expect(error).toBeInstanceOf(Error);
  });
});
