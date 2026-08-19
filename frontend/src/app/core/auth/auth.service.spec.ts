import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { joinApiUrl } from '../config/api-config';
import { environment } from '../config/environment';
import { provideCoreHttp } from '../http/provide-core-http';
import { AUTH_TOKEN_KEY } from './auth-token.store';
import { AuthUser } from './auth.models';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login-stub',
  template: '',
})
class LoginStub {}

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const user: AuthUser = {
  id: '01900000-0000-7000-8000-000000000001',
  name: 'Ada Lovelace',
  email: 'ada@example.com',
  active: true,
  createdAt: '2026-08-13T12:00:00Z',
  updatedAt: '2026-08-13T12:00:00Z',
};

function unauthorized(path: string) {
  return {
    timestamp: '2026-08-12T14:00:00Z',
    status: 401,
    code: 'UNAUTHORIZED',
    message: 'Não autenticado.',
    path,
  };
}

describe('AuthService', () => {
  let auth: AuthService;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: LoginStub }]),
      ],
    });
    auth = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('starts in loading until initialize runs', () => {
    expect(auth.status()).toBe('loading');
    expect(auth.user()).toBeNull();
    expect(auth.getAccessToken()).toBeNull();
  });

  it('becomes unauthenticated when there is no token', async () => {
    await auth.initialize();
    expect(auth.status()).toBe('unauthenticated');
    expect(auth.user()).toBeNull();
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });

  it('restores the session when a token exists and /users/me returns 200', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    expect(auth.status()).toBe('authenticated');
    expect(auth.user()).toEqual(user);
    expect(auth.getAccessToken()).toBe('access-token');
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBe('access-token');
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });

  it('clears the session when stored token is rejected with 401', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'expired-token');
    const pending = auth.initialize();
    httpTesting
      .expectOne(api('/users/me'))
      .flush(unauthorized('/api/v1/users/me'), { status: 401, statusText: 'Unauthorized' });
    await pending;

    expect(auth.status()).toBe('unauthenticated');
    expect(auth.user()).toBeNull();
    expect(auth.getAccessToken()).toBeNull();
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });

  it('keeps the token when /users/me returns 500', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(
      {
        timestamp: '2026-08-12T14:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'Erro interno.',
        path: '/api/v1/users/me',
      },
      { status: 500, statusText: 'Server Error' },
    );
    await pending;

    expect(auth.status()).toBe('authenticated');
    expect(auth.getAccessToken()).toBe('access-token');
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBe('access-token');
  });

  it('keeps the token when /users/me fails with a network error', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting
      .expectOne(api('/users/me'))
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
    await pending;

    expect(auth.status()).toBe('authenticated');
    expect(auth.getAccessToken()).toBe('access-token');
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBe('access-token');
  });

  it('stores only the access token after a successful login', async () => {
    const pending = auth.login({ email: 'ada@example.com', password: 'secret-password' });
    const loginRequest = httpTesting.expectOne(api('/auth/login'));
    expect(loginRequest.request.body).toEqual({
      email: 'ada@example.com',
      password: 'secret-password',
    });
    loginRequest.flush({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 1800 });
    await Promise.resolve();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    expect(auth.status()).toBe('authenticated');
    expect(auth.user()).toEqual(user);
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBe('jwt-token');
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(1);
    expect(sessionStorage.key(0)).toBe(AUTH_TOKEN_KEY);
  });

  it('does not create a session after register', async () => {
    await auth.initialize();
    const pending = auth.register({
      name: 'Ada Lovelace',
      email: 'ada@example.com',
      password: 'secret-password',
    });
    httpTesting
      .expectOne(api('/auth/register'))
      .flush(user, { status: 201, statusText: 'Created' });
    await pending;

    expect(auth.status()).toBe('unauthenticated');
    expect(auth.getAccessToken()).toBeNull();
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });

  it('logout removes the token and goes unauthenticated', async () => {
    const pending = auth.login({ email: 'ada@example.com', password: 'secret-password' });
    httpTesting
      .expectOne(api('/auth/login'))
      .flush({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 1800 });
    await Promise.resolve();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    await auth.logout();

    expect(auth.status()).toBe('unauthenticated');
    expect(auth.user()).toBeNull();
    expect(auth.getAccessToken()).toBeNull();
    expect(sessionStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
    expect(router.url).toBe('/login');
  });

  it('does not write the JWT to localStorage', async () => {
    const setItem = vi.spyOn(localStorage, 'setItem');
    const pending = auth.login({ email: 'ada@example.com', password: 'secret-password' });
    httpTesting
      .expectOne(api('/auth/login'))
      .flush({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 1800 });
    await Promise.resolve();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;
    expect(setItem).not.toHaveBeenCalled();
    setItem.mockRestore();
  });
});
