import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import {
  parseCreateGoalContributionResult,
  parseCreateGoalRedemptionResult,
  parseFinancialGoal,
  parseFinancialGoalPage,
  parseGoalContributionList,
  parseGoalRedemptionList,
} from './financial-goals-parse';
import {
  CreateFinancialGoalRequest,
  CreateGoalContributionRequest,
  CreateGoalContributionResult,
  CreateGoalRedemptionRequest,
  CreateGoalRedemptionResult,
  FinancialGoal,
  FinancialGoalListParams,
  FinancialGoalPage,
  GoalContribution,
  GoalRedemption,
  UpdateFinancialGoalRequest,
} from './financial-goals.models';

@Injectable({ providedIn: 'root' })
export class FinancialGoalsService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(params: FinancialGoalListParams = {}): Observable<FinancialGoalPage> {
    let httpParams = new HttpParams();
    if (params.status != null) {
      httpParams = httpParams.set('status', params.status);
    }
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/financial-goals'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parseFinancialGoalPage(body);
          if (parsed == null) {
            throw new Error('Financial goals response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  get(goalId: string): Observable<FinancialGoal> {
    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}`))
      .pipe(
        map((body) =>
          this.requireGoal(body, 'Financial goal response did not match the expected contract.'),
        ),
      );
  }

  create(request: CreateFinancialGoalRequest): Observable<FinancialGoal> {
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, '/financial-goals'), request)
      .pipe(
        map((body) =>
          this.requireGoal(
            body,
            'Create financial goal response did not match the expected contract.',
          ),
        ),
      );
  }

  update(goalId: string, request: UpdateFinancialGoalRequest): Observable<FinancialGoal> {
    return this.http
      .put<unknown>(
        joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireGoal(
            body,
            'Update financial goal response did not match the expected contract.',
          ),
        ),
      );
  }

  contribute(
    goalId: string,
    request: CreateGoalContributionRequest,
  ): Observable<CreateGoalContributionResult> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}/contributions`),
        request,
      )
      .pipe(
        map((body) => {
          const parsed = parseCreateGoalContributionResult(body);
          if (parsed == null) {
            throw new Error('Create contribution response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  listContributions(goalId: string): Observable<GoalContribution[]> {
    return this.http
      .get<unknown>(
        joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}/contributions`),
      )
      .pipe(
        map((body) => {
          const parsed = parseGoalContributionList(body);
          if (parsed == null) {
            throw new Error('Goal contributions response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  redeem(
    goalId: string,
    request: CreateGoalRedemptionRequest,
  ): Observable<CreateGoalRedemptionResult> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}/redemptions`),
        request,
      )
      .pipe(
        map((body) => {
          const parsed = parseCreateGoalRedemptionResult(body);
          if (parsed == null) {
            throw new Error('Create redemption response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  listRedemptions(goalId: string): Observable<GoalRedemption[]> {
    return this.http
      .get<unknown>(
        joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}/redemptions`),
      )
      .pipe(
        map((body) => {
          const parsed = parseGoalRedemptionList(body);
          if (parsed == null) {
            throw new Error('Goal redemptions response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  complete(goalId: string): Observable<FinancialGoal> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}/complete`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireGoal(
            body,
            'Complete financial goal response did not match the expected contract.',
          ),
        ),
      );
  }

  cancel(goalId: string): Observable<FinancialGoal> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/financial-goals/${encodeURIComponent(goalId)}/cancel`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireGoal(
            body,
            'Cancel financial goal response did not match the expected contract.',
          ),
        ),
      );
  }

  private requireGoal(body: unknown, message: string): FinancialGoal {
    const parsed = parseFinancialGoal(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }
}
