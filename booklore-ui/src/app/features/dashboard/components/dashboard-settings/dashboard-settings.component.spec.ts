import {CdkDragDrop} from '@angular/cdk/drag-drop';
import {TestBed} from '@angular/core/testing';
import {TranslocoService} from '@jsverse/transloco';
import {DynamicDialogRef} from 'primeng/dynamicdialog';
import {BehaviorSubject, of} from 'rxjs';
import {describe, expect, it} from 'vitest';
import {DashboardConfig, ScrollerConfig, ScrollerType} from '../../models/dashboard-config.model';
import {DashboardConfigService} from '../../services/dashboard-config.service';
import {MagicShelfService} from '../../../magic-shelf/service/magic-shelf.service';
import {DashboardSettingsComponent} from './dashboard-settings.component';

describe('DashboardSettingsComponent', () => {
  it('reorders scrollers and updates their order after a drag', () => {
    TestBed.configureTestingModule({
      providers: [
        {provide: DashboardConfigService, useValue: {config$: of()}},
        {provide: DynamicDialogRef, useValue: {}},
        {
          provide: MagicShelfService,
          useValue: {shelvesState$: new BehaviorSubject({shelves: []})}
        },
        {
          provide: TranslocoService,
          useValue: {langChanges$: of('en'), translate: (key: string) => key}
        }
      ]
    });

    const component = TestBed.runInInjectionContext(() => new DashboardSettingsComponent());
    component.config = {
      scrollers: [
        createScroller('1', 1),
        createScroller('2', 2),
        createScroller('3', 3)
      ]
    } as DashboardConfig;
    const draggable = component as unknown as {
      onScrollerDrop(event: CdkDragDrop<ScrollerConfig[]>): void;
    };

    draggable.onScrollerDrop({previousIndex: 0, currentIndex: 2} as CdkDragDrop<ScrollerConfig[]>);

    expect(component.config.scrollers.map(scroller => scroller.id)).toEqual(['2', '3', '1']);
    expect(component.config.scrollers.map(scroller => scroller.order)).toEqual([1, 2, 3]);
  });
});

function createScroller(id: string, order: number): ScrollerConfig {
  return {
    id,
    order,
    type: ScrollerType.LATEST_ADDED,
    title: '',
    enabled: true,
    maxItems: 20
  };
}
