import { HttpInterceptorFn } from '@angular/common/http';
import { apiErrorInterceptor } from './api-error.interceptor';

/**
 * First interceptor is outermost (request flows down, error flows back up).
 * B3 should register the auth interceptor before this list so it can attach
 * Authorization on the way out and observe normalized 401 responses on the way back.
 * Do not add an empty auth interceptor here.
 */
export const httpInterceptors: HttpInterceptorFn[] = [apiErrorInterceptor];
