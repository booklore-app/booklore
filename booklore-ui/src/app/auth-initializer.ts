import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {OAuthEvent, OAuthService} from 'angular-oauth2-oidc';
import {AppSettingsService} from './core/service/app-settings.service';
import {AuthService, websocketInitializer} from './core/service/auth.service';
import {filter} from 'rxjs/operators';
import {AuthInitializationService} from './auth-initialization-service';

export function initializeAuthFactory() {
  return () => {
    const oauthService = inject(OAuthService);
    const appSettingsService = inject(AppSettingsService);
    const authService = inject(AuthService);
    const router = inject(Router);
    const authInitService = inject(AuthInitializationService);

    return new Promise<void>((resolve) => {
      const sub = appSettingsService.appSettings$.subscribe(settings => {
        if (!settings) return;

        sub.unsubscribe();

        if (settings.oidcEnabled && settings.oidcProviderDetails) {
          const details = settings.oidcProviderDetails;

          oauthService.configure({
            issuer: details.issuerUri,
            clientId: details.clientId,
            scope: 'openid profile email offline_access',
            redirectUri: window.location.origin + '/oauth2-callback',
            responseType: 'code',
            showDebugInformation: false,
            requireHttps: false,
            strictDiscoveryDocumentValidation: false,
          });

          oauthService.loadDiscoveryDocumentAndTryLogin()
            .then(() => {
              if (oauthService.hasValidAccessToken()) {
                console.log('[OIDC] Valid access token found');
                oauthService.setupAutomaticSilentRefresh();
                websocketInitializer(authService);
              } else {
                console.warn('[OIDC] No valid access token. Will proceed to app and show login page.');
              }
            })
            .catch(err => {
              console.error('[OIDC] Failed to load discovery document or login:', err);
              authInitService.setOidcFailed(true);
            })
            .finally(() => {
              authInitService.markAsInitialized();
              resolve();
            });

        } else if (settings.remoteAuthEnabled) {
          authService.remoteLogin().subscribe({
            next: () => {
              authInitService.markAsInitialized();
              resolve();
            },
            error: err => {
              console.error('[Remote Login] failed:', err);
              authInitService.markAsInitialized();
              resolve();
            }
          });

        } else {
          authInitService.markAsInitialized();
          resolve();
        }
      });
    });
  };
}
