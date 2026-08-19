import { CategoryType } from './categories.models';

export function categoryTypeLabel(type: CategoryType): string {
  if (type === 'INCOME') {
    return 'Receita';
  }
  if (type === 'EXPENSE') {
    return 'Despesa';
  }
  return type;
}

export function categoryStatusLabel(active: boolean): string {
  return active ? 'Ativa' : 'Inativa';
}
