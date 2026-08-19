import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideCoreHttp } from '../../core/http/provide-core-http';
import { RegisterPage } from './register-page';

describe('RegisterPage', () => {
  it('should create', async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [provideCoreHttp(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(RegisterPage);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
