export interface NavItem {
  readonly id: string;
  readonly label: string;
  readonly path: string;
}

export const APP_NAV_ITEMS: readonly NavItem[] = [
  { id: 'dashboard', label: 'Dashboard', path: '/dashboard' },
];
