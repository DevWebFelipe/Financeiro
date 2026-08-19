import { Component, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeader } from '../header/app-header';
import { AppNavigation } from '../navigation/app-navigation';

/** Keep in sync with layout CSS `@media (min-width: 64rem)`. */
const SHELL_DESKTOP_MEDIA = '(min-width: 64rem)';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, AppHeader, AppNavigation],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  host: {
    '(document:keydown)': 'onDocumentKeydown($event)',
  },
})
export class MainLayout {
  private readonly destroyRef = inject(DestroyRef);
  private readonly header = viewChild(AppHeader);
  private readonly drawer = viewChild<ElementRef<HTMLElement>>('drawer');
  private readonly drawerClose = viewChild<ElementRef<HTMLButtonElement>>('drawerClose');
  private readonly mainContent = viewChild<ElementRef<HTMLElement>>('mainContent');

  readonly sidebarCollapsed = signal(false);
  readonly mobileNavigationOpen = signal(false);

  private opener: HTMLElement | null = null;

  constructor() {
    this.bindDesktopMedia();
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  toggleMobileNavigation(): void {
    if (this.mobileNavigationOpen()) {
      this.closeMobileNavigation({ restoreFocus: false });
      return;
    }
    this.openMobileNavigation();
  }

  openMobileNavigation(): void {
    const menuButton = this.header()?.menuButton()?.nativeElement;
    this.opener = menuButton ?? this.currentFocus();
    this.mobileNavigationOpen.set(true);
    queueMicrotask(() => this.drawerClose()?.nativeElement.focus());
  }

  closeMobileNavigation(options?: { restoreFocus?: boolean }): void {
    if (!this.mobileNavigationOpen()) {
      return;
    }

    this.mobileNavigationOpen.set(false);
    const restoreFocus = options?.restoreFocus !== false;
    const opener = this.opener;
    this.opener = null;

    if (restoreFocus) {
      queueMicrotask(() => opener?.focus());
    }
  }

  skipToContent(event: Event): void {
    event.preventDefault();
    this.mainContent()?.nativeElement.focus();
  }

  onDocumentKeydown(event: KeyboardEvent): void {
    if (!this.mobileNavigationOpen()) {
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeMobileNavigation();
      return;
    }

    if (event.key === 'Tab') {
      this.retainFocus(event);
    }
  }

  private retainFocus(event: KeyboardEvent): void {
    const root = this.drawer()?.nativeElement;
    if (root == null) {
      return;
    }

    const focusable = focusableElements(root);
    if (focusable.length === 0) {
      event.preventDefault();
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = document.activeElement;

    if (event.shiftKey && active === first) {
      event.preventDefault();
      last.focus();
      return;
    }

    if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private bindDesktopMedia(): void {
    if (typeof window.matchMedia !== 'function') {
      return;
    }

    const media = window.matchMedia(SHELL_DESKTOP_MEDIA);
    const onChange = (event: MediaQueryListEvent) => {
      if (event.matches) {
        this.closeMobileNavigation({ restoreFocus: false });
      }
    };

    media.addEventListener('change', onChange);
    this.destroyRef.onDestroy(() => media.removeEventListener('change', onChange));
  }

  private currentFocus(): HTMLElement | null {
    const active = document.activeElement;
    return active instanceof HTMLElement ? active : null;
  }
}

function focusableElements(root: HTMLElement): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>('a[href], button:not([disabled])'));
}
