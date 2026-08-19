import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parseProjectionResponse } from './projections-parse';
import { ProjectionQueryParams, ProjectionResponse } from './projections.models';

@Injectable({ providedIn: 'root' })
export class ProjectionsService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  get(params: ProjectionQueryParams = {}): Observable<ProjectionResponse> {
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
    if (params.months != null) {
      httpParams = httpParams.set('months', String(params.months));
    }
    if (params.accountId != null) {
      httpParams = httpParams.set('accountId', params.accountId);
    }
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/projections'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parseProjectionResponse(body);
          if (parsed == null) {
            throw new Error('Projections response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }
}
