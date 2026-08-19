import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../config/api-config';
import { isApiError } from '../errors/api-error';
import { parseAccessToken, parseAuthUser } from './auth-parse';
import { AuthTokenStore } from './auth-token.store';
import { AuthStatus, AuthUser, LoginRequest, RegisterRequest } from './auth.models';
import { redirectToLogin } from './redirect-to-login';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStore = inject(AuthTokenStore);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly router = inject(Router);

  private readonly statusState = signal<AuthStatus>('loading');
  private readonly userState = signal<AuthUser | null>(null);
  private memoryToken: string | null = null;
  private initializePromise: Promise<void> | null = null;
  private sessionInvalidationInProgress = false;

  readonly status = this.statusState.asReadonly();
  readonly user = this.userState.asReadonly();

  getAccessToken(): string | null {
    if (this.memoryToken != null && this.memoryToken.length > 0) {
      return this.memoryToken;
    }
    return this.tokenStore.read();
  }

  initialize(): Promise<void> {
    if (this.initializePromise == null) {
      this.initializePromise = this.restoreSession();
    }
    return this.initializePromise;
  }

  async login(request: LoginRequest): Promise<void> {
    const body = await firstValueFrom(
      this.http.post<unknown>(this.apiUrl('/auth/login'), {
        email: request.email,
        password: request.password,
      }),
    );
    const token = parseAccessToken(body);
    if (token == null) {
      throw new Error('Login response did not include an access token.');
    }

    this.persistToken(token);

    try {
      const user = await this.fetchCurrentUser();
      this.userState.set(user);
      this.statusState.set('authenticated');
    } catch (error: unknown) {
      if (isApiError(error) && error.status === 401) {
        this.clearSession();
      }
      throw error;
    }
  }

  async register(request: RegisterRequest): Promise<void> {
    await firstValueFrom(
      this.http.post<unknown>(this.apiUrl('/auth/register'), {
        name: request.name,
        email: request.email,
        password: request.password,
      }),
    );
  }

  logout(): Promise<boolean> {
    this.clearSession();
    return redirectToLogin(this.router);
  }

  handleUnauthorized(): void {
    if (this.sessionInvalidationInProgress) {
      this.clearSession();
      return;
    }

    this.sessionInvalidationInProgress = true;
    const wasLoading = this.statusState() === 'loading';
    this.clearSession();
    if (wasLoading) {
      return;
    }
    void redirectToLogin(this.router, this.router.url);
  }

  private async restoreSession(): Promise<void> {
    this.statusState.set('loading');
    const token = this.getAccessToken();
    if (token == null) {
      this.userState.set(null);
      this.statusState.set('unauthenticated');
      return;
    }

    this.memoryToken = token;

    try {
      const user = await this.fetchCurrentUser();
      this.userState.set(user);
      this.statusState.set('authenticated');
    } catch (error: unknown) {
      if (isApiError(error) && error.status === 401) {
        this.clearSession();
        return;
      }
      this.userState.set(null);
      this.statusState.set('authenticated');
    }
  }

  private async fetchCurrentUser(): Promise<AuthUser | null> {
    const body = await firstValueFrom(this.http.get<unknown>(this.apiUrl('/users/me')));
    return parseAuthUser(body);
  }

  private persistToken(token: string): void {
    this.sessionInvalidationInProgress = false;
    this.memoryToken = token;
    this.tokenStore.write(token);
  }

  private clearSession(): void {
    this.memoryToken = null;
    this.tokenStore.clear();
    this.userState.set(null);
    this.statusState.set('unauthenticated');
  }

  private apiUrl(path: string): string {
    return joinApiUrl(this.apiBaseUrl, path);
  }
}
