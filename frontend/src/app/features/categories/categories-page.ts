import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, EMPTY, firstValueFrom, startWith, Subject, switchMap } from 'rxjs';
import { ApiError, isApiError } from '../../core/errors/api-error';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { categoryStatusLabel, categoryTypeLabel } from './categories-format';
import {
  Category,
  CategoryActiveFilter,
  CATEGORY_TYPE_OPTIONS,
  CategoryListParams,
  CategoryTypeFilter,
  WritableCategoryType,
} from './categories.models';
import { CategoriesService } from './categories.service';

@Component({
  selector: 'app-categories-page',
  imports: [ReactiveFormsModule, EmptyState, ErrorState],
  templateUrl: './categories-page.html',
  styleUrl: './categories-page.css',
})
export class CategoriesPage {
  private readonly categoriesService = inject(CategoriesService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reload = new Subject<void>();
  private pendingAction: 'create' | 'update' | 'deactivate' | null = null;

  readonly status = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly categories = signal<Category[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly formMode = signal<'closed' | 'create' | 'edit'>('closed');
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly typeFilter = signal<CategoryTypeFilter>('');
  readonly activeFilter = signal<CategoryActiveFilter>('');

  readonly isEmpty = computed(() => this.status() === 'loaded' && this.categories().length === 0);
  readonly hasFilters = computed(() => this.typeFilter() !== '' || this.activeFilter() !== '');
  readonly typeOptions = CATEGORY_TYPE_OPTIONS;
  readonly categoryTypeLabel = categoryTypeLabel;
  readonly categoryStatusLabel = categoryStatusLabel;

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    type: this.formBuilder.nonNullable.control<WritableCategoryType | ''>('', Validators.required),
  });

  constructor() {
    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.status.set('loading');
          this.categories.set([]);
          this.error.set(null);
          this.actionError.set(null);
          return this.categoriesService.list(this.listParams()).pipe(
            catchError((error: unknown) => {
              this.error.set(isApiError(error) ? error : null);
              this.status.set('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((categories) => {
        this.categories.set(categories);
        this.status.set('loaded');
      });
  }

  retry(): void {
    this.reload.next();
  }

  onTypeFilterChange(value: CategoryTypeFilter): void {
    this.typeFilter.set(value);
    this.reload.next();
  }

  onActiveFilterChange(value: CategoryActiveFilter): void {
    this.activeFilter.set(value);
    this.reload.next();
  }

  openCreate(): void {
    this.resetForm();
    this.formMode.set('create');
    this.editingId.set(null);
  }

  openEdit(category: Category): void {
    this.resetForm();
    const writableType = this.toWritableType(category.type);
    this.form.patchValue({
      name: category.name,
      type: writableType ?? '',
    });
    this.formMode.set('edit');
    this.editingId.set(category.id);
  }

  closeForm(): void {
    this.resetForm();
    this.formMode.set('closed');
    this.editingId.set(null);
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.submitting() || this.formMode() === 'closed') {
      this.form.markAllAsTouched();
      return;
    }

    const mode = this.formMode();
    const { name, type } = this.form.getRawValue();
    if (type === '') {
      this.form.controls.type.markAsTouched();
      return;
    }

    this.submitting.set(true);
    this.formError.set(null);
    this.actionError.set(null);

    try {
      if (mode === 'create') {
        this.pendingAction = 'create';
        await firstValueFrom(this.categoriesService.create({ name: name.trim(), type }));
      } else {
        const categoryId = this.editingId();
        if (categoryId == null) {
          return;
        }
        this.pendingAction = 'update';
        await firstValueFrom(
          this.categoriesService.update(categoryId, { name: name.trim(), type }),
        );
      }
      this.closeForm();
      this.reload.next();
    } catch (error: unknown) {
      this.handleMutationError(error);
    } finally {
      this.submitting.set(false);
      this.pendingAction = null;
    }
  }

  async deactivate(category: Category): Promise<void> {
    if (this.submitting() || !category.active) {
      return;
    }

    this.submitting.set(true);
    this.actionError.set(null);
    this.pendingAction = 'deactivate';
    try {
      await firstValueFrom(this.categoriesService.deactivate(category.id));
      this.reload.next();
    } catch (error: unknown) {
      this.handleMutationError(error);
    } finally {
      this.submitting.set(false);
      this.pendingAction = null;
    }
  }

  fieldError(controlName: 'name' | 'type'): string | null {
    const control = this.form.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('api')) {
      const apiError = control.getError('api');
      return typeof apiError === 'string' ? apiError : null;
    }
    if (control.hasError('required')) {
      if (controlName === 'name') {
        return 'Informe o nome.';
      }
      if (controlName === 'type') {
        return 'Selecione o tipo.';
      }
    }
    if (control.hasError('maxlength')) {
      return 'O nome deve ter no máximo 255 caracteres.';
    }
    return null;
  }

  private listParams(): CategoryListParams {
    const type = this.typeFilter();
    const active = this.activeFilter();
    return {
      ...(type !== '' ? { type } : {}),
      ...(active === 'true' ? { active: true } : active === 'false' ? { active: false } : {}),
    };
  }

  private handleMutationError(error: unknown): void {
    if (!isApiError(error)) {
      this.setMutationMessage('Não foi possível concluir a operação.');
      return;
    }

    if (error.code === 'VALIDATION_ERROR' && error.fields != null) {
      this.applyFieldErrors(error.fields);
      if (this.formError() == null && this.unmappedFieldCount(error.fields) > 0) {
        this.formError.set('Revise os dados informados.');
      }
      return;
    }

    if (error.code === 'VALIDATION_ERROR') {
      this.formError.set('Revise os dados informados.');
      return;
    }

    if (error.status === 403) {
      this.actionError.set('Você não tem permissão para esta operação.');
      return;
    }

    if (error.code === 'CONFLICT') {
      this.setMutationMessage(
        'Já existe uma categoria com este nome e tipo. Escolha outro nome ou altere o tipo.',
      );
      return;
    }

    this.setMutationMessage('Não foi possível concluir a operação.');
  }

  private setMutationMessage(message: string): void {
    if (this.formMode() === 'closed') {
      this.actionError.set(message);
      return;
    }
    this.formError.set(message);
  }

  private applyFieldErrors(fields: Record<string, string>): void {
    for (const [key, message] of Object.entries(fields)) {
      const control = this.form.get(key);
      if (control == null) {
        continue;
      }
      control.setErrors({ ...control.errors, api: message });
      control.markAsTouched();
    }
  }

  private unmappedFieldCount(fields: Record<string, string>): number {
    return Object.keys(fields).filter((key) => this.form.get(key) == null).length;
  }

  private resetForm(): void {
    this.form.reset({ name: '', type: '' });
    this.formError.set(null);
  }

  private toWritableType(type: string): WritableCategoryType | null {
    return type === 'INCOME' || type === 'EXPENSE' ? type : null;
  }
}
