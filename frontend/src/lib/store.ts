import { create } from 'zustand';
import type { Lang } from './i18n';

type Theme = 'light' | 'dark';
interface UiState {
  theme: Theme; lang: Lang;
  toggleTheme: () => void;
  setLang: (l: Lang) => void;
}

const initialTheme = (): Theme =>
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';

export const useUiStore = create<UiState>((set) => ({
  theme: initialTheme(),
  lang: 'en',
  toggleTheme: () => set((s) => ({ theme: s.theme === 'dark' ? 'light' : 'dark' })),
  setLang: (lang) => set({ lang }),
}));
