import { APIRequestContext, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { apiGetJson, Invoice } from './api';
import { todayIsoDate } from './dates';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function repoRoot(): string {
  return path.resolve(process.cwd(), '..');
}

/**
 * Simulates the official invoice scheduler (`closeDueInvoices`) for a single
 * invoice whose `closingDate` is already on or before today. There is no HTTP
 * close endpoint in V1 (`POST /invoices/{id}/close` is out of scope).
 */
export async function closeInvoiceIfDue(
  request: APIRequestContext,
  token: string,
  invoiceId: string,
): Promise<Invoice> {
  if (!UUID.test(invoiceId)) {
    throw new Error(`Invalid invoice id: ${invoiceId}`);
  }

  const before = await apiGetJson<Invoice>(request, `/invoices/${invoiceId}`, token);
  if (before.status === 'CLOSED') {
    return before;
  }

  const today = todayIsoDate();
  expect(
    before.closingDate <= today,
    `Invoice ${invoiceId} is not due to close yet (closingDate=${before.closingDate}, today=${today}).`,
  ).toBeTruthy();
  expect(before.status).toBe('OPEN');

  const db = process.env['POSTGRES_DB'] ?? 'financial_control';
  const user = process.env['POSTGRES_USER'] ?? 'financial_control';
  const sql =
    `UPDATE credit_card_invoices ` +
    `SET status = 'CLOSED', updated_at = NOW() ` +
    `WHERE id = '${invoiceId}' ` +
    `AND status = 'OPEN' ` +
    `AND closing_date <= (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date;`;

  execFileSync(
    'docker',
    ['compose', 'exec', '-T', 'postgres', 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-c', sql],
    { cwd: repoRoot(), stdio: 'pipe', encoding: 'utf8' },
  );

  const after = await apiGetJson<Invoice>(request, `/invoices/${invoiceId}`, token);
  expect(after.status, JSON.stringify(after)).toBe('CLOSED');
  return after;
}
