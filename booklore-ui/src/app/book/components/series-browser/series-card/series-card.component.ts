import {Component, Input, inject} from '@angular/core';
import {Series} from '../../../model/series.model';
import {UrlHelperService} from '../../../../utilities/service/url-helper.service';
import {NgClass} from '@angular/common';
import {Button} from 'primeng/button';
import {Router} from '@angular/router';

@Component({
  selector: 'app-series-card',
  standalone: true,
  templateUrl: './series-card.component.html',
  styleUrls: ['./series-card.component.scss'],
  imports: [NgClass, Button]
})
export class SeriesCardComponent {
  @Input() series!: Series;

  isImageLoaded: boolean = false;
  protected urlHelper = inject(UrlHelperService);
  private router = inject(Router);

  onImageLoad(): void {
    this.isImageLoaded = true;
  }

  goToFirstBook(): void {
    this.router.navigate(['/book', this.series.firstBookId], { queryParams: { tab: 'view' } });
  }

  openSeriesInfo(event: MouseEvent): void {
    event.stopPropagation();
    this.goToFirstBook();
  }
}

