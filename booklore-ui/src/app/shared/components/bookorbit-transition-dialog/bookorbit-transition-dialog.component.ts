import {Component, inject} from '@angular/core';
import {TranslocoDirective} from '@jsverse/transloco';
import {DynamicDialogRef} from 'primeng/dynamicdialog';

@Component({
  selector: 'app-bookorbit-transition-dialog',
  standalone: true,
  imports: [TranslocoDirective],
  templateUrl: './bookorbit-transition-dialog.component.html',
  styleUrl: './bookorbit-transition-dialog.component.scss'
})
export class BookOrbitTransitionDialogComponent {
  private readonly dialogRef = inject(DynamicDialogRef);

  protected close(): void {
    this.dialogRef.close();
  }
}
