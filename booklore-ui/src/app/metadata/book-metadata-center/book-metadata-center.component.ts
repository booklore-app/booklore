import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {BookMetadataCenterService} from './book-metadata-center.service';
import {UserService} from '../../user.service';
import {Book, BookRecommendation} from '../../book/model/book.model';
import {Subscription, switchMap} from 'rxjs';
import {filter, take} from 'rxjs/operators';
import {BookService} from '../../book/service/book.service';
import {AppSettingsService} from '../../core/service/app-settings.service';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {NgIf} from '@angular/common';
import {MetadataViewerComponent} from './metadata-viewer/metadata-viewer.component';
import {MetadataEditorComponent} from './metadata-editor/metadata-editor.component';
import {MetadataSearcherComponent} from './metadata-searcher/metadata-searcher.component';

@Component({
  selector: 'app-book-metadata-center',
  standalone: true,
  templateUrl: './book-metadata-center.component.html',
  styleUrl: './book-metadata-center.component.scss',
  imports: [
    TabList,
    Tabs,
    Tab,
    NgIf,
    TabPanels,
    MetadataViewerComponent,
    TabPanel,
    MetadataEditorComponent,
    MetadataSearcherComponent
  ]
})
export class BookMetadataCenterComponent implements OnInit, OnDestroy {

  private route = inject(ActivatedRoute);
  private bookService = inject(BookService);
  private metadataCenterService = inject(BookMetadataCenterService);
  private userService = inject(UserService);
  private appSettingsService = inject(AppSettingsService);

  book: Book | undefined;
  recommendedBooks: BookRecommendation[] = [];
  tab: string = 'view';
  canEditMetadata: boolean = false;

  private userSubscription: Subscription = Subscription.EMPTY;
  private routeSubscription: Subscription = Subscription.EMPTY;

  private appSettings$ = this.appSettingsService.appSettings$;

  ngOnInit(): void {
    this.bookService.loadBooks();

    this.routeSubscription = this.route.paramMap
      .pipe(
        switchMap(params => {
          const bookIdStr = params.get('bookId');
          const bookId = Number(bookIdStr);
          if (isNaN(bookId)) throw new Error('Invalid book ID');

          return this.route.queryParamMap.pipe(
            switchMap(queryParams => {
              const rawTab = queryParams.get('tab');
              const validTabs = ['view', 'edit', 'match'];
              this.tab = validTabs.includes(rawTab || '') ? rawTab! : 'view';
              return this.bookService.getBookByIdFromAPI(bookId, true);
            })
          );
        })
      )
      .subscribe(book => {
        this.book = book;
        if (book?.metadata) {
          this.metadataCenterService.emit(book.metadata);
          this.fetchBookRecommendationsIfNeeded(book.metadata.bookId);
        }
      });

    this.userSubscription = this.userService.userData$.subscribe(userData => {
      const userPermissions = userData?.permissions;
      this.canEditMetadata = userPermissions?.canEditMetadata ?? false;
    });
  }

  ngOnDestroy(): void {
    this.userSubscription.unsubscribe();
    this.routeSubscription.unsubscribe();
  }

  private fetchBookRecommendationsIfNeeded(bookId: number): void {
    this.appSettings$
      .pipe(
        filter(settings => settings != null),
        take(1)
      )
      .subscribe(settings => {
        const similarBookRecommendation = settings!.similarBookRecommendation ?? false;
        if (similarBookRecommendation) {
          this.bookService.getBookRecommendations(bookId).subscribe(recommendations => {
            this.recommendedBooks = recommendations;
          });
        }
      });
  }
}
