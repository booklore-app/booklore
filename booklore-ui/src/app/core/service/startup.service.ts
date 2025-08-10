import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { UserService } from '../../settings/user-management/user.service';
import { filter, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class StartupService {
  private auth = inject(AuthService);
  private userSvc = inject(UserService);

  load(): Promise<void> {
    this.auth.token$
      .pipe(filter(t => !!t))
      .subscribe(() => {
        this.userSvc.getMyself()
          .pipe(catchError(() => of(null)))
          .subscribe(user => {
            if (user) {
              this.userSvc.setInitialUser(user);
            }
          });
      });
    return Promise.resolve();
  }
}
