import { useQuery } from '@tanstack/react-query';
import { motion } from 'motion/react';
import { AlertTriangle, ArrowRight, CheckCircle2, Copy, Database, Inbox, ShieldAlert, Zap } from 'lucide-react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { KpiCard } from '@/components/KpiCard';
import { PageHeader } from '@/components/PageHeader';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { api } from '@/lib/api';
import { formatCompact, formatPercent, formatTime } from '@/lib/format';

export function OverviewPage() {
  const { data, isError, error } = useQuery({ queryKey: ['stats'], queryFn: api.stats, refetchInterval: 3000 });
  const s = data;

  const stages = s && [
    { name: 'Webhook + outbox', value: s.outboxPending, note: 'pending relay' },
    { name: 'Idempotent consumer', value: s.duplicatesDropped, note: 'duplicates dropped' },
    { name: 'DQ gate', value: s.quarantined, note: 'quarantined' },
    { name: 'Curated store', value: s.transactions, note: 'promoted' },
    { name: 'Data lake', value: s.lakeRecords, note: `${s.lakeBatches} batches` },
  ];

  return (
    <div>
      <PageHeader title="Pipeline overview" description="Live health of the ingestion → quality → authorization → lake pipeline. Auto-refreshes every 3 seconds." />
      {isError && (
        <Card className="mb-4 border-danger/40"><CardContent className="flex items-center gap-2 p-4 text-sm text-danger">
          <AlertTriangle className="h-4 w-4" /> Backend unreachable: {(error as Error).message}. Start it with <code className="font-mono">make up</code>.
        </CardContent></Card>
      )}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard label="Curated transactions" value={s ? formatCompact(s.transactions) : '—'} hint={s ? `${formatCompact(s.approved)} approved · ${formatCompact(s.declined)} declined` : undefined} icon={Zap} />
        <KpiCard label="DQ pass rate" value={s ? formatPercent(s.dqPassRate) : '—'} hint={s ? `${s.quarantined} quarantined` : undefined} icon={CheckCircle2} tone={s && s.dqPassRate < 0.9 ? 'warning' : 'success'} />
        <KpiCard label="Duplicates absorbed" value={s ? formatCompact(s.duplicatesDropped) : '—'} hint="exactly-once effect" icon={Copy} />
        <KpiCard label="Dead letters" value={s ? String(s.deadLetters) : '—'} hint={s ? `${s.unexported} awaiting lake export` : undefined} icon={ShieldAlert} tone={s && s.deadLetters > 0 ? 'danger' : 'default'} />
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader><CardTitle>Throughput (last 30 min)</CardTitle><CardDescription>Curated events per minute; declines highlighted</CardDescription></CardHeader>
          <CardContent className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={(s?.throughput ?? []).map((p) => ({ ...p, t: formatTime(p.minute) }))}>
                <defs>
                  <linearGradient id="g1" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.4} /><stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0} /></linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                <XAxis dataKey="t" tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                <Area type="monotone" dataKey="total" name="Events" stroke="hsl(var(--primary))" fill="url(#g1)" strokeWidth={2} />
                <Area type="monotone" dataKey="declined" name="Declined" stroke="hsl(var(--danger))" fill="hsl(var(--danger))" fillOpacity={0.15} strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Data flow</CardTitle><CardDescription>Volume at each stage</CardDescription></CardHeader>
          <CardContent>
            <ol className="space-y-2">
              {(stages ?? []).map((st, i) => (
                <motion.li key={st.name} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.05 }}
                  className="flex items-center justify-between rounded-md border bg-muted/40 px-3 py-2 text-sm">
                  <span className="flex items-center gap-2">{i === 0 ? <Inbox className="h-4 w-4 text-primary" /> : i === 4 ? <Database className="h-4 w-4 text-primary" /> : <ArrowRight className="h-4 w-4 text-muted-foreground" />}{st.name}</span>
                  <span className="text-right"><span className="font-semibold tabular-nums">{formatCompact(st.value)}</span><span className="block text-[10px] text-muted-foreground">{st.note}</span></span>
                </motion.li>
              ))}
            </ol>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
