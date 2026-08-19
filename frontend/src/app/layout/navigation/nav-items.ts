export interface NavItem {
  readonly id: string;
  readonly label: string;
  readonly path: string;
}

export const APP_NAV_ITEMS: readonly NavItem[] = [
  { id: 'dashboard', label: 'Dashboard', path: '/dashboard' },
  { id: 'accounts', label: 'Contas', path: '/accounts' },
  { id: 'credit-cards', label: 'Cartões', path: '/credit-cards' },
  { id: 'invoices', label: 'Faturas', path: '/invoices' },
  { id: 'categories', label: 'Categorias', path: '/categories' },
  { id: 'expenses', label: 'Despesas', path: '/expenses' },
  { id: 'incomes', label: 'Receitas', path: '/incomes' },
  { id: 'payables', label: 'Contas a pagar', path: '/payables' },
];
