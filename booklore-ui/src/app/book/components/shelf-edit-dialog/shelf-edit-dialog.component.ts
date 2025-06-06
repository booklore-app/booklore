import {Component, inject, OnInit, ViewChild} from '@angular/core';
import {ShelfService} from '../../service/shelf.service';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {Button} from 'primeng/button';
import {IconPickerComponent} from '../../../utilities/component/icon-picker/icon-picker.component';
import {InputText} from 'primeng/inputtext';

import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Shelf} from '../../model/shelf.model';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-shelf-edit-dialog',
  imports: [
    Button,
    IconPickerComponent,
    InputText,
    ReactiveFormsModule,
    FormsModule
],
  templateUrl: './shelf-edit-dialog.component.html',
  standalone: true,
  styleUrl: './shelf-edit-dialog.component.scss'
})
export class ShelfEditDialogComponent implements OnInit{

  private shelfService = inject(ShelfService);
  private dynamicDialogConfig = inject(DynamicDialogConfig);
  private dynamicDialogRef = inject(DynamicDialogRef);
  private messageService = inject(MessageService);

  shelfName: string = '';
  selectedIcon: string | null = null;
  shelf!: Shelf | undefined;

  @ViewChild(IconPickerComponent) iconPicker: IconPickerComponent | undefined;

  ngOnInit(): void {
    const shelfId = this.dynamicDialogConfig?.data.shelfId;
    this.shelf = this.shelfService.getShelfById(shelfId);
    if(this.shelf) {
      this.shelfName = this.shelf.name;
      this.selectedIcon = 'pi pi-' + this.shelf.icon;
    }
  }

  openIconPicker() {
    if (this.iconPicker) {
      this.iconPicker.open();
    }
  }

  clearSelectedIcon() {
    this.selectedIcon = null;
  }

  onIconSelected(icon: string) {
    this.selectedIcon = icon;
  }

  save() {
    const shelf: Shelf = {
      name: this.shelfName,
      icon: this.selectedIcon?.replace('pi pi-', '') || 'heart'
    };

    this.shelfService.updateShelf(shelf, this.shelf?.id).subscribe({
      next: () => {
        this.messageService.add({severity: 'success', summary: 'Shelf Updated', detail: 'The shelf was updated successfully.'});
        this.dynamicDialogRef.close();
      },
      error: (e) => {
        this.messageService.add({severity: 'error', summary: 'Update Failed', detail: 'An error occurred while updating the shelf. Please try again.'});
        console.error(e);
      }
    });
  }
}
