package com.cardflow.platform.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.cardflow.platform.ingest.SignatureVerifier;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** End-to-end against real Postgres + RabbitMQ: webhook -> outbox -> broker -> idempotent consumer -> curated table. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PipelineIT {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.rabbitmq.host", rabbit::getHost);
        r.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        r.add("cardflow.lake.export-interval-ms", () -> "3600000");
    }

    @LocalServerPort int port;
    @Autowired SignatureVerifier verifier;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void setUp() { RestAssured.port = port; }

    private static String event(String id, String amount, String currency) {
        return """
            {"eventId":"%s","schemaVersion":1,"eventType":"AUTHORIZATION","cardToken":"tok_ABCDEFGH1234",
             "merchantName":"Cafe Aroma","mcc":"5814","amount":%s,"currency":"%s","country":"US","occurredAt":"%s"}
            """.formatted(id, amount, currency, Instant.now());
    }

    private void post(String body, int expectedStatus) {
        given().contentType(ContentType.JSON).header("X-Signature", verifier.sign(body.getBytes(StandardCharsets.UTF_8)))
                .body(body).post("/api/v1/webhooks/card-events").then().statusCode(expectedStatus);
    }

    @Test void rejectsUnsignedWebhook() {
        given().contentType(ContentType.JSON).body(event("x", "1", "USD")).post("/api/v1/webhooks/card-events").then().statusCode(401);
    }

    @Test void duplicateDeliveryHasExactlyOneEffect() {
        String id = UUID.randomUUID().toString();
        String body = event(id, "42.10", "USD");
        post(body, 202);
        post(body, 202);   // provider retry
        post(body, 202);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT count(*) FROM card_transaction WHERE event_id = ?", Long.class, id)).isEqualTo(1L));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE event_id = ?", Long.class, id)).isEqualTo(1L);
    }

    @Test void invalidEventIsQuarantinedNotLost() {
        String id = UUID.randomUUID().toString();
        post(event(id, "10", "XXA"), 202);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT failed_rules FROM dq_quarantine WHERE event_id = ?", String.class, id))
                        .contains("CURRENCY_ISO4217"));
        given().get("/api/v1/transactions?q=" + id).then().statusCode(200).body("total", equalTo(0));
    }
}
