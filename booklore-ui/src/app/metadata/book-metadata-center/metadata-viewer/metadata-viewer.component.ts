import {Component, inject, OnInit} from '@angular/core';
import {Button} from 'primeng/button';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';
import {Observable} from 'rxjs';
import {BookService} from '../../../book/service/book.service';
import {BookMetadataCenterService} from '../book-metadata-center.service';
import {Rating} from 'primeng/rating';
import {FormsModule} from '@angular/forms';
import {Tag} from 'primeng/tag';
import {BookMetadata} from '../../../book/model/book.model';
import {Divider} from 'primeng/divider';
import {UrlHelperService} from '../../../utilities/service/url-helper.service';
import {UserService} from '../../../user.service';
import {SplitButton} from 'primeng/splitbutton';
import {MenuItem, MessageService} from 'primeng/api';
import {BookSenderComponent} from '../../../book/components/book-sender/book-sender.component';
import {DialogService} from 'primeng/dynamicdialog';
import {EmailService} from '../../../settings/email/email.service';
import {ShelfAssignerComponent} from '../../../book/components/shelf-assigner/shelf-assigner.component';

@Component({
  selector: 'app-metadata-viewer',
  standalone: true,
  templateUrl: './metadata-viewer.component.html',
  styleUrl: './metadata-viewer.component.scss',
  imports: [Button, NgForOf, NgIf, AsyncPipe, Rating, FormsModule, Tag, Divider, SplitButton]
})
export class MetadataViewerComponent implements OnInit {

  private dialogService = inject(DialogService);
  private emailService = inject(EmailService);
  private messageService = inject(MessageService);
  private bookService = inject(BookService);
  private metadataCenterService = inject(BookMetadataCenterService);

  protected urlHelper = inject(UrlHelperService);
  protected userService = inject(UserService);

  metadata$: Observable<BookMetadata | null> = this.metadataCenterService.currentMetadata$;
  items: MenuItem[] | undefined;


  ngOnInit(): void {
    this.items = [
      {
        label: 'Custom Send',
        command: () => {
          this.metadata$.subscribe((metadata) => {
            if (metadata) {
              this.dialogService.open(BookSenderComponent, {
                header: 'Send Book to Email',
                modal: true,
                closable: true,
                style: {
                  position: 'absolute',
                  top: '15%',
                },
                data: {
                  book: metadata.bookId,
                }
              });
            }
          });
        }
      }
    ];
  }

  read(bookId: number): void {
    this.bookService.readBook(bookId);
  }

  closeDialog(): void {
    this.metadataCenterService.closeDialog(true);
  }

  getAuthorNames(authors: string[]): string {
    return authors.join(', ');
  }

  download(bookId: number) {
    this.bookService.downloadFile(bookId);
  }

  quickSend(bookId: number) {
    this.emailService.emailBookQuick(bookId).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'info',
          summary: 'Success',
          detail: 'The book sending has been scheduled.',
        });
      },
      error: (err) => {
        const errorMessage = err?.error?.message || 'An error occurred while sending the book.';
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: errorMessage,
        });
      },
    });
  }

  assignShelf(bookId: number) {
    this.dialogService.open(ShelfAssignerComponent, {
      header: `Update Book's Shelves`,
      modal: true,
      closable: true,
      contentStyle: {overflow: 'auto'},
      baseZIndex: 10,
      style: {
        position: 'absolute',
        top: '15%',
      },
      data: {
        book: this.bookService.getBookByIdFromState(bookId),
      },
    });
  }
}
