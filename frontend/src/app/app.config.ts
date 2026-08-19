import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideAuth } from './core/auth/provide-auth';
import { provideCoreHttp } from './core/http/provide-core-http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideCoreHttp(),
    provideAuth(),
  ],
};
