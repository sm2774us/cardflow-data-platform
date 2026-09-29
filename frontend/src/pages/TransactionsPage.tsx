import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { getRouteApi } from '@tanstack/react-router';
import type { ColumnDef } from '@tanstack/react-table';
import { Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { DataTable } from '@/components/DataTable';
import { DecisionBadge } from '@/components/DecisionBadge';
import { PageHeader } from '@/components/PageHeader';
import { Card } from '@/components/ui/card';
import { Input, Select } from '@/components/ui/input';
import { api } from '@/lib/api';
import { formatCurrency, shortId } from '@/lib/format';
import type { Transaction } from '@/lib/types';

const routeApi = getRouteApi('/transactions');
const SIZE = 15;

export function TransactionsPage() {
  const { page, decision, q } = routeApi.useSearch();
  const navigate = routeApi.useNavigate();
  const [text, setText] = useState(q ?? '');

  useEffect(() => {   // debounce free-text search into the URL (shareable, back-button friendly)
    const id = setTimeout(() => { if ((q ?? '') !== text) void navigate({ search: (p) => ({ ...p, q: text || undefined, page: 0 }) }); }, 350);
    return () => clearTimeout(id);
  }, [text, q, navigate]);

  const { data, isFetching } = useQuery({
    queryKey: ['transactions', page, decision, q],
    queryFn: () => api.transactions({ page, size: SIZE, decision, q }),
    placeholderData: keepPreviousData,
  });

  const columns = useMemo<ColumnDef<Transaction, unknown>[]>(() => [
    { header: 'Event', accessorKey: 'eventId', cell: (c) => <span className="font-mono text-xs">{shortId(c.getValue<string>())}</span> },
    { header: 'Merchant', accessorKey: 'merchantName' },
    { header: 'Type', accessorKey: 'eventType', cell: (c) => <span className="text-xs text-muted-foreground">{c.getValue<string>()}</span> },
    { header: 'Amount', accessorKey: 'amount', cell: (c) => <span className="tabular-nums">{formatCurrency(c.getValue<number>(), c.row.original.currency)}</span> },
    { header: 'Country', accessorKey: 'country' },
    { header: 'Risk', accessorKey: 'riskScore', cell: (c) => <span className="tabular-nums">{c.getValue<number>().toFixed(2)}</span> },
    { header: 'Decision', accessorKey: 'decision', cell: (c) => <DecisionBadge value={c.getValue<string>()} /> },
    { header: 'Reason', accessorKey: 'declineReason', cell: (c) => <span className="text-xs text-muted-foreground">{c.getValue<string | undefined>() ?? '—'}</span> },
  ], []);

  return (
    <div>
      <PageHeader title="Curated transactions" description="Server-side paginated and filtered; state lives in the URL via TanStack Router typed search params." />
      <Card>
        <div className="flex flex-wrap gap-2 border-b p-3">
          <div className="relative min-w-[14rem] flex-1">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input className="pl-9" placeholder="Search merchant, card token or exact event id" value={text} onChange={(e) => setText(e.target.value)} aria-label="Search transactions" />
          </div>
          <Select className="w-44" aria-label="Decision filter" value={decision ?? ''} onChange={(e) => void navigate({ search: (p) => ({ ...p, decision: e.target.value || undefined, page: 0 }) })}>
            <option value="">All decisions</option><option>APPROVED</option><option>DECLINED</option><option>SETTLED</option><option>REVERSED</option>
          </Select>
        </div>
        <DataTable data={data?.items ?? []} columns={columns} total={data?.total ?? 0} page={page} size={SIZE} loading={isFetching}
          onPageChange={(p) => void navigate({ search: (prev) => ({ ...prev, page: p }) })} empty="No transactions yet — fire a burst on the Live stream page." />
      </Card>
    </div>
  );
}
