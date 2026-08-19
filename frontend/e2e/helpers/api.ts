import { APIRequestContext, expect } from '@playwright/test';
import { API_BASE_URL } from './env';
import { E2eUser } from './identity';

export interface ApiErrorBody {
  readonly status: number;
  readonly code: string;
  readonly message: string;
}

export async function registerUser(request: APIRequestContext, user: E2eUser): Promise<void> {
  const response = await request.post(`${API_BASE_URL}/auth/register`, {
    data: { name: user.name, email: user.email, password: user.password },
  });
  expect(response.status(), await response.text()).toBe(201);
}

export async function loginApi(request: APIRequestContext, user: E2eUser): Promise<string> {
  const response = await request.post(`${API_BASE_URL}/auth/login`, {
    data: { email: user.email, password: user.password },
  });
  expect(response.status(), await response.text()).toBe(200);
  const body = (await response.json()) as { accessToken?: string; tokenType?: string };
  expect(body.tokenType).toBe('Bearer');
  expect(body.accessToken).toBeTruthy();
  return body.accessToken as string;
}

export function bearer(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

export async function apiGet(
  request: APIRequestContext,
  path: string,
  token: string,
): Promise<{ status: number; body: unknown }> {
  const response = await request.get(`${API_BASE_URL}${path}`, { headers: bearer(token) });
  const text = await response.text();
  let body: unknown = text;
  try {
    body = text.length > 0 ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { status: response.status(), body };
}

export async function apiGetJson<T>(
  request: APIRequestContext,
  path: string,
  token: string,
): Promise<T> {
  const response = await apiGet(request, path, token);
  expect(response.status, JSON.stringify(response.body)).toBe(200);
  return response.body as T;
}

export interface AccountBalance {
  readonly accountId: string;
  readonly totalBalance: number;
  readonly reservedAmount: number;
  readonly availableBalance: number;
  readonly balance?: number;
}

export interface Account {
  readonly id: string;
  readonly name: string;
  readonly type: string;
  readonly initialBalance: number;
  readonly active: boolean;
}

export interface Invoice {
  readonly id: string;
  readonly creditCardId: string;
  readonly referenceYear: number;
  readonly referenceMonth: number;
  readonly closingDate: string;
  readonly dueDate: string;
  readonly status: string;
  readonly totalAmount: number;
  readonly paidAmount: number;
  readonly remainingAmount: number;
}

export interface CreditCardLimit {
  readonly creditLimit: number;
  readonly usedLimit: number;
  readonly availableLimit: number;
}

export interface CreditCardCredit {
  readonly id: string;
  readonly amount: number;
  readonly remainingAmount: number;
  readonly reason: string;
  readonly origin: string;
}

export interface FinancialGoal {
  readonly id: string;
  readonly name: string;
  readonly currentAmount: number;
  readonly targetAmount: number;
  readonly progressPercent: number;
  readonly status: string;
}

export interface Transfer {
  readonly id: string;
  readonly sourceAccountId: string;
  readonly destinationAccountId: string;
  readonly amount: number;
  readonly status: string;
}
