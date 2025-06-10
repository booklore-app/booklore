import {inject, Injectable} from '@angular/core';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {ShelfAssignerComponent} from '../shelf-assigner/shelf-assigner.component';
import {LockUnlockMetadataDialogComponent} from './lock-unlock-metadata-dialog/lock-unlock-metadata-dialog.component';
import {MetadataFetchOptionsComponent} from '../../metadata/metadata-options-dialog/metadata-fetch-options/metadata-fetch-options.component';
import {MetadataRefreshType} from '../../metadata/model/request/metadata-refresh-type.enum';

@Injectable({providedIn: 'root'})
export class BookDialogHelperService {

  private dialogService = inject(DialogService);

  openShelfAssigner(bookIds: Set<number>): DynamicDialogRef {
    return this.dialogService.open(ShelfAssignerComponent, {
      header: `Update Books' Shelves`,
      modal: true,
      closable: true,
      contentStyle: {overflow: 'auto'},
      baseZIndex: 10,
      style: {
        position: 'absolute',
        top: '15%',
      },
      data: {
        isMultiBooks: true,
        bookIds,
      },
    });
  }

  openLockUnlockMetadataDialog(bookIds: Set<number>): DynamicDialogRef {
    const count = bookIds.size;
    return this.dialogService.open(LockUnlockMetadataDialogComponent, {
      header: `Lock or Unlock Metadata for ${count} Selected Book${count > 1 ? 's' : ''}`,
      modal: true,
      closable: true,
      data: {
        bookIds: Array.from(bookIds),
      },
    });
  }

  openMetadataRefreshDialog(bookIds: Set<number>): DynamicDialogRef {
    return this.dialogService.open(MetadataFetchOptionsComponent, {
      header: 'Metadata Refresh Options',
      modal: true,
      closable: true,
      data: {
        bookIds: Array.from(bookIds),
        metadataRefreshType: MetadataRefreshType.BOOKS,
      },
    });
  }
}
