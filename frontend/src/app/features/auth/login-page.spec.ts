import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  it('should create', async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideCoreHttp(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginPage);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
