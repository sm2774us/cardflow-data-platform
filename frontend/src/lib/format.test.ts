import { describe, expect, it } from 'vitest';
import { formatCompact, formatCurrency, formatPercent, shortId } from './format';

describe('format', () => {
  it('formats currency and falls back on invalid codes', () => {
    expect(formatCurrency(1234.5, 'USD')).toBe('$1,234.50');
    expect(formatCurrency(5, 'NOPE!')).toBe('5.00 NOPE!');
  });
  it('formats compact numbers and percentages', () => {
    expect(formatCompact(12_500)).toBe('12.5K');
    expect(formatPercent(0.9876)).toBe('98.8%');
  });
  it('shortens long ids only', () => {
    expect(shortId('abc')).toBe('abc');
    expect(shortId('123e4567-e89b-12d3-a456')).toBe('123e4567…456');
  });
});
