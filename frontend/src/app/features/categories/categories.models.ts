export type CategoryType = string;

export type WritableCategoryType = 'INCOME' | 'EXPENSE';

export interface Category {
  readonly id: string;
  readonly name: string;
  readonly type: CategoryType;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CategoryListParams {
  readonly type?: WritableCategoryType;
  readonly active?: boolean;
}

export interface CreateCategoryRequest {
  readonly name: string;
  readonly type: WritableCategoryType;
}

export interface UpdateCategoryRequest {
  readonly name: string;
  readonly type: WritableCategoryType;
}

export const CATEGORY_TYPE_OPTIONS: readonly {
  readonly value: WritableCategoryType;
  readonly label: string;
}[] = [
  { value: 'EXPENSE', label: 'Despesa' },
  { value: 'INCOME', label: 'Receita' },
];

export type CategoryTypeFilter = '' | WritableCategoryType;
export type CategoryActiveFilter = '' | 'true' | 'false';
