import {Component, inject, OnDestroy} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Divider} from 'primeng/divider';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {MessageService} from 'primeng/api';
import {filter, take, takeUntil} from 'rxjs/operators';
import {Subject} from 'rxjs';

import {BookService} from '../../../book/service/book.service';
import {Book} from '../../../book/model/book.model';
import {FileMoveRequest, FileOperationsService} from '../../service/file-operations-service';
import {LibraryService} from "../../../book/service/library.service";
import {AppSettingsService} from '../../../core/service/app-settings.service';

@Component({
  selector: 'app-file-mover-component',
  standalone: true,
  imports: [Button, FormsModule, TableModule, Divider],
  templateUrl: './file-mover-component.html',
  styleUrl: './file-mover-component.scss'
})
export class FileMoverComponent implements OnDestroy {
  private config = inject(DynamicDialogConfig);
  private ref = inject(DynamicDialogRef);
  private bookService = inject(BookService);
  private libraryService = inject(LibraryService);
  private fileOperationsService = inject(FileOperationsService);
  private messageService = inject(MessageService);
  private appSettingsService = inject(AppSettingsService);
  private destroy$ = new Subject<void>();

  libraryPatterns: {
    libraryId: number | null;
    libraryName: string;
    pattern: string;
    source: string;
    bookCount: number;
  }[] = [];
  defaultMovePattern = '';
  loading = false;
  patternsCollapsed = false;

  bookIds: Set<number> = new Set();
  books: Book[] = [];
  filePreviews: { originalPath: string; newPath: string; isMoved?: boolean }[] = [];

  constructor() {
    this.bookIds = new Set(this.config.data?.bookIds ?? []);
    this.books = this.bookService.getBooksByIdsFromState([...this.bookIds]);
    this.loadDefaultPattern();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDefaultPattern(): void {
    this.appSettingsService.appSettings$.pipe(
      filter(settings => settings != null),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe(settings => {
      this.defaultMovePattern = settings?.uploadPattern || '';
      this.loadLibraryPatterns();
    });
  }

  private loadLibraryPatterns(): void {
    this.libraryService.libraryState$.pipe(
      filter(state => state.loaded && state.libraries != null),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe(state => {
      const booksByLibrary = new Map<number | null, Book[]>();
      this.books.forEach(book => {
        const libraryId =
          book.libraryId ??
          book.libraryPath?.id ??
          (book as any).library?.id ??
          null;
        if (!booksByLibrary.has(libraryId)) {
          booksByLibrary.set(libraryId, []);
        }
        booksByLibrary.get(libraryId)!.push(book);
      });

      this.libraryPatterns = Array.from(booksByLibrary.entries()).map(([libraryId, books]) => {
        let libraryName = 'Unknown Library';
        let pattern = this.defaultMovePattern;
        let source = 'App Default';

        if (libraryId) {
          const library = state.libraries?.find(lib => lib.id === libraryId);
          if (library) {
            libraryName = library.name;
            if (library.fileNamingPattern) {
              pattern = library.fileNamingPattern;
              source = 'Library Setting';
            }
          }
        }

        return {
          libraryId,
          libraryName,
          pattern,
          source,
          bookCount: books.length
        };
      });

      this.applyPattern();
    });
  }

  applyPattern(): void {
    this.filePreviews = this.books.map(book => {
      const meta = book.metadata!;
      const fileName = book.fileName ?? '';
      const extension = fileName.match(/\.[^.]+$/)?.[0] ?? '';
      const fileSubPath = book.fileSubPath ? `${book.fileSubPath.replace(/\/+$/g, '')}/` : '';

      const relativeOriginalPath = `${fileSubPath}${fileName}`;
      const libraryPathPrefix =
        book.libraryPath?.id != null
          ? this.libraryService.getLibraryPathById(book.libraryPath.id)?.replace(/\/+$/g, '') ?? ''
          : '';
      const originalPath = `${libraryPathPrefix}/${relativeOriginalPath}`.replace(/\/\/+/g, '/');

      const bookLibraryId =
        book.libraryId ??
        book.libraryPath?.id ??
        (book as any).library?.id ??
        null;
      const libraryPattern = this.libraryPatterns.find(p => p.libraryId === bookLibraryId);
      const pattern = libraryPattern?.pattern || this.defaultMovePattern;

      const values: Record<string, string> = {
        authors: this.sanitize(meta.authors?.join(', ') || 'Unknown Author'),
        title: this.sanitize(meta.title || 'Untitled'),
        year: this.formatYear(meta.publishedDate),
        series: this.sanitize(meta.seriesName || ''),
        seriesIndex: this.formatSeriesIndex(meta.seriesNumber ?? undefined),
        language: this.sanitize(meta.language || ''),
        publisher: this.sanitize(meta.publisher || ''),
        isbn: this.sanitize(meta.isbn13 || meta.isbn10 || ''),
        currentFilename: this.sanitize(fileName)
      };

      let newPath: string;

      if (!pattern?.trim()) {
        newPath = `${fileSubPath}${fileName}`;
      } else {
        newPath = pattern.replace(/<([^<>]+)>/g, (_, block) => {
          const placeholders = [...block.matchAll(/{(.*?)}/g)].map(m => m[1]);
          const allHaveValues = placeholders.every(key => values[key]?.trim());
          return allHaveValues
            ? block.replace(/{(.*?)}/g, (_: string, key: string) => values[key] ?? '')
            : '';
        });

        newPath = newPath.replace(/{(.*?)}/g, (_, key) => values[key] ?? '');

        if (!newPath.endsWith(extension)) {
          newPath += extension;
        }
      }

      const relativeNewPath = newPath;
      const fullNewPath = `${libraryPathPrefix}/${relativeNewPath}`.replace(/\/\/+/g, '/');

      return {
        bookId: book.id,
        originalPath,
        relativeOriginalPath,
        libraryPathPrefix,
        newPath: fullNewPath,
        relativeNewPath
      };

    });
  }

  get movedFileCount(): number {
    return this.filePreviews.filter(p => p.isMoved).length;
  }

  sanitize(input: string | undefined): string {
    return input?.replace(/[\\/:*?"<>|]/g, '')
      .replace(/[\x00-\x1F\x7F]/g, '')
      .replace(/\s+/g, ' ')
      .trim() ?? '';
  }

  formatYear(dateStr?: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return isNaN(date.getTime()) ? '' : date.getFullYear().toString();
  }

  formatSeriesIndex(seriesNumber?: number): string {
    if (seriesNumber == null) return '';
    return this.sanitize(seriesNumber.toString());
  }

  saveChanges(): void {
    this.loading = true;

    const request: FileMoveRequest = {
      bookIds: [...this.bookIds]
    };

    this.fileOperationsService.moveFiles(request).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loading = false;
        this.filePreviews.forEach(p => (p.isMoved = true));
        this.messageService.add({
          severity: 'success',
          summary: 'Files Organized!',
          detail: `Successfully organized ${this.filePreviews.length} file${this.filePreviews.length === 1 ? '' : 's'}.`,
          life: 3000
        });
      },
      error: () => {
        this.loading = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Oops! Something went wrong',
          detail: 'We had trouble organizing your files. Please try again.',
          life: 3000
        });
      }
    });
  }

  cancel(): void {
    this.ref.close();
  }

  togglePatternsCollapsed(): void {
    this.patternsCollapsed = !this.patternsCollapsed;
  }
}
