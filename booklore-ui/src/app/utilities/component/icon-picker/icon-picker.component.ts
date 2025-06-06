import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DialogModule} from 'primeng/dialog';

import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-icon-picker',
  templateUrl: './icon-picker.component.html',
  styleUrls: ['./icon-picker.component.scss'],
  imports: [
    DialogModule,
    FormsModule
],
  standalone: true
})
export class IconPickerComponent {
  iconCategories: string[] = [
    "address-book", "align-center", "align-justify", "align-left", "align-right", "android",
    "angle-double-down", "angle-double-left", "angle-double-right", "angle-double-up", "angle-down", "angle-left",
    "angle-right", "angle-up", "apple", "arrow-circle-down", "arrow-circle-left", "arrow-circle-right",
    "arrow-circle-up", "arrow-down-left-and-arrow-up-right-to-center", "arrow-down-left", "arrow-down-right", "arrow-down", "arrow-left",
    "arrow-right-arrow-left", "arrow-right", "arrow-up-left", "arrow-up-right-and-arrow-down-left-from-center", "arrow-up-right", "arrow-up",
    "arrows-alt", "arrows-h", "arrows-v", "asterisks", "at", "backward", "ban", "barcode", "bars", "bell-slash", "bell", "bitcoin",
    "bolt", "book", "bookmark-fill", "bookmark", "box", "briefcase", "building-columns", "building", "bullseye", "calculator", "calendar-clock", "calendar-minus",
    "calendar-plus", "calendar-times", "calendar", "camera", "car", "caret-down", "caret-left", "caret-right", "caret-up", "cart-arrow-down", "cart-minus", "cart-plus",
    "chart-bar", "chart-line", "chart-pie", "chart-scatter", "check-circle", "check-square", "check", "chevron-circle-down", "chevron-circle-left", "chevron-circle-right",
    "chevron-circle-up", "chevron-down", "chevron-left", "chevron-right", "chevron-up", "circle-fill", "circle-off", "circle-on", "circle", "clipboard", "clock", "clone", "cloud-download", "cloud-upload",
    "cloud", "code", "cog", "comment", "comments", "compass", "copy", "credit-card", "crown", "database", "delete-left", "desktop",
    "directions-alt", "directions", "discord", "dollar", "download", "eject", "ellipsis-h", "ellipsis-v", "envelope", "equals", "eraser", "ethereum",
    "euro", "exclamation-circle", "exclamation-triangle", "expand", "external-link", "eye-slash", "eye", "face-smile", "facebook", "fast-backward", "fast-forward", "file-arrow-up",
    "file-check", "file-edit", "file-excel", "file-export", "file-import", "file-o", "file-pdf", "file-plus", "file-word", "file", "filter-fill", "filter-slash",
    "filter", "flag-fill", "flag", "folder-open", "folder-plus", "folder", "forward", "gauge", "gift", "github", "globe", "google",
    "graduation-cap", "hammer", "hashtag", "headphones", "heart-fill", "heart", "history", "home", "hourglass", "id-card", "image", "images",
    "inbox", "indian-rupee", "info-circle", "info", "instagram", "key", "language", "lightbulb", "link", "linkedin", "list-check", "list",
    "lock-open", "lock", "map-marker", "map", "mars", "megaphone", "microchip-ai", "microchip", "microphone", "microsoft", "minus-circle", "minus",
    "mobile", "money-bill", "moon", "objects-column", "palette", "paperclip", "pause-circle", "pause", "paypal", "pen-to-square", "pencil", "percentage",
    "phone", "pinterest", "play-circle", "play", "plus-circle", "plus", "pound", "power-off", "prime", "print", "qrcode", "question-circle",
    "question", "receipt", "reddit", "refresh", "replay", "reply", "save", "search-minus", "search-plus", "search", "send", "server",
    "share-alt", "shield", "shop", "shopping-bag", "shopping-cart", "sign-in", "sign-out", "sitemap", "slack", "sliders-h", "sliders-v", "sort-alpha-alt-down",
    "sort-alpha-alt-up", "sort-alpha-down", "sort-alpha-up", "sort-alt-slash", "sort-alt", "sort-amount-down-alt", "sort-amount-down", "sort-amount-up-alt",
    "sort-amount-up", "sort-down-fill", "sort-down", "sort-numeric-alt-down",
    "sort-numeric-alt-up", "sort-numeric-down", "sort-numeric-up", "sort-up-fill", "sort-up", "sort", "sparkles", "spinner-dotted", "spinner", "star-fill", "star-half-fill", "star-half",
    "star", "step-backward-alt", "step-backward", "step-forward-alt", "step-forward", "stop-circle", "stop", "stopwatch", "sun", "sync", "table", "tablet",
    "tag", "tags", "telegram", "th-large", "thumbs-down-fill", "thumbs-down", "thumbs-up-fill", "thumbs-up", "thumbtack", "ticket", "tiktok", "times-circle",
    "times", "trash", "trophy", "truck", "turkish-lira", "twitch", "twitter", "undo", "unlock", "upload", "user-edit", "user-minus",
    "user-plus", "user", "users", "venus", "verified", "video", "vimeo", "volume-down", "volume-off", "volume-up", "wallet", "warehouse",
    "wave-pulse", "whatsapp", "wifi", "window-maximize", "window-minimize", "wrench", "youtube"
  ];

  icons: string[] = this.createIconList(this.iconCategories);
  iconDialogVisible: boolean = false;
  searchText: string = '';

  selectedIcon: string | null = null;
  @Output() iconSelected = new EventEmitter<string>();

  createIconList(categories: string[]): string[] {
    return categories.map(iconName => `pi pi-${iconName}`);
  }

  filteredIcons(): string[] {
    if (!this.searchText) {
      return this.icons;
    }
    return this.icons.filter(icon => icon.toLowerCase().includes(this.searchText.toLowerCase()));
  }

  open() {
    this.iconDialogVisible = true;
  }

  selectIcon(icon: string) {
    this.selectedIcon = icon;
    this.iconDialogVisible = false;
    this.iconSelected.emit(icon);
  }

}
