import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {UserService} from '../../../settings/user-management/user.service';
import {Book, BookRecommendation} from '../../model/book.model';
import {Observable, Subscription} from 'rxjs';
import {distinctUntilChanged, filter, map, shareReplay, switchMap, take, tap} from 'rxjs/operators';
import {BookService} from '../../service/book.service';
import {AppSettingsService} from '../../../core/service/app-settings.service';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {MetadataViewerComponent} from './metadata-viewer/metadata-viewer.component';
import {MetadataEditorComponent} from './metadata-editor/metadata-editor.component';
import {MetadataSearcherComponent} from './metadata-searcher/metadata-searcher.component';

@Component({
  selector: 'app-book-metadata-center',
  standalone: true,
  templateUrl: './book-metadata-center.component.html',
  imports: [
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
    MetadataViewerComponent,
    MetadataEditorComponent,
    MetadataSearcherComponent
  ],
  styleUrls: ['./book-metadata-center.component.scss']
})
export class BookMetadataCenterComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private bookService = inject(BookService);
  private userService = inject(UserService);
  private appSettingsService = inject(AppSettingsService);

  book$!: Observable<Book>;
  recommendedBooks: BookRecommendation[] = [];
  tab: string = 'view';
  canEditMetadata: boolean = false;
  admin: boolean = false;

  private userSubscription: Subscription = Subscription.EMPTY;
  private routeSubscription: Subscription = Subscription.EMPTY;
  private tabSubscription: Subscription = Subscription.EMPTY;
  private appSettings$ = this.appSettingsService.appSettings$;

  ngOnInit(): void {
    this.bookService.loadBooks();

    this.book$ = this.route.paramMap.pipe(
      map(params => Number(params.get('bookId'))),
      filter(bookId => !isNaN(bookId)),
      distinctUntilChanged(),
      switchMap(bookId =>
        this.bookService.bookState$.pipe(
          map(state => state.books?.find(b => b.id === bookId)),
          filter((book): book is Book => !!book && !!book.metadata),
          distinctUntilChanged((a, b) => a.id === b.id && a.metadata === b.metadata),
          switchMap(book =>
            this.bookService.getBookByIdFromAPI(book.id, true).pipe(
              tap(bookWithDescription => {
                this.fetchBookRecommendationsIfNeeded(bookWithDescription.metadata!.bookId);
              })
            )
          )
        )
      ),
      shareReplay(1)
    );

    this.tabSubscription = this.route.queryParamMap.pipe(
      map(queryParams => queryParams.get('tab') ?? 'view'),
      distinctUntilChanged()
    ).subscribe(tab => {
      const validTabs = ['view', 'edit', 'match'];
      this.tab = validTabs.includes(tab) ? tab : 'view';
    });

    this.userSubscription = this.userService.userState$.subscribe(userData => {
      const userPermissions = userData?.permissions;
      this.canEditMetadata = userPermissions?.canEditMetadata ?? false;
      this.admin = userPermissions?.admin ?? false;
    });
  }

  ngOnDestroy(): void {
    this.userSubscription.unsubscribe();
    this.routeSubscription.unsubscribe();
    this.tabSubscription.unsubscribe();
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
            this.recommendedBooks = recommendations;
          });
        }
      });
  }
}
