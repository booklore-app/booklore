import {Component, inject, OnInit} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TableModule} from 'primeng/table';
import {Tooltip} from 'primeng/tooltip';
import {Checkbox} from 'primeng/checkbox';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {AppSettingsService} from '../../../core/service/app-settings.service';
import {filter, take} from 'rxjs/operators';
import {MessageService} from 'primeng/api';
import {AppSettingKey} from '../../../core/model/app-settings.model';

@Component({
  selector: 'app-metadata-provider-settings',
  imports: [
    ReactiveFormsModule,
    TableModule,
    Tooltip,
    Checkbox,
    InputText,
    Button,
    FormsModule
  ],
  templateUrl: './metadata-provider-settings.component.html',
  styleUrl: './metadata-provider-settings.component.scss'
})
export class MetadataProviderSettingsComponent implements OnInit {
  hardcoverToken: string = '';
  amazonCookie: string = '';
  hardcoverEnabled: boolean = false;
  amazonEnabled: boolean = false;
  goodreadsEnabled: boolean = false;
  googleEnabled: boolean = false;

  private appSettingsService = inject(AppSettingsService);
  private messageService = inject(MessageService);

  private appSettings$ = this.appSettingsService.appSettings$;

  ngOnInit(): void {
    this.appSettings$
      .pipe(
        filter(settings => settings != null),
        take(1)
      )
      .subscribe(settings => {
        const metadataProviderSettings = settings!.metadataProviderSettings;
        this.amazonEnabled = metadataProviderSettings?.amazon?.enabled ?? false;
        this.amazonCookie = metadataProviderSettings?.amazon?.cookie ?? "";
        this.goodreadsEnabled = metadataProviderSettings?.goodReads?.enabled ?? false;
        this.googleEnabled = metadataProviderSettings?.google?.enabled ?? false;
        this.hardcoverToken = metadataProviderSettings?.hardcover?.apiKey ?? '';
        this.hardcoverEnabled = metadataProviderSettings?.hardcover?.enabled ?? false;
      });
  }

  onTokenChange(newToken: string): void {
    this.hardcoverToken = newToken;
    if (!newToken.trim()) {
      this.hardcoverEnabled = false;
    }
  }

  saveSettings(): void {
    const payload = [
      {
        key: AppSettingKey.METADATA_PROVIDER_SETTINGS,
        newValue: {
          amazon: {
            enabled: this.amazonEnabled,
            cookie: this.amazonCookie
          },
          goodReads: {enabled: this.goodreadsEnabled},
          google: {enabled: this.googleEnabled},
          hardcover: {
            enabled: this.hardcoverEnabled,
            apiKey: this.hardcoverToken.trim()
          }
        }
      }
    ];

    this.appSettingsService.saveSettings(payload).subscribe({
      next: () =>
        this.messageService.add({
          severity: 'success',
          summary: 'Saved',
          detail: 'Metadata provider settings saved.'
        }),
      error: () =>
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to save metadata provider settings.'
        })
    });
  }
}
