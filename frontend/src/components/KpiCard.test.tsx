import { render, screen } from '@testing-library/react';
import { Activity } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { KpiCard } from './KpiCard';

describe('KpiCard', () => {
  it('renders label, value and hint', () => {
    render(<KpiCard label="Throughput" value="1.2K" hint="last 30 min" icon={Activity} />);
    expect(screen.getByText('Throughput')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-value')).toHaveTextContent('1.2K');
    expect(screen.getByText('last 30 min')).toBeInTheDocument();
  });
});
