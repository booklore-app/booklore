import {Component, inject, OnDestroy, OnInit, Optional} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {UserService} from '../../../settings/user-management/user.service';
import {Book, BookRecommendation} from '../../model/book.model';
import {BehaviorSubject, Observable, Subscription} from 'rxjs';
import {distinctUntilChanged, filter, map, shareReplay, switchMap, take, tap} from 'rxjs/operators';
import {BookService} from '../../service/book.service';
import {AppSettingsService} from '../../../core/service/app-settings.service';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {MetadataViewerComponent} from './metadata-viewer/metadata-viewer.component';
import {MetadataEditorComponent} from './metadata-editor/metadata-editor.component';
import {MetadataSearcherComponent} from './metadata-searcher/metadata-searcher.component';
import {DynamicDialogConfig} from 'primeng/dynamicdialog';
import {BookMetadataHostService} from '../../../book-metadata-host-service';

@Component({
  selector: 'app-book-metadata-center',
  standalone: true,
  templateUrl: './book-metadata-center.component.html',
  imports: [Tabs, TabList, Tab, TabPanels, TabPanel, MetadataViewerComponent, MetadataEditorComponent, MetadataSearcherComponent],
  styleUrls: ['./book-metadata-center.component.scss']
})
export class BookMetadataCenterComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private bookService = inject(BookService);
  private userService = inject(UserService);
  private appSettingsService = inject(AppSettingsService);
  private metadataHostService = inject(BookMetadataHostService);

  book$!: Observable<Book>;
  recommendedBooks: BookRecommendation[] = [];
  tab: string = 'view';
  canEditMetadata: boolean = false;
  admin: boolean = false;

  private userSubscription: Subscription = Subscription.EMPTY;
  private tabSubscription: Subscription = Subscription.EMPTY;
  private recommendationSubscription: Subscription = Subscription.EMPTY;
  private appSettings$ = this.appSettingsService.appSettings$;
  private currentBookId$ = new BehaviorSubject<number | null>(null);

  constructor(@Optional() private config?: DynamicDialogConfig) {
  }

  ngOnInit(): void {
    this.bookService.loadBooks();
    const bookIdFromDialog: number | undefined = this.config?.data?.bookId;
    console.log('a')
    if (bookIdFromDialog != null) {
      this.currentBookId$.next(bookIdFromDialog);
      console.log('b')
    } else {
      this.route.paramMap
        .pipe(
          map(params => Number(params.get('bookId'))),
          filter(bookId => !isNaN(bookId))
        )
        .subscribe(bookId => this.currentBookId$.next(bookId));
    }

    this.metadataHostService.bookSwitches$
      .pipe(
        filter((bookId): bookId is number => !!bookId),
        distinctUntilChanged()
      )
      .subscribe(bookId => {
        console.log('c')
        this.currentBookId$.next(bookId);
      });

    this.book$ = this.currentBookId$.pipe(
      filter((bookId): bookId is number => bookId != null),
      distinctUntilChanged(),
      switchMap(bookId =>
        this.bookService.bookState$.pipe(
          map(state => state.books?.find(b => b.id === bookId)),
          filter((book): book is Book => !!book && !!book.metadata),
          switchMap(book => this.bookService.getBookByIdFromAPI(book.id, true))
        )
      ),
      shareReplay(1)
    );

    this.recommendationSubscription = this.currentBookId$.pipe(
      filter((id): id is number => id != null),
      tap(bookId => this.fetchBookRecommendationsIfNeeded(bookId))
    ).subscribe();

    this.tabSubscription = this.route.queryParamMap.pipe(
      map(params => params.get('tab') ?? 'view'),
      distinctUntilChanged()
    ).subscribe(tab => {
      const validTabs = ['view', 'edit', 'match'];
      this.tab = validTabs.includes(tab) ? tab : 'view';
    });

    this.userSubscription = this.userService.userState$.subscribe(userData => {
      this.canEditMetadata = userData?.permissions?.canEditMetadata ?? false;
      this.admin = userData?.permissions?.admin ?? false;
    });
  }

  private fetchBookRecommendationsIfNeeded(bookId: number): void {
    this.appSettings$
      .pipe(
        filter(settings => settings != null),
        take(1)
      )
      .subscribe(settings => {
        if (settings!.similarBookRecommendation ?? false) {
          this.bookService.getBookRecommendations(bookId).subscribe(recommendations => {
            this.recommendedBooks = recommendations.sort(
              (a, b) => (b.similarityScore ?? 0) - (a.similarityScore ?? 0)
            );
          });
        }
      });
  }

  ngOnDestroy(): void {
    this.userSubscription.unsubscribe();
    this.tabSubscription.unsubscribe();
    this.recommendationSubscription.unsubscribe();
  }
}
