import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DynamicDialogRef} from 'primeng/dynamicdialog';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {getTranslocoModule} from '../../../core/testing/transloco-testing';
import {BookOrbitTransitionDialogComponent} from './bookorbit-transition-dialog.component';

describe('BookOrbitTransitionDialogComponent', () => {
  const dialogRef = {close: vi.fn()};
  let fixture: ComponentFixture<BookOrbitTransitionDialogComponent>;

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [BookOrbitTransitionDialogComponent, getTranslocoModule()],
      providers: [{provide: DynamicDialogRef, useValue: dialogRef}]
    }).compileComponents();

    fixture = TestBed.createComponent(BookOrbitTransitionDialogComponent);
    fixture.detectChanges();
  });

  it('shows the transition message and canonical actions', () => {
    expect(fixture.nativeElement.textContent).toContain('BookOrbit is the official successor to BookLore');

    const links = Array.from(fixture.nativeElement.querySelectorAll('.dialog-link')) as HTMLAnchorElement[];
    expect(links.map(link => link.href)).toEqual([
      'https://bookorbit.app/',
      'https://github.com/bookorbit/bookorbit',
      'https://bookorbit.app/migration/'
    ]);
  });

  it('closes from the dialog close button', () => {
    fixture.nativeElement.querySelector('.close-button').click();

    expect(dialogRef.close).toHaveBeenCalledOnce();
  });
});
