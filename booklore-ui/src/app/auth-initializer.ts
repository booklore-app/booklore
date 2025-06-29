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
        if (settings) {
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
                console.log('[OIDC] Discovery document loaded and login attempted');
                if (oauthService.hasValidAccessToken()) {
                  console.log('[OIDC] Valid access token found after tryLogin');
                  router.navigate(['/dashboard']);
                  oauthService.setupAutomaticSilentRefresh();
                  websocketInitializer(authService);
                  authInitService.markAsInitialized();
                  resolve();
                } else {
                  console.log('[OIDC] No valid access token found, attempting silent login with prompt=none');
                  oauthService.initCodeFlow();
                  resolve();
                }
              })
              .catch(err => {
                authInitService.markAsInitialized();
                console.error(
                  'OIDC initialization failed: Unable to complete OpenID Connect discovery or login. ' +
                  'This may be due to an incorrect issuer URL, client ID, or network issue. ' +
                  'Falling back to local login. Details:', err
                );
                resolve();
              });
          } else if (settings.remoteAuthEnabled) {
            authService.remoteLogin().subscribe({
              next: () => {
                resolve();
              },
              error: resolve
            });
          } else {
            authInitService.markAsInitialized();
            resolve();
          }
          sub.unsubscribe();
        }
      });
    });
  };
}
