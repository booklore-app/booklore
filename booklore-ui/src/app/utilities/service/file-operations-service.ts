import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_CONFIG} from '../../config/api-config';

export interface FileMoveRequest {
  bookIds: number[];
  pattern: string;
}

@Injectable({
  providedIn: 'root'
})
export class FileOperationsService {
  private readonly baseUrl = `${API_CONFIG.BASE_URL}/api/v1/files`;

  private http = inject(HttpClient);

  moveFiles(request: FileMoveRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/move`, request);
  }
}
