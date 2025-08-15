import {Component, inject, OnInit} from '@angular/core';
import {Button} from 'primeng/button';

import {MessageService, PrimeTemplate} from 'primeng/api';
import {RadioButton} from 'primeng/radiobutton';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TableModule} from 'primeng/table';
import {Tooltip} from 'primeng/tooltip';
import {EmailProvider} from '../email-provider/email-provider.model';
import {EmailRecipient} from './email-recipient.model';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {EmailProviderService} from '../email-provider/email-provider.service';
import {EmailRecipientService} from './email-recipient.service';
import {CreateEmailProviderDialogComponent} from '../create-email-provider-dialog/create-email-provider-dialog.component';
import {CreateEmailRecipientDialogComponent} from '../create-email-recipient-dialog/create-email-recipient-dialog.component';

@Component({
  selector: 'app-email-recipient',
  imports: [
    Button,
    PrimeTemplate,
    RadioButton,
    ReactiveFormsModule,
    TableModule,
    Tooltip,
    FormsModule
  ],
  templateUrl: './email-recipient.component.html',
  styleUrl: './email-recipient.component.scss'
})
export class EmailRecipientComponent implements OnInit {
  recipientEmails: EmailRecipient[] = [];
  editingRecipientIds: number[] = [];
  ref: DynamicDialogRef | undefined;
  private dialogService = inject(DialogService);
  private emailRecipientService = inject(EmailRecipientService);
  private messageService = inject(MessageService);
  defaultRecipientId: any;

  ngOnInit(): void {
    this.loadRecipientEmails();
  }

  loadRecipientEmails(): void {
    this.emailRecipientService.getRecipients().subscribe({
      next: (recipients: EmailRecipient[]) => {
        this.recipientEmails = recipients.map((recipient) => ({
          ...recipient,
          isEditing: false,
        }));
        const defaultRecipient = recipients.find((recipient) => recipient.defaultRecipient);
        this.defaultRecipientId = defaultRecipient ? defaultRecipient.id : null;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load recipient emails',
        });
      },
    });
  }

  toggleEditRecipient(recipient: EmailRecipient): void {
    recipient.isEditing = !recipient.isEditing;
    if (recipient.isEditing) {
      this.editingRecipientIds.push(recipient.id);
    } else {
      this.editingRecipientIds = this.editingRecipientIds.filter((id) => id !== recipient.id);
    }
  }

  saveRecipient(recipient: EmailRecipient): void {
    this.emailRecipientService.updateRecipient(recipient).subscribe({
      next: () => {
        recipient.isEditing = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Recipient updated successfully',
        });
        this.loadRecipientEmails();
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to update recipient',
        });
      },
    });
  }

  deleteRecipient(recipient: EmailRecipient): void {
    if (confirm(`Are you sure you want to delete recipient "${recipient.email}"?`)) {
      this.emailRecipientService.deleteRecipient(recipient.id).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: `Recipient "${recipient.email}" deleted successfully`,
          });
          this.loadRecipientEmails();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to delete recipient',
          });
        },
      });
    }
  }

  openAddRecipientDialog() {
    this.ref = this.dialogService.open(CreateEmailRecipientDialogComponent, {
      header: 'Add New Recipient',
      modal: true,
      closable: true,
      style: {position: 'absolute', top: '15%'},
    });
    this.ref.onClose.subscribe((result) => {
      if (result) {
        this.loadRecipientEmails();
      }
    });
  }

  setDefaultRecipient(recipient: EmailRecipient) {
    this.emailRecipientService.setDefaultRecipient(recipient.id).subscribe(() => {
      this.defaultRecipientId = recipient.id;
      this.messageService.add({
        severity: 'success',
        summary: 'Default Recipient Set',
        detail: `${recipient.email} is now the default recipient.`
      });
    });
  }
}
