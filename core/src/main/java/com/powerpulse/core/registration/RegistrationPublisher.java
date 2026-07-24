package com.powerpulse.core.registration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RegistrationPublisher {

    private static final String REGISTRATION_TOPIC = "registration";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RegistrationPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(HomeRegistrationEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            kafkaTemplate
                    .send(REGISTRATION_TOPIC, event.homeId(), json)
                    .get(10, TimeUnit.SECONDS);

        } catch (JsonProcessingException e) {
            throw new RegistrationPublishException(
                    "Registration mesajı JSON formatına dönüştürülemedi.",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RegistrationPublishException(
                    "Registration mesajı gönderilirken işlem kesildi.",
                    e
            );
        } catch (ExecutionException | TimeoutException e) {
            throw new RegistrationPublishException(
                    "Registration mesajı Kafka'ya gönderilemedi.",
                    e
            );
        }
    }
}