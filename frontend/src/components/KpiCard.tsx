import type { LucideIcon } from 'lucide-react';
import { motion } from 'motion/react';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

interface Props { label: string; value: string; hint?: string; icon: LucideIcon; tone?: 'default' | 'success' | 'warning' | 'danger' }

const tones = { default: 'text-primary bg-primary/10', success: 'text-success bg-success/15', warning: 'text-warning bg-warning/15', danger: 'text-danger bg-danger/15' };

export function KpiCard({ label, value, hint, icon: Icon, tone = 'default' }: Props) {
  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.25 }}>
      <Card>
        <CardContent className="flex items-start justify-between gap-3 p-5">
          <div>
            <p className="text-xs font-medium text-muted-foreground">{label}</p>
            <p className="mt-1 text-2xl font-semibold tabular-nums" data-testid="kpi-value">{value}</p>
            {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
          </div>
          <span className={cn('rounded-md p-2', tones[tone])}><Icon className="h-4 w-4" /></span>
        </CardContent>
      </Card>
    </motion.div>
  );
}
