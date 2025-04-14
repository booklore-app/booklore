import {Component, inject, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {Divider} from 'primeng/divider';
import {NgForOf, NgIf} from '@angular/common';
import {MetadataProvider} from '../../model/provider.model';
import {FetchMetadataRequest} from '../../model/request/fetch-metadata-request.model';
import {ProgressSpinner} from 'primeng/progressspinner';
import {MetadataPickerComponent} from '../metadata-picker/metadata-picker.component';
import {BookMetadataCenterService} from '../book-metadata-center.service';
import {MultiSelect} from 'primeng/multiselect';
import {BookMetadata} from '../../../book/model/book.model';
import {BookService} from '../../../book/service/book.service';
import {Observable} from 'rxjs';
import {AppSettings} from '../../../core/model/app-settings.model';
import {AppSettingsService} from '../../../core/service/app-settings.service';
import {filter, take} from 'rxjs/operators';

@Component({
  selector: 'app-metadata-searcher',
  templateUrl: './metadata-searcher.component.html',
  styleUrls: ['./metadata-searcher.component.scss'],
  imports: [ReactiveFormsModule, Button, InputText, Divider, NgForOf, NgIf, ProgressSpinner, MetadataPickerComponent, MultiSelect],
  standalone: true
})
export class MetadataSearcherComponent implements OnInit {

  form: FormGroup;
  providers = Object.values(MetadataProvider);
  allFetchedMetadata: BookMetadata[] = [];
  selectedFetchedMetadata!: BookMetadata | null;
  bookId!: number;
  loading: boolean = false;

  private formBuilder = inject(FormBuilder);
  private metadataCenterService = inject(BookMetadataCenterService);
  private bookService = inject(BookService);
  private appSettingsService = inject(AppSettingsService);

  appSettings$: Observable<AppSettings | null> = this.appSettingsService.appSettings$;

  constructor() {
    this.form = this.formBuilder.group({
      provider: null,
      title: [''],
      author: [''],
    });
  }

  ngOnInit() {
    this.metadataCenterService.currentMetadata$.subscribe((metadata => {
      if (metadata) {
        this.bookId = metadata.bookId;
        this.form.patchValue(({
          provider: Object.values(MetadataProvider),
          title: metadata.title || null,
          author: metadata.authors?.length! > 0 ? metadata.authors[0] : ''
        }));
      }
    }));

    this.appSettings$
      .pipe(
        filter(settings => settings != null),
        take(1)
      )
      .subscribe(settings => {
        if (settings.autoBookSearch) {
          this.onSubmit();
        }
      });
  }

  get isSearchEnabled(): boolean {
    const providerSelected = !!this.form.get('provider')?.value;
    const title = this.form.get('title')?.value;
    return providerSelected && title;
  }

  onSubmit(): void {
    if (this.form.valid) {
      const providerKeys = Object.keys(MetadataProvider).filter(key =>
        (this.form.get('provider')?.value as string[]).includes(MetadataProvider[key as keyof typeof MetadataProvider])
      );
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
      this.bookService.fetchBookMetadata(fetchRequest.bookId, fetchRequest)
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
    if (!metadata.provider || !metadata.providerBookId) {
      throw new Error("Invalid metadata: 'provider' or 'providerBookId' is missing.");
    }
    switch (metadata.provider) {
      case "Amazon":
        return `<a href="https://www.amazon.com/dp/${metadata.providerBookId}" target="_blank">Amazon</a>`;
      case "GoodReads":
        return `<a href="https://www.goodreads.com/book/show/${metadata.providerBookId}" target="_blank">Goodreads</a>`;
      case "Google":
        return `<a href="https://books.google.com/books?id=${metadata.providerBookId}" target="_blank">Google</a>`;
      default:
        throw new Error(`Unsupported provider: ${metadata.provider}`);
    }
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
