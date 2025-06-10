import {Subject} from 'rxjs';
import {debounceTime} from 'rxjs/operators';
import {inject, Injectable} from '@angular/core';
import {MessageService} from 'primeng/api';
import {UserService} from '../../../settings/user-management/user.service';

@Injectable({
  providedIn: 'root'
})
export class CoverScaleManager {
  private readonly baseWidth = 135;
  private readonly baseHeight = 220;

  private readonly messageService = inject(MessageService);
  private readonly userService = inject(UserService);

  private readonly scaleChangeSubject = new Subject<number>();
  readonly scaleChange$ = this.scaleChangeSubject.asObservable();

  scaleFactor = 1.0;

  constructor() {
    this.scaleChange$
      .pipe(debounceTime(1000))
      .subscribe(scale => this.persistScale(scale));
  }

  setScale(scale: number): void {
    this.scaleFactor = scale;
    this.scaleChangeSubject.next(scale);
  }

  get currentCardSize(): { width: number; height: number } {
    return {
      width: Math.round(this.baseWidth * this.scaleFactor),
      height: Math.round(this.baseHeight * this.scaleFactor),
    };
  }

  get gridColumnMinWidth(): string {
    return `${this.currentCardSize.width}px`;
  }

  private persistScale(scale: number): void {
    const user = this.userService.getCurrentUser();
    const entityViewPreferences = user?.userSettings?.entityViewPreferences;
    if (!user || !entityViewPreferences) {
      return;
    }
    entityViewPreferences.global = entityViewPreferences.global ?? {};
    entityViewPreferences.global.coverSize = scale;
    this.userService.updateUserSetting(user.id, 'entityViewPreferences', entityViewPreferences);
    this.messageService.add({
      severity: 'success',
      summary: 'Cover Size Saved',
      detail: `Cover size set to ${scale.toFixed(2)}x.`
    });
  }
}
