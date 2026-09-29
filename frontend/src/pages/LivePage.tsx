import { useMutation } from '@tanstack/react-query';
import { AnimatePresence, motion } from 'motion/react';
import { Pause, Play, Rocket, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { DecisionBadge } from '@/components/DecisionBadge';
import { PageHeader } from '@/components/PageHeader';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input, Label } from '@/components/ui/input';
import { useLiveEvents } from '@/hooks/useSse';
import { api } from '@/lib/api';
import { formatCurrency, formatTime, shortId } from '@/lib/format';
import type { BurstRequest } from '@/lib/types';

export function LivePage() {
  const { events, connected, paused, setPaused, clear } = useLiveEvents(80);
  const [cfg, setCfg] = useState<BurstRequest>({ count: 100, duplicatePct: 10, invalidPct: 10, poisonCount: 2 });
  const burst = useMutation({ mutationFn: api.burst });
  const num = (k: keyof BurstRequest) => (e: React.ChangeEvent<HTMLInputElement>) => setCfg({ ...cfg, [k]: Number(e.target.value) });

  return (
    <div>
      <PageHeader title="Live event stream" description="Server-sent events from the consumer. Fire a burst to watch validation, duplicate suppression, quarantine and retries in real time."
        actions={<Badge variant={connected ? 'success' : 'danger'}>{connected ? 'connected' : 'reconnecting…'}</Badge>} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="h-fit">
          <CardHeader><CardTitle>Traffic simulator</CardTitle><CardDescription>Injected via the same ingest path as the webhook</CardDescription></CardHeader>
          <CardContent className="grid gap-3">
            {([['count', 'Events (1–2000)'], ['duplicatePct', 'Duplicate delivery %'], ['invalidPct', 'Invalid payload %'], ['poisonCount', 'Poison messages (→ DLQ)']] as const).map(([k, label]) => (
              <div key={k} className="grid gap-1"><Label htmlFor={k}>{label}</Label><Input id={k} type="number" min={0} value={cfg[k]} onChange={num(k)} /></div>
            ))}
            <Button onClick={() => burst.mutate(cfg)} disabled={burst.isPending}><Rocket className="h-4 w-4" />{burst.isPending ? 'Sending…' : 'Fire burst'}</Button>
            {burst.isSuccess && <p className="text-xs text-muted-foreground">Sent {burst.data.valid} valid · {burst.data.invalid} invalid · {burst.data.duplicates} duplicates · {burst.data.poison} poison</p>}
            {burst.isError && <p className="text-xs text-danger">{(burst.error as Error).message}</p>}
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <div><CardTitle>Events</CardTitle><CardDescription>Newest first · last 80</CardDescription></div>
            <div className="flex gap-1">
              <Button variant="outline" size="sm" onClick={() => setPaused(!paused)}>{paused ? <Play className="h-3.5 w-3.5" /> : <Pause className="h-3.5 w-3.5" />}{paused ? 'Resume' : 'Pause'}</Button>
              <Button variant="outline" size="sm" onClick={clear}><Trash2 className="h-3.5 w-3.5" />Clear</Button>
            </div>
          </CardHeader>
          <CardContent className="max-h-[32rem] overflow-y-auto">
            {events.length === 0 && <p className="py-12 text-center text-sm text-muted-foreground">Waiting for events… fire a burst.</p>}
            <ul className="divide-y" aria-live="polite">
              <AnimatePresence initial={false}>
                {events.map((e) => (
                  <motion.li key={`${e.eventId}-${e.at}-${e.outcome}`} layout initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0 }} className="flex items-center gap-3 py-2 text-sm">
                    <span className="w-16 font-mono text-xs text-muted-foreground">{formatTime(e.at)}</span>
                    <DecisionBadge value={e.outcome} />
                    <span className="min-w-0 flex-1 truncate">{e.merchant ?? <span className="font-mono text-xs">{shortId(e.eventId)}</span>}<span className="ml-2 text-xs text-muted-foreground">{e.detail}</span></span>
                    {e.amount != null && <span className="tabular-nums">{formatCurrency(e.amount, e.currency ?? 'USD')}</span>}
                  </motion.li>
                ))}
              </AnimatePresence>
            </ul>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
