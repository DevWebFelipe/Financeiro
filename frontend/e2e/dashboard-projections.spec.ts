import { expect, test } from '@playwright/test';
import { registerUser } from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { todayIsoDate } from './helpers/dates';
import {
  createAccountExpense,
  createBankAccount,
  createCategory,
  createIncome,
  payExpense,
  receiveIncome,
} from './helpers/flows';
import { uniqueName, uniqueUser } from './helpers/identity';
import { formatBrl } from './helpers/money';
import { attachRequestObserver, expectNoErrorBanner, gotoFeature, waitUntilNotBusy } from './helpers/ui';

test.describe('Dashboard and projections', () => {
  test('loads official dashboard and projections after real movements', async ({
    page,
    request,
  }) => {
    const user = uniqueUser('dash');
    const accountName = uniqueName('Conta dash');
    const incomeCategory = uniqueName('Cat rec dash');
    const expenseCategory = uniqueName('Cat desp dash');
    const incomeName = uniqueName('Receita dash');
    const expenseName = uniqueName('Despesa dash');
    const today = todayIsoDate();

    await registerUser(request, user);
    await loginViaUi(page, user);
    await createBankAccount(page, accountName, 200);
    await createCategory(page, incomeCategory, 'INCOME');
    await createCategory(page, expenseCategory, 'EXPENSE');
    await createIncome(page, {
      description: incomeName,
      categoryName: incomeCategory,
      amount: 300,
      expectedDate: today,
    });
    await receiveIncome(page, incomeName, accountName, 300, today);
    await createAccountExpense(page, {
      description: expenseName,
      categoryName: expenseCategory,
      accountName,
      amount: 40,
      expenseDate: today,
      dueDate: today,
    });
    await payExpense(page, expenseName, accountName, 40, today);

    const observer = attachRequestObserver(page);
    await gotoFeature(page, '/dashboard', 'Dashboard');
    await expectNoErrorBanner(page);
    await expect(page.getByRole('heading', { name: 'Situação atual' })).toBeVisible();
    await expect(page.getByText('Saldo total')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Projeção' })).toBeVisible();
    await expect(page.getByText('Entradas projetadas')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Contas a pagar' })).toBeVisible();
    await expect(page.getByText(formatBrl(460)).first()).toBeVisible();

    await gotoFeature(page, '/projections', 'Projeções');
    await expectNoErrorBanner(page);
    await expect(page.getByRole('heading', { name: 'Período e conta' })).toBeVisible();
    await expect(page.locator('#filter-period-mode')).toBeVisible();
    await waitUntilNotBusy(page);
    await expect(page.getByText('Fluxo de caixa projetado').first()).toBeVisible();

    const loops = observer.duplicates(20);
    expect(loops, loops.join('\n')).toEqual([]);
  });
});
