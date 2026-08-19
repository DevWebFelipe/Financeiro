import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EmptyState } from './empty-state';

@Component({
  imports: [EmptyState],
  template: `
    <app-empty-state [message]="message">
      @if (showAction) {
        <button type="button">Cadastrar</button>
      }
    </app-empty-state>
  `,
})
class EmptyStateHost {
  message = 'Não há itens para exibir.';
  showAction = false;
}

describe('EmptyState', () => {
  it('renders the empty message without treating it as an error', async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateHost],
    }).compileComponents();

    const fixture = TestBed.createComponent(EmptyStateHost);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não há itens para exibir.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('projects an optional action provided by the feature', async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateHost],
    }).compileComponents();

    const fixture = TestBed.createComponent(EmptyStateHost);
    fixture.componentInstance.showAction = true;
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.textContent).toContain('Cadastrar');
  });
});
