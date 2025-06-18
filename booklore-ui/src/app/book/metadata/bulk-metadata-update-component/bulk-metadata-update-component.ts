import {Component, inject, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {Chips} from 'primeng/chips';
import {DatePicker} from 'primeng/datepicker';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {MessageService} from 'primeng/api';

import {BookService} from '../../service/book.service';
import {Book, BulkMetadataUpdateRequest} from '../../model/book.model';
import {Checkbox} from 'primeng/checkbox';
import {ProgressSpinner} from 'primeng/progressspinner';

@Component({
  selector: 'app-bulk-metadata-update-component',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputText,
    Button,
    Tooltip,
    Chips,
    DatePicker,
    FormsModule,
    Checkbox,
    ProgressSpinner
  ],
  providers: [MessageService],
  templateUrl: './bulk-metadata-update-component.html',
  styleUrl: './bulk-metadata-update-component.scss'
})
export class BulkMetadataUpdateComponent implements OnInit {
  metadataForm!: FormGroup;
  bookIds: number[] = [];
  books: Book[] = [];
  showBookList = true;
  mergeCategories = true;
  loading = false;

  private readonly config = inject(DynamicDialogConfig);
  private readonly ref = inject(DynamicDialogRef);
  private readonly fb = inject(FormBuilder);
  private readonly bookService = inject(BookService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.bookIds = this.config.data?.bookIds ?? [];
    this.books = this.bookService.getBooksByIdsFromState(this.bookIds);

    this.metadataForm = this.fb.group({
      authors: [],
      publisher: [''],
      language: [''],
      seriesName: [''],
      seriesTotal: [''],
      publishedDate: [null],
      genres: []
    });
  }

  onSubmit(): void {
    if (this.metadataForm.valid) {
      const formValue = this.metadataForm.value;

      const payload: BulkMetadataUpdateRequest = {
        bookIds: this.bookIds,
        authors: formValue.authors?.length ? formValue.authors : undefined,
        publisher: formValue.publisher?.trim() || undefined,
        language: formValue.language?.trim() || undefined,
        seriesName: formValue.seriesName?.trim() || undefined,
        seriesTotal: formValue.seriesTotal || undefined,
        publishedDate: formValue.publishedDate
          ? new Date(formValue.publishedDate).toISOString().split('T')[0]
          : undefined,
        genres: formValue.genres?.length ? formValue.genres : undefined
      };
      this.loading = true;
      this.bookService.updateBooksMetadata(payload, this.mergeCategories).subscribe({
        next: (updatedBooks) => {
          this.loading = false;
          this.messageService.add({
            severity: 'success',
            summary: 'Metadata Updated',
            detail: `${updatedBooks.length} book${updatedBooks.length > 1 ? 's' : ''} updated successfully`,
          });
          this.ref.close(true);
        },
        error: (error) => {
          console.error('Bulk metadata update failed:', error);
          this.loading = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Update Failed',
            detail: 'An error occurred while updating book metadata',
          });
        }
      });
    }
  }
}
