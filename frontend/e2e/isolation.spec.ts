import { expect, test } from '@playwright/test';
import { apiGet, loginApi, registerUser } from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { createBankAccount } from './helpers/flows';
import { uniqueName, uniqueUser } from './helpers/identity';
import { gotoFeature, rowByText } from './helpers/ui';

test.describe('User isolation', () => {
  test('user B cannot see or fetch resources created by user A', async ({ page, request }) => {
    const userA = uniqueUser('iso-a');
    const userB = uniqueUser('iso-b');
    const accountName = uniqueName('Conta A');

    await registerUser(request, userA);
    await registerUser(request, userB);

    await loginViaUi(page, userA);
    await createBankAccount(page, accountName, 100);
    await expect(rowByText(page, accountName)).toBeVisible();

    const tokenA = await loginApi(request, userA);
    const accountsA = await apiGet(request, '/accounts', tokenA);
    expect(accountsA.status).toBe(200);
    const listA = accountsA.body as Array<{ id: string; name: string }>;
    const accountA = listA.find((item) => item.name === accountName);
    expect(accountA).toBeTruthy();

    await page.getByRole('button', { name: 'Sair' }).click();
    await loginViaUi(page, userB);
    await gotoFeature(page, '/accounts', 'Contas');
    await expect(page.getByText(accountName)).toHaveCount(0);
    await expect(page.getByText('Nenhuma conta cadastrada.')).toBeVisible();

    const tokenB = await loginApi(request, userB);
    const foreign = await apiGet(request, `/accounts/${accountA!.id}`, tokenB);
    expect(foreign.status).toBe(404);

    const listB = await apiGet(request, '/accounts', tokenB);
    expect(listB.status).toBe(200);
    expect(listB.body).toEqual([]);
  });
});
