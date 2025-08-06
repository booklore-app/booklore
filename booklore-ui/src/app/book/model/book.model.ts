import {Shelf} from './shelf.model';
import {NewPdfReaderSetting} from '../../settings/user-management/user.service';

export type BookType = "PDF" | "EPUB" | "CBX";

export interface Book {
  id: number;
  bookType: BookType;
  libraryId: number;
  metadata?: BookMetadata;
  shelves?: Shelf[];
  lastReadTime?: string;
  addedOn?: string;
  epubProgress?: EpubProgress;
  pdfProgress?: PdfProgress;
  cbxProgress?: CbxProgress;
  filePath?: string;
  fileSubPath?: string;
  fileName?: string;
  fileSizeKb?: number;
  seriesCount?: number | null;
  metadataMatchScore?: number | null;
  readStatus?: ReadStatus;
  dateFinished?: string;
  libraryPath?: { id: number };
}

export interface EpubProgress {
  cfi: string;
  percentage: number;
}

export interface PdfProgress {
  page: number;
  percentage: number;
}

export interface CbxProgress {
  page: number;
  percentage: number;
}

export interface BookMetadata {
  bookId: number;
  title?: string;
  subtitle?: string;
  publisher?: string;
  publishedDate?: string;
  description?: string;
  seriesName?: string;
  seriesNumber?: number | null;
  seriesTotal?: number | null;
  isbn13?: string;
  isbn10?: string;
  asin?: string;
  goodreadsId?: string;
  hardcoverId?: string;
  googleId?: string;
  pageCount?: number | null;
  language?: string;
  rating?: number | null;
  reviewCount?: number | null;
  amazonRating?: number | null;
  amazonReviewCount?: number | null;
  goodreadsRating?: number | null;
  goodreadsReviewCount?: number | null;
  hardcoverRating?: number | null;
  hardcoverReviewCount?: number | null;
  personalRating?: number | null;
  coverUpdatedOn?: string;
  authors?: string[];
  categories?: string[];
  provider?: string;
  providerBookId?: string;
  thumbnailUrl?: string | null;

  titleLocked?: boolean;
  subtitleLocked?: boolean;
  publisherLocked?: boolean;
  publishedDateLocked?: boolean;
  descriptionLocked?: boolean;
  seriesNameLocked?: boolean;
  seriesNumberLocked?: boolean;
  seriesTotalLocked?: boolean;
  isbn13Locked?: boolean;
  isbn10Locked?: boolean;
  asinLocked?: boolean;
  goodreadsIdLocked?: boolean;
  hardcoverIdLocked?: boolean;
  googleIdLocked?: boolean;
  pageCountLocked?: boolean;
  languageLocked?: boolean;
  personalRatingLocked?: boolean;
  amazonRatingLocked?: boolean;
  amazonReviewCountLocked?: boolean;
  goodreadsRatingLocked?: boolean;
  goodreadsReviewCountLocked?: boolean;
  hardcoverRatingLocked?: boolean;
  hardcoverReviewCountLocked?: boolean;
  coverUpdatedOnLocked?: boolean;
  authorsLocked?: boolean;
  categoriesLocked?: boolean;
  coverLocked?: boolean;

  [key: string]: any;
}

export interface MetadataClearFlags {
  title?: boolean;
  subtitle?: boolean;
  publisher?: boolean;
  publishedDate?: boolean;
  description?: boolean;
  seriesName?: boolean;
  seriesNumber?: boolean;
  seriesTotal?: boolean;
  isbn13?: boolean;
  isbn10?: boolean;
  asin?: boolean;
  goodreadsId?: boolean;
  hardcoverId?: boolean;
  googleId?: boolean;
  pageCount?: boolean;
  language?: boolean;
  amazonRating?: boolean;
  amazonReviewCount?: boolean;
  goodreadsRating?: boolean;
  goodreadsReviewCount?: boolean;
  hardcoverRating?: boolean;
  hardcoverReviewCount?: boolean;
  personalRating?: boolean;
  authors?: boolean;
  categories?: boolean;
  cover?: boolean;
}

export interface MetadataUpdateWrapper {
  metadata: BookMetadata;
  clearFlags: MetadataClearFlags;
}

export interface PdfViewerSetting {
  zoom: string | number;
  spread: 'off' | 'even' | 'odd';
}

export interface EpubViewerSetting {
  theme: string;
  font: string;
  fontSize: number;
  flow: string;
  lineHeight: number;
  letterSpacing: number;
}

export interface CbxViewerSetting {
  pageSpread: CbxPageSpread;
  pageViewMode: CbxPageViewMode;
}

export interface BookSetting {
  pdfSettings?: PdfViewerSetting;
  epubSettings?: EpubViewerSetting;
  cbxSettings?: CbxViewerSetting;
  newPdfSettings?: NewPdfReaderSetting;

  [key: string]: any;
}

export interface BookRecommendation {
  book: Book;
  similarityScore: number;
}

export const enum CbxPageViewMode {
  SINGLE_PAGE = 'SINGLE_PAGE',
  TWO_PAGE = 'TWO_PAGE',
}

export const enum CbxPageSpread {
  EVEN = 'EVEN',
  ODD = 'ODD',
}

export const enum PdfPageViewMode {
  SINGLE_PAGE = 'SINGLE_PAGE',
  TWO_PAGE = 'TWO_PAGE',
}

export const enum PdfPageSpread {
  EVEN = 'EVEN',
  ODD = 'ODD',
}

export interface BulkMetadataUpdateRequest {
  bookIds: number[];
  authors?: string[];
  clearAuthors?: boolean;
  publisher?: string;
  clearPublisher?: boolean;
  language?: string;
  clearLanguage?: boolean;
  seriesName?: string;
  clearSeriesName?: boolean;
  seriesTotal?: number | null;
  clearSeriesTotal?: boolean;
  publishedDate?: string | null;
  clearPublishedDate?: boolean;
  genres?: string[];
  clearGenres?: boolean;
}

export interface BookDeletionResponse {
  deleted: number[];
  failedFileDeletions: number[];
}

export enum ReadStatus {
  UNREAD = 'UNREAD',
  READING = 'READING',
  RE_READING = 'RE_READING',
  READ = 'READ',
  PARTIALLY_READ = 'PARTIALLY_READ',
  PAUSED = 'PAUSED',
  WONT_READ = 'WONT_READ',
  ABANDONED = 'ABANDONED',
  UNSET = 'UNSET'
}
