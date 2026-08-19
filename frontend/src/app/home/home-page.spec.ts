import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideCoreHttp } from '../core/http/provide-core-http';
import { HomePage } from './home-page';

describe('HomePage', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideCoreHttp(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(HomePage);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the application title', async () => {
    const fixture = TestBed.createComponent(HomePage);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Financial Control');
  });
});
