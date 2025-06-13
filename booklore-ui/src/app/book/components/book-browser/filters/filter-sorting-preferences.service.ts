import {inject, Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {UserService} from '../../../../settings/user-management/user.service';
import {MessageService} from 'primeng/api';

@Injectable({
  providedIn: 'root'
})
export class FilterSortPreferenceService {

  readonly filterSortingOptions = [
    { label: 'Alphabetical (A–Z)', value: 'alphabetical' },
    { label: 'Book Count (High to Low)', value: 'count' }
  ];

  private readonly userService = inject(UserService);
  private readonly messageService = inject(MessageService);

  private readonly sortModeSubject = new BehaviorSubject<'alphabetical' | 'count'>('count');
  readonly sortMode$ = this.sortModeSubject.asObservable();

  initValue(value: 'alphabetical' | 'count' | null | undefined): void {
    const resolved = value ?? 'count';
    this.sortModeSubject.next(resolved);
  }

  get selectedFilterSorting(): 'alphabetical' | 'count' {
    return this.sortModeSubject.value;
  }

  set selectedFilterSorting(value: 'alphabetical' | 'count') {
    if (this.sortModeSubject.value !== value) {
      this.sortModeSubject.next(value);
      this.savePreference(value);
    }
  }

  private savePreference(value: 'alphabetical' | 'count'): void {
    const user = this.userService.getCurrentUser();
    if (!user) return;

    user.userSettings.filterSortingMode = value;
    this.userService.updateUserSetting(user.id, 'filterSortingMode', value);

    this.messageService.add({
      severity: 'success',
      summary: 'Preferences Updated',
      detail: 'Your filter sorting preference has been saved.',
      life: 1500
    });
  }
}
