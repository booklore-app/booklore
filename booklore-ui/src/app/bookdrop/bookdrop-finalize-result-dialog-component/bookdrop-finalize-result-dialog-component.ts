import {Component, OnDestroy} from '@angular/core';
import {NgClass} from '@angular/common';
import {BookdropFileResult} from '../bookdrop-file-task.service';
import {DynamicDialogConfig, DynamicDialogRef} from "primeng/dynamicdialog";

@Component({
  selector: 'app-bookdrop-finalize-result-dialog-component',
  imports: [
    NgClass
  ],
  templateUrl: './bookdrop-finalize-result-dialog-component.html',
  styleUrl: './bookdrop-finalize-result-dialog-component.scss'
})
export class BookdropFinalizeResultDialogComponent implements OnDestroy {

  results: BookdropFileResult[] = [];

  constructor(public ref: DynamicDialogRef, public config: DynamicDialogConfig) {
    this.results = config.data.results;
  }

  ngOnDestroy(): void {
    this.ref?.close();
  }
}
