import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parsePayablePage } from './payables-parse';
import { PayableListParams, PayablePage } from './payables.models';

@Injectable({ providedIn: 'root' })
export class PayablesService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(params: PayableListParams = {}): Observable<PayablePage> {
    let httpParams = new HttpParams();
    if (params.startDate != null) {
      httpParams = httpParams.set('startDate', params.startDate);
    }
    if (params.endDate != null) {
      httpParams = httpParams.set('endDate', params.endDate);
    }
    if (params.year != null) {
      httpParams = httpParams.set('year', String(params.year));
    }
    if (params.month != null) {
      httpParams = httpParams.set('month', String(params.month));
    }
    if (params.includeWithoutDueDate === true) {
      httpParams = httpParams.set('includeWithoutDueDate', 'true');
    }
    if (params.status != null) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.overdue != null) {
      httpParams = httpParams.set('overdue', String(params.overdue));
    }
    if (params.withoutCreditCard === true) {
      httpParams = httpParams.set('withoutCreditCard', 'true');
    }
    if (params.categoryId != null) {
      httpParams = httpParams.set('categoryId', params.categoryId);
    }
    if (params.responsibleType != null) {
      httpParams = httpParams.set('responsibleType', params.responsibleType);
    }
    if (params.search != null) {
      httpParams = httpParams.set('search', params.search);
    }
    if (params.sort != null) {
      httpParams = httpParams.set('sort', params.sort);
    }
    if (params.direction != null) {
      httpParams = httpParams.set('direction', params.direction);
    }
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/payables'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parsePayablePage(body);
          if (parsed == null) {
            throw new Error('Payables response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }
}
