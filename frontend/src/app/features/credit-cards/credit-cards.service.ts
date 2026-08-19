import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parseCreditCard, parseCreditCardLimit, parseCreditCardList } from './credit-cards-parse';
import {
  CreateCreditCardRequest,
  CreditCard,
  CreditCardLimit,
  CreditCardListParams,
  CreditCardWithLimit,
  UpdateCreditCardRequest,
} from './credit-cards.models';

@Injectable({ providedIn: 'root' })
export class CreditCardsService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(params: CreditCardListParams = {}): Observable<CreditCard[]> {
    let httpParams = new HttpParams();
    if (params.holderName != null) {
      httpParams = httpParams.set('holderName', params.holderName);
    }

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/credit-cards'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parseCreditCardList(body);
          if (parsed == null) {
            throw new Error('Credit cards response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  get(cardId: string): Observable<CreditCard> {
    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, `/credit-cards/${encodeURIComponent(cardId)}`))
      .pipe(
        map((body) =>
          this.requireCard(body, 'Credit card response did not match the expected contract.'),
        ),
      );
  }

  getLimit(cardId: string): Observable<CreditCardLimit> {
    const path = `/credit-cards/${encodeURIComponent(cardId)}/limit`;
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, path)).pipe(
      map((body) => {
        const parsed = parseCreditCardLimit(body);
        if (parsed == null) {
          throw new Error('Credit card limit response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  listWithLimits(params: CreditCardListParams = {}): Observable<CreditCardWithLimit[]> {
    return this.list(params).pipe(
      switchMap((cards) => {
        if (cards.length === 0) {
          return of([]);
        }

        return forkJoin(
          cards.map((card) => this.getLimit(card.id).pipe(map((limit) => ({ card, limit })))),
        );
      }),
    );
  }

  create(request: CreateCreditCardRequest): Observable<CreditCard> {
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, '/credit-cards'), request)
      .pipe(
        map((body) =>
          this.requireCard(
            body,
            'Create credit card response did not match the expected contract.',
          ),
        ),
      );
  }

  update(cardId: string, request: UpdateCreditCardRequest): Observable<CreditCard> {
    return this.http
      .put<unknown>(
        joinApiUrl(this.apiBaseUrl, `/credit-cards/${encodeURIComponent(cardId)}`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireCard(
            body,
            'Update credit card response did not match the expected contract.',
          ),
        ),
      );
  }

  deactivate(cardId: string): Observable<CreditCard> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/credit-cards/${encodeURIComponent(cardId)}/deactivate`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireCard(
            body,
            'Deactivate credit card response did not match the expected contract.',
          ),
        ),
      );
  }

  activate(cardId: string): Observable<CreditCard> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/credit-cards/${encodeURIComponent(cardId)}/activate`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireCard(
            body,
            'Activate credit card response did not match the expected contract.',
          ),
        ),
      );
  }

  private requireCard(body: unknown, message: string): CreditCard {
    const parsed = parseCreditCard(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }
}
