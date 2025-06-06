import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';

import {Observable, Subscription} from 'rxjs';
import {Select} from 'primeng/select';
import {RadioButton} from 'primeng/radiobutton';
import {Divider} from 'primeng/divider';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {User, UserService, UserSettings} from '../user-management/user.service';
import {MessageService} from 'primeng/api';
import {CbxPageSpread, CbxPageViewMode} from '../../book/model/book.model';

@Component({
  selector: 'app-reader-preferences',
  templateUrl: './reader-preferences.component.html',
  standalone: true,
  styleUrls: ['./reader-preferences.component.scss'],
  imports: [Select, FormsModule, RadioButton, Divider, Button, Tooltip]
})
export class ReaderPreferences implements OnInit, OnDestroy {

  readonly cbxSpreads = [
    {name: 'Even', key: 'EVEN'},
    {name: 'Odd', key: 'ODD'}
  ];

  readonly cbxViewModes = [
    {name: 'Single Page', key: 'SINGLE_PAGE'},
    {name: 'Two Page', key: 'TWO_PAGE'},
  ];


  readonly spreads = [
    {name: 'Even', key: 'even'},
    {name: 'Odd', key: 'odd'},
    {name: 'None', key: 'off'}
  ];

  readonly zooms = [
    {name: 'Auto Zoom', key: 'auto'},
    {name: 'Page Fit', key: 'page-fit'},
    {name: 'Page Width', key: 'page-width'},
    {name: 'Actual Size', key: 'page-actual'}
  ];

  readonly themes = [
    {name: 'White', key: 'white'},
    {name: 'Black', key: 'black'},
    {name: 'Grey', key: 'grey'},
    {name: 'Sepia', key: 'sepia'}
  ];

  readonly fonts = [
    {name: 'Serif', key: 'serif'},
    {name: 'Sans Serif', key: 'sans-serif'},
    {name: 'Roboto', key: 'roboto'},
    {name: 'Cursive', key: 'cursive'},
    {name: 'Monospace', key: 'monospace'}
  ];

  readonly flowOptions = [
    {name: 'Paginated', key: 'paginated'},
    {name: 'Scrolled', key: 'scrolled'}
  ];

  readonly scopeOptions = ['Global', 'Individual'];

  selectedCbxSpread!: CbxPageSpread;
  selectedCbxViewMode!: CbxPageViewMode;

  selectedSpread!: 'even' | 'odd' | 'off';
  selectedZoom!: string;
  selectedTheme!: string;
  selectedFont!: string;
  selectedFlow!: string;
  fontSize = 100;
  showSidebar = false;

  selectedPdfScope!: string;
  selectedEpubScope!: string;
  selectedCbxScope!: string;

  private readonly userService = inject(UserService);
  private readonly messageService = inject(MessageService);

  userData$: Observable<User | null> = this.userService.userState$;
  private subscription?: Subscription;
  private currentUser: User | null = null;

  ngOnInit(): void {
    this.subscription = this.userData$.subscribe(user => {
      if (user) {
        this.currentUser = user;
        this.loadPreferences(user.userSettings);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  private loadPreferences(settings: UserSettings): void {
    this.selectedPdfScope = settings.perBookSetting.pdf;
    this.selectedEpubScope = settings.perBookSetting.epub;
    this.selectedCbxScope = settings.perBookSetting.cbx;

    this.selectedSpread = settings.pdfReaderSetting.pageSpread;
    this.selectedZoom = settings.pdfReaderSetting.pageZoom;
    this.showSidebar = settings.pdfReaderSetting.showSidebar;

    this.selectedTheme = settings.epubReaderSetting.theme;
    this.selectedFlow = settings.epubReaderSetting.flow;
    this.fontSize = settings.epubReaderSetting.fontSize;
    this.selectedFont = settings.epubReaderSetting.font;

    this.selectedCbxSpread = settings.cbxReaderSetting.pageSpread;
    this.selectedCbxViewMode = settings.cbxReaderSetting.pageViewMode;
  }

  private updatePreference(path: string[], value: any): void {
    if (!this.currentUser) return;
    let target: any = this.currentUser.userSettings;
    for (let i = 0; i < path.length - 1; i++) {
      target = target[path[i]] ||= {};
    }
    target[path.at(-1)!] = value;

    const [rootKey] = path;
    const updatedValue = this.currentUser.userSettings[rootKey as keyof UserSettings];
    this.userService.updateUserSetting(this.currentUser.id, rootKey, updatedValue);
    this.messageService.add({
      severity: 'success',
      summary: 'Preferences Updated',
      detail: 'Your preferences have been saved successfully.',
      life: 2000
    });
  }

  onThemeChange() {
    this.updatePreference(['epubReaderSetting', 'theme'], this.selectedTheme);
  }

  onFontChange() {
    this.updatePreference(['epubReaderSetting', 'font'], this.selectedFont);
  }

  onFlowChange() {
    this.updatePreference(['epubReaderSetting', 'flow'], this.selectedFlow);
  }

  onFontSizeChange() {
    this.updatePreference(['epubReaderSetting', 'fontSize'], this.fontSize);
  }

  onSpreadChange() {
    this.updatePreference(['pdfReaderSetting', 'pageSpread'], this.selectedSpread);
  }

  onCbxSpreadChange() {
    this.updatePreference(['cbxReaderSetting', 'pageSpread'], this.selectedCbxSpread);
  }

  onCbxViewModeChange() {
    this.updatePreference(['cbxReaderSetting', 'pageViewMode'], this.selectedCbxViewMode);
  }

  onZoomChange() {
    this.updatePreference(['pdfReaderSetting', 'pageZoom'], this.selectedZoom);
  }

  onPdfScopeChange() {
    this.updatePreference(['perBookSetting', 'pdf'], this.selectedPdfScope);
  }

  onEpubScopeChange() {
    this.updatePreference(['perBookSetting', 'epub'], this.selectedEpubScope);
  }

  onCbxScopeChange() {
    this.updatePreference(['perBookSetting', 'cbx'], this.selectedCbxScope);
  }

  increaseFontSize() {
    if (this.fontSize < 250) {
      this.fontSize += 10;
      this.onFontSizeChange();
    }
  }

  decreaseFontSize() {
    if (this.fontSize > 50) {
      this.fontSize -= 10;
      this.onFontSizeChange();
    }
  }
}
