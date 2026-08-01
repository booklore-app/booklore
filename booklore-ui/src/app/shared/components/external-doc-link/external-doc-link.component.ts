import {Component, Input} from '@angular/core';
import {Tooltip} from 'primeng/tooltip';

export type DocType = 'kobo' | 'opds' | 'metadataManager' | 'koReader' | 'email'
  | 'amazonCookie' | 'fetchConfig' | 'hardcover' | 'taskManagement' | 'fileNamePatterns'
  | 'authentication';

@Component({
  selector: 'app-external-doc-link',
  standalone: true,
  imports: [Tooltip],
  template: `
    <i class="pi pi-external-link external-link-icon"
       [pTooltip]="tooltip"
       [tooltipPosition]="tooltipPosition"
       [style.font-size]="size"
       (click)="openLink()"
       style="cursor: pointer;">
    </i>
  `,
  styles: [`
    .external-link-icon {
      color: #0ea5e9 !important;
    }
  `]
})
export class ExternalDocLinkComponent {
  // Local archived copy (public/docs/) - the original booklore.org/docs site went offline.
  private readonly BASE_URL = '/docs';

  private readonly DOC_URLS: Record<DocType, string> = {
    kobo: `${this.BASE_URL}/integration/kobo.html`,
    opds: `${this.BASE_URL}/integration/opds.html`,
    metadataManager: `${this.BASE_URL}/metadata/metadata-manager.html`,
    koReader: `${this.BASE_URL}/integration/koreader.html`,
    email: `${this.BASE_URL}/email-setup.html`,
    amazonCookie: `${this.BASE_URL}/metadata/amazon-cookie.html`,
    hardcover: `${this.BASE_URL}/metadata/hardcover-token.html`,
    fetchConfig: `${this.BASE_URL}/metadata/metadata-fetch-configuration.html`,
    taskManagement: `${this.BASE_URL}/tools/task-manager.html`,
    fileNamePatterns: `${this.BASE_URL}/metadata/file-naming-patterns.html`,
    authentication: `${this.BASE_URL}/authentication/overview.html#setting-up-oidc`
  };

  @Input() docType!: DocType;
  @Input() tooltip: string = 'View documentation';
  @Input() tooltipPosition: 'top' | 'bottom' | 'left' | 'right' = 'right';
  @Input() size: string = '1rem';

  openLink(): void {
    const url = this.DOC_URLS[this.docType];
    if (url) {
      window.open(url, '_blank');
    }
  }
}
