import {Component, Input, inject} from '@angular/core';
import {Series} from '../../../model/series.model';
import {UrlHelperService} from '../../../../utilities/service/url-helper.service';
import {NgClass} from '@angular/common';

@Component({
  selector: 'app-series-card',
  standalone: true,
  templateUrl: './series-card.component.html',
  styleUrls: ['./series-card.component.scss'],
  imports: [NgClass]
})
export class SeriesCardComponent {
  @Input() series!: Series;

  isImageLoaded: boolean = false;
  protected urlHelper = inject(UrlHelperService);

  onImageLoad(): void {
    this.isImageLoaded = true;
  }
}

