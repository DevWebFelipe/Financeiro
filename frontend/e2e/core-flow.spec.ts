import { expect, test } from '@playwright/test';
import { Account, AccountBalance, apiGetJson, loginApi, registerUser } from './helpers/api';
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
import { gotoFeature, rowByText } from './helpers/ui';

test.describe('Core cash flow', () => {
  test('account → income receipt → expense payment → official balances', async ({
    page,
    request,
  }) => {
    const user = uniqueUser('core');
    const accountName = uniqueName('Conta core');
    const incomeCategory = uniqueName('Cat receita');
    const expenseCategory = uniqueName('Cat despesa');
    const incomeDescription = uniqueName('Salário');
    const expenseDescription = uniqueName('Aluguel');
    const today = todayIsoDate();
    const incomeAmount = 1000;
    const expenseAmount = 250;

    await registerUser(request, user);
    await loginViaUi(page, user);

    await createBankAccount(page, accountName);
    await createCategory(page, incomeCategory, 'INCOME');
    await createCategory(page, expenseCategory, 'EXPENSE');
    await createIncome(page, {
      description: incomeDescription,
      categoryName: incomeCategory,
      amount: incomeAmount,
      expectedDate: today,
    });
    await receiveIncome(page, incomeDescription, accountName, incomeAmount, today);

    await gotoFeature(page, '/accounts', 'Contas');
    const afterIncome = rowByText(page, accountName);
    await expect(afterIncome).toContainText(formatBrl(incomeAmount));

    await createAccountExpense(page, {
      description: expenseDescription,
      categoryName: expenseCategory,
      accountName,
      amount: expenseAmount,
      expenseDate: today,
      dueDate: today,
    });
    await payExpense(page, expenseDescription, accountName, expenseAmount, today);

    const token = await loginApi(request, user);
    const accounts = await apiGetJson<Account[]>(request, '/accounts', token);
    const account = accounts.find((item) => item.name === accountName);
    expect(account).toBeTruthy();
    const balance = await apiGetJson<AccountBalance>(
      request,
      `/accounts/${account!.id}/balance`,
      token,
    );

    await gotoFeature(page, '/accounts', 'Contas');
    const row = rowByText(page, accountName);
    await expect(row).toContainText(formatBrl(balance.totalBalance));
    await expect(row).toContainText(formatBrl(balance.availableBalance));
    await expect(row).toContainText(formatBrl(balance.reservedAmount));
    expect(balance.totalBalance).toBe(incomeAmount - expenseAmount);
  });
});
