import {ComponentFixture, TestBed} from '@angular/core/testing';
import {BehaviorSubject} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {getTranslocoModule} from '../../../core/testing/transloco-testing';
import {UserService, UserState} from '../../../features/settings/user-management/user.service';
import {LocalStorageService} from '../../service/local-storage.service';
import {BookOrbitAnnouncementComponent} from './bookorbit-announcement.component';

describe('BookOrbitAnnouncementComponent', () => {
  const userState = new BehaviorSubject<UserState>({user: null, loaded: true, error: null});
  const storage = {
    get: vi.fn(),
    set: vi.fn()
  };
  let fixture: ComponentFixture<BookOrbitAnnouncementComponent>;

  beforeEach(async () => {
    vi.clearAllMocks();
    storage.get.mockReturnValue(false);
    userState.next({user: null, loaded: true, error: null});

    await TestBed.configureTestingModule({
      imports: [BookOrbitAnnouncementComponent, getTranslocoModule()],
      providers: [
        {provide: UserService, useValue: {userState$: userState.asObservable()}},
        {provide: LocalStorageService, useValue: storage}
      ]
    }).compileComponents();
  });

  function createFixture(admin: boolean): void {
    userState.next({
      user: {permissions: {admin}} as UserState['user'],
      loaded: true,
      error: null
    });
    fixture = TestBed.createComponent(BookOrbitAnnouncementComponent);
    fixture.detectChanges();
  }

  it('shows the announcement to administrators', () => {
    createFixture(true);

    expect(fixture.nativeElement.querySelector('.bookorbit-announcement')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('BookOrbit is the official successor to BookLore');

    const links = Array.from(fixture.nativeElement.querySelectorAll('.announcement-link')) as HTMLAnchorElement[];
    expect(links.map(link => link.href)).toEqual([
      'https://bookorbit.app/',
      'https://github.com/bookorbit/bookorbit',
      'https://bookorbit.app/migration/'
    ]);
  });

  it('does not show the announcement to non-administrators', () => {
    createFixture(false);

    expect(fixture.nativeElement.querySelector('.bookorbit-announcement')).toBeNull();
  });

  it('keeps a previously dismissed announcement hidden', () => {
    storage.get.mockReturnValue(true);
    createFixture(true);

    expect(fixture.nativeElement.querySelector('.bookorbit-announcement')).toBeNull();
  });

  it('stores the dismissal and hides the announcement', () => {
    createFixture(true);

    fixture.nativeElement.querySelector('.dismiss-button').click();
    fixture.detectChanges();

    expect(storage.set).toHaveBeenCalledWith('bookorbit-successor-banner-v1', true);
    expect(fixture.nativeElement.querySelector('.bookorbit-announcement')).toBeNull();
  });
});
