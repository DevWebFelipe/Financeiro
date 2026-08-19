import { isRecord } from '../../core/errors/api-error';
import { Category } from './categories.models';

export function parseCategoryList(body: unknown): Category[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const categories: Category[] = [];
  for (const item of body) {
    const parsed = parseCategory(item);
    if (parsed == null) {
      return null;
    }
    categories.push(parsed);
  }
  return categories;
}

export function parseCategory(value: unknown): Category | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const name = typeof value['name'] === 'string' ? value['name'] : null;
  const type = parseCategoryType(value['type']);
  const active = typeof value['active'] === 'boolean' ? value['active'] : null;
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    name == null ||
    type == null ||
    active == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return { id, name, type, active, createdAt, updatedAt };
}

function parseCategoryType(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}
