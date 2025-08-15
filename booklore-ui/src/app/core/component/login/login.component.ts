import {Component, inject, OnInit} from '@angular/core';
import {AuthService} from '../../service/auth.service';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {Password} from 'primeng/password';
import {Button} from 'primeng/button';
import {Message} from 'primeng/message';
import {InputText} from 'primeng/inputtext';
import {OAuthService} from 'angular-oauth2-oidc';
import {AppSettingsService} from '../../service/app-settings.service';
import {AppSettings} from '../../model/app-settings.model';
import {Observable} from 'rxjs';
import {filter, take} from 'rxjs/operators';
import {PublicAppSettings, PublicAppSettingService} from '../../../public-app-settings.service';

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    Password,
    Button,
    Message,
    InputText
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  username = '';
  password = '';
  errorMessage = '';
  oidcEnabled = false;
  oidcName = 'OIDC';

  private authService = inject(AuthService);
  private oAuthService = inject(OAuthService);
  private appSettingsService = inject(PublicAppSettingService);
  private router = inject(Router);

  publicAppSettings$: Observable<PublicAppSettings | null> = this.appSettingsService.publicAppSettings$;

  ngOnInit(): void {
    this.publicAppSettings$
      .pipe(
        filter(settings => settings != null),
        take(1)
      )
      .subscribe(publicSettings => {
        this.oidcEnabled = publicSettings!.oidcEnabled;
        this.oidcName = publicSettings!.oidcProviderDetails?.providerName || 'OIDC';
      });
  }

  login(): void {
    this.authService.internalLogin({username: this.username, password: this.password}).subscribe({
      next: (response) => {
        if (response.isDefaultPassword === 'true') {
          this.router.navigate(['/change-password']);
        } else {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (error) => {
        if (error.status === 0) {
          this.errorMessage = 'Cannot connect to the server. Please check your connection and try again.';
        } else {
          this.errorMessage = error?.error?.message || 'An unexpected error occurred. Please try again.';
        }
      }
    });
  }

  loginWithOidc(): void {
    this.oAuthService.initCodeFlow();
  }
}
