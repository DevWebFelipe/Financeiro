import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { isApiError } from '../errors/api-error';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();
  const authorizedRequest =
    token != null && token.length > 0
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (isApiError(error) && error.status === 401 && !isCredentialRequest(req.url)) {
        auth.handleUnauthorized();
      }
      return throwError(() => error);
    }),
  );
};

function isCredentialRequest(url: string): boolean {
  return url.includes('/auth/login') || url.includes('/auth/register');
}
