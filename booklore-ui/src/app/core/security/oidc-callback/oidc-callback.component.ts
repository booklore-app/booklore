import {Component, inject, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {OAuthService} from 'angular-oauth2-oidc';
import {AuthService} from '../../service/auth.service';

@Component({
  selector: 'app-oidc-callback',
  templateUrl: './oidc-callback.component.html',
  styleUrls: ['./oidc-callback.component.scss']
})
export class OidcCallbackComponent implements OnInit {

  private router = inject(Router);
  private oAuthService = inject(OAuthService);
  private authService = inject(AuthService);

  ngOnInit(): void {
    /*this.oAuthService.tryLoginCodeFlow().then(info => {
      if (this.oAuthService.hasValidAccessToken()) {
        const accessToken = this.oAuthService.getAccessToken();
        const refreshToken = this.oAuthService.getRefreshToken();
        this.authService.saveOidcTokens(accessToken, refreshToken ?? '');

        this.authService.getRxStompService().activate();
        this.oAuthService.setupAutomaticSilentRefresh();
        this.router.navigate(['/dashboard']);
      } else {
        console.error('OIDC code exchange failed');
        this.router.navigate(['/login']);
      }
    }).catch(err => {
      console.error('OIDC error:', err);
      this.router.navigate(['/login']);
    });*/
  }
}
