import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {Divider} from 'primeng/divider';

import {FetchMetadataRequest} from '../../model/request/fetch-metadata-request.model';
import {ProgressSpinner} from 'primeng/progressspinner';
import {MetadataPickerComponent} from '../metadata-picker/metadata-picker.component';
import {BookMetadataCenterService} from '../book-metadata-center.service';
import {MultiSelect} from 'primeng/multiselect';
import {Book, BookMetadata} from '../../../model/book.model';
import {BookService} from '../../../service/book.service';
import {combineLatest, Observable, Subject, Subscription, takeUntil} from 'rxjs';
import {AppSettings} from '../../../../core/model/app-settings.model';
import {AppSettingsService} from '../../../../core/service/app-settings.service';
import {distinctUntilChanged, filter, switchMap} from 'rxjs/operators';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-metadata-searcher',
  templateUrl: './metadata-searcher.component.html',
  styleUrls: ['./metadata-searcher.component.scss'],
  imports: [ReactiveFormsModule, Button, InputText, Divider, ProgressSpinner, MetadataPickerComponent, MultiSelect],
  standalone: true
})
export class MetadataSearcherComponent implements OnInit, OnDestroy {
  form: FormGroup;
  providers: string[] = [];
  allFetchedMetadata: BookMetadata[] = [];
  selectedFetchedMetadata!: BookMetadata | null;
  bookId!: number;
  loading: boolean = false;

  private formBuilder = inject(FormBuilder);
  private metadataCenterService = inject(BookMetadataCenterService);
  private bookService = inject(BookService);
  private appSettingsService = inject(AppSettingsService);
  private route = inject(ActivatedRoute);

  private subscription: Subscription = new Subscription();
  private cancelRequest$ = new Subject<void>();

  appSettings$: Observable<AppSettings | null> = this.appSettingsService.appSettings$;
  bookChanged$: Observable<Book | null> = this.metadataCenterService.bookChanged$;

  constructor() {
    this.form = this.formBuilder.group({
      provider: null,
      title: [''],
      author: [''],
    });
  }

  ngOnInit() {
    this.subscription.add(
      this.route.paramMap.pipe(
        switchMap(params => {
          const bookId = +params.get('id')!;
          if (this.bookId !== bookId) {
            this.bookId = bookId;
            this.cancelRequest$.next();
            this.loading = false;
            this.selectedFetchedMetadata = null;
            this.allFetchedMetadata = [];
            this.form.patchValue({
              provider: this.providers,
              title: '',
              author: '',
            });
          }
          return combineLatest([this.bookChanged$, this.appSettings$]);
        }),
        filter(([book, settings]) => !!book && !!settings),
        distinctUntilChanged(([prevBook], [currBook]) => prevBook?.id === currBook?.id)
      ).subscribe(([book, settings]) => {

        this.providers = Object.entries(settings!.metadataProviderSettings)
          .filter(([key, value]) => value.enabled)
          .map(([key]) => key.charAt(0).toUpperCase() + key.slice(1));

        const autoBookSearchEnabled = settings!.autoBookSearch ?? false;

        if (book) {
          this.selectedFetchedMetadata = null;
          this.allFetchedMetadata = [];
          this.bookId = book.id;

          this.form.patchValue({
            provider: this.providers,
            title: book.metadata?.title || null,
            author: book.metadata?.authors?.length ? book.metadata.authors[0] : ''
          });

          if (autoBookSearchEnabled) {
            this.onSubmit();
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.cancelRequest$.next();
    this.subscription.unsubscribe();
    this.allFetchedMetadata = [];
    this.selectedFetchedMetadata = null;
  }

  get isSearchEnabled(): boolean {
    const providerSelected = !!this.form.get('provider')?.value;
    const title = this.form.get('title')?.value;
    return providerSelected && title;
  }

  onSubmit(): void {
    if (this.form.valid) {
      const providerKeys = this.form.get('provider')?.value;
      if (!providerKeys) {
        return;
      }
      const fetchRequest: FetchMetadataRequest = {
        bookId: this.bookId,
        providers: providerKeys,
        title: this.form.get('title')?.value,
        author: this.form.get('author')?.value
      };
      this.loading = true;
      this.cancelRequest$.next();
      this.bookService.fetchBookMetadata(fetchRequest.bookId, fetchRequest)
        .pipe(takeUntil(this.cancelRequest$))
        .subscribe({
          next: (fetchedMetadata) => {
            this.loading = false;
            this.allFetchedMetadata = fetchedMetadata.map((fetchedMetadata) => ({
              ...fetchedMetadata,
              thumbnailUrl: fetchedMetadata.thumbnailUrl
            }));
          },
          error: () => {
            this.loading = false;
          }
        });
    } else {
      console.warn('Form is invalid. Please fill in all required fields.');
    }
  }

  buildProviderLink(metadata: BookMetadata): string {
    if (metadata.asin) {
      return `<a href="https://www.amazon.com/dp/${metadata.asin}" target="_blank">Amazon</a>`;
    } else if (metadata.goodreadsId) {
      return `<a href="https://www.goodreads.com/book/show/${metadata.goodreadsId}" target="_blank">Goodreads</a>`;
    } else if (metadata.googleId) {
      return `<a href="https://books.google.com/books?id=${metadata.googleId}" target="_blank">Google</a>`;
    } else if (metadata.hardcoverId) {
      return `<a href="https://hardcover.app/books/${metadata.hardcoverId}" target="_blank">Hardcover</a>`;
    }

    throw new Error("No provider ID found in metadata.");
  }

  truncateText(text: string | null, length: number): string {
    const safeText = text ?? '';
    return safeText.length > length ? safeText.substring(0, length) + '...' : safeText;
  }

  onBookClick(fetchedMetadata: BookMetadata) {
    this.selectedFetchedMetadata = fetchedMetadata;
  }

  onGoBack() {
    this.selectedFetchedMetadata = null;
  }

  sanitizeHtml(htmlString: string | null | undefined): string {
    if (!htmlString) {
      return '';
    }
    return htmlString.replace(/<\/?[^>]+(>|$)/g, '').trim();
  }
}
