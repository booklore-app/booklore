import {inject, Injectable} from '@angular/core';
import {API_CONFIG} from './config/api-config';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {catchError, map, switchMap, tap} from 'rxjs/operators';
import {BookService} from './book/service/book.service';
import {GroupRule} from './magic-shelf-component/magic-shelf-component';
import {BookRuleEvaluatorService} from './book-rule-evaluator.service';

export interface MagicShelf {
  id?: number | null;
  name: string;
  icon?: string;
  filterJson: string;
}

export interface MagicShelfState {
  shelves: MagicShelf[];
  loaded: boolean;
  error: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class MagicShelfService {
  private readonly url = `${API_CONFIG.BASE_URL}/api/magic-shelves`;

  private readonly http = inject(HttpClient);
  private readonly bookService = inject(BookService);
  private readonly ruleEvaluatorService = inject(BookRuleEvaluatorService);

  private readonly shelvesStateSubject = new BehaviorSubject<MagicShelfState>({
    shelves: [],
    loaded: false,
    error: null,
  });
  public readonly shelvesState$ = this.shelvesStateSubject.asObservable();

  constructor() {
    this.loadUserShelves().subscribe();
  }

  private get state(): MagicShelfState {
    return this.shelvesStateSubject.value;
  }

  loadUserShelves(): Observable<MagicShelf[]> {
    return this.http.get<MagicShelf[]>(this.url).pipe(
      tap((shelves) => this.updateShelves(shelves)),
      catchError((error) => {
        this.updateError(error.message);
        return of([]);
      })
    );
  }

  saveShelf(data: { id?: number; name: string | null; icon: string | null; group: any }): Observable<MagicShelf> {
    const payload: MagicShelf = {
      id: data.id,
      name: data.name ?? '',
      icon: data.icon ?? 'pi pi-book',
      filterJson: JSON.stringify(data.group),
    };

    return this.http.post<MagicShelf>(this.url, payload).pipe(
      tap((newShelf) => {
        const shelves = this.state.shelves;
        const updated = shelves.some((s) => s.id === newShelf.id)
          ? shelves.map((s) => (s.id === newShelf.id ? newShelf : s))
          : [...shelves, newShelf];

        this.updateShelves(updated);
      }),
      catchError((error) => {
        this.updateError(error.message);
        throw error;
      })
    );
  }

  getShelf(id: number): Observable<MagicShelf | undefined> {
    return this.shelvesState$.pipe(
      map(state => state.shelves.find(shelf => shelf.id === id))
    );
  }

  deleteShelf(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`).pipe(
      tap(() => {
        const updated = this.state.shelves.filter((s) => s.id !== id);
        this.updateShelves(updated);
      }),
      catchError((error) => {
        this.updateError(error.message);
        throw error;
      })
    );
  }

  private updateShelves(shelves: MagicShelf[]): void {
    this.shelvesStateSubject.next({
      shelves,
      loaded: true,
      error: null,
    });
  }

  private updateError(error: string): void {
    this.shelvesStateSubject.next({
      ...this.state,
      error,
      loaded: true,
    });
  }

  getBookCount(shelfId: number): Observable<number> {
    return this.getShelf(shelfId).pipe(
      switchMap((shelf) => {
        if (!shelf) return of(0);
        let group: GroupRule;
        try {
          group = JSON.parse(shelf.filterJson);
        } catch (e) {
          console.error('Invalid filter JSON', e);
          return of(0);
        }

        return this.bookService.bookState$.pipe(
          map((state) =>
            (state.books ?? []).filter((book) =>
              this.ruleEvaluatorService.evaluateGroup(book, group)
            ).length
          )
        );
      })
    );
  }
}
