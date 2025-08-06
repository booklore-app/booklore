import { Injectable } from '@angular/core';
import {API_CONFIG} from '../../config/api-config';

@Injectable({
  providedIn: 'root'
})
export class UrlHelperService {
  private readonly baseUrl = API_CONFIG.BASE_URL;

  getCoverUrl(bookId: number, coverUpdatedOn?: string): string {
    if (!coverUpdatedOn) {
      return 'assets/images/missing-cover.jpg';
    }
    return `${this.baseUrl}/api/v1/books/${bookId}/cover?${coverUpdatedOn}`;
  }

  getBackupCoverUrl(bookId: number): string {
    return `${this.baseUrl}/api/v1/books/${bookId}/backup-cover`;
  }

  getBookdropCoverUrl(bookdropId: number): string {
    return `${this.baseUrl}/api/bookdrop/${bookdropId}/cover`;
  }
}
