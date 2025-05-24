import {Component, HostListener, inject, OnInit} from '@angular/core';
import {NgFor, NgIf} from '@angular/common';
import {ActivatedRoute} from '@angular/router';
import {CbxReaderService} from '../../service/cbx-reader.service';
import {BookService} from '../../service/book.service';
import {UserService} from '../../../settings/user-management/user.service';
import {MessageService} from 'primeng/api';
import {forkJoin} from 'rxjs';
import {BookSetting, CbxPageSpread, CbxPageViewMode} from '../../model/book.model';
import {ProgressSpinner} from 'primeng/progressspinner';
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-cbx-reader',
  standalone: true,
  imports: [NgIf, NgFor, ProgressSpinner, FormsModule],
  templateUrl: './cbx-reader.component.html',
  styleUrl: './cbx-reader.component.scss'
})
export class CbxReaderComponent implements OnInit {
  goToPageInput: number | null = null;
  bookId!: number;
  pages: number[] = [];
  currentPage = 0;
  isLoading = true;

  pageSpread: CbxPageSpread = CbxPageSpread.ODD;
  pageViewMode: CbxPageViewMode = CbxPageViewMode.SINGLE_PAGE;

  private touchStartX = 0;
  private touchEndX = 0;

  private route = inject(ActivatedRoute);
  private cbxReaderService = inject(CbxReaderService);
  private bookService = inject(BookService);
  private userService = inject(UserService);
  private messageService = inject(MessageService);

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      this.isLoading = true;
      this.bookId = +params.get('bookId')!;

      forkJoin([
        this.bookService.getBookByIdFromAPI(this.bookId, false),
        this.bookService.getBookSetting(this.bookId),
        this.userService.getMyself(),
        this.cbxReaderService.getAvailablePages(this.bookId)
      ]).subscribe({
        next: ([cbxMeta, bookSettings, myself, pages]) => {
          const userSettings = myself.userSettings;
          const global = userSettings.perBookSetting.cbx === 'Global';

          this.pageViewMode = global
            ? userSettings.cbxReaderSetting.pageViewMode || CbxPageViewMode.SINGLE_PAGE
            : bookSettings.cbxSettings?.pageViewMode || userSettings.cbxReaderSetting.pageViewMode || CbxPageViewMode.SINGLE_PAGE;

          this.pageSpread = global
            ? userSettings.cbxReaderSetting.pageSpread || CbxPageSpread.ODD
            : bookSettings.cbxSettings?.pageSpread || userSettings.cbxReaderSetting.pageSpread || CbxPageSpread.ODD;

          this.pages = pages;
          this.currentPage = (cbxMeta.cbxProgress?.page || 1) - 1;
          this.alignCurrentPageToParity();
          this.isLoading = false;
        },
        error: (err) => {
          const errorMessage = err?.error?.message || 'Failed to load the book';
          this.messageService.add({severity: 'error', summary: 'Error', detail: errorMessage});
          this.isLoading = false;
        }
      });
    });
  }

  get isTwoPageView(): boolean {
    return this.pageViewMode === CbxPageViewMode.TWO_PAGE;
  }

  toggleView() {
    if (!this.isTwoPageView && this.isPhonePortrait()) return;
    this.pageViewMode = this.isTwoPageView ? CbxPageViewMode.SINGLE_PAGE : CbxPageViewMode.TWO_PAGE;
    this.alignCurrentPageToParity();
    this.updateViewerSetting();
  }

  toggleSpreadDirection() {
    this.pageSpread = this.pageSpread === CbxPageSpread.ODD ? CbxPageSpread.EVEN : CbxPageSpread.ODD;
    this.alignCurrentPageToParity();
    this.updateViewerSetting();
  }

  nextPage() {
    const previousPage = this.currentPage;

    if (this.isTwoPageView) {
      if (this.currentPage + 2 < this.pages.length) {
        this.currentPage += 2;
      } else if (this.currentPage + 1 < this.pages.length) {
        this.currentPage += 1;
      }
    } else if (this.currentPage < this.pages.length - 1) {
      this.currentPage++;
    }

    if (this.currentPage !== previousPage) {
      this.updateProgress();
    }
  }

  previousPage() {
    if (this.isTwoPageView) {
      this.currentPage = Math.max(0, this.currentPage - 2);
    } else {
      this.currentPage = Math.max(0, this.currentPage - 1);
    }
    this.updateProgress();
  }

  private alignCurrentPageToParity() {
    if (!this.pages.length || !this.isTwoPageView) return;

    const desiredOdd = this.pageSpread === CbxPageSpread.ODD;
    for (let i = this.currentPage; i >= 0; i--) {
      if ((this.pages[i] % 2 === 1) === desiredOdd) {
        this.currentPage = i;
        this.updateProgress();
        return;
      }
    }
    for (let i = 0; i < this.pages.length; i++) {
      if ((this.pages[i] % 2 === 1) === desiredOdd) {
        this.currentPage = i;
        this.updateProgress();
        return;
      }
    }
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'ArrowRight') this.nextPage();
    else if (event.key === 'ArrowLeft') this.previousPage();
  }

  @HostListener('touchstart', ['$event'])
  onTouchStart(event: TouchEvent) {
    this.touchStartX = event.changedTouches[0].screenX;
  }

  @HostListener('touchend', ['$event'])
  onTouchEnd(event: TouchEvent) {
    this.touchEndX = event.changedTouches[0].screenX;
    this.handleSwipeGesture();
  }

  @HostListener('window:resize')
  onResize() {
    this.enforcePortraitSinglePageView();
  }

  private handleSwipeGesture() {
    const delta = this.touchEndX - this.touchStartX;
    if (Math.abs(delta) >= 50) delta < 0 ? this.nextPage() : this.previousPage();
  }

  private enforcePortraitSinglePageView() {
    if (this.isPhonePortrait() && this.isTwoPageView) {
      this.pageViewMode = CbxPageViewMode.SINGLE_PAGE;
      this.updateViewerSetting();
    }
  }

  private isPhonePortrait(): boolean {
    return window.innerWidth < 768 && window.innerHeight > window.innerWidth;
  }

  get imageUrls(): string[] {
    if (!this.pages.length) return [];

    const urls = [this.cbxReaderService.getPageImageUrl(this.bookId, this.pages[this.currentPage])];
    if (this.isTwoPageView && this.currentPage + 1 < this.pages.length) {
      urls.push(this.cbxReaderService.getPageImageUrl(this.bookId, this.pages[this.currentPage + 1]));
    }

    return urls;
  }

  private updateViewerSetting(): void {
    const bookSetting: BookSetting = {
      cbxSettings: {
        pageSpread: this.pageSpread,
        pageViewMode: this.pageViewMode,
      }
    }
    this.bookService.updateViewerSetting(bookSetting, this.bookId).subscribe();
  }

  updateProgress(): void {
    const percentage = this.pages.length > 0
      ? Math.round(((this.currentPage + 1) / this.pages.length) * 1000) / 10
      : 0;

    this.bookService.saveCbxProgress(this.bookId, this.currentPage + 1, percentage).subscribe();
  }
  goToPage(page: number): void {
    if (page < 1 || page > this.pages.length) return;

    const targetIndex = page - 1;
    if (targetIndex === this.currentPage) return;

    this.currentPage = targetIndex;
    this.alignCurrentPageToParity();
    this.updateProgress();
  }
}
