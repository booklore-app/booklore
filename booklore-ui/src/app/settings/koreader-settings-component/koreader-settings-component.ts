import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {Button} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {MessageService} from 'primeng/api';
import {KoreaderService} from '../../koreader-service';

@Component({
  standalone: true,
  selector: 'app-koreader-settings-component',
  imports: [
    CommonModule,
    FormsModule,
    InputText,
    ToggleSwitch,
    Button,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './koreader-settings-component.html',
  styleUrls: ['./koreader-settings-component.scss']
})
export class KoreaderSettingsComponent implements OnInit {
  editMode = true;
  showPassword = false;
  koReaderSyncEnabled = false;
  koReaderUsername = '';
  koReaderPassword = '';
  credentialsSaved = false;
  readonly koreaderEndpoint = `${window.location.origin}/api/koreader`;

  private readonly messageService = inject(MessageService);
  private readonly koreaderService = inject(KoreaderService);

  ngOnInit() {
    this.koreaderService.getUser().subscribe({
      next: koreaderUser => {
        this.koReaderUsername = koreaderUser.username;
        this.koReaderPassword = koreaderUser.password;
        this.koReaderSyncEnabled = koreaderUser.syncEnabled;
        this.credentialsSaved = true;
      },
      error: err => {
        if (err.status === 404) {
          this.messageService.add({severity: 'warn', summary: 'User Not Found', detail: 'No KOReader account found. Please create one to enable sync.', life: 5000});
        } else {
          this.messageService.add({severity: 'error', summary: 'Load Error', detail: 'Unable to retrieve KOReader account. Please try again.'});
        }
      }
    });
  }

  get canSave(): boolean {
    const u = this.koReaderUsername?.trim() ?? '';
    const p = this.koReaderPassword ?? '';
    return u.length > 0 && p.length >= 6;
  }

  onEditSave() {
    if (!this.editMode) {
      this.saveCredentials();
    }
    this.editMode = !this.editMode;
  }

  onToggleEnabled(enabled: boolean) {
    this.koreaderService.toggleSync(enabled).subscribe({
      next: () => {
        this.koReaderSyncEnabled = enabled;
        this.messageService.add({severity: 'success', summary: 'Sync Updated', detail: `KOReader sync has been ${enabled ? 'enabled' : 'disabled'}.`});
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Update Failed', detail: 'Unable to update KOReader sync setting. Please try again.'});
      }
    });
  }

  toggleShowPassword() {
    this.showPassword = !this.showPassword;
  }

  saveCredentials() {
    this.koreaderService.createUser(this.koReaderUsername, this.koReaderPassword)
      .subscribe({
        next: () => {
          this.credentialsSaved = true;
          this.messageService.add({severity: 'success', summary: 'Saved', detail: 'KOReader account saved successfully.'});
        },
        error: () =>
          this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to save KOReader credentials. Please try again.'})
      });
  }

  copyText(text: string) {
    if (!text) {
      return;
    }
    navigator.clipboard.writeText(text).catch(err => {
      console.error('Copy failed', err);
    });
  }
}
