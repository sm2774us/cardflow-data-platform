package com.cardflow.platform.processing;

import com.cardflow.platform.config.AmqpConfig;
import java.nio.charset.StandardCharsets;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Thin adapter: broker message -> EventProcessor. Exceptions propagate so retry/backoff/DLQ policy applies. */
@Component
public class CardEventListener {
    private final EventProcessor processor;

    public CardEventListener(EventProcessor processor) { this.processor = processor; }

    @RabbitListener(queues = AmqpConfig.QUEUE)
    public void onMessage(Message message) {
        processor.process(message.getMessageProperties().getMessageId(), new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
