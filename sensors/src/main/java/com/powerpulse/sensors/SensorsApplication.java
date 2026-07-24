package com.powerpulse.sensors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PowerPulse - Telemetry Sensors Simulator
 * <p>
 * Gercek bir sensor/API olmadigi icin bu servis ev/cihaz basina sahte (simule)
 * anlik guc tuketimi (wattage) uretip Kafka'daki "telemetry" topic'ine
 * CONTRACTS.md'de tanimli JSON semasiyla gonderir. Ayrica her cihazin en son
 * degerini Apache Ignite'ta (in-memory cache) tutar ki hizli okuma gerektiren
 * servisler (ör. Core) veritabanina/Kafka'ya gitmeden en guncel degeri gorebilsin.
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

@Configuration
class IgniteConfig {

    @Bean(destroyMethod = "close")
    IgniteClient igniteClient(@Value("${ignite.address}") String address) {
        ClientConfiguration cfg = new ClientConfiguration().setAddresses(address);
        return Ignition.startClient(cfg);
    }
}

@Configuration
class RestClientConfig {
    // Core artik /api/internal/** disindaki her uc icin JWT istiyor;
    // /api/internal/** ise ayri bir paylasilan sifre (shared secret)
    // header'i ("X-Internal-Api-Key") bekliyor. Bu interceptor, bu
    // RestTemplate ile Core'a giden HER istege o header'i otomatik
    // ekler - CoreHomeRegistry'deki cagri kodunda degisiklik gerekmez.
    @Bean
    RestTemplate restTemplate(
            @Value("${powerpulse.core.internal-api-key}") String internalApiKey
    ) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-Internal-Api-Key", internalApiKey);
            return execution.execute(request, body);
        });
        return restTemplate;
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
 * Demo veriler. UUID'ler, Core'un POST /api/homes/register ile local
 * veritabaninda gercekten olusturdugu ev/cihaz kayitlariyla birebir
 * eslesecek sekilde sabitlendi (23.07.2026 tarihinde local'de kaydedildi).
 * Core'un veritabani sifirlanir/yeniden kaydedilirse bu ID'lerin de
 * guncellenmesi gerekir.
 */
final class DemoData {
    private DemoData() {
    }

    static List<SimulatedHome> homes() {
        return List.of(
                new SimulatedHome("efd05518-805a-4e13-8494-15b5f637da62", "Kadıköy Evi", List.of(
                        new SimulatedAppliance("3fe52554-d6dc-4c0a-acef-c58212618e7c", "Klima", 1200, 300),
                        new SimulatedAppliance("a7181c81-c8d2-46a2-9026-28bd048d8ed1", "Buzdolabı", 180, 20),
                        new SimulatedAppliance("84b27a97-b759-4518-921f-05f3f7bc56f8", "Çamaşır Makinesi", 900, 100)
                )),
                new SimulatedHome("9a26b1fe-148a-4d83-b221-d07f5fa5c87b", "Beşiktaş Evi", List.of(
                        new SimulatedAppliance("204154a9-9188-4bd8-8223-a61b50bc76a8", "Klima", 1500, 350),
                        new SimulatedAppliance("3f5ab8b1-4fd2-4ad4-9018-5bded694acec", "Fırın", 2000, 200),
                        new SimulatedAppliance("8a68108e-3c95-45a8-8260-488465fd25b5", "Televizyon", 120, 15)
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

/**
 * Her cihazin en son telemetry degerini Ignite'ta ("latest_telemetry" cache)
 * "homeId:applianceId" anahtariyla saklar. Amac: Core gibi tuketiciler
 * Kafka'nin tum gecmisini taramadan "su an ne kadar tuketiyor" sorusuna
 * hizlica cevap alabilsin.
 */
@Service
class IgniteTelemetryCache {
    private static final Logger log = LoggerFactory.getLogger(IgniteTelemetryCache.class);
    private static final String CACHE_NAME = "latest_telemetry";

    private final IgniteClient igniteClient;
    private final ObjectMapper objectMapper;

    IgniteTelemetryCache(IgniteClient igniteClient, ObjectMapper objectMapper) {
        this.igniteClient = igniteClient;
        this.objectMapper = objectMapper;
    }

    void cacheLatest(TelemetryPayload payload) {
        try {
            ClientCache<String, String> cache = igniteClient.getOrCreateCache(CACHE_NAME);
            String key = payload.homeId() + ":" + payload.applianceId();
            cache.put(key, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            // Ignite gecici olarak erisilemez olsa bile telemetry akisi durmasin,
            // sadece logla ve devam et.
            log.warn("Ignite'a yazilamadi (key={}): {}", payload.homeId(), e.getMessage());
        }
    }
}

/**
 * Core'un GET /api/homes ve GET /api/homes/{id} yanitlarindan sadece
 * ihtiyacimiz olan alanlari okur; digerleri (consumption, bill, quota vb.)
 * Sensors'i ilgilendirmedigi icin @JsonIgnoreProperties(ignoreUnknown = true)
 * ile yok sayilir.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record CoreHomeSummary(String id) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record CoreApplianceDetail(String id, String name, BigDecimal safeLimitWatt) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record CoreHomeDetail(String id, String name, List<CoreApplianceDetail> appliances) {
}

/**
 * Sensors'in "hangi ev/cihazlari simule edecegim" listesini artik kod icine
 * gomulu (hardcoded) DemoData yerine, gercek zamanli olarak Core'dan ceker.
 * Boylece web app uzerinden "Evi Duzenle" ile eklenen/silinen cihazlar da
 * otomatik olarak simulasyona dahil olur/cikar - Core tarafinda hicbir
 * degisiklik gerekmez, tamamen Sensors modulu icinde cozulur.
 * <p>
 * Core baslangicta ayakta degilse veya gecici olarak erisilemezse en son
 * bilinen liste korunur; hic basarili senkronizasyon olmadiysa (uygulama
 * daha yeni ayaga kalktiysa ve Core henuz acilmadiysa) DemoData'daki 2
 * sabit ev gecici bir fallback olarak kullanilir, boylece Sensors hicbir
 * zaman telemetry uretmeyi tamamen durdurmaz.
 */
@Component
class CoreHomeRegistry {
    private static final Logger log = LoggerFactory.getLogger(CoreHomeRegistry.class);

    private final RestTemplate restTemplate;
    private final String coreBaseUrl;
    private volatile List<SimulatedHome> homes = DemoData.homes();
    private volatile boolean everSucceeded = false;

    CoreHomeRegistry(
            RestTemplate restTemplate,
            @Value("${powerpulse.core.base-url:http://localhost:8080/api}") String coreBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.coreBaseUrl = coreBaseUrl;
    }

    List<SimulatedHome> currentHomes() {
        return homes;
    }

    @Scheduled(
            fixedRateString = "${powerpulse.core.sync-interval-ms:15000}",
            initialDelay = 0
    )
    void refresh() {
        try {
            // /homes DEGIL /internal/homes: normal /api/homes artik JWT ile
            // korunuyor ve sadece o an giris yapmis TEK bir kullanicinin
            // evlerini donuyor - Sensors ise sistemdeki TUM kullanicilarin
            // evlerini simule etmesi gerektigi icin Core'un ayri, sistem
            // geneli /api/internal/** ucunu (paylasilan sifreyle korunan)
            // kullanir (bkz. RestClientConfig).
            CoreHomeSummary[] summaries = restTemplate.getForObject(
                    coreBaseUrl + "/internal/homes", CoreHomeSummary[].class);

            if (summaries == null || summaries.length == 0) {
                log.info("Core'da kayitli ev bulunamadi, mevcut liste korunuyor ({} ev).", homes.size());
                return;
            }

            List<SimulatedHome> refreshed = new ArrayList<>();
            for (CoreHomeSummary summary : summaries) {
                CoreHomeDetail detail = restTemplate.getForObject(
                        coreBaseUrl + "/internal/homes/" + summary.id(), CoreHomeDetail.class);

                if (detail == null || detail.appliances() == null) {
                    continue;
                }

                List<SimulatedAppliance> appliances = detail.appliances().stream()
                        .map(CoreHomeRegistry::toSimulatedAppliance)
                        .toList();

                refreshed.add(new SimulatedHome(detail.id(), detail.name(), appliances));
            }

            if (!refreshed.isEmpty()) {
                homes = refreshed;
                everSucceeded = true;
                log.info(
                        "Core'dan ev/cihaz listesi guncellendi: {} ev, {} cihaz.",
                        refreshed.size(),
                        refreshed.stream().mapToInt(h -> h.appliances().size()).sum()
                );
            }
        } catch (RestClientException e) {
            if (everSucceeded) {
                log.warn("Core'dan liste yenilenemedi, en son bilinen liste kullaniliyor: {}", e.getMessage());
            } else {
                log.warn(
                        "Core'a henuz ulasilamiyor ({}), gecici olarak DemoData sabit verisiyle devam ediliyor.",
                        e.getMessage()
                );
            }
        }
    }

    // Core, cihazlar icin sadece guvenli ust limit (safeLimitWatt) tutar; simulasyon
    // icin makul bir ortalama tuketim (baseWattage) ve dalgalanma (variance) turetiyoruz.
    private static SimulatedAppliance toSimulatedAppliance(CoreApplianceDetail appliance) {
        double safeLimit = appliance.safeLimitWatt() == null ? 1000 : appliance.safeLimitWatt().doubleValue();
        double baseWattage = safeLimit * 0.7;
        double variance = safeLimit * 0.15;
        return new SimulatedAppliance(appliance.id(), appliance.name(), baseWattage, variance);
    }
}

@Component
class TelemetrySimulator {
    private static final Logger log = LoggerFactory.getLogger(TelemetrySimulator.class);

    private final TelemetryProducer producer;
    private final IgniteTelemetryCache igniteCache;
    private final CoreHomeRegistry coreHomeRegistry;
    private final Random random = new Random();

    TelemetrySimulator(TelemetryProducer producer, IgniteTelemetryCache igniteCache, CoreHomeRegistry coreHomeRegistry) {
        this.producer = producer;
        this.igniteCache = igniteCache;
        this.coreHomeRegistry = coreHomeRegistry;
    }

    @Scheduled(fixedRateString = "${powerpulse.sensors.interval-ms:5000}")
    void tick() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        List<SimulatedHome> homes = coreHomeRegistry.currentHomes();
        for (SimulatedHome home : homes) {
            for (SimulatedAppliance appliance : home.appliances()) {
                double wattage = simulateWattage(appliance);
                TelemetryPayload payload = new TelemetryPayload(
                        home.id(), appliance.id(), appliance.name(), wattage, timestamp);
                producer.send(payload);
                igniteCache.cacheLatest(payload);
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
