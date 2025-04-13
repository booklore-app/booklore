import {Component, inject, OnInit} from '@angular/core';
import {Divider} from "primeng/divider";
import {DropdownModule} from 'primeng/dropdown';
import {FormsModule} from '@angular/forms';
import {AppSettingsService} from '../core/service/app-settings.service';
import {Observable} from 'rxjs';
import {AppSettings} from '../core/model/app-settings.model';
import {Select} from 'primeng/select';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {MessageService} from 'primeng/api';
import {BookService} from '../book/service/book.service';

@Component({
  selector: 'app-global-preferences',
  imports: [
    Divider,
    DropdownModule,
    FormsModule,
    Select,
    Button,
    Tooltip
  ],
  templateUrl: './global-preferences.component.html',
  styleUrl: './global-preferences.component.scss'
})
export class GlobalPreferencesComponent implements OnInit {

  resolutionOptions = [
    {label: '250x350', value: '250x350'},
    {label: '375x525', value: '375x525'},
    {label: '500x700', value: '500x700'},
    {label: '625x875', value: '625x875'}
  ];

  selectedResolution: string | undefined = '250x350';

  private appSettingsService = inject(AppSettingsService);
  private messageService = inject(MessageService);
  private bookService = inject(BookService);

  appSettings$: Observable<AppSettings | null> = this.appSettingsService.appSettings$;

  ngOnInit(): void {
    this.appSettings$.subscribe(settings => {
      if (settings?.coverSettings) {
        this.selectedResolution = settings.coverSettings.resolution;
      }
    });
  }

  onResolutionChange(): void {
    if (this.selectedResolution) {
      this.onSaveSettings();
    }
  }

  onSaveSettings(): void {
    const settingsToSave = [
      {key: 'cover_image_resolution', newValue: this.selectedResolution}
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

  regenerateCovers(): void {
    this.bookService.regenerateCovers().subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Cover Regeneration Started',
          detail: 'Book covers are being regenerated.'
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to start cover regeneration.'
        });
      }
    });
  }
}
