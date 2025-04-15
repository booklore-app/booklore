import {Component, inject} from '@angular/core';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {BookPreferences} from './book-preferences/book-preferences.component';
import {AdminComponent} from './admin/admin.component';
import {UserService} from '../user.service';
import {AsyncPipe, NgIf} from '@angular/common';
import {EmailComponent} from './email/email.component';
import {GlobalPreferencesComponent} from './global-preferences/global-preferences.component';

@Component({
  selector: 'app-settings',
  imports: [
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
    BookPreferences,
    AdminComponent,
    NgIf,
    AsyncPipe,
    EmailComponent,
    GlobalPreferencesComponent
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {

  protected userService = inject(UserService);

}
