package com.cardflow.platform.api;

import com.cardflow.platform.processing.LiveEventBus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/stream")
public class StreamController {
    private final LiveEventBus bus;

    public StreamController(LiveEventBus bus) { this.bus = bus; }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() { return bus.subscribe(); }
}
