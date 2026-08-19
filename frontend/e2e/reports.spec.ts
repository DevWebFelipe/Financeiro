import { expect, test } from '@playwright/test';
import { apiGetJson, Invoice, loginApi, registerUser } from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { todayIsoDate } from './helpers/dates';
import {
  createAccountExpense,
  createBankAccount,
  createCardExpense,
  createCategory,
  createCreditCard,
  createIncome,
  payExpense,
  receiveIncome,
} from './helpers/flows';
import { uniqueName, uniqueUser } from './helpers/identity';
import { formatBrl } from './helpers/money';
import { clickButton, gotoFeature, waitUntilNotBusy } from './helpers/ui';

test.describe('Reports and PDF', () => {
  test('consults expenses, incomes, cash flow and invoices, then downloads a real PDF', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const user = uniqueUser('rep');
    const accountName = uniqueName('Conta rel');
    const incomeCategory = uniqueName('Cat rec rel');
    const expenseCategory = uniqueName('Cat desp rel');
    const incomeName = uniqueName('Receita rel');
    const expenseName = uniqueName('Despesa rel');
    const cardName = uniqueName('Cartão rel');
    const purchaseName = uniqueName('Compra rel');
    const today = todayIsoDate();

    await registerUser(request, user);
    await loginViaUi(page, user);
    await createBankAccount(page, accountName, 800);
    await createCategory(page, incomeCategory, 'INCOME');
    await createCategory(page, expenseCategory, 'EXPENSE');
    await createIncome(page, {
      description: incomeName,
      categoryName: incomeCategory,
      amount: 120,
      expectedDate: today,
    });
    await receiveIncome(page, incomeName, accountName, 120, today);
    await createAccountExpense(page, {
      description: expenseName,
      categoryName: expenseCategory,
      accountName,
      amount: 35,
      expenseDate: today,
      dueDate: today,
    });
    await payExpense(page, expenseName, accountName, 35, today);
    await createCreditCard(page, {
      name: cardName,
      holderName: user.name,
      creditLimit: 1500,
      closingDay: 10,
      dueDay: 20,
    });
    await createCardExpense(page, {
      description: purchaseName,
      categoryName: expenseCategory,
      cardName,
      amount: 80,
      expenseDate: today,
      dueDate: today,
    });

    await consultReport(page, 'expenses');
    await expect(page.getByText(expenseName)).toBeVisible();
    await expect(page.getByText(formatBrl(35)).first()).toBeVisible();

    await consultReport(page, 'incomes');
    await expect(page.getByText(incomeName)).toBeVisible();
    await expect(page.getByText(formatBrl(120)).first()).toBeVisible();

    await consultReport(page, 'cash-flow');
    await waitUntilNotBusy(page);
    await expect(page.getByRole('heading', { name: 'Relatórios' })).toBeVisible();

    const token = await loginApi(request, user);
    const cards = await apiGetJson<Array<{ id: string }>>(request, '/credit-cards', token);
    const invoices = await apiGetJson<Invoice[]>(
      request,
      `/credit-cards/${cards[0]!.id}/invoices`,
      token,
    );
    expect(invoices[0]).toBeTruthy();

    await page.locator('#report-type').selectOption('invoices');
    await page.locator('#filter-invoice-id').fill(invoices[0]!.id);
    await clickButton(page, 'Consultar');
    await waitUntilNotBusy(page);
    await expect(page.getByText(purchaseName).or(page.getByText(formatBrl(80))).first()).toBeVisible();

    const downloadPromise = page.waitForEvent('download');
    await page.locator('#report-type').selectOption('expenses');
    await page.locator('#filter-start-date').fill(today);
    await page.locator('#filter-end-date').fill(today);
    await clickButton(page, 'Baixar PDF');
    const download = await downloadPromise;
    const suggested = download.suggestedFilename();
    expect(suggested.toLowerCase()).toMatch(/\.pdf$/);
    const stream = await download.createReadStream();
    expect(stream).toBeTruthy();
    const chunks: Buffer[] = [];
    for await (const chunk of stream!) {
      chunks.push(Buffer.from(chunk));
    }
    const bytes = Buffer.concat(chunks);
    expect(bytes.byteLength).toBeGreaterThan(100);
    expect(bytes.subarray(0, 4).toString('utf8')).toBe('%PDF');
  });
});

async function consultReport(page: import('@playwright/test').Page, type: string): Promise<void> {
  const today = todayIsoDate();
  await gotoFeature(page, '/reports', 'Relatórios');
  await page.locator('#report-type').selectOption(type);
  if (await page.locator('#filter-start-date').isVisible()) {
    await page.locator('#filter-start-date').fill(today);
    await page.locator('#filter-end-date').fill(today);
  }
  await clickButton(page, 'Consultar');
  await waitUntilNotBusy(page);
}
