import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {BookMetadata} from '../book/model/book.model';
import {API_CONFIG} from '../config/api-config';

export enum BookdropFileStatus {
  PENDING_REVIEW = 'PENDING_REVIEW',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
}

export interface BookdropFinalizePayload {
  uploadPattern: string;
  files: {
    fileId: number;
    libraryId: number;
    pathId: number;
    metadata: BookMetadata;
  }[];
}

export interface BookdropFile {
  showDetails: boolean;
  id: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  originalMetadata: BookMetadata;
  fetchedMetadata?: BookMetadata;
  createdAt: string;
  updatedAt: string;
  status: BookdropFileStatus;
}

export interface BookdropFileResult {
  fileName: string;
  success: boolean;
  message: string;
}

export interface BookdropFinalizeResult {
  results: BookdropFileResult[];
}

@Injectable({
  providedIn: 'root',
})
export class BookdropFileTaskService {
  private readonly url = `${API_CONFIG.BASE_URL}/api/bookdrop`;
  private http = inject(HttpClient);

  getPendingFiles(): Observable<BookdropFile[]> {
    return this.http.get<BookdropFile[]>(`${this.url}/files?status=pending`);
  }

  finalizeImport(payload: BookdropFinalizePayload): Observable<BookdropFinalizeResult> {
    return this.http.post<BookdropFinalizeResult>(`${this.url}/imports/finalize`, payload);
  }

  discardAllFile(): Observable<void> {
    return this.http.delete<void>(`${this.url}/files`);
  }
}
