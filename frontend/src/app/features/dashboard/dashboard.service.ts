import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parseDashboardResponse } from './dashboard-parse';
import { DashboardResponse } from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  getDashboard(): Observable<DashboardResponse> {
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, '/dashboard')).pipe(
      map((body) => {
        const parsed = parseDashboardResponse(body);
        if (parsed == null) {
          throw new Error('Dashboard response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }
}
