import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {MetadataEditorComponent} from '../book-metadata-center-component/metadata-editor/metadata-editor.component';
import {MetadataSearcherComponent} from '../book-metadata-center-component/metadata-searcher/metadata-searcher.component';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {Book} from '../../book/model/book.model';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {BookService} from '../../book/service/book.service';
import {BehaviorSubject, combineLatest, Observable, Subscription} from 'rxjs';
import {UserService} from '../../settings/user-management/user.service';
import {distinctUntilChanged, filter, map, shareReplay, switchMap} from 'rxjs/operators';

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
  filteredBooks: Book[] = [];
  loading = false;

  currentIndex$ = new BehaviorSubject<number>(0);
  book$!: Observable<Book>;

  canEditMetadata = false;
  admin = false;

  private readonly config = inject(DynamicDialogConfig);
  private readonly ref = inject(DynamicDialogRef);
  private readonly bookService = inject(BookService);
  private readonly userService = inject(UserService);

  private userSubscription: Subscription = Subscription.EMPTY;

  ngOnInit(): void {
    this.bookIds = this.config.data?.bookIds ?? [];

    this.userSubscription = this.userService.userState$.subscribe(userData => {
      const userPermissions = userData?.permissions;
      this.canEditMetadata = userPermissions?.canEditMetadata ?? false;
      this.admin = userPermissions?.admin ?? false;
    });

    const filteredBooks$ = this.bookService.bookState$.pipe(
      map(state => state.books?.filter(book =>
        !!book?.metadata && this.bookIds.includes(book.id)
      ) ?? []),
      filter(books => books.length > 0),
      map(books => {
        this.filteredBooks = books;
        return books;
      })
    );

    this.book$ = combineLatest([filteredBooks$, this.currentIndex$]).pipe(
      map(([books, index]) => books[index]),
      filter((book): book is Book => !!book),
      distinctUntilChanged((a, b) => a.id === b.id && a.metadata === b.metadata),
      switchMap(book =>
        this.bookService.getBookByIdFromAPI(book.id, true)
      ),
      shareReplay(1)
    );
  }

  ngOnDestroy(): void {
    this.userSubscription.unsubscribe();
    this.currentIndex$.complete();
  }

  handleNextBook() {
    const next = this.currentIndex$.value + 1;
    if (next < this.filteredBooks.length) {
      this.currentIndex$.next(next);
    }
  }

  handlePreviousBook() {
    const prev = this.currentIndex$.value - 1;
    if (prev >= 0) {
      this.currentIndex$.next(prev);
    }
  }

  handleCloseDialogButton() {
    this.ref.close();
  }

  get disableNext(): boolean {
    return this.currentIndex$.value >= this.filteredBooks.length - 1;
  }

  get disablePrevious(): boolean {
    return this.currentIndex$.value <= 0;
  }
}
