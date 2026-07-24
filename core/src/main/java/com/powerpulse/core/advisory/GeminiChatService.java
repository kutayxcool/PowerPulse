package com.powerpulse.core.advisory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// GeminiEnergyAdvisoryService'e benzer sekilde Gemini'yi dogrudan cagirir,
// ama JSON semasi bekleyip parse etmek yerine serbest metin (chat cevabi)
// dondurur - "Gemini Asistanina Sor" ozelligi icin kullanilir.
@Service
public class GeminiChatService {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiChatService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public GeminiChatService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String ask(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiAdvisoryUnavailableException(
                    "GEMINI_API_KEY tanımlı değildir."
            );
        }

        try {
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
                                    List.of(Map.of("text", prompt))
                            )
                    ),
                    "generationConfig",
                    Map.of("temperature", 0.7)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    url,
                    requestBody,
                    Map.class
            );

            return extractText(response).trim();
        } catch (AiAdvisoryUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Gemini sohbet çağrısı başarısız oldu.", exception);

            throw new AiAdvisoryUnavailableException(
                    "Cevap alınamadı.",
                    exception
            );
        }
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
                (Map<String, Object>) firstCandidate.get("content");

        List<Object> parts = (List<Object>) content.get("parts");

        Map<String, Object> firstPart =
                (Map<String, Object>) parts.get(0);

        return (String) firstPart.get("text");
    }
}
