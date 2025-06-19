import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {MetadataEditorComponent} from '../book-metadata-center/metadata-editor/metadata-editor.component';
import {MetadataSearcherComponent} from '../book-metadata-center/metadata-searcher/metadata-searcher.component';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {Book} from '../../model/book.model';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {BookService} from '../../service/book.service';
import {MessageService} from 'primeng/api';
import {Subscription} from 'rxjs';
import {UserService} from '../../../settings/user-management/user.service';
import {BookMetadataCenterService} from '../book-metadata-center/book-metadata-center.service';

@Component({
  selector: 'app-multi-book-metadata-editor-component',
  imports: [
    MetadataEditorComponent,
    MetadataSearcherComponent,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs
  ],
  templateUrl: './multi-book-metadata-editor-component.html',
  standalone: true,
  styleUrl: './multi-book-metadata-editor-component.scss'
})
export class MultiBookMetadataEditorComponent implements OnInit, OnDestroy {

  bookIds: number[] = [];
  books: Book[] = [];
  loading = false;

  private userSubscription: Subscription = Subscription.EMPTY;
  private metadataSubscription: Subscription = Subscription.EMPTY;
  canEditMetadata: boolean = false;
  admin: boolean = false;
  currentIndex = 0;

  private readonly config = inject(DynamicDialogConfig);
  private readonly ref = inject(DynamicDialogRef);
  private readonly bookService = inject(BookService);
  private readonly messageService = inject(MessageService);
  private userService = inject(UserService);
  private metadataCenterService = inject(BookMetadataCenterService);

  ngOnInit(): void {
    this.bookIds = this.config.data?.bookIds ?? [];

    this.userSubscription = this.userService.userState$.subscribe(userData => {
      const userPermissions = userData?.permissions;
      this.canEditMetadata = userPermissions?.canEditMetadata ?? false;
      this.admin = userPermissions?.admin ?? false;
    });

    this.metadataSubscription = this.metadataCenterService.currentMetadata$.subscribe(updatedMetadata => {
      if (updatedMetadata) {
        const index = this.books.findIndex(book => book.metadata?.bookId === updatedMetadata.bookId);
        if (index !== -1) {
          this.books[index] = {
            ...this.books[index],
            metadata: updatedMetadata
          };
        }
      }
    });

    this.loading = true;
    this.bookService.getBooksByIdsFromAPI(this.bookIds, true).subscribe({
      next: (fetchedBooks) => {
        this.books = fetchedBooks;
        this.loading = false;

        if (this.books.length > 0) {
          this.metadataCenterService.emitBookChanged(this.books[this.currentIndex]);
          this.metadataCenterService.emitMetadata(this.books[this.currentIndex].metadata!);
        }
      },
      error: (err) => {
        this.loading = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to load books',
          detail: err.message || 'An error occurred while fetching books.'
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.userSubscription.unsubscribe();
    this.metadataSubscription.unsubscribe();
    this.metadataCenterService.emitBookChanged(null);
  }

  handleNextBook() {
    if (this.currentIndex < this.books.length - 1) {
      this.currentIndex++;
      const book = this.books[this.currentIndex];
      this.metadataCenterService.emitBookChanged(book);
      this.metadataCenterService.emitMetadata(book.metadata!);
    }
  }

  handlePreviousBook() {
    if (this.currentIndex > 0) {
      this.currentIndex--;
      const book = this.books[this.currentIndex];
      this.metadataCenterService.emitBookChanged(book);
      this.metadataCenterService.emitMetadata(book.metadata!);
    }
  }

  handleCloseDialogButton() {
    this.ref.close();
  }
}
