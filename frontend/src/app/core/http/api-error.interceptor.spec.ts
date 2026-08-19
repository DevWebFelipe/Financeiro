import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { API_BASE_URL } from '../config/api-config';
import { environment } from '../config/environment';
import {
  ApiError,
  HttpClientErrorCode,
  isApiError,
  isHttpTransportError,
} from '../errors/api-error';
import { provideCoreHttp } from './provide-core-http';

function expectApiError(value: unknown): ApiError {
  expect(isApiError(value)).toBe(true);
  if (!isApiError(value)) {
    throw new Error('expected ApiError');
  }
  return value;
}

describe('core HTTP infrastructure', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let navigate: ReturnType<typeof vi.fn>;
  let sessionGetItem: ReturnType<typeof vi.spyOn>;
  let sessionSetItem: ReturnType<typeof vi.spyOn>;
  let sessionRemoveItem: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    navigate = vi.fn();
    sessionGetItem = vi.spyOn(sessionStorage, 'getItem');
    sessionSetItem = vi.spyOn(sessionStorage, 'setItem');
    sessionRemoveItem = vi.spyOn(sessionStorage, 'removeItem');

    TestBed.configureTestingModule({
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate } },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionGetItem.mockRestore();
    sessionSetItem.mockRestore();
    sessionRemoveItem.mockRestore();
  });

  it('provides the centralized API base URL', () => {
    expect(TestBed.inject(API_BASE_URL)).toBe(environment.apiBaseUrl);
  });

  it('propagates a valid ApiError from the interceptor', () => {
    const body: ApiError = {
      timestamp: '2026-08-12T14:00:00Z',
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Dados inválidos.',
      path: '/api/v1/accounts',
      fields: { name: 'O nome é obrigatório.' },
    };

    let captured: unknown;
    http.get('/api/v1/accounts').subscribe({
      error: (error: unknown) => {
        captured = error;
      },
    });

    httpTesting
      .expectOne('/api/v1/accounts')
      .flush(body, { status: 400, statusText: 'Bad Request' });

    expect(expectApiError(captured)).toEqual(body);
  });

  it('forwards 401 without logout, navigation or sessionStorage', () => {
    const body: ApiError = {
      timestamp: '2026-08-12T14:00:00Z',
      status: 401,
      code: 'UNAUTHORIZED',
      message: 'Não autenticado.',
      path: '/api/v1/accounts',
    };

    let captured: unknown;
    http.get('/api/v1/accounts').subscribe({
      error: (error: unknown) => {
        captured = error;
      },
    });

    httpTesting
      .expectOne('/api/v1/accounts')
      .flush(body, { status: 401, statusText: 'Unauthorized' });

    const apiError = expectApiError(captured);
    expect(apiError.status).toBe(401);
    expect(apiError.code).toBe('UNAUTHORIZED');
    expect(navigate).not.toHaveBeenCalled();
    expect(sessionGetItem).not.toHaveBeenCalled();
    expect(sessionSetItem).not.toHaveBeenCalled();
    expect(sessionRemoveItem).not.toHaveBeenCalled();
  });

  it('forwards 403 without treating it as session termination', () => {
    const body: ApiError = {
      timestamp: '2026-08-12T14:00:00Z',
      status: 403,
      code: 'BUSINESS_RULE_VIOLATION',
      message: 'Operação não permitida.',
      path: '/api/v1/accounts',
    };

    let captured: unknown;
    http.get('/api/v1/accounts').subscribe({
      error: (error: unknown) => {
        captured = error;
      },
    });

    httpTesting.expectOne('/api/v1/accounts').flush(body, { status: 403, statusText: 'Forbidden' });

    const apiError = expectApiError(captured);
    expect(apiError.status).toBe(403);
    expect(apiError.code).not.toBe('UNAUTHORIZED');
    expect(navigate).not.toHaveBeenCalled();
    expect(sessionGetItem).not.toHaveBeenCalled();
    expect(sessionSetItem).not.toHaveBeenCalled();
    expect(sessionRemoveItem).not.toHaveBeenCalled();
  });

  it('does not map a network failure to 401', () => {
    let captured: unknown;
    http.get('http://localhost:8080/api/v1/accounts').subscribe({
      error: (error: unknown) => {
        captured = error;
      },
    });

    httpTesting
      .expectOne('http://localhost:8080/api/v1/accounts')
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    const apiError = expectApiError(captured);
    expect(isHttpTransportError(apiError)).toBe(true);
    expect(apiError.status).toBe(0);
    expect(apiError.code).toBe(HttpClientErrorCode.Transport);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('lets successful responses pass through', () => {
    let payload: unknown;
    http.get('/api/v1/health').subscribe((value) => {
      payload = value;
    });

    httpTesting.expectOne('/api/v1/health').flush({ status: 'UP' });
    expect(payload).toEqual({ status: 'UP' });
  });
});
