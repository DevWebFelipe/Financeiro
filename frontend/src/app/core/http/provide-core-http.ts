import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { API_BASE_URL } from '../config/api-config';
import { environment } from '../config/environment';
import { httpInterceptors } from './http-interceptors';

export function provideCoreHttp(): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideHttpClient(withInterceptors(httpInterceptors)),
    { provide: API_BASE_URL, useValue: environment.apiBaseUrl },
  ]);
}
