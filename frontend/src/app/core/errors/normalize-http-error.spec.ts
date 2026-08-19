import { HttpErrorResponse } from '@angular/common/http';
import { ApiError, HttpClientErrorCode, isHttpTransportError } from './api-error';
import { normalizeHttpError } from './normalize-http-error';

const validApiError: ApiError = {
  timestamp: '2026-08-12T14:00:00Z',
  status: 400,
  code: 'VALIDATION_ERROR',
  message: 'Dados inválidos.',
  path: '/api/v1/accounts',
  fields: { amount: 'O valor deve ser maior que zero.' },
};

describe('normalizeHttpError', () => {
  it('preserves a valid backend ApiError including fields', () => {
    const error = new HttpErrorResponse({
      status: 400,
      statusText: 'Bad Request',
      url: '/api/v1/accounts',
      error: validApiError,
    });

    const normalized = normalizeHttpError(error);

    expect(normalized).toEqual(validApiError);
    expect(normalized.fields).toEqual({ amount: 'O valor deve ser maior que zero.' });
  });

  it('keeps the semantic code from a partial backend body', () => {
    const error = new HttpErrorResponse({
      status: 400,
      statusText: 'Bad Request',
      url: '/api/v1/expenses/pay',
      error: {
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Não é possível realizar o pagamento.',
      },
    });

    const normalized = normalizeHttpError(error);

    expect(normalized.code).toBe('BUSINESS_RULE_VIOLATION');
    expect(normalized.message).toBe('Não é possível realizar o pagamento.');
    expect(normalized.status).toBe(400);
    expect(normalized.path).toBe('/api/v1/expenses/pay');
  });

  it('does not treat HTML or empty bodies as backend semantic codes', () => {
    const htmlError = new HttpErrorResponse({
      status: 500,
      statusText: 'Internal Server Error',
      url: '/api/v1/accounts',
      error: '<html>unexpected</html>',
    });

    const htmlNormalized = normalizeHttpError(htmlError);
    expect(htmlNormalized.code).toBe(HttpClientErrorCode.UnparseableResponse);
    expect(htmlNormalized.status).toBe(500);
    expect(htmlNormalized.message).toContain('<html>');

    const emptyError = new HttpErrorResponse({
      status: 502,
      statusText: 'Bad Gateway',
      url: '/api/v1/accounts',
      error: null,
    });

    const emptyNormalized = normalizeHttpError(emptyError);
    expect(emptyNormalized.code).toBe(HttpClientErrorCode.UnparseableResponse);
    expect(emptyNormalized.status).toBe(502);
  });

  it('distinguishes network failures from HTTP 401', () => {
    const error = new HttpErrorResponse({
      status: 0,
      statusText: 'Unknown Error',
      url: 'http://localhost:8080/api/v1/accounts',
      error: new ProgressEvent('error'),
    });

    const normalized = normalizeHttpError(error);

    expect(isHttpTransportError(normalized)).toBe(true);
    expect(normalized.status).toBe(0);
    expect(normalized.code).not.toBe('UNAUTHORIZED');
    expect(normalized.code).not.toBe(401);
  });

  it('returns an already normalized ApiError unchanged in shape', () => {
    const normalized = normalizeHttpError(validApiError);
    expect(normalized).toEqual(validApiError);
  });

  it('parses an ApiError JSON string body', () => {
    const error = new HttpErrorResponse({
      status: 404,
      statusText: 'Not Found',
      url: '/api/v1/accounts/1',
      error: JSON.stringify({
        timestamp: '2026-08-12T14:00:00Z',
        status: 404,
        code: 'NOT_FOUND',
        message: 'Recurso não encontrado.',
        path: '/api/v1/accounts/1',
      }),
    });

    const normalized = normalizeHttpError(error);
    expect(normalized.code).toBe('NOT_FOUND');
    expect(normalized.status).toBe(404);
  });
});
