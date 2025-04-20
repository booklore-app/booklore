import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {Book, BookMetadata} from '../../book/model/book.model';

@Injectable({
  providedIn: 'root'
})
export class BookMetadataCenterService {

  private bookMetadataSubject = new BehaviorSubject<BookMetadata | null>(null);
  currentMetadata$ = this.bookMetadataSubject.asObservable();

  emit(bookMetadata: BookMetadata) {
    this.bookMetadataSubject.next(bookMetadata);
  }
}
