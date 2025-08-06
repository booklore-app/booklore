import {inject, Injectable} from '@angular/core';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {GithubSupportDialog} from './utilities/component/github-support-dialog/github-support-dialog';
import {LibraryCreatorComponent} from './book/components/library-creator/library-creator.component';
import {BookUploaderComponent} from './utilities/component/book-uploader/book-uploader.component';
import {UserProfileDialogComponent} from './settings/global-preferences/user-profile-dialog/user-profile-dialog.component';
import {MagicShelfComponent} from './magic-shelf-component/magic-shelf-component';

@Injectable({
  providedIn: 'root',
})
export class DialogLauncherService {

  dialogService = inject(DialogService);

  open(options: { component: any; header: string; top?: string; width?: string }): DynamicDialogRef {
    const isMobile = window.innerWidth <= 768;
    const {component, header, top, width} = options;
    return this.dialogService.open(component, {
      header,
      modal: true,
      closable: true,
      contentStyle: {
        overflowY: 'hidden',
      },
      style: {
        position: 'absolute',
        ...(top ? {top} : {}),
        ...(isMobile
          ? {
            width: '90vw',
            maxWidth: '90vw',
            minWidth: '90vw',
          }
          : width
            ? {width}
            : {}),
      },
    });
  }

  openGithubSupportDialog(): void {
    this.open({
      component: GithubSupportDialog,
      header: 'Support BookLore on GitHub',
      top: '25%'
    });
  }

  openLibraryCreatorDialog(): void {
    this.open({
      component: LibraryCreatorComponent,
      header: 'Create New Library',
      top: '15%'
    });
  }

  openFileUploadDialog(): void {
    this.open({
      component: BookUploaderComponent,
      header: 'Book Uploader',
      top: '10%'
    });
  }

  openUserProfileDialog(): void {
    this.open({
      component: UserProfileDialogComponent,
      header: 'User Profile Information',
      top: '10%'
    });
  }

  openMagicShelfDialog(): void {
    this.open({
      component: MagicShelfComponent,
      header: 'Magic Shelf Creator'
    });
  }
}
