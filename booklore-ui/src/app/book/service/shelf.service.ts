import {inject, Injectable} from '@angular/core';
import { BehaviorSubject, Observable, of, tap } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Shelf } from '../model/shelf.model';
import { SortOption } from '../model/sort.model';
import { BookService } from './book.service';
import { ShelfState } from '../model/state/shelf-state.model';
import {API_CONFIG} from '../../config/api-config';
import {Library} from '../model/library.model';

@Injectable({
  providedIn: 'root',
})
export class ShelfService {
  private readonly url = `${API_CONFIG.BASE_URL}/api/v1/shelves`;
  private shelfStateSubject = new BehaviorSubject<ShelfState>({
    shelves: null,
    loaded: false,
    error: null,
  });
  shelfState$ = this.shelfStateSubject.asObservable();

  private http = inject(HttpClient);
  private bookService = inject(BookService);

  constructor() {
    this.loadShelves();
  }

  private loadShelves(): void {
    this.http.get<Shelf[]>(this.url).pipe(
      catchError(error => {
        this.shelfStateSubject.next({
          shelves: null,
          loaded: true,
          error: error.message,
        });
        return of([]);
      })
    ).subscribe(shelves => {
      this.shelfStateSubject.next({
        shelves,
        loaded: true,
        error: null,
      });
    });
  }

  getShelfById(shelfId: number): Shelf | undefined {
    let shelfState = this.shelfStateSubject.value;
    return shelfState.shelves?.find(shelf => shelf.id === shelfId);
  }

  createShelf(shelf: Shelf): Observable<Shelf> {
    return this.http.post<Shelf>(this.url, shelf).pipe(
      map(newShelf => {
        const currentState = this.shelfStateSubject.value;
        const updatedShelves = currentState.shelves ? [...currentState.shelves, newShelf] : [newShelf];
        this.shelfStateSubject.next({ ...currentState, shelves: updatedShelves });
        return newShelf;
      }),
      catchError(error => {
        const currentState = this.shelfStateSubject.value;
        this.shelfStateSubject.next({ ...currentState, error: error.message });
        throw error;
      })
    );
  }

  updateShelf(shelf: Shelf, shelfId: number | undefined): Observable<Shelf> {
    return this.http.put<Shelf>(`${this.url}/${shelfId}`, shelf).pipe(
      map(updatedShelf => {
        const currentState = this.shelfStateSubject.value;
        const updatedShelves = currentState.shelves ? currentState.shelves.map(existingShelf =>
          existingShelf.id === updatedShelf.id ? updatedShelf : existingShelf) : [updatedShelf];
        this.shelfStateSubject.next({...currentState, shelves: updatedShelves,});
        return updatedShelf;
      }),
      catchError(error => {
        throw error;
      })
    );
  }

  deleteShelf(shelfId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${shelfId}`).pipe(
      tap(() => {
        this.bookService.removeBooksFromShelf(shelfId);
        const currentState = this.shelfStateSubject.value;
        const updatedShelves = currentState.shelves?.filter(shelf => shelf.id !== shelfId) || [];
        this.shelfStateSubject.next({ ...currentState, shelves: updatedShelves });
      }),
      catchError(error => {
        const currentState = this.shelfStateSubject.value;
        this.shelfStateSubject.next({ ...currentState, error: error.message });
        return of();
      })
    );
  }

  getBookCount(shelfId: number): Observable<number> {
    return this.bookService.bookState$.pipe(
      map(state =>
        (state.books || []).filter(book => book.shelves?.some(shelf => shelf.id === shelfId)).length
      )
    );
  }

  getUnshelvedBookCount(): Observable<number> {
    return this.bookService.bookState$.pipe(
      map(state =>
        (state.books || []).filter(book => !book.shelves || book.shelves.length === 0).length
      )
    );
  }
}
