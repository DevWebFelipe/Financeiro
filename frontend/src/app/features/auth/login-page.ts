import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { toSafeInternalUrl } from '../../core/auth/internal-url';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<ApiError | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();

    try {
      await this.auth.login({ email, password });
      this.form.reset();
      await this.router.navigateByUrl(this.returnUrl());
    } catch (error: unknown) {
      this.form.controls.password.reset();
      this.error.set(isApiError(error) ? error : null);
    } finally {
      this.submitting.set(false);
    }
  }

  private returnUrl(): string {
    return toSafeInternalUrl(this.route.snapshot.queryParamMap.get('returnUrl')) ?? '/';
  }
}
