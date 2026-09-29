package com.cardflow.platform.config;

import com.cardflow.platform.repo.OpsRepository;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology: events exchange -> quorum work queue (dead-letters to DLX -> DLQ).
 * Listener retry (exponential backoff) is configured in application.yml; once exhausted the recoverer below
 * persists the poison message for the ops UI and rejects it (no requeue) so the broker parks it in the DLQ.
 */
@Configuration
public class AmqpConfig {
    public static final String EXCHANGE = "cardflow.events";
    public static final String QUEUE = "cardflow.events.process";
    public static final String DLX = "cardflow.events.dlx";
    public static final String DLQ = "cardflow.events.dlq";
    public static final String ROUTING_KEY = "card.event";

    @Bean TopicExchange eventsExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(DLX, true, false); }

    @Bean Queue processQueue() {
        return QueueBuilder.durable(QUEUE).quorum()
                .withArguments(Map.of("x-dead-letter-exchange", DLX, "x-dead-letter-routing-key", "dead"))
                .build();
    }
    @Bean Queue deadLetterQueue() { return QueueBuilder.durable(DLQ).quorum().build(); }

    @Bean Binding processBinding(Queue processQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(processQueue).to(eventsExchange).with("card.#");
    }
    @Bean Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dead");
    }

    @Bean MessageRecoverer poisonMessageRecoverer(OpsRepository ops) {
        return (message, cause) -> {
            String id = message.getMessageProperties().getMessageId();
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            Throwable root = cause.getCause() != null ? cause.getCause() : cause;
            ops.saveDeadLetter(id == null ? "unknown" : id, body, String.valueOf(root.getMessage()));
            new RejectAndDontRequeueRecoverer().recover(message, cause);
        };
    }
}
