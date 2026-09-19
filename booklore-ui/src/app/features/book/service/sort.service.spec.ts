import {describe, expect, it} from 'vitest';
import {Book} from '../model/book.model';
import {SortDirection, SortOption} from '../model/sort.model';
import {SortService} from './sort.service';

describe('SortService', () => {
  const service = new SortService();
  const ratedBook = createBook(1, 4.5);
  const unratedBook = createBook(2);

  it.each([SortDirection.ASCENDING, SortDirection.DESCENDING])(
    'keeps missing values last when sorting %s',
    direction => {
      const criterion: SortOption = {field: 'rating', direction, label: 'Rating'};

      const sorted = service.applySort([unratedBook, ratedBook], criterion);

      expect(sorted.map(book => book.id)).toEqual([ratedBook.id, unratedBook.id]);
    }
  );

  function createBook(id: number, rating?: number): Book {
    return {
      id,
      libraryId: 1,
      libraryName: 'Test Library',
      metadata: {title: `Book ${id}`, rating}
    } as Book;
  }
});
