import {Component, inject, OnInit} from '@angular/core';
import {Divider} from 'primeng/divider';
import {MetadataAdvancedFetchOptionsComponent} from '../../metadata/metadata-options-dialog/metadata-advanced-fetch-options/metadata-advanced-fetch-options.component';
import {MetadataProviderSettingsComponent} from '../global-preferences/metadata-provider-settings/metadata-provider-settings.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Tooltip} from 'primeng/tooltip';
import {MetadataRefreshOptions} from '../../metadata/model/request/metadata-refresh-options.model';
import {AppSettingsService} from '../../core/service/app-settings.service';
import {MessageService} from 'primeng/api';
import {Observable} from 'rxjs';
import {AppSettingKey, AppSettings, MetadataPersistenceSettings} from '../../core/model/app-settings.model';
import {filter, take} from 'rxjs/operators';
import {MetadataMatchWeightsComponent} from '../global-preferences/metadata-match-weights-component/metadata-match-weights-component';
import {ToggleSwitch} from 'primeng/toggleswitch';

@Component({
  selector: 'app-metadata-settings-component',
  standalone: true,
  imports: [
    Divider,
    MetadataAdvancedFetchOptionsComponent,
    MetadataProviderSettingsComponent,
    ReactiveFormsModule,
    Tooltip,
    FormsModule,
    MetadataMatchWeightsComponent,
    ToggleSwitch
  ],
  templateUrl: './metadata-settings-component.html',
  styleUrl: './metadata-settings-component.scss'
})
export class MetadataSettingsComponent implements OnInit {

  currentMetadataOptions!: MetadataRefreshOptions;
  metadataPersistence: MetadataPersistenceSettings = {
    saveToOriginalFile: false,
    backupMetadata: true,
    backupCover: true
  };
  metadataDownloadOnBookdrop: boolean = true;

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
      if (settings?.metadataPersistenceSettings) {
        this.metadataPersistence = {...settings.metadataPersistenceSettings};
      }
      this.metadataDownloadOnBookdrop = settings?.metadataDownloadOnBookdrop;
    });
  }

  onMetadataDownloadOnBookdropToggle(checked: boolean) {
    this.saveSetting(AppSettingKey.METADATA_DOWNLOAD_ON_BOOKDROP, checked);
  }

  onPersistenceToggle(key: keyof MetadataPersistenceSettings): void {
    if (key === 'saveToOriginalFile') {
      this.metadataPersistence.saveToOriginalFile = !this.metadataPersistence.saveToOriginalFile;

      if (!this.metadataPersistence.saveToOriginalFile) {
        this.metadataPersistence.backupMetadata = false;
        this.metadataPersistence.backupCover = false;
      }
    } else {
      this.metadataPersistence[key] = !this.metadataPersistence[key];
    }

    this.saveSetting(AppSettingKey.METADATA_PERSISTENCE_SETTINGS, this.metadataPersistence);
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

  protected readonly AppSettingKey = AppSettingKey;
}
