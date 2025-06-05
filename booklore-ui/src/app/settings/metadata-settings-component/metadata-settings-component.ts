import {Component, inject, OnInit} from '@angular/core';
import {Divider} from 'primeng/divider';
import {MetadataAdvancedFetchOptionsComponent} from '../../book/metadata/metadata-options-dialog/metadata-advanced-fetch-options/metadata-advanced-fetch-options.component';
import {MetadataProviderSettingsComponent} from '../global-preferences/metadata-provider-settings/metadata-provider-settings.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Tooltip} from 'primeng/tooltip';
import {MetadataRefreshOptions} from '../../book/metadata/model/request/metadata-refresh-options.model';
import {AppSettingsService} from '../../core/service/app-settings.service';
import {MessageService} from 'primeng/api';
import {Observable} from 'rxjs';
import {AppSettingKey, AppSettings} from '../../core/model/app-settings.model';
import {filter, take} from 'rxjs/operators';

@Component({
  selector: 'app-metadata-settings-component',
  imports: [
    Divider,
    MetadataAdvancedFetchOptionsComponent,
    MetadataProviderSettingsComponent,
    ReactiveFormsModule,
    Tooltip,
    FormsModule
  ],
  templateUrl: './metadata-settings-component.html',
  styleUrl: './metadata-settings-component.scss'
})
export class MetadataSettingsComponent implements OnInit {

  currentMetadataOptions!: MetadataRefreshOptions;

  private appSettingsService = inject(AppSettingsService);
  private messageService = inject(MessageService);

  appSettings$: Observable<AppSettings | null> = this.appSettingsService.appSettings$;

  ngOnInit(): void {
    this.appSettings$.pipe(
      filter(settings => !!settings),
      take(1)
    ).subscribe(settings => {
      if (settings?.metadataRefreshOptions) {
        this.currentMetadataOptions = settings.metadataRefreshOptions;
      }
    });
  }

  onMetadataSubmit(metadataRefreshOptions: MetadataRefreshOptions): void {
    this.saveSetting(AppSettingKey.QUICK_BOOK_MATCH, metadataRefreshOptions);
  }

  private saveSetting(key: string, value: unknown): void {
    this.appSettingsService.saveSettings([{key, newValue: value}]).subscribe({
      next: () =>
        this.showMessage('success', 'Settings Saved', 'The settings were successfully saved!'),
      error: () =>
        this.showMessage('error', 'Error', 'There was an error saving the settings.')
    });
  }

  private showMessage(severity: 'success' | 'error', summary: string, detail: string): void {
    this.messageService.add({severity, summary, detail});
  }
}
