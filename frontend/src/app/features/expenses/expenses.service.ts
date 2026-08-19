import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parseExpense, parseExpensePage, parseInstallment } from './expenses-parse';
import {
  CreateExpenseRequest,
  Expense,
  ExpenseInstallment,
  ExpenseListParams,
  ExpensePage,
  PayExpenseRequest,
  RefundExpenseRequest,
  UpdateExpenseRequest,
} from './expenses.models';

@Injectable({ providedIn: 'root' })
export class ExpensesService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(params: ExpenseListParams = {}): Observable<ExpensePage> {
    let httpParams = new HttpParams();
    if (params.startDate != null) {
      httpParams = httpParams.set('startDate', params.startDate);
    }
    if (params.endDate != null) {
      httpParams = httpParams.set('endDate', params.endDate);
    }
    if (params.status != null) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.categoryId != null) {
      httpParams = httpParams.set('categoryId', params.categoryId);
    }
    if (params.accountId != null) {
      httpParams = httpParams.set('accountId', params.accountId);
    }
    if (params.responsibleType != null) {
      httpParams = httpParams.set('responsibleType', params.responsibleType);
    }
    if (params.paymentMethod != null) {
      httpParams = httpParams.set('paymentMethod', params.paymentMethod);
    }
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/expenses'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parseExpensePage(body);
          if (parsed == null) {
            throw new Error('Expenses response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  get(expenseId: string): Observable<Expense> {
    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, `/expenses/${encodeURIComponent(expenseId)}`))
      .pipe(
        map((body) =>
          this.requireExpense(body, 'Expense response did not match the expected contract.'),
        ),
      );
  }

  create(request: CreateExpenseRequest): Observable<Expense> {
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, '/expenses'), request)
      .pipe(
        map((body) =>
          this.requireExpense(body, 'Create expense response did not match the expected contract.'),
        ),
      );
  }

  update(expenseId: string, request: UpdateExpenseRequest): Observable<Expense> {
    return this.http
      .put<unknown>(
        joinApiUrl(this.apiBaseUrl, `/expenses/${encodeURIComponent(expenseId)}`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireExpense(body, 'Update expense response did not match the expected contract.'),
        ),
      );
  }

  cancel(expenseId: string): Observable<Expense> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/expenses/${encodeURIComponent(expenseId)}/cancel`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireExpense(body, 'Cancel expense response did not match the expected contract.'),
        ),
      );
  }

  pay(expenseId: string, request: PayExpenseRequest): Observable<Expense> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/expenses/${encodeURIComponent(expenseId)}/pay`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireExpense(body, 'Pay expense response did not match the expected contract.'),
        ),
      );
  }

  payInstallment(
    expenseId: string,
    installmentId: string,
    request: PayExpenseRequest,
  ): Observable<Expense> {
    const path = `/expenses/${encodeURIComponent(expenseId)}/installments/${encodeURIComponent(installmentId)}/payments`;
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, path), request)
      .pipe(
        map((body) =>
          this.requireExpense(
            body,
            'Pay installment response did not match the expected contract.',
          ),
        ),
      );
  }

  refund(expenseId: string, request?: RefundExpenseRequest): Observable<Expense> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/expenses/${encodeURIComponent(expenseId)}/refund`),
        request ?? {},
      )
      .pipe(
        map((body) =>
          this.requireExpense(body, 'Refund expense response did not match the expected contract.'),
        ),
      );
  }

  listInstallments(expenseId: string): Observable<ExpenseInstallment[]> {
    return this.http
      .get<unknown>(
        joinApiUrl(this.apiBaseUrl, `/expenses/${encodeURIComponent(expenseId)}/installments`),
      )
      .pipe(
        map((body) => {
          if (!Array.isArray(body)) {
            throw new Error('Installments response did not match the expected contract.');
          }
          const items: ExpenseInstallment[] = [];
          for (const item of body) {
            const installment = parseInstallment(item);
            if (installment == null) {
              throw new Error('Installments response did not match the expected contract.');
            }
            items.push(installment);
          }
          return items;
        }),
      );
  }

  private requireExpense(body: unknown, message: string): Expense {
    const parsed = parseExpense(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }
}
