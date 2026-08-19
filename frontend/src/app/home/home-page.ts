import { Component, inject } from '@angular/core';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  readonly auth = inject(AuthService);
}
