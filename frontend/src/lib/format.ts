export const formatCurrency = (amount: number, currency = 'USD', locale = 'en-US'): string => {
  try {
    return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(amount);
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
};

export const formatCompact = (n: number): string =>
  new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 }).format(n);

export const formatPercent = (ratio: number, digits = 1): string => `${(ratio * 100).toFixed(digits)}%`;

export const formatTime = (iso: string): string =>
  new Date(iso).toLocaleTimeString('en-US', { hour12: false });

export const shortId = (id: string): string => (id.length > 12 ? `${id.slice(0, 8)}…${id.slice(-3)}` : id);
