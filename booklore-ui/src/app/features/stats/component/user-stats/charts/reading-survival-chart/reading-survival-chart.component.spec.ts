import {TestBed} from '@angular/core/testing';
import {TranslocoService} from '@jsverse/transloco';
import 'chartjs-plugin-datalabels';
import {describe, expect, it} from 'vitest';
import {Book} from '../../../../../book/model/book.model';
import {BookService} from '../../../../../book/service/book.service';
import {ReadingSurvivalChartComponent} from './reading-survival-chart.component';

describe('ReadingSurvivalChartComponent', () => {
  function createComponent(books: Book[]): ReadingSurvivalChartComponent {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: BookService,
          useValue: {getCurrentBookState: () => ({loaded: true, books})}
        },
        {provide: TranslocoService, useValue: {translate: (key: string) => key}}
      ]
    });

    return TestBed.runInInjectionContext(() => new ReadingSurvivalChartComponent());
  }

  function calculate(component: ReadingSurvivalChartComponent): void {
    const calculate = component as unknown as {calculateSurvivalCurve(): void};
    calculate.calculateSurvivalCurve();
  }

  it('does not render an invalid danger-zone range when no threshold has a drop', () => {
    const completedBook = {pdfProgress: {page: 100, percentage: 100}} as Book;
    const component = createComponent([completedBook]);
    calculate(component);

    expect(component.dangerZoneRange).toBe('—');
    expect(component.dangerZoneDrop).toBe('0%');
  });

  it('includes an audiobook-only book among started books', () => {
    const audiobook = {audiobookProgress: {positionMs: 1000, percentage: 62.5}} as Book;
    const component = createComponent([audiobook]);
    calculate(component);

    expect(component.totalStarted).toBe(1);
  });
});
