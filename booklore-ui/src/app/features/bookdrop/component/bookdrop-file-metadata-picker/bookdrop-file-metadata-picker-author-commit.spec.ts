import {beforeEach, describe, expect, it, vi} from 'vitest';
import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {ConfirmationService} from 'primeng/api';
import {TranslocoService} from '@jsverse/transloco';

import {BookdropFileMetadataPickerComponent} from './bookdrop-file-metadata-picker.component';
import {UrlHelperService} from '../../../../shared/service/url-helper.service';
import {MetadataUtilsService} from '../../../../shared/metadata';
import {AppSettingsService} from '../../../../shared/service/app-settings.service';
import {FormControl, FormGroup} from '@angular/forms';

/**
 * LOC-545: an author typed into the chips input was only committed to the
 * `authors` FormControl on Enter or autocomplete selection. Calling
 * finalizeImport() without pressing Enter silently dropped the pending value.
 */
describe('BookdropFileMetadataPickerComponent commitPendingAuthor', () => {
  let component: BookdropFileMetadataPickerComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {provide: ConfirmationService, useValue: {confirm: vi.fn()}},
        {provide: UrlHelperService, useValue: {getBookdropCoverUrl: vi.fn(() => '')}},
        {provide: MetadataUtilsService, useValue: {}},
        {provide: AppSettingsService, useValue: {appSettings$: of(null)}},
        {provide: TranslocoService, useValue: {translate: vi.fn((key: string) => key)}},
      ],
    });

    component = TestBed.createComponent(BookdropFileMetadataPickerComponent).componentInstance;
    component.metadataForm = new FormGroup({
      authors: new FormControl<string[]>([]),
    });
    component.copiedFields = {};
    component.savedFields = {};
    component.bookdropFileId = 1;
    component.fetchedMetadata = {} as never;
  });

  it('flushes a typed-but-not-confirmed author into the authors control', () => {
    component.authorInputValue = 'Helen Woodruff';

    component.commitPendingAuthor();

    expect(component.metadataForm.get('authors')?.value).toEqual(['Helen Woodruff']);
    expect(component.authorInputValue).toBe('');
    expect(component.metadataForm.get('authors')?.dirty).toBe(true);
  });

  it('preserves existing chips alongside the newly committed author', () => {
    component.metadataForm.get('authors')?.setValue(['Prudentius']);
    component.authorInputValue = 'Helen Woodruff';

    component.commitPendingAuthor();

    expect(component.metadataForm.get('authors')?.value).toEqual(['Prudentius', 'Helen Woodruff']);
  });

  it('does not add a duplicate when the author is already a chip', () => {
    component.metadataForm.get('authors')?.setValue(['Helen Woodruff']);
    component.authorInputValue = 'Helen Woodruff';

    component.commitPendingAuthor();

    expect(component.metadataForm.get('authors')?.value).toEqual(['Helen Woodruff']);
  });

  it('ignores whitespace-only input', () => {
    component.metadataForm.get('authors')?.setValue(['Helen Woodruff']);
    component.authorInputValue = '   ';

    component.commitPendingAuthor();

    expect(component.metadataForm.get('authors')?.value).toEqual(['Helen Woodruff']);
  });
});
