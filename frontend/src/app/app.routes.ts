import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';
import { LoginPage } from './features/auth/login-page';
import { RegisterPage } from './features/auth/register-page';
import { MainLayout } from './layout/main-layout/main-layout';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPage,
    canActivate: [guestGuard],
    title: 'Entrar — Financeiro',
  },
  {
    path: 'register',
    component: RegisterPage,
    canActivate: [guestGuard],
    title: 'Criar conta — Financeiro',
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-page').then((module) => module.DashboardPage),
        title: 'Dashboard — Financeiro',
      },
      {
        path: 'accounts',
        loadComponent: () =>
          import('./features/accounts/accounts-page').then((module) => module.AccountsPage),
        title: 'Contas — Financeiro',
      },
      {
        path: 'categories',
        loadComponent: () =>
          import('./features/categories/categories-page').then((module) => module.CategoriesPage),
        title: 'Categorias — Financeiro',
      },
      {
        path: 'expenses',
        loadComponent: () =>
          import('./features/expenses/expenses-page').then((module) => module.ExpensesPage),
        title: 'Despesas — Financeiro',
      },
      {
        path: 'incomes',
        loadComponent: () =>
          import('./features/incomes/incomes-page').then((module) => module.IncomesPage),
        title: 'Receitas — Financeiro',
      },
      {
        path: 'payables',
        loadComponent: () =>
          import('./features/payables/payables-page').then((module) => module.PayablesPage),
        title: 'Contas a pagar — Financeiro',
      },
      { path: '**', redirectTo: 'dashboard' },
    ],
  },
];
