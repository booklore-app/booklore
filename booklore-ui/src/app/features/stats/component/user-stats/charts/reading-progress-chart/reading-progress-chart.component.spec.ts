import {TestBed} from '@angular/core/testing';
import {TranslocoService} from '@jsverse/transloco';
import 'chartjs-plugin-datalabels';
import {describe, expect, it} from 'vitest';
import {Book} from '../../../../../book/model/book.model';
import {BookService} from '../../../../../book/service/book.service';
import {ReadingProgressChartComponent} from './reading-progress-chart.component';

describe('ReadingProgressChartComponent', () => {
  function createComponent(): ReadingProgressChartComponent {
    TestBed.configureTestingModule({
      providers: [
        {provide: BookService, useValue: {}},
        {provide: TranslocoService, useValue: {translate: (key: string) => key}}
      ]
    });

    return TestBed.runInInjectionContext(() => new ReadingProgressChartComponent());
  }

  it('places fractional progress immediately above a boundary in the next bucket', () => {
    const component = createComponent();
    const calculate = component as unknown as {
      processReadingProgressStats(books: Book[]): {progressRange: string; count: number}[];
    };
    const books = [
      {pdfProgress: {page: 1, percentage: 25.1}},
      {pdfProgress: {page: 1, percentage: 50.5}},
      {pdfProgress: {page: 1, percentage: 75.5}},
      {pdfProgress: {page: 1, percentage: 99.5}}
    ] as Book[];

    const counts = Object.fromEntries(
      calculate.processReadingProgressStats(books).map(item => [item.progressRange, item.count])
    );

    expect(counts['26-50%']).toBe(1);
    expect(counts['51-75%']).toBe(1);
    expect(counts['76-99%']).toBe(2);
    expect(counts['100%']).toBe(0);
  });
});
