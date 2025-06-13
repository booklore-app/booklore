import {Injectable, inject} from '@angular/core';
import {Subject} from 'rxjs';
import {debounceTime} from 'rxjs/operators';
import {MessageService} from 'primeng/api';
import {UserService} from '../../../settings/user-management/user.service';

@Injectable({
  providedIn: 'root'
})
export class CoverScalePreferenceService {

  private readonly BASE_WIDTH = 135;
  private readonly BASE_HEIGHT = 220;
  private readonly DEBOUNCE_MS = 1000;

  private readonly messageService = inject(MessageService);
  private readonly userService = inject(UserService);

  private readonly scaleChangeSubject = new Subject<number>();
  readonly scaleChange$ = this.scaleChangeSubject.asObservable();

  scaleFactor = 1.0;

  constructor() {
    this.scaleChange$
      .pipe(debounceTime(this.DEBOUNCE_MS))
      .subscribe(scale => this.saveScalePreference(scale));
  }

  initScaleValue(scale: number | undefined): void {
    this.scaleFactor = scale ?? 1.0;
  }

  setScale(scale: number): void {
    this.scaleFactor = scale;
    this.scaleChangeSubject.next(scale);
  }

  get currentCardSize(): { width: number; height: number } {
    return {
      width: Math.round(this.BASE_WIDTH * this.scaleFactor),
      height: Math.round(this.BASE_HEIGHT * this.scaleFactor),
    };
  }

  get gridColumnMinWidth(): string {
    return `${this.currentCardSize.width}px`;
  }

  private saveScalePreference(scale: number): void {
    const user = this.userService.getCurrentUser();
    const prefs = user?.userSettings?.entityViewPreferences;

    if (!user || !prefs) return;

    prefs.global = {...prefs.global, coverSize: scale};
    this.userService.updateUserSetting(user.id, 'entityViewPreferences', prefs);

    this.messageService.add({
      severity: 'success',
      summary: 'Cover Size Saved',
      detail: `Cover size set to ${scale.toFixed(2)}x.`,
      life: 1500
    });
  }
}
