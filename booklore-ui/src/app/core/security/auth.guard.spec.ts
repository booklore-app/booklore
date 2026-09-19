import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot} from '@angular/router';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from '../../shared/service/auth.service';
import {AuthGuard} from './auth.guard';

describe('AuthGuard', () => {
  const getInternalAccessToken = vi.fn();
  const navigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        {provide: AuthService, useValue: {getInternalAccessToken}},
        {provide: Router, useValue: {navigate}}
      ]
    });
  });

  it('blocks anonymous access to the change-password route', () => {
    getInternalAccessToken.mockReturnValue(null);

    const result = runGuard('/change-password');

    expect(result).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('allows a default-password user to open the change-password route', () => {
    getInternalAccessToken.mockReturnValue(createToken({isDefaultPassword: true}));

    expect(runGuard('/change-password')).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('redirects a default-password user away from other protected routes', () => {
    getInternalAccessToken.mockReturnValue(createToken({isDefaultPassword: true}));

    expect(runGuard('/dashboard')).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/change-password']);
  });

  function runGuard(url: string) {
    return TestBed.runInInjectionContext(() => AuthGuard(
      {} as ActivatedRouteSnapshot,
      {url} as RouterStateSnapshot
    ));
  }

  function createToken(payload: Record<string, unknown>): string {
    return `header.${btoa(JSON.stringify(payload))}.signature`;
  }
});
