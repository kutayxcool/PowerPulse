package com.powerpulse.core.advisory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiEnergyAdvisoryService
        implements EnergyAdvisoryService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GeminiEnergyAdvisoryService.class
            );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiEnergyAdvisoryService(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AdvisoryResult generateAdvisory(
            EnergyAdvisoryContext context
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiAdvisoryUnavailableException(
                    "GEMINI_API_KEY tanımlı değildir."
            );
        }

        try {
            String prompt = buildPrompt(context);

            String url = """
                    https://generativelanguage.googleapis.com/\
                    v1beta/models/%s:generateContent?key=%s
                    """
                    .formatted(model, apiKey)
                    .replaceAll("\\s+", "");

            Map<String, Object> requestBody = Map.of(
                    "contents",
                    List.of(
                            Map.of(
                                    "parts",
                                    List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.postForObject(
                            url,
                            requestBody,
                            Map.class
                    );

            String rawText = extractText(response);
            String json = stripMarkdownFences(rawText);

            return objectMapper.readValue(
                    json,
                    AdvisoryResult.class
            );
        } catch (AiAdvisoryUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(
                    "Gemini AI çağrısı başarısız oldu.",
                    exception
            );

            throw new AiAdvisoryUnavailableException(
                    "AI tavsiyesi oluşturulamadı.",
                    exception
            );
        }
    }

    private String buildPrompt(EnergyAdvisoryContext context) {
        String anomalyText = context.anomalies().isEmpty()
                ? "Belirgin bir cihaz anomalisi yok."
                : context.anomalies()
                .stream()
                .map(anomaly -> "%s cihazı art arda %d kez "
                        .formatted(
                                anomaly.applianceName(),
                                anomaly.consecutiveBreaches()
                        )
                        + "güvenli limitin üzerinde tüketim yaptı"
                )
                .reduce((first, second) ->
                        first + "; " + second)
                .orElse("");

        return """
                Sen PowerPulse enerji takip uygulamasının
                enerji tasarrufu danışmanısın.

                Aşağıdaki ev için kısa ve uygulanabilir enerji
                tasarrufu tavsiyeleri üret.

                Ev: %s
                Toplam tüketim: %.4f kWh
                Aylık kota: %.4f kWh
                Güncel fatura: %.2f TL
                Kota aşıldı mı: %s
                Anomaliler: %s

                Yalnızca aşağıdaki şemaya uygun geçerli JSON döndür.
                Markdown veya ek açıklama kullanma.

                {
                  "title": "kısa başlık",
                  "recommendations": [
                    "öneri 1",
                    "öneri 2",
                    "öneri 3"
                  ],
                  "estimatedSavingPercentage": 10,
                  "estimatedSavingAmount": 25.50
                }
                """
                .formatted(
                        context.homeName(),
                        context.totalConsumptionKwh(),
                        context.budgetQuotaKwh(),
                        context.currentBillAmount(),
                        context.quotaBreached()
                                ? "evet"
                                : "hayır",
                        anomalyText
                );
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            throw new AiAdvisoryUnavailableException(
                    "Gemini boş cevap döndürdü."
            );
        }

        List<Object> candidates =
                (List<Object>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new AiAdvisoryUnavailableException(
                    "Gemini cevabında aday bulunamadı."
            );
        }

        Map<String, Object> firstCandidate =
                (Map<String, Object>) candidates.get(0);

        Map<String, Object> content =
                (Map<String, Object>)
                        firstCandidate.get("content");

        List<Object> parts =
                (List<Object>) content.get("parts");

        Map<String, Object> firstPart =
                (Map<String, Object>) parts.get(0);

        return (String) firstPart.get("text");
    }

    private String stripMarkdownFences(String text) {
        if (text == null || text.isBlank()) {
            throw new AiAdvisoryUnavailableException(
                    "Gemini boş tavsiye metni döndürdü."
            );
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                    .replaceFirst("^```(?:json)?", "")
                    .trim();

            if (cleaned.endsWith("```")) {
                cleaned = cleaned
                        .substring(0, cleaned.length() - 3)
                        .trim();
            }
        }

        return cleaned;
    }
}