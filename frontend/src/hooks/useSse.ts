import { useEffect, useRef, useState } from 'react';
import type { LiveEvent } from '@/lib/types';

const BASE = import.meta.env.VITE_API_URL ?? '';

/** Subscribes to the server-sent event stream; EventSource auto-reconnects, we surface connection state and cap memory. */
export function useLiveEvents(max = 100) {
  const [events, setEvents] = useState<LiveEvent[]>([]);
  const [connected, setConnected] = useState(false);
  const paused = useRef(false);
  const [pausedState, setPausedState] = useState(false);

  useEffect(() => {
    const es = new EventSource(`${BASE}/api/v1/stream/events`);
    es.onopen = () => setConnected(true);
    es.onerror = () => setConnected(false);
    es.addEventListener('pipeline', (m) => {
      if (paused.current) return;
      const ev = JSON.parse((m as MessageEvent<string>).data) as LiveEvent;
      setEvents((prev) => [ev, ...prev].slice(0, max));
    });
    return () => es.close();
  }, [max]);

  return {
    events, connected, paused: pausedState,
    setPaused: (p: boolean) => { paused.current = p; setPausedState(p); },
    clear: () => setEvents([]),
  };
}
