import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { joinApiUrl } from '../config/api-config';
import { environment } from '../config/environment';
import { provideCoreHttp } from '../http/provide-core-http';
import { authGuard, guestGuard } from './auth.guard';
import { AUTH_TOKEN_KEY } from './auth-token.store';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-private-stub',
  template: 'private',
})
class PrivateStub {}

@Component({
  selector: 'app-login-stub',
  template: 'login',
})
class LoginStub {}

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

describe('authGuard', () => {
  let router: Router;
  let auth: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: LoginStub },
          { path: 'private', component: PrivateStub, canActivate: [authGuard] },
        ]),
      ],
    });
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('waits while auth is loading and then allows an authenticated user', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'jwt-token');
    const pendingInit = auth.initialize();
    const pendingNav = router.navigateByUrl('/private');

    httpTesting.expectOne(api('/users/me')).flush({
      id: '01900000-0000-7000-8000-000000000001',
      name: 'Ada',
      email: 'ada@example.com',
      active: true,
      createdAt: '2026-08-13T12:00:00Z',
      updatedAt: '2026-08-13T12:00:00Z',
    });
    await pendingInit;
    await pendingNav;

    expect(router.url).toBe('/private');
  });

  it('redirects unauthenticated users to login and preserves an internal URL', async () => {
    await auth.initialize();
    await router.navigateByUrl('/private');
    expect(router.url).toBe('/login?returnUrl=%2Fprivate');
  });

  it('does not preserve an external return URL as an open redirect', async () => {
    await auth.initialize();
    const result = await new Promise<unknown>((resolve) => {
      TestBed.runInInjectionContext(() => {
        const outcome = authGuard(
          {} as never,
          { url: 'https://site-malicioso.com', root: {} as never } as never,
        );
        if (typeof outcome !== 'boolean' && 'subscribe' in outcome) {
          outcome.subscribe((value) => resolve(value));
          return;
        }
        resolve(outcome);
      });
    });

    expect(String(result)).toContain('/login');
    expect(String(result)).not.toContain('site-malicioso.com');
    expect(String(result)).not.toContain('returnUrl');
  });
});

@Component({
  selector: 'app-dashboard-stub',
  template: 'dashboard',
})
class DashboardStub {}

describe('guestGuard', () => {
  let router: Router;
  let auth: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: LoginStub, canActivate: [guestGuard] },
          { path: 'dashboard', component: DashboardStub },
        ]),
      ],
    });
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('allows unauthenticated users to open login', async () => {
    await auth.initialize();
    await router.navigateByUrl('/login');
    expect(router.url).toBe('/login');
  });

  it('sends authenticated users from login to /dashboard', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'jwt-token');
    const pendingInit = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush({
      id: '01900000-0000-7000-8000-000000000001',
      name: 'Ada',
      email: 'ada@example.com',
      active: true,
      createdAt: '2026-08-13T12:00:00Z',
      updatedAt: '2026-08-13T12:00:00Z',
    });
    await pendingInit;
    await router.navigateByUrl('/login');
    expect(router.url).toBe('/dashboard');
  });
});
