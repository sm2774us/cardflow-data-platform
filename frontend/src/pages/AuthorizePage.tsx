import { useMutation } from '@tanstack/react-query';
import { motion } from 'motion/react';
import { ShieldCheck, ShieldX } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '@/components/PageHeader';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input, Label } from '@/components/ui/input';
import { api } from '@/lib/api';
import type { AuthRequest } from '@/lib/types';

const presets: Record<string, AuthRequest> = {
  'Coffee (approve)': { cardToken: 'tok_DEMO00000001', mcc: '5814', amount: 4.5, country: 'US' },
  'Casino (blocked MCC)': { cardToken: 'tok_DEMO00000002', mcc: '7995', amount: 250, country: 'US' },
  'Large FX wire abroad': { cardToken: 'tok_DEMO00000003', mcc: '4829', amount: 8000, country: 'KP' },
};

export function AuthorizePage() {
  const [form, setForm] = useState<AuthRequest>(presets['Coffee (approve)']!);
  const m = useMutation({ mutationFn: api.authorize });
  const set = <K extends keyof AuthRequest>(k: K, v: AuthRequest[K]) => setForm((f) => ({ ...f, [k]: v }));
  const d = m.data;

  return (
    <div>
      <PageHeader title="Real-time authorization" description="What-if decisioning: deterministic rules plus an ML risk score behind a circuit breaker. Set CARDFLOW_FRAUD_CHAOS_FAILURE_RATE to watch degraded mode." />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Request</CardTitle><CardDescription>POST /api/v1/authorizations</CardDescription></CardHeader>
          <CardContent className="grid gap-3">
            <div className="flex flex-wrap gap-2">{Object.entries(presets).map(([name, p]) => <Button key={name} variant="secondary" size="sm" onClick={() => setForm(p)}>{name}</Button>)}</div>
            <div className="grid gap-1"><Label htmlFor="tok">Card token</Label><Input id="tok" value={form.cardToken} onChange={(e) => set('cardToken', e.target.value)} /></div>
            <div className="grid grid-cols-3 gap-3">
              <div className="grid gap-1"><Label htmlFor="mcc">MCC</Label><Input id="mcc" value={form.mcc} onChange={(e) => set('mcc', e.target.value)} /></div>
              <div className="grid gap-1"><Label htmlFor="amt">Amount</Label><Input id="amt" type="number" min={0} step="0.01" value={form.amount} onChange={(e) => set('amount', Number(e.target.value))} /></div>
              <div className="grid gap-1"><Label htmlFor="cty">Country</Label><Input id="cty" maxLength={2} value={form.country} onChange={(e) => set('country', e.target.value.toUpperCase())} /></div>
            </div>
            <Button onClick={() => m.mutate(form)} disabled={m.isPending}>Authorize</Button>
            {m.isError && <p className="text-xs text-danger">{(m.error as Error).message}</p>}
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Decision</CardTitle><CardDescription>Rules first, model score second</CardDescription></CardHeader>
          <CardContent>
            {!d && <p className="py-10 text-center text-sm text-muted-foreground">Submit a request to see the decision.</p>}
            {d && (
              <motion.div initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }} className="grid gap-4">
                <div className={`flex items-center gap-3 rounded-lg p-4 ${d.approved ? 'bg-success/15 text-success' : 'bg-danger/15 text-danger'}`}>
                  {d.approved ? <ShieldCheck className="h-8 w-8" /> : <ShieldX className="h-8 w-8" />}
                  <div><p className="text-lg font-semibold">{d.approved ? 'APPROVED' : 'DECLINED'}</p><p className="text-xs">risk score {d.riskScore.toFixed(4)}</p></div>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-muted" aria-label="Risk score"><div className="h-full rounded-full bg-primary transition-all" style={{ width: `${Math.min(100, d.riskScore * 100)}%` }} /></div>
                <div className="flex flex-wrap gap-2">{d.reasons.length ? d.reasons.map((r) => <Badge key={r} variant="danger">{r}</Badge>) : <Badge variant="success">no rule violations</Badge>}{d.degraded && <Badge variant="warning">fraud model degraded — fallback used</Badge>}</div>
              </motion.div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
