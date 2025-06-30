import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthInitializationService {
  private initialized = new BehaviorSubject<boolean>(false);
  initialized$ = this.initialized.asObservable();

  private oidcFailed = new BehaviorSubject<boolean>(false);
  oidcFailed$ = this.oidcFailed.asObservable();

  markAsInitialized() {
    this.initialized.next(true);
  }

  setOidcFailed(failed: boolean) {
    this.oidcFailed.next(failed);
  }
}
