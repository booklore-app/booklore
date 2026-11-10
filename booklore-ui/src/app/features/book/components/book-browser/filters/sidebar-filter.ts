import {combineLatest, Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {BookFilter} from './BookFilter';
import {BookState} from '../../../model/state/book-state.model';
import {fileSizeRanges, isValueMissing, matchScoreRanges, NO_VALUE_ID, pageCountRanges, ratingRanges} from '../book-filter/book-filter.config';
import {Book, ReadStatus} from '../../../model/book.model';
import {BookFilterMode} from '../../../../settings/user-management/user.service';
import {doesYearMatchFilterValue, getBookPublishedYear} from '../book-filter/year-range';

export function isRatingInRange(rating: number | undefined | null, rangeId: string | number): boolean {
  if (rating == null) return false;
  const numericId = typeof rangeId === 'string' ? Number(rangeId) : rangeId;
  const range = ratingRanges.find(r => r.id === numericId);
  if (!range) return false;
  return rating >= range.min && rating < range.max;
}

export function isRatingInRange10(rating: number | undefined | null, rangeId: string | number): boolean {
  if (rating == null) return false;
  const numericId = typeof rangeId === 'string' ? Number(rangeId) : rangeId;
  return Math.round(rating) === numericId;
}

export function isFileSizeInRange(fileSizeKb: number | undefined, rangeId: string | number): boolean {
  if (fileSizeKb == null) return false;
  const numericId = typeof rangeId === 'string' ? Number(rangeId) : rangeId;
  const range = fileSizeRanges.find(r => r.id === numericId);
  if (!range) return false;
  return fileSizeKb >= range.min && fileSizeKb < range.max;
}

export function isPageCountInRange(pageCount: number | undefined, rangeId: string | number): boolean {
  if (pageCount == null) return false;
  const numericId = typeof rangeId === 'string' ? Number(rangeId) : rangeId;
  const range = pageCountRanges.find(r => r.id === numericId);
  if (!range) return false;
  return pageCount >= range.min && pageCount < range.max;
}

export function isMatchScoreInRange(score: number | undefined | null, rangeId: string | number): boolean {
  if (score == null) return false;
  const normalizedScore = score > 1 ? score / 100 : score;
  const numericId = typeof rangeId === 'string' ? Number(rangeId) : rangeId;
  const range = matchScoreRanges.find(r => r.id === numericId);
  if (!range) return false;
  return normalizedScore >= range.min && normalizedScore < range.max;
}

export function doesBookMatchReadStatus(book: Book, selected: unknown[]): boolean {
  const status = book.readStatus ?? ReadStatus.UNSET;
  return selected.includes(status);
}

// Filters where selecting several values in AND mode means the book must have all of them
const MATCH_ALL_FILTERS = new Set<string>([
  'author', 'category', 'series', 'publisher', 'tag', 'mood',
  'comicCharacter', 'comicTeam', 'comicLocation', 'comicCreator'
]);

export function doesBookMatchFilter(
  book: Book,
  filterType: string,
  filterValues: unknown[],
  mode: BookFilterMode
): boolean {
  if (!Array.isArray(filterValues) || filterValues.length === 0) {
    return mode === 'or';
  }

  const effectiveMode = mode === 'not' ? 'or' : mode;

  // "No ..." entry: matches books that have no value for this filter, alone or together with other values
  if (filterValues.includes(NO_VALUE_ID)) {
    const otherValues = filterValues.filter(v => v !== NO_VALUE_ID);
    // A book cannot both have no value and have all of several values
    if (effectiveMode !== 'or' && MATCH_ALL_FILTERS.has(filterType) && otherValues.length > 0) return false;
    if (isValueMissing(book, filterType)) return true;
    if (otherValues.length === 0) return false;
    filterValues = otherValues;
  }

  switch (filterType) {
    case 'author':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.authors?.includes(val as string))
        : filterValues.every(val => book.metadata?.authors?.includes(val as string));
    case 'category':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.categories?.includes(val as string))
        : filterValues.every(val => book.metadata?.categories?.includes(val as string));
    case 'series':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.seriesName?.trim() === val)
        : filterValues.every(val => book.metadata?.seriesName?.trim() === val);
    case 'bookType':
      return book.isPhysical ? filterValues.includes('PHYSICAL') : filterValues.includes(book.primaryFile?.bookType);
    case 'readStatus':
      return doesBookMatchReadStatus(book, filterValues);
    case 'personalRating':
      return filterValues.some(range => isRatingInRange10(book.personalRating, range as string | number));
    case 'publisher':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.publisher === val)
        : filterValues.every(val => book.metadata?.publisher === val);
    case 'matchScore':
      return filterValues.some(range => isMatchScoreInRange(book.metadataMatchScore, range as string | number));
    case 'library':
      return effectiveMode === 'or'
        ? filterValues.some(val => val == book.libraryId)
        : filterValues.every(val => val == book.libraryId);
    case 'shelf':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.shelves?.some(s => s.id == val))
        : filterValues.every(val => book.shelves?.some(s => s.id == val));
    case 'shelfStatus':
      const shelved = book.shelves && book.shelves.length > 0 ? 'shelved' : 'unshelved';
      return filterValues.includes(shelved);
    case 'tag':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.tags?.includes(val as string))
        : filterValues.every(val => book.metadata?.tags?.includes(val as string));
    case 'publishedDate': {
      const bookYear = getBookPublishedYear(book.metadata?.publishedDate);
      return filterValues.some(val => doesYearMatchFilterValue(bookYear, val));
    }
    case 'fileSize':
      return filterValues.some(range => isFileSizeInRange(book.fileSizeKb, range as string | number));
    case 'amazonRating':
      return filterValues.some(range => isRatingInRange(book.metadata?.amazonRating, range as string | number));
    case 'goodreadsRating':
      return filterValues.some(range => isRatingInRange(book.metadata?.goodreadsRating, range as string | number));
    case 'hardcoverRating':
      return filterValues.some(range => isRatingInRange(book.metadata?.hardcoverRating, range as string | number));
    case 'language':
      return filterValues.includes(book.metadata?.language);
    case 'pageCount':
      return filterValues.some(range => isPageCountInRange(book.metadata?.pageCount!, range as string | number));
    case 'mood':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.moods?.includes(val as string))
        : filterValues.every(val => book.metadata?.moods?.includes(val as string));
    case 'ageRating':
      return filterValues.some(val => {
        const numVal = typeof val === 'string' ? Number(val) : val;
        return book.metadata?.ageRating === numVal;
      });
    case 'contentRating':
      return filterValues.includes(book.metadata?.contentRating);
    case 'narrator':
      return filterValues.includes(book.metadata?.narrator);
    case 'comicCharacter':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.comicMetadata?.characters?.includes(val as string))
        : filterValues.every(val => book.metadata?.comicMetadata?.characters?.includes(val as string));
    case 'comicTeam':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.comicMetadata?.teams?.includes(val as string))
        : filterValues.every(val => book.metadata?.comicMetadata?.teams?.includes(val as string));
    case 'comicLocation':
      return effectiveMode === 'or'
        ? filterValues.some(val => book.metadata?.comicMetadata?.locations?.includes(val as string))
        : filterValues.every(val => book.metadata?.comicMetadata?.locations?.includes(val as string));
    case 'comicCreator': {
      const comic = book.metadata?.comicMetadata;
      if (!comic) return false;
      const allCreators: string[] = [];
      const roles: [string[] | undefined, string][] = [
        [comic.pencillers, 'penciller'],
        [comic.inkers, 'inker'],
        [comic.colorists, 'colorist'],
        [comic.letterers, 'letterer'],
        [comic.coverArtists, 'coverArtist'],
        [comic.editors, 'editor']
      ];
      for (const [names, role] of roles) {
        if (names) {
          for (const name of names) {
            allCreators.push(`${name}:${role}`);
          }
        }
      }
      return effectiveMode === 'or'
        ? filterValues.some(val => allCreators.includes(val as string))
        : filterValues.every(val => allCreators.includes(val as string));
    }
    default:
      return false;
  }
}

