import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {UserService} from './user-management/user.service';
import { AsyncPipe } from '@angular/common';
import {EmailComponent} from './email/email.component';
import {GlobalPreferencesComponent} from './global-preferences/global-preferences.component';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {UserManagementComponent} from './user-management/user-management.component';
import {AuthenticationSettingsComponent} from '../core/security/oauth2-management/authentication-settings.component';
import {ViewPreferencesParentComponent} from './view-preferences-parent/view-preferences-parent.component';
import {ReaderPreferences} from './reader-preferences/reader-preferences.component';
import {MetadataSettingsComponent} from './metadata-settings-component/metadata-settings-component';

export enum SettingsTab {
  ReaderSettings = 'reader-settings',
  ViewPreferences = 'view-settings',
  UserManagement = 'user-management',
  EmailSettings = 'email-settings',
  MetadataSettings = 'metadata-settings',
  ApplicationSettings = 'app-settings',
  AuthenticationSettings = 'auth-settings'
}

@Component({
  selector: 'app-settings',
  imports: [
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
    AsyncPipe,
    EmailComponent,
    GlobalPreferencesComponent,
    UserManagementComponent,
    AuthenticationSettingsComponent,
    ViewPreferencesParentComponent,
    ReaderPreferences,
    MetadataSettingsComponent
],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent implements OnInit, OnDestroy {

  protected userService = inject(UserService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private routeSub!: Subscription;

  SettingsTab = SettingsTab;

  private validTabs = Object.values(SettingsTab);
  private _activeTab: SettingsTab = SettingsTab.ReaderSettings;

  get activeTab(): SettingsTab {
    return this._activeTab;
  }

  set activeTab(value: SettingsTab) {
    this._activeTab = value;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: value },
      queryParamsHandling: 'merge'
    });
  }

  ngOnInit(): void {
    this.routeSub = this.route.queryParams.subscribe(params => {
      const tabParam = params['tab'];
      if (this.validTabs.includes(tabParam)) {
        this._activeTab = tabParam as SettingsTab;
      } else {
        this._activeTab = SettingsTab.ReaderSettings;
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { tab: this._activeTab },
          queryParamsHandling: 'merge',
          replaceUrl: true
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub.unsubscribe();
  }
}
