import { expect, Page } from '@playwright/test';
import { clickButton, fillNumber, gotoFeature, rowByText, selectOptionContaining, waitUntilNotBusy } from './ui';

export async function createBankAccount(
  page: Page,
  name: string,
  initialBalance?: number,
): Promise<void> {
  await gotoFeature(page, '/accounts', 'Contas');
  await clickButton(page, 'Nova conta');
  await page.locator('#account-name').fill(name);
  await page.locator('#account-type').selectOption('BANK_ACCOUNT');
  if (initialBalance != null) {
    await fillNumber(page.locator('#account-initial-balance'), initialBalance);
  }
  await clickButton(page, 'Salvar');
  await expect(rowByText(page, name)).toBeVisible();
  await waitUntilNotBusy(page);
}

export async function createCategory(page: Page, name: string, type: 'INCOME' | 'EXPENSE'): Promise<void> {
  await gotoFeature(page, '/categories', 'Categorias');
  await clickButton(page, 'Nova categoria');
  await page.locator('#category-name').fill(name);
  await page.locator('#category-type').selectOption(type);
  await clickButton(page, 'Salvar');
  await expect(rowByText(page, name)).toBeVisible();
  await waitUntilNotBusy(page);
}

export async function createIncome(
  page: Page,
  options: {
    description: string;
    categoryName: string;
    amount: number;
    expectedDate: string;
  },
): Promise<void> {
  await gotoFeature(page, '/incomes', 'Receitas');
  await clickButton(page, 'Nova receita');
  await page.locator('#income-category').selectOption({ label: options.categoryName });
  await page.locator('#income-description').fill(options.description);
  await fillNumber(page.locator('#income-amount'), options.amount);
  await page.locator('#income-expected-date').fill(options.expectedDate);
  await clickButton(page, 'Salvar');
  await expect(rowByText(page, options.description)).toBeVisible();
  await waitUntilNotBusy(page);
}

export async function receiveIncome(
  page: Page,
  description: string,
  accountName: string,
  amount: number,
  date: string,
): Promise<void> {
  await gotoFeature(page, '/incomes', 'Receitas');
  await rowByText(page, description).getByRole('button', { name: 'Receber' }).click();
  await expect(page.getByRole('heading', { name: 'Receber receita' })).toBeVisible();
  await page.locator('#receive-account').selectOption({ label: accountName });
  await fillNumber(page.locator('#receive-amount'), amount);
  await page.locator('#receive-date').fill(date);
  await clickButton(page, 'Confirmar recebimento');
  await expect(rowByText(page, description)).toContainText('Recebida');
  await waitUntilNotBusy(page);
}

export async function createAccountExpense(
  page: Page,
  options: {
    description: string;
    categoryName: string;
    accountName: string;
    amount: number;
    expenseDate: string;
    dueDate: string;
  },
): Promise<void> {
  await gotoFeature(page, '/expenses', 'Despesas');
  await clickButton(page, 'Nova despesa');
  await page.locator('#expense-category').selectOption({ label: options.categoryName });
  await page.locator('#expense-description').fill(options.description);
  await fillNumber(page.locator('#expense-total'), options.amount);
  await page.locator('#expense-date').fill(options.expenseDate);
  await page.locator('#expense-due-date').fill(options.dueDate);
  await page.locator('#expense-payment-method').selectOption('ACCOUNT');
  await page.locator('#expense-account').selectOption({ label: options.accountName });
  await page.locator('#expense-responsible').selectOption('MINE');
  await clickButton(page, 'Salvar');
  await expect(rowByText(page, options.description)).toBeVisible();
  await waitUntilNotBusy(page);
}

export async function payExpense(
  page: Page,
  description: string,
  accountName: string,
  amount: number,
  paymentDate: string,
): Promise<void> {
  await gotoFeature(page, '/expenses', 'Despesas');
  await rowByText(page, description).getByRole('button', { name: 'Pagar' }).click();
  await expect(page.getByRole('heading', { name: 'Pagar despesa' })).toBeVisible();
  await page.locator('#pay-account').selectOption({ label: accountName });
  await fillNumber(page.locator('#pay-amount'), amount);
  await page.locator('#pay-date').fill(paymentDate);
  await clickButton(page, 'Confirmar pagamento');
  await expect(rowByText(page, description)).toContainText('Paga');
  await waitUntilNotBusy(page);
}

export async function createCreditCard(
  page: Page,
  options: {
    name: string;
    holderName: string;
    creditLimit: number;
    closingDay: number;
    dueDay: number;
  },
): Promise<void> {
  await gotoFeature(page, '/credit-cards', 'Cartões');
  await clickButton(page, 'Novo cartão');
  await page.locator('#credit-card-name').fill(options.name);
  await page.locator('#credit-card-holder-name').fill(options.holderName);
  await fillNumber(page.locator('#credit-card-limit'), options.creditLimit);
  await fillNumber(page.locator('#credit-card-closing-day'), options.closingDay);
  await fillNumber(page.locator('#credit-card-due-day'), options.dueDay);
  await clickButton(page, 'Salvar');
  await expect(page.getByRole('heading', { name: options.name })).toBeVisible();
  await waitUntilNotBusy(page);
}

export async function createCardExpense(
  page: Page,
  options: {
    description: string;
    categoryName: string;
    cardName: string;
    amount: number;
    expenseDate: string;
    dueDate: string;
  },
): Promise<void> {
  await gotoFeature(page, '/expenses', 'Despesas');
  await clickButton(page, 'Nova despesa');
  await page.locator('#expense-category').selectOption({ label: options.categoryName });
  await page.locator('#expense-description').fill(options.description);
  await fillNumber(page.locator('#expense-total'), options.amount);
  await page.locator('#expense-date').fill(options.expenseDate);
  await page.locator('#expense-due-date').fill(options.dueDate);
  await page.locator('#expense-payment-method').selectOption('CREDIT_CARD');
  await selectOptionContaining(page.locator('#expense-credit-card'), options.cardName);
  await page.locator('#expense-responsible').selectOption('MINE');
  await clickButton(page, 'Salvar');
  await expect(rowByText(page, options.description)).toBeVisible();
  await waitUntilNotBusy(page);
}

export async function openInvoiceDetail(page: Page, cardName: string): Promise<void> {
  await gotoFeature(page, '/invoices', 'Faturas');
  await selectOptionContaining(page.locator('#filter-card'), cardName);
  await waitUntilNotBusy(page);
  await page.getByRole('button', { name: 'Detalhes' }).first().click();
  await expect(page.getByRole('heading', { name: 'Detalhes da fatura' })).toBeVisible();
  await waitUntilNotBusy(page);
}
