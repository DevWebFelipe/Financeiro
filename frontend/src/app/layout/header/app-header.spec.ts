import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { joinApiUrl } from '../../core/config/api-config';
import { environment } from '../../core/config/environment';
import { AUTH_TOKEN_KEY } from '../../core/auth/auth-token.store';
import { AuthService } from '../../core/auth/auth.service';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { AppHeader } from './app-header';

@Component({
  selector: 'app-login-stub',
  template: 'login',
})
class LoginStub {}

@Component({
  selector: 'app-dashboard-stub',
  template: 'dashboard',
})
class DashboardStub {}

const api = (path: string) => joinApiUrl(environment.apiBaseUrl, path);

const user = {
  id: '01900000-0000-7000-8000-000000000001',
  name: 'Ada Lovelace',
  email: 'ada@example.com',
  active: true,
  createdAt: '2026-08-13T12:00:00Z',
  updatedAt: '2026-08-13T12:00:00Z',
};

describe('AppHeader', () => {
  let auth: AuthService;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AppHeader],
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: LoginStub },
          { path: 'dashboard', component: DashboardStub },
        ]),
      ],
    }).compileComponents();
    auth = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('shows the authenticated user name', async () => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, 'access-token');
    const pending = auth.initialize();
    httpTesting.expectOne(api('/users/me')).flush(user);
    await pending;

    const fixture = TestBed.createComponent(AppHeader);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ada Lovelace');
    httpTesting.expectNone(api('/users/me'));
  });

  it('calls AuthService.logout from the header action', async () => {
    await auth.initialize();
    const fixture = TestBed.createComponent(AppHeader);
    fixture.detectChanges();

    const logout = vi.spyOn(auth, 'logout');
    const button = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((element) => element.textContent?.includes('Sair'));
    button?.click();

    expect(logout).toHaveBeenCalledOnce();
    await fixture.whenStable();
    expect(router.url).toBe('/login');
  });

  it('names the mobile menu button', async () => {
    await auth.initialize();
    const fixture = TestBed.createComponent(AppHeader);
    fixture.detectChanges();
    const menu = fixture.nativeElement.querySelector('.header__menu') as HTMLButtonElement;
    expect(menu.textContent).toContain('Abrir menu');
    expect(menu.getAttribute('aria-expanded')).toBe('false');
  });
});
