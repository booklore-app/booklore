import { Injectable } from '@angular/core';
import {API_CONFIG} from '../../config/api-config';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class NewPdfReaderService {

  private readonly url = `${API_CONFIG.BASE_URL}/api/v1/pdf`;

  constructor(private http: HttpClient) {}

  getAvailablePages(bookId: number) {
    return this.http.get<number[]>(`${this.url}/${bookId}/pages`);
  }

  getPageImageUrl(bookId: number, page: number): string {
    return `${this.url}/${bookId}/pages/${page}`;
  }
}
