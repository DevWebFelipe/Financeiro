import { request } from '@playwright/test';
import { API_BASE_URL } from './helpers/env';

export default async function globalSetup(): Promise<void> {
  const context = await request.newContext();
  try {
    const health = await context.get(`${API_BASE_URL}/health`);
    if (!health.ok()) {
      throw new Error(
        `Backend health check failed (${health.status()}). ` +
          'Start PostgreSQL and the backend before E2E: scripts/start.ps1 or scripts/run-e2e.ps1.',
      );
    }
    const body = (await health.json()) as { status?: string };
    if (body.status !== 'UP') {
      throw new Error(`Backend is not UP. Received: ${JSON.stringify(body)}`);
    }
  } finally {
    await context.dispose();
  }
}
