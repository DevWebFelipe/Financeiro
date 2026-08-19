export interface E2eUser {
  readonly name: string;
  readonly email: string;
  readonly password: string;
}

export function uniqueSuffix(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function uniqueUser(prefix = 'e2e'): E2eUser {
  const suffix = uniqueSuffix();
  return {
    name: `${prefix} ${suffix}`,
    email: `${prefix}.${suffix}@example.test`.toLowerCase(),
    password: 'E2ePassw0rd!',
  };
}

export function uniqueName(prefix: string): string {
  return `${prefix} ${uniqueSuffix()}`;
}
