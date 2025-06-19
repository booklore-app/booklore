import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {Book, BookMetadata} from '../../model/book.model';

@Injectable({
  providedIn: 'root'
})
export class BookMetadataCenterService {

  private bookMetadataSubject = new BehaviorSubject<BookMetadata | null>(null);
  currentMetadata$ = this.bookMetadataSubject.asObservable();

  emitMetadata(bookMetadata: BookMetadata) {
    this.bookMetadataSubject.next(bookMetadata);
  }

  bookChangedSubject = new BehaviorSubject<Book | null>(null);
  bookChanged$ = this.bookChangedSubject.asObservable();

  emitBookChanged(book: Book | null) {
    this.bookChangedSubject.next(book);
  }
}
