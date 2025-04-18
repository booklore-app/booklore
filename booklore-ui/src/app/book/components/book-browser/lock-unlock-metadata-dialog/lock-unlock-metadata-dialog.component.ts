import {Component, inject} from '@angular/core';
import {Button} from 'primeng/button';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {MessageService} from 'primeng/api';
import {BookService} from '../../../service/book.service';

@Component({
  selector: 'app-lock-unlock-metadata-dialog',
  imports: [
    Button
  ],
  templateUrl: './lock-unlock-metadata-dialog.component.html',
  styleUrl: './lock-unlock-metadata-dialog.component.scss'
})
export class LockUnlockMetadataDialogComponent {
  private bookService = inject(BookService);
  private dynamicDialogConfig = inject(DynamicDialogConfig);
  private dialogRef = inject(DynamicDialogRef);
  private messageService = inject(MessageService);

  bookIds: Set<number> = this.dynamicDialogConfig.data.bookIds;

  toggleLock(action: 'LOCK' | 'UNLOCK'): void {
    this.bookService.toggleAllLock(this.bookIds, action).subscribe({
      next: () => {
        const isLock = action === 'LOCK';
        this.messageService.add({
          severity: 'success',
          summary: `Metadata ${isLock ? 'Locked' : 'Unlocked'}`,
          detail: `All selected books have been ${isLock ? 'locked' : 'unlocked'} successfully.`,
        });
        this.dialogRef.close(action.toLowerCase());
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: `Failed to ${action === 'LOCK' ? 'Lock' : 'Unlock'}`,
          detail: `An error occurred while ${action === 'LOCK' ? 'locking' : 'unlocking'} metadata.`,
        });
      }
    });
  }
}
