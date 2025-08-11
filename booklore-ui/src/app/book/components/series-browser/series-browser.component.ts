import { Component, inject } from "@angular/core";
import { AsyncPipe, NgForOf } from "@angular/common";
import { SeriesService } from "../../service/series.service";

@Component({
  selector: "app-series-browser",
  standalone: true,
  templateUrl: "./series-browser.component.html",
  styleUrls: ["./series-browser.component.scss"],
  imports: [NgForOf, AsyncPipe],
})
export class SeriesBrowserComponent {
  private seriesService = inject(SeriesService);
  series$ = this.seriesService.getSeries();
}
