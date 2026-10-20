import {Book} from '../../../../../book/model/book.model';

export function getBookRatingOnFivePointScale(book: Book): number {
  if (book.personalRating) {
    return book.personalRating / 2;
  }

  return book.metadata?.goodreadsRating ||
    book.metadata?.amazonRating ||
    book.metadata?.hardcoverRating ||
    0;
}
