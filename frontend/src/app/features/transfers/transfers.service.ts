import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parseTransfer, parseTransferList } from './transfers-parse';
import { CreateTransferRequest, Transfer, TransferListParams } from './transfers.models';

@Injectable({ providedIn: 'root' })
export class TransfersService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(params: TransferListParams = {}): Observable<Transfer[]> {
    let httpParams = new HttpParams();
    if (params.startDate != null) {
      httpParams = httpParams.set('startDate', params.startDate);
    }
    if (params.endDate != null) {
      httpParams = httpParams.set('endDate', params.endDate);
    }
    if (params.accountId != null) {
      httpParams = httpParams.set('accountId', params.accountId);
    }

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/transfers'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parseTransferList(body);
          if (parsed == null) {
            throw new Error('Transfers response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  get(transferId: string): Observable<Transfer> {
    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, `/transfers/${encodeURIComponent(transferId)}`))
      .pipe(
        map((body) =>
          this.requireTransfer(body, 'Transfer response did not match the expected contract.'),
        ),
      );
  }

  create(request: CreateTransferRequest): Observable<Transfer> {
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, '/transfers'), request)
      .pipe(
        map((body) =>
          this.requireTransfer(
            body,
            'Create transfer response did not match the expected contract.',
          ),
        ),
      );
  }

  reverse(transferId: string): Observable<Transfer> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/transfers/${encodeURIComponent(transferId)}/reverse`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireTransfer(
            body,
            'Reverse transfer response did not match the expected contract.',
          ),
        ),
      );
  }

  private requireTransfer(body: unknown, message: string): Transfer {
    const parsed = parseTransfer(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }
}
