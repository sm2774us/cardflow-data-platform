import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RotateCcw } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '@/components/PageHeader';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog';
import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { api } from '@/lib/api';
import { formatTime, shortId } from '@/lib/format';
import type { QuarantineItem } from '@/lib/types';

function QuarantineRow({ item }: { item: QuarantineItem }) {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [payload, setPayload] = useState(item.rawPayload);
  const replay = useMutation({
    mutationFn: () => api.replayQuarantine(item.id, payload),
    onSuccess: () => { setOpen(false); void qc.invalidateQueries({ queryKey: ['quarantine'] }); },
  });
  const pretty = () => { try { return JSON.stringify(JSON.parse(item.rawPayload), null, 2); } catch { return item.rawPayload; } };
  return (
    <TR>
      <TD className="font-mono text-xs">{shortId(item.eventId)}</TD>
      <TD><div className="flex flex-wrap gap-1">{item.failedRules.split(',').map((r) => <Badge key={r} variant="warning">{r}</Badge>)}</div></TD>
      <TD className="text-xs text-muted-foreground">{formatTime(item.createdAt)}</TD>
      <TD className="text-right">
        {item.replayed ? <Badge variant="outline">replayed</Badge> : (
          <Button size="sm" variant="outline" onClick={() => { setPayload(pretty()); setOpen(true); }}><RotateCcw className="h-3.5 w-3.5" />Fix & replay</Button>
        )}
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogContent>
            <DialogTitle className="text-base font-semibold">Correct payload and replay</DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">Edit the JSON so it satisfies the failed rules, then re-drive it through the outbox. The idempotency record is cleared for this event id.</DialogDescription>
            <textarea aria-label="Payload" className="h-56 w-full rounded-md border bg-muted/40 p-3 font-mono text-xs" value={payload} onChange={(e) => setPayload(e.target.value)} />
            {replay.isError && <p className="text-xs text-danger">{(replay.error as Error).message}</p>}
            <div className="flex justify-end gap-2"><DialogClose asChild><Button variant="ghost">Cancel</Button></DialogClose><Button onClick={() => replay.mutate()} disabled={replay.isPending}>Replay</Button></div>
          </DialogContent>
        </Dialog>
      </TD>
    </TR>
  );
}

export function QualityPage() {
  const qc = useQueryClient();
  const rules = useQuery({ queryKey: ['rules'], queryFn: api.rules, staleTime: Infinity });
  const quarantine = useQuery({ queryKey: ['quarantine'], queryFn: api.quarantine, refetchInterval: 4000 });
  const dlq = useQuery({ queryKey: ['dlq'], queryFn: api.deadLetters, refetchInterval: 4000 });
  const replayDlq = useMutation({ mutationFn: (id: number) => api.replayDlq(id), onSuccess: () => void qc.invalidateQueries({ queryKey: ['dlq'] }) });

  return (
    <div>
      <PageHeader title="Data quality & recovery" description="Events failing the promotion gate are quarantined with the exact failed rule codes. Poison messages that exhaust retries land in the dead-letter store. Both are replayable." />
      <Tabs defaultValue="quarantine">
        <TabsList>
          <TabsTrigger value="quarantine">Quarantine ({quarantine.data?.length ?? 0})</TabsTrigger>
          <TabsTrigger value="dlq">Dead letters ({dlq.data?.length ?? 0})</TabsTrigger>
          <TabsTrigger value="rules">Rules ({rules.data?.length ?? 0})</TabsTrigger>
        </TabsList>
        <TabsContent value="quarantine"><Card><Table>
          <THead><TR className="hover:bg-transparent"><TH>Event</TH><TH>Failed rules</TH><TH>At</TH><TH /></TR></THead>
          <TBody>
            {(quarantine.data ?? []).map((i) => <QuarantineRow key={i.id} item={i} />)}
            {quarantine.data?.length === 0 && <TR><TD colSpan={4} className="py-10 text-center text-muted-foreground">Nothing quarantined. Fire a burst with invalid % &gt; 0.</TD></TR>}
          </TBody></Table></Card></TabsContent>
        <TabsContent value="dlq"><Card><Table>
          <THead><TR className="hover:bg-transparent"><TH>Event</TH><TH>Error after retries</TH><TH>At</TH><TH /></TR></THead>
          <TBody>
            {(dlq.data ?? []).map((d) => (
              <TR key={d.id}><TD className="font-mono text-xs">{shortId(d.eventId)}</TD><TD className="max-w-md truncate text-xs text-danger">{d.error}</TD><TD className="text-xs text-muted-foreground">{formatTime(d.createdAt)}</TD>
                <TD className="text-right">{d.replayed ? <Badge variant="outline">replayed</Badge> : <Button size="sm" variant="outline" disabled={replayDlq.isPending} onClick={() => replayDlq.mutate(d.id)}><RotateCcw className="h-3.5 w-3.5" />Replay</Button>}</TD></TR>
            ))}
            {dlq.data?.length === 0 && <TR><TD colSpan={4} className="py-10 text-center text-muted-foreground">DLQ empty. Fire a burst with poison messages &gt; 0.</TD></TR>}
          </TBody></Table></Card></TabsContent>
        <TabsContent value="rules"><Card><CardHeader><CardTitle>Promotion-gate rules</CardTitle><CardDescription>All must pass for an event to reach the curated table and the lake</CardDescription></CardHeader>
          <CardContent><ul className="grid gap-2 sm:grid-cols-2">{(rules.data ?? []).map((r) => (
            <li key={r.code} className="rounded-md border p-3"><p className="font-mono text-xs font-semibold text-primary">{r.code}</p><p className="mt-1 text-sm text-muted-foreground">{r.description}</p></li>
          ))}</ul></CardContent></Card></TabsContent>
      </Tabs>
    </div>
  );
}
