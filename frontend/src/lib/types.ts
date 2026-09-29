export type Outcome = 'APPROVED' | 'DECLINED' | 'QUARANTINED' | 'DUPLICATE';

export interface Transaction {
  eventId: string; eventType: string; cardToken: string; merchantName: string; mcc: string;
  amount: number; currency: string; country: string; occurredAt: string;
  decision: string; declineReason?: string; riskScore: number; createdAt: string;
}
export interface PageResult<T> { items: T[]; total: number; page: number; size: number }
export interface ThroughputPoint { minute: string; total: number; declined: number }
export interface PipelineStats {
  outboxPending: number; transactions: number; approved: number; declined: number; quarantined: number;
  deadLetters: number; duplicatesDropped: number; lakeBatches: number; lakeRecords: number; unexported: number;
  dqPassRate: number; throughput: ThroughputPoint[];
}
export interface QuarantineItem { id: number; eventId: string; rawPayload: string; failedRules: string; createdAt: string; replayed: boolean }
export interface DeadLetterItem { id: number; eventId: string; rawPayload: string; error: string; createdAt: string; replayed: boolean }
export interface DqRule { code: string; description: string }
export interface LiveEvent { eventId: string; outcome: Outcome; merchant?: string; amount?: number; currency?: string; detail?: string; at: string }
export interface AuthRequest { cardToken: string; mcc: string; amount: number; country: string }
export interface AuthDecision { approved: boolean; reasons: string[]; riskScore: number; degraded: boolean }
export interface BurstRequest { count: number; duplicatePct: number; invalidPct: number; poisonCount: number }
export interface BurstResult { valid: number; invalid: number; duplicates: number; poison: number }
