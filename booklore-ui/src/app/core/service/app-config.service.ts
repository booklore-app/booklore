import {DOCUMENT, isPlatformBrowser} from '@angular/common';
import {effect, inject, Injectable, PLATFORM_ID, signal} from '@angular/core';
import {AppState} from '../model/app-state.model';

@Injectable({
  providedIn: 'root',
})
export class AppConfigService {
  private readonly STORAGE_KEY = 'appConfigState';
  appState = signal<AppState>({});
  document = inject(DOCUMENT);
  platformId = inject(PLATFORM_ID);
  private initialized = false;

  constructor() {
    const initialState = this.loadAppState();
    this.appState.set({...initialState});
    this.document.documentElement.classList.add('p-dark');
    effect(
      () => {
        const state = this.appState();
        if (!this.initialized || !state) {
          this.initialized = true;
          return;
        }
        this.saveAppState(state);
      },
      {allowSignalWrites: true}
    );
  }

  private loadAppState(): AppState {
    if (isPlatformBrowser(this.platformId)) {
      const storedState = localStorage.getItem(this.STORAGE_KEY);
      if (storedState) {
        return JSON.parse(storedState);
      }
    }
    return {
      preset: 'Aura',
      primary: 'green',
      surface: 'neutral',
    };
  }

  private saveAppState(state: AppState): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(state));
    }
  }
}
