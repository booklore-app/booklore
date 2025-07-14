import {Component, inject} from '@angular/core';
import {LiveNotificationBoxComponent} from '../live-notification-box/live-notification-box.component';
import {MetadataProgressWidgetComponent} from '../metadata-progress-widget-component/metadata-progress-widget-component';
import {MetadataProgressService} from '../../service/metadata-progress-service';
import {map} from 'rxjs/operators';
import {AsyncPipe} from '@angular/common';

@Component({
  selector: 'app-unified-notification-popover-component',
  imports: [
    LiveNotificationBoxComponent,
    MetadataProgressWidgetComponent,
    AsyncPipe
  ],
  templateUrl: './unified-notification-popover-component.html',
  standalone: true,
  styleUrl: './unified-notification-popover-component.scss'
})
export class UnifiedNotificationBoxComponent {
  metadataProgressService = inject(MetadataProgressService);

  hasMetadataTasks$ = this.metadataProgressService.activeTasks$.pipe(
    map(tasks => Object.keys(tasks).length > 0)
  );
}
