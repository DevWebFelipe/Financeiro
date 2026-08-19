import { expect, test } from '@playwright/test';
import { apiGetJson, FinancialGoal, loginApi, registerUser } from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { todayIsoDate } from './helpers/dates';
import { createBankAccount } from './helpers/flows';
import { uniqueName, uniqueUser } from './helpers/identity';
import { formatBrl, formatProgressPercent } from './helpers/money';
import { clickButton, fillNumber, gotoFeature, rowByText, waitUntilNotBusy } from './helpers/ui';

test.describe('Financial goals', () => {
  test('creates a goal, contributes, redeems and shows official amounts', async ({
    page,
    request,
  }) => {
    const user = uniqueUser('goal');
    const accountName = uniqueName('Conta meta');
    const goalName = uniqueName('Meta');
    const today = todayIsoDate();
    const target = 400;
    const contribution = 100;
    const redemption = 25;

    await registerUser(request, user);
    await loginViaUi(page, user);
    await createBankAccount(page, accountName, 500);

    await gotoFeature(page, '/goals', 'Metas');
    await clickButton(page, 'Nova meta');
    await page.locator('#goal-account').selectOption({ label: accountName });
    await page.locator('#goal-name').fill(goalName);
    await fillNumber(page.locator('#goal-target-amount'), target);
    await page.locator('#goal-target-date').fill(today);
    await clickButton(page, 'Salvar');
    await expect(rowByText(page, goalName)).toBeVisible();
    await expect(rowByText(page, goalName)).toContainText(formatBrl(0));
    await expect(rowByText(page, goalName)).toContainText('Ativa');

    await rowByText(page, goalName).getByRole('button', { name: 'Contribuir' }).click();
    await expect(page.getByRole('heading', { name: 'Contribuir para a meta' })).toBeVisible();
    await fillNumber(page.locator('#contribute-amount'), contribution);
    await page.locator('#contribute-date').fill(today);
    await clickButton(page, 'Confirmar contribuição');
    await waitUntilNotBusy(page);

    const token = await loginApi(request, user);
    const pageBody = await apiGetJson<{ items: FinancialGoal[] }>(
      request,
      '/financial-goals',
      token,
    );
    const afterContribute = pageBody.items.find((item) => item.name === goalName);
    expect(afterContribute).toBeTruthy();

    await gotoFeature(page, '/goals', 'Metas');
    const contributedRow = rowByText(page, goalName);
    await expect(contributedRow).toContainText(formatBrl(afterContribute!.currentAmount));
    await expect(contributedRow).toContainText(formatProgressPercent(afterContribute!.progressPercent));

    await contributedRow.getByRole('button', { name: 'Resgatar' }).click();
    await expect(page.getByRole('heading', { name: 'Resgatar da meta' })).toBeVisible();
    await fillNumber(page.locator('#redeem-amount'), redemption);
    await page.locator('#redeem-date').fill(today);
    await clickButton(page, 'Confirmar resgate');
    await expect(rowByText(page, goalName)).not.toContainText(
      formatBrl(afterContribute!.currentAmount),
    );

    const afterRedeem = await apiGetJson<FinancialGoal>(
      request,
      `/financial-goals/${afterContribute!.id}`,
      token,
    );
    await expect(rowByText(page, goalName)).toContainText(formatBrl(afterRedeem.currentAmount));
    await expect(rowByText(page, goalName)).toContainText(
      formatProgressPercent(afterRedeem.progressPercent),
    );

    await rowByText(page, goalName).getByRole('button', { name: 'Detalhes' }).click();
    await expect(page.getByRole('heading', { name: 'Detalhes da meta' })).toBeVisible();
    await expect(page.getByText(formatBrl(contribution)).first()).toBeVisible();
    await expect(page.getByText(formatBrl(redemption)).first()).toBeVisible();
  });
});
