import { expect, test } from '@playwright/test';
import { loginViaUi, logoutViaUi, registerViaUi, expectSessionTokenAbsent, expectSessionTokenPresent } from './helpers/auth';
import { uniqueUser } from './helpers/identity';
import { attachRequestObserver, gotoFeature } from './helpers/ui';

test.describe('Authentication', () => {
  test('registers, logs in, reaches a protected route, logs out and logs in again', async ({
    page,
  }) => {
    const observer = attachRequestObserver(page);
    const user = uniqueUser('auth');

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);
    await expectSessionTokenAbsent(page);

    await registerViaUi(page, user);
    await loginViaUi(page, user);
    await expectSessionTokenPresent(page);
    await expect(page.getByText(user.name)).toBeVisible();

    await gotoFeature(page, '/accounts', 'Contas');
    await expect(page).toHaveURL(/\/accounts/);
    await expect(page.getByText('Nenhuma conta cadastrada.')).toBeVisible();

    await logoutViaUi(page);
    await expectSessionTokenAbsent(page);

    await page.goto('/accounts');
    await expect(page).toHaveURL(/\/login/);

    await loginViaUi(page, user);
    await expect(page).toHaveURL(/\/dashboard/);
    await expectSessionTokenPresent(page);

    const loops = observer.duplicates(20);
    expect(loops, loops.join('\n')).toEqual([]);
  });
});
