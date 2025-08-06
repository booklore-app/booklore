import {Component, inject, OnInit} from '@angular/core';
import {AppMenuitemComponent} from './app.menuitem.component';
import {AsyncPipe} from '@angular/common';
import {MenuModule} from 'primeng/menu';
import {LibraryService} from '../../../book/service/library.service';
import {Observable, of} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {ShelfService} from '../../../book/service/shelf.service';
import {BookService} from '../../../book/service/book.service';
import {LibraryShelfMenuService} from '../../../book/service/library-shelf-menu.service';
import {AppVersion, VersionService} from '../../../core/service/version.service';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {VersionChangelogDialogComponent} from './version-changelog-dialog/version-changelog-dialog.component';
import {UserService} from '../../../settings/user-management/user.service';
import {MagicShelf, MagicShelfService, MagicShelfState} from '../../../magic-shelf-service';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [AppMenuitemComponent, MenuModule, AsyncPipe],
  templateUrl: './app.menu.component.html',
})
export class AppMenuComponent implements OnInit {
  libraryMenu$: Observable<any> | undefined;
  shelfMenu$: Observable<any> | undefined;
  homeMenu$: Observable<any> | undefined;
  magicShelfMenu$: Observable<any> | undefined;

  versionInfo: AppVersion | null = null;
  dynamicDialogRef: DynamicDialogRef | undefined;

  private libraryService = inject(LibraryService);
  private shelfService = inject(ShelfService);
  private bookService = inject(BookService);
  private versionService = inject(VersionService);
  private libraryShelfMenuService = inject(LibraryShelfMenuService);
  private dialogService = inject(DialogService);
  private userService = inject(UserService);
  private magicShelfService = inject(MagicShelfService);

  librarySortField: 'name' | 'id' = 'name';
  librarySortOrder: 'asc' | 'desc' = 'desc';
  shelfSortField: 'name' | 'id' = 'name';
  shelfSortOrder: 'asc' | 'desc' = 'asc';


  ngOnInit(): void {
    this.versionService.getVersion().subscribe((data) => {
      this.versionInfo = data;
    });

    this.userService.userState$.pipe(
      filter(settings => !!settings))
      .subscribe(user => {
        if (user?.userSettings.sidebarLibrarySorting) {
          this.librarySortField = this.validateSortField(user.userSettings.sidebarLibrarySorting.field);
          this.librarySortOrder = this.validateSortOrder(user.userSettings.sidebarLibrarySorting.order);
        }
        if (user?.userSettings.sidebarShelfSorting) {
          this.shelfSortField = this.validateSortField(user.userSettings.sidebarShelfSorting.field);
          this.shelfSortOrder = this.validateSortOrder(user.userSettings.sidebarShelfSorting.order);
        }
        this.initMenus();
      });

    this.homeMenu$ = this.bookService.bookState$.pipe(
      map((bookState) => [
        {
          label: 'Home',
          items: [
            {
              label: 'Dashboard',
              icon: 'pi pi-fw pi-home',
              routerLink: ['/dashboard'],
            },
            {
              label: 'All Books',
              type: 'All Books',
              icon: 'pi pi-fw pi-book',
              routerLink: ['/all-books'],
              bookCount$: of(bookState.books ? bookState.books.length : 0),
            },
          ],
        },
      ])
    );
  }

  private initMenus(): void {
    this.libraryMenu$ = this.libraryService.libraryState$.pipe(
      map((state) => {
        const libraries = state.libraries ?? [];
        const sortedLibraries = this.sortArray(libraries, this.librarySortField, this.librarySortOrder);
        return [
          {
            label: 'Libraries',
            type: 'library',
            hasDropDown: true,
            hasCreate: true,
            items: sortedLibraries.map((library) => ({
              menu: this.libraryShelfMenuService.initializeLibraryMenuItems(library),
              label: library.name,
              type: 'Library',
              icon: 'pi pi-' + library.icon,
              routerLink: [`/library/${library.id}/books`],
              bookCount$: this.libraryService.getBookCount(library.id ?? 0),
            })),
          },
        ];
      })
    );

    this.magicShelfMenu$ = this.magicShelfService.shelvesState$.pipe(
      map((state: MagicShelfState) => {
        const shelves = state.shelves ?? [];
        const sortedShelves = this.sortArray(shelves, 'name', 'asc');
        return [
          {
            label: 'Magic Shelves',
            type: 'magicShelf',
            hasDropDown: true,
            hasCreate: true,
            items: sortedShelves.map((shelf) => ({
              label: shelf.name,
              type: 'magicShelfItem',
              icon: 'pi pi-' + shelf.icon,
              menu: this.libraryShelfMenuService.initializeMagicShelfMenuItems(shelf),
              routerLink: [`/magic-shelf/${shelf.id}/books`],
              bookCount$: this.magicShelfService.getBookCount(shelf.id ?? 0),
            })),
          },
        ];
      })
    );

    this.shelfMenu$ = this.shelfService.shelfState$.pipe(
      map((state) => {
        const shelves = state.shelves ?? [];
        const sortedShelves = this.sortArray(shelves, this.shelfSortField, this.shelfSortOrder);

        const shelfItems = sortedShelves.map((shelf) => ({
          menu: this.libraryShelfMenuService.initializeShelfMenuItems(shelf),
          label: shelf.name,
          type: 'Shelf',
          icon: 'pi pi-' + shelf.icon,
          routerLink: [`/shelf/${shelf.id}/books`],
          bookCount$: this.shelfService.getBookCount(shelf.id ?? 0),
        }));

        const unshelvedItem = {
          label: 'Unshelved',
          type: 'Shelf',
          icon: 'pi pi-inbox',
          routerLink: ['/unshelved-books'],
          bookCount$: this.shelfService.getUnshelvedBookCount?.() ?? of(0),
        };

        return [
          {
            type: 'shelf',
            label: 'Shelves',
            hasDropDown: true,
            hasCreate: false,
            items: [unshelvedItem, ...shelfItems],
          },
        ];
      })
    );
  }

  openChangelogDialog() {
    const isMobile = window.innerWidth <= 768;
    this.dynamicDialogRef = this.dialogService.open(VersionChangelogDialogComponent, {
      header: 'What’s New',
      modal: true,
      closable: true,
      style: {
        position: 'absolute',
        top: '10%',
        bottom: '10%',
        width: isMobile ? '90vw' : '800px',
        maxWidth: isMobile ? '90vw' : '800px',
        minWidth: isMobile ? '90vw' : '800px',
      },
    });
  }

  getVersionUrl(version: string | undefined): string {
    if (!version) return '#';
    return version.startsWith('v')
      ? `https://github.com/adityachandelgit/BookLore/releases/tag/${version}`
      : `https://github.com/adityachandelgit/BookLore/commit/${version}`;
  }

  private sortArray<T>(array: T[], field: 'name' | 'id', order: 'asc' | 'desc'): T[] {
    return [...array].sort((a, b) => {
      const aVal = (a as any)[field] ?? '';
      const bVal = (b as any)[field] ?? '';
      let comparison = 0;

      if (typeof aVal === 'string' && typeof bVal === 'string') {
        comparison = aVal.localeCompare(bVal);
      } else if (typeof aVal === 'number' && typeof bVal === 'number') {
        comparison = aVal - bVal;
      }

      return order === 'asc' ? comparison : -comparison;
    });
  }

  private validateSortField(field: string): 'name' | 'id' {
    return field === 'id' ? 'id' : 'name';
  }

  private validateSortOrder(order: string): 'asc' | 'desc' {
    return order === 'desc' ? 'desc' : 'asc';
  }
}
