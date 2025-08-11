import {Component, ElementRef, EventEmitter, inject, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {Book, ReadStatus} from '../../../model/book.model';
import {Button} from 'primeng/button';
import {MenuModule} from 'primeng/menu';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {DialogService} from 'primeng/dynamicdialog';
import {ShelfAssignerComponent} from '../../shelf-assigner/shelf-assigner.component';
import {BookService} from '../../../service/book.service';
import {CheckboxChangeEvent, CheckboxModule} from 'primeng/checkbox';
import {FormsModule} from '@angular/forms';
import {MetadataFetchOptionsComponent} from '../../../../metadata/metadata-options-dialog/metadata-fetch-options/metadata-fetch-options.component';
import {MetadataRefreshType} from '../../../../metadata/model/request/metadata-refresh-type.enum';
import {MetadataRefreshRequest} from '../../../../metadata/model/request/metadata-refresh-request.model';
import {UrlHelperService} from '../../../../utilities/service/url-helper.service';
import {NgClass} from '@angular/common';
import {UserService} from '../../../../settings/user-management/user.service';
import {filter, Subject} from 'rxjs';
import {EmailService} from '../../../../settings/email/email.service';
import {TieredMenu} from 'primeng/tieredmenu';
import {BookSenderComponent} from '../../book-sender/book-sender.component';
import {Router} from '@angular/router';
import {ProgressBar} from 'primeng/progressbar';
import {BookMetadataCenterComponent} from '../../../../metadata/book-metadata-center-component/book-metadata-center.component';
import {takeUntil} from 'rxjs/operators';
import {readStatusLabels} from '../book-filter/book-filter.component';
import {ResetProgressTypes} from '../../../../shared/constants/reset-progress-type';

@Component({
  selector: 'app-book-card',
  templateUrl: './book-card.component.html',
  styleUrls: ['./book-card.component.scss'],
  imports: [Button, MenuModule, CheckboxModule, FormsModule, NgClass, TieredMenu, ProgressBar],
  standalone: true
})
export class BookCardComponent implements OnInit, OnDestroy {

  @Output() checkboxClick = new EventEmitter<{ index: number; bookId: number; selected: boolean; shiftKey: boolean }>();

  @Input() index!: number;
  @Input() book!: Book;
  @Input() isCheckboxEnabled: boolean = false;
  @Input() onBookSelect?: (bookId: number, selected: boolean) => void;
  @Input() isSelected: boolean = false;
  @Input() bottomBarHidden: boolean = false;
  @Input() readButtonHidden: boolean = false;
  @Input() isSeriesCollapsed: boolean = false;

  @ViewChild('checkboxElem') checkboxElem!: ElementRef<HTMLInputElement>;

  items: MenuItem[] | undefined;
  isHovered: boolean = false;
  isImageLoaded: boolean = false;

  private bookService = inject(BookService);
  private dialogService = inject(DialogService);
  private userService = inject(UserService);
  private emailService = inject(EmailService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  protected urlHelper = inject(UrlHelperService);
  private confirmationService = inject(ConfirmationService);

  private userPermissions: any;
  private metadataCenterViewMode: 'route' | 'dialog' = 'route';
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.userService.userState$
      .pipe(
        filter(userState => !!userState),
        takeUntil(this.destroy$)
      )
      .subscribe(userState => {
        this.userPermissions = userState.user?.permissions;
        this.metadataCenterViewMode = userState?.user?.userSettings.metadataCenterViewMode ?? 'route';
        this.initMenu();
      });
  }

  get progressPercentage(): number | null {
    if (this.book.epubProgress?.percentage != null) {
      return this.book.epubProgress.percentage;
    }
    if (this.book.pdfProgress?.percentage != null) {
      return this.book.pdfProgress.percentage;
    }
    if (this.book.cbxProgress?.percentage != null) {
      return this.book.cbxProgress.percentage;
    }
    return null;
  }

  get koProgressPercentage(): number | null {
    if (this.book.koreaderProgress?.percentage != null) {
      return this.book.koreaderProgress.percentage;
    }
    return null;
  }

  onImageLoad(): void {
    this.isImageLoaded = true;
  }

  readBook(book: Book): void {
    this.bookService.readBook(book.id);
  }

  private initMenu() {
    this.items = [
      {
        label: 'Assign Shelf',
        icon: 'pi pi-folder',
        command: () => this.openShelfDialog()
      },
      {
        label: 'View Details',
        icon: 'pi pi-info-circle',
        command: () => {
          setTimeout(() => {
            if (this.metadataCenterViewMode === 'route') {
              this.router.navigate(['/book', this.book.id], {
                queryParams: {tab: 'view'}
              });
            } else {
              this.dialogService.open(BookMetadataCenterComponent, {
                width: '95%',
                data: {bookId: this.book.id},
                modal: true,
                dismissableMask: true,
                showHeader: false
              });
            }
          }, 150);
        },
      },
      ...this.getPermissionBasedMenuItems(),
      ...this.moreMenuItems(),
    ];
  }

  private getPermissionBasedMenuItems(): MenuItem[] {
    const items: MenuItem[] = [];

    if (this.hasDownloadPermission()) {
      items.push({
        label: 'Download',
        icon: 'pi pi-download',
        command: () => {
          this.bookService.downloadFile(this.book.id);
        },
      });
    }

    if (this.hasDeleteBookPermission()) {
      items.push({
        label: 'Delete Book',
        icon: 'pi pi-trash',
        command: () => {
          this.confirmationService.confirm({
            message: `Are you sure you want to delete "${this.book.metadata?.title}"?`,
            header: 'Confirm Deletion',
            icon: 'pi pi-exclamation-triangle',
            acceptIcon: 'pi pi-trash',
            rejectIcon: 'pi pi-times',
            acceptButtonStyleClass: 'p-button-danger',
            accept: () => {
              this.bookService.deleteBooks(new Set([this.book.id])).subscribe();
            }
          });
        },
      });
    }

    if (this.hasEmailBookPermission()) {
      items.push(
        {
          label: 'Send Book',
          icon: 'pi pi-envelope',
          items: [{
            label: 'Quick Send',
            icon: 'pi pi-envelope',
            command: () => {
              this.emailService.emailBookQuick(this.book.id).subscribe({
                next: () => {
                  this.messageService.add({
                    severity: 'info',
                    summary: 'Success',
                    detail: 'The book sending has been scheduled.',
                  });
                },
                error: (err) => {
                  const errorMessage = err?.error?.message || 'An error occurred while sending the book.';
                  this.messageService.add({
                    severity: 'error',
                    summary: 'Error',
                    detail: errorMessage,
                  });
                },
              });
            }
          },
            {
              label: 'Custom Send',
              icon: 'pi pi-envelope',
              command: () => {
                this.dialogService.open(BookSenderComponent, {
                  header: 'Send Book to Email',
                  modal: true,
                  closable: true,
                  style: {
                    position: 'absolute',
                    top: '15%',
                  },
                  data: {
                    bookId: this.book.id,
                  }
                });
              }
            }
          ]
        });
    }

    if (this.hasEditMetadataPermission()) {
      items.push({
        label: 'Metadata',
        icon: 'pi pi-database',
        items: [
          {
            label: 'Search Metadata',
            icon: 'pi pi-sparkles',
            command: () => {
              setTimeout(() => {
                this.router.navigate(['/book', this.book.id], {
                  queryParams: {tab: 'match'}
                })
              }, 150);
            },
          },
          {
            label: 'Auto Fetch',
            icon: 'pi pi-bolt',
            command: () => {
              const metadataRefreshRequest: MetadataRefreshRequest = {
                quick: true,
                refreshType: MetadataRefreshType.BOOKS,
                bookIds: [this.book.id],
              };
              this.bookService.autoRefreshMetadata(metadataRefreshRequest).subscribe();
            },
          },
          {
            label: 'Advanced Fetch',
            icon: 'pi pi-database',
            command: () => {
              this.dialogService.open(MetadataFetchOptionsComponent, {
                header: 'Metadata Refresh Options',
                modal: true,
                closable: true,
                data: {
                  bookIds: [this.book!.id],
                  metadataRefreshType: MetadataRefreshType.BOOKS,
                },
              });
            },
          }
        ]
      });
    }

    return items;
  }

  private moreMenuItems(): MenuItem[] {
    const items: MenuItem[] = [];

    if (this.hasEditMetadataPermission()) {
      items.push({
        label: 'More Actions',
        icon: 'pi pi-ellipsis-h',
        items: [
          {
            label: 'Read Status',
            icon: 'pi pi-book',
            items: Object.entries(readStatusLabels).map(([status, label]) => ({
              label,
              command: () => {
                this.bookService.updateBookReadStatus(this.book.id, status as ReadStatus).subscribe({
                  next: () => {
                    this.messageService.add({
                      severity: 'success',
                      summary: 'Read Status Updated',
                      detail: `Marked as "${label}"`,
                      life: 2000
                    });
                  },
                  error: () => {
                    this.messageService.add({
                      severity: 'error',
                      summary: 'Update Failed',
                      detail: 'Could not update read status.',
                      life: 3000
                    });
                  }
                });
              }
            }))
          },
          {
            label: 'Reset Booklore Progress',
            icon: 'pi pi-undo',
            command: () => {
              this.bookService.resetProgress(this.book.id, ResetProgressTypes.BOOKLORE).subscribe({
                next: () => {
                  this.messageService.add({
                    severity: 'success',
                    summary: 'Progress Reset',
                    detail: 'Booklore reading progress has been reset.',
                    life: 1500
                  });
                },
                error: () => {
                  this.messageService.add({
                    severity: 'error',
                    summary: 'Failed',
                    detail: 'Could not reset Booklore progress.',
                    life: 1500
                  });
                }
              });
            },
          },
          {
            label: 'Reset KOReader Progress',
            icon: 'pi pi-undo',
            command: () => {
              this.bookService.resetProgress(this.book.id, ResetProgressTypes.KOREADER).subscribe({
                next: () => {
                  this.messageService.add({
                    severity: 'success',
                    summary: 'Progress Reset',
                    detail: 'KOReader reading progress has been reset.',
                    life: 1500
                  });
                },
                error: () => {
                  this.messageService.add({
                    severity: 'error',
                    summary: 'Failed',
                    detail: 'Could not reset KOReader progress.',
                    life: 1500
                  });
                }
              });
            },
          }
        ]
      });
    }

    return items;
  }

  private openShelfDialog(): void {
    this.dialogService.open(ShelfAssignerComponent, {
      header: `Update Book's Shelves`,
      modal: true,
      closable: true,
      contentStyle: {overflow: 'auto'},
      baseZIndex: 10,
      style: {
        position: 'absolute',
        top: '15%',
      },
      data: {
        book: this.book,
      },
    });
  }

  openBookInfo(book: Book): void {
    if (this.metadataCenterViewMode === 'route') {
      this.router.navigate(['/book', book.id], {
        queryParams: {tab: 'view'}
      });
    } else {
      this.dialogService.open(BookMetadataCenterComponent, {
        width: '85%',
        data: {bookId: book.id},
        modal: true,
        dismissableMask: true,
        showHeader: false
      });
    }
  }

  private isAdmin(): boolean {
    return this.userPermissions?.admin ?? false;
  }

  private hasEditMetadataPermission(): boolean {
    return this.isAdmin() || (this.userPermissions?.canEditMetadata ?? false);
  }

  private hasDownloadPermission(): boolean {
    return this.isAdmin() || (this.userPermissions?.canDownload ?? false);
  }

  private hasEmailBookPermission(): boolean {
    return this.isAdmin() || (this.userPermissions?.canEmailBook ?? false);
  }

  private hasDeleteBookPermission(): boolean {
    return this.isAdmin() || (this.userPermissions?.canDeleteBook ?? false);
  }

  private lastMouseEvent: MouseEvent | null = null;

  captureMouseEvent(event: MouseEvent): void {
    this.lastMouseEvent = event;
  }

  toggleSelection(event: CheckboxChangeEvent): void {
    if (this.isCheckboxEnabled) {
      this.isSelected = event.checked;
      const shiftKey = this.lastMouseEvent?.shiftKey ?? false;
      this.checkboxClick.emit({
        index: this.index,
        bookId: this.book.id,
        selected: event.checked,
        shiftKey: shiftKey,
      });
      if (this.onBookSelect) {
        this.onBookSelect(this.book.id, event.checked);
      }
      this.lastMouseEvent = null;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
