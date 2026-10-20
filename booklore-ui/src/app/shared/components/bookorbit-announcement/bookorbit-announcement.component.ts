import {AsyncPipe} from '@angular/common';
import {Component, inject} from '@angular/core';
import {TranslocoDirective} from '@jsverse/transloco';
import {UserService} from '../../../features/settings/user-management/user.service';
import {LocalStorageService} from '../../service/local-storage.service';

const DISMISSED_STORAGE_KEY = 'bookorbit-successor-banner-v1';

@Component({
  selector: 'app-bookorbit-announcement',
  standalone: true,
  imports: [AsyncPipe, TranslocoDirective],
  templateUrl: './bookorbit-announcement.component.html',
  styleUrl: './bookorbit-announcement.component.scss'
})
export class BookOrbitAnnouncementComponent {
  protected readonly userService = inject(UserService);
  private readonly localStorageService = inject(LocalStorageService);

  protected dismissed = this.localStorageService.get<boolean>(DISMISSED_STORAGE_KEY) ?? false;

  protected dismiss(): void {
    this.dismissed = true;
    this.localStorageService.set(DISMISSED_STORAGE_KEY, true);
  }
}
