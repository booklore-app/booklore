import {beforeEach, describe, expect, it, vi} from 'vitest';
import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {ConfirmationService, MessageService} from 'primeng/api';
import {TranslocoTestingModule} from '@jsverse/transloco';
import {ActivatedRoute} from '@angular/router';

import {BookdropFileReviewComponent} from './bookdrop-file-review.component';
import {BookdropService} from '../../service/bookdrop.service';
import {LibraryService} from '../../../book/service/library.service';
import {AppSettingsService} from '../../../../shared/service/app-settings.service';
import {UrlHelperService} from '../../../../shared/service/url-helper.service';
import {DialogLauncherService} from '../../../../shared/services/dialog-launcher.service';
import {PageTitleService} from '../../../../shared/service/page-title.service';

const PENDING_FILE = {
  id: 7,
  fileName: 'prudentius.epub',
  filePath: '/bookdrop/prudentius.epub',
  originalMetadata: {title: 'Psychomachia', authors: ['Prudentius']},
  fetchedMetadata: {title: 'Psychomachia'},
  status: 'PENDING_REVIEW',
};

describe('BookdropFileReviewComponent pending-author flush (LOC-545)', () => {
  let finalizeSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    finalizeSpy = vi.fn(() => of({successCount: 1, failures: []}));

    TestBed.configureTestingModule({
      imports: [
        TranslocoTestingModule.forRoot({
          langs: {en: {}},
          translocoConfig: {availableLangs: ['en'], defaultLang: 'en', reRenderOnLangChange: false},
          preloadLangs: true,
        }),
      ],
      providers: [
        {
          provide: BookdropService,
          useValue: {
            getPendingFiles: vi.fn(() => of({content: [PENDING_FILE], page: {totalElements: 1}})),
            finalizeImport: finalizeSpy,
          },
        },
        {provide: LibraryService, useValue: {libraryState$: of({libraries: []})}},
        {provide: ConfirmationService, useValue: {confirm: vi.fn()}},
        {provide: MessageService, useValue: {add: vi.fn()}},
        {provide: AppSettingsService, useValue: {appSettings$: of({uploadPattern: '', metadataProviderSpecificFields: {}})}},
        {provide: UrlHelperService, useValue: {getBookdropCoverUrl: vi.fn(() => '')}},
        {provide: DialogLauncherService, useValue: {openBookdropFinalizeResultDialog: vi.fn()}},
        {provide: PageTitleService, useValue: {setPageTitle: vi.fn()}},
        {provide: ActivatedRoute, useValue: {queryParams: of({})}},
      ],
    });
  });

  it('carries an author typed but not confirmed into the finalize payload', () => {
    const fixture = TestBed.createComponent(BookdropFileReviewComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    const fileUi = component.bookdropFileUis[0];
    fileUi.selected = true;
    fileUi.selectedLibraryId = '1';
    fileUi.selectedPathId = '2';

    // expand the details panel the way a user does
    (fixture.nativeElement.querySelector('.file-name') as HTMLElement).click();
    fixture.detectChanges(false);

    expect(component.metadataPickers.length).toBe(1);

    const picker = component.metadataPickers.first;
    picker.authorInputValue = 'Helen Woodruff';

    (component as unknown as {finalizeImport(): void}).finalizeImport();

    const payload = finalizeSpy.mock.calls[0][0] as {files: {metadata: {authors: string[]}}[]};
    expect(payload.files[0].metadata.authors).toEqual(['Prudentius', 'Helen Woodruff']);
    expect(picker.authorInputValue).toBe('');
  });
});
