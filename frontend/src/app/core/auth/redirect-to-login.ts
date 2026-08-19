import { Router } from '@angular/router';
import { isAuthPath, toSafeInternalUrl } from './internal-url';

export function redirectToLogin(router: Router, returnUrl?: string): Promise<boolean> {
  if (isAuthPath(router.url)) {
    return Promise.resolve(true);
  }

  const safeReturnUrl = toSafeInternalUrl(returnUrl);
  return router.navigate(
    ['/login'],
    safeReturnUrl ? { queryParams: { returnUrl: safeReturnUrl } } : {},
  );
}
