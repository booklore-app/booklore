import {describe, expect, it} from 'vitest';
import {Book} from '../../../../../book/model/book.model';
import {getBookRatingOnFivePointScale} from './author-universe-rating';

describe('getBookRatingOnFivePointScale', () => {
  it('normalizes a ten-point personal rating', () => {
    expect(getBookRatingOnFivePointScale(createBook({personalRating: 10}))).toBe(5);
    expect(getBookRatingOnFivePointScale(createBook({personalRating: 6}))).toBe(3);
  });

  it('falls back to an external five-point rating', () => {
    expect(getBookRatingOnFivePointScale(createBook({goodreadsRating: 4.2}))).toBe(4.2);
  });

  function createBook(ratings: {personalRating?: number; goodreadsRating?: number}): Book {
    return {
      id: 1,
      libraryId: 1,
      libraryName: 'Test Library',
      personalRating: ratings.personalRating,
      metadata: {title: 'Test Book', goodreadsRating: ratings.goodreadsRating}
    } as Book;
  }
});
