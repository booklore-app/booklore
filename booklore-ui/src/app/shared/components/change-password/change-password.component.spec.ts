import {TestBed} from '@angular/core/testing';
import {MessageService} from 'primeng/api';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {TranslocoService} from '@jsverse/transloco';
import {UserService} from '../../../features/settings/user-management/user.service';
import {AuthService} from '../../service/auth.service';
import {ChangePasswordComponent} from './change-password.component';

describe('ChangePasswordComponent', () => {
  const changePassword = vi.fn();
  const translate = vi.fn((key: string) => key);
  let component: ChangePasswordComponent;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        {provide: UserService, useValue: {changePassword}},
        {provide: AuthService, useValue: {logout: vi.fn()}},
        {provide: MessageService, useValue: {add: vi.fn()}},
        {provide: TranslocoService, useValue: {translate}}
      ]
    });
    component = TestBed.runInInjectionContext(() => new ChangePasswordComponent());
  });

  it('rejects a new password shorter than eight characters', () => {
    component.currentPassword = 'current-password';
    component.newPassword = 'short';
    component.confirmNewPassword = 'short';

    component.changePassword();

    expect(changePassword).not.toHaveBeenCalled();
    expect(component.errorMessage).toBe('shared.setup.validation.passwordMinLength');
  });

  it('accepts eight characters as the minimum length', () => {
    component.newPassword = '12345678';

    expect(component.newPasswordMeetsMinimumLength).toBe(true);
  });
});
