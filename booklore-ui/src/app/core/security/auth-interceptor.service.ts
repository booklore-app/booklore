import {HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest} from '@angular/common/http';
import {inject} from '@angular/core';
import {catchError, switchMap, take} from 'rxjs/operators';
import {Observable, ReplaySubject, throwError} from 'rxjs';
import {AuthService} from '../../shared/service/auth.service';
import {API_CONFIG} from '../config/api-config';

export const AuthInterceptorService: HttpInterceptorFn = (req, next: HttpHandlerFn) => {
  const authService = inject(AuthService);

  const token = authService.getInternalAccessToken();
  const isApiRequest = req.url.startsWith(`${API_CONFIG.BASE_URL}/api/`);
  const isRefreshRequest = req.url === `${API_CONFIG.BASE_URL}/api/v1/auth/refresh`;

  const authReq = (token && isApiRequest) ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isRefreshRequest) {
        return handle401Error(authService, authReq, next);
      }
      return throwError(() => error);
    })
  );
};

let isRefreshing = false;
let refreshTokenSubject = new ReplaySubject<string>(1);

function handle401Error(authService: AuthService, request: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject = new ReplaySubject<string>(1);

    return authService.internalRefreshToken().pipe(
      switchMap(response => {
        isRefreshing = false;
        const { accessToken, refreshToken } = response;
        if (accessToken && refreshToken) {
          authService.saveInternalTokens(accessToken, refreshToken);
          refreshTokenSubject.next(accessToken);
        }
        return next(request.clone({
          setHeaders: { Authorization: `Bearer ${accessToken}` }
        }));
      }),
      catchError(err => {
        isRefreshing = false;
        refreshTokenSubject.error(err);
        authService.forceLogout('session_expired');
        return throwError(() => err);
      })
    );
  }

  return refreshTokenSubject.pipe(
    take(1),
    switchMap(token =>
      next(request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      }))
    )
  );
}
