import { expect, test } from '@playwright/test';
import { registerUser } from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { uniqueName, uniqueUser } from './helpers/identity';
import { createBankAccount } from './helpers/flows';
import {
  attachRequestObserver,
  clickButton,
  expectNoErrorBanner,
  gotoFeature,
  openDesktopNav,
  waitForHeading,
  waitUntilNotBusy,
} from './helpers/ui';

const ROUTES: ReadonlyArray<{ path: string; heading: string; nav: string }> = [
  { path: '/dashboard', heading: 'Dashboard', nav: 'Dashboard' },
  { path: '/accounts', heading: 'Contas', nav: 'Contas' },
  { path: '/credit-cards', heading: 'Cartões', nav: 'Cartões' },
  { path: '/invoices', heading: 'Faturas', nav: 'Faturas' },
  { path: '/categories', heading: 'Categorias', nav: 'Categorias' },
  { path: '/expenses', heading: 'Despesas', nav: 'Despesas' },
  { path: '/incomes', heading: 'Receitas', nav: 'Receitas' },
  { path: '/payables', heading: 'Contas a pagar', nav: 'Contas a pagar' },
  { path: '/transfers', heading: 'Transferências', nav: 'Transferências' },
  { path: '/goals', heading: 'Metas', nav: 'Metas' },
  { path: '/projections', heading: 'Projeções', nav: 'Projeções' },
  { path: '/reports', heading: 'Relatórios', nav: 'Relatórios' },
];

test.describe('Visual validation', () => {
  test('desktop shell, empty states, form and table after mutation', async ({ page, request }) => {
    await page.setViewportSize({ width: 1366, height: 768 });
    const observer = attachRequestObserver(page);
    const user = uniqueUser('vis-d');
    await registerUser(request, user);
    await loginViaUi(page, user);

    await expect(page.locator('.shell__sidebar')).toBeVisible();
    await expect(page.getByRole('button', { name: /Abrir menu/ })).toHaveCount(0);

    for (const route of ROUTES) {
      await openDesktopNav(page, route.nav);
      await waitForHeading(page, route.heading);
      await waitUntilNotBusy(page);
      await expectNoErrorBanner(page);
    }

    await gotoFeature(page, '/accounts', 'Contas');
    await expect(page.getByText('Nenhuma conta cadastrada.')).toBeVisible();
    const accountName = uniqueName('Conta visual');
    await createBankAccount(page, accountName, 10);
    await expect(page.getByRole('table')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(accountName) })).toBeVisible();

    await gotoFeature(page, '/credit-cards', 'Cartões');
    await expect(page.getByText('Nenhum cartão cadastrado.')).toBeVisible();
    await clickButton(page, 'Novo cartão');
    await expect(page.locator('#credit-card-form-title')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.locator('#credit-card-form-title')).toHaveCount(0);

    const loops = observer.duplicates(20);
    expect(loops, loops.join('\n')).toEqual([]);
  });

  test('mobile drawer navigation, Escape and structural layout', async ({ page, request }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    const user = uniqueUser('vis-m');
    await registerUser(request, user);
    await loginViaUi(page, user);

    await expect(page.locator('.shell__sidebar')).toBeHidden();
    await expect(page.getByRole('button', { name: /Abrir menu/ })).toBeVisible();

    await page.getByRole('button', { name: /Abrir menu/ }).click();
    await expect(page.locator('#mobile-nav')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Menu' })).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.locator('#mobile-nav')).toBeHidden();

    await page.getByRole('button', { name: /Abrir menu/ }).click();
    await page.locator('#mobile-nav').getByRole('link', { name: 'Contas', exact: true }).click();
    await waitForHeading(page, 'Contas');
    await expect(page.getByText('Nenhuma conta cadastrada.')).toBeVisible();

    await page.getByRole('button', { name: /Abrir menu/ }).click();
    await page.locator('#mobile-nav').getByRole('link', { name: 'Relatórios', exact: true }).click();
    await waitForHeading(page, 'Relatórios');
    await expect(page.getByRole('button', { name: 'Consultar' })).toBeVisible();
  });
});
