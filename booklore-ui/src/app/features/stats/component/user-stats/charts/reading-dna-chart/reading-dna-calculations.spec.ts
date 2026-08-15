import {describe, expect, it} from 'vitest';
import {Book} from '../../../../../book/model/book.model';
import {getReadingProgress, isHighPersonalRating} from './reading-dna-calculations';

describe('Reading DNA calculations', () => {
  describe('isHighPersonalRating', () => {
    it('uses the high-rating threshold for the ten-point scale', () => {
      expect(isHighPersonalRating(7)).toBe(false);
      expect(isHighPersonalRating(8)).toBe(true);
      expect(isHighPersonalRating(10)).toBe(true);
    });
  });

  describe('getReadingProgress', () => {
    it('includes audiobook progress', () => {
      const book = {
        epubProgress: {percentage: 25},
        audiobookProgress: {percentage: 75}
      } as Book;

      expect(getReadingProgress(book)).toBe(75);
    });

    it('returns zero when a book has no progress', () => {
      expect(getReadingProgress({} as Book)).toBe(0);
    });
  });
});
