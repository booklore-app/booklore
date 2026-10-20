import {beforeEach, describe, expect, it} from 'vitest';
import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TestBed} from '@angular/core/testing';
import {AutoComplete} from 'primeng/autocomplete';

/**
 * LOC-539 follow-up. `commitPendingAuthor()` flushes `authorInputValue` into
 * the `authors` control at save time, which only works if PrimeNG actually
 * writes typed text into that ngModel. The editor spec sets the property
 * directly, so it never exercised that assumption. This does: it drives the
 * real <p-autoComplete>, configured as the author input is, with a real
 * input event.
 */
@Component({
  standalone: true,
  imports: [AutoComplete, FormsModule],
  template: `
    <p-autoComplete
      [(ngModel)]="authorInputValue"
      [ngModelOptions]="{ standalone: true }"
      [suggestions]="filteredAuthors"
      [forceSelection]="false"
      [dropdown]="false"
      placeholder="Add author..."
    />
  `,
})
class HostComponent {
  authorInputValue = '';
  filteredAuthors: string[] = [];
}

describe('author autocomplete ngModel binding', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<HostComponent>>;

  beforeEach(() => {
    TestBed.configureTestingModule({imports: [HostComponent]});
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  const type = (text: string) => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    input.value = text;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  it('writes typed text into the bound model without Enter or a suggestion click', () => {
    type('Helen Woodruff');

    expect(fixture.componentInstance.authorInputValue).toBe('Helen Woodruff');
  });

  it('leaves the model empty when nothing was typed', () => {
    expect(fixture.componentInstance.authorInputValue).toBe('');
  });
});
