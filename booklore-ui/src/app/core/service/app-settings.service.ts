import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {AppSettings} from '../model/app-settings.model';
import {API_CONFIG} from '../../config/api-config';
import {catchError, finalize, shareReplay, tap} from 'rxjs/operators';

@Injectable({providedIn: 'root'})
export class AppSettingsService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${API_CONFIG.BASE_URL}/api/v1/settings`;

  private loading$: Observable<AppSettings> | null = null;
  private appSettingsSubject = new BehaviorSubject<AppSettings | null>(null);
  appSettings$ = this.appSettingsSubject.asObservable().pipe(
    tap(state => {
      if (!state && !this.loading$) {
        this.loading$ = this.fetchAppSettings().pipe(
          shareReplay(1),
          finalize(() => (this.loading$ = null))
        );
        this.loading$.subscribe();
      }
    })
  );

  private fetchAppSettings(): Observable<AppSettings> {
    return this.http.get<AppSettings>(this.apiUrl).pipe(
      tap(settings => this.appSettingsSubject.next(settings)),
      catchError(err => {
        console.error('Error loading app settings:', err);
        this.appSettingsSubject.next(null);
        throw err;
      })
    );
  }

  saveSettings(settings: { key: string; newValue: any }[]): Observable<void> {
    const payload = settings.map(setting => ({
      name: setting.key,
      value: setting.newValue
    }));

    return this.http.put<void>(this.apiUrl, payload).pipe(
      tap(() => {
        this.loading$ = this.fetchAppSettings().pipe(
          shareReplay(1),
          finalize(() => (this.loading$ = null))
        );
        this.loading$.subscribe();
      }),
      catchError(err => {
        console.error('Error saving settings:', err);
        return of();
      })
    );
  }
}