export function filterBooksByFilters(
  books: Book[],
  activeFilters: Record<string, unknown[]> | null,
  mode: BookFilterMode,
  excludeFilterType?: string
): Book[] {
  if (!activeFilters) return books;

  const filterEntries = Object.entries(activeFilters)
    .filter(([type]) => type !== excludeFilterType);

  if (filterEntries.length === 0) return books;

  return books.filter(book => {
    const matches = filterEntries.map(([filterType, filterValues]) =>
      doesBookMatchFilter(book, filterType, filterValues, mode)
    );
    if (mode === 'not') return matches.every(m => !m);
    return mode === 'or' ? matches.some(m => m) : matches.every(m => m);
  });
}

export class SideBarFilter implements BookFilter {

  constructor(private selectedFilter$: Observable<unknown>, private selectedFilterMode$: Observable<BookFilterMode>) {
  }

  filter(bookState: BookState): Observable<BookState> {
    return combineLatest([this.selectedFilter$, this.selectedFilterMode$]).pipe(
      map(([activeFilters, mode]) => {
        if (bookState.books == null) return bookState;
        if (!activeFilters) return bookState;
        const filteredBooks = filterBooksByFilters(
          bookState.books || [],
          activeFilters as Record<string, unknown[]>,
          mode
        );
        return {...bookState, books: filteredBooks};
      })
    );
  }
}
