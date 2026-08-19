import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { CategoriesPage } from './categories-page';
import { Category } from './categories.models';
import { CategoriesService } from './categories.service';

const CATEGORY_ID = '01900000-0000-7000-8000-000000000001';

function category(overrides: Partial<Category> = {}): Category {
  return {
    id: CATEGORY_ID,
    name: 'Mercado',
    type: 'EXPENSE',
    active: true,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

const loadError: ApiError = {
  timestamp: '2026-08-19T15:00:00Z',
  status: 500,
  code: 'INTERNAL_ERROR',
  message: 'Erro interno.',
  path: '/api/v1/categories',
};

describe('CategoriesPage', () => {
  let list: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let update: ReturnType<typeof vi.fn>;
  let deactivate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    list = vi.fn();
    create = vi.fn();
    update = vi.fn();
    deactivate = vi.fn();
    await TestBed.configureTestingModule({
      imports: [CategoriesPage],
      providers: [
        {
          provide: CategoriesService,
          useValue: { list, create, update, deactivate },
        },
      ],
    }).compileComponents();
  });

  it('shows a loading state without placeholder rows', () => {
    list.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Carregando categorias.');
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });

  it('renders official category data after load', async () => {
    list.mockReturnValue(of([category()]));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Categorias');
    expect(text).toContain('Mercado');
    expect(text).toContain('Despesa');
    expect(text).toContain('Ativa');
  });

  it('shows an empty state with a real create action', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma categoria cadastrada.');
    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nova categoria');
  });

  it('shows an error state and retries the categories load', async () => {
    list.mockReturnValueOnce(throwError(() => loadError)).mockReturnValueOnce(of([category()]));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('Não foi possível carregar as categorias.');
    expect(fixture.nativeElement.textContent).not.toContain(loadError.message);

    const retry = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Tentar novamente'));
    retry?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Mercado');
  });

  it('reloads with official server-side filters', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.onTypeFilterChange('INCOME');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith({ type: 'INCOME' });

    fixture.componentInstance.onActiveFilterChange('false');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(list).toHaveBeenLastCalledWith({ type: 'INCOME', active: false });
  });

  it('labels inactive categories and hides deactivate without reactivate', async () => {
    list.mockReturnValue(of([category({ active: false })]));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Inativa');
    expect(text).not.toContain('Reativar');
    expect(text).not.toContain('Desativar');
    expect(fixture.nativeElement.querySelector('tr.is-inactive')).not.toBeNull();
  });

  it('deactivates an active category and reloads', async () => {
    list
      .mockReturnValueOnce(of([category()]))
      .mockReturnValueOnce(of([category({ active: false })]));
    deactivate.mockReturnValue(of(category({ active: false })));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const button = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((candidate) => candidate.textContent?.includes('Desativar'));
    button?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(deactivate).toHaveBeenCalledWith(CATEGORY_ID);
    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Inativa');
  });

  it('requires name and type before submitting a new category', async () => {
    list.mockReturnValue(of([]));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Informe o nome.');
    expect(fixture.nativeElement.textContent).toContain('Selecione o tipo.');
  });

  it('creates a category and reloads the list', async () => {
    list.mockReturnValueOnce(of([])).mockReturnValueOnce(of([category()]));
    create.mockReturnValue(of(category()));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({ name: 'Mercado', type: 'EXPENSE' });
    await fixture.componentInstance.submit();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(create).toHaveBeenCalledWith({ name: 'Mercado', type: 'EXPENSE' });
    expect(list).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Mercado');
  });

  it('binds validation field errors to the corresponding controls', async () => {
    list.mockReturnValue(of([]));
    create.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Dados inválidos.',
        path: '/api/v1/categories',
        fields: { name: 'O nome é obrigatório.' },
      })),
    );
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ name: 'Mercado', type: 'INCOME' });
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('O nome é obrigatório.');
    expect(fixture.nativeElement.textContent).not.toContain('/api/v1/categories');
  });

  it('shows a contextual conflict message on duplicate create', async () => {
    list.mockReturnValue(of([]));
    create.mockReturnValue(
      throwError(() => ({
        timestamp: '2026-08-19T15:00:00Z',
        status: 409,
        code: 'CONFLICT',
        message: 'Já existe uma categoria com este nome e tipo.',
        path: '/api/v1/categories',
      })),
    );
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreate();
    fixture.componentInstance.form.setValue({ name: 'Mercado', type: 'EXPENSE' });
    await fixture.componentInstance.submit();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Já existe uma categoria com este nome e tipo.');
    expect(text).not.toContain('/api/v1/categories');
  });

  it('updates a category with name and type only', async () => {
    list
      .mockReturnValueOnce(of([category()]))
      .mockReturnValueOnce(of([category({ name: 'Moradia', type: 'EXPENSE' })]));
    update.mockReturnValue(of(category({ name: 'Moradia', type: 'EXPENSE' })));
    const fixture = TestBed.createComponent(CategoriesPage);
    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openEdit(category());
    fixture.componentInstance.form.patchValue({ name: 'Moradia', type: 'EXPENSE' });
    await fixture.componentInstance.submit();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(update).toHaveBeenCalledWith(CATEGORY_ID, { name: 'Moradia', type: 'EXPENSE' });
  });

  it('does not inject HttpClient in the page', () => {
    list.mockReturnValue(NEVER);
    const fixture = TestBed.createComponent(CategoriesPage);
    expect(fixture.componentInstance).toBeTruthy();
    expect(list).toHaveBeenCalledTimes(1);
  });
});
