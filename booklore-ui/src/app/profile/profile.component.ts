import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {NgIf} from '@angular/common';
import {User, UserService} from '../user.service';
import {MessageService} from 'primeng/api';
import {Password} from 'primeng/password';
import {InputTextModule} from 'primeng/inputtext';
import {Subscription} from 'rxjs';
import {ToastModule} from 'primeng/toast';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    FormsModule,
    Button,
    NgIf,
    Password,
    InputTextModule,
    ToastModule,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit, OnDestroy {
  protected userService = inject(UserService);
  private messageService = inject(MessageService);

  currentPassword: string = '';
  newPassword: string = '';
  confirmNewPassword: string = '';

  isEditing = false;
  currentUser: User | null = null;
  editUserData: Partial<User> = {};
  private userSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.userSubscription = this.userService.userData$.subscribe(user => {
      this.currentUser = user;
      this.resetEditForm();
    });
  }

  ngOnDestroy(): void {
    this.userSubscription?.unsubscribe();
  }

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
    if (this.isEditing) {
      this.resetEditForm();
    }
  }

  resetEditForm(): void {
    if (this.currentUser) {
      this.editUserData = {
        username: this.currentUser.username,
        name: this.currentUser.name,
        email: this.currentUser.email,
      };
    }
  }

  saveChanges(): void {
    if (!this.currentUser) {
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'User data not available.'});
      return;
    }

    if (this.editUserData.name === this.currentUser.name && this.editUserData.email === this.currentUser.email) {
      this.messageService.add({severity: 'info', summary: 'Info', detail: 'No changes detected.'});
      this.isEditing = false;
      return;
    }

    this.userService.updateUser(this.currentUser.id, this.editUserData).subscribe({
      next: () => {
        this.messageService.add({severity: 'success', summary: 'Success', detail: 'Profile updated successfully'});
        this.isEditing = false;
        this.resetEditForm();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: err.error?.message || 'Failed to update profile'
        });
      }
    });
  }

  resetPasswordForm(): void {
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmNewPassword = '';
  }

  submitPasswordChange() {
    if (!this.currentUser) {
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'User data not available.'});
      return;
    }

    if (!this.currentPassword || !this.newPassword || !this.confirmNewPassword) {
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'All fields are required'});
      return;
    }

    if (this.newPassword !== this.confirmNewPassword) {
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'Passwords do not match'});
      this.newPassword = '';
      this.confirmNewPassword = '';
      return;
    }

    this.userService.changePassword(this.currentPassword, this.newPassword).subscribe({
        next: () => {
          this.messageService.add({severity: 'success', summary: 'Success', detail: 'Password changed successfully'});
          this.resetPasswordForm();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: err?.message || 'Failed to change password'
          });
        }
      });
  }
}
