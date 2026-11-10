import {describe, expect, it} from 'vitest';
import {Book} from '../../../model/book.model';
import {isValueMissing, NO_VALUE_ID} from '../book-filter/book-filter.config';
import {filterBooksByFilters} from './sidebar-filter';

const book = (id: number, fields: Record<string, unknown> = {}, bookType: string | null = 'EPUB') =>
  ({
    id,
    primaryFile: bookType ? {bookType} : undefined,
    metadata: fields,
    ...(bookType === null ? {isPhysical: true} : {})
  } as unknown as Book);

const books = [
  book(1, {authors: ['Ann'], seriesName: 'Saga', publisher: 'Acme', publishedDate: '1990-05-01', pageCount: 120, language: 'en'}),
  book(2, {authors: [], seriesName: '   ', publishedDate: undefined}),                       // everything missing, an ebook
  book(3, {authors: ['Bob'], seriesName: 'Saga', publisher: 'Acme', publishedDate: '2000-01-01', narrator: 'Nina'}, 'AUDIOBOOK'),
  book(4, {authors: ['Bob']}, 'AUDIOBOOK'),                                                  // audiobook without a narrator
  book(5, {authors: ['Cy'], seriesName: 'Other', pageCount: 700}),                           // ebook: no narrator is not "missing"
  book(6, {}, 'CBX'),                                                                        // comic without characters
  book(7, {comicMetadata: {characters: ['Hero']}}, 'CBX'),
  book(8, {})                                                                                // plain book with nothing
];

const ids = (list: Book[]) => list.map(b => b.id);
const pick = (type: string, values: unknown[], mode: 'and' | 'or' | 'not' | 'single' = 'and') =>
  ids(filterBooksByFilters(books, {[type]: values}, mode as never));

describe('"No ..." entry in the matcher', () => {
  it('finds books without a series, treating a blank series as none', () => {
    expect(pick('series', [NO_VALUE_ID])).toEqual([2, 4, 6, 7, 8]);
  });

  it('finds books without an author, an publisher and a year', () => {
    expect(pick('author', [NO_VALUE_ID])).toEqual([2, 6, 7, 8]);
    expect(pick('publisher', [NO_VALUE_ID])).toEqual([2, 4, 5, 6, 7, 8]);
    expect(pick('publishedDate', [NO_VALUE_ID])).toEqual([2, 4, 5, 6, 7, 8]);
  });

  it('combines with real values as a union in OR mode', () => {
    expect(pick('series', [NO_VALUE_ID, 'Other'], 'or')).toEqual([2, 4, 5, 6, 7, 8]);
    expect(pick('author', [NO_VALUE_ID, 'Ann'], 'or')).toEqual([1, 2, 6, 7, 8]);
  });

  it('cannot be combined with a real value in AND mode for multi-value filters', () => {
    expect(pick('author', [NO_VALUE_ID, 'Ann'], 'and')).toEqual([]);
    expect(pick('series', [NO_VALUE_ID, 'Saga'], 'single')).toEqual([]);
  });

  it('excludes the books without a value in NOT mode', () => {
    expect(pick('series', [NO_VALUE_ID], 'not')).toEqual([1, 3, 5]);
  });

  it('works together with a year range, which is always a union', () => {
    expect(pick('publishedDate', [NO_VALUE_ID, '1990-1995'])).toEqual([1, 2, 4, 5, 6, 7, 8]);
    expect(pick('publishedDate', ['1990-1995'])).toEqual([1]);
  });

  it('works for numeric range filters next to a real range id', () => {
    expect(pick('pageCount', [NO_VALUE_ID])).toEqual([2, 3, 4, 6, 7, 8]);
    expect(pick('pageCount', [NO_VALUE_ID, 5])).toEqual([2, 3, 4, 5, 6, 7, 8]);
  });

  it('only counts audiobooks for narrator, so ebooks are not listed as missing a narrator', () => {
    expect(pick('narrator', [NO_VALUE_ID])).toEqual([4]);
    expect(isValueMissing(book(9, {}), 'narrator')).toBe(false);
  });

  it('only counts comics for the comic filters', () => {
    expect(pick('comicCharacter', [NO_VALUE_ID])).toEqual([6]);
    expect(pick('comicCharacter', [NO_VALUE_ID, 'Hero'], 'or')).toEqual([6, 7]);
  });

  it('does not exist for filters where every book already has a value', () => {
    expect(isValueMissing(books[0], 'readStatus')).toBe(false);
    expect(isValueMissing(books[0], 'shelfStatus')).toBe(false);
    expect(pick('readStatus', [NO_VALUE_ID])).toEqual([]);
  });

  it('never marks a book with a value as missing', () => {
    expect(isValueMissing(books[0], 'series')).toBe(false);
    expect(isValueMissing(books[0], 'publishedDate')).toBe(false);
    expect(isValueMissing(books[1], 'series')).toBe(true);
  });

  it('treats a physical book without a year as missing a year', () => {
    expect(isValueMissing(book(10, {}, null), 'publishedDate')).toBe(true);
    expect(isValueMissing(book(10, {}, null), 'narrator')).toBe(false);
  });
});
