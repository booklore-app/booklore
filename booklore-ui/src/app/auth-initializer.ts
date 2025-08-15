import {inject} from '@angular/core';
import {OAuthService} from 'angular-oauth2-oidc';
import {AuthService, websocketInitializer} from './core/service/auth.service';
import {AuthInitializationService} from './auth-initialization-service';
import {PublicAppSettingService} from './public-app-settings.service';

export function initializeAuthFactory() {
  return () => {
    const oauthService = inject(OAuthService);
    const publicAppSettingService = inject(PublicAppSettingService);
    const authService = inject(AuthService);
    const authInitService = inject(AuthInitializationService);

    return new Promise<void>((resolve) => {
      const sub = publicAppSettingService.publicAppSettings$.subscribe(publicSettings => {
        if (publicSettings) {
          if (publicSettings.oidcEnabled && publicSettings.oidcProviderDetails) {
            const details = publicSettings.oidcProviderDetails;

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
                  authService.tokenSubject.next(oauthService.getAccessToken())
                  console.log('[OIDC] Valid access token found after tryLogin');
                  oauthService.setupAutomaticSilentRefresh();
                  websocketInitializer(authService);
                  authInitService.markAsInitialized();
                  resolve();
                } else {
                  console.log('[OIDC] No valid access token found, attempting silent login with prompt=none');
                  oauthService.initImplicitFlow();
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
