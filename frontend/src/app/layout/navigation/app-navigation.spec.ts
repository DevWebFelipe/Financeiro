import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { AppNavigation } from './app-navigation';

describe('AppNavigation', () => {
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppNavigation],
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'dashboard', component: AppNavigation },
          { path: 'accounts', component: AppNavigation },
          { path: 'categories', component: AppNavigation },
          { path: 'expenses', component: AppNavigation },
          { path: 'incomes', component: AppNavigation },
          { path: 'payables', component: AppNavigation },
        ]),
      ],
    }).compileComponents();
    router = TestBed.inject(Router);
  });

  it('renders the Dashboard item as an internal router link', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    await fixture.whenStable();
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement | null;
    expect(link).not.toBeNull();
    expect(link?.textContent).toContain('Dashboard');
    expect(link?.getAttribute('href')).toBe('/dashboard');
  });

  it('renders the Accounts item now that the feature exists', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    await fixture.whenStable();
    const links = Array.from(
      fixture.nativeElement.querySelectorAll('a') as NodeListOf<HTMLAnchorElement>,
    );
    const accounts = links.find((link) => link.textContent?.includes('Contas'));
    expect(accounts?.getAttribute('href')).toBe('/accounts');
  });

  it('renders the Categories item now that the feature exists', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    await fixture.whenStable();
    const links = Array.from(
      fixture.nativeElement.querySelectorAll('a') as NodeListOf<HTMLAnchorElement>,
    );
    const categories = links.find((link) => link.textContent?.includes('Categorias'));
    expect(categories?.getAttribute('href')).toBe('/categories');
  });

  it('renders the Expenses item now that the feature exists', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    await fixture.whenStable();
    const links = Array.from(
      fixture.nativeElement.querySelectorAll('a') as NodeListOf<HTMLAnchorElement>,
    );
    const expenses = links.find((link) => link.textContent?.includes('Despesas'));
    expect(expenses?.getAttribute('href')).toBe('/expenses');
  });

  it('renders the Incomes item now that the feature exists', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    await fixture.whenStable();
    const links = Array.from(
      fixture.nativeElement.querySelectorAll('a') as NodeListOf<HTMLAnchorElement>,
    );
    const incomes = links.find((link) => link.textContent?.includes('Receitas'));
    expect(incomes?.getAttribute('href')).toBe('/incomes');
  });

  it('renders the Payables item now that the feature exists', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    await fixture.whenStable();
    const links = Array.from(
      fixture.nativeElement.querySelectorAll('a') as NodeListOf<HTMLAnchorElement>,
    );
    const payables = links.find((link) => link.textContent?.includes('Contas a pagar'));
    expect(payables?.getAttribute('href')).toBe('/payables');
  });

  it('marks the active route with aria-current', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    await router.navigateByUrl('/dashboard');
    fixture.detectChanges();
    await fixture.whenStable();

    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(link.getAttribute('aria-current')).toBe('page');
    expect(link.classList.contains('is-active')).toBe(true);
  });

  it('exposes a named navigation landmark', async () => {
    const fixture = TestBed.createComponent(AppNavigation);
    fixture.detectChanges();
    const nav = fixture.nativeElement.querySelector('nav');
    expect(nav?.getAttribute('aria-label')).toBe('Principal');
  });
});
