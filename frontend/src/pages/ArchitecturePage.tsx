import { CheckCircle2 } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { jdMatrix } from '@/data/jdMatrix';

const flow = ['Provider webhook (HMAC)', 'Transactional outbox', 'RabbitMQ quorum queue', 'Idempotent consumer', 'DQ gate', 'Authorize + persist', 'S3 lake (NDJSON.gz)', 'Snowflake / Athena'];

export function ArchitecturePage() {
  return (
    <div>
      <PageHeader title="Architecture & JD traceability" description="Every requirement in the Cloud Data Solutions posting mapped to the code that demonstrates it." />
      <Card className="mb-4"><CardHeader><CardTitle>End-to-end flow</CardTitle></CardHeader><CardContent>
        <ol className="flex flex-wrap items-center gap-2 text-xs">
          {flow.map((f, i) => (<li key={f} className="flex items-center gap-2"><span className="rounded-md border bg-accent px-2.5 py-1.5 font-medium text-accent-foreground">{f}</span>{i < flow.length - 1 && <span className="text-muted-foreground">→</span>}</li>))}
        </ol>
        <p className="mt-3 text-xs text-muted-foreground">At-least-once delivery + idempotent consumption = exactly-once effect. Poison messages retry with backoff, then park in the DLQ; invalid data is quarantined; both are replayable.</p>
      </CardContent></Card>
      <div className="grid gap-3">
        {jdMatrix.map((i) => (
          <Card key={i.requirement}><CardContent className="grid gap-2 p-4 md:grid-cols-[1fr_2fr]">
            <div className="flex items-start gap-2"><CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" /><div><p className="text-sm font-medium">{i.requirement}</p><Badge className="mt-1" variant={i.tier === 'required' ? 'default' : 'outline'}>{i.tier}</Badge></div></div>
            <div><p className="text-sm text-muted-foreground">{i.evidence}</p><p className="mt-2 flex flex-wrap gap-1">{i.where.map((w) => <code key={w} className="rounded bg-muted px-1.5 py-0.5 font-mono text-[11px]">{w}</code>)}</p></div>
          </CardContent></Card>
        ))}
      </div>
    </div>
  );
}
