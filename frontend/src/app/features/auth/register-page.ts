import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { ErrorState } from '../../shared/components/error-state/error-state';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, ErrorState],
  templateUrl: './register-page.html',
  styleUrl: './register-page.css',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<ApiError | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
  });

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    const { name, email, password } = this.form.getRawValue();

    try {
      await this.auth.register({ name, email, password });
      this.form.reset();
      await this.router.navigateByUrl('/login');
    } catch (error: unknown) {
      this.form.controls.password.reset();
      this.error.set(isApiError(error) ? error : null);
    } finally {
      this.submitting.set(false);
    }
  }
}
