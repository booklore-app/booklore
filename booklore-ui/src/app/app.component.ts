import {Component, inject, OnInit} from '@angular/core';
import {RxStompService} from './shared/websocket/rx-stomp.service';
import {BookService} from './book/service/book.service';
import {NotificationEventService} from './shared/websocket/notification-event.service';
import {parseLogNotification, parseTaskMessage} from './shared/websocket/model/log-notification.model';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {Toast} from 'primeng/toast';
import {RouterOutlet} from '@angular/router';
import {AuthInitializationService} from './auth-initialization-service';
import {AppConfigService} from './core/service/app-config.service';
import {MetadataBatchProgressNotification} from './core/model/metadata-batch-progress.model';
import {MetadataProgressService} from './core/service/metadata-progress-service';
import {BookdropFileNotification, BookdropFileService} from './bookdrop/bookdrop-file.service';
import {TaskEventService} from './shared/websocket/task-event.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  standalone: true,
  imports: [ConfirmDialog, Toast, RouterOutlet]
})
export class AppComponent implements OnInit {

  loading = true;
  private authInit = inject(AuthInitializationService);
  private bookService = inject(BookService);
  private rxStompService = inject(RxStompService);
  private notificationEventService = inject(NotificationEventService);
  private metadataProgressService = inject(MetadataProgressService);
  private bookdropFileService = inject(BookdropFileService);
  private taskEventService = inject(TaskEventService);
  private appConfigService = inject(AppConfigService);

  ngOnInit(): void {
    this.authInit.initialized$.subscribe(ready => {
      this.loading = !ready;
    });
    this.rxStompService.watch('/user/queue/book-add').subscribe(msg =>
      this.bookService.handleNewlyCreatedBook(JSON.parse(msg.body))
    );
    this.rxStompService.watch('/user/queue/books-remove').subscribe(msg =>
      this.bookService.handleRemovedBookIds(JSON.parse(msg.body))
    );
    this.rxStompService.watch('/user/queue/book-metadata-update').subscribe(msg =>
      this.bookService.handleBookUpdate(JSON.parse(msg.body))
    );
    this.rxStompService.watch('/user/queue/book-metadata-batch-update').subscribe(msg =>
      this.bookService.handleMultipleBookUpdates(JSON.parse(msg.body))
    );
    this.rxStompService.watch('/user/queue/book-metadata-batch-progress').subscribe(msg =>
      this.metadataProgressService.handleIncomingProgress(JSON.parse(msg.body) as MetadataBatchProgressNotification)
    );
    this.rxStompService.watch('/user/queue/log').subscribe(msg => {
      const logNotification = parseLogNotification(msg.body);
      this.notificationEventService.handleNewNotification(logNotification);
    });
    this.rxStompService.watch('/user/queue/task').subscribe(msg =>
      this.taskEventService.handleTaskMessage(parseTaskMessage(msg.body))
    );
    this.rxStompService.watch('/user/queue/bookdrop-file').subscribe(msg => {
      const notification = JSON.parse(msg.body) as BookdropFileNotification;
      this.bookdropFileService.handleIncomingFile(notification);
    });
  }
}
