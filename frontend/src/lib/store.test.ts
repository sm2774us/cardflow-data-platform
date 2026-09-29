import { beforeEach, describe, expect, it } from 'vitest';
import { translate } from './i18n';
import { useUiStore } from './store';

describe('ui store', () => {
  beforeEach(() => useUiStore.setState({ theme: 'light', lang: 'en' }));
  it('toggles theme', () => {
    useUiStore.getState().toggleTheme();
    expect(useUiStore.getState().theme).toBe('dark');
  });
  it('switches language and translates', () => {
    useUiStore.getState().setLang('pt');
    expect(useUiStore.getState().lang).toBe('pt');
    expect(translate('pt', 'transactions')).toBe('Transações');
    expect(translate('es', 'overview')).toBe('Resumen');
  });
});
