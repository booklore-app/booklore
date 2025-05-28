import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Select} from 'primeng/select';
import {Tooltip} from 'primeng/tooltip';
import {SidebarLibrarySorting, SidebarShelfSorting, User, UserService, UserSettings} from '../../user-management/user.service';
import {MessageService} from 'primeng/api';
import {Observable, Subscription} from 'rxjs';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-sidebar-sorting-preferences',
  imports: [
    Select,
    Tooltip,
    FormsModule
  ],
  templateUrl: './sidebar-sorting-preferences.component.html',
  styleUrl: './sidebar-sorting-preferences.component.scss'
})
export class SidebarSortingPreferencesComponent implements OnInit, OnDestroy {

  readonly sortingOptions = [
    {label: 'Name | Ascending', value: {field: 'name', order: 'asc'}},
    {label: 'Name | Descending', value: {field: 'name', order: 'desc'}},
    {label: 'Creation Date | Ascending', value: {field: 'id', order: 'asc'}},
    {label: 'Creation Date | Descending', value: {field: 'id', order: 'desc'}},
  ];

  selectedLibrarySorting: SidebarLibrarySorting = {field: 'id', order: 'asc'};
  selectedShelfSorting: SidebarShelfSorting = {field: 'id', order: 'asc'};

  private readonly userService = inject(UserService);
  private readonly messageService = inject(MessageService);

  userData$: Observable<User | null> = this.userService.userData$;
  private subscription?: Subscription;
  private currentUser: User | null = null;

  ngOnInit(): void {
    this.subscription = this.userData$.subscribe(user => {
      if (user) {
        this.currentUser = user;
        this.loadPreferences(user.userSettings);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  private loadPreferences(settings: UserSettings): void {
    this.selectedLibrarySorting = settings.sidebarLibrarySorting;
    this.selectedShelfSorting = settings.sidebarShelfSorting;
  }


  private updatePreference(path: string[], value: any): void {
    if (!this.currentUser) return;
    let target: any = this.currentUser.userSettings;
    for (let i = 0; i < path.length - 1; i++) {
      target = target[path[i]] ||= {};
    }
    target[path.at(-1)!] = value;

    const [rootKey] = path;
    const updatedValue = this.currentUser.userSettings[rootKey as keyof UserSettings];
    this.userService.updateUserSetting(this.currentUser.id, rootKey, updatedValue);
    this.messageService.add({
      severity: 'success',
      summary: 'Preferences Updated',
      detail: 'Your preferences have been saved successfully.',
      life: 2000
    });
  }

  onLibrarySortingChange() {
    this.updatePreference(['sidebarLibrarySorting'], this.selectedLibrarySorting);
  }

  onShelfSortingChange() {
    this.updatePreference(['sidebarShelfSorting'], this.selectedShelfSorting);
  }

}
