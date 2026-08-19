import { expect, test } from '@playwright/test';
import { apiGetJson, Invoice, loginApi, registerUser } from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { pastCycleCardDates, todayIsoDate } from './helpers/dates';
import {
  createBankAccount,
  createCardExpense,
  createCategory,
  createCreditCard,
} from './helpers/flows';
import { uniqueName, uniqueUser } from './helpers/identity';
import { closeInvoiceIfDue } from './helpers/invoice-close';
import { formatBrl } from './helpers/money';
import { clickButton, fillNumber, gotoFeature, selectOptionContaining, waitUntilNotBusy } from './helpers/ui';

test.describe('Invoice agreement', () => {
  test('closes an eligible invoice, creates an agreement and anticipates an installment', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const user = uniqueUser('agr');
    const accountName = uniqueName('Conta acordo');
    const categoryName = uniqueName('Cat acordo');
    const cardName = uniqueName('Cartão acordo');
    const purchaseName = uniqueName('Compra acordo');
    const today = todayIsoDate();
    const cycle = pastCycleCardDates(today);
    const purchaseAmount = 200;
    const installmentAmount = 100;

    await registerUser(request, user);
    await loginViaUi(page, user);
    await createBankAccount(page, accountName, 1000);
    await createCategory(page, categoryName, 'EXPENSE');
    await createCreditCard(page, {
      name: cardName,
      holderName: user.name,
      creditLimit: 3000,
      closingDay: cycle.closingDay,
      dueDay: cycle.dueDay,
    });
    await createCardExpense(page, {
      description: purchaseName,
      categoryName,
      cardName,
      amount: purchaseAmount,
      expenseDate: cycle.purchaseDate,
      dueDate: today,
    });

    const token = await loginApi(request, user);
    const cards = await apiGetJson<Array<{ id: string; name: string }>>(
      request,
      '/credit-cards',
      token,
    );
    const card = cards.find((item) => item.name === cardName);
    expect(card).toBeTruthy();
    const invoices = await apiGetJson<Invoice[]>(
      request,
      `/credit-cards/${card!.id}/invoices`,
      token,
    );
    const source = invoices.find((item) => item.remainingAmount > 0);
    expect(source).toBeTruthy();
    const closed = await closeInvoiceIfDue(request, token, source!.id);

    await gotoFeature(page, '/invoices', 'Faturas');
    await selectOptionContaining(page.locator('#filter-card'), cardName);
    await page.locator('#filter-status').selectOption('CLOSED');
    await waitUntilNotBusy(page);
    await page.getByRole('button', { name: 'Detalhes' }).first().click();
    await expect(page.getByRole('heading', { name: 'Detalhes da fatura' })).toBeVisible();
    await waitUntilNotBusy(page);
    await expect(page.locator('.detail-list')).toContainText('Fechada');
    await expect(page.getByText(formatBrl(closed.remainingAmount)).first()).toBeVisible();

    await clickButton(page, 'Nova negociação');
    await fillNumber(page.locator('#invoice-agreement-entry'), 0);
    await page.locator('#invoice-agreement-account').selectOption({ label: accountName });
    await page.locator('#invoice-agreement-date').fill(today);
    await fillNumber(page.locator('#invoice-agreement-count'), 2);
    await fillNumber(page.locator('#invoice-agreement-installment'), installmentAmount);
    await page
      .locator('#invoice-agreement-entry')
      .locator('xpath=ancestor::form')
      .getByRole('button', { name: 'Continuar' })
      .click();
    await expect(page.getByRole('heading', { name: 'Confirmar negociação?' })).toBeVisible();
    await clickButton(page, 'Confirmar');
    await expect(page.getByText('Acordo criado com sucesso.')).toBeVisible();
    await expect(page.locator('.agreement').first()).toBeVisible();
    await expect(page.locator('.agreement').getByText('Ativo').first()).toBeVisible();
    await expect(page.getByText(formatBrl(installmentAmount)).first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Pagar parcela' }).first()).toBeVisible();

    await page.getByRole('button', { name: 'Pagar parcela' }).first().click();
    await expect(page.getByRole('heading', { name: 'Pagar parcela' })).toBeVisible();
    await page.locator('#invoice-installment-account').selectOption({ label: accountName });
    await fillNumber(page.locator('#invoice-installment-amount'), installmentAmount);
    await page.locator('#invoice-installment-date').fill(today);
    await page.locator('#invoice-installment-settled').check();
    await page
      .locator('#invoice-installment-account')
      .locator('xpath=ancestor::form')
      .getByRole('button', { name: 'Pagar parcela' })
      .click();
    await expect(page.getByRole('heading', { name: 'Confirmar quitação da parcela?' })).toBeVisible();
    await clickButton(page, 'Confirmar quitação');
    await waitUntilNotBusy(page);
    await expect(page.getByText('Paga').first()).toBeVisible();
  });
});
