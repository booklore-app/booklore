import {inject, Injectable, OnDestroy} from '@angular/core';
import {BehaviorSubject, Subscription} from 'rxjs';
import {map, filter, take} from 'rxjs/operators';
import {BookdropFileApiService} from './bookdrop-file-api.service';
import {AuthService} from '../core/service/auth.service';

export interface BookdropFileNotification {
  pendingCount: number;
  totalCount: number;
  lastUpdatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BookdropFileService implements OnDestroy {
  private summarySubject = new BehaviorSubject<BookdropFileNotification>({
    pendingCount: 0,
    totalCount: 0
  });

  summary$ = this.summarySubject.asObservable();

  hasPendingFiles$ = this.summary$.pipe(
    map(summary => summary.pendingCount > 0)
  );

  private apiService = inject(BookdropFileApiService);
  private authService = inject(AuthService);
  private subscriptions = new Subscription();

  constructor() {
    this.authService.token$
      .pipe(filter(t => !!t), take(1))
      .subscribe(() => this.refresh());
  }

  handleIncomingFile(summary: BookdropFileNotification): void {
    this.summarySubject.next(summary);
  }

  refresh(): void {
    const sub = this.apiService.getNotification().subscribe({
      next: summary => this.summarySubject.next(summary),
      error: err => console.warn('Failed to refresh bookdrop file summary:', err)
    });
    this.subscriptions.add(sub);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.summarySubject.complete();
  }
}
