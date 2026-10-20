import {TestBed} from '@angular/core/testing';
import {firstValueFrom, of} from 'rxjs';
import {describe, expect, it} from 'vitest';
import {Book, ReadStatus} from '../../book/model/book.model';
import {BookService} from '../../book/service/book.service';
import {SeriesDataService} from './series-data.service';

describe('SeriesDataService', () => {
  it('treats a series of UNSET books as unread', async () => {
    const books = [
      createSeriesBook(1, ReadStatus.UNSET),
      createSeriesBook(2, ReadStatus.UNSET)
    ];
    TestBed.configureTestingModule({
      providers: [
        SeriesDataService,
        {provide: BookService, useValue: {bookState$: of({loaded: true, books})}}
      ]
    });

    const summaries = await firstValueFrom(TestBed.inject(SeriesDataService).allSeries$);

    expect(summaries).toHaveLength(1);
    expect(summaries[0].seriesStatus).toBe(ReadStatus.UNREAD);
  });

  it('treats a read book plus an UNSET book as partially read', async () => {
    const books = [
      createSeriesBook(1, ReadStatus.READ),
      createSeriesBook(2, ReadStatus.UNSET)
    ];
    TestBed.configureTestingModule({
      providers: [
        SeriesDataService,
        {provide: BookService, useValue: {bookState$: of({loaded: true, books})}}
      ]
    });

    const summaries = await firstValueFrom(TestBed.inject(SeriesDataService).allSeries$);

    expect(summaries[0].seriesStatus).toBe(ReadStatus.PARTIALLY_READ);
  });

  function createSeriesBook(id: number, readStatus: ReadStatus): Book {
    return {
      id,
      libraryId: 1,
      libraryName: 'Test Library',
      readStatus,
      metadata: {
        title: `Book ${id}`,
        seriesName: 'Test Series',
        seriesNumber: id
      }
    } as Book;
  }
});
