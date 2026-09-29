import type {
  AuthDecision, AuthRequest, BurstRequest, BurstResult, DeadLetterItem, DqRule, PageResult,
  PipelineStats, QuarantineItem, Transaction,
} from './types';

const BASE = import.meta.env.VITE_API_URL ?? '';

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });
  if (!res.ok) {
    let detail = res.statusText;
    try { detail = ((await res.json()) as { detail?: string }).detail ?? detail; } catch { /* non-JSON error body */ }
    throw new ApiError(res.status, detail);
  }
  return (await res.json()) as T;
}

export const api = {
  stats: () => request<PipelineStats>('/api/v1/pipeline/stats'),
  transactions: (p: { page: number; size: number; decision?: string; q?: string }) => {
    const qs = new URLSearchParams({ page: String(p.page), size: String(p.size) });
    if (p.decision) qs.set('decision', p.decision);
    if (p.q) qs.set('q', p.q);
    return request<PageResult<Transaction>>(`/api/v1/transactions?${qs}`);
  },
  rules: () => request<DqRule[]>('/api/v1/dq/rules'),
  quarantine: () => request<QuarantineItem[]>('/api/v1/dq/quarantine?limit=100'),
  replayQuarantine: (id: number, payload?: string) =>
    request<{ replayed: boolean }>(`/api/v1/dq/quarantine/${id}/replay`, { method: 'POST', body: JSON.stringify(payload ? { payload } : {}) }),
  deadLetters: () => request<DeadLetterItem[]>('/api/v1/dlq?limit=100'),
  replayDlq: (id: number) => request<{ replayed: boolean }>(`/api/v1/dlq/${id}/replay`, { method: 'POST', body: '{}' }),
  authorize: (r: AuthRequest) => request<AuthDecision>('/api/v1/authorizations', { method: 'POST', body: JSON.stringify(r) }),
  burst: (r: BurstRequest) => request<BurstResult>('/api/v1/simulator/burst', { method: 'POST', body: JSON.stringify(r) }),
};
