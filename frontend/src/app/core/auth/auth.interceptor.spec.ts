import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { joinApiUrl } from '../config/api-config';
import { environment } from '../config/environment';
import { provideCoreHttp } from '../http/provide-core-http';
import { AUTH_TOKEN_KEY } from './auth-token.store';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login-stub',
  template: '',
})
class LoginStub {}

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let auth: AuthService;
  let logSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    logSpy = vi.spyOn(console, 'log');
    TestBed.configureTestingModule({
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: LoginStub }]),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpTesting.verify();
    logSpy.mockRestore();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('adds Authorization when a token exists', () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'jwt-token');
    http.get(api('/accounts')).subscribe();
    const request = httpTesting.expectOne(api('/accounts'));
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush([]);
    expect(logSpy).not.toHaveBeenCalled();
  });

  it('does not add Authorization when there is no token', () => {
    http.get(api('/accounts')).subscribe();
    const request = httpTesting.expectOne(api('/accounts'));
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.headers.get('Authorization')).toBeNull();
    request.flush([]);
  });

  it('does not send Bearer null or undefined', () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, '');
    http.get(api('/accounts')).subscribe();
    const request = httpTesting.expectOne(api('/accounts'));
    expect(request.request.headers.get('Authorization')).toBeNull();
    request.flush([]);
  });

  it('invalidates the session on 401', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'jwt-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush({
      id: '01900000-0000-7000-8000-000000000001',
      name: 'Ada',
      email: 'ada@example.com',
      active: true,
      createdAt: '2026-08-13T12:00:00Z',
      updatedAt: '2026-08-13T12:00:00Z',
    });
    await pending;

    http.get(api('/accounts')).subscribe({ error: () => undefined });
    httpTesting.expectOne(api('/accounts')).flush(
      {
        timestamp: '2026-08-12T14:00:00Z',
        status: 401,
        code: 'UNAUTHORIZED',
        message: 'Não autenticado.',
        path: '/api/v1/accounts',
      },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(auth.status()).toBe('unauthenticated');
    expect(auth.getAccessToken()).toBeNull();
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
    expect(logSpy.mock.calls.flat().join(' ')).not.toContain('jwt-token');
  });

  it('does not invalidate the session on 403', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'jwt-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush({
      id: '01900000-0000-7000-8000-000000000001',
      name: 'Ada',
      email: 'ada@example.com',
      active: true,
      createdAt: '2026-08-13T12:00:00Z',
      updatedAt: '2026-08-13T12:00:00Z',
    });
    await pending;

    http.get(api('/accounts')).subscribe({ error: () => undefined });
    httpTesting.expectOne(api('/accounts')).flush(
      {
        timestamp: '2026-08-12T14:00:00Z',
        status: 403,
        code: 'BUSINESS_RULE_VIOLATION',
        message: 'Operação não permitida.',
        path: '/api/v1/accounts',
      },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(auth.status()).toBe('authenticated');
    expect(auth.getAccessToken()).toBe('jwt-token');
  });

  it('does not invalidate the session on 500', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'jwt-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush({
      id: '01900000-0000-7000-8000-000000000001',
      name: 'Ada',
      email: 'ada@example.com',
      active: true,
      createdAt: '2026-08-13T12:00:00Z',
      updatedAt: '2026-08-13T12:00:00Z',
    });
    await pending;

    http.get(api('/accounts')).subscribe({ error: () => undefined });
    httpTesting.expectOne(api('/accounts')).flush(
      {
        timestamp: '2026-08-12T14:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: '/api/v1/accounts',
      },
      { status: 500, statusText: 'Server Error' },
    );

    expect(auth.status()).toBe('authenticated');
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBe('jwt-token');
  });

  it('does not invalidate the session on a network error', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'jwt-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush({
      id: '01900000-0000-7000-8000-000000000001',
      name: 'Ada',
      email: 'ada@example.com',
      active: true,
      createdAt: '2026-08-13T12:00:00Z',
      updatedAt: '2026-08-13T12:00:00Z',
    });
    await pending;

    http.get(api('/accounts')).subscribe({ error: () => undefined });
    httpTesting
      .expectOne(api('/accounts'))
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(auth.status()).toBe('authenticated');
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBe('jwt-token');
  });
});
