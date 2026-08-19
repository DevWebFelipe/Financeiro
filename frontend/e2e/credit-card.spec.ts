import { expect, test } from '@playwright/test';
import {
  apiGetJson,
  CreditCardCredit,
  CreditCardLimit,
  Invoice,
  loginApi,
  registerUser,
} from './helpers/api';
import { loginViaUi } from './helpers/auth';
import { pastCycleCardDates, todayIsoDate } from './helpers/dates';
import {
  createBankAccount,
  createCardExpense,
  createCategory,
  createCreditCard,
  openInvoiceDetail,
} from './helpers/flows';
import { uniqueName, uniqueUser } from './helpers/identity';
import { formatBrl } from './helpers/money';
import { clickButton, fillNumber, gotoFeature, waitUntilNotBusy } from './helpers/ui';

test.describe('Credit card invoice chain', () => {
  test('card → purchase → invoice → adjustment → credit → payment → official limit', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const user = uniqueUser('card');
    const accountName = uniqueName('Conta cartão');
    const categoryName = uniqueName('Cat cartão');
    const cardName = uniqueName('Cartão');
    const purchaseName = uniqueName('Compra');
    const today = todayIsoDate();
    const cycle = pastCycleCardDates(today);
    const purchaseAmount = 400;
    const discountAmount = 20;
    const creditAmount = 30;
    const paymentAmount = 50;

    await registerUser(request, user);
    await loginViaUi(page, user);
    await createBankAccount(page, accountName, 1000);
    await createCategory(page, categoryName, 'EXPENSE');

    await createCreditCard(page, {
      name: cardName,
      holderName: user.name,
      creditLimit: 2000,
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

    await openInvoiceDetail(page, cardName);
    await expect(page.getByText(formatBrl(purchaseAmount)).first()).toBeVisible();
    await expect(page.locator('[data-status]').first()).toContainText(/Aberta|Fechada|Agendada/);

    await page.locator('#invoice-adjust-type').selectOption('DISCOUNT');
    await fillNumber(page.locator('#invoice-adjust-amount'), discountAmount);
    await page.locator('#invoice-adjust-reason').fill('Ajuste E2E de desconto');
    await clickButton(page, 'Registrar ajuste');
    await expect(page.getByText('Desconto').first()).toBeVisible();
    await expect(page.getByText(formatBrl(discountAmount)).first()).toBeVisible();

    await gotoFeature(page, '/credit-cards', 'Cartões');
    await page.getByRole('article').filter({ hasText: cardName }).getByRole('button', { name: 'Detalhes' }).click();
    await expect(page.getByRole('heading', { name: 'Detalhes do cartão' })).toBeVisible();
    await waitUntilNotBusy(page);
    await clickButton(page, 'Adicionar crédito');
    await fillNumber(page.locator('#card-credit-amount'), creditAmount);
    await page.locator('#card-credit-reason').fill('Crédito manual E2E');
    await clickButton(page, 'Salvar crédito');
    await expect(page.getByText('Crédito adicionado com sucesso.')).toBeVisible();
    await expect(page.getByText(formatBrl(creditAmount)).first()).toBeVisible();
    await expect(page.getByText('Aplicar crédito')).toHaveCount(0);

    await openInvoiceDetail(page, cardName);
    await page.locator('#invoice-pay-account').selectOption({ label: accountName });
    await fillNumber(page.locator('#invoice-pay-amount'), paymentAmount);
    await page.locator('#invoice-pay-date').fill(today);
    await clickButton(page, 'Pagar');
    await expect(page.getByText(formatBrl(paymentAmount)).first()).toBeVisible();
    await expect(page.getByText('Ativo').first()).toBeVisible();

    const token = await loginApi(request, user);
    const cards = await apiGetJson<Array<{ id: string; name: string }>>(
      request,
      '/credit-cards',
      token,
    );
    const card = cards.find((item) => item.name === cardName);
    expect(card).toBeTruthy();
    const limit = await apiGetJson<CreditCardLimit>(
      request,
      `/credit-cards/${card!.id}/limit`,
      token,
    );
    const credits = await apiGetJson<CreditCardCredit[]>(
      request,
      `/credit-cards/${card!.id}/credits`,
      token,
    );
    const invoices = await apiGetJson<Invoice[]>(
      request,
      `/credit-cards/${card!.id}/invoices`,
      token,
    );
    expect(invoices.length).toBeGreaterThan(0);
    expect(credits.length).toBeGreaterThan(0);

    await gotoFeature(page, '/credit-cards', 'Cartões');
    const cardCard = page.getByRole('article').filter({ hasText: cardName });
    await expect(cardCard).toContainText(formatBrl(limit.usedLimit));
    await expect(cardCard).toContainText(formatBrl(limit.availableLimit));
    await expect(cardCard).toContainText(formatBrl(limit.creditLimit));
  });
});
