import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { APP_NAV_ITEMS } from './nav-items';

@Component({
  selector: 'app-navigation',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './app-navigation.html',
  styleUrl: './app-navigation.css',
})
export class AppNavigation {
  readonly collapsed = input(false);
  readonly navId = input<string | null>(null);
  readonly navigated = output<void>();
  readonly items = APP_NAV_ITEMS;

  onNavigate(): void {
    this.navigated.emit();
  }
}
