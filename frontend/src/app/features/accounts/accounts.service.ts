import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parseAccount, parseAccountBalance, parseAccountList } from './accounts-parse';
import {
  Account,
  AccountBalance,
  AccountWithBalance,
  CreateAccountRequest,
  UpdateAccountRequest,
} from './accounts.models';

@Injectable({ providedIn: 'root' })
export class AccountsService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(): Observable<Account[]> {
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, '/accounts')).pipe(
      map((body) => {
        const parsed = parseAccountList(body);
        if (parsed == null) {
          throw new Error('Accounts response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  getBalance(accountId: string): Observable<AccountBalance> {
    const path = `/accounts/${encodeURIComponent(accountId)}/balance`;
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, path)).pipe(
      map((body) => {
        const parsed = parseAccountBalance(body);
        if (parsed == null) {
          throw new Error('Account balance response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  listWithBalances(): Observable<AccountWithBalance[]> {
    return this.list().pipe(
      switchMap((accounts) => {
        if (accounts.length === 0) {
          return of([]);
        }

        return forkJoin(
          accounts.map((account) =>
            this.getBalance(account.id).pipe(map((balance) => ({ account, balance }))),
          ),
        );
      }),
    );
  }

  create(request: CreateAccountRequest): Observable<Account> {
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, '/accounts'), request)
      .pipe(
        map((body) =>
          this.requireAccount(body, 'Create account response did not match the expected contract.'),
        ),
      );
  }

  update(accountId: string, request: UpdateAccountRequest): Observable<Account> {
    return this.http
      .put<unknown>(
        joinApiUrl(this.apiBaseUrl, `/accounts/${encodeURIComponent(accountId)}`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireAccount(body, 'Update account response did not match the expected contract.'),
        ),
      );
  }

  deactivate(accountId: string): Observable<Account> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/accounts/${encodeURIComponent(accountId)}/deactivate`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireAccount(
            body,
            'Deactivate account response did not match the expected contract.',
          ),
        ),
      );
  }

  activate(accountId: string): Observable<Account> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/accounts/${encodeURIComponent(accountId)}/activate`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireAccount(
            body,
            'Activate account response did not match the expected contract.',
          ),
        ),
      );
  }

  private requireAccount(body: unknown, message: string): Account {
    const parsed = parseAccount(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }
}
