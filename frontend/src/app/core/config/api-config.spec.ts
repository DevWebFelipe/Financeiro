import { API_BASE_URL, joinApiUrl } from './api-config';
import { environment as developmentEnvironment } from './environment.development';
import { environment as productionEnvironment } from './environment.production';

describe('API configuration', () => {
  it('uses the official development API base URL', () => {
    expect(developmentEnvironment.production).toBe(false);
    expect(developmentEnvironment.apiBaseUrl).toBe('http://localhost:8080/api/v1');
  });

  it('uses a same-origin production API base URL', () => {
    expect(productionEnvironment.production).toBe(true);
    expect(productionEnvironment.apiBaseUrl).toBe('/api/v1');
  });

  it('does not store secrets in environment files', () => {
    for (const env of [developmentEnvironment, productionEnvironment]) {
      expect(Object.keys(env).sort()).toEqual(['apiBaseUrl', 'production']);
    }
  });

  it('joins API paths without duplicating slashes', () => {
    expect(joinApiUrl('http://localhost:8080/api/v1', '/accounts')).toBe(
      'http://localhost:8080/api/v1/accounts',
    );
    expect(joinApiUrl('http://localhost:8080/api/v1/', 'accounts')).toBe(
      'http://localhost:8080/api/v1/accounts',
    );
    expect(joinApiUrl('/api/v1', 'accounts')).toBe('/api/v1/accounts');
  });

  it('exposes API_BASE_URL as an injection token', () => {
    expect(API_BASE_URL.toString()).toContain('API_BASE_URL');
  });
});
