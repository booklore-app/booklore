import {Component, inject, OnInit} from '@angular/core';
import {InputText} from 'primeng/inputtext';
import {Textarea} from 'primeng/textarea';
import {Button} from 'primeng/button';
import {Divider} from 'primeng/divider';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Observable} from 'rxjs';
import {BookMetadataCenterService} from '../book-metadata-center.service';
import {AsyncPipe, NgClass, NgIf} from '@angular/common';
import {MessageService} from 'primeng/api';
import {BookMetadata} from '../../../book/model/book.model';
import {UrlHelperService} from '../../../utilities/service/url-helper.service';
import {FileUpload, FileUploadErrorEvent, FileUploadEvent} from 'primeng/fileupload';
import {HttpResponse} from '@angular/common/http';
import {BookService} from '../../../book/service/book.service';
import {ProgressSpinner} from 'primeng/progressspinner';
import {Tooltip} from 'primeng/tooltip';

@Component({
  selector: 'app-metadata-editor',
  standalone: true,
  templateUrl: './metadata-editor.component.html',
  styleUrls: ['./metadata-editor.component.scss'],
  imports: [
    InputText,
    Textarea,
    Button,
    Divider,
    FormsModule,
    AsyncPipe,
    NgIf,
    ReactiveFormsModule,
    FileUpload,
    ProgressSpinner,
    NgClass,
    Tooltip
  ]
})
export class MetadataEditorComponent implements OnInit {

  private metadataCenterService = inject(BookMetadataCenterService);
  private messageService = inject(MessageService);
  private bookService = inject(BookService);
  protected urlHelper = inject(UrlHelperService);

  bookMetadata$: Observable<BookMetadata | null> = this.metadataCenterService.currentMetadata$;
  metadataForm: FormGroup;
  currentBookId!: number;
  isUploading = false;
  isLoading = false;

  constructor() {
    this.metadataForm = new FormGroup({
      title: new FormControl(''),
      subtitle: new FormControl(''),
      authors: new FormControl(''),
      categories: new FormControl(''),
      publisher: new FormControl(''),
      publishedDate: new FormControl(''),
      isbn10: new FormControl(''),
      isbn13: new FormControl(''),
      description: new FormControl(''),
      pageCount: new FormControl(''),
      language: new FormControl(''),
      rating: new FormControl(''),
      reviewCount: new FormControl(''),
      seriesName: new FormControl(''),
      seriesNumber: new FormControl(''),
      seriesTotal: new FormControl(''),

      titleLocked: new FormControl(false),
      subtitleLocked: new FormControl(false),
      authorsLocked: new FormControl(false),
      categoriesLocked: new FormControl(false),
      publisherLocked: new FormControl(false),
      publishedDateLocked: new FormControl(false),
      isbn10Locked: new FormControl(false),
      isbn13Locked: new FormControl(false),
      descriptionLocked: new FormControl(false),
      pageCountLocked: new FormControl(false),
      languageLocked: new FormControl(false),
      ratingLocked: new FormControl(false),
      reviewCountLocked: new FormControl(false),
      seriesNameLocked: new FormControl(false),
      seriesNumberLocked: new FormControl(false),
      seriesTotalLocked: new FormControl(false),
      thumbnailUrlLocked: new FormControl(false),
    });
  }

