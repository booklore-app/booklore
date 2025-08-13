import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Series} from '../model/series.model';
import {API_CONFIG} from '../../config/api-config';
@Injectable({
  providedIn: 'root',
})
export class SeriesService {
  private http = inject(HttpClient);
  private readonly url = `${API_CONFIG.BASE_URL}/api/v1/series`;

  getSeries(): Observable<Series[]> {
    return this.http.get<Series[]>(this.url);
  }
}