import { expect, Locator, Page } from '@playwright/test';

export async function waitForHeading(page: Page, name: string): Promise<void> {
  await expect(page.getByRole('heading', { level: 1, name })).toBeVisible();
}

export async function waitUntilNotBusy(page: Page): Promise<void> {
  await expect(page.locator('[aria-busy="true"]')).toHaveCount(0, { timeout: 20_000 });
}

export async function openDesktopNav(page: Page, label: string): Promise<void> {
  const sidebar = page.locator('.shell__sidebar');
  if (await sidebar.isVisible()) {
    await sidebar.getByRole('link', { name: label, exact: true }).click();
    return;
  }
  await page.getByRole('button', { name: /Abrir menu/ }).click();
  await page.locator('#mobile-nav').getByRole('link', { name: label, exact: true }).click();
}

export async function gotoFeature(page: Page, path: string, heading: string): Promise<void> {
  await page.goto(path);
  await waitForHeading(page, heading);
  await waitUntilNotBusy(page);
}

export async function selectOptionContaining(locator: Locator, text: string): Promise<void> {
  const option = locator.locator('option').filter({ hasText: text }).first();
  const value = await option.getAttribute('value');
  if (value == null || value.length === 0) {
    throw new Error(`No select option containing "${text}"`);
  }
  await locator.selectOption(value);
}

export async function clickButton(page: Page | Locator, name: string): Promise<void> {
  await page.getByRole('button', { name, exact: true }).click();
}

export function rowByText(page: Page, text: string): Locator {
  return page.locator('tr', { hasText: text }).first();
}

export async function fillNumber(locator: Locator, value: number): Promise<void> {
  await locator.fill(String(value));
}

export async function selectByLabel(locator: Locator, label: string): Promise<void> {
  await locator.selectOption({ label });
}

export async function expectNoErrorBanner(page: Page): Promise<void> {
  await expect(page.locator('app-error-state')).toHaveCount(0);
}

export function attachRequestObserver(page: Page): {
  duplicates: (threshold?: number) => string[];
} {
  const counts = new Map<string, number>();
  page.on('request', (request) => {
    const type = request.resourceType();
    if (type !== 'xhr' && type !== 'fetch') {
      return;
    }
    const key = `${request.method()} ${request.url()}`;
    counts.set(key, (counts.get(key) ?? 0) + 1);
  });
  return {
    duplicates(threshold = 12): string[] {
      return [...counts.entries()]
        .filter(([, count]) => count >= threshold)
        .map(([url, count]) => `${count}x ${url}`);
    },
  };
}
