import type { ReactNode } from 'react';

export const PageHeader = ({ title, description, actions }: { title: string; description?: string; actions?: ReactNode }) => (
  <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 className="text-xl font-semibold tracking-tight">{title}</h1>
      {description && <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{description}</p>}
    </div>
    {actions}
  </div>
);
