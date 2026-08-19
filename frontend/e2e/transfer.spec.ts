import { expect, test } from '@playwright/test';
import { Account, AccountBalance, apiGetJson, loginApi, registerUser, Transfer } from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { todayIsoDate } from './helpers/dates';
import { createBankAccount } from './helpers/flows';
import { uniqueName, uniqueUser } from './helpers/identity';
import { formatBrl } from './helpers/money';
import { clickButton, fillNumber, gotoFeature, rowByText, waitUntilNotBusy } from './helpers/ui';

test.describe('Transfers', () => {
  test('transfers between two bank accounts and reverses the official history', async ({
    page,
    request,
  }) => {
    const user = uniqueUser('xfer');
    const sourceName = uniqueName('Origem');
    const destinationName = uniqueName('Destino');
    const today = todayIsoDate();
    const opening = 800;
    const amount = 150;

    await registerUser(request, user);
    await loginViaUi(page, user);
    await createBankAccount(page, sourceName, opening);
    await createBankAccount(page, destinationName, 50);

    await gotoFeature(page, '/transfers', 'Transferências');
    await clickButton(page, 'Nova transferência');
    await page.locator('#transfer-source').selectOption({ label: sourceName });
    await page.locator('#transfer-destination').selectOption({ label: destinationName });
    await fillNumber(page.locator('#transfer-amount'), amount);
    await page.locator('#transfer-date').fill(today);
    await page.locator('#transfer-description').fill(uniqueName('Repasse'));
    await clickButton(page, 'Salvar');
    await expect(page.getByText('Ativa').first()).toBeVisible();
    await waitUntilNotBusy(page);

    const token = await loginApi(request, user);
    const accounts = await apiGetJson<Account[]>(request, '/accounts', token);
    const source = accounts.find((item) => item.name === sourceName);
    const destination = accounts.find((item) => item.name === destinationName);
    expect(source && destination).toBeTruthy();

    const sourceAfter = await apiGetJson<AccountBalance>(
      request,
      `/accounts/${source!.id}/balance`,
      token,
    );
    const destinationAfter = await apiGetJson<AccountBalance>(
      request,
      `/accounts/${destination!.id}/balance`,
      token,
    );

    await gotoFeature(page, '/accounts', 'Contas');
    await expect(rowByText(page, sourceName)).toContainText(formatBrl(sourceAfter.totalBalance));
    await expect(rowByText(page, destinationName)).toContainText(
      formatBrl(destinationAfter.totalBalance),
    );

    await gotoFeature(page, '/transfers', 'Transferências');
    await rowByText(page, 'Ativa').getByRole('button', { name: 'Estornar' }).click();
    await expect(page.getByRole('heading', { name: 'Estornar transferência' })).toBeVisible();
    await clickButton(page, 'Confirmar estorno');
    await expect(page.getByText('Estornada').first()).toBeVisible();

    const transfers = await apiGetJson<Transfer[]>(request, '/transfers', token);
    expect(transfers[0]?.status).toBe('REVERSED');

    const sourceReversed = await apiGetJson<AccountBalance>(
      request,
      `/accounts/${source!.id}/balance`,
      token,
    );
    const destinationReversed = await apiGetJson<AccountBalance>(
      request,
      `/accounts/${destination!.id}/balance`,
      token,
    );
    await gotoFeature(page, '/accounts', 'Contas');
    await expect(rowByText(page, sourceName)).toContainText(formatBrl(sourceReversed.totalBalance));
    await expect(rowByText(page, destinationName)).toContainText(
      formatBrl(destinationReversed.totalBalance),
    );
    expect(sourceReversed.totalBalance).toBe(opening);
    expect(destinationReversed.totalBalance).toBe(50);
  });
});
