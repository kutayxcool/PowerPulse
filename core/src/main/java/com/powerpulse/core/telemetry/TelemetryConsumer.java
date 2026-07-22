package com.powerpulse.core.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryConsumer.class);

    private final ObjectMapper objectMapper;
    private final TelemetryProcessingService processingService;

    public TelemetryConsumer(
            ObjectMapper objectMapper,
            TelemetryProcessingService processingService
    ) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${powerpulse.kafka.telemetry-topic:telemetry}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String json) {
        try {
            TelemetryPayload payload = objectMapper.readValue(
                    json,
                    TelemetryPayload.class
            );

            processingService.process(payload);

        } catch (JsonProcessingException exception) {
            log.error(
                    "Geçersiz telemetry JSON mesajı atlandı: {}",
                    json,
                    exception
            );
        } catch (IllegalArgumentException exception) {
            log.error(
                    "Geçersiz telemetry değeri atlandı: {}",
                    exception.getMessage()
            );
        }
    }
}