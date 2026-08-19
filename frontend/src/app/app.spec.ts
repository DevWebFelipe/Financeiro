import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { provideCoreHttp } from './core/http/provide-core-http';

describe('App', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideCoreHttp(), provideHttpClientTesting()],
    }).compileComponents();
    await TestBed.inject(AuthService).initialize();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the router outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
