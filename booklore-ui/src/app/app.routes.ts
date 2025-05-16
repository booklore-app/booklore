import {Routes} from '@angular/router';
import {BookBrowserComponent} from './book/components/book-browser/book-browser.component';
import {MainDashboardComponent} from './dashboard/components/main-dashboard/main-dashboard.component';
import {AppLayoutComponent} from './layout/component/layout-main/app.layout.component';
import {LoginComponent} from './core/component/login/login.component';
import {AuthGuard} from './auth.guard';
import {SettingsComponent} from './settings/settings.component';
import {PdfViewerComponent} from './book/components/pdf-viewer/pdf-viewer.component';
import {EpubViewerComponent} from './book/components/epub-viewer/component/epub-viewer.component';
import {ChangePasswordComponent} from './core/component/change-password/change-password.component';
import {BookMetadataCenterComponent} from './book/metadata/book-metadata-center/book-metadata-center.component';
import {SetupComponent} from './core/setup/setup.component';
import {SetupGuard} from './core/setup/setup.guard';
import {SetupRedirectGuard} from './core/setup/setup-redirect.guard';
import {EmptyComponent} from './core/empty/empty.component';
import {LoginGuard} from './core/setup/ login.guard';
import {OidcCallbackComponent} from './core/security/oidc-callback/oidc-callback.component';

export const routes: Routes = [
  {
    path: '',
    canActivate: [SetupRedirectGuard],
    pathMatch: 'full',
    component: EmptyComponent
  },
  {
    path: 'setup',
    component: SetupComponent,
    canActivate: [SetupGuard]
  },
  {path: 'oauth2-callback', component: OidcCallbackComponent},
  {
    path: '',
    component: AppLayoutComponent,
    children: [
      {path: 'dashboard', component: MainDashboardComponent, canActivate: [AuthGuard]},
      {path: 'all-books', component: BookBrowserComponent, canActivate: [AuthGuard]},
      {path: 'settings', component: SettingsComponent, canActivate: [AuthGuard]},
      {path: 'library/:libraryId/books', component: BookBrowserComponent, canActivate: [AuthGuard]},
      {path: 'shelf/:shelfId/books', component: BookBrowserComponent, canActivate: [AuthGuard]},
      {path: 'unshelved-books', component: BookBrowserComponent, canActivate: [AuthGuard]},
      {path: 'book/:bookId', component: BookMetadataCenterComponent, canActivate: [AuthGuard]}
    ]
  },
  {
    path: 'pdf-viewer/book/:bookId',
    component: PdfViewerComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'epub-viewer/book/:bookId',
    component: EpubViewerComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [LoginGuard]
  },
  {
    path: 'change-password',
    component: ChangePasswordComponent,
    canActivate: [SetupGuard]
  },
  {
    path: '**',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];
