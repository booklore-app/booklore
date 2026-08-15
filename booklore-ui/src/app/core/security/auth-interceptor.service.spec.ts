import {HttpErrorResponse, HttpHandlerFn, HttpRequest} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {Subject, firstValueFrom, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from '../../shared/service/auth.service';
import {API_CONFIG} from '../config/api-config';
import {AuthInterceptorService} from './auth-interceptor.service';

describe('AuthInterceptorService', () => {
  const getInternalAccessToken = vi.fn(() => 'expired-access-token');
  const internalRefreshToken = vi.fn();
  const saveInternalTokens = vi.fn();
  const forceLogout = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{
        provide: AuthService,
        useValue: {getInternalAccessToken, internalRefreshToken, saveInternalTokens, forceLogout}
      }]
    });
  });

  it('does not retry a 401 response from the refresh endpoint', async () => {
    const request = new HttpRequest('POST', `${API_CONFIG.BASE_URL}/api/v1/auth/refresh`, null);
    const unauthorized = new HttpErrorResponse({status: 401});

    const response = TestBed.runInInjectionContext(() =>
      AuthInterceptorService(request, () => throwError(() => unauthorized))
    );

    await expect(firstValueFrom(response)).rejects.toBe(unauthorized);
    expect(internalRefreshToken).not.toHaveBeenCalled();
  });

  it('fails every queued request when token refresh fails', async () => {
    const refreshResult = new Subject<{accessToken: string; refreshToken: string}>();
    internalRefreshToken.mockReturnValue(refreshResult);
    const unauthorized = new HttpErrorResponse({status: 401});
    const next: HttpHandlerFn = vi.fn(() => throwError(() => unauthorized));

    const firstRequest = interceptProtectedRequest('/api/v1/books', next);
    const secondRequest = interceptProtectedRequest('/api/v1/users/me', next);
    const firstResult = firstValueFrom(firstRequest);
    const secondResult = firstValueFrom(secondRequest);

    refreshResult.error(unauthorized);

    await expect(firstResult).rejects.toBe(unauthorized);
    await expect(secondResult).rejects.toBe(unauthorized);
    expect(forceLogout).toHaveBeenCalledOnce();
  });

  function interceptProtectedRequest(path: string, next: HttpHandlerFn) {
    const request = new HttpRequest('GET', `${API_CONFIG.BASE_URL}${path}`);
    return TestBed.runInInjectionContext(() => AuthInterceptorService(request, next));
  }
});
