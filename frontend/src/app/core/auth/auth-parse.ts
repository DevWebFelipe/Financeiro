import { isRecord } from '../errors/api-error';
import { AuthUser } from './auth.models';

export function parseAccessToken(body: unknown): string | null {
  if (!isRecord(body)) {
    return null;
  }

  const token = body['accessToken'];
  if (typeof token !== 'string') {
    return null;
  }

  const trimmed = token.trim();
  return trimmed.length > 0 ? trimmed : null;
}

export function parseAuthUser(body: unknown): AuthUser | null {
  if (!isRecord(body)) {
    return null;
  }

  const id = body['id'];
  const name = body['name'];
  const email = body['email'];
  const active = body['active'];
  const createdAt = body['createdAt'];
  const updatedAt = body['updatedAt'];

  if (
    typeof id !== 'string' ||
    typeof name !== 'string' ||
    typeof email !== 'string' ||
    typeof active !== 'boolean' ||
    typeof createdAt !== 'string' ||
    typeof updatedAt !== 'string'
  ) {
    return null;
  }

  return { id, name, email, active, createdAt, updatedAt };
}
