import { expect, Page } from '@playwright/test';
import { E2eUser } from './identity';

export async function registerViaUi(page: Page, user: E2eUser): Promise<void> {
  await page.goto('/register');
  await expect(page.getByRole('heading', { name: 'Criar conta' })).toBeVisible();
  await page.locator('#name').fill(user.name);
  await page.locator('#email').fill(user.email);
  await page.locator('#password').fill(user.password);
  await page.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();
}

export async function loginViaUi(page: Page, user: E2eUser): Promise<void> {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();
  await page.locator('#email').fill(user.email);
  await page.locator('#password').fill(user.password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
}

export async function logoutViaUi(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();
}

export async function expectSessionTokenPresent(page: Page): Promise<void> {
  const token = await page.evaluate(() => sessionStorage.getItem('fc.auth.accessToken'));
  expect(token).toBeTruthy();
}

export async function expectSessionTokenAbsent(page: Page): Promise<void> {
  const token = await page.evaluate(() => sessionStorage.getItem('fc.auth.accessToken'));
  expect(token).toBeNull();
}
