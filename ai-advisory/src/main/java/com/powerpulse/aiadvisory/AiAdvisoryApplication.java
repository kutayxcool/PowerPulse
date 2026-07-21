package com.powerpulse.aiadvisory;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * PowerPulse - AI Advisory Service (Kutay)
 * <p>
 * Core'un CONTRACTS.md'de tanimli /api/ai/recommendation endpoint'ini karsilarken
 * cagirdigi ic (internal) servis. Google Gemini API'ye baglanip enerji tasarrufu
 * tavsiyesi uretir. CONTRACTS.md bolum 3 - EnergyAdvisoryService sozlesmesini uygular.
 */
@SpringBootApplication
public class AiAdvisoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAdvisoryApplication.class, args);
    }
}

/**
 * CONTRACTS.md bolum 3 - EnergyAdvisoryContext (Core -> AI Advisory Service).
 */
record EnergyAdvisoryContext(
        String homeId,
        String homeName,
        double totalConsumptionKwh,
        double budgetQuotaKwh,
        double currentBillAmount,
        boolean quotaBreached,
        List<ApplianceAnomaly> anomalies
) {
}

record ApplianceAnomaly(String applianceName, int consecutiveBreaches) {
}

/**
 * CONTRACTS.md bolum 3 - AdvisoryResult (AI Advisory Service -> Core).
 */
record AdvisoryResult(
        String title,
        List<String> recommendations,
        double estimatedSavingPercentage,
        double estimatedSavingAmount
) {
}

interface EnergyAdvisoryService {
    AdvisoryResult generateAdvisory(EnergyAdvisoryContext context);
}

class AiAdvisoryUnavailableException extends RuntimeException {
    AiAdvisoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

@Service
class GeminiEnergyAdvisoryService implements EnergyAdvisoryService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEnergyAdvisoryService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    GeminiEnergyAdvisoryService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public AdvisoryResult generateAdvisory(EnergyAdvisoryContext context) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiAdvisoryUnavailableException("GEMINI_API_KEY tanimli degil", null);
        }
        try {
            String prompt = buildPrompt(context);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s"
                    .formatted(model, apiKey);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);
            String rawText = extractText(response);
            String json = stripMarkdownFences(rawText);
            return objectMapper.readValue(json, AdvisoryResult.class);
        } catch (Exception e) {
            log.error("Gemini AI cagrisi basarisiz oldu", e);
            throw new AiAdvisoryUnavailableException("AI tavsiyesi olusturulamadi", e);
        }
    }

    private String buildPrompt(EnergyAdvisoryContext context) {
        String anomalyText = context.anomalies().isEmpty()
                ? "Belirgin bir anomali yok."
                : context.anomalies().stream()
                .map(a -> "%s cihazi ust uste %d kez normalin uzerinde tuketim yapti".formatted(
                        a.applianceName(), a.consecutiveBreaches()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("");

        return """
                Sen PowerPulse adli bir enerji tuketim takip uygulamasinin AI danismanisin.
                Asagidaki ev icin enerji tasarrufu tavsiyesi uret ve SADECE asagidaki JSON semasina
                uygun, gecerli bir JSON dondur. Baska hicbir metin, aciklama veya markdown ekleme.

                Ev: %s
                Toplam tuketim: %.1f kWh
                Kota: %.1f kWh
                Guncel fatura: %.2f TL
                Kota asildi mi: %s
                Anomaliler: %s

                JSON semasi:
                {
                  "title": "kisa baslik",
                  "recommendations": ["oneri 1", "oneri 2", "oneri 3"],
                  "estimatedSavingPercentage": sayi,
                  "estimatedSavingAmount": sayi
                }
                """.formatted(
                context.homeName(),
                context.totalConsumptionKwh(),
                context.budgetQuotaKwh(),
                context.currentBillAmount(),
                context.quotaBreached() ? "evet" : "hayir",
                anomalyText
        );
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        List<Object> candidates = (List<Object>) response.get("candidates");
        Map<String, Object> firstCandidate = (Map<String, Object>) candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
        List<Object> parts = (List<Object>) content.get("parts");
        Map<String, Object> firstPart = (Map<String, Object>) parts.get(0);
        return (String) firstPart.get("text");
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}

/**
 * Core'un cagiracagi ic endpoint. Public REST sozlesmesi (GET /api/ai/recommendation)
 * Core tarafinda tanimli; bu servis sadece Core'un arkada cagirdigi internal API'dir.
 */
@RestController
@RequestMapping("/internal/advisory")
class AdvisoryController {

    private final EnergyAdvisoryService advisoryService;

    AdvisoryController(EnergyAdvisoryService advisoryService) {
        this.advisoryService = advisoryService;
    }

    @PostMapping
    public AdvisoryResult generate(@RequestBody EnergyAdvisoryContext context) {
        return advisoryService.generateAdvisory(context);
    }
}

@RestControllerAdvice
class AdvisoryExceptionHandler {

    @ExceptionHandler(AiAdvisoryUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(AiAdvisoryUnavailableException ex,
                                                                   HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "status", 503,
                "error", "Service Unavailable",
                "message", "AI servisi su anda kullanilamiyor: " + ex.getMessage(),
                "path", request.getRequestURI(),
                "timestamp", OffsetDateTime.now().toString()
        );
        return ResponseEntity.status(503).body(body);
    }
}
