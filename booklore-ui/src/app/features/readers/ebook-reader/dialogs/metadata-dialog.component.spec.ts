import {TestBed} from '@angular/core/testing';
import {TranslocoService} from '@jsverse/transloco';
import {describe, expect, it} from 'vitest';
import {UrlHelperService} from '../../../../shared/service/url-helper.service';
import {ReaderBookMetadataDialogComponent} from './metadata-dialog.component';

describe('ReaderBookMetadataDialogComponent', () => {
  it('keeps a date-only publication date on the same calendar day', () => {
    TestBed.configureTestingModule({
      providers: [
        {provide: UrlHelperService, useValue: {}},
        {provide: TranslocoService, useValue: {translate: (key: string) => key}}
      ]
    });

    const component = TestBed.runInInjectionContext(() => new ReaderBookMetadataDialogComponent());

    expect(component.formatDate('2024-01-01')).toBe('January 1, 2024');
  });
});
