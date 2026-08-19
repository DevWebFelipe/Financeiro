import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ErrorState } from './error-state';

@Component({
  imports: [ErrorState],
  template: `
    <app-error-state [message]="message" [retryLabel]="retryLabel" (retry)="retried = true" />
  `,
})
class ErrorStateHost {
  message = 'Não foi possível carregar os dados.';
  retryLabel: string | null = 'Tentar novamente';
  retried = false;
}

describe('ErrorState', () => {
  it('renders the provided message as an alert', async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorStateHost],
    }).compileComponents();

    const fixture = TestBed.createComponent(ErrorStateHost);
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('Não foi possível carregar os dados.');
    expect(alert.textContent).toContain('Tentar novamente');
  });

  it('omits retry when no label is provided', async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorStateHost],
    }).compileComponents();

    const fixture = TestBed.createComponent(ErrorStateHost);
    fixture.componentInstance.retryLabel = null;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('emits retry from the action button', async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorStateHost],
    }).compileComponents();

    const fixture = TestBed.createComponent(ErrorStateHost);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    button.click();
    expect(fixture.componentInstance.retried).toBe(true);
  });
});
