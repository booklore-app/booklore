import {TestBed} from '@angular/core/testing';
import {firstValueFrom, of} from 'rxjs';
import {beforeEach, describe, expect, it} from 'vitest';
import {Book} from '../../../model/book.model';
import {BookService} from '../../../service/book.service';
import {LibraryService} from '../../../service/library.service';
import {BookRuleEvaluatorService} from '../../../../magic-shelf/service/book-rule-evaluator.service';
import {EntityType} from '../book-browser.component';
import {BookFilterService} from './book-filter.service';
import {NO_VALUE_ID} from './book-filter.config';

const book = (id: number, fields: Record<string, unknown>, bookType = 'EPUB') =>
  ({id, libraryId: 1, primaryFile: {bookType}, metadata: fields} as unknown as Book);

const books = [
  book(1, {seriesName: 'Big Saga', publishedDate: '1990-03-01', narrator: 'Nina'}, 'AUDIOBOOK'),
  book(2, {seriesName: 'Big Saga', publishedDate: '2010-03-01'}),
  book(3, {seriesName: 'Big Saga', publishedDate: '2000-03-01'}),
  book(4, {seriesName: 'Small Saga'}),
  book(5, {}),
  book(6, {}),
  book(7, {}, 'AUDIOBOOK')
];

describe('BookFilterService "No ..." entries', () => {
  let service: BookFilterService;

  const listOf = async (type: Parameters<BookFilterService['createFilterStreams']>[0] extends never ? never : string) => {
    const streams = service.createFilterStreams(of(null), of(EntityType.LIBRARY)) as Record<string, any>;
    return await firstValueFrom(streams[type]) as {value: {id: unknown; name: string}; bookCount: number}[];
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {provide: BookService, useValue: {bookState$: of({books, loaded: true, error: null})}},
        {provide: LibraryService, useValue: {libraryState$: of({libraries: []})}},
        {provide: BookRuleEvaluatorService, useValue: {evaluateGroup: () => true}}
      ]
    });
    service = TestBed.inject(BookFilterService);
  });

  it('adds a "No Series" entry with the right count and pins it first, ahead of bigger series', async () => {
    const list = await listOf('series');
    expect(list[0].value.id).toBe(NO_VALUE_ID);
    expect(list[0].value.name).toBe('No Series');
    expect(list[0].bookCount).toBe(3);               // books 5, 6, 7
    expect(list.slice(1).map(f => f.value.name)).toEqual(['Big Saga', 'Small Saga']);
  });

  it('adds "No Year" ahead of the years, which stay newest first', async () => {
    const list = await listOf('publishedDate');
    expect(list.map(f => f.value.name)).toEqual(['No Year', '2010', '2000', '1990']);
    expect(list[0].bookCount).toBe(4);               // books 4, 5, 6 and 7 have no year
  });

  it('lists only audiobooks under "No Narrator"', async () => {
    const list = await listOf('narrator');
    expect(list.map(f => [f.value.name, f.bookCount])).toEqual([['No Narrator', 1], ['Nina', 1]]);   // only book 7: ebooks without a narrator are not listed
  });

  it('has no "No ..." entry when every book has a value', async () => {
    const list = await listOf('readStatus');
    expect(list.some(f => f.value.id === NO_VALUE_ID)).toBe(false);
  });

  it('keeps the "No ..." id as text for numeric filters, so it survives the URL round trip', () => {
    expect(service.processFilterValue('pageCount', NO_VALUE_ID)).toBe(NO_VALUE_ID);
    expect(service.processFilterValue('pageCount', '3')).toBe(3);
    expect(service.processFilterValue('series', 'Big Saga')).toBe('Big Saga');
  });
});
