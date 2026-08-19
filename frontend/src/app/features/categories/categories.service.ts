import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import { parseCategory, parseCategoryList } from './categories-parse';
import {
  Category,
  CategoryListParams,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from './categories.models';

@Injectable({ providedIn: 'root' })
export class CategoriesService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(params: CategoryListParams = {}): Observable<Category[]> {
    let httpParams = new HttpParams();
    if (params.type != null) {
      httpParams = httpParams.set('type', params.type);
    }
    if (params.active != null) {
      httpParams = httpParams.set('active', String(params.active));
    }

    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, '/categories'), { params: httpParams })
      .pipe(
        map((body) => {
          const parsed = parseCategoryList(body);
          if (parsed == null) {
            throw new Error('Categories response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  create(request: CreateCategoryRequest): Observable<Category> {
    return this.http
      .post<unknown>(joinApiUrl(this.apiBaseUrl, '/categories'), request)
      .pipe(
        map((body) =>
          this.requireCategory(
            body,
            'Create category response did not match the expected contract.',
          ),
        ),
      );
  }

  update(categoryId: string, request: UpdateCategoryRequest): Observable<Category> {
    return this.http
      .put<unknown>(
        joinApiUrl(this.apiBaseUrl, `/categories/${encodeURIComponent(categoryId)}`),
        request,
      )
      .pipe(
        map((body) =>
          this.requireCategory(
            body,
            'Update category response did not match the expected contract.',
          ),
        ),
      );
  }

  deactivate(categoryId: string): Observable<Category> {
    return this.http
      .post<unknown>(
        joinApiUrl(this.apiBaseUrl, `/categories/${encodeURIComponent(categoryId)}/deactivate`),
        {},
      )
      .pipe(
        map((body) =>
          this.requireCategory(
            body,
            'Deactivate category response did not match the expected contract.',
          ),
        ),
      );
  }

  private requireCategory(body: unknown, message: string): Category {
    const parsed = parseCategory(body);
    if (parsed == null) {
      throw new Error(message);
    }
    return parsed;
  }
}
