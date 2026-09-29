import { Badge } from '@/components/ui/badge';

const variant = (d: string) =>
  d === 'DECLINED' ? 'danger' : d === 'QUARANTINED' ? 'warning' : d === 'DUPLICATE' ? 'outline' : d === 'APPROVED' || d === 'SETTLED' ? 'success' : 'default';

export const DecisionBadge = ({ value }: { value: string }) => <Badge variant={variant(value)}>{value}</Badge>;
