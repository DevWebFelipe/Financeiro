import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { normalizeHttpError } from '../errors/normalize-http-error';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: unknown) => throwError(() => normalizeHttpError(error, req.url))),
  );
};
