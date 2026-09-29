import { useUiStore } from './store';

export type Lang = 'en' | 'es' | 'pt';

const dict = {
  en: { overview: 'Overview', live: 'Live stream', transactions: 'Transactions', quality: 'Data quality', authorize: 'Authorization', architecture: 'Architecture & JD map', subtitle: 'Event-driven card data platform' },
  es: { overview: 'Resumen', live: 'Flujo en vivo', transactions: 'Transacciones', quality: 'Calidad de datos', authorize: 'Autorización', architecture: 'Arquitectura y mapa JD', subtitle: 'Plataforma de datos de tarjetas basada en eventos' },
  pt: { overview: 'Visão geral', live: 'Fluxo ao vivo', transactions: 'Transações', quality: 'Qualidade de dados', authorize: 'Autorização', architecture: 'Arquitetura e mapa JD', subtitle: 'Plataforma de dados de cartões orientada a eventos' },
} as const;

export type MessageKey = keyof (typeof dict)['en'];
export const translate = (lang: Lang, key: MessageKey): string => dict[lang][key];
export const useT = () => {
  const lang = useUiStore((s) => s.lang);
  return (key: MessageKey) => translate(lang, key);
};
