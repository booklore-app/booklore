import {inject, Injectable} from '@angular/core';
import {ConfirmationService, MessageService} from 'primeng/api';
import {MenuItem} from 'primeng/api';
import {BookService} from './book.service';
import {readStatusLabels} from '../components/book-browser/book-filter/book-filter.component';
import {ReadStatus} from '../model/book.model';

@Injectable({
  providedIn: 'root'
})
export class BookMenuService {

  confirmationService = inject(ConfirmationService);
  messageService = inject(MessageService);
  bookService = inject(BookService);


  getMetadataMenuItems(updateMetadata: () => void, bulkEditMetadata: () => void, multiBookEditMetadata: () => void): MenuItem[] {
    return [
      {
        label: 'Refresh Metadata',
        icon: 'pi pi-sync',
        command: updateMetadata
      },
      {
        label: 'Bulk Metadata Editor',
        icon: 'pi pi-table',
        command: bulkEditMetadata
      },
      {
        label: 'Multi-Book Metadata Editor',
        icon: 'pi pi-clone',
        command: multiBookEditMetadata
      }
    ];
  }

  getTieredMenuItems(selectedBooks: Set<number>): MenuItem[] {
    return [
      {
        label: 'Update Read Status',
        icon: 'pi pi-book',
        items: Object.entries(readStatusLabels).map(([status, label]) => ({
          label,
          command: () => {
            this.confirmationService.confirm({
              message: `Are you sure you want to mark selected books as "${label}"?`,
              header: 'Confirm Read Status Update',
              icon: 'pi pi-exclamation-triangle',
              acceptLabel: 'Yes',
              rejectLabel: 'No',
              accept: () => {
                this.bookService.updateBookReadStatus(Array.from(selectedBooks), status as ReadStatus).subscribe({
                  next: () => {
                    this.messageService.add({
                      severity: 'success',
                      summary: 'Read Status Updated',
                      detail: `Marked as "${label}"`,
                      life: 2000
                    });
                  },
                  error: () => {
                    this.messageService.add({
                      severity: 'error',
                      summary: 'Update Failed',
                      detail: 'Could not update read status.',
                      life: 3000
                    });
                  }
                });
              }
            });
          }
        }))
      },
      {
        label: 'Reset Progress',
        icon: 'pi pi-undo',
        command: () => {
          this.confirmationService.confirm({
            message: 'Are you sure you want to reset progress for selected books?',
            header: 'Confirm Reset',
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Yes',
            rejectLabel: 'No',
            accept: () => {
              this.bookService.resetProgress(Array.from(selectedBooks)).subscribe({
                next: () => {
                  this.messageService.add({
                    severity: 'success',
                    summary: 'Progress Reset',
                    detail: 'Reading progress has been reset.',
                    life: 1500
                  });
                },
                error: () => {
                  this.messageService.add({
                    severity: 'error',
                    summary: 'Failed',
                    detail: 'Could not reset progress.',
                    life: 1500
                  });
                }
              });
            }
          });
        }
      }
    ];
  }
}
