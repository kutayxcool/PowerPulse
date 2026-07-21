package com.powerpulse.sensors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * PowerPulse - Telemetry Sensors Simulator
 * <p>
 * Gercek bir sensor/API olmadigi icin bu servis ev/cihaz basina sahte (simule)
 * anlik guc tuketimi (wattage) uretip Kafka'daki "telemetry" topic'ine
 * CONTRACTS.md'de tanimli JSON semasiyla gonderir.
 */
@SpringBootApplication
@EnableScheduling
public class SensorsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SensorsApplication.class, args);
    }
}

@Configuration
class JacksonConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

/**
 * CONTRACTS.md 1.1 - "telemetry" Kafka mesaj semasi.
 * homeId/applianceId UUID string olarak tutulur (ekip karari, bkz. CONTRACTS.md bolum 0).
 */
record TelemetryPayload(
        String homeId,
        String applianceId,
        String applianceName,
        double wattage,
        String timestamp
) {
}

record SimulatedAppliance(String id, String name, double baseWattage, double variance) {
}

record SimulatedHome(String id, String name, List<SimulatedAppliance> appliances) {
}

/**
 * Demo veriler. UUID'ler sabit tutuldu ki Core/AI/Web App tarafinda
 * uctan uca test ederken herkes ayni ev/cihaz kimlikleriyle calissin.
 */
final class DemoData {
    private DemoData() {
    }

    static List<SimulatedHome> homes() {
        return List.of(
                new SimulatedHome("11111111-1111-1111-1111-111111111111", "Kadıköy Evi", List.of(
                        new SimulatedAppliance("21111111-1111-1111-1111-111111111111", "Klima", 1200, 300),
                        new SimulatedAppliance("21111111-1111-1111-1111-111111111112", "Buzdolabı", 180, 20),
                        new SimulatedAppliance("21111111-1111-1111-1111-111111111113", "Çamaşır Makinesi", 900, 100)
                )),
                new SimulatedHome("11111111-1111-1111-1111-111111111112", "Beşiktaş Evi", List.of(
                        new SimulatedAppliance("22222222-2222-2222-2222-222222222221", "Klima", 1500, 350),
                        new SimulatedAppliance("22222222-2222-2222-2222-222222222222", "Fırın", 2000, 200),
                        new SimulatedAppliance("22222222-2222-2222-2222-222222222223", "Televizyon", 120, 15)
                ))
        );
    }
}

@Service
class TelemetryProducer {
    private static final String TOPIC = "telemetry";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    TelemetryProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    void send(TelemetryPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            // Partition key olarak homeId kullanilir; ayni evin mesajlari sirali islenir.
            kafkaTemplate.send(TOPIC, payload.homeId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Telemetry mesaji JSON'a cevrilemedi", e);
        }
    }
}

@Component
class TelemetrySimulator {
    private static final Logger log = LoggerFactory.getLogger(TelemetrySimulator.class);

    private final TelemetryProducer producer;
    private final List<SimulatedHome> homes = DemoData.homes();
    private final Random random = new Random();

    TelemetrySimulator(TelemetryProducer producer) {
        this.producer = producer;
    }

    @Scheduled(fixedRateString = "${powerpulse.sensors.interval-ms:5000}")
    void tick() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        for (SimulatedHome home : homes) {
            for (SimulatedAppliance appliance : home.appliances()) {
                double wattage = simulateWattage(appliance);
                TelemetryPayload payload = new TelemetryPayload(
                        home.id(), appliance.id(), appliance.name(), wattage, timestamp);
                producer.send(payload);
                log.info("Telemetry gonderildi: {} - {} -> {} W", home.name(), appliance.name(), wattage);
            }
        }
    }

    private double simulateWattage(SimulatedAppliance appliance) {
        double noise = (random.nextDouble() * 2 - 1) * appliance.variance();
        double wattage = appliance.baseWattage() + noise;
        // %5 ihtimalle anomali uret - AI Advisory Service'in anomali tespitini test edebilmek icin
        if (random.nextDouble() < 0.05) {
            wattage *= 2.5;
        }
        return Math.max(0, Math.round(wattage * 10.0) / 10.0);
    }
}
