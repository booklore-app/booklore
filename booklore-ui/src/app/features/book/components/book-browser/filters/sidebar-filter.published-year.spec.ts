import {describe, expect, it} from 'vitest';
import {Book} from '../../../model/book.model';
import {FILTER_CONFIGS, FILTER_EXTRACTORS} from '../book-filter/book-filter.config';
import {filterBooksByFilters} from './sidebar-filter';

const book = (id: number, publishedDate?: string) =>
  ({id, metadata: {publishedDate}} as unknown as Book);

const books = [
  book(1, '1985-03-01'),
  book(2, '1990-01-01'),   // 1 January: must not slip into 1989 in time zones behind UTC
  book(3, '1995-07-15'),
  book(4, '2000-12-31'),
  book(5, '2010-05-05'),
  book(6, undefined)
];

const ids = (list: Book[]) => list.map(b => b.id);

describe('Published Year filter', () => {
  it('keeps selecting a single year', () => {
    expect(ids(filterBooksByFilters(books, {publishedDate: ['1995']}, 'and'))).toEqual([3]);
  });

  it('selects a closed range including both ends', () => {
    expect(ids(filterBooksByFilters(books, {publishedDate: ['1990-2000']}, 'and'))).toEqual([2, 3, 4]);
  });

  it('selects open-ended ranges', () => {
    expect(ids(filterBooksByFilters(books, {publishedDate: ['2000-']}, 'and'))).toEqual([4, 5]);
    expect(ids(filterBooksByFilters(books, {publishedDate: ['-1990']}, 'and'))).toEqual([1, 2]);
  });

  it('treats several values as a union, so a range and a year can be combined', () => {
    expect(ids(filterBooksByFilters(books, {publishedDate: ['1985', '2000-']}, 'and'))).toEqual([1, 4, 5]);
  });

  it('excludes the range in NOT mode (books without a date are kept)', () => {
    expect(ids(filterBooksByFilters(books, {publishedDate: ['1990-2000']}, 'not'))).toEqual([1, 5, 6]);
  });

  it('never matches books without a published date', () => {
    expect(ids(filterBooksByFilters(books, {publishedDate: ['-2100']}, 'and'))).not.toContain(6);
  });

  it('lists the years newest first in the filter panel', () => {
    expect(FILTER_CONFIGS.publishedDate.sortMode).toBe('sortIndex');
    const entries = books.flatMap(b => FILTER_EXTRACTORS.publishedDate(b));
    const sorted = [...entries].sort((a, b) => (a.sortIndex ?? 999) - (b.sortIndex ?? 999)).map(e => e.name);
    expect(sorted).toEqual(['2010', '2000', '1995', '1990', '1985']);
  });
});
