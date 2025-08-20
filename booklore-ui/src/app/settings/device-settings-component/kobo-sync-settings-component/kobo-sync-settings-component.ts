import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {ConfirmationService, MessageService} from 'primeng/api';
import {Clipboard} from '@angular/cdk/clipboard';
import {KoboService, KoboSyncSettings} from '../kobo.service';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {UserService} from '../../user-management/user.service';
import {Subject} from 'rxjs';
import {filter, takeUntil} from 'rxjs/operators';

@Component({
  selector: 'app-kobo-sync-setting-component',
  standalone: true,
  templateUrl: './kobo-sync-settings-component.html',
  styleUrl: './kobo-sync-settings-component.scss',
  imports: [FormsModule, Button, InputText, ConfirmDialog],
  providers: [MessageService, ConfirmationService]
})
export class KoboSyncSettingsComponent implements OnInit, OnDestroy {
  private koboService = inject(KoboService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private clipboard = inject(Clipboard);
  protected userService = inject(UserService);

  private readonly destroy$ = new Subject<void>();
  hasPermission = false;

  koboToken = '';
  credentialsSaved = false;
  showToken = false;

  ngOnInit() {
    this.userService.userState$.pipe(
      filter(userState => !!userState?.user && userState.loaded),
      takeUntil(this.destroy$)
    ).subscribe(userState => {
      this.hasPermission = (userState.user?.permissions.canSyncKobo || userState.user?.permissions.admin) ?? false;
      if (this.hasPermission) {
        this.koboService.getUser().subscribe({
          next: (settings: KoboSyncSettings) => {
            this.koboToken = settings.token;
            this.credentialsSaved = !!settings.token;
          },
          error: () => {
            this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to load Kobo settings'});
          }
        });
      }
    });
  }

  copyText(text: string) {
    this.clipboard.copy(text);
    this.messageService.add({severity: 'success', summary: 'Copied', detail: 'Token copied to clipboard'});
  }

  toggleShowToken() {
    this.showToken = !this.showToken;
  }

  confirmRegenerateToken() {
    this.confirmationService.confirm({
      message: 'This will generate a new token and invalidate the previous one. Continue?',
      header: 'Confirm Regeneration',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.regenerateToken()
    });
  }

  private regenerateToken() {
    this.koboService.createOrUpdateToken().subscribe({
      next: (settings) => {
        this.koboToken = settings.token;
        this.credentialsSaved = true;
        this.messageService.add({severity: 'success', summary: 'Token regenerated', detail: 'New token generated successfully'});
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to regenerate token'});
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
