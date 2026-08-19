import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { MainLayout } from './main-layout';

@Component({
  selector: 'app-outlet-stub',
  template: 'child',
})
class OutletStub {}

describe('MainLayout', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideCoreHttp(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'dashboard', component: OutletStub }]),
      ],
    }).compileComponents();
    await TestBed.inject(AuthService).initialize();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('renders the authenticated shell and a router outlet', async () => {
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('header')).not.toBeNull();
    expect(compiled.querySelector('aside')).not.toBeNull();
    expect(compiled.querySelector('main')).not.toBeNull();
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });

  it('toggles the collapsed sidebar state', () => {
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    const layout = fixture.componentInstance;
    const compiled = fixture.nativeElement as HTMLElement;

    expect(layout.sidebarCollapsed()).toBe(false);
    expect(compiled.querySelector('.shell--collapsed')).toBeNull();

    layout.toggleSidebar();
    fixture.detectChanges();

    expect(layout.sidebarCollapsed()).toBe(true);
    expect(compiled.querySelector('.shell--collapsed')).not.toBeNull();
    expect(compiled.querySelector('.shell__collapse')?.textContent).toContain('Expandir menu');
  });

  it('opens and closes the mobile navigation', async () => {
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    const layout = fixture.componentInstance;
    const compiled = fixture.nativeElement as HTMLElement;
    const menu = compiled.querySelector('.header__menu') as HTMLButtonElement;

    menu.focus();
    menu.click();
    fixture.detectChanges();
    await Promise.resolve();

    expect(layout.mobileNavigationOpen()).toBe(true);
    expect(compiled.querySelector('.shell__drawer.is-open')).not.toBeNull();
    expect(compiled.querySelector('#mobile-nav')?.getAttribute('role')).toBe('dialog');
    expect(document.activeElement?.textContent).toContain('Fechar');

    layout.closeMobileNavigation();
    fixture.detectChanges();
    await Promise.resolve();

    expect(layout.mobileNavigationOpen()).toBe(false);
    expect(compiled.querySelector('.shell__drawer.is-open')).toBeNull();
    expect(document.activeElement).toBe(menu);
  });

  it('closes the drawer on Escape and restores focus to the menu button', async () => {
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const menu = compiled.querySelector('.header__menu') as HTMLButtonElement;

    menu.focus();
    menu.click();
    fixture.detectChanges();
    await Promise.resolve();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    await Promise.resolve();

    expect(fixture.componentInstance.mobileNavigationOpen()).toBe(false);
    expect(document.activeElement).toBe(menu);
  });

  it('closes the drawer after selecting a route', async () => {
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    const layout = fixture.componentInstance;

    layout.openMobileNavigation();
    fixture.detectChanges();
    await Promise.resolve();

    const drawerLink = fixture.nativeElement.querySelector('.shell__drawer a') as HTMLAnchorElement;
    drawerLink.click();
    fixture.detectChanges();

    expect(layout.mobileNavigationOpen()).toBe(false);
  });
});
