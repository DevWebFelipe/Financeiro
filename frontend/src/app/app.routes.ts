import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';
import { LoginPage } from './features/auth/login-page';
import { RegisterPage } from './features/auth/register-page';
import { HomePage } from './home/home-page';

export const routes: Routes = [
  { path: 'login', component: LoginPage, canActivate: [guestGuard] },
  { path: 'register', component: RegisterPage, canActivate: [guestGuard] },
  { path: '', component: HomePage, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
