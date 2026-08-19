import { HttpInterceptorFn } from '@angular/common/http';
import { authInterceptor } from '../auth/auth.interceptor';
import { apiErrorInterceptor } from './api-error.interceptor';

/**
 * First interceptor is outermost.
 * Request: auth (Authorization) → error interceptor → backend.
 * Error response: backend → error normalization → auth (401 session).
 */
export const httpInterceptors: HttpInterceptorFn[] = [authInterceptor, apiErrorInterceptor];
