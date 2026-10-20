import {beforeEach, describe, expect, it, vi} from 'vitest';
import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {MessageService} from 'primeng/api';
import {Router} from '@angular/router';
import {TranslocoService} from '@jsverse/transloco';

import {MetadataEditorComponent} from './metadata-editor.component';
import {BookService} from '../../../../book/service/book.service';
import {BookMetadataManageService} from '../../../../book/service/book-metadata-manage.service';
import {TaskHelperService} from '../../../../settings/task-management/task-helper.service';
import {UrlHelperService} from '../../../../../shared/service/url-helper.service';
import {BookDialogHelperService} from '../../../../book/components/book-browser/book-dialog-helper.service';
import {BookNavigationService} from '../../../../book/service/book-navigation.service';
import {BookMetadataHostService} from '../../../../../shared/service/book-metadata-host.service';
import {UserService} from '../../../../settings/user-management/user.service';
import {AppSettingsService} from '../../../../../shared/service/app-settings.service';
import {MetadataUpdateWrapper} from '../../../../book/model/book.model';

/**
 * LOC-539: an author typed into the chips input was only copied into the
 * `authors` form control on Enter or on clicking a suggestion. Clicking Save
 * straight after typing sent `authors: []`, so the name was never persisted —
 * and because the input kept showing the text, the loss only surfaced after a
 * reload (and as "Unknown Author" on a generated cover).
 */
describe('MetadataEditorComponent author commit', () => {
  let component: MetadataEditorComponent;
  let sentWrappers: MetadataUpdateWrapper[];

  beforeEach(() => {
    sentWrappers = [];

    TestBed.configureTestingModule({
      providers: [
        {provide: MessageService, useValue: {add: vi.fn()}},
        {provide: BookService, useValue: {bookState$: of({loaded: false, books: []})}},
        {
          provide: BookMetadataManageService,
          useValue: {
            updateBookMetadata: vi.fn((_id: number, wrapper: MetadataUpdateWrapper) => {
              sentWrappers.push(wrapper);
              return of({});
            }),
          },
        },
        {provide: TaskHelperService, useValue: {}},
        {provide: UrlHelperService, useValue: {}},
        {provide: BookDialogHelperService, useValue: {}},
        {provide: BookNavigationService, useValue: {getNavigationState: vi.fn(() => of(null))}},
        {provide: BookMetadataHostService, useValue: {}},
        {provide: Router, useValue: {}},
        {provide: UserService, useValue: {}},
        {provide: AppSettingsService, useValue: {appSettings$: of(null)}},
        {provide: TranslocoService, useValue: {translate: vi.fn((key: string) => key)}},
      ],
    });

    component = TestBed.createComponent(MetadataEditorComponent).componentInstance;
    component.currentBookId = 105;
    component.originalMetadata = {bookId: 105, authors: []} as never;
  });

  const savedAuthors = () => sentWrappers[0].metadata.authors;

  it('saves an author that was typed but never confirmed with Enter', () => {
    component.authorInputValue = 'Helen Woodruff';

    component.onSave();

    expect(savedAuthors()).toEqual(['Helen Woodruff']);
    expect(component.authorInputValue).toBe('');
  });

  it('keeps authors already confirmed as chips', () => {
    component.metadataForm.get('authors')?.setValue(['Prudentius']);
    component.authorInputValue = 'Helen Woodruff';

    component.onSave();

    expect(savedAuthors()).toEqual(['Prudentius', 'Helen Woodruff']);
  });

  it('does not duplicate an author that is already a chip', () => {
    component.metadataForm.get('authors')?.setValue(['Helen Woodruff']);
    component.authorInputValue = 'Helen Woodruff';

    component.onSave();

    expect(savedAuthors()).toEqual(['Helen Woodruff']);
  });

  it('ignores whitespace left in the input', () => {
    component.metadataForm.get('authors')?.setValue(['Helen Woodruff']);
    component.authorInputValue = '   ';

    component.onSave();

    expect(savedAuthors()).toEqual(['Helen Woodruff']);
  });
});
