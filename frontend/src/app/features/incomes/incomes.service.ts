import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import {
  parseIncome,
  parseIncomeMovement,
  parseIncomeMovementPage,
  parseIncomePage,
} from './incomes-parse';
import {
  CreateIncomeReceiptRequest,
  CreateIncomeRequest,
  Income,
  IncomeListParams,
  IncomeMovement,
  IncomeMovementPage,
  IncomePage,
  UpdateIncomeRequest,
} from './incomes.models';

@Injectable({ providedIn: 'root' })
export class IncomesService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(params: IncomeListParams = {}): Observable<IncomePage> {
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
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/incomes'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parseIncomePage(body);
          if (parsed == null) {
            throw new Error('Incomes response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  get(incomeId: string): Observable<Income> {
    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, `/incomes/${encodeURIComponent(incomeId)}`))
      .pipe(
        map((body) =>
          this.requireIncome(body, 'Income response did not match the expected contract.'),
        ),
      );
  }

  create(request: CreateIncomeRequest): Observable<Income> {
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, '/incomes'), request)
      .pipe(
        map((body) =>
          this.requireIncome(body, 'Create income response did not match the expected contract.'),
        ),
      );
  }

  update(incomeId: string, request: UpdateIncomeRequest): Observable<Income> {
    return this.http
      .put<unknown>(
        joinApiUrl(this.apiBaseUrl, `/incomes/${encodeURIComponent(incomeId)}`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireIncome(body, 'Update income response did not match the expected contract.'),
        ),
      );
  }

  cancel(incomeId: string): Observable<Income> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/incomes/${encodeURIComponent(incomeId)}/cancel`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireIncome(body, 'Cancel income response did not match the expected contract.'),
        ),
      );
  }

  createReceipt(incomeId: string, request: CreateIncomeReceiptRequest): Observable<IncomeMovement> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/incomes/${encodeURIComponent(incomeId)}/receipts`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireMovement(
            body,
            'Create receipt response did not match the expected contract.',
          ),
        ),
      );
  }

  listMovements(incomeId: string, page = 0, size = 20): Observable<IncomeMovementPage> {
    const httpParams = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http
      .get<unknown>(
        joinApiUrl(this.apiBaseUrl, `/incomes/${encodeURIComponent(incomeId)}/movements`),
        { params: httpParams },
      )
      .pipe(
        map((body) => {
          const parsed = parseIncomeMovementPage(body);
          if (parsed == null) {
            throw new Error('Income movements response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  reverseMovement(incomeId: string, movementId: string): Observable<IncomeMovement> {
    const path = `/incomes/${encodeURIComponent(incomeId)}/movements/${encodeURIComponent(movementId)}/reverse`;
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, path), {})
      .pipe(
        map((body) =>
          this.requireMovement(
            body,
            'Reverse movement response did not match the expected contract.',
          ),
        ),
      );
  }

  private requireIncome(body: unknown, message: string): Income {
    const parsed = parseIncome(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }

  private requireMovement(body: unknown, message: string): IncomeMovement {
    const parsed = parseIncomeMovement(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }
}
