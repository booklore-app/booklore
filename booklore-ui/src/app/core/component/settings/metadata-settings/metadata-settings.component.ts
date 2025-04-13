import {Component, inject, OnInit} from '@angular/core';
import {MetadataAdvancedFetchOptionsComponent} from '../../../../metadata/metadata-options-dialog/metadata-advanced-fetch-options/metadata-advanced-fetch-options.component';
import {MetadataRefreshOptions} from '../../../../metadata/model/request/metadata-refresh-options.model';
import {AppSettingsService} from '../../../service/app-settings.service';
import {Observable} from 'rxjs';
import {AppSettings} from '../../../model/app-settings.model';
import {Tooltip} from 'primeng/tooltip';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-metadata-settings',
  imports: [
    MetadataAdvancedFetchOptionsComponent,
    Tooltip
  ],
  templateUrl: './metadata-settings.component.html',
  styleUrl: './metadata-settings.component.scss'
})
export class MetadataSettingsComponent implements OnInit {

  currentMetadataOptions!: MetadataRefreshOptions;

  private appSettingsService = inject(AppSettingsService);
  private messageService = inject(MessageService);

  appSettings$: Observable<AppSettings | null> = this.appSettingsService.appSettings$;

  ngOnInit(): void {
    this.appSettings$.subscribe(settings => {
      if (settings) {
        this.currentMetadataOptions = settings.metadataRefreshOptions;
      }
    });
  }

  onMetadataSubmit(metadataRefreshOptions: MetadataRefreshOptions) {
    const settingsToSave = [
      {key: 'quick_book_match', newValue: metadataRefreshOptions}
    ];

    this.appSettingsService.saveSettings(settingsToSave).subscribe({
      next: () => {
        this.messageService.add({severity: 'success', summary: 'Settings Saved', detail: 'The settings were successfully saved!'});
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'There was an error saving the settings.'});
      }
    });
  }
}
