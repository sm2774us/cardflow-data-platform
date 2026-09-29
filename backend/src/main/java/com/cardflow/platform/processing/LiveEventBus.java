package com.cardflow.platform.processing;

import com.cardflow.platform.domain.Records.LiveEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** In-process fan-out of pipeline outcomes to SSE clients (per-replica; a shared broker fan-out would replace this at scale). */
@Component
public class LiveEventBus {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter e = new SseEmitter(0L);
        emitters.add(e);
        e.onCompletion(() -> emitters.remove(e));
        e.onTimeout(() -> emitters.remove(e));
        e.onError(t -> emitters.remove(e));
        return e;
    }

    public void publish(LiveEvent ev) {
        for (SseEmitter e : emitters) {
            try { e.send(SseEmitter.event().name("pipeline").data(ev)); }
            catch (IOException | IllegalStateException ex) { emitters.remove(e); }
        }
    }
}
