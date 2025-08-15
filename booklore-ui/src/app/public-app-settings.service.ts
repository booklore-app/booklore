import {inject, Injectable} from '@angular/core';
import {API_CONFIG} from './config/api-config';
import {BehaviorSubject, Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {OidcProviderDetails} from './core/model/app-settings.model';
import {catchError, finalize, shareReplay, tap} from 'rxjs/operators';

export interface PublicAppSettings {
  oidcEnabled: boolean;
  oidcProviderDetails: OidcProviderDetails;
}

@Injectable({providedIn: 'root'})
export class PublicAppSettingService {
  private http = inject(HttpClient);
  private readonly url = `${API_CONFIG.BASE_URL}/api/v1/public-settings`;

  private loading$: Observable<PublicAppSettings> | null = null;
  private publicAppSettingsSubject = new BehaviorSubject<PublicAppSettings | null>(null);

  publicAppSettings$ = this.publicAppSettingsSubject.asObservable().pipe(
    tap(state => {
      if (!state && !this.loading$) {
        this.loading$ = this.fetchPublicSettings().pipe(
          shareReplay(1),
          finalize(() => (this.loading$ = null))
        );
        this.loading$.subscribe();
      }
    })
  );

  private fetchPublicSettings(): Observable<PublicAppSettings> {
    return this.http.get<PublicAppSettings>(this.url).pipe(
      tap(settings => this.publicAppSettingsSubject.next(settings)),
      catchError(err => {
        console.error('Failed to fetch public settings', err);
        throw err;
      })
    );
  }
}
