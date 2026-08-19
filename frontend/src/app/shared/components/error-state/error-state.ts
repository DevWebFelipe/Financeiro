import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-state',
  templateUrl: './error-state.html',
  styleUrl: './error-state.css',
})
export class ErrorState {
  readonly message = input.required<string>();
  readonly retryLabel = input<string | null>(null);
  readonly retry = output<void>();
}