  ngOnInit(): void {
    this.bookMetadata$.subscribe((metadata) => {
      if (metadata) {
        this.currentBookId = metadata.bookId;

        this.metadataForm.patchValue({
          title: metadata.title || null,
          subtitle: metadata.subtitle || null,
          authors: (metadata.authors || []).sort().join(', '),
          categories: (metadata.categories || []).sort().join(', '),
          publisher: metadata.publisher || null,
          publishedDate: metadata.publishedDate || null,
          isbn10: metadata.isbn10 || null,
          isbn13: metadata.isbn13 || null,
          description: metadata.description || null,
          pageCount: metadata.pageCount || null,
          language: metadata.language || null,
          rating: metadata.rating || null,
          reviewCount: metadata.reviewCount || null,
          seriesName: metadata.seriesName || null,
          seriesNumber: metadata.seriesNumber || null,
          seriesTotal: metadata.seriesTotal || null,

          titleLocked: metadata.titleLocked || false,
          subtitleLocked: metadata.subtitleLocked || false,
          authorsLocked: metadata.authorsLocked || false,
          categoriesLocked: metadata.categoriesLocked || false,
          publisherLocked: metadata.publisherLocked || false,
          publishedDateLocked: metadata.publishedDateLocked || false,
          isbn10Locked: metadata.isbn10Locked || false,
          isbn13Locked: metadata.isbn13Locked || false,
          descriptionLocked: metadata.descriptionLocked || false,
          pageCountLocked: metadata.pageCountLocked || false,
          languageLocked: metadata.languageLocked || false,
          ratingLocked: metadata.ratingLocked || false,
          reviewCountLocked: metadata.reviewCountLocked || false,
          seriesNameLocked: metadata.seriesNameLocked || false,
          seriesNumberLocked: metadata.seriesNumberLocked || false,
          seriesTotalLocked: metadata.seriesTotalLocked || false,
          thumbnailUrlLocked: metadata.coverLocked || false,
        });

        if (metadata.titleLocked) this.metadataForm.get('title')?.disable();
        if (metadata.subtitleLocked) this.metadataForm.get('subtitle')?.disable();
        if (metadata.authorsLocked) this.metadataForm.get('authors')?.disable();
        if (metadata.categoriesLocked) this.metadataForm.get('categories')?.disable();
        if (metadata.publisherLocked) this.metadataForm.get('publisher')?.disable();
        if (metadata.publishedDateLocked) this.metadataForm.get('publishedDate')?.disable();
        if (metadata.languageLocked) this.metadataForm.get('language')?.disable();
        if (metadata.isbn10Locked) this.metadataForm.get('isbn10')?.disable();
        if (metadata.isbn13Locked) this.metadataForm.get('isbn13')?.disable();
        if (metadata.reviewCountLocked) this.metadataForm.get('reviewCount')?.disable();
        if (metadata.ratingLocked) this.metadataForm.get('rating')?.disable();
        if (metadata.pageCountLocked) this.metadataForm.get('pageCount')?.disable();
        if (metadata.descriptionLocked) this.metadataForm.get('description')?.disable();
        if (metadata.seriesNameLocked) this.metadataForm.get('seriesName')?.disable();
        if (metadata.seriesNumberLocked) this.metadataForm.get('seriesNumber')?.disable();
        if (metadata.seriesTotalLocked) this.metadataForm.get('seriesTotal')?.disable();
      }
    });
  }

  onSave(): void {
    this.bookService.updateBookMetadata(this.currentBookId, this.buildMetadata(undefined), false).subscribe({
      next: (response) => {
        this.messageService.add({severity: 'info', summary: 'Success', detail: 'Book metadata updated'});
        this.metadataCenterService.emit(response);
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to update book metadata'});
      }
    });
  }

  toggleLock(field: string): void {
    const isLocked = this.metadataForm.get(field + 'Locked')?.value;
    const updatedLockedState = !isLocked;
    this.metadataForm.get(field + 'Locked')?.setValue(updatedLockedState);
    if (updatedLockedState) {
      this.metadataForm.get(field)?.disable();
    } else {
      this.metadataForm.get(field)?.enable();
    }
    this.updateMetadata(undefined);
  }

  lockAll(): void {
    Object.keys(this.metadataForm.controls).forEach((key) => {
      if (key.endsWith('Locked')) {
        this.metadataForm.get(key)?.setValue(true);
        const fieldName = key.replace('Locked', '');
        this.metadataForm.get(fieldName)?.disable();
      }
    });
    this.updateMetadata(true);
  }

  unlockAll(): void {
    Object.keys(this.metadataForm.controls).forEach((key) => {
      if (key.endsWith('Locked')) {
        this.metadataForm.get(key)?.setValue(false);
        const fieldName = key.replace('Locked', '');
        this.metadataForm.get(fieldName)?.enable();
      }
    });
    this.updateMetadata(false);
  }

