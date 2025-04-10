import {Component, inject, OnInit, OnDestroy} from '@angular/core';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {MetadataEditorComponent} from './metadata-editor/metadata-editor.component';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {MetadataSearcherComponent} from './metadata-searcher/metadata-searcher.component';
import {BookMetadataCenterService} from './book-metadata-center.service';
import {MetadataViewerComponent} from './metadata-viewer/metadata-viewer.component';
import {UserService} from '../../user.service';
import {Subscription} from 'rxjs';
import {NgIf} from '@angular/common';
import {Book} from '../../book/model/book.model';

@Component({
  selector: 'app-book-metadata-center',
  standalone: true,
  templateUrl: './book-metadata-center.component.html',
  styleUrl: './book-metadata-center.component.scss',
  imports: [Tabs, TabList, Tab, TabPanels, MetadataEditorComponent, TabPanel, MetadataSearcherComponent, MetadataViewerComponent, NgIf],
})
export class BookMetadataCenterComponent implements OnInit, OnDestroy {

  private dynamicDialogConfig = inject(DynamicDialogConfig);
  private dynamicDialogRef = inject(DynamicDialogRef);
  private metadataCenterService = inject(BookMetadataCenterService);
  private userService = inject(UserService);

  tab = 'view';
  canEditMetadata: boolean = false;
  private userSubscription: Subscription = Subscription.EMPTY;
  book: Book | undefined;

  ngOnInit(): void {
    this.book = this.dynamicDialogConfig.data.book;
    this.tab = this.dynamicDialogConfig.data.tab;

    if (this.book?.metadata) {
      this.metadataCenterService.emit(this.book.metadata);
    }

    this.userSubscription = this.userService.userData$.subscribe(userData => {
      const userPermissions = userData?.permissions;
      this.canEditMetadata = userPermissions?.canEditMetadata ?? false;
    });

    this.metadataCenterService.dialogClose$.subscribe((close) => {
      if (close) {
        this.metadataCenterService.closeDialog(false);
        this.dynamicDialogRef.close();
      }
    });
  }

  ngOnDestroy(): void {
    // Clean up subscription to prevent memory leaks
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
  }
}
