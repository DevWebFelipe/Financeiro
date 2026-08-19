import { Injectable } from '@angular/core';

export const AUTH_TOKEN_KEY = 'fc.auth.accessToken';

@Injectable({ providedIn: 'root' })
export class AuthTokenStore {
  read(): string | null {
    try {
      const value = sessionStorage.getItem(AUTH_TOKEN_KEY);
      return value != null && value.length > 0 ? value : null;
    } catch {
      return null;
    }
  }

  write(token: string): void {
    try {
      sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    } catch {
      // Private mode / quota: keep the token only in AuthService memory.
    }
  }

  clear(): void {
    try {
      sessionStorage.removeItem(AUTH_TOKEN_KEY);
    } catch {
      // Ignore storage access failures.
    }
  }
}
