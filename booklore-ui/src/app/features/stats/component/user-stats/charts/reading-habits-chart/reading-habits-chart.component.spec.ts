import {TestBed} from '@angular/core/testing';
import {TranslocoService} from '@jsverse/transloco';
import 'chartjs-plugin-datalabels';
import {describe, expect, it} from 'vitest';
import {Book} from '../../../../../book/model/book.model';
import {BookService} from '../../../../../book/service/book.service';
import {ReadingHabitsChartComponent} from './reading-habits-chart.component';

describe('ReadingHabitsChartComponent', () => {
  it('uses audiobook progress in its habit calculations', () => {
    TestBed.configureTestingModule({
      providers: [
        {provide: BookService, useValue: {}},
        {provide: TranslocoService, useValue: {translate: (key: string) => key}}
      ]
    });

    const component = TestBed.runInInjectionContext(() => new ReadingHabitsChartComponent());
    const progress = component as unknown as {getBookProgress(book: Book): number};
    const audiobook = {audiobookProgress: {positionMs: 1000, percentage: 62.5}} as Book;

    expect(progress.getBookProgress(audiobook)).toBe(62.5);
  });
});
