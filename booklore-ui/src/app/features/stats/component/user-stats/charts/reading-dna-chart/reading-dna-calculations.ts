import {Book} from '../../../../../book/model/book.model';

const HIGH_RATING_THRESHOLD = 8;

export function isHighPersonalRating(rating: number): boolean {
  return rating >= HIGH_RATING_THRESHOLD;
}

export function getReadingProgress(book: Book): number {
  return Math.max(
    book.epubProgress?.percentage || 0,
    book.pdfProgress?.percentage || 0,
    book.cbxProgress?.percentage || 0,
    book.koreaderProgress?.percentage || 0,
    book.koboProgress?.percentage || 0,
    book.audiobookProgress?.percentage || 0
  );
}
