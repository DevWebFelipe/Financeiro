import { Component, ElementRef, inject, input, output, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './app-header.html',
  styleUrl: './app-header.css',
})
export class AppHeader {
  readonly auth = inject(AuthService);
  readonly mobileNavOpen = input(false);
  readonly menuToggle = output<void>();
  readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

  onLogout(): void {
    void this.auth.logout();
  }
}
