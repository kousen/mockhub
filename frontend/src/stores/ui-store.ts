import { create } from 'zustand';

type Theme = 'light' | 'dark';

interface UiState {
  mobileNavOpen: boolean;
  theme: Theme;
}

interface UiActions {
  toggleMobileNav: () => void;
  closeMobileNav: () => void;
  setTheme: (theme: Theme) => void;
}

export const useUiStore = create<UiState & UiActions>()((set) => ({
  mobileNavOpen: false,
  theme: 'light',

  toggleMobileNav: () => set((state) => ({ mobileNavOpen: !state.mobileNavOpen })),

  closeMobileNav: () => set({ mobileNavOpen: false }),

  setTheme: (theme: Theme) => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    set({ theme });
  },
}));