  private buildMetadata(shouldLockAllFields: boolean | undefined) {
    const updatedBookMetadata: BookMetadata = {
      bookId: this.currentBookId,
      title: this.metadataForm.get('title')?.value,
      subtitle: this.metadataForm.get('subtitle')?.value,
      authors: this.metadataForm.get('authors')?.value.split(',').map((author: string) => author.trim()),
      categories: this.metadataForm.get('categories')?.value.split(',').map((category: string) => category.trim()),
      publisher: this.metadataForm.get('publisher')?.value,
      publishedDate: this.metadataForm.get('publishedDate')?.value,
      isbn10: this.metadataForm.get('isbn10')?.value,
      isbn13: this.metadataForm.get('isbn13')?.value,
      description: this.metadataForm.get('description')?.value,
      pageCount: this.metadataForm.get('pageCount')?.value,
      rating: this.metadataForm.get('rating')?.value,
      reviewCount: this.metadataForm.get('reviewCount')?.value,
      language: this.metadataForm.get('language')?.value,
      seriesName: this.metadataForm.get('seriesName')?.value,
      seriesNumber: this.metadataForm.get('seriesNumber')?.value,
      seriesTotal: this.metadataForm.get('seriesTotal')?.value,

      titleLocked: this.metadataForm.get('titleLocked')?.value,
      subtitleLocked: this.metadataForm.get('subtitleLocked')?.value,
      authorsLocked: this.metadataForm.get('authorsLocked')?.value,
      categoriesLocked: this.metadataForm.get('categoriesLocked')?.value,
      publisherLocked: this.metadataForm.get('publisherLocked')?.value,
      publishedDateLocked: this.metadataForm.get('publishedDateLocked')?.value,
      isbn10Locked: this.metadataForm.get('isbn10Locked')?.value,
      isbn13Locked: this.metadataForm.get('isbn13Locked')?.value,
      descriptionLocked: this.metadataForm.get('descriptionLocked')?.value,
      pageCountLocked: this.metadataForm.get('pageCountLocked')?.value,
      languageLocked: this.metadataForm.get('languageLocked')?.value,
      ratingLocked: this.metadataForm.get('ratingLocked')?.value,
      reviewCountLocked: this.metadataForm.get('reviewCountLocked')?.value,
      seriesNameLocked: this.metadataForm.get('seriesNameLocked')?.value,
      seriesNumberLocked: this.metadataForm.get('seriesNumberLocked')?.value,
      seriesTotalLocked: this.metadataForm.get('seriesTotalLocked')?.value,
      coverLocked: this.metadataForm.get('thumbnailUrlLocked')?.value,

      ...(shouldLockAllFields !== undefined && {allFieldsLocked: shouldLockAllFields}),
    };
    return updatedBookMetadata;
  }

  private updateMetadata(shouldLockAllFields: boolean | undefined): void {
    this.bookService.updateBookMetadata(this.currentBookId, this.buildMetadata(shouldLockAllFields), false).subscribe({
      next: (response) => {
        this.metadataCenterService.emit(response);

        if (shouldLockAllFields !== undefined) {
          this.messageService.add({
            severity: 'success',
            summary: shouldLockAllFields ? 'Metadata Locked' : 'Metadata Unlocked',
            detail: shouldLockAllFields
              ? 'All fields have been successfully locked.'
              : 'All fields have been successfully unlocked.',
          });
        }
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to update lock state',
        });
      }
    });
  }

  closeDialog() {
    this.metadataCenterService.closeDialog(true);
  }

  getUploadCoverUrl(): string {
    return this.bookService.getUploadCoverUrl(this.currentBookId);
  }

  onBeforeSend(): void {
    this.isUploading = true;
  }

  onUpload(event: FileUploadEvent): void {
    const response: HttpResponse<any> = event.originalEvent as HttpResponse<any>;
    if (response && response.status === 200) {
      const bookMetadata: BookMetadata = response.body as BookMetadata;
      this.bookService.handleBookMetadataUpdate(this.currentBookId, bookMetadata);
      this.metadataCenterService.emit(bookMetadata);
      this.isUploading = false;
    } else {
      this.isUploading = false;
      this.messageService.add({
        severity: 'error', summary: 'Upload Failed', detail: 'An error occurred while uploading the cover', life: 3000
      });
    }
  }

  onUploadError($event: FileUploadErrorEvent) {
    this.isUploading = false;
    this.messageService.add({
      severity: 'error', summary: 'Upload Error', detail: 'An error occurred while uploading the cover', life: 3000
    });
  }

  regenerateCover(bookId: number) {
    this.bookService.regenerateCover(bookId).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Book cover regenerated successfully. Refresh page to see the new cover.'
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to start cover regeneration'
        });
      }
    });
  }
}
