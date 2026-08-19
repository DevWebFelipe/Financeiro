import { inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs';
import { AuthStatus } from './auth.models';
import { AuthService } from './auth.service';
import { toSafeInternalUrl } from './internal-url';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return waitForSettledStatus(auth).pipe(
    map((status) => {
      if (status === 'authenticated') {
        return true;
      }

      const returnUrl = toSafeInternalUrl(state.url);
      return router.createUrlTree(
        ['/login'],
        returnUrl ? { queryParams: { returnUrl } } : undefined,
      );
    }),
  );
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return waitForSettledStatus(auth).pipe(
    map((status) => (status === 'authenticated' ? router.createUrlTree(['/dashboard']) : true)),
  );
};

function waitForSettledStatus(auth: AuthService) {
  return toObservable(auth.status).pipe(
    filter((status): status is Exclude<AuthStatus, 'loading'> => status !== 'loading'),
    take(1),
  );
}
